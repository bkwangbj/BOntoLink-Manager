package com.beiktech.bontolink.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BizNamespaceVersion {
    private String id;
    private String nsCode;
    private String version;
    private String uri;
    private String snapshotData;
    private String owlContent;
    private LocalDateTime publishTime;
    private Integer isCurrent;
    private Integer status;
    private String rdfsLabel;
    private String rdfsComment;
    private String rdfsSeeAlso;
    private String rdfsDefinedBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
