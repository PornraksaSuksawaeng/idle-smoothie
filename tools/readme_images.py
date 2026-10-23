#!/usr/bin/env python3
"""Build the images of README.md from the game's assets.

Reads   app/src/main/resources/images/...   (the game's assets)
        app/src/main/resources/audio/music.mp3
Writes  docs/readme/mine.png                 the mine as a cross-section: the surface, then one room per fruit
        docs/readme/zone_<fruit>_walk.gif    a Pulpie walking
        docs/readme/zone_<fruit>_crate.png   the full crate
        docs/readme/zone_<fruit>_soil.png    a square of the soil texture
        docs/readme/crate_empty.png
        docs/readme/manager_<rarity>.png     the five star fruit managers
        docs/readme/stations.png             the headframe, elevator cabin, bridge and bar at the same height
        docs/readme/icons.png                every icon in images/ui/
        art/preview/soundtrack.mp4           the music over a still of the loading screen, to upload on
                                             github.com (git ignores art/); built only if ffmpeg is found

logo.png, loading.png and screenshot.png in docs/readme/ are not made here: they come from outside the
game's resources. Every image is written at the size the README shows it, on the game's soil color so it
reads on GitHub's light and dark themes. A missing asset is reported and skipped.

Usage (from the repo root, needs Pillow: python3 -m pip install Pillow):
    python3 tools/readme_images.py                       the images, and the soundtrack if ffmpeg is found
    python3 tools/readme_images.py --still loading.png   use this picture for the soundtrack video
ffmpeg is taken from the PATH, or from the imageio-ffmpeg package (python3 -m pip install imageio-ffmpeg).
"""

import re
import shutil
import subprocess
import sys
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
RESOURCES = ROOT / "app" / "src" / "main" / "resources"
IMAGES = RESOURCES / "images"
MUSIC = RESOURCES / "audio" / "music.mp3"
OUT = ROOT / "docs" / "readme"
SOUNDTRACK = ROOT / "art" / "preview" / "soundtrack.mp4"

FRUITS = ["strawberry", "blueberry", "kiwi", "mango", "dragon_fruit"]
RARITIES = ["common", "rare", "epic", "legendary", "mythic"]
ICONS = ["coin", "speed", "energy", "settings"]  # shown first, in this order; other icons follow
SOIL = (0xA8, 0x77, 0x4A, 255)                   # SOIL_COLORS[0] in MineView
CELL = 256                                       # sprite sheet cell, see tools/sprites.py
RADIUS = 10

# Shown sizes, in pixels
MINE_WIDTH = 320
TILE = 80            # one cell of the fruit zones table
MANAGER_SIZE = 96
STATION_HEIGHT = 110
ICON_SIZE = 40
VIDEO_SIZE = (540, 960)

# The game's layout, in game pixels (MineView), used to draw the mine like the game does
GAME_WIDTH = 540
SURFACE_HEIGHT = 400
TOP_DIRT_HEIGHT = 167
EDGE_HEIGHT = 20
GROUND_RAISE = 7
SHAFT_WIDTH = 110
HEADFRAME_WIDTH_RATIO = 1.1
HEADFRAME_PLATFORM_Y = 0.89
HEADFRAME_EDGE_X = 0.784
CABIN_WIDTH_RATIO = (0.66 - 0.34) / (0.74 - 0.26)
BAR_WIDTH = 175
BAR_CENTER_X = 0.69
BAR_SINK = 5
BAR_SIDE_X = 0.105
BRIDGE_START_Y = 0.30
BRIDGE_OVERLAP = 4
BRIDGE_LEFT, BRIDGE_RIGHT = 9, 440
BRIDGE_TRUNK_TOP, BRIDGE_FOOT_TOP, BRIDGE_FOOT_BOTTOM = 150, 195, 211
BRIDGE_POSTS = [(44, 68), (206, 230), (368, 392)]
ROOM_IMAGE_WIDTH = 920
ROOM_IMAGE_GROUND = 245
PULPIE_SIZE = 80          # a 256px cell drawn at 80px, feet at y 236 and body at x 120 of the cell
ZONE_MARGIN = 22          # soil shown around each room


def load(path):
    """Returns the image as RGBA, or None (and says so) if the file does not exist yet."""
    file = IMAGES / path
    if not file.is_file():
        print(f"missing, skipped: images/{path}")
        return None
    return Image.open(file).convert("RGBA")


