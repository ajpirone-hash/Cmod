// Keeps the app working with no signal. Bump VERSION when files change.
var VERSION = "gangbox-v10";
var FILES = [
  "./", "index.html", "styles.css?v=10", "app.js?v=10", "tools.js?v=10", "config.js?v=10", "manifest.webmanifest",
  "icons/icon-192.png", "icons/icon-512.png", "icons/apple-touch-icon.png",
];

self.addEventListener("install", function (e) {
  e.waitUntil(caches.open(VERSION).then(function (c) { return c.addAll(FILES); }).then(function () { return self.skipWaiting(); }));
});

self.addEventListener("activate", function (e) {
  e.waitUntil(caches.keys().then(function (keys) {
    return Promise.all(keys.filter(function (k) { return k !== VERSION; }).map(function (k) { return caches.delete(k); }));
  }).then(function () { return self.clients.claim(); }));
});

// Try the network first so crews get updates; fall back to the saved copy offline.
self.addEventListener("fetch", function (e) {
  if (e.request.method !== "GET" || new URL(e.request.url).origin !== location.origin) return;
  e.respondWith(
    // "no-cache" makes the phone check GitHub for a newer copy every time
    // instead of reusing its own 10-minute browser cache.
    fetch(e.request.url, { cache: "no-cache", credentials: "same-origin" }).then(function (res) {
      var copy = res.clone();
      caches.open(VERSION).then(function (c) { c.put(e.request, copy); });
      return res;
    }).catch(function () {
      return caches.match(e.request, { ignoreSearch: true }).then(function (r) { return r || caches.match("index.html"); });
    })
  );
});
