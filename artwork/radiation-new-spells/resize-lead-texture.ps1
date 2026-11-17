Add-Type -AssemblyName System.Drawing
$src = Join-Path $PSScriptRoot 'lead_curtain_imagegen-source.png'
$dst = Join-Path $PSScriptRoot 'lead_curtain_metal-64.png'
$inputImage = [System.Drawing.Bitmap]::new($src)
$outputImage = [System.Drawing.Bitmap]::new(64,64,[System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
for($y=0;$y -lt 64;$y++) { for($x=0;$x -lt 64;$x++) {
    $sx=[int][Math]::Floor(($x+0.5)*$inputImage.Width/64)
    $sy=[int][Math]::Floor(($y+0.5)*$inputImage.Height/64)
    $outputImage.SetPixel($x,$y,$inputImage.GetPixel($sx,$sy))
}}
$outputImage.Save($dst,[System.Drawing.Imaging.ImageFormat]::Png)
$inputImage.Dispose();$outputImage.Dispose()
Write-Output $dst
