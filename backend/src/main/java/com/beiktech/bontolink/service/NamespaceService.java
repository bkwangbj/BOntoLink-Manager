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
        if (v.getIsCurrent() == null) v.setIsCurrent(0);
        mapper.insertVersion(v);
        return v;
    }

    public void deleteVersion(String id) { mapper.deleteVersion(id); }
}
