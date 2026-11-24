package sistemapizzaria.dao;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import sistemapizzaria.pizza.CategoriaTipo;
import sistemapizzaria.pizza.Sabor;
import sistemapizzaria.pizza.TipoPizza;

public class SaborDaoSql implements SaborDao {

    private static SaborDaoSql instance;

    private final String insert = "INSERT INTO sabores (nome, categoria) VALUES (?, ?)";
    private final String selectAll =
        "SELECT s.id, s.nome, s.categoria, t.preco_cm2 " +
        "FROM sabores s " +
        "INNER JOIN tipos_pizza t ON s.categoria = t.categoria " +
        "ORDER BY s.nome";
    private final String selectById =
        "SELECT s.id, s.nome, s.categoria, t.preco_cm2 " +
        "FROM sabores s " +
        "INNER JOIN tipos_pizza t ON s.categoria = t.categoria " +
        "WHERE s.id = ?";

    private SaborDaoSql() {
    }

    public static synchronized SaborDaoSql getSaborDaoSql() {
        if (instance == null) {
            instance = new SaborDaoSql();
        }
        return instance;
    }

    @Override
    public void add(Sabor sabor) throws SQLException, IOException {
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, sabor.getNome().trim());
            stmt.setString(2, sabor.getTipo().getCategoria().name());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    sabor.setId(rs.getLong(1));
                }
            }
        }
    }

    @Override
    public List<Sabor> getAll() throws SQLException, IOException {
        List<Sabor> lista = new ArrayList<>();
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(selectAll);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearSabor(rs));
            }
        }
        return lista;
    }

    @Override
    public Sabor getById(long id) throws SQLException, IOException {
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(selectById)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapearSabor(rs);
                }
            }
        }
        return null;
    }

    @Override
    public Sabor getByNome(String nome) throws SQLException, IOException {
        // A comparação é feita em Java e ignora maiúsculas/minúsculas e acentos (veja TextoUtil): o LOWER do SQLite
        // só trata letras ASCII, então "RÚCULA" não seria reconhecido como o mesmo sabor de "Rúcula".
        String procurado = TextoUtil.normalizar(nome);
        for (Sabor sabor : getAll()) {
            if (TextoUtil.normalizar(sabor.getNome()).equals(procurado)) {
                return sabor;
            }
        }
        return null;
    }

    private Sabor mapearSabor(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        String nome = rs.getString("nome");
        CategoriaTipo cat = CategoriaTipo.valueOf(rs.getString("categoria"));
        double preco = rs.getDouble("preco_cm2");
        TipoPizza tipo = new TipoPizza(cat, preco);
        return new Sabor(id, nome, tipo);
    }
}
