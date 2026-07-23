package com.beiktech.bontolink.service;

import com.beiktech.bontolink.data.entity.BizNamespace;
import com.beiktech.bontolink.data.entity.BizNamespaceVersion;
import com.beiktech.bontolink.data.mapper.BizNamespaceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * 命名空间业务服务：管理本体命名空间（BizNamespace）的 CRUD 及版本（BizNamespaceVersion）的创建与切换。
 */
@Service
public class NamespaceService {
    @Autowired private BizNamespaceMapper mapper;

    /** 查询所有命名空间列表。 */
    public List<BizNamespace> list() { return mapper.listAll(); }

    /** 按 nsCode 查询单个命名空间。 */
    public BizNamespace get(String code) { return mapper.findByCode(code); }

    /**
     * 创建命名空间，并自动生成初始版本（默认 "1.0"）设为当前版本。
     * - ID 格式："namespace-" + UUID
     * - status 默认 1（启用），currVersion 默认 "1.0"
     */
    public BizNamespace create(BizNamespace ns) {
        if (ns.getId() == null) ns.setId("namespace-" + UUID.randomUUID());
        if (ns.getStatus() == null) ns.setStatus(1);
        if (ns.getCurrVersion() == null) ns.setCurrVersion("1.0");
        mapper.insert(ns);
        // 新增命名空间时自动创建第一个版本，并设为当前版本
        BizNamespaceVersion first = new BizNamespaceVersion();
        first.setNsCode(ns.getNsCode());
        first.setVersion(ns.getCurrVersion());
        first.setUri(ns.getNsUri() != null ? ns.getNsUri() : "");
        first.setRdfsLabel((ns.getNsName() != null ? ns.getNsName() : ns.getNsCode()) + " v" + ns.getCurrVersion());
        createVersion(first);
        return ns;
    }

    /**
     * 更新命名空间基本信息，返回更新后的最新数据（按 nsCode 重新查询）。
     */
    public BizNamespace update(BizNamespace ns) {
        mapper.update(ns);
        return mapper.findByCode(ns.getNsCode());
    }

    /** 按 nsCode 查询该命名空间下所有版本列表。 */
    public List<BizNamespaceVersion> listVersions(String code) { return mapper.listVersions(code); }

    /**
     * 新建命名空间版本，并自动设为当前版本（同时清除同命名空间下其他版本的当前标记，并同步主表 curr_version）。
     * - ID 格式："namespace-v-" + UUID
     * - publishTime 为空时默认当前时间
     */
    public BizNamespaceVersion createVersion(BizNamespaceVersion v) {
        if (v.getId() == null) v.setId("namespace-v-" + UUID.randomUUID());
        if (v.getStatus() == null) v.setStatus(1);
        if (v.getPublishTime() == null) v.setPublishTime(java.time.LocalDateTime.now());
        // 新建版本默认成为当前版本：先清掉旧的当前标记，再插入并同步主表版本号
        v.setIsCurrent(1);
        mapper.clearCurrentByNs(v.getNsCode());
        mapper.insertVersion(v);
        mapper.updateCurrVersion(v.getNsCode(), v.getVersion());
        return v;
    }

    /** 设为当前版本：标记该版本为当前，其余取消，并同步命名空间主表 curr_version */
    public BizNamespaceVersion setCurrentVersion(String id) {
        BizNamespaceVersion v = mapper.findVersionById(id);
        if (v == null) throw new IllegalArgumentException("版本不存在");
        v.setIsCurrent(1);
        mapper.clearCurrentByNs(v.getNsCode());
        mapper.setVersionCurrent(id);
        mapper.updateCurrVersion(v.getNsCode(), v.getVersion());
        return v;
    }

    /** 删除指定版本记录（调用方应确保不删除唯一版本）。 */
    public void deleteVersion(String id) { mapper.deleteVersion(id); }
}
