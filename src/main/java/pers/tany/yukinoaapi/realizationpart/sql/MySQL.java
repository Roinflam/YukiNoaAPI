// 文件：MySQL.java
// 路径：src/pers/tany/yukinoaapi/realizationpart/sql/MySQL.java
package pers.tany.yukinoaapi.realizationpart.sql;

import pers.tany.yukinoaapi.Main;
import pers.tany.yukinoaapi.interfacepart.sql.ISql;
import pers.tany.yukinoaapi.util.LogUtil;

import javax.annotation.Nullable;
import java.sql.*;

/**
 * MySQL数据库操作实现类
 * 提供MySQL数据库的连接和基本操作
 */
public abstract class MySQL implements ISql {

    private final String ip;
    private final String port;
    private final String userName;
    private final String userPassword;
    private final String databaseName;
    private Connection connection;

    /**
     * 链接MySQL数据库
     *
     * @param ip           数据库IP地址
     * @param port         数据库端口
     * @param userName     用户名
     * @param userPassword 密码
     * @param databaseName 数据库名称
     */
    public MySQL(String ip, String port, String userName, String userPassword, String databaseName) {
        this.ip = ip;
        this.port = port;
        this.userName = userName;
        this.userPassword = userPassword;
        this.databaseName = databaseName;

        connect();
    }

    /**
     * 建立数据库连接
     */
    private void connect() {
        try {
            // 从配置文件获取自定义参数
            String customParams = "";
            if (Main.config != null) {
                customParams = Main.config.getString("Database.MySQL.Parameters", "");
            }

            // 构建连接URL
            String params = (customParams != null && !customParams.isEmpty())
                    ? customParams
                    : "useSSL=false&autoReconnect=true&characterEncoding=UTF-8";

            String url = String.format("jdbc:mysql://%s:%s/%s?%s", ip, port, databaseName, params);

            connection = DriverManager.getConnection(url, userName, userPassword);
            LogUtil.debug("MySQL数据库连接成功: " + databaseName);
        } catch (SQLException e) {
            LogUtil.error("MySQL数据库连接失败", e);
        }
    }

    /**
     * 检查并重新连接（如果连接已断开）
     */
    private void ensureConnection() {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(3)) {
                LogUtil.debug("MySQL连接已断开，正在重新连接...");
                connect();
            }
        } catch (SQLException e) {
            LogUtil.error("检查MySQL连接状态失败", e);
            connect();
        }
    }

    @Override
    public Connection getConnection() {
        ensureConnection();
        return connection;
    }

    @Override
    public void doCommand(String statement) {
        ensureConnection();

        PreparedStatement ps = null;
        try {
            ps = connection.prepareStatement(statement);
            ps.executeUpdate();
            LogUtil.debug("执行SQL: " + statement);
        } catch (SQLException e) {
            LogUtil.error("执行SQL失败: " + statement, e);
        } finally {
            ISql.closePreparedStatement(ps);
        }
    }

    @Nullable
    @Override
    public ResultSet selectTable(String statement) {
        ensureConnection();

        try {
            PreparedStatement ps = connection.prepareStatement(statement);
            LogUtil.debug("查询SQL: " + statement);
            return ps.executeQuery();
        } catch (SQLException e) {
            LogUtil.error("查询SQL失败: " + statement, e);
            return null;
        }
    }

    @Override
    public void clearTable(String table) {
        doCommand("DELETE FROM " + table);
        LogUtil.debug("已清空数据表: " + table);
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                LogUtil.debug("MySQL连接已关闭: " + databaseName);
            }
        } catch (SQLException e) {
            LogUtil.error("关闭MySQL连接失败", e);
        }
    }

    /**
     * 执行带参数的SQL语句（防SQL注入）
     *
     * @param sql    SQL语句，使用?作为占位符
     * @param params 参数列表
     * @return 影响的行数，失败返回-1
     */
    public int executeUpdate(String sql, Object... params) {
        ensureConnection();

        PreparedStatement ps = null;
        try {
            ps = connection.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps.executeUpdate();
        } catch (SQLException e) {
            LogUtil.error("执行参数化SQL失败: " + sql, e);
            return -1;
        } finally {
            ISql.closePreparedStatement(ps);
        }
    }

    /**
     * 执行带参数的查询语句（防SQL注入）
     *
     * @param sql    SQL语句，使用?作为占位符
     * @param params 参数列表
     * @return 查询结果
     */
    @Nullable
    public ResultSet executeQuery(String sql, Object... params) {
        ensureConnection();

        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps.executeQuery();
        } catch (SQLException e) {
            LogUtil.error("执行参数化查询失败: " + sql, e);
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
        ensureConnection();

        try {
            DatabaseMetaData meta = connection.getMetaData();
            ResultSet rs = meta.getTables(null, null, tableName, new String[]{"TABLE"});
            boolean exists = rs.next();
            rs.close();
            return exists;
        } catch (SQLException e) {
            LogUtil.error("检查表是否存在失败: " + tableName, e);
            return false;
        }
    }

    public String getIp() {
        return ip;
    }

    public String getPort() {
        return port;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public String getDatabaseName() {
        return databaseName;
    }
}