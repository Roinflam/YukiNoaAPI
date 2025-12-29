package pers.tany.yukinoaapi.realizationpart.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import pers.tany.yukinoaapi.Main;
import pers.tany.yukinoaapi.interfacepart.sql.ISql;
import pers.tany.yukinoaapi.util.LangUtil;
import pers.tany.yukinoaapi.util.LogUtil;

import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * MySQL数据库操作实现类
 * 使用HikariCP连接池管理数据库连接
 *
 * @author tany
 * @since 1.0.0
 */
public class MySQL implements ISql {

    /**
     * 数据库连接池
     */
    private HikariDataSource dataSource;

    /**
     * 数据库名称
     */
    private final String databaseName;

    /**
     * 数据库地址
     */
    private final String ip;

    /**
     * 数据库端口
     */
    private final String port;

    /**
     * 数据库用户名
     */
    private final String userName;

    /**
     * 数据库密码
     */
    private final String userPassword;

    /**
     * 构造MySQL数据库连接
     *
     * @param ip           数据库IP地址
     * @param port         数据库端口
     * @param userName     用户名
     * @param userPassword 密码
     * @param databaseName 数据库名
     */
    public MySQL(String ip, String port, String userName, String userPassword, String databaseName) {
        this.ip = ip;
        this.port = port;
        this.userName = userName;
        this.userPassword = userPassword;
        this.databaseName = databaseName;

        initDataSource();
    }

    /**
     * 初始化HikariCP连接池
     */
    private void initDataSource() {
        try {
            HikariConfig config = new HikariConfig();

            // 获取自定义连接参数
            String customParams = Main.config.getString("Database.MySQL.Parameters",
                    "useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8");

            String jdbcUrl = "jdbc:mysql://" + ip + ":" + port + "/" + databaseName + "?" + customParams;

            config.setJdbcUrl(jdbcUrl);
            config.setUsername(userName);
            config.setPassword(userPassword);

            // 连接池配置
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setIdleTimeout(300000);
            config.setConnectionTimeout(10000);
            config.setMaxLifetime(600000);

            // 连接测试
            config.setConnectionTestQuery("SELECT 1");

            dataSource = new HikariDataSource(config);

            LogUtil.success(LangUtil.get("database.mysql-connect-success", databaseName));
        } catch (Exception e) {
            LogUtil.error(LangUtil.get("database.mysql-connect-fail"), e);
        }
    }

    /**
     * 确保连接可用
     */
    private void ensureConnection() {
        try {
            if (dataSource == null || dataSource.isClosed()) {
                LogUtil.warning(LangUtil.get("database.mysql-reconnecting"));
                initDataSource();
            }
        } catch (Exception e) {
            LogUtil.error(LangUtil.get("database.mysql-check-fail"), e);
        }
    }

    @Override
    public Connection getConnection() {
        ensureConnection();
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            LogUtil.error(LangUtil.get("database.mysql-connect-fail"), e);
            return null;
        }
    }

    @Override
    public void doCommand(String statement) {
        ensureConnection();
        LogUtil.debug(LangUtil.get("database.sql-execute", statement));

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(statement);
        } catch (SQLException e) {
            LogUtil.error(LangUtil.get("database.sql-execute-fail", statement), e);
        }
    }

    @Override
    @Nullable
    public ResultSet selectTable(String statement) {
        ensureConnection();
        LogUtil.debug(LangUtil.get("database.sql-query", statement));

        try {
            Connection conn = dataSource.getConnection();
            Statement stmt = conn.createStatement();
            return stmt.executeQuery(statement);
        } catch (SQLException e) {
            LogUtil.error(LangUtil.get("database.sql-query-fail", statement), e);
            return null;
        }
    }

    /**
     * 执行参数化更新语句
     *
     * @param sql    SQL语句，使用?作为占位符
     * @param params 参数列表
     * @return 影响的行数，失败返回-1
     */
    public int doCommand(String sql, Object... params) {
        ensureConnection();
        LogUtil.debug(LangUtil.get("database.sql-execute", sql));

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }

            return ps.executeUpdate();
        } catch (SQLException e) {
            LogUtil.error(LangUtil.get("database.sql-param-execute-fail", sql), e);
            return -1;
        }
    }

    /**
     * 执行参数化查询语句
     *
     * @param sql    SQL语句，使用?作为占位符
     * @param params 参数列表
     * @return 查询结果集，失败返回null
     */
    @Nullable
    public ResultSet selectTable(String sql, Object... params) {
        ensureConnection();
        LogUtil.debug(LangUtil.get("database.sql-query", sql));

        try {
            Connection conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);

            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }

            return ps.executeQuery();
        } catch (SQLException e) {
            LogUtil.error(LangUtil.get("database.sql-param-query-fail", sql), e);
            return null;
        }
    }

    /**
     * 检查表是否存在
     *
     * @param tableName 表名
     * @return 存在返回true
     */
    public boolean tableExists(String tableName) {
        String sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = ? AND table_name = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, databaseName);
            ps.setString(2, tableName);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            LogUtil.error(LangUtil.get("database.mysql-table-check-fail", tableName), e);
        }

        return false;
    }

    @Override
    public void clearTable(String table) {
        doCommand("TRUNCATE TABLE " + table);
        LogUtil.debug(LangUtil.get("database.table-cleared", table));
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            LogUtil.info(LangUtil.get("database.mysql-closed", databaseName));
        }
    }

    @Override
    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }

    /**
     * 获取数据库名称
     *
     * @return 数据库名称
     */
    public String getDatabaseName() {
        return databaseName;
    }
}