package com.lanyuan.starter.database.repository;

import com.lanyuan.starter.database.entity.OperationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {}

