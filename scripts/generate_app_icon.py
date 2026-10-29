# /// script
# dependencies = ["pillow", "numpy", "scipy"]
# ///
"""Original Toybox launcher artwork. Regenerate with: uv run scripts/generate_app_icon.py."""
from pathlib import Path
from math import cos, sin, radians
import json
import numpy as np
from PIL import Image, ImageDraw, ImageCms
from scipy.ndimage import distance_transform_edt

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app/src/main/res"
SOURCES = ROOT / "design-assets/icons"
SIZE = 1024
BACKGROUND = "#DEEAF3"
PROFILE = ImageCms.ImageCmsProfile(ImageCms.createProfile("sRGB")).tobytes()
parts = {"background": [], "blocks": [], "ball": []}


def tone(color, amount):
    channels = [int(color[i:i + 2], 16) for i in (1, 3, 5)]
    if amount > 0:
        channels = [round(c + (255 - c) * amount) for c in channels]
    else:
        channels = [round(c * (1 + amount)) for c in channels]
    return "#" + "".join(f"{c:02X}" for c in channels)


def polygon(layer, points, color):
    parts[layer].append({"kind": "polygon", "points": points, "color": color})


def line(layer, points, color, width):
    parts[layer].append({"kind": "line", "points": points, "color": color, "width": width})


def circle(layer, x, y, radius, color):
    parts[layer].append({"kind": "circle", "center": [x, y], "radius": radius, "color": color})


def cube(layer, x, y, color, width=140, height=48, depth=96, rotation=0):
    top = [(x, y - height / 2), (x + width / 2, y),
           (x, y + height / 2), (x - width / 2, y)]
    left = [top[3], top[2], (x, y + height / 2 + depth), (x - width / 2, y + depth)]
    right = [top[2], top[1], (x + width / 2, y + depth), (x, y + height / 2 + depth)]
    pivot = (x, y + depth / 2)
    def turn(points):
        angle = radians(rotation)
        return [(pivot[0] + (px - pivot[0]) * cos(angle) - (py - pivot[1]) * sin(angle),
                 pivot[1] + (px - pivot[0]) * sin(angle) + (py - pivot[1]) * cos(angle))
                for px, py in points]
    # Opaque material faces express cube geometry; the launcher supplies its own shadows.
    polygon(layer, turn(left), color)
    polygon(layer, turn(right), tone(color, -.23))
    polygon(layer, turn(top), tone(color, .27))


circle("background", 512, 512, 512, BACKGROUND)
polygon("background", [(182, 707), (512, 810), (842, 707), (842, 731), (512, 834), (182, 731)], "#425B77")
polygon("background", [(182, 707), (512, 604), (842, 707), (512, 810)], "#718DA9")
for offset in [-.5, 0, .5]:
    # Three wide grid divisions echo the actual Toybox arena, while staying legible at icon size.
    t = (offset + 1) / 2
    line("background", [(182 + 330 * t, 707 - 103 * t), (512 + 330 * t, 810 - 103 * t)], "#90A8BF", 4)
    line("background", [(842 - 330 * t, 707 - 103 * t), (512 - 330 * t, 810 - 103 * t)], "#90A8BF", 4)

# The stepped silhouette represents the 50-block pyramid without tiny, unreadable detail.
cube("blocks", 356, 586, "#FF686B")
cube("blocks", 512, 586, "#60D394")
cube("blocks", 668, 586, "#4D9DE0")
cube("blocks", 434, 454, "#FFBC42")
cube("blocks", 590, 454, "#A882DD")
cube("blocks", 512, 322, "#F78FB3")
cube("blocks", 760, 336, "#FFBC42", width=98, height=34, depth=74, rotation=-23)

line("ball", [(145, 579), (177, 568)], "#4D9DE0", 14)
line("ball", [(157, 527), (182, 519)], "#4D9DE0", 14)
circle("ball", 254, 554, 80, "#36516F")
circle("ball", 254, 554, 73, "#F4F6FA")


def path_for(part, scale=1):
    def point(p): return f"{p[0] * scale:.5f},{p[1] * scale:.5f}"
    if part["kind"] == "circle":
        x, y = part["center"]; r = part["radius"]
        return (f"M{(x-r)*scale:.5f},{y*scale:.5f}"
                f"a{r*scale:.5f},{r*scale:.5f} 0 1,0 {2*r*scale:.5f},0"
                f"a{r*scale:.5f},{r*scale:.5f} 0 1,0 {-2*r*scale:.5f},0Z")
    return "M" + "L".join(point(p) for p in part["points"]) + ("Z" if part["kind"] == "polygon" else "")


def svg(groups):
    body = []
    for group in groups:
        body.append(f'<g id="{group}">')
        for part in parts[group]:
            path = path_for(part, 24 / SIZE)
            if part["kind"] == "line":
                body.append(f'<path d="{path}" fill="none" stroke="{part["color"]}" stroke-width="{part["width"]*24/SIZE:.5f}"/>')
            else:
                body.append(f'<path d="{path}" fill="{part["color"]}"/>')
        body.append('</g>')
    return '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24">\n' + '\n'.join(body) + '\n</svg>\n'


