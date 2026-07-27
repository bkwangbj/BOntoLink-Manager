# BOntoLink 部署方案

> Jean 独立部署 · 项目模块化 · 未来微服务扩展

---

## 一、当前架构：Jean 独立部署（单体多进程）

**Jean 是一台独立服务器**，运行完整的 BOntoLink 应用栈。所有组件在同一台机器上，但以**两个独立进程**运行，已经为未来拆分做好了准备。

```
┌──────────────────────────────────────────────────────────────┐
│  Jean 服务器                                                  │
│  ┌────────────────────────────────────┐                      │
│  │  Nginx (80)                        │                      │
│  │  ├── /api/            → admin:8088 │                      │
│  │  ├── /api/ontology/   → ont:8089   │                      │
│  │  └── /                → SPA dist   │                      │
│  └────────────────────────────────────┘                      │
│                                                              │
│  ┌────────────────────┐  ┌────────────────────┐              │
│  │  bontolink-admin   │  │  bontolink-ontology│              │
│  │  端口 8088          │  │  端口 8089          │              │
│  │  /bontolink        │  │  /bontolink-ontology│              │
│  │  systemd 服务       │  │  systemd 服务       │              │
│  │  管理后台 CRUD      │  │  Jena 推理/SPARQL  │              │
│  └────────┬───────────┘  └────────┬───────────┘              │
│           │                       │                          │
│           └───────┬───────────────┘                          │
│                   ▼                                          │
│  ┌────────────────────────────────────┐                      │
│  │  PostgreSQL (共享数据库)            │                      │
│  │  两个服务连同一数据库，同一 Schema   │                      │
│  └────────────────────────────────────┘                      │
│                                                              │
│  部署脚本: deploy/deploy-jean.sh                             │
└──────────────────────────────────────────────────────────────┘
```

### 为什么用双进程而不是单进程？

| | 单进程（admin 内含 ontology） | ✅ 双进程（独立部署） |
|---|---|---|
| **风险隔离** | ontology OOM 或死锁会拖垮整个 admin | ontology 崩了不影响 CRUD 操作 |
| **资源分配** | 共享同一堆内存，互相争抢 | 各自 -Xmx，互不干扰 |
| **独立扩缩** | 无法单独扩容 ontology | 可给 ontology 分配更多 CPU/内存 |
| **拆分成本** | 需要从 admin 代码里拆出 ontology | **Nginx 改一行 proxy_pass 即可** |
| **运维粒度** | 要重启一起重启 | 各自独立启停、独立升级 |

### 两个服务的关系

- **代码层面**：admin 的 pom.xml 依赖 ontology 模块的 jar，在 Java 编译期调用 ontology 的 service 类（如同义词扩展）
- **运行时层面**：两个是独立的 Spring Boot 进程，各自启动、各自监听端口、各自管理自己的线程和内存
- **数据库**：共享同一个 PostgreSQL 数据库（同一个 Schema `bonto_link_manager`）
- **调用方式**：admin 通过 HTTP 调用 ontology 的 REST API，或直接调 ontology 的 Java service 类（同进程内）—— 这取决于代码实现

---

## 二、未来扩展：拆成集群/微服务

项目代码已经按模块组织好了，未来扩展只需**配置变更，不动代码**。

### Step 1：把 Ontology 搬到另一台机器

```
当前（Jean 一台）                      未来（两台服务器）
┌──────────────┐                    ┌────────────────┐
│  Jean        │                    │  Jean           │
│  admin 8088  │                    │  admin 8088     │
│  ont   8089  │                    │  (无需改代码)    │
│  Nginx       │                    │  Nginx          │
│  PG          │                    │  PG             │
└──────────────┘                    │  /api/ontology/ │
                                    │  → ont:8089    │   ← 只改这里
                                    └────────┬───────┘
                                             │ 网络
                                             ▼
                                    ┌────────────────┐
                                    │  数据服务器      │
                                    │  ontology 8089  │
                                    │  Jena TDB2      │
                                    │  pgvector       │
                                    └────────────────┘
```

