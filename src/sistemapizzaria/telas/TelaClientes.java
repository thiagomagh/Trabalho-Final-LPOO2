package sistemapizzaria.telas;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;

import sistemapizzaria.SistemaPizzaria;
import sistemapizzaria.cliente.Cliente;
import sistemapizzaria.controller.ClienteController;
import sistemapizzaria.model.ClienteTableModel;

/**
 * View principal para Gerenciamento de Clientes (Padrão MVC - Aula 06).
 */
public class TelaClientes extends JFrame {

    private static final long serialVersionUID = 1L;
    private JPanel contentPane;
    private JTextField txtFiltroSobrenome;
    private JTextField txtFiltroTelefone;
    private JButton btnCadastrarCliente;
    private JButton btnAdicionarSabores;
    private JButton btnAtualizarPrecos;
    private JButton btnExcluirCliente;
    private JTextField txtNome;
    private JTextField txtTelefone;
    private JTextField txtSobrenome;
    private JButton btnPedidosDoCliente;
    private JButton btnEditarCliente;
    private JButton btnGerenciarPedidos;
    private JTable tabelaClientes;
    private final ClienteTableModel modeloTabela;
    private final ClienteController controller;
    private JLabel lblTelefone;
    private boolean modoEdicao = false;
    private JButton btnCancelarEdicao;

