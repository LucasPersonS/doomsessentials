param(
  [switch]$DryRun,
  [switch]$Rollback
)

$ErrorActionPreference = 'Stop'

function Strip-JsonComments([string]$text) {
  $text -replace '(?m)//.*$', '' -replace '(?s)/\*.*?\*/', ''
}

function Read-JsonFile([string]$path) {
  if (-not (Test-Path $path)) { return $null }
  $raw = Get-Content -LiteralPath $path -Raw
  $clean = Strip-JsonComments $raw
  try { return $clean | ConvertFrom-Json } catch { return $null }
}

function Write-JsonFile([string]$path, $obj) {
  $dir = Split-Path -Parent $path
  if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir | Out-Null }
  $json = $obj | ConvertTo-Json -Depth 20
  Set-Content -LiteralPath $path -Value $json -NoNewline
}

function NowStamp() { Get-Date -Format 'yyyyMMdd_HHmmss' }

function SafeCopy([string]$src, [string]$dst) {
  if (-not (Test-Path $src)) { return $false }
  $dir = Split-Path -Parent $dst
  if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir | Out-Null }
  Copy-Item -LiteralPath $src -Destination $dst -Force
  return $true
}

function ReplaceNsTokens([string]$content, [string]$ns) {
  $content = $content -replace "\b$([regex]::Escape($ns)):", 'doomsday:'
  $content = $content -replace "\b$([regex]::Escape($ns))\.", 'doomsday.'
  return $content
}

function ReadJsonText([string]$path) {
  if (-not (Test-Path $path)) { return $null }
  return (Get-Content -LiteralPath $path -Raw)
}

function WriteText([string]$path, [string]$text) {
  $dir = Split-Path -Parent $path
  if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir | Out-Null }
  Set-Content -LiteralPath $path -Value $text -NoNewline
}

function MergeLang([string]$srcLangPath, [string]$dstLangPath, [string]$ns, [ref]$renamedKeys) {
  if (-not (Test-Path $srcLangPath)) { return }
  $srcObj = Read-JsonFile $srcLangPath
  if ($null -eq $srcObj) { return }
  $dstObj = Read-JsonFile $dstLangPath
  $dstTable = @{}
  if ($dstObj -ne $null) { $dstObj.PSObject.Properties | ForEach-Object { $dstTable[$_.Name] = $_.Value } }
  $changed = $false
  $srcObj.PSObject.Properties | ForEach-Object {
    $k = $_.Name
    $v = $_.Value
    $newK = $k
    if ($k -match "^$([regex]::Escape($ns))\.") { $newK = $k -replace "^$([regex]::Escape($ns))\.", 'doomsday.' }
    if ($dstTable.ContainsKey($newK)) {
      if ($dstTable[$newK] -ne $v) {
        $altK = $newK + '.alt'
        $i = 1
        while ($dstTable.ContainsKey($altK)) { $i++; $altK = $newK + ".alt$i" }
        $dstTable[$altK] = $v
        $renamedKeys.Value[$newK] = $altK
        $changed = $true
      }
    } else {
      $dstTable[$newK] = $v
      $changed = $true
    }
  }
  if ($changed -and -not $DryRun) { Write-JsonFile $dstLangPath $dstTable }
}

function EnsureBackup([string]$dir) {
  $stamp = NowStamp
  $backupRoot = Join-Path $dir "_migration_backup"
  if (-not (Test-Path $backupRoot)) { New-Item -ItemType Directory -Path $backupRoot | Out-Null }
  $backupDir = Join-Path $backupRoot $stamp
  New-Item -ItemType Directory -Path $backupDir | Out-Null
  $src = Join-Path $dir 'Doomsday'
  if (Test-Path $src) { Copy-Item -LiteralPath $src -Destination (Join-Path $backupDir 'Doomsday') -Recurse -Force }
  return $backupDir
}

function RollbackFrom([string]$packsRoot, [string]$backupDir) {
  $src = Join-Path $backupDir 'Doomsday'
  $dst = Join-Path $packsRoot 'Doomsday'
  if (-not (Test-Path $src)) { throw "Backup not found: $src" }
  if (Test-Path $dst) { Remove-Item -LiteralPath $dst -Recurse -Force }
  Copy-Item -LiteralPath $src -Destination $dst -Recurse -Force
}

