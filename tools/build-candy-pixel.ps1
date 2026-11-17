param([string[]]$Tasks=@('build'))
$ErrorActionPreference='Stop'
$candyRoot=Split-Path $PSScriptRoot -Parent
$candyJdk='C:\Program Files\Microsoft\jdk-17.0.15.6-hotspot'
$candyGradle=Get-ChildItem -LiteralPath (Join-Path $env:USERPROFILE '.gradle\wrapper\dists\gradle-8.8-bin') -Directory | ForEach-Object {Join-Path $_.FullName 'gradle-8.8'} | Where-Object {Test-Path -LiteralPath (Join-Path $_ 'lib')} | Select-Object -First 1
if(!$candyGradle){throw 'Project-selected Gradle 8.8 not installed.'}
$candyClasspath=(Get-ChildItem -LiteralPath (Join-Path $candyGradle 'lib') -Filter '*.jar' -File).FullName -join ';'
$env:JAVA_HOME=$candyJdk
$env:Path="$candyJdk\bin;$env:Path"
$candyArgs=@('--add-opens=java.base/java.util=ALL-UNNAMED','--add-opens=java.base/java.lang=ALL-UNNAMED',
 '--add-opens=java.base/java.lang.invoke=ALL-UNNAMED','--add-opens=java.prefs/java.util.prefs=ALL-UNNAMED',
 '--add-opens=java.base/java.nio.charset=ALL-UNNAMED','--add-opens=java.base/java.net=ALL-UNNAMED',
 '--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED','-Dfile.encoding=UTF-8',
 '-Dnet.minecraftforge.gradle.check.certs=false',"-javaagent:$candyGradle\lib\agents\gradle-instrumentation-agent-8.8.jar",
 '-cp',$candyClasspath,'org.gradle.launcher.Main','--offline','--no-daemon','--console=plain')+$Tasks
Push-Location -LiteralPath $candyRoot
try {
 & (Join-Path $candyJdk 'bin\java.exe') $candyArgs *> (Join-Path $candyRoot 'artwork\candy-first-five-20261017\build.log')
 $candyExit=$LASTEXITCODE
 Get-Content -LiteralPath (Join-Path $candyRoot 'artwork\candy-first-five-20261017\build.log') -Tail 65
 exit $candyExit
} finally {Pop-Location}
