package sistemapizzaria.telas;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
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
import sistemapizzaria.controller.PedidoController;
import sistemapizzaria.model.CarrinhoPizzaTableModel;
import sistemapizzaria.model.ClienteTableModel;
import sistemapizzaria.model.PedidoTableModel;
import sistemapizzaria.pizza.Pizza;

/**
 * View responsável pela realização e edição de Pedidos de Pizzas (Padrão MVC - Aula 06).
 */
public class TelaPedidos extends JFrame {

    private static final long serialVersionUID = 1L;
    private JPanel contentPane;

    // UI Busca Clientes
    private JTextField txtBusca;
    private JTable tabelaClientesBusca;
    private final ClienteTableModel modeloTabelaClientes;

    // UI Histórico Pedidos
    private JTable tabelaPedidos;
    private final PedidoTableModel modeloTabelaPedidos;
    private JButton btnEditarPedido;
    private JButton btnExcluirPedido;

    // UI Carrinho
    private JPanel pnlCarrinho;
    private JTable tabelaCarrinho;
    private final CarrinhoPizzaTableModel modeloCarrinho;
    private JLabel lblPrecoTotal;
    private JButton btnFinalizarSalvar;

    // Painel Extraído
    private PainelMontagemPizza painelPizza;

    private final PedidoController controller;
    private final TelaClientes telaClientesRef;

    public TelaPedidos(TelaClientes telaClientes) {
        this.telaClientesRef = telaClientes;
        this.modeloTabelaClientes = new ClienteTableModel();
        this.modeloTabelaPedidos = new PedidoTableModel();
        this.modeloCarrinho = new CarrinhoPizzaTableModel();
        this.controller = new PedidoController(this, modeloTabelaClientes, modeloTabelaPedidos, modeloCarrinho);

        setTitle("Pedidos de Clientes");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(850, 700));
        setLocationRelativeTo(null);

        contentPane = new JPanel(new BorderLayout());
        contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(contentPane);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        JScrollPane mainScroll = new JScrollPane(mainPanel);
        mainScroll.getVerticalScrollBar().setUnitIncrement(16);
        contentPane.add(mainScroll, BorderLayout.CENTER);

