package sistemapizzaria.dao;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import sistemapizzaria.cliente.Pedido;
import sistemapizzaria.cliente.StatusPedido;
import sistemapizzaria.excecoes.PedidoNaoEditavelException;
import sistemapizzaria.formas.Circulo;
import sistemapizzaria.formas.Forma;
import sistemapizzaria.formas.Quadrado;
import sistemapizzaria.formas.TrianguloEquilatero;
import sistemapizzaria.pizza.CategoriaTipo;
import sistemapizzaria.pizza.Pizza;
import sistemapizzaria.pizza.Sabor;
import sistemapizzaria.pizza.TipoPizza;

public class PedidoDaoSql implements PedidoDao {

    private static PedidoDaoSql instance;

    private final String insertPedido = "INSERT INTO pedidos (id_cliente, preco_total, status) VALUES (?, ?, ?)";
    // Só altera o total de um pedido que continua ABERTO no banco. O estado muda apenas por updateStatus: uma cópia
    // desatualizada do pedido (ex.: já marcado como entregue em "Gerenciar Pedidos") não pode reabri-lo nem alterá-lo.
    private final String updatePedidoAberto = "UPDATE pedidos SET preco_total = ? WHERE id = ? AND status = 'ABERTO'";
    private final String updateStatus = "UPDATE pedidos SET status = ?, preco_total = ? WHERE id = ?";
    private final String selectStatus = "SELECT status FROM pedidos WHERE id = ?";
    private final String deletePedido = "DELETE FROM pedidos WHERE id = ?";
    private final String selectByCliente = "SELECT id, preco_total, status FROM pedidos WHERE id_cliente = ? ORDER BY id";
    private final String selectById = "SELECT id, preco_total, status FROM pedidos WHERE id = ?";
    private final String selectAbertos = "SELECT id, preco_total, status FROM pedidos WHERE status = 'ABERTO'";

    private final String insertPizza = "INSERT INTO pizzas (id_pedido, tipo_forma, dimensao) VALUES (?, ?, ?)";
    private final String deletePizzasDoPedido = "DELETE FROM pizzas WHERE id_pedido = ?";
    private final String selectPizzasDoPedido = "SELECT id, tipo_forma, dimensao FROM pizzas WHERE id_pedido = ? ORDER BY id";

    private final String insertPizzaSabor = "INSERT INTO pizzas_sabores (id_pizza, id_sabor) VALUES (?, ?)";
    private final String selectSaboresDaPizza =
        "SELECT s.id, s.nome, s.categoria, t.preco_cm2 " +
        "FROM pizzas_sabores ps " +
        "INNER JOIN sabores s ON ps.id_sabor = s.id " +
        "INNER JOIN tipos_pizza t ON s.categoria = t.categoria " +
        "WHERE ps.id_pizza = ? " +
        "ORDER BY ps.id";

    private PedidoDaoSql() {
    }

    public static synchronized PedidoDaoSql getPedidoDaoSql() {
        if (instance == null) {
            instance = new PedidoDaoSql();
        }
        return instance;
    }

