package com.shipyard.welding.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("safety_review")
public class SafetyReview {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String reviewNo;

    private Long scheduleId;

    private Long workstationId;

    private Long welderId;

    private String triggerType;

    private String triggerDetail;

    private String certExpiringIds;

    private Integer hasHeightWork;

    private Integer hasHotWork;

    private Integer hasDeviation;

    private Long reviewerId;

    private String reviewStatus;

    private LocalDateTime reviewTime;

    private String reviewOpinion;

    private String safetyMeasures;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
