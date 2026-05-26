package com.bjtu.canteen.controller;

import com.bjtu.canteen.model.ApiResult;
import com.bjtu.canteen.model.Feedback;
import com.bjtu.canteen.model.Food;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import com.bjtu.canteen.mapper.CanteenMapper;
import com.bjtu.canteen.mapper.FeedbackMapper;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 菜品与反馈API（内存版，无需数据库即可运行演示）
 *
 * GET  /canteen/windows    - 获取所有窗口及菜品
 * GET  /canteen/window/{id} - 获取单个窗口菜品详情
 * POST /canteen/feedback   - 提交评价
 * GET  /canteen/feedback   - 查看评价列表
 */
@RestController
@RequestMapping("/canteen")
@CrossOrigin(origins = "*")
public class CanteenController {

    // ---- 内置数据（不需要数据库也能跑） ----
    private static final List<Map<String, Object>> WINDOWS = new ArrayList<>();
    private static final List<Feedback> FEEDBACKS = new CopyOnWriteArrayList<>();
    @Autowired
    private FeedbackMapper feedbackMapper;
    @Autowired
    private CanteenMapper canteenMapper;
    static {
        String[][] foodData = {
            // windowId, name, price
            {"1","红烧肉盖饭","12.00"},{"1","鱼香茄子盖饭","10.00"},
            {"1","番茄炒蛋盖饭","9.00"},{"1","宫保鸡丁盖饭","11.00"},
            {"2","牛肉面","13.00"},{"2","排骨面","12.00"},
            {"2","素汤面","8.00"},{"2","炸酱面","9.00"},
            {"3","红烧肉","9.00"},{"3","清炒时蔬","5.00"},
            {"3","麻婆豆腐","6.00"},{"3","土豆丝","4.00"},
            {"4","大肉包","2.50"},{"4","豆沙包","2.00"},
            {"4","馒头","1.00"},{"4","花卷","1.50"},
            {"5","麻辣烫(素)","10.00"},{"5","麻辣烫(荤)","14.00"},{"5","冒菜","12.00"},
            {"6","珍珠奶茶","8.00"},{"6","豆浆","3.00"},{"6","酸奶","5.00"},{"6","蛋糕","6.00"},
            {"7","清真炒饭","11.00"},{"7","牛肉汤面","13.00"},{"7","清真小炒","10.00"}
        };

        String[] names = {"米饭窗口","面条窗口","炒菜窗口","包子馒头","麻辣烫","饮品甜点","清真窗口"};
        String[] cats  = {"主食/盖饭","面食","炒菜/小炒","主食/点心","小吃/烫菜","饮品/甜点","清真食品"};

        for (int i = 1; i <= 7; i++) {
            Map<String, Object> win = new LinkedHashMap<>();
            win.put("id", i);
            win.put("windowName", names[i-1]);
            win.put("category", cats[i-1]);
            win.put("isOpen", 1);
            List<Map<String,Object>> foods = new ArrayList<>();
            for (String[] fd : foodData) {
                if (fd[0].equals(String.valueOf(i))) {
                    Map<String,Object> f = new LinkedHashMap<>();
                    f.put("id", foods.size()+1);
                    f.put("foodName", fd[1]);
                    f.put("foodPrice", fd[2]);
                    foods.add(f);
                }
            }
            win.put("foods", foods);
            WINDOWS.add(win);
        }
    }

    /** 获取所有窗口 */
    @GetMapping("/windows")
    public ApiResult<?> getWindows() {
        return ApiResult.ok(WINDOWS);
    }

    /** 获取单个窗口详情 */
    @GetMapping("/window/{id}")
    public ApiResult<?> getWindow(@PathVariable int id) {
        // 从数据库读菜品
        List<Food> foods = canteenMapper.findFoodsByWindow(id);
        Map<String, Object> win = WINDOWS.stream()
            .filter(w -> w.get("id").equals(id))
            .findFirst().orElse(null);
        if (win == null) return ApiResult.fail("窗口不存在");
        Map<String, Object> result = new java.util.LinkedHashMap<>(win);
        result.put("foods", foods);  // 用数据库数据覆盖
        return ApiResult.ok(result);
    }

    /** 提交评价 */
    @PostMapping("/feedback")
    public ApiResult<?> addFeedback(@RequestBody Feedback fb) {
        if (fb.getRating() == null || fb.getRating() < 1 || fb.getRating() > 5)
        return ApiResult.fail("评分必须在1-5之间");
        feedbackMapper.insert(fb);
        return ApiResult.ok("评价提交成功");
    }

    /** 获取评价列表 */
    @GetMapping("/feedback")
    public ApiResult<?> getFeedback(@RequestParam(defaultValue = "20") int limit) {
       return ApiResult.ok(feedbackMapper.findRecent(limit));
    }
}
