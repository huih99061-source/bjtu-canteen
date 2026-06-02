// ====================================================
// ui.js - UI渲染、交互逻辑
// ====================================================

let currentStar = 0;
const queueHistory = {};    // { windowId: number[] } 历史队列长度
const HISTORY_LEN = 60;
const prevQueueCounts = {};
let currentSeatView = 'dot';
let _lastSeats = [];

const SPEED_VALUES = [30, 60, 120, 300, 600, 1800, 3600];
const SPEED_LABELS = ['0.5x', '1x', '2x', '5x', '10x', '30x', '60x'];
const WIN_COLORS   = ['#1f6feb','#3fb950','#d29922','#f85149','#db6d28','#8957e5','#58a6ff'];
const WIN_NAMES    = ['米饭','面条','炒菜','包子','麻辣烫','饮品','清真'];
const _countTimers = {};
const PERIOD_MAP = {
  breakfast: { text: '早餐时段', cls: 'breakfast' },
  lunch: { text: '午餐高峰', cls: 'lunch' },
  dinner: { text: '晚餐时段', cls: 'dinner' },
  off: { text: '非就餐', cls: 'off' }
};

// ====================================================
// A: 队列历史记录 & Sparkline 折线图
// ====================================================
function recordQueueHistory(windows) {
  (windows || []).forEach(w => {
    if (!queueHistory[w.id]) queueHistory[w.id] = [];
    queueHistory[w.id].push(w.queueCount || 0);
    if (queueHistory[w.id].length > HISTORY_LEN) queueHistory[w.id].shift();
  });
}

function drawSparkline(canvas, data, color) {
  if (!canvas || data.length < 2) return;
  const ctx = canvas.getContext('2d');
  const w = canvas.width, h = canvas.height;
  ctx.clearRect(0, 0, w, h);
  const max = Math.max(...data, 1);
  const pts = data.map((v, i) => ({
    x: (i / (data.length - 1)) * w,
    y: h - (v / max) * (h - 4) - 2
  }));
  // 渐变填充
  const grad = ctx.createLinearGradient(0, 0, 0, h);
  grad.addColorStop(0, color + '55');
  grad.addColorStop(1, color + '00');
  ctx.beginPath();
  ctx.moveTo(pts[0].x, h);
  pts.forEach(p => ctx.lineTo(p.x, p.y));
  ctx.lineTo(pts[pts.length - 1].x, h);
  ctx.closePath();
  ctx.fillStyle = grad;
  ctx.fill();
  // 折线（贝塞尔平滑）
  ctx.beginPath();
  ctx.moveTo(pts[0].x, pts[0].y);
  for (let i = 1; i < pts.length; i++) {
    const mx = (pts[i - 1].x + pts[i].x) / 2;
    ctx.bezierCurveTo(mx, pts[i - 1].y, mx, pts[i].y, pts[i].x, pts[i].y);
  }
  ctx.strokeStyle = color;
  ctx.lineWidth = 1.5;
  ctx.lineJoin = 'round';
  ctx.stroke();
}

// ====================================================
// 初始化UI骨架
// ====================================================
function initUI() {
  renderTableSkeleton();
  renderWindowCards([]);
  renderMenuBtns();
}

// 渲染桌椅骨架（7桌×4座）
function renderTableSkeleton() {
  const container = document.getElementById('tableContainer');
  container.innerHTML = '';
  for (let t = 1; t <= 80; t++) {
    const group = document.createElement('div');
    group.className = 'table-group';
    const label = document.createElement('div');
    label.className = 'table-label';
    label.textContent = `${t}号桌`;
    group.appendChild(label);
    const grid = document.createElement('div');
    grid.className = 'seat-grid';
    for (let s = 1; s <= 4; s++) {
      const seatId = (t - 1) * 4 + s;
      const btn = document.createElement('button');
      btn.className = 'seat-btn empty';
      btn.id = `seat-${seatId}`;
      btn.title = `${t}号桌 ${s}号座`;
      btn.onclick = () => handleSeatClick(seatId);
      grid.appendChild(btn);
    }
    group.appendChild(grid);
    container.appendChild(group);
  }
}

