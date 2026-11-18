#!/usr/bin/env python3
"""
FocusFit content pack tool (no dependencies, Python 3.8+).

  python content-pack/tools/build_manifest.py            # validate + refresh checksums (keeps version)
  python content-pack/tools/build_manifest.py --bump     # same, and increase the version (publish an update)
  python content-pack/tools/build_manifest.py --check    # CI: fail if invalid or manifest is out of date

The app only installs a pack whose version is higher than the one it has, whose SHA-256 checksums
match manifest.json, and that passes the same rules as ContentValidator.kt (mirrored below).
"""
import argparse
import datetime
import hashlib
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
FILES = ["workouts.json", "meals.json"]
SUPPORTED_FORMAT = 1
REQUIRED_ROUTINES = {"core_a", "core_b", "mobility", "quick_core"}
MEAL_TYPES = {"BREAKFAST", "LUNCH", "DINNER", "PRE_TRAINING", "SNACK"}
FOOD_TAGS = {"MEAT", "PORK", "FISH", "EGGS", "DAIRY", "GLUTEN", "NUTS", "SOY"}
YOUTUBE_ID = re.compile(r"^[A-Za-z0-9_-]{11}$")


def read_lf(name: str) -> bytes:
    """Bytes as GitHub serves them (LF line endings), whatever the local checkout uses."""
    return (ROOT / name).read_bytes().replace(b"\r\n", b"\n")


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def validate(workouts: dict, meals: dict) -> list:
    errors = []
    exercises = workouts.get("exercises", [])
    ids = [e.get("id") for e in exercises]
    errors += [f"Duplicate exercise id '{i}'" for i in sorted({i for i in ids if ids.count(i) > 1})]
    idset = set(ids)
    for e in exercises:
        eid = e.get("id")
        if e.get("reps") is None and e.get("seconds") is None:
            errors.append(f"Exercise '{eid}' needs reps or seconds")
        for key in ("easierId", "harderId"):
            if e.get(key) is not None and e[key] not in idset:
                errors.append(f"Exercise '{eid}': {key} '{e[key]}' doesn't exist")
        yt = e.get("youtubeId")
        if yt is not None and not YOUTUBE_ID.match(yt):
            errors.append(f"Exercise '{eid}': youtubeId '{yt}' isn't an 11-character YouTube id")
        if e.get("videoStartSec", 0) < 0:
            errors.append(f"Exercise '{eid}': videoStartSec can't be negative")
    routines = workouts.get("routines", [])
    rids = {r.get("id") for r in routines}
    errors += [f"Missing routine '{r}'" for r in sorted(REQUIRED_ROUTINES - rids)]
    for r in routines:
        if not r.get("steps"):
            errors.append(f"Routine '{r.get('id')}' has no steps")
        if r.get("format") not in ("SETS", "CIRCUIT"):
            errors.append(f"Routine '{r.get('id')}': unknown format '{r.get('format')}'")
        for s in r.get("steps", []):
            if s.get("exerciseId") not in idset:
                errors.append(f"Routine '{r.get('id')}' uses unknown exercise '{s.get('exerciseId')}'")
    meal_list = meals.get("meals", [])
    mids = [m.get("id") for m in meal_list]
    errors += [f"Duplicate meal id '{i}'" for i in sorted({i for i in mids if mids.count(i) > 1})]
    for m in meal_list:
        if m.get("type") not in MEAL_TYPES:
            errors.append(f"Meal '{m.get('id')}': unknown type '{m.get('type')}'")
        if m.get("proteinG", 0) < 0:
            errors.append(f"Meal '{m.get('id')}': protein can't be negative")
        unknown = set(m.get("contains", [])) - FOOD_TAGS
        if unknown:
            errors.append(f"Meal '{m.get('id')}': unknown 'contains' tags {sorted(unknown)}")
    errors += [f"No meal ideas of type {t}" for t in sorted(MEAL_TYPES - {m.get('type') for m in meal_list})]
    return errors


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--bump", action="store_true", help="increase the version number")
    ap.add_argument("--check", action="store_true", help="only verify (for CI)")
    args = ap.parse_args()

    data = {name: read_lf(name) for name in FILES}
    try:
        workouts = json.loads(data["workouts.json"])
        meals = json.loads(data["meals.json"])
    except json.JSONDecodeError as e:
        print(f"Invalid JSON: {e}")
        return 1

    errors = validate(workouts, meals)
    if errors:
        print("Content is invalid:\n  - " + "\n  - ".join(errors))
        return 1

    manifest_path = ROOT / "manifest.json"
    manifest = json.loads(manifest_path.read_text("utf-8")) if manifest_path.exists() else {"version": 1}
    checksums = {name: sha256(b) for name, b in data.items()}

    if args.check:
        if manifest.get("files") != checksums:
            print("manifest.json checksums are out of date. Run: python content-pack/tools/build_manifest.py --bump")
            return 1
        print(f"OK: content v{manifest['version']} is valid and checksums match.")
        return 0

    changed = manifest.get("files") != checksums
    if args.bump:
        manifest["version"] = int(manifest.get("version", 1)) + 1
        manifest["publishedAt"] = datetime.date.today().isoformat()
    manifest["format"] = SUPPORTED_FORMAT
    manifest["files"] = checksums
    manifest_path.write_bytes((json.dumps(manifest, indent=2, ensure_ascii=False) + "\n").encode("utf-8"))
    print(f"manifest.json written: version {manifest['version']}" + (" (files changed - remember --bump to publish)" if changed and not args.bump else ""))
    return 0


if __name__ == "__main__":
    sys.exit(main())
