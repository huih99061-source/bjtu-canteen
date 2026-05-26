// package com.bjtu.canteen.simulator;

// import com.bjtu.canteen.model.Seat;
// import com.bjtu.canteen.model.SimSnapshot;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.stereotype.Component;

// import jakarta.annotation.PostConstruct;
// import java.time.LocalDate;
// import java.time.LocalDateTime;
// import java.time.DayOfWeek;
// import java.time.format.DateTimeFormatter;
// import java.util.*;
// import java.util.concurrent.ConcurrentHashMap;

// /**
//  * 食堂就餐仿真引擎
//  *
//  * 仿真规则：
//  * 1. 人流量按就餐时段差异化（早/午/晚 泊松分布，非高峰均匀分布）
//  * 2. 座位状态: empty → reserved → dining → empty
//  * 3. 窗口排队: 人员到达 → 排队 → 打饭完成(avg_serve_seconds) → 离队就餐
//  * 4. 就餐时间: 早餐10-15min，午/晚 20-30min
//  */
// @Component
// public class CanteenSimulator {
//     // 记录排队中的人对应的座位 (personId -> seatId)
// private final Map<Integer, Integer> pendingSeatMap = new ConcurrentHashMap<>();
// private static final int MAX_QUEUE = 25; // 每窗口最大排队人数
// private int personIdCounter = 0;


// private int generatePersonId() {
//     return ++personIdCounter;
// }
//     @Value("${simulation.speed:60}")
//     private int simSpeed; // 仿真速度倍率

//     @Value("${simulation.auto-start:true}")
//     private boolean autoStart;

//     // ---- 仿真时钟 ----
//     private LocalDateTime simDateTime;
//     private boolean running = false;

//     // ---- 窗口排队状态 (windowId -> 队列中的人 List<remaining_serve_ticks>) ----
//     private final Map<Integer, Queue<Integer>> windowQueues = new ConcurrentHashMap<>();
//     // 窗口ID -> 当前排队人数
//     private final Map<Integer, Integer> windowQueueCount = new ConcurrentHashMap<>();

//     // ---- 座位状态 ----
//     // seatId -> {status, remaining_ticks}
//     private final Map<Integer, SeatState> seatStates = new ConcurrentHashMap<>();

//     // ---- 统计 ----
//     private int totalPeople = 0;
//     private int totalSeats = 320; // 80桌×4座

//     // 窗口基础数据（id, avgServeSeconds, name, category）
//     private static final int[][] WINDOW_DATA = {
//         // {id, avgServeSeconds}
//         {1, 80}, {2, 90}, {3, 100}, {4, 60}, {5, 120}, {6, 50}, {7, 90}
//     };
//     private static final String[] WINDOW_NAMES = {
//         "米饭窗口", "面条窗口", "炒菜窗口", "包子馒头", "麻辣烫", "饮品甜点", "清真窗口"
//     };
//     private static final String[] WINDOW_CATS = {
//         "主食/盖饭", "面食", "炒菜/小炒", "主食/点心", "小吃/烫菜", "饮品/甜点", "清真食品"
//     };

//     private final Random random = new Random();

//     @PostConstruct
//     public void init() {
//         // 仿真时间从当天早上6:30开始
//         simDateTime = LocalDate.now().atTime(6, 30);
//         initSeats();
//         initQueues();
//         if (autoStart) {
//             running = true;
//         }
//     }

//     private void initSeats() {
//         seatStates.clear();
//         totalSeats = 320;
//         for (int i = 1; i <= totalSeats; i++) {
//             seatStates.put(i, new SeatState("empty", 0));
//         }
//     }

//     private void initQueues() {
//         windowQueues.clear();
//         windowQueueCount.clear();
//         for (int[] w : WINDOW_DATA) {
//             windowQueues.put(w[0], new LinkedList<>());
//             windowQueueCount.put(w[0], 0);
//         }
//     }

//     /**
//      * 核心Tick - 每隔realMs毫秒调用一次，推进simSpeed秒的仿真时间
//      * 由 SimScheduler 每秒调用
//      */
//     public synchronized void tick() {
//         if (!running) return;

//         // 推进仿真时间
//         simDateTime = simDateTime.plusSeconds(simSpeed);

//         int hour = simDateTime.getHour();
//         int minute = simDateTime.getMinute();
//         String mealPeriod = getMealPeriod(hour);
//         String dayType = getDayType(simDateTime.getDayOfWeek());

//         // 1. 计算本tick新到达人数
//         int arrivals = calcArrivals(mealPeriod, dayType, simSpeed);