// ====================================================
// 核心渲染：根据快照更新全部UI
// ====================================================
function renderSnapshot(snap) {
  if (!snap) return;
  recordQueueHistory(snap.windows);

  // 时钟
  document.getElementById('simTime').textContent = snap.simTime || '--:--';
  document.getElementById('simDate').textContent = snap.simDate || '';

  // 时段徽章
  const pInfo = PERIOD_MAP[snap.mealPeriod] || PERIOD_MAP.off;
  const badge = document.getElementById('mealPeriodBadge');
  badge.textContent = pInfo.text;
  badge.className = `period-badge ${pInfo.cls}`;

  // 顶栏时段渐变
  document.querySelector('.topbar').className = 'topbar ' + (snap.mealPeriod || 'off');

  // 统计（数字滚动）
  animateCount('statPeople', snap.totalPeople || 0);
  animateCount('statEmpty', snap.emptySeatCount || 0);
  const occupy = snap.totalSeatCount > 0
    ? Math.round((snap.totalSeatCount - snap.emptySeatCount) / snap.totalSeatCount * 100)
    : 0;
  renderOccupancyRing(occupy);

  // 时间轴游标
  updateTimeline(snap.simTime);

  // 座位数据缓存（热力图用）
  _lastSeats = snap.seats || [];
  renderHeatmap();

  // 折线图（面板打开时刷新）
  if (document.getElementById('chartPanel').classList.contains('open')) renderQueueChart();

  // 窗口列表（左侧）
  renderWindowList(snap.windows || []);

  // 标记各窗口是否营业
  const breakfastOnly = [2, 4, 7];
  (snap.windows || []).forEach(w => {
    if (snap.mealPeriod === 'breakfast') {
      w.isOpenNow = breakfastOnly.includes(w.id);
    } else if (snap.mealPeriod === 'off') {
      w.isOpenNow = false;
    } else {
      w.isOpenNow = true;
    }
  });

  // 窗口卡片（平面图）
  renderWindowCards(snap.windows || []);

  // 座位
  renderSeats(snap.seats || []);
}

// 左侧窗口排队列表
function renderWindowList(windows) {
  const el = document.getElementById('windowList');
  el.innerHTML = '';
  const maxQ = Math.max(...windows.map(w => w.queueCount || 0), 15);
  windows.forEach(w => {
    const q = w.queueCount || 0;
    const pct = Math.min(100, Math.round(q / maxQ * 100));
    const div = document.createElement('div');
    div.className = 'window-item';
    div.onclick = () => openWindowModal(w.id);
    div.innerHTML = `
      <div class="window-item-top">
        <div>
          <div class="win-name">${w.windowName}</div>
          <div class="win-cat">${w.category}</div>
        </div>
        <span style="color:var(--queue-${w.queueLevel || 'low'});font-weight:700;font-size:15px">${q}</span>
      </div>
      <div class="queue-bar-wrap">
        <div class="queue-bar ${w.queueLevel || 'low'}" style="width:${pct}%"></div>
      </div>
      <div class="queue-info">
        <span>排队 ${q} 人</span>
        <span>约 ${w.estimatedWaitMin || 0} 分钟</span>
      </div>
    `;
    el.appendChild(div);
    // Sparkline：历史折线图
    const canvas = document.createElement('canvas');
    canvas.className = 'sparkline-canvas';
    canvas.height = 28;
    div.appendChild(canvas);
    canvas.width = div.clientWidth || 192;
    const spColor = w.queueLevel === 'high' ? '#f85149' : w.queueLevel === 'medium' ? '#d29922' : '#3fb950';
    drawSparkline(canvas, queueHistory[w.id] || [], spColor);
  });
}

