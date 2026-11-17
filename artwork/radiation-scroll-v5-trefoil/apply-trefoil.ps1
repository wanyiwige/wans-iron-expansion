$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.IO;
public static class RadiationTrefoilFinish {
 public static void Finish(string source,string master,string output) {
  string[] expected={"..XXX..","..XXX..",".......","...X...","XX...X.","XXX.XXX",".XX.XX."};
  using(var art=new Bitmap(source))using(var stamp=new Bitmap(7,7,PixelFormat.Format32bppArgb))
  using(var input=new Bitmap(master))using(var result=new Bitmap(input)) {
   if(input.Width!=16||input.Height!=16)throw new Exception("Expected native 16px scroll.");
   for(int y=0;y<7;y++)for(int x=0;x<7;x++) {
    Color c=art.GetPixel(Math.Min(art.Width-1,(int)((x+.5)*art.Width/7)),Math.Min(art.Height-1,(int)((y+.5)*art.Height/7)));
    bool solid=c.A>=128;
    if(solid!=(expected[y][x]=='X'))throw new Exception("Generated emblem grid mismatch at "+x+","+y);
    stamp.SetPixel(x,y,solid?Color.FromArgb(255,235,0):Color.FromArgb(0,0,0,0));
   }
   for(int y=5;y<12;y++)for(int x=5;x<12;x++){
    Color old=result.GetPixel(x,y);
    if(old.A>0&&old.R>200&&old.G>150&&old.B<80)result.SetPixel(x,y,Color.FromArgb(26,27,26));
   }
   for(int y=0;y<7;y++)for(int x=0;x<7;x++){
    Color c=stamp.GetPixel(x,y);if(c.A==0)continue;
    Color basePixel=input.GetPixel(x+5,y+5);
    if(basePixel.A==0||(basePixel.G>200&&basePixel.R<150))throw new Exception("Emblem crosses the preserved scroll outline.");
    result.SetPixel(x+5,y+5,c);
   }
   stamp.Save(Path.Combine(output,"trefoil-7.png"),ImageFormat.Png);
   result.Save(Path.Combine(output,"scroll_radiation-black-16.png"),ImageFormat.Png);
  }
 }
}
'@
$previous=Join-Path (Split-Path $PSScriptRoot -Parent) 'radiation-scroll-v4-uniform/scroll_radiation-black-16.png'
[RadiationTrefoilFinish]::Finish((Join-Path $PSScriptRoot 'trefoil-imagegen-source.png'),$previous,$PSScriptRoot)
