$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.IO;
public static class ToxicAtlasExport {
    static Color[] palette={
        Color.FromArgb(16,20,15),Color.FromArgb(28,34,24),Color.FromArgb(43,47,31),
        Color.FromArgb(58,66,31),Color.FromArgb(76,86,35),Color.FromArgb(99,113,41),
        Color.FromArgb(125,146,43),Color.FromArgb(150,181,36),Color.FromArgb(178,215,27),
        Color.FromArgb(203,238,17),Color.FromArgb(224,250,35),Color.FromArgb(245,255,125),
        Color.FromArgb(255,255,213),Color.FromArgb(239,213,22),Color.FromArgb(185,161,27),
        Color.FromArgb(31,33,34),Color.FromArgb(53,56,57),Color.FromArgb(79,84,83),
        Color.FromArgb(110,116,112),Color.FromArgb(145,151,143),Color.FromArgb(185,190,173),
        Color.FromArgb(64,68,45),Color.FromArgb(109,116,69),Color.FromArgb(153,164,93)
    };
    static Color Nearest(Color c) {
        int best=0;double distance=Double.MaxValue;
        for(int i=0;i<palette.Length;i++) {double r=c.R-palette[i].R,g=c.G-palette[i].G,b=c.B-palette[i].B;double d=2*r*r+4*g*g+b*b;if(d<distance){distance=d;best=i;}}
        return palette[best];
    }
    public static void Run(string input,string resources,string artwork) {
        string[] names={"gamma_jet","waste_drum_cast","raycat_purification","proton_lance","critical_fission","reactor_ward",
            "radiation_mark","spark","puddle","splat","shield_panel","burst","beam_segment","pulse","smoke","cleanse"};
        using(var source=new Bitmap(input)) {
            for(int tile=0;tile<16;tile++) {
                bool icon=tile<6;int size=icon?64:32;
                string folder=Path.Combine(resources,icon?"textures/gui/spell_icons":"textures/entity/toxic");Directory.CreateDirectory(folder);
                using(var image=new Bitmap(size,size,PixelFormat.Format32bppArgb)) {
                    for(int y=0;y<size;y++)for(int x=0;x<size;x++) {
                        int sx=Math.Min(source.Width-1,(int)(((tile%4)+(x+.5)/size)*source.Width/4));
                        int sy=Math.Min(source.Height-1,(int)(((tile/4)+(y+.5)/size)*source.Height/4));
                        Color c=source.GetPixel(sx,sy);
                        image.SetPixel(x,y,c.A<128?(icon?Color.FromArgb(22,29,20):Color.FromArgb(0,0,0,0)):Nearest(c));
                    }
                    image.Save(Path.Combine(folder,names[tile]+".png"),ImageFormat.Png);
                }
                File.WriteAllText(Path.Combine(folder,names[tile]+".png.mcmeta"),"{\"texture\":{\"blur\":false,\"clamp\":false}}");
            }
        }
        using(var sheet=new Bitmap(576,384,PixelFormat.Format32bppArgb))using(var g=Graphics.FromImage(sheet)) {
            g.Clear(Color.FromArgb(22,29,20));
            for(int tile=0;tile<6;tile++)using(var icon=new Bitmap(Path.Combine(resources,"textures/gui/spell_icons",names[tile]+".png")))
                for(int y=0;y<64;y++)for(int x=0;x<64;x++)using(var b=new SolidBrush(icon.GetPixel(x,y)))g.FillRectangle(b,(tile%3)*192+x*3,(tile/3)*192+y*3,3,3);
            sheet.Save(Path.Combine(artwork,"opaque-spell-icons-preview.png"),ImageFormat.Png);
        }
    }
}
'@
$projectRoot=Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
[ToxicAtlasExport]::Run((Join-Path $PSScriptRoot 'toxic-atlas-imagegen.png'),(Join-Path $projectRoot 'src/main/resources/assets/alex_caves_spellsbook'),$PSScriptRoot)
