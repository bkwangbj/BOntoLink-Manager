package com.beiktech.bontolink.data.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OntClass {
    private String id;
    private String rid;
    private String apiName;
    private String nsCode;
    private String categoryCode;
    private String displayName;
    private String rdfsLabel;
    private String rdfsComment;
    private String description;
    private String icon;
    private String color;
    private Integer status;
    private String metadata;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