        // 1. BUSCA DE CLIENTES
        JPanel pnlBuscaWrapper = new JPanel(new BorderLayout(5, 5));
        pnlBuscaWrapper.setBorder(BorderFactory.createTitledBorder("Buscar Cliente (Telefone)"));
        pnlBuscaWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));

        JPanel pnlBuscaTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlBuscaTop.add(new JLabel("Telefone:"));
        txtBusca = new JTextField(15);
        pnlBuscaTop.add(txtBusca);
        JButton btnBuscar = new JButton("Buscar");
        pnlBuscaTop.add(btnBuscar);
        pnlBuscaWrapper.add(pnlBuscaTop, BorderLayout.NORTH);

        tabelaClientesBusca = new JTable(modeloTabelaClientes);
        tabelaClientesBusca.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPaneBusca = new JScrollPane(tabelaClientesBusca);
        pnlBuscaWrapper.add(scrollPaneBusca, BorderLayout.CENTER);
        mainPanel.add(pnlBuscaWrapper);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // 2. HISTÓRICO DE PEDIDOS
        JPanel pnlPedidosWrapper = new JPanel(new BorderLayout());
        pnlPedidosWrapper.setBorder(BorderFactory.createTitledBorder("Histórico de Pedidos"));
        pnlPedidosWrapper.setPreferredSize(new Dimension(750, 150));

        tabelaPedidos = new JTable(modeloTabelaPedidos);
        tabelaPedidos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPanePedidos = new JScrollPane(tabelaPedidos);
        pnlPedidosWrapper.add(scrollPanePedidos, BorderLayout.CENTER);

        JPanel pnlAcoesPedido = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnAdicionarPedido = new JButton("Adicionar Novo Pedido");
        btnEditarPedido = new JButton("Editar Pedido Selecionado");
        btnExcluirPedido = new JButton("Excluir Pedido Selecionado");
        JButton btnGerenciarPedidos = new JButton("Gerenciar Pedidos");

        btnEditarPedido.setVisible(false);
        btnExcluirPedido.setVisible(false);
        btnExcluirPedido.setForeground(Color.WHITE);
        btnExcluirPedido.setBackground(new Color(220, 53, 69));

        pnlAcoesPedido.add(btnAdicionarPedido);
        pnlAcoesPedido.add(btnEditarPedido);
        pnlAcoesPedido.add(btnExcluirPedido);
        pnlAcoesPedido.add(btnGerenciarPedidos);
        pnlPedidosWrapper.add(pnlAcoesPedido, BorderLayout.SOUTH);

        mainPanel.add(pnlPedidosWrapper);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // 3. PAINEL DE MONTAGEM DE PIZZA
        // Os sabores chegam ao painel pelo controller: a View não acessa a camada DAO diretamente (MVC)
        painelPizza = new PainelMontagemPizza(controller::listarSaboresDisponiveis, novaPizza -> {
            controller.adicionarPizzaAoCarrinho(novaPizza);
        });
        painelPizza.setAcaoCancelarEdicao(controller::cancelarEdicaoPizza);
        painelPizza.setVisible(false);
        mainPanel.add(painelPizza);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // 4. CARRINHO DE COMPRAS
        pnlCarrinho = new JPanel(new BorderLayout());
        pnlCarrinho.setBorder(BorderFactory.createTitledBorder("Carrinho de Pizzas"));
        pnlCarrinho.setPreferredSize(new Dimension(750, 150));

        tabelaCarrinho = new JTable(modeloCarrinho);
        pnlCarrinho.add(new JScrollPane(tabelaCarrinho), BorderLayout.CENTER);

        JPanel pnlCarrinhoBottom = new JPanel(new BorderLayout());
        JPanel pnlAcoesCarrinho = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnRemoverDaLista = new JButton("Remover Pizza do Carrinho");
        btnRemoverDaLista.setForeground(Color.WHITE);
        btnRemoverDaLista.setBackground(new Color(220, 53, 69));
        JButton btnEditarPizza = new JButton("Editar Pizza");
        pnlAcoesCarrinho.add(btnEditarPizza);
        pnlAcoesCarrinho.add(btnRemoverDaLista);

        JPanel pnlTotal = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        lblPrecoTotal = new JLabel("Preço Total: R$ 0,00");
        btnFinalizarSalvar = new JButton("Finalizar Pedido");
        pnlTotal.add(lblPrecoTotal);
        pnlTotal.add(btnFinalizarSalvar);

        pnlCarrinhoBottom.add(pnlAcoesCarrinho, BorderLayout.WEST);
        pnlCarrinhoBottom.add(pnlTotal, BorderLayout.EAST);
        pnlCarrinho.add(pnlCarrinhoBottom, BorderLayout.SOUTH);

        pnlCarrinho.setVisible(false);
        mainPanel.add(pnlCarrinho);

        // ======================================
        // EVENTOS DA VIEW -> CONTROLLER
        // ======================================
        txtBusca.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (!Character.isDigit(e.getKeyChar())) e.consume();
            }
        });

        btnBuscar.addActionListener(e -> controller.buscarClientePorTelefone(txtBusca.getText()));
        txtBusca.addActionListener(e -> controller.buscarClientePorTelefone(txtBusca.getText()));

        tabelaClientesBusca.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                controller.aoSelecionarClienteNaTabela(tabelaClientesBusca.getSelectedRow());
            }
        });

        tabelaPedidos.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean selecionou = tabelaPedidos.getSelectedRow() != -1;
                btnEditarPedido.setVisible(selecionou);
                btnExcluirPedido.setVisible(selecionou);
            }
        });

        btnAdicionarPedido.addActionListener(e -> controller.iniciarNovoPedido());
        btnEditarPedido.addActionListener(e -> controller.editarPedidoSelecionado(tabelaPedidos.getSelectedRow()));
        btnExcluirPedido.addActionListener(e -> controller.excluirPedidoSelecionado(tabelaPedidos.getSelectedRow()));

        btnGerenciarPedidos.addActionListener(e -> {
            TelaGerenciarPedidos gerenciar = new TelaGerenciarPedidos();
            Cliente c = controller.getClienteAtual();
            if (c != null) {
                gerenciar.abrirComCliente(c.getTelefone());
            }
            gerenciar.setVisible(true);
        });

        btnEditarPizza.addActionListener(e -> controller.editarPizzaDoCarrinho(tabelaCarrinho.getSelectedRow()));
        btnRemoverDaLista.addActionListener(e -> controller.removerPizzaDoCarrinho(tabelaCarrinho.getSelectedRow()));
        btnFinalizarSalvar.addActionListener(e -> controller.finalizarOuSalvarPedido());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (telaClientesRef != null) telaClientesRef.atualizarTabela();
            }
        });

        pack();
    }

    public TelaPedidos(SistemaPizzaria sistema, TelaClientes telaClientes) {
        this(telaClientes);
    }

    public void selecionarPrimeiraLinhaCliente() {
        if (modeloTabelaClientes.getRowCount() > 0) {
            tabelaClientesBusca.setRowSelectionInterval(0, 0);
        }
    }

    public void selecionarLinhaPedido(int row) {
        if (row >= 0 && row < modeloTabelaPedidos.getRowCount()) {
            tabelaPedidos.setRowSelectionInterval(row, row);
        }
    }

    public void exibirPainelMontagemECarrinho(boolean modoEdicaoPedido, Pizza pizzaInicialParaCarregar) {
        btnFinalizarSalvar.setText(modoEdicaoPedido ? "Salvar Alterações" : "Finalizar Pedido");
        painelPizza.limparFormulario();
        if (pizzaInicialParaCarregar != null) {
            painelPizza.carregarPizzaParaEdicao(pizzaInicialParaCarregar);
        }
        painelPizza.setVisible(true);
        pnlCarrinho.setVisible(true);
        contentPane.revalidate();
    }

    public void ocultarPainelMontagemECarrinho() {
        painelPizza.setVisible(false);
        pnlCarrinho.setVisible(false);
        contentPane.revalidate();
    }

    // Carrega a pizza escolhida no carrinho para edição: o botão do painel passa a "Atualizar pizza"
    public void carregarPizzaNoPainelMontagem(Pizza pizza) {
        painelPizza.carregarPizzaParaEdicao(pizza);
        painelPizza.definirModoEdicao(true);
    }

    // Esvazia o painel de montagem e sai do modo de edição de pizza
    public void limparPainelMontagem() {
        painelPizza.limparFormulario();
    }

    public void atualizarLabelPrecoTotal(double valorTotal) {
        lblPrecoTotal.setText(String.format("Preço Total: R$ %.2f", valorTotal));
    }

    public void buscarClienteAutomaticamente(String telefone) {
        txtBusca.setText(telefone);
        controller.buscarClientePorTelefone(telefone);
    }

    public boolean confirmarAcao(String pergunta) {
        int confirm = JOptionPane.showConfirmDialog(this, pergunta, "Confirmar", JOptionPane.YES_NO_OPTION);
        return confirm == JOptionPane.YES_OPTION;
    }

    public void exibirSucesso(String mensagem) {
        JOptionPane.showMessageDialog(this, mensagem, "Sucesso", JOptionPane.INFORMATION_MESSAGE);
    }

    public void exibirAviso(String mensagem) {
        JOptionPane.showMessageDialog(this, mensagem, "Aviso", JOptionPane.WARNING_MESSAGE);
    }
}
