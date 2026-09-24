$ErrorActionPreference = 'Stop'

if (-not (Get-Command hermes -ErrorAction SilentlyContinue)) {
  throw 'Hermes CLI is required: https://hermes-agent.nousresearch.com/docs'
}
if (-not (Get-Command python -ErrorAction SilentlyContinue)) {
  throw 'Python is required to verify the plugin configuration.'
}

$defaultHome = python -c 'from hermes_constants import get_hermes_home; print(get_hermes_home())'
$profileRoot = Split-Path -Parent $defaultHome
$rootDir = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$pluginDir = Join-Path $rootDir '.hermes\plugins\agent-audit'
$hookScript = Join-Path $pluginDir 'shell_hook.py'
$hookCommand = "python $hookScript"
$hookValue = ConvertTo-Json -Compress @(@{ command = $hookCommand })
$manifest = Join-Path $pluginDir 'plugin.yaml'

function Approve-AuditHooks([string]$home) {
  $previousHome = $env:HERMES_HOME
  $previousAccept = $env:HERMES_ACCEPT_HOOKS
  try {
    $env:HERMES_HOME = $home
    $env:HERMES_ACCEPT_HOOKS = '1'
    python -c 'from agent.shell_hooks import register_from_config; from hermes_cli.config import load_config; register_from_config(load_config(), accept_hooks=False)'
    if ($LASTEXITCODE -ne 0) {
      throw "Failed to approve agent-audit shell hooks for $home."
    }
  } finally {
    $env:HERMES_HOME = $previousHome
    $env:HERMES_ACCEPT_HOOKS = $previousAccept
  }
}

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

foreach ($event in @('post_tool_call', 'on_skill_lifecycle', 'pre_verify', 'on_session_end')) {
  hermes config set "hooks.$event" $hookValue
  if ($LASTEXITCODE -ne 0) {
    throw "Failed to configure agent-audit shell hook: $event"
  }
}
Approve-AuditHooks $defaultHome

foreach ($profile in @('project-manager', 'coder', 'reviewer')) {
  hermes --profile $profile config set plugins.enabled '["agent-audit"]'
  if ($LASTEXITCODE -ne 0) {
    throw "Failed to enable agent-audit for $profile."
  }
  $actual = hermes --profile $profile config get plugins.enabled --json | Out-String | ConvertFrom-Json
  if (@($actual).Count -ne 1 -or @($actual)[0] -ne 'agent-audit') {
    throw "Read-back mismatch for $profile plugins.enabled."
  }
  foreach ($event in @('post_tool_call', 'on_skill_lifecycle', 'pre_verify', 'on_session_end')) {
    hermes --profile $profile config set "hooks.$event" $hookValue
    if ($LASTEXITCODE -ne 0) {
      throw "Failed to configure agent-audit shell hook for $profile`: $event"
    }
  }
  Approve-AuditHooks (Join-Path $profileRoot $profile)
}

Write-Output 'Amaazon Hermes project plugin installed for project-manager, coder, reviewer, and the default Desktop profile.'
Write-Output 'Restart Hermes Desktop after installation so the shell hooks register.'
