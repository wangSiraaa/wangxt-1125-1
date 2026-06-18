package com.shipyard.welding.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("welder_certificate")
public class WelderCertificate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long welderId;

    private String certNo;

    private String certType;

    private String certLevel;

    private String materialSpec;

    private String thicknessRange;

    private String weldPosition;

    private LocalDate issueDate;

    private LocalDate expiryDate;

    private String issueOrg;

    private Integer certStatus;

    private String certFile;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