//         // 2. 分配到各窗口排队
//         // distributeArrivals(arrivals);
//         //distributeArrivals(arrivals, mealPeriod);

//         arriveAndReserve(arrivals, mealPeriod);
//         // 3. 更新窗口服务进度（各服务1秒 = simSpeed个服务单位）
//         updateWindowQueues(simSpeed, mealPeriod);

//         if ("off".equals(mealPeriod)) {
//             for (int[] w : WINDOW_DATA) {
//                 windowQueues.get(w[0]).clear();
//                 windowQueueCount.put(w[0], 0);
//             }
//         }

//         // 4. 更新座位状态
//         updateSeats(mealPeriod);

//         // 5. 统计当前总人数
//         calcTotalPeople();

//         // 新的一天重置
//         if (hour == 6 && minute < (simSpeed / 60 + 1)) {
//             resetDay();
//         }
//     }

//     /**
//      * 计算本tick新到达的人数（泊松/均匀分布）
//      */
//     private int calcArrivals(String mealPeriod, String dayType, int tickSeconds) {
//         double baseRate; // 每分钟到达人数
//         switch (mealPeriod) {
//             case "breakfast": baseRate = 1.5; break;
//             case "lunch":     baseRate = 8.0; break;
//             case "dinner":    baseRate = 5.0; break;
//             default:          baseRate = 0.0; break;
//         }
//         if ("weekend".equals(dayType)) baseRate *= 0.5;

//         double lambda = baseRate * tickSeconds / 60.0;
//         return poissonSample(lambda);
//     }

//     /**
//      * 泊松分布采样
//      */
//     private int poissonSample(double lambda) {
//         if (lambda <= 0) return 0;
//         int k = 0;
//         double p = 1.0;
//         double L = Math.exp(-lambda);
//         do {
//             k++;
//             p *= random.nextDouble();
//         } while (p > L);
//         return k - 1;
//     }

//     /**
//      * 将新到达人员分配到各窗口
//      * 策略: 优先选择排队少的窗口，加权随机
//      */
//     private void distributeArrivals(int count, String mealPeriod) {
//         // for (int i = 0; i < count; i++) {
//         //     int winId = chooseWindow();
//         //     int avgServe = getAvgServe(winId);
//         //     // 随机波动 ±20%
//         //     int serveTime = (int)(avgServe * (0.8 + random.nextDouble() * 0.4));
//         //     windowQueues.get(winId).offer(serveTime);
//         //     windowQueueCount.put(winId, windowQueues.get(winId).size());
//         // }
//         for (int i = 0; i < count; i++) {
//         int winId = chooseWindow(mealPeriod);
//         int avgServe = getAvgServe(winId);
//         int serveTime = (int)(avgServe * (0.8 + random.nextDouble() * 0.4));
//         windowQueues.get(winId).offer(serveTime);
//         windowQueueCount.put(winId, windowQueues.get(winId).size());
//     }
//     }

//     private void arriveAndReserve(int count, String mealPeriod) {
//     for (int i = 0; i < count; i++) {
//         // 第一步：先找空座占座
//         List<Integer> emptySeats = new ArrayList<>();
//         for (Map.Entry<Integer, SeatState> e : seatStates.entrySet()) {
//             if ("empty".equals(e.getValue().status)) emptySeats.add(e.getKey());
//         }
//         if (emptySeats.isEmpty()) continue; // 没座位了，此人离开

//         // 占座，保留30分钟（等打饭回来）
//         int seatId = emptySeats.get(random.nextInt(emptySeats.size()));
//         seatStates.get(seatId).status = "reserved";
//         seatStates.get(seatId).remainingSeconds = 30 * 60;

//         // 第二步：去排队打饭
//         int[] openWins = getOpenWindows(mealPeriod);
//         if (openWins.length == 0) continue;

//         // 超过上限则放弃
//         int winId = chooseWindow(mealPeriod);
        
//         if (windowQueues.get(winId).size() >= MAX_QUEUE) {
//             boolean found = false;
//             for (int id : openWins) {
//                 if (windowQueues.get(id).size() < MAX_QUEUE) {
//                     winId = id;
//                     found = true;
//                     break;
//                 }
//             }
//             if (!found) {
//                 // 排队满了，放弃占座离开
//                 seatStates.get(seatId).status = "empty";
//                 continue;
//             }
//         }
//         int avgServe = getAvgServe(winId);
//         int serveTime = (int)(avgServe * (0.8 + random.nextDouble() * 0.4));
//         // 把占座的seatId绑定到队列里，打完饭直接回那个座
//         windowQueues.get(winId).offer(serveTime);
//         windowQueueCount.put(winId, windowQueues.get(winId).size());

