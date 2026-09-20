$ErrorActionPreference = 'Stop'

if (-not (Get-Command hermes -ErrorAction SilentlyContinue)) {
  throw 'Hermes CLI is required: https://hermes-agent.nousresearch.com/docs'
}
if (-not (Get-Command python -ErrorAction SilentlyContinue)) {
  throw 'Python is required to apply the YAML capability policy.'
}

$rootDir = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$profiles = @(
  @{ Name = 'project-manager'; Source = 'project-manager' }
  @{ Name = 'coder'; Source = 'coder' }
)
Push-Location $rootDir
try {
  foreach ($entry in $profiles) {
    $profile = $entry.Name
    $source = ".\.hermes\profiles\$($entry.Source)"
    hermes profile install $source --name $profile --alias --force --yes
    if ($LASTEXITCODE -ne 0) {
      throw "Failed to install Hermes Profile Distribution: $profile"
    }
    hermes --profile $profile config set terminal.cwd $rootDir
    if ($LASTEXITCODE -ne 0) {
      throw "Failed to configure project cwd: $profile"
    }
    hermes --profile $profile config set kanban.auto_decompose false
    if ($LASTEXITCODE -ne 0) {
      throw "Failed to disable automatic Kanban decomposition: $profile"
    }
    if ($profile -eq 'project-manager') {
      hermes --profile $profile config set skills.external_dirs '[]'
      if ($LASTEXITCODE -ne 0) {
        throw "Failed to clear legacy duplicate Skill directory: $profile"
      }
    }
  }
} finally {
  Pop-Location
}

$policyScript = Join-Path $rootDir '.hermes\scripts\apply-hermes-capabilities.py'
$policyFile = Join-Path $rootDir '.hermes\profiles'
python $policyScript --policy $policyFile --profile project-manager --profile coder --project-root $rootDir
if ($LASTEXITCODE -ne 0) {
  throw 'Failed to apply the YAML capability policy.'
}

if ($env:HERMES_MODEL) {
  $provider = if ($env:HERMES_PROVIDER) { $env:HERMES_PROVIDER } else { 'custom' }
  foreach ($entry in $profiles) {
    $profile = $entry.Name
    hermes --profile $profile config set model.provider $provider
    hermes --profile $profile config set model.default $env:HERMES_MODEL
    if ($env:HERMES_BASE_URL) {
      hermes --profile $profile config set model.base_url $env:HERMES_BASE_URL
    }
    if ($LASTEXITCODE -ne 0) {
      throw "Failed to configure model route: $profile"
    }
  }
} else {
  Write-Warning 'Model route not configured. Run hermes model for each Profile, or set HERMES_MODEL (and optionally HERMES_PROVIDER/HERMES_BASE_URL) before rerunning.'
}

Write-Output 'Amaazon Hermes Profiles installed.'
