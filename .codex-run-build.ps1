$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.15.6-hotspot"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
Set-Location -LiteralPath $PSScriptRoot
& .\gradlew.bat build --no-daemon *>&1 | Tee-Object -LiteralPath ".codex-external-build.log"
exit $LASTEXITCODE
