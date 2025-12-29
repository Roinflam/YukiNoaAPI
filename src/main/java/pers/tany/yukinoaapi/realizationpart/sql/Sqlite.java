// 文件：Sqlite.java
// 路径：src/pers/tany/yukinoaapi/realizationpart/sql/Sqlite.java
package pers.tany.yukinoaapi.realizationpart.sql;

import pers.tany.yukinoaapi.interfacepart.configuration.IFile;
import pers.tany.yukinoaapi.interfacepart.sql.ISql;
import pers.tany.yukinoaapi.util.LogUtil;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.sql.*;

/**
 * SQLite数据库操作实现类
 * 提供SQLite数据库的连接和基本操作
 */
public abstract class Sqlite implements ISql {

    private final String pluginName;
    private final String dirName;
    private final String dataBaseName;
    private final File databaseFile;
    private Connection connection;

    /**
     * 打开指定插件文件夹下的SQLite数据库
     * 如果不存在则创建
     *
     * @param pluginName   插件的名称
     * @param dirName      文件夹名称（相对于插件文件夹）
     * @param dataBaseName 数据库的名称（无需带.db扩展名）
     */
    public Sqlite(String pluginName, String dirName, String dataBaseName) {
        this.pluginName = pluginName;
        this.dirName = dirName;
        this.dataBaseName = dataBaseName;

        // 构建数据库文件路径
        File dbFile = null;
        try {
            File dir = new File(IFile.formattedPath(
                    new File("plugins").getCanonicalPath() + "\\" + pluginName + "\\" + dirName
            ));
            dir.mkdirs();
            dbFile = new File(dir, dataBaseName + ".db");
        } catch (IOException e) {
            LogUtil.error("构建SQLite数据库路径失败", e);
        }
        this.databaseFile = dbFile;

        connect();
    }

    /**
     * 建立数据库连接
     */
    private void connect() {
        if (databaseFile == null) {
            LogUtil.error("SQLite数据库文件路径无效");
            return;
        }

        try {
            // 加载SQLite驱动
            Class.forName("org.sqlite.JDBC");

            String url = "jdbc:sqlite:" + databaseFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);

            // 启用外键支持
            doCommand("PRAGMA foreign_keys = ON");

            LogUtil.debug("SQLite数据库连接成功: " + dataBaseName);
        } catch (ClassNotFoundException e) {
            LogUtil.error("SQLite驱动加载失败，请确保已添加SQLite JDBC依赖", e);
        } catch (SQLException e) {
            LogUtil.error("SQLite数据库连接失败", e);
        }
    }

    /**
     * 检查并重新连接（如果连接已断开）
     */
    private void ensureConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                LogUtil.debug("SQLite连接已断开，正在重新连接...");
                connect();
            }
        } catch (SQLException e) {
            LogUtil.error("检查SQLite连接状态失败", e);
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
        // SQLite需要VACUUM来释放空间
        doCommand("VACUUM");
        LogUtil.debug("已清空数据表: " + table);
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                LogUtil.debug("SQLite连接已关闭: " + dataBaseName);
            }
        } catch (SQLException e) {
            LogUtil.error("关闭SQLite连接失败", e);
        }
    }

    /**
     * 删除数据库文件并关闭连接
     *
     * @return 是否删除成功
     */
    public boolean deleteDatabase() {
        close();

        if (databaseFile != null && databaseFile.exists()) {
            boolean deleted = databaseFile.delete();
            if (deleted) {
                LogUtil.debug("SQLite数据库文件已删除: " + dataBaseName);
            } else {
                LogUtil.warning("SQLite数据库文件删除失败: " + dataBaseName);
            }
            return deleted;
        }
        return false;
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
        String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name=?";
        ResultSet rs = executeQuery(sql, tableName);

        if (rs != null) {
            try {
                boolean exists = rs.next();
                rs.close();
                return exists;
            } catch (SQLException e) {
                LogUtil.error("检查表是否存在失败: " + tableName, e);
            }
        }
        return false;
    }

    /**
     * 备份数据库到指定文件
     *
     * @param backupFile 备份文件
     * @return 是否备份成功
     */
    public boolean backup(File backupFile) {
        if (databaseFile == null || !databaseFile.exists()) {
            return false;
        }

        try {
            java.nio.file.Files.copy(
                    databaseFile.toPath(),
                    backupFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );
            LogUtil.debug("SQLite数据库备份成功: " + backupFile.getName());
            return true;
        } catch (IOException e) {
            LogUtil.error("SQLite数据库备份失败", e);
            return false;
        }
    }

    public String getPluginName() {
        return pluginName;
    }

    public String getDirName() {
        return dirName;
    }

    public String getDataBaseName() {
        return dataBaseName;
    }

    public File getDatabaseFile() {
        return databaseFile;
    }
}