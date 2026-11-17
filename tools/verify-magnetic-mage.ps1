param([string]$Version = '1.0.15')
$ErrorActionPreference = 'Stop'
$project = Split-Path $PSScriptRoot -Parent
$assets = Join-Path $project 'src/main/resources/assets/alex_caves_spellsbook'
Add-Type -AssemblyName System.IO.Compression.FileSystem
function Assert($condition, $message) {
    if (-not $condition) { throw $message }
}
function Read-ZipText($zip, $name) {
    $entry = $zip.GetEntry($name)
    Assert ($null -ne $entry) "Missing ZIP entry: $name"
    $reader = [IO.StreamReader]::new($entry.Open())
    try { return $reader.ReadToEnd() } finally { $reader.Dispose() }
}
$geo = (Get-Content (Join-Path $assets 'geo/magnetic_mage.geo.json') -Raw | ConvertFrom-Json).'minecraft:geometry'[0]
$names = @($geo.bones.name)
Assert (($names | Select-Object -Unique).Count -eq $names.Count) 'Duplicate bones'
foreach ($bone in $geo.bones) {
    if ($bone.parent) { Assert ($names -contains $bone.parent) "Missing parent: $($bone.name)" }
}
$reference = [IO.Compression.ZipFile]::OpenRead((Join-Path $project 'libs/irons_spellbooks-3.16.3.jar'))
try {
    $original = (Read-ZipText $reference 'assets/irons_spellbooks/geo/abstract_casting_mob.geo.json' | ConvertFrom-Json).'minecraft:geometry'[0]
    foreach ($bone in $original.bones) {
        $match = $geo.bones | Where-Object name -eq $bone.name
        Assert ($null -ne $match) "Missing native bone: $($bone.name)"
        Assert ($match.parent -eq $bone.parent) "Changed parent: $($bone.name)"
        Assert (($match.pivot -join ',') -eq ($bone.pivot -join ',')) "Changed pivot: $($bone.name)"
        Assert (($match.rotation -join ',') -eq ($bone.rotation -join ',')) "Changed rotation: $($bone.name)"
    }
    $null = Read-ZipText $reference 'assets/irons_spellbooks/animations/casting_animations.json' | ConvertFrom-Json
} finally { $reference.Dispose() }
$png = [IO.File]::ReadAllBytes((Join-Path $assets 'textures/entity/magnetic_mage.png'))
$width = $png[16] * 16777216 + $png[17] * 65536 + $png[18] * 256 + $png[19]
$height = $png[20] * 16777216 + $png[21] * 65536 + $png[22] * 256 + $png[23]
Assert ($width -eq $geo.description.texture_width -and $height -eq $geo.description.texture_height) 'Texture dimensions differ from UV atlas'
foreach ($language in 'zh_cn','en_us') {
    $lang = Get-Content (Join-Path $assets "lang/$language.json") -Raw | ConvertFrom-Json
    Assert ($null -ne $lang.'entity.alex_caves_spellsbook.magnetic_mage') 'Missing entity name'
    Assert ($null -ne $lang.'item.alex_caves_spellsbook.magnetic_mage_spawn_egg') 'Missing egg name'
}
$jarPath = Join-Path $project "build/libs/alex_caves_spellsbook-$Version.jar"
$jar = [IO.Compression.ZipFile]::OpenRead($jarPath)
try {
    foreach ($entry in @(
        'cn/wanyiwige/alexcavesspellsbook/entities/MagneticMageEntity.class',
        'cn/wanyiwige/alexcavesspellsbook/entities/MagneticMageAttackGoal.class',
        'cn/wanyiwige/alexcavesspellsbook/client/MagneticMageModel.class',
        'cn/wanyiwige/alexcavesspellsbook/client/render/MagneticMageRenderer.class',
        'assets/alex_caves_spellsbook/geo/magnetic_mage.geo.json',
        'assets/alex_caves_spellsbook/textures/entity/magnetic_mage.png',
        'assets/alex_caves_spellsbook/models/item/magnetic_mage_spawn_egg.json')) {
        Assert ($null -ne $jar.GetEntry($entry)) "Missing release entry: $entry"
    }
    $metadata = Read-ZipText $jar 'META-INF/mods.toml'
    Assert ($metadata.Contains('modId="geckolib"')) 'Missing GeckoLib dependency'
    Assert ($metadata.Contains('version="' + $Version + '"')) 'Wrong release version'
} finally { $jar.Dispose() }
Write-Output "PASS: native bone hierarchy/transforms, animation resource, ${width}x${height} atlas, language keys, release classes/assets/dependency."
Write-Output 'Static verification only; this does not test in-game rendering or combat.'
Get-FileHash -LiteralPath $jarPath -Algorithm SHA256
