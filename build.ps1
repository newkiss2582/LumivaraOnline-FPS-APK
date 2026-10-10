param(
  [Parameter(Mandatory=$true)][string]$PlatformJar,
  [Parameter(Mandatory=$true)][string]$BuildTools,
  [Parameter(Mandatory=$true)][string]$SigningDir,
  [Parameter(Mandatory=$true)][string]$OutputApk
)
$ErrorActionPreference = 'Stop'
$projectDir = $PSScriptRoot
$buildDir = Join-Path $projectDir 'build'
$PlatformJar = (Resolve-Path -LiteralPath $PlatformJar).Path
$BuildTools = (Resolve-Path -LiteralPath $BuildTools).Path
$javaHome = ((& java -XshowSettings:properties -version 2>&1 | Select-String 'java.home = (.+)').Matches.Groups[1].Value).Trim()
foreach ($sub in @('generated','classes','dex')) { New-Item -ItemType Directory -Path (Join-Path $buildDir $sub) -Force | Out-Null }
function Check-BuildStep { if ($LASTEXITCODE -ne 0) { throw "Build tool failed with exit code $LASTEXITCODE" } }
& (Join-Path $BuildTools 'aapt2.exe') compile --dir (Join-Path $projectDir 'res') -o (Join-Path $buildDir 'resources.zip')
Check-BuildStep
& (Join-Path $BuildTools 'aapt2.exe') link -o (Join-Path $buildDir 'unsigned.apk') -I $PlatformJar --manifest (Join-Path $projectDir 'AndroidManifest.xml') -A (Join-Path $projectDir 'assets') --java (Join-Path $buildDir 'generated') --min-sdk-version 26 --target-sdk-version 36 --version-code 1 --version-name '1.0.0' (Join-Path $buildDir 'resources.zip')
Check-BuildStep
$javaFiles = @(Get-ChildItem -LiteralPath (Join-Path $projectDir 'src') -Recurse -Filter '*.java' | Select-Object -ExpandProperty FullName)
$javaFiles += @(Get-ChildItem -LiteralPath (Join-Path $buildDir 'generated') -Recurse -Filter '*.java' | Select-Object -ExpandProperty FullName)
& javac --release 8 -encoding UTF-8 -classpath $PlatformJar -d (Join-Path $buildDir 'classes') @javaFiles
Check-BuildStep
& (Join-Path $javaHome 'bin\jar.exe') cf (Join-Path $buildDir 'classes.jar') -C (Join-Path $buildDir 'classes') .
Check-BuildStep
& java -cp (Join-Path $BuildTools 'lib\d8.jar') com.android.tools.r8.D8 --lib $PlatformJar --min-api 26 --output (Join-Path $buildDir 'dex') (Join-Path $buildDir 'classes.jar')
Check-BuildStep
Add-Type -AssemblyName System.IO.Compression.FileSystem
$apkZip = [IO.Compression.ZipFile]::Open((Join-Path $buildDir 'unsigned.apk'), [IO.Compression.ZipArchiveMode]::Update)
try { foreach ($dex in Get-ChildItem -LiteralPath (Join-Path $buildDir 'dex') -Filter '*.dex') { [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($apkZip, $dex.FullName, $dex.Name) | Out-Null } }
finally { $apkZip.Dispose() }
& (Join-Path $BuildTools 'zipalign.exe') -p -f 4 (Join-Path $buildDir 'unsigned.apk') (Join-Path $buildDir 'aligned.apk')
Check-BuildStep
New-Item -ItemType Directory -Path $SigningDir -Force | Out-Null
$keyFile = Join-Path $SigningDir 'lumivara-android.jks'
$passwordFile = Join-Path $SigningDir 'signing-password.txt'
if (-not (Test-Path -LiteralPath $keyFile) -or -not (Test-Path -LiteralPath $passwordFile)) { throw 'Existing signing key and password are required for an update build.' }
$env:LUMIVARA_STOREPASS = [IO.File]::ReadAllText($passwordFile)

try {
  & java -jar (Join-Path $BuildTools 'lib\apksigner.jar') sign --ks $keyFile --ks-key-alias lumivara --ks-pass env:LUMIVARA_STOREPASS --key-pass env:LUMIVARA_STOREPASS --out $OutputApk (Join-Path $buildDir 'aligned.apk')
  Check-BuildStep
} finally { Remove-Item Env:LUMIVARA_STOREPASS -ErrorAction SilentlyContinue }
& java -jar (Join-Path $BuildTools 'lib\apksigner.jar') verify --verbose $OutputApk
Check-BuildStep
& (Join-Path $BuildTools 'aapt2.exe') dump badging $OutputApk
Check-BuildStep





