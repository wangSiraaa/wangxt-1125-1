-- =============================================
-- 船厂焊接工位开工许可系统 - 数据库表结构
-- =============================================

CREATE DATABASE IF NOT EXISTS shipyard_welding DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE shipyard_welding;

-- ---------------------------------------------
-- 系统用户表
-- ---------------------------------------------
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    real_name VARCHAR(50) NOT NULL COMMENT '真实姓名',
    role_code VARCHAR(20) NOT NULL COMMENT '角色编码: TEAM_LEADER-班组长, WELDER-焊工, INSPECTOR-质检员, ADMIN-管理员',
    phone VARCHAR(20) COMMENT '手机号',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-启用, 0-停用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- ---------------------------------------------
-- 焊工表
-- ---------------------------------------------
DROP TABLE IF EXISTS welder;
CREATE TABLE welder (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    welder_no VARCHAR(30) NOT NULL COMMENT '焊工编号',
    welder_name VARCHAR(50) NOT NULL COMMENT '焊工姓名',
    gender VARCHAR(10) COMMENT '性别',
    id_card VARCHAR(18) COMMENT '身份证号',
    user_id BIGINT COMMENT '关联系统用户ID',
    entry_date DATE COMMENT '入职日期',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-在岗, 0-离岗',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_welder_no (welder_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='焊工表';

-- ---------------------------------------------
-- 焊工资格证书表
-- ---------------------------------------------
DROP TABLE IF EXISTS welder_certificate;
CREATE TABLE welder_certificate (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    welder_id BIGINT NOT NULL COMMENT '焊工ID',
    cert_no VARCHAR(50) NOT NULL COMMENT '证书编号',
    cert_type VARCHAR(50) NOT NULL COMMENT '证书类型: 如GTAW钨极氩弧焊, SMAW焊条电弧焊, GMAW熔化极气体保护焊等',
    cert_level VARCHAR(30) COMMENT '资格等级: 如I级, II级, III级',
    material_spec VARCHAR(100) COMMENT '认可材质规格',
    thickness_range VARCHAR(50) COMMENT '认可厚度范围',
    weld_position VARCHAR(50) COMMENT '认可焊接位置: 如平焊、立焊、横焊、仰焊',
    issue_date DATE NOT NULL COMMENT '发证日期',
    expiry_date DATE NOT NULL COMMENT '到期日期',
    issue_org VARCHAR(100) COMMENT '发证机构',
    cert_status TINYINT NOT NULL DEFAULT 1 COMMENT '证书状态: 1-有效, 2-即将过期, 3-已过期, 0-已作废',
    cert_file VARCHAR(255) COMMENT '证书附件路径',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_cert_no (cert_no),
    KEY idx_welder_id (welder_id),
    KEY idx_expiry_date (expiry_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='焊工资格证书表';

-- ---------------------------------------------
-- 工位表
-- ---------------------------------------------
DROP TABLE IF EXISTS workstation;
CREATE TABLE workstation (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    station_no VARCHAR(30) NOT NULL COMMENT '工位编号',
    station_name VARCHAR(100) NOT NULL COMMENT '工位名称',
    area VARCHAR(50) COMMENT '所属区域/车间',
    equipment VARCHAR(200) COMMENT '设备信息',
    current_status VARCHAR(20) NOT NULL DEFAULT 'IDLE' COMMENT '当前状态: IDLE-空闲, SCHEDULED-已排班, WORKING-开工中, INSPECTING-首检中, SUSPENDED-已暂停',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '启用状态: 1-启用, 0-停用',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_station_no (station_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工位表';

-- ---------------------------------------------
-- 工艺卡表
-- ---------------------------------------------
DROP TABLE IF EXISTS process_card;
CREATE TABLE process_card (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    card_no VARCHAR(50) NOT NULL COMMENT '工艺卡编号',
    card_name VARCHAR(200) NOT NULL COMMENT '工艺卡名称',
    project_name VARCHAR(200) COMMENT '所属项目/分段',
    work_section VARCHAR(100) COMMENT '工序/工步',
    material VARCHAR(100) COMMENT '母材材质',
    material_spec VARCHAR(100) COMMENT '母材规格',
    weld_method VARCHAR(50) COMMENT '焊接方法',
    weld_material VARCHAR(100) COMMENT '焊接材料',
    weld_current VARCHAR(50) COMMENT '焊接电流参数',
    weld_voltage VARCHAR(50) COMMENT '焊接电压参数',
    weld_speed VARCHAR(50) COMMENT '焊接速度',
    preheat_temp VARCHAR(50) COMMENT '预热温度',
    interpass_temp VARCHAR(50) COMMENT '层间温度',
    gas_type VARCHAR(50) COMMENT '保护气体种类',
    gas_flow VARCHAR(50) COMMENT '气体流量',
    tech_requirement TEXT COMMENT '技术要求',
    quality_standard VARCHAR(200) COMMENT '质量标准',
    card_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '工艺卡状态: DRAFT-草稿, APPROVED-已审批, ISSUED-已下发, OBSOLETE-已作废',
    issuer_id BIGINT COMMENT '下发人ID',
    issue_time DATETIME COMMENT '下发时间',
    creator_id BIGINT COMMENT '创建人ID',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_card_no (card_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工艺卡表';

-- ---------------------------------------------
-- 排班表
-- ---------------------------------------------
DROP TABLE IF EXISTS work_schedule;
CREATE TABLE work_schedule (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    schedule_no VARCHAR(50) NOT NULL COMMENT '排班单号',
    workstation_id BIGINT NOT NULL COMMENT '工位ID',
    welder_id BIGINT NOT NULL COMMENT '焊工ID',
    process_card_id BIGINT NOT NULL COMMENT '工艺卡ID',
    schedule_date DATE NOT NULL COMMENT '排班日期',
    shift_type VARCHAR(20) COMMENT '班次: DAY-白班, NIGHT-夜班',
    plan_start_time DATETIME COMMENT '计划开始时间',
    plan_end_time DATETIME COMMENT '计划结束时间',
    task_desc VARCHAR(500) COMMENT '任务描述',
    schedule_status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED' COMMENT '排班状态: SCHEDULED-已排班, STARTED-已开工, INSPECTING-首检中, COMPLETED-已完成, SUSPENDED-已暂停, CANCELLED-已取消',
    team_leader_id BIGINT COMMENT '班组长ID',
    actual_start_time DATETIME COMMENT '实际开始时间',
    actual_end_time DATETIME COMMENT '实际结束时间',
    suspend_reason VARCHAR(500) COMMENT '暂停原因',
    suspend_time DATETIME COMMENT '暂停时间',
    resume_time DATETIME COMMENT '恢复时间',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_schedule_no (schedule_no),
    KEY idx_workstation_id (workstation_id),
    KEY idx_welder_id (welder_id),
    KEY idx_schedule_date (schedule_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排班表';

-- ---------------------------------------------
-- 焊工工艺卡确认记录表
-- ---------------------------------------------
DROP TABLE IF EXISTS process_confirm;
CREATE TABLE process_confirm (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    schedule_id BIGINT NOT NULL COMMENT '排班ID',
    welder_id BIGINT NOT NULL COMMENT '焊工ID',
    process_card_id BIGINT NOT NULL COMMENT '工艺卡ID',
    confirm_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '确认状态: PENDING-待确认, CONFIRMED-已确认, REJECTED-已拒绝',
    reject_reason VARCHAR(500) COMMENT '拒绝原因',
    confirm_time DATETIME COMMENT '确认时间',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_schedule_id (schedule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工艺卡确认记录表';

-- ---------------------------------------------
-- 安全检查表
-- ---------------------------------------------
DROP TABLE IF EXISTS safety_check;
CREATE TABLE safety_check (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    schedule_id BIGINT NOT NULL COMMENT '排班ID',
    workstation_id BIGINT NOT NULL COMMENT '工位ID',
    check_type VARCHAR(50) NOT NULL COMMENT '检查类型: PREWORK-开工前检查, DAILY-日常检查',
    check_item VARCHAR(200) NOT NULL COMMENT '检查项目',
    check_result TINYINT COMMENT '检查结果: 1-合格, 0-不合格',
    check_desc VARCHAR(500) COMMENT '检查说明',
    checker_id BIGINT COMMENT '检查人ID',
    check_time DATETIME COMMENT '检查时间',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_schedule_id (schedule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='安全检查表';

-- ---------------------------------------------
-- 首件检查记录表
-- ---------------------------------------------
DROP TABLE IF EXISTS first_article_inspection;
CREATE TABLE first_article_inspection (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    inspection_no VARCHAR(50) NOT NULL COMMENT '首检单号',
    schedule_id BIGINT NOT NULL COMMENT '排班ID',
    workstation_id BIGINT NOT NULL COMMENT '工位ID',
    welder_id BIGINT NOT NULL COMMENT '焊工ID',
    process_card_id BIGINT NOT NULL COMMENT '工艺卡ID',
    inspector_id BIGINT COMMENT '质检员ID',
    inspection_time DATETIME COMMENT '检查时间',
    appearance_check TINYINT COMMENT '外观检查: 1-合格, 0-不合格',
    size_check TINYINT COMMENT '尺寸检查: 1-合格, 0-不合格',
    ndt_check TINYINT COMMENT '无损检测: 1-合格, 0-不合格',
    overall_result TINYINT NOT NULL COMMENT '综合判定: 1-合格, 0-不合格',
    defect_desc TEXT COMMENT '缺陷描述',
    rectification_advice TEXT COMMENT '整改意见',
    recheck_required TINYINT NOT NULL DEFAULT 0 COMMENT '是否需要复检: 1-是, 0-否',
    recheck_result TINYINT COMMENT '复检结果: 1-合格, 0-不合格',
    recheck_time DATETIME COMMENT '复检时间',
    rechecker_id BIGINT COMMENT '复检人ID',
    attachment VARCHAR(255) COMMENT '附件路径',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_inspection_no (inspection_no),
    KEY idx_schedule_id (schedule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='首件检查记录表';

-- ---------------------------------------------
-- 工序暂停记录表
-- ---------------------------------------------
DROP TABLE IF EXISTS process_suspension;
CREATE TABLE process_suspension (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    suspension_no VARCHAR(50) NOT NULL COMMENT '暂停单号',
    schedule_id BIGINT NOT NULL COMMENT '排班ID',
    workstation_id BIGINT NOT NULL COMMENT '工位ID',
    suspension_type VARCHAR(30) NOT NULL COMMENT '暂停类型: FIRST_ARTICLE_FAIL-首件不合格, SAFETY_ISSUE-安全问题, CERT_EXPIRED-资格过期, OTHER-其他',
    suspension_reason TEXT NOT NULL COMMENT '暂停原因',
    suspension_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '暂停状态: ACTIVE-生效中, RESOLVED-已解除',
    reporter_id BIGINT COMMENT '上报人ID',
    report_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上报时间',
    resolver_id BIGINT COMMENT '处理人ID',
    resolve_time DATETIME COMMENT '解除时间',
    resolve_measure TEXT COMMENT '解除措施',
    affect_next_process TINYINT NOT NULL DEFAULT 1 COMMENT '是否影响下道工序: 1-是, 0-否',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_suspension_no (suspension_no),
    KEY idx_schedule_id (schedule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工序暂停记录表';

-- =============================================
-- 初始化数据
-- =============================================

-- 系统用户
INSERT INTO sys_user (username, password, real_name, role_code, phone, status) VALUES
('admin', '123456', '系统管理员', 'ADMIN', '13800000000', 1),
('leader01', '123456', '张班长', 'TEAM_LEADER', '13800000001', 1),
('welder01', '123456', '李焊工', 'WELDER', '13800000002', 1),
('welder02', '123456', '王焊工', 'WELDER', '13800000003', 1),
('inspector01', '123456', '赵质检', 'INSPECTOR', '13800000004', 1);

-- 焊工信息
INSERT INTO welder (welder_no, welder_name, gender, id_card, user_id, entry_date, status) VALUES
('W001', '李焊工', '男', '310101199001011234', 3, '2020-01-15', 1),
('W002', '王焊工', '男', '310101199202022345', 4, '2021-03-20', 1);

-- 焊工资格证书
INSERT INTO welder_certificate (welder_id, cert_no, cert_type, cert_level, material_spec, thickness_range, weld_position, issue_date, expiry_date, issue_org, cert_status) VALUES
(1, 'CERT20230001', 'GTAW钨极氩弧焊', 'II级', 'Q345R, Q235B', '6-30mm', '平焊、立焊', '2023-01-10', '2026-01-09', '中国船级社', 1),
(1, 'CERT20230002', 'SMAW焊条电弧焊', 'II级', 'Q345R', '8-40mm', '平焊、横焊、立焊', '2023-03-15', '2025-03-14', '中国船级社', 2),
(2, 'CERT20230003', 'GMAW熔化极气体保护焊', 'I级', 'Q235B', '4-20mm', '平焊', '2022-06-20', '2024-06-19', '中国船级社', 3);

-- 工位信息
INSERT INTO workstation (station_no, station_name, area, equipment, current_status, status) VALUES
('WS001', '焊接工位1号', '船体车间A区', '氩弧焊机1台、气体保护焊机1台', 'IDLE', 1),
('WS002', '焊接工位2号', '船体车间A区', '焊条电弧焊机2台', 'IDLE', 1),
('WS003', '焊接工位3号', '船体车间B区', '自动焊接设备1套', 'IDLE', 1);

-- 工艺卡
INSERT INTO process_card (card_no, card_name, project_name, work_section, material, material_spec, weld_method, weld_material, weld_current, weld_voltage, weld_speed, preheat_temp, interpass_temp, gas_type, gas_flow, tech_requirement, quality_standard, card_status, issuer_id, issue_time, creator_id) VALUES
('PC20240001', '甲板分段平焊工艺卡', 'H1234船-甲板分段', '对接平焊', 'Q345R', 't=16mm', 'GTAW+SMAW', 'ER50-6焊丝、J507焊条', '80-120A', '12-16V', '8-12cm/min', '>=100°C', '<=250°C', 'Ar 99.99%', '10-15L/min', '严格控制层间温度，焊前预热100°C以上', 'GB 50205-2020 一级焊缝', 'ISSUED', 2, '2024-01-10 09:00:00', 2),
('PC20240002', '舷侧板立焊工艺卡', 'H1234船-舷侧分段', '立对接焊', 'Q345R', 't=20mm', 'GMAW', 'ER50-6焊丝', '160-200A', '24-28V', '15-20cm/min', '>=80°C', '<=200°C', '80%Ar+20%CO2', '18-22L/min', '采用多层多道焊，单道厚度不超过3mm', 'GB 50205-2020 二级焊缝', 'ISSUED', 2, '2024-01-12 14:30:00', 2),
('PC20240003', '球扁钢角焊工艺卡', 'H1234船-舱壁分段', '角接焊', 'Q235B', 't=12mm', 'SMAW', 'J422焊条', '90-130A', '22-26V', '10-15cm/min', '', '', '', '', '焊脚尺寸6mm，无气孔、夹渣', 'GB 50205-2020 三级焊缝', 'DRAFT', NULL, NULL, 2);
