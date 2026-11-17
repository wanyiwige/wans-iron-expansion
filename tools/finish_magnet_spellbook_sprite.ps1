param(
    [Parameter(Mandatory = $true)][string]$SourcePath,
    [Parameter(Mandatory = $true)][string]$OutputPath,
    [Parameter(Mandatory = $true)][string]$PreviewPath
)

Add-Type -AssemblyName System.Drawing

$palette = @(
    [System.Drawing.Color]::FromArgb(17, 19, 28),
    [System.Drawing.Color]::FromArgb(32, 37, 47),
    [System.Drawing.Color]::FromArgb(51, 59, 67),
    [System.Drawing.Color]::FromArgb(77, 86, 88),
    [System.Drawing.Color]::FromArgb(112, 123, 116),
    [System.Drawing.Color]::FromArgb(157, 167, 151),
    [System.Drawing.Color]::FromArgb(205, 210, 183),
    [System.Drawing.Color]::FromArgb(75, 25, 35),
    [System.Drawing.Color]::FromArgb(130, 37, 49),
    [System.Drawing.Color]::FromArgb(193, 51, 62),
    [System.Drawing.Color]::FromArgb(247, 91, 79),
    [System.Drawing.Color]::FromArgb(20, 43, 72),
    [System.Drawing.Color]::FromArgb(31, 83, 134),
    [System.Drawing.Color]::FromArgb(42, 137, 196),
    [System.Drawing.Color]::FromArgb(96, 206, 234),
    [System.Drawing.Color]::FromArgb(244, 234, 194)
)

$source = [System.Drawing.Bitmap]::new($SourcePath)
$sprite = [System.Drawing.Bitmap]::new(32, 32, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
try {
    $canvas = [System.Drawing.Graphics]::FromImage($sprite)
    try {
        $canvas.Clear([System.Drawing.Color]::Transparent)
        $canvas.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
        $canvas.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
        $canvas.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
        $canvas.DrawImage($source, [System.Drawing.Rectangle]::new(0, 0, 32, 32), 0, 0, $source.Width, $source.Height, [System.Drawing.GraphicsUnit]::Pixel)
    } finally {
        $canvas.Dispose()
    }

    for ($y = 0; $y -lt 32; $y++) {
        for ($x = 0; $x -lt 32; $x++) {
            $pixel = $sprite.GetPixel($x, $y)
            if ($pixel.A -lt 128) {
                $sprite.SetPixel($x, $y, [System.Drawing.Color]::Transparent)
                continue
            }
            $best = $palette[0]
            $bestDistance = [double]::PositiveInfinity
            foreach ($candidate in $palette) {
                $distance = [math]::Pow($pixel.R - $candidate.R, 2) + [math]::Pow($pixel.G - $candidate.G, 2) + [math]::Pow($pixel.B - $candidate.B, 2)
                if ($distance -lt $bestDistance) {
                    $bestDistance = $distance
                    $best = $candidate
                }
            }
            $sprite.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, $best.R, $best.G, $best.B))
        }
    }

    $sprite.Save($OutputPath, [System.Drawing.Imaging.ImageFormat]::Png)

    $preview = [System.Drawing.Bitmap]::new(512, 512, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    try {
        $previewCanvas = [System.Drawing.Graphics]::FromImage($preview)
        try {
            $previewCanvas.Clear([System.Drawing.Color]::Transparent)
            $previewCanvas.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
            $previewCanvas.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
            $previewCanvas.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
            $previewCanvas.DrawImage($sprite, [System.Drawing.Rectangle]::new(0, 0, 512, 512), 0, 0, 32, 32, [System.Drawing.GraphicsUnit]::Pixel)
        } finally {
            $previewCanvas.Dispose()
        }
        $preview.Save($PreviewPath, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $preview.Dispose()
    }
} finally {
    $sprite.Dispose()
    $source.Dispose()
}