// 平面图窗口卡片
function renderWindowCards(windows) {
  const el = document.getElementById('windowCards');
  if (windows.length === 0) {
    el.innerHTML = '<div style="color:var(--text-sub);text-align:center;grid-column:1/-1;padding:20px">加载中…</div>';
    return;
  }
  el.innerHTML = '';
  windows.forEach(w => {
    const q = w.queueCount || 0;
    const level = w.queueLevel || 'low';
    // B: 趋势指示
    const prevQ = prevQueueCounts[w.id] !== undefined ? prevQueueCounts[w.id] : q;
    const delta = q - prevQ;
    prevQueueCounts[w.id] = q;
    let trendHtml = '';
    if (delta > 0) trendHtml = `<span class="win-trend up">↑${delta}</span>`;
    else if (delta < 0) trendHtml = `<span class="win-trend down">↓${Math.abs(delta)}</span>`;

    const card = document.createElement('div');
    card.className = `win-card ${level}`;
    card.onclick = () => openWindowModal(w.id);
    card.title = `点击查看${w.windowName}菜单`;

    // B: 圆点竖排，人多双列
    const DOTS_MAX = 12;
    const dots = Math.min(q, DOTS_MAX);
    let dotsHtml = '';
    for (let i = 0; i < dots; i++) {
      dotsHtml += `<span class="queue-dot" style="color:var(--queue-${level});background:var(--queue-${level})"></span>`;
    }
    const overflowHtml = q > DOTS_MAX
      ? `<span style="font-size:9px;color:var(--text-sub)">+${q - DOTS_MAX}</span>` : '';
    const flowPct = Math.min(100, Math.round(q / 15 * 100));

    // card.innerHTML = `
    //   <div class="win-card-name">${w.windowName}</div>
    //   <div class="win-queue-dots">${dotsHtml || '<span style="color:var(--text-sub);font-size:10px">无人排队</span>'}</div>
    //   <div class="win-count-label" style="color:var(--queue-${level})">${q} 人排队</div>
    //   <div class="win-wait-label">约 ${w.estimatedWaitMin || 0} 分钟</div>
    // `;
    const closedMask = w.isOpenNow === false
      ? `<div style="position:absolute;inset:0;background:rgba(0,0,0,0.55);border-radius:6px;display:flex;align-items:center;justify-content:center;font-size:11px;color:#8b949e;font-weight:700;">暂未营业</div>`
      : '';

    card.innerHTML = `
  ${trendHtml}
  <div class="win-card-name">${w.windowName}</div>
  <div class="queue-lane${q > 5 ? ' two-col' : ''}">
    ${q > 0 ? dotsHtml + overflowHtml : '<span class="queue-empty-label">暂无排队</span>'}
  </div>
  <div class="queue-flow-bar ${level}" style="width:${flowPct}%"></div>
  <div class="win-count-label" style="color:var(--queue-${level})">${q} 人 · 约${w.estimatedWaitMin || 0}分钟</div>
  ${closedMask}
`;
    el.appendChild(card);
  });
}

// 座位渲染
function renderSeats(seats) {
  seats.forEach(s => {
    const btn = document.getElementById(`seat-${s.id}`);
    if (!btn) return;
    btn.className = `seat-btn ${s.status}`;
    const statusText = { empty: '空座', reserved: '已占座', dining: '就餐中' };
    btn.title = `${s.tableId}号桌 ${s.seatIndex}号座 [${statusText[s.status] || s.status}]`;
  });
}

// 菜单快速入口按钮
function renderMenuBtns() {
  const wins = [
    { id: 1, name: '米饭窗口' }, { id: 2, name: '面条窗口' }, { id: 3, name: '炒菜窗口' },
    { id: 4, name: '包子馒头' }, { id: 5, name: '麻辣烫' }, { id: 6, name: '饮品甜点' }, { id: 7, name: '清真窗口' }
  ];
  const el = document.getElementById('menuBtns');
  wins.forEach(w => {
    const btn = document.createElement('button');
    btn.className = 'menu-btn';
    btn.textContent = `🍽️ ${w.name}`;
    btn.onclick = () => openWindowModal(w.id);
    el.appendChild(btn);
  });
}

// ====================================================
// 窗口详情弹窗
// ====================================================
async function openWindowModal(winId) {
  const modal = document.getElementById('windowModal');
  const title = document.getElementById('modalTitle');
  const content = document.getElementById('modalContent');

  title.textContent = '加载中…';
  content.innerHTML = '<div style="text-align:center;padding:40px;color:var(--text-sub)">加载中…</div>';
  modal.classList.add('open');

  const res = await apiGetWindow(winId);
  if (!res || res.code !== 200) {
    content.innerHTML = '<div style="color:var(--red);text-align:center;padding:20px">加载失败，请检查后端连接</div>';
    return;
  }
  const win = res.data;
  title.textContent = `${win.windowName} — ${win.category}`;

  const foods = win.foods || [];
  if (foods.length === 0) {
    content.innerHTML = '<div style="color:var(--text-sub);text-align:center;padding:20px">暂无菜品数据</div>';
    return;
  }
  const grid = document.createElement('div');
  grid.className = 'food-grid';
  foods.forEach(f => {
    const item = document.createElement('div');
    item.className = 'food-item';
    item.innerHTML = `
      <div class="food-name">${f.foodName}</div>
      <div class="food-price">¥${parseFloat(f.foodPrice).toFixed(2)}</div>
    `;
    grid.appendChild(item);
  });
  content.innerHTML = '';
  content.appendChild(grid);
}

