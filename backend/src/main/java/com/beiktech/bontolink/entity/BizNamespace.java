package com.beiktech.bontolink.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BizNamespace {
    private String id;
    private String nsCode;
    private String nsName;
    private String nsUri;
    private String hierarchyPath;
    private String currVersion;
    private Integer status;
    private String metadata;
    private String rdfsLabel;
    private String rdfsComment;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
