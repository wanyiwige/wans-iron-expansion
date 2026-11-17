$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$paletteHex=@('0C1321','182138','2C2D49','4B4967','163D48','16575C','107D71','11A580','29C96C','62E54C','A5EF4C','E6F583','7BBB9C','35566A','B8DBC1','C3D944')
$palette=@($paletteHex | ForEach-Object {[System.Drawing.ColorTranslator]::FromHtml('#'+$_)})
$specs=@(
    @{Name='reactor-gauntlet';Width=36;Height=21},
    @{Name='fission-staff';Width=37;Height=37},
    @{Name='radiation-grimoire';Width=18;Height=17}
)
$report=@()
foreach($spec in $specs){
    $source=New-Object System.Drawing.Bitmap (Join-Path $PSScriptRoot ('sources/'+$spec.Name+'.png'))
    $minX=$source.Width;$minY=$source.Height;$maxX=-1;$maxY=-1
    for($y=0;$y -lt $source.Height;$y++){
        for($x=0;$x -lt $source.Width;$x++){
            if($source.GetPixel($x,$y).A -ge 128){
                $minX=[Math]::Min($minX,$x);$maxX=[Math]::Max($maxX,$x)
                $minY=[Math]::Min($minY,$y);$maxY=[Math]::Max($maxY,$y)
            }
        }
    }
    if($maxX -lt 0){throw 'Empty source image'}
    $sourceWidth=$maxX-$minX+1;$sourceHeight=$maxY-$minY+1
    $fit=[Math]::Min(($spec.Width-2)/$sourceWidth,($spec.Height-2)/$sourceHeight)
    $drawWidth=[Math]::Max(1,[int][Math]::Round($sourceWidth*$fit))
    $drawHeight=[Math]::Max(1,[int][Math]::Round($sourceHeight*$fit))
    $offsetX=[int][Math]::Floor(($spec.Width-$drawWidth)/2)
    $offsetY=[int][Math]::Floor(($spec.Height-$drawHeight)/2)
    $native=New-Object System.Drawing.Bitmap $spec.Width,$spec.Height
    for($y=0;$y -lt $drawHeight;$y++){
        for($x=0;$x -lt $drawWidth;$x++){
            $sx=$minX+[int][Math]::Floor(($x+.5)*$sourceWidth/$drawWidth)
            $sy=$minY+[int][Math]::Floor(($y+.5)*$sourceHeight/$drawHeight)
            $color=$source.GetPixel([Math]::Min($maxX,$sx),[Math]::Min($maxY,$sy))
            if($color.A -lt 128){continue}
            $best=$palette[0];$distance=[double]::MaxValue
            foreach($candidate in $palette){
                $dr=[int]$color.R-[int]$candidate.R;$dg=[int]$color.G-[int]$candidate.G;$db=[int]$color.B-[int]$candidate.B
                $score=$dr*$dr+$dg*$dg+$db*$db
                if($score -lt $distance){$distance=$score;$best=$candidate}
            }
            $native.SetPixel($offsetX+$x,$offsetY+$y,$best)
        }
    }
    $native.Save((Join-Path $PSScriptRoot ('native/'+$spec.Name+'.png')),[System.Drawing.Imaging.ImageFormat]::Png)
    $display=New-Object System.Drawing.Bitmap ($spec.Width*2),($spec.Height*2)
    for($y=0;$y -lt $spec.Height;$y++){
        for($x=0;$x -lt $spec.Width;$x++){
            $color=$native.GetPixel($x,$y)
            for($dy=0;$dy -lt 2;$dy++){for($dx=0;$dx -lt 2;$dx++){$display.SetPixel($x*2+$dx,$y*2+$dy,$color)}}
        }
    }
    $display.Save((Join-Path $PSScriptRoot ($spec.Name+'.png')),[System.Drawing.Imaging.ImageFormat]::Png)
    $report+=@{name=$spec.Name;referenceSize=@($display.Width,$display.Height);nativeSize=@($native.Width,$native.Height);cellSize=@(2,2);palette=$paletteHex;binaryAlpha=$true}
    $native.Dispose();$display.Dispose();$source.Dispose()
}
$report | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $PSScriptRoot 'export.json') -Encoding UTF8

$preview=New-Object System.Drawing.Bitmap 980,700
$g=[System.Drawing.Graphics]::FromImage($preview)
$g.Clear([System.Drawing.Color]::FromArgb(255,35,41,45))
$font=New-Object System.Drawing.Font 'Consolas',12
$brush=[System.Drawing.Brushes]::White
$g.DrawString('Reference pixel density: one logical pixel = 2x2 image pixels',$font,$brush,20,16)
$g.DrawString('Preview magnification: 6x nearest-neighbor; same pixel size for all items',$font,$brush,20,40)
$positions=@(@(28,112),@(520,112),@(640,412))
$order=@($specs[1],$specs[0],$specs[2])
for($i=0;$i -lt $order.Count;$i++){
    $spec=$order[$i];$at=$positions[$i]
    $g.DrawString(($spec.Name+'  '+($spec.Width*2)+'x'+($spec.Height*2)),$font,$brush,$at[0],$at[1]-26)
    $im=New-Object System.Drawing.Bitmap (Join-Path $PSScriptRoot ('native/'+$spec.Name+'.png'))
    for($y=0;$y -lt $im.Height;$y++){for($x=0;$x -lt $im.Width;$x++){
        $color=$im.GetPixel($x,$y);if($color.A -eq 0){continue}
        $fill=New-Object System.Drawing.SolidBrush $color
        $g.FillRectangle($fill,($at[0]+$x*12),($at[1]+$y*12),12,12);$fill.Dispose()
    }}
    $im.Dispose()
}
$preview.Save((Join-Path $PSScriptRoot 'preview.png'),[System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose();$font.Dispose();$preview.Dispose()