function closeModal(e) {
  if (!e || e.target === document.getElementById('windowModal')) {
    document.getElementById('windowModal').classList.remove('open');
  }
}

// ====================================================
// 座位点击
// ====================================================
async function handleSeatClick(seatId) {
  const btn = document.getElementById(`seat-${seatId}`);
  if (!btn || !btn.classList.contains('empty')) return;

  if (!useLocalSim) {
    const res = await apiReserveSeat(seatId);
    if (res && res.code === 200) {
      btn.className = 'seat-btn reserved';
      showToast(`✅ ${res.data}`);
    } else {
      showToast('❌ 占座失败');
    }
  } else {
    // 本地仿真模式
    const idx = seatId - 1;
    if (localState.seats[idx] && localState.seats[idx].status === 'empty') {
      localState.seats[idx].status = 'reserved';
      localState.seatTimers[idx] = 30 * 60;
      btn.className = 'seat-btn reserved';
      showToast(`✅ 占座成功，${Math.ceil(seatId / 4)}号桌 ${((seatId - 1) % 4) + 1}号座`);
    }
  }
}

// ====================================================
// 评价
// ====================================================
function setStar(val) {
  currentStar = val;
  document.querySelectorAll('.star').forEach(s => {
    s.classList.toggle('active', parseInt(s.dataset.v) <= val);
  });
}

async function submitFeedback() {
  const winId = parseInt(document.getElementById('fbWindow').value);
  const comment = document.getElementById('fbComment').value.trim();
  if (!winId) { showToast('请选择窗口'); return; }
  if (!currentStar) { showToast('请选择评分'); return; }

  const res = await apiSubmitFeedback({ windowId: winId, rating: currentStar, commentText: comment });
  if (res && res.code === 200) {
    showToast('✅ 评价提交成功！感谢反馈');
    document.getElementById('fbWindow').value = '';
    document.getElementById('fbComment').value = '';
    setStar(0);
    loadFeedback();
  } else {
    const reason = (res && res.msg) ? res.msg : '请检查后端连接';
    showToast('❌ 提交失败：' + reason);
  }
}

async function loadFeedback() {
  const res = await apiGetFeedback();
  if (!res || res.code !== 200) return;
  const list = document.getElementById('feedbackList');
  const items = res.data || [];
  if (items.length === 0) {
    list.innerHTML = '<div style="color:var(--text-sub);font-size:12px;text-align:center;padding:10px">暂无评价</div>';
    return;
  }
  list.innerHTML = items.slice(0, 10).map(f => `
    <div class="fb-item">
      <div class="fb-item-top">
        <span class="fb-win">${f.windowName || '未知窗口'}</span>
        <span class="fb-stars">${'★'.repeat(f.rating || 0)}${'☆'.repeat(5 - (f.rating || 0))}</span>
      </div>
      <div class="fb-comment">${f.commentText || '（无文字评价）'}</div>
      <div class="fb-time">${f.createdAt || ''}</div>
    </div>
  `).join('');
}

// ====================================================
// 速度滑块
// ====================================================
function setSpeedBySlider(idx) {
  const val = SPEED_VALUES[parseInt(idx)];
  document.getElementById('speedLabel').textContent = SPEED_LABELS[parseInt(idx)];
  setSpeed(val);
}

// ====================================================
// 数字滚动动画
// ====================================================
function animateCount(elId, target) {
  const el = document.getElementById(elId);
  if (!el) return;
  const start = parseInt(el.textContent) || 0;
  if (start === target) return;
  if (_countTimers[elId]) clearInterval(_countTimers[elId]);
  let step = 0;
  _countTimers[elId] = setInterval(() => {
    step++;
    el.textContent = Math.round(start + (target - start) * step / 10);
    if (step >= 10) { el.textContent = target; clearInterval(_countTimers[elId]); }
  }, 40);
}

