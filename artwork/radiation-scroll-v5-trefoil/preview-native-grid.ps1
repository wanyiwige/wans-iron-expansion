$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.IO;
public static class NativeScrollGrid {
 public static void Preview(string folder) {
  using(var source=new Bitmap(Path.Combine(folder,"scroll_radiation-black-16.png")))
  using(var image=new Bitmap(640,384,PixelFormat.Format32bppArgb))using(var g=Graphics.FromImage(image))
  using(var font=new Font("Consolas",11))using(var grid=new Pen(Color.FromArgb(75,118,125,115),1)) {
   if(source.Width!=16||source.Height!=16)throw new Exception("Expected the native 16px master.");
   g.Clear(Color.FromArgb(41,44,42));
   g.DrawString("Actual 16x16 master / 16x view",font,Brushes.White,16,12);
   g.DrawString("Same image + source-pixel grid",font,Brushes.White,326,12);
   for(int copy=0;copy<2;copy++)for(int y=0;y<16;y++)for(int x=0;x<16;x++) {
    Color c=source.GetPixel(x,y);if(c.A==0)continue;
    using(var brush=new SolidBrush(c))g.FillRectangle(brush,16+copy*310+x*16,48+y*16,16,16);
   }
   for(int i=0;i<=16;i++){
    g.DrawLine(grid,326+i*16,48,326+i*16,304);g.DrawLine(grid,326,48+i*16,582,48+i*16);
   }
   g.DrawString("Yellow, green and grey all use ONE source-pixel lattice.",font,Brushes.White,16,326);
   image.Save(Path.Combine(folder,"native-16-grid-proof.png"),ImageFormat.Png);
  }
 }
}
'@
[NativeScrollGrid]::Preview($PSScriptRoot)
