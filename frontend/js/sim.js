// ====================================================
// sim.js - 前端本地仿真（后端不可用时的备用模式）
// 同时管理轮询逻辑
// ====================================================

let pollingTimer = null;
let localSimRunning = false;
let useLocalSim = false; // 是否使用本地仿真（后端不可用时）

// ---- 本地仿真状态 ----
const localState = {
  hour: 6,   // 从早上6:30开始
  minute: 30,
  second: 0,
  dayType: 'workday',
  speed: 60,
  running: true,
  totalSeats: 320,
  seats: [],
  windows: [
    { id: 1, windowName: '米饭窗口', category: '主食/盖饭', avgSec: 80, queueCount: 0, serveAccum: 0 },
    { id: 2, windowName: '面条窗口', category: '面食', avgSec: 90, queueCount: 0, serveAccum: 0 },
    { id: 3, windowName: '炒菜窗口', category: '炒菜/小炒', avgSec: 100, queueCount: 0, serveAccum: 0 },
    { id: 4, windowName: '包子馒头', category: '主食/点心', avgSec: 60, queueCount: 0, serveAccum: 0 },
    { id: 5, windowName: '麻辣烫', category: '小吃/烫菜', avgSec: 120, queueCount: 0, serveAccum: 0 },
    { id: 6, windowName: '饮品甜点', category: '饮品/甜点', avgSec: 50, queueCount: 0, serveAccum: 0 },
    { id: 7, windowName: '清真窗口', category: '清真食品', avgSec: 90, queueCount: 0, serveAccum: 0 },
  ],
  seatTimers: [], // 各座位剩余秒数
};

function initLocalSim() {
  localState.seats = [];
  localState.seatTimers = [];
  for (let i = 1; i <= localState.totalSeats; i++) {
    // windowId: 记录此座位对应去哪个窗口排队（-1表示空闲）
    localState.seats.push({ id: i, status: 'empty', tableId: Math.ceil(i / 4), seatIndex: ((i - 1) % 4) + 1, windowId: -1 });
    localState.seatTimers.push(0);
  }
  // 重置服务累积器
  localState.windows.forEach(w => { w.serveAccum = 0; });
}

function localSimTick() {
  if (!localState.running) return;

  const speed = localState.speed;
  localState.second += speed;
  if (localState.second >= 60) {
    localState.minute += Math.floor(localState.second / 60);
    localState.second %= 60;
  }
  if (localState.minute >= 60) {
    localState.hour += Math.floor(localState.minute / 60);
    localState.minute %= 60;
  }
  if (localState.hour >= 24) {
    localState.hour = 6;
    localState.minute = 30;
    initLocalSim();
    localState.windows.forEach(w => { w.queueCount = 0; w.serveAccum = 0; });
  }

  const period = getMealPeriodLocal(localState.hour);
  const MAX_QUEUE = 15;

  // 步骤1：新人到达（仅就餐时段）——先占座，再排队
  const arrivals = calcArrivalsLocal(period, speed);
  for (let i = 0; i < arrivals; i++) {
    const emptySeats = localState.seats.filter(s => s.status === 'empty');
    if (emptySeats.length === 0) continue;

    const seat = emptySeats[Math.floor(Math.random() * emptySeats.length)];
    const seatIdx = localState.seats.indexOf(seat);
    seat.status = 'reserved';
    localState.seatTimers[seatIdx] = 45 * 60; // 最多等45分钟

    // 只在未满的开放窗口中选择
    const openWins = getOpenWindowsLocal(period);
    const notFull = openWins.filter(w => w.queueCount < MAX_QUEUE);
    if (notFull.length === 0) {
      seat.status = 'empty';
      seat.windowId = -1;
      localState.seatTimers[seatIdx] = 0;
      continue;
    }
    // 加权随机（排队少的权重高），从notFull中选
    const weights = notFull.map(w => 1 / (w.queueCount + 1));
    const total = weights.reduce((a, b) => a + b, 0);
    let r = Math.random() * total;
    let chosenWin = notFull[notFull.length - 1];
    for (let j = 0; j < notFull.length; j++) {
      r -= weights[j];
      if (r <= 0) { chosenWin = notFull[j]; break; }
    }
    chosenWin.queueCount++;
    seat.windowId = chosenWin.id; // 记录该座位绑定的窗口
  }

  // 步骤2：服务完成——用累积器解决avgSec>speed时服务率为0的问题
  localState.windows.forEach(win => {
    if (win.queueCount <= 0) return;
    win.serveAccum += speed;
    const servedPeople = Math.floor(win.serveAccum / win.avgSec);
    if (servedPeople === 0) return;
    win.serveAccum -= servedPeople * win.avgSec;
    const actualServed = Math.min(servedPeople, win.queueCount);
    win.queueCount = Math.max(0, win.queueCount - actualServed);

    // 打完饭：优先找属于这个窗口的reserved座位，没有则找任意reserved，再没有找空座
    for (let i = 0; i < actualServed; i++) {
      let pool = localState.seats.filter(s => s.status === 'reserved' && s.windowId === win.id);
      if (pool.length === 0) pool = localState.seats.filter(s => s.status === 'reserved');
      if (pool.length === 0) pool = localState.seats.filter(s => s.status === 'empty');
      if (pool.length === 0) break; // 实在没座就算了
      const s = pool[Math.floor(Math.random() * pool.length)];
      const idx = localState.seats.indexOf(s);
      s.status = 'dining';
      s.windowId = -1;
      localState.seatTimers[idx] = (period === 'breakfast'
        ? (10 + Math.floor(Math.random() * 6))
        : (20 + Math.floor(Math.random() * 11))) * 60;
    }
  });

  // 步骤3：更新座位倒计时（dining和reserved都要倒计时）
  for (let i = 0; i < localState.seats.length; i++) {
    const s = localState.seats[i];
    if (s.status === 'dining') {
      localState.seatTimers[i] -= speed;
      if (localState.seatTimers[i] <= 0) {
        s.status = 'empty';
        s.windowId = -1;
        localState.seatTimers[i] = 0;
      }
    } else if (s.status === 'reserved') {
      localState.seatTimers[i] -= speed;
      if (localState.seatTimers[i] <= 0) {
        // 超时放弃：释放座位并同步减少对应窗口的队列数
        const w = localState.windows.find(w => w.id === s.windowId);
        if (w && w.queueCount > 0) w.queueCount--;
        s.status = 'empty';
        s.windowId = -1;
        localState.seatTimers[i] = 0;
      }
    }
  }

  // 步骤4：非就餐时段——就餐人员额外加速离场（2倍速），队列继续自然消化直至清空
  if (period === 'off') {
    for (let i = 0; i < localState.seats.length; i++) {
      const s = localState.seats[i];
      if (s.status === 'dining') {
        localState.seatTimers[i] -= speed; // 额外再扣一倍，加速清场
        if (localState.seatTimers[i] <= 0) {
          s.status = 'empty';
          s.windowId = -1;
          localState.seatTimers[i] = 0;
        }
      }
    }
    // reserved座位继续等待服务（步骤2会继续消化队列），不强制清零
  }
}

