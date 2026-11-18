# Product Specification — FocusFit

## 1. Vision

"Something to get my focus in." FocusFit is not a strict diet or fitness app. It gives the user
**one clear daily list** of small, achievable actions that add up to weight loss and a better
lifestyle, and it nudges at the right moments without nagging.

### Principles
1. **Relaxed, not restrictive.** No mandatory calorie logging. Check-ins are yes/no taps.
2. **Built around the user's real week.** Sports the user already does are the backbone of the plan.
3. **Progressive.** Goals start easy and grow (e.g. steps 6k → 10k) so streaks are achievable.
4. **Private and free.** Everything stays on the phone. No account, no backend, no ads.
5. **≤ 30 seconds a day** of interaction for the core loop.

## 2. Reference user (example profile)

| Field | Value |
|---|---|
| Sex / age | Male, 33 |
| Height / weight | 1.74 m / 84 kg (BMI 27.7) |
| Goal | Lose weight (target ~78 kg, then reassess) |
| Training | 3×/week: 2× Krav Maga, 1× CrossFit |
| Lifestyle goals | Read more, walk more steps, relaxed but decent diet |

Derived targets (see [HEALTH_ENGINE.md](HEALTH_ENGINE.md)): ~2,050 kcal/day average, ~150 g protein,
~3 L water, steps 6k → 10k over 6 weeks, 15 min reading/day, pace ~0.4–0.5 kg/week.

## 3. Features

### F1 — Onboarding & Profile
- Steps: basics (sex, age, height, weight) → goal (lose / maintain / gain, target weight, pace) →
  activities (sport, days of week, time) + work pattern (office / remote / off per weekday) → lifestyle goals (steps, reading, sleep, water) →
  food preferences (likes, dislikes, allergies, diet style) → notification preferences & quiet hours.
- Everything editable later in **Profile**. Changing any field regenerates the plans.

### F2 — Today (home screen)
- Header: date, weekly score ring, current streak.
- Auto-generated to-do cards for the day, e.g.:
  - 🥋 Krav Maga 19:00 (training day) / 🚶 30-min walk (rest day)
  - 👟 Steps 4,210 / 7,000 (live from Health Connect, progress bar)
  - 📖 Read 15 min (tap to start a timer, or tick)
  - 🍗 Protein at each meal (3 taps: breakfast / lunch / dinner)
  - 💧 Water 0 / 3 L (+250 ml button)
  - 😴 In bed by 23:30
- Custom to-dos can be added (one-off or recurring).
- Suggestion banner (see F7) at the top when relevant.

### F3 — Training Plan
- **Fully user-defined.** The user picks each sport, which weekdays, start time and duration.
  The app only fills the *remaining* days with light suggestions, and every slot can be edited,
  swapped, or set to "rest". Templates can be saved (e.g. "Normal week", "Holiday week").
- **Day context:** each weekday is also tagged **Office / Remote / Off**. This changes the
  suggestions (e.g. office day → walk part of the commute or a lunch walk, pack lunch;
  remote day → short walk breaks every 2 h).
- Session types: **Sport** (Krav Maga, CrossFit, …), **Active recovery** (walk, mobility),
  **Optional home session** (15-min core/posture), **Rest**.
- Example week (reference user: trains Mon, Tue, Fri; Wednesday is an office day):

| Mon | Tue | Wed (office) | Thu | Fri | Sat | Sun |
|---|---|---|---|---|---|---|
| 🥋 Krav Maga | 🏋️ CrossFit | Lunch walk 20' + mobility 10' | Core A 15' + walk 30' | 🥋 Krav Maga | Core B 15' + long walk 60' | Rest + weekly review |

  Mon + Tue are back-to-back sessions, so Wednesday stays light (recovery + steps only).
- **Guided core & mobility plans** (see [WORKOUT_LIBRARY.md](WORKOUT_LIBRARY.md)):
  Core A (stability), Core B (combat-focused power/rotation), Mobility, and a 5-min Quick Core.
  Session player with exercise cues, work/rest timer, vibration/sound cues, swap to an
  easier/harder variant, and RPE logging. A 6-week progression unlocks harder levels
  automatically when sessions feel easy (RPE ≤ 7).

