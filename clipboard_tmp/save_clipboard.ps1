Add-Type -AssemblyName System.Windows.Forms
$img = [System.Windows.Forms.Clipboard]::GetImage()
if ($img) {
  $dir = "F:\claude"
  if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
  $timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
  $randomId = -join ((48..57) + (97..122) | Get-Random -Count 8 | ForEach-Object { [char]$_ })
  $path = "$dir\clip_${timestamp}_${randomId}_$(Get-Random -Max 9999).png"
  $img.Save($path)
  Write-Host "IMAGE_SAVED:$path"
} else {
  Write-Host "NO_IMAGE_IN_CLIPBOARD"
}
