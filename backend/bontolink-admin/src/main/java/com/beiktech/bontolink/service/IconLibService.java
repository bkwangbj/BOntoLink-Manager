package com.beiktech.bontolink.service;

import com.beiktech.bontolink.data.entity.BizCategory;
import com.beiktech.bontolink.data.entity.IconLibGroup;
import com.beiktech.bontolink.data.entity.IconLibIcon;
import com.beiktech.bontolink.data.mapper.BizCategoryMapper;
import com.beiktech.bontolink.data.mapper.IconLibMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 图标库业务服务：管理图标分组（两级目录）与图标条目，并在启动时按行业分类结构自动种入演示图标。
 */
@Service
public class IconLibService {
    private static final Logger log = LoggerFactory.getLogger(IconLibService.class);

    @Autowired private IconLibMapper mapper;
    @Autowired private BizCategoryMapper categoryMapper;

    /** 从 classpath:seed/icons-antd.json 加载到内存（启动时一次性） */
    private List<DemoIcon> ANTD_POOL = Collections.emptyList();
    private Map<String, String> ANTD_ZH = Collections.emptyMap();

    private void loadAntdPool() {
        if (!ANTD_POOL.isEmpty()) return;
        try {
            ClassPathResource res = new ClassPathResource("seed/icons-antd.json");
            if (!res.exists()) { log.warn("seed/icons-antd.json 不存在，跳过加载"); return; }
            try (InputStream is = res.getInputStream()) {
                JsonNode root = new ObjectMapper().readTree(is);
                JsonNode arr = root.path("icons");
                List<DemoIcon> pool = new ArrayList<>();
                Map<String, String> zh = new HashMap<>();
                if (arr.isArray()) {
                    for (JsonNode n : arr) {
                        String name = n.path("name").asText("");
                        if (name.isEmpty()) continue;
                        pool.add(new DemoIcon(name, n.path("viewBox").asText("0 0 1024 1024"), n.path("content").asText("")));
                        String z = n.path("zh").asText("");
                        if (!z.isEmpty()) zh.put(name, z);
                    }
                }
                ANTD_POOL = pool;
                ANTD_ZH = zh;
                log.info("Loaded antd icon pool: {} icons", pool.size());
            }
        } catch (Exception e) {
            log.warn("加载 antd 图标池失败: {}", e.getMessage());
        }
    }

    /** 把分组名映射到一组关键字（中英文），按命中数从高到低排序 antd 图标 */
    private List<String> keywordsForGroup(String name) {
        if (name == null) return Collections.emptyList();
        List<String> kws = new ArrayList<>();
        // 水利系
        if (name.contains("水土") || name.contains("保持")) { kws.add("environment"); kws.add("tree"); kws.add("aim"); }
        if (name.contains("水资源"))               { kws.add("cloud-download"); kws.add("dashboard"); kws.add("database"); }
        if (name.contains("水文"))                 { kws.add("line-chart"); kws.add("area-chart"); kws.add("dot-chart"); kws.add("fund"); }
        if (name.contains("水环境") || name.contains("环境")) { kws.add("environment"); kws.add("filter"); kws.add("experiment"); }
        if (name.contains("水生态") || name.contains("生态")) { kws.add("bug"); kws.add("apartment"); kws.add("partition"); }
        if (name.contains("水利工程") || name.contains("工程") || name.contains("库") || name.contains("闸")) { kws.add("tool"); kws.add("build"); kws.add("setting"); kws.add("cluster"); }
        if (name.contains("防洪") || name.contains("减灾") || name.contains("应急") || name.contains("防")) { kws.add("alert"); kws.add("warning"); kws.add("notification"); kws.add("safety"); kws.add("fire"); kws.add("thunderbolt"); }
        if (name.contains("农田") || name.contains("灌溉") || name.contains("农"))   { kws.add("crown"); kws.add("flag"); kws.add("gold"); kws.add("trophy"); }
        if (name.contains("城乡") || name.contains("供水") || name.contains("城"))   { kws.add("home"); kws.add("shop"); kws.add("bank"); kws.add("environment"); }
        if (name.contains("统计"))                                                  { kws.add("bar-chart"); kws.add("pie-chart"); kws.add("line-chart"); kws.add("area-chart"); kws.add("fund"); kws.add("dot-chart"); kws.add("heat-map"); kws.add("stock"); }
        // 行业
        if (name.contains("交通"))                                                  { kws.add("car"); kws.add("rocket"); kws.add("send"); kws.add("environment"); }
        if (name.contains("环保"))                                                  { kws.add("environment"); kws.add("filter"); kws.add("medicine-box"); kws.add("experiment"); }
        if (name.contains("林业") || name.contains("林"))                            { kws.add("apartment"); kws.add("partition"); kws.add("cluster"); kws.add("flag"); }
        if (name.contains("水利") && !name.contains("水利工程") && !name.contains("水利统计")) {
            kws.add("dashboard"); kws.add("cluster"); kws.add("database"); kws.add("setting");
        }
        if (name.contains("农业") || name.contains("乡村") || name.contains("农村"))  { kws.add("home"); kws.add("crown"); kws.add("gold"); kws.add("trophy"); kws.add("flag"); }
        // 兜底（任何未命中的关键字）
        if (kws.isEmpty()) { kws.add("folder"); kws.add("appstore"); kws.add("database"); kws.add("setting"); kws.add("tag"); }
        return kws;
    }

