$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.IO;
public static class ToxicFaceTiles {
 public static void Export(string source,string output) {
  string[] names={"waste_front","waste_back","waste_left","waste_right","waste_top","waste_bottom"};
  using(var src=new Bitmap(source))for(int tile=0;tile<6;tile++)using(var dst=new Bitmap(128,128,PixelFormat.Format32bppArgb)) {
   for(int y=0;y<128;y++)for(int x=0;x<128;x++) {
    int sx=Math.Min(src.Width-1,(int)(((tile%3)+(x+.5)/128)*src.Width/3));
    int sy=Math.Min(src.Height-1,(int)(((tile/3)+(y+.5)/128)*src.Height/2));
    Color c=src.GetPixel(sx,sy);
    dst.SetPixel(x,y,Color.FromArgb(255,c.R,c.G,c.B));
   }
   dst.Save(Path.Combine(output,names[tile]+".png"),ImageFormat.Png);
  }
 }
}
'@
$projectRoot=Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$destination=Join-Path $projectRoot 'src/main/resources/assets/alex_caves_spellsbook/textures/entity/toxic'
[ToxicFaceTiles]::Export((Join-Path $PSScriptRoot 'waste-six-faces-imagegen.png'),$destination)
