/* 无依赖 Canvas 图表：折线图 / 分组柱状图 */
const PALETTE = ['#e8552d', '#2f6f8f', '#16803c', '#b45309'];

function niceMax(value) {
  if (!Number.isFinite(value) || value <= 0) return 1;
  const exponent = Math.floor(Math.log10(value));
  const base = 10 ** exponent;
  const n = value / base;
  let nice;
  if (n <= 1) nice = 1;
  else if (n <= 2) nice = 2;
  else if (n <= 2.5) nice = 2.5;
  else if (n <= 5) nice = 5;
  else nice = 10;
  return nice * base;
}

function textWidth(ctx, text, font) {
  ctx.save(); ctx.font = font; const w = ctx.measureText(String(text)).width; ctx.restore();
  return w;
}

function createTooltip(canvas) {
  const parent = canvas.parentElement;
  const existing = parent.querySelector('.chart-tooltip');
  if (existing) existing.remove();
  const tip = document.createElement('div');
  tip.className = 'chart-tooltip';
  parent.appendChild(tip);
  return tip;
}

function mount(canvas, drawFn) {
  let hover = -1;
  const ctx = canvas.getContext('2d');
  const tooltip = createTooltip(canvas);
  let destroyed = false;

  const render = () => {
    if (destroyed) return;
    const rect = canvas.getBoundingClientRect();
    if (!rect.width || !rect.height) return;
    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    canvas.width = Math.round(rect.width * dpr);
    canvas.height = Math.round(rect.height * dpr);
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.clearRect(0, 0, rect.width, rect.height);
    drawFn(ctx, rect.width, rect.height, hover, tooltip);
  };

  const onMove = (event) => {
    const rect = canvas.getBoundingClientRect();
    const x = event.clientX - rect.left;
    const geometry = canvas.__geometry;
    if (!geometry) return;
    let idx = -1;
    geometry.xs.forEach((px, i) => {
      if (Math.abs(px - x) <= Math.max(10, (geometry.step || 20) / 2)) idx = i;
    });
    if (idx !== hover) {
      hover = idx;
      render();
    }
  };
  const onLeave = () => { hover = -1; render(); };

  canvas.addEventListener('mousemove', onMove);
  canvas.addEventListener('mouseleave', onLeave);
  const ro = new ResizeObserver(() => requestAnimationFrame(render));
  ro.observe(canvas);
  render();

  return {
    destroy() {
      destroyed = true;
      ro.disconnect();
      canvas.removeEventListener('mousemove', onMove);
      canvas.removeEventListener('mouseleave', onLeave);
      tooltip.remove();
    }
  };
}

function drawAxes(ctx, w, h, pad, labels, yMax, yFormat, maxLabels) {
  const plotW = w - pad.left - pad.right;
  const plotH = h - pad.top - pad.bottom;
  ctx.save();
  ctx.strokeStyle = '#eef0f3';
  ctx.lineWidth = 1;
  ctx.fillStyle = '#9ca3af';
  ctx.font = '11px "PingFang SC", "Microsoft YaHei", system-ui, sans-serif';
  const ticks = 4;
  for (let i = 0; i <= ticks; i += 1) {
    const y = pad.top + (plotH * i) / ticks;
    ctx.beginPath();
    ctx.moveTo(pad.left, y);
    ctx.lineTo(pad.left + plotW, y);
    ctx.stroke();
    const value = yMax - (yMax * i) / ticks;
    ctx.textAlign = 'right';
    ctx.textBaseline = 'middle';
    ctx.fillText(yFormat ? yFormat(value) : String(Math.round(value)), pad.left - 8, y);
  }
  if (labels && labels.length) {
    const count = Math.max(1, Math.floor(plotW / 62));
    const stride = Math.ceil(labels.length / Math.min(count, maxLabels || count));
    ctx.fillStyle = '#8b95a1';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'top';
    labels.forEach((label, i) => {
      if (i % stride !== 0 && i !== labels.length - 1) return;
      const x = labels.length === 1 ? pad.left + plotW / 2 : pad.left + (plotW * i) / (labels.length - 1);
      ctx.fillText(String(label), x, pad.top + plotH + 10);
    });
  }
  ctx.restore();
}