//         // 记录这个人对应的座位，打完饭回来就餐
//         pendingSeatMap.put(generatePersonId(), seatId);
//     }
// }


//     // 各时段开放的窗口ID
// // 早餐: 面条(2)、包子馒头(4)、清真(7)
// // 午餐/晚餐: 全部开放
// private int[] getOpenWindows(String mealPeriod) {
//     if ("breakfast".equals(mealPeriod)) {
//         return new int[]{2, 4, 7};
//     }
//     return new int[]{1, 2, 3, 4, 5, 6, 7};
// }
//     /**
//      * 加权随机选择窗口（排队少的权重高）
//      */
//     private int chooseWindow(String mealPeriod) {
//         // int[] ids = {1,2,3,4,5,6,7};
//         // double[] weights = new double[7];
//         // double totalWeight = 0;
//         // for (int i = 0; i < 7; i++) {
//         //     int q = windowQueueCount.getOrDefault(ids[i], 0);
//         //     weights[i] = 1.0 / (q + 1);
//         //     totalWeight += weights[i];
//         // }
//         // double r = random.nextDouble() * totalWeight;
//         // double cum = 0;
//         // for (int i = 0; i < 7; i++) {
//         //     cum += weights[i];
//         //     if (r <= cum) return ids[i];
//         // }
//         // return 1;

//         int[] ids = getOpenWindows(mealPeriod);
//     double[] weights = new double[ids.length];
//     double totalWeight = 0;
//     for (int i = 0; i < ids.length; i++) {
//         int q = windowQueueCount.getOrDefault(ids[i], 0);
//         weights[i] = 1.0 / (q + 1);
//         totalWeight += weights[i];
//     }
//     double r = random.nextDouble() * totalWeight;
//     double cum = 0;
//     for (int i = 0; i < ids.length; i++) {
//         cum += weights[i];
//         if (r <= cum) return ids[i];
//     }
//     return ids[0];
//     }

//     private int getAvgServe(int winId) {
//         for (int[] w : WINDOW_DATA) {
//             if (w[0] == winId) return w[1];
//         }
//         return 90;
//     }

//     /**
//      * 更新窗口队列（模拟服务进度）
//      */
//     private void updateWindowQueues(int tickSeconds, String mealPeriod) {
//         for (int[] w : WINDOW_DATA) {
//             int winId = w[0];
//             Queue<Integer> queue = windowQueues.get(winId);
//             if (queue == null || queue.isEmpty()) continue;

//             int remaining = tickSeconds;
//             while (remaining > 0 && !queue.isEmpty()) {
//                 int front = queue.peek();
//                 if (front <= remaining) {
//                     remaining -= front;
//                     queue.poll(); // 服务完成，离队
//                     // 完成服务的人去占座
//                     //reserveOrDineSeat(mealPeriod);
//                     startDining(mealPeriod);
//                 } else {
//                     // 部分服务
//                     queue.poll();
//                     queue.offer(front - remaining);
//                     remaining = 0;
//                 }
//             }
//             windowQueueCount.put(winId, queue.size());
//         }
//     }
//     private void startDining(String mealPeriod) {
//     // 找一个 reserved 状态的座位，改为 dining
//     List<Integer> reservedSeats = new ArrayList<>();
//     for (Map.Entry<Integer, SeatState> e : seatStates.entrySet()) {
//         if ("reserved".equals(e.getValue().status)) reservedSeats.add(e.getKey());
//     }
//     if (reservedSeats.isEmpty()) {
//         // 没有reserved座位，找空座直接就餐
//         List<Integer> emptySeats = new ArrayList<>();
//         for (Map.Entry<Integer, SeatState> e : seatStates.entrySet()) {
//             if ("empty".equals(e.getValue().status)) emptySeats.add(e.getKey());
//         }
//         if (emptySeats.isEmpty()) return;
//         int seatId = emptySeats.get(random.nextInt(emptySeats.size()));
//         setDining(seatId, mealPeriod);
//         return;
//     }
//     int seatId = reservedSeats.get(random.nextInt(reservedSeats.size()));
//     setDining(seatId, mealPeriod);
// }

