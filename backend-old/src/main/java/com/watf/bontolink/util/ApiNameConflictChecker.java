package com.watf.bontolink.util;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;

/**
 * API_NAME 全局冲突检测工具（Java版本）
 * 用途：检测数据库中所有 api_name 字段的重复和冲突
 * 使用：java -cp "backend/target/classes:backend/lib/*" com.watf.bontolink.util.ApiNameConflictChecker
 */
public class ApiNameConflictChecker {

    private static final String DEFAULT_DB_PATH = "c:/beiktech-jyx/BOntoLink02/backend/bontolink.db";
    private static final String JDBC_URL = "jdbc:sqlite:";

    public static void main(String[] args) {
        String dbPath = args.length > 0 ? args[0] : DEFAULT_DB_PATH;

        if (!Files.exists(Paths.get(dbPath))) {
            System.err.println("[错误] 数据库文件不存在: " + dbPath);
            System.exit(1);
        }

        System.out.println("==============================================");
        System.out.println("  BOntoLink API_NAME 冲突检测工具 (Java)");
        System.out.println("==============================================\n");
        System.out.println("[信息] 数据库路径: " + dbPath);
        System.out.println("[执行] 开始检测...\n");

        try (Connection conn = DriverManager.getConnection(JDBC_URL + dbPath)) {
            // 0. 统计信息
            printStatistics(conn);

            // 1-8. 表内重复检测
            checkTableInternal(conn, "ont_class", "api_name", "id", "display_name");
            checkTableInternal(conn, "ont_shared_properties", "prop_code", "id", "rdfs_label");
            checkTableInternal(conn, "ont_value_types", "api_name", "id", "rdfs_label");
            checkTableInternal(conn, "ont_enum_types", "api_name", "id", "rdfs_label");
            checkTableInternal(conn, "ont_enum_items", "api_name", "id", "label");
            checkTableInternal(conn, "ont_interface", "api_name", "id", "display_name");
            checkTableInternal(conn, "ont_link_types", "link_type_code", "id", "link_name");
            checkTableInternal(conn, "ont_struct_types", "struct_code", "id", "rdfs_label");

            // 9. 跨表冲突检测（核心）
            checkCrossTableConflicts(conn);

            // 10. 命名规范检查
            checkNamingConvention(conn);

            System.out.println("\n==============================================");
            System.out.println("[完成] 检测完成");
            System.out.println("==============================================\n");

        } catch (SQLException e) {
            System.err.println("[错误] 数据库操作失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * 0. 打印统计信息
     */
    private static void printStatistics(Connection conn) throws SQLException {
        System.out.println("============ API_NAME 全局统计 ============");
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

        System.out.printf("%-30s %-15s %-15s %-15s%n", "TABLE_NAME", "TOTAL_COUNT", "UNIQUE_COUNT", "DUPLICATE_COUNT");
        System.out.println("-----------------------------------------------------------------------------------");

        for (String[] table : tables) {
            String sql = String.format(
                    "SELECT COUNT(*) AS total, COUNT(DISTINCT %s) AS unique FROM %s WHERE %s IS NOT NULL",
                    table[1], table[0], table[1]
            );
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    int total = rs.getInt("total");
                    int unique = rs.getInt("unique");
                    int duplicate = total - unique;
                    System.out.printf("%-30s %-15d %-15d %-15d%n", table[0], total, unique, duplicate);
                }
            }
        }
        System.out.println();
    }

    /**
     * 1-8. 表内重复检测
     */
    private static void checkTableInternal(Connection conn, String tableName, String apiNameColumn,
                                            String idColumn, String labelColumn) throws SQLException {
        System.out.printf("============ %s 内部重复 ============%n", tableName);

        String sql = String.format(
                "SELECT %s, COUNT(*) AS cnt, GROUP_CONCAT(%s, ', ') AS ids " +
                        "FROM %s WHERE %s IS NOT NULL " +
                        "GROUP BY %s HAVING COUNT(*) > 1",
                apiNameColumn, idColumn, tableName, apiNameColumn, apiNameColumn
        );

        boolean found = false;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                if (!found) {
                    System.out.printf("%-30s %-10s %-50s%n", "API_NAME", "COUNT", "IDS");
                    System.out.println("------------------------------------------------------------------------------");
                    found = true;
                }
                System.out.printf("%-30s %-10d %-50s%n",
                        rs.getString(1),
                        rs.getInt("cnt"),
                        rs.getString("ids")
                );
            }
        }

