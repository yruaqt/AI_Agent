package com.lanyuan.starter.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lanyuan.starter.common.exception.BusinessException;
import com.lanyuan.starter.common.exception.ErrorCode;
import com.lanyuan.starter.common.web.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/** 农事任务查询、教师修改和合法状态流转。 */
@Service
public class FarmingTaskService {

    private final FarmingTaskRepository repository;
    private final TaskStateMachine stateMachine;
    private final CurrentUser currentUser;
    private final ObjectMapper objectMapper;

    public FarmingTaskService(FarmingTaskRepository repository,
                              TaskStateMachine stateMachine,
                              CurrentUser currentUser,
                              ObjectMapper objectMapper) {
        this.repository = repository;
        this.stateMachine = stateMachine;
        this.currentUser = currentUser;
        this.objectMapper = objectMapper;
    }

    public Page<FarmingTask> list(Long orchardId, LocalDate date,
                                  TaskStatus status, Pageable pageable) {
        return repository.findWithFilters(orchardId, date, status, pageable);
    }

    public FarmingTask detail(Long taskId) {
        return repository.findById(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "农事任务不存在"));
    }

    @Transactional
    public FarmingTask update(Long taskId, String title, String content,
                              TaskPriority priority, String suggestedTime,
                              String safetyNotice) {
        requireAdmin();
        FarmingTask task = detail(taskId);
        if (task.getStatus() == TaskStatus.DONE || task.getStatus() == TaskStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.CONFLICT, "已结束任务不能再修改内容");
        }
        task.setTitle(title.trim());
        task.setContent(content.trim());
        task.setPriority(priority);
        task.setSuggestedTime(blankToNull(suggestedTime));
        task.setSafetyNotice(blankToNull(safetyNotice));
        return repository.save(task);
    }

    @Transactional
    public FarmingTask changeStatus(Long taskId, TaskStatus target, String remark) {
        FarmingTask task = detail(taskId);
        authorizeTransition(task, target);
        stateMachine.validate(task.getStatus(), target);
        if (!currentUser.isAdmin() && task.getStatus() == TaskStatus.TODO && target == TaskStatus.DOING) {
            task.setAssigneeId(currentUser.id());
        }
        task.setStatus(target);
        task.setStatusRemark(blankToNull(remark));
        return repository.save(task);
    }

    public FarmingTaskView view(FarmingTask task) {
        return FarmingTaskView.from(task, objectMapper);
    }

    private void authorizeTransition(FarmingTask task, TaskStatus target) {
        if (currentUser.isAdmin()) return;
        boolean claim = task.getStatus() == TaskStatus.TODO && target == TaskStatus.DOING;
        boolean finish = task.getStatus() == TaskStatus.DOING
                && target == TaskStatus.DONE
                && currentUser.id().equals(task.getAssigneeId());
        if (!claim && !finish) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "学生只能领取待执行任务或完成本人任务");
        }
    }

    private void requireAdmin() {
        if (!currentUser.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可以修改任务内容");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
