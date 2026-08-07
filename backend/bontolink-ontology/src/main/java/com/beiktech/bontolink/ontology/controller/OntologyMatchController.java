package com.beiktech.bontolink.ontology.controller;

import com.beiktech.bontolink.ontology.model.EntityMatch;
import com.beiktech.bontolink.ontology.model.ExpandedText;
import com.beiktech.bontolink.ontology.service.OntologyModelManager;
import com.beiktech.bontolink.ontology.service.expansion.SemanticExpansionService;
import com.beiktech.bontolink.ontology.service.matching.OntologyMatchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 本体匹配 Controller
 * <p>
 * 依赖 OntologyModelManager 管理 OntModel 生命周期：
 * - 查询时自动检查版本号，落后则异步重建
 * - 支持手动触发 rebuild
 */
@Slf4j
@RestController
@RequestMapping("/api/ontology")
public class OntologyMatchController {

    private final SemanticExpansionService expansionService;
    private final OntologyMatchService matchService;
    private final OntologyModelManager modelManager;

    public OntologyMatchController(SemanticExpansionService expansionService,
                                   OntologyMatchService matchService,
                                   OntologyModelManager modelManager) {
        this.expansionService = expansionService;
        this.matchService = matchService;
        this.modelManager = modelManager;
    }

    /**
     * 预览扩充效果
     */
    @PostMapping("/preview-expansion")
    public Map<String, Object> previewExpansion(@RequestBody Map<String, String> body) {
        try {
            String query = body.get("query");
            if (query == null || query.trim().isEmpty()) {
                return error(400, "查询文本不能为空");
            }

            ExpandedText expanded = expansionService.expandQuery(query);

            Map<String, Object> result = new HashMap<>();
            result.put("original", expanded.getOriginalText());
            result.put("expanded", expanded.getExpandedText());
            result.put("synonyms", expanded.getSynonymMap());
            result.put("stats", expanded.getStats());

            return success(result);
        } catch (Exception e) {
            log.error("预览扩充失败", e);
            return error(500, "预览失败: " + e.getMessage());
        }
    }

    /**
     * 本体匹配检索
     */
    @PostMapping("/match")
    public Map<String, Object> match(@RequestBody Map<String, Object> body) {
        try {
            String query = (String) body.get("query");
            if (query == null || query.trim().isEmpty()) {
                return error(400, "查询文本不能为空");
            }

            Integer topK = body.get("topK") != null ? ((Number) body.get("topK")).intValue() : 10;
            Double threshold = body.get("threshold") != null ? ((Number) body.get("threshold")).doubleValue() : 0.6;

            // 扩充查询
            ExpandedText expanded = expansionService.expandQuery(query);

            // 匹配实体
            List<EntityMatch> matches = matchService.match(query, topK, threshold);

            Map<String, Object> result = new HashMap<>();
            result.put("query", query);
            result.put("expandedQuery", expanded.getExpandedText());
            result.put("matches", matches);
            result.put("totalMatches", matches.size());

            return success(result);
        } catch (Exception e) {
            log.error("匹配失败", e);
            return error(500, "匹配失败: " + e.getMessage());
        }
    }

    /**
     * 健康检查（含 OntModel 状态）
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> info = new HashMap<>();
        info.put("status", "running");
        info.put("modelVersion", modelManager.getLoadedVersion());
        info.put("dbVersion", modelManager.getDbVersion());
        info.put("rebuildCount", modelManager.getRebuildCount());
        info.put("modelReady", modelManager.getLoadedVersion() >= 0);
        return success(info);
    }

    /**
     * 强制重建 OntModel（admin 端修改本体数据后调用）
     */
    @PostMapping("/rebuild")
    public Map<String, Object> rebuild() {
        int size = modelManager.rebuild();
        Map<String, Object> result = new HashMap<>();
        result.put("success", size >= 0);
        result.put("statementCount", Math.max(0, size));
        result.put("version", modelManager.getLoadedVersion());
        return success(result);
    }