        if (!found) {
            System.out.println("(无重复)");
        }
        System.out.println();
    }

    /**
     * 9. 跨表全局冲突检测
     */
    private static void checkCrossTableConflicts(Connection conn) throws SQLException {
        System.out.println("============ 跨表全局冲突检测（核心）============");

        String sql = """
            WITH all_api_names AS (
              SELECT api_name, 'ont_class' AS source, id FROM ont_class WHERE api_name IS NOT NULL
              UNION ALL
              SELECT prop_code, 'ont_shared_properties', id FROM ont_shared_properties WHERE prop_code IS NOT NULL
              UNION ALL
              SELECT api_name, 'ont_value_types', id FROM ont_value_types WHERE api_name IS NOT NULL
              UNION ALL
              SELECT api_name, 'ont_enum_types', id FROM ont_enum_types WHERE api_name IS NOT NULL
              UNION ALL
              SELECT api_name, 'ont_enum_items', id FROM ont_enum_items WHERE api_name IS NOT NULL
              UNION ALL
              SELECT api_name, 'ont_interface', id FROM ont_interface WHERE api_name IS NOT NULL
              UNION ALL
              SELECT link_type_code, 'ont_link_types', id FROM ont_link_types WHERE link_type_code IS NOT NULL
              UNION ALL
              SELECT struct_code, 'ont_struct_types', id FROM ont_struct_types WHERE struct_code IS NOT NULL
            )
            SELECT
              api_name,
              COUNT(*) AS conflict_count,
              COUNT(DISTINCT source) AS affected_tables,
              GROUP_CONCAT(DISTINCT source, ' + ') AS conflict_sources
            FROM all_api_names
            GROUP BY api_name
            HAVING COUNT(*) > 1 OR COUNT(DISTINCT source) > 1
            ORDER BY conflict_count DESC, api_name
        """;

        boolean found = false;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                if (!found) {
                    System.out.printf("%-30s %-15s %-20s %-60s%n",
                            "API_NAME", "CONFLICT_COUNT", "AFFECTED_TABLES", "CONFLICT_SOURCES");
                    System.out.println("------------------------------------------------------------------------------------------------------------------------------------");
                    found = true;
                }
                System.out.printf("%-30s %-15d %-20d %-60s%n",
                        rs.getString("api_name"),
                        rs.getInt("conflict_count"),
                        rs.getInt("affected_tables"),
                        rs.getString("conflict_sources")
                );
            }
        }

        if (!found) {
            System.out.println("(无冲突) ✓");
        }
        System.out.println();
    }

    /**
     * 10. 命名规范检查（非法字符）
     */
    private static void checkNamingConvention(Connection conn) throws SQLException {
        System.out.println("============ 命名规范检查（非法字符）============");

        String[][] tables = {
                {"ont_class", "api_name"},
                {"ont_shared_properties", "prop_code"},
                {"ont_value_types", "api_name"},
                {"ont_enum_types", "api_name"},
                {"ont_interface", "api_name"}
        };

        boolean found = false;
        for (String[] table : tables) {
            // SQLite 不支持正则，用 Java 过滤
            String sql = String.format("SELECT %s, id FROM %s WHERE %s IS NOT NULL", table[1], table[0], table[1]);

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    String apiName = rs.getString(1);
                    String id = rs.getString(2);

                    // 检查是否只包含 [a-z0-9_]（允许 PascalCase 的对象类型除外）
                    boolean isValid = table[0].equals("ont_class") ?
                            apiName.matches("[A-Za-z][A-Za-z0-9]*") :  // 对象类型允许 PascalCase
                            apiName.matches("[a-z0-9_]+");              // 其他必须 snake_case

                    if (!isValid) {
                        if (!found) {
                            System.out.printf("%-30s %-30s %-50s %-50s%n",
                                    "TABLE", "API_NAME", "ID", "ISSUE");
                            System.out.println("-----------------------------------------------------------------------------------------------------------------------------------------------------");
                            found = true;
                        }
                        String issue = "";
                        if (apiName.matches(".*[A-Z].*") && !table[0].equals("ont_class")) {
                            issue = "包含大写字母（应为 snake_case）";
                        } else if (apiName.matches(".*[^a-zA-Z0-9_].*")) {
                            issue = "包含非法字符（仅允许 a-z, 0-9, _）";
                        } else if (apiName.contains(" ")) {
                            issue = "包含空格";
                        }
                        System.out.printf("%-30s %-30s %-50s %-50s%n",
                                table[0], apiName, id, issue);
                    }
                }
            }
        }

        if (!found) {
            System.out.println("(全部符合规范) ✓");
        }
        System.out.println();
    }
}
