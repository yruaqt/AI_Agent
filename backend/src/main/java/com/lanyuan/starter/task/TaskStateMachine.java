package com.lanyuan.starter.task;

import com.lanyuan.starter.common.exception.BusinessException;
import com.lanyuan.starter.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** 文档状态流转规则，禁止 DONE 直接退回 DRAFT 等非法操作。 */
@Component
public class TaskStateMachine {

    private static final Map<TaskStatus, Set<TaskStatus>> TRANSITIONS = Map.of(
            TaskStatus.DRAFT, EnumSet.of(TaskStatus.CONFIRMED, TaskStatus.CANCELLED),
            TaskStatus.CONFIRMED, EnumSet.of(TaskStatus.TODO, TaskStatus.CANCELLED),
            TaskStatus.TODO, EnumSet.of(TaskStatus.DOING, TaskStatus.CANCELLED),
            TaskStatus.DOING, EnumSet.of(TaskStatus.DONE, TaskStatus.CANCELLED),
            TaskStatus.DONE, EnumSet.noneOf(TaskStatus.class),
            TaskStatus.CANCELLED, EnumSet.noneOf(TaskStatus.class)
    );

    public void validate(TaskStatus current, TaskStatus target) {
        if (current == target) return;
        if (!TRANSITIONS.getOrDefault(current, Set.of()).contains(target)) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "任务状态不能从 " + current + " 变更为 " + target
            );
        }
    }
}
