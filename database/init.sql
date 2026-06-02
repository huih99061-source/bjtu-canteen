-- =====================================================
-- 北京交通大学就餐仿真系统 - 数据库初始化脚本
-- =====================================================

SET NAMES utf8mb4;
CREATE DATABASE IF NOT EXISTS bjtu_canteen DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE bjtu_canteen;

-- 楼层表
CREATE TABLE IF NOT EXISTS floor (
    id INT PRIMARY KEY AUTO_INCREMENT,
    floor_name VARCHAR(50) NOT NULL COMMENT '楼层名称',
    window_count INT DEFAULT 0 COMMENT '窗口数量',
    seat_count INT DEFAULT 0 COMMENT '总座位数',
    open_time TIME DEFAULT '06:30:00' COMMENT '开放时间',
    close_time TIME DEFAULT '21:00:00' COMMENT '关闭时间'
) COMMENT='楼层信息表';

-- 窗口表
CREATE TABLE IF NOT EXISTS window_info (
    id INT PRIMARY KEY AUTO_INCREMENT,
    floor_id INT NOT NULL COMMENT '所属楼层',
    window_name VARCHAR(50) NOT NULL COMMENT '窗口名称',
    category VARCHAR(50) COMMENT '经营品类',
    avg_serve_seconds INT DEFAULT 90 COMMENT '平均服务时间(秒)',
    is_open TINYINT(1) DEFAULT 1 COMMENT '是否营业',
) COMMENT='窗口信息表';

-- 菜品表
CREATE TABLE IF NOT EXISTS food (
    id INT PRIMARY KEY AUTO_INCREMENT,
    window_id INT NOT NULL COMMENT '所属窗口',
    food_name VARCHAR(100) NOT NULL COMMENT '菜品名称',
    food_price DECIMAL(6,2) NOT NULL COMMENT '菜品价格',
    food_amount INT DEFAULT 100 COMMENT '剩余数量',
) COMMENT='菜品表';

-- 座位表
CREATE TABLE IF NOT EXISTS seat (
    id INT PRIMARY KEY AUTO_INCREMENT,
    floor_id INT NOT NULL COMMENT '所属楼层',
    table_id INT NOT NULL COMMENT '桌号',
    seat_index INT NOT NULL COMMENT '座位序号',
    x_pos DECIMAL(8,2) COMMENT 'X坐标百分比',
    y_pos DECIMAL(8,2) COMMENT 'Y坐标百分比',
    status ENUM('empty','reserved','dining') DEFAULT 'empty' COMMENT '状态',
) COMMENT='座位表';

-- 排队记录表
CREATE TABLE IF NOT EXISTS queue_record (
    id INT PRIMARY KEY AUTO_INCREMENT,
    window_id INT NOT NULL,
    queue_count INT DEFAULT 0,
    record_time DATETIME DEFAULT CURRENT_TIMESTAMP,
) COMMENT='排队记录表';

-- 评价表
CREATE TABLE IF NOT EXISTS feedback (
    id INT PRIMARY KEY AUTO_INCREMENT,
    window_id INT NOT NULL,
    food_id INT,
    rating INT,
    comment_text VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT='评价表';

-- 仿真数据记录表
CREATE TABLE IF NOT EXISTS sim_record (
    id INT PRIMARY KEY AUTO_INCREMENT,
    sim_time DATETIME NOT NULL COMMENT '仿真时间',
    total_people INT DEFAULT 0 COMMENT '食堂总人数',
    empty_seats INT DEFAULT 0 COMMENT '空座数量',
    record_at DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT='仿真数据记录表';

-- =====================================================
-- 初始数据
-- =====================================================

-- 插入楼层
INSERT INTO floor (floor_name, window_count, seat_count) VALUES
('学生活动中心食堂（一层）', 7, 200);

-- 插入窗口
INSERT INTO window_info (floor_id, window_name, category, avg_serve_seconds) VALUES
(1, '米饭窗口', '主食/盖饭', 80),
(1, '面条窗口', '面食', 90),
(1, '炒菜窗口', '炒菜/小炒', 100),
(1, '包子馒头', '主食/点心', 60),
(1, '麻辣烫', '小吃/烫菜', 120),
(1, '饮品甜点', '饮品/甜点', 50),
(1, '清真窗口', '清真食品', 90);

-- 插入菜品
INSERT INTO food (window_id, food_name, food_price) VALUES
(1,'红烧肉盖饭',12.00),(1,'鱼香茄子盖饭',10.00),(1,'番茄炒蛋盖饭',9.00),(1,'宫保鸡丁盖饭',11.00),
(2,'牛肉面',13.00),(2,'排骨面',12.00),(2,'素汤面',8.00),(2,'炸酱面',9.00),
(3,'红烧肉',9.00),(3,'清炒时蔬',5.00),(3,'麻婆豆腐',6.00),(3,'土豆丝',4.00),
(4,'大肉包',2.50),(4,'豆沙包',2.00),(4,'馒头',1.00),(4,'花卷',1.50),
(5,'麻辣烫(素)',10.00),(5,'麻辣烫(荤)',14.00),(5,'冒菜',12.00),
(6,'珍珠奶茶',8.00),(6,'豆浆',3.00),(6,'酸奶',5.00),(6,'蛋糕',6.00),
(7,'清真炒饭',11.00),(7,'牛肉汤面',13.00),(7,'清真小炒',10.00);

-- 插入座位 (80张桌子，每桌约4个座位)
INSERT INTO seat (floor_id, table_id, seat_index, x_pos, y_pos, status)
SELECT 1, t.tid, s.sid,
    (10 + (t.tid-1)*13) * 1.0,
    (20 + (s.sid-1)*15) * 1.0,
    'empty'
FROM
    (SELECT  1 tid UNION SELECT  2 UNION SELECT  3 UNION SELECT  4 UNION SELECT  5
     UNION SELECT  6 UNION SELECT  7 UNION SELECT  8 UNION SELECT  9 UNION SELECT 10
     UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15
     UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20
     UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24 UNION SELECT 25
     UNION SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29 UNION SELECT 30
     UNION SELECT 31 UNION SELECT 32 UNION SELECT 33 UNION SELECT 34 UNION SELECT 35
     UNION SELECT 36 UNION SELECT 37 UNION SELECT 38 UNION SELECT 39 UNION SELECT 40
     UNION SELECT 41 UNION SELECT 42 UNION SELECT 43 UNION SELECT 44 UNION SELECT 45
     UNION SELECT 46 UNION SELECT 47 UNION SELECT 48 UNION SELECT 49 UNION SELECT 50
     UNION SELECT 51 UNION SELECT 52 UNION SELECT 53 UNION SELECT 54 UNION SELECT 55
     UNION SELECT 56 UNION SELECT 57 UNION SELECT 58 UNION SELECT 59 UNION SELECT 60
     UNION SELECT 61 UNION SELECT 62 UNION SELECT 63 UNION SELECT 64 UNION SELECT 65
     UNION SELECT 66 UNION SELECT 67 UNION SELECT 68 UNION SELECT 69 UNION SELECT 70
     UNION SELECT 71 UNION SELECT 72 UNION SELECT 73 UNION SELECT 74 UNION SELECT 75
     UNION SELECT 76 UNION SELECT 77 UNION SELECT 78 UNION SELECT 79 UNION SELECT 80) t,
    (SELECT 1 sid UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) s;