// private void setDining(int seatId, String mealPeriod) {
//     int dineSeconds;
//     if ("breakfast".equals(mealPeriod)) {
//         dineSeconds = (10 + random.nextInt(6)) * 60;  // 10~15分钟
//     } else {
//         dineSeconds = (20 + random.nextInt(11)) * 60; // 20~30分钟
//     }
//     seatStates.get(seatId).status = "dining";
//     seatStates.get(seatId).remainingSeconds = dineSeconds;
// }

//     /**
//      * 服务完成的人占座或直接就餐
//      */
//     private void reserveOrDineSeat(String mealPeriod) {
//         // // 找一个空座
//         // List<Integer> emptySeats = new ArrayList<>();
//         // for (Map.Entry<Integer, SeatState> e : seatStates.entrySet()) {
//         //     if ("empty".equals(e.getValue().status)) emptySeats.add(e.getKey());
//         // }
//         // if (emptySeats.isEmpty()) return;
//         // int seatId = emptySeats.get(random.nextInt(emptySeats.size()));
//         // // 就餐时间: 15-30分钟（以秒计）
//         // int dineSeconds = (15 + random.nextInt(16)) * 60;
//         // // 转为tick单位（实际秒数/simSpeed = tick数，但我们直接用秒存储剩余就餐秒数）
//         // seatStates.get(seatId).status = "dining";
//         // seatStates.get(seatId).remainingSeconds = dineSeconds;
//         List<Integer> emptySeats = new ArrayList<>();
//     for (Map.Entry<Integer, SeatState> e : seatStates.entrySet()) {
//         if ("empty".equals(e.getValue().status)) emptySeats.add(e.getKey());
//     }
//     // 座位满了也要让人离开队列，不能卡住
//     if (emptySeats.isEmpty()) return;
//     int seatId = emptySeats.get(random.nextInt(emptySeats.size()));
//     // 早餐就餐时间短，午晚餐长
//     int dineSeconds;
//     if ("breakfast".equals(mealPeriod)) {
//         dineSeconds = (10 + random.nextInt(6)) * 60;  // 10~15分钟
//     } else {
//         dineSeconds = (20 + random.nextInt(11)) * 60; // 20~30分钟
//     }
//     seatStates.get(seatId).status = "dining";
//     seatStates.get(seatId).remainingSeconds = dineSeconds;
//     }

//     /**
//      * 更新座位状态
//      */
//     private void updateSeats(String mealPeriod) {
//         for (Map.Entry<Integer, SeatState> e : seatStates.entrySet()) {
//             SeatState s = e.getValue();
//             if ("dining".equals(s.status)) {
//                 s.remainingSeconds -= simSpeed;
//                 if (s.remainingSeconds <= 0) {
//                     s.status = "empty";
//                     s.remainingSeconds = 0;
//                 }
//             } else if ("reserved".equals(s.status)) {
//                 // 占座超时自动变为空座
//                 s.remainingSeconds -= simSpeed;
//                 if (s.remainingSeconds <= 0) {
//                     s.status = "empty";
//                 }
//             }
//         }

//         // 非就餐时间段，逐渐清空座位
//         if ("off".equals(mealPeriod)) {
//             // for (SeatState s : seatStates.values()) {
//             //     if (!"empty".equals(s.status) && random.nextDouble() < 0.01) {
//             //         s.status = "empty";
//             //         s.remainingSeconds = 0;
//             //     }
//             // }
//             for (Map.Entry<Integer, SeatState> e : seatStates.entrySet()) {
//             SeatState s = e.getValue();
//             if (!"empty".equals(s.status)) {
//                 // 额外多扣一倍时间，让人走得更快
//                 s.remainingSeconds -= simSpeed;
//                 if (s.remainingSeconds <= 0) {
//                     s.status = "empty";
//                     s.remainingSeconds = 0;
//                 }
//             }
//         }
//         }
//     }

//     private void calcTotalPeople() {
//         int inQueue = windowQueueCount.values().stream().mapToInt(Integer::intValue).sum();
//         int dining = (int) seatStates.values().stream().filter(s -> "dining".equals(s.status)).count();
//         totalPeople = inQueue + dining;
//     }

//     private void resetDay() {
//         // 次日6:30重置座位和队列
//         initSeats();
//         initQueues();
//         simDateTime = simDateTime.toLocalDate().plusDays(1).atTime(6, 30);
//     }

//     // ======================== 公开查询方法 ========================

