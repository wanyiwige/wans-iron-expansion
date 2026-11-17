param([string[]]$Tasks=@('build'))
$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
$jdk='C:\Program Files\Microsoft\jdk-17.0.15.6-hotspot'
$gradleRoot=Join-Path $env:USERPROFILE '.gradle\wrapper\dists\gradle-8.8-bin'
$gradleHome=Get-ChildItem -LiteralPath $gradleRoot -Directory | ForEach-Object {Join-Path $_.FullName 'gradle-8.8'} | Where-Object {Test-Path -LiteralPath (Join-Path $_ 'lib')} | Select-Object -First 1
if(!$gradleHome){throw 'The project-selected Gradle 8.8 distribution is not installed.'}
$classPath=(Get-ChildItem -LiteralPath (Join-Path $gradleHome 'lib') -Filter '*.jar' -File | Select-Object -ExpandProperty FullName) -join ';'
$env:JAVA_HOME=$jdk
$env:Path="$jdk\bin;$env:Path"
Set-Location -LiteralPath $projectRoot
$launcherArgs=@(
 '--add-opens=java.base/java.util=ALL-UNNAMED',
 '--add-opens=java.base/java.lang=ALL-UNNAMED',
 '--add-opens=java.base/java.lang.invoke=ALL-UNNAMED',
 '--add-opens=java.prefs/java.util.prefs=ALL-UNNAMED',
 '--add-opens=java.base/java.nio.charset=ALL-UNNAMED',
 '--add-opens=java.base/java.net=ALL-UNNAMED',
 '--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED',
 '-Dfile.encoding=UTF-8','-Duser.country=CN','-Duser.language=zh',
 '-Dnet.minecraftforge.gradle.check.certs=false',
 "-javaagent:$gradleHome\lib\agents\gradle-instrumentation-agent-8.8.jar",
 '-cp',$classPath,'org.gradle.launcher.Main','--offline','--no-daemon','--console=plain'
)+$Tasks
& (Join-Path $jdk 'bin\java.exe') $launcherArgs *>&1 | Tee-Object -LiteralPath (Join-Path $projectRoot 'development-checks\toxic-audit\build.log')
exit $LASTEXITCODE
