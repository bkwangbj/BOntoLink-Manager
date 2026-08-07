package com.beiktech.bontolink.tool.jena;

import com.beiktech.bontolink.ontology.config.OntologyEngineConfig;
import com.beiktech.bontolink.ontology.service.OntologyModelManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 工具模块 - Jena 本体查询服务
 *
 * 基于 ontology 模块的 {@link OntologyModelManager} 管理 OntModel 生命周期，
 * 提供 SPARQL 查询、OWL 类/属性列举、类详情查看等只读能力。
 *
 * 设计要点：
 * - 不做关键词全文检索（参考架构设计文档，Jena 只做推理/精确 URI 查询），
 *   关键词检索请走数据库模块或向量模块。
 * - 模型未就绪（jena-enabled=false 或仍在重建）时返回明确错误。
 */
@Slf4j
@Service
public class JenaToolService {

    /** OntModel 命名空间前缀（与 OntologyModelManager 保持一致） */
    private static final String NS_PREFIX = "http://bontolink.beiktech.com/ontology#";

    private final OntologyModelManager modelManager;
    private final OntologyEngineConfig engineConfig;

    public JenaToolService(OntologyModelManager modelManager, OntologyEngineConfig engineConfig) {
        this.modelManager = modelManager;
        this.engineConfig = engineConfig;
    }

    /**
     * 模块状态：Jena 是否启用、模型是否就绪、版本与规模统计。
     */
    public Map<String, Object> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("enabled", engineConfig.isJenaEnabled());
        status.put("reasonerType", engineConfig.getReasonerType());
        status.put("storageMode", engineConfig.getStorageMode());

        if (!engineConfig.isJenaEnabled()) {
            status.put("ready", false);
            status.put("note", "Jena 未启用 (bontolink.ontology.jena-enabled=false)");
            return status;
        }

