package pers.tany.yukinoaapi.realizationpart.sql;

import pers.tany.yukinoaapi.interfacepart.sql.ISql;
import pers.tany.yukinoaapi.util.LangUtil;
import pers.tany.yukinoaapi.util.LogUtil;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SQLite数据库操作实现类
 *
 * @author tany
 * @since 1.0.0
 */
public class Sqlite implements ISql {

    /**
     * 数据库连接
     */
    private Connection connection;

    /**
     * 数据库文件路径
     */
    private final String dbPath;

    /**
     * 数据库文件
     */
    private final File dbFile;

    /**
     * 构造SQLite数据库连接
     *
     * @param pluginName   插件名称
     * @param dirName      目录名称
     * @param dataBaseName 数据库名称（不含.db后缀）
     */
    public Sqlite(String pluginName, String dirName, String dataBaseName) {
        String path;
        try {
            path = new File("plugins/" + pluginName + "/" + dirName).getCanonicalPath();
        } catch (IOException e) {
            LogUtil.error(LangUtil.get("database.sqlite-path-fail"), e);
            path = "plugins/" + pluginName + "/" + dirName;
        }

        // 确保目录存在
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        this.dbPath = path + "/" + dataBaseName + ".db";
        this.dbFile = new File(dbPath);

        initConnection();
    }

    /**
     * 初始化数据库连接
     */
    private void initConnection() {
        try {
            // 加载SQLite驱动
            Class.forName("org.sqlite.JDBC");

            if (dbFile == null) {
                LogUtil.error(LangUtil.get("database.sqlite-path-invalid"));
                return;
            }

            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);

            // 启用外键支持
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
            }

            LogUtil.success(LangUtil.get("database.sqlite-connect-success", dbFile.getName()));
        } catch (ClassNotFoundException e) {
            LogUtil.error(LangUtil.get("database.sqlite-driver-fail"), e);
        } catch (SQLException e) {
            LogUtil.error(LangUtil.get("database.sqlite-connect-fail"), e);
        }
    }

    /**
     * 确保连接可用
     */
    private void ensureConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                LogUtil.warning(LangUtil.get("database.sqlite-reconnecting"));
                initConnection();
            }
        } catch (SQLException e) {
            LogUtil.error(LangUtil.get("database.sqlite-check-fail"), e);
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
        LogUtil.debug(LangUtil.get("database.sql-execute", statement));

        try (Statement stmt = connection.createStatement()) {
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
            Statement stmt = connection.createStatement();
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

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
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
            PreparedStatement ps = connection.prepareStatement(sql);
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
        String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name=?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LogUtil.error(LangUtil.get("database.sqlite-table-check-fail", tableName), e);
        }

        return false;
    }

    @Override
    public void clearTable(String table) {
        doCommand("DELETE FROM " + table);
        doCommand("DELETE FROM sqlite_sequence WHERE name = '" + table + "'");
        LogUtil.debug(LangUtil.get("database.table-cleared", table));
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                LogUtil.info(LangUtil.get("database.sqlite-closed", dbFile.getName()));
            }
        } catch (SQLException e) {
            LogUtil.error(LangUtil.get("database.sqlite-close-fail"), e);
        }
    }

    @Override
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * 删除数据库文件
     *
     * @return 删除成功返回true
     */
    public boolean deleteDatabase() {
        close();

        if (dbFile.exists()) {
            boolean deleted = dbFile.delete();
            if (deleted) {
                LogUtil.info(LangUtil.get("database.sqlite-deleted", dbFile.getName()));
            } else {
                LogUtil.error(LangUtil.get("database.sqlite-delete-fail", dbFile.getName()));
            }
            return deleted;
        }

        return true;
    }

    /**
     * 备份数据库文件
     *
     * @param backupFile 备份文件
     * @return 备份成功返回true
     */
    public boolean backup(File backupFile) {
        try {
            Files.copy(dbFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            LogUtil.success(LangUtil.get("database.sqlite-backup-success", backupFile.getName()));
            return true;
        } catch (IOException e) {
            LogUtil.error(LangUtil.get("database.sqlite-backup-fail"), e);
            return false;
        }
    }

    /**
     * 获取数据库文件路径
     *
     * @return 数据库文件路径
     */
    public String getDbPath() {
        return dbPath;
    }

    /**
     * 获取数据库文件
     *
     * @return 数据库文件
     */
    public File getDbFile() {
        return dbFile;
    }
}