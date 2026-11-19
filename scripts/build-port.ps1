param([Parameter(Mandatory=$true)][string]$Version)
$ErrorActionPreference='Stop'
$project=Split-Path $PSScriptRoot -Parent
$cache=Join-Path $env:USERPROFILE '.gradle/caches'
$mcRoot=Join-Path $env:APPDATA '.minecraft'
$runtimeVersion=if($Version -eq '26.1'){'26.1.2'}else{$Version}
$game=Join-Path $mcRoot "versions/$runtimeVersion/$runtimeVersion.jar"
$metadata=Get-Content (Join-Path $mcRoot "versions/$runtimeVersion/$runtimeVersion.json") -Raw | ConvertFrom-Json
$cp=[Collections.Generic.List[string]]::new()
$cp.Add($game)
foreach($library in $metadata.libraries) {
    if($library.rules -and -not ($library.rules | Where-Object {
        $_.action -eq 'allow' -and ($null -eq $_.os -or ($_.os.name -eq 'windows' -and !$_.os.arch))
    })) { continue }
    $path=Join-Path "$mcRoot/libraries" $library.downloads.artifact.path
    if(Test-Path -LiteralPath $path) { $cp.Add($path); continue }
    $group,$artifact,$release=$library.name.Split(':')
    $match=Get-ChildItem "$cache/modules-2/files-2.1/$group/$artifact/$release" -Recurse -Filter (Split-Path $path -Leaf) -ErrorAction SilentlyContinue | Select-Object -First 1
    if($match) { $cp.Add($match.FullName) }
}
foreach($coordinate in @(
    'net.fabricmc/fabric-loader/0.19.3',
    'net.fabricmc/sponge-mixin/0.17.4+mixin.0.8.7',
    'maven.modrinth/modmenu/20.0.3',
    'org.lwjgl/lwjgl-glfw/3.4.1'
)) {
    $match=Get-ChildItem "$cache/modules-2/files-2.1/$coordinate" -Recurse -Filter '*.jar' | Where-Object Name -NotMatch 'natives' | Select-Object -First 1
    $cp.Add($match.FullName)
}
$build=(New-Item -ItemType Directory -Force "$project/build/ports/$Version").FullName
$classes=(New-Item -ItemType Directory -Force "$build/classes").FullName
$source="$project/ports/$Version/src/main"
$arguments=@('--release','25','-proc:none','-cp',($cp -join ';'),'-d',$classes)
$arguments+=(Get-ChildItem "$source/java" -Recurse -Filter '*.java').FullName
$argumentFile=Join-Path $build 'javac.args'
[IO.File]::WriteAllLines($argumentFile,($arguments | ForEach-Object {'"'+$_.Replace('\','/')+'"'}))
& javac "@$argumentFile"
if($LASTEXITCODE) { throw "Compilation failed for $Version" }
(New-Item -ItemType Directory -Force "$project/build/libs") | Out-Null
$out="$project/build/libs/hotbar-repeat-fix-1.0.4+$Version.jar"
& jar --create --file $out -C $classes . -C "$source/resources" .
if($LASTEXITCODE) { throw "Packaging failed for $Version" }
[IO.File]::WriteAllText("$build/classpath.txt",($cp -join ';'))
Write-Output $out

