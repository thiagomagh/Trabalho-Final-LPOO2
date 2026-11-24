package sistemapizzaria.telas;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import sistemapizzaria.formas.Circulo;
import sistemapizzaria.formas.Forma;
import sistemapizzaria.formas.Quadrado;
import sistemapizzaria.formas.TrianguloEquilatero;
import sistemapizzaria.pizza.Pizza;
import sistemapizzaria.pizza.Sabor;

/**
 * Componente View para montagem polimórfica de Pizza (Forma + Sabores) (Padrão MVC - Aula 06).
 * Os sabores disponíveis são fornecidos pelo controller (a View não acessa o banco de dados).
 */
public class PainelMontagemPizza extends JPanel {

    private static final long serialVersionUID = 1L;

    private JComboBox<String> comboBoxFormaPizza;
    private JRadioButton radioLadoRaio;
    private JRadioButton radioArea;
    private ButtonGroup buttonGroup;
    private JTextField txtTamanho;
    private JLabel lblDefinirTamanho;
    private JLabel lblObs;
    private JLabel lblTamanhoLadodiametro;
    private JLabel lblrea;
    private JComboBox<String> comboBox_1;
    private JComboBox<String> comboBox_2;
    private JLabel lblPre;
    private JButton btnAdicionarPizza;
    private JButton btnCancelarEdicao;

    private List<Sabor> listaSaboresCache = new ArrayList<>();
    private final Supplier<List<Sabor>> fornecedorSabores;
    private final PizzaAdicionadaListener listener;
    private Runnable acaoCancelarEdicao;

    // Verdadeiro enquanto os combos de sabores são refeitos (evita atualizar a pré-visualização a cada item)
    private boolean recarregandoSabores = false;

    // Dados da pizza carregada para edição. O campo de tamanho mostra só 2 casas decimais; se o usuário não mexer
    // nele, a medida exata é reaproveitada, para que editar uma pizza não altere (de leve) a sua área.
    private String formatoCarregado;
    private String textoCarregado;
    private double dimensaoCarregada;

    public interface PizzaAdicionadaListener {
        void onPizzaAdicionada(Pizza pizza);
    }

