import java.sql.*;
import java.io.*;
import java.util.*;

/**
 * 从 SQLite 导出动作类型相关表数据，生成 PostgreSQL INSERT SQL
 * 运行: java -cp "D:/maven/repository/org/xerial/sqlite-jdbc/3.28.0/sqlite-jdbc-3.28.0.jar;." export_action_types.java
 */
public class export_action_types {

    static final String[] TABLES = {
        "ont_class_action",
        "ont_class_action_rule",
        "ont_action_rule_property_mapping",
        "ont_action_rule_condition",
        "ont_action_link_rule_config",
        "ont_action_link_prop_mapping",
        "ont_action_form_section",
        "ont_action_form_param",
        "ont_action_form_param_display",
        "ont_action_form_display_object",
        "ont_action_form_display_string",
        "ont_action_form_display_number",
        "ont_action_form_display_boolean",
        "ont_action_form_override_block",
        "ont_action_form_override_item",
        "ont_action_override_condition_group",
        "ont_action_override_condition_item",
        "ont_action_function_rule_config",
        "ont_action_function_param_mapping",
        "ont_action_function_exception_map",
        "ont_action_notification_rule_config",
        "ont_action_webhook_rule_config",
        "ont_action_webhook_input_mapping",
        "ont_action_submit_standard_config",
        "ont_action_submit_condition_node",
        "ont_action_form_global_config"
    };

    public static void main(String[] args) throws Exception {
        String dbPath = args.length > 0 ? args[0] : "F:/aidata/bontolink.db";
        String outPath = args.length > 1 ? args[1] : "F:/aidata/action_types_pg.sql";

        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);

        PrintWriter out = new PrintWriter(new FileWriter(outPath));
        out.println("-- 动作类型数据迁移 from SQLite to PostgreSQL");
        out.println("-- 生成时间: " + new java.util.Date());
        out.println("SET search_path TO bonto_link_manager;");
        out.println();

        // 先查有哪些表
        DatabaseMetaData meta = conn.getMetaData();
        ResultSet tableRs = meta.getTables(null, null, "%", new String[]{"TABLE"});
        Set<String> existingTables = new HashSet<>();
        while (tableRs.next()) existingTables.add(tableRs.getString("TABLE_NAME").toLowerCase());
        tableRs.close();

        System.out.println("SQLite 中的表: " + existingTables);

        int totalRows = 0;
        for (String table : TABLES) {
            if (!existingTables.contains(table.toLowerCase())) {
                System.out.println("跳过(不存在): " + table);
                continue;
            }
            int rows = exportTable(conn, table, out);
            totalRows += rows;
            System.out.println("导出 " + table + ": " + rows + " 行");
        }

        out.flush();
        out.close();
        conn.close();
        System.out.println("完成，共 " + totalRows + " 行，输出: " + outPath);
    }

    static int exportTable(Connection conn, String table, PrintWriter out) throws SQLException {
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery("SELECT * FROM " + table);
        ResultSetMetaData rsMeta = rs.getMetaData();
        int colCount = rsMeta.getColumnCount();

        // 列名
        List<String> cols = new ArrayList<>();
        for (int i = 1; i <= colCount; i++) cols.add(rsMeta.getColumnName(i));

        out.println("-- " + table);
        out.println("DELETE FROM " + table + " WHERE id IN (SELECT id FROM " + table + ");");

        int rows = 0;
        while (rs.next()) {
            StringBuilder sb = new StringBuilder();
            sb.append("INSERT INTO ").append(table).append(" (");
            sb.append(String.join(", ", cols));
            sb.append(") VALUES (");
            for (int i = 1; i <= colCount; i++) {
                if (i > 1) sb.append(", ");
                Object val = rs.getObject(i);
                if (val == null) {
                    sb.append("NULL");
                } else {
                    String s = val.toString()
                        .replace("'", "''")          // 转义单引号
                        .replace("\\", "\\\\");       // 转义反斜杠
                    sb.append("'").append(s).append("'");
                }
            }
            sb.append(") ON CONFLICT (id) DO UPDATE SET ");
            // 除 id 外所有列都更新
            boolean first = true;
            for (String col : cols) {
                if ("id".equals(col)) continue;
                if (!first) sb.append(", ");
                sb.append(col).append("=EXCLUDED.").append(col);
                first = false;
            }
            sb.append(";");
            out.println(sb);
            rows++;
        }
        out.println();
        rs.close();
        st.close();
        return rows;
    }
}
