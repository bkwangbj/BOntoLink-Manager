package com.beiktech.bontolink.data.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BizGroup {
    private String id;
    private String parentId;
    private String categoryCode;
    private String domainCode;      // 所属领域 (关联 ont_biz_category.category_code)
    private String gName;
    private Integer gSort;
    private String icon;
    private String color;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
