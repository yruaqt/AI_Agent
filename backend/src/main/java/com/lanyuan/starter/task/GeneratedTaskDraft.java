package com.lanyuan.starter.task;

import java.util.List;

/** 百炼模型必须输出的任务 JSON 结构。 */
public record GeneratedTaskDraft(
        String weatherSummary,
        List<Item> tasks
) {
    public record Item(
            String type,
            String title,
            String content,
            String priority,
            String suggestedTime,
            String basis,
            String safetyNotice
    ) {}
}
