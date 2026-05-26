package com.bjtu.canteen.model;

import java.util.List;

/**
 * 楼层模型
 */
public class Floor {
    private Integer id;
    private String floorName;
    private Integer windowCount;
    private Integer seatCount;
    private String openTime;
    private String closeTime;
    private List<WindowInfo> windows;
    private Integer currentPeople;  // 当前人数（仿真实时）
    private Integer emptySeatCount; // 空座数量（仿真实时）

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getFloorName() { return floorName; }
    public void setFloorName(String floorName) { this.floorName = floorName; }
    public Integer getWindowCount() { return windowCount; }
    public void setWindowCount(Integer windowCount) { this.windowCount = windowCount; }
    public Integer getSeatCount() { return seatCount; }
    public void setSeatCount(Integer seatCount) { this.seatCount = seatCount; }
    public String getOpenTime() { return openTime; }
    public void setOpenTime(String openTime) { this.openTime = openTime; }
    public String getCloseTime() { return closeTime; }
    public void setCloseTime(String closeTime) { this.closeTime = closeTime; }
    public List<WindowInfo> getWindows() { return windows; }
    public void setWindows(List<WindowInfo> windows) { this.windows = windows; }
    public Integer getCurrentPeople() { return currentPeople; }
    public void setCurrentPeople(Integer currentPeople) { this.currentPeople = currentPeople; }
    public Integer getEmptySeatCount() { return emptySeatCount; }
    public void setEmptySeatCount(Integer emptySeatCount) { this.emptySeatCount = emptySeatCount; }
}