def render(group):
    image = Image.new("RGBA", (SIZE, SIZE))
    draw = ImageDraw.Draw(image)
    for part in parts[group]:
        if part["kind"] == "circle":
            x, y = part["center"]; r = part["radius"]
            draw.ellipse((x-r, y-r, x+r, y+r), fill=part["color"])
        elif part["kind"] == "polygon":
            draw.polygon(part["points"], fill=part["color"])
        else:
            draw.line(part["points"], fill=part["color"], width=part["width"])
    # Crisp foreground alpha; no baked semitransparent shadows, glows, or texture bleed.
    return image


def save(image, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, optimize=True, icc_profile=PROFILE)


layers = {group: render(group) for group in parts}
y, x = np.indices((SIZE, SIZE))
circle_mask = ((x + .5 - 512) ** 2 + (y + .5 - 512) ** 2) <= 512 ** 2
background = np.array(layers["background"])
background[~circle_mask] = 0
background[circle_mask, 3] = 255
layers["background"] = Image.fromarray(background)

SOURCES.mkdir(parents=True, exist_ok=True)
for group, image in layers.items():
    save(image, RES / "drawable-nodpi" / f"ic_toybox_{group}.png")
    (SOURCES / f"ic_physics_toybox_{group}.svg").write_text(svg([group]))
    mask = np.array(image)[:, :, 3] > 127
    # PICO's generated icon format uses an interior 32-pixel distance ramp in all RGBA channels.
    field = np.clip(distance_transform_edt(mask) / 32 * 255, 0, 255).astype(np.uint8)
    sdf = Image.fromarray(np.repeat(field[:, :, None], 4, axis=2), "RGBA")
    save(sdf, RES / "drawable-nodpi" / f"ic_toybox_sdf_{group}.png")

composite = layers["background"].copy()
composite.alpha_composite(layers["blocks"])
composite.alpha_composite(layers["ball"])
save(composite, RES / "mipmap-xxxhdpi" / "ic_toybox_launcher.png")
save(composite, SOURCES / "ic_physics_toybox.png")
(SOURCES / "ic_physics_toybox.svg").write_text(svg(["background", "blocks", "ball"]))

foreground = Image.new("RGBA", (SIZE, SIZE))
for group in ["background", "blocks", "ball"]:
    if group == "background":
        platform = render(group)
        array = np.array(platform)
        array[np.all(array[:, :, :3] == [222, 234, 243], axis=2)] = 0
        platform = Image.fromarray(array)
        foreground.alpha_composite(platform)
    else:
        foreground.alpha_composite(layers[group])
adaptive_foreground = Image.new("RGBA", (SIZE, SIZE))
smaller = foreground.resize((820, 820), Image.Resampling.LANCZOS)
adaptive_foreground.alpha_composite(smaller, (102, 102))
save(adaptive_foreground, RES / "drawable-nodpi" / "ic_toybox_foreground.png")

# A separate true monochrome silhouette for Android themed/adaptive launchers.
mono_paths = []
for group in ["blocks", "ball"]:
    for part in parts[group]:
        if part["kind"] != "line":
            mono_paths.append(f'<path android:fillColor="#FFFFFFFF" android:pathData="{path_for(part,24/SIZE)}"/>')
mono = ('<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="108dp" android:height="108dp" android:viewportWidth="30" android:viewportHeight="30">\n'
        '<group android:translateX="3" android:translateY="3">\n' + '\n'.join(mono_paths) + '\n</group>\n</vector>\n')
(RES / "drawable" / "ic_toybox_monochrome.xml").write_text(mono)

# A compact contact sheet supports visual review at actual launcher sizes.
sheet = Image.new("RGB", (800, 340), "#F4F6FA")
for side, left, top in [(256, 24, 42), (128, 324, 106), (64, 512, 138), (48, 646, 146)]:
    thumb = composite.resize((side, side), Image.Resampling.LANCZOS)
    sheet.paste(thumb, (left, top), thumb)
save(sheet, SOURCES / "ic_physics_toybox_preview.png")

checks = []
for group, image in layers.items():
    data = np.array(image)
    mask = data[:, :, 3] > 0
    assert not np.any(mask & ~circle_mask), f"{group} artwork outside the inscribed circle"
    assert set(np.unique(data[:, :, 3])).issubset({0, 255})
    checks.append({"layer": group, "size": image.size, "opaque_pixels": int(mask.sum())})
assert np.all(np.array(layers["background"])[circle_mask, 3] == 255)
for path in (RES / "drawable-nodpi").glob("ic_toybox_*.png"):
    assert path.stat().st_size < 4 * 1024 * 1024, path
(SOURCES / "icon-validation.json").write_text(json.dumps({"passed": True, "size": SIZE, "sdf_ramp_px": 32, "layers": checks}, indent=2))
print("Generated original Toybox SVG, launcher PNG, 3 PICO layers/SDFs and Android compatibility assets.")
