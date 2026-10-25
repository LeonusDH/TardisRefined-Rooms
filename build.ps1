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
# stderr склеивается внутри cmd: иначе PowerShell оборачивает каждую строку javac/Gradle в NativeCommandError
cmd /c "gradlew.bat build --console=plain --no-daemon 2>&1" | Out-File $log -Append -Encoding utf8
"=== EXIT $LASTEXITCODE" | Out-File $log -Append -Encoding utf8