export function lineChart(canvas, options = {}) {
  const {
    labels = [], series = [], yFormat = (v) => String(Math.round(v)), maxLabels = 12
  } = options;
  const normalized = series.map((s, i) => ({
    name: s.name || `系列${i + 1}`,
    color: s.color || PALETTE[i % PALETTE.length],
    data: Array.isArray(s.data) ? s.data.map((v) => Number(v) || 0) : [],
    area: s.area !== false
  }));
  const all = normalized.flatMap((s) => s.data);
  const rawMax = Math.max(0, ...all);
  const yMax = niceMax(rawMax || 1);

  return mount(canvas, (ctx, w, h, hover, tooltip) => {
    if (!labels.length || !normalized.length) {
      ctx.fillStyle = '#9ca3af';
      ctx.font = '13px "PingFang SC", "Microsoft YaHei", system-ui';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillText('暂无数据', w / 2, h / 2);
      tooltip.style.opacity = '0';
      return;
    }
    const pad = { left: 54, right: 18, top: 20, bottom: 38 };
    const plotW = w - pad.left - pad.right;
    const plotH = h - pad.top - pad.bottom;
    drawAxes(ctx, w, h, pad, labels, yMax, yFormat, maxLabels);
    const step = labels.length === 1 ? 0 : plotW / (labels.length - 1);
    const xs = labels.map((_, i) => (labels.length === 1 ? pad.left + plotW / 2 : pad.left + step * i));
    canvas.__geometry = { xs, step };

    normalized.forEach((s) => {
      const points = s.data.map((v, i) => ({
        x: xs[i],
        y: pad.top + plotH - (Math.min(v, yMax) / yMax) * plotH
      }));
      if (!points.length) return;
      if (s.area) {
        const grad = ctx.createLinearGradient(0, pad.top, 0, pad.top + plotH);
        grad.addColorStop(0, `${s.color}2a`);
        grad.addColorStop(1, `${s.color}00`);
        ctx.beginPath();
        ctx.moveTo(points[0].x, pad.top + plotH);
        points.forEach((p) => ctx.lineTo(p.x, p.y));
        ctx.lineTo(points[points.length - 1].x, pad.top + plotH);
        ctx.closePath();
        ctx.fillStyle = grad;
        ctx.fill();
      }
      ctx.beginPath();
      points.forEach((p, i) => { if (i === 0) ctx.moveTo(p.x, p.y); else ctx.lineTo(p.x, p.y); });
      ctx.strokeStyle = s.color;
      ctx.lineWidth = 2;
      ctx.lineJoin = 'round';
      ctx.stroke();

      if (points.length <= 40) {
        points.forEach((p) => {
          ctx.beginPath();
          ctx.arc(p.x, p.y, 2.6, 0, Math.PI * 2);
          ctx.fillStyle = s.color;
          ctx.fill();
        });
      }
    });

    if (hover >= 0 && hover < labels.length) {
      const x = xs[hover];
      ctx.save();
      ctx.strokeStyle = '#cfd5dd';
      ctx.setLineDash([4, 4]);
      ctx.beginPath();
      ctx.moveTo(x, pad.top);
      ctx.lineTo(x, pad.top + plotH);
      ctx.stroke();
      ctx.restore();
      normalized.forEach((s) => {
        const y = pad.top + plotH - (Math.min(s.data[hover] || 0, yMax) / yMax) * plotH;
        ctx.beginPath();
        ctx.arc(x, y, 4, 0, Math.PI * 2);
        ctx.fillStyle = '#fff';
        ctx.fill();
        ctx.lineWidth = 2;
        ctx.strokeStyle = s.color;
        ctx.stroke();
      });
      tooltip.style.opacity = '1';
      tooltip.innerHTML = `<div style="font-weight:600;margin-bottom:2px">${labels[hover]}</div>` +
        normalized.map((s) => `<div style="display:flex;align-items:center;gap:6px"><span style="width:6px;height:6px;border-radius:50%;background:${s.color}"></span>${s.name}：${yFormat ? yFormat(s.data[hover] || 0) : s.data[hover]}</div>`).join('');
      const tipX = Math.min(Math.max(x + 12, 8), w - 150);
      tooltip.style.left = `${tipX}px`;
      tooltip.style.top = '12px';
    } else {
      tooltip.style.opacity = '0';
    }
  });
}

