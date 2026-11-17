const fs=require('node:fs');
const path=require('node:path');
const sharp=require('sharp');
const output=path.resolve(__dirname,'../src/main/resources/assets/alex_caves_spellsbook/textures/models/armor');
const colors={navy:[35,45,68,255],iron:[77,99,112,255],dark:[17,26,42,255],gold:[214,176,101,255],red:[194,55,75,255],blue:[39,127,191,255],cyan:[110,213,232,255]};
const draw=(layer)=>{
 const px=Buffer.alloc(64*32*4);
 const put=(x,y,c)=>{if(x<0||y<0||x>=64||y>=32)return;let i=(y*64+x)*4;px.set(c,i)};
 const rect=(x,y,w,h,c)=>{for(let iy=y;iy<y+h;iy++)for(let ix=x;ix<x+w;ix++)put(ix,iy,c)};
 rect(0,0,64,32,colors.navy);
 for(let y=0;y<32;y+=4)for(let x=(y/4)%2;x<64;x+=6)rect(x,y,1,1,colors.iron);
 rect(0,0,32,1,colors.gold);rect(0,15,32,1,colors.dark);
 rect(16,16,24,1,colors.gold);rect(16,31,24,1,colors.dark);
 rect(40,16,8,16,colors.iron);rect(48,16,8,16,colors.iron);
 rect(17,17,2,14,colors.red);rect(37,17,2,14,colors.blue);
 rect(42,19,2,10,colors.red);rect(52,19,2,10,colors.blue);
 if(layer===2){rect(0,16,16,16,colors.iron);rect(16,16,16,16,colors.iron);rect(4,17,3,14,colors.red);rect(25,17,3,14,colors.blue)}
 return sharp(px,{raw:{width:64,height:32,channels:4}}).png().toBuffer();
};
(async()=>{fs.mkdirSync(output,{recursive:true});for(const i of [1,2])fs.writeFileSync(path.join(output,`magnetic_mage_layer_${i}.png`),await draw(i));})();