    /** 根据关键字列表从 antd 池里挑出若干图标（名称包含关键字即视为命中） */
    private List<DemoIcon> pickFromAntdPool(List<String> keywords, int limit) {
        if (ANTD_POOL.isEmpty()) return Collections.emptyList();
        LinkedHashMap<String, DemoIcon> hit = new LinkedHashMap<>();
        for (String kw : keywords) {
            for (DemoIcon di : ANTD_POOL) {
                if (di.name.contains(kw) && !hit.containsKey(di.name)) {
                    hit.put(di.name, di);
                    if (hit.size() >= limit) return new ArrayList<>(hit.values());
                }
            }
        }
        return new ArrayList<>(hit.values());
    }

    /** 应用就绪后自动为空分组补一批演示图标（幂等：已有图标的分组不动） */
    @EventListener(ApplicationReadyEvent.class)
    public void autoSeedDemoIconsOnReady() {
        try {
            Map<String, Object> r = seedDemoIcons(false);
            log.info("Auto-seed demo icons: created={} skippedGroups={}", r.get("created"), r.get("skippedGroups"));
        } catch (Exception e) {
            log.warn("Auto-seed demo icons failed: {}", e.getMessage());
        }
    }

    /** 列表：返回所有 group + 每组 icon 数,以及全部 icon(供前端按 groupId 索引) */
    /**
     * 查询全量数据：返回所有分组列表与图标列表，前端按 groupId 自行索引。
     */
    public Map<String, Object> all() {
        List<IconLibGroup> groups = mapper.listGroups();
        List<IconLibIcon> icons = mapper.listIcons();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("groups", groups);
        r.put("icons", icons);
        return r;
    }

    /**
     * 创建图标分组。
     * @param parentId 父组 ID，null 或空字符串表示顶级；最多支持两级目录
     * @param name     分组显示名称
     * @return 新建的分组实体（含生成的 id）
     */
    public IconLibGroup createGroup(String parentId, String name) {
        // 严格限制 2 级
        if (parentId != null && !parentId.isEmpty()) {
            IconLibGroup parent = mapper.findGroup(parentId);
            if (parent == null) throw new IllegalArgumentException("父节点不存在");
            if (parent.getParentId() != null && !parent.getParentId().isEmpty()) {
                throw new IllegalArgumentException("最多支持两级目录");
            }
        }
        IconLibGroup g = new IconLibGroup();
        // ID 格式: "ig-" + UUID
        g.setId("ig-" + UUID.randomUUID());
        g.setParentId(parentId == null || parentId.isEmpty() ? null : parentId);
        g.setName(name);
        g.setSort(0);
        g.setSource("manual");
        mapper.insertGroup(g);
        return g;
    }