function getMealPeriodLocal(h) {
  if (h >= 7 && h < 9)  return 'breakfast';
  if (h >= 11 && h < 13) return 'lunch';
  if (h >= 17 && h < 19) return 'dinner';
  return 'off';
}

function getArrivalMultiplier(hour, minute, period) {
  let t; // minutes since period start
  if (period === 'breakfast') t = (hour - 7)  * 60 + minute;
  else if (period === 'lunch')    t = (hour - 11) * 60 + minute;
  else if (period === 'dinner')   t = (hour - 17) * 60 + minute;
  else return 0;
  // 梯形：前30分钟爬坡，中间60分钟高峰，后30分钟下坡
  if (t < 0 || t >= 120) return 0;
  if (t < 30) return t / 30;
  if (t < 90) return 1.0;
  return (120 - t) / 30;
}

function calcArrivalsLocal(period, tickSec) {
  if (period === 'off') return 0;
  let rate;
  switch (period) {
    case 'breakfast': rate = 3.0;  break; // 高峰3人/分钟，早餐轻度
    case 'lunch':     rate = 10.0; break; // 高峰10人/分钟，午餐主峰
    case 'dinner':    rate = 7.0;  break; // 高峰7人/分钟，晚餐次峰
    default: return 0;
  }
  const mult = getArrivalMultiplier(localState.hour, localState.minute, period);
  const lambda = rate * mult * tickSec / 60;
  return poissonLocal(lambda);
}

function poissonLocal(lambda) {
  if (lambda <= 0) return 0;
  let k = 0, p = 1, L = Math.exp(-lambda);
  do { k++; p *= Math.random(); } while (p > L);
  return k - 1;
}

// function chooseWindowLocal() {
//   const weights = localState.windows.map(w => 1 / (w.queueCount + 1));
//   const total = weights.reduce((a, b) => a + b, 0);
//   let r = Math.random() * total;
//   for (let i = 0; i < localState.windows.length; i++) {
//     r -= weights[i];
//     if (r <= 0) return localState.windows[i];
//   }
//   return localState.windows[0];
// }

function getOpenWindowsLocal(period) {
  // 早餐只开: 面条(id=2)、包子馒头(id=4)、清真(id=7)
  // 午餐/晚餐: 全部开放
  if (period === 'breakfast') {
    return localState.windows.filter(w => [2, 4, 7].includes(w.id));
  }
  return localState.windows;
}

