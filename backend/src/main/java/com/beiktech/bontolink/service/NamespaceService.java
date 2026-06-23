package com.beiktech.bontolink.service;

import com.beiktech.bontolink.entity.BizNamespace;
import com.beiktech.bontolink.entity.BizNamespaceVersion;
import com.beiktech.bontolink.mapper.BizNamespaceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class NamespaceService {
    @Autowired private BizNamespaceMapper mapper;

    public List<BizNamespace> list() { return mapper.listAll(); }
    public BizNamespace get(String code) { return mapper.findByCode(code); }

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

    public BizNamespace update(BizNamespace ns) {
        mapper.update(ns);
        return mapper.findByCode(ns.getNsCode());
    }

    public List<BizNamespaceVersion> listVersions(String code) { return mapper.listVersions(code); }

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

    public void deleteVersion(String id) { mapper.deleteVersion(id); }
}
