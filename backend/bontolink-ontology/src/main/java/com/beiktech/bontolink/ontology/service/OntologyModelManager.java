package com.beiktech.bontolink.ontology.service;

import com.beiktech.bontolink.data.mapper.BizNamespaceMapper;
import com.beiktech.bontolink.data.mapper.OntologyMapper;
import com.beiktech.bontolink.data.mapper.OntologyVersionMapper;
import com.beiktech.bontolink.data.entity.BizNamespace;
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
    private final BizNamespaceMapper namespaceMapper;

    public OntologyModelManager(OntologyVersionMapper versionMapper,
                                 OntologyMapper ontologyMapper,
                                 OntologyEngineConfig config,
                                 BizNamespaceMapper namespaceMapper) {
        this.versionMapper = versionMapper;
        this.ontologyMapper = ontologyMapper;
        this.config = config;
        this.namespaceMapper = namespaceMapper;
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

    /** 版本号缓存：用于在单次请求期间避免重复查询数据库 */
    private volatile int cachedDbVersion = -1;
    private volatile long cachedVersionTimestamp = 0;
    /** 版本号缓存有效期（毫秒），默认 5 秒 */
    private static final long VERSION_CACHE_TTL_MS = 5000;

    /** 默认命名空间前缀 */
    public static final String NS_PREFIX = "http://bontolink.beiktech.com/ontology#";

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
     * 获取数据库中的最新版本号（带缓存）
     * 缓存有效期 5 秒，避免在单次请求中重复查询数据库
     */
    public int getDbVersion() {
        long now = System.currentTimeMillis();
        // 如果缓存有效，直接返回
        if (cachedDbVersion != -1 && (now - cachedVersionTimestamp) < VERSION_CACHE_TTL_MS) {
            return cachedDbVersion;
        }

        // 缓存失效，查询数据库并更新缓存
        Integer v = versionMapper.getCurrentVersion();
        cachedDbVersion = v != null ? v : 0;
        cachedVersionTimestamp = now;
        return cachedDbVersion;
    }

    /**
     * 清除版本号缓存（重建后调用）
     */
    private void clearVersionCache() {
        cachedDbVersion = -1;
        cachedVersionTimestamp = 0;
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

            // 1b. 加载命名空间 URI 映射（ns_code → ns_uri）
            java.util.Map<String, String> nsUriMap = new java.util.HashMap<>();
            try {
                List<BizNamespace> namespaces = namespaceMapper.listAll();
                for (BizNamespace ns : namespaces) {
                    if (ns.getNsCode() != null && ns.getNsUri() != null) {
                        String uri = ns.getNsUri();
                        if (!uri.endsWith("#") && !uri.endsWith("/")) uri = uri + "#";
                        nsUriMap.put(ns.getNsCode(), uri);
                        newModel.setNsPrefix(ns.getNsCode(), uri);
                    }
                }
                log.info("  加载 {} 个命名空间", nsUriMap.size());
            } catch (Exception e) {
                log.warn("  加载命名空间失败，使用默认前缀: {}", e.getMessage());
            }

            // 2. 加载类定义（OWL Class）
            List<Map<String, Object>> classes = ontologyMapper.listClasses();
            log.info("  加载 {} 个类", classes.size());

            for (Map<String, Object> clazz : classes) {
                String id = (String) clazz.get("id");
                String apiName = (String) clazz.get("api_name");
                String displayName = (String) clazz.get("display_name");
                String nsCode = (String) clazz.get("ns_code");
                String parentClassId = (String) clazz.get("parent_class_id");
                String categoryCode = (String) clazz.get("category_code");
                String status = clazz.get("status") != null ? clazz.get("status").toString() : "1";

                if (apiName == null || id == null) continue;

                // 根据命名空间决定 URI 前缀
                String classNs = (nsCode != null && nsUriMap.containsKey(nsCode))
                        ? nsUriMap.get(nsCode) : NS_PREFIX;
                String classUri = classNs + apiName;

                // 创建 OWL Class
                OntClass ontClass = newModel.createClass(classUri);

                // 添加标注属性
                if (displayName != null) {
                    ontClass.addLabel(displayName, "zh");
                }
                ontClass.addProperty(RDFS.label, apiName);
                if (nsCode != null) {
                    ontClass.addProperty(newModel.createProperty(NS_PREFIX + "nsCode"), nsCode);
                }

                // 继承关系（parent_class_id）
                if (parentClassId != null && !parentClassId.isEmpty()) {
                    String parentApiName = findApiName(classes, parentClassId);
                    if (parentApiName != null) {
                        String parentNsCode = findNsCode(classes, parentClassId);
                        String parentNs = (parentNsCode != null && nsUriMap.containsKey(parentNsCode))
                                ? nsUriMap.get(parentNsCode) : NS_PREFIX;
                        OntClass parentClass = newModel.createClass(parentNs + parentApiName);
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

                String sourceNs = nsUriMap.getOrDefault(findNsCode(classes, sourceId), NS_PREFIX);
                String targetNs = nsUriMap.getOrDefault(findNsCode(classes, targetId), NS_PREFIX);

                // 创建 Object Property（用 source 类的命名空间）
                OntProperty prop = newModel.createObjectProperty(sourceNs + apiName);

                // domain / range
                OntClass domain = newModel.getOntClass(sourceNs + sourceApiName);
                OntClass range = newModel.getOntClass(targetNs + targetApiName);
                if (domain != null) prop.addDomain(domain);
                if (range != null) prop.addRange(range);

                if (displayName != null) {
                    prop.addLabel(displayName, "zh");
                }

                // 基数约束
                if ("one_to_one".equals(cardinality)) {
                    prop.convertToFunctionalProperty();
                } else if ("many_to_one".equals(cardinality)) {
                    prop.convertToFunctionalProperty();
                }
            }

            // 4. 先加载枚举类型（Data Property 加 range 时需要枚举 OntClass 已存在）
            List<Map<String, Object>> enumTypes = ontologyMapper.listEnumTypes();
            log.info("  加载 {} 个枚举类型", enumTypes.size());

            for (Map<String, Object> enumType : enumTypes) {
                String enumApiName = (String) enumType.get("api_name");
                String enumLabel = (String) enumType.get("rdfs_label");
                String enumComment = (String) enumType.get("rdfs_comment");
                String enumTypeKind = (String) enumType.get("enum_type");

                if (enumApiName == null) continue;

                OntClass enumClass = newModel.createClass(NS_PREFIX + "Enum_" + enumApiName);
                if (enumLabel != null) enumClass.addLabel(enumLabel, "zh");
                if (enumComment != null) enumClass.addComment(enumComment, "zh");
                enumClass.addProperty(newModel.createProperty(NS_PREFIX + "enumType"), enumTypeKind);

                String enumId = (String) enumType.get("id");
                List<Map<String, Object>> enumItems = ontologyMapper.listEnumItems(enumId);
                for (Map<String, Object> item : enumItems) {
                    String itemCode = (String) item.get("code");
                    String itemLabel = (String) item.get("label");
                    String itemApiName = (String) item.get("api_name");
                    if (itemCode == null) continue;

                    org.apache.jena.ontology.Individual individual = newModel.createIndividual(
                            NS_PREFIX + "Enum_" + enumApiName + "_" + itemCode, enumClass);
                    if (itemLabel != null) individual.addLabel(itemLabel, "zh");
                    individual.addProperty(newModel.createProperty(NS_PREFIX + "code"), itemCode);
                    if (itemApiName != null)
                        individual.addProperty(newModel.createProperty(NS_PREFIX + "apiName"), itemApiName);
                }
            }

            // 5. 加载数据属性（Data Property — 类属性），枚举约束的加 rdfs:range
            int propCount = 0;
            for (Map<String, Object> clazz : classes) {
                String classId = (String) clazz.get("id");
                String classApiName = (String) clazz.get("api_name");
                String classNsCode  = (String) clazz.get("ns_code");
                if (classId == null || classApiName == null) continue;

                String classNs = nsUriMap.getOrDefault(classNsCode, NS_PREFIX);
                List<Map<String, Object>> properties = ontologyMapper.listProperties(classId);
                OntClass ontClass = newModel.getOntClass(classNs + classApiName);
                if (ontClass == null) continue;

                for (Map<String, Object> prop : properties) {
                    String propApiName = (String) prop.get("api_name");
                    String propDisplayName = (String) prop.get("display_name");
                    String isRequired = prop.get("is_required") != null ? prop.get("is_required").toString() : "0";
                    String enumApiName = (String) prop.get("enum_api_name");
                    String vtConstraintType = (String) prop.get("vt_constraint_type");
                    String dataType = (String) prop.get("data_type");

                    if (propApiName == null) continue;

                    // 数据属性 URI 用类的命名空间前缀
                    DatatypeProperty dataProp = newModel.createDatatypeProperty(classNs + classApiName + "." + propApiName);
                    dataProp.addDomain(ontClass);

                    if (propDisplayName != null) dataProp.addLabel(propDisplayName, "zh");

                    // 存储 data_type（XSD 数据类型）
                    if (dataType != null) {
                        dataProp.addProperty(newModel.createProperty(NS_PREFIX + "dataType"), dataType);
                    }

                    // 枚举约束：加 rdfs:range 指向枚举类
                    if ("Enum".equals(vtConstraintType) && enumApiName != null) {
                        OntClass enumClass = newModel.getOntClass(NS_PREFIX + "Enum_" + enumApiName);
                        if (enumClass != null) dataProp.addRange(enumClass);
                    }

                    if ("1".equals(isRequired)) dataProp.convertToFunctionalProperty();

                    propCount++;
                }
            }
            log.info("  加载 {} 个数据属性", propCount);

            // 6. 加载值类型（Value Type）
            List<Map<String, Object>> valueTypes = ontologyMapper.listValueTypes();
            log.info("  加载 {} 个值类型", valueTypes.size());

            for (Map<String, Object> valueType : valueTypes) {
                String vtApiName = (String) valueType.get("api_name");
                String vtLabel = (String) valueType.get("rdfs_label");
                String vtComment = (String) valueType.get("rdfs_comment");
                String baseType = (String) valueType.get("base_type");
                String constraintType = (String) valueType.get("constraint_type");
                String enumId = (String) valueType.get("enum_id");
                String enumApiName = (String) valueType.get("enum_api_name");

                if (vtApiName == null) continue;

                // 创建值类型为 Datatype
                org.apache.jena.ontology.OntResource vtResource = newModel.createOntResource(NS_PREFIX + "ValueType_" + vtApiName);
                if (vtLabel != null) {
                    vtResource.addLabel(vtLabel, "zh");
                }
                if (vtComment != null) {
                    vtResource.addComment(vtComment, "zh");
                }

                // 添加值类型属性
                vtResource.addProperty(newModel.createProperty(NS_PREFIX + "baseType"), baseType);
                vtResource.addProperty(newModel.createProperty(NS_PREFIX + "constraintType"), constraintType);

                // 如果关联了枚举，建立连接
                if (enumId != null && enumApiName != null) {
                    OntClass enumClass = newModel.getOntClass(NS_PREFIX + "Enum_" + enumApiName);
                    if (enumClass != null) {
                        vtResource.addProperty(newModel.createProperty(NS_PREFIX + "refersToEnum"), enumClass);
                    }
                }
            }

            // 7. 加载链接类型（ont_link_types）→ 有方向的 OWL ObjectProperty
            // 链接类型的左右两侧各自代表一个方向的关系：
            //   l_api_name：以左端实体为主语（domain=左类, range=右类）
            //   r_api_name：以右端实体为主语（domain=右类, range=左类）
            // 两者在语义上不一定互为逆属性（主语视角不同, 不能无条件互换）
            List<Map<String, Object>> linkTypes = ontologyMapper.listLinkTypes();
            log.info("  加载 {} 个链接类型", linkTypes.size());

            org.apache.jena.rdf.model.Property propSubjectSide   = newModel.createProperty(NS_PREFIX + "subjectSide");
            org.apache.jena.rdf.model.Property propLinkTypeId    = newModel.createProperty(NS_PREFIX + "linkTypeId");
            org.apache.jena.rdf.model.Property propPerspectiveOf = newModel.createProperty(NS_PREFIX + "perspectiveOf");

            int linkTypePropCount = 0;
            for (Map<String, Object> lt : linkTypes) {
                String ltId        = (String) lt.get("link_type_id");
                String rdfsComment = (String) lt.get("rdfs_comment");
                String rdfsLabel   = (String) lt.get("rdfs_label");

                String lClassApi   = (String) lt.get("l_class_api");
                String lNsCode     = (String) lt.get("l_ns_code");
                String rClassApi   = (String) lt.get("r_class_api");
                String rNsCode     = (String) lt.get("r_ns_code");

                if (lClassApi == null || rClassApi == null) continue;

                String lNs = nsUriMap.getOrDefault(lNsCode, NS_PREFIX);
                String rNs = nsUriMap.getOrDefault(rNsCode, NS_PREFIX);

                OntClass lClass = newModel.getOntClass(lNs + lClassApi);
                OntClass rClass = newModel.getOntClass(rNs + rClassApi);

                String lApiName  = (String) lt.get("l_api_name");
                String lDisplay  = (String) lt.get("l_display_name");
                Object lEnabled  = lt.get("l_enabled");
                String lCard     = (String) lt.get("l_cardinality");

                String rApiName  = (String) lt.get("r_api_name");
                String rDisplay  = (String) lt.get("r_display_name");
                Object rEnabled  = lt.get("r_enabled");
                String rCard     = (String) lt.get("r_cardinality");

                // 以左端实体为主语的 ObjectProperty
                OntProperty lProp = null;
                if (lApiName != null && !"0".equals(String.valueOf(lEnabled))) {
                    lProp = newModel.createObjectProperty(lNs + lApiName);
                    if (lClass != null) lProp.addDomain(lClass);
                    if (rClass != null) lProp.addRange(rClass);
                    if (lDisplay != null) lProp.addLabel(lDisplay, "zh");
                    if (rdfsLabel != null) lProp.addLabel(rdfsLabel, "en");
                    String cmt = "以左端实体（" + lClassApi + "）为主语的关系："
                            + (lDisplay != null ? lDisplay : lApiName)
                            + (rdfsComment != null ? "。" + rdfsComment : "");
                    lProp.addComment(cmt, "zh");
                    lProp.addProperty(propSubjectSide, "left");
                    if (ltId != null) lProp.addProperty(propLinkTypeId, ltId);
                    if ("one".equals(lCard)) lProp.convertToFunctionalProperty();
                    linkTypePropCount++;
                }

                // 以右端实体为主语的 ObjectProperty
                OntProperty rProp = null;
                if (rApiName != null && !"0".equals(String.valueOf(rEnabled))) {
                    rProp = newModel.createObjectProperty(rNs + rApiName);
                    if (rClass != null) rProp.addDomain(rClass);
                    if (lClass != null) rProp.addRange(lClass);
                    if (rDisplay != null) rProp.addLabel(rDisplay, "zh");
                    if (rdfsLabel != null) rProp.addLabel(rdfsLabel, "en");
                    String cmt = "以右端实体（" + rClassApi + "）为主语的关系："
                            + (rDisplay != null ? rDisplay : rApiName)
                            + (rdfsComment != null ? "。" + rdfsComment : "");
                    rProp.addComment(cmt, "zh");
                    rProp.addProperty(propSubjectSide, "right");
                    if (ltId != null) rProp.addProperty(propLinkTypeId, ltId);
                    if ("one".equals(rCard)) rProp.convertToFunctionalProperty();
                    linkTypePropCount++;
                }

                // 双侧均有时互相标注 perspectiveOf（记录关联关系, 非 owl:inverseOf）
                if (lProp != null && rProp != null) {
                    lProp.addProperty(propPerspectiveOf, rProp);
                    rProp.addProperty(propPerspectiveOf, lProp);
                }
            }
            log.info("  加载链接类型 ObjectProperty {} 个", linkTypePropCount);

            // 8. 加载物理表和数据源信息（ont_class_ds JOIN sys_data_source）
            List<Map<String, Object>> allClassDs = ontologyMapper.listAllClassDatasources();
            log.info("  加载 {} 条物理表绑定", allClassDs.size());

            org.apache.jena.rdf.model.Property propHasTable   = newModel.createProperty(NS_PREFIX + "hasPhysicalTable");
            org.apache.jena.rdf.model.Property propDsCode     = newModel.createProperty(NS_PREFIX + "dsCode");
            org.apache.jena.rdf.model.Property propDsName     = newModel.createProperty(NS_PREFIX + "dsName");
            org.apache.jena.rdf.model.Property propDsType     = newModel.createProperty(NS_PREFIX + "dsType");
            org.apache.jena.rdf.model.Property propTableName  = newModel.createProperty(NS_PREFIX + "physicalTable");
            org.apache.jena.rdf.model.Property propTableLabel = newModel.createProperty(NS_PREFIX + "tableLabel");
            org.apache.jena.rdf.model.Property propRelType    = newModel.createProperty(NS_PREFIX + "relType");
            org.apache.jena.rdf.model.Property propPkKeys     = newModel.createProperty(NS_PREFIX + "pkKeys");
            org.apache.jena.rdf.model.Property propJoinOn     = newModel.createProperty(NS_PREFIX + "joinOnKeys");

            // 先按 class_id 分组（避免重复查类）
            java.util.Map<String, List<Map<String, Object>>> dsByClass = new java.util.LinkedHashMap<>();
            for (Map<String, Object> row : allClassDs) {
                String classId = (String) row.get("class_id");
                if (classId == null) continue;
                dsByClass.computeIfAbsent(classId, k -> new java.util.ArrayList<>()).add(row);
            }

            // 为每个类找到对应 OntClass，附加物理表节点
            for (Map<String, Object> clazz : classes) {
                String classId      = (String) clazz.get("id");
                String classApiName = (String) clazz.get("api_name");
                String classNsCode  = (String) clazz.get("ns_code");
                if (classId == null || classApiName == null) continue;

                List<Map<String, Object>> dsList = dsByClass.get(classId);
                if (dsList == null || dsList.isEmpty()) continue;

                String classNs = nsUriMap.getOrDefault(classNsCode, NS_PREFIX);
                OntClass ontClass = newModel.getOntClass(classNs + classApiName);
                if (ontClass == null) continue;

                for (Map<String, Object> ds : dsList) {
                    String physicalTable = (String) ds.get("physical_table");
                    String dsCode        = (String) ds.get("ds_code");
                    if (physicalTable == null || dsCode == null) continue;

                    // 每张物理表建一个空白节点，挂到类上
                    org.apache.jena.rdf.model.Resource tableNode = newModel.createResource(
                            NS_PREFIX + "Table_" + dsCode + "_" + physicalTable);

                    tableNode.addProperty(propTableName,  physicalTable);
                    tableNode.addProperty(propDsCode,     dsCode);

                    String tableLabel = (String) ds.get("table_label");
                    if (tableLabel != null) tableNode.addProperty(propTableLabel, tableLabel);

                    String dsName = (String) ds.get("ds_name");
                    if (dsName != null) tableNode.addProperty(propDsName, dsName);

                    String dsType = (String) ds.get("ds_type");
                    if (dsType != null) tableNode.addProperty(propDsType, dsType);

                    String relType = (String) ds.get("rel_type");
                    if (relType != null) tableNode.addProperty(propRelType, relType);

                    String pkKeys = (String) ds.get("pk_keys");
                    if (pkKeys != null) tableNode.addProperty(propPkKeys, pkKeys);

                    String joinOnKeys = (String) ds.get("join_on_keys");
                    if (joinOnKeys != null) tableNode.addProperty(propJoinOn, joinOnKeys);

                    ontClass.addProperty(propHasTable, tableNode);
                }
            }

            // 10. 同步版本号并切换模型
            clearVersionCache();
            int dbVersion = getDbVersion();
            this.currentModel = newModel;
            this.loadedVersion = dbVersion;
            this.rebuildCount.incrementAndGet();

            long elapsed = System.currentTimeMillis() - start;
            log.info("OntModel 重建完成: {} 个语句, {} 个类, {} 个枚举类型, {} 个值类型, {} 条物理表绑定, 耗时 {}ms, 版本 {}",
                    newModel.size(), classes.size(), enumTypes.size(), valueTypes.size(), allClassDs.size(), elapsed, dbVersion);

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
     * 在类列表中按 id 查找 ns_code
     */
    private String findNsCode(List<Map<String, Object>> classes, String id) {
        for (Map<String, Object> c : classes) {
            if (id.equals(c.get("id"))) {
                return (String) c.get("ns_code");
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
