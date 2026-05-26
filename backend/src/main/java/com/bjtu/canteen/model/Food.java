package com.bjtu.canteen.model;

import java.math.BigDecimal;

/**
 * 菜品模型
 */
public class Food {
    private Integer id;
    private Integer windowId;
    private String foodName;
    private BigDecimal foodPrice;
    private Integer foodAmount;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getWindowId() { return windowId; }
    public void setWindowId(Integer windowId) { this.windowId = windowId; }
    public String getFoodName() { return foodName; }
    public void setFoodName(String foodName) { this.foodName = foodName; }
    public BigDecimal getFoodPrice() { return foodPrice; }
    public void setFoodPrice(BigDecimal foodPrice) { this.foodPrice = foodPrice; }
    public Integer getFoodAmount() { return foodAmount; }
    public void setFoodAmount(Integer foodAmount) { this.foodAmount = foodAmount; }
}
