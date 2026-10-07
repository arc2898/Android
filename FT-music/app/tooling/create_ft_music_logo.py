from pathlib import Path
from PIL import Image, ImageDraw

SIZE = 1024
CHARCOAL = (17, 21, 19, 255)
CHARCOAL_DEEP = (11, 14, 12, 255)
PISTACHIO = (191, 233, 141, 255)
PISTACHIO_LIGHT = (225, 247, 182, 255)

image = Image.new('RGBA', (SIZE, SIZE), CHARCOAL_DEEP)
draw = ImageDraw.Draw(image)

# Rounded charcoal tile with a quiet inner border.
draw.rounded_rectangle((24, 24, SIZE - 24, SIZE - 24), radius=240, fill=CHARCOAL)
draw.rounded_rectangle(
    (42, 42, SIZE - 42, SIZE - 42),
    radius=222,
    outline=(191, 233, 141, 70),
    width=8,
)

# Abstract FT monogram: two pistachio bars and a musical stem.
bar_width = 70
for y, end in ((300, 620), (450, 550), (600, 490)):
    draw.rounded_rectangle(
        (245, y, end, y + bar_width), radius=bar_width // 2, fill=PISTACHIO
    )
draw.rounded_rectangle((570, 270, 640, 760), radius=35, fill=PISTACHIO_LIGHT)
draw.rounded_rectangle((570, 270, 800, 340), radius=35, fill=PISTACHIO_LIGHT)
# Note head and a small negative-space cut create the music identity.
draw.ellipse((690, 650, 830, 790), fill=PISTACHIO)
draw.ellipse((724, 650, 810, 736), fill=CHARCOAL)

assets = Path('assets/icons')
assets.mkdir(parents=True, exist_ok=True)
image.save(assets / 'ft_music_mark.png')
image.resize((512, 512), Image.Resampling.LANCZOS).save(assets / 'ft_music_lighter.png')

# Generate legacy Android launcher bitmaps from the same source of truth.
res = Path('android/app/src/main/res')
for density, size in {
    'mdpi': 48,
    'hdpi': 72,
    'xhdpi': 96,
    'xxhdpi': 144,
    'xxxhdpi': 192,
}.items():
    resized = image.resize((size, size), Image.Resampling.LANCZOS)
    target = res / f'mipmap-{density}'
    target.mkdir(parents=True, exist_ok=True)
    resized.save(target / 'ic_launcher.png')
    resized.save(target / 'launcher_icon.png')