function chooseWindowLocal(period) {
  const openWins = getOpenWindowsLocal(period);
  const weights = openWins.map(w => 1 / (w.queueCount + 1));
  const total = weights.reduce((a, b) => a + b, 0);
  let r = Math.random() * total;
  for (let i = 0; i < openWins.length; i++) {
    r -= weights[i];
    if (r <= 0) return openWins[i];
  }
  return openWins[0];
}

function assignSeat(period) {
  // const empty = localState.seats.filter(s => s.status === 'empty');
  // if (empty.length === 0) return;
  // const s = empty[Math.floor(Math.random() * empty.length)];
  // const idx = localState.seats.indexOf(s);
  // s.status = 'dining';
  // localState.seatTimers[idx] = (15 + Math.floor(Math.random() * 16)) * 60;
  const empty = localState.seats.filter(s => s.status === 'empty');
  if (empty.length === 0) return;
  const s = empty[Math.floor(Math.random() * empty.length)];
  const idx = localState.seats.indexOf(s);
  s.status = 'dining';
  if (period === 'breakfast') {
    localState.seatTimers[idx] = (10 + Math.floor(Math.random() * 6)) * 60;  // 10~15分钟
  } else {
    localState.seatTimers[idx] = (20 + Math.floor(Math.random() * 11)) * 60; // 20~30分钟
  }
}

function buildSnapshotFromLocal() {
  const period = getMealPeriodLocal(localState.hour);
  const hh = String(localState.hour).padStart(2, '0');
  const mm = String(localState.minute).padStart(2, '0');
  const empty = localState.seats.filter(s => s.status === 'empty').length;
  const dining = localState.seats.filter(s => s.status === 'dining').length;
  const inQueue = localState.windows.reduce((a, w) => a + w.queueCount, 0);

  return {
    simTime: `${hh}:${mm}`,
    simDate: '仿真运行',
    mealPeriod: period,
    dayType: localState.dayType,
    totalPeople: Math.round(inQueue + dining),
    emptySeatCount: empty,
    totalSeatCount: localState.totalSeats,
    speed: localState.speed,
    running: localState.running,
    windows: localState.windows.map(w => {
      const q = w.queueCount;
      return {
        id: w.id,
        windowName: w.windowName,
        category: w.category,
        queueCount: q,
        estimatedWaitMin: Math.round(q * w.avgSec / 60),
        queueLevel: q <= 3 ? 'low' : q <= 8 ? 'medium' : 'high'
      };
    }),
    seats: localState.seats.map(s => ({ ...s, floorId: 1 }))
  };
}

// ====================================================
// 轮询控制
// ====================================================

let localTickTimer = null;

function startPolling() {
  initLocalSim();
  tryConnect();
}

async function tryConnect() {
  const result = await apiGetSnapshot();
  if (result && result.code === 200) {
    // 后端可用
    useLocalSim = false;
    showToast('✅ 已连接后端仿真服务');
    startServerPolling();
  } else {
    // 后端不可用，使用本地仿真
    useLocalSim = true;
    showToast('⚠️ 后端未连接，使用前端本地仿真模式');
    startLocalSim();
  }
}

function startServerPolling() {
  pollingTimer = setInterval(async () => {
    const res = await apiGetSnapshot();
    if (res && res.code === 200) {
      renderSnapshot(res.data);
    } else {
      // 切换到本地仿真
      clearInterval(pollingTimer);
      useLocalSim = true;
      startLocalSim();
    }
  }, 1000);
}

function startLocalSim() {
  localState.running = true;
  localTickTimer = setInterval(() => {
    localSimTick();
    const snap = buildSnapshotFromLocal();
    renderSnapshot(snap);
  }, 1000);
}

// 控制API（先尝试后端，失败则控制本地）
async function simControl(action) {
  if (!useLocalSim) {
    const fnMap = { start: apiSimStart, pause: apiSimPause, reset: apiSimReset };
    const res = await fnMap[action]();
    if (res && res.code === 200) { showToast(res.data); return; }
  }
  // 本地控制
  if (action === 'start') { localState.running = true; showToast('仿真已启动'); }
  if (action === 'pause') { localState.running = false; showToast('仿真已暂停'); }
  if (action === 'reset') {
    localState.hour = 6; localState.minute = 30; localState.second = 0;
    initLocalSim(); localState.windows.forEach(w => { w.queueCount = 0; w.serveAccum = 0; });
    showToast('仿真已重置');
  }
}

async function setSpeed(val) {
  const s = parseInt(val);
  if (!useLocalSim) {
    await apiSetSpeed(s);
  }
  localState.speed = s;
}
