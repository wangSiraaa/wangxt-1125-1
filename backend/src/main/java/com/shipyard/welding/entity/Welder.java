package com.shipyard.welding.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("welder")
public class Welder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String welderNo;

    private String welderName;

    private String gender;

    private String idCard;

    private Long userId;

    private LocalDate entryDate;

    private Integer status;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
