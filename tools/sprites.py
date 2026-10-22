#!/usr/bin/env python3
"""Clean the generated Pulpie sprite sheets and pack them for the game.

Reads   art/raw/<fruit>/<animation>.png          (a grid sheet)
   or   art/raw/<fruit>/<animation>/01.png ...   (one image per frame)
Writes  app/src/main/resources/images/pulpies/<fruit>/<animation>.png   (one row of CELL x CELL frames)
        art/preview/<fruit>_<animation>.gif     (the loop)
        art/preview/<fruit>_all.gif             (every animation of the fruit side by side)

For each sheet: remove a plain background if the image has no transparency, give each piece to the
grid cell its center falls in, scale the sheet so the Pulpie has TARGET_HEIGHT, then place every
frame with the feet on GROUND_Y and the body center on BODY_X.

Usage (from the repo root, needs Pillow: python3 -m pip install Pillow):
    python3 tools/sprites.py                    every fruit that has files
    python3 tools/sprites.py strawberry kiwi    only these fruits
"""

import statistics
import sys
from collections import deque
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

# --- Settings you may tune -------------------------------------------------------------------

FRUITS = ["strawberry", "blueberry", "kiwi", "mango", "dragon_fruit"]
# animation -> (columns, rows) of the sheet, frames read left to right then top to bottom
GRIDS = {"walk": (3, 2), "fill": (3, 2), "carry": (3, 2), "deposit": (3, 2), "idle": (2, 2)}

CELL = 256              # size of one output frame, in pixels
EDGE_MARGIN = 4         # empty border kept in each cell: the game shrinks the whole row at once,
                        # so a frame touching its edge bleeds into the next frame
TARGET_HEIGHT = 200     # Pulpie height from the top of the leaf to the bottom of the feet fill
GROUND_Y = 236          # y of the bottom of the feet in every cell (the outline goes a little lower)
BODY_X = 120            # x of the body center in every cell; left of 128 to leave room for the crate

# Hand correction per sheet, on top of the automatic height match: 1.05 makes that sheet 5% bigger.
# Use it when a pose makes the measured height lie (a squashed jump, a raised leaf...).
SCALE_FIX = {
    ("strawberry", "walk"): 1.0,
    ("strawberry", "fill"): 1.0,
    ("strawberry", "carry"): 1.0,
    ("strawberry", "deposit"): 1.0,
    ("strawberry", "idle"): 1.0,
}
# Hand shift per sheet in output pixels (dx, dy), if the automatic anchor is off for a sheet.
OFFSET_FIX = {
    ("kiwi", "deposit"): (-8, 0),   # frame 4 pushes the crate past the cell margin
}

PREVIEW_MS = {"walk": 100, "fill": 120, "carry": 100, "deposit": 120, "idle": 180}
PREVIEW_BG = (190, 225, 240)

# --- Detection thresholds --------------------------------------------------------------------

ALPHA_MIN = 16          # a pixel with less alpha is background
SPECK_MAX = 40          # pieces of this many pixels or fewer are dropped
BG_TOLERANCE = 40       # max channel difference to a background color for the flood fill
BG_SAMPLE = 40          # size of the corner squares where the background colors are sampled
BODY_BAND = (0.30, 0.50)  # rows, as a share of the Pulpie height, where only the body is (no leaf, arms or crate)
FEET_BAND = 0.40        # feet are searched within body center +- this share of the body width
HUE_TOLERANCE = 18      # body color match, in Pillow hue units (0-255)
BACK_BAND = 0.45        # back-half feet are searched from body center - this share of the body width
FEET_COLOR_GAP = 0.05   # body-colored feet this share of the height above the back-half feet are wrong

ROOT = Path(__file__).resolve().parent.parent
RAW = ROOT / "art" / "raw"
PREVIEW = ROOT / "art" / "preview"
OUT = ROOT / "app" / "src" / "main" / "resources" / "images" / "pulpies"


class SpriteError(Exception):
    pass


# --- Loading ---------------------------------------------------------------------------------

def load_rgba(path):
    image = Image.open(path).convert("RGBA")
    if image.getchannel("A").getextrema()[0] == 255:
        image = remove_background(image)
    return image


