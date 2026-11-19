# Uses the Windows 10/11 media-session API. No files, extensions or HTTP server.
$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
Add-Type -AssemblyName System.Runtime.WindowsRuntime
$null = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType=WindowsRuntime]
$null = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties, Windows.Media.Control, ContentType=WindowsRuntime]
$asTask = [System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object {
    $_.Name -eq 'AsTask' -and $_.IsGenericMethodDefinition -and $_.GetGenericArguments().Count -eq 1 -and
    $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1'
} | Select-Object -First 1
function Await($operation, $resultType, $timeoutMs = 3000) {
    $task = $asTask.MakeGenericMethod($resultType).Invoke($null, @($operation))
    if (-not $task.Wait($timeoutMs)) { throw 'Media API timeout' }
    return $task.Result
}
$manager = Await ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])
while ($true) {
    try {
        $current = $manager.GetCurrentSession()
        $currentSource = if ($null -eq $current) { '' } else { [string]$current.SourceAppUserModelId }
        $ranked = foreach ($candidate in $manager.GetSessions() | Select-Object -First 12) {
            try {
                $source = [string]$candidate.SourceAppUserModelId
                $status = $candidate.GetPlaybackInfo().PlaybackStatus.ToString()
                if ($status -notin @('Playing', 'Paused')) { continue }
                $score = if ($status -eq 'Playing') { 100 } else { 0 }
                if ($candidate.GetTimelineProperties().LastUpdatedTime.Year -ge 2000) { $score += 40 }
                if ($source -match '(?i)(yandex.*music|music.*yandex)') { $score += 20 }
                if ($source -eq $currentSource) { $score += 1 }
                [PSCustomObject]@{ Session=$candidate; Score=$score }
            } catch { continue } # A player may close while the sessions are enumerated.
        }
        $payload = $null
        foreach ($entry in $ranked | Sort-Object Score -Descending | Select-Object -First 3) {
            try {
                $session = $entry.Session
                $media = Await ($session.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties]) 1000
                $title = [string]$media.Title
                if ([string]::IsNullOrWhiteSpace($title)) { continue }
                $playback = $session.GetPlaybackInfo()
                $status = $playback.PlaybackStatus.ToString()
                if ($status -notin @('Playing', 'Paused')) { continue }
                $paused = $status -eq 'Paused'
                $rate = $playback.PlaybackRate
                if ($null -eq $rate -or $rate -le 0) { $rate = 1.0 }
                $timeline = $session.GetTimelineProperties()
                $position = [Math]::Max(0, ($timeline.Position - $timeline.StartTime).TotalMilliseconds)
                # WinRT supplies a stamped position, which can be reused between updates.
                $positionKnown = $timeline.LastUpdatedTime.Year -ge 2000
                if (-not $paused -and $positionKnown) { $position += [Math]::Max(0, ([DateTimeOffset]::Now - $timeline.LastUpdatedTime).TotalMilliseconds) * $rate }
                $duration = ($timeline.EndTime - $timeline.StartTime).TotalMilliseconds
                if ($duration -gt 0) { $position = [Math]::Min($position, $duration) }
                $artist = $media.Artist
                if ([string]::IsNullOrWhiteSpace($artist)) { $artist = $media.AlbumArtist }
                $payload = @{ provider=$session.SourceAppUserModelId; title=$title; artist=[string]$artist; positionMs=[long]$position; paused=$paused; durationMs=[long][Math]::Max(0,$duration); rate=[double]$rate; positionKnown=$positionKnown }
                break
            } catch { continue }
        }
        if ($null -eq $payload) { [Console]::WriteLine('{}') }
        else { [Console]::WriteLine(($payload | ConvertTo-Json -Compress)) }
    } catch {
        [Console]::WriteLine('{}') # Transient session change; poll again without restarting the worker.
    }
    Start-Sleep -Milliseconds 500
}
