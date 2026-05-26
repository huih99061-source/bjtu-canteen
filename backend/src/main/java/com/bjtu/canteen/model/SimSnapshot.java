package com.bjtu.canteen.model;

import java.util.List;

/**
 * 仿真状态快照 - 前端轮询的核心数据结构
 */
public class SimSnapshot {
    private String simTime;          // 仿真时间 HH:mm
    private String simDate;          // 仿真日期
    private String mealPeriod;       // 就餐时段: breakfast/lunch/dinner/off
    private String dayType;          // 日期类型: workday/weekend
    private Integer totalPeople;     // 食堂总人数
    private Integer emptySeatCount;  // 空座数
    private Integer totalSeatCount;  // 总座数
    private Integer speed;           // 当前仿真速度
    private boolean running;         // 是否运行中

    private List<WindowSimData> windows;  // 各窗口实时数据
    private List<Seat> seats;             // 座位状态

    public static class WindowSimData {
        private Integer id;
        private String windowName;
        private String category;
        private Integer queueCount;
        private String queueLevel;
        private Integer estimatedWaitMin;

        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        public String getWindowName() { return windowName; }
        public void setWindowName(String windowName) { this.windowName = windowName; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public Integer getQueueCount() { return queueCount; }
        public void setQueueCount(Integer queueCount) { this.queueCount = queueCount; }
        public String getQueueLevel() { return queueLevel; }
        public void setQueueLevel(String queueLevel) { this.queueLevel = queueLevel; }
        public Integer getEstimatedWaitMin() { return estimatedWaitMin; }
        public void setEstimatedWaitMin(Integer estimatedWaitMin) { this.estimatedWaitMin = estimatedWaitMin; }
    }

    public String getSimTime() { return simTime; }
    public void setSimTime(String simTime) { this.simTime = simTime; }
    public String getSimDate() { return simDate; }
    public void setSimDate(String simDate) { this.simDate = simDate; }
    public String getMealPeriod() { return mealPeriod; }
    public void setMealPeriod(String mealPeriod) { this.mealPeriod = mealPeriod; }
    public String getDayType() { return dayType; }
    public void setDayType(String dayType) { this.dayType = dayType; }
    public Integer getTotalPeople() { return totalPeople; }
    public void setTotalPeople(Integer totalPeople) { this.totalPeople = totalPeople; }
    public Integer getEmptySeatCount() { return emptySeatCount; }
    public void setEmptySeatCount(Integer emptySeatCount) { this.emptySeatCount = emptySeatCount; }
    public Integer getTotalSeatCount() { return totalSeatCount; }
    public void setTotalSeatCount(Integer totalSeatCount) { this.totalSeatCount = totalSeatCount; }
    public Integer getSpeed() { return speed; }
    public void setSpeed(Integer speed) { this.speed = speed; }
    public boolean isRunning() { return running; }
    public void setRunning(boolean running) { this.running = running; }
    public List<WindowSimData> getWindows() { return windows; }
    public void setWindows(List<WindowSimData> windows) { this.windows = windows; }
    public List<Seat> getSeats() { return seats; }
    public void setSeats(List<Seat> seats) { this.seats = seats; }
}
