package com.beiktech.bontolink.common;

import com.beiktech.bontolink.config.DynamicDataSourceRegistry;
import com.beiktech.bontolink.entity.SysDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 数据源连接公共工具：按 sys_data_source 配置动态建立 JDBC 连接。
 * <p><b>推荐使用</b> {@link #open(SysDataSource)} — 从连接池获取连接（用于物理表同步、枚举同步等生产路径）。
 * <p><b>单次测试使用</b> {@link #openDirect(SysDataSource)} — 用 DriverManager 直连（用于连通性测试、监控探测），
 * 不占用池连接。调用方需负责关闭返回的 Connection。</p>
 */
@Component
public class DataSourceConnector {

    /** 默认登录超时(秒)，避免不可达主机长时间阻塞 */
    public static final int LOGIN_TIMEOUT = 8;

    @Autowired
    private DynamicDataSourceRegistry registry;

    /**
     * 从连接池获取连接（推荐用于生产路径：物理表同步、枚举同步等持续性数据操作）。
     * <p>URL/用户名/密码取自身份配置，池参数取自统一配置 + 每数据源覆盖。</p>
     *
     * @throws SQLException 数据源为空 / 非 JDBC 类型 / 连接失败
     */
    public Connection open(SysDataSource ds) throws SQLException {
        if (ds == null) throw new SQLException("数据源不存在");
        if ("mongodb".equalsIgnoreCase(ds.getDsType())) {
            throw new SQLException("MongoDB 暂不支持 JDBC 连接");
        }
        try {
            return registry.getOrCreate(ds.getId()).getConnection();
        } catch (Exception e) {
            throw new SQLException("从连接池获取连接失败: " + e.getMessage(), e);
        }
    }

    /**
     * 使用 DriverManager 直连（一次性测试/探测用），不占用池连接。
     * <p>保留旧行为，调用方负责 try-with-resources 关闭。</p>
     *
     * @throws SQLException 数据源为空 / 非 JDBC 类型 / 未配置连接地址 / 驱动未集成 / 连接失败
     */
    public Connection openDirect(SysDataSource ds) throws SQLException {
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
        // 账号密码 + 注释上报参数一并放进 Properties
        Properties props = new Properties();
        if (ds.getUsername() != null) props.setProperty("user", ds.getUsername());
        if (ds.getPassword() != null) props.setProperty("password", ds.getPassword());
        remarksProps(url).forEach(props::setProperty);

        DriverManager.setLoginTimeout(LOGIN_TIMEOUT);
        return DriverManager.getConnection(url, props);
    }

    /**
     * 按 JDBC 方言返回"注释上报"所需的连接属性，让 DatabaseMetaData 能读到表/字段注释(REMARKS)。
     * <p>MySQL/MariaDB 需 useInformationSchema=true；Oracle/达梦 需 remarksReporting=true；
     * PostgreSQL/KingbaseES 默认即返回注释，SQLite 无表注释，均无需额外参数。</p>
     * <p>供连接池({@link com.beiktech.bontolink.config.DynamicDataSourceRegistry})与直连共用，
     * 否则物理表同步走连接池时读不到注释，中文名只能退回物理表名。</p>
     */
    public static Map<String, String> remarksProps(String url) {
        Map<String, String> m = new HashMap<>();
        if (url == null) return m;
        String u = url.toLowerCase();
        if (u.startsWith("jdbc:mysql") || u.startsWith("jdbc:mariadb")) {
            m.put("useInformationSchema", "true");
        } else if (u.startsWith("jdbc:oracle") || u.startsWith("jdbc:dm")) {
            m.put("remarksReporting", "true");
        }
        return m;
    }
}
