$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

$workspaceRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$mysql = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
$outputPath = Join-Path $workspaceRoot "backend\docs\missing-image-source-audit.csv"

$sql = @"
SELECT ts.id, ts.source_content_id, ts.name, COALESCE(ts.road_address, ts.lot_address, '')
FROM tourist_spot ts
WHERE ts.active = 1
  AND NULLIF(TRIM(ts.thumbnail_url), '') IS NULL
  AND NULLIF(TRIM(ts.original_image_url), '') IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM tourist_spot_image image
      WHERE image.tourist_spot_id = ts.id
        AND NULLIF(TRIM(image.url), '') IS NOT NULL
  )
ORDER BY ts.id
"@

$rows = & $mysql --protocol=TCP -h 127.0.0.1 -P 3307 -u root `
    --default-character-set=utf8mb4 --batch --raw --skip-column-names danyeogam -e $sql
if ($LASTEXITCODE -ne 0) {
    throw "Failed to read missing-image tourist spots from MySQL."
}

$spots = foreach ($row in $rows) {
    $columns = $row -split "`t", 4
    if ($columns.Count -eq 4) {
        [pscustomobject]@{
            TouristSpotId = [long]$columns[0]
            TourApiContentId = $columns[1]
            Name = $columns[2]
            Address = $columns[3]
        }
    }
}

if ($spots.Count -ne 202) {
    throw "Expected 202 missing-image spots, but found $($spots.Count)."
}

