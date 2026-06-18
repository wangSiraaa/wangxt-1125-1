package com.shipyard.welding.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("process_card")
public class ProcessCard {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String cardNo;

    private String cardName;

    private String projectName;

    private String workSection;

    private String material;

    private String materialSpec;

    private String weldMethod;

    private String weldMaterial;

    private String weldCurrent;

    private String weldVoltage;

    private String weldSpeed;

    private String preheatTemp;

    private String interpassTemp;

    private String gasType;

    private String gasFlow;

    private String techRequirement;

    private String qualityStandard;

    private String cardStatus;

    private Long issuerId;

    private LocalDateTime issueTime;

    private Long creatorId;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
