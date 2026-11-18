# Gang Box (Android app)

A simple Android app for jobsite crews to build a gang box tool list and send it to the warehouse.

## What it does

1. **Job info** at the top: job name/address, who's asking, date needed, and (optionally) the warehouse email.
2. **Tool list** grouped by trade (Power Tools, Plumbing, Electrical, Hand Tools, Safety, and more). Tap a tool to check it, use **− / +** to set how many, and add a short note if needed (size, brand, etc.).
3. **Search** to find a tool fast. **Add tool** puts anything that's missing on the list (it stays saved on that phone). Tools you added have a trash can icon to remove them.
4. **Send to warehouse** shows a quick review, then opens the phone's normal share menu (email, text, Teams, Google Drive...). A spreadsheet file is attached, and the email body also lists the tools in plain text.

Everything is saved on the phone, so closing the app doesn't lose the list. Use the **⋮** menu to uncheck everything for the next job.

## The file the warehouse gets

A `.csv` file named like `GangBox_Smith_Residence_2026-11-20.csv`. It opens directly in Excel with these columns:

| Job | Requested By | Date Needed | Category | Tool | Qty | Notes |
|-----|--------------|-------------|----------|------|-----|-------|

## Getting the app onto a phone

GitHub builds the app automatically whenever the code changes (see the **Actions** tab, "Gang Box Android app").

- **From the main branch:** open the repo's **Releases** page on the phone, download `GangBox.apk`, and tap it to install.
- **From any other build:** open the run in the **Actions** tab, download **GangBox-apk** (a zip), unzip it, and copy `GangBox.apk` to the phone.

The first time, Android will ask you to allow installing apps from your browser or file manager. That's normal for apps that aren't from the Play Store.

## Changing the starter tool list

The built-in list lives in `app/src/main/java/com/gangbox/app/data/DefaultTools.kt`. Edit the names under each category, and the next build will include them. On phones that already have the app, use **⋮ > Reset tool list** to pick up the new list (tools the crew added are kept).

## For developers

- Kotlin + Jetpack Compose, min Android 8.0 (API 26).
- Build: `./gradlew assembleRelease` (output in `app/build/outputs/apk/release/`). The release build is signed with the debug key so it installs without setup; use a real signing key before publishing to the Play Store.
- Tests: `./gradlew testDebugUnitTest` (covers the CSV export).
