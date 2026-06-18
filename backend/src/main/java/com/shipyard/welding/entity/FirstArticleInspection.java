package com.shipyard.welding.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("first_article_inspection")
public class FirstArticleInspection {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String inspectionNo;

    private Long scheduleId;

    private Long workstationId;

    private Long welderId;

    private Long processCardId;

    private Long inspectorId;

    private LocalDateTime inspectionTime;

    private Integer appearanceCheck;

    private Integer sizeCheck;

    private Integer ndtCheck;

    private Integer overallResult;

    private String defectDesc;

    private String rectificationAdvice;

    private Integer recheckRequired;

    private Integer recheckResult;

    private LocalDateTime recheckTime;

    private Long recheckerId;

    private String attachment;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