        try {
            Map<String, Object> stats = modelManager.getStats();
            status.put("ready", ((Number) stats.get("loadedVersion")).intValue() >= 0);
            status.put("loadedVersion", stats.get("loadedVersion"));
            status.put("dbVersion", stats.get("dbVersion"));
            status.put("statements", stats.get("statements"));
            status.put("classes", stats.get("classes"));
            status.put("properties", stats.get("properties"));
            status.put("rebuilding", stats.get("rebuilding"));
        } catch (Exception e) {
            status.put("ready", false);
            status.put("error", e.getMessage());
        }
        return status;
    }

    /**
     * 执行 SPARQL 查询（SELECT / ASK / CONSTRUCT / DESCRIBE 均支持）。
     *
     * @param sparql SPARQL 语句
     * @param limit  结果行数上限（仅 SELECT 生效，<=0 表示不限制）
     * @return 含 queryType 与结果数据的映射
     */
    public Map<String, Object> sparql(String sparql, int limit) {
        return sparql(sparql, limit, "json");
    }

    /**
     * 执行 SPARQL 查询，支持多格式输出。
     *
     * @param sparql SPARQL 语句
     * @param limit  结果行数上限（仅 SELECT 生效，<=0 表示不限制）
     * @param format 输出格式：json(默认) | csv(SELECT) | turtle(CONSTRUCT/DESCRIBE)
     */
    public Map<String, Object> sparql(String sparql, int limit, String format) {
        String fmt = (format == null || format.isBlank()) ? "json" : format.trim().toLowerCase();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("format", fmt);
        if (sparql == null || sparql.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "SPARQL 不能为空");
            return result;
        }

        OntModel model = modelManager.getModel();
        if (model == null) {
            result.put("success", false);
            result.put("message", "OntModel 未就绪（Jena 未启用或仍在重建）");
            return result;
        }

        try {
            Query query = QueryFactory.create(sparql);
            try (QueryExecution qe = QueryExecutionFactory.create(query, model)) {
                String type = query.isSelectType() ? "SELECT"
                        : query.isAskType() ? "ASK"
                        : query.isConstructType() ? "CONSTRUCT"
                        : query.isDescribeType() ? "DESCRIBE" : "UNKNOWN";
                result.put("queryType", type);

                if (query.isSelectType()) {
                    List<Map<String, Object>> rows = new ArrayList<>();
                    ResultSet rs = qe.execSelect();
                    int i = 0;
                    while (rs.hasNext() && (limit <= 0 || i < limit)) {
                        QuerySolution sol = rs.next();
                        Map<String, Object> row = new LinkedHashMap<>();
                        Iterator<String> varNames = sol.varNames();
                        while (varNames.hasNext()) {
                            String var = varNames.next();
                            RDFNode node = sol.get(var);
                            row.put(var, node == null ? null : nodeToDisplay(node));
                        }
                        rows.add(row);
                        i++;
                    }
                    result.put("success", true);
                    result.put("vars", rs.getResultVars());
                    result.put("rows", rows);
                    result.put("rowCount", rows.size());
                    if ("csv".equals(fmt)) result.put("csv", toCsv(rs.getResultVars(), rows));
                } else if (query.isAskType()) {
                    result.put("success", true);
                    result.put("ask", qe.execAsk());
                } else if (query.isConstructType() || query.isDescribeType()) {
                    org.apache.jena.rdf.model.Model m = query.isConstructType()
                            ? qe.execConstruct() : qe.execDescribe();
                    result.put("success", true);
                    result.put("tripleCount", m.size());
                    result.put("triples", modelToTriples(m, limit));
                    if ("turtle".equals(fmt)) {
                        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                        m.write(baos, "TURTLE");
                        result.put("turtle", baos.toString(java.nio.charset.StandardCharsets.UTF_8));
                    }
                } else {
                    result.put("success", false);
                    result.put("message", "不支持的查询类型: " + type);
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("SPARQL 执行失败: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "SPARQL 执行失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 列出所有 OWL 类（跳过匿名类与 OWL/RDF/RDFS 内置类）。
     */
    public Map<String, Object> listClasses() {
        Map<String, Object> result = new LinkedHashMap<>();
        OntModel model = modelManager.getModel();
        if (model == null) {
            result.put("success", false);
            result.put("message", "OntModel 未就绪");
            return result;
        }
        try {
            List<Map<String, Object>> classes = new ArrayList<>();
            ExtendedIterator<OntClass> iter = model.listClasses();
            while (iter.hasNext()) {
                OntClass ontClass = iter.next();
                if (ontClass.isAnon()) continue;
                String uri = ontClass.getURI();
                if (uri == null || uri.contains("/owl#") || uri.contains("/rdf#") || uri.contains("/rdfs#")) continue;

                Map<String, Object> info = new LinkedHashMap<>();
                info.put("uri", uri);
                info.put("localName", ontClass.getLocalName());
                info.put("label", ontClass.getLabel("zh") != null ? ontClass.getLabel("zh") : ontClass.getLocalName());

                List<String> superClasses = new ArrayList<>();
                ExtendedIterator<OntClass> superIter = ontClass.listSuperClasses(true);
                while (superIter.hasNext()) {
                    OntClass sup = superIter.next();
                    if (!sup.isAnon() && sup.getURI() != null) superClasses.add(sup.getLocalName());
                }
                info.put("superClasses", superClasses);
                classes.add(info);
            }
            result.put("success", true);
            result.put("classes", classes);
            result.put("count", classes.size());
            return result;
        } catch (Exception e) {
            log.warn("列出 OWL 类失败", e);
            result.put("success", false);
            result.put("message", "查询失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 列出所有 OWL 属性（ObjectProperty / DatatypeProperty）。
     */
    public Map<String, Object> listProperties() {
        Map<String, Object> result = new LinkedHashMap<>();
        OntModel model = modelManager.getModel();
        if (model == null) {
            result.put("success", false);
            result.put("message", "OntModel 未就绪");
            return result;
        }
        try {
            List<Map<String, Object>> properties = new ArrayList<>();
            ExtendedIterator<OntProperty> iter = model.listAllOntProperties();
            while (iter.hasNext()) {
                OntProperty prop = iter.next();
                if (prop.isAnon()) continue;

                Map<String, Object> info = new LinkedHashMap<>();
                info.put("uri", prop.getURI());
                info.put("localName", prop.getLocalName());
                info.put("label", prop.getLabel("zh") != null ? prop.getLabel("zh") : prop.getLocalName());
                info.put("type", prop.isObjectProperty() ? "ObjectProperty"
                        : prop.isDatatypeProperty() ? "DatatypeProperty" : "Property");

                List<String> domains = new ArrayList<>();
                ExtendedIterator<? extends org.apache.jena.ontology.OntResource> di = prop.listDomain();
                while (di.hasNext()) {
                    org.apache.jena.ontology.OntResource d = di.next();
                    if (!d.isAnon() && d.getURI() != null) domains.add(d.getLocalName());
                }
                info.put("domains", domains);

                List<String> ranges = new ArrayList<>();
                ExtendedIterator<? extends org.apache.jena.ontology.OntResource> ri = prop.listRange();
                while (ri.hasNext()) {
                    org.apache.jena.ontology.OntResource r = ri.next();
                    if (!r.isAnon() && r.getURI() != null) ranges.add(r.getLocalName());
                }
                info.put("ranges", ranges);
                properties.add(info);
            }
            result.put("success", true);
            result.put("properties", properties);
            result.put("count", properties.size());
            return result;
        } catch (Exception e) {
            log.warn("列出 OWL 属性失败", e);
            result.put("success", false);
            result.put("message", "查询失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 查看某个类的详细信息（父类、子类、关联属性）。
     */
    public Map<String, Object> classDetail(String localName) {
        Map<String, Object> result = new LinkedHashMap<>();
        OntModel model = modelManager.getModel();
        if (model == null) {
            result.put("success", false);
            result.put("message", "OntModel 未就绪");
            return result;
        }
        try {
            OntClass ontClass = resolveClass(model, localName);
            if (ontClass == null) {
                result.put("success", false);
                result.put("message", "类不存在: " + localName);
                return result;
            }

            result.put("uri", ontClass.getURI());
            result.put("localName", ontClass.getLocalName());
            result.put("label", ontClass.getLabel("zh"));

            List<String> superClasses = new ArrayList<>();
            ExtendedIterator<OntClass> si = ontClass.listSuperClasses(true);
            while (si.hasNext()) {
                OntClass sup = si.next();
                if (!sup.isAnon() && sup.getURI() != null) superClasses.add(sup.getLocalName());
            }
            result.put("superClasses", superClasses);

            List<String> subClasses = new ArrayList<>();
            ExtendedIterator<OntClass> subi = ontClass.listSubClasses(true);
            while (subi.hasNext()) {
                OntClass sub = subi.next();
                if (!sub.isAnon() && sub.getURI() != null) subClasses.add(sub.getLocalName());
            }
            result.put("subClasses", subClasses);

            List<Map<String, Object>> props = new ArrayList<>();
            ExtendedIterator<OntProperty> pi = model.listAllOntProperties();
            while (pi.hasNext()) {
                OntProperty prop = pi.next();
                if (prop.isAnon()) continue;
                ExtendedIterator<? extends org.apache.jena.ontology.OntResource> di = prop.listDomain();
                while (di.hasNext()) {
                    org.apache.jena.ontology.OntResource d = di.next();
                    if (d.equals(ontClass)) {
                        Map<String, Object> p = new LinkedHashMap<>();
                        p.put("localName", prop.getLocalName());
                        p.put("label", prop.getLabel("zh"));
                        p.put("type", prop.isObjectProperty() ? "ObjectProperty"
                                : prop.isDatatypeProperty() ? "DatatypeProperty" : "Property");
                        props.add(p);
                        break;
                    }
                }
            }
            result.put("properties", props);
            result.put("success", true);
            return result;
        } catch (Exception e) {
            log.warn("查询类详情失败: {}", localName, e);
            result.put("success", false);
            result.put("message", "查询失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 查询枚举类的所有 Individual（枚举项）。
     *
     * @param enumApiName 枚举类的 api_name（例如 "AdministrativeRegion"）
     * @return 枚举项列表，每项包含 uri, localName, label, code, apiName, 以及枚举类的 enumType
     */
    public Map<String, Object> getEnumIndividuals(String enumApiName) {
        Map<String, Object> result = new LinkedHashMap<>();
        OntModel model = modelManager.getModel();
        if (model == null) {
            result.put("success", false);
            result.put("message", "OntModel 未就绪");
            return result;
        }

        try {
            // 枚举类的 URI: http://bontolink.beiktech.com/ontology#Enum_{api_name}
            String enumClassUri = NS_PREFIX + "Enum_" + enumApiName;
            OntClass enumClass = model.getOntClass(enumClassUri);

            if (enumClass == null) {
                result.put("success", false);
                result.put("message", "枚举类不存在: " + enumApiName);
                return result;
            }

            // 查询枚举类的 enumType 属性
            Property enumTypeProp = model.getProperty(NS_PREFIX + "enumType");
            String enumType = null;
            if (enumTypeProp != null && enumClass.hasProperty(enumTypeProp)) {
                RDFNode enumTypeNode = enumClass.getPropertyValue(enumTypeProp);
                if (enumTypeNode != null) {
                    enumType = enumTypeNode.toString();
                }
            }

            // 从本体获取所有 Individual
            Map<String, Map<String, Object>> itemsMap = new LinkedHashMap<>();
            ExtendedIterator<? extends org.apache.jena.ontology.OntResource> instances = enumClass.listInstances(false);

            while (instances.hasNext()) {
                org.apache.jena.ontology.OntResource instance = instances.next();
                if (instance.isIndividual()) {
                    Individual individual = instance.asIndividual();

                    // 获取 code 属性（作为 key）
                    Property codeProp = model.getProperty(NS_PREFIX + "code");
                    String code = null;
                    if (codeProp != null && individual.hasProperty(codeProp)) {
                        RDFNode codeNode = individual.getPropertyValue(codeProp);
                        if (codeNode != null) {
                            code = codeNode.toString();
                        }
                    }

                    if (code == null) continue;

                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("uri", individual.getURI());
                    item.put("localName", individual.getLocalName());
                    item.put("label", individual.getLabel("zh"));
                    item.put("code", code);

                    // 获取 apiName 属性
                    Property apiNameProp = model.getProperty(NS_PREFIX + "apiName");
                    if (apiNameProp != null && individual.hasProperty(apiNameProp)) {
                        RDFNode apiNameNode = individual.getPropertyValue(apiNameProp);
                        if (apiNameNode != null) {
                            item.put("apiName", apiNameNode.toString());
                        }
                    }

                    itemsMap.put(code, item);
                }
            }

            // 补充数据库中的额外字段（parent_code, level, sort_num, status）
            // 需要依赖注入 JdbcTemplate，但这个 Service 没有，所以在 SemanticExpandService 中处理

            result.put("success", true);
            result.put("enumClass", enumApiName);
            result.put("enumType", enumType);  // 枚举类型：general_single / general_multi / biz_single / biz_multi
            result.put("count", itemsMap.size());
            result.put("items", new ArrayList<>(itemsMap.values()));
            return result;

        } catch (Exception e) {
            log.warn("查询枚举Individual失败: {}", enumApiName, e);
            result.put("success", false);
            result.put("message", "查询失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 查看某个属性的详细信息。
     *
     * <p>返回比属性列表更丰富的元数据：注释（中/英）、定义域、值域、
     * 超属性（subPropertyOf 的反向）、子属性、逆属性（inverseOf）、
     * 等价属性（equivalentProperty），以及 OWL 特征（函数/反函数/对称/传递）。
     */
    public Map<String, Object> propertyDetail(String localName) {
        Map<String, Object> result = new LinkedHashMap<>();
        OntModel model = modelManager.getModel();
        if (model == null) {
            result.put("success", false);
            result.put("message", "OntModel 未就绪");
            return result;
        }
        try {
            OntProperty prop = model.getOntProperty(NS_PREFIX + localName);
            if (prop == null) {
                // 跨命名空间扫描
                Map<String, String> prefixMap = model.getNsPrefixMap();
                for (String ns : prefixMap.values()) {
                    OntProperty cand = model.getOntProperty(ns + localName);
                    if (cand != null) { prop = cand; break; }
                }
            }
            if (prop == null) {
                // 兜底：按 localName 全量扫描
                ExtendedIterator<OntProperty> scan = model.listAllOntProperties();
                while (scan.hasNext()) {
                    OntProperty cand = scan.next();
                    if (!cand.isAnon() && localName.equalsIgnoreCase(cand.getLocalName())) {
                        prop = cand;
                        break;
                    }
                }
            }
            if (prop == null) {
                result.put("success", false);
                result.put("message", "属性不存在: " + localName);
                return result;
            }

            result.put("uri", prop.getURI());
            result.put("localName", prop.getLocalName());
            result.put("label", prop.getLabel("zh"));
            result.put("labelEn", prop.getLabel(null));
            result.put("type", prop.isObjectProperty() ? "ObjectProperty"
                    : prop.isDatatypeProperty() ? "DatatypeProperty" : "Property");
            result.put("comment", prop.getComment("zh"));
            result.put("commentEn", prop.getComment(null));

            // 定义域 / 值域
            List<String> domains = new ArrayList<>();
            ExtendedIterator<? extends org.apache.jena.ontology.OntResource> di = prop.listDomain();
            while (di.hasNext()) {
                org.apache.jena.ontology.OntResource d = di.next();
                if (!d.isAnon() && d.getURI() != null) domains.add(d.getLocalName());
            }
            result.put("domains", domains);

            List<String> ranges = new ArrayList<>();
            ExtendedIterator<? extends org.apache.jena.ontology.OntResource> ri = prop.listRange();
            while (ri.hasNext()) {
                org.apache.jena.ontology.OntResource r = ri.next();
                if (!r.isAnon() && r.getURI() != null) ranges.add(r.getLocalName());
            }
            result.put("ranges", ranges);

            // 数据类型（dataType）
            Property dataTypeProp = model.getProperty(NS_PREFIX + "dataType");
            if (dataTypeProp != null && prop.hasProperty(dataTypeProp)) {
                RDFNode dataTypeNode = prop.getPropertyValue(dataTypeProp);
                if (dataTypeNode != null) {
                    result.put("dataType", dataTypeNode.toString());
                }
            }

            // 物理表名 + 数据源编码（跨库取数用）
            Property physicalTableProp = model.getProperty(NS_PREFIX + "physicalTable");
            if (physicalTableProp != null && prop.hasProperty(physicalTableProp)) {
                RDFNode n = prop.getPropertyValue(physicalTableProp);
                if (n != null) result.put("physicalTable", n.toString());
            }
            Property dataSourceCodeProp = model.getProperty(NS_PREFIX + "dataSourceCode");
            if (dataSourceCodeProp != null && prop.hasProperty(dataSourceCodeProp)) {
                RDFNode n = prop.getPropertyValue(dataSourceCodeProp);
                if (n != null) result.put("dataSourceCode", n.toString());
            }

            // 超属性（该属性是其子类）/ 子属性
            List<String> supers = new ArrayList<>();
            ExtendedIterator<? extends OntProperty> spi = prop.listSuperProperties();
            while (spi.hasNext()) {
                OntProperty sp = spi.next();
                if (!sp.isAnon() && sp.getURI() != null) supers.add(sp.getLocalName());
            }
            result.put("superProperties", supers);

            List<String> subs = new ArrayList<>();
            ExtendedIterator<? extends OntProperty> subi = prop.listSubProperties();
            while (subi.hasNext()) {
                OntProperty sb = subi.next();
                if (!sb.isAnon() && sb.getURI() != null) subs.add(sb.getLocalName());
            }
            result.put("subProperties", subs);

            // 逆属性
            OntProperty inverse = prop.getInverseOf();
            result.put("inverseOf", (inverse != null && !inverse.isAnon() && inverse.getURI() != null)
                    ? inverse.getLocalName() : null);

            // 等价属性
            List<String> equivalents = new ArrayList<>();
            ExtendedIterator<? extends OntProperty> epi = prop.listEquivalentProperties();
            while (epi.hasNext()) {
                OntProperty ep = epi.next();
                if (!ep.isAnon() && ep.getURI() != null) equivalents.add(ep.getLocalName());
            }
            result.put("equivalentProperties", equivalents);

            // OWL 特征
            Map<String, Boolean> chars = new LinkedHashMap<>();
            chars.put("functional", prop.isFunctionalProperty());
            chars.put("inverseFunctional", prop.isInverseFunctionalProperty());
            chars.put("symmetric", prop.isSymmetricProperty());
            chars.put("transitive", prop.isTransitiveProperty());
            result.put("characteristics", chars);

            result.put("success", true);
            return result;
        } catch (Exception e) {
            log.warn("查询属性详情失败: {}", localName, e);
            result.put("success", false);
            result.put("message", "查询失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 通用资源页（仿 Fuseki resource explorer，只读）。
     *
     * <p>以 URI 为中心，返回任意 IRI 资源的：标签、类型（rdf:type）、
     * 出站三元组（作为 subject，按谓词分组）与入站三元组（作为 object，分页）。
     * 点击任意宾语/主语可继续下钻。适用于类、属性、个体、枚举值、匿名资源。
     *
     * @param uri  资源完整 URI
     * @param page 入站三元组页码（1 起）
     * @param size 入站三元组每页条数
     */
    public Map<String, Object> resource(String uri, int page, int size) {
        Map<String, Object> result = new LinkedHashMap<>();
        OntModel model = modelManager.getModel();
        if (model == null) {
            result.put("success", false);
            result.put("message", "OntModel 未就绪");
            return result;
        }
        if (uri == null || uri.isBlank()) {
            result.put("success", false);
            result.put("message", "uri 不能为空");
            return result;
        }
        try {
            Resource res = model.getResource(uri);
            boolean hasOut = model.contains(res, null, (RDFNode) null);
            boolean hasIn = model.contains(null, null, res);
            if (!hasOut && !hasIn) {
                result.put("success", false);
                result.put("message", "未找到该资源的相关数据: " + uri);
                return result;
            }

            result.put("uri", uri);
            result.put("localName", res.getLocalName());
            result.put("label", labelOf(model, res));

            // 类型（rdf:type，对任意资源通用）
            List<Map<String, Object>> types = new ArrayList<>();
            StmtIterator ti = model.listStatements(res, RDF.type, (RDFNode) null);
            while (ti.hasNext()) {
                RDFNode t = ti.next().getObject();
                if (t.isResource() && t.asResource().getURI() != null) types.add(refMap(model, t.asResource()));
            }
            result.put("types", types);

            // 出站三元组（按谓词分组），限制条数防超大
            int maxOutgoing = 300;
            Map<String, Map<String, Object>> byPred = new LinkedHashMap<>();
            int outTotal = 0;
            boolean outTruncated = false;
            StmtIterator so = model.listStatements(res, null, (RDFNode) null);
            while (so.hasNext()) {
                Statement st = so.next();
                outTotal++;
                if (outTotal > maxOutgoing) { outTruncated = true; continue; }
                Property p = st.getPredicate();
                Map<String, Object> group = byPred.computeIfAbsent(p.getURI(), k -> {
                    Map<String, Object> g = new LinkedHashMap<>();
                    g.put("predicate", p.getURI());
                    g.put("predicateLocalName", p.getLocalName());
                    g.put("predicateLabel", labelOf(model, p));
                    g.put("objects", new ArrayList<Map<String, Object>>());
                    return g;
                });
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> objs = (List<Map<String, Object>>) group.get("objects");
                objs.add(nodeToMap(model, st.getObject()));
            }
            result.put("outgoing", new ArrayList<>(byPred.values()));
            result.put("outgoingTotal", outTotal);
            result.put("outgoingTruncated", outTruncated);

            // 入站三元组（分页）
            List<Map<String, Object>> allIn = new ArrayList<>();
            StmtIterator si = model.listStatements(null, null, res);
            while (si.hasNext()) {
                Statement st = si.next();
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("subject", refMap(model, st.getSubject()));
                row.put("predicate", refMap(model, st.getPredicate()));
                allIn.add(row);
            }
            int inTotal = allIn.size();
            int p = Math.max(1, page);
            int s = size <= 0 ? 50 : size;
            int from = Math.min((p - 1) * s, inTotal);
            int to = Math.min(from + s, inTotal);
            result.put("incoming", allIn.subList(from, to));
            result.put("incomingTotal", inTotal);
            result.put("page", p);
            result.put("size", s);
            result.put("success", true);
            return result;
        } catch (Exception e) {
            log.warn("查询资源失败: {}", uri, e);
            result.put("success", false);
            result.put("message", "查询失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 成员浏览：列出某个类的全部个体（实例 / 枚举值通用，只读）。
     *
     * @param classRef 类引用：完整 URI，或本体命名空间下的 localName
     * @param keyword  关键字（匹配 localName / label / code，可空）
     * @param page     页码（1 起）
     * @param size     每页条数
     */
    public Map<String, Object> members(String classRef, String keyword, int page, int size) {
        Map<String, Object> result = new LinkedHashMap<>();
        OntModel model = modelManager.getModel();
        if (model == null) {
            result.put("success", false);
            result.put("message", "OntModel 未就绪");
            return result;
        }
        OntClass cls = resolveClass(model, classRef);
        if (cls == null) {
            result.put("success", false);
            result.put("message", "类不存在: " + classRef);
            return result;
        }
        try {
            Property codeProp = model.getProperty(NS_PREFIX + "code");
            String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim().toLowerCase();
            List<Map<String, Object>> all = new ArrayList<>();
            ExtendedIterator<? extends org.apache.jena.ontology.OntResource> it = cls.listInstances(true);
            while (it.hasNext()) {
                org.apache.jena.ontology.OntResource inst = it.next();
                if (inst.isAnon() || inst.getURI() == null) continue;
                String ln = inst.getLocalName();
                String label = labelOf(model, inst);
                String code = null;
                if (codeProp != null) {
                    Statement cs = inst.getProperty(codeProp);
                    if (cs != null && cs.getObject().isLiteral()) code = cs.getObject().asLiteral().getString();
                }
                if (kw != null) {
                    boolean hit = (ln != null && ln.toLowerCase().contains(kw))
                            || (label != null && label.toLowerCase().contains(kw))
                            || (code != null && code.toLowerCase().contains(kw));
                    if (!hit) continue;
                }
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("localName", ln);
                m.put("uri", inst.getURI());
                m.put("label", label);
                m.put("code", code);
                all.add(m);
            }
            // 排序：code 数字优先，其次 localName
            all.sort((a, b) -> {
                String ca = (String) a.get("code"), cb = (String) b.get("code");
                Integer na = parseIntOrNull(ca), nb = parseIntOrNull(cb);
                if (na != null && nb != null) return na.compareTo(nb);
                if (na != null) return -1;
                if (nb != null) return 1;
                String la = String.valueOf(a.get("localName")), lb = String.valueOf(b.get("localName"));
                return la.compareTo(lb);
            });
            int total = all.size();
            int p = Math.max(1, page);
            int s = size <= 0 ? 50 : size;
            int from = Math.min((p - 1) * s, total);
            int to = Math.min(from + s, total);
            result.put("classUri", cls.getURI());
            result.put("classLocalName", cls.getLocalName());
            result.put("classLabel", labelOf(model, cls));
            result.put("members", all.subList(from, to));
            result.put("total", total);
            result.put("page", p);
            result.put("size", s);
            result.put("success", true);
            return result;
        } catch (Exception e) {
            log.warn("查询类成员失败: {}", classRef, e);
            result.put("success", false);
            result.put("message", "查询失败: " + e.getMessage());
            return result;
        }
    }

    /** 解析类引用：含 "#" 或 "://" 视为完整 URI，否则按本体命名空间补全。 */
    private OntClass resolveClass(OntModel model, String classRef) {
        if (classRef == null || classRef.isBlank()) return null;
        // 完整 URI 直接查
        if (classRef.contains("#") || classRef.startsWith("http")) {
            return model.getOntClass(classRef);
        }
        // localName：先尝试各命名空间前缀拼接，找到即返回
        Map<String, String> prefixMap = model.getNsPrefixMap();
        for (String ns : prefixMap.values()) {
            OntClass c = model.getOntClass(ns + classRef);
            if (c != null) return c;
        }
        // 兜底：全量扫描按 localName 匹配
        ExtendedIterator<OntClass> iter = model.listClasses();
        while (iter.hasNext()) {
            OntClass c = iter.next();
            if (!c.isAnon() && classRef.equals(c.getLocalName())) return c;
        }
        return null;
    }

    /** 资源引用 → 简洁映射（可点击下钻）。 */
    private Map<String, Object> refMap(OntModel model, Resource r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("uri", r.getURI());
        m.put("localName", r.getLocalName());
        m.put("label", labelOf(model, r));
        m.put("isResource", true);
        return m;
    }

    /** RDF 节点 → 统一映射：字面量（含语言/数据类型）或资源引用。 */
    private Map<String, Object> nodeToMap(OntModel model, RDFNode node) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (node.isLiteral()) {
            org.apache.jena.rdf.model.Literal lit = node.asLiteral();
            m.put("isLiteral", true);
            m.put("value", lit.getString());
            m.put("lang", lit.getLanguage());
            m.put("datatype", lit.getDatatypeURI());
        } else if (node.isResource()) {
            Resource r = node.asResource();
            if (r.isAnon()) {
                m.put("isAnon", true);
                m.put("isResource", true);
                m.put("value", "_:" + r.getId().getLabelString());
            } else {
                m.putAll(refMap(model, r));
            }
        }
        return m;
    }

    /** 取资源标签：优先 @zh，其次任意语言，最后回退 localName。 */
    private String labelOf(OntModel model, Resource r) {
        if (r == null || r.getURI() == null) return null;
        StmtIterator it = model.listStatements(r, RDFS.label, (RDFNode) null);
        String fallback = null;
        while (it.hasNext()) {
            Statement st = it.next();
            if (st.getObject().isLiteral()) {
                org.apache.jena.rdf.model.Literal l = st.getObject().asLiteral();
                if ("zh".equalsIgnoreCase(l.getLanguage())) return l.getString();
                if (fallback == null) fallback = l.getString();
            }
        }
        return fallback != null ? fallback : r.getLocalName();
    }

    private Integer parseIntOrNull(String s) {
        if (s == null) return null;
        try { return Integer.valueOf(s.trim()); } catch (Exception e) { return null; }
    }

    /**
     * 元数据：命名空间/前缀、本体声明（URI/标签/注释/imports）、规模统计。只读。
     */
    public Map<String, Object> vocab() {
        Map<String, Object> result = new LinkedHashMap<>();
        OntModel model = modelManager.getModel();
        if (model == null) {
            result.put("success", false);
            result.put("message", "OntModel 未就绪");
            return result;
        }
        try {
            List<Map<String, Object>> prefixes = new ArrayList<>();
            model.getNsPrefixMap().forEach((p, ns) -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("prefix", p);
                m.put("ns", ns);
                prefixes.add(m);
            });

            List<String> imports = new ArrayList<>();
            String ontologyUri = null, ontologyLabel = null, ontologyComment = null;
            ExtendedIterator<Ontology> oi = model.listOntologies();
            while (oi.hasNext()) {
                Ontology o = oi.next();
                if (ontologyUri == null) ontologyUri = o.getURI();
                if (ontologyLabel == null) ontologyLabel = o.getLabel("zh") != null ? o.getLabel("zh") : o.getLabel(null);
                if (ontologyComment == null) ontologyComment = o.getComment("zh") != null ? o.getComment("zh") : o.getComment(null);
                StmtIterator im = model.listStatements(o, OWL.imports, (RDFNode) null);
                while (im.hasNext()) {
                    RDFNode n = im.next().getObject();
                    if (n.isResource() && n.asResource().getURI() != null) imports.add(n.asResource().getURI());
                }
            }

            result.put("prefixes", prefixes);
            result.put("prefixCount", prefixes.size());
            result.put("ontologyUri", ontologyUri);
            result.put("ontologyLabel", ontologyLabel);
            result.put("ontologyComment", ontologyComment);
            result.put("imports", imports);
            result.put("statements", model.size());
            result.put("classes", countKind(model, "class"));
            result.put("properties", countKind(model, "property"));
            result.put("individuals", countIndividuals(model));
            result.put("success", true);
            return result;
        } catch (Exception e) {
            log.warn("读取本体元数据失败", e);
            result.put("success", false);
            result.put("message", "查询失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 实体类型清单及数量（kind 计数）。只读。
     */
    public Map<String, Object> kinds() {
        Map<String, Object> result = new LinkedHashMap<>();
        OntModel model = modelManager.getModel();
        if (model == null) {
            result.put("success", false);
            result.put("message", "OntModel 未就绪");
            return result;
        }
        try {
            List<Map<String, Object>> kinds = new ArrayList<>();
            kinds.add(kindCount("class", countKind(model, "class")));
            kinds.add(kindCount("objectProperty", countKind(model, "objectProperty")));
            kinds.add(kindCount("datatypeProperty", countKind(model, "datatypeProperty")));
            kinds.add(kindCount("annotationProperty", countKind(model, "annotationProperty")));
            kinds.add(kindCount("individual", countIndividuals(model)));
            kinds.add(kindCount("functionalProperty", countKind(model, "functionalProperty")));
            result.put("kinds", kinds);
            result.put("success", true);
            return result;
        } catch (Exception e) {
            log.warn("统计实体类型失败", e);
            result.put("success", false);
            result.put("message", "查询失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 通用实体目录检索（kind 可扩展），支持命名空间与关键字过滤、分页。只读。
     *
     * @param kind class | objectProperty | datatypeProperty | annotationProperty | individual | all
     */
    public Map<String, Object> entities(String kind, String ns, String keyword, int page, int size) {
        Map<String, Object> result = new LinkedHashMap<>();
        OntModel model = modelManager.getModel();
        if (model == null) {
            result.put("success", false);
            result.put("message", "OntModel 未就绪");
            return result;
        }
        try {
            String k = (kind == null || kind.isBlank()) ? "all" : kind.trim().toLowerCase();
            String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim().toLowerCase();
            boolean wantAll = "all".equals(k);

            List<Map<String, Object>> all = new ArrayList<>();
            if (wantAll || "class".equals(k)) collectClasses(model, all);
            if (wantAll || "objectproperty".equals(k)) collectProps(model, all, true, false);
            if (wantAll || "datatypeproperty".equals(k)) collectProps(model, all, false, true);
            if (wantAll || "annotationproperty".equals(k)) collectAnnotations(model, all);
            if (wantAll || "individual".equals(k)) collectIndividuals(model, all);

            List<Map<String, Object>> filtered = new ArrayList<>();
            for (Map<String, Object> m : all) {
                String uri = (String) m.get("uri");
                if (ns != null && !ns.isBlank() && (uri == null || !uri.startsWith(ns))) continue;
                if (kw != null) {
                    String ln = String.valueOf(m.get("localName")).toLowerCase();
                    String lb = String.valueOf(m.get("label")).toLowerCase();
                    if (!ln.contains(kw) && !lb.contains(kw)) continue;
                }
                filtered.add(m);
            }
            int total = filtered.size();
            int p = Math.max(1, page);
            int s = size <= 0 ? 50 : size;
            int from = Math.min((p - 1) * s, total);
            int to = Math.min(from + s, total);
            result.put("kind", k);
            result.put("items", filtered.subList(from, to));
            result.put("total", total);
            result.put("page", p);
            result.put("size", s);
            result.put("success", true);
            return result;
        } catch (Exception e) {
            log.warn("实体目录检索失败", e);
            result.put("success", false);
            result.put("message", "查询失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 类层级树（subClassOf），支持指定根与深度限制。只读。
     *
     * @param root  类引用（空 = 全部顶层类）
     * @param depth 最大深度（默认 4，上限 8）
     */
    public Map<String, Object> hierarchy(String root, int depth) {
        Map<String, Object> result = new LinkedHashMap<>();
        OntModel model = modelManager.getModel();
        if (model == null) {
            result.put("success", false);
            result.put("message", "OntModel 未就绪");
            return result;
        }
        try {
            int maxDepth = depth <= 0 ? 4 : Math.min(depth, 8);
            List<Map<String, Object>> tree = new ArrayList<>();
            Set<String> visited = new HashSet<>();
            if (root == null || root.isBlank()) {
                ExtendedIterator<OntClass> roots = model.listHierarchyRootClasses();
                while (roots.hasNext()) {
                    OntClass c = roots.next();
                    if (c.isAnon()) continue;
                    String u = c.getURI();
                    if (u == null || u.contains("/owl#") || u.contains("/rdf#") || u.contains("/rdfs#")) continue;
                    tree.add(buildClassNode(model, c, 1, maxDepth, visited));
                }
            } else {
                OntClass c = resolveClass(model, root);
                if (c == null) {
                    result.put("success", false);
                    result.put("message", "类不存在: " + root);
                    return result;
                }
                tree.add(buildClassNode(model, c, 1, maxDepth, visited));
            }
            result.put("tree", tree);
            result.put("rootCount", tree.size());
            result.put("maxDepth", maxDepth);
            result.put("success", true);
            return result;
        } catch (Exception e) {
            log.warn("构建类树失败", e);
            result.put("success", false);
            result.put("message", "查询失败: " + e.getMessage());
            return result;
        }
    }

    // ---------------- M2 辅助 ----------------

    private Map<String, Object> buildClassNode(OntModel model, OntClass cls, int depth, int maxDepth, Set<String> visited) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("uri", cls.getURI());
        node.put("localName", cls.getLocalName());
        node.put("label", labelOf(model, cls));
        node.put("memberCount", countDirectInstances(cls));
        List<Map<String, Object>> children = new ArrayList<>();
        if (depth < maxDepth && cls.getURI() != null && visited.add(cls.getURI())) {
            ExtendedIterator<OntClass> subs = cls.listSubClasses(true);
            while (subs.hasNext()) {
                OntClass s = subs.next();
                if (s.isAnon() || s.getURI() == null || s.equals(cls)) continue;
                children.add(buildClassNode(model, s, depth + 1, maxDepth, visited));
            }
        }
        node.put("children", children);
        return node;
    }

    private Map<String, Object> kindCount(String kind, int count) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("kind", kind);
        m.put("count", count);
        return m;
    }

    private Map<String, Object> entityMap(String uri, String localName, String label, String kind) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("uri", uri);
        m.put("localName", localName);
        m.put("label", label);
        m.put("kind", kind);
        return m;
    }

    /** 按类别计数（class/property/objectProperty/datatypeProperty/annotationProperty/functionalProperty）。 */
    private int countKind(OntModel model, String kind) {
        int n = 0;
        switch (kind) {
            case "class": {
                ExtendedIterator<OntClass> it = model.listClasses();
                while (it.hasNext()) {
                    OntClass c = it.next();
                    if (c.isAnon()) continue;
                    String u = c.getURI();
                    if (u == null || u.contains("/owl#") || u.contains("/rdf#") || u.contains("/rdfs#")) continue;
                    n++;
                }
                break;
            }
            case "objectProperty": case "datatypeProperty": case "property": case "functionalProperty": {
                ExtendedIterator<OntProperty> it = model.listAllOntProperties();
                while (it.hasNext()) {
                    OntProperty p = it.next();
                    if (p.isAnon()) continue;
                    if ("objectProperty".equals(kind) && !p.isObjectProperty()) continue;
                    if ("datatypeProperty".equals(kind) && !p.isDatatypeProperty()) continue;
                    if ("functionalProperty".equals(kind) && !p.isFunctionalProperty()) continue;
                    n++;
                }
                break;
            }
            case "annotationProperty": {
                StmtIterator it = model.listStatements(null, RDF.type, OWL.AnnotationProperty);
                while (it.hasNext()) { it.next(); n++; }
                break;
            }
            default:
        }
        return n;
    }

    private int countIndividuals(OntModel model) {
        int n = 0;
        ExtendedIterator<Individual> it = model.listIndividuals();
        while (it.hasNext()) { if (!it.next().isAnon()) n++; }
        return n;
    }

    private int countDirectInstances(OntClass cls) {
        int n = 0;
        ExtendedIterator<? extends org.apache.jena.ontology.OntResource> it = cls.listInstances(true);
        while (it.hasNext()) { if (!it.next().isAnon()) n++; }
        return n;
    }

    private void collectClasses(OntModel model, List<Map<String, Object>> out) {
        ExtendedIterator<OntClass> it = model.listClasses();
        while (it.hasNext()) {
            OntClass c = it.next();
            if (c.isAnon()) continue;
            String u = c.getURI();
            if (u == null || u.contains("/owl#") || u.contains("/rdf#") || u.contains("/rdfs#")) continue;
            out.add(entityMap(u, c.getLocalName(), labelOf(model, c), "class"));
        }
    }

    private void collectProps(OntModel model, List<Map<String, Object>> out, boolean obj, boolean dt) {
        ExtendedIterator<OntProperty> it = model.listAllOntProperties();
        while (it.hasNext()) {
            OntProperty p = it.next();
            if (p.isAnon()) continue;
            if (obj && !p.isObjectProperty()) continue;
            if (dt && !p.isDatatypeProperty()) continue;
            String kind = p.isObjectProperty() ? "objectProperty" : p.isDatatypeProperty() ? "datatypeProperty" : "property";
            out.add(entityMap(p.getURI(), p.getLocalName(), labelOf(model, p), kind));
        }
    }

    private void collectAnnotations(OntModel model, List<Map<String, Object>> out) {
        StmtIterator it = model.listStatements(null, RDF.type, OWL.AnnotationProperty);
        while (it.hasNext()) {
            Resource r = it.next().getSubject();
            if (r.getURI() == null) continue;
            out.add(entityMap(r.getURI(), r.getLocalName(), labelOf(model, r), "annotationProperty"));
        }
    }

    private void collectIndividuals(OntModel model, List<Map<String, Object>> out) {
        ExtendedIterator<Individual> it = model.listIndividuals();
        while (it.hasNext()) {
            Individual i = it.next();
            if (i.isAnon() || i.getURI() == null) continue;
            out.add(entityMap(i.getURI(), i.getLocalName(), labelOf(model, i), "individual"));
        }
    }

    /**
     * 预设 SPARQL 查询模板（通用，可直接执行或改参数）。
     */
    public Map<String, Object> queryTemplates() {
        Map<String, Object> result = new LinkedHashMap<>();
        String ns = NS_PREFIX;
        List<Map<String, Object>> templates = new ArrayList<>();
        templates.add(tpl("所有类", "列出本体中全部 OWL 类及中文标签",
                "SELECT ?c ?l WHERE { ?c a <http://www.w3.org/2002/07/owl#Class> . OPTIONAL { ?c <http://www.w3.org/2000/01/rdf-schema#label> ?l } } LIMIT 100"));
        templates.add(tpl("所有属性", "列出全部对象/数据属性",
                "SELECT ?p ?t WHERE { { ?p a <http://www.w3.org/2002/07/owl#ObjectProperty> BIND('Object' AS ?t) } UNION { ?p a <http://www.w3.org/2002/07/owl#DatatypeProperty> BIND('Datatype' AS ?t) } } LIMIT 200"));
        templates.add(tpl("类的个体", "列出某个类的全部个体（改类名）",
                "SELECT ?i ?l WHERE { ?i a <" + ns + "Enum_DWLX> . OPTIONAL { ?i <http://www.w3.org/2000/01/rdf-schema#label> ?l } } LIMIT 100"));
        templates.add(tpl("资源全部三元组", "查看某资源的全部出站属性（改 URI）",
                "SELECT ?p ?o WHERE { <" + ns + "Enum_DWLX_201> ?p ?o } LIMIT 200"));
        templates.add(tpl("按标签检索", "按中文标签模糊检索资源",
                "SELECT ?s ?l WHERE { ?s <http://www.w3.org/2000/01/rdf-schema#label> ?l . FILTER(CONTAINS(?l, '水利')) } LIMIT 50"));
        templates.add(tpl("实体数量统计", "按 rdf:type 统计实体数量 Top 20",
                "SELECT ?t (COUNT(?s) AS ?c) WHERE { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?t . FILTER(isIRI(?t)) } GROUP BY ?t ORDER BY DESC(?c) LIMIT 20"));
        result.put("templates", templates);
        result.put("count", templates.size());
        result.put("success", true);
        return result;
    }

    private Map<String, Object> tpl(String name, String description, String sparql) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("description", description);
        m.put("sparql", sparql);
        return m;
    }

    /** SELECT 结果值 → 友好显示：字面量取纯文本，资源取 URI。 */
    private String nodeToDisplay(RDFNode node) {
        if (node.isLiteral()) return node.asLiteral().getString();
        if (node.isResource()) {
            Resource r = node.asResource();
            return r.getURI() != null ? r.getURI() : r.toString();
        }
        return node.toString();
    }

    /** SELECT 结果 → CSV 文本（含表头）。 */
    private String toCsv(List<String> vars, List<Map<String, Object>> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", vars)).append('\n');
        for (Map<String, Object> row : rows) {
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < vars.size(); i++) {
                if (i > 0) line.append(',');
                line.append(csvEscape(row.get(vars.get(i))));
            }
            sb.append(line).append('\n');
        }
        return sb.toString();
    }

    private String csvEscape(Object v) {
        if (v == null) return "";
        String s = String.valueOf(v);
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            s = "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private List<String> modelToTriples(org.apache.jena.rdf.model.Model m, int limit) {
        List<String> triples = new ArrayList<>();
        int i = 0;
        Iterator<org.apache.jena.rdf.model.Statement> it = m.listStatements();
        while (it.hasNext() && (limit <= 0 || i < limit)) {
            org.apache.jena.rdf.model.Statement st = it.next();
            Resource s = st.getSubject();
            String p = st.getPredicate().getURI();
            RDFNode o = st.getObject();
            triples.add((s.getURI() != null ? s.getURI() : s.toString()) + " <" + p + "> "
                    + (o.isLiteral() ? "\"" + o.asLiteral().getString() + "\"" : o.toString()));
            i++;
        }
        return triples;
    }
}