- Missed session → offers to move it or replace it with a walk.
- Each session can be marked done with an RPE (effort 1–10) to inform suggestions.

### F4 — Nutrition Plan (relaxed)
- **Plate method** guide (½ vegetables, ¼ protein, ¼ carbs, a thumb of fat).
- **Daily targets** shown as ranges, not hard numbers (kcal, protein, water).
- **Training-day vs rest-day** guidance (a bit more carbs around training).
- **80/20 rule:** 2–3 "free meals" per week, the user picks when; shown as tokens.
- **Meal idea pool:** simple breakfasts, lunches, dinners and snacks filtered by preferences;
  "shuffle" button for new ideas. Stored locally as a seed database.
- **Daily check-in:** protein each meal ✔, veg in 2 meals ✔, no sugary drinks ✔, water ✔.
- Optional: weekly shopping list generated from chosen meal ideas.

### F5 — Habits: Steps, Reading, Water, Sleep
- **Steps:** read from Android **Health Connect** (free, on-device). Weekly goal auto-increments
  by +500–1,000 if the previous week's average was hit on ≥ 5 days.
- **Reading:** book shelf (title, total pages), timer or pages logged; stats per week/month.
- **Water & sleep:** simple ticks/counters.

### F6 — Progress
- Weekly weight log (same day/time recommended), 7-day moving average trend line.
- Optional waist measurement every 2 weeks.
- Weekly score = % of to-dos completed; streaks per habit; monthly calendar heatmap.

### F7 — Smart Suggestions (rule-based, offline)
A small rules engine evaluates context (time, day plan, progress, history) and emits at most
**1 banner + 2 notifications per day** (excluding scheduled reminders).

| Trigger | Suggestion |
|---|---|
| 18:00 and steps < 50 % of goal | "You're at 3.9k. A 25-min walk gets you to 7k." |
| Training day, 2–3 h before session | "Krav Maga at 19:00 — have a carb + protein snack around 17:00." |
| After training logged | "Good session. Prioritise protein at dinner (~40 g)." |
| 21:30 and reading not done | "10 pages before bed?" |
| 3 days missing protein check-ins | Shows 3 high-protein easy snack ideas. |
| Weight trend flat for 3 weeks | Suggest −150 kcal or +1,000 steps/day. |
| Weight dropping > 1 % / week for 2 weeks | Suggest eating a bit more to protect muscle. |
| Missed training session | Offer reschedule or replacement walk. |
| Office day, morning | "Office day: get off one stop early or take a 20-min lunch walk." |
| Two training days in a row | Next day suggests mobility + extra sleep instead of hard work. |
| Sunday 19:00 | Weekly review: wins, misses, next week's adjustments. |

### F8 — Notifications
- Scheduled reminders: training (−2 h and −30 min), water (every 2–3 h, daytime), reading
  (evening), bedtime, weekly weigh-in, weekly review.
- Contextual nudges from F7.
- Per-category on/off, quiet hours (default 23:00–07:30), max-per-day cap.
- Actionable notifications: "Done", "Snooze 1 h", "+250 ml".

## 4. Screens

1. Onboarding (6 steps)
2. **Today** (home)
3. **Plan** — tabs: Training · Nutrition
4. **Habits** — Steps · Reading · Water · Sleep
5. **Progress** — weight trend, streaks, heatmap
6. **Profile & Settings** — profile, preferences, notifications, data export/import

Bottom navigation: Today · Plan · Habits · Progress · Profile.

## 5. Out of scope (MVP)
- Cloud sync, accounts, social features.
- Barcode scanning / detailed calorie logging.
- Wearable-specific integrations beyond Health Connect.
- AI/LLM coach (possible later, see roadmap — would need a free/local option to keep cost at €0).

## 6. Non-functional requirements
- Min Android 9 (API 28); target latest stable API. Health Connect needs Android 9+.
- Works fully offline. All data on device (Room DB); JSON export/import for backup.
- Battery: no constant background service; WorkManager + exact alarms only for scheduled reminders.
- Accessibility: dynamic font sizes, TalkBack labels, dark mode.
