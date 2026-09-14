$ErrorActionPreference = "Stop"
$frontend = (Resolve-Path (Join-Path $PSScriptRoot "..\danyeogam-frontend")).Path
Push-Location $frontend
try {
    npm run dev
}
finally {
    Pop-Location
}

