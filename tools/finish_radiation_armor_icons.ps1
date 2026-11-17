$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$projectRoot=Split-Path $PSScriptRoot -Parent
$art=Join-Path $projectRoot 'artwork/radiation-armor-release-20261023'
$assets=Join-Path $projectRoot 'src/main/resources/assets/alex_caves_spellsbook/textures/item'
$source=[System.Drawing.Bitmap]::new((Join-Path $art 'icons-generated-source.png'))
$rectangles=Get-Content -LiteralPath (Join-Path $art 'icon-crops.json') -Raw | ConvertFrom-Json
$pal=Get-Content -LiteralPath (Join-Path $projectRoot 'artwork/radiation-armor-v2-20261022/texture-materials-v4/palette.json') -Raw | ConvertFrom-Json
$colors=@($pal.PSObject.Properties | ForEach-Object {[System.Drawing.ColorTranslator]::FromHtml($_.Value)})
$sizes=@{helmet=@(14,14);chestplate=@(14,12);leggings=@(13,12);boots=@(14,9)}
$names=@('helmet','chestplate','leggings','boots')
foreach($name in $names){
 $crop=$rectangles.$name
 $dest=[System.Drawing.Bitmap]::new(16,16,[System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
 $tw=$sizes[$name][0];$th=$sizes[$name][1]
 $dx=[int][Math]::Floor((16-$tw)/2);$dy=[int][Math]::Floor((16-$th)/2)
 for($y=0;$y -lt $th;$y++){for($x=0;$x -lt $tw;$x++){
  $sx=[int][Math]::Floor($crop[0]+($x+0.5)*$crop[2]/$tw)
  $sy=[int][Math]::Floor($crop[1]+($y+0.5)*$crop[3]/$th)
  $pixel=$source.GetPixel($sx,$sy)
  if($pixel.A -lt 128){continue}
  $nearest=$colors[0];$distance=[double]::PositiveInfinity
  foreach($color in $colors){$diff=[Math]::Pow($pixel.R-$color.R,2)+[Math]::Pow($pixel.G-$color.G,2)+[Math]::Pow($pixel.B-$color.B,2);if($diff -lt $distance){$distance=$diff;$nearest=$color}}
  $dest.SetPixel($dx+$x,$dy+$y,$nearest)
 }}
 $path=Join-Path $assets ('critical_warden_'+$name+'.png')
 $dest.Save($path,[System.Drawing.Imaging.ImageFormat]::Png)
 Copy-Item -LiteralPath $path -Destination (Join-Path $art ('critical_warden_'+$name+'.png')) -Force
 [System.IO.File]::WriteAllText(($path+'.mcmeta'),'{"texture":{"blur":false,"clamp":true}}',[System.Text.UTF8Encoding]::new($false))
 $dest.Dispose()
}
$source.Dispose()
$board=[System.Drawing.Bitmap]::new(768,230)
$g=[System.Drawing.Graphics]::FromImage($board)
$g.Clear([System.Drawing.ColorTranslator]::FromHtml('#242932'))
$g.InterpolationMode=[System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode=[System.Drawing.Drawing2D.PixelOffsetMode]::Half
$font=[System.Drawing.Font]::new('Consolas',13)
for($i=0;$i -lt 4;$i++){
 $im=[System.Drawing.Image]::FromFile((Join-Path $assets ('critical_warden_'+$names[$i]+'.png')))
 $g.DrawImage($im,[System.Drawing.Rectangle]::new(30+$i*185,30,128,128),0,0,16,16,[System.Drawing.GraphicsUnit]::Pixel)
 $g.DrawString($names[$i]+' 16x16',$font,[System.Drawing.Brushes]::White,15+$i*185,177)
 $im.Dispose()
}
$board.Save((Join-Path $art 'icons-native-preview.png'),[System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose();$board.Dispose();$font.Dispose()
Write-Output 'Saved four native 16x16 palette-constrained icons and nearest-neighbor preview.'
