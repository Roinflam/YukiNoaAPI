package pers.tany.yukinoaapi.realizationpart.sql;


import pers.tany.yukinoaapi.interfacepart.configuration.IFile;
import pers.tany.yukinoaapi.interfacepart.sql.ISql;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * sqlite数据库的操作
 */
public abstract class Sqlite implements ISql {
    private String name;
    private String dirName;
    private String dataBaseName;
    private Connection connection;

    /**
     * 打开插件文件夹指定插件文件夹下的Sqlite数据库
     * 如果不存在则创建
     *
     * @param name         插件的名称
     * @param dirName      文件夹名称
     * @param dataBaseName 数据库的名称(无需带扩展名)
     */
    public Sqlite(String name, String dirName, String dataBaseName) {
        try {
            File dir = new File(IFile.formattedPath(new File("plugins").getCanonicalPath() + "\\" + name + "\\" + dirName));
            dir.mkdirs();

            this.name = name;
            this.dirName = dirName;
            this.dataBaseName = dataBaseName;
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + IFile.formattedPath(new File("plugins").getCanonicalPath() + "\\" + name + "\\" + dirName + "\\" + dataBaseName + ".db"));
        } catch (IOException | SQLException e) {
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

    /**
     * 删除并且关闭当前数据库
     *
     * @throws IOException  信号，一个I / O异常发生了某种类型的。此类是由失败或中断的I / O操作产生的异常的通用类。
     * @throws SQLException 在数据库访问错误或其他错误中提供信息的异常。
     */
    public void delDatabase() throws IOException, SQLException {
        File file = new File(IFile.formattedPath(new File("plugins").getCanonicalPath() + "\\" + name + "\\" + dirName + "\\" + dataBaseName + ".db"));
        file.delete();
        connection.close();
    }

    public String getName() {
        return name;
    }

    public String getDirName() {
        return dirName;
    }

    public String getDataBaseName() {
        return dataBaseName;
    }
}
