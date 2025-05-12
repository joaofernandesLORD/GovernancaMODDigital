import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;

// Classe de painel informativo
class PainelInfoLinha extends JPanel {
    private JLabel lblTotalMODs;
    private JLabel lblValorMODs;
    private JLabel lblProducaoHora;
    private JLabel lblValorProducaoHora;
    private JLabel lblProducaoDia;
    private JLabel lblValorProducaoDia;
    private JLabel lblTotalPostos;
    private JLabel lblValorTotalPostos;

    public PainelInfoLinha(String titulo) {
        setLayout(new GridLayout(1, 0, 0, 0));
        setBorder(BorderFactory.createEmptyBorder(-60, 180, 1, 1));
        setBackground(CorFundo.FUNDO_SECUNDARIO);

        // Total de MODs
        lblTotalMODs = new JLabel("Total de MODs:");
        lblTotalMODs.setFont(new Font("Arial", Font.BOLD, 10));
        add(lblTotalMODs);

        lblValorMODs = new JLabel("0");
        lblValorMODs.setFont(new Font("Arial", Font.PLAIN, 10));
        add(lblValorMODs);

        // Produção/Hora
        lblProducaoHora = new JLabel("Produção/Hora:");
        lblProducaoHora.setFont(new Font("Arial", Font.BOLD, 10));
        add(lblProducaoHora);

        lblValorProducaoHora = new JLabel("0");
        lblValorProducaoHora.setFont(new Font("Arial", Font.PLAIN, 10));
        add(lblValorProducaoHora);

        // Produção/Dia
        lblProducaoDia = new JLabel("Produção/Dia:");
        lblProducaoDia.setFont(new Font("Arial", Font.BOLD, 10));
        add(lblProducaoDia);

        lblValorProducaoDia = new JLabel("0");
        lblValorProducaoDia.setFont(new Font("Arial", Font.PLAIN, 10));
        add(lblValorProducaoDia);

        // Total de Postos
        lblTotalPostos = new JLabel("Total de Postos:");
        lblTotalPostos.setFont(new Font("Arial", Font.BOLD, 10));
        add(lblTotalPostos);

        lblValorTotalPostos = new JLabel("0");
        lblValorTotalPostos.setFont(new Font("Arial", Font.PLAIN, 10));
        add(lblValorTotalPostos);
    }

    public void atualizarInformacoes(List<String> postos) {
        int totalMODs = calcularTotalMODs(postos);
        double producaoHora = calcularProducaoHora(postos);
        double producaoDia = producaoHora * 8;
        int totalPostos = postos.size();

        lblValorMODs.setText(String.valueOf(totalMODs));
        lblValorProducaoHora.setText(String.format("%.2f", producaoHora));
        lblValorProducaoDia.setText(String.format("%.2f", producaoDia));
        lblValorTotalPostos.setText(String.valueOf(totalPostos));
    }

