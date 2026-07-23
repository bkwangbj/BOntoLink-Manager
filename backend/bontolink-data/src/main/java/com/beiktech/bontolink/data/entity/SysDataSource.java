package com.beiktech.bontolink.data.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SysDataSource {
    private String id;
    private String categoryCode;
    private String dsCode;
    private String dsName;
    private String dsType;
    private String jdbcDriver;
    private String jdbcUrl;
    private String username;
    private String password;
    private String mongoUrl;
    private Integer status;
    private String remark;
    private Integer refCount;
    private String connectStatus;     // online / offline / risk
    private Integer activeConn;
    private Integer maxConn;
    private Integer responseMs;
    private Integer collectionCnt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
