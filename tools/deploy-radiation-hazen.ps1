param([string]$PlanPath='', [string]$OutputDirectory='')
$ErrorActionPreference='Stop'
$deploymentProjectRoot=Split-Path -Parent $PSScriptRoot
$deploymentInstanceRoot=Split-Path -Parent $deploymentProjectRoot
$deploymentModsRoot=[IO.Path]::GetFullPath((Join-Path $deploymentInstanceRoot 'mods'))
$deploymentOutput=if($OutputDirectory){[IO.Path]::GetFullPath($OutputDirectory)}else{Join-Path $deploymentProjectRoot 'outputs/radiation-hazen-deploy-20261016'}
$deploymentPlanPath=if($PlanPath){[IO.Path]::GetFullPath($PlanPath)}else{Join-Path $deploymentOutput 'preflight.json'}
$deploymentPlan=Get-Content -LiteralPath $deploymentPlanPath -Raw -Encoding UTF8 | ConvertFrom-Json
$deploymentSource=[IO.Path]::GetFullPath($deploymentPlan.source)
$deploymentTarget=[IO.Path]::GetFullPath($deploymentPlan.target)
$deploymentPrevious=[IO.Path]::GetFullPath($deploymentPlan.previous.path)
if((Split-Path -Parent $deploymentTarget) -ne $deploymentModsRoot -or (Split-Path -Parent $deploymentPrevious) -ne $deploymentModsRoot){throw 'Deployment paths escaped the intended mods directory.'}
if(Test-Path -LiteralPath $deploymentTarget){throw 'Destination already exists; no replacement was attempted.'}
function Get-DeploymentHash([string]$path){(Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant()}
if((Get-DeploymentHash $deploymentSource) -ne $deploymentPlan.sha256){throw 'Verified source JAR changed.'}
if((Get-DeploymentHash $deploymentPrevious) -ne $deploymentPlan.previous.sha256){throw 'Installed predecessor changed after preflight.'}
$deploymentTime=[DateTimeOffset]::UtcNow.ToOffset([TimeSpan]::FromHours(8))
$deploymentBackup=Join-Path $deploymentProjectRoot ('deployment-backups/radiation-hazen-'+$deploymentTime.ToString('yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Path $deploymentBackup | Out-Null
$deploymentOldCopy=Join-Path $deploymentBackup (Split-Path -Leaf $deploymentPrevious)
Copy-Item -LiteralPath $deploymentPrevious -Destination $deploymentOldCopy
if((Get-DeploymentHash $deploymentOldCopy) -ne $deploymentPlan.previous.sha256){throw 'Rollback backup failed hash verification.'}
Copy-Item -LiteralPath $deploymentPlanPath -Destination (Join-Path $deploymentBackup 'preflight.json')
$deploymentConfig=Join-Path $deploymentInstanceRoot 'config/wans-hazen-recreation-common.toml'
if(Test-Path -LiteralPath $deploymentConfig){
 $deploymentConfigCopy=Join-Path $deploymentBackup 'wans-hazen-recreation-common.toml'
 Copy-Item -LiteralPath $deploymentConfig -Destination $deploymentConfigCopy
 if((Get-DeploymentHash $deploymentConfig) -ne (Get-DeploymentHash $deploymentConfigCopy)){throw 'Configuration backup hash mismatch.'}
}
$deploymentStage=Join-Path $deploymentBackup 'verified-release-staged.jar'
Copy-Item -LiteralPath $deploymentSource -Destination $deploymentStage
if((Get-DeploymentHash $deploymentStage) -ne $deploymentPlan.sha256){throw 'Staged release hash mismatch.'}
$deploymentRetired=Join-Path $deploymentBackup 'previous-live.jar'
$deploymentRemoved=$false
try {
 Move-Item -LiteralPath $deploymentPrevious -Destination $deploymentRetired
 $deploymentRemoved=$true
 Move-Item -LiteralPath $deploymentStage -Destination $deploymentTarget
 if((Get-DeploymentHash $deploymentTarget) -ne $deploymentPlan.sha256){throw 'Deployed file failed hash verification.'}
 $deploymentActive=@(Get-ChildItem -LiteralPath $deploymentModsRoot -File -Filter 'wans_iron_expansion-*.jar')
 if($deploymentActive.Count -ne 1 -or $deploymentActive[0].FullName -ne $deploymentTarget){throw 'Expected exactly one active Wan Iron Expansion JAR.'}
 $deploymentReceipt=[ordered]@{
  deployed=$true;timestamp=$deploymentTime.ToString('o');version=$deploymentPlan.version
  source=$deploymentSource;target=$deploymentTarget;sha256=(Get-DeploymentHash $deploymentTarget)
  rollback_backup=$deploymentOldCopy;rollback_sha256=(Get-DeploymentHash $deploymentOldCopy)
  backup_directory=$deploymentBackup;active_jar_count=$deploymentActive.Count
  hazen_config_preserved=$true;game_restarted=$false;gameplay_visual_multiplayer_verified=$false
 }
 $deploymentReceipt | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $deploymentOutput 'deployment.json') -Encoding UTF8
 $deploymentReceipt | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $deploymentBackup 'deployment.json') -Encoding UTF8
 $deploymentReceipt | ConvertTo-Json -Depth 6
} catch {
 if($deploymentRemoved){
  if(Test-Path -LiteralPath $deploymentTarget){
   if((Get-DeploymentHash $deploymentTarget) -eq $deploymentPlan.sha256){Remove-Item -LiteralPath $deploymentTarget}
   else {throw 'Unexpected destination content encountered; verified rollback backup retained for manual recovery.'}
  }
  if(!(Test-Path -LiteralPath $deploymentPrevious)){Copy-Item -LiteralPath $deploymentOldCopy -Destination $deploymentPrevious}
  if((Get-DeploymentHash $deploymentPrevious) -ne $deploymentPlan.previous.sha256){throw 'Rollback hash mismatch; retain verified backup for recovery.'}
 }
 throw
}
