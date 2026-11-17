$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$taskRoot=$PSScriptRoot
$plan=Get-Content -LiteralPath (Join-Path $taskRoot 'uv-plan.json') -Raw | ConvertFrom-Json
$sourceImage=[System.Drawing.Bitmap]::new((Join-Path $taskRoot 'uv-painted-source.png'))
$bitmap=[System.Drawing.Bitmap]::new([int]$plan.width,[int]$plan.height,[System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$hex=@{
    ink='#08131F';n0='#11162B';n1='#1B233B';n2='#2B3854';n3='#455774';
    t0='#06333F';t1='#074B55';t2='#08727B';t3='#21A695';t4='#5CD1B0';
    r0='#132D35';r1='#314A52';s0='#3A5564';s1='#708D9A';s2='#B7D4D2';
    g0='#006A43';g1='#11BF60';g2='#75ED70';y0='#8FA83B';y1='#DFF65B';y2='#F0FF9A'
}
$palette=@{}
foreach($key in $hex.Keys){$palette[$key]=[System.Drawing.ColorTranslator]::FromHtml($hex[$key])}
$cloth=@('hood','hood_crown','coat','sleeve','tail','front_tail','side_tail','trouser','collar')
$steel=@('connector','cap')
function Get-Family([string]$role){
    if($cloth -contains $role){return @('ink','n0','n1','n2','n3')}
    if($steel -contains $role){return @('ink','t0','s0','s1','s2')}
    if($role -in @('belt','sole')){return @('ink','r0','r1','s0')}
    if($role -eq 'core'){return @('g0','g1','g2','y0','y1','y2')}
    if($role -eq 'trefoil'){return @('ink','n0','n1','t0')}
    return @('ink','t0','t1','t2','t3','t4','s0','s1')
}
$cache=@{}
foreach($face in $plan.faces){
    $family=Get-Family $face.role
    $x=[int]$face.pixels[0];$y=[int]$face.pixels[1];$w=[int]$face.pixels[2];$h=[int]$face.pixels[3]
    for($py=0;$py -lt $h;$py++){
        for($px=0;$px -lt $w;$px++){
            $sx=[int][math]::Floor(($x+$px+0.5)*$sourceImage.Width/$plan.width)
            $sy=[int][math]::Floor(($y+$py+0.5)*$sourceImage.Height/$plan.height)
            $sample=$sourceImage.GetPixel($sx,$sy)
            $cacheKey=$face.role+':'+$sample.ToArgb()
            if(-not $cache.ContainsKey($cacheKey)){
                $distance=[double]::MaxValue;$chosen=$palette[$family[0]]
                foreach($entry in $family){
                    $candidate=$palette[$entry]
                    $dr=[int]$sample.R-[int]$candidate.R;$dg=[int]$sample.G-[int]$candidate.G;$db=[int]$sample.B-[int]$candidate.B
                    $value=2*$dr*$dr+3*$dg*$dg+2*$db*$db
                    if($value -lt $distance){$distance=$value;$chosen=$candidate}
                }
                $cache[$cacheKey]=$chosen
            }
            $bitmap.SetPixel($x+$px,$y+$py,$cache[$cacheKey])
        }
    }
}
$bitmap.Save((Join-Path $taskRoot 'critical_warden_v2_painted_base.png'),[System.Drawing.Imaging.ImageFormat]::Png)
$hex | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $taskRoot 'palette.json') -Encoding UTF8
$bitmap.Dispose();$sourceImage.Dispose()
Write-Output 'Sampled painted UV atlas onto native 128x128 grid and material-specific color families.'
