(() => {
  if (location.origin !== 'https://lumivaraonline.com' || window.__lumivaraSelectPicker) return;
  window.__lumivaraSelectPicker = true;
  let active = null;
  const labels = () => window.__lumivaraSelectPickerLanguage === 'th'
    ? {choose:'เลือก',cancel:'ยกเลิก',apply:'ตกลง'}
    : window.__lumivaraSelectPickerLanguage === 'en'
      ? {choose:'Choose',cancel:'Cancel',apply:'Apply'}
      : {choose:'เลือก / Choose',cancel:'ยกเลิก / Cancel',apply:'ตกลง / Apply'};
  window.__lumivaraPickerLocale = {setLocale(locale) {
    if(locale !== 'th' && locale !== 'en') return;
    window.__lumivaraSelectPickerLanguage = locale;
    if(!active) return;
    const text=labels();active.cancel.textContent=text.cancel;
    if(active.done)active.done.textContent=text.apply;
    if(active.fallbackTitle){active.heading.textContent=text.choose;active.dialog.setAttribute('aria-label',text.choose);}
  }};
  const suppress = event => { event.preventDefault(); event.stopImmediatePropagation(); };
  const close = () => {
    if (!active) return;
    const { dialog, select, observer } = active;
    active = null;
    observer.disconnect();
    if (typeof dialog.close === 'function' && dialog.open) dialog.close();
    dialog.remove();
    if (select.isConnected) select.focus({ preventScroll: true });
  };
  const open = select => {
    if (active || select.disabled || !document.body) return;
    const options = Array.from(select.options);
    const picked = new Set(options.filter(option => option.selected));
    const dialog = document.createElement('dialog');
    dialog.setAttribute('data-lumivara-picker', '');
    Object.assign(dialog.style, { position: 'fixed', inset: '0', margin: 'auto', padding: '0',
      width: 'min(92vw, 540px)', maxHeight: '90vh', border: '2px solid #dbb763', borderRadius: '12px',
      background: '#142347', color: '#f8f1e0', zIndex: '2147483647', boxSizing: 'border-box' });
    const container = document.createElement('div'); dialog.append(container);
    const shadow = container.attachShadow({ mode: 'open' });
    const style = document.createElement('style');
    style.textContent = `:host{font:16px sans-serif}*{box-sizing:border-box}section{padding:12px;display:flex;flex-direction:column;max-height:86vh}h2{font-size:18px;margin:0 0 10px;color:#f3d38b}.items{overflow:auto;overscroll-behavior:contain;flex:1;min-height:0}button{font:inherit;color:#f8f1e0;background:#263f70;border:1px solid #647ea8;border-radius:6px;padding:10px;min-height:44px;touch-action:manipulation}button:focus{outline:2px solid #dbb763}.option{display:block;text-align:left;width:100%;margin:4px 0}.option[aria-selected=true]{background:#405b91;border-color:#dbb763}.option:disabled{opacity:.45}.group{color:#f3d38b;padding:10px 0 4px}footer{display:flex;gap:8px;padding-top:10px}footer button{flex:1}`;
    shadow.append(style);
    const panel = document.createElement('section');
    const heading = document.createElement('h2');
    const associatedLabel = select.labels && select.labels[0];
    let labelText = '';
    if (associatedLabel) {
      const copy = associatedLabel.cloneNode(true);
      copy.querySelectorAll('select').forEach(control => control.remove()); labelText = copy.textContent.trim();
    }
    const fallbackTitle = !select.getAttribute('aria-label') && !labelText;
    heading.textContent = (select.getAttribute('aria-label') || labelText || labels().choose).slice(0, 160);
    dialog.setAttribute('aria-label', heading.textContent);
    panel.append(heading);
    const items = document.createElement('div'); items.className = 'items'; items.setAttribute('role', 'listbox');
    if (select.multiple) items.setAttribute('aria-multiselectable', 'true');
    const commit = () => {
      if (!select.isConnected || select.disabled) { close(); return; }
      let changed = false;
      options.forEach(option => {
        if (!Array.from(select.options).includes(option)) return;
        const chosen = picked.has(option);
        if (option.selected !== chosen) changed = true;
        option.selected = chosen;
      });
      close();
      if (changed) {
        select.dispatchEvent(new Event('input', { bubbles: true }));
        select.dispatchEvent(new Event('change', { bubbles: true }));
      }
    };
    let previousGroup = null;
    options.forEach(option => {
      const group = option.parentElement && option.parentElement.tagName === 'OPTGROUP' ? option.parentElement : null;
      if (group && group !== previousGroup) {
        const label = document.createElement('div'); label.className = 'group'; label.textContent = group.label; items.append(label);
      }
      previousGroup = group;
      const button = document.createElement('button'); button.type = 'button'; button.className = 'option';
      button.textContent = option.label || option.textContent;
      button.disabled = option.disabled || !!(group && group.disabled) || option.hidden;
      button.setAttribute('role', 'option'); button.setAttribute('aria-selected', String(picked.has(option)));
      button.addEventListener('click', event => {
        suppress(event);
        if (button.disabled || !select.isConnected) return;
        if (select.multiple) {
          if (picked.has(option)) picked.delete(option); else picked.add(option);
          button.setAttribute('aria-selected', String(picked.has(option)));
        } else { picked.clear(); picked.add(option); commit(); }
      });
      items.append(button);
    });
    panel.append(items);
    const footer = document.createElement('footer');
    const cancel = document.createElement('button'); cancel.type = 'button'; cancel.textContent = labels().cancel;
    cancel.addEventListener('click', event => { suppress(event); close(); }); footer.append(cancel);
    let done = null;
    if (select.multiple) {
      done = document.createElement('button'); done.type = 'button'; done.textContent = labels().apply;
      done.addEventListener('click', event => { suppress(event); commit(); }); footer.append(done);
    }
    panel.append(footer); shadow.append(panel); document.body.append(dialog);
    const observer = new MutationObserver(() => { if (!select.isConnected || select.disabled) close(); });
    observer.observe(document.documentElement, { childList: true, subtree: true, attributes: true, attributeFilter: ['disabled'] });
    active = { dialog, select, observer, heading, fallbackTitle, cancel, done };
    dialog.addEventListener('cancel', event => { event.preventDefault(); close(); });
    dialog.addEventListener('keydown', event => { event.stopPropagation(); if (event.key === 'Escape') { event.preventDefault(); close(); } });
    for (const kind of ['pointerdown', 'pointerup', 'touchstart', 'touchend', 'mousedown', 'mouseup', 'click']) {
      dialog.addEventListener(kind, event => event.stopPropagation(), { passive: kind.startsWith('touch') });
    }
    if (typeof dialog.showModal === 'function') dialog.showModal(); else dialog.setAttribute('open', '');
    const focus = items.querySelector('[aria-selected="true"]:not(:disabled)') || items.querySelector('button:not(:disabled)') || cancel;
    focus.focus({ preventScroll: true });
  };
  const intercept = event => {
    const source = event.composedPath ? event.composedPath()[0] : event.target;
    const target = source && typeof source.closest === 'function' ? source.closest('select') : null;
    if (!target || target.disabled) return;
    if (event.type === 'keydown' && !['Enter', ' ', 'ArrowDown'].includes(event.key)) return;
    if (event.type.startsWith('pointer') && event.button !== 0) return;
    suppress(event);
    // Wait for release so the new dialog cannot steal the opening gesture.
    if (!['pointerdown', 'mousedown', 'touchstart'].includes(event.type)) open(target);
  };
  // Bind cancelable touch handlers ONLY to selects, never window/document/body.
  // Ordinary game touches no longer wait for a page-wide non-passive interceptor.
  const installed = new WeakSet();
  const install = select => {
    if (installed.has(select)) return;
    installed.add(select);select.style.touchAction = 'manipulation';
    for (const kind of ['pointerdown', 'pointerup', 'mousedown', 'mouseup', 'touchstart', 'touchend', 'click', 'keydown']) {
      select.addEventListener(kind, intercept, { capture: true, passive: false });
    }
  };
  const scan = node => {
    if (node.nodeType !== 1) return;
    if (node.tagName === 'SELECT') install(node);
    node.querySelectorAll('select').forEach(install);
  };
  const start = () => {
    scan(document.documentElement);
    new MutationObserver(records => {
      for (const record of records) for (const node of record.addedNodes) scan(node);
    }).observe(document.documentElement, { childList: true, subtree: true });
  };
  if (document.documentElement) start(); else document.addEventListener('DOMContentLoaded', start, { once: true });
})();