    @Override
    public void add(long idCliente, Pedido pedido) throws SQLException, IOException {
        try (Connection con = ConnectionFactory.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement stmt = con.prepareStatement(insertPedido, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setLong(1, idCliente);
                    stmt.setDouble(2, pedido.calcularPrecoTotal());
                    stmt.setString(3, pedido.getStatus().name());
                    stmt.executeUpdate();
                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            pedido.setIdentificador(rs.getInt(1));
                        }
                    }
                }
                inserirPizzasDoPedido(con, pedido);
                con.commit();
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    // Regrava as pizzas e o total de um pedido que continua ABERTO. Lança PedidoNaoEditavelException se, no banco,
    // o pedido já não estiver aberto (ou tiver sido excluído), sem alterar nada.
    @Override
    public void update(Pedido pedido) throws SQLException, IOException {
        try (Connection con = ConnectionFactory.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement stmt = con.prepareStatement(updatePedidoAberto)) {
                    stmt.setDouble(1, pedido.calcularPrecoTotal());
                    stmt.setInt(2, pedido.getIdentificador());
                    if (stmt.executeUpdate() == 0) {
                        throw new PedidoNaoEditavelException(descreverPedidoNaoEditavel(con, pedido.getIdentificador()));
                    }
                }
                // Remove pizzas antigas (e pizzas_sabores em cascata) e reinsere as novas
                try (PreparedStatement stmtDel = con.prepareStatement(deletePizzasDoPedido)) {
                    stmtDel.setInt(1, pedido.getIdentificador());
                    stmtDel.executeUpdate();
                }
                inserirPizzasDoPedido(con, pedido);
                con.commit();
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    // Muda o estado do pedido usando o estado que está NO BANCO (e não o da cópia em memória, que pode estar desatualizada).
    // Também mantém o total gravado coerente com a regra de preços:
    //  - ABERTO -> a caminho/entregue: grava (congela) o total calculado com os preços de agora;
    //  - a caminho/entregue -> a caminho/entregue: mantém o total já gravado;
    //  - a caminho/entregue -> ABERTO: o pedido volta a acompanhar a tabela de preços, então grava o total atual.
    @Override
    public void updateStatus(int idPedido, StatusPedido novoStatus) throws SQLException, IOException {
        try (Connection con = ConnectionFactory.getConnection()) {
            con.setAutoCommit(false);
            try {
                Pedido atual = buscarPedido(con, idPedido);
                if (atual == null) {
                    throw new PedidoNaoEditavelException("O pedido #" + idPedido + " não existe mais e não pode ser alterado.");
                }

                double total;
                if (atual.getStatus() != StatusPedido.ABERTO && novoStatus != StatusPedido.ABERTO) {
                    total = atual.getPrecoTotal();
                } else {
                    total = atual.calcularPrecoTotal();
                }

                try (PreparedStatement stmt = con.prepareStatement(updateStatus)) {
                    stmt.setString(1, novoStatus.name());
                    stmt.setDouble(2, total);
                    stmt.setInt(3, idPedido);
                    stmt.executeUpdate();
                }
                con.commit();
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    @Override
    public void delete(int idPedido) throws SQLException, IOException {
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(deletePedido)) {
            stmt.setInt(1, idPedido);
            stmt.executeUpdate();
        }
    }

    @Override
    public List<Pedido> getByClienteId(long idCliente) throws SQLException, IOException {
        List<Pedido> pedidos = new ArrayList<>();
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(selectByCliente)) {
            stmt.setLong(1, idCliente);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Pedido pedido = mapearPedido(con, rs);
                    pedidos.add(pedido);
                }
            }
        }
        return pedidos;
    }

    @Override
    public Pedido getById(int idPedido) throws SQLException, IOException {
        try (Connection con = ConnectionFactory.getConnection()) {
            return buscarPedido(con, idPedido);
        }
    }

