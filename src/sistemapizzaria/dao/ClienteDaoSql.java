package sistemapizzaria.dao;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import sistemapizzaria.cliente.Cliente;
import sistemapizzaria.cliente.Pedido;

public class ClienteDaoSql implements ClienteDao {

    private static ClienteDaoSql instance;

    private final String insert = "INSERT INTO clientes (nome, sobrenome, telefone) VALUES (?, ?, ?)";
    private final String update = "UPDATE clientes SET nome = ?, sobrenome = ?, telefone = ? WHERE id = ?";
    private final String deleteById = "DELETE FROM clientes WHERE id = ?";
    private final String deleteByTel = "DELETE FROM clientes WHERE telefone = ?";
    private final String selectAll = "SELECT id, nome, sobrenome, telefone FROM clientes ORDER BY nome, sobrenome";
    private final String selectById = "SELECT id, nome, sobrenome, telefone FROM clientes WHERE id = ?";
    private final String selectByTelefone = "SELECT id, nome, sobrenome, telefone FROM clientes WHERE telefone = ?";
    // O telefone é guardado só com dígitos, então o LIKE do SQLite resolve a busca. O sobrenome NÃO é filtrado no SQL
    // (veja TextoUtil): a comparação sem diferenciar maiúsculas/acentos é feita em Java.
    private final String selectByTelefoneParcial =
        "SELECT id, nome, sobrenome, telefone FROM clientes " +
        "WHERE telefone LIKE ? " +
        "ORDER BY nome, sobrenome";

    private ClienteDaoSql() {
    }

    public static synchronized ClienteDaoSql getClienteDaoSql() {
        if (instance == null) {
            instance = new ClienteDaoSql();
        }
        return instance;
    }

    @Override
    public void add(Cliente cliente) throws SQLException, IOException {
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, cliente.getNome().trim());
            stmt.setString(2, cliente.getSobrenome().trim());
            stmt.setString(3, cliente.getTelefone().trim());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    cliente.setId(rs.getLong(1));
                }
            }
        }
    }

    @Override
    public void update(Cliente cliente) throws SQLException, IOException {
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(update)) {
            stmt.setString(1, cliente.getNome().trim());
            stmt.setString(2, cliente.getSobrenome().trim());
            stmt.setString(3, cliente.getTelefone().trim());
            stmt.setLong(4, cliente.getId());
            stmt.executeUpdate();
        }
    }

    @Override
    public void delete(Cliente cliente) throws SQLException, IOException {
        try (Connection con = ConnectionFactory.getConnection()) {
            // A FK em pedidos possui ON DELETE CASCADE, excluindo automaticamente os pedidos e pizzas do cliente (Requisito 1.c)
            if (cliente.getId() > 0) {
                try (PreparedStatement stmt = con.prepareStatement(deleteById)) {
                    stmt.setLong(1, cliente.getId());
                    stmt.executeUpdate();
                }
            } else {
                try (PreparedStatement stmt = con.prepareStatement(deleteByTel)) {
                    stmt.setString(1, cliente.getTelefone());
                    stmt.executeUpdate();
                }
            }
        }
    }

    @Override
    public List<Cliente> getAll() throws SQLException, IOException {
        List<Cliente> clientes = new ArrayList<>();
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(selectAll);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                clientes.add(mapearClienteSemPedidos(rs));
            }
        }
        carregarPedidosParaLista(clientes);
        return clientes;
    }

    @Override
    public Cliente getById(long id) throws SQLException, IOException {
        Cliente cliente = null;
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(selectById)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    cliente = mapearClienteSemPedidos(rs);
                }
            }
        }
        if (cliente != null) {
            List<Pedido> pedidos = DaoFactory.getPedidoDao().getByClienteId(cliente.getId());
            cliente.setPedidos(pedidos);
        }
        return cliente;
    }

    @Override
    public Cliente getByTelefone(String telefone) throws SQLException, IOException {
        if (telefone == null || telefone.trim().isEmpty()) {
            return null;
        }
        Cliente cliente = null;
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(selectByTelefone)) {
            stmt.setString(1, telefone.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    cliente = mapearClienteSemPedidos(rs);
                }
            }
        }
        if (cliente != null) {
            List<Pedido> pedidos = DaoFactory.getPedidoDao().getByClienteId(cliente.getId());
            cliente.setPedidos(pedidos);
        }
        return cliente;
    }

    @Override
    public List<Cliente> buscarPorFiltro(String filtro) throws SQLException, IOException {
        if (filtro == null || filtro.trim().isEmpty()) {
            return getAll();
        }
        // O mesmo termo é procurado no sobrenome (sem diferenciar maiúsculas/acentos) ou no telefone
        String termoSobrenome = TextoUtil.normalizar(filtro);
        String termoTelefone = filtro.trim();
        List<Cliente> clientes = new ArrayList<>();
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(selectAll);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Cliente c = mapearClienteSemPedidos(rs);
                if (TextoUtil.normalizar(c.getSobrenome()).contains(termoSobrenome)
                        || c.getTelefone().contains(termoTelefone)) {
                    clientes.add(c);
                }
            }
        }
        carregarPedidosParaLista(clientes);
        return clientes;
    }

    @Override
    public List<Cliente> buscarPorSobrenomeETelefone(String sobrenome, String telefone) throws SQLException, IOException {
        String termoSobrenome = TextoUtil.normalizar(sobrenome);
        String termoTelefone = "%" + (telefone != null ? telefone.trim() : "") + "%";
        List<Cliente> clientes = new ArrayList<>();
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(selectByTelefoneParcial)) {
            stmt.setString(1, termoTelefone);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Cliente c = mapearClienteSemPedidos(rs);
                    // Parte do sobrenome, sem diferenciar maiúsculas nem acentos (ex.: "alvares" encontra "Álvares")
                    if (TextoUtil.normalizar(c.getSobrenome()).contains(termoSobrenome)) {
                        clientes.add(c);
                    }
                }
            }
        }
        carregarPedidosParaLista(clientes);
        return clientes;
    }

    private Cliente mapearClienteSemPedidos(ResultSet rs) throws SQLException {
        return new Cliente(
            rs.getLong("id"),
            rs.getString("nome"),
            rs.getString("sobrenome"),
            rs.getString("telefone")
        );
    }

    private void carregarPedidosParaLista(List<Cliente> clientes) throws SQLException, IOException {
        PedidoDao pedidoDao = DaoFactory.getPedidoDao();
        for (Cliente c : clientes) {
            c.setPedidos(pedidoDao.getByClienteId(c.getId()));
        }
    }
}
