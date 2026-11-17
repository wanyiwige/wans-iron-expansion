$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$drawingReferences = if ($PSVersionTable.PSVersion.Major -le 5) { @('System.Drawing') } else { @('System.Drawing.Common','System.Drawing.Primitives','System.Private.Windows.GdiPlus') }
Add-Type -ReferencedAssemblies $drawingReferences -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Imaging;
public static class ToxicScrollExport {
    static Color[] palette = {
        Color.FromArgb(17,18,17), Color.FromArgb(31,32,29),
        Color.FromArgb(44,45,47), Color.FromArgb(71,72,76),
        Color.FromArgb(118,120,124), Color.FromArgb(163,165,165),
        Color.FromArgb(46,48,23), Color.FromArgb(62,65,29),
        Color.FromArgb(78,81,35), Color.FromArgb(94,97,43),
        Color.FromArgb(111,114,52), Color.FromArgb(133,135,65),
        Color.FromArgb(156,158,81), Color.FromArgb(178,180,101),
        Color.FromArgb(225,247,0), Color.FromArgb(207,237,8),
        Color.FromArgb(242,230,0), Color.FromArgb(212,206,5)
    };
    public static void Export(string input, string output, int size) {
        using(var source = new Bitmap(input))
        using(var target = new Bitmap(size,size,PixelFormat.Format32bppArgb)) {
            for(int y=0;y<size;y++) for(int x=0;x<size;x++) {
                int sx=Math.Min(source.Width-1,(int)((x+0.5)*source.Width/size));
                int sy=Math.Min(source.Height-1,(int)((y+0.5)*source.Height/size));
                Color c=source.GetPixel(sx,sy);
                if(c.A<128) { target.SetPixel(x,y,Color.FromArgb(0,0,0,0)); continue; }
                int best=0; double distance=Double.MaxValue;
                for(int i=0;i<palette.Length;i++) {
                    double r=c.R-palette[i].R,g=c.G-palette[i].G,b=c.B-palette[i].B;
                    double d=2*r*r+4*g*g+b*b;
                    if(d<distance) {distance=d;best=i;}
                }
                target.SetPixel(x,y,palette[best]);
            }
            target.Save(output,ImageFormat.Png);
        }
    }
    public static void Preview(string folder) {
        using(var sheet=new Bitmap(800,510,PixelFormat.Format32bppArgb))
        using(var g=Graphics.FromImage(sheet))
        using(var font=new Font("Consolas",12)) {
            g.Clear(Color.FromArgb(36,39,43));
            string[] versions={"scroll_toxic-v1.png","scroll_toxic-32.png","scroll_toxic-64.png"};
            string[] labels={"Original design", "32px / 18-color export", "64px / 18-color export"};
            for(int k=0;k<3;k++) {
                int col=15+k*265;
                g.DrawString(labels[k],font,Brushes.White,col,8);
                using(var src=new Bitmap(System.IO.Path.Combine(folder,versions[k]))) {
                    int[] sizes={16,32,48}; int row=40;
                    foreach(int size in sizes) {
                        g.FillRectangle(Brushes.Gray,col,row,72,72);
                        for(int y=0;y<size;y++) for(int x=0;x<size;x++) {
                            Color c=src.GetPixel(Math.Min(src.Width-1,(int)((x+0.5)*src.Width/size)),Math.Min(src.Height-1,(int)((y+0.5)*src.Height/size)));
                            if(c.A>0) using(var brush=new SolidBrush(c)) g.FillRectangle(brush,col+(72-size)/2+x,row+(72-size)/2+y,1,1);
                        }
                        g.DrawString(size+" screen px",font,Brushes.White,col+82,row+26); row+=84;
                    }
                    // Integer magnification of the native raster, without interpolation.
                    int logical=k==1?32:64;
                    int scale=192/logical;
                    for(int y=0;y<logical;y++) for(int x=0;x<logical;x++) {
                        Color c=src.GetPixel(Math.Min(src.Width-1,(int)((x+0.5)*src.Width/logical)),Math.Min(src.Height-1,(int)((y+0.5)*src.Height/logical)));
                        if(c.A>0) using(var brush=new SolidBrush(c)) g.FillRectangle(brush,col+2+x*scale,298+y*scale,scale,scale);
                    }
                }
            }
            sheet.Save(System.IO.Path.Combine(folder,"inventory-scale-check.png"),ImageFormat.Png);
        }
    }
}
'@
$assetFolder = $PSScriptRoot
$sourceImage = Join-Path $assetFolder 'scroll_toxic-v2-pixel-source.png'
foreach ($textureSize in @(32,64)) {
    [ToxicScrollExport]::Export($sourceImage,(Join-Path $assetFolder "scroll_toxic-$textureSize.png"),$textureSize)
}
[ToxicScrollExport]::Preview($assetFolder)
