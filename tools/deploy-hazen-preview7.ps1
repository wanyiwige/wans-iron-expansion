$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$projectRoot = Split-Path -Parent $PSScriptRoot
$instanceRoot = Split-Path -Parent $projectRoot
$modsRoot = Join-Path $instanceRoot 'mods'
$source = Join-Path $projectRoot 'build/libs/wans_iron_expansion-1.2.1-hazen-preview.7.jar'
$oldJar = Join-Path $modsRoot 'wans_iron_expansion-1.2.1-hazen-preview.6.jar'
$target = Join-Path $modsRoot (Split-Path -Leaf $source)
$expected = '43ecdeef73d73ac16fcb8ee4021db6e319516786a883c0eae9b2b5ddda9dc5d8'
function Hash([string]$path) { (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant() }
function ActiveJars {
    foreach ($jar in Get-ChildItem -LiteralPath $modsRoot -Filter '*.jar' -File) {
        $zip = [IO.Compression.ZipFile]::OpenRead($jar.FullName)
        try {
            $entry = $zip.GetEntry('META-INF/mods.toml')
            if ($entry) {
                $reader = [IO.StreamReader]::new($entry.Open())
                try { $metadata = $reader.ReadToEnd() } finally { $reader.Dispose() }
                foreach ($section in [regex]::Split($metadata, '(?m)^\s*\[\[mods\]\]\s*$') | Select-Object -Skip 1) {
                    $header = ($section -split '\[\[',2)[0]
                    if ($header -match 'modId\s*=\s*"alex_caves_spellsbook"') { $jar.FullName; break }
                }
            }
        } finally { $zip.Dispose() }
    }
}
if ((Hash $source) -ne $expected) { throw 'Release hash mismatch' }
$active = @(ActiveJars)
if ($active.Count -ne 1 -or $active[0] -ne $oldJar) { throw "Unexpected active JARs: $active" }
$oldHash = Hash $oldJar
if ($oldHash -ne '16e72f714f9a9f64ada04ea6cb909e38e702094d9fafb5e1b294452ab8b46d12') { throw 'Crash JAR changed' }
$backupRoot = Join-Path $projectRoot ('development-backups/before-preview7-fix-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Path $backupRoot | Out-Null
$backupJar = Join-Path $backupRoot (Split-Path -Leaf $oldJar)
Copy-Item -LiteralPath $oldJar -Destination $backupJar
if ((Hash $backupJar) -ne $oldHash) { throw 'Backup hash mismatch' }
$configPath = Join-Path $instanceRoot 'config/wans-hazen-recreation-common.toml'
$configHash = Hash $configPath
Copy-Item -LiteralPath $configPath -Destination $backupRoot
if ((Hash (Join-Path $backupRoot (Split-Path -Leaf $configPath))) -ne $configHash) { throw 'Config backup mismatch' }
Copy-Item -LiteralPath (Join-Path $instanceRoot 'crash-reports/crash-2026-10-16_12.50.40-client.txt') -Destination $backupRoot
Copy-Item -LiteralPath (Join-Path $instanceRoot 'logs/latest.log') -Destination $backupRoot
$stage = $target + '.deploying'
if ((Test-Path -LiteralPath $stage) -or (Test-Path -LiteralPath $target)) { throw 'Target already exists' }
Copy-Item -LiteralPath $source -Destination $stage
if ((Hash $stage) -ne $expected) { throw 'Staged hash mismatch' }
# Move the crash version out of the active directory; retain both verified copies.
$retired = Join-Path $backupRoot 'retired-active-preview6.jar'
Move-Item -LiteralPath $oldJar -Destination $retired
try { Move-Item -LiteralPath $stage -Destination $target } catch {
    Copy-Item -LiteralPath $backupJar -Destination $oldJar
    throw
}
if ((Hash $target) -ne $expected) { throw 'Deployed hash mismatch' }
$active = @(ActiveJars)
if ($active.Count -ne 1 -or $active[0] -ne $target) { throw 'Duplicate mod ID after deployment' }
if ((Hash $configPath) -ne $configHash) { throw 'Config changed' }
$record = [ordered]@{
    timestamp = (Get-Date).ToString('o')
    status = 'deployed_verified'
    release = $target
    sha256 = $expected
    backup = $backupJar
    previous_sha256 = $oldHash
    active_same_mod_id_count = $active.Count
    configuration_preserved_sha256 = $configHash
    cause = 'MobEffect constructor invokes initializeClient before Effect.kind is assigned'
    fix = 'State-independent SoulFireEffect client callback; generic effects use inherited callback'
    checks = @('Java 17 build/reobfJar', 'release verification errors=[]', 'old callback negative control', 'nine-effect constructor callback simulation', 'packaged callback bytecode inspection')
    acceptance = 'Full Minecraft client restart, world entry and gameplay not yet tested'
}
$record | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $backupRoot 'deployment.json') -Encoding UTF8
$record | ConvertTo-Json -Depth 5
