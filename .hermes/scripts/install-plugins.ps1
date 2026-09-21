$ErrorActionPreference = 'Stop'

if (-not (Get-Command hermes -ErrorAction SilentlyContinue)) {
  throw 'Hermes CLI is required: https://hermes-agent.nousresearch.com/docs'
}
if (-not (Get-Command python -ErrorAction SilentlyContinue)) {
  throw 'Python is required to verify the plugin configuration.'
}

$rootDir = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$pluginDir = Join-Path $rootDir '.hermes\plugins\agent-audit'
$manifest = Join-Path $pluginDir 'plugin.yaml'
if (-not (Test-Path -LiteralPath $manifest -PathType Leaf)) {
  throw "Plugin manifest not found: $manifest"
}

hermes plugins validate $pluginDir --json
if ($LASTEXITCODE -ne 0) {
  throw 'Hermes plugin validation failed.'
}
hermes plugins doctor $pluginDir --ci
if ($LASTEXITCODE -ne 0) {
  throw 'Hermes Plugin Doctor failed.'
}

foreach ($profile in @('project-manager', 'coder', 'reviewer')) {
  hermes --profile $profile config set plugins.enabled '["agent-audit"]'
  if ($LASTEXITCODE -ne 0) {
    throw "Failed to enable agent-audit for $profile."
  }
  $actual = hermes --profile $profile config get plugins.enabled --json | Out-String | ConvertFrom-Json
  if (@($actual).Count -ne 1 -or @($actual)[0] -ne 'agent-audit') {
    throw "Read-back mismatch for $profile plugins.enabled."
  }
}

Write-Output 'Amaazon Hermes project plugin installed for project-manager, coder, and reviewer.'
Write-Output 'Start Hermes with HERMES_ENABLE_PROJECT_PLUGINS=true to enable project-plugin discovery.'
