$ErrorActionPreference = 'Continue'
Set-Location $PSScriptRoot
$log = Join-Path $PSScriptRoot 'build.log'
"=== start $(Get-Date)" | Out-File $log -Encoding utf8
# Find a JDK 21
$cands = @()
if ($env:JAVA_HOME) { $cands += $env:JAVA_HOME }
$roots = @("$env:ProgramFiles\Java","$env:ProgramFiles\Eclipse Adoptium","$env:ProgramFiles\Microsoft","$env:ProgramFiles\Zulu","$env:ProgramFiles\BellSoft","$env:APPDATA\PrismLauncher\java","$env:LOCALAPPDATA\Programs\PrismLauncher\java","$env:USERPROFILE\.jdks","$env:USERPROFILE\scoop\apps","$env:USERPROFILE\.gradle\jdks")
foreach ($r in $roots) { if (Test-Path $r) { Get-ChildItem $r -Recurse -Filter java.exe -ErrorAction SilentlyContinue -Depth 4 | ForEach-Object { $cands += (Split-Path (Split-Path $_.FullName)) } } }
$jdk = $null
foreach ($c in ($cands | Select-Object -Unique)) {
  $j = Join-Path $c 'bin\java.exe'; $jc = Join-Path $c 'bin\javac.exe'
  if ((Test-Path $j) -and (Test-Path $jc)) {
    $v = (& $j -version 2>&1 | Out-String)
    "candidate $c :: $($v.Split("`n")[0])" | Out-File $log -Append -Encoding utf8
    if (-not $jdk -and $v -match 'version "21') { $jdk = $c }
  }
}
if (-not $jdk) { "NO JDK 21 FOUND" | Out-File $log -Append -Encoding utf8; "=== EXIT 99" | Out-File $log -Append -Encoding utf8; exit 99 }
"USING JDK $jdk" | Out-File $log -Append -Encoding utf8
$env:JAVA_HOME = $jdk; $env:Path = "$jdk\bin;$env:Path"
# Java не переносит «!» в пути (это разделитель в адресах jar:file:…!/…): игра (датаген, runClient) не находит Minecraft.
# Поэтому на время сборки подключаем папку как отдельный диск без «!» в пути.
$drive = $null
$code = 1
try {
  if ($PSScriptRoot.Contains('!')) {
    # Диск, оставшийся с прошлого запуска (например, окно закрыли посреди сборки), — отключаем.
    foreach ($line in (subst)) {
      if ($line -match '^([A-Z]):\\: => (.+)$' -and $Matches[2].TrimEnd('\') -eq $PSScriptRoot.TrimEnd('\')) { subst "$($Matches[1]):" /d | Out-Null }
    }
    $used = (Get-PSDrive -PSProvider FileSystem).Name
    $drive = @('X','W','V','U','T','S','R','Q') | Where-Object { $used -notcontains $_ } | Select-Object -First 1
    if (-not $drive) { "NO FREE DRIVE LETTER" | Out-File $log -Append -Encoding utf8; $code = 98; return }
    subst "${drive}:" "$PSScriptRoot"
    Set-Location "${drive}:\"
    "USING DRIVE ${drive}: for $PSScriptRoot" | Out-File $log -Append -Encoding utf8
  }
  # stderr склеивается внутри cmd: иначе PowerShell оборачивает каждую строку javac/Gradle в NativeCommandError
  cmd /c "gradlew.bat build --console=plain --no-daemon 2>&1" | Out-File $log -Append -Encoding utf8
  $code = $LASTEXITCODE
} finally {
  if ($drive) { Set-Location $PSScriptRoot; subst "${drive}:" /d | Out-Null }
  "=== EXIT $code" | Out-File $log -Append -Encoding utf8
}
