package pers.tany.yukinoaapi.interfacepart.sql;

import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * SQL数据库相关接口
 */
public interface ISql {

    /**
     * 返回对象的Connection
     *
     * @return 这个对象的Connection实例
     */
    Connection getConnection();

    /**
     * 执行sqlite语句
     *
     * @param statement 执行的sqlite语句
     * @throws SQLException 在数据库访问错误或其他错误中提供信息的异常。
     */
    void doCommand(String statement);

    /**
     * 执行查询语句，返回查询的结果
     *
     * @param statement 执行的语句
     * @return 查询的结果
     */
    @Nullable
    ResultSet selectTable(String statement);

    /**
     * 清空数据表内容
     *
     * @param table 数据表名称
     */
    void clearTable(String table);
}