//     public synchronized SimSnapshot getSnapshot() {
//         SimSnapshot snap = new SimSnapshot();
//         snap.setSimTime(simDateTime.format(DateTimeFormatter.ofPattern("HH:mm")));
//         snap.setSimDate(simDateTime.format(DateTimeFormatter.ofPattern("MM月dd日")));
//         snap.setMealPeriod(getMealPeriod(simDateTime.getHour()));
//         snap.setDayType(getDayType(simDateTime.getDayOfWeek()));
//         snap.setTotalPeople(totalPeople);
//         snap.setTotalSeatCount(totalSeats);
//         snap.setRunning(running);
//         snap.setSpeed(simSpeed);

//         // 空座统计
//         long empty = seatStates.values().stream().filter(s -> "empty".equals(s.status)).count();
//         snap.setEmptySeatCount((int) empty);

//         // 窗口数据
//         List<SimSnapshot.WindowSimData> winList = new ArrayList<>();
//         for (int i = 0; i < WINDOW_DATA.length; i++) {
//             int winId = WINDOW_DATA[i][0];
//             int avgSec = WINDOW_DATA[i][1];
//             int q = windowQueueCount.getOrDefault(winId, 0);
//             SimSnapshot.WindowSimData wd = new SimSnapshot.WindowSimData();
//             wd.setId(winId);
//             wd.setWindowName(WINDOW_NAMES[i]);
//             wd.setCategory(WINDOW_CATS[i]);
//             wd.setQueueCount(q);
//             wd.setEstimatedWaitMin((q * avgSec) / 60);
//             if (q <= 3) wd.setQueueLevel("low");
//             else if (q <= 8) wd.setQueueLevel("medium");
//             else wd.setQueueLevel("high");
//             winList.add(wd);
//         }
//         snap.setWindows(winList);

//         // 座位数据
//         List<Seat> seats = new ArrayList<>();
//         for (Map.Entry<Integer, SeatState> e : seatStates.entrySet()) {
//             Seat seat = new Seat();
//             seat.setId(e.getKey());
//             seat.setFloorId(1);
//             // tableId = (id-1)/4 + 1
//             seat.setTableId((e.getKey() - 1) / 4 + 1);
//             seat.setSeatIndex((e.getKey() - 1) % 4 + 1);
//             seat.setStatus(e.getValue().status);
//             seats.add(seat);
//         }
//         snap.setSeats(seats);

//         return snap;
//     }

//     // ======================== 控制方法 ========================

//     public synchronized void start() { running = true; }
//     public synchronized void pause() { running = false; }
//     public synchronized void reset() {
//         simDateTime = LocalDate.now().atTime(6, 30);
//         initSeats();
//         initQueues();
//         totalPeople = 0;
//     }
//     public synchronized void setSpeed(int speed) {
//         this.simSpeed = Math.max(1, Math.min(speed, 3600));
//     }
//     public synchronized boolean isRunning() { return running; }
//     public synchronized int getSpeed() { return simSpeed; }

//     // 手动占座
//     public synchronized boolean reserveSeat(int seatId) {
//         SeatState s = seatStates.get(seatId);
//         if (s != null && "empty".equals(s.status)) {
//             s.status = "reserved";
//             s.remainingSeconds = 30 * 60; // 30分钟保留期
//             return true;
//         }
//         return false;
//     }

//     // ======================== 工具方法 ========================

//     private String getMealPeriod(int hour) {
//         if (hour >= 7 && hour < 9) return "breakfast";
//         if (hour >= 11 && hour < 13) return "lunch";
//         if (hour >= 17 && hour < 19) return "dinner";
//         return "off";
//     }

//     private String getDayType(DayOfWeek dow) {
//         return (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) ? "weekend" : "workday";
//     }

//     // 座位内部状态
//     static class SeatState {
//         String status;
//         int remainingSeconds;
//         SeatState(String status, int remaining) {
//             this.status = status;
//             this.remainingSeconds = remaining;
//         }
//     }
// }




package com.bjtu.canteen.simulator;

