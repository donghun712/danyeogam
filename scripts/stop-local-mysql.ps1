$ErrorActionPreference = "Stop"

$mysqlAdmin = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqladmin.exe"
if (-not (Test-Path -LiteralPath $mysqlAdmin)) {
    throw "mysqladmin.exe was not found."
}

& $mysqlAdmin --protocol=TCP -h 127.0.0.1 -P 3307 -u root shutdown
if ($LASTEXITCODE -ne 0) {
    throw "Local MySQL shutdown failed or the server was not running."
}
$mapping = (subst.exe) | Where-Object { $_ -match '^T:\\:' }
if ($mapping) {
    subst.exe T: /d | Out-Null
}
Write-Host "Local MySQL stopped."
