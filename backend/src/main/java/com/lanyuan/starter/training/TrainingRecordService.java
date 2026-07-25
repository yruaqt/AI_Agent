package com.lanyuan.starter.training;

import com.lanyuan.starter.common.exception.BusinessException;
import com.lanyuan.starter.common.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class TrainingRecordService {

    private final TrainingRecordRepository repository;

    public TrainingRecordService(TrainingRecordRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public TrainingRecord create(Long studentId, Long orchardId, Long taskId, LocalDate recordDate,
                                 Integer inspectedTreeCount, Integer abnormalTreeCount,
                                 String imageUrl, String phenomenon, String measure, String result) {
        TrainingRecord record = new TrainingRecord();
        record.setStudentId(studentId);
        record.setOrchardId(orchardId);
        record.setTaskId(taskId);
        record.setRecordDate(recordDate);
        record.setInspectedTreeCount(inspectedTreeCount);
        record.setAbnormalTreeCount(abnormalTreeCount);
        record.setImageUrl(imageUrl);
        record.setPhenomenon(phenomenon);
        record.setMeasure(measure);
        record.setResult(result);
        record.setReviewStatus("PENDING");
        return repository.save(record);
    }

    public Page<TrainingRecord> list(Long orchardId, Long studentId, LocalDate startDate,
                                     LocalDate endDate, PageRequest pr) {
        return repository.findWithFilters(orchardId, studentId, startDate, endDate, pr);
    }

    public TrainingRecord detail(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "实训记录不存在"));
    }

    /**
     * 校验实训记录归属：只有记录所属学生可以操作
     */
    public void checkOwnership(Long recordId, Long userId) {
        TrainingRecord record = repository.findById(recordId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "实训记录不存在"));
        if (!record.getStudentId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作：该记录不属于当前用户");
        }
    }

    @Transactional
    public TrainingRecord update(Long id, Long orchardId, Long taskId, LocalDate recordDate,
                                 Integer inspectedTreeCount, Integer abnormalTreeCount,
                                 String imageUrl, String phenomenon, String measure, String result) {
        TrainingRecord record = detail(id);
        // 已评价的记录不得修改（除非教师退回）
        if ("APPROVED".equals(record.getReviewStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "已评价的记录不得修改");
        }
        record.setOrchardId(orchardId);
        record.setTaskId(taskId);
        record.setRecordDate(recordDate);
        record.setInspectedTreeCount(inspectedTreeCount);
        record.setAbnormalTreeCount(abnormalTreeCount);
        record.setImageUrl(imageUrl);
        record.setPhenomenon(phenomenon);
        record.setMeasure(measure);
        record.setResult(result);
        return repository.save(record);
    }

    @Transactional
    public TrainingRecord review(Long id, Integer score, String comment, String status) {
        TrainingRecord record = detail(id);
        record.setScore(score);
        record.setComment(comment);
        record.setReviewStatus(status != null ? status : "APPROVED");
        return repository.save(record);
    }
}
