"""Hand-set 16px equipment sprites after inspecting vanilla and installed Iron's assets."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import json
root=Path(__file__).resolve().parents[1]
tex=root/'src/main/resources/assets/alex_caves_spellsbook/textures/item'
art=root/'artwork/candy-wizard-finish-20261017';art.mkdir(exist_ok=True)
palette={'.':(0,0,0,0),'o':'#282332','d':'#3A2C48','b':'#534063','m':'#725580','h':'#92729B','p':'#8F4D70','r':'#B57491','s':'#C3A27E','c':'#E4D0AE','w':'#F8E8CA','t':'#568479','g':'#96BCA1','y':'#C79E65'}
sprites={
'hat':[
'................',
'.........ooo....',
'........ombdo...',
'.......omhbdo...',
'......omhbo.....',
'......mhbdo.....',
'.....omhbdo.....',
'.....mbbbddo....',
'....ombbbddo....',
'....prrcpppo....',
'...opgctpppdo...',
'..ombbbbbbdddo..',
'.omhbbbbbbddddo.',
'.occsssssssddo..',
'..oooooodddoo...',
'................'],
'robe':[
'................',
'..ooo......ooo..',
'.omhco....ocbdo.',
'.mhcwbo..obcsdo.',
'.mbcwwbgpbcsddo.',
'.mbbcwrrcsbbddo.',
'.mbbbcwcsbbbddo.',
'.mbdbbcpbbdbddo.',
'.sctobbcbbotcso.',
'..ooobcybd.ooo..',
'.....brpbd......',
'.....bbbbbdo....',
'.....mbbbddo....',
'.....bccssdo....',
'......odddo.....',
'................'],
'leggings':[
'................',
'...ooooooo......',
'..omhrrpppdo....',
'..ombbybppdo....',
'..ombbbbdddo....',
'..omhbbohbdo....',
'..omhboobddo....',
'..ombdo.obdo....',
'..ombdo.obdo....',
'..ombdo.obdo....',
'..ombdo.obdo....',
'..ombdo.obdo....',
'..ocss..csdo....',
'..oddo..dddo....',
'................',
'................'],
'boots':[
'................',
'................',
'...ooo....ooo...',
'...cwso...cso...',
'...mppo...bpo...',
'...mbdo...bdo...',
'...mydo...ydo...',
'...mbdo...bdo...',
'..omhdo...bddo..',
'.omhbdo...bdddo.',
'.mhbbdo...bbddo.',
'.mssddo...ssddo.',
'..oooo.....ooo..',
'................',
'................',
'................']}
names=['糖霜巫师帽','糖霜巫师袍','糖霜巫师长裤','糖霜巫师靴']
sheet=Image.new('RGB',(800,240),'#35343C');d=ImageDraw.Draw(sheet);font=ImageFont.truetype('C:/Windows/Fonts/msyh.ttc',20)
for i,((kind,rows),label) in enumerate(zip(sprites.items(),names)):
 assert len(rows)==16 and all(len(row)==16 for row in rows),(kind,[len(r) for r in rows])
 im=Image.new('RGBA',(16,16))
 for y,row in enumerate(rows):
  for x,key in enumerate(row):im.putpixel((x,y),palette[key] if key=='.' else tuple(bytes.fromhex(palette[key][1:]))+(255,))
 name='candy_wizard_'+kind
 im.save(tex/(name+'.png'));im.save(art/(name+'.png'))
 (tex/(name+'.png.mcmeta')).write_text(json.dumps({'texture':{'blur':False,'clamp':True}}))
 preview=im.resize((160,160),Image.Resampling.NEAREST);sheet.paste(preview,(i*200+20,20),preview);d.text((i*200+35,197),label,font=font,fill='#E4D0AE')
sheet.save(art/'inventory-icons.png')
print('Four native 16x16 equipment icons.')
