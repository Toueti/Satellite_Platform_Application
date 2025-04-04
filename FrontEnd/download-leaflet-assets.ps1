$urls = @(
    "https://raw.githubusercontent.com/Leaflet/Leaflet/main/dist/images/marker-icon-2x.png",
    "https://raw.githubusercontent.com/Leaflet/Leaflet/main/dist/images/marker-icon.png",
    "https://raw.githubusercontent.com/Leaflet/Leaflet/main/dist/images/marker-shadow.png"
)

$destination = "public/leaflet"

foreach ($url in $urls) {
    $fileName = Split-Path $url -Leaf
    $outFile = Join-Path $destination $fileName
    Invoke-WebRequest -Uri $url -OutFile $outFile
    Write-Host "Downloaded $fileName"
}