操作步骤：
```bash
# 1. 在新机器上部署 ontology（已有现成脚本）
./deploy/ontology/deploy-ontology.sh install

# 2. 停掉 Jean 上的 ontology
./deploy-jean.sh stop        # 全部停
# 或: ssh jean 'systemctl stop bontolink-ontology'

# 3. 修改 Jean 的 Nginx 配置，把 /api/ontology/ 代理到新机器
#    proxy_pass http://新机器:8089/bontolink-ontology/;

# 4. 重载 Nginx
ssh jean 'nginx -s reload'
```

**零代码修改**，只改 Nginx 一行配置。

### Step 2：水平扩展（集群）

```
                  Nginx 负载均衡
                  │         │
          ┌───────┴─────────┴───────┐
          │                         │
    Jean-1 (admin)           Jean-2 (admin)
          │                         │
          └─────────┬───────────────┘
                    │
               PostgreSQL 主库
                    │
               Ontology 服务（独立）
```

- Admin 无状态，可以水平扩展 N 台
- 共享 PostgreSQL 数据库（Flyway 自动迁移兼容）
- 前端 SPA 可以部署到 CDN 或独立 Nginx

### Step 3：完整微服务

```
API Gateway
├── /api/*          → admin-service （Spring Cloud / K8s Service）
├── /api/ontology/* → ontology-service
├── /api/search/*   → search-service（抽出来独立部署）
└── /api/rag/*      → rag-service（将来加）

每个服务独立数据库 / 独立部署 / 独立扩缩
```

---

## 三、部署脚本分工

| 脚本 | 部署目标 | 部署内容 | 适用场景 |
|---|---|---|---|
| `deploy/deploy-jean.sh` | Jean 一台服务器 | admin + ontology + 前端 + Nginx + PG | **当前阶段：独立部署** |
| `deploy/ontology/deploy-ontology.sh` | 远程数据服务器 | ontology + Jena TDB2 + pgvector | **未来阶段：拆出 ontology** |

两个脚本不冲突——未来把 ontology 拆到远程服务器后，Jean 上只需跑 `deploy-jean.sh`（不含 ontology 部分），远程跑 `deploy-ontology.sh`。

---

## 四、Jean 首次部署步骤

### 4.1 前置条件（Jean 服务器）

```bash
# JDK 21
tar -xzf jdk-21_linux-x64_bin.tar.gz -C /usr/lib/jvm/

# Nginx
yum install -y nginx       # CentOS/RHEL
# apt install -y nginx     # Ubuntu

# PostgreSQL 15+
yum install -y postgresql15-server
/usr/pgsql-15/bin/postgresql-15-setup initdb
systemctl start postgresql
systemctl enable postgresql
```

### 4.2 从开发机一键部署

```bash
# 首次安装
JEAN_HOST=192.168.1.100 DB_PASS=MyPass123 \
  ./deploy/deploy-jean.sh install

# 日常升级
./deploy/deploy-jean.sh upgrade

# 查看状态
./deploy/deploy-jean.sh status

# 查看 ontology 日志
./deploy/deploy-jean.sh logs ontology

# 只查看 admin 日志
./deploy/deploy-jean.sh logs admin
```

### 4.3 验证

```bash
curl http://jean/api/health
# → {"code":0,"msg":"成功","data":"OK"}

curl http://jean/api/ontology/health
# → {"code":0,"msg":"成功","data":"OK"}
```

---

## 五、扩展阅读

| 文档 | 内容 |
|---|---|
| [deploy-jean.sh](deploy-jean.sh) | Jean 独立部署脚本（含 admin + ontology 双进程） |
| [ontology/README.md](ontology/README.md) | Ontology 独立部署文档（数据服务器场景） |
| [ontology/deploy-ontology.sh](ontology/deploy-ontology.sh) | Ontology 远程部署脚本 |
| [../CLAUDE.md](../CLAUDE.md) | 项目开发协作指南 |
