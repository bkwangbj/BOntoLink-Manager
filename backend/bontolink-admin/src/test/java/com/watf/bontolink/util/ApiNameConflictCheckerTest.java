package com.watf.bontolink.util;

import org.junit.jupiter.api.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("API_NAME 全局冲突检测")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApiNameConflictCheckerTest {

    private static final String DB_PATH = "bontolink.db";
    private static final String JDBC_URL = "jdbc:sqlite:";
    private static Connection conn;

    @BeforeAll
    static void setup() throws SQLException {
        assumeTrue(Files.exists(Paths.get(DB_PATH)), "数据库文件不存在: " + DB_PATH);
        conn = DriverManager.getConnection(JDBC_URL + DB_PATH);
    }

    @AfterAll
    static void teardown() throws SQLException {
        if (conn != null && !conn.isClosed()) conn.close();
    }

    private record Violation(String table, String apiName, String ids, int count) {}

    private List<Violation> queryInternalDuplicates(String tableName, String apiNameCol,
                                                     String idCol) throws SQLException {
        String sql = String.format(
            "SELECT %s, COUNT(*) AS cnt, GROUP_CONCAT(%s, ', ') AS ids " +
            "FROM %s WHERE %s IS NOT NULL GROUP BY %s HAVING COUNT(*) > 1",
            apiNameCol, idCol, tableName, apiNameCol, apiNameCol
        );
        List<Violation> result = new ArrayList<>();
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(new Violation(tableName, rs.getString(1), rs.getString("ids"), rs.getInt("cnt")));
            }
        }
        return result;
    }

    @Test
    @Order(0)
    @DisplayName("0. API_NAME 全局统计")
    void printStatistics() throws SQLException {
        String[][] tables = {
            {"ont_class", "api_name"},
            {"ont_shared_properties", "prop_code"},
            {"ont_value_types", "api_name"},
            {"ont_enum_types", "api_name"},
            {"ont_enum_items", "api_name"},
            {"ont_interface", "api_name"},
            {"ont_link_types", "link_type_code"},
            {"ont_struct_types", "struct_code"}
        };

        System.out.printf("%-30s %-15s %-15s %-15s%n", "TABLE_NAME", "TOTAL", "UNIQUE", "DUPLICATE");
        System.out.println("-".repeat(80));

        for (String[] table : tables) {
            String sql = String.format(
                "SELECT COUNT(*) AS total, COUNT(DISTINCT %s) AS uniq FROM %s WHERE %s IS NOT NULL",
                table[1], table[0], table[1]
            );
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    int total = rs.getInt("total");
                    int uniq = rs.getInt("uniq");
                    System.out.printf("%-30s %-15d %-15d %-15d%n", table[0], total, uniq, total - uniq);
                }
            }
        }
    }

    @Test
    @Order(1)
    @DisplayName("1-8. 表内 api_name 无重复")
    void noInternalDuplicates() throws SQLException {
        record TableDef(String table, String col, String idCol) {}
        List<TableDef> defs = List.of(
            new TableDef("ont_class",              "api_name",       "id"),
            new TableDef("ont_shared_properties",  "prop_code",      "id"),
            new TableDef("ont_value_types",        "api_name",       "id"),
            new TableDef("ont_enum_types",         "api_name",       "id"),
            new TableDef("ont_enum_items",         "api_name",       "id"),
            new TableDef("ont_interface",          "api_name",       "id"),
            new TableDef("ont_link_types",         "link_type_code", "id"),
            new TableDef("ont_struct_types",       "struct_code",    "id")
        );

        List<Violation> all = new ArrayList<>();
        for (TableDef d : defs) {
            List<Violation> v = queryInternalDuplicates(d.table(), d.col(), d.idCol());
            if (!v.isEmpty()) {
                System.out.printf("[重复] %-30s%n", d.table());
                v.forEach(vv -> System.out.printf("  %-30s cnt=%-4d ids=%s%n", vv.apiName(), vv.count(), vv.ids()));
            }
            all.addAll(v);
        }

        assertTrue(all.isEmpty(), "发现表内 api_name 重复，共 " + all.size() + " 处");
    }

    @Test
    @Order(2)
    @DisplayName("9. 跨表 api_name 无冲突")
    void noCrossTableConflicts() throws SQLException {
        String sql = """
            WITH all_api_names AS (
              SELECT api_name,       'ont_class'             AS src, id FROM ont_class             WHERE api_name       IS NOT NULL
              UNION ALL
              SELECT prop_code,      'ont_shared_properties' AS src, id FROM ont_shared_properties WHERE prop_code      IS NOT NULL
              UNION ALL
              SELECT api_name,       'ont_value_types'       AS src, id FROM ont_value_types       WHERE api_name       IS NOT NULL
              UNION ALL
              SELECT api_name,       'ont_enum_types'        AS src, id FROM ont_enum_types        WHERE api_name       IS NOT NULL
              UNION ALL
              SELECT api_name,       'ont_enum_items'        AS src, id FROM ont_enum_items        WHERE api_name       IS NOT NULL
              UNION ALL
              SELECT api_name,       'ont_interface'         AS src, id FROM ont_interface         WHERE api_name       IS NOT NULL
              UNION ALL
              SELECT link_type_code, 'ont_link_types'        AS src, id FROM ont_link_types        WHERE link_type_code IS NOT NULL
              UNION ALL
              SELECT struct_code,    'ont_struct_types'      AS src, id FROM ont_struct_types      WHERE struct_code    IS NOT NULL
            )
            SELECT api_name, COUNT(*) AS cnt, COUNT(DISTINCT src) AS tables,
                   GROUP_CONCAT(DISTINCT src, ' + ') AS sources
            FROM all_api_names
            GROUP BY api_name HAVING COUNT(*) > 1 OR COUNT(DISTINCT src) > 1
            ORDER BY cnt DESC, api_name
            """;

        List<String> conflicts = new ArrayList<>();
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String line = String.format("%-30s cnt=%-4d tables=%-2d  %s",
                    rs.getString("api_name"), rs.getInt("cnt"),
                    rs.getInt("tables"), rs.getString("sources"));
                System.out.println(line);
                conflicts.add(rs.getString("api_name"));
            }
        }

        assertTrue(conflicts.isEmpty(), "发现跨表 api_name 冲突，共 " + conflicts.size() + " 个");
    }

    @Test
    @Order(3)
    @DisplayName("10. api_name 命名规范（无非法字符）")
    void namingConvention() throws SQLException {
        record TableDef(String table, String col, boolean pascalCase) {}
        List<TableDef> defs = List.of(
            new TableDef("ont_class",             "api_name",  true),
            new TableDef("ont_shared_properties", "prop_code", false),
            new TableDef("ont_value_types",       "api_name",  false),
            new TableDef("ont_enum_types",        "api_name",  false),
            new TableDef("ont_interface",         "api_name",  false)
        );

        List<String> violations = new ArrayList<>();
        for (TableDef d : defs) {
            String sql = String.format("SELECT %s, id FROM %s WHERE %s IS NOT NULL", d.col(), d.table(), d.col());
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    String name = rs.getString(1);
                    boolean valid = d.pascalCase()
                        ? name.matches("[A-Za-z][A-Za-z0-9]*")
                        : name.matches("[a-z0-9_]+");
                    if (!valid) {
                        String msg = String.format("[%s] %s (id=%s)", d.table(), name, rs.getString(2));
                        System.out.println(msg);
                        violations.add(msg);
                    }
                }
            }
        }

        assertTrue(violations.isEmpty(), "发现命名规范违规，共 " + violations.size() + " 处");
    }
}
