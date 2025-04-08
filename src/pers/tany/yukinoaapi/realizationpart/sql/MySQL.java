package pers.tany.yukinoaapi.realizationpart.sql;


import pers.tany.yukinoaapi.Main;
import pers.tany.yukinoaapi.interfacepart.sql.ISql;

import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * mysql数据库的操作
 */
public abstract class MySQL implements ISql {
    private String ip;
    private String port;
    private String userName;
    private String userPassword;
    private String databaseName;
    private Connection connection;

    /**
     * 链接MySql数据库
     *
     * @param ip           ip
     * @param port         端口
     * @param userName     用户名
     * @param userPassword 密码
     * @param databaseName 数据库名称
     */
    public MySQL(String ip, String port, String userName, String userPassword, String databaseName) {
        try {
            this.ip = ip;
            this.port = port;
            this.userName = userName;
            this.userPassword = userPassword;
            this.databaseName = databaseName;

            // 从配置文件获取自定义参数
            String customParams = Main.config.getString("Database.MySQL.Parameters", "");

            // 构建连接URL，如果自定义参数为空则使用默认参数
            String url = String.format(
                    "jdbc:mysql://%s:%s/%s?%s",
                    ip,
                    port,
                    databaseName,
                    !customParams.isEmpty() ? customParams : "useSSL=false&autoReconnect=true"
            );

            connection = DriverManager.getConnection(url, userName, userPassword);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public void doCommand(String statement) {
        try {
            connection.prepareStatement(statement).executeUpdate(statement);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Nullable
    @Override
    public ResultSet selectTable(String statement) {
        try {
            return connection.prepareStatement(statement).executeQuery(statement);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void clearTable(String table) {
        this.doCommand("delete from " + table);
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
