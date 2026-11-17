from pathlib import Path
from PIL import Image, ImageDraw
from magnetic_icon_style import canvas as magnetic_canvas
import shutil

OUT = Path(__file__).parents[1] / "src/main/resources/assets/alex_caves_spellsbook/textures/gui/spell_icons"
S = 4
C = {
    "dark": "#111827", "metal": "#596273", "light": "#d9e2ef", "white": "#f8fbff",
    "red": "#ee173b", "red2": "#8f1026", "blue": "#1689ff", "blue2": "#123ba8",
    "cyan": "#61d9ff", "pink": "#ff6a82", "green": "#62c987"
}


def canvas():
    image = magnetic_canvas()
    return image, ImageDraw.Draw(image)


def px(draw, xy, color):
    draw.rectangle(xy, fill=C[color] if color in C else color)


def line(draw, points, color, width=1):
    draw.line(points, fill=C[color], width=width)


def save(name, image):
    OUT.mkdir(parents=True, exist_ok=True)
    path = OUT / f"{name}.png"
    image.resize((64, 64), Image.Resampling.NEAREST).save(path)
    if name in {"em_pulse", "magnetron_deployment", "galena_shatter"}:
        compat = OUT.parents[3] / "traveloptics/textures/gui/spell_icons" / path.name
        compat.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(path, compat)


def magnetize():
    im, d = canvas(); d.ellipse((5, 4, 10, 11), fill=C["metal"], outline=C["dark"])
    px(d, (7, 5, 8, 8), "light"); d.arc((1, 2, 14, 14), 115, 255, fill=C["red"], width=2)
    d.arc((1, 2, 14, 14), 295, 75, fill=C["blue"], width=2); px(d, (2, 10, 3, 11), "pink"); px(d, (12, 4, 13, 5), "cyan")
    save("magnetize", im)


def pole(name, color, attraction):
    im, d = canvas(); d.rounded_rectangle((3, 2, 12, 12), 2, fill=C["dark"])
    d.rectangle((5, 3, 10, 10), fill=C[color]); d.rectangle((7, 5, 10, 8), fill=C["dark"])
    d.rectangle((5, 9, 10, 11), fill=C["light"])
    if attraction:
        line(d, [(1, 7), (4, 7)], "red", 2); line(d, [(14, 7), (11, 7)], "red", 2); px(d, (3, 6, 4, 8), "pink"); px(d, (11, 6, 12, 8), "pink")
    else:
        line(d, [(4, 7), (1, 7)], "blue", 2); line(d, [(11, 7), (14, 7)], "blue", 2); px(d, (0, 6, 1, 8), "cyan"); px(d, (14, 6, 15, 8), "cyan")
    save(name, im)


def em_pulse():
    im, d = canvas(); d.ellipse((5, 5, 10, 10), fill=C["white"], outline=C["dark"])
    d.arc((2, 2, 13, 13), 90, 270, fill=C["red"], width=2); d.arc((2, 2, 13, 13), 270, 90, fill=C["blue"], width=2)
    d.arc((0, 0, 15, 15), 105, 255, fill=C["red2"]); d.arc((0, 0, 15, 15), 285, 75, fill=C["blue2"])
    save("em_pulse", im)


def ferric_recall():
    im, d = canvas(); d.polygon([(5,6),(10,4),(13,7),(8,10)], fill=C["metal"], outline=C["dark"]); line(d, [(6,6),(10,5),(12,7)], "white")
    for pts, col in [([(1,2),(4,5)],"red"), ([(14,2),(11,5)],"blue"), ([(1,13),(5,10)],"red"), ([(14,13),(10,10)],"blue")]: line(d, pts, col, 2)
    save("ferric_recall", im)


def seeking_barrage():
    im, d = canvas()
    for off, col in [(0,"red"),(3,"blue"),(6,"red")]:
        line(d, [(2,12-off),(10,4-off)], col, 2); d.polygon([(9,2-off),(14,1-off),(13,6-off)], fill=C["light"], outline=C["dark"])
    save("seeking_barrage", im)


