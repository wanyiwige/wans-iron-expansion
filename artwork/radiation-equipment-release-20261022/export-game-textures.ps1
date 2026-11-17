$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$project=Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$target=Join-Path $project 'src/main/resources/assets/alex_caves_spellsbook/textures/item'
$sources=Join-Path $PSScriptRoot 'approved-sources'
New-Item -ItemType Directory -Path $sources -Force | Out-Null
$rows=@(
    @{Id='mechanical_radiation_eye';Native='artwork/radiation-mechanical-eye-20261022/native/mechanical-radiation-eye.png';Approved='C:/Users/WANYIW~1/AppData/Local/Temp/codex-clipboard-f70c8713-26ae-40fd-866d-1818105da1c8.png';Size=64},
    @{Id='radiation_grimoire';Native='artwork/radiation-equipment-reference-v2-20261022/native/radiation-grimoire.png';Approved='C:/Users/WANYIW~1/AppData/Local/Temp/codex-clipboard-e1786d76-7acf-4b1d-82f6-1863c75a9831.png';Size=32},
    @{Id='fission_staff';Native='artwork/radiation-equipment-reference-v2-20261022/native/fission-staff.png';Approved='C:/Users/WANYIW~1/AppData/Local/Temp/codex-clipboard-582f88fe-359b-45b6-8ce9-3ded9003a5e2.png';Size=64}
)
$report=@()
foreach($row in $rows){
    Copy-Item -LiteralPath $row.Approved -Destination (Join-Path $sources ($row.Id+'.png'))
    $inputPath=Join-Path $project $row.Native
    $input=New-Object System.Drawing.Bitmap $inputPath
    $output=New-Object System.Drawing.Bitmap $row.Size,$row.Size
    $ox=[int][Math]::Floor(($row.Size-$input.Width)/2)
    $oy=[int][Math]::Floor(($row.Size-$input.Height)/2)
    for($y=0;$y -lt $input.Height;$y++){for($x=0;$x -lt $input.Width;$x++){
        $output.SetPixel($ox+$x,$oy+$y,$input.GetPixel($x,$y))
    }}
    $output.Save((Join-Path $target ($row.Id+'.png')),[System.Drawing.Imaging.ImageFormat]::Png)
    $report+=@{id=$row.Id;nativeSize=@($input.Width,$input.Height);textureSize=$row.Size;offset=@($ox,$oy);source=$row.Native;approvedScreenshot=$row.Approved;method='copy native pixels 1:1 into transparent power-of-two canvas'}
    $input.Dispose();$output.Dispose()
}
$report | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $PSScriptRoot 'texture-export.json') -Encoding UTF8
