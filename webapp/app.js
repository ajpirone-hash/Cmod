/* Gang Box List: check off tools and send the list to the warehouse. */
(function () {
  "use strict";

  var CONFIG = window.GANGBOX_CONFIG || {};
  var STARTER = window.GANGBOX_TOOLS || [];
  var STORE_KEY = "gangbox.v1";

  // ---------- Saved state (kept on the phone) ----------
  var state = load();

  function load() {
    var empty = { job: { jobName: "", requestedBy: "", dateNeeded: "" }, picks: {}, custom: [], open: {}, tipDismissed: false, warehouseEmail: "" };
    try {
      var raw = localStorage.getItem(STORE_KEY);
      if (!raw) return empty;
      var s = JSON.parse(raw);
      return {
        job: Object.assign(empty.job, s.job || {}),
        picks: s.picks || {},
        custom: Array.isArray(s.custom) ? s.custom : [],
        open: s.open || {},
        tipDismissed: !!s.tipDismissed,
        warehouseEmail: s.warehouseEmail || "",
      };
    } catch (e) {
      return empty;
    }
  }

  function save() {
    try { localStorage.setItem(STORE_KEY, JSON.stringify(state)); } catch (e) { /* storage full or blocked */ }
  }

  // A link like .../?to=warehouse@company.com sets the warehouse email.
  (function readLinkSettings() {
    var to = new URLSearchParams(location.search).get("to");
    if (to && /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(to.trim())) {
      state.warehouseEmail = to.trim();
      save();
    }
  })();

  function warehouseEmail() {
    return state.warehouseEmail || CONFIG.warehouseEmail || "";
  }

  // ---------- Tool list ----------
  function toolId(category, name) { return category + "|" + name; }

  function categories() {
    var cats = STARTER.map(function (c) {
      return { name: c.name, tools: c.tools.map(function (t) {
        var name = typeof t === "string" ? t : t.name;
        return { id: toolId(c.name, name), name: name, category: c.name, custom: false, choose: typeof t === "string" ? null : t.choose || null };
      }) };
    });
    state.custom.forEach(function (t) {
      var cat = cats.filter(function (c) { return c.name === t.category; })[0];
      if (!cat) { cat = { name: t.category, tools: [] }; cats.push(cat); }
      cat.tools.push({ id: t.id, name: t.name, category: t.category, custom: true });
    });
    return cats;
  }

  function allTools() {
    return categories().reduce(function (a, c) { return a.concat(c.tools); }, []);
  }

  function pickedTools() {
    return allTools().filter(function (t) { return state.picks[t.id]; });
  }

  // ---------- Elements ----------
  var $ = function (id) { return document.getElementById(id); };
  var els = {
    title: $("title"), jobName: $("jobName"), requestedBy: $("requestedBy"), dateNeeded: $("dateNeeded"),
    search: $("search"), toolList: $("toolList"), addToolBtn: $("addToolBtn"),
    countText: $("countText"), sendBtn: $("sendBtn"), menuBtn: $("menuBtn"),
    modal: $("modal"), modalTitle: $("modalTitle"), modalBody: $("modalBody"), modalActions: $("modalActions"),
    toast: $("toast"), installTip: $("installTip"), installTipText: $("installTipText"),
    installBtn: $("installBtn"), installDismiss: $("installDismiss"),
  };

  if (CONFIG.company) {
    els.title.textContent = CONFIG.company + " Gang Box";
    document.title = els.title.textContent;
  }

  function esc(s) {
    return String(s).replace(/[&<>"']/g, function (c) {
      return { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c];
    });
  }

  // ---------- Job info ----------
  ["jobName", "requestedBy", "dateNeeded"].forEach(function (key) {
    els[key].value = state.job[key] || "";
    els[key].addEventListener("input", function () { state.job[key] = els[key].value; save(); });
  });

  // ---------- Render tool list ----------
  function render() {
    var q = els.search.value.trim().toLowerCase();
    var html = "";
    var any = false;
    categories().forEach(function (cat) {
      var tools = q ? cat.tools.filter(function (t) { return t.name.toLowerCase().indexOf(q) >= 0 || cat.name.toLowerCase().indexOf(q) >= 0; }) : cat.tools;
      if (!tools.length) return;
      any = true;
      var picked = cat.tools.filter(function (t) { return state.picks[t.id]; }).length;
      var open = q || state.open[cat.name];
      html += '<div class="cat' + (open ? " open" : "") + '" data-cat="' + esc(cat.name) + '">' +
        '<button type="button" class="cat-head" data-action="cat" aria-expanded="' + (open ? "true" : "false") + '">' +
        '<span class="arrow">▶</span><span class="cat-name">' + esc(cat.name) + "</span>" +
        (picked ? '<span class="badge">' + picked + "</span>" : "") + "</button>" +
        '<div class="cat-tools">' + tools.map(toolHtml).join("") + "</div></div>";
    });
    if (!any) {
      html = '<div class="empty">No tools match "' + esc(els.search.value) + '". Tap the button below to add it.</div>';
    }
    els.toolList.innerHTML = html;
    updateCount();
  }

  function toolHtml(t) {
    var p = state.picks[t.id];
    return '<div class="tool' + (p ? " on" : "") + '" data-id="' + esc(t.id) + '">' +
      '<button type="button" class="tool-main" data-action="toggle" aria-pressed="' + (p ? "true" : "false") + '">' +
      '<span class="box">' + (p ? "✓" : "") + '</span><span class="tool-name">' + esc(t.name) + "</span></button>" +
      '<div class="tool-extra">' +
      '<div class="qty"><span class="qty-label">How many:</span>' +
      '<button type="button" data-action="minus" aria-label="One less"' + (p && p.qty <= 1 ? " disabled" : "") + ">−</button>" +
      '<span class="qty-num">' + (p ? p.qty : 1) + "</span>" +
      '<button type="button" data-action="plus" aria-label="One more">+</button></div>' +
      (t.choose ? chooseHtml(t, p) : "") +
      '<input class="note" type="text" data-action="note" placeholder="Note (size, brand...) optional" value="' + esc(p ? p.note : "") + '">' +
      (t.custom ? '<button type="button" class="remove-tool" data-action="remove">Remove this tool from the list</button>' : "") +
      "</div></div>";
  }

  // Big buttons for a required choice, like a chain fall's lift length.
  function chooseHtml(t, p) {
    var cur = p && p.opt;
    return '<div class="choose"><div class="choose-label">' + esc(t.choose.label) + ':</div><div class="choose-opts">' +
      t.choose.options.map(function (o) {
        return '<button type="button" class="opt' + (cur === o ? " on" : "") + '" data-action="opt" data-val="' + esc(o) + '">' + esc(o) + "</button>";
      }).join("") + "</div></div>";
  }

  function updateCount() {
    var n = Object.keys(state.picks).length;
    els.countText.textContent = n === 0 ? "No tools picked yet" : n === 1 ? "1 tool picked" : n + " tools picked";
    els.sendBtn.disabled = n === 0;
  }

  els.search.addEventListener("input", render);

  els.toolList.addEventListener("click", function (e) {
    var btn = e.target.closest("[data-action]");
    if (!btn) return;
    var action = btn.getAttribute("data-action");
    if (action === "cat") {
      var catEl = btn.closest(".cat");
      var name = catEl.getAttribute("data-cat");
      state.open[name] = !catEl.classList.contains("open");
      save();
      catEl.classList.toggle("open", state.open[name]);
      btn.setAttribute("aria-expanded", state.open[name] ? "true" : "false");
      return;
    }
    var toolEl = btn.closest(".tool");
    if (!toolEl) return;
    var id = toolEl.getAttribute("data-id");
    if (action === "toggle") {
      if (state.picks[id]) delete state.picks[id];
      else state.picks[id] = { qty: 1, note: "" };
    } else if (action === "plus" && state.picks[id]) {
      state.picks[id].qty = Math.min(999, state.picks[id].qty + 1);
    } else if (action === "minus" && state.picks[id]) {
      state.picks[id].qty = Math.max(1, state.picks[id].qty - 1);
    } else if (action === "opt" && state.picks[id]) {
      var val = btn.getAttribute("data-val");
      state.picks[id].opt = state.picks[id].opt === val ? "" : val;
    } else if (action === "remove") {
      var tool = state.custom.filter(function (t) { return t.id === id; })[0];
      confirmBox("Remove " + (tool ? tool.name : "this tool") + "?", "It comes off the list on this phone.", "Remove", function () {
        state.custom = state.custom.filter(function (t) { return t.id !== id; });
        delete state.picks[id];
        save(); render();
      }, true);
      return;
    } else {
      return;
    }
    save();
    // Redraw just this tool so the page doesn't jump.
    var t = allTools().filter(function (x) { return x.id === id; })[0];
    var wrap = document.createElement("div");
    wrap.innerHTML = toolHtml(t);
    toolEl.replaceWith(wrap.firstChild);
    var catEl2 = els.toolList.querySelector('.cat[data-cat="' + cssEsc(t.category) + '"]');
    if (catEl2) {
      var n = categories().filter(function (c) { return c.name === t.category; })[0].tools.filter(function (x) { return state.picks[x.id]; }).length;
      var head = catEl2.querySelector(".cat-head");
      var badge = head.querySelector(".badge");
      if (n && !badge) { badge = document.createElement("span"); badge.className = "badge"; head.appendChild(badge); }
      if (badge) { if (n) badge.textContent = n; else badge.remove(); }
    }
    updateCount();
  });

  els.toolList.addEventListener("input", function (e) {
    if (e.target.getAttribute("data-action") !== "note") return;
    var id = e.target.closest(".tool").getAttribute("data-id");
    if (state.picks[id]) { state.picks[id].note = e.target.value; save(); }
  });

  function cssEsc(s) { return window.CSS && CSS.escape ? CSS.escape(s) : s.replace(/"/g, '\\"'); }

  // ---------- Pop-ups ----------
  function openModal(title, bodyHtml, actions) {
    els.modalTitle.textContent = title;
    els.modalBody.innerHTML = bodyHtml;
    els.modalActions.innerHTML = "";
    actions.forEach(function (a) {
      var b = document.createElement("button");
      b.type = "button";
      b.className = "btn " + (a.style || "btn-outline");
      b.textContent = a.label;
      b.addEventListener("click", a.onClick);
      els.modalActions.appendChild(b);
    });
    els.modal.hidden = false;
  }
  function closeModal() { els.modal.hidden = true; }
  els.modal.addEventListener("click", function (e) { if (e.target === els.modal) closeModal(); });

  function confirmBox(title, text, yesLabel, onYes, danger) {
    openModal(title, "<p>" + esc(text) + "</p>", [
      { label: yesLabel, style: danger ? "btn-danger" : "btn-primary", onClick: function () { closeModal(); onYes(); } },
      { label: "Cancel", style: "btn-plain", onClick: closeModal },
    ]);
  }

  var toastTimer;
  function toast(msg) {
    els.toast.textContent = msg;
    els.toast.hidden = false;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () { els.toast.hidden = true; }, 3500);
  }

  // ---------- Add a tool ----------
  els.addToolBtn.addEventListener("click", function () {
    var cats = categories().map(function (c) { return c.name; });
    if (cats.indexOf("Other") < 0) cats.push("Other");
    openModal("Add a tool",
      '<label class="field"><span>Tool name</span><input id="newToolName" type="text" autocapitalize="words" value="' + esc(els.search.value.trim()) + '"></label>' +
      '<label class="field"><span>Where does it go?</span><select id="newToolCat">' +
      cats.map(function (c) { return "<option>" + esc(c) + "</option>"; }).join("") + "</select></label>" +
      '<div id="newToolErr" class="warn"></div>',
      [
        { label: "Add it and check it", style: "btn-primary", onClick: function () {
          var name = $("newToolName").value.trim();
          var cat = $("newToolCat").value;
          if (!name) { $("newToolErr").textContent = "Type the tool name first."; return; }
          var id = toolId(cat, name);
          if (allTools().some(function (t) { return t.id.toLowerCase() === id.toLowerCase(); })) {
            $("newToolErr").textContent = "That tool is already on the list."; return;
          }
          state.custom.push({ id: id, name: name, category: cat });
          state.picks[id] = { qty: 1, note: "" };
          state.open[cat] = true;
          save(); closeModal();
          els.search.value = "";
          render();
          toast(name + " added and checked");
        } },
        { label: "Cancel", style: "btn-plain", onClick: closeModal },
      ]);
    setTimeout(function () { var i = $("newToolName"); if (i) i.focus(); }, 50);
  });

  // ---------- Building the file ----------
  function prettyDate(iso) {
    var m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(iso || "");
    if (!m) return iso || "";
    return Number(m[2]) + "/" + Number(m[3]) + "/" + m[1];
  }

  function orderLines() {
    return pickedTools().map(function (t) {
      var p = state.picks[t.id];
      return { category: t.category, tool: t.name, option: p.opt || "", optionLabel: t.choose ? t.choose.label : "", needsOption: !!t.choose, qty: p.qty, note: (p.note || "").trim() };
    });
  }

  function csvCell(v) {
    v = String(v).replace(/\r\n|\r|\n/g, " ");
    if (v && "=+-@".indexOf(v[0]) >= 0) v = "'" + v; // keep Excel from treating it as a formula
    if (/[",]/.test(v) || v !== v.trim()) v = '"' + v.replace(/"/g, '""') + '"';
    return v;
  }

  function buildCsv(job, lines) {
    var rows = [["Job", "Requested By", "Date Needed", "Category", "Tool", "Size / Length", "Qty", "Notes"]];
    lines.forEach(function (l) {
      rows.push([job.jobName.trim(), job.requestedBy.trim(), prettyDate(job.dateNeeded), l.category, l.tool, l.option, String(l.qty), l.note]);
    });
    return "﻿" + rows.map(function (r) { return r.map(csvCell).join(","); }).join("\r\n") + "\r\n";
  }

  function fileName(job) {
    var part = job.jobName.trim().replace(/[^A-Za-z0-9-]+/g, "_").replace(/^_+|_+$/g, "").slice(0, 40);
    var d = new Date();
    var today = d.getFullYear() + "-" + String(d.getMonth() + 1).padStart(2, "0") + "-" + String(d.getDate()).padStart(2, "0");
    return "GangBox_" + (part ? part + "_" : "") + today + ".csv";
  }

  function subjectLine(job) {
    return (CONFIG.company ? CONFIG.company + " " : "") + "Gang box list - " + (job.jobName.trim() || "No job name") +
      (job.dateNeeded ? " (needed " + prettyDate(job.dateNeeded) + ")" : "");
  }

  function lineText(l) {
    return l.qty + " x " + l.tool + (l.option ? ", " + l.option + " " + l.optionLabel.toLowerCase() : "") + (l.note ? " (" + l.note + ")" : "");
  }

  function plainText(job, lines) {
    var t = "Gang box tool list (" + lines.length + (lines.length === 1 ? " item" : " items") + ")\n\n";
    if (job.jobName.trim()) t += "Job: " + job.jobName.trim() + "\n";
    if (job.requestedBy.trim()) t += "Requested by: " + job.requestedBy.trim() + "\n";
    if (job.dateNeeded) t += "Needed: " + prettyDate(job.dateNeeded) + "\n";
    t += "\n";
    lines.forEach(function (l) { t += "- " + lineText(l) + "\n"; });
    return t;
  }

  // Exposed for testing.
  window.GangBox = { buildCsv: buildCsv, csvCell: csvCell, fileName: fileName, plainText: plainText };

  // ---------- Sending ----------
  els.sendBtn.addEventListener("click", function () {
    var job = state.job;
    var lines = orderLines();
    var warn = "";
    if (!job.jobName.trim()) warn += '<p class="warn">⚠ No job name. The warehouse won\'t know where this goes.</p>';
    lines.forEach(function (l) {
      if (l.needsOption && !l.option) warn += '<p class="warn">⚠ ' + esc(l.tool) + ": pick the " + esc(l.optionLabel.toLowerCase()) + ".</p>";
    });
    if (!job.requestedBy.trim()) warn += '<p class="warn">⚠ Add your name so the warehouse can call you.</p>';
    var list = "<ul>" + lines.map(function (l) {
      return "<li><b>" + l.qty + " ×</b> " + esc(l.tool) + (l.option ? ", <b>" + esc(l.option) + "</b> " + esc(l.optionLabel.toLowerCase()) : "") + (l.note ? " <span class='small'>(" + esc(l.note) + ")</span>" : "") + "</li>";
    }).join("") + "</ul>";
    var head = job.jobName.trim() ? "<p><b>Job:</b> " + esc(job.jobName) + (job.dateNeeded ? "<br><b>Needed:</b> " + esc(prettyDate(job.dateNeeded)) : "") + "</p>" : "";

    var actions = [];
    var note;
    if (directEmailOn()) {
      actions.push({ label: "Send it", style: "btn-primary", onClick: function () { sendDirect(job, lines); } });
      note = "Tap Send it and the list goes straight to the office by email, with a spreadsheet attached.";
    } else if (canShareFile()) {
      actions.push({ label: "Send it", style: "btn-primary", onClick: function () { shareFile(job, lines); } });
      actions.push({ label: "Email the list as text instead", style: "btn-outline", onClick: function () { emailFallback(job, lines); } });
      note = "Tap Send it, then pick Mail, Messages, or whatever you use to reach the warehouse. The list goes as a spreadsheet file.";
    } else {
      actions.push({ label: "Send by email", style: "btn-primary", onClick: function () { emailFallback(job, lines); } });
      note = "This opens an email with the list. A spreadsheet copy is also saved on this device.";
    }
    actions.push({ label: "Go back and change it", style: "btn-plain", onClick: closeModal });

    openModal("Check your list (" + lines.length + ")", warn + head + list + '<p class="small">' + note + "</p>", actions);
  });

  // ---------- Sending straight to the office inbox (FormSubmit.co) ----------
  function directEmailOn() { return !!(CONFIG.sendTo || "").trim(); }

  function sendDirect(job, lines) {
    openModal("Sending...", "<p>Sending your list. Keep this page open for a moment.</p>", []);
    var form = new FormData();
    form.append("_subject", subjectLine(job));
    form.append("_template", "table");
    form.append("_captcha", "false");
    form.append("Job", job.jobName.trim() || "(no job name)");
    form.append("Requested by", job.requestedBy.trim() || "(no name)");
    form.append("Date needed", prettyDate(job.dateNeeded) || "(not given)");
    form.append("Tools", lines.map(function (l) {
      return lineText(l);
    }).join("\n"));
    form.append("Spreadsheet", makeFile(job, lines));

    var controller = window.AbortController ? new AbortController() : null;
    var timer = setTimeout(function () { if (controller) controller.abort(); }, 30000);
    fetch("https://formsubmit.co/ajax/" + encodeURIComponent(CONFIG.sendTo.trim()), {
      method: "POST",
      headers: { Accept: "application/json" },
      body: form,
      signal: controller ? controller.signal : undefined,
    }).then(function (res) {
      return res.json().catch(function () { return {}; }).then(function (data) {
        if (!res.ok || String(data.success) !== "true") throw new Error(data.message || ("HTTP " + res.status));
      });
    }).then(function () {
      clearTimeout(timer);
      openModal("Sent! ✓", "<p>The office has your list. You can clear it now and start fresh for the next job.</p>", [
        { label: "Clear the list", style: "btn-primary", onClick: function () { clearChecks(); closeModal(); toast("List cleared. Ready for the next job."); } },
        { label: "Keep my list", style: "btn-plain", onClick: closeModal },
      ]);
    }).catch(function () {
      clearTimeout(timer);
      var actions = [{ label: "Try again", style: "btn-primary", onClick: function () { sendDirect(job, lines); } }];
      if (canShareFile()) actions.push({ label: "Send it another way", style: "btn-outline", onClick: function () { shareFile(job, lines); } });
      actions.push({ label: "Email it from my phone instead", style: "btn-outline", onClick: function () { emailFallback(job, lines); } });
      actions.push({ label: "Close", style: "btn-plain", onClick: closeModal });
      openModal("Didn't send", "<p class=\"warn\">The list didn't go through. You might not have signal right now.</p><p>Your list is saved. Try again in a minute, or send it another way.</p>", actions);
    });
  }

  function makeFile(job, lines) {
    return new File([buildCsv(job, lines)], fileName(job), { type: "text/csv" });
  }

  function canShareFile() {
    try {
      return !!(navigator.canShare && navigator.share &&
        navigator.canShare({ files: [new File(["x"], "test.csv", { type: "text/csv" })] }));
    } catch (e) { return false; }
  }

  function shareFile(job, lines) {
    var file = makeFile(job, lines);
    var text = plainText(job, lines);
    if (warehouseEmail()) text = "To: " + warehouseEmail() + "\n\n" + text;
    navigator.share({ files: [file], title: subjectLine(job), text: text })
      .then(function () { afterSend(); })
      .catch(function (err) {
        if (err && err.name === "AbortError") return; // they closed the share menu
        emailFallback(job, lines);
      });
  }

  function emailFallback(job, lines) {
    // Save a spreadsheet copy, then open an email with the list in it.
    var url = URL.createObjectURL(makeFile(job, lines));
    var a = document.createElement("a");
    a.href = url; a.download = fileName(job);
    document.body.appendChild(a); a.click(); a.remove();
    setTimeout(function () { URL.revokeObjectURL(url); }, 10000);
    location.href = "mailto:" + encodeURIComponent(warehouseEmail()) +
      "?subject=" + encodeURIComponent(subjectLine(job)) +
      "&body=" + encodeURIComponent(plainText(job, lines));
    setTimeout(afterSend, 800);
  }

  function afterSend() {
    openModal("Did it send?", "<p>If the warehouse got it, you can clear the checks and start fresh for the next job.</p>", [
      { label: "Yes, clear the list", style: "btn-primary", onClick: function () { clearChecks(); closeModal(); toast("List cleared. Ready for the next job."); } },
      { label: "Keep my list", style: "btn-plain", onClick: closeModal },
    ]);
  }

  function clearChecks() {
    state.picks = {};
    state.job.jobName = ""; state.job.dateNeeded = "";
    els.jobName.value = ""; els.dateNeeded.value = "";
    save(); render();
  }

  // ---------- Menu ----------
  els.menuBtn.addEventListener("click", function () {
    openModal("Options", "", [
      { label: "Start a new list (clear all checks)", style: "btn-outline", onClick: function () {
        confirmBox("Clear all checks?", "This unchecks every tool and clears the job name and date. Your name stays.", "Clear it", clearChecks, true);
      } },
      { label: "How to put this on my home screen", style: "btn-outline", onClick: function () { closeModal(); showInstallTip(true); } },
      { label: "Close", style: "btn-plain", onClick: closeModal },
    ]);
  });

  // ---------- Add to home screen ----------
  var deferredPrompt = null;
  var isStandalone = window.matchMedia("(display-mode: standalone)").matches || window.navigator.standalone === true;
  var isIos = /iphone|ipad|ipod/i.test(navigator.userAgent) || (navigator.platform === "MacIntel" && navigator.maxTouchPoints > 1);

  window.addEventListener("beforeinstallprompt", function (e) {
    e.preventDefault();
    deferredPrompt = e;
    if (!state.tipDismissed && !isStandalone) showInstallTip(false);
  });

  function showInstallTip(force) {
    if (isStandalone && !force) return;
    if (isStandalone) {
      els.installTipText.innerHTML = "<b>You're all set.</b> This is already on your home screen.";
      els.installBtn.hidden = true;
    } else if (deferredPrompt) {
      els.installTipText.innerHTML = "<b>Tip:</b> Put this on your home screen so it opens like an app.";
      els.installBtn.hidden = false;
    } else if (isIos) {
      els.installTipText.innerHTML = "<b>Tip: put this on your home screen.</b><br>In Safari, tap the <b>Share</b> button (the square with the arrow ⬆️), then tap <b>Add to Home Screen</b>.";
      els.installBtn.hidden = true;
    } else {
      els.installTipText.innerHTML = "<b>Tip: put this on your home screen.</b><br>Tap your browser's menu (⋮ at the top), then tap <b>Add to Home screen</b> or <b>Install app</b>.";
      els.installBtn.hidden = true;
    }
    els.installTip.hidden = false;
    if (force) window.scrollTo({ top: 0, behavior: "smooth" });
  }

  els.installBtn.addEventListener("click", function () {
    if (!deferredPrompt) return;
    deferredPrompt.prompt();
    deferredPrompt.userChoice.finally(function () {
      deferredPrompt = null;
      els.installTip.hidden = true;
    });
  });
  els.installDismiss.addEventListener("click", function () {
    els.installTip.hidden = true;
    state.tipDismissed = true; save();
  });
  if (!state.tipDismissed && !isStandalone && isIos) showInstallTip(false);

  // ---------- Offline support ----------
  if ("serviceWorker" in navigator && location.protocol !== "file:") {
    window.addEventListener("load", function () { navigator.serviceWorker.register("sw.js").catch(function () {}); });
  }

  // Drop checks for tools that are no longer on the list (e.g. after a list update).
  (function prunePicks() {
    var ids = {};
    allTools().forEach(function (t) { ids[t.id] = true; });
    Object.keys(state.picks).forEach(function (id) { if (!ids[id]) delete state.picks[id]; });
    save();
  })();

  render();
})();
