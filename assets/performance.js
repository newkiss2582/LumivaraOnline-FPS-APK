(() => {
  if (location.origin !== 'https://lumivaraonline.com') return;
  const desired = [0, 60, 120].includes(window.__lumivaraFpsLimit) ? String(window.__lumivaraFpsLimit) : '0';
  if (window.__lumivaraFpsEdition) { window.__lumivaraFpsEdition.configure(desired); return; }
  const fpsKey = 'pixelrpg.fps-limit', saverKey = 'pixelrpg.battery-saver', marker = 'lumivara.fps-edition.initialized';
  let fps = desired, applied = null;
  const store = () => { try { localStorage.setItem(fpsKey, fps); } catch {} };
  try { if (localStorage.getItem(marker) === null) { localStorage.setItem(saverKey, 'off'); localStorage.setItem(marker, '1'); } } catch {}
  store();
  const apply = () => {
    const select = document.getElementById('fps-limit');
    if (!select || typeof select.onchange !== 'function' || !Array.from(select.options).some(option => option.value === fps)) return false;
    if (applied !== select || select.value !== fps) { select.value = fps; select.dispatchEvent(new Event('change', { bubbles: true })); applied = select; }
    const saver = document.getElementById('battery-saver-idle');
    try {
      const value = localStorage.getItem(saverKey);
      if (saver && typeof saver.onchange === 'function' && Array.from(saver.options).some(option => option.value === value) && saver.value !== value) {
        saver.value = value; saver.dispatchEvent(new Event('change', { bubbles: true }));
      }
    } catch {}
    return true;
  };
  let observer;
  window.__lumivaraFpsEdition = { configure(value) {
    if (!['0', '60', '120'].includes(value)) return;
    if (value !== fps) { fps = value; store(); applied = null; }
    apply();
  }};
  const start = () => {
    const style = document.createElement('style');style.textContent = 'canvas{touch-action:none}';
    (document.head || document.documentElement).append(style);
    if (apply()) return;
    observer = new MutationObserver(() => { if (apply()) observer.disconnect(); });
    observer.observe(document.documentElement, { childList: true, subtree: true });
    setTimeout(() => observer.disconnect(), 30000);
  };
  if (document.documentElement) start(); else document.addEventListener('DOMContentLoaded', start, { once: true });
})();
