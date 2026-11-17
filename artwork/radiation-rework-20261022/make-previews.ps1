$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$root=Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$names=@('isotope_brand','neutron_rebound','moderation_field','lead_curtain','chain_reaction','radiation_film','cloud_chamber_trail','cherenkov_wake','decay_transposition','critical_matrix')
$font=[Drawing.Font]::new('Consolas',9);$brush=[Drawing.Brushes]::Gainsboro
function Graphics([Drawing.Bitmap]$bmp){$g=[Drawing.Graphics]::FromImage($bmp);$g.Clear([Drawing.Color]::FromArgb(23,30,34));$g.InterpolationMode=[Drawing.Drawing2D.InterpolationMode]::NearestNeighbor;$g.PixelOffsetMode=[Drawing.Drawing2D.PixelOffsetMode]::Half;return $g}
$sheet=[Drawing.Bitmap]::new(900,440);$g=Graphics $sheet
for($i=0;$i -lt 10;$i++){
    $x=($i%5)*180;$y=[int][Math]::Floor($i/5)*220;$src=[Drawing.Bitmap]::new((Join-Path $PSScriptRoot ('native-icons/'+$names[$i]+'.png')))
    $g.DrawImage($src,[Drawing.Rectangle]::new($x+18,$y+8,144,144),0,0,32,32,[Drawing.GraphicsUnit]::Pixel)
    $g.DrawImage($src,[Drawing.Rectangle]::new($x+23,$y+166,32,32),0,0,32,32,[Drawing.GraphicsUnit]::Pixel)
    $g.DrawImage($src,[Drawing.Rectangle]::new($x+76,$y+174,16,16),0,0,32,32,[Drawing.GraphicsUnit]::Pixel)
    $g.DrawString(($i+1).ToString('00')+' '+$names[$i],$font,$brush,$x+5,$y+202)
    $src.Dispose()
}
$g.Dispose();$sheet.Save((Join-Path $PSScriptRoot 'preview-icons-native.png'),[Drawing.Imaging.ImageFormat]::Png);$sheet.Dispose()
$vfx=@('isotope','neutron','field','curtain','chain','film','cloud','wake','shell','matrix','impact')
$sheet=[Drawing.Bitmap]::new(570,($vfx.Count*112+28));$g=Graphics $sheet
$g.DrawString('WORLD VFX: four actual native frames / nearest-neighbor preview',$font,$brush,8,5)
for($i=0;$i -lt $vfx.Count;$i++){
    $y=$i*112+28;$src=[Drawing.Bitmap]::new((Join-Path $root ('src/main/resources/assets/alex_caves_spellsbook/textures/entity/toxic/rework/'+$vfx[$i]+'.png')));$s=$src.Width
    $g.DrawString($vfx[$i],$font,$brush,8,$y+41)
    for($f=0;$f -lt 4;$f++){
        $x=112+$f*112
        for($cy=0;$cy -lt 12;$cy++){for($cx=0;$cx -lt 12;$cx++){$color=if(($cx+$cy)%2 -eq 0){[Drawing.Color]::FromArgb(45,54,59)}else{[Drawing.Color]::FromArgb(36,43,48)};$tile=[Drawing.SolidBrush]::new($color);$g.FillRectangle($tile,$x+$cx*8,$y+$cy*8,8,8);$tile.Dispose()}}
        $g.DrawImage($src,[Drawing.Rectangle]::new($x,$y,96,96),0,$f*$s,$s,$s,[Drawing.GraphicsUnit]::Pixel)
    };$src.Dispose()
}
$g.Dispose();$sheet.Save((Join-Path $PSScriptRoot 'preview-vfx-native.png'),[Drawing.Imaging.ImageFormat]::Png);$sheet.Dispose();$font.Dispose()