    public PainelMontagemPizza(Supplier<List<Sabor>> fornecedorSabores, PizzaAdicionadaListener listener) {
        this.fornecedorSabores = fornecedorSabores;
        this.listener = listener;

        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createTitledBorder("Montar Pizza"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Linha 0: Forma da Pizza
        gbc.gridx = 0; gbc.gridy = 0;
        add(new JLabel("Forma da Pizza:"), gbc);
        comboBoxFormaPizza = new JComboBox<>(new String[]{"Círculo", "Quadrado", "Triângulo Equilátero"});
        gbc.gridx = 1; gbc.gridwidth = 2;
        add(comboBoxFormaPizza, gbc);

        // Linha 1: Radios de definição
        radioLadoRaio = new JRadioButton("Definir tamanho por raio/lado");
        radioArea = new JRadioButton("Definir tamanho por área (cm²)");
        buttonGroup = new ButtonGroup();
        buttonGroup.add(radioLadoRaio);
        buttonGroup.add(radioArea);
        radioLadoRaio.setSelected(true);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        add(radioLadoRaio, gbc);
        gbc.gridx = 2; gbc.gridwidth = 2;
        add(radioArea, gbc);

        // Linha 2: Tamanho e Obs
        lblDefinirTamanho = new JLabel("Definir Raio (cm) :");
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1;
        add(lblDefinirTamanho, gbc);

        txtTamanho = new JTextField(10);
        gbc.gridx = 1;
        add(txtTamanho, gbc);

        lblObs = new JLabel(String.format("Obs: raio deve ter entre %.0f a %.0f cm", Circulo.MIN_RAIO, Circulo.MAX_RAIO));
        gbc.gridx = 2; gbc.gridwidth = 2;
        add(lblObs, gbc);

        // Linha 3: Labels de Resultados
        lblTamanhoLadodiametro = new JLabel("Tamanho Lado/Raio : -- cm ");
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        add(lblTamanhoLadodiametro, gbc);

        lblrea = new JLabel("Área : -- cm²");
        gbc.gridx = 2; gbc.gridwidth = 2;
        add(lblrea, gbc);

        // Linha 4: Sabores
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 1;
        add(new JLabel("Sabores (até 2):"), gbc);

        comboBox_1 = new JComboBox<>();
        gbc.gridx = 1; gbc.gridwidth = 1;
        add(comboBox_1, gbc);

        comboBox_2 = new JComboBox<>();
        gbc.gridx = 2; gbc.gridwidth = 2;
        add(comboBox_2, gbc);

        recarregarSabores();

        // Linha 5: Preço, Adicionar (ou Atualizar, no modo de edição) e Cancelar edição
        lblPre = new JLabel("Preço : R$ 0,00");
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        add(lblPre, gbc);

        btnAdicionarPizza = new JButton("Adicionar pizza");
        gbc.gridx = 2; gbc.gridwidth = 1;
        add(btnAdicionarPizza, gbc);

        btnCancelarEdicao = new JButton("Cancelar edição");
        btnCancelarEdicao.setVisible(false);
        gbc.gridx = 3; gbc.gridwidth = 1;
        add(btnCancelarEdicao, gbc);

        configurarEventos();
    }

    // Define o que acontece quando o usuário desiste de editar uma pizza do carrinho
    public void setAcaoCancelarEdicao(Runnable acao) {
        this.acaoCancelarEdicao = acao;
    }

    // Alterna entre montar uma pizza nova ("Adicionar pizza") e editar uma pizza do carrinho ("Atualizar pizza")
    public void definirModoEdicao(boolean edicao) {
        btnAdicionarPizza.setText(edicao ? "Atualizar pizza" : "Adicionar pizza");
        btnCancelarEdicao.setVisible(edicao);
        setBorder(BorderFactory.createTitledBorder(edicao ? "Editar Pizza do Carrinho" : "Montar Pizza"));
        revalidate();
        repaint();
    }

    public void recarregarSabores() {
        List<Sabor> sabores = (fornecedorSabores != null) ? fornecedorSabores.get() : null;
        listaSaboresCache = (sabores != null) ? new ArrayList<>(sabores) : new ArrayList<>();

        recarregandoSabores = true;
        try {
            comboBox_1.removeAllItems();
            comboBox_2.removeAllItems();
            comboBox_1.addItem("Selecione um sabor...");
            comboBox_2.addItem("Nenhuma (Pizza de 1 sabor)");
            for (Sabor sabor : listaSaboresCache) {
                String desc = sabor.getNome() + " - " + sabor.getTipo().getCategoria();
                comboBox_1.addItem(desc);
                comboBox_2.addItem(desc);
            }
        } finally {
            recarregandoSabores = false;
        }
    }

    private void configurarEventos() {
        ActionListener radioListener = e -> {
            atualizarLabelObs();
            atualizarPreview();
        };
        radioLadoRaio.addActionListener(radioListener);
        radioArea.addActionListener(radioListener);

        txtTamanho.addActionListener(e -> atualizarPreview());
        txtTamanho.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                atualizarPreview();
            }
        });
        txtTamanho.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                atualizarPreview();
            }

            @Override
            public void keyTyped(KeyEvent e) {
                char caractere = e.getKeyChar();
                if (!Character.isDigit(caractere) && caractere != '.' && caractere != ',') {
                    e.consume();
                }
            }
        });

        comboBoxFormaPizza.addActionListener(e -> {
            atualizarLabelObs();
            atualizarPreview();
        });

        ActionListener saborListener = e -> {
            if (!recarregandoSabores) {
                atualizarPreview();
            }
        };
        comboBox_1.addActionListener(saborListener);
        comboBox_2.addActionListener(saborListener);

        btnAdicionarPizza.addActionListener(e -> {
            try {
                Pizza novaPizza = construirPizzaLocal();
                if (novaPizza.calcularPreco() <= 0) {
                    JOptionPane.showMessageDialog(this, "A pizza calculada possui valor R$ 0,00. Verifique os tamanhos, sabores e a tabela de preços.");
                    return;
                }
                if (listener != null) {
                    listener.onPizzaAdicionada(novaPizza);
                }
                limparFormulario();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro na Pizza", JOptionPane.WARNING_MESSAGE);
            }
        });

        btnCancelarEdicao.addActionListener(e -> {
            if (acaoCancelarEdicao != null) {
                acaoCancelarEdicao.run();
            } else {
                limparFormulario();
            }
        });
    }

    private void atualizarLabelObs() {
        String formaSelecionada = comboBoxFormaPizza.getSelectedItem().toString();
        radioLadoRaio.setText(formaSelecionada.equals("Círculo") ? "Definir tamanho por raio" : "Definir tamanho por lado");
        if (radioArea.isSelected()) {
            lblDefinirTamanho.setText("Definir Área (cm²):");
            // Faixa realmente aceita pela forma escolhida: o círculo e o triângulo aceitam menos do que 100 a 1600 cm²,
            // porque o raio (7 a 23 cm) e o lado (20 a 60 cm) também têm limites.
            lblObs.setText("Obs: a área deve ter entre " + novaForma(formaSelecionada).descreverFaixaDeArea());
        } else {
            if (formaSelecionada.equals("Círculo")) {
                lblDefinirTamanho.setText("Definir Raio (cm) :");
                lblObs.setText(String.format("Obs: raio deve ter entre %.0f a %.0f cm", Circulo.MIN_RAIO, Circulo.MAX_RAIO));
            } else {
                lblDefinirTamanho.setText("Definir Lado (cm):");
                if (formaSelecionada.equals("Quadrado")) {
                    lblObs.setText(String.format("Obs: lado deve ter entre %.0f a %.0f cm", Quadrado.MIN_LADO, Quadrado.MAX_LADO));
                } else {
                    lblObs.setText(String.format("Obs: lado deve ter entre %.0f a %.0f cm", TrianguloEquilatero.MIN_LADO, TrianguloEquilatero.MAX_LADO));
                }
            }
        }
    }

    // Mostra, enquanto o usuário digita, o raio/lado e a área calculados e o preço.
    // O tamanho e a área não dependem dos sabores: aparecem assim que o valor digitado é válido; só o preço espera o sabor.
    private void atualizarPreview() {
        if (txtTamanho.getText().trim().isEmpty()) {
            mostrarRotulosNeutros();
            return;
        }

        Forma forma;
        try {
            forma = construirForma();
        } catch (Exception e) {
            lblTamanhoLadodiametro.setText("Tamanho inválido");
            lblTamanhoLadodiametro.setToolTipText(e.getMessage());
            lblrea.setText("Área : -- cm²");
            lblPre.setText("Preço : R$ 0,00");
            return;
        }

        lblTamanhoLadodiametro.setToolTipText(null);
        String nomeMedida = (forma instanceof Circulo) ? "Raio" : "Lado";
        lblTamanhoLadodiametro.setText(String.format("Tamanho %s: %.2f cm", nomeMedida, obterDimensao(forma)));
        lblrea.setText(String.format("Área: %.2f cm²", forma.calcularArea()));

        try {
            double precoCalculado = montarPizza(forma).calcularPreco();
            if (precoCalculado > 0) {
                lblPre.setText(String.format("Preço: R$ %.2f", precoCalculado));
            } else {
                lblPre.setText("Preço : R$ 0,00");
            }
        } catch (Exception e) {
            // Ainda não há sabor escolhido: sem sabor não há preço
            lblPre.setText("Preço : R$ 0,00");
        }
    }

    private void mostrarRotulosNeutros() {
        lblTamanhoLadodiametro.setText("Tamanho Lado/Raio : -- cm ");
        lblTamanhoLadodiametro.setToolTipText(null);
        lblrea.setText("Área : -- cm²");
        lblPre.setText("Preço : R$ 0,00");
    }

    // Cria a forma escolhida (círculo, quadrado ou triângulo) a partir do nome exibido no combo
    private Forma novaForma(String formato) {
        if ("Círculo".equals(formato)) return new Circulo();
        if ("Quadrado".equals(formato)) return new Quadrado();
        return new TrianguloEquilatero();
    }

    private double obterDimensao(Forma forma) {
        if (forma instanceof Circulo) return ((Circulo) forma).getRaio();
        if (forma instanceof Quadrado) return ((Quadrado) forma).getLado();
        if (forma instanceof TrianguloEquilatero) return ((TrianguloEquilatero) forma).getLado();
        return 0;
    }

    // Monta a forma a partir do que foi digitado (raio/lado ou área). Lança exceção com o motivo se o valor for inválido.
    private Forma construirForma() throws Exception {
        String textoTamanho = txtTamanho.getText().trim().replace(",", ".");
        if (textoTamanho.isEmpty()) {
            throw new Exception("Informe a dimensão ou área da pizza.");
        }

        // Só aceita número decimal simples (ex.: 12 ou 12.5). O Double.parseDouble aceitaria também "NaN" ou "1e3".
        if (!textoTamanho.matches("\\d+(\\.\\d*)?")) {
            throw new Exception("Valor numérico inválido.");
        }
        double valorDigitado;
        try {
            valorDigitado = Double.parseDouble(textoTamanho);
        } catch (NumberFormatException e) {
            throw new Exception("Valor numérico inválido.");
        }

        String formato = comboBoxFormaPizza.getSelectedItem().toString();
        Forma formaReal = novaForma(formato);

        if (radioLadoRaio.isSelected()) {
            boolean tamanhoInalterado = textoCarregado != null
                    && formato.equals(formatoCarregado)
                    && txtTamanho.getText().equals(textoCarregado);
            formaReal.definirPorDimensao(tamanhoInalterado ? dimensaoCarregada : valorDigitado);
        } else {
            formaReal.definirPorArea(valorDigitado);
        }
        return formaReal;
    }

    // Junta a forma já validada aos sabores escolhidos nos combos
    private Pizza montarPizza(Forma forma) throws Exception {
        int idx1 = comboBox_1.getSelectedIndex();
        if (idx1 <= 0 || idx1 - 1 >= listaSaboresCache.size()) {
            throw new Exception("Escolha o sabor principal!");
        }

        Pizza novaPizza = new Pizza();
        novaPizza.setForma(forma);
        novaPizza.adicionarSabor(listaSaboresCache.get(idx1 - 1));

        int idx2 = comboBox_2.getSelectedIndex();
        if (idx2 > 0 && idx2 - 1 < listaSaboresCache.size()) {
            novaPizza.adicionarSabor(listaSaboresCache.get(idx2 - 1));
        }

        return novaPizza;
    }

    public Pizza construirPizzaLocal() throws Exception {
        return montarPizza(construirForma());
    }

    public void carregarPizzaParaEdicao(Pizza pizzaOriginal) {
        recarregarSabores();
        if (pizzaOriginal == null) return;
        Forma f = pizzaOriginal.getForma();

        radioLadoRaio.setSelected(true);
        if (f instanceof Circulo) {
            comboBoxFormaPizza.setSelectedItem("Círculo");
        } else if (f instanceof Quadrado) {
            comboBoxFormaPizza.setSelectedItem("Quadrado");
        } else if (f instanceof TrianguloEquilatero) {
            comboBoxFormaPizza.setSelectedItem("Triângulo Equilátero");
        }
        txtTamanho.setText(String.format("%.2f", obterDimensao(f)));

        // Guarda a medida exata: se o usuário não alterar o campo, é ela (e não o texto arredondado) que vale
        formatoCarregado = comboBoxFormaPizza.getSelectedItem().toString();
        textoCarregado = txtTamanho.getText();
        dimensaoCarregada = obterDimensao(f);

        List<Sabor> sabores = pizzaOriginal.getSabores();
        if (!sabores.isEmpty()) {
            String sabor1Desc = sabores.get(0).getNome() + " - " + sabores.get(0).getTipo().getCategoria();
            comboBox_1.setSelectedItem(sabor1Desc);
        }
        if (sabores.size() > 1) {
            String sabor2Desc = sabores.get(1).getNome() + " - " + sabores.get(1).getTipo().getCategoria();
            comboBox_2.setSelectedItem(sabor2Desc);
        } else {
            comboBox_2.setSelectedIndex(0);
        }

        atualizarLabelObs();
        atualizarPreview();
    }

    // Esvazia o formulário e volta ao modo normal de "Adicionar pizza"
    public void limparFormulario() {
        recarregarSabores();
        txtTamanho.setText("");
        formatoCarregado = null;
        textoCarregado = null;
        mostrarRotulosNeutros();

        if (comboBox_1.getItemCount() > 0) comboBox_1.setSelectedIndex(0);
        if (comboBox_2.getItemCount() > 0) comboBox_2.setSelectedIndex(0);

        radioLadoRaio.setSelected(true);
        atualizarLabelObs();
        definirModoEdicao(false);
    }
}
