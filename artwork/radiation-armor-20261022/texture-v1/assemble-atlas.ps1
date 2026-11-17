$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$taskRoot = $PSScriptRoot
$uvPlan = Get-Content -LiteralPath (Join-Path $taskRoot 'uv-plan.json') -Raw | ConvertFrom-Json
$sourceImage = [System.Drawing.Bitmap]::new((Join-Path $taskRoot 'material-library-source.png'))
$paletteHex = @(
    '#080F1B','#101426','#15192E','#202540','#2D3353','#3D4568','#536080',
    '#05212D','#063441','#074651','#075A63','#08727B','#11898A','#22AD99','#45CFB3','#78E4C4',
    '#004C33','#006C3C','#009745','#00BF50','#27CF62','#50EE73','#91F546','#C5FA83','#EAFCA2','#F3FFD2',
    '#8C9E34','#B4C548','#DFF65B','#F1FF7A',
    '#273D4B','#3E5969','#617480','#7F9CA8','#9DB8BC','#D2E4E4',
    '#0C1924','#18313A','#263B42','#35535A'
)
$colors = @($paletteHex | ForEach-Object { [System.Drawing.ColorTranslator]::FromHtml($_) })
$colorCache = @{}
function Get-NearestPaletteColor([System.Drawing.Color]$sample) {
    $key = $sample.ToArgb()
    if ($colorCache.ContainsKey($key)) { return $colorCache[$key] }
    $bestDistance = [double]::MaxValue
    $bestColor = $colors[0]
    foreach ($candidate in $colors) {
        $dr = [int]$sample.R - [int]$candidate.R
        $dg = [int]$sample.G - [int]$candidate.G
        $db = [int]$sample.B - [int]$candidate.B
        $distance = 2*$dr*$dr + 3*$dg*$dg + 2*$db*$db
        if ($distance -lt $bestDistance) { $bestDistance = $distance; $bestColor = $candidate }
    }
    $colorCache[$key] = $bestColor
    return $bestColor
}
$native = [System.Drawing.Bitmap]::new(128,128,[System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
for ($tile = 0; $tile -lt 16; $tile++) {
    $column = $tile % 4
    $row = [math]::Floor($tile / 4)
    $cellWidth = $sourceImage.Width / 4.0
    $cellHeight = $sourceImage.Height / 4.0
    # Omit the generated sheet's narrow cell borders; sample one color per native pixel.
    $marginX = $cellWidth * 0.025
    $marginY = $cellHeight * 0.025
    for ($py = 0; $py -lt 32; $py++) {
        for ($px = 0; $px -lt 32; $px++) {
            $sx = [math]::Floor($column*$cellWidth + $marginX + ($px+0.5)*($cellWidth-2*$marginX)/32)
            $sy = [math]::Floor($row*$cellHeight + $marginY + ($py+0.5)*($cellHeight-2*$marginY)/32)
            $sample = $sourceImage.GetPixel([int]$sx,[int]$sy)
            $native.SetPixel($column*32+$px,$row*32+$py,(Get-NearestPaletteColor $sample))
        }
    }
}
$native.Save((Join-Path $taskRoot 'material-library-native128.png'),[System.Drawing.Imaging.ImageFormat]::Png)
$atlas = [System.Drawing.Bitmap]::new([int]$uvPlan.width,[int]$uvPlan.height,[System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
foreach ($face in $uvPlan.faces) {
    $tileX = ([int]$face.tile % 4)*32
    $tileY = [int][math]::Floor([int]$face.tile / 4)*32
    $width = [int]$face.pixel_width
    $height = [int]$face.pixel_height
    $atlasX = [int]$face.rect[0]
    $atlasY = [int]$face.rect[1]
    # Crop native painted pixels rather than resizing each surface.
    $offsetX = [int][math]::Floor((32-$width)/2)
    $offsetY = [int][math]::Floor((32-$height)/2)
    if ($face.material -eq 'metal' -or $face.material -eq 'edge') { $offsetX = 1; $offsetY = 1 }
    if ($face.material -eq 'lime') { $offsetX = 8; $offsetY = 8 }
    for ($py = -1; $py -le $height; $py++) {
        for ($px = -1; $px -le $width; $px++) {
            $sampleX = [math]::Min(31,[math]::Max(0,$offsetX+[math]::Min($width-1,[math]::Max(0,$px))))
            $sampleY = [math]::Min(31,[math]::Max(0,$offsetY+[math]::Min($height-1,[math]::Max(0,$py))))
            $atlas.SetPixel($atlasX+$px,$atlasY+$py,$native.GetPixel($tileX+$sampleX,$tileY+$sampleY))
        }
    }
}
$atlas.Save((Join-Path $taskRoot 'critical_warden_armor_v1.png'),[System.Drawing.Imaging.ImageFormat]::Png)
foreach ($entry in @(@($native,'material-library-preview.png',6),@($atlas,'texture-atlas-preview.png',4))) {
    $bitmap = $entry[0]
    $factor = [int]$entry[2]
    $preview = [System.Drawing.Bitmap]::new($bitmap.Width*$factor,$bitmap.Height*$factor,[System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [System.Drawing.Graphics]::FromImage($preview)
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
    $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
    $graphics.DrawImage($bitmap,[System.Drawing.Rectangle]::new(0,0,$preview.Width,$preview.Height),0,0,$bitmap.Width,$bitmap.Height,[System.Drawing.GraphicsUnit]::Pixel)
    $preview.Save((Join-Path $taskRoot $entry[1]),[System.Drawing.Imaging.ImageFormat]::Png)
    $graphics.Dispose(); $preview.Dispose()
}
$sourceImage.Dispose(); $native.Dispose(); $atlas.Dispose()
Write-Output "Saved 256x256 texture atlas from 128x128 native painted material library, uniform 2 px per model unit."
