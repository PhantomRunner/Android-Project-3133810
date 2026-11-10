import json
import shutil
from pathlib import Path
from tempfile import TemporaryDirectory


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "docs"
ASSETS = ROOT / "local-assets"
COLORS = ("red", "yellow", "blue", "green")
CARD_FIELDS = ("name", "subtitle", "type", "color", "cost", "power", "ram", "unitTypes", "eddies")


def read_catalog():
    sets = {}
    cards = {}
    printings = []
    sources = []
    image_paths = set()
    collector_numbers = set()

    for color in COLORS:
        path = SOURCE / f"catalog-{color}.json"
        data = json.loads(path.read_text(encoding="utf-8"))
        if data["version"] != 1 or data["color"] != color.upper():
            raise ValueError(f"Unexpected catalog version or color in {path.name}")
        sources.append({"color": data["color"], **data["source"]})

        for card_set in data["sets"]:
            set_id = card_set["id"]
            if sets.setdefault(set_id, card_set) != card_set:
                raise ValueError(f"Conflicting metadata for set {set_id}")

        set_ids = {card_set["id"] for card_set in data["sets"]}
        for row in data["cards"]:
            printing_id = row["id"]
            card_id = f"card:{printing_id}"
            number = row["collectorNumber"]
            set_id = row["setId"]
            image_path = row["imagePath"]
            image_prefix = f"card-images/{color}/{printing_id}"
            if not printing_id or "/" in printing_id or "\\" in printing_id or card_id in cards:
                raise ValueError(f"Invalid or duplicate printing ID: {printing_id}")
            if not number or set_id not in set_ids or (set_id, number) in collector_numbers:
                raise ValueError(f"Invalid set or collector number for {printing_id}")
            if row["color"] != color.upper() or image_path not in (f"{image_prefix}.jpg", f"{image_prefix}.webp"):
                raise ValueError(f"Image path or color does not match {printing_id}")

            card = {"id": card_id, **{field: row[field] for field in CARD_FIELDS}}
            if "powerLabel" in row:
                card["powerLabel"] = row["powerLabel"]
            cards[card_id] = card
            printing = {
                "id": printing_id,
                "cardId": card_id,
                "setId": set_id,
                "collectorNumber": number,
                "imagePath": image_path,
            }
            for field in ("variant", "finish", "imageSource"):
                if field in row:
                    printing[field] = row[field]
            printings.append(printing)
            image_paths.add(image_path)
            collector_numbers.add((set_id, number))

    files = {
        path.relative_to(SOURCE).as_posix()
        for path in (SOURCE / "card-images").rglob("*")
        if path.is_file() and path.suffix.lower() in (".jpg", ".jpeg", ".webp", ".png")
    }
    if files != image_paths or not image_paths:
        raise ValueError(f"Missing images: {sorted(image_paths - files)}\n"
                         f"Images without metadata: {sorted(files - image_paths)}")

    return {
        "version": 1,
        "sources": sources,
        "sets": list(sets.values()),
        "cards": list(cards.values()),
        "printings": printings,
    }


def prepare_catalog():
    catalog = read_catalog()
    output = ASSETS / "debug" / "collection"
    output.parent.mkdir(parents=True, exist_ok=True)

    # Finish copying outside the debug source set before replacing its previous package.
    with TemporaryDirectory(dir=ASSETS) as directory:
        staging = Path(directory) / "collection"
        staging.mkdir()
        for printing in catalog["printings"]:
            relative_path = printing["imagePath"]
            target = staging / relative_path
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(SOURCE / relative_path, target)
        (staging / "catalog.json").write_text(
            json.dumps(catalog, ensure_ascii=False, indent=2) + "\n", encoding="utf-8",
        )

        previous = Path(directory) / "previous"
        if output.exists():
            output.rename(previous)
        try:
            staging.rename(output)
        except OSError:
            if previous.exists():
                previous.rename(output)
            raise

    print(f"Prepared {len(catalog['cards'])} cards and {len(catalog['printings'])} images in {output}")


if __name__ == "__main__":
    try:
        prepare_catalog()
    except (KeyError, ValueError, OSError) as error:
        raise SystemExit(f"Collection package could not be prepared: {error}") from error
