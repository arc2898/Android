from PIL import Image, ImageDraw

size = 512
image = Image.new("RGBA", (size, size), (11, 16, 32, 255))
draw = ImageDraw.Draw(image)
# Rounded dark tile.
draw.rounded_rectangle((0, 0, size - 1, size - 1), radius=112, fill=(11, 16, 32, 255))
# Violet F strokes.
for y, end in ((166, 304), (256, 260), (346, 240)):
    draw.line((128, y, end, y), fill=(124, 58, 237, 255), width=42)
# White T stem and top; cyan lower accent.
draw.line((350, 150, 350, 362), fill=(248, 250, 252, 255), width=30)
draw.line((350, 150, 406, 150), fill=(248, 250, 252, 255), width=30)
draw.line((350, 238, 392, 238), fill=(248, 250, 252, 255), width=30)
draw.line((422, 286, 422, 362), fill=(34, 211, 238, 255), width=22)
image.save("assets/icons/ft_music_mark.png")