    /**
     * 获取 OntModel 统计信息
     */
    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return success(modelManager.getStats());
    }

    /**
     * 列出所有 OWL 类
     */
    @GetMapping("/classes")
    public Map<String, Object> listClasses() {
        try {
            org.apache.jena.ontology.OntModel model = modelManager.getModel();
            if (model == null) {
                return error(500, "OntModel 未就绪");
            }

            List<Map<String, Object>> classes = new ArrayList<>();
            org.apache.jena.util.iterator.ExtendedIterator<org.apache.jena.ontology.OntClass> iter = model.listClasses();

            while (iter.hasNext()) {
                org.apache.jena.ontology.OntClass ontClass = iter.next();
                if (ontClass.isAnon()) continue; // 跳过匿名类

                Map<String, Object> classInfo = new HashMap<>();
                classInfo.put("uri", ontClass.getURI());
                classInfo.put("localName", ontClass.getLocalName());

                // rdfs:label 优先中文
                String label = ontClass.getLabel("zh");
                if (label == null) label = ontClass.getLabel(null);
                classInfo.put("label", label != null ? label : ontClass.getLocalName());

                // rdfs:comment
                String comment = ontClass.getComment(null);
                classInfo.put("comment", comment);

                // 父类
                List<String> superClasses = new ArrayList<>();
                org.apache.jena.util.iterator.ExtendedIterator<org.apache.jena.ontology.OntClass> superIter = ontClass.listSuperClasses(true);
                while (superIter.hasNext()) {
                    org.apache.jena.ontology.OntClass superClass = superIter.next();
                    if (!superClass.isAnon() && superClass.getURI() != null) {
                        superClasses.add(superClass.getLocalName());
                    }
                }
                classInfo.put("superClasses", superClasses);

                classes.add(classInfo);
            }

            return success(classes);
        } catch (Exception e) {
            log.error("列出 OWL 类失败", e);
            return error(500, "查询失败: " + e.getMessage());
        }
    }

    /**
     * 列出所有 OWL 属性
     */
    @GetMapping("/properties")
    public Map<String, Object> listProperties() {
        try {
            org.apache.jena.ontology.OntModel model = modelManager.getModel();
            if (model == null) {
                return error(500, "OntModel 未就绪");
            }

            List<Map<String, Object>> properties = new ArrayList<>();
            org.apache.jena.util.iterator.ExtendedIterator<org.apache.jena.ontology.OntProperty> iter = model.listAllOntProperties();

            while (iter.hasNext()) {
                org.apache.jena.ontology.OntProperty prop = iter.next();
                if (prop.isAnon()) continue;

                Map<String, Object> propInfo = new HashMap<>();
                propInfo.put("uri", prop.getURI());
                propInfo.put("localName", prop.getLocalName());

                // rdfs:label 优先中文
                String label = prop.getLabel("zh");
                if (label == null) label = prop.getLabel(null);
                propInfo.put("label", label != null ? label : prop.getLocalName());

                // 属性类型
                if (prop.isObjectProperty()) {
                    propInfo.put("type", "ObjectProperty");
                } else if (prop.isDatatypeProperty()) {
                    propInfo.put("type", "DatatypeProperty");
                } else {
                    propInfo.put("type", "Property");
                }

                // Domain
                List<String> domains = new ArrayList<>();
                org.apache.jena.util.iterator.ExtendedIterator<? extends org.apache.jena.ontology.OntResource> domainIter = prop.listDomain();
                while (domainIter.hasNext()) {
                    org.apache.jena.ontology.OntResource domain = domainIter.next();
                    if (!domain.isAnon() && domain.getURI() != null) {
                        domains.add(domain.getLocalName());
                    }
                }
                propInfo.put("domains", domains);

                // Range
                List<String> ranges = new ArrayList<>();
                org.apache.jena.util.iterator.ExtendedIterator<? extends org.apache.jena.ontology.OntResource> rangeIter = prop.listRange();
                while (rangeIter.hasNext()) {
                    org.apache.jena.ontology.OntResource range = rangeIter.next();
                    if (!range.isAnon() && range.getURI() != null) {
                        ranges.add(range.getLocalName());
                    }
                }
                propInfo.put("ranges", ranges);

                properties.add(propInfo);
            }

            return success(properties);
        } catch (Exception e) {
            log.error("列出 OWL 属性失败", e);
            return error(500, "查询失败: " + e.getMessage());
        }
    }

    /**
     * 查看某个类的详细信息
     */
    @GetMapping("/class/{localName}")
    public Map<String, Object> getClassDetail(@PathVariable String localName) {
        try {
            org.apache.jena.ontology.OntModel model = modelManager.getModel();
            if (model == null) {
                return error(500, "OntModel 未就绪");
            }

            // 遍历所有已注册命名空间前缀查找类（不能硬编码单一 ns，业务命名空间不同）
            org.apache.jena.ontology.OntClass ontClass = null;
            java.util.Map<String, String> prefixMap = model.getNsPrefixMap();
            for (String pfxUri : prefixMap.values()) {
                org.apache.jena.ontology.OntClass candidate = model.getOntClass(pfxUri + localName);
                if (candidate != null) {
                    ontClass = candidate;
                    break;
                }
            }
            // 兜底：全量扫描（处理未注册前缀的情况）
            if (ontClass == null) {
                org.apache.jena.util.iterator.ExtendedIterator<org.apache.jena.ontology.OntClass> allIter = model.listClasses();
                while (allIter.hasNext()) {
                    org.apache.jena.ontology.OntClass c = allIter.next();
                    if (!c.isAnon() && localName.equals(c.getLocalName())) {
                        ontClass = c;
                        break;
                    }
                }
            }

            if (ontClass == null) {
                return error(404, "类不存在: " + localName);
            }

            Map<String, Object> detail = new HashMap<>();
            detail.put("uri", ontClass.getURI());
            detail.put("localName", ontClass.getLocalName());
            String detailLabel = ontClass.getLabel("zh");
            if (detailLabel == null) detailLabel = ontClass.getLabel(null);
            detail.put("label", detailLabel);
            detail.put("comment", ontClass.getComment(null));

            // 父类
            List<String> superClasses = new ArrayList<>();
            org.apache.jena.util.iterator.ExtendedIterator<org.apache.jena.ontology.OntClass> superIter = ontClass.listSuperClasses(true);
            while (superIter.hasNext()) {
                org.apache.jena.ontology.OntClass superClass = superIter.next();
                if (!superClass.isAnon() && superClass.getURI() != null) {
                    superClasses.add(superClass.getLocalName());
                }
            }
            detail.put("superClasses", superClasses);

            // 子类
            List<String> subClasses = new ArrayList<>();
            org.apache.jena.util.iterator.ExtendedIterator<org.apache.jena.ontology.OntClass> subIter = ontClass.listSubClasses(true);
            while (subIter.hasNext()) {
                org.apache.jena.ontology.OntClass subClass = subIter.next();
                if (!subClass.isAnon() && subClass.getURI() != null) {
                    subClasses.add(subClass.getLocalName());
                }
            }
            detail.put("subClasses", subClasses);

            // 关联属性（domain 为此类的属性）
            List<Map<String, Object>> properties = new ArrayList<>();
            org.apache.jena.util.iterator.ExtendedIterator<org.apache.jena.ontology.OntProperty> propIter = model.listAllOntProperties();
            while (propIter.hasNext()) {
                org.apache.jena.ontology.OntProperty prop = propIter.next();
                if (prop.isAnon()) continue;

                org.apache.jena.util.iterator.ExtendedIterator<? extends org.apache.jena.ontology.OntResource> domainIter = prop.listDomain();
                while (domainIter.hasNext()) {
                    org.apache.jena.ontology.OntResource domain = domainIter.next();
                    if (domain.equals(ontClass)) {
                        Map<String, Object> propInfo = new HashMap<>();
                        propInfo.put("localName", prop.getLocalName());
                        String pLabel = prop.getLabel("zh");
                        if (pLabel == null) pLabel = prop.getLabel(null);
                        propInfo.put("label", pLabel);
                        propInfo.put("type", prop.isObjectProperty() ? "ObjectProperty" :
                                           prop.isDatatypeProperty() ? "DatatypeProperty" : "Property");
                        properties.add(propInfo);
                        break;
                    }
                }
            }
            detail.put("properties", properties);

            return success(detail);
        } catch (Exception e) {
            log.error("查询类详情失败: {}", localName, e);
            return error(500, "查询失败: " + e.getMessage());
        }
    }

    /**
     * 列出所有枚举类型
     */
    @GetMapping("/enums")
    public Map<String, Object> listEnums() {
        try {
            org.apache.jena.ontology.OntModel model = modelManager.getModel();
            if (model == null) {
                return error(500, "OntModel 未就绪");
            }

            List<Map<String, Object>> enums = new ArrayList<>();
            org.apache.jena.util.iterator.ExtendedIterator<org.apache.jena.ontology.OntClass> iter = model.listClasses();

            while (iter.hasNext()) {
                org.apache.jena.ontology.OntClass ontClass = iter.next();
                if (ontClass.isAnon() || ontClass.getURI() == null) continue;

                // 只获取枚举类型（URI 包含 Enum_ 前缀）
                if (!ontClass.getLocalName().startsWith("Enum_")) continue;

                Map<String, Object> enumInfo = new HashMap<>();
                enumInfo.put("uri", ontClass.getURI());
                enumInfo.put("localName", ontClass.getLocalName().substring(5)); // 去掉 Enum_ 前缀

                String label = ontClass.getLabel("zh");
                if (label == null) label = ontClass.getLabel(null);
                enumInfo.put("label", label != null ? label : enumInfo.get("localName"));

                String comment = ontClass.getComment("zh");
                if (comment == null) comment = ontClass.getComment(null);
                enumInfo.put("comment", comment);

                // 获取枚举类型（general_single/general_multi 等）
                org.apache.jena.rdf.model.Statement enumTypeStmt = ontClass.getProperty(
                        model.getProperty(com.beiktech.bontolink.ontology.service.OntologyModelManager.NS_PREFIX + "enumType"));
                if (enumTypeStmt != null) {
                    enumInfo.put("enumType", enumTypeStmt.getString());
                }

                enums.add(enumInfo);
            }

            return success(enums);
        } catch (Exception e) {
            log.error("查询枚举列表失败", e);
            return error(500, "查询失败: " + e.getMessage());
        }
    }

    /**
     * 查询指定枚举的所有枚举项
     */
    @GetMapping("/enum/{enumName}/items")
    public Map<String, Object> listEnumItems(@PathVariable String enumName) {
        try {
            org.apache.jena.ontology.OntModel model = modelManager.getModel();
            if (model == null) {
                return error(500, "OntModel 未就绪");
            }

            String enumClassUri = com.beiktech.bontolink.ontology.service.OntologyModelManager.NS_PREFIX + "Enum_" + enumName;
            org.apache.jena.ontology.OntClass enumClass = model.getOntClass(enumClassUri);

            if (enumClass == null) {
                return error(404, "枚举类型不存在: " + enumName);
            }

            List<Map<String, Object>> items = new ArrayList<>();
            org.apache.jena.util.iterator.ExtendedIterator<? extends org.apache.jena.rdf.model.Resource> iter =
                    enumClass.listInstances();

            while (iter.hasNext()) {
                org.apache.jena.rdf.model.Resource res = iter.next();
                if (!res.canAs(org.apache.jena.ontology.Individual.class)) continue;
                org.apache.jena.ontology.Individual individual = res.as(org.apache.jena.ontology.Individual.class);

                Map<String, Object> itemInfo = new HashMap<>();
                itemInfo.put("uri", individual.getURI());

                String label = individual.getLabel("zh");
                if (label == null) label = individual.getLabel(null);
                itemInfo.put("label", label);

                // 获取 code
                org.apache.jena.rdf.model.Statement codeStmt = individual.getProperty(
                        model.getProperty(com.beiktech.bontolink.ontology.service.OntologyModelManager.NS_PREFIX + "code"));
                if (codeStmt != null) {
                    itemInfo.put("code", codeStmt.getString());
                }

                // 获取 apiName
                org.apache.jena.rdf.model.Statement apiNameStmt = individual.getProperty(
                        model.getProperty(com.beiktech.bontolink.ontology.service.OntologyModelManager.NS_PREFIX + "apiName"));
                if (apiNameStmt != null) {
                    itemInfo.put("apiName", apiNameStmt.getString());
                }

                items.add(itemInfo);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("enumName", enumName);
            result.put("items", items);
            result.put("itemCount", items.size());

            return success(result);
        } catch (Exception e) {
            log.error("查询枚举项失败: {}", enumName, e);
            return error(500, "查询失败: " + e.getMessage());
        }
    }

    /**
     * 列出所有值类型
     */
    @GetMapping("/value-types")
    public Map<String, Object> listValueTypes() {
        try {
            org.apache.jena.ontology.OntModel model = modelManager.getModel();
            if (model == null) {
                return error(500, "OntModel 未就绪");
            }

            List<Map<String, Object>> valueTypes = new ArrayList<>();
            String nsPrefix = com.beiktech.bontolink.ontology.service.OntologyModelManager.NS_PREFIX;

            // 遍历所有语句，查找值类型资源
            org.apache.jena.rdf.model.ResIterator resIter = model.listSubjects();

            while (resIter.hasNext()) {
                org.apache.jena.rdf.model.Resource resource = resIter.next();
                if (!resource.isURIResource()) continue;
                String localName = resource.getLocalName();
                if (localName == null || !localName.startsWith("ValueType_")) continue;

                Map<String, Object> vtInfo = new HashMap<>();
                vtInfo.put("uri", resource.getURI());
                vtInfo.put("localName", localName.substring(10)); // 去掉 ValueType_ 前缀

                // 尝试获取标签
                org.apache.jena.rdf.model.Statement labelStmt = resource.getProperty(
                        model.getProperty("http://www.w3.org/2000/01/rdf-schema#label"));
                if (labelStmt != null && labelStmt.getObject().isLiteral()) {
                    vtInfo.put("label", labelStmt.getString());
                }

                // 获取 baseType
                org.apache.jena.rdf.model.Statement baseTypeStmt = resource.getProperty(
                        model.getProperty(nsPrefix + "baseType"));
                if (baseTypeStmt != null) {
                    vtInfo.put("baseType", baseTypeStmt.getString());
                }

                // 获取 constraintType
                org.apache.jena.rdf.model.Statement constraintTypeStmt = resource.getProperty(
                        model.getProperty(nsPrefix + "constraintType"));
                if (constraintTypeStmt != null) {
                    vtInfo.put("constraintType", constraintTypeStmt.getString());
                }

                // 获取关联的枚举
                org.apache.jena.rdf.model.Statement refEnumStmt = resource.getProperty(
                        model.getProperty(nsPrefix + "refersToEnum"));
                if (refEnumStmt != null && refEnumStmt.getObject().isResource()) {
                    org.apache.jena.rdf.model.Resource enumRes = refEnumStmt.getResource();
                    vtInfo.put("enumUri", enumRes.getURI());
                    vtInfo.put("enumName", enumRes.getLocalName().substring(5)); // 去掉 Enum_ 前缀
                }

                valueTypes.add(vtInfo);
            }

            return success(valueTypes);
        } catch (Exception e) {
            log.error("查询值类型列表失败", e);
            return error(500, "查询失败: " + e.getMessage());
        }
    }

    private Map<String, Object> success(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 0);
        response.put("msg", "success");
        response.put("data", data);
        return response;
    }

    private Map<String, Object> error(int code, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", code);
        response.put("msg", message);
        response.put("data", null);
        return response;
    }
}
