package com.beiktech.bontolink.common;

import com.beiktech.bontolink.entity.SysDataSource;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * 数据源连接公共工具：按 sys_data_source 配置动态建立 JDBC 连接。
 * 统一处理：连接地址校验、驱动加载、登录超时。调用方负责关闭返回的 Connection。
 */
@Component
public class DataSourceConnector {

    /** 默认登录超时(秒)，避免不可达主机长时间阻塞 */
    public static final int LOGIN_TIMEOUT = 8;

    /**
     * 按数据源配置打开一个 JDBC 连接（调用方负责关闭，建议 try-with-resources）。
     *
     * @throws SQLException 数据源为空 / 非 JDBC 类型 / 未配置连接地址 / 驱动未集成 / 连接失败
     */
    public Connection open(SysDataSource ds) throws SQLException {
        if (ds == null) throw new SQLException("数据源不存在");
        if ("mongodb".equalsIgnoreCase(ds.getDsType())) {
            throw new SQLException("MongoDB 暂不支持 JDBC 连接");
        }
        String url = ds.getJdbcUrl();
        if (url == null || url.isBlank()) {
            throw new SQLException("未配置 JDBC 连接地址");
        }
        String driver = ds.getJdbcDriver();
        if (driver != null && !driver.isBlank()) {
            try {
                Class.forName(driver);
            } catch (ClassNotFoundException e) {
                throw new SQLException("后端未集成该数据库的 JDBC 驱动: " + driver);
            }
        }
        // 账号密码 + 注释上报参数一并放进 Properties:
        // 多数驱动默认不通过 DatabaseMetaData 返回表/字段注释(REMARKS),
        // 必须按方言开启对应开关, 否则同步拿到的注释恒为空 → 中文名取不到。
        Properties props = new Properties();
        if (ds.getUsername() != null) props.setProperty("user", ds.getUsername());
        if (ds.getPassword() != null) props.setProperty("password", ds.getPassword());
        applyRemarksProps(url, props);

        DriverManager.setLoginTimeout(LOGIN_TIMEOUT);
        return DriverManager.getConnection(url, props);
    }

    /** 按 JDBC 方言开启"注释上报", 让 DatabaseMetaData 能读到表/字段注释(REMARKS) */
    private static void applyRemarksProps(String url, Properties props) {
        String u = url.toLowerCase();
        if (u.startsWith("jdbc:mysql") || u.startsWith("jdbc:mariadb")) {
            // MySQL/MariaDB: 走 information_schema 才会带回 table/column comment
            props.setProperty("useInformationSchema", "true");
        } else if (u.startsWith("jdbc:oracle") || u.startsWith("jdbc:dm")) {
            // Oracle / 达梦: 开启 REMARKS 上报
            props.setProperty("remarksReporting", "true");
        }
        // PostgreSQL/KingbaseES 默认即返回注释; SQLite 无表注释, 均无需额外参数
    }
}
