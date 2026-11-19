# Gang Box web app (the crew link)

The same gang box checklist as the Android app, but as a web page. Crews just tap a link: nothing to install, and it works on iPhone and Android.

**Crew link:** https://ajpirone-hash.github.io/Cmod/

## What the crew does

1. Tap the link (from your email or text).
2. Fill in the job name, their name, and the date needed.
3. Tap a trade (Plumbing, Electrical...) to open it, then tap each tool they need. Use **−** and **+** for how many.
4. Tap **Send to warehouse**, check the list, and tap **Send it**. The list is emailed straight to the office inbox set in `config.js` (`sendTo`), with a spreadsheet that opens in Excel attached. No mail app needed. If there's no signal, they can try again or send it from their phone's share menu.
5. After sending, tap **Yes, clear the list** to start fresh next time.

The list is saved on their phone, so closing the page doesn't lose anything. After the first visit it also works with no signal.

## Putting it on the home screen

- **iPhone:** open the link in Safari, tap the Share button (square with an arrow), then **Add to Home Screen**.
- **Android:** open the link in Chrome, tap **Add to Home Screen** on the tip at the top (or menu ⋮ > Add to Home screen).

## Settings

- **Where lists are emailed:** `sendTo` in `config.js`. Sending uses the free FormSubmit.co service. The very first list triggers a one-time "confirm your email" message to that inbox; click it once. FormSubmit then sends a random code you can put in `sendTo` instead of the address, so the address isn't visible in the page. Leave `sendTo` blank to go back to the share-menu way.

- **Warehouse email:** put it in `config.js` (`warehouseEmail`), or add it to the link you send, like
  `https://ajpirone-hash.github.io/Cmod/?to=warehouse@yourcompany.com`
- **Company name** at the top: `company` in `config.js`.
- **Tool list:** edit `tools.js`. After changing any file, also bump `VERSION` in `sw.js` so phones pick up the change.

## How it's hosted

GitHub Pages, free. `.github/workflows/gangbox-web.yml` publishes this folder whenever it changes on the repo's default branch. The repo must be public and **Settings > Pages > Source** must be set to **GitHub Actions**.
