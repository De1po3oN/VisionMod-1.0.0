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
function Await($operation, $resultType) {
    $task = $asTask.MakeGenericMethod($resultType).Invoke($null, @($operation))
    if (-not $task.Wait(3000)) { throw 'Media API timeout' }
    return $task.Result
}
$manager = Await ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])
while ($true) {
    try {
        $session = $manager.GetCurrentSession()
        foreach ($candidate in $manager.GetSessions()) {
            if ($candidate.GetPlaybackInfo().PlaybackStatus.ToString() -eq 'Playing') { $session = $candidate; break }
        }
        if ($null -eq $session) {
            [Console]::WriteLine('{}')
        } else {
            $media = Await ($session.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])
            $timeline = $session.GetTimelineProperties()
            $paused = $session.GetPlaybackInfo().PlaybackStatus.ToString() -ne 'Playing'
            $position = [Math]::Max(0, ($timeline.Position - $timeline.StartTime).TotalMilliseconds)
            # The Windows timeline is stamped, and may not update every poll.
            if (-not $paused) { $position += [Math]::Max(0, ([DateTimeOffset]::Now - $timeline.LastUpdatedTime).TotalMilliseconds) }
            $duration = ($timeline.EndTime - $timeline.StartTime).TotalMilliseconds
            if ($duration -gt 0) { $position = [Math]::Min($position, $duration) }
            $artist = $media.Artist
            if ([string]::IsNullOrWhiteSpace($artist)) { $artist = $media.AlbumArtist }
            $title = [string]$media.Title
            if ($title.Length -gt 140) { $title = $title.Substring(0,140) }
            $artist = [string]$artist
            if ($artist.Length -gt 140) { $artist = $artist.Substring(0,140) }
            $payload = @{ provider=$session.SourceAppUserModelId; title=$title; artist=$artist; positionMs=[long]$position; paused=$paused }
            [Console]::WriteLine(($payload | ConvertTo-Json -Compress))
        }
    } catch {
        [Console]::WriteLine('{"error":"Media session unavailable"}')
    }
    Start-Sleep -Milliseconds 2000
}
