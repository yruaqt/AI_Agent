package com.lanyuan.starter.database.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DatabaseStatusService {
    private final JdbcTemplate jdbcTemplate;
    public DatabaseStatusService(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }
    public DatabaseStatus probe() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return new DatabaseStatus(Integer.valueOf(1).equals(result) ? "UP" : "UNKNOWN", null);
        } catch (Exception ex) { return new DatabaseStatus("DOWN", ex.getClass().getSimpleName()); }
    }
    public record DatabaseStatus(String status, String errorType) {}
}

