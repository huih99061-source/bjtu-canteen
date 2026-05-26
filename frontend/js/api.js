// ====================================================
// api.js - 与后端Spring Boot接口通信
// ====================================================

const API_BASE = 'http://49.233.22.6:8888/api';

async function apiFetch(path, options = {}) {
  try {
    const resp = await fetch(API_BASE + path, {
      headers: { 'Content-Type': 'application/json' },
      ...options
    });
    return await resp.json();
  } catch (e) {
    console.warn('[API Error]', path, e.message);
    return null;
  }
}

// 获取仿真快照（核心轮询接口）
async function apiGetSnapshot() {
  return apiFetch('/sim/snapshot');
}

// 仿真控制
async function apiSimStart() { return apiFetch('/sim/start', { method: 'POST' }); }
async function apiSimPause() { return apiFetch('/sim/pause', { method: 'POST' }); }
async function apiSimReset() { return apiFetch('/sim/reset', { method: 'POST' }); }
async function apiSetSpeed(s) { return apiFetch('/sim/speed', { method: 'POST', body: JSON.stringify({ speed: parseInt(s) }) }); }

// 占座
async function apiReserveSeat(seatId) {
  return apiFetch(`/sim/seat/${seatId}/reserve`, { method: 'POST' });
}

// 菜品
async function apiGetWindows() { return apiFetch('/canteen/windows'); }
async function apiGetWindow(id) { return apiFetch(`/canteen/window/${id}`); }

// 评价
async function apiSubmitFeedback(data) {
  return apiFetch('/canteen/feedback', { method: 'POST', body: JSON.stringify(data) });
}
async function apiGetFeedback() { return apiFetch('/canteen/feedback?limit=20'); }