    /**
     * 重命名图标分组。ID 不存在时返回 null（由 Controller 决定是否 404）。
     */
    public IconLibGroup renameGroup(String id, String name) {
        IconLibGroup g = mapper.findGroup(id);
        if (g == null) return null;
        g.setName(name);
        g.setSort(g.getSort() == null ? 0 : g.getSort());
        mapper.updateGroup(g);
        return g;
    }

    /** 删除组：连同子组与组内图标一并删除 */
    public void deleteGroup(String id) {
        // 收集自身 + 所有子组 id
        List<IconLibGroup> all = mapper.listGroups();
        Set<String> ids = new HashSet<>();
        ids.add(id);
        for (IconLibGroup g : all) {
            if (id.equals(g.getParentId())) ids.add(g.getId());
        }
        // 删除组内图标
        mapper.deleteIconsByGroups(ids);
        // 删除组本身
        mapper.deleteGroupBatch(ids);
    }

    /**
     * 向指定分组添加单个图标。
     * @param groupId 目标分组 ID
     * @param name    图标名称（建议中文，用于搜索/展示）
     * @param viewBox SVG viewBox，为空时默认 "0 0 1024 1024"
     * @param content SVG path 内容字符串
     */
    public IconLibIcon addIcon(String groupId, String name, String viewBox, String content) {
        IconLibGroup g = mapper.findGroup(groupId);
        if (g == null) throw new IllegalArgumentException("目标分组不存在");
        IconLibIcon ic = new IconLibIcon();
        ic.setId("ii-" + UUID.randomUUID());
        ic.setGroupId(groupId);
        ic.setName(name);
        ic.setViewBox(viewBox == null || viewBox.isEmpty() ? "0 0 1024 1024" : viewBox);
        ic.setContent(content);
        ic.setSort(0);
        mapper.insertIcon(ic);
        return ic;
    }

    /** 删除单个图标。 */
    public void deleteIcon(String id) { mapper.deleteIcon(id); }

