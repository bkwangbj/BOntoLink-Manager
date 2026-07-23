package com.beiktech.bontolink.data.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BizCategory {
    private String id;
    private String parentId;
    private String rid;
    private String categoryCode;
    /** 1=行业 2=领域 3=分组 */
    private Integer categoryType;
    private String nsCode;
    private Integer status;
    private Integer sort;
    private String icon;
    private String color;
    private String rdfsLabel;
    private String rdfsComment;
    private String rdfsSeeAlso;
    private String rdfsDefinedBy;
    private String description;
    private String metadata;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
