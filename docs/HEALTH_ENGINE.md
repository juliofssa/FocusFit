# Health Engine — Formulas & Rules

All targets are computed on-device from the profile. Values are **estimates** and are shown as
ranges. Not medical advice.

## 1. Body metrics

- **BMI** = weight / height²
- **Healthy weight range** = 18.5 × h² … 24.9 × h²

## 2. Energy

- **BMR (Mifflin–St Jeor)**
  - Male: `10·kg + 6.25·cm − 5·age + 5`
  - Female: `10·kg + 6.25·cm − 5·age − 161`
- **Activity factor** from weekly training count + daily steps:

| Profile | Factor |
|---|---|
| Desk job, < 5k steps, 0–1 sessions | 1.2 |
| 1–3 sessions **or** 5–8k steps | 1.375 |
| 3–5 sessions **and** ≥ 7k steps | 1.465 |
| 6+ sessions | 1.55 |

- **TDEE** = BMR × factor
- **Deficit** by pace: relaxed 0.25 kg/wk → −275 kcal; standard 0.5 kg/wk → −450 kcal (capped so
  intake never goes below BMR × 1.1).
- **Training-day / rest-day split:** training days +150 kcal, rest days −100 kcal (weekly average
  unchanged).

## 3. Macros

- **Protein:** 1.6–2.0 g/kg (default 1.8 g/kg; use goal weight if BMI > 30).
- **Fat:** 0.8 g/kg (min 20 % of kcal).
- **Carbs:** remaining kcal ÷ 4.
- **Water:** 35 ml/kg, + 500 ml on training days.
- Shown to the user as "palms of protein" equivalents too (1 palm ≈ 25–30 g protein).

## 4. Habits

- **Steps:** start at `max(6,000, current 7-day average + 1,000)`, +1,000 per week when hit on
  ≥ 5 of 7 days, cap at 10,000 (user can raise).
- **Reading:** start 15 min/day (≈ 10 pages); +5 min after 2 weeks with ≥ 80 % completion, cap 30.
- **Sleep:** target 7–8 h; bedtime = wake time − 7.5 h.

## 5. Progress & adaptation (weekly, Sunday)

- Weight trend = 7-day moving average of logged weights (or weekly single weigh-in).
- Rate = Δ trend over 2 weeks.
  - Rate < 0.2 kg/wk for 3 weeks → suggest −150 kcal **or** +1,000 steps.
  - Rate > 1 % bodyweight/wk for 2 weeks → suggest +150 kcal.
- Recalculate BMR/TDEE whenever weight changes by ≥ 2 kg.

## 6. Worked example — reference profile

Male, 33 y, 174 cm, 84 kg, 2× Krav Maga + 1× CrossFit per week, goal: lose weight (standard pace).

| Metric | Calculation | Result |
|---|---|---|
| BMI | 84 / 1.74² | **27.7** (overweight) |
| Healthy range | 18.5–24.9 × 3.03 | **56–75 kg** |
| BMR | 840 + 1,087.5 − 165 + 5 | **≈ 1,770 kcal** |
| TDEE | 1,770 × 1.375–1.465 | **≈ 2,430–2,590 kcal** (≈ 2,500) |
| Intake (avg) | 2,500 − 450 | **≈ 2,050 kcal/day** |
| Training days (3) | +150 | **≈ 2,200 kcal** |
| Rest days (4) | −100 | **≈ 1,950 kcal** |
| Protein | 1.8 × 84 | **≈ 150 g** (≈ 5 palms) |
| Fat | 0.8 × 84 | **≈ 67 g** |
| Carbs | (2,050 − 600 − 603) / 4 | **≈ 210 g** |
| Water | 35 × 84 (+0.5 L training) | **≈ 3.0 L** (3.5 L training days) |
| Steps | week 1 → week 5 | **6k → 7k → 8k → 9k → 10k** |
| Reading | | **15 min/day** |
| Expected pace | | **≈ 0.4–0.5 kg/week → ~78 kg in ~12–14 weeks** |

### Example day (training day, relaxed)
- **Breakfast:** Greek yogurt + oats + fruit (≈ 30 g protein)
- **Lunch:** chicken/fish + rice or potatoes + big salad (≈ 40 g)
- **Pre-training snack (17:00):** banana + protein shake or ham/cheese toast (≈ 25 g)
- **Dinner:** eggs/omelette or lean meat + vegetables + small carb (≈ 40 g)
- **Optional:** cottage cheese or skyr before bed (≈ 15 g)
