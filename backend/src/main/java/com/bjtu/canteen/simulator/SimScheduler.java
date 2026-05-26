package com.bjtu.canteen.simulator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 仿真调度器 - 每秒触发一次仿真Tick
 */
@Component
public class SimScheduler {

    @Autowired
    private CanteenSimulator simulator;

    /**
     * 每1000ms（1秒真实时间）触发一次仿真
     * 仿真引擎内部会根据speed倍率推进相应的仿真时间
     */
    @Scheduled(fixedRate = 1000)
    public void tick() {
        simulator.tick();
    }
}
