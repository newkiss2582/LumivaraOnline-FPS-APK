param([string]$PlaywrightModule=$env:PLAYWRIGHT_MODULE,[string]$BrowserPath=$env:TEST_BROWSER)
$ErrorActionPreference='Stop'
if ($PlaywrightModule) {$env:PLAYWRIGHT_MODULE=$PlaywrightModule}
if ($BrowserPath) {$env:TEST_BROWSER=$BrowserPath}
foreach ($fpsScript in @('single-screen-test','display-requests-test','background-frame-test','background-timers-test','screen-off-test','browser-retention-test','fps-settings-test','app-language-test','select-picker-test')) {
    & node (Join-Path $PSScriptRoot "$fpsScript.cjs")
    if ($LASTEXITCODE -ne 0) {throw "Failed: $fpsScript"}
}
$fpsProject=Split-Path -Parent $PSScriptRoot
$fpsClasses=Join-Path $fpsProject 'build/pure-tests'
New-Item -ItemType Directory -Path $fpsClasses -Force | Out-Null
$fpsSources=@('DisplayRefreshPolicy','RenderGeometry','FitGeometry','ProfileIdentity','NavigationPolicy','OAuthHandshake','ReturnLinkPolicy','LanguageChoice') | ForEach-Object {Join-Path $fpsProject "src/com/lumivaraonline/fps/$_.java"}
$fpsNames=@('FpsPolicyTest','FitGeometryTest','NavigationPolicyTest','OAuthHandshakeTest','ReturnLinkPolicyTest','LanguageChoiceTest')
$fpsSources+=$fpsNames | ForEach-Object {Join-Path $PSScriptRoot "$_.java"}
& javac --release 8 -d $fpsClasses @fpsSources
if ($LASTEXITCODE -ne 0) {throw 'Pure Java compile failed'}
foreach ($fpsName in $fpsNames) {& java -cp $fpsClasses $fpsName; if ($LASTEXITCODE -ne 0) {throw "Failed: $fpsName"}}
