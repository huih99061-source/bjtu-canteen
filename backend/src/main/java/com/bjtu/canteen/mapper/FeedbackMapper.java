package com.bjtu.canteen.mapper;

import com.bjtu.canteen.model.Feedback;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface FeedbackMapper {

    @Insert("INSERT INTO feedback(window_id, food_id, rating, comment_text) VALUES(#{windowId}, #{foodId}, #{rating}, #{commentText})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Feedback feedback);

    @Select("SELECT f.*, w.window_name FROM feedback f LEFT JOIN window_info w ON f.window_id = w.id ORDER BY f.created_at DESC LIMIT #{limit}")
    @Results({
        @Result(property = "windowId",     column = "window_id"),
        @Result(property = "foodId",       column = "food_id"),
        @Result(property = "commentText",  column = "comment_text"),
        @Result(property = "createdAt",    column = "created_at"),
        @Result(property = "windowName",   column = "window_name")
    })
    List<Feedback> findRecent(@Param("limit") int limit);
}