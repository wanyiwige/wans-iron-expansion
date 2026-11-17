param([string]$Task = 'build', [string]$Project = '', [string]$Log = 'build.log', [switch]$SkipCertificateProbe)
$ErrorActionPreference = 'Stop'
$hazenProjectRoot = Split-Path -Parent $PSScriptRoot
if (-not $Project) { $Project = $hazenProjectRoot }
$hazenGradleRoot = 'C:/Users/WANYIWIGE/.gradle/wrapper/dists/gradle-8.8-bin/dl7vupf4psengwqhwktix4v1/gradle-8.8'
$hazenClasspath = ((Get-ChildItem -LiteralPath "$hazenGradleRoot/lib" -Filter '*.jar' -File).FullName -join ';')
$hazenArgs = @('--add-opens=java.base/java.util=ALL-UNNAMED', '--add-opens=java.base/java.lang=ALL-UNNAMED',
    '--add-opens=java.base/java.lang.invoke=ALL-UNNAMED', '--add-opens=java.prefs/java.util.prefs=ALL-UNNAMED',
    '--add-opens=java.base/java.nio.charset=ALL-UNNAMED', '--add-opens=java.base/java.net=ALL-UNNAMED',
    '--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED', '-Dfile.encoding=UTF-8', '-Duser.language=en', '-Duser.country=US',
    "-javaagent:$hazenGradleRoot/lib/agents/gradle-instrumentation-agent-8.8.jar", '-cp', $hazenClasspath,
    'org.gradle.launcher.Main', '--offline', '--no-daemon', $Task)
if ($SkipCertificateProbe) { $hazenArgs = @('-Dnet.minecraftforge.gradle.check.certs=false') + $hazenArgs }
Push-Location -LiteralPath $Project
try {
    & 'C:/Program Files/Microsoft/jdk-17.0.15.6-hotspot/bin/java.exe' $hazenArgs *> "$hazenProjectRoot/development-records/hazen-recreation/$Log"
    $hazenExit = $LASTEXITCODE
    Get-Content -LiteralPath "$hazenProjectRoot/development-records/hazen-recreation/$Log" -Tail 65
    exit $hazenExit
} finally { Pop-Location }
