$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$taskProject = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskFolder = Join-Path $taskProject 'development-checks/foolish-boss-icons'
$taskBackup = Join-Path $taskProject 'development-backups/foolish-icons-20261009'
$taskIcons = Join-Path $taskProject 'src/main/resources/assets/alex_caves_spellsbook/textures/gui/spell_icons'
$taskSources = Join-Path $taskFolder 'originals'
New-Item -ItemType Directory -Force -Path $taskBackup,$taskSources | Out-Null
$taskEntries = Get-Content -LiteralPath (Join-Path $taskFolder 'generation-manifest.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$taskLang = Get-Content -LiteralPath (Join-Path $taskProject 'src/main/resources/assets/alex_caves_spellsbook/lang/zh_cn.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$taskSheet = New-Object System.Drawing.Bitmap(1008,528)
$taskGraphics = [System.Drawing.Graphics]::FromImage($taskSheet)
$taskGraphics.Clear([System.Drawing.Color]::FromArgb(255,25,25,30))
$taskGraphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$taskGraphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$taskFont = New-Object System.Drawing.Font('Microsoft YaHei',10)
$taskIndex = 0
foreach ($taskEntry in $taskEntries) {
    $taskTarget = Join-Path $taskIcons ($taskEntry.id + '.png')
    $taskOld = Join-Path $taskBackup ($taskEntry.id + '.png')
    if (!(Test-Path -LiteralPath $taskOld)) {
        Copy-Item -LiteralPath $taskTarget -Destination $taskOld
        if ((Get-FileHash -LiteralPath $taskTarget).Hash -ne (Get-FileHash -LiteralPath $taskOld).Hash) { throw 'Backup hash mismatch' }
    }
    Copy-Item -LiteralPath $taskEntry.generated_source -Destination (Join-Path $taskSources ($taskEntry.id + '.png'))
    $taskOriginal = [System.Drawing.Bitmap]::FromFile($taskEntry.generated_source)
    $taskSmall = New-Object System.Drawing.Bitmap(32,32)
    for ($taskY = 0; $taskY -lt 32; $taskY++) {
        for ($taskX = 0; $taskX -lt 32; $taskX++) {
            $taskColor = $taskOriginal.GetPixel([int][Math]::Floor(($taskX + 0.5) * $taskOriginal.Width / 32), [int][Math]::Floor(($taskY + 0.5) * $taskOriginal.Height / 32))
            $taskSmall.SetPixel($taskX,$taskY,[System.Drawing.Color]::FromArgb(255,$taskColor.R,$taskColor.G,$taskColor.B))
        }
    }
    $taskSmall.Save($taskTarget,[System.Drawing.Imaging.ImageFormat]::Png)
    $taskEntry | Add-Member -Force NoteProperty width 32
    $taskEntry | Add-Member -Force NoteProperty height 32
    $taskEntry | Add-Member -Force NoteProperty alpha_min 255
    $taskEntry | Add-Member -Force NoteProperty alpha_max 255
    $taskEntry | Add-Member -Force NoteProperty sha256 ((Get-FileHash -LiteralPath $taskTarget).Hash.ToLowerInvariant())
    $taskColumn = $taskIndex % 7
    $taskRow = [int][Math]::Floor($taskIndex / 7)
    $taskGraphics.DrawImage($taskSmall,(New-Object System.Drawing.Rectangle(($taskColumn*144+8),($taskRow*176+8),128,128)),0,0,32,32,[System.Drawing.GraphicsUnit]::Pixel)
    $taskName = $taskLang.('spell.alex_caves_spellsbook.' + $taskEntry.id)
    $taskGraphics.DrawString($taskName,$taskFont,[System.Drawing.Brushes]::White,($taskColumn*144+8),($taskRow*176+144))
    $taskIndex++
    $taskSmall.Dispose()
    $taskOriginal.Dispose()
}
$taskGraphics.Dispose()
$taskFont.Dispose()
$taskSheet.Save((Join-Path $taskFolder 'preview.png'),[System.Drawing.Imaging.ImageFormat]::Png)
$taskSheet.Dispose()
$taskEntries | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $taskFolder 'generation-manifest.json') -Encoding UTF8
Write-Output "Packaged $taskIndex opaque 32x32 icons."