    private int calcularTotalMODs(List<String> postos) {
        int totalMODs = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader("postoMestre.csv"))) {
            String linha;
            reader.readLine(); // Pula cabeçalho

            while ((linha = reader.readLine()) != null) {
                String[] dados = linha.split(",");
                if (dados.length > 1 && postos.contains(dados[1].trim())) {
                    if (dados.length > 2 && !dados[2].trim().isEmpty()) {
                        try {
                            totalMODs += Integer.parseInt(dados[2].trim());
                        } catch (NumberFormatException e) {
                            // Ignora valores não numéricos
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao ler arquivo postoMestre.csv: " + e.getMessage());
        }

        return totalMODs;
    }

    private double calcularProducaoHora(List<String> postos) {
        double producaoTotal = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader("postoMestre.csv"))) {
            String linha;
            reader.readLine(); // Pula cabeçalho

            while ((linha = reader.readLine()) != null) {
                String[] dados = linha.split(",");
                if (dados.length > 1 && postos.contains(dados[1].trim())) {
                    if (dados.length > 6 && !dados[6].trim().isEmpty()) { // CAPHora está na coluna 6
                        try {
                            producaoTotal += Double.parseDouble(dados[6].trim());
                        } catch (NumberFormatException e) {
                            // Ignora valores não numéricos
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao ler arquivo postoMestre.csv: " + e.getMessage());
        }

        return producaoTotal;
    }
}

class PainelLA extends JPanel {
    DefaultListModel<String> postosSelecionadosModel;
    private JList<String> listaPostosSelecionados;
    private JScrollPane scrollPostosSelecionados;
    private Stack<TelaSystem.PostoExcluido> historicoExclusoes;
    private JButton btnDesfazer;
    private boolean modoSelecaoAtivo = false;
    private Timer timer;
    private JComboBox<String> comboModelos;
    private List<String> modelosDisponiveis;
    private JLabel statusProducao;
    private PainelInfoLinha painelInfoLinha;

    public PainelLA(DefaultListModel<String> model,
            Stack<TelaSystem.PostoExcluido> historico,
            List<String> modelos,
            JLabel statusProducao,
            PainelInfoLinha painelInfoLinha) { // Adicionado novo parâmetro

        this.postosSelecionadosModel = model;
        this.historicoExclusoes = historico;
        this.statusProducao = statusProducao;
        this.modelosDisponiveis = modelos;
        this.painelInfoLinha = painelInfoLinha; // Armazena a referência

        setLayout(new BorderLayout());
        configurarPainel();

        // Atualiza o painel de informações com a lista inicial (vazia)
        List<String> postos = Collections.list(model.elements());
        painelInfoLinha.atualizarInformacoes(postos);
    }

    private void configurarPainel() {

        // Painel superior com botões e combobox
        JPanel painelSuperior = new JPanel(new BorderLayout());

        // Painel de botões
        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // Botão Adicionar
        JButton btnAdicionar = new JButton("Adicionar");
        btnAdicionar.setBackground(CorFundo.FUNDO_SECUNDARIO);
        btnAdicionar.setForeground(Color.BLACK);

        btnAdicionar.addActionListener(e -> adicionarPostosPorModelo());

        painelBotoes.add(btnAdicionar);

        // Botão Selecionar
        JButton btnSelecionar = new JButton("Selecionar");
        btnSelecionar.setBackground(TelaSystem.VERMELHO);
        btnSelecionar.setForeground(Color.BLACK);
        btnSelecionar.setEnabled(!postosSelecionadosModel.isEmpty());

        // Botão Excluir
        JButton btnExcluir = new JButton("Excluir");
        btnExcluir.setBackground(CorFundo.FUNDO_SECUNDARIO);
        btnExcluir.setForeground(Color.BLACK);
        btnExcluir.setEnabled(false); // Inicialmente desativado

        // Botão Limpar
        JButton btnLimpar = new JButton("Limpar");
        btnLimpar.setBackground(CorFundo.FUNDO_SECUNDARIO);
        btnLimpar.setForeground(Color.BLACK);
        btnLimpar.setEnabled(!postosSelecionadosModel.isEmpty());

        btnLimpar.addActionListener(e -> {
            if (!modoSelecaoAtivo) {
                historicoExclusoes.clear();
                postosSelecionadosModel.clear();
                btnDesfazer.setEnabled(false);

                statusProducao.setText("Lista de postos foi completamente limpa");
                statusProducao.setForeground(TelaSystem.VERMELHO);
                timerStatus();
            }
        });

        btnSelecionar.addActionListener(e -> {
            modoSelecaoAtivo = !modoSelecaoAtivo;
            btnSelecionar.setBackground(modoSelecaoAtivo ? TelaSystem.VERDE : TelaSystem.VERMELHO);
            btnExcluir.setEnabled(modoSelecaoAtivo); // Ativa/desativa o botão Excluir conforme o modoSelecaoAtivo
            btnLimpar.setEnabled(!modoSelecaoAtivo && !postosSelecionadosModel.isEmpty()); // Desativa o Limpar se

            if (!modoSelecaoAtivo) {
                listaPostosSelecionados.clearSelection();
                btnExcluir.setEnabled(false); // Desativa o botão Excluir quando sai do modo seleção
            }
        });

        btnExcluir.addActionListener(e -> {
            if (modoSelecaoAtivo && listaPostosSelecionados.getSelectedIndex() != -1) {
                int selectedIndex = listaPostosSelecionados.getSelectedIndex();
                String postoExcluido = postosSelecionadosModel.getElementAt(selectedIndex);
                historicoExclusoes.push(new TelaSystem.PostoExcluido(postoExcluido, selectedIndex));
                postosSelecionadosModel.remove(selectedIndex);
                btnDesfazer.setEnabled(!historicoExclusoes.isEmpty());

                statusProducao.setText("Posto '" + postoExcluido + "' removido");
                statusProducao.setForeground(TelaSystem.VERMELHO);
                timerStatus();
            }
        });
        painelBotoes.add(btnSelecionar);
        painelBotoes.add(btnExcluir);

        // Botão Desfazer
        btnDesfazer = new JButton("Desfazer");
        btnDesfazer.setBackground(CorFundo.FUNDO_SECUNDARIO);
        btnDesfazer.setForeground(Color.BLACK);
        btnDesfazer.setEnabled(!historicoExclusoes.isEmpty());

        btnDesfazer.addActionListener(e -> {
            if (!historicoExclusoes.isEmpty()) {
                TelaSystem.PostoExcluido ultimoExcluido = historicoExclusoes.pop();
                postosSelecionadosModel.add(ultimoExcluido.indice, ultimoExcluido.nome);
                listaPostosSelecionados.setSelectedIndex(ultimoExcluido.indice);
                btnDesfazer.setEnabled(!historicoExclusoes.isEmpty());

                statusProducao.setText("Desfeito - '" + ultimoExcluido.nome + "' readicionado");
                statusProducao.setForeground(TelaSystem.VERDE);
                timerStatus();
            }
        });
        painelBotoes.add(btnDesfazer);
        painelBotoes.add(btnLimpar);

        painelSuperior.add(painelBotoes, BorderLayout.NORTH);

        comboModelos = new JComboBox<>();
        comboModelos.setEditable(false);
        comboModelos.setBackground(Color.WHITE);

        // Carrega os modelos específicos para esta linha
        DefaultComboBoxModel<String> comboBoxModel = new DefaultComboBoxModel<>();
        for (String modelo : modelosDisponiveis) {
            comboBoxModel.addElement(modelo);
        }
        comboModelos.setModel(comboBoxModel);

        // Adiciona ActionListener para quando um modelo for selecionado
        comboModelos.addActionListener(e -> {
            String modeloSelecionado = (String) comboModelos.getSelectedItem();
            if (modeloSelecionado != null) {
                statusProducao.setText("Modelo selecionado: " + modeloSelecionado);
                statusProducao.setForeground(TelaSystem.VERDE);
                timerStatus();
            }
        });

        JPanel painelModelo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        painelModelo.add(new JLabel("Modelo: "));
        painelModelo.add(comboModelos);

        painelSuperior.add(painelModelo, BorderLayout.SOUTH);
        add(painelSuperior, BorderLayout.NORTH);

        // Lista de postos selecionados
        listaPostosSelecionados = new JList<>(postosSelecionadosModel);
        listaPostosSelecionados.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaPostosSelecionados.setFont(new Font("Arial", Font.PLAIN, 12));
        listaPostosSelecionados.setFixedCellHeight(20);
        listaPostosSelecionados.setSelectionBackground(new Color(184, 207, 229));
        listaPostosSelecionados.setSelectionForeground(Color.BLACK);
        listaPostosSelecionados.setBackground(Color.WHITE);

        // Adiciona KeyListener para mover itens com setas
        listaPostosSelecionados.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!modoSelecaoAtivo) {
                    return;
                }

                int index = listaPostosSelecionados.getSelectedIndex();
                if (index == -1) {
                    return;
                }

                int newIndex = -1;

                if (e.getKeyCode() == KeyEvent.VK_UP) {
                    if (index > 0) {
                        String item = postosSelecionadosModel.remove(index);
                        postosSelecionadosModel.add(index - 1, item);
                        newIndex = index - 1;
                    }
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    if (index < postosSelecionadosModel.getSize() - 1) {
                        String item = postosSelecionadosModel.remove(index);
                        postosSelecionadosModel.add(index + 1, item);
                        newIndex = index + 1;
                    }
                    e.consume();
                }

                if (newIndex != -1) {
                    listaPostosSelecionados.setSelectedIndex(newIndex);
                    listaPostosSelecionados.ensureIndexIsVisible(newIndex);
                }
            }
        });

        // Adiciona MouseListener para mostrar informações ao clicar
        listaPostosSelecionados.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (modoSelecaoAtivo) {
                    return;
                } else if (e.getClickCount() == 2) {
                    int index = listaPostosSelecionados.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        String postoSelecionado = postosSelecionadosModel.getElementAt(index);
                        String[] infoPosto = buscarInfoPostoNoCSV(postoSelecionado);

                        JPopupMenu popupInfo = new JPopupMenu();

                        // Adiciona item de status (último elemento do array)
                        JMenuItem itemStatus = new JMenuItem("Status: " + infoPosto[infoPosto.length - 1]);
                        itemStatus.setEnabled(false);
                        popupInfo.add(itemStatus);

                        // Adiciona os campos de informação
                        String[] campos = { "MOD",
                                "Ciclo (seg.)",
                                "Lote",
                                "Local",
                                "Rendimento",
                                "UPH",
                                "UPPH",
                                "CAPHora",
                                "CAPTurno",
                                "Ociosidade",
                                "Ocupação" };

                        int[] indices = { 2, 4, 5, 12, 3, 6, 7, 8, 9, 10, 11 };

                        for (int i = 0; i < campos.length; i++) {
                            if (indices[i] < infoPosto.length - 1) { // -1 para ignorar o status
                                JMenuItem item = new JMenuItem(campos[i] + ": " + infoPosto[indices[i]]);
                                item.setEnabled(false);
                                popupInfo.add(item);
                            }
                        }
                        // Adiciona botão de edição
                        JMenuItem itemEditar = new JMenuItem("Editar Posto");
                        itemEditar.addActionListener(ev -> {
                            JanelaEdicaoPosto janelaEdicao = new JanelaEdicaoPosto(
                                    (JFrame) SwingUtilities.getWindowAncestor(PainelLA.this), postoSelecionado);
                            janelaEdicao.setVisible(true);

                            if (janelaEdicao.foiEditado()) {
                                statusProducao.setText("Posto '" + postoSelecionado + "' editado com sucesso");
                                statusProducao.setForeground(TelaSystem.VERDE);
                                timerStatus();
                            }
                        });
                        popupInfo.addSeparator();
                        popupInfo.add(itemEditar);

                        popupInfo.show(listaPostosSelecionados, e.getX(), e.getY() + 20);
                    }
                }
            }
        });

        scrollPostosSelecionados = new JScrollPane(listaPostosSelecionados);
        add(scrollPostosSelecionados, BorderLayout.CENTER);

        // Listener para atualizar estado dos botões
        postosSelecionadosModel.addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) {
                atualizarEstadoBotoes();
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
                atualizarEstadoBotoes();
            }

            @Override
            public void contentsChanged(ListDataEvent e) {
                atualizarEstadoBotoes();
            }

            private void atualizarEstadoBotoes() {
                boolean listaVazia = postosSelecionadosModel.isEmpty();
                boolean itemSelecionado = listaPostosSelecionados.getSelectedIndex() != -1;

                btnSelecionar.setEnabled(!listaVazia);
                btnExcluir.setEnabled(modoSelecaoAtivo && itemSelecionado);
                btnLimpar.setEnabled(!modoSelecaoAtivo && !listaVazia);
                btnDesfazer.setEnabled(!historicoExclusoes.isEmpty());

                // Atualiza o painel de informações
                List<String> postos = Collections.list(postosSelecionadosModel.elements());
                painelInfoLinha.atualizarInformacoes(postos);

                if (listaVazia) {
                    historicoExclusoes.clear();
                    btnDesfazer.setEnabled(false);
                    btnExcluir.setEnabled(false);
                    modoSelecaoAtivo = false;
                    btnSelecionar.setBackground(TelaSystem.VERMELHO);
                    listaPostosSelecionados.clearSelection();
                }
            }
        });
    }

    private void adicionarPostosPorModelo() {
        String modeloSelecionado = (String) comboModelos.getSelectedItem();
        if (modeloSelecionado == null || modeloSelecionado.isEmpty()) {
            statusProducao.setText("Selecione um modelo primeiro");
            statusProducao.setForeground(TelaSystem.VERMELHO);
            timerStatus();
            return;
        }

        // Extrai apenas a parte do modelo (ex: A15 de A15_Campinas)
        String codigoModelo = modeloSelecionado.split("_")[0];

        List<String> postosParaAdicionar = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader("postoMestre.csv"))) {
            String linha;
            reader.readLine(); // Pula cabeçalho

            while ((linha = reader.readLine()) != null) {
                String[] dados = linha.split(",");
                if (dados.length > 0) {
                    String codigoPosto = dados[0].trim();

                    // Verifica se o código do posto contém o código do modelo
                    // Ex: Se modelo é "RL52", procura por "RL52" no CodigoPosto
                    if (codigoPosto.contains(codigoModelo)) {
                        String nomePosto = dados[1].trim();
                        if (!postosParaAdicionar.contains(nomePosto)) {
                            postosParaAdicionar.add(nomePosto);
                        }
                    }
                }
            }
        } catch (IOException e) {
            statusProducao.setText("Erro ao ler arquivo de postos");
            statusProducao.setForeground(TelaSystem.VERMELHO);
            timerStatus();
            return;
        }

        if (postosParaAdicionar.isEmpty()) {
            statusProducao.setText("Nenhum posto encontrado para o modelo " + modeloSelecionado);
            statusProducao.setForeground(TelaSystem.VERMELHO);
            timerStatus();
            return;
        }

        // Adiciona os postos ao modelo
        for (String posto : postosParaAdicionar) {
            if (!postosSelecionadosModel.contains(posto)) {
                postosSelecionadosModel.addElement(posto);
            }
        }

        statusProducao.setText(postosParaAdicionar.size() + " postos adicionados para " + modeloSelecionado);
        statusProducao.setForeground(TelaSystem.VERDE);
        timerStatus();
    }

    private String[] buscarInfoPostoNoCSV(String postoNome) {
        String[] info = { "S.I", "S.I", "S.I", "S.I", "S.I", "S.I", "S.I", "S.I", "S.I", "S.I", "S.I", "S.I", "S.I" };
        boolean editado = false;

        try {
            // Primeiro verifica no posto.csv (arquivo editável)
            String[] infoEditado = buscarInfoNoArquivo("posto.csv", postoNome);

            // Depois verifica no postoMestre.csv para comparação
            String[] infoOriginal = buscarInfoNoArquivo("postoMestre.csv", postoNome);

            // Verifica se houve edição
            if (infoEditado != null && infoOriginal != null) {
                for (int i = 0; i < Math.min(infoEditado.length, infoOriginal.length); i++) {
                    if (!infoEditado[i].equals(infoOriginal[i])) {
                        editado = true;
                        break;
                    }
                }
            }

            if (infoEditado != null) {
                info = infoEditado;
                // Adiciona informação sobre edição no último campo
                info = Arrays.copyOf(info, info.length + 1);
                info[info.length - 1] = editado ? "EDITADO" : "ORIGINAL";
            }
        } catch (IOException e) {
            System.err.println("Erro ao ler arquivos de posto: " + e.getMessage());
        }

        return info;
    }

    private String[] buscarInfoNoArquivo(String arquivo, String postoNome) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(arquivo))) {
            String linha;
            reader.readLine(); // Pula cabeçalho

            while ((linha = reader.readLine()) != null) {
                String[] dados = linha.split(",");
                if (dados.length > 1 && dados[1].trim().equalsIgnoreCase(postoNome.trim())) {
                    return Arrays.stream(dados)
                            .map(String::trim)
                            .toArray(String[]::new);
                }
            }
        }
        return null;
    }

    private void timerStatus() {
        if (timer != null) {
            timer.cancel();
        }

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    statusProducao.setText(" ");
                });
            }
        }, 3000);
    }

    class JanelaEdicaoPosto extends JDialog {
        private JTextField[] campos;
        private JButton btnSalvar;
        private String nomePostoOriginal;
        private boolean editado = false;

        public JanelaEdicaoPosto(JFrame parent, String nomePosto) {
            super(parent, "Editar Posto: " + nomePosto, true);
            this.nomePostoOriginal = nomePosto;

            setLayout(new BorderLayout());
            setSize(500, 400);
            setLocationRelativeTo(parent);

            // Painel de campos
            JPanel painelCampos = new JPanel(new GridLayout(0, 2, 5, 5));
            String[] cabecalhos = { "CodigoPosto", "NomePosto", "MOD", "Rendimento", "Ciclo", "Lote",
                    "UPH", "UPPH", "CAPHora", "CAPTurno", "Ociosidade", "Ocupação", "Local" };
            campos = new JTextField[cabecalhos.length];

            // Busca informações atuais do posto
            String[] infoAtual = buscarInfoPostoNoCSV(nomePosto);

            for (int i = 0; i < cabecalhos.length; i++) {
                painelCampos.add(new JLabel(cabecalhos[i] + ":"));
                campos[i] = new JTextField();
                if (i < infoAtual.length) {
                    campos[i].setText(infoAtual[i]);
                }
                painelCampos.add(campos[i]);
            }

            add(new JScrollPane(painelCampos), BorderLayout.CENTER);

            // Painel de botões
            JPanel painelBotoes = new JPanel();
            btnSalvar = new JButton("Salvar");
            btnSalvar.addActionListener(e -> salvarAlteracoes());

            JButton btnCancelar = new JButton("Cancelar");
            btnCancelar.addActionListener(e -> dispose());

            painelBotoes.add(btnSalvar);
            painelBotoes.add(btnCancelar);
            add(painelBotoes, BorderLayout.SOUTH);
        }

        private void salvarAlteracoes() {
            try {
                // Lê todas as linhas do arquivo
                List<String> linhas = Files.readAllLines(Paths.get("posto.csv"));

                // Encontra e atualiza a linha do posto
                for (int i = 0; i < linhas.size(); i++) {
                    String[] dados = linhas.get(i).split(",");
                    if (dados.length > 1 && dados[1].trim().equalsIgnoreCase(nomePostoOriginal)) {
                        // Atualiza os dados
                        for (int j = 0; j < Math.min(campos.length, dados.length); j++) {
                            dados[j] = campos[j].getText().trim();
                        }
                        linhas.set(i, String.join(",", dados));
                        editado = true;
                        break;
                    }
                }

                // Escreve de volta no arquivo
                Files.write(Paths.get("posto.csv"), linhas);

                JOptionPane.showMessageDialog(this, "Posto atualizado com sucesso!", "Sucesso",
                        JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Erro ao salvar alterações: " + e.getMessage(), "Erro",
                        JOptionPane.ERROR_MESSAGE);
            }
        }

        public boolean foiEditado() {
            return editado;
        }
    }

}