// ====================================================
// 占座率环形图
// ====================================================
function renderOccupancyRing(pct) {
  const el = document.getElementById('ringChart');
  if (!el) return;
  const r = 26, cx = 32, cy = 32, circ = 2 * Math.PI * r;
  const dash = (circ * Math.min(pct, 100) / 100).toFixed(1);
  const hex = pct < 60 ? '#3fb950' : pct < 85 ? '#d29922' : '#f85149';
  el.innerHTML = `<svg width="64" height="64" viewBox="0 0 64 64">
    <circle cx="${cx}" cy="${cy}" r="${r}" fill="none" stroke="#21262d" stroke-width="6"/>
    <circle cx="${cx}" cy="${cy}" r="${r}" fill="none" stroke="${hex}" stroke-width="6"
      stroke-dasharray="${dash} ${circ.toFixed(1)}" stroke-dashoffset="${(circ*.25).toFixed(1)}"
      stroke-linecap="round" style="transition:stroke-dasharray .6s ease,stroke .4s"/>
    <text x="${cx}" y="${cy}" text-anchor="middle" dominant-baseline="middle"
      fill="${hex}" font-size="11" font-weight="800" font-family="inherit">${pct}%</text>
  </svg>`;
}

// ====================================================
// 时段时间轴游标
// ====================================================
function updateTimeline(simTime) {
  const cursor = document.getElementById('timelineCursor');
  if (!cursor || !simTime || simTime === '--:--') return;
  const [h, m] = simTime.split(':').map(Number);
  const pct = Math.max(0, Math.min(100, ((h - 6) * 60 + m) / (18 * 60) * 100));
  cursor.style.left = pct.toFixed(2) + '%';
}

// ====================================================
// 折线图面板
// ====================================================
function toggleChartPanel() {
  const panel = document.getElementById('chartPanel');
  panel.classList.toggle('open');
  if (panel.classList.contains('open')) renderQueueChart();
}

function renderQueueChart() {
  const canvas = document.getElementById('queueChart');
  if (!canvas) return;
  canvas.width  = canvas.parentElement.clientWidth - 40;
  canvas.height = 130;
  const ctx = canvas.getContext('2d');
  const w = canvas.width, h = canvas.height;
  ctx.clearRect(0, 0, w, h);

  const allVals = Object.values(queueHistory).flatMap(a => a);
  const maxQ = Math.max(...allVals, 5);
  const padB = 18, padT = 6;
  const chartH = h - padB - padT;

  // 网格
  ctx.strokeStyle = '#21262d'; ctx.lineWidth = 1;
  for (let i = 0; i <= 4; i++) {
    const y = padT + chartH - (i / 4) * chartH;
    ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(w, y); ctx.stroke();
    ctx.fillStyle = '#8b949e'; ctx.font = '9px sans-serif';
    ctx.fillText(Math.round(maxQ * i / 4), 2, y - 2);
  }

  // 各窗口曲线
  Object.entries(queueHistory).forEach(([id, data]) => {
    if (data.length < 2) return;
    const color = WIN_COLORS[(parseInt(id) - 1) % WIN_COLORS.length];
    ctx.beginPath();
    data.forEach((v, i) => {
      const x = (i / (HISTORY_LEN - 1)) * w;
      const y = padT + chartH - (v / maxQ) * chartH;
      i === 0 ? ctx.moveTo(x, y) : ctx.lineTo(x, y);
    });
    ctx.strokeStyle = color; ctx.lineWidth = 1.5; ctx.lineJoin = 'round'; ctx.stroke();
  });

  // 图例
  WIN_NAMES.forEach((name, i) => {
    const x = (i / WIN_NAMES.length) * w + 4;
    ctx.fillStyle = WIN_COLORS[i];
    ctx.fillRect(x, h - 12, 10, 6);
    ctx.fillStyle = '#8b949e'; ctx.font = '9px sans-serif';
    ctx.fillText(name, x + 12, h - 6);
  });
}

// ====================================================
// 座位热力图
// ====================================================
function setSeatView(view, btn) {
  currentSeatView = view;
  document.querySelectorAll('.view-btn').forEach(b => b.classList.remove('active'));
  if (btn) btn.classList.add('active');
  const tc = document.getElementById('tableContainer');
  const hc = document.getElementById('heatmapCanvas');
  if (view === 'dot') {
    tc.style.display = '';
    hc.classList.remove('show');
    if (_animRaf) { cancelAnimationFrame(_animRaf); _animRaf = null; }
  } else {
    tc.style.display = 'none';
    hc.classList.add('show');
    renderHeatmap();
  }
}

// ====================================================
// 座位动画图（人员沿通道流动）
// ====================================================
let _animRaf = null;
const _particles = [];
const _prevStates = {};
const _seatPosMap = {};
const _seatStatusMap = {};
let _animCellW = 0, _animCellH = 0, _animCOLS = 8, _animCH = 0;

