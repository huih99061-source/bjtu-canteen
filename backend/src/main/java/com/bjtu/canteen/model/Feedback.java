package com.bjtu.canteen.model;

/**
 * 评价/反馈模型
 */
public class Feedback {
    private Integer id;
    private Integer windowId;
    private Integer foodId;
    private Integer rating;
    private String commentText;
    private String createdAt;
    private String windowName; // 关联查询

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getWindowId() { return windowId; }
    public void setWindowId(Integer windowId) { this.windowId = windowId; }
    public Integer getFoodId() { return foodId; }
    public void setFoodId(Integer foodId) { this.foodId = foodId; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getCommentText() { return commentText; }
    public void setCommentText(String commentText) { this.commentText = commentText; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getWindowName() { return windowName; }
    public void setWindowName(String windowName) { this.windowName = windowName; }
}
