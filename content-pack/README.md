# FocusFit content pack

The exercises (with demo videos), routines and meal ideas that FocusFit downloads. Phones check this folder
**weekly on Wi-Fi** (or via *Profile → Exercises, videos & meals → Check now*) and install a new version only if:

1. `manifest.json` has a **higher `version`** than what the phone has,
2. the SHA-256 **checksums** in `manifest.json` match the downloaded files, and
3. the content passes the **same rules as the app** (all routines present, referenced exercises exist,
   valid YouTube ids, every meal type has ideas…).

Otherwise the phone keeps its current content. Nothing is uploaded; the app only downloads these 3 files.

## Make it reachable (one-time)
The app reads `https://raw.githubusercontent.com/juliofssa/FocusFit/main/content-pack/`. GitHub only serves
that URL for **public** repos. Either:
- make the `FocusFit` repo public, **or**
- create a small public repo (e.g. `FocusFit-content`) with this folder's files, and put its raw URL in the app:
  *Profile → Exercises, videos & meals → Source*, e.g. `https://raw.githubusercontent.com/juliofssa/FocusFit-content/main/`.

## Publish an update
1. Edit `workouts.json` and/or `meals.json` (see formats below).
2. Run `python content-pack/tools/build_manifest.py --bump`. It validates, refreshes the checksums and increases the version.
   Optionally write a short `"notes"` in `manifest.json` (shown in the app after updating).
3. Commit and push. The **Content pack check** GitHub Action runs the same validation.

## Formats
**Exercise** (in `workouts.json` → `exercises`):
```json
{ "id": "dead_bug", "youtubeId": "GbSC02oU3To", "videoStartSec": 0, "name": "Dead bug", "category": "core",
  "focus": "anti-extension", "reps": 8, "perSide": true, "cues": ["Low back pressed into the floor"],
  "easierId": "dead_bug_bent", "harderId": "dead_bug_straight", "equipment": "none" }
```
- `reps` **or** `seconds` is required. `youtubeId` is the 11 characters after `watch?v=` / `youtu.be/` / `shorts/`.
- Pick videos that allow embedding: on YouTube, *Share → Embed* must be available.

**Routine** (in `workouts.json` → `routines`): `core_a`, `core_b`, `mobility` and `quick_core` must exist.
`format` is `SETS` (all sets of one exercise, then the next) or `CIRCUIT` (rounds through all exercises).

**Meal** (in `meals.json` → `meals`):
```json
{ "id": "tofu_stirfry", "name": "Crispy tofu & veg stir-fry with rice", "type": "DINNER", "proteinG": 30,
  "tags": ["quick", "vegan"], "contains": ["SOY"] }
```
- `type`: `BREAKFAST`, `LUNCH`, `DINNER`, `PRE_TRAINING`, `SNACK`.
- `contains` drives diet filters: `MEAT`, `PORK`, `FISH`, `EGGS`, `DAIRY`, `GLUTEN`, `NUTS`, `SOY`.
  Keep at least one vegan idea per type. `"office"` in `tags` puts a lunch first on office days.
