package com.beiktech.bontolink.ontology.service;

import com.beiktech.bontolink.data.mapper.OntologyMapper;
import com.beiktech.bontolink.data.mapper.OntologyVersionMapper;
import com.beiktech.bontolink.ontology.config.OntologyEngineConfig;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Ontology 本体模型管理器
 *
 * 方案 A：版本号驱动的模型重建
 * - 每个 ontology 实例独立从 PostgreSQL 构建 OntModel
 * - 通过 ont_ontology_version 表的版本号判断是否需要重建
 * - admin 端改数据后 version+1，各实例检测到变化后自动重建
 * - 重建期间旧模型继续服务，重建完成后再切换
 *
 * 适用场景：单实例或集群部署，读重写轻
 */
@Slf4j
@Service
public class OntologyModelManager {

    private final OntologyVersionMapper versionMapper;
    private final OntologyMapper ontologyMapper;
    private final OntologyEngineConfig config;

    public OntologyModelManager(OntologyVersionMapper versionMapper,
                                 OntologyMapper ontologyMapper,
                                 OntologyEngineConfig config) {
        this.versionMapper = versionMapper;
        this.ontologyMapper = ontologyMapper;
        this.config = config;
    }

    /** 当前活跃的 OntModel（volatile 保证多线程可见性） */
    private volatile OntModel currentModel;

    /** 当前加载的版本号 */
    private volatile int loadedVersion = -1;

    /** 是否正在重建 */
    private volatile boolean rebuilding = false;

    /** 读多写少，用读写锁保护重建过程 */
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /** 重建计数（监控用） */
    private final AtomicInteger rebuildCount = new AtomicInteger(0);

    /** 默认命名空间前缀 */
    private static final String NS_PREFIX = "http://bontolink.beiktech.com/ontology#";

    // ========== 初始化 ==========

    @PostConstruct
    public void init() {
        if (!config.isJenaEnabled()) {
            log.info("Jena OntModel 已禁用 (bontolink.ontology.jena-enabled=false)，跳过初始化");
            return;
        }
        log.info("OntologyModelManager 初始化，storageMode={}, reasonerType={}",
                config.getStorageMode(), config.getReasonerType());
        try {
            rebuildModel();
            log.info("OntologyModelManager 初始化完成，加载版本 {}, 模型大小: {} 个语句",
                    loadedVersion, currentModel != null ? currentModel.size() : 0);
        } catch (Exception e) {
            log.warn("初始化构建 OntModel 失败，将在首次查询时重试: {}", e.getMessage());
        }
    }

    // ========== 公开接口 ==========

    /**
     * 获取当前 OntModel。
     * 如果版本号有更新，自动触发异步重建。
     * 调用方直接使用返回的模型执行查询，无需关心版本管理。
     *
     * @return OntModel，如果 jena-enabled=false 则返回 null
     */
    public OntModel getModel() {
        if (!config.isJenaEnabled()) {
            return null; // Jena 禁用，直接返回 null
        }

        // 检查版本号，落后则触发异步重建
        checkAndRebuildIfNeeded();

        OntModel model = currentModel;
        if (model == null) {
            // 首次或重建失败，同步重建
            rebuildModel();
            model = currentModel;
        }
        return model;
    }

    /**
     * 强制重建 OntModel（admin 端修改本体数据后调用）
     *
     * @return 重建后的语句数，-1 表示失败
     */
    public int rebuild() {
        log.info("收到手动重建请求");
        return rebuildModel();
    }

    /**
     * 获取当前加载的版本号
     */
    public int getLoadedVersion() {
        return loadedVersion;
    }

    /**
     * 获取数据库中的最新版本号
     */
    public int getDbVersion() {
        Integer v = versionMapper.getCurrentVersion();
        return v != null ? v : 0;
    }

    /**
     * 获取重建次数（监控用）
     */
    public int getRebuildCount() {
        return rebuildCount.get();
    }