    public TelaClientes() {
        this.modeloTabela = new ClienteTableModel();
        this.controller = new ClienteController(this, this.modeloTabela);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Gerenciamento de Clientes - Sistema Pizzaria (MVC & JDBC)");
        setMinimumSize(new Dimension(900, 700));
        setLocationRelativeTo(null);

        contentPane = new JPanel(new BorderLayout(10, 10));
        contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(contentPane);

        // ============================================
        // PAINEL SUPERIOR (Ações Globais, Busca e Cadastro)
        // ============================================
        JPanel pnlNorte = new JPanel();
        pnlNorte.setLayout(new BoxLayout(pnlNorte, BoxLayout.Y_AXIS));

        // --- Ações Globais ---
        JPanel pnlAcoesGlobais = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnAdicionarSabores = new JButton("Adicionar Sabores");
        btnAdicionarSabores.addActionListener(e -> {
            CadastroSabor telaSabor = new CadastroSabor();
            telaSabor.setLocationRelativeTo(this);
            telaSabor.setVisible(true);
        });

        btnAtualizarPrecos = new JButton("Atualizar Preços");
        btnAtualizarPrecos.addActionListener(e -> {
            TelaAtualizarPreco telaPreco = new TelaAtualizarPreco();
            telaPreco.setLocationRelativeTo(this);
            telaPreco.setVisible(true);
        });

        btnGerenciarPedidos = new JButton("Gerenciar Pedidos");
        btnGerenciarPedidos.addActionListener(e -> {
            TelaGerenciarPedidos telaPedidos = new TelaGerenciarPedidos();
            telaPedidos.setLocationRelativeTo(this);
            telaPedidos.setVisible(true);
        });

        pnlAcoesGlobais.add(btnAdicionarSabores);
        pnlAcoesGlobais.add(btnAtualizarPrecos);
        pnlAcoesGlobais.add(btnGerenciarPedidos);
        pnlNorte.add(pnlAcoesGlobais);

        // --- Busca de Clientes (Requisito 1.d: Filtro por sobrenome, parte do sobrenome e/ou telefone) ---
        JPanel pnlBusca = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        pnlBusca.setBorder(BorderFactory.createTitledBorder("Pesquisar Clientes (Sobrenome e/ou Telefone)"));

        pnlBusca.add(new JLabel("Sobrenome:"));
        txtFiltroSobrenome = new JTextField(20);
        pnlBusca.add(txtFiltroSobrenome);

        pnlBusca.add(new JLabel("Telefone:"));
        txtFiltroTelefone = new JTextField(15);
        pnlBusca.add(txtFiltroTelefone);

        JButton btnLimparFiltro = new JButton("Limpar Filtro");
        pnlBusca.add(btnLimparFiltro);
        pnlNorte.add(pnlBusca);

        KeyAdapter filtroKeyListener = new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                controller.filtrarClientes(txtFiltroSobrenome.getText(), txtFiltroTelefone.getText());
            }
        };
        txtFiltroSobrenome.addKeyListener(filtroKeyListener);
        txtFiltroTelefone.addKeyListener(filtroKeyListener);

        btnLimparFiltro.addActionListener(e -> {
            txtFiltroSobrenome.setText("");
            txtFiltroTelefone.setText("");
            controller.atualizarTabela();
        });

        // --- Formulário de Cadastro ---
        JPanel pnlFormulario = new JPanel(new GridBagLayout());
        pnlFormulario.setBorder(BorderFactory.createTitledBorder("Dados do Cliente"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Linha 1: Labels
        gbc.gridy = 0; gbc.gridx = 0;
        pnlFormulario.add(new JLabel("Nome:"), gbc);
        gbc.gridx = 1;
        pnlFormulario.add(new JLabel("Sobrenome:"), gbc);
        gbc.gridx = 2;
        lblTelefone = new JLabel("Telefone:");
        pnlFormulario.add(lblTelefone, gbc);

        // Linha 2: TextFields
        gbc.gridy = 1; gbc.gridx = 0;
        gbc.weightx = 1.0;
        txtNome = new JTextField();
        pnlFormulario.add(txtNome, gbc);

        gbc.gridx = 1;
        txtSobrenome = new JTextField();
        pnlFormulario.add(txtSobrenome, gbc);

        gbc.gridx = 2;
        txtTelefone = new JTextField();
        txtTelefone.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (!Character.isDigit(e.getKeyChar())) {
                    e.consume();
                }
            }
        });
        pnlFormulario.add(txtTelefone, gbc);

        // Linha 3: Botões
        gbc.gridy = 2; gbc.gridx = 0; gbc.gridwidth = 3;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;

        JPanel pnlBotoesForm = new JPanel(new FlowLayout());
        btnCadastrarCliente = new JButton("Cadastrar Cliente");
        btnCancelarEdicao = new JButton("Cancelar Edição");
        btnCancelarEdicao.setVisible(false);
        pnlBotoesForm.add(btnCadastrarCliente);
        pnlBotoesForm.add(btnCancelarEdicao);
        pnlFormulario.add(pnlBotoesForm, gbc);

        pnlNorte.add(pnlFormulario);
        contentPane.add(pnlNorte, BorderLayout.NORTH);

        // ============================================
        // PAINEL CENTRAL (Tabela)
        // ============================================
        tabelaClientes = new JTable(modeloTabela);
        tabelaClientes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(tabelaClientes);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Lista de Clientes"));
        contentPane.add(scrollPane, BorderLayout.CENTER);

        // ============================================
        // PAINEL INFERIOR (Ações da Tabela)
        // ============================================
        JPanel pnlSul = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));

        btnPedidosDoCliente = new JButton("Pedidos do Cliente");
        btnEditarCliente = new JButton("Editar Cliente");
        btnExcluirCliente = new JButton("Excluir Cliente");
        btnExcluirCliente.setForeground(Color.BLACK);
        btnExcluirCliente.setBackground(Color.RED);

        pnlSul.add(btnPedidosDoCliente);
        pnlSul.add(btnEditarCliente);
        pnlSul.add(btnExcluirCliente);
        contentPane.add(pnlSul, BorderLayout.SOUTH);

        // ============================================
        // EVENTOS -> CONTROLLER
        // ============================================
        btnCadastrarCliente.addActionListener(e -> {
            controller.salvarCliente(
                txtNome.getText(),
                txtSobrenome.getText(),
                txtTelefone.getText(),
                modoEdicao
            );
        });

        btnCancelarEdicao.addActionListener(e -> controller.cancelarEdicao());

        btnEditarCliente.addActionListener(e -> {
            controller.iniciarEdicao(tabelaClientes.getSelectedRow());
        });

        btnExcluirCliente.addActionListener(e -> {
            controller.excluirCliente(tabelaClientes.getSelectedRow(), modoEdicao);
        });

        btnPedidosDoCliente.addActionListener(e -> {
            int linha = tabelaClientes.getSelectedRow();
            if (linha == -1) {
                exibirAviso("Selecione um cliente.");
                return;
            }
            Cliente c = controller.obterClienteNaLinha(linha);
            if (c != null) {
                TelaPedidos tela = new TelaPedidos(TelaClientes.this);
                tela.setVisible(true);
                tela.buscarClienteAutomaticamente(c.getTelefone());
            }
        });

        atualizarTabela();
        pack();
    }

    public TelaClientes(SistemaPizzaria sistema) {
        this();
    }

    public void preencherFormularioParaEdicao(Cliente cliente) {
        txtTelefone.setText(cliente.getTelefone());
        txtNome.setText(cliente.getNome());
        txtSobrenome.setText(cliente.getSobrenome());

        modoEdicao = true;
        btnCancelarEdicao.setVisible(true);
        btnCadastrarCliente.setText("Atualizar Informações");
    }

    public void sairModoEdicaoELimpar() {
        modoEdicao = false;
        txtNome.setText("");
        txtSobrenome.setText("");
        txtTelefone.setText("");
        txtTelefone.setEditable(true);
        btnCadastrarCliente.setText("Cadastrar Cliente");
        btnCancelarEdicao.setVisible(false);
    }

    public void atualizarTabela() {
        controller.atualizarTabela();
    }

    public boolean confirmarAcao(String pergunta) {
        int confirm = JOptionPane.showConfirmDialog(this, pergunta, "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return confirm == JOptionPane.YES_OPTION;
    }

    public void exibirSucesso(String mensagem) {
        JOptionPane.showMessageDialog(this, mensagem, "Sucesso", JOptionPane.INFORMATION_MESSAGE);
    }

    public void exibirAviso(String mensagem) {
        JOptionPane.showMessageDialog(this, mensagem, "Aviso", JOptionPane.WARNING_MESSAGE);
    }
}