export function barChart(canvas, options = {}) {
  const {
    labels = [], series = [], yFormat = (v) => String(Math.round(v)), maxLabels = 12
  } = options;
  const normalized = series.map((s, i) => ({
    name: s.name || `系列${i + 1}`,
    color: s.color || PALETTE[i % PALETTE.length],
    data: Array.isArray(s.data) ? s.data.map((v) => Number(v) || 0) : []
  }));
  const all = normalized.flatMap((s) => s.data);
  const yMax = niceMax(Math.max(0, ...all) || 1);

  return mount(canvas, (ctx, w, h, hover, tooltip) => {
    if (!labels.length || !normalized.length) {
      ctx.fillStyle = '#9ca3af';
      ctx.font = '13px "PingFang SC", "Microsoft YaHei", system-ui';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillText('暂无数据', w / 2, h / 2);
      tooltip.style.opacity = '0';
      return;
    }
    const pad = { left: 54, right: 18, top: 20, bottom: 38 };
    const plotW = w - pad.left - pad.right;
    const plotH = h - pad.top - pad.bottom;
    drawAxes(ctx, w, h, pad, labels, yMax, yFormat, maxLabels);
    const groupWidth = plotW / labels.length;
    const barGap = 3;
    const barWidth = Math.min(44, Math.max(4, (groupWidth * 0.7 - barGap * (normalized.length - 1)) / normalized.length));
    const groupInner = barWidth * normalized.length + barGap * (normalized.length - 1);
    const xs = labels.map((_, i) => pad.left + groupWidth * i + groupWidth / 2);
    canvas.__geometry = { xs, step: groupWidth };

    normalized.forEach((s, si) => {
      s.data.forEach((value, i) => {
        const x = xs[i] - groupInner / 2 + si * (barWidth + barGap);
        const barH = Math.max(value === 0 ? 0 : 1.5, (Math.min(value, yMax) / yMax) * plotH);
        const y = pad.top + plotH - barH;
        ctx.fillStyle = s.color;
        ctx.beginPath();
        const r = Math.min(3, barWidth / 2);
        ctx.moveTo(x, y + barH);
        ctx.lineTo(x, y + r);
        ctx.quadraticCurveTo(x, y, x + r, y);
        ctx.lineTo(x + barWidth - r, y);
        ctx.quadraticCurveTo(x + barWidth, y, x + barWidth, y + r);
        ctx.lineTo(x + barWidth, y + barH);
        ctx.closePath();
        ctx.fill();
      });
    });

    if (hover >= 0 && hover < labels.length) {
      ctx.save();
      ctx.fillStyle = 'rgba(232, 85, 45, .05)';
      ctx.fillRect(pad.left + groupWidth * hover, pad.top, groupWidth, plotH);
      ctx.restore();
      tooltip.style.opacity = '1';
      tooltip.innerHTML = `<div style="font-weight:600;margin-bottom:2px">${labels[hover]}</div>` +
        normalized.map((s) => `<div style="display:flex;align-items:center;gap:6px"><span style="width:6px;height:6px;border-radius:50%;background:${s.color}"></span>${s.name}：${yFormat ? yFormat(s.data[hover] || 0) : s.data[hover]}</div>`).join('');
      const tipX = Math.min(Math.max(xs[hover] + 10, 8), w - 150);
      tooltip.style.left = `${tipX}px`;
      tooltip.style.top = '12px';
    } else {
      tooltip.style.opacity = '0';
    }
  });
}

export { PALETTE };