    /**
     * 获取模型统计信息
     */
    public Map<String, Object> getStats() {
        OntModel model = currentModel;
        return Map.of(
            "loadedVersion", loadedVersion,
            "dbVersion", getDbVersion(),
            "rebuildCount", rebuildCount.get(),
            "statements", model != null ? model.size() : 0,
            "classes", model != null ? countClasses(model) : 0,
            "properties", model != null ? countProperties(model) : 0,
            "rebuilding", rebuilding
        );
    }

    // ========== 版本检查 ==========

    /**
     * 检查版本号，如果数据库版本更新则触发异步重建
     */
    private void checkAndRebuildIfNeeded() {
        if (rebuilding) {
            return; // 正在重建中，不重复触发
        }

        try {
            int dbVersion = getDbVersion();
            if (dbVersion > loadedVersion) {
                log.info("检测到版本变化: {} → {}，触发异步重建", loadedVersion, dbVersion);
                CompletableFuture.runAsync(this::rebuildModel);
            }
        } catch (Exception e) {
            log.warn("版本检查失败: {}", e.getMessage());
        }
    }

    // ========== 模型构建 ==========

    /**
     * 从 PostgreSQL 重建 OntModel
     * 读取 ont_class / ont_class_property / ont_class_link 等表
     * 构建完整的 OWL 本体模型
     */
    private int rebuildModel() {
        if (rebuilding) {
            log.debug("已有重建在进行中，跳过");
            return -1;
        }

        rebuilding = true;
        lock.writeLock().lock();
        try {
            log.info("开始从数据库重建 OntModel...");
            long start = System.currentTimeMillis();

            // 1. 创建 OntModel（根据配置选择推理器类型）
            OntModelSpec spec = createOntModelSpec();
            OntModel newModel = ModelFactory.createOntologyModel(spec);
            newModel.setNsPrefix("bontolink", NS_PREFIX);

            // 2. 加载类定义（OWL Class）
            List<Map<String, Object>> classes = ontologyMapper.listClasses();
            log.info("  加载 {} 个类", classes.size());

            for (Map<String, Object> clazz : classes) {
                String id = (String) clazz.get("id");
                String apiName = (String) clazz.get("api_name");
                String displayName = (String) clazz.get("display_name");
                String parentClassId = (String) clazz.get("parent_class_id");
                String categoryCode = (String) clazz.get("category_code");
                String status = clazz.get("status") != null ? clazz.get("status").toString() : "1";

                if (apiName == null || id == null) continue;

                // 创建 OWL Class
                OntClass ontClass = newModel.createClass(NS_PREFIX + apiName);

                // 添加标注属性
                if (displayName != null) {
                    ontClass.addLabel(displayName, "zh");
                }
                ontClass.addProperty(RDFS.label, apiName);

                // 继承关系（parent_class_id）
                if (parentClassId != null && !parentClassId.isEmpty()) {
                    // 查找父类的 api_name
                    String parentApiName = findApiName(classes, parentClassId);
                    if (parentApiName != null) {
                        OntClass parentClass = newModel.createClass(NS_PREFIX + parentApiName);
                        ontClass.addSuperClass(parentClass);
                    }
                }

                // 行业分类
                if (categoryCode != null) {
                    ontClass.addProperty(newModel.createProperty(NS_PREFIX + "categoryCode"), categoryCode);
                }

                // 状态
                ontClass.addProperty(newModel.createProperty(NS_PREFIX + "status"), status);
            }

            // 3. 加载对象属性（Object Property — 类关系）
            List<Map<String, Object>> links = ontologyMapper.listLinks();
            log.info("  加载 {} 个类关系", links.size());

            for (Map<String, Object> link : links) {
                String apiName = (String) link.get("api_name");
                String sourceId = (String) link.get("source_class_id");
                String targetId = (String) link.get("target_class_id");
                String cardinality = (String) link.get("cardinality");
                String displayName = (String) link.get("display_name");

                if (apiName == null || sourceId == null || targetId == null) continue;

                String sourceApiName = findApiName(classes, sourceId);
                String targetApiName = findApiName(classes, targetId);
                if (sourceApiName == null || targetApiName == null) continue;

                // 创建 Object Property
                OntProperty prop = newModel.createObjectProperty(NS_PREFIX + apiName);

                // domain / range
                OntClass domain = newModel.getOntClass(NS_PREFIX + sourceApiName);
                OntClass range = newModel.getOntClass(NS_PREFIX + targetApiName);
                if (domain != null) prop.addDomain(domain);
                if (range != null) prop.addRange(range);

                if (displayName != null) {
                    prop.addLabel(displayName, "zh");
                }

                // 基数约束
                if ("one_to_one".equals(cardinality)) {
                    prop.convertToFunctionalProperty();
                } else if ("many_to_one".equals(cardinality)) {
                    // 从多到一：target 端 functional
                    prop.convertToFunctionalProperty();
                }
            }

            // 4. 加载数据属性（Data Property — 类属性）
            // 目前从 OntologyMapper 不直接批查所有属性，
            // 先按类逐个加载（数据量不大时 OK，后续可优化）
            int propCount = 0;
            for (Map<String, Object> clazz : classes) {
                String classId = (String) clazz.get("id");
                String classApiName = (String) clazz.get("api_name");
                if (classId == null || classApiName == null) continue;

                List<Map<String, Object>> properties = ontologyMapper.listProperties(classId);
                OntClass ontClass = newModel.getOntClass(NS_PREFIX + classApiName);
                if (ontClass == null) continue;

                for (Map<String, Object> prop : properties) {
                    String propApiName = (String) prop.get("api_name");
                    String dataType = (String) prop.get("data_type");
                    String propDisplayName = (String) prop.get("display_name");
                    String isRequired = prop.get("is_required") != null ? prop.get("is_required").toString() : "0";

                    if (propApiName == null) continue;

                    DatatypeProperty dataProp = newModel.createDatatypeProperty(NS_PREFIX + classApiName + "." + propApiName);
                    dataProp.addDomain(ontClass);

                    if (propDisplayName != null) {
                        dataProp.addLabel(propDisplayName, "zh");
                    }

                    // 必要属性标记为 Functional
                    if ("1".equals(isRequired)) {
                        dataProp.convertToFunctionalProperty();
                    }

                    propCount++;
                }
            }
            log.info("  加载 {} 个数据属性", propCount);

            // 5. 同步版本号并切换模型
            int dbVersion = getDbVersion();
            this.currentModel = newModel;
            this.loadedVersion = dbVersion;
            this.rebuildCount.incrementAndGet();

            long elapsed = System.currentTimeMillis() - start;
            log.info("OntModel 重建完成: {} 个语句, {} 个类, {} 个属性, 耗时 {}ms, 版本 {}",
                    newModel.size(), classes.size(), propCount, elapsed, dbVersion);

            return (int) newModel.size();

        } catch (Exception e) {
            log.error("OntModel 重建失败", e);
            return -1;
        } finally {
            rebuilding = false;
            lock.writeLock().unlock();
        }
    }

    /**
     * 根据配置创建 OntModelSpec
     */
    private OntModelSpec createOntModelSpec() {
        String reasonerType = config.getReasonerType() != null
                ? config.getReasonerType().toUpperCase() : "NONE";

        return switch (reasonerType) {
            case "RDFS" -> OntModelSpec.RDFS_MEM;
            case "OWL" -> OntModelSpec.OWL_MEM;
            case "OWL_MICRO" -> OntModelSpec.OWL_MEM_MICRO_RULE_INF;
            default -> OntModelSpec.OWL_MEM;  // 默认 OWL 内存模型
        };
    }

    // ========== 工具方法 ==========

    /**
     * 在类列表中按 id 查找 api_name
     */
    private String findApiName(List<Map<String, Object>> classes, String id) {
        for (Map<String, Object> c : classes) {
            if (id.equals(c.get("id"))) {
                return (String) c.get("api_name");
            }
        }
        return null;
    }

    /**
     * 统计 OntModel 中的类数
     */
    private long countClasses(OntModel model) {
        return model.listClasses().toSet().size();
    }

    /**
     * 统计 OntModel 中的属性数
     */
    private long countProperties(OntModel model) {
        return model.listAllOntProperties().toSet().size();
    }
}
