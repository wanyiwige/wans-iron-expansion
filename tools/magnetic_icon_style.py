"""Shared opaque, square pixel panel for the magnetic spell icon generators."""
from PIL import Image, ImageDraw

def canvas():
    image = Image.new('RGBA', (16, 16), (0, 0, 0, 255))
    for y in range(16):
        for x in range(16):
            red = max(0, 1 - x / 12)
            blue = max(0, (x - 3) / 12)
            glow = max(0, 1 - ((x - 7.5)**2 + (y - 7.5)**2) / 100)
            image.putpixel((x, y), (int(22 + 30*red + 7*glow),
                                    int(25 + 8*red + 10*blue + 8*glow),
                                    int(39 + 7*red + 35*blue + 9*glow), 255))
    draw = ImageDraw.Draw(image)
    draw.rectangle((0,0,15,15), outline='#34445f')
    draw.line((1,0,14,0), fill='#6d829e')
    draw.line((1,15,14,15), fill='#202b43')
    draw.line((0,1,0,14), fill='#703c56')
    draw.line((15,1,15,14), fill='#305a87')
    draw.line((1,3,1,6), fill='#723347')
    draw.line((14,9,14,12), fill='#265280')
    return image
