$ErrorActionPreference = "Stop"

$workspaceRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$backendEnvPath = Join-Path $workspaceRoot "backend\.env"
$seedSourcePath = Join-Path $workspaceRoot "db\danyeogam_tour_seed.sql"
$seedImportPath = Join-Path ([IO.Path]::GetTempPath()) "danyeogam_tour_seed.sql"
$externalImagesSourcePath = Join-Path $workspaceRoot "db\danyeogam_external_images.sql"
$externalImagesImportPath = Join-Path ([IO.Path]::GetTempPath()) "danyeogam_external_images.sql"
$mysql = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"

$configuration = @{}
Get-Content -LiteralPath $backendEnvPath | ForEach-Object {
    if ($_ -match '^([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
        $configuration[$matches[1]] = $matches[2].Trim()
    }
}

$env:MYSQL_PWD = $configuration["MYSQL_PASSWORD"]
try {
    $count = & $mysql --protocol=TCP -h 127.0.0.1 -P 3307 `
        -u $configuration["MYSQL_USER"] -N -B $configuration["MYSQL_DATABASE"] `
        -e "SELECT COUNT(*) FROM tourist_spot;"
    if ($LASTEXITCODE -ne 0) {
        throw "The Flyway schema is not ready. Start the backend once before importing the seed."
    }
    if ([int]$count -gt 0) {
        Write-Host "Seed import skipped: tourist_spot already contains $count rows."
    } else {
        Copy-Item -LiteralPath $seedSourcePath -Destination $seedImportPath -Force
        try {
            $mysqlSourcePath = $seedImportPath.Replace('\', '/')
            & $mysql --protocol=TCP -h 127.0.0.1 -P 3307 `
                -u $configuration["MYSQL_USER"] $configuration["MYSQL_DATABASE"] `
                -e "source $mysqlSourcePath"
            if ($LASTEXITCODE -ne 0) {
                throw "Seed import failed."
            }
        }
        finally {
            Remove-Item -LiteralPath $seedImportPath -Force -ErrorAction SilentlyContinue
        }
        Write-Host "Tour seed imported."
    }

    if (Test-Path -LiteralPath $externalImagesSourcePath) {
        Copy-Item -LiteralPath $externalImagesSourcePath -Destination $externalImagesImportPath -Force
        try {
            $mysqlExternalImagesPath = $externalImagesImportPath.Replace('\', '/')
            & $mysql --protocol=TCP -h 127.0.0.1 -P 3307 `
                -u $configuration["MYSQL_USER"] $configuration["MYSQL_DATABASE"] `
                -e "source $mysqlExternalImagesPath"
            if ($LASTEXITCODE -ne 0) {
                throw "Verified external image import failed."
            }
        }
        finally {
            Remove-Item -LiteralPath $externalImagesImportPath -Force -ErrorAction SilentlyContinue
        }
        Write-Host "Verified tourist images applied."
    }
}
finally {
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
}
