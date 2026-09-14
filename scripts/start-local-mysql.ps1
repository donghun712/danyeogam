$ErrorActionPreference = "Stop"

$workspaceRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$backendEnvPath = Join-Path $workspaceRoot "backend\.env"
$dataDirectory = Join-Path $workspaceRoot ".tmp\local-mysql-data"
$mysqld = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqld.exe"
$mysql = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
$mysqlAdmin = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqladmin.exe"

foreach ($requiredPath in @($backendEnvPath, $mysqld, $mysql, $mysqlAdmin)) {
    if (-not (Test-Path -LiteralPath $requiredPath)) {
        throw "Required path was not found: $requiredPath"
    }
}

$existingMapping = (subst.exe) | Where-Object { $_ -match '^T:\\:' }
if ($existingMapping -and $existingMapping -notmatch [regex]::Escape($workspaceRoot)) {
    throw "T: is already mapped to another directory. Remove that mapping or change the local MySQL drive letter."
}
if (-not $existingMapping) {
    subst.exe T: $workspaceRoot
}

New-Item -ItemType Directory -Force -Path $dataDirectory | Out-Null
if (-not (Test-Path -LiteralPath (Join-Path $dataDirectory "mysql"))) {
    & $mysqld --initialize-insecure --console `
        --basedir="C:\Program Files\MySQL\MySQL Server 8.0" `
        --datadir="T:\.tmp\local-mysql-data"
    if ($LASTEXITCODE -ne 0) {
        throw "MySQL data directory initialization failed."
    }
}

& $mysqlAdmin --protocol=TCP -h 127.0.0.1 -P 3307 -u root ping --silent 2>$null
if ($LASTEXITCODE -ne 0) {
    Start-Process -FilePath $mysqld `
        -ArgumentList "--defaults-file=T:\scripts\local-mysql.ini" `
        -WindowStyle Hidden

    $ready = $false
    for ($attempt = 0; $attempt -lt 30; $attempt++) {
        Start-Sleep -Milliseconds 500
        & $mysqlAdmin --protocol=TCP -h 127.0.0.1 -P 3307 -u root ping --silent 2>$null
        if ($LASTEXITCODE -eq 0) {
            $ready = $true
            break
        }
    }
    if (-not $ready) {
        $errorLog = Join-Path $dataDirectory "mysqld.err"
        if (Test-Path -LiteralPath $errorLog) {
            Get-Content -LiteralPath $errorLog -Tail 30
        }
        throw "Local MySQL did not become ready on port 3307."
    }
}

$configuration = @{}
Get-Content -LiteralPath $backendEnvPath | ForEach-Object {
    if ($_ -match '^([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
        $configuration[$matches[1]] = $matches[2].Trim()
    }
}

$database = $configuration["MYSQL_DATABASE"]
$username = $configuration["MYSQL_USER"]
$password = $configuration["MYSQL_PASSWORD"]
if ($database -notmatch '^[A-Za-z0-9_]+$' -or $username -notmatch '^[A-Za-z0-9_]+$') {
    throw "MYSQL_DATABASE and MYSQL_USER must contain only letters, numbers, and underscores."
}
$escapedPassword = $password.Replace("'", "''")
$setupSql = @"
CREATE DATABASE IF NOT EXISTS ``$database`` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER IF NOT EXISTS '$username'@'localhost' IDENTIFIED BY '$escapedPassword';
ALTER USER '$username'@'localhost' IDENTIFIED BY '$escapedPassword';
CREATE USER IF NOT EXISTS '$username'@'127.0.0.1' IDENTIFIED BY '$escapedPassword';
ALTER USER '$username'@'127.0.0.1' IDENTIFIED BY '$escapedPassword';
GRANT ALL PRIVILEGES ON ``$database``.* TO '$username'@'localhost';
GRANT ALL PRIVILEGES ON ``$database``.* TO '$username'@'127.0.0.1';
FLUSH PRIVILEGES;
"@
& $mysql --protocol=TCP -h 127.0.0.1 -P 3307 -u root -e $setupSql
if ($LASTEXITCODE -ne 0) {
    throw "Failed to create the local application database and user."
}

Write-Host "Local MySQL is ready at 127.0.0.1:3307."
Write-Host "Next: start the backend with scripts\start-backend.ps1"

