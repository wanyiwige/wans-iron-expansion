$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.IO;
public static class RadiationEffectPreview {
 public static void Make(string folder,string output) {
  string[] names={"smoke","splat","puddle","spark","cleanse","beam_segment","pulse","burst","shield_panel"};
  string[] labels={"烟雾","飞溅","废液液面","能量火花","净化旋纹","光束外层","脉冲环","命中爆闪","屏障面片"};
  string[] uses={"伽马喷流","喷流 / 投桶 / 屏障破裂","废料桶投掷","多个法术共用","射线净化","质子贯穿","贯穿 / 裂变 / 屏障释放","贯穿 / 临界裂变","反应堆屏障"};
  using(var sheet=new Bitmap(900,1020,PixelFormat.Format32bppArgb))using(var g=Graphics.FromImage(sheet))
  using(var title=new Font("Microsoft YaHei UI",16,FontStyle.Bold))
  using(var main=new Font("Microsoft YaHei UI",13,FontStyle.Bold))
  using(var small=new Font("Microsoft YaHei UI",10))
  using(var mono=new Font("Consolas",11))
  using(var fg=new SolidBrush(Color.FromArgb(232,238,224)))
  using(var muted=new SolidBrush(Color.FromArgb(164,179,158)))
  using(var dark=new SolidBrush(Color.FromArgb(36,41,36)))
  using(var light=new SolidBrush(Color.FromArgb(49,55,48))) {
   g.Clear(Color.FromArgb(19,24,20));
   g.DrawString("辐射学派 · 法术特效贴图",title,fg,24,16);
   g.DrawString("原图 32 × 32 · 每个像素放大 6 倍 · 方格表示透明区域",small,muted,25,53);
   for(int i=0;i<names.Length;i++) {
    int x=(i%3)*300+24,y=(i/3)*306+92;
    for(int cy=0;cy<192;cy+=12)for(int cx=0;cx<192;cx+=12)
     g.FillRectangle(((cx/12+cy/12)%2==0)?dark:light,x+28+cx,y+cy,12,12);
    using(var src=new Bitmap(Path.Combine(folder,names[i]+".png"))) {
     if(src.Width!=32||src.Height!=32)throw new Exception(names[i]+" is not 32px");
     for(int sy=0;sy<32;sy++)for(int sx=0;sx<32;sx++) {
      Color c=src.GetPixel(sx,sy);if(c.A==0)continue;
      using(var brush=new SolidBrush(c))g.FillRectangle(brush,x+28+sx*6,y+sy*6,6,6);
     }
    }
    g.DrawString((i+1)+". "+labels[i],main,fg,x,y+204);
    g.DrawString(names[i]+".png",mono,muted,x,y+232);
    g.DrawString(uses[i],small,muted,x,y+256);
   }
   sheet.Save(output,ImageFormat.Png);
  }
 }
}
'@
$projectRoot=Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$folder=Join-Path $projectRoot 'src/main/resources/assets/alex_caves_spellsbook/textures/entity/toxic'
[RadiationEffectPreview]::Make($folder,(Join-Path $PSScriptRoot 'radiation-effect-textures-preview.png'))
