package com.lanyuan.starter.task;

import java.time.LocalDate;
import java.util.List;

public record FarmingTaskView(
        String id,
        String batchId,
        String orchardId,
        LocalDate date,
        String type,
        String title,
        String content,
        String priority,
        String suggestedTime,
        String status,
        String basis,
        String safetyNotice,
        String statusRemark,
        String assigneeId,
        List<Object> citations
) {
    static FarmingTaskView from(FarmingTask task, com.fasterxml.jackson.databind.ObjectMapper mapper) {
        return new FarmingTaskView(
                String.valueOf(task.getId()), String.valueOf(task.getBatchId()),
                String.valueOf(task.getOrchardId()), task.getTaskDate(), task.getType(),
                task.getTitle(), task.getContent(), task.getPriority().name(), task.getSuggestedTime(),
                task.getStatus().name(), task.getBasis(), task.getSafetyNotice(), task.getStatusRemark(),
                task.getAssigneeId() == null ? null : String.valueOf(task.getAssigneeId()),
                readCitations(task.getCitationsJson(), mapper)
        );
    }

    private static List<Object> readCitations(String value, com.fasterxml.jackson.databind.ObjectMapper mapper) {
        if (value == null || value.isBlank()) return List.of();
        try {
            return mapper.readValue(value, new com.fasterxml.jackson.core.type.TypeReference<List<Object>>() {});
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            return List.of();
        }
    }
}
