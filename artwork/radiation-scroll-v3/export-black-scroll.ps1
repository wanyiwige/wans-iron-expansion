$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.IO;
public static class BlackRadiationScroll {
 static Color[] palette={Color.FromArgb(13,14,13),Color.FromArgb(26,27,26),Color.FromArgb(42,43,42),Color.FromArgb(61,62,61),Color.FromArgb(82,83,82),Color.FromArgb(111,112,111),Color.FromArgb(139,140,139),Color.FromArgb(255,235,0),Color.FromArgb(221,190,0),Color.FromArgb(99,255,0),Color.FromArgb(27,108,0)};
 public static void Export(string source,string output,int size) {
  using(var src=new Bitmap(source))using(var dst=new Bitmap(size,size,PixelFormat.Format32bppArgb)) {
   for(int y=0;y<size;y++)for(int x=0;x<size;x++) {
    Color c=src.GetPixel(Math.Min(src.Width-1,(int)((x+.5)*src.Width/size)),Math.Min(src.Height-1,(int)((y+.5)*src.Height/size)));
    if(c.A<128){dst.SetPixel(x,y,Color.FromArgb(0,0,0,0));continue;}
    Color best=palette[0];double distance=Double.MaxValue;
    foreach(Color p in palette){double r=c.R-p.R,g=c.G-p.G,b=c.B-p.B,d=2*r*r+4*g*g+b*b;if(d<distance){distance=d;best=p;}}
    dst.SetPixel(x,y,best);
   }
   dst.Save(output,ImageFormat.Png);
  }
 }
 static void Draw(Graphics g,string file,int x,int y,int size) {
  using(var src=new Bitmap(file))for(int dy=0;dy<size;dy++)for(int dx=0;dx<size;dx++) {
   Color c=src.GetPixel(dx*src.Width/size,dy*src.Height/size);if(c.A==0)continue;
   using(var brush=new SolidBrush(c))g.FillRectangle(brush,x+dx,y+dy,1,1);
  }
 }
 public static void Preview(string folder,string old) {
  using(var dst=new Bitmap(1000,590,PixelFormat.Format32bppArgb))using(var g=Graphics.FromImage(dst))
  using(var font=new Font("Consolas",12))using(var small=new Font("Consolas",10)) {
   g.Clear(Color.FromArgb(43,43,43));
   string[] files={Path.Combine(folder,"scroll_nature.png"),Path.Combine(folder,"scroll_fire.png"),old,Path.Combine(folder,"scroll_radiation-black-16.png"),Path.Combine(folder,"scroll_radiation-black-32.png")};
   string[] names={"Original nature","Original fire","Previous 64px","New 16px","New 32px"};
   g.DrawString("Full texture (nearest-neighbour enlargement)",font,Brushes.White,15,12);
   for(int i=0;i<files.Length;i++){
    int x=14+i*198;g.DrawString(names[i],small,Brushes.White,x,45);Draw(g,files[i],x,75,176);
    using(var src=new Bitmap(files[i]))using(var native=new Bitmap(16,16,PixelFormat.Format32bppArgb)) {
     for(int y=0;y<16;y++)for(int xx=0;xx<16;xx++)native.SetPixel(xx,y,src.GetPixel(xx*src.Width/16,y*src.Height/16));
     string tmp=Path.Combine(folder,"qa-16-"+i+".png");native.Save(tmp,ImageFormat.Png);
     Draw(g,tmp,x+40,340,96);Draw(g,tmp,x+80,465,16);
    }
   }
   g.DrawString("16px slot simulation, magnified 6x for inspection",font,Brushes.White,15,294);
   g.DrawString("16px at 1x below each column; this is offline raster QA, not an in-game screenshot.",small,Brushes.Silver,15,535);
   dst.Save(Path.Combine(folder,"black-scroll-comparison.png"),ImageFormat.Png);
  }
 }
}
'@
[BlackRadiationScroll]::Export((Join-Path $PSScriptRoot 'imagegen-black-source.png'),(Join-Path $PSScriptRoot 'scroll_radiation-black-16.png'),16)
[BlackRadiationScroll]::Export((Join-Path $PSScriptRoot 'imagegen-black-source.png'),(Join-Path $PSScriptRoot 'scroll_radiation-black-32.png'),32)
$projectRoot=Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
[BlackRadiationScroll]::Preview($PSScriptRoot,(Join-Path $projectRoot 'src/main/resources/assets/alex_caves_spellsbook/textures/item/scroll_toxic.png'))
