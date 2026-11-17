Add-Type -AssemblyName System.Drawing
$root=Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$dir=Join-Path $root 'src\main\resources\assets\alex_caves_spellsbook\textures\entity\toxic'
function Canvas { return [System.Drawing.Bitmap]::new(32,32,[System.Drawing.Imaging.PixelFormat]::Format32bppArgb) }
function Save([System.Drawing.Bitmap]$b,[string]$name){
    $b.Save((Join-Path $dir ($name+'.png')),[System.Drawing.Imaging.ImageFormat]::Png);$b.Dispose()
}
function Rect([System.Drawing.Bitmap]$b,[int]$x,[int]$y,[int]$w,[int]$h,[string]$hex){
    $c=[System.Drawing.ColorTranslator]::FromHtml($hex)
    for($yy=$y;$yy -lt $y+$h;$yy++){for($xx=$x;$xx -lt $x+$w;$xx++){
        if($xx -ge 0 -and $xx -lt 32 -and $yy -ge 0 -and $yy -lt 32){$b.SetPixel($xx,$yy,$c)}
    }}
}
function Cropped([string]$name,[int]$x,[int]$y,[int]$w,[int]$h,[int]$dx,[int]$dy,[int]$dw,[int]$dh){
    $iconName=@{neutron='neutron_rebound';curtain='lead_curtain';film='radiation_film';shell='decay_transposition'}[$name]
    $src=[System.Drawing.Bitmap]::new((Join-Path $root ('src\main\resources\assets\alex_caves_spellsbook\textures\gui\spell_icons\'+$iconName+'.png')))
    $dst=Canvas
    for($yy=0;$yy -lt $dh;$yy++){for($xx=0;$xx -lt $dw;$xx++){
        $sx=2*($x+[int][Math]::Floor(($xx+0.5)*$w/$dw))
        $sy=2*($y+[int][Math]::Floor(($yy+0.5)*$h/$dh))
        $color=$src.GetPixel($sx,$sy)
        if([Math]::Max($color.R,[Math]::Max($color.G,$color.B)) -lt 52){$color=[System.Drawing.Color]::Transparent}
        elseif($name -eq 'film'){
            if($color.G -gt 135){$color=[System.Drawing.Color]::FromArgb(255,174,255,184)}
            else{$color=[System.Drawing.Color]::FromArgb(255,74,177,97)}
        }
        elseif($name -eq 'shell'){
            if($color.G -gt 135){$color=[System.Drawing.Color]::FromArgb(255,163,248,108)}
            else{$color=[System.Drawing.Color]::FromArgb(255,63,172,69)}
        }
        $dst.SetPixel($dx+$xx,$dy+$yy,$color)
    }}
    $src.Dispose();Save $dst $name
}
Cropped 'neutron' 2 1 14 14 6 6 20 20
Cropped 'curtain' 8 7 16 19 0 0 32 32
Cropped 'film' 2 7 9 23 9 1 14 30
Cropped 'shell' 2 7 11 22 7 1 18 30
$trail=Canvas
Rect $trail 12 12 8 8 '#A8FFEC'
Rect $trail 14 14 4 4 '#FFFFFF'
Rect $trail 10 15 2 2 '#48E96F'
Rect $trail 20 15 2 2 '#48E96F'
Save $trail 'trail'
$wake=Canvas
Rect $wake 5 12 22 8 '#146DA9'
Rect $wake 8 12 16 4 '#42CFF5'
Rect $wake 12 12 8 2 '#B4FFFF'
Save $wake 'wake'
$matrix=Canvas
Rect $matrix 3 3 26 26 '#909B9B'
Rect $matrix 6 6 20 20 '#1B2326'
Rect $matrix 9 9 14 14 '#75E640'
Rect $matrix 12 12 8 8 '#D6FF5D'
Save $matrix 'matrix'
$chain=Canvas
Rect $chain 2 15 7 3 '#3CB835'
Rect $chain 9 12 5 3 '#91FF5B'
Rect $chain 14 15 6 3 '#E4FF93'
Rect $chain 20 11 5 3 '#91FF5B'
Rect $chain 25 14 5 3 '#3CB835'
Save $chain 'chain'
Set-Content -LiteralPath (Join-Path $dir 'trail.png.mcmeta') -Value '{"texture":{"blur":false,"clamp":false}}' -NoNewline -Encoding ascii
Write-Output 'Refined world sprites to single reusable pixel motifs.'