    // Recalcula e grava o total apenas dos pedidos ABERTOS; os pedidos já fechados mantêm o total da época da venda.
    @Override
    public void recalcularPrecosPedidosAbertos() throws SQLException, IOException {
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(selectAbertos);
             ResultSet rs = stmt.executeQuery()) {
            List<Pedido> abertos = new ArrayList<>();
            while (rs.next()) {
                abertos.add(mapearPedido(con, rs));
            }
            for (Pedido p : abertos) {
                try (PreparedStatement stmtUpd = con.prepareStatement(updatePedidoAberto)) {
                    stmtUpd.setDouble(1, p.calcularPrecoTotal());
                    stmtUpd.setInt(2, p.getIdentificador());
                    stmtUpd.executeUpdate();
                }
            }
        }
    }

    // Lê um pedido (com suas pizzas) usando uma conexão já aberta; devolve null se ele não existir.
    private Pedido buscarPedido(Connection con, int idPedido) throws SQLException {
        try (PreparedStatement stmt = con.prepareStatement(selectById)) {
            stmt.setInt(1, idPedido);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapearPedido(con, rs);
                }
            }
        }
        return null;
    }

    // Mensagem explicando por que o pedido não pôde ser alterado (já não está aberto ou foi excluído).
    private String descreverPedidoNaoEditavel(Connection con, int idPedido) throws SQLException {
        try (PreparedStatement stmt = con.prepareStatement(selectStatus)) {
            stmt.setInt(1, idPedido);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return "O pedido #" + idPedido + " não está mais aberto (estado atual: " + rs.getString(1)
                            + ") e não pode ser alterado.";
                }
            }
        }
        return "O pedido #" + idPedido + " não existe mais e não pode ser alterado.";
    }

    private void inserirPizzasDoPedido(Connection con, Pedido pedido) throws SQLException {
        for (Pizza pizza : pedido.getPizzas()) {
            Forma forma = pizza.getForma();
            String tipoForma;
            double dimensao;
            if (forma instanceof Circulo) {
                tipoForma = "CIRCULO";
                dimensao = ((Circulo) forma).getRaio();
            } else if (forma instanceof Quadrado) {
                tipoForma = "QUADRADO";
                dimensao = ((Quadrado) forma).getLado();
            } else if (forma instanceof TrianguloEquilatero) {
                tipoForma = "TRIANGULO";
                dimensao = ((TrianguloEquilatero) forma).getLado();
            } else {
                throw new SQLException("Forma geométrica desconhecida para persistência.");
            }

            long idPizzaGerado = 0;
            try (PreparedStatement stmtPizza = con.prepareStatement(insertPizza, Statement.RETURN_GENERATED_KEYS)) {
                stmtPizza.setInt(1, pedido.getIdentificador());
                stmtPizza.setString(2, tipoForma);
                stmtPizza.setDouble(3, dimensao);
                stmtPizza.executeUpdate();
                try (ResultSet rs = stmtPizza.getGeneratedKeys()) {
                    if (rs.next()) {
                        idPizzaGerado = rs.getLong(1);
                        pizza.setId(idPizzaGerado);
                    }
                }
            }

            try (PreparedStatement stmtPS = con.prepareStatement(insertPizzaSabor)) {
                for (Sabor sabor : pizza.getSabores()) {
                    stmtPS.setLong(1, idPizzaGerado);
                    stmtPS.setLong(2, sabor.getId());
                    stmtPS.executeUpdate();
                }
            }
        }
    }

    private Pedido mapearPedido(Connection con, ResultSet rsPedido) throws SQLException {
        int idPedido = rsPedido.getInt("id");
        double precoTotal = rsPedido.getDouble("preco_total");
        StatusPedido status = StatusPedido.valueOf(rsPedido.getString("status"));

        Pedido pedido = new Pedido(idPedido, precoTotal, status);
        List<Pizza> pizzas = carregarPizzasDoPedido(con, idPedido);
        for (Pizza p : pizzas) {
            pedido.adicionarPizza(p);
        }
        return pedido;
    }

    private List<Pizza> carregarPizzasDoPedido(Connection con, int idPedido) throws SQLException {
        List<Pizza> pizzas = new ArrayList<>();
        try (PreparedStatement stmtPizzas = con.prepareStatement(selectPizzasDoPedido)) {
            stmtPizzas.setInt(1, idPedido);
            try (ResultSet rsPizzas = stmtPizzas.executeQuery()) {
                while (rsPizzas.next()) {
                    long idPizza = rsPizzas.getLong("id");
                    String tipoForma = rsPizzas.getString("tipo_forma");
                    double dimensao = rsPizzas.getDouble("dimensao");

                    // Reconstrói polimorficamente a subclasse de Forma (Herança - Aula 05)
                    Forma forma;
                    if ("CIRCULO".equalsIgnoreCase(tipoForma)) {
                        forma = new Circulo();
                    } else if ("QUADRADO".equalsIgnoreCase(tipoForma)) {
                        forma = new Quadrado();
                    } else {
                        forma = new TrianguloEquilatero();
                    }
                    forma.definirPorDimensao(dimensao);

                    Pizza pizza = new Pizza(idPizza, forma);

                    try (PreparedStatement stmtSabores = con.prepareStatement(selectSaboresDaPizza)) {
                        stmtSabores.setLong(1, idPizza);
                        try (ResultSet rsSabores = stmtSabores.executeQuery()) {
                            while (rsSabores.next()) {
                                long idSabor = rsSabores.getLong("id");
                                String nomeSabor = rsSabores.getString("nome");
                                CategoriaTipo cat = CategoriaTipo.valueOf(rsSabores.getString("categoria"));
                                double precoCm2 = rsSabores.getDouble("preco_cm2");
                                Sabor sabor = new Sabor(idSabor, nomeSabor, new TipoPizza(cat, precoCm2));
                                pizza.adicionarSabor(sabor);
                            }
                        }
                    }

                    pizzas.add(pizza);
                }
            }
        }
        return pizzas;
    }
}
