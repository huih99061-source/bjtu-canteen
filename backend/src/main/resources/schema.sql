CREATE TABLE IF NOT EXISTS floor (
    id INT PRIMARY KEY AUTO_INCREMENT,
    floor_name VARCHAR(50) NOT NULL,
    window_count INT DEFAULT 0,
    seat_count INT DEFAULT 0,
    open_time TIME DEFAULT '06:30:00',
    close_time TIME DEFAULT '21:00:00'
);

CREATE TABLE IF NOT EXISTS window_info (
    id INT PRIMARY KEY AUTO_INCREMENT,
    floor_id INT NOT NULL,
    window_name VARCHAR(50) NOT NULL,
    category VARCHAR(50),
    avg_serve_seconds INT DEFAULT 90,
    is_open TINYINT(1) DEFAULT 1
);

CREATE TABLE IF NOT EXISTS food (
    id INT PRIMARY KEY AUTO_INCREMENT,
    window_id INT NOT NULL,
    food_name VARCHAR(100) NOT NULL,
    food_price DECIMAL(6,2) NOT NULL,
    food_amount INT DEFAULT 100
);

CREATE TABLE IF NOT EXISTS seat (
    id INT PRIMARY KEY AUTO_INCREMENT,
    floor_id INT NOT NULL,
    table_id INT NOT NULL,
    seat_index INT NOT NULL,
    x_pos DECIMAL(8,2),
    y_pos DECIMAL(8,2),
    status ENUM('empty','reserved','dining') DEFAULT 'empty'
);

CREATE TABLE IF NOT EXISTS queue_record (
    id INT PRIMARY KEY AUTO_INCREMENT,
    window_id INT NOT NULL,
    queue_count INT DEFAULT 0,
    record_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS feedback (
    id INT PRIMARY KEY AUTO_INCREMENT,
    window_id INT NOT NULL,
    food_id INT,
    rating INT,
    comment_text VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sim_record (
    id INT PRIMARY KEY AUTO_INCREMENT,
    sim_time DATETIME NOT NULL,
    total_people INT DEFAULT 0,
    empty_seats INT DEFAULT 0,
    record_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 初始数据（INSERT IGNORE：已存在则跳过）
INSERT IGNORE INTO floor (id, floor_name, window_count, seat_count) VALUES
(1, '学生活动中心食堂（一层）', 7, 320);

INSERT IGNORE INTO window_info (id, floor_id, window_name, category, avg_serve_seconds) VALUES
(1, 1, '米饭窗口', '主食/盖饭', 80),
(2, 1, '面条窗口', '面食', 90),
(3, 1, '炒菜窗口', '炒菜/小炒', 100),
(4, 1, '包子馒头', '主食/点心', 60),
(5, 1, '麻辣烫', '小吃/烫菜', 120),
(6, 1, '饮品甜点', '饮品/甜点', 50),
(7, 1, '清真窗口', '清真食品', 90);

-- 清空并重建菜品数据（保证无重复）
DELETE FROM food;
ALTER TABLE food AUTO_INCREMENT = 1;

INSERT INTO food (window_id, food_name, food_price) VALUES
-- 米饭窗口（8道）
(1,'红烧肉盖饭',12.00),(1,'鱼香茄子盖饭',10.00),(1,'番茄炒蛋盖饭',9.00),(1,'宫保鸡丁盖饭',11.00),
(1,'梅菜扣肉饭',13.00),(1,'咖喱鸡腿饭',12.00),(1,'叉烧饭',11.00),(1,'米饭+例汤',3.00),
-- 面条窗口（8道）
(2,'牛肉面',13.00),(2,'排骨面',12.00),(2,'素汤面',8.00),(2,'炸酱面',9.00),
(2,'番茄鸡蛋面',9.00),(2,'热干面',8.00),(2,'担担面',11.00),(2,'重庆小面',10.00),
-- 炒菜窗口（10道）
(3,'红烧肉',9.00),(3,'清炒时蔬',5.00),(3,'麻婆豆腐',6.00),(3,'土豆丝',4.00),
(3,'地三鲜',7.00),(3,'鱼香肉丝',8.00),(3,'尖椒炒鸡蛋',6.00),
(3,'水煮肉片',12.00),(3,'酸菜鱼',14.00),(3,'蒜薹炒腊肉',10.00),
-- 包子馒头（10道）
(4,'大肉包',2.50),(4,'豆沙包',2.00),(4,'馒头',1.00),(4,'花卷',1.50),
(4,'韭菜鸡蛋包',2.50),(4,'香菇青菜包',2.00),(4,'糖三角',1.50),
(4,'南瓜饼',3.00),(4,'煎饺(6个)',6.00),(4,'小笼包(4个)',7.00),
-- 麻辣烫（8道）
(5,'麻辣烫(素)',10.00),(5,'麻辣烫(荤)',14.00),(5,'冒菜',12.00),
(5,'酸辣粉',9.00),(5,'肥肠粉',10.00),(5,'红油抄手',9.00),
(5,'凉拌粉皮',7.00),(5,'麻酱凉面',8.00),
-- 饮品甜点（10道）
(6,'珍珠奶茶',8.00),(6,'豆浆',3.00),(6,'酸奶',5.00),(6,'蛋糕',6.00),
(6,'红豆汤',5.00),(6,'绿豆汤',5.00),(6,'芋圆',8.00),
(6,'豆腐脑',4.00),(6,'鲜榨橙汁',9.00),(6,'冰粉',6.00),
-- 清真窗口（8道）
(7,'清真炒饭',11.00),(7,'牛肉汤面',13.00),(7,'清真小炒',10.00),
(7,'羊肉泡馍',14.00),(7,'手抓饭',12.00),(7,'烤馕',3.00),
(7,'羊肉串(3串)',9.00),(7,'孜然牛肉盖饭',14.00);
