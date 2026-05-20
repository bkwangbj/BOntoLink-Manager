package com.beiktech.bontolink.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BizGroup {
    private String id;
    private String parentId;
    private String categoryCode;
    private String gName;
    private Integer gSort;
    private String icon;
    private String color;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
