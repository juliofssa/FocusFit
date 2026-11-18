# Roadmap

Status: ✅ M0 · ✅ M1 (onboarding, profile/sports/work-week persisted in Room v2, validation) ·
✅ M2 (saved daily progress, water counter, custom to-dos, streak ≥ 80 %/day, weekly score — Room v3) ·
✅ M3 (guided core/mobility player with timer, swaps, RPE + 3-level progression; meal ideas with shuffle/hide,
office-day lunches, 3 free meals/week — Room v4). Week editing lives in Profile → Edit. Shopping list moved to "Later". ·
✅ M4 (6 reminder channels, AlarmManager scheduling with midnight rollover + boot/time-change reschedule,
Done / +250 ml / Snooze actions, skip-if-done, quiet hours, settings screen with preview & test button). ·
✅ M5 (Health Connect steps — read-only, live on Today, 7-day chart; weekly +1k step-goal progression;
reading tracker with timer, books, pages, stats & streak — Room v5; bedtime setting drives the sleep to-do). ·
✅ M6 (10 offline suggestion rules, Today banner with action + dismiss, hourly checks → max 2 notifications/day,
never duplicates a reminder, "Smart suggestions" toggle — Room v6). ·
✅ M7 (weight log + waist, 7-day trend chart with goal line, pace & weeks-to-goal, targets recalc at ±2 kg,
plateau / too-fast advice, 8-week consistency heatmap + best streak, weekly review, Monday weigh-in reminder — Room v7). ·
✅ M8 (JSON backup & restore via file picker, Glance home-screen widget, R8-shrunk release APK signed with the
local debug key). **All milestones done.**

## Upgrades

| Phase | Scope | Status |
|---|---|---|
| **A — For anyone** | Neutral onboarding (no pre-filled author profile) + Food step; diet styles (vegetarian/pescatarian/vegan) & exclusions (allergies) filtering meal ideas, vegan options for every meal type; target overrides (kcal, protein, water, fixed steps, reading) with suggested values shown; free meals per week; configurable reminder times (core/mobility, reading, last water, Sunday review); this/next-week view with one-off skip / move / add + undo & reset — Room v8 | ✅ |
| **B — Videos & online content** | Verified, embeddable YouTube demo for all 24 exercises; in-app player (android-youtube-player, no API key) in session overview, during sessions (pauses timer) and in a new Exercise library; personal video per exercise (paste any YouTube link); `content-pack/` (manifest + SHA-256 + validator tool + GitHub Action); weekly Wi-Fi update with checksum & validation, atomic install, built-in fallback; Profile card: version, Check now, Source URL, Use built-in | ✅ |
| **C — Quality of life** | Health Connect sleep & workout auto-ticks; boxing-style round timer for combat sports | Planned |

| Milestone | Scope | Done when |
|---|---|---|
| **M0 — Setup** | Android Studio, project skeleton, theme, navigation, Room, Hilt | App launches on phone with 5 empty tabs |
| **M1 — Profile & engine** | Onboarding, `HealthCalculator`, targets, unit tests | Reference profile yields ~2,050 kcal / ~150 g protein |
| **M2 — Today** | Task generation, check-offs, custom to-dos, water counter, streaks | Daily list works for a full week |
| **M3 — Plans** | Training week editor, core/mobility library + guided session player + progression, nutrition plan, plate guide, meal idea pool, free-meal tokens | Can run Core A with timer; Plan tab shows the week and meal ideas |
| **M4 — Notifications** | Channels, scheduled reminders, actions, quiet hours, boot re-schedule | Reminders fire on time after reboot |
| **M5 — Habits** | Health Connect steps, progressive goals, reading tracker | Live steps on Today; reading log |
| **M6 — Suggestions** | Rules engine + worker + banner, daily cap | Evening step nudge fires correctly |
| **M7 — Progress** | Weight log, trend chart, heatmap, weekly review | Sunday review with adjustments |
| **M8 — Polish** | Export/import, accessibility, dark mode, home-screen widget | Personal "v1.0" |

## Later ideas (keep €0)
- Home-screen widget with today's list (Glance).
- Wear OS tile.
- On-device AI coach (e.g. Gemini Nano where supported) — free, no server.
- Simple recipe builder & shopping list.
- Optional sync via user's own Google Drive file.
