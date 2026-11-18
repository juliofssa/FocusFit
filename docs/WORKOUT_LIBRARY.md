# Workout Library — Core & Mobility Plans

Built-in, no-equipment home routines that fill the non-sport days. They are designed to
**support** the user's sports: a strong, stable core for Krav Maga (bracing for strikes,
rotation for punches/kicks, ground work) and CrossFit (bracing under load).

Every routine runs in the app as a **guided session**: exercise card with cues, work/rest
timer, voice/vibration cues, "swap exercise" and "done" with effort rating (RPE 1–10).

## 1. Where it goes in the week (reference user)

| Mon | Tue | Wed (office) | Thu | Fri | Sat | Sun |
|---|---|---|---|---|---|---|
| 🥋 Krav Maga | 🏋️ CrossFit | 🧘 Mobility 10' (evening) | 🎯 **Core A** 15' + walk 30' | 🥋 Krav Maga | 🎯 **Core B** 15' + long walk | Rest |

- Core is never placed the day **before** a sport session if it can be avoided, and never on two
  consecutive days. The user can move or turn off any slot.

## 2. Routines

### 🎯 Core A — Stability & bracing (~15 min)
Straight sets, 30 s rest between sets, 60 s between exercises.

| # | Exercise | Sets × reps/time | Key cues |
|---|---|---|---|
| 1 | Dead bug | 3 × 8 / side | Low back pressed into floor, slow exhale |
| 2 | Front plank | 3 × 30 s | Squeeze glutes, ribs down, straight line |
| 3 | Side plank | 2 × 20 s / side | Hips high, elbow under shoulder |
| 4 | Bird dog | 2 × 8 / side | Don't rotate hips, 2 s hold at top |
| 5 | Glute bridge | 3 × 12 | Drive through heels, pause at top |

### 🎯 Core B — Power & rotation, combat-focused (~15 min)
Circuit: do 1→5 back to back, 60 s rest, **3 rounds**.

| # | Exercise | Work | Key cues |
|---|---|---|---|
| 1 | Mountain climbers | 30 s | Hips level, fast knees |
| 2 | Reverse crunch | 10 reps | Curl pelvis up, control the way down |
| 3 | Russian twist | 12 / side | Rotate from the chest, not just the arms |
| 4 | Hollow hold | 20 s | Low back down; bend knees to scale |
| 5 | Bear crawl hold | 20 s | Knees 2 cm off the floor, flat back |

### 🧘 Mobility — Recovery (~10 min)
Do once, 45–60 s each (per side where relevant). Good after back-to-back training days
or on office days to undo sitting.

1. Cat–cow (60 s)
2. World's greatest stretch (45 s / side)
3. 90/90 hip switches (60 s)
4. Thoracic rotations on all fours (45 s / side)
5. Hip-flexor lunge stretch (45 s / side)
6. Deep squat hold (60 s)
7. Child's pose with breathing (60 s)

### ⚡ Quick Core — "no time" version (5 min)
Plank 40 s · dead bug 10/side · side plank 20 s/side · hollow hold 20 s · rest 20 s between.
Offered automatically when the user skips a core slot or has a busy office day.

## 3. 6-week progression

| Weeks | Core A | Core B | Rule |
|---|---|---|---|
| 1–2 | As written | 3 rounds as written | Learn form |
| 3–4 | +10 s on holds, +2 reps | Work 40 s / +2 reps | Unlocks after 4 sessions with RPE ≤ 7 |
| 5–6 | Harder variants: plank shoulder taps, side plank hip dips, dead bug with straight legs | 4 rounds; hollow rocks instead of hollow hold | Unlocks after 4 more sessions with RPE ≤ 7 |
| 7+ | New cycle with new exercises (e.g. Pallof press with band, L-sit tuck, ab wheel if available) | | User picks equipment in profile |

- If RPE ≥ 9 twice in a row, the app suggests staying at the current level or scaling down.
- Every exercise has an easier and a harder variant, so "swap" keeps the training effect.

## 4. Exercise data (seed JSON, stored locally)

Each exercise has: `id`, `name`, `category` (core/mobility), `focus` (anti-extension,
anti-rotation, rotation, hip, thoracic…), `defaultSets`, `reps` or `seconds`, `cues[]`,
`easierId`, `harderId`, `equipment` (none/band/mat), `illustration` (simple vector, offline).

> If something hurts (sharp pain, especially lower back), stop and swap for the easier
> variant. Not medical advice.