def armament():
    im, d = canvas(); d.polygon([(7,1),(10,4),(8,10),(6,10),(5,4)], fill=C["light"], outline=C["dark"])
    d.rectangle((4,10,10,11), fill=C["metal"], outline=C["dark"]); d.rectangle((7,12,8,14), fill=C["red2"])
    d.arc((1,2,14,14), 105,250,fill=C["red"],width=2); d.arc((1,2,14,14),290,75,fill=C["blue"],width=2); save("teletor_armament",im)


def levitation():
    im,d=canvas(); d.ellipse((6,3,9,6),fill=C["light"],outline=C["dark"]); d.polygon([(5,7),(10,7),(9,12),(6,12)],fill=C["metal"],outline=C["dark"])
    d.rectangle((2,12,5,13),fill=C["blue"]); d.rectangle((10,1,13,2),fill=C["red"]); line(d,[(4,11),(11,3)],"cyan"); line(d,[(5,12),(12,4)],"pink"); save("magnetic_levitation",im)


def scan():
    im,d=canvas(); d.polygon([(2,8),(5,5),(10,5),(13,8),(10,11),(5,11)],fill=C["metal"],outline=C["dark"]); d.ellipse((6,6,9,9),fill=C["cyan"])
    for box,col in [((1,2,4,2),"red"),((1,2,1,5),"red"),((11,13,14,13),"blue"),((14,10,14,13),"blue")]: line(d,[(box[0],box[1]),(box[2],box[3])],col)
    save("notor_scan",im)


def boundroid():
    im,d=canvas(); d.polygon([(4,6),(11,6),(13,12),(2,12)],fill=C["metal"],outline=C["dark"]); d.rectangle((6,3,9,6),fill=C["light"],outline=C["dark"])
    line(d,[(7,0),(7,4)],"red",2); line(d,[(9,0),(9,4)],"blue",2); d.polygon([(6,3),(10,3),(8,5)],fill=C["dark"]); save("boundroid_drop",im)


def slime():
    im,d=canvas(); d.ellipse((1,6,8,13),fill=C["green"],outline=C["dark"]); d.ellipse((8,4,14,11),fill=C["metal"],outline=C["dark"]); px(d,(4,8,4,8),"red"); px(d,(11,6,11,6),"blue"); line(d,[(7,8),(9,8)],"white",2); save("ferrouslime_cohesion",im)


def tesla():
    im,d=canvas(); d.ellipse((5,5,10,10),fill=C["white"],outline=C["dark"]); px(d,(6,6,7,9),"red"); px(d,(8,6,9,9),"blue")
    line(d,[(0,3),(4,5),(2,8)],"red",2); line(d,[(15,3),(11,5),(13,8)],"blue",2); line(d,[(3,14),(6,11)],"red"); line(d,[(12,14),(9,11)],"blue"); save("tesla_arc_node",im)


def vortex():
    im,d=canvas(); d.ellipse((2,4,7,9),fill=C["red"],outline=C["dark"]); d.ellipse((8,7,13,12),fill=C["blue"],outline=C["dark"])
    d.arc((1,1,14,14),190,345,fill=C["cyan"],width=2); d.arc((1,1,14,14),10,165,fill=C["pink"],width=2); save("dipole_vortex",im)


def magnetron():
    im,d=canvas(); d.polygon([(3,4),(5,2),(11,2),(13,4),(13,11),(10,14),(5,14),(2,11)],fill=C["metal"],outline=C["dark"])
    d.rectangle((4,5,11,10),fill=C["dark"]); px(d,(5,6,7,8),"red"); px(d,(9,6,11,8),"blue"); d.rectangle((6,11,9,12),fill=C["light"]); save("magnetron_deployment",im)


def galena():
    im,d=canvas(); d.polygon([(3,3),(8,1),(13,4),(12,11),(7,14),(2,10)],fill=C["metal"],outline=C["dark"])
    line(d,[(8,2),(7,6),(5,8),(7,13)],"white",2); line(d,[(7,6),(3,5)],"red",2); line(d,[(6,8),(11,10)],"blue",2); save("galena_shatter",im)


if __name__ == "__main__":
    from render_magnetic_icons import art_available, render_icons
    if art_available():
        render_icons()
    else:
        magnetize(); pole("scarlet_attraction","red",True); pole("azure_repulsion","blue",False); em_pulse()
        armament(); levitation(); scan(); boundroid(); slime(); tesla(); vortex(); magnetron(); galena()
