$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.IO;
public static class ToxicEmitterTiles {
 public static void Export(string source,string output) {
  string[] names={"emitter_shell","emitter_frame","emitter_cell"};
  int[] tiles={0,2,3};
  using(var src=new Bitmap(source))for(int i=0;i<3;i++)using(var dst=new Bitmap(128,128,PixelFormat.Format32bppArgb)) {
   int tile=tiles[i];
   for(int y=0;y<128;y++)for(int x=0;x<128;x++) {
    int sx=Math.Min(src.Width-1,(int)(((tile%2)+(x+.5)/128)*src.Width/2));
    int sy=Math.Min(src.Height-1,(int)(((tile/2)+(y+.5)/128)*src.Height/2));
    Color c=src.GetPixel(sx,sy);dst.SetPixel(x,y,Color.FromArgb(255,c.R,c.G,c.B));
   }
   dst.Save(Path.Combine(output,names[i]+".png"),ImageFormat.Png);
  }
 }
}
'@
$projectRoot=Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$destination=Join-Path $projectRoot 'src/main/resources/assets/alex_caves_spellsbook/textures/entity/toxic'
[ToxicEmitterTiles]::Export((Join-Path $PSScriptRoot 'industrial-materials-imagegen.png'),$destination)
