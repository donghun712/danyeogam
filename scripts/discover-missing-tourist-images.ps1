$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

$workspaceRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$auditPath = Join-Path $workspaceRoot "backend\docs\missing-image-source-audit.csv"
$outputPath = Join-Path $workspaceRoot "backend\docs\missing-image-web-discovery.csv"

function Normalize-Text([string]$value) {
    if ([string]::IsNullOrWhiteSpace($value)) { return "" }
    return (($value -replace '\[[^\]]*\]', '') -replace '\([^\)]*\)', '' -replace '[^\p{L}\p{Nd}]', '').ToLowerInvariant()
}

function Get-LocalityToken([string]$address) {
    $tokens = @($address -split '\s+' | Where-Object { $_ -match '(시|군|구|읍|면)$' })
    if ($tokens.Count -eq 0) { return "" }
    return $tokens[0]
}

function Decode-JsonString([string]$value) {
    if ([string]::IsNullOrWhiteSpace($value)) { return "" }
    try {
        return ('"' + ($value -replace '"', '\"') + '"' | ConvertFrom-Json)
    } catch {
        return $value -replace '\\u0026', '&' -replace '\\/', '/'
    }
}

function Get-Host([string]$url) {
    try { return ([uri]$url).DnsSafeHost.ToLowerInvariant() } catch { return "" }
}

function Test-ImageUrl([string]$url) {
    $handler = $null
    $client = $null
    $request = $null
    $response = $null
    try {
        $handler = [System.Net.Http.HttpClientHandler]::new()
        $handler.AllowAutoRedirect = $true
        $client = [System.Net.Http.HttpClient]::new($handler)
        $client.Timeout = [TimeSpan]::FromSeconds(25)
        $client.DefaultRequestHeaders.UserAgent.ParseAdd("Mozilla/5.0 DanyeogamImageDiscovery/1.0")
        $request = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::Get, $url)
        $request.Headers.Range = [System.Net.Http.Headers.RangeHeaderValue]::new(0, 1023)
        $response = $client.SendAsync($request, [System.Net.Http.HttpCompletionOption]::ResponseHeadersRead).GetAwaiter().GetResult()
        $contentType = [string]$response.Content.Headers.ContentType.MediaType
        return [pscustomobject]@{
            Valid = ([int]$response.StatusCode -in @(200, 206)) -and $contentType.StartsWith('image/')
            HttpStatus = [int]$response.StatusCode
            ContentType = $contentType
        }
    } catch {
        return [pscustomobject]@{ Valid = $false; HttpStatus = 0; ContentType = "" }
    } finally {
        if ($null -ne $response) { $response.Dispose() }
        if ($null -ne $request) { $request.Dispose() }
        if ($null -ne $client) { $client.Dispose() }
        if ($null -ne $handler) { $handler.Dispose() }
    }
}

$unresolvedStatuses = @('NO_VERIFIED_IMAGE', 'COMMONS_SOURCE_MISSING', 'SOURCE_PROVENANCE_WEAK')
$spots = @(Import-Csv -LiteralPath $auditPath | Where-Object { $_.Status -in $unresolvedStatuses })

