param([switch]$Partial)
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$art=$PSScriptRoot
$project=Split-Path (Split-Path $art -Parent) -Parent
$manifest=Get-Content -Raw -LiteralPath (Join-Path $art 'sources.json') | ConvertFrom-Json
$icons=Join-Path $project 'src/main/resources/assets/alex_caves_spellsbook/textures/gui/spell_icons'
$effects=Join-Path $project 'src/main/resources/assets/alex_caves_spellsbook/textures/entity/toxic/rework'
$sourceDir=Join-Path $art 'sources'
$nativeDir=Join-Path $art 'native-icons'
New-Item -ItemType Directory -Force -Path $effects,$sourceDir,$nativeDir | Out-Null
$hex=@('#11171b','#253438','#45565c','#788a8d','#b6c8c8','#e5eeea','#103c29','#246c3d','#45974b','#70bf55','#b4ee3b','#e2f65b','#a58c22','#edcd2d','#fff14b','#fffac4','#163a64','#146d9a','#1cacdc','#54dff1','#c8f8ff')
$palette=@($hex | ForEach-Object {[System.Drawing.ColorTranslator]::FromHtml($_)})
function Quantize([System.Drawing.Color]$c){
    $best=$palette[0];$dist=[double]::MaxValue
    foreach($q in $palette){$d=($c.R-$q.R)*($c.R-$q.R)+($c.G-$q.G)*($c.G-$q.G)+($c.B-$q.B)*($c.B-$q.B);if($d -lt $dist){$dist=$d;$best=$q}}
    return $best
}
function Bitmap([int]$w,[int]$h){return [System.Drawing.Bitmap]::new($w,$h,[System.Drawing.Imaging.PixelFormat]::Format32bppArgb)}
function Sample([System.Drawing.Bitmap]$src,[int]$x,[int]$y,[int]$w,[int]$h,[int]$px,[int]$py,[int]$size){
    $sx=[Math]::Min($src.Width-1,$x+[int][Math]::Floor(($px+.5)*$w/$size))
    $sy=[Math]::Min($src.Height-1,$y+[int][Math]::Floor(($py+.5)*$h/$size))
    return $src.GetPixel($sx,$sy)
}
function SavePng([System.Drawing.Bitmap]$bmp,[string]$path){$bmp.Save($path,[System.Drawing.Imaging.ImageFormat]::Png)}
function Meta([string]$path){[IO.File]::WriteAllText($path+'.mcmeta','{"texture":{"blur":false,"clamp":true}}',[Text.UTF8Encoding]::new($false))}
function Visible([System.Drawing.Color]$c,[string]$id){
    if($c.A -lt 192 -or [Math]::Max($c.R,[Math]::Max($c.G,$c.B)) -le 45){return $false}
    if($id -eq 'curtain'){return (($c.R -gt 180 -and $c.G -gt 220 -and $c.B -gt 110) -or ($c.R -gt 244 -and $c.G -gt 220 -and $c.B -lt 90))}
    return $true
}
foreach($row in $manifest.icons.PSObject.Properties){
    $id=$row.Name;$source=$row.Value.source;Copy-Item -LiteralPath $source -Destination (Join-Path $sourceDir ('icon-'+$id+'.png')) -Force
    $src=[System.Drawing.Bitmap]::new($source);$native=Bitmap 32 32;$large=Bitmap 64 64
    for($y=0;$y -lt 32;$y++){for($x=0;$x -lt 32;$x++){
        $c=Sample $src 0 0 $src.Width $src.Height $x $y 32
        $q=if([Math]::Max($c.R,[Math]::Max($c.G,$c.B)) -lt 55){$palette[0]}else{Quantize $c}
        $native.SetPixel($x,$y,$q)
        for($dy=0;$dy -lt 2;$dy++){for($dx=0;$dx -lt 2;$dx++){$large.SetPixel($x*2+$dx,$y*2+$dy,$q)}}
    }}
    SavePng $native (Join-Path $nativeDir ($id+'.png'));$dest=Join-Path $icons ($id+'.png');SavePng $large $dest;Meta $dest
    $src.Dispose();$native.Dispose();$large.Dispose()
}
foreach($row in $manifest.effects.PSObject.Properties){
    $id=$row.Name;if($id -eq 'metal'){continue}
    $source=$row.Value.source;Copy-Item -LiteralPath $source -Destination (Join-Path $sourceDir ('vfx-'+$id+'.png')) -Force
    $src=[System.Drawing.Bitmap]::new($source);$size=if($id -eq 'matrix'){16}else{32};$strip=Bitmap $size ($size*4)
    $bounds=@();$extent=1.0
    for($f=0;$f -lt 4;$f++){
        $l=[int][Math]::Floor(($f%2)*$src.Width/2);$t=[int][Math]::Floor([Math]::Floor($f/2)*$src.Height/2)
        $w=[int][Math]::Floor($src.Width/2);$h=[int][Math]::Floor($src.Height/2)
        $minX=$w;$minY=$h;$maxX=-1;$maxY=-1
        for($iy=0;$iy -lt 128;$iy++){for($ix=0;$ix -lt 128;$ix++){
            $x=[int][Math]::Floor(($ix+.5)*$w/128);$y=[int][Math]::Floor(($iy+.5)*$h/128);$c=$src.GetPixel($l+$x,$t+$y)
            if(Visible $c $id){$minX=[Math]::Min($minX,$x);$minY=[Math]::Min($minY,$y);$maxX=[Math]::Max($maxX,$x);$maxY=[Math]::Max($maxY,$y)}
        }}
        if($maxX -lt 0){throw ('Empty generated frame: '+$id+' '+$f)}
        $extent=[Math]::Max($extent,[Math]::Max($maxX-$minX+1,$maxY-$minY+1))
        $bounds+=,@{left=$l;top=$t;width=$w;height=$h;cx=$l+($minX+$maxX)/2;cy=$t+($minY+$maxY)/2}
    }
    $margin=if($size -eq 16){1}else{3};$cropSize=$extent*$size/($size-2*$margin)
    for($frame=0;$frame -lt 4;$frame++){
        $tile=Bitmap $size $size;$left=[int][Math]::Floor(($frame%2)*$src.Width/2);$top=[int][Math]::Floor([Math]::Floor($frame/2)*$src.Height/2)
        for($y=0;$y -lt $size;$y++){for($x=0;$x -lt $size;$x++){
            $box=$bounds[$frame];$sx=[int][Math]::Floor($box.cx+($x+.5-$size/2)*$cropSize/$size);$sy=[int][Math]::Floor($box.cy+($y+.5-$size/2)*$cropSize/$size)
            if($sx -lt $box.left -or $sx -ge $box.left+$box.width -or $sy -lt $box.top -or $sy -ge $box.top+$box.height){continue}
            $c=$src.GetPixel($sx,$sy)
            if(Visible $c $id){$q=Quantize $c;$tile.SetPixel($x,$y,$q)}
        }}
        # Registration on the same logical grid removes accidental atlas-centering wobble.
        $minX=$size;$minY=$size;$maxX=-1;$maxY=-1
        for($y=0;$y -lt $size;$y++){for($x=0;$x -lt $size;$x++){if($tile.GetPixel($x,$y).A -gt 0){$minX=[Math]::Min($minX,$x);$maxX=[Math]::Max($maxX,$x);$minY=[Math]::Min($minY,$y);$maxY=[Math]::Max($maxY,$y)}}}
        $shiftX=if($maxX -ge 0){[int][Math]::Floor(($size-1-$minX-$maxX)/2)}else{0};$shiftY=if($maxY -ge 0){[int][Math]::Floor(($size-1-$minY-$maxY)/2)}else{0}
        for($y=0;$y -lt $size;$y++){for($x=0;$x -lt $size;$x++){$tx=$x+$shiftX;$ty=$y+$shiftY;if($tx -ge 0 -and $tx -lt $size -and $ty -ge 0 -and $ty -lt $size){$strip.SetPixel($tx,$frame*$size+$ty,$tile.GetPixel($x,$y))}}}
        $tile.Dispose()
    }
    $dest=Join-Path $effects ($id+'.png');SavePng $strip $dest;Meta $dest;$src.Dispose();$strip.Dispose()
}
$pixel=Bitmap 1 1;$pixel.SetPixel(0,0,[Drawing.Color]::White);$dest=Join-Path $effects 'pixel.png';SavePng $pixel $dest;Meta $dest;$pixel.Dispose()
$statusDir=Join-Path $project 'src/main/resources/assets/alex_caves_spellsbook/textures/mob_effect'
New-Item -ItemType Directory -Force -Path $statusDir | Out-Null
$status=Bitmap 16 16;$brand=[Drawing.Bitmap]::new((Join-Path $effects 'isotope.png'))
for($y=0;$y -lt 16;$y++){for($x=0;$x -lt 16;$x++){$status.SetPixel($x,$y,$brand.GetPixel($x*2+1,$y*2+1))}}
$dest=Join-Path $statusDir 'radiation_mark.png';SavePng $status $dest;Meta $dest;$status.Dispose();$brand.Dispose()
if($manifest.effects.metal){
    $source=$manifest.effects.metal.source;Copy-Item -LiteralPath $source -Destination (Join-Path $sourceDir 'metal.png') -Force
    $src=[Drawing.Bitmap]::new($source);$quarters=@()
    for($i=0;$i -lt 4;$i++){$q=Bitmap 32 32;for($y=0;$y -lt 32;$y++){for($x=0;$x -lt 32;$x++){$c=Sample $src ([int](($i%2)*$src.Width/2)) ([int]([Math]::Floor($i/2)*$src.Height/2)) ([int]($src.Width/2)) ([int]($src.Height/2)) $x $y 32;$q.SetPixel($x,$y,(Quantize $c))}};$quarters+=,$q}
    $metal=Bitmap 64 64
    for($y=0;$y -lt 64;$y++){for($x=0;$x -lt 64;$x++){
        $c=$quarters[0].GetPixel($x%32,$y%32)
        if($y -ge 28 -and $y -lt 36){$c=$quarters[2].GetPixel($x%32,12+$y-28)}
        if($y -lt 3 -or $y -ge 61){$c=$quarters[1].GetPixel($x%32,$y%3)}
        if($x -ge 48 -and $y -ge 48){$c=$quarters[3].GetPixel(($x-48)*2,($y-48)*2)}
        $metal.SetPixel($x,$y,$c)
    }}
    $dest=Join-Path $project 'src/main/resources/assets/alex_caves_spellsbook/textures/entity/toxic/lead_curtain_metal.png';SavePng $metal $dest;Meta $dest;SavePng $metal (Join-Path $art 'lead-metal-native64.png')
    foreach($q in $quarters){$q.Dispose()};$metal.Dispose();$src.Dispose()
}
Write-Output ('Exported '+@($manifest.icons.PSObject.Properties).Count+' native32 icons and '+@($manifest.effects.PSObject.Properties).Count+' generated world assets.')
