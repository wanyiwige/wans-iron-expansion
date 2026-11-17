Add-Type -AssemblyName System.Drawing
$source = Join-Path $PSScriptRoot 'spell_icons_imagegen-source.png'
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$iconDir = Join-Path $root 'src\main\resources\assets\alex_caves_spellsbook\textures\gui\spell_icons'
$effectDir = Join-Path $root 'src\main\resources\assets\alex_caves_spellsbook\textures\entity\toxic'
$names = @('isotope_brand','neutron_rebound','moderation_field','lead_curtain','chain_reaction',
           'radiation_film','cloud_chamber_trail','cherenkov_wake','decay_transposition','critical_matrix')
$effects = @('isotope','neutron','field','curtain','chain','film','cloud','wake','shell','matrix')
$palette = @('#12151A','#20262A','#343C40','#566065','#7D8789','#AAB4B4','#D9E1DA',
             '#102C19','#1B511E','#318525','#51BC31','#86ED46','#CEFC64',
             '#71510D','#B99617','#FFF018','#F2FFBC','#123766','#167AA8','#39C5E8','#9AF6FF')
$colors = @()
foreach($hex in $palette){$colors += [System.Drawing.ColorTranslator]::FromHtml($hex)}
function Nearest([System.Drawing.Color]$c) {
    $best=$colors[0];$distance=[int]::MaxValue
    foreach($p in $colors) {
        $d=[int](($c.R-$p.R)*($c.R-$p.R)+($c.G-$p.G)*($c.G-$p.G)+($c.B-$p.B)*($c.B-$p.B))
        if($d -lt $distance){$distance=$d;$best=$p}
    }
    return $best
}
$src=[System.Drawing.Bitmap]::new($source)
for($i=0;$i -lt 10;$i++){
    $icon=[System.Drawing.Bitmap]::new(64,64,[System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $vfx=[System.Drawing.Bitmap]::new(32,32,[System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $col=$i%4;$row=[int][Math]::Floor($i/4)
    for($y=0;$y -lt 32;$y++){for($x=0;$x -lt 32;$x++){
        $sx=[int][Math]::Floor(($col+($x+0.5)/32.0)*$src.Width/4)
        $sy=[int][Math]::Floor(($row+($y+0.5)/32.0)*$src.Height/4)
        $original=$src.GetPixel($sx,$sy);$q=Nearest $original
        for($dy=0;$dy -lt 2;$dy++){for($dx=0;$dx -lt 2;$dx++){
            $icon.SetPixel($x*2+$dx,$y*2+$dy,$q)
        }}
        if([Math]::Max($original.R,[Math]::Max($original.G,$original.B)) -lt 52){
            $vfx.SetPixel($x,$y,[System.Drawing.Color]::Transparent)
        }else{
            $vfx.SetPixel($x,$y,$q)
        }
    }}
    $icon.Save((Join-Path $iconDir ($names[$i]+'.png')),[System.Drawing.Imaging.ImageFormat]::Png)
    $vfx.Save((Join-Path $effectDir ($effects[$i]+'.png')),[System.Drawing.Imaging.ImageFormat]::Png)
    $icon.Dispose();$vfx.Dispose()
}
$src.Dispose()
Copy-Item (Join-Path $PSScriptRoot 'lead_curtain_metal-64.png') (Join-Path $effectDir 'lead_curtain_metal.png') -Force
Write-Output 'Exported 10 opaque 64px icons and 10 transparent 32px effect sprites.'