$results = $spots | ForEach-Object -Parallel {
    $spot = $_
    $ProgressPreference = "SilentlyContinue"

    function Normalize-Text([string]$value) {
        if ([string]::IsNullOrWhiteSpace($value)) { return "" }
        return (($value -replace '\[[^\]]*\]', '') -replace '\([^\)]*\)', '' -replace '[^\p{L}\p{Nd}]', '').ToLowerInvariant()
    }
    function Get-LocalityToken([string]$address) {
        $tokens = @($address -split '\s+' | Where-Object { $_ -match '(시|군|구|읍|면)$' })
        if ($tokens.Count -eq 0) { return "" }
        return $tokens[0]
    }
    function Decode-JsonString([string]$value) {
        if ([string]::IsNullOrWhiteSpace($value)) { return "" }
        try { return ('"' + ($value -replace '"', '\"') + '"' | ConvertFrom-Json) }
        catch { return $value -replace '\\u0026', '&' -replace '\\/', '/' }
    }
    function Get-Host([string]$url) {
        try { return ([uri]$url).DnsSafeHost.ToLowerInvariant() } catch { return "" }
    }
    function Test-ImageUrl([string]$url) {
        $handler = $null; $client = $null; $request = $null; $response = $null
        try {
            $handler = [System.Net.Http.HttpClientHandler]::new()
            $handler.AllowAutoRedirect = $true
            $client = [System.Net.Http.HttpClient]::new($handler)
            $client.Timeout = [TimeSpan]::FromSeconds(25)
            $client.DefaultRequestHeaders.UserAgent.ParseAdd("Mozilla/5.0 DanyeogamImageDiscovery/1.0")
            $request = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::Get, $url)
            $request.Headers.Range = [System.Net.Http.Headers.RangeHeaderValue]::new(0, 1023)
            $response = $client.SendAsync($request, [System.Net.Http.HttpCompletionOption]::ResponseHeadersRead).GetAwaiter().GetResult()
            $contentType = [string]$response.Content.Headers.ContentType.MediaType
            return [pscustomobject]@{ Valid = ([int]$response.StatusCode -in @(200, 206)) -and $contentType.StartsWith('image/'); HttpStatus = [int]$response.StatusCode; ContentType = $contentType }
        } catch { return [pscustomobject]@{ Valid = $false; HttpStatus = 0; ContentType = "" } }
        finally {
            if ($null -ne $response) { $response.Dispose() }
            if ($null -ne $request) { $request.Dispose() }
            if ($null -ne $client) { $client.Dispose() }
            if ($null -ne $handler) { $handler.Dispose() }
        }
    }

    $name = [string]$spot.Name
    $nameForSearch = ($name -replace '\s*\[[^\]]*\]\s*', ' ' -replace '\s+', ' ').Trim()
    $nameNorm = Normalize-Text $nameForSearch
    $locality = Get-LocalityToken ([string]$spot.Address)
    $query = "$nameForSearch $locality 관광"
    $searchUrl = "https://search.naver.com/search.naver?where=image&sm=tab_jum&query=$([uri]::EscapeDataString($query))"
    $base = [ordered]@{
        TouristSpotId = $spot.TouristSpotId
        TourApiContentId = $spot.TourApiContentId
        Name = $spot.Name
        Address = $spot.Address
        Status = 'NO_WEB_CANDIDATE'
        ImageUrl = ''
        SourcePageUrl = ''
        SourceTitle = ''
        ProviderHost = ''
        HttpStatus = ''
        ContentType = ''
        VerifiedAt = (Get-Date).ToString('yyyy-MM-dd')
        Notes = ''
    }

    try {
        Start-Sleep -Milliseconds (Get-Random -Minimum 350 -Maximum 900)
        $response = Invoke-WebRequest -UseBasicParsing -Uri $searchUrl -TimeoutSec 30 -Headers @{
            'User-Agent' = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/140.0 Safari/537.36'
            'Accept-Language' = 'ko-KR,ko;q=0.9,en;q=0.7'
            'Referer' = 'https://search.naver.com/'
        }
        $matches = [regex]::Matches($response.Content, '"originalUrl":"(?<url>(?:\\.|[^"\\])*)"')
        $candidates = [System.Collections.Generic.List[object]]::new()
        foreach ($match in $matches) {
            $imageUrl = Decode-JsonString $match.Groups['url'].Value
            if (-not $imageUrl.StartsWith('http')) { continue }
            $imageHost = Get-Host $imageUrl
            $snippetLength = [Math]::Min(7000, $response.Content.Length - $match.Index)
            $snippet = $response.Content.Substring($match.Index, $snippetLength)
            $titleMatch = [regex]::Match($snippet, '"artclTitle":"(?<title>(?:\\.|[^"\\])*)"')
            $pageMatch = [regex]::Match($snippet, '"cntsUrl":"(?<page>(?:\\.|[^"\\])*)"')
            $encodedTitle = if ($titleMatch.Success) { (Decode-JsonString $titleMatch.Groups['title'].Value) -replace '\+', ' ' } else { '' }
            $encodedPage = if ($pageMatch.Success) { (Decode-JsonString $pageMatch.Groups['page'].Value) -replace '\+', ' ' } else { '' }
            $title = if ($encodedTitle) { [uri]::UnescapeDataString($encodedTitle) } else { '' }
            $pageUrl = if ($encodedPage) { [uri]::UnescapeDataString($encodedPage) } else { '' }
            $pageHost = Get-Host $pageUrl
            $titleNorm = Normalize-Text $title
            $nameMatched = $nameNorm.Length -ge 3 -and $titleNorm.Length -ge 3 -and ($titleNorm.Contains($nameNorm) -or $nameNorm.Contains($titleNorm))
            if (-not $nameMatched) { continue }

            $score = 80
            if ($locality -and $title.Contains($locality)) { $score += 20 }
            if ($imageHost -eq 'tong.visitkorea.or.kr') { $score += 60 }
            elseif ($imageHost -match '(^|\.)((go|or|ac)\.kr)$') { $score += 45 }
            elseif ($imageHost -eq 'upload.wikimedia.org') { $score += 40 }
            elseif ($imageHost -match '(visit|tour|museum|gallery|heritage)') { $score += 15 }
            elseif ($pageHost -match '(^|\.)((go|or|ac)\.kr)$' -or $pageHost -match '(visitkorea|visitseoul|ggtour|k-heritage)') { $score += 35 }
            else { continue }

            $providerHost = if ($pageHost) { $pageHost } else { $imageHost }
            $candidates.Add([pscustomobject]@{ Score = $score; ImageUrl = $imageUrl; PageUrl = $pageUrl; Title = $title; ProviderHost = $providerHost; ImageHost = $imageHost })
        }

        foreach ($candidate in @($candidates | Sort-Object Score -Descending | Select-Object -First 8)) {
            $check = Test-ImageUrl $candidate.ImageUrl
            if (-not $check.Valid) { continue }
            $base.Status = if ($candidate.ImageHost -match '(^|\.)visitkorea\.or\.kr$') { 'APPROVED_TOURAPI_INDEX' } else { 'APPROVED_OFFICIAL_WEB' }
            $base.ImageUrl = $candidate.ImageUrl
            $base.SourcePageUrl = $candidate.PageUrl
            $base.SourceTitle = $candidate.Title
            $base.ProviderHost = $candidate.ProviderHost
            $base.HttpStatus = $check.HttpStatus
            $base.ContentType = $check.ContentType
            $base.Notes = "네이버 이미지 검색 원문 제목에서 장소명 일치, 검색어에 지역 포함, 이미지 GET 응답 확인; 점수 $($candidate.Score)"
            break
        }
        if ($base.Status -eq 'NO_WEB_CANDIDATE') {
            $base.Notes = "장소명이 원문 제목과 일치하고 신뢰 가능한 호스트에서 정상 응답한 후보 없음"
        }
    } catch {
        $base.Status = 'SEARCH_ERROR'
        $base.Notes = $_.Exception.Message
    }
    [pscustomobject]$base
} -ThrottleLimit 1

$results | Sort-Object { [long]$_.TouristSpotId } | Export-Csv -LiteralPath $outputPath -NoTypeInformation -Encoding utf8BOM
$results | Group-Object Status | Sort-Object Name | Format-Table Name, Count -AutoSize
Write-Host "Saved $($results.Count) rows to $outputPath"
