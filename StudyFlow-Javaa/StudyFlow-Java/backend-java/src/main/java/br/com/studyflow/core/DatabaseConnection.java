package br.com.studyflow.core;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Padrão SINGLETON #1
 * Garante uma única conexão MySQL compartilhada por toda a aplicação.
 * Qualquer chamada a {@code DatabaseConnection.getInstance()} devolve a mesma instância.
 */
public final class DatabaseConnection {

    private static volatile DatabaseConnection instancia;

    private final Object acesso = new Object(); // serializa o uso da conexão
    private Connection conexao;

    private DatabaseConnection() {
    }

    public static DatabaseConnection getInstance() {
        if (instancia == null) {
            synchronized (DatabaseConnection.class) {
                if (instancia == null) { // double-checked locking
                    instancia = new DatabaseConnection();
                }
            }
        }
        return instancia;
    }

    private Connection conexaoAtiva() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("Driver MySQL carregado com sucesso!");
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                    "Driver MySQL não encontrado no classpath da aplicação.", e
            );
        }

        if (conexao == null || conexao.isClosed() || !conexao.isValid(2)) {
            conexao = DriverManager.getConnection(
                    Config.urlJdbc(),
                    Config.usuario(),
                    Config.senha()
            );
            conexao.setAutoCommit(true);
        }

        return conexao;
    }

    /** INSERT/UPDATE/DELETE. */
    public long executar(String sql, Object... parametros) {
        synchronized (acesso) {
            try (PreparedStatement ps = conexaoAtiva().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                preencher(ps, parametros);
                ps.executeUpdate();
                try (ResultSet chaves = ps.getGeneratedKeys()) {
                    return chaves.next() ? chaves.getLong(1) : 0L;
                }
            } catch (SQLException e) {
                throw new ErroBancoException(e);
            }
        }
    }

    /** SELECT. */
    public List<Map<String, Object>> consultar(String sql, Object... parametros) {
        synchronized (acesso) {
            try (PreparedStatement ps = conexaoAtiva().prepareStatement(sql)) {
                preencher(ps, parametros);
                try (ResultSet rs = ps.executeQuery()) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int colunas = meta.getColumnCount();
                    List<Map<String, Object>> linhas = new ArrayList<>();
                    while (rs.next()) {
                        Map<String, Object> linha = new LinkedHashMap<>();
                        for (int i = 1; i <= colunas; i++) {
                            linha.put(meta.getColumnLabel(i), rs.getObject(i));
                        }
                        linhas.add(linha);
                    }
                    return linhas;
                }
            } catch (SQLException e) {
                throw new ErroBancoException(e);
            }
        }
    }

    public Map<String, Object> consultarUm(String sql, Object... parametros) {
        List<Map<String, Object>> linhas = consultar(sql, parametros);
        return linhas.isEmpty() ? null : linhas.get(0);
    }

    public void fechar() {
        synchronized (acesso) {
            try {
                if (conexao != null) {
                    conexao.close();
                }
            } catch (SQLException ignorado) {

            } finally {
                conexao = null;
            }
        }
    }

    private static void preencher(PreparedStatement ps, Object[] parametros) throws SQLException {
        for (int i = 0; i < parametros.length; i++) {
            ps.setObject(i + 1, converter(parametros[i]));
        }
    }

    private static Object converter(Object valor) {
        if (valor instanceof LocalDate d) {
            return java.sql.Date.valueOf(d);
        }
        if (valor instanceof LocalDateTime t) {
            return Timestamp.valueOf(t);
        }
        if (valor instanceof LocalTime h) {
            return java.sql.Time.valueOf(h);
        }
        return valor;
    }
}
