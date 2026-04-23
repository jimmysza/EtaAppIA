# Script para iniciar desarrollo con ePayco + ngrok
# Uso: .\start-epayco-dev.ps1

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "  INICIANDO DESARROLLO CON EPAYCO + NGROK" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host ""

# Verificar si ngrok está instalado
$ngrokInstalled = Get-Command ngrok -ErrorAction SilentlyContinue
if (-not $ngrokInstalled) {
    Write-Host "❌ ERROR: ngrok no está instalado" -ForegroundColor Red
    Write-Host ""
    Write-Host "Instala ngrok con:" -ForegroundColor Yellow
    Write-Host "  choco install ngrok" -ForegroundColor Yellow
    Write-Host "O descarga desde: https://ngrok.com/download" -ForegroundColor Yellow
    exit 1
}

Write-Host "✅ ngrok encontrado" -ForegroundColor Green
Write-Host ""

# Verificar si Spring Boot está corriendo en puerto 8080
$port8080 = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
if (-not $port8080) {
    Write-Host "⚠️  ADVERTENCIA: No se detectó Spring Boot en puerto 8080" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Debes iniciar tu aplicación primero:" -ForegroundColor Yellow
    Write-Host "  mvnw spring-boot:run" -ForegroundColor Yellow
    Write-Host ""
    $continue = Read-Host "¿Quieres continuar de todos modos? (s/n)"
    if ($continue -ne "s" -and $continue -ne "S") {
        exit 0
    }
} else {
    Write-Host "✅ Spring Boot está corriendo en puerto 8080" -ForegroundColor Green
}

Write-Host ""
Write-Host "🌐 Iniciando ngrok..." -ForegroundColor Cyan
Write-Host ""

# Iniciar ngrok en background y capturar la URL
$ngrokJob = Start-Job -ScriptBlock {
    ngrok http 8080 --log=stdout
}

# Esperar 5 segundos para que ngrok se inicie
Start-Sleep -Seconds 5

# Obtener la URL de ngrok desde la API local
try {
    $ngrokApi = Invoke-RestMethod -Uri "http://127.0.0.1:4040/api/tunnels" -ErrorAction Stop
    $ngrokUrl = $ngrokApi.tunnels[0].public_url
    
    if (-not $ngrokUrl) {
        throw "No se pudo obtener la URL de ngrok"
    }
    
    Write-Host "✅ ngrok iniciado correctamente" -ForegroundColor Green
    Write-Host ""
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
    Write-Host "  URL PÚBLICA: $ngrokUrl" -ForegroundColor Green
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
    Write-Host ""
    
    # Preguntar si quiere actualizar checkout.html automáticamente
    Write-Host "¿Quieres actualizar checkout.html con esta URL? (s/n): " -NoNewline -ForegroundColor Yellow
    $update = Read-Host
    
    if ($update -eq "s" -or $update -eq "S") {
        $checkoutPath = "src\main\resources\templates\cliente\checkout.html"
        
        if (Test-Path $checkoutPath) {
            $content = Get-Content $checkoutPath -Raw
            
            # Buscar y reemplazar la línea de baseUrl
            $pattern = 'const baseUrl = "https://[^"]+";'
            $replacement = "const baseUrl = `"$ngrokUrl`";"
            
            if ($content -match $pattern) {
                $newContent = $content -replace $pattern, $replacement
                Set-Content -Path $checkoutPath -Value $newContent -NoNewline
                
                Write-Host ""
                Write-Host "✅ checkout.html actualizado con la nueva URL" -ForegroundColor Green
                Write-Host ""
            } else {
                Write-Host ""
                Write-Host "⚠️  No se encontró el patrón 'const baseUrl' en checkout.html" -ForegroundColor Yellow
                Write-Host "   Actualiza manualmente la línea ~663:" -ForegroundColor Yellow
                Write-Host "   const baseUrl = `"$ngrokUrl`";" -ForegroundColor Yellow
                Write-Host ""
            }
        } else {
            Write-Host ""
            Write-Host "❌ No se encontró $checkoutPath" -ForegroundColor Red
            Write-Host ""
        }
    } else {
        Write-Host ""
        Write-Host "⚠️  Recuerda actualizar manualmente checkout.html:" -ForegroundColor Yellow
        Write-Host "   Línea ~663: const baseUrl = `"$ngrokUrl`";" -ForegroundColor Yellow
        Write-Host ""
    }
    
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
    Write-Host "  WEBHOOKS DE EPAYCO:" -ForegroundColor Cyan
    Write-Host "  $ngrokUrl/cliente/pago/confirmacion" -ForegroundColor Green
    Write-Host ""
    Write-Host "  DASHBOARD DE NGROK:" -ForegroundColor Cyan
    Write-Host "  http://127.0.0.1:4040" -ForegroundColor Green
    Write-Host ""
    Write-Host "  TU APLICACIÓN:" -ForegroundColor Cyan
    Write-Host "  http://localhost:8080" -ForegroundColor Green
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "🎯 TODO LISTO PARA PROBAR EPAYCO" -ForegroundColor Green
    Write-Host ""
    Write-Host "Presiona Ctrl+C para detener ngrok" -ForegroundColor Yellow
    Write-Host ""
    
    # Esperar a que el usuario presione Ctrl+C
    Wait-Job $ngrokJob
    
} catch {
    Write-Host ""
    Write-Host "❌ ERROR al obtener la URL de ngrok" -ForegroundColor Red
    Write-Host "   Mensaje: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    Write-Host "Verifica que ngrok esté corriendo:" -ForegroundColor Yellow
    Write-Host "  1. Abre otra terminal" -ForegroundColor Yellow
    Write-Host "  2. Ejecuta: ngrok http 8080" -ForegroundColor Yellow
    Write-Host "  3. Copia la URL manualmente" -ForegroundColor Yellow
    Write-Host ""
    
    Stop-Job $ngrokJob
    Remove-Job $ngrokJob
    exit 1
}

# Cleanup
Stop-Job $ngrokJob
Remove-Job $ngrokJob
