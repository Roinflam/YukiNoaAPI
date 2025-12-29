// 文件：ISql.java
// 路径：src/pers/tany/yukinoaapi/interfacepart/sql/ISql.java
package pers.tany.yukinoaapi.interfacepart.sql;

import pers.tany.yukinoaapi.util.LogUtil;

import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * SQL数据库相关接口
 * 提供数据库基本操作的统一规范
 */
public interface ISql {

    /**
     * 返回对象的Connection
     *
     * @return 这个对象的Connection实例
     */
    Connection getConnection();

    /**
     * 执行SQL语句（增删改）
     *
     * @param statement 执行的SQL语句
     */
    void doCommand(String statement);

    /**
     * 执行查询语句，返回查询的结果
     *
     * @param statement 执行的SQL语句
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

    /**
     * 关闭数据库连接
     * 建议在插件卸载时调用
     */
    default void close() {
        try {
            Connection conn = getConnection();
            if (conn != null && !conn.isClosed()) {
                conn.close();
                LogUtil.debug("数据库连接已关闭");
            }
        } catch (SQLException e) {
            LogUtil.error("关闭数据库连接失败", e);
        }
    }

    /**
     * 检查数据库连接是否有效
     *
     * @return 连接有效返回true
     */
    default boolean isConnected() {
        try {
            Connection conn = getConnection();
            return conn != null && !conn.isClosed() && conn.isValid(3);
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * 安全关闭ResultSet
     *
     * @param rs ResultSet对象
     */
    static void closeResultSet(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException ignored) {
            }
        }
    }

    /**
     * 安全关闭PreparedStatement
     *
     * @param ps PreparedStatement对象
     */
    static void closePreparedStatement(PreparedStatement ps) {
        if (ps != null) {
            try {
                ps.close();
            } catch (SQLException ignored) {
            }
        }
    }
}