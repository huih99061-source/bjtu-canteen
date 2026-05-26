package com.bjtu.canteen.controller;

import com.bjtu.canteen.model.ApiResult;
import com.bjtu.canteen.model.SimSnapshot;
import com.bjtu.canteen.simulator.CanteenSimulator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 仿真控制API
 * GET  /sim/snapshot  - 获取实时快照（前端每秒轮询）
 * POST /sim/start     - 启动仿真
 * POST /sim/pause     - 暂停仿真
 * POST /sim/reset     - 重置仿真
 * POST /sim/speed     - 设置速度
 * POST /sim/seat/{id}/reserve - 手动占座
 */
@RestController
@RequestMapping("/sim")
@CrossOrigin(origins = "*")
public class SimController {

    @Autowired
    private CanteenSimulator simulator;

    /** 获取实时快照（核心接口，前端轮询） */
    @GetMapping("/snapshot")
    public ApiResult<SimSnapshot> snapshot() {
        return ApiResult.ok(simulator.getSnapshot());
    }

    /** 启动仿真 */
    @PostMapping("/start")
    public ApiResult<?> start() {
        simulator.start();
        return ApiResult.ok("仿真已启动");
    }

    /** 暂停仿真 */
    @PostMapping("/pause")
    public ApiResult<?> pause() {
        simulator.pause();
        return ApiResult.ok("仿真已暂停");
    }

    /** 重置仿真 */
    @PostMapping("/reset")
    public ApiResult<?> reset() {
        simulator.reset();
        return ApiResult.ok("仿真已重置");
    }

    /** 设置仿真速度（倍率） */
    @PostMapping("/speed")
    public ApiResult<?> speed(@RequestBody Map<String, Integer> body) {
        Integer speed = body.get("speed");
        if (speed == null || speed < 1) return ApiResult.fail("速度参数无效");
        simulator.setSpeed(speed);
        return ApiResult.ok("速度已设为 " + speed + "x");
    }

    /** 手动占座 */
    @PostMapping("/seat/{seatId}/reserve")
    public ApiResult<?> reserveSeat(@PathVariable int seatId) {
        boolean ok = simulator.reserveSeat(seatId);
        if (ok) return ApiResult.ok("占座成功，座位号: " + seatId);
        return ApiResult.fail("占座失败，该座位不是空座");
    }
}