def remove_background(image):
    """Flood-fills from the four corners, so light areas inside the outline are kept.

    The background colors are sampled in the corners, so a painted checkerboard, whose squares of
    one color only touch at their corners, is removed as a whole.
    """
    width, height = image.size
    pixels = image.load()
    colors = background_colors(image)

    def is_background(pixel):
        return any(max(abs(pixel[0] - c[0]), abs(pixel[1] - c[1]), abs(pixel[2] - c[2])) <= BG_TOLERANCE
                   for c in colors)

    seen = bytearray(width * height)
    queue = deque()
    for cx, cy in [(0, 0), (width - 1, 0), (0, height - 1), (width - 1, height - 1)]:
        if is_background(pixels[cx, cy]) and not seen[cy * width + cx]:
            seen[cy * width + cx] = 1
            queue.append((cx, cy))
    while queue:
        x, y = queue.popleft()
        pixels[x, y] = (0, 0, 0, 0)
        for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
            if 0 <= nx < width and 0 <= ny < height and not seen[ny * width + nx]:
                if is_background(pixels[nx, ny]):
                    seen[ny * width + nx] = 1
                    queue.append((nx, ny))
    return image


def background_colors(image):
    """Returns the colors that cover at least 5% of the four corner squares."""
    width, height = image.size
    size = min(BG_SAMPLE, width // 4, height // 4)
    counts = {}
    for x0, y0 in [(0, 0), (width - size, 0), (0, height - size), (width - size, height - size)]:
        data = image.crop((x0, y0, x0 + size, y0 + size)).tobytes()
        for i in range(0, len(data), 4):
            key = (data[i] // 8 * 8 + 4, data[i + 1] // 8 * 8 + 4, data[i + 2] // 8 * 8 + 4)
            counts[key] = counts.get(key, 0) + 1
    total = 4 * size * size
    return [color for color, count in counts.items() if count >= 0.05 * total]


def find_pieces(image):
    """Returns the connected opaque pieces bigger than a speck, as lists of flat pixel indexes."""
    width, height = image.size
    alpha = image.getchannel("A").tobytes()
    seen = bytearray(width * height)
    pieces = []
    for start in range(width * height):
        if seen[start] or alpha[start] < ALPHA_MIN:
            continue
        seen[start] = 1
        stack = [start]
        piece = []
        while stack:
            i = stack.pop()
            piece.append(i)
            x = i % width
            for j in (i - width, i + width, i - 1 if x > 0 else -1, i + 1 if x < width - 1 else -1):
                if 0 <= j < width * height and not seen[j] and alpha[j] >= ALPHA_MIN:
                    seen[j] = 1
                    stack.append(j)
        if len(piece) > SPECK_MAX:
            pieces.append(piece)
    return pieces


def cut_frame(image, pieces):
    """Builds one frame from its pieces. Returns the frame and the mask of its biggest piece."""
    width = image.size[0]
    full = Image.new("L", image.size, 0)
    main = Image.new("L", image.size, 0)
    full_px, main_px = full.load(), main.load()
    biggest = max(pieces, key=len)
    for piece in pieces:
        target = (full_px, main_px) if piece is biggest else (full_px,)
        for i in piece:
            for px in target:
                px[i % width, i // width] = 255
    # widen by a pixel to keep the soft edge pixels under ALPHA_MIN
    full = full.filter(ImageFilter.MaxFilter(3))
    box = full.getbbox()
    frame = Image.new("RGBA", image.size, (0, 0, 0, 0))
    frame.paste(image, (0, 0), full)
    return frame.crop(box), main.crop(box)


def load_frames(fruit, animation):
    folder = RAW / fruit / animation
    sheet = RAW / fruit / f"{animation}.png"
    name = f"{fruit}/{animation}"
    if folder.is_dir():
        files = sorted(folder.glob("*.png"))
        if not files:
            raise SpriteError(f"{name}/: the folder holds no .png frame")
        frames = []
        for path in files:
            image = load_rgba(path)
            pieces = find_pieces(image)
            if not pieces:
                raise SpriteError(f"{name}/{path.name}: the frame is empty")
            frames.append(cut_frame(image, pieces))
        return frames
    if not sheet.is_file():
        return None
    columns, rows = GRIDS[animation]
    image = load_rgba(sheet)
    width, height = image.size
    cells = [[] for _ in range(columns * rows)]
    for piece in find_pieces(image):
        cx = sum(i % width for i in piece) / len(piece)
        cy = sum(i // width for i in piece) / len(piece)
        column = min(int(cx * columns / width), columns - 1)
        row = min(int(cy * rows / height), rows - 1)
        cells[row * columns + column].append(piece)
    for index, pieces in enumerate(cells):
        if not pieces:
            raise SpriteError(f"{name}.png: frame {index + 1} (row {index // columns + 1}, "
                              f"column {index % columns + 1}) is empty; expected a "
                              f"{columns}x{rows} grid read left to right, top to bottom")
    return [cut_frame(image, pieces) for pieces in cells]


# --- Measuring -------------------------------------------------------------------------------

def longest_run(row):
    best_start, best_length, start = 0, 0, None
    for x, value in enumerate(bytes(row) + b"\0"):
        if value and start is None:
            start = x
        elif not value and start is not None:
            if x - start > best_length:
                best_start, best_length = start, x - start
            start = None
    return best_start, best_length


def measure(frame, main):
    """Returns (body_x, feet_y, back_feet_y, top_y) in frame pixels, ignoring arms, crate and loose pieces.

    feet_y looks for the body color; back_feet_y is the fallback when the feet have another color.
    """
    width, height = main.size
    top = main.getbbox()[1]
    bottom = main.getbbox()[3]
    rows = main.tobytes()
    centers, widths = [], []
    for y in range(int(top + BODY_BAND[0] * (bottom - top)), int(top + BODY_BAND[1] * (bottom - top))):
        start, length = longest_run(rows[y * width:(y + 1) * width])
        centers.append(start + (length - 1) / 2)
        widths.append(length)
    body_x = statistics.median(centers)
    body_width = statistics.median(widths)

    # The body color is the most common saturated hue in the middle of the body band.
    hsv = frame.convert("RGB").convert("HSV").load()
    histogram = [0] * 256
    y0, y1 = int(top + BODY_BAND[0] * (bottom - top)), int(top + BODY_BAND[1] * (bottom - top))
    for y in range(y0, y1):
        for x in range(int(body_x - body_width / 4), int(body_x + body_width / 4)):
            h, s, v = hsv[x, y]
            if rows[y * width + x] and s > 90 and v > 90:
                histogram[h] += 1
    body_hue = max(range(256), key=lambda h: sum(histogram[(h + d) % 256] for d in range(-4, 5)))

    # The feet are the lowest body-colored pixels under the body.
    feet = None
    x0 = max(0, int(body_x - FEET_BAND * body_width))
    x1 = min(width, int(body_x + FEET_BAND * body_width) + 1)
    for y in range(bottom - 1, top, -1):
        for x in range(x0, x1):
            h, s, v = hsv[x, y]
            hue_gap = min(abs(h - body_hue), 256 - abs(h - body_hue))
            if rows[y * width + x] and s > 90 and v > 90 and hue_gap <= HUE_TOLERANCE:
                feet = y
                break
        if feet is not None:
            break
    if feet is None:
        feet = bottom - 1

    # Second guess for fruits whose feet are not the body color: the lowest pixel in the back half
    # of the body, where the crate held in front never is.
    back_feet = bottom - 1
    x0 = max(0, int(body_x - BACK_BAND * body_width))
    for y in range(bottom - 1, top, -1):
        if any(rows[y * width + x] for x in range(x0, int(body_x) + 1)):
            back_feet = y
            break
    return body_x, feet, back_feet, top


def choose_feet(measures_by_animation):
    """Uses the back-half feet for the whole fruit when its body-colored feet sit clearly too high."""
    measures = [m for measures in measures_by_animation.values() for m in measures]
    gap = statistics.median(back - feet for _, feet, back, _ in measures)
    height = statistics.median(back - top for _, _, back, top in measures)
    use_back = gap > FEET_COLOR_GAP * height
    return {animation: [(body_x, back if use_back else feet, top) for body_x, feet, back, top in measures]
            for animation, measures in measures_by_animation.items()}, use_back


# --- Packing ---------------------------------------------------------------------------------

def pack(fruit, animation, frames, measures):
    height = statistics.median(feet - top for _, feet, top in measures)
    scale = TARGET_HEIGHT / height * SCALE_FIX.get((fruit, animation), 1.0)
    dx, dy = OFFSET_FIX.get((fruit, animation), (0, 0))
    cells, clipped = [], []
    for number, ((frame, _), (body_x, feet, _)) in enumerate(zip(frames, measures), start=1):
        size = (max(1, round(frame.width * scale)), max(1, round(frame.height * scale)))
        scaled = frame.resize(size, Image.LANCZOS)
        left = round(BODY_X + dx - body_x * scale)
        top = round(GROUND_Y + dy - feet * scale)
        bbox = scaled.getchannel("A").getbbox()
        if bbox and (left + bbox[0] < EDGE_MARGIN or top + bbox[1] < EDGE_MARGIN
                     or left + bbox[2] > CELL - EDGE_MARGIN or top + bbox[3] > CELL - EDGE_MARGIN):
            clipped.append(number)
        cell = Image.new("RGBA", (CELL, CELL), (0, 0, 0, 0))
        cell.paste(scaled, (left, top))  # the cell is empty, so a plain copy keeps the alpha exact
        cells.append(cell)
    return cells, height, scale, clipped


def write_sheet(fruit, animation, cells):
    path = OUT / fruit / f"{animation}.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    strip = Image.new("RGBA", (CELL * len(cells), CELL), (0, 0, 0, 0))
    for index, cell in enumerate(cells):
        strip.paste(cell, (index * CELL, 0))
    strip.save(path)
    return path


def on_background(cell, guides=False):
    image = Image.new("RGBA", cell.size, PREVIEW_BG + (255,))
    if guides:
        draw = ImageDraw.Draw(image)
        draw.line([(0, GROUND_Y), (cell.width, GROUND_Y)], fill=(120, 160, 180))
        draw.line([(BODY_X, 0), (BODY_X, cell.height)], fill=(120, 160, 180))
    image.alpha_composite(cell)
    return image.convert("RGB")


def write_gif(path, images, duration):
    path.parent.mkdir(parents=True, exist_ok=True)
    images[0].save(path, save_all=True, append_images=images[1:], duration=duration, loop=0)


def write_comparison(fruit, sheets):
    """Every animation side by side, each looping at its own length, with the ground and body guides."""
    names = list(sheets)
    length = 1
    for cells in sheets.values():
        length = length * len(cells) // _gcd(length, len(cells))
    label_height = 18
    images = []
    for step in range(length):
        image = Image.new("RGB", (CELL * len(names), CELL + label_height), "white")
        draw = ImageDraw.Draw(image)
        for index, name in enumerate(names):
            cells = sheets[name]
            image.paste(on_background(cells[step % len(cells)], guides=True), (index * CELL, label_height))
            draw.text((index * CELL + 6, 3), name, fill="black")
        images.append(image)
    write_gif(PREVIEW / f"{fruit}_all.gif", images, 120)


def _gcd(a, b):
    while b:
        a, b = b, a % b
    return a


def main(fruits):
    unknown = [fruit for fruit in fruits if fruit not in FRUITS]
    if unknown:
        raise SpriteError(f"unknown fruit {', '.join(unknown)}; expected one of {', '.join(FRUITS)}")
    done = 0
    for fruit in fruits:
        frames_by_animation = {}
        for animation in GRIDS:
            frames = load_frames(fruit, animation)
            if frames is not None:
                frames_by_animation[animation] = frames
        if not frames_by_animation:
            print(f"{fruit:13} skipped, no files in {(RAW / fruit).relative_to(ROOT)}/")
            continue
        measures, use_back = choose_feet({animation: [measure(frame, main) for frame, main in frames]
                                          for animation, frames in frames_by_animation.items()})
        print(f"{fruit:13} feet found by {'their place under the back half' if use_back else 'the body color'}")
        sheets = {}
        for animation, frames in frames_by_animation.items():
            cells, height, scale, clipped = pack(fruit, animation, frames, measures[animation])
            path = write_sheet(fruit, animation, cells)
            write_gif(PREVIEW / f"{fruit}_{animation}.gif", [on_background(c) for c in cells],
                      PREVIEW_MS[animation])
            sheets[animation] = cells
            note = (f"  WARNING: frames {clipped} reach the {EDGE_MARGIN}px cell margin; "
                    f"move them with OFFSET_FIX or shrink them with SCALE_FIX" if clipped else "")
            print(f"{fruit:13} {animation:8} {len(cells)} frames  measured {height:6.1f}px  "
                  f"scale {scale:.3f}  -> {path.relative_to(ROOT)}{note}")
            done += 1
        missing = [animation for animation in GRIDS if animation not in sheets]
        if missing:
            print(f"{fruit:13} missing: {', '.join(missing)}")
        write_comparison(fruit, sheets)
    if done == 0:
        raise SpriteError(f"no sheet found under {RAW.relative_to(ROOT)}/")


if __name__ == "__main__":
    try:
        main(sys.argv[1:] or FRUITS)
    except SpriteError as error:
        sys.exit(f"sprites.py: {error}")
