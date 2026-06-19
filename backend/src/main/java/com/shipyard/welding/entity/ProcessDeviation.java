package com.shipyard.welding.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("process_deviation")
public class ProcessDeviation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String deviationNo;

    private Long scheduleId;

    private Long processCardId;

    private String deviationType;

    private String originalValue;

    private String deviationValue;

    private String deviationReason;

    private String riskAssessment;

    private String mitigationMeasure;

    private Long applicantId;

    private LocalDateTime applyTime;

    private Long reviewerId;

    private LocalDateTime reviewTime;

    private String reviewOpinion;

    private String deviationStatus;

    private LocalDate validUntil;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
