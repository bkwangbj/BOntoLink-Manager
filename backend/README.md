# BOntoLink 后台模块结构说明

## 📁 目录结构

```
backend/
├── pom.xml                           ← 后台聚合 POM
├── bontolink-data/                   ← 数据访问层（JAR）
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/beiktech/bontolink/data/
│       │   ├── config/BontoLinkDataAutoConfiguration.java
│       │   ├── entity/               ← 8 个实体类
│       │   └── mapper/               ← 23 个 MyBatis Mapper
│       └── resources/
│           └── db/migration/         ← Flyway 迁移脚本
│               ├── common/           ← 通用种子数据
│               ├── sqlite/           ← SQLite 方言
│               └── postgresql/       ← PostgreSQL 方言
│
├── bontolink-base/                   ← 基础设施层（JAR）
│   ├── pom.xml
│   └── src/main/
│       └── java/com/beiktech/bontolink/base/
│           └── datasource/           ← 动态数据源管理
│               ├── DataSourceConnector.java
│               ├── DynamicDataSourceRegistry.java
│               ├── DynamicPoolProperties.java
│               └── PoolMetrics.java
│
├── bontolink-admin/                  ← 管理后台服务（Spring Boot 应用）
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/beiktech/bontolink/
│       │   ├── BontoLinkApplication.java
│       │   ├── controller/           ← 28 个 REST 控制器
│       │   ├── service/              ← 13 个业务服务
│       │   ├── common/               ← 通用工具类
│       │   └── config/               ← 配置类
│       └── resources/
│           ├── application.yml       ← 主配置
│           ├── application-sqlite.yml
│           ├── application-postgresql.yml
│           └── seed/                 ← 种子数据（如有）
│
└── bontolink-ontology/               ← 本体服务（Spring Boot 应用）
    ├── pom.xml
    └── src/main/
        ├── java/com/beiktech/bontolink/ontology/
        │   ├── BontoLinkOntologyApplication.java
        │   ├── controller/           ← 本体查询/推理 API（待实现）
        │   ├── service/              ← Jena 集成服务（待实现）
        │   └── model/                ← 数据模型（待实现）
        └── resources/
            └── application.yml       ← 端口 8089
```

## 🎯 模块职责

### 1. bontolink-data（数据访问层）
- **职责**：提供统一的数据访问接口
- **内容**：
  - MyBatis Mapper（23 个）
  - 实体类（8 个）
  - Flyway 数据库迁移脚本
  - 自动配置类（供其他模块引用）
- **被依赖**：bontolink-base、bontolink-admin、bontolink-ontology

### 2. bontolink-base（基础设施层）
- **职责**：提供与本体无关的公共基础设施
- **内容**：
  - 动态数据源管理（HikariCP 连接池）
  - 连接池监控（PoolMetrics）
  - 数据源连接器（DataSourceConnector）
  - （未来可扩展：字典管理、缓存工具等）
- **依赖**：bontolink-data
- **被依赖**：bontolink-admin、bontolink-ontology

### 3. bontolink-admin（管理后台）
- **职责**：本体元数据管理 CRUD
- **端口**：`8088`
- **路径**：`/bontolink`
- **功能**：
  - 类型管理（类/属性/关系）
  - 命名空间管理
  - 分组/分类管理
  - 图标库管理
  - 数据源配置
  - 字典管理

### 4. bontolink-ontology（本体服务）
- **职责**：本体推理、查询、SQL 生成
- **端口**：`8089`
- **路径**：`/bontolink-ontology`
- **功能**（待实现）：
  - 本体信息标准化查询 API
  - OWL 推理（基于 Apache Jena）
  - SPARQL 查询支持
  - 本体驱动的 SQL 生成器
  - 数据查询执行器

## 🔧 开发指南

### 编译所有模块
```bash
cd backend
mvn clean compile
```

### 安装到本地仓库
```bash
cd backend
mvn clean install -DskipTests
```

### 单独运行模块

**管理后台：**
```bash
cd backend/bontolink-admin
mvn spring-boot:run
```
访问：http://localhost:8088/bontolink/api/health

**本体服务：**
```bash
cd backend/bontolink-ontology
mvn spring-boot:run
```
访问：http://localhost:8089/bontolink-ontology/actuator/health

### VS Code 调试配置
`.vscode/launch.json` 已配置两个启动项：
1. **BOntoLink Admin (管理后台)**
2. **BOntoLink Ontology (本体服务)**

## 📦 依赖关系

```
bontolink-data (JAR - 数据访问层)
    ↑
bontolink-base (JAR - 基础设施层)
    ↑
    ├─── bontolink-admin (Spring Boot App, 端口 8088)
    └─── bontolink-ontology (Spring Boot App, 端口 8089)
```

**设计原则**：
- **bontolink-data**：纯数据访问层，不依赖任何业务模块
- **bontolink-base**：基础设施层，依赖 data 层，提供公共基础服务（数据源管理等）
- **bontolink-admin/ontology**：应用层，依赖 base 层，实现具体业务

## ⚠️ 注意事项

1. **Mapper 扫描**：统一由 `bontolink-data` 的 `BontoLinkDataAutoConfiguration` 管理，应用模块不需要重复 `@MapperScan`
2. **Flyway 脚本**：仅在 `bontolink-data` 的 `resources/db/migration/` 维护
3. **包名规范**：
   - 数据层：`com.beiktech.bontolink.data.*`
   - 基础设施层：`com.beiktech.bontolink.base.*`
   - 管理后台：`com.beiktech.bontolink.*`
   - 本体服务：`com.beiktech.bontolink.ontology.*`
4. **端口分配**：
   - Admin: 8088
   - Ontology: 8089

## 🚀 下一步工作

### bontolink-ontology 核心功能实现

1. **Jena 集成**
   - 从数据层构建 OntModel
   - 支持内存/TDB2 存储模式
   - 集成推理器（RDFS/OWL）

2. **API 设计**
   ```
   GET  /api/ontology/classes              ← 查询所有类
   GET  /api/ontology/class/{id}           ← 查询类详情
   POST /api/ontology/reasoning/infer      ← 执行推理
   POST /api/ontology/sql/generate         ← 生成 SQL
   POST /api/ontology/query/execute        ← 执行查询
   ```

3. **SQL 生成器**
   - 根据本体定义生成查询 SQL
   - 处理继承关系和关联
   - 映射物理表字段

---

**最后更新**：2026-07-23  
**版本**：1.0.0
