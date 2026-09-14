$ErrorActionPreference = "Stop"
$backend = (Resolve-Path (Join-Path $PSScriptRoot "..\backend")).Path
Push-Location $backend
try {
    $env:SPRING_PROFILES_ACTIVE = "local"
    & .\gradlew.bat bootRun
}
finally {
    Pop-Location
}