def scaled(image, width=None, height=None):
    if width is None:
        width = round(image.width * height / image.height)
    if height is None:
        height = round(image.height * width / image.width)
    return image.resize((max(1, round(width)), max(1, round(height))), Image.LANCZOS)


def rounded(image, radius=RADIUS):
    mask = Image.new("L", image.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, image.width - 1, image.height - 1), radius, fill=255)
    out = image.convert("RGBA")
    out.putalpha(mask)
    return out


def tile(image, size, fill=0.8):
    """The image fitted in a square soil tile, centered, its bottom on the tile's lower part."""
    out = Image.new("RGBA", (size, size), SOIL)
    box = size * fill
    fitted = scaled(image, width=box) if image.width >= image.height else scaled(image, height=box)
    if fitted.height > box:
        fitted = scaled(image, height=box)
    out.alpha_composite(fitted, ((size - fitted.width) // 2, round(size - (size - box) / 2 - fitted.height)))
    return out


def cells(sheet):
    return [sheet.crop((i * CELL, 0, (i + 1) * CELL, CELL)) for i in range(sheet.width // CELL)]


def paste(canvas, image, x, y, width=None, height=None):
    canvas.alpha_composite(scaled(image, width, height), (round(x), round(y)))


# --- The mine ---------------------------------------------------------------------------------

def mine():
    surface, ground = load("mine/surface.png"), load("mine/ground.png")
    zones = [(fruit, load(f"mine/room_{fruit}.png"), load(f"mine/soil_{fruit}.png"),
              load(f"mine/crate_{fruit}.png"), load(f"pulpies/{fruit}/walk.png")) for fruit in FRUITS]
    zones = [zone for zone in zones if zone[1] is not None and zone[2] is not None]
    if surface is None or not zones:
        return
    room_width = GAME_WIDTH - ZONE_MARGIN
    room_scale = room_width / ROOM_IMAGE_WIDTH
    room_heights = [room.getchannel("A").getbbox()[3] * room_scale for _, room, *_ in zones]
    top = SURFACE_HEIGHT + TOP_DIRT_HEIGHT
    zone_heights = [ZONE_MARGIN + height + ZONE_MARGIN for height in room_heights]
    canvas = Image.new("RGBA", (GAME_WIDTH, round(top + sum(zone_heights))), SOIL)
    paste(canvas, surface, 0, 0, GAME_WIDTH, SURFACE_HEIGHT)

    y = top
    for (fruit, room, soil, crate, walk), zone_height in zip(zones, zone_heights):
        source_height = soil.width * zone_height / GAME_WIDTH
        paste(canvas, soil.crop((0, 0, soil.width, round(min(soil.height, source_height)))),
              0, y, GAME_WIDTH, zone_height)
        room_top = y + ZONE_MARGIN
        paste(canvas, room, 0, room_top, room_width, room.height * room_scale)
        floor_y = room_top + ROOM_IMAGE_GROUND * room_scale
        if crate is not None:
            crate_width = 54
            paste(canvas, crate, SHAFT_WIDTH + 10, floor_y - crate.height * crate_width / crate.width + 4,
                  width=crate_width)
        if walk is not None:
            pulpie = cells(walk)[1]
            paste(canvas, pulpie, 300 - 120 * PULPIE_SIZE / CELL, floor_y - 236 * PULPIE_SIZE / CELL, PULPIE_SIZE,
                  PULPIE_SIZE)
        y += zone_height

    if ground is not None:
        paste(canvas, ground, 0, SURFACE_HEIGHT - GROUND_RAISE, GAME_WIDTH, TOP_DIRT_HEIGHT + EDGE_HEIGHT)
    draw_shaft(canvas, SURFACE_HEIGHT, canvas.height)
    draw_surface_buildings(canvas)
    rounded(scaled(canvas, width=MINE_WIDTH)).save(OUT / "mine.png", optimize=True)


def draw_shaft(canvas, top, bottom):
    parts = [load(f"mine/shaft_{name}.png") for name in ("top", "middle", "bottom")]
    if None in parts:
        return
    shaft_top, middle, shaft_bottom = (scaled(part, width=SHAFT_WIDTH) for part in parts)
    y = top + shaft_top.height
    end = bottom - shaft_bottom.height
    while y < end:
        piece = middle.crop((0, 0, SHAFT_WIDTH, min(middle.height, round(end - y))))
        canvas.alpha_composite(piece, (0, round(y)))
        y += middle.height
    canvas.alpha_composite(shaft_top, (0, round(top)))
    canvas.alpha_composite(shaft_bottom, (0, round(end)))


def draw_surface_buildings(canvas):
    headframe, elevator = load("mine/headframe.png"), load("mine/elevator.png")
    bar, bridge = load("mine/bar.png"), load("mine/bridge.png")
    if headframe is None:
        return
    head_width = SHAFT_WIDTH * HEADFRAME_WIDTH_RATIO
    head_height = headframe.height * head_width / headframe.width
    head_x = (SHAFT_WIDTH - head_width) / 2
    if bar is not None:
        bar_height = bar.height * BAR_WIDTH / bar.width
        bar_x = GAME_WIDTH * BAR_CENTER_X - BAR_WIDTH / 2
        if bridge is not None:
            draw_bridge(canvas, bridge, head_x + head_width * HEADFRAME_EDGE_X - BRIDGE_OVERLAP,
                        bar_x + BAR_WIDTH * BAR_SIDE_X + BRIDGE_OVERLAP,
                        SURFACE_HEIGHT - head_height * (HEADFRAME_PLATFORM_Y - BRIDGE_START_Y))
    paste(canvas, headframe, head_x, SURFACE_HEIGHT - head_height * HEADFRAME_PLATFORM_Y, head_width, head_height)
    if elevator is not None:
        cabin_width = SHAFT_WIDTH * CABIN_WIDTH_RATIO
        cabin_height = elevator.height * cabin_width / elevator.width
        paste(canvas, elevator, (SHAFT_WIDTH - cabin_width) / 2, SURFACE_HEIGHT - cabin_height, cabin_width,
              cabin_height)
    if bar is not None:
        paste(canvas, bar, bar_x, SURFACE_HEIGHT + BAR_SINK - bar_height, BAR_WIDTH, bar_height)


def draw_bridge(canvas, bridge, deck_left, deck_right, top):
    """The deck as drawn, its posts stretched down to the grass, like MineView.drawBridge."""
    scale = (deck_right - deck_left) / (BRIDGE_RIGHT - BRIDGE_LEFT)
    image_x = deck_left - BRIDGE_LEFT * scale
    paste(canvas, bridge.crop((0, 0, bridge.width, BRIDGE_FOOT_TOP)), image_x, top,
          bridge.width * scale, BRIDGE_FOOT_TOP * scale)
    foot_height = (BRIDGE_FOOT_BOTTOM - BRIDGE_FOOT_TOP) * scale
    foot_y = SURFACE_HEIGHT + BAR_SINK - foot_height
    trunk_y = top + BRIDGE_FOOT_TOP * scale
    for left, right in BRIDGE_POSTS:
        x, width = image_x + left * scale, (right - left) * scale
        paste(canvas, bridge.crop((left, BRIDGE_TRUNK_TOP, right, BRIDGE_FOOT_TOP)), x, trunk_y, width,
              foot_y - trunk_y)
        paste(canvas, bridge.crop((left, BRIDGE_FOOT_TOP, right, BRIDGE_FOOT_BOTTOM)), x, foot_y, width,
              foot_height)


# --- Fruit zones, managers, stations, icons ---------------------------------------------------

def zones():
    for fruit in FRUITS:
        walk = load(f"pulpies/{fruit}/walk.png")
        if walk is not None:
            frames = []
            for frame in cells(walk):
                image = Image.new("RGBA", (CELL, CELL), SOIL)
                image.alpha_composite(frame)
                frames.append(rounded(scaled(image, TILE, TILE)).convert("RGBA"))
            save_gif(frames, OUT / f"zone_{fruit}_walk.gif", 100)
        crate = load(f"mine/crate_{fruit}.png")
        if crate is not None:
            rounded(tile(crate, TILE)).save(OUT / f"zone_{fruit}_crate.png", optimize=True)
        soil = load(f"mine/soil_{fruit}.png")
        if soil is not None:
            side = min(soil.size) // 2
            left, top = (soil.width - side) // 2, (soil.height - side) // 2
            rounded(scaled(soil.crop((left, top, left + side, top + side)), TILE, TILE)).save(
                OUT / f"zone_{fruit}_soil.png", optimize=True)
    empty = load("mine/crate_empty.png")
    if empty is not None:
        rounded(tile(empty, TILE)).save(OUT / "crate_empty.png", optimize=True)


def save_gif(frames, path, duration):
    """A looping GIF whose rounded corners stay transparent."""
    paletted = []
    for frame in frames:
        p = frame.convert("RGB").quantize(255)
        p.paste(255, mask=frame.getchannel("A").point(lambda v: 255 if v < 128 else 0))
        paletted.append(p)
    paletted[0].save(path, save_all=True, append_images=paletted[1:], duration=duration, loop=0,
                     transparency=255, disposal=2, optimize=False)


def managers():
    sheet = load("managers/rarities.png")
    if sheet is None:
        return
    for rarity, cell in zip(RARITIES, cells(sheet)):
        rounded(tile(cell, MANAGER_SIZE, fill=1.0)).save(OUT / f"manager_{rarity}.png", optimize=True)


def stations():
    parts = [load(f"mine/{name}.png") for name in ("headframe", "elevator", "bridge", "bar")]
    parts = [scaled(part.crop(part.getchannel("A").getbbox()), height=STATION_HEIGHT)
             for part in parts if part is not None]
    strip(parts, OUT / "stations.png", pad=14, gap=22)


def icons():
    files = sorted((IMAGES / "ui").glob("*.png"), key=lambda f: (ICONS.index(f.stem) if f.stem in ICONS
                                                                 else len(ICONS), f.stem))
    missing = [name for name in ICONS if not (IMAGES / "ui" / f"{name}.png").is_file()]
    for name in missing:
        print(f"missing, skipped: images/ui/{name}.png")
    parts = [scaled(Image.open(f).convert("RGBA"), height=ICON_SIZE) for f in files]
    strip(parts, OUT / "icons.png", pad=10, gap=16)


def strip(parts, path, pad, gap):
    if not parts:
        return
    height = max(part.height for part in parts)
    image = Image.new("RGBA", (2 * pad + sum(p.width for p in parts) + gap * (len(parts) - 1), 2 * pad + height),
                      SOIL)
    x = pad
    for part in parts:
        image.alpha_composite(part, (x, pad + height - part.height))
        x += part.width + gap
    rounded(image).save(path, optimize=True)


# --- Soundtrack -------------------------------------------------------------------------------

def ffmpeg():
    path = shutil.which("ffmpeg")
    if path:
        return path
    try:
        import imageio_ffmpeg
        return imageio_ffmpeg.get_ffmpeg_exe()
    except ImportError:
        return None


def soundtrack(still):
    tool = ffmpeg()
    if tool is None or not MUSIC.is_file() or not still.is_file():
        print("soundtrack skipped: needs ffmpeg, " + str(MUSIC.relative_to(ROOT)) + " and the still " + str(still))
        return
    SOUNDTRACK.parent.mkdir(parents=True, exist_ok=True)
    frame = SOUNDTRACK.with_suffix(".png")
    image = Image.open(still).convert("RGB")
    scaled(image, *VIDEO_SIZE).save(frame)
    # -shortest overshoots with a looped still image, so the video is cut at the music's length
    probe = subprocess.run([tool, "-hide_banner", "-i", str(MUSIC)], capture_output=True, text=True).stderr
    hours, minutes, seconds = re.search(r"Duration: (\d+):(\d+):([\d.]+)", probe).groups()
    duration = int(hours) * 3600 + int(minutes) * 60 + float(seconds)
    subprocess.run([tool, "-y", "-loglevel", "error", "-loop", "1", "-framerate", "1", "-i", str(frame),
                    "-i", str(MUSIC), "-map", "0:v", "-map", "1:a", "-t", f"{duration:.2f}",
                    "-c:v", "libx264", "-tune", "stillimage", "-pix_fmt", "yuv420p", "-r", "1",
                    "-c:a", "aac", "-b:a", "160k", "-movflags", "+faststart", str(SOUNDTRACK)], check=True)
    frame.unlink()
    print(f"{SOUNDTRACK.relative_to(ROOT)}  {SOUNDTRACK.stat().st_size / 1e6:.1f} MB")


def main(args):
    still = Path(args[args.index("--still") + 1]) if "--still" in args else OUT / "loading.png"
    OUT.mkdir(parents=True, exist_ok=True)
    for build in (mine, zones, managers, stations, icons):
        build()
    soundtrack(still)
    total = sum(f.stat().st_size for f in OUT.iterdir() if f.is_file())
    print(f"{OUT.relative_to(ROOT)}/  {len(list(OUT.iterdir()))} files, {total / 1e6:.1f} MB")


if __name__ == "__main__":
    main(sys.argv[1:])