function GetPackInfo([string]$packDir) {
  $meta = Read-JsonFile (Join-Path $packDir 'gunpack.meta.json')
  if ($null -eq $meta) { return $null }
  $ns = $meta.namespace
  if ([string]::IsNullOrWhiteSpace($ns)) { return $null }
  $assets = Join-Path $packDir "assets\$ns"
  $data = Join-Path $packDir "data\$ns"
  return @{ ns = $ns; assets = $assets; data = $data; packDir = $packDir }
}

function DetectGunIds($pack) {
  $indexGunsDir = Join-Path $pack.data 'index\guns'
  if (-not (Test-Path $indexGunsDir)) { return @() }
  Get-ChildItem -LiteralPath $indexGunsDir -Filter '*.json' -File | ForEach-Object {
    $_.BaseName
  }
}

function FindDisplayPath($pack, [string]$gunId) { Join-Path $pack.assets ("display\guns\" + $gunId + '_display.json') }
function FindDataPath($pack, [string]$gunId) { Join-Path $pack.data ("data\guns\" + $gunId + '_data.json') }
function TargetIndexPath([string]$doomsData, [string]$id) { Join-Path $doomsData ("index\guns\" + $id + '.json') }
function TargetDataPath([string]$doomsData, [string]$id) { Join-Path $doomsData ("data\guns\" + $id + '_data.json') }
function TargetDisplayPath([string]$doomsAssets, [string]$id) { Join-Path $doomsAssets ("display\guns\" + $id + '_display.json') }

function EnsureDoomsdayPaths([string]$packsRoot) {
  $doomsRoot = Join-Path $packsRoot 'Doomsday'
  $assets = Join-Path $doomsRoot 'assets\doomsday'
  $data = Join-Path $doomsRoot 'data\doomsday'
  foreach ($p in @($assets, $data)) { if (-not (Test-Path $p)) { New-Item -ItemType Directory -Path $p | Out-Null } }
  foreach ($sub in @('display\guns','textures\gun\uv','textures\gun\hud','textures\gun\slot','textures\gun\lod','geo_models\gun','geo_models\gun\lod','animations','tacz_sounds','lang','index\guns','data\guns','recipes\gun')) {
    $aSub = Join-Path $assets $sub
    $dSub = Join-Path $data $sub
    $dir = if ($sub -like 'index*' -or $sub -like 'data*' -or $sub -like 'recipes*') { $dSub } else { $aSub }
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir | Out-Null }
  }
  return @{ root = $doomsRoot; assets = $assets; data = $data }
}

function NextUniqueId([string]$baseId, [string]$doomsData) {
  $id = $baseId
  $n = 0
  while ((Test-Path (TargetIndexPath $doomsData $id)) -or (Test-Path (TargetDataPath $doomsData $id))) {
    $n++
    $id = "$baseId" + "_dup$n"
  }
  return $id
}

function EnsureUniqueFile([string]$src, [string]$dst) {
  if (-not (Test-Path $src)) { return $dst }
  if (-not (Test-Path $dst)) { SafeCopy $src $dst | Out-Null; return $dst }
  return $dst
}

function CopyTexturesModelsUpdateDisplay($pack, $dispObj, [string]$doomsAssets) {
  $updatePath = {
    param($value, $type)
    if (-not $value) { return $null }
    $rel = ($value -replace '^doomsday:', '')
    $relFs = $rel -replace '/', '\'
    if ($type -eq 'model') { $src = Join-Path $pack.assets ("geo_models\" + $relFs + '.json'); $dst = Join-Path $doomsAssets ("geo_models\" + $relFs + '.json') }
    elseif ($type -eq 'texture') { $src = Join-Path $pack.assets ("textures\" + $relFs + '.png'); $dst = Join-Path $doomsAssets ("textures\" + $relFs + '.png') }
    else { return $value }
    $final = EnsureUniqueFile $src $dst
    $relNew = ($final -replace [regex]::Escape($doomsAssets + '\'), '')
    $relNew = $relNew -replace '\\', '/'
    return 'doomsday:' + $relNew -replace '\.(json|png)$',''
  }
  if ($dispObj.model) { $dispObj.model = & $updatePath $dispObj.model 'model' }
  if ($dispObj.texture) { $dispObj.texture = & $updatePath $dispObj.texture 'texture' }
  if ($dispObj.hud) { $dispObj.hud = & $updatePath $dispObj.hud 'texture' }
  if ($dispObj.slot) { $dispObj.slot = & $updatePath $dispObj.slot 'texture' }
  if ($dispObj.lod -and $dispObj.lod.model) { $dispObj.lod.model = & $updatePath $dispObj.lod.model 'model' }
  if ($dispObj.lod -and $dispObj.lod.texture) { $dispObj.lod.texture = & $updatePath $dispObj.lod.texture 'texture' }
}

function CopyAnimationsUpdateDisplay($pack, $dispObj, [string]$doomsAssets) {
  $anim = $dispObj.animation
  if (-not $anim) { return }
  $aRel = $anim -replace '^doomsday:', ''
  $gltfSrc = Join-Path $pack.assets ("animations\" + $aRel + '.gltf')
  $jsonSrc = Join-Path $pack.assets ("animations\" + $aRel + '.animation.json')
  $gltfDst = Join-Path $doomsAssets ("animations\" + $aRel + '.gltf')
  $jsonDst = Join-Path $doomsAssets ("animations\" + $aRel + '.animation.json')
  if (Test-Path $gltfSrc) { $gltfFinal = EnsureUniqueFile $gltfSrc $gltfDst } else { $gltfFinal = $null }
  if (Test-Path $jsonSrc) { $jsonFinal = EnsureUniqueFile $jsonSrc $jsonDst } else { $jsonFinal = $null }
  $finalBase = $null
  if ($gltfFinal) { $finalBase = [System.IO.Path]::GetFileNameWithoutExtension($gltfFinal) }
  elseif ($jsonFinal) { $finalBase = [System.IO.Path]::GetFileNameWithoutExtension($jsonFinal) -replace '\.animation$','' }
  if ($finalBase) { $dispObj.animation = 'doomsday:' + $finalBase }
}

function CopySounds($pack, [string]$doomsAssets, [string]$displayText, [string]$ns) {
  $srcSounds = Join-Path $pack.assets 'sounds.json'
  if (Test-Path $srcSounds) {
    $raw = ReadJsonText $srcSounds
    $rewritten = ReplaceNsTokens $raw $ns
    $dstSounds = Join-Path $doomsAssets 'sounds.json'
    if ($DryRun) { } else {
      if (Test-Path $dstSounds) {
        $dstRaw = ReadJsonText $dstSounds
        $merged = ($dstRaw + "\n" + $rewritten)
        WriteText $dstSounds $merged
      } else { WriteText $dstSounds $rewritten }
    }
  }
  $srcOgg = Join-Path $pack.assets 'tacz_sounds'
  if (Test-Path $srcOgg) { Copy-Item -LiteralPath $srcOgg -Destination (Join-Path $doomsAssets 'tacz_sounds') -Recurse -Force }
}

function MigratePackGuns([string]$packsRoot, $pack, $dooms) {
  $indexGunsDir = Join-Path $pack.data 'index\guns'
  if (-not (Test-Path $indexGunsDir)) { return @{ migrated = 0; ids = @() } }
  $ids = @()
  $idMap = @{}
  foreach ($file in Get-ChildItem -LiteralPath $indexGunsDir -Filter '*.json' -File) {
    $gunIdBase = $file.BaseName
    $indexPath = $file.FullName
    $indexTextOrig = ReadJsonText $indexPath
    $idxOrigObj = Strip-JsonComments $indexTextOrig | ConvertFrom-Json
    $srcDispRel = $null
    $srcDataRel = $null
    if ($idxOrigObj -and $idxOrigObj.display) { $srcDispRel = ($idxOrigObj.display -replace "^$([regex]::Escape($pack.ns)):", '') }
    if ($idxOrigObj -and $idxOrigObj.data) { $srcDataRel = ($idxOrigObj.data -replace "^$([regex]::Escape($pack.ns)):", '') }
    $displayPath = if ($srcDispRel) { Join-Path $pack.assets ("display\guns\" + $srcDispRel + '.json') } else { $null }
    $dataPath = if ($srcDataRel) { Join-Path $pack.data ("data\guns\" + $srcDataRel + '.json') } else { $null }
    $existsDisplay = ($displayPath -and (Test-Path $displayPath))
    $existsData = ($dataPath -and (Test-Path $dataPath))
    if (-not $existsDisplay -or -not $existsData) { continue }
    $newIndexText = ReplaceNsTokens $indexTextOrig $pack.ns
    $displayText = ReadJsonText $displayPath
    $dataText = ReadJsonText $dataPath
    $newDisplayText = ReplaceNsTokens $displayText $pack.ns
    $newDataText = ReplaceNsTokens $dataText $pack.ns
    $idxObj = Strip-JsonComments $newIndexText | ConvertFrom-Json
    $dispObj = Strip-JsonComments $newDisplayText | ConvertFrom-Json
    $dataObj = Strip-JsonComments $newDataText | ConvertFrom-Json
    $targetId = $gunIdBase
    $idxObj.display = "doomsday:" + $targetId + "_display"
    $idxObj.data = "doomsday:" + $targetId + "_data"
    $idMap[$targetId] = $targetId
    $targetIndex = TargetIndexPath $dooms.data $targetId
    $targetData = TargetDataPath $dooms.data $targetId
    $targetDisplay = TargetDisplayPath $dooms.assets $targetId
    if (-not $DryRun) {
      Write-JsonFile $targetIndex $idxObj
      CopyTexturesModelsUpdateDisplay $pack $dispObj $dooms.assets
      CopyAnimationsUpdateDisplay $pack $dispObj $dooms.assets
      Write-JsonFile $targetDisplay $dispObj
      Write-JsonFile $targetData $dataObj
      CopySounds $pack $dooms.assets $newDisplayText $pack.ns
      $rk = @{}
      MergeLang (Join-Path $pack.assets 'lang\en_us.json') (Join-Path $dooms.assets 'lang\en_us.json') $pack.ns ([ref]$rk)
      MergeLang (Join-Path $pack.assets 'lang\zh_cn.json') (Join-Path $dooms.assets 'lang\zh_cn.json') $pack.ns ([ref]$rk)
    }
    $ids += $targetId
  }
  return @{ migrated = $ids.Count; ids = $ids; idMap = $idMap }
}

function CleanupDuplicates([string]$doomsRoot) {
  $idxDir = Join-Path $doomsRoot 'data\doomsday\index\guns'
  $dataDir = Join-Path $doomsRoot 'data\doomsday\data\guns'
  $dispDir = Join-Path $doomsRoot 'assets\doomsday\display\guns'
  foreach ($dir in @($idxDir,$dataDir,$dispDir)) {
    if (-not (Test-Path $dir)) { continue }
    Get-ChildItem -LiteralPath $dir -Filter '*_dup*.json' -File | ForEach-Object { Remove-Item -LiteralPath $_.FullName -Force }
  }
}

function MigrateAmmoForPack($pack, $dooms, $idMap) {
  $ammoCopied = @()
  foreach ($baseId in $idMap.Keys) {
    $dataPath = FindDataPath $pack $baseId
    if (-not (Test-Path $dataPath)) { continue }
    $dataObj = Strip-JsonComments (Get-Content -LiteralPath $dataPath -Raw) | ConvertFrom-Json
    if ($dataObj.ammo -and ($dataObj.ammo -match "^$([regex]::Escape($pack.ns)):(.+)$")) {
      $ammoId = $Matches[1]
      $srcIndex = Join-Path $pack.data ("index\ammo\" + $ammoId + '.json')
      $srcDisplay = Join-Path $pack.assets ("display\ammo\" + $ammoId + '_display.json')
      if (Test-Path $srcIndex) {
        $raw = Get-Content -LiteralPath $srcIndex -Raw
        $rew = ReplaceNsTokens $raw $pack.ns
        $obj = Strip-JsonComments $rew | ConvertFrom-Json
        $dstIndex = Join-Path $dooms.data ("index\ammo\" + $ammoId + '.json')
        if (-not $DryRun) { Write-JsonFile $dstIndex $obj }
        $ammoCopied += $ammoId
      }
      if (Test-Path $srcDisplay) {
        $raw = Get-Content -LiteralPath $srcDisplay -Raw
        $rew = ReplaceNsTokens $raw $pack.ns
        if (-not $DryRun) { WriteText (Join-Path $dooms.assets ("display\ammo\" + $ammoId + '_display.json')) $rew }
      }
      $rk = @{}
      MergeLang (Join-Path $pack.assets 'lang\en_us.json') (Join-Path $dooms.assets 'lang\en_us.json') $pack.ns ([ref]$rk)
      MergeLang (Join-Path $pack.assets 'lang\zh_cn.json') (Join-Path $dooms.assets 'lang\zh_cn.json') $pack.ns ([ref]$rk)
    }
  }
  return $ammoCopied
}

function MigrateAllAmmoFromPack($pack, $dooms) {
  $srcIndexDir = Join-Path $pack.data 'index\ammo'
  $copied = @()
  if (Test-Path $srcIndexDir) {
    Get-ChildItem -LiteralPath $srcIndexDir -Filter '*.json' -File | ForEach-Object {
      $raw = Get-Content -LiteralPath $_.FullName -Raw
      $rew = ReplaceNsTokens $raw $pack.ns
      $obj = Strip-JsonComments $rew | ConvertFrom-Json
      $dstIndex = Join-Path $dooms.data ("index\ammo\" + $_.BaseName + '.json')
      if (-not $DryRun) { Write-JsonFile $dstIndex $obj }
      $copied += $_.BaseName
    }
  }
  $srcDisplayDir = Join-Path $pack.assets 'display\ammo'
  if (Test-Path $srcDisplayDir) {
    Get-ChildItem -LiteralPath $srcDisplayDir -Filter '*_display.json' -File | ForEach-Object {
      $raw = Get-Content -LiteralPath $_.FullName -Raw
      $rew = ReplaceNsTokens $raw $pack.ns
      $dst = Join-Path $dooms.assets ('display\ammo\' + $_.Name)
      if (-not $DryRun) { WriteText $dst $rew }
    }
  }
  return $copied
}

function CopyRecipesForPack($pack, $dooms, $idMap) {
  $srcDir = Join-Path $pack.data 'recipes\gun'
  if (-not (Test-Path $srcDir)) { return @() }
  $copied = @()
  Get-ChildItem -LiteralPath $srcDir -Filter '*.json' -File | ForEach-Object {
    $nameBase = $_.BaseName
    $raw = Get-Content -LiteralPath $_.FullName -Raw
    $rew = ReplaceNsTokens $raw $pack.ns
    foreach ($k in $idMap.Keys) {
      $v = $idMap[$k]
      $rew = $rew -replace ("doomsday:" + [regex]::Escape($k) + '(?=\b)'), ('doomsday:' + $v)
    }
    $dstName = if ($idMap.ContainsKey($nameBase)) { $idMap[$nameBase] } else { $nameBase }
    $dstPath = Join-Path $dooms.data ("recipes\gun\" + $dstName + '.json')
    if (-not $DryRun) { WriteText $dstPath $rew }
    $copied += $dstName
  }
  return $copied
}

function ScanAmmoDeps($doomsData) {
  $deps = New-Object System.Collections.Generic.HashSet[string]
  $gunsDir = Join-Path $doomsData 'data\guns'
  Get-ChildItem -LiteralPath $gunsDir -Filter '*_data.json' -File | ForEach-Object {
    $text = Get-Content -LiteralPath $_.FullName -Raw
    $clean = Strip-JsonComments $text
    try {
      $obj = $clean | ConvertFrom-Json
      if ($obj.ammo -and ($obj.ammo -match '(^[^:]+):')) {
        $ns = $Matches[1]
        if ($ns -ne 'tacz' -and $ns -ne 'doomsday') { [void]$deps.Add($ns) }
      }
    } catch {}
  }
  return @($deps)
}

function UpdateDoomsdayDependencies([string]$doomsAssets, $deps) {
  $infoPath = Join-Path $doomsAssets 'gunpack_info.json'
  $info = Read-JsonFile $infoPath
  if ($null -eq $info) { return }
  $depsObj = $info.dependencies
  if ($null -eq $depsObj) { $depsObj = @{} }
  elseif ($depsObj -isnot [hashtable]) {
    $tmp = @{}
    $depsObj.PSObject.Properties | ForEach-Object { $tmp[$_.Name] = $_.Value }
    $depsObj = $tmp
  }
  $changed = $false
  foreach ($d in $deps) { if (-not $depsObj.ContainsKey($d)) { $depsObj[$d] = '*'; $changed = $true } }
  $info.dependencies = $depsObj
  if ($changed -and -not $DryRun) { Write-JsonFile $infoPath $info }
}

function ValidateDoomsday([string]$doomsRoot) {
  $errors = @()
  $indexDir = Join-Path $doomsRoot 'data\doomsday\index\guns'
  Get-ChildItem -LiteralPath $indexDir -Filter '*.json' -File | ForEach-Object {
    $idx = $_.FullName
    try {
      $obj = Strip-JsonComments (Get-Content -LiteralPath $idx -Raw) | ConvertFrom-Json
      if (-not $obj.display -or -not $obj.data) { $errors += "Missing display/data in $idx"; return }
      $dispPath = Join-Path $doomsRoot ('assets\doomsday\display\guns\' + ($obj.display -replace '^doomsday:', '') + '.json')
      $dataPath = Join-Path $doomsRoot ('data\doomsday\data\guns\' + ($obj.data -replace '^doomsday:', '') + '.json')
      if (-not (Test-Path $dispPath)) { $errors += "Display not found: $dispPath"; return }
      if (-not (Test-Path $dataPath)) { $errors += "Data not found: $dataPath"; return }
      $disp = Strip-JsonComments (Get-Content -LiteralPath $dispPath -Raw) | ConvertFrom-Json
      $checkTex = {
        param($value, $type)
        if (-not $value) { return }
        $rel = ($value -replace '^doomsday:', '') -replace '/', '\'
        $p = if ($type -eq 'model') { Join-Path $doomsRoot ('assets\doomsday\geo_models\' + $rel + '.json') } else { Join-Path $doomsRoot ('assets\doomsday\textures\' + $rel + '.png') }
        if (-not (Test-Path $p)) { $errors += ("Missing " + $type + ": " + $p) }
      }
      & $checkTex $disp.model 'model'
      & $checkTex $disp.texture 'texture'
      & $checkTex $disp.hud 'texture'
      & $checkTex $disp.slot 'texture'
      if ($disp.lod) { & $checkTex $disp.lod.model 'model'; & $checkTex $disp.lod.texture 'texture' }
      $data = Strip-JsonComments (Get-Content -LiteralPath $dataPath -Raw) | ConvertFrom-Json
      if ($data.ammo -and ($data.ammo -match '^doomsday:(.+)$')) {
        $ammoId = $Matches[1]
        $ammoIndex = Join-Path $doomsRoot ('data\doomsday\index\ammo\' + $ammoId + '.json')
        if (-not (Test-Path $ammoIndex)) { $errors += "Missing ammo index: $ammoIndex" }
      }
    } catch { $errors += "Invalid JSON: $idx" }
  }
  return $errors
}

function Main() {
  $projectRoot = Split-Path -Parent $PSScriptRoot
  $packsRoot = Join-Path $projectRoot 'bin\main\data\packs\tacz'
  if (-not (Test-Path $packsRoot)) { $packsRoot = Join-Path $projectRoot 'run\tacz' }
  $dooms = EnsureDoomsdayPaths $packsRoot
  if ($Rollback) {
    $latestBackupRoot = Join-Path $packsRoot '_migration_backup'
    $latest = Get-ChildItem -LiteralPath $latestBackupRoot -Directory | Sort-Object Name -Descending | Select-Object -First 1
    if ($null -eq $latest) { throw 'No backup available' }
    RollbackFrom $packsRoot $latest.FullName
    Write-Output "Rollback completed from $($latest.FullName)"
    return
  }
  $backupDir = EnsureBackup $packsRoot
  $packDirs = Get-ChildItem -LiteralPath $packsRoot -Directory | Where-Object { $_.Name -ne 'Doomsday' }
  $summary = @()
  foreach ($dir in $packDirs) {
    $info = GetPackInfo $dir.FullName
    if ($null -eq $info) { continue }
    if ($info.ns -eq 'tacz') { continue }
    $guns = DetectGunIds $info
    $res = @{ migrated = $null; ids = $null; idMap = @{} }
    $ammoIds = @()
    $recipes = @()
    if ($guns.Count -gt 0) {
      $res = MigratePackGuns $packsRoot $info $dooms
      $ammoIds = MigrateAmmoForPack $info $dooms $res.idMap
      $recipes = CopyRecipesForPack $info $dooms $res.idMap
    }
    $extraAmmo = MigrateAllAmmoFromPack $info $dooms
    $summary += @{ pack = $dir.Name; namespace = $info.ns; migrated = $res.migrated; ids = $res.ids; ammo = ($ammoIds + $extraAmmo); recipes = $recipes }
  }
  $deps = ScanAmmoDeps $dooms.data
  UpdateDoomsdayDependencies $dooms.assets $deps
  CleanupDuplicates $dooms.root
  $errors = ValidateDoomsday $dooms.root
  $log = @{ timestamp = NowStamp; backup = $backupDir; summary = $summary; dependencies = $deps; errors = $errors }
  $logPath = Join-Path $packsRoot ('_migration_backup\' + (Split-Path $backupDir -Leaf) + '\migration.log.json')
  if (-not $DryRun) { Write-JsonFile $logPath $log }
  Write-Output (ConvertTo-Json $log -Depth 10)
}

Main
