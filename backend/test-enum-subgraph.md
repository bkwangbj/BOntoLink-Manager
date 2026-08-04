# 测试枚举类型 subgraph 功能

## 测试步骤

1. **启动后端服务**
   ```bash
   cd backend
   mvn -q -DskipTests spring-boot:run
   ```

2. **等待服务就绪**
   ```bash
   until curl -sf http://localhost:8088/bontolink/api/health > /dev/null; do sleep 2; done
   ```

3. **测试 subgraph 接口（假设有枚举类型）**
   
   如果有枚举类型的向量数据，可以测试：
   ```bash
   curl -X POST http://localhost:8088/bontolink/api/tool/subgraph \
     -H "Content-Type: application/json" \
     -d '{
       "question": "行政区划",
       "nsCode": "w_wtr",
       "topK": 5,
       "threshold": 0.6
     }' | jq .
   ```

4. **预期结果**
   
   返回的 `classes` 数组中，如果包含枚举类型，应该有 `enumItems` 字段：
   ```json
   {
     "code": 200,
     "msg": "success",
     "data": {
       "success": true,
       "classes": [
         {
           "localName": "Enum_AdministrativeRegion",
           "uri": "http://bontolink.beiktech.com/ontology#Enum_AdministrativeRegion",
           "label": "行政区划",
           "enumItems": [
             {
               "uri": "http://bontolink.beiktech.com/ontology#Enum_AdministrativeRegion_110000",
               "localName": "Enum_AdministrativeRegion_110000",
               "label": "北京市",
               "code": "110000",
               "apiName": "beijing"
             }
           ]
         }
       ]
     }
   }
   ```

## 功能说明

### 新增特性

1. **从 Jena 本体查询枚举项**
   - 枚举类在本体中以 `Enum_{api_name}` 形式存在（例如：`Enum_AdministrativeRegion`）
   - 枚举项是枚举类的 Individual 实例
   - 通过 `JenaToolService.getEnumIndividuals(enumApiName)` 查询
   - 返回包含 uri, localName, label, code, apiName 的完整信息

2. **枚举项缓存**
   - 使用 `ConcurrentHashMap` 缓存枚举项数据
   - 首次查询时从 Jena 本体加载，后续直接返回缓存
   - 提供 `clearEnumItemsCache()` 和 `clearEnumItemsCache(enumApiName)` 方法清理缓存

3. **枚举类型识别**
   - 在 `subgraph` 方法中，当 `entity_type` 为 `"enum"` 时
   - 先查缓存，未命中再调用 Jena 查询
   - 查询成功后自动缓存结果

4. **返回格式增强**
   - 在 `classes` 列表中，枚举类型会额外包含 `enumItems` 字段
   - 枚举项包含从本体中提取的完整信息
   - 与本体结构完全一致

### 与数据库表的关系

- **本体中**：枚举类和枚举项都以 OWL 类和 Individual 形式存在
- **数据库中**：`ont_enum_types` 和 `ont_enum_items` 表存储原始数据
- **同步机制**：本体通过 `OntologyModelManager` 从数据库加载并构建
- **查询来源**：`subgraph` 接口从本体查询，确保与推理结果一致

### 缓存清理时机

在以下场景需要清理缓存：
- 本体重建后（`OntologyModelManager` 重建完成时）
- 枚举数据更新后（可选，因为本体重建会自动更新）

可以在对应的 Service 中调用：
```java
@Autowired
private SemanticExpandService semanticExpandService;

// 本体重建后清空全部缓存
semanticExpandService.clearEnumItemsCache();

// 或清空特定枚举的缓存
semanticExpandService.clearEnumItemsCache("AdministrativeRegion");
```

## 性能优化

- **缓存命中率**：同一枚举多次查询只需从 Jena 加载一次
- **并发安全**：使用 `ConcurrentHashMap` 保证线程安全
- **数据一致性**：从本体查询确保与推理结果一致
- **Jena 本身缓存**：OntModel 在内存中，Individual 查询已经很快

## 实现细节

### JenaToolService.getEnumIndividuals()

```java
// 枚举类 URI: http://bontolink.beiktech.com/ontology#Enum_{api_name}
String enumClassUri = NS_PREFIX + "Enum_" + enumApiName;
OntClass enumClass = model.getOntClass(enumClassUri);

// 查询该类的所有 Individual 实例
ExtendedIterator<? extends OntResource> instances = enumClass.listInstances(false);

// 提取每个 Individual 的属性：label, code, apiName
```

### SemanticExpandService.subgraph()

```java
// 1. 检测到枚举类型
if ("enum".equalsIgnoreCase(entityType)) {
    // 2. 先查缓存
    List<Map<String, Object>> items = enumItemsCache.get(apiName);
    
    // 3. 缓存未命中，调用 Jena 查询
    if (items == null) {
        Map<String, Object> enumResult = jenaToolService.getEnumIndividuals(apiName);
        items = enumResult.get("items");
        enumItemsCache.put(apiName, items);  // 缓存
    }
    
    // 4. 附加到返回的 class 对象上
    cls.put("enumItems", items);
}
```