$results = $spots | ForEach-Object -Parallel {
    $spot = $_
    $ProgressPreference = "SilentlyContinue"

    function Normalize-Name([string]$value) {
        if ([string]::IsNullOrWhiteSpace($value)) { return "" }
        return (($value -replace '\[[^\]]*\]', '') -replace '\([^\)]*\)', '' -replace '[^\p{L}\p{Nd}]', '').ToLowerInvariant()
    }

    function Get-NodeText($node) {
        if ($null -eq $node) { return "" }
        if ($node -is [System.Xml.XmlNode]) { return $node.InnerText }
        return [string]$node
    }

    function Get-XmlDocument([string]$url) {
        for ($attempt = 1; $attempt -le 3; $attempt++) {
            try {
                $response = Invoke-WebRequest -Uri $url -TimeoutSec 25 -Headers @{ "User-Agent" = "DanyeogamImageAudit/1.0" }
                return [xml]$response.Content
            } catch {
                if ($attempt -eq 3) { throw }
                Start-Sleep -Milliseconds (300 * $attempt)
            }
        }
    }

    function Test-ImageUrl([string]$url) {
        try {
            $handler = [System.Net.Http.HttpClientHandler]::new()
            $handler.AllowAutoRedirect = $true
            $client = [System.Net.Http.HttpClient]::new($handler)
            $client.Timeout = [TimeSpan]::FromSeconds(25)
            $client.DefaultRequestHeaders.UserAgent.ParseAdd("DanyeogamImageAudit/1.0")
            $request = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::Get, $url)
            $request.Headers.Range = [System.Net.Http.Headers.RangeHeaderValue]::new(0, 0)
            $response = $client.SendAsync($request, [System.Net.Http.HttpCompletionOption]::ResponseHeadersRead).GetAwaiter().GetResult()
            $contentType = $response.Content.Headers.ContentType.MediaType
            $valid = ([int]$response.StatusCode -in @(200, 206)) -and $contentType -like 'image/*'
            return [pscustomobject]@{
                Valid = $valid
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

    $base = [ordered]@{
        TouristSpotId = $spot.TouristSpotId
        TourApiContentId = $spot.TourApiContentId
        Name = $spot.Name
        Address = $spot.Address
        Status = "NO_VERIFIED_IMAGE"
        Provider = ""
        ProviderItemName = ""
        ImageUrl = ""
        SourcePageUrl = ""
        LicenseCode = ""
        LicenseUrl = ""
        Attribution = ""
        ImageDescription = ""
        HttpStatus = ""
        ContentType = ""
        VerifiedAt = (Get-Date).ToString("yyyy-MM-dd")
        Notes = "공식·공개 라이선스 이미지 후보를 확인하지 못함"
    }

    # This page and its images were manually checked before the batch audit.
    if ($spot.TouristSpotId -eq 796) {
        $imageUrl = "https://www.yeongjong.go.kr/other/attach/process.file.do?TP=dn&out=inline&w=1000&sn=30946&key=3B733D3D2EADEDA"
        $check = Test-ImageUrl $imageUrl
        $base.Status = if ($check.Valid) { "APPROVED" } else { "URL_FAILED" }
        $base.Provider = "인천광역시 영종구청"
        $base.ProviderItemName = "영종역사관"
        $base.ImageUrl = $imageUrl
        $base.SourcePageUrl = "https://www.yeongjong.go.kr/tour/regional_tour/yeongjong/view.do?trspt_sn=217"
        $base.LicenseCode = "KOGL_TYPE_1"
        $base.LicenseUrl = "https://www.kogl.or.kr/info/licenseType1.do"
        $base.Attribution = "출처: 인천광역시 영종구청 (공공누리 제1유형)"
        $base.ImageDescription = "영종역사관 관광지 이미지"
        $base.HttpStatus = $check.HttpStatus
        $base.ContentType = $check.ContentType
        $base.Notes = "장소명·주소·페이지 라이선스·이미지 응답 확인. 동적 URL이므로 장기 핫링크 금지"
        [pscustomobject]$base
        return
    }

    $queries = [System.Collections.Generic.List[string]]::new()
    $queries.Add($spot.Name)
    $withoutBrackets = ($spot.Name -replace '\s*\[[^\]]*\]\s*', ' ').Trim()
    if ($withoutBrackets -ne $spot.Name) { $queries.Add($withoutBrackets) }
    $withoutParentheses = ($withoutBrackets -replace '\s*\([^\)]*\)\s*', ' ').Trim()
    if ($withoutParentheses -and -not $queries.Contains($withoutParentheses)) { $queries.Add($withoutParentheses) }

    $bestItem = $null
    $bestScore = -1
    $bestQuery = ""
    $addressTokens = @($spot.Address -split '\s+' | Where-Object { $_ -match '(시|군|구)$' })

    foreach ($query in $queries) {
        try {
            $encoded = [uri]::EscapeDataString($query)
            $listUrl = "https://www.cha.go.kr/cha/SearchKindOpenapiList.do?ccbaMnm1=$encoded&pageIndex=1"
            $xml = Get-XmlDocument $listUrl
            foreach ($item in @($xml.result.item)) {
                if ($null -eq $item) { continue }
                $officialName = Get-NodeText $item.ccbaMnm1
                $queryNorm = Normalize-Name $query
                $officialNorm = Normalize-Name $officialName
                $score = 0
                $addressMatched = $false
                if ($queryNorm -and $officialNorm -eq $queryNorm) { $score += 100 }
                elseif ($queryNorm.Length -ge 3 -and ($officialNorm.Contains($queryNorm) -or $queryNorm.Contains($officialNorm))) { $score += 60 }
                foreach ($token in $addressTokens) {
                    if ((Get-NodeText $item.ccsiName) -eq $token) {
                        $score += 25
                        $addressMatched = $true
                        break
                    }
                }
                if ((Get-NodeText $item.ccbaCncl) -eq 'N') { $score += 5 }
                if ($score -gt $bestScore) {
                    $bestScore = $score
                    $bestItem = [pscustomobject]@{
                        Name = $officialName
                        Kdcd = Get-NodeText $item.ccbaKdcd
                        Ctcd = Get-NodeText $item.ccbaCtcd
                        Asno = Get-NodeText $item.ccbaAsno
                        AddressMatched = $addressMatched
                    }
                    $bestQuery = $query
                }
            }
        } catch {
            $base.Notes = "국가유산청 목록 API 오류: $($_.Exception.Message)"
        }
    }

    # 85점 미만은 이름이 같은 타 지역 문화유산일 수 있다. 예를 들어
    # 경산 동산서당과 창녕 동산서당처럼 명칭 유사도만으로는 안전하지 않다.
    if ($null -ne $bestItem -and $bestScore -ge 85 -and $bestItem.AddressMatched) {
        try {
            $kdcd = $bestItem.Kdcd
            $ctcd = $bestItem.Ctcd
            $asno = $bestItem.Asno
            $imageApiUrl = "https://www.cha.go.kr/cha/SearchImageOpenapi.do?ccbaKdcd=$kdcd&ccbaCtcd=$ctcd&ccbaAsno=$asno"
            $imageXml = Get-XmlDocument $imageApiUrl
            $urls = @($imageXml.result.item.imageUrl)
            $nuriCodes = @($imageXml.result.item.imageNuri)
            $descriptions = @($imageXml.result.item.ccimDesc)
            $selectedIndex = -1
            for ($index = 0; $index -lt $urls.Count; $index++) {
                if (-not [string]::IsNullOrWhiteSpace((Get-NodeText $urls[$index])) -and (Get-NodeText $nuriCodes[$index]) -eq 'A') {
                    $selectedIndex = $index
                    break
                }
            }
            if ($selectedIndex -lt 0) {
                for ($index = 0; $index -lt $urls.Count; $index++) {
                    if (-not [string]::IsNullOrWhiteSpace((Get-NodeText $urls[$index]))) {
                        $selectedIndex = $index
                        break
                    }
                }
            }

            if ($selectedIndex -ge 0) {
                $imageUrl = (Get-NodeText $urls[$selectedIndex]) -replace '^http://www\.khs\.go\.kr/', 'https://www.khs.go.kr/'
                $check = Test-ImageUrl $imageUrl
                $nuri = Get-NodeText $nuriCodes[$selectedIndex]
                $base.Status = if ($check.Valid -and $nuri -eq 'A') { "APPROVED" } elseif ($check.Valid) { "CONDITIONAL" } else { "URL_FAILED" }
                $base.Provider = "국가유산청 국가유산포털"
                $base.ProviderItemName = $bestItem.Name
                $base.ImageUrl = $imageUrl
                $base.SourcePageUrl = "https://www.heritage.go.kr/heri/cul/culSelectDetail.do?ccbaAsno=$asno&ccbaCtcd=$ctcd&ccbaKdcd=$kdcd&pageNo=1_1_1_0"
                $base.LicenseCode = "KHS_IMAGE_NURI_$nuri"
                $base.LicenseUrl = "https://www.heritage.go.kr/heri/html/HtmlPage.do?pageNo=1_5_1_0"
                $base.Attribution = "출처: 국가유산청 국가유산포털"
                $base.ImageDescription = Get-NodeText $descriptions[$selectedIndex]
                $base.HttpStatus = $check.HttpStatus
                $base.ContentType = $check.ContentType
                $base.Notes = "이름·소재지 자동 대조 점수 $bestScore; 검색어 '$bestQuery'; imageNuri=$nuri"
                [pscustomobject]$base
                return
            }
            $base.Status = "OFFICIAL_NO_IMAGE"
            $base.Provider = "국가유산청 국가유산포털"
            $base.ProviderItemName = $bestItem.Name
            $base.SourcePageUrl = "https://www.heritage.go.kr/heri/cul/culSelectDetail.do?ccbaAsno=$asno&ccbaCtcd=$ctcd&ccbaKdcd=$kdcd&pageNo=1_1_1_0"
            $base.Notes = "공식 국가유산 항목은 일치하지만 이미지 API가 사진을 제공하지 않음; 대조 점수 $bestScore"
        } catch {
            $base.Status = "SOURCE_ERROR"
            $base.Provider = "국가유산청 국가유산포털"
            $base.ProviderItemName = $bestItem.Name
            $base.Notes = "국가유산청 이미지 API 오류: $($_.Exception.Message)"
        }
    }

    # Openverse is candidate discovery only. Its own documentation requires checking
    # each work's source record, so these results are never auto-approved.
    try {
        $encoded = [uri]::EscapeDataString($withoutBrackets)
        $openverseUrl = "https://api.openverse.org/v1/images/?q=$encoded&page_size=5&license=cc0,by,by-sa,pdm"
        $openverse = Invoke-RestMethod -Uri $openverseUrl -TimeoutSec 25 -Headers @{ "User-Agent" = "DanyeogamImageAudit/1.0" }
        $spotNorm = Normalize-Name $withoutBrackets
        $candidate = $openverse.results | Where-Object {
            $titleNorm = Normalize-Name ([string]($_.title))
            $titleNorm -and $spotNorm -and ($titleNorm.Contains($spotNorm) -or $spotNorm.Contains($titleNorm))
        } | Select-Object -First 1
        if ($null -ne $candidate) {
            $check = Test-ImageUrl ([string]($candidate.url))
            $base.Status = "OPENVERSE_REVIEW"
            $base.Provider = "Openverse/$($candidate.source)"
            $base.ProviderItemName = [string]($candidate.title)
            $base.ImageUrl = [string]($candidate.url)
            $base.SourcePageUrl = [string]($candidate.foreign_landing_url)
            $base.LicenseCode = ([string]($candidate.license)).ToUpperInvariant()
            $base.LicenseUrl = [string]($candidate.license_url)
            $base.Attribution = "작가: $($candidate.creator); 원문과 라이선스 재확인 필요"
            $base.ImageDescription = [string]($candidate.title)
            $base.HttpStatus = $check.HttpStatus
            $base.ContentType = $check.ContentType
            $base.Notes = "공개 라이선스 검색 후보. Openverse 안내에 따라 원 제공처의 라이선스와 장소를 수동 재확인해야 함"
        }
    } catch {
        if ($base.Status -eq "NO_VERIFIED_IMAGE") {
            $base.Notes = "Openverse 조회 오류: $($_.Exception.Message)"
        }
    }

    [pscustomobject]$base
} -ThrottleLimit 6

$orderedResults = $results | Sort-Object TouristSpotId

# 2026-09-15 Wikimedia Commons 원문 API에서 제목, 설명, 작가, 원본 URL과
# 라이선스를 재검증한 항목이다. CDN은 자동 검사 중 429를 반환했으므로 앱에서는
# 원격 핫링크하지 않고 라이선스 조건에 맞춰 자체 저장소에 보관해야 한다.
$commonsVerifiedIds = @(6370, 25741, 30556, 31529, 32363, 40526, 46703)
foreach ($row in $orderedResults) {
    if ($row.TouristSpotId -in $commonsVerifiedIds -and $row.Status -eq 'OPENVERSE_REVIEW') {
        $row.Status = 'APPROVED_REHOST_REQUIRED'
        $row.Notes = 'Wikimedia Commons 원문 API에서 장소 설명·작가·라이선스 확인. CDN 핫링크 대신 자체 저장 필요'
    } elseif ($row.TouristSpotId -in @(27799, 29965) -and $row.Status -eq 'OPENVERSE_REVIEW') {
        $row.Status = 'COMMONS_SOURCE_MISSING'
        $row.Notes = 'Openverse가 가리킨 Commons pageid가 원문 API에서 missing으로 확인되어 사용 제외'
    } elseif ($row.TouristSpotId -eq 38581 -and $row.Status -eq 'OPENVERSE_REVIEW') {
        $row.Status = 'SOURCE_PROVENANCE_WEAK'
        $row.Notes = 'Commons 메타데이터의 원출처가 네이버 이미지 검색 경유 링크여서 권리 사슬을 확정할 수 없어 사용 제외'
    }
}

$orderedResults | Export-Csv -LiteralPath $outputPath -NoTypeInformation -Encoding utf8BOM

$summary = $orderedResults | Group-Object Status | Sort-Object Count -Descending | Select-Object Name, Count
$summary | Format-Table -AutoSize
Write-Host "Audited: $($orderedResults.Count)"
Write-Host "Output: $outputPath"
