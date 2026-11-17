"""Render generated spell illustrations on the unchanged approved magnetic panel."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import json
from magnetic_icon_style import canvas

ROOT=Path(__file__).resolve().parents[1]
ART=ROOT/'tools/magnetic-icon-art'
ASSETS=ROOT/'src/main/resources/assets'
OWN=['magnetize','scarlet_attraction','azure_repulsion','teletor_armament','magnetic_levitation',
     'notor_scan','boundroid_drop','ferrouslime_cohesion','tesla_arc_node','dipole_vortex',
     'polarity_collapse','magnetic_rail_lance','dipole_anvil','magnetized_fracture_chain','polarity_reversal','ferrous_sandstorm']
EXTERNAL=['em_pulse','magnetron_deployment','galena_shatter']
SPELLS=[('alex_caves_spellsbook',sid) for sid in OWN]+[('traveloptics',sid) for sid in EXTERNAL]

def art_available():
    return all((ART/(sid+'.png')).exists() for ns,sid in SPELLS)

def render_icons():
    assert art_available(),'Complete the 19 generated subject masters before rendering'
    for ns,sid in SPELLS:
        art=Image.open(ART/(sid+'.png')).convert('RGBA')
        assert art.size==(32,32)
        panel=canvas().resize((64,64),Image.Resampling.NEAREST)
        panel.alpha_composite(art.resize((64,64),Image.Resampling.NEAREST))
        path=ASSETS/ns/'textures/gui/spell_icons'/(sid+'.png')
        path.parent.mkdir(parents=True,exist_ok=True);panel.save(path)
        aliases=[]
        if ns=='traveloptics':aliases.append(ASSETS/'alex_caves_spellsbook/textures/gui/spell_icons'/(sid+'.png'))
        if sid in OWN[10:]:aliases.append(ASSETS/'alex_caves_spellsbook/textures/spell_icons'/(sid+'.png'))
        for alias in aliases:alias.parent.mkdir(parents=True,exist_ok=True);alias.write_bytes(path.read_bytes())
    print('Rendered all 19 illustrated icons and 9 compatibility aliases on the approved opaque panel.')

def preview():
    lang=json.loads((ASSETS/'alex_caves_spellsbook/lang/zh_cn.json').read_text(encoding='utf-8'))
    sheet=Image.new('RGB',(1000,680),'#161f30');draw=ImageDraw.Draw(sheet)
    font=ImageFont.truetype('C:/Windows/Fonts/msyh.ttc',16)
    draw.text((20,10),'磁极学派 · 参照铁魔法本体风格重绘主体',font=font,fill='white')
    for i,(ns,sid) in enumerate(SPELLS):
        x=20+(i%5)*196;y=44+(i//5)*155
        image=Image.open(ASSETS/ns/'textures/gui/spell_icons'/(sid+'.png')).convert('RGB')
        sheet.paste(image.resize((112,112),Image.Resampling.NEAREST),(x+30,y))
        name=lang.get('spell.'+ns+'.'+sid,lang.get('spell.alex_caves_spellsbook.'+sid,sid))
        draw.text((x,y+115),name,font=font,fill='#e4ecfa')
    out=ROOT/'development-checks/magnetic-icon-redraw/redrawn-icons-preview.png'
    out.parent.mkdir(parents=True,exist_ok=True);sheet.save(out)

if __name__=='__main__':render_icons();preview()
