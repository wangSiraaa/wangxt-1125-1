package com.shipyard.welding.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("work_schedule")
public class WorkSchedule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String scheduleNo;

    private Long workstationId;

    private Long welderId;

    private Long processCardId;

    private String weldSeamNo;

    private String machineNo;

    private String materialBatch;

    private String repairReason;

    private Long hotWorkPermitId;

    private Long safetyReviewId;

    private Integer requireSafetyReview;

    private LocalDate scheduleDate;

    private String shiftType;

    private LocalDateTime planStartTime;

    private LocalDateTime planEndTime;

    private String taskDesc;

    private String scheduleStatus;

    private Long teamLeaderId;

    private LocalDateTime actualStartTime;

    private LocalDateTime actualEndTime;

    private String suspendReason;

    private LocalDateTime suspendTime;

    private LocalDateTime resumeTime;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
