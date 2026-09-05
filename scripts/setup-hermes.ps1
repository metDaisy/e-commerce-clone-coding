$ErrorActionPreference = 'Stop'

if (-not (Get-Command hermes -ErrorAction SilentlyContinue)) {
  throw 'Hermes CLI is required: https://hermes-agent.nousresearch.com/docs'
}
if (-not (Get-Command python -ErrorAction SilentlyContinue)) {
  throw 'Python is required to apply the YAML capability policy.'
}

$rootDir = Split-Path -Parent $PSScriptRoot
$profiles = @(
  'project-manager'
  'prototype-coder'
  'reviewer-general'
  'reviewer-deep'
  'reviewer-coordinator'
  'refactor-coder'
)
$skills = @{
  'project-manager' = @(
  )
  'prototype-coder' = @(
    'skills-sh/github/awesome-copilot/java-springboot'
    'skills-sh/github/awesome-copilot/java-junit'
  )
  'reviewer-general' = @(
    'skills-sh/mattpocock/skills/code-review'
    'skills-sh/alannkl/skills/simplify-code'
    'skills-sh/toss/es-toolkit/compat-review'
  )
  'reviewer-deep' = @(
    'skills-sh/mattpocock/skills/improve-codebase-architecture'
    'skills-sh/getsentry/warden/architecture-review'
    'skills-sh/jabrena/plinth/305-frameworks-spring-boot-modulith'
    'skills-sh/affaan-m/ecc/jpa-patterns'
  )
  'reviewer-coordinator' = @()
  'refactor-coder' = @(
    'skills-sh/github/awesome-copilot/java-refactoring-extract-method'
    'skills-sh/github/awesome-copilot/java-refactoring-remove-parameter'
  )
}

Push-Location $rootDir
try {
  foreach ($profile in $profiles) {
    $source = ".\.hermes\profile-distributions\$profile"
    hermes profile install $source --name $profile --alias --force --yes
    if ($LASTEXITCODE -ne 0) {
      throw "Failed to install Hermes Profile Distribution: $profile"
    }
    hermes --profile $profile config set terminal.cwd $rootDir
    if ($LASTEXITCODE -ne 0) {
      throw "Failed to configure project cwd: $profile"
    }
  }
} finally {
  Pop-Location
}

foreach ($profile in $profiles) {
  foreach ($identifier in $skills[$profile]) {
    $output = (& hermes --profile $profile skills install $identifier --yes 2>&1 | Out-String)
    if ($LASTEXITCODE -ne 0 -or $output -match 'Error:|Installation blocked:' -or ($output -notmatch 'Installed:' -and $output -notmatch 'already installed')) {
      Write-Error $output
      throw "Failed to install skills.sh Skill '$identifier' for Profile '$profile'"
    }
    Write-Output "skills.sh: $identifier -> $profile"
  }
}

$policyScript = Join-Path $rootDir 'scripts\apply-hermes-capabilities.py'
$policyFile = Join-Path $rootDir '.hermes\profile-capabilities'
python $policyScript --policy $policyFile
if ($LASTEXITCODE -ne 0) {
  throw 'Failed to apply the YAML capability policy.'
}

if ($env:HERMES_MODEL) {
  $provider = if ($env:HERMES_PROVIDER) { $env:HERMES_PROVIDER } else { 'custom' }
  foreach ($profile in $profiles) {
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

Write-Output 'Amaazon Hermes Profile Distributions installed.'