// 缓入缓出：走廊行走感
function _easeInOut(t) { return t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2; }

function _rr(ctx, x, y, w, h, r) {
  ctx.beginPath();
  ctx.moveTo(x + r, y);
  ctx.arcTo(x + w, y,     x + w, y + h, r);
  ctx.arcTo(x + w, y + h, x,     y + h, r);
  ctx.arcTo(x,     y + h, x,     y,     r);
  ctx.arcTo(x,     y,     x + w, y,     r);
  ctx.closePath();
}

function _buildSeatPos(cellW, cellH, COLS) {
  for (let t = 0; t < 80; t++) {
    const col = t % COLS, row = Math.floor(t / COLS);
    const tx0 = col * cellW + 5, ty0 = row * cellH + 5;
    const bw = cellW - 10, bh = cellH - 10;
    const offX = [bw * 0.20, bw * 0.58];
    const offY = [bh * 0.28, bh * 0.60];
    for (let s = 0; s < 4; s++) {
      _seatPosMap[t * 4 + s + 1] = {
        x: tx0 + offX[s % 2],
        y: ty0 + offY[Math.floor(s / 2)],
      };
    }
  }
}

// 生成走廊路径粒子：edge → 竖向通道 → 横向通道入口 → 座位（3段折线）
function _makeParticle(seatId, prevStatus, curStatus) {
  const pos = _seatPosMap[seatId];
  if (!pos) return null;

  const tableN = Math.floor((seatId - 1) / 4); // 0-indexed
  const col = tableN % _animCOLS;
  const row = Math.floor(tableN / _animCOLS);
  const cellW = _animCellW, cellH = _animCellH;
  const ROWS = Math.ceil(80 / _animCOLS);

  // 竖向通道 x：每列左侧间隔中心（col * cellW + 2.5）
  const aisleX = col * cellW + 2.5;
  // 横向通道 y：本行顶部间隔中心（row * cellH + 2.5）
  const aisleY = row * cellH + 2.5;
  // 入场/出场边缘：上半区从顶部进，下半区从底部进
  const fromTop = row < ROWS / 2;
  const edgeY = fromTop ? -10 : _animCH + 10;

  const dotR = 5;
  const seatX = pos.x + dotR, seatY = pos.y + dotR;
  const isArrival = prevStatus === 'empty';
  const color = isArrival
    ? (curStatus === 'dining' ? '#3fb950' : '#d29922')
    : (prevStatus === 'dining' ? '#3fb950' : '#d29922');

  // 路径：3个关键点，2段
  // 到达：边缘 → (aisleX, aisleY) → 座位
  // 离开：座位 → (aisleX, aisleY) → 边缘
  const waypoints = isArrival
    ? [{x: aisleX, y: edgeY}, {x: aisleX, y: aisleY}, {x: seatX, y: seatY}]
    : [{x: seatX,  y: seatY}, {x: aisleX, y: aisleY}, {x: aisleX, y: edgeY}];

  const SPEED = 0.42; // px/ms ≈ 420 px/s
  const delay = Math.random() * 350;
  const now = performance.now();
  const d0 = Math.max(100, Math.hypot(
    waypoints[1].x - waypoints[0].x,
    waypoints[1].y - waypoints[0].y
  ) / SPEED);

  return { waypoints, segIdx: 0, segStart: now + delay, segDur: d0,
           totalSegs: 2, color, done: false };
}

