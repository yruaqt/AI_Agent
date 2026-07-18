package com.lanyuan.starter.database.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "sys_operation_log")
public class OperationLog extends BaseEntity {
    @Column(nullable = false, length = 64) private String moduleName;
    @Column(nullable = false, length = 128) private String operationName;
    @Column(length = 64) private String operatorName;
    @Column(length = 64) private String requestId;
    @Column(nullable = false) private boolean success = true;
    @Column(length = 1000) private String detail;
    protected OperationLog() {}
    public OperationLog(String moduleName, String operationName) { this.moduleName = moduleName; this.operationName = operationName; }
    public String getModuleName() { return moduleName; }
    public String getOperationName() { return operationName; }
    public String getOperatorName() { return operatorName; }
    public String getRequestId() { return requestId; }
    public boolean isSuccess() { return success; }
    public String getDetail() { return detail; }
}

