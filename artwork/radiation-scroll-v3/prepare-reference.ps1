$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System.Drawing;
using System.Drawing.Imaging;
public static class ScrollReferenceScale {
 public static void Enlarge(string input,string output,int factor) {
  using(var src=new Bitmap(input))using(var dst=new Bitmap(src.Width*factor,src.Height*factor,PixelFormat.Format32bppArgb)) {
   for(int y=0;y<dst.Height;y++)for(int x=0;x<dst.Width;x++)dst.SetPixel(x,y,src.GetPixel(x/factor,y/factor));
   dst.Save(output,ImageFormat.Png);
  }
 }
}
'@
[ScrollReferenceScale]::Enlarge((Join-Path $PSScriptRoot 'scroll_nature.png'),(Join-Path $PSScriptRoot 'reference-nature-512.png'),32)
[ScrollReferenceScale]::Enlarge((Join-Path $PSScriptRoot 'scroll_fire.png'),(Join-Path $PSScriptRoot 'reference-fire-512.png'),32)
$projectRoot=Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
[ScrollReferenceScale]::Enlarge((Join-Path $projectRoot 'src/main/resources/assets/alex_caves_spellsbook/textures/item/scroll_toxic.png'),(Join-Path $PSScriptRoot 'previous-radiation-512.png'),8)