    /** 批量删除图标，ids 为空时直接返回。 */
    public void deleteIconBatch(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) return;
        mapper.deleteIconBatch(ids);
    }

    /**
     * 按 行业 → 领域 结构种子建目录(仅在表为空时执行,或可手动 force=true 重新建)
     * - 一级:行业(category_type=1)
     * - 二级:领域(category_type=2)
     */
    public Map<String, Object> seedFromCategory(boolean force) {
        if (!force && mapper.countGroups() > 0) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("created", 0);
            r.put("skipped", true);
            return r;
        }
        List<BizCategory> all = categoryMapper.listAll();
        // 用 id 索引
        Map<String, BizCategory> byId = new HashMap<>();
        for (BizCategory c : all) byId.put(c.getId(), c);

        // 1) 行业列表
        List<BizCategory> industries = all.stream()
                .filter(c -> c.getCategoryType() != null && c.getCategoryType() == 1)
                .sorted(Comparator.comparing(c -> c.getSort() == null ? 0 : c.getSort()))
                .collect(Collectors.toList());

        // 2) 创建行业(顶级 group)与领域(二级 group)，并记录 categoryId -> groupId 映射，供领域层回溯父组使用
        int created = 0;
        Map<String, String> categoryId2GroupId = new HashMap<>();
        for (BizCategory ind : industries) {
            IconLibGroup g = new IconLibGroup();
            g.setId("ig-" + UUID.randomUUID());
            g.setParentId(null);
            g.setName(ind.getRdfsLabel() == null || ind.getRdfsLabel().isEmpty() ? ind.getCategoryCode() : ind.getRdfsLabel());
            g.setSort(ind.getSort() == null ? 0 : ind.getSort());
            g.setSource("seed");
            mapper.insertGroup(g);
            categoryId2GroupId.put(ind.getId(), g.getId());
            created++;
        }
        // 领域
        List<BizCategory> domains = all.stream()
                .filter(c -> c.getCategoryType() != null && c.getCategoryType() == 2)
                .sorted(Comparator.comparing(c -> c.getSort() == null ? 0 : c.getSort()))
                .collect(Collectors.toList());
        for (BizCategory dom : domains) {
            String parentGroupId = null;
            // 向上回溯找行业
            BizCategory cur = dom;
            while (cur != null && cur.getParentId() != null && !"0".equals(cur.getParentId())) {
                BizCategory p = byId.get(cur.getParentId());
                if (p == null) break;
                if (p.getCategoryType() != null && p.getCategoryType() == 1) {
                    parentGroupId = categoryId2GroupId.get(p.getId());
                    break;
                }
                cur = p;
            }
            if (parentGroupId == null) continue; // 没找到行业则跳过
            IconLibGroup g = new IconLibGroup();
            g.setId("ig-" + UUID.randomUUID());
            g.setParentId(parentGroupId);
            g.setName(dom.getRdfsLabel() == null || dom.getRdfsLabel().isEmpty() ? dom.getCategoryCode() : dom.getRdfsLabel());
            g.setSort(dom.getSort() == null ? 0 : dom.getSort());
            g.setSource("seed");
            mapper.insertGroup(g);
            created++;
        }
        // 建完目录后立即按分组关键字种一批演示图标（仅在 icon 表为空时执行）
        if (mapper.countIcons() == 0) {
            try { seedDemoIcons(false); } catch (Exception ignore) { }
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("created", created);
        r.put("skipped", false);
        return r;
    }

    // ============================================================
    //                   Demo 图标按分组结构种入
    // ============================================================

    /**
     * 按分组名称关键字，向 icon_lib_icon 表批量种入演示图标。
     * - 默认仅对图标数 = 0 的分组生效；force=true 会覆盖（先清空再种）。
     * - 顶级（行业）分组：种入 6 ~ 8 个通用图标
     * - 二级（领域）分组：按关键字匹配若干主题图标
     */
    public Map<String, Object> seedDemoIcons(boolean force) {
        loadAntdPool();
        List<IconLibGroup> groups = mapper.listGroups();
        int total = 0, skippedGroups = 0;

        for (IconLibGroup g : groups) {
            int existing = mapper.countIconsByGroup(g.getId());
            if (existing > 0 && !force) { skippedGroups++; continue; }
            if (force && existing > 0) {
                // 清空（仅本组）
                List<IconLibIcon> icons = mapper.listIconsByGroup(g.getId());
                Set<String> ids = new HashSet<>();
                for (IconLibIcon ic : icons) ids.add(ic.getId());
                if (!ids.isEmpty()) mapper.deleteIconBatch(ids);
            }
            List<DemoIcon> picks = pickIconsForGroup(g);
            int sort = 0;
            for (DemoIcon p : picks) {
                IconLibIcon ic = new IconLibIcon();
                ic.setId("ii-" + UUID.randomUUID());
                ic.setGroupId(g.getId());
                ic.setName(p.name);
                ic.setViewBox(p.viewBox);
                ic.setContent(p.content);
                ic.setSort(sort++);
                mapper.insertIcon(ic);
                total++;
            }
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("created", total);
        r.put("skippedGroups", skippedGroups);
        r.put("force", force);
        return r;
    }

    /** 按分组名称关键字筛选要种入的图标集合：优先用 antd 池（开源 Apache-2.0），未命中则回退到手绘 fallback */
    private List<DemoIcon> pickIconsForGroup(IconLibGroup g) {
        String name = g.getName() == null ? "" : g.getName();
        boolean isTop = g.getParentId() == null || g.getParentId().isEmpty();

        // 1) 优先从 antd 池里挑（领域 8~12 个，行业 12~16 个）
        List<String> kws = keywordsForGroup(name);
        int limit = isTop ? 16 : 12;
        List<DemoIcon> out = new ArrayList<>(pickFromAntdPool(kws, limit));

        // 池为空 / 不足，使用手绘兜底
        if (out.size() < 5) {
            boolean water     = name.contains("水") || name.contains("湖") || name.contains("河") || name.contains("库");
            boolean ecology   = name.contains("生态") || name.contains("环境") || name.contains("环保") || name.contains("林");
            boolean transport = name.contains("交通");
            boolean emergency = name.contains("应急") || name.contains("防");
            boolean agri      = name.contains("农") || name.contains("田") || name.contains("灌");
            boolean engineer  = name.contains("工程") || name.contains("闸");
            boolean stat      = name.contains("统计");
            boolean supply    = name.contains("供水");
            boolean soil      = name.contains("水土") || name.contains("保持");

            if (water)     { out.add(I_WATER_DROP); out.add(I_WAVE); out.add(I_RIVER); out.add(I_FAUCET); }
            if (soil)      { out.add(I_MOUNTAIN); out.add(I_TREE); out.add(I_SHIELD); }
            if (engineer)  { out.add(I_DAM); out.add(I_GEAR); out.add(I_BUILDING); }
            if (supply)    { out.add(I_PIPELINE); out.add(I_TAP); out.add(I_HOUSE); }
            if (ecology)   { out.add(I_LEAF); out.add(I_TREE); out.add(I_RECYCLE); }
            if (transport) { out.add(I_CAR); out.add(I_TRUCK); out.add(I_ROAD); out.add(I_SHIP); }
            if (emergency) { out.add(I_BELL); out.add(I_SHIELD); out.add(I_WARNING); out.add(I_FIRE); }
            if (agri)      { out.add(I_PLANT); out.add(I_WHEAT); out.add(I_SUN); out.add(I_TRACTOR); }
            if (stat)      { out.add(I_CHART_BAR); out.add(I_CHART_PIE); out.add(I_CHART_LINE); }
            if (isTop)     { out.add(I_DATABASE); out.add(I_NETWORK); out.add(I_LAYERS); out.add(I_GLOBE); }
            if (out.isEmpty()) { out.add(I_FOLDER); out.add(I_DATABASE); out.add(I_TAG); out.add(I_GEAR); out.add(I_GLOBE); }
        }
        // 去重 + 限制
        LinkedHashSet<DemoIcon> set = new LinkedHashSet<>(out);
        List<DemoIcon> list = new ArrayList<>(set);
        return list.subList(0, Math.min(list.size(), limit));
    }

    private static final class DemoIcon {
        final String name; final String viewBox; final String content;
        DemoIcon(String name, String content) { this.name = name; this.viewBox = "0 0 24 24"; this.content = content; }
        DemoIcon(String name, String viewBox, String content) { this.name = name; this.viewBox = viewBox; this.content = content; }
        @Override public boolean equals(Object o) { return o instanceof DemoIcon d && name.equals(d.name); }
        @Override public int hashCode() { return name.hashCode(); }
    }

    /* —— 通用 SVG 图标库（24x24，currentColor） —— */
    private static final DemoIcon I_WATER_DROP = new DemoIcon("水滴",
            "<path d='M12 2 C8 8 4 12 4 16 a8 8 0 1 0 16 0 C20 12 16 8 12 2 Z' fill='currentColor'/>");
    private static final DemoIcon I_WAVE = new DemoIcon("波浪",
            "<path d='M2 12 Q5 8 8 12 T14 12 T20 12 T26 12' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round'/>" +
            "<path d='M2 17 Q5 13 8 17 T14 17 T20 17 T26 17' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round'/>");
    private static final DemoIcon I_RIVER = new DemoIcon("河流",
            "<path d='M4 4 C8 8 6 12 10 14 S16 18 14 22' fill='none' stroke='currentColor' stroke-width='2.5' stroke-linecap='round'/>" +
            "<path d='M10 4 C12 6 14 8 12 12' fill='none' stroke='currentColor' stroke-width='1.5' stroke-linecap='round' opacity='0.5'/>");
    private static final DemoIcon I_DAM = new DemoIcon("水坝",
            "<path d='M2 18 L22 18 L22 21 L2 21 Z' fill='currentColor'/>" +
            "<path d='M6 4 L8 4 L8 18 L6 18 Z M11 6 L13 6 L13 18 L11 18 Z M16 4 L18 4 L18 18 L16 18 Z' fill='currentColor'/>");
    private static final DemoIcon I_FAUCET = new DemoIcon("水龙头",
            "<path d='M8 3 H16 V7 H8 Z M10 7 H14 V11 H10 Z M11 11 H13 V15 H11 Z' fill='currentColor'/>" +
            "<circle cx='12' cy='18' r='3' fill='none' stroke='currentColor' stroke-width='2'/>");
    private static final DemoIcon I_TAP = new DemoIcon("水阀",
            "<rect x='10' y='10' width='4' height='12' fill='currentColor'/>" +
            "<circle cx='12' cy='6' r='4' fill='none' stroke='currentColor' stroke-width='2'/>" +
            "<line x1='12' y1='2' x2='12' y2='10' stroke='currentColor' stroke-width='2'/>");
    private static final DemoIcon I_PIPELINE = new DemoIcon("管道",
            "<path d='M3 10 H10 V6 H14 V10 H21 V14 H14 V18 H10 V14 H3 Z' fill='currentColor'/>");
    private static final DemoIcon I_HOUSE = new DemoIcon("房屋",
            "<path d='M3 11 L12 3 L21 11 L21 21 L14 21 V15 H10 V21 L3 21 Z' fill='currentColor'/>");

    private static final DemoIcon I_MOUNTAIN = new DemoIcon("山",
            "<path d='M2 20 L8 8 L13 16 L17 11 L22 20 Z' fill='currentColor'/>");
    private static final DemoIcon I_TREE = new DemoIcon("树",
            "<path d='M12 2 L6 12 L9 12 L4 19 L10 19 L10 22 L14 22 L14 19 L20 19 L15 12 L18 12 Z' fill='currentColor'/>");
    private static final DemoIcon I_LEAF = new DemoIcon("叶",
            "<path d='M20 4 C12 4 4 8 4 16 a6 6 0 0 0 6 6 C18 22 22 14 20 4 Z' fill='currentColor'/>" +
            "<path d='M9 18 L17 8' stroke='#fff' stroke-width='1.5'/>");
    private static final DemoIcon I_RECYCLE = new DemoIcon("循环",
            "<path d='M7 7 L4 11 L7 15 M5 11 H15 a4 4 0 0 1 4 4' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round'/>" +
            "<path d='M17 17 L20 13 L17 9 M19 13 H9 a4 4 0 0 1 -4 -4' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round'/>");

    private static final DemoIcon I_CAR = new DemoIcon("汽车",
            "<path d='M3 16 V12 L5 8 H19 L21 12 V16 Z M5 16 H19 V18 H17 V20 H15 V18 H9 V20 H7 V18 H5 Z' fill='currentColor'/>" +
            "<circle cx='8' cy='14' r='1.5' fill='#fff'/><circle cx='16' cy='14' r='1.5' fill='#fff'/>");
    private static final DemoIcon I_TRUCK = new DemoIcon("货车",
            "<path d='M2 8 H14 V17 H2 Z M14 11 H18 L21 14 V17 H14 Z' fill='currentColor'/>" +
            "<circle cx='6' cy='19' r='2' fill='currentColor'/><circle cx='18' cy='19' r='2' fill='currentColor'/>");
    private static final DemoIcon I_ROAD = new DemoIcon("公路",
            "<path d='M3 21 L9 3 H15 L21 21 Z' fill='currentColor'/>" +
            "<path d='M12 5 V8 M12 11 V14 M12 17 V20' stroke='#fff' stroke-width='1.5'/>");
    private static final DemoIcon I_SHIP = new DemoIcon("船",
            "<path d='M2 17 L4 21 H20 L22 17 Z' fill='currentColor'/>" +
            "<path d='M5 11 H19 V15 H5 Z' fill='currentColor'/>" +
            "<path d='M12 3 V11 M12 5 H17' stroke='currentColor' stroke-width='2'/>");

    private static final DemoIcon I_BELL = new DemoIcon("警铃",
            "<path d='M12 2 a6 6 0 0 1 6 6 V13 L20 16 H4 L6 13 V8 a6 6 0 0 1 6 -6 Z' fill='currentColor'/>" +
            "<path d='M10 18 a2 2 0 0 0 4 0' fill='none' stroke='currentColor' stroke-width='2'/>");
    private static final DemoIcon I_SHIELD = new DemoIcon("盾牌",
            "<path d='M12 2 L20 5 V12 C20 17 16 21 12 22 C8 21 4 17 4 12 V5 Z' fill='currentColor'/>" +
            "<path d='M9 12 L11 14 L15 9' stroke='#fff' stroke-width='2' fill='none'/>");
    private static final DemoIcon I_WARNING = new DemoIcon("警告",
            "<path d='M12 2 L22 20 H2 Z' fill='currentColor'/>" +
            "<rect x='11' y='9' width='2' height='6' fill='#fff'/>" +
            "<circle cx='12' cy='17' r='1' fill='#fff'/>");
    private static final DemoIcon I_FIRE = new DemoIcon("火",
            "<path d='M12 2 C 14 6 10 8 12 12 C14 8 17 11 17 14 a5 5 0 0 1 -10 0 C7 10 10 6 12 2 Z' fill='currentColor'/>");

    private static final DemoIcon I_PLANT = new DemoIcon("植物",
            "<path d='M12 22 V10 M12 14 C8 14 6 11 6 7 C9 7 12 9 12 13 M12 12 C16 12 18 9 18 5 C15 5 12 7 12 11' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round'/>");
    private static final DemoIcon I_WHEAT = new DemoIcon("麦穗",
            "<path d='M12 22 V8' stroke='currentColor' stroke-width='2'/>" +
            "<path d='M12 8 L8 4 M12 8 L16 4 M12 12 L8 8 M12 12 L16 8 M12 16 L8 12 M12 16 L16 12' stroke='currentColor' stroke-width='2' stroke-linecap='round'/>");
    private static final DemoIcon I_SUN = new DemoIcon("太阳",
            "<circle cx='12' cy='12' r='4' fill='currentColor'/>" +
            "<path d='M12 2 V5 M12 19 V22 M2 12 H5 M19 12 H22 M5 5 L7 7 M17 17 L19 19 M5 19 L7 17 M17 7 L19 5' stroke='currentColor' stroke-width='2' stroke-linecap='round'/>");
    private static final DemoIcon I_TRACTOR = new DemoIcon("拖拉机",
            "<rect x='3' y='10' width='10' height='7' fill='currentColor'/>" +
            "<rect x='13' y='12' width='6' height='5' fill='currentColor'/>" +
            "<circle cx='7' cy='19' r='2.5' fill='currentColor'/>" +
            "<circle cx='17' cy='19' r='1.5' fill='currentColor'/>");

    private static final DemoIcon I_CHART_BAR = new DemoIcon("柱状图",
            "<path d='M4 20 V14 H7 V20 Z M10 20 V8 H13 V20 Z M16 20 V11 H19 V20 Z' fill='currentColor'/>");
    private static final DemoIcon I_CHART_PIE = new DemoIcon("饼图",
            "<path d='M12 2 A10 10 0 0 1 22 12 H12 Z' fill='currentColor'/>" +
            "<circle cx='12' cy='12' r='10' fill='none' stroke='currentColor' stroke-width='2'/>");
    private static final DemoIcon I_CHART_LINE = new DemoIcon("折线图",
            "<path d='M3 20 L9 12 L13 16 L21 4' fill='none' stroke='currentColor' stroke-width='2.5' stroke-linecap='round' stroke-linejoin='round'/>" +
            "<circle cx='9' cy='12' r='2' fill='currentColor'/><circle cx='13' cy='16' r='2' fill='currentColor'/><circle cx='21' cy='4' r='2' fill='currentColor'/>");

    private static final DemoIcon I_DATABASE = new DemoIcon("数据库",
            "<ellipse cx='12' cy='5' rx='8' ry='3' fill='currentColor'/>" +
            "<path d='M4 5 V11 a8 3 0 0 0 16 0 V5' fill='currentColor' opacity='0.7'/>" +
            "<path d='M4 11 V17 a8 3 0 0 0 16 0 V11' fill='currentColor' opacity='0.4'/>");
    private static final DemoIcon I_NETWORK = new DemoIcon("网络",
            "<circle cx='12' cy='5' r='3' fill='currentColor'/>" +
            "<circle cx='5' cy='17' r='3' fill='currentColor'/>" +
            "<circle cx='19' cy='17' r='3' fill='currentColor'/>" +
            "<path d='M12 8 L5 14 M12 8 L19 14 M5 17 H19' stroke='currentColor' stroke-width='2'/>");
    private static final DemoIcon I_LAYERS = new DemoIcon("层叠",
            "<path d='M12 2 L22 7 L12 12 L2 7 Z' fill='currentColor'/>" +
            "<path d='M2 12 L12 17 L22 12' fill='none' stroke='currentColor' stroke-width='2'/>" +
            "<path d='M2 17 L12 22 L22 17' fill='none' stroke='currentColor' stroke-width='2'/>");
    private static final DemoIcon I_GLOBE = new DemoIcon("地球",
            "<circle cx='12' cy='12' r='10' fill='none' stroke='currentColor' stroke-width='2'/>" +
            "<ellipse cx='12' cy='12' rx='4' ry='10' fill='none' stroke='currentColor' stroke-width='2'/>" +
            "<line x1='2' y1='12' x2='22' y2='12' stroke='currentColor' stroke-width='2'/>");
    private static final DemoIcon I_BUILDING = new DemoIcon("建筑",
            "<rect x='5' y='3' width='14' height='18' fill='currentColor'/>" +
            "<rect x='8' y='6' width='2' height='2' fill='#fff'/><rect x='14' y='6' width='2' height='2' fill='#fff'/>" +
            "<rect x='8' y='11' width='2' height='2' fill='#fff'/><rect x='14' y='11' width='2' height='2' fill='#fff'/>" +
            "<rect x='10' y='16' width='4' height='5' fill='#fff'/>");
    private static final DemoIcon I_GEAR = new DemoIcon("齿轮",
            "<path d='M12 8 a4 4 0 1 0 0 8 a4 4 0 0 0 0 -8 Z M11 1 H13 V4 H11 Z M11 20 H13 V23 H11 Z M1 11 H4 V13 H1 Z M20 11 H23 V13 H20 Z M4 4 L6 6 M18 18 L20 20 M4 20 L6 18 M18 6 L20 4' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round'/>");
    private static final DemoIcon I_FOLDER = new DemoIcon("文件夹",
            "<path d='M3 7 V19 a2 2 0 0 0 2 2 H19 a2 2 0 0 0 2 -2 V9 a2 2 0 0 0 -2 -2 H12 L10 5 H5 a2 2 0 0 0 -2 2 Z' fill='currentColor'/>");
    private static final DemoIcon I_TAG = new DemoIcon("标签",
            "<path d='M2 12 L12 2 H22 V12 L12 22 Z' fill='currentColor'/>" +
            "<circle cx='17' cy='7' r='1.5' fill='#fff'/>");
}