import com.bjtu.canteen.model.Seat;
import com.bjtu.canteen.model.SimSnapshot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class CanteenSimulator {

    @Value("${simulation.speed:60}")
    private int simSpeed;

    @Value("${simulation.auto-start:true}")
    private boolean autoStart;

    // 仿真时钟
    private LocalDateTime simDateTime;
    private boolean running = false;

    // 窗口基础数据
    private static final int[] WIN_IDS      = {1, 2, 3, 4, 5, 6, 7};
    private static final int[] WIN_AVG_SEC  = {80,90,100,60,120,50,90};
    private static final String[] WIN_NAMES = {"米饭窗口","面条窗口","炒菜窗口","包子馒头","麻辣烫","饮品甜点","清真窗口"};
    private static final String[] WIN_CATS  = {"主食/盖饭","面食","炒菜/小炒","主食/点心","小吃/烫菜","饮品/甜点","清真食品"};
    private static final int MAX_QUEUE = 15;
    private static final int TOTAL_SEATS = 320; // 80桌×4座

    // 座位状态
    private static class SeatState {
        String status = "empty"; // empty / reserved / dining
        int remainingSeconds = 0;
    }
    private final Map<Integer, SeatState> seatStates = new ConcurrentHashMap<>();

    // 每个人的状态
    private static class Person {
        int seatId;          // 已占的座位ID
        int windowId;        // 正在排队的窗口ID
        int remainServeSeconds; // 还需等待的服务秒数
        boolean served;      // 是否已打完饭

        Person(int seatId, int windowId, int serveSeconds) {
            this.seatId = seatId;
            this.windowId = windowId;
            this.remainServeSeconds = serveSeconds;
            this.served = false;
        }
    }

    // 正在排队的人（按窗口分组）
    private final Map<Integer, List<Person>> windowPersonQueues = new ConcurrentHashMap<>();

    // 窗口排队人数缓存（用于快速读取）
    private final Map<Integer, Integer> windowQueueCount = new ConcurrentHashMap<>();

    private final Random random = new Random();

    @PostConstruct
    public void init() {
        simDateTime = LocalDate.now().atTime(6, 30);
        initSeats();
        initQueues();
        if (autoStart) running = true;
    }

    private void initSeats() {
        seatStates.clear();
        for (int i = 1; i <= TOTAL_SEATS; i++) {
            seatStates.put(i, new SeatState());
        }
    }

    private void initQueues() {
        windowPersonQueues.clear();
        windowQueueCount.clear();
        for (int id : WIN_IDS) {
            windowPersonQueues.put(id, new CopyOnWriteArrayList<>());
            windowQueueCount.put(id, 0);
        }
    }

    // ========== 核心Tick ==========
    public synchronized void tick() {
        if (!running) return;

        simDateTime = simDateTime.plusSeconds(simSpeed);
        int hour = simDateTime.getHour();
        int minute = simDateTime.getMinute();
        String mealPeriod = getMealPeriod(hour);
        String dayType = getDayType(simDateTime.getDayOfWeek());

        // 1. 新人到达：先占座，再排队
        int arrivals = calcArrivals(mealPeriod, dayType, simSpeed, hour, minute);
        for (int i = 0; i < arrivals; i++) {
            tryArrive(mealPeriod);
        }

        // 2. 推进排队服务进度
        processQueues(simSpeed, mealPeriod);

        // 3. 推进就餐倒计时
        processDining(mealPeriod);

        // 4. 非就餐时段：不清空队列，让剩余排队人员继续被服务直至队列清空
        //    同时加速座位清场（dining额外再扣一倍速）
        if ("off".equals(mealPeriod)) {
            for (Map.Entry<Integer, SeatState> e : seatStates.entrySet()) {
                SeatState s = e.getValue();
                if ("reserved".equals(s.status)) {
                    s.remainingSeconds -= simSpeed;
                    if (s.remainingSeconds <= 0) {
                        s.status = "empty";
                        s.remainingSeconds = 0;
                    }
                } else if ("dining".equals(s.status)) {
                    // 额外再扣一倍，加速就餐人员离场
                    s.remainingSeconds -= simSpeed;
                    if (s.remainingSeconds <= 0) {
                        s.status = "empty";
                        s.remainingSeconds = 0;
                    }
                }
            }
        }
    }

    /**
     * 一个人到达：先占座，再去排队
     */
    private void tryArrive(String mealPeriod) {
        // 找空座
        List<Integer> emptyList = new ArrayList<>();
        for (Map.Entry<Integer, SeatState> e : seatStates.entrySet()) {
            if ("empty".equals(e.getValue().status)) emptyList.add(e.getKey());
        }
        if (emptyList.isEmpty()) return; // 没座，离开

        // 占座
        int seatId = emptyList.get(random.nextInt(emptyList.size()));
        seatStates.get(seatId).status = "reserved";
        seatStates.get(seatId).remainingSeconds = 40 * 60; // 最多等40分钟

        // 选窗口排队
        int winId = chooseWindow(mealPeriod);
        if (winId < 0) {
            // 没有可用窗口，放弃座位离开
            seatStates.get(seatId).status = "empty";
            return;
        }

        // 计算服务时间
        int avgSec = WIN_AVG_SEC[winId - 1];
        int serveTime = (int)(avgSec * (0.8 + random.nextDouble() * 0.4));

        // 加入队列
        Person p = new Person(seatId, winId, serveTime);
        windowPersonQueues.get(winId).add(p);
        windowQueueCount.put(winId, windowPersonQueues.get(winId).size());
    }

    /**
     * 推进窗口服务：每个窗口同时服务1人，消耗服务时间
     */
    private void processQueues(int tickSeconds, String mealPeriod) {
        for (int winId : WIN_IDS) {
            List<Person> queue = windowPersonQueues.get(winId);
            if (queue == null || queue.isEmpty()) continue;

            int timeLeft = tickSeconds;
            while (timeLeft > 0 && !queue.isEmpty()) {
                Person first = queue.get(0);
                if (first.remainServeSeconds <= timeLeft) {
                    // 服务完成
                    timeLeft -= first.remainServeSeconds;
                    queue.remove(0);
                    // 打完饭回到已占的座位就餐
                    SeatState seat = seatStates.get(first.seatId);
                    if (seat != null) {
                        int dineSeconds = calcDineSeconds(mealPeriod);
                        seat.status = "dining";
                        seat.remainingSeconds = dineSeconds;
                    }
                } else {
                    // 部分消耗
                    first.remainServeSeconds -= timeLeft;
                    timeLeft = 0;
                }
            }
            windowQueueCount.put(winId, queue.size());
        }
    }

    /**
     * 推进就餐倒计时，吃完离开
     */
    private void processDining(String mealPeriod) {
        for (SeatState s : seatStates.values()) {
            if ("dining".equals(s.status)) {
                s.remainingSeconds -= simSpeed;
                if (s.remainingSeconds <= 0) {
                    s.status = "empty";
                    s.remainingSeconds = 0;
                }
            }
        }
    }

    /**
     * 非就餐时段：加速清场，清空所有队列
     */
    private void clearAll() {
        // 清空队列，队列中的人放弃座位离开
        for (int winId : WIN_IDS) {
            List<Person> queue = windowPersonQueues.get(winId);
            if (queue != null) {
                for (Person p : queue) {
                    SeatState s = seatStates.get(p.seatId);
                    if (s != null) s.status = "empty";
                }
                queue.clear();
            }
            windowQueueCount.put(winId, 0);
        }
        // 就餐中的人加速离开（额外再扣一倍）
        for (SeatState s : seatStates.values()) {
            if ("dining".equals(s.status)) {
                s.remainingSeconds -= simSpeed;
                if (s.remainingSeconds <= 0) {
                    s.status = "empty";
                }
            }
        }
    }

    // ========== 工具方法 ==========

    private int calcDineSeconds(String mealPeriod) {
        if ("breakfast".equals(mealPeriod)) {
            return (10 + random.nextInt(6)) * 60;
        }
        return (20 + random.nextInt(11)) * 60;
    }

    private int calcArrivals(String mealPeriod, String dayType, int tickSeconds, int hour, int minute) {
        double rate;
        switch (mealPeriod) {
            case "breakfast": rate = 3.0;  break;
            case "lunch":     rate = 10.0; break;
            case "dinner":    rate = 7.0;  break;
            default:          return 0;
        }
        if ("weekend".equals(dayType)) rate *= 0.5;
        double mult = getArrivalMultiplier(mealPeriod, hour, minute);
        return poissonSample(rate * mult * tickSeconds / 60.0);
    }

    // 梯形：前30分钟爬坡，中间60分钟高峰，后30分钟下坡
    private double getArrivalMultiplier(String mealPeriod, int hour, int minute) {
        int t;
        switch (mealPeriod) {
            case "breakfast": t = (hour - 7)  * 60 + minute; break;
            case "lunch":     t = (hour - 11) * 60 + minute; break;
            case "dinner":    t = (hour - 17) * 60 + minute; break;
            default:          return 0;
        }
        if (t < 0 || t >= 120) return 0;
        if (t < 30) return t / 30.0;
        if (t < 90) return 1.0;
        return (120 - t) / 30.0;
    }

    private int poissonSample(double lambda) {
        if (lambda <= 0) return 0;
        int k = 0;
        double p = 1.0, L = Math.exp(-Math.min(lambda, 20));
        do { k++; p *= random.nextDouble(); } while (p > L);
        return k - 1;
    }

    private int[] getOpenWindows(String mealPeriod) {
        if ("breakfast".equals(mealPeriod)) return new int[]{2, 4, 7};
        return WIN_IDS;
    }

    private int chooseWindow(String mealPeriod) {
        int[] open = getOpenWindows(mealPeriod);
        // 过滤掉已满的窗口
        List<Integer> available = new ArrayList<>();
        for (int id : open) {
            if (windowPersonQueues.get(id).size() < MAX_QUEUE) {
                available.add(id);
            }
        }
        if (available.isEmpty()) return -1;

        // 加权随机，排队少的权重高
        double total = 0;
        double[] weights = new double[available.size()];
        for (int i = 0; i < available.size(); i++) {
            weights[i] = 1.0 / (windowPersonQueues.get(available.get(i)).size() + 1);
            total += weights[i];
        }
        double r = random.nextDouble() * total;
        double cum = 0;
        for (int i = 0; i < available.size(); i++) {
            cum += weights[i];
            if (r <= cum) return available.get(i);
        }
        return available.get(0);
    }

    private String getMealPeriod(int hour) {
        if (hour >= 7  && hour < 9)  return "breakfast";
        if (hour >= 11 && hour < 13) return "lunch";
        if (hour >= 17 && hour < 19) return "dinner";
        return "off";
    }

    private String getDayType(DayOfWeek dow) {
        return (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) ? "weekend" : "workday";
    }

    // ========== 公开查询 ==========

    public synchronized SimSnapshot getSnapshot() {
        SimSnapshot snap = new SimSnapshot();
        snap.setSimTime(simDateTime.format(DateTimeFormatter.ofPattern("HH:mm")));
        snap.setSimDate(simDateTime.format(DateTimeFormatter.ofPattern("MM月dd日")));
        snap.setMealPeriod(getMealPeriod(simDateTime.getHour()));
        snap.setDayType(getDayType(simDateTime.getDayOfWeek()));
        snap.setRunning(running);
        snap.setSpeed(simSpeed);
        snap.setTotalSeatCount(TOTAL_SEATS);

        long empty = seatStates.values().stream().filter(s -> "empty".equals(s.status)).count();
        long dining = seatStates.values().stream().filter(s -> "dining".equals(s.status)).count();
        long reserved = seatStates.values().stream().filter(s -> "reserved".equals(s.status)).count();
        snap.setEmptySeatCount((int) empty);

        int inQueue = windowQueueCount.values().stream().mapToInt(Integer::intValue).sum();
        snap.setTotalPeople((int)(dining + reserved + inQueue));

        // 窗口数据
        List<SimSnapshot.WindowSimData> winList = new ArrayList<>();
        for (int i = 0; i < WIN_IDS.length; i++) {
            int winId = WIN_IDS[i];
            int q = windowQueueCount.getOrDefault(winId, 0);
            SimSnapshot.WindowSimData wd = new SimSnapshot.WindowSimData();
            wd.setId(winId);
            wd.setWindowName(WIN_NAMES[i]);
            wd.setCategory(WIN_CATS[i]);
            wd.setQueueCount(q);
            wd.setEstimatedWaitMin(q * WIN_AVG_SEC[i] / 60);
            wd.setQueueLevel(q <= 3 ? "low" : q <= 8 ? "medium" : "high");
            winList.add(wd);
        }
        snap.setWindows(winList);

        // 座位数据
        List<Seat> seats = new ArrayList<>();
        for (Map.Entry<Integer, SeatState> e : seatStates.entrySet()) {
            Seat seat = new Seat();
            seat.setId(e.getKey());
            seat.setFloorId(1);
            seat.setTableId((e.getKey() - 1) / 4 + 1);
            seat.setSeatIndex((e.getKey() - 1) % 4 + 1);
            seat.setStatus(e.getValue().status);
            seats.add(seat);
        }
        snap.setSeats(seats);

        return snap;
    }

    // ========== 控制 ==========

    public synchronized void start() { running = true; }
    public synchronized void pause() { running = false; }
    public synchronized void reset() {
        simDateTime = LocalDate.now().atTime(6, 30);
        initSeats();
        initQueues();
    }
    public synchronized void setSpeed(int speed) {
        this.simSpeed = Math.max(1, Math.min(speed, 3600));
    }
    public synchronized boolean isRunning() { return running; }
    public synchronized int getSpeed() { return simSpeed; }

    public synchronized boolean reserveSeat(int seatId) {
        SeatState s = seatStates.get(seatId);
        if (s != null && "empty".equals(s.status)) {
            s.status = "reserved";
            s.remainingSeconds = 30 * 60;
            return true;
        }
        return false;
    }
}