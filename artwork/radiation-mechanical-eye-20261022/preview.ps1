$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$source=New-Object System.Drawing.Bitmap (Join-Path $PSScriptRoot 'mechanical-radiation-eye.png')
$preview=New-Object System.Drawing.Bitmap 624,408
$g=[System.Drawing.Graphics]::FromImage($preview)
$g.Clear([System.Drawing.Color]::FromArgb(255,35,41,45))
$font=New-Object System.Drawing.Font 'Consolas',12
$g.DrawString('72x42 PNG / 2x2 pixel cells / 8x preview',$font,[System.Drawing.Brushes]::White,20,14)
for($y=0;$y -lt $source.Height;$y++){for($x=0;$x -lt $source.Width;$x++){
    $color=$source.GetPixel($x,$y);if($color.A -eq 0){continue}
    $brush=New-Object System.Drawing.SolidBrush $color
    $g.FillRectangle($brush,(24+$x*8),(48+$y*8),8,8);$brush.Dispose()
}}
$preview.Save((Join-Path $PSScriptRoot 'preview.png'),[System.Drawing.Imaging.ImageFormat]::Png)
$font.Dispose();$g.Dispose();$preview.Dispose();$source.Dispose()
