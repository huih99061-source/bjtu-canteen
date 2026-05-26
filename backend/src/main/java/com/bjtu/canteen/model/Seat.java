package com.bjtu.canteen.model;

/**
 * 座位模型
 * 状态: empty(空座) | reserved(占座) | dining(就餐中)
 */
public class Seat {
    private Integer id;
    private Integer floorId;
    private Integer tableId;
    private Integer seatIndex;
    private Double xPos;
    private Double yPos;
    private String status; // empty / reserved / dining

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getFloorId() { return floorId; }
    public void setFloorId(Integer floorId) { this.floorId = floorId; }
    public Integer getTableId() { return tableId; }
    public void setTableId(Integer tableId) { this.tableId = tableId; }
    public Integer getSeatIndex() { return seatIndex; }
    public void setSeatIndex(Integer seatIndex) { this.seatIndex = seatIndex; }
    public Double getXPos() { return xPos; }
    public void setXPos(Double xPos) { this.xPos = xPos; }
    public Double getYPos() { return yPos; }
    public void setYPos(Double yPos) { this.yPos = yPos; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
