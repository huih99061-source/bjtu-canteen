package com.bjtu.canteen.model;

import java.util.List;

/**
 * 窗口（档口）模型
 */
public class WindowInfo {
    private Integer id;
    private Integer floorId;
    private String windowName;
    private String category;
    private Integer avgServeSeconds;
    private Integer isOpen;
    private List<Food> foods;

    // 仿真实时字段
    private Integer queueCount;       // 当前排队人数
    private Integer estimatedWaitSec; // 预计等待秒数
    private String queueLevel;        // 排队等级: low/medium/high

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getFloorId() { return floorId; }
    public void setFloorId(Integer floorId) { this.floorId = floorId; }
    public String getWindowName() { return windowName; }
    public void setWindowName(String windowName) { this.windowName = windowName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Integer getAvgServeSeconds() { return avgServeSeconds; }
    public void setAvgServeSeconds(Integer avgServeSeconds) { this.avgServeSeconds = avgServeSeconds; }
    public Integer getIsOpen() { return isOpen; }
    public void setIsOpen(Integer isOpen) { this.isOpen = isOpen; }
    public List<Food> getFoods() { return foods; }
    public void setFoods(List<Food> foods) { this.foods = foods; }
    public Integer getQueueCount() { return queueCount; }
    public void setQueueCount(Integer queueCount) {
        this.queueCount = queueCount;
        // 自动计算等待时间和等级
        if (avgServeSeconds != null) {
            this.estimatedWaitSec = queueCount * avgServeSeconds;
        }
        if (queueCount <= 3) this.queueLevel = "low";
        else if (queueCount <= 8) this.queueLevel = "medium";
        else this.queueLevel = "high";
    }
    public Integer getEstimatedWaitSec() { return estimatedWaitSec; }
    public void setEstimatedWaitSec(Integer estimatedWaitSec) { this.estimatedWaitSec = estimatedWaitSec; }
    public String getQueueLevel() { return queueLevel; }
    public void setQueueLevel(String queueLevel) { this.queueLevel = queueLevel; }
}
