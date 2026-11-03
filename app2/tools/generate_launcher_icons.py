from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app" / "src" / "main" / "res"
ARTWORK = ROOT / "artwork"

DENSITIES = {
    "mdpi": (48, 108),
    "hdpi": (72, 162),
    "xhdpi": (96, 216),
    "xxhdpi": (144, 324),
    "xxxhdpi": (192, 432),
}


def save_png(image: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, "PNG", optimize=True)


source = Image.open(ARTWORK / "trailgo_launcher_source.png").convert("RGBA")
foreground = Image.open(ARTWORK / "trailgo_launcher_foreground.png").convert("RGBA")

for density, (legacy_size, adaptive_size) in DENSITIES.items():
    folder = RES / f"mipmap-{density}"
    legacy = source.resize((legacy_size, legacy_size), Image.Resampling.LANCZOS)
    save_png(legacy, folder / "ic_launcher.png")

    round_mask = Image.new("L", (legacy_size, legacy_size), 0)
    ImageDraw.Draw(round_mask).ellipse((0, 0, legacy_size - 1, legacy_size - 1), fill=255)
    round_icon = legacy.copy()
    round_icon.putalpha(round_mask)
    save_png(round_icon, folder / "ic_launcher_round.png")

    adaptive_foreground = foreground.resize(
        (adaptive_size, adaptive_size), Image.Resampling.LANCZOS
    )
    save_png(adaptive_foreground, folder / "ic_launcher_foreground.png")

    background = Image.new("RGBA", (adaptive_size, adaptive_size), "#F8F6EF")
    save_png(background, folder / "ic_launcher_background.png")
