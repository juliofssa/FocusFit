# FocusFit — Healthy Lifestyle To-Do App (Android)

A personal, offline-first Android app that turns a healthy lifestyle into one short daily to-do list —
training, steps, reading, nutrition check-ins, water and sleep — with reminders, smart suggestions,
a training plan built around your own sports and a relaxed nutrition plan.

**Cost: €0.** Free/open-source libraries only, no backend, no account, no ads, no paid APIs,
no Play Store fee (install the APK directly on your phone). Internet is only used to stream exercise
videos and download content-pack updates — nothing about you is ever uploaded.

## Features

| Tab | What it does |
|---|---|
| **Today** | Auto-generated to-dos for the day (your sport/core/walk, steps, reading, protein, water, sleep + your own to-dos), live steps from Health Connect, water counter, streak & weekly score, smart suggestion banner, ▶ start guided workouts |
| **Plan** | Your week (sports on your days, office/remote days, core/mobility/walks on free days) with levels; nutrition targets, meal ideas (shuffle / hide, office-day lunches), 3 free meals a week |
| **Habits** | Steps (Health Connect, 7-day chart, goal +1,000/week when hit 5+ days), reading timer, books & stats |
| **Progress** | Weight log + 7-day trend chart, pace & weeks to goal, consistency heatmap, weekly review (wins + focus) |
| **Profile** | Edit profile/sports/week, notification settings, backup & restore |

Also: 🎬 demo videos for every exercise (in-app YouTube player, or your own video per exercise),
exercises & meals that update online from [`content-pack/`](content-pack/README.md) (weekly, Wi-Fi, verified),
diet filters & custom targets, one-week plan changes, guided core & mobility sessions (timer, easier/harder swaps, effort rating, 3-level progression),
reminders with Done / +250 ml / Snooze buttons, quiet hours, hourly smart suggestions (max 2/day),
home-screen widget, dark mode & Material You colours.

## Install on your phone
See [docs/SETUP.md](docs/SETUP.md). Short version: build in Android Studio (or
`./gradlew :app:assembleRelease`) and install `app/build/outputs/apk/release/app-release.apk`.

## Documentation

| Doc | What's inside |
|---|---|
| [docs/PRODUCT_SPEC.md](docs/PRODUCT_SPEC.md) | Goals, features, screens, notifications, suggestion rules |
| [docs/HEALTH_ENGINE.md](docs/HEALTH_ENGINE.md) | Formulas for calories/macros/steps/reading + worked example profile |
| [docs/WORKOUT_LIBRARY.md](docs/WORKOUT_LIBRARY.md) | Core & mobility routines, progression plan |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Tech stack, modules, data model, background work |
| [docs/ROADMAP.md](docs/ROADMAP.md) | Milestones M0–M8 (all done) and later ideas |
| [docs/SETUP.md](docs/SETUP.md) | Toolchain, building, installing, Health Connect |
| [docs/visual-plan.html](docs/visual-plan.html) | Original visual plan: mockups, flows, architecture |

## Project layout
```
app/src/main/java/com/focusfit/app/
  domain/         pure Kotlin rules (targets, week planner, daily tasks, workouts, reminders,
                  suggestions, habits, progress) — unit-tested in app/src/test
  data/           Room DB (v7), DataStore settings, Health Connect, backup, repositories
  notifications/  channels, AlarmManager scheduler, receivers
  ui/             Compose screens per tab + onboarding, guided session, settings
  widget/         Glance home-screen widget
app/src/main/assets/  workouts.json, meals.json (editable content)
```

> Not medical advice. Targets are general estimates; check with a doctor or nutritionist if you
> have health conditions.
