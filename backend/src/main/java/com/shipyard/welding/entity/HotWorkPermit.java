package com.shipyard.welding.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("hot_work_permit")
public class HotWorkPermit {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String permitNo;

    private Long scheduleId;

    private Long workstationId;

    private String hotWorkType;

    private String fireLevel;

    private String workContent;

    private LocalDateTime workStartTime;

    private LocalDateTime workEndTime;

    private Long applicantId;

    private LocalDateTime applyTime;

    private Long approverId;

    private LocalDateTime approveTime;

    private String permitStatus;

    private Integer fireExtinguisher;

    private Integer fireWatchman;

    private Integer combustibleCleared;

    private Integer ventCheck;

    private String gasTestResult;

    private Long gasTesterId;

    private LocalDateTime gasTestTime;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