function _animLoop() {
  const canvas = document.getElementById('heatmapCanvas');
  if (!canvas || currentSeatView !== 'heat') { _animRaf = null; return; }

  const ctx = canvas.getContext('2d');
  const cw = canvas.width, ch = canvas.height;
  const COLS = _animCOLS, cellW = _animCellW, cellH = _animCellH;
  const ROWS = Math.ceil(80 / COLS);
  const now = performance.now();

  ctx.fillStyle = '#080c14';
  ctx.fillRect(0, 0, cw, ch);

  // 通道地板（稍亮于背景 #080c14，制造走廊感）
  ctx.fillStyle = '#0d1520';
  for (let c = 0; c <= COLS; c++) ctx.fillRect(c * cellW - 5, 0, 10, ch);
  for (let r = 0; r <= ROWS; r++) ctx.fillRect(0, r * cellH - 5, cw, 10);

  // 通道中心虚线引导线
  ctx.save();
  ctx.setLineDash([3, 6]);
  ctx.strokeStyle = '#ffffff0e';
  ctx.lineWidth = 1;
  for (let c = 0; c <= COLS; c++) {
    ctx.beginPath(); ctx.moveTo(c * cellW, 0); ctx.lineTo(c * cellW, ch); ctx.stroke();
  }
  for (let r = 0; r <= ROWS; r++) {
    ctx.beginPath(); ctx.moveTo(0, r * cellH); ctx.lineTo(cw, r * cellH); ctx.stroke();
  }
  ctx.setLineDash([]);
  ctx.restore();

  // 绘制桌子和静态座位圆点
  for (let t = 0; t < 80; t++) {
    const col = t % COLS, row = Math.floor(t / COLS);
    const tx = col * cellW + 5, ty = row * cellH + 5;
    const bw = cellW - 10, bh = cellH - 10;

    let diningN = 0, reservedN = 0;
    for (let s = 0; s < 4; s++) {
      const st = _seatStatusMap[t * 4 + s + 1];
      if (st === 'dining') diningN++;
      else if (st === 'reserved') reservedN++;
    }
    const occ = (diningN + reservedN) / 4;

    if (occ > 0.5) {
      ctx.shadowColor = occ > 0.75 ? '#f8514955' : '#d2992233';
      ctx.shadowBlur = 8;
    }
    ctx.fillStyle = occ === 0 ? '#161b22'
      : occ <= 0.25 ? '#192030'
      : occ <= 0.5  ? '#1e2535'
      : occ <= 0.75 ? '#261f22'
      : '#2d1a1a';
    _rr(ctx, tx, ty, bw, bh, 6);
    ctx.fill();
    ctx.shadowBlur = 0;

    ctx.strokeStyle = occ > 0.75 ? '#f8514940' : occ > 0.5 ? '#d2992230' : '#30363d';
    ctx.lineWidth = 1;
    _rr(ctx, tx, ty, bw, bh, 6);
    ctx.stroke();

    ctx.fillStyle = '#3a4049';
    ctx.font = '8px sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'top';
    ctx.fillText(String(t + 1), tx + bw / 2, ty + 2);

    for (let s = 0; s < 4; s++) {
      const pos = _seatPosMap[t * 4 + s + 1];
      if (!pos) continue;
      const st = _seatStatusMap[t * 4 + s + 1] || 'empty';
      const dotR = 5, cx = pos.x + dotR, cy = pos.y + dotR;
      ctx.beginPath();
      ctx.arc(cx, cy, dotR, 0, Math.PI * 2);
      if (st === 'empty') {
        ctx.fillStyle = '#21262d'; ctx.fill();
        ctx.strokeStyle = '#30363d'; ctx.lineWidth = 0.8; ctx.stroke();
      } else {
        const dc = st === 'dining' ? '#3fb950' : '#d29922';
        ctx.shadowColor = dc; ctx.shadowBlur = st === 'dining' ? 9 : 5;
        ctx.fillStyle = dc; ctx.fill(); ctx.shadowBlur = 0;
      }
    }
  }

  // 出入口标记：上半区顶部 cols 1,3,5,7；下半区底部 cols 0,2,4,6
  ctx.font = 'bold 8px sans-serif';
  ctx.textBaseline = 'middle';
  [[1,3,5,7].map(c => [c * cellW, 0,  true]),
   [0,2,4,6].map(c => [c * cellW, ch, false])].flat().forEach(([dx, dy, atTop]) => {
    const dh = 10, dw = 14;
    const ry = atTop ? dy : dy - dh;
    ctx.fillStyle = '#1a2d42';
    _rr(ctx, dx - dw / 2, ry, dw, dh, 2);
    ctx.fill();
    ctx.strokeStyle = '#3a6a9066';
    ctx.lineWidth = 1;
    _rr(ctx, dx - dw / 2, ry, dw, dh, 2);
    ctx.stroke();
    ctx.fillStyle = '#5a9ac888';
    ctx.textAlign = 'center';
    ctx.fillText(atTop ? '▼' : '▲', dx, ry + dh / 2);
  });

  // 入座率 → 粒子半径（人越多粒子越大）
  const _occCount = Object.values(_seatStatusMap).filter(s => s !== 'empty').length;
  const _pR = 3.5 + (_occCount / 320) * 2; // 3.5~5.5 px

  // 推进并绘制路径粒子
  for (let i = _particles.length - 1; i >= 0; i--) {
    const p = _particles[i];
    if (p.done) { _particles.splice(i, 1); continue; }
    const elapsed = now - p.segStart;
    if (elapsed < 0) continue; // 延迟未到

    const t = Math.min(1, elapsed / p.segDur);
    const ease = _easeInOut(t);
    const from = p.waypoints[p.segIdx], to = p.waypoints[p.segIdx + 1];
    const x = from.x + (to.x - from.x) * ease;
    const y = from.y + (to.y - from.y) * ease;

    // 首段淡入，末段淡出
    let alpha = 1;
    if (p.segIdx === 0 && t < 0.25) alpha = t / 0.25;
    if (p.segIdx === p.totalSegs - 1 && t > 0.75) alpha = (1 - t) / 0.25;

    // 拖尾
    if (p.prevX !== undefined) {
      ctx.save();
      ctx.globalAlpha = Math.max(0, alpha * 0.28);
      ctx.beginPath();
      ctx.arc(p.prevX, p.prevY, _pR * 0.7, 0, Math.PI * 2);
      ctx.fillStyle = p.color;
      ctx.shadowColor = p.color; ctx.shadowBlur = 4;
      ctx.fill();
      ctx.restore();
    }
    p.prevX = x; p.prevY = y;

    ctx.save();
    ctx.globalAlpha = Math.max(0, alpha);
    ctx.beginPath();
    ctx.arc(x, y, _pR, 0, Math.PI * 2);
    ctx.fillStyle = p.color;
    ctx.shadowColor = p.color;
    ctx.shadowBlur = 12;
    ctx.fill();
    ctx.restore();

    if (t >= 1) {
      p.segIdx++;
      if (p.segIdx >= p.totalSegs) {
        p.done = true;
      } else {
        p.segStart = now;
        const nf = p.waypoints[p.segIdx], nt = p.waypoints[p.segIdx + 1];
        p.segDur = Math.max(80, Math.hypot(nt.x - nf.x, nt.y - nf.y) / 0.42);
      }
    }
  }

  _animRaf = requestAnimationFrame(_animLoop);
}

