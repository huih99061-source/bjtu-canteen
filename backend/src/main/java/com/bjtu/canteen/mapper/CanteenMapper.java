package com.bjtu.canteen.mapper;

import com.bjtu.canteen.model.Food;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface CanteenMapper {

    @Select("SELECT id, window_id, food_name, food_price, food_amount FROM food WHERE window_id = #{windowId}")
    @Results({
        @Result(property = "windowId",   column = "window_id"),
        @Result(property = "foodName",   column = "food_name"),
        @Result(property = "foodPrice",  column = "food_price"),
        @Result(property = "foodAmount", column = "food_amount")
    })
    List<Food> findFoodsByWindow(@Param("windowId") int windowId);
}