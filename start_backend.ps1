# start_backend.ps1
# Script to open multiple Command Prompt windows for Satellite Platform Application backend

# Define paths (adjust these to your actual paths if needed)
$imageProcessingPath = "C:\Users\User\Desktop\PFA2\Projects\ProjectVersionFinal\image_porcessing"
$geeAppPath = "C:\Users\User\Desktop\PFA2\Projects\ProjectVersionFinal\gee_app_with_cache_logic"
$springBootPath = "C:\Users\User\Desktop\PFA2\Projects\ProjectVersionFinal\BackEnd"

# Start Redis and RabbitMQ in a WSL terminal window
Write-Host "Opening WSL terminal to start Redis and RabbitMQ..."
# Use wsl.exe to open a terminal and run the start commands
Start-Process wsl.exe -ArgumentList "--distribution Ubuntu-20.04 --exec /bin/bash -c 'sudo service redis-server start && sudo systemctl start rabbitmq-server && echo Services started. Press Enter to continue... && read'"
Write-Host "WSL terminal opened. Waiting for services to start..."
# Add a small delay to ensure services start (adjust as needed)
Start-Sleep -Seconds 5
Write-Host "Proceeding with backend startup..."

# Function to check if a path exists and log the result
function Test-PathWithLog {
    param (
        [string]$path,
        [string]$description
    )
    Write-Host "Checking $description at: $path"
    if (Test-Path $path) {
        Write-Host "$description exists."
        return $true
    } else {
        Write-Host "$description does not exist!"
        return $false
    }
}

# Window 1: Image Processing (REST_API_version2.py)
Write-Host "Setting up Window 1: Image Processing..."
if (Test-PathWithLog -path $imageProcessingPath -description "Image Processing directory") {
    $pythonFile1 = Join-Path $imageProcessingPath "app\REST_API_version2.py"
    if (Test-PathWithLog -path $pythonFile1 -description "REST_API_version2.py") {
        Write-Host "Running command: python ./app/REST_API_version2.py in $imageProcessingPath"
        Start-Process cmd.exe -ArgumentList "/k cd /d `"$imageProcessingPath`" && python ./app/REST_API_version2.py"
        Write-Host "Window 1 started."
    } else {
        Write-Host "Skipping Window 1: Python script not found."
    }
} else {
    Write-Host "Skipping Window 1: Directory not found."
}

# Window 2: GEE App (app.py)
Write-Host "Setting up Window 2: GEE App..."
if (Test-PathWithLog -path $geeAppPath -description "GEE App directory") {
    $pythonFile2 = Join-Path $geeAppPath "app.py"
    if (Test-PathWithLog -path $pythonFile2 -description "app.py") {
        Write-Host "Running command: python app.py in $geeAppPath"
        Start-Process cmd.exe -ArgumentList "/k cd /d `"$geeAppPath`" && python app.py"
        Write-Host "Window 2 started."
    } else {
        Write-Host "Skipping Window 2: Python script not found."
    }
} else {
    Write-Host "Skipping Window 2: Directory not found."
}

# Window 3: MongoDB (mongod)
Write-Host "Setting up Window 3: MongoDB..."
Write-Host "Running command: mongod"
Start-Process cmd.exe -ArgumentList "/k mongod"
Write-Host "Window 3 started."

# Window 4: Spring Boot (mvn spring-boot:run)
Write-Host "Setting up Window 4: Spring Boot..."
if (Test-PathWithLog -path $springBootPath -description "Spring Boot directory") {
    Write-Host "Running command: mvn spring-boot:run in $springBootPath"
    Start-Process cmd.exe -ArgumentList "/k cd /d `"$springBootPath`" && mvn spring-boot:run"
    Write-Host "Window 4 started."
} else {
    Write-Host "Skipping Window 4: Directory not found."
}