public class TelaSystem extends JFrame {
    private static final String POSTOS_FILE = "postos.txt";
    private int Prod_total;
    private int diasUteis;
    private int horasDisponiveis;
    private JLabel statusDataInicial;
    private JLabel statusDataFinal;
    private JLabel statusPeriodo;
    private JTextField txtProducao;
    private JFormattedTextField txtDataInicial;
    private JFormattedTextField txtDataFinal;
    private Timer timer;
    private JLabel statusProducao;
    private javax.swing.Timer sugestoesTimer;
    private DefaultListModel<String> postosSelecionadosModel;
    private Stack<PostoExcluido> historicoExclusoes = new Stack<>();
    private JFrame janelaPostos = null;
    private Map<Integer, JFrame> janelasPostos = new HashMap<>();
    private PainelInfoLinha painelInfoLinha1;
    private PainelInfoLinha painelInfoLinha2;
    private PainelInfoLinha painelInfoLinha3;
    private PainelInfoLinha painelInfoLinha4;

    public static final Color VERMELHO = new Color(112, 19, 19);
    public static final Color VERDE = new Color(16, 145, 1);

    public TelaSystem() {
        super("Governança de MOD Digital");
        setResizable(false);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setIconImage(new ImageIcon("Resource_Image/icon.png").getImage());
        setLocationRelativeTo(null);

        // Inicializa os componentes necessários para o painel de postos
        postosSelecionadosModel = new DefaultListModel<>();
        historicoExclusoes = new Stack<>();

        getContentPane().setBackground(CorFundo.FUNDO_PRINCIPAL);
        setLayout(null);
        copiarPostoMestreParaPosto();
        configurarInterface();

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowIconified(WindowEvent e) {
                if (janelaPostos != null) {
                    janelaPostos.setExtendedState(JFrame.ICONIFIED);
                }
            }

            @Override
            public void windowDeiconified(WindowEvent e) {
                if (janelaPostos != null) {
                    janelaPostos.setExtendedState(JFrame.NORMAL);
                    janelaPostos.toFront();
                }
            }

            @Override
            public void windowClosing(WindowEvent e) {
                // Fechar todas as janelas de postos
                for (JFrame janela : janelasPostos.values()) {
                    janela.dispose();
                }

                // Fechar timers e limpar recursos
                if (timer != null)
                    timer.cancel();
                if (sugestoesTimer != null && sugestoesTimer.isRunning()) {
                    sugestoesTimer.stop();
                }
                dispose(); // Fecha a janela
            }
        });

        setVisible(true);
    }

    private void copiarPostoMestreParaPosto() {
        try {
            Path mestrePath = Paths.get("postoMestre.csv");
            Path postoPath = Paths.get("posto.csv");

            // Limpa o arquivo posto.csv se existir
            if (Files.exists(postoPath)) {
                Files.delete(postoPath);
            }

            // Copia o conteúdo do mestre para o posto
            Files.copy(mestrePath, postoPath);
        } catch (IOException e) {
            System.err.println("Erro ao copiar arquivo postoMestre.csv para posto.csv: " + e.getMessage());
        }
    }

    private void controlarVisibilidadeBotoesLinhaFA(boolean mostrar) {
        Component[] components = getContentPane().getComponents();
        for (Component comp : components) {
            if (comp instanceof JButton) {
                JButton btn = (JButton) comp;
                if (btn.getText().contains("Linha") && btn.getText().contains("FA")) {
                    btn.setVisible(mostrar);
                    btn.setEnabled(mostrar);
                }
            }
        }
    }

    private void controlarVisibilidadeBotoesLinhaLA(boolean mostrar) {
        Component[] components = getContentPane().getComponents();
        for (Component comp : components) {
            if (comp instanceof JButton) {
                JButton btn = (JButton) comp;
                if (btn.getText().contains("Linha") && btn.getText().contains("LA")) {
                    btn.setVisible(mostrar);
                    btn.setEnabled(mostrar);
                }
            }
        }
    }

    private void controlarVisibilidadeBotoesLinhaBMS(boolean mostrar) {
        Component[] components = getContentPane().getComponents();
        for (Component comp : components) {
            if (comp instanceof JButton) {
                JButton btn = (JButton) comp;
                if (btn.getText().contains("Linha") && btn.getText().contains("BMS")) {
                    btn.setVisible(mostrar);
                    btn.setEnabled(mostrar);
                }
            }
        }
    }

    private void controlarVisibilidadeBotoesLinhaSMT(boolean mostrar) {
        Component[] components = getContentPane().getComponents();
        for (Component comp : components) {
            if (comp instanceof JButton) {
                JButton btn = (JButton) comp;
                if (btn.getText().contains("Linha") && btn.getText().contains("SMT")) {
                    btn.setVisible(mostrar);
                    btn.setEnabled(mostrar);
                }
            }
        }
    }

    private Map<String, List<String>> carregarModelosPorLinha() {
        Map<String, List<String>> modelosPorLinha = new HashMap<>();
        String csvFile = "Modelo.csv";
        String line;
        String csvSplitBy = ",";

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            // Pular cabeçalho
            br.readLine();

            while ((line = br.readLine()) != null) {
                String[] data = line.split(csvSplitBy);
                if (data.length >= 4) { // Verifica se tem pelo menos 4 colunas
                    String linha = data[0].trim();
                    String modelo = data[3].trim();

                    modelosPorLinha.putIfAbsent(linha, new ArrayList<>());
                    if (!modelosPorLinha.get(linha).contains(modelo)) {
                        modelosPorLinha.get(linha).add(modelo);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao ler arquivo Modelo.csv: " + e.getMessage());
        }

        return modelosPorLinha;
    }

    public static class PostoExcluido {
        String nome;
        int indice;

        public PostoExcluido(String nome, int indice) {
            this.nome = nome;
            this.indice = indice;
        }
    }

    private void configurarInterface() {
        setSize(1200, 700);
        setLocationRelativeTo(null);

        int xPos = 20;
        int yPos = 20;
        int labelWidth = 100;
        int fieldWidth = 100;
        int statusWidth = 300;
        int postoY = 250;
        int postoHeight = 50;
        int postoWidth = 180;
        int rowHeight = 30;

        // Produção Total
        JLabel lblProducao = new JLabel("Produção total:");
        lblProducao.setFont(new Font("Arial", Font.BOLD, 12));
        lblProducao.setBounds(xPos, yPos + 3, labelWidth, 20);
        add(lblProducao);

        txtProducao = new JTextField();
        txtProducao.setBounds(xPos + labelWidth + 10, yPos, fieldWidth, 25);
        txtProducao.setDocument(new NumberOnlyDocument());
        add(txtProducao);

        int Xbtn = 400;
        int Ybtn = yPos;
        int Witdthbtn = 80;
        int Heightbtn = 50;

        // Botões FA
        JButton btnAbrirFA1 = new JButton("Linha 1 | FA");
        btnAbrirFA1.setBounds(20, postoY, postoWidth, postoHeight);
        btnAbrirFA1.setVisible(false); // Inicia oculto
        btnAbrirFA1.setEnabled(false); // Inicia desativado
        add(btnAbrirFA1);

        JButton btnAbrirFA2 = new JButton("Linha 2 | FA");
        btnAbrirFA2.setBounds(20, postoY + 100, postoWidth, postoHeight);
        btnAbrirFA2.setVisible(false); // Inicia oculto
        btnAbrirFA2.setEnabled(false); // Inicia desativado
        add(btnAbrirFA2);

        JButton btnAbrirFA3 = new JButton("Linha 3 | FA");
        btnAbrirFA3.setBounds(20, postoY + 200, postoWidth, postoHeight);
        btnAbrirFA3.setVisible(false); // Inicia oculto
        btnAbrirFA3.setEnabled(false); // Inicia desativado
        add(btnAbrirFA3);

        JButton btnAbrirFA4 = new JButton("Linha 4 | FA");
        btnAbrirFA4.setBounds(20, postoY + 300, postoWidth, postoHeight);
        btnAbrirFA4.setVisible(false); // Inicia oculto
        btnAbrirFA4.setEnabled(false); // Inicia desativado
        add(btnAbrirFA4);

        // Botões LA (4 botões)
        JButton btnAbrirLA1 = new JButton("Linha 1 | LA");
        btnAbrirLA1.setBounds(20, postoY, postoWidth, postoHeight);
        btnAbrirLA1.setVisible(false);
        btnAbrirLA1.setEnabled(false);
        btnAbrirLA1.addActionListener(e -> {
            abrirPainelPostos("Final Assembly Line | One", 1);
            // Atualiza o painel de informações com a lista vazia inicialmente
            painelInfoLinha1.atualizarInformacoes(new ArrayList<>());
        });
        add(btnAbrirLA1);

        JButton btnAbrirLA2 = new JButton("Linha 2 | LA");
        btnAbrirLA2.setBounds(20, postoY + 100, postoWidth, postoHeight);
        btnAbrirLA2.setVisible(false); // Inicia oculto
        btnAbrirLA2.setEnabled(false); // Inicia desativado
        btnAbrirLA2.addActionListener(e -> {
            abrirPainelPostos("Final Assembly Line | Two", 2);
            // Atualiza o painel de informações com a lista vazia inicialmente
            painelInfoLinha2.atualizarInformacoes(new ArrayList<>());
        });
        add(btnAbrirLA2);

        JButton btnAbrirLA3 = new JButton("Linha 3 | LA");
        btnAbrirLA3.setBounds(20, postoY + 200, postoWidth, postoHeight);
        btnAbrirLA3.setVisible(false); // Inicia oculto
        btnAbrirLA3.setEnabled(false); // Inicia desativado
        btnAbrirLA3.addActionListener(e -> {
            abrirPainelPostos("Final Assembly Line | Tree", 3);
            // Atualiza o painel de informações com a lista vazia inicialmente
            painelInfoLinha3.atualizarInformacoes(new ArrayList<>());
        });
        add(btnAbrirLA3);

        JButton btnAbrirLA4 = new JButton("Linha 4 | LA");
        btnAbrirLA4.setBounds(20, postoY + 300, postoWidth, postoHeight);
        btnAbrirLA4.setVisible(false); // Inicia oculto
        btnAbrirLA4.setEnabled(false); // Inicia desativado
        btnAbrirLA4.addActionListener(e -> {
            abrirPainelPostos("Final Assembly Line | Four", 4);
            // Atualiza o painel de informações com a lista vazia inicialmente
            painelInfoLinha4.atualizarInformacoes(new ArrayList<>());
        });
        add(btnAbrirLA4);

        // Botões BMS (4 botões)
        JButton btnAbrirBMS1 = new JButton("Linha 1 | BMS");
        btnAbrirBMS1.setVisible(false); // Inicia oculto
        btnAbrirBMS1.setEnabled(false); // Inicia desativado
        btnAbrirBMS1.setBounds(20, postoY, postoWidth, postoHeight);
        add(btnAbrirBMS1);

        JButton btnAbrirBMS2 = new JButton("Linha 2 | BMS");
        btnAbrirBMS2.setVisible(false); // Inicia oculto
        btnAbrirBMS2.setEnabled(false); // Inicia desativado
        btnAbrirBMS2.setBounds(20, postoY + 100, postoWidth, postoHeight);
        add(btnAbrirBMS2);

        JButton btnAbrirBMS3 = new JButton("Linha 3 | BMS");
        btnAbrirBMS3.setVisible(false); // Inicia oculto
        btnAbrirBMS3.setEnabled(false); // Inicia desativado
        btnAbrirBMS3.setBounds(20, postoY + 200, postoWidth, postoHeight);
        add(btnAbrirBMS3);

        JButton btnAbrirBMS4 = new JButton("Linha 4 | BMS");
        btnAbrirBMS4.setVisible(false); // Inicia oculto
        btnAbrirBMS4.setEnabled(false); // Inicia desativado
        btnAbrirBMS4.setBounds(20, postoY + 300, postoWidth, postoHeight);
        add(btnAbrirBMS4);

        // Botões SMT (4 botões)
        JButton btnAbrirSMT1 = new JButton("Linha 1 | SMT");
        btnAbrirSMT1.setVisible(false); // Inicia oculto
        btnAbrirSMT1.setEnabled(false); // Inicia desativado
        btnAbrirSMT1.setBounds(20, postoY, postoWidth, postoHeight);
        add(btnAbrirSMT1);

        JButton btnAbrirSMT2 = new JButton("Linha 2 | SMT");
        btnAbrirSMT2.setVisible(false); // Inicia oculto
        btnAbrirSMT2.setEnabled(false); // Inicia desativado
        btnAbrirSMT2.setBounds(20, postoY + 100, postoWidth, postoHeight);
        add(btnAbrirSMT2);

        JButton btnAbrirSMT3 = new JButton("Linha 3 | SMT");
        btnAbrirSMT3.setVisible(false); // Inicia oculto
        btnAbrirSMT3.setEnabled(false); // Inicia desativado
        btnAbrirSMT3.setBounds(20, postoY + 200, postoWidth, postoHeight);
        add(btnAbrirSMT3);

        JButton btnAbrirSMT4 = new JButton("Linha 4 | SMT");
        btnAbrirSMT4.setVisible(false); // Inicia oculto
        btnAbrirSMT4.setEnabled(false); // Inicia desativado
        btnAbrirSMT4.setBounds(20, postoY + 300, postoWidth, postoHeight);
        add(btnAbrirSMT4);

        painelInfoLinha1 = new PainelInfoLinha("1");
        painelInfoLinha1.setBounds(24, postoY + 5, 900, 85);
        painelInfoLinha1.setVisible(false);
        add(painelInfoLinha1);

        painelInfoLinha2 = new PainelInfoLinha("2");
        painelInfoLinha2.setBounds(24, postoY + 105, 900, 85);
        painelInfoLinha2.setVisible(false);
        add(painelInfoLinha2);

        painelInfoLinha3 = new PainelInfoLinha("3");
        painelInfoLinha3.setBounds(24, postoY + 205, 900, 85);
        painelInfoLinha3.setVisible(false);
        add(painelInfoLinha3);

        painelInfoLinha4 = new PainelInfoLinha("4");
        painelInfoLinha4.setBounds(24, postoY + 305, 900, 85);
        painelInfoLinha4.setVisible(false);
        add(painelInfoLinha4);

        // Botão FA
        JButton btnFA = new JButton("FA");
        btnFA.setBounds(Xbtn, Ybtn, Witdthbtn, Heightbtn);
        btnFA.addActionListener(e -> {
            controlarVisibilidadeBotoesLinhaFA(true);
            controlarVisibilidadeBotoesLinhaLA(false);
            controlarVisibilidadeBotoesLinhaBMS(false);
            controlarVisibilidadeBotoesLinhaSMT(false);

            painelInfoLinha1.setVisible(true);
            painelInfoLinha2.setVisible(true);
            painelInfoLinha3.setVisible(true);
            painelInfoLinha4.setVisible(true);
            statusProducao.setText("Área FA selecionada");
            statusProducao.setForeground(VERDE);
            timerStatus();
        });
        add(btnFA);

        // Botão LA
        JButton btnLA = new JButton("LA");
        btnLA.setBounds(Xbtn + Witdthbtn, Ybtn, Witdthbtn, Heightbtn);
        btnLA.addActionListener(e -> {
            controlarVisibilidadeBotoesLinhaFA(false);
            controlarVisibilidadeBotoesLinhaLA(true);
            controlarVisibilidadeBotoesLinhaBMS(false);
            controlarVisibilidadeBotoesLinhaSMT(false);

            painelInfoLinha1.setVisible(true);
            painelInfoLinha2.setVisible(true);
            painelInfoLinha3.setVisible(true);
            painelInfoLinha4.setVisible(true);
            statusProducao.setText("Área LA selecionada");
            statusProducao.setForeground(VERDE);
            timerStatus();
        });
        add(btnLA);

        // Botão BMS
        JButton btnBMS = new JButton("BMS");
        btnBMS.setBounds(Xbtn + Witdthbtn * 2, Ybtn, Witdthbtn, Heightbtn);
        btnBMS.addActionListener(e -> {
            controlarVisibilidadeBotoesLinhaFA(false);
            controlarVisibilidadeBotoesLinhaLA(false);
            controlarVisibilidadeBotoesLinhaBMS(true);
            controlarVisibilidadeBotoesLinhaSMT(false);

            painelInfoLinha1.setVisible(true);
            painelInfoLinha2.setVisible(true);
            painelInfoLinha3.setVisible(true);
            painelInfoLinha4.setVisible(true);
            statusProducao.setText("Área BMS selecionada");
            statusProducao.setForeground(VERDE);
            timerStatus();
        });
        add(btnBMS);

        // Botão SMT
        JButton btnSMT = new JButton("SMT");
        btnSMT.setBounds(Xbtn + Witdthbtn * 3, Ybtn, Witdthbtn, Heightbtn);
        btnSMT.addActionListener(e -> {
            controlarVisibilidadeBotoesLinhaFA(false);
            controlarVisibilidadeBotoesLinhaLA(false);
            controlarVisibilidadeBotoesLinhaBMS(false);
            controlarVisibilidadeBotoesLinhaSMT(true);

            painelInfoLinha1.setVisible(true);
            painelInfoLinha2.setVisible(true);
            painelInfoLinha3.setVisible(true);
            painelInfoLinha4.setVisible(true);
            statusProducao.setText("Área SMT selecionada");
            statusProducao.setForeground(VERDE);
            timerStatus();
        });
        add(btnSMT);

        yPos += rowHeight;

        // Data Inicial
        JLabel lblDataInicial = new JLabel("Data Inicial:");
        lblDataInicial.setFont(new Font("Arial", Font.BOLD, 12));
        lblDataInicial.setBounds(xPos, yPos + 3, labelWidth, 20);
        add(lblDataInicial);

        txtDataInicial = createDateField(xPos + labelWidth + 10, yPos, fieldWidth);
        add(txtDataInicial);

        statusDataInicial = new JLabel("Não validado");
        statusDataInicial.setFont(new Font("Arial", Font.BOLD, 10));
        statusDataInicial.setBounds(xPos + labelWidth + fieldWidth + 20, yPos, statusWidth, 20);
        statusDataInicial.setForeground(Color.GRAY);
        add(statusDataInicial);

        yPos += rowHeight;

        // Data Final
        JLabel lblDataFinal = new JLabel("Data Final:");
        lblDataFinal.setFont(new Font("Arial", Font.BOLD, 12));
        lblDataFinal.setBounds(xPos, yPos + 3, labelWidth, 20);
        add(lblDataFinal);

        txtDataFinal = createDateField(xPos + labelWidth + 10, yPos, fieldWidth);
        add(txtDataFinal);

        statusDataFinal = new JLabel("Não validado");
        statusDataFinal.setFont(new Font("Arial", Font.BOLD, 10));
        statusDataFinal.setBounds(xPos + labelWidth + fieldWidth + 20, yPos, statusWidth, 20);
        statusDataFinal.setForeground(Color.GRAY);
        add(statusDataFinal);

        yPos += rowHeight;

        // Status do Período
        statusPeriodo = new JLabel(" ");
        statusPeriodo.setFont(new Font("Arial", Font.BOLD, 10));
        statusPeriodo.setBounds(xPos + 30, yPos - 5, 300, 15);
        add(statusPeriodo);

        yPos += 25;

        // Botão Calcular
        JButton btnCalcular = new JButton("Calcular");
        btnCalcular.setBounds(xPos + 30, yPos - 14, 140, 25);
        btnCalcular.addActionListener(this::calcular);
        add(btnCalcular);

        // Botão Reset
        JButton btnReset = new JButton("Reset");
        btnReset.setBounds(xPos + 30, yPos + 9, 140, 25);
        btnReset.addActionListener(e -> {

            resetarSistema();

            controlarVisibilidadeBotoesLinhaFA(false);
            controlarVisibilidadeBotoesLinhaLA(false);
            controlarVisibilidadeBotoesLinhaBMS(false);
            controlarVisibilidadeBotoesLinhaSMT(false);

            painelInfoLinha1.setVisible(false);
            painelInfoLinha2.setVisible(false);
            painelInfoLinha3.setVisible(false);
            painelInfoLinha4.setVisible(false);

        });
        add(btnReset);

        // Status da Produção
        statusProducao = new JLabel(" ");
        statusProducao.setFont(new Font("Arial", Font.BOLD, 10));
        statusProducao.setHorizontalAlignment(SwingConstants.CENTER);
        statusProducao.setOpaque(true);
        statusProducao.setBackground(CorFundo.FUNDO_SECUNDARIO);
        statusProducao.setForeground(Color.BLACK);
        statusProducao.setBounds(140, 1, 1000, 18);
        add(statusProducao);

    }

    // Método para resetar o sistema
    private void resetarSistema() {
        // Limpa campos de texto
        txtProducao.setText("");
        txtDataInicial.setText("");
        txtDataFinal.setText("");

        // Reseta status
        statusDataInicial.setText("Não validado");
        statusDataInicial.setForeground(Color.GRAY);
        statusDataFinal.setText("Não validado");
        statusDataFinal.setForeground(Color.GRAY);
        statusPeriodo.setText(" ");
        statusProducao.setText(" ");

        // Fecha todas as janelas de postos
        for (JFrame janela : janelasPostos.values()) {
            janela.dispose();
        }
        janelasPostos.clear();

        // Reseta o histórico de exclusões
        historicoExclusoes.clear();

        // Feedback visual
        statusProducao.setText("Sistema resetado - todos os campos limpos");
        statusProducao.setForeground(VERDE);
        timerStatus();
    }

    private void abrirPainelPostos(String titulo, int numeroLinha) {
        // Verifica se a janela já existe
        if (janelasPostos.containsKey(numeroLinha)) {
            JFrame janelaExistente = janelasPostos.get(numeroLinha);
            if (janelaExistente.isVisible()) {
                janelaExistente.toFront();
                return;
            }
        }

        // Carrega os modelos específicos para esta linha
        Map<String, List<String>> modelosPorLinha = carregarModelosPorLinha();
        String chaveLinha = "Linha" + numeroLinha;
        List<String> modelosLinha = modelosPorLinha.getOrDefault(chaveLinha, new ArrayList<>());

        // Obtém o painel de informações correspondente
        PainelInfoLinha painelInfo = null;
        switch (numeroLinha) {
            case 1:
                painelInfo = painelInfoLinha1;
                break;
            case 2:
                painelInfo = painelInfoLinha2;
                break;
            case 3:
                painelInfo = painelInfoLinha3;
                break;
            case 4:
                painelInfo = painelInfoLinha4;
                break;
        }

        // Cria uma nova janela
        JFrame janelaPostos = new JFrame(titulo);
        janelaPostos.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        janelaPostos.setSize(500, 600);
        janelaPostos.setIconImage(new ImageIcon("Resource_Image/icon.png").getImage());
        janelaPostos.setAlwaysOnTop(true);

        // Configura o comportamento de fechamento
        janelaPostos.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                janelaPostos.setExtendedState(JFrame.ICONIFIED);
            }
        });

        // Cria modelos específicos para cada linha
        DefaultListModel<String> modelLinha = new DefaultListModel<>();
        Stack<PostoExcluido> historicoLinha = new Stack<>();

        // Cria o painel com os modelos específicos
        PainelLA painel = new PainelLA(
                modelLinha,
                historicoLinha,
                modelosLinha,
                statusProducao,
                painelInfo // Passa o painel de informações correspondente
        );

        janelaPostos.add(painel);

        // Centraliza a janela na tela com um pequeno deslocamento em cascata
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screenSize.width - janelaPostos.getWidth()) / 2;
        int y = (screenSize.height - janelaPostos.getHeight()) / 2;

        // Aplica um pequeno deslocamento para cada janela
        x += 20 * (numeroLinha - 1);
        y += 20 * (numeroLinha - 1);

        janelaPostos.setLocation(x, y);

        // Armazena a referência da janela
        janelasPostos.put(numeroLinha, janelaPostos);
        janelaPostos.setVisible(true);
    }

    private JFormattedTextField createDateField(int x, int y, int width) {
        try {
            MaskFormatter mask = new MaskFormatter("##/##/####");
            mask.setPlaceholderCharacter('_');
            JFormattedTextField field = new JFormattedTextField(mask);
            field.setBounds(x, y, width, 25);

            field.addPropertyChangeListener("value", evt -> verificarDatas());

            return field;
        } catch (ParseException e) {
            JOptionPane.showMessageDialog(this, "Erro ao configurar campo de data", "Erro",
                    JOptionPane.ERROR_MESSAGE);
            return new JFormattedTextField();
        }
    }

    private void verificarDatas() {
        boolean dataInicialValida = validarData(txtDataInicial, statusDataInicial, "inicial");
        boolean dataFinalValida = validarData(txtDataFinal, statusDataFinal, "final");

        if (dataInicialValida && dataFinalValida) {
            verificarPeriodo();
        } else {
            statusPeriodo.setText(" ");
        }
    }

    private boolean validarData(JFormattedTextField field, JLabel status, String tipo) {
        String text = field.getText().replace("_", "");

        if (text.length() != 10) {
            status.setText("Data " + tipo + " incompleta");
            status.setForeground(VERMELHO);
            return false;
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDate data = LocalDate.parse(text, formatter);
            LocalDate hoje = LocalDate.now();
            LocalDate minData = LocalDate.of(2025, 1, 1);

            if (data.isBefore(minData)) {
                status.setText("Data " + tipo + " deve ser a partir de 2025");
                status.setForeground(VERMELHO);
                return false;
            }

            if (data.isBefore(hoje)) {
                status.setText("Data " + tipo + " deve ser futura");
                status.setForeground(VERMELHO);
                return false;
            }

            status.setText("Data " + tipo + " válida");
            status.setForeground(VERDE);
            return true;
        } catch (Exception e) {
            status.setText("Data " + tipo + " inválida");
            status.setForeground(VERMELHO);
            return false;
        }
    }

    private void verificarPeriodo() {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDate dataInicial = LocalDate.parse(txtDataInicial.getText().replace("_", ""), formatter);
            LocalDate dataFinal = LocalDate.parse(txtDataFinal.getText().replace("_", ""), formatter);

            if (dataInicial.isAfter(dataFinal)) {
                statusPeriodo.setText("Período incorreto (inicial após final)");
                statusPeriodo.setForeground(VERMELHO);
            } else if (dataInicial.isEqual(dataFinal)) {
                statusPeriodo.setText("Período válido (datas iguais)");
                statusPeriodo.setForeground(VERDE);
            } else {
                statusPeriodo.setText("Período válido (inicial anterior à final)");
                statusPeriodo.setForeground(VERDE);
            }
        } catch (Exception e) {
            statusPeriodo.setText("Erro ao validar período");
            statusPeriodo.setForeground(VERMELHO);
        }
    }

    private int calcularDiasUteis(LocalDate startDate, LocalDate endDate) {
        int diasUteis = 0;
        LocalDate date = startDate;

        while (!date.isAfter(endDate)) {
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                diasUteis++;
            }
            date = date.plusDays(1);
        }

        return diasUteis;
    }

    private void calcular(ActionEvent e) {
        try {
            Prod_total = Integer.parseInt(txtProducao.getText());

            verificarDatas();
            if (statusPeriodo.getForeground() == VERMELHO) {
                statusProducao.setText("Corrija o período antes de calcular");
                statusProducao.setForeground(VERMELHO);
                timerStatus();
                return;
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDate dataInicial = LocalDate.parse(txtDataInicial.getText().replace("_", ""), formatter);
            LocalDate dataFinal = LocalDate.parse(txtDataFinal.getText().replace("_", ""), formatter);

            this.diasUteis = calcularDiasUteis(dataInicial, dataFinal);
            this.horasDisponiveis = diasUteis * 8;

            String statusText = "Produção: " + Prod_total + " | Dias úteis: " + diasUteis + " | Horas disponíveis: "
                    + horasDisponiveis;
            statusProducao.setText(statusText);
            statusProducao.setForeground(VERDE);

            imprimirNoTerminal(Prod_total, dataInicial, dataFinal, diasUteis, horasDisponiveis);

            timerStatus();

        } catch (NumberFormatException ex) {
            statusProducao.setText("Valor inválido para produção");
            statusProducao.setForeground(VERMELHO);
            timerStatus();
        }
    }

    private void imprimirNoTerminal(int producao, LocalDate dataInicial, LocalDate dataFinal,
            int diasUteis, int horasDisponiveis) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String separador = "-------------------------------------Calculo de MOD-------------------------------------";

        System.out.println(separador);
        System.out.println("[Total Produção] | " + producao);
        System.out.println("[Data Inicial]   | " + dataInicial.format(formatter));
        System.out.println("[Data Final]     | " + dataFinal.format(formatter));
        System.out.println("[Dias Úteis]     | " + diasUteis);
        System.out.println("[Horas Dispo.]   | " + horasDisponiveis);
        System.out.println(separador + "\n");
    }

    private void timerStatus() {
        if (timer != null) {
            timer.cancel();
        }

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    statusProducao.setText(" ");
                });
            }
        }, 3000);
    }

    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Nimbus Look and Feel não encontrado, usando padrão.");
        }

        SwingUtilities.invokeLater(() -> {
            TelaSystem tela = new TelaSystem();
            tela.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    if (tela.postosSelecionadosModel != null) {
                        tela.postosSelecionadosModel.clear();
                    }
                    if (tela.timer != null) {
                        tela.timer.cancel();
                    }
                    if (tela.sugestoesTimer != null && tela.sugestoesTimer.isRunning()) {
                        tela.sugestoesTimer.stop();
                    }
                    System.out.println("Janela fechada, timers cancelados.");
                }
            });
        });
    }
}

class NumberOnlyDocument extends javax.swing.text.PlainDocument {
    @Override
    public void insertString(int offs, String str, javax.swing.text.AttributeSet a)
            throws javax.swing.text.BadLocationException {
        if (str == null)
            return;

        String newStr = str.replaceAll("[^0-9]", "");
        super.insertString(offs, newStr, a);
    }
}