function renderHeatmap() {
  if (currentSeatView !== 'heat' || _lastSeats.length === 0) return;
  const canvas = document.getElementById('heatmapCanvas');
  if (!canvas) return;

  const COLS = 8, ROWS = 10;
  const cw = canvas.parentElement.clientWidth - 24;
  const cellW = Math.floor(cw / COLS);
  const cellH = 80;

  if (canvas.width !== cw) {
    canvas.width = cw;
    canvas.height = cellH * ROWS;
    _animCellW = cellW; _animCellH = cellH; _animCOLS = COLS;
    _animCH = canvas.height;
    _buildSeatPos(cellW, cellH, COLS);
  }

  _lastSeats.forEach(s => { _seatStatusMap[s.id] = s.status; });

  const MAX_P = 100;
  _lastSeats.forEach(seat => {
    const prev = _prevStates[seat.id];
    _prevStates[seat.id] = seat.status;
    if (prev === undefined || prev === seat.status) return;
    if (prev !== 'empty' && seat.status !== 'empty') return; // 忽略 reserved↔dining
    if (_particles.length >= MAX_P) return;
    const p = _makeParticle(seat.id, prev, seat.status);
    if (p) _particles.push(p);
  });

  if (!_animRaf) _animRaf = requestAnimationFrame(_animLoop);
}

// ====================================================
// 键盘快捷键
// ====================================================
document.addEventListener('keydown', e => {
  if (e.target.tagName === 'TEXTAREA' || e.target.tagName === 'INPUT') return;
  if (e.code === 'Space') {
    e.preventDefault();
    simControl(typeof localState !== 'undefined' && localState.running ? 'pause' : 'start');
  }
  if (e.code === 'KeyR') simControl('reset');
  if (e.code === 'ArrowRight' || e.code === 'ArrowLeft') {
    const sl = document.getElementById('speedSlider');
    if (!sl) return;
    let v = parseInt(sl.value) + (e.code === 'ArrowRight' ? 1 : -1);
    v = Math.max(0, Math.min(6, v));
    sl.value = v; setSpeedBySlider(v);
  }
});

// ====================================================
// Toast 提示
// ====================================================
let toastTimer = null;
function showToast(msg) {
  const el = document.getElementById('toast');
  el.textContent = msg;
  el.classList.add('show');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => el.classList.remove('show'), 2500);
}
