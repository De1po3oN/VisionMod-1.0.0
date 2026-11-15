// Spotify and SoundCloud tab adapter. No password, cookie, network token or
// raw audio is accessed; only visible track metadata and playback position.
(function () {
  "use strict";
  const spotify=location.hostname==="open.spotify.com";
  const youtube=location.hostname==="music.youtube.com";
  const provider=spotify?"Spotify":youtube?"YouTube Music":"SoundCloud";
  const pick=(selectors,root=document)=>{
    for(const selector of selectors){
      const node=root.querySelector(selector);
      const value=(node?.getAttribute("title")||node?.textContent||"").trim();
      if(value)return value;
    }
    return "";
  };
  function timeMs(str){
    const parts=String(str||"").trim().split(":");
    if(parts.length<2||parts.length>3)return null;
    const nums=parts.map(s=>Number(s));
    if(nums.some(n=>!Number.isFinite(n)||n<0))return null;
    return nums.reduce((value,number)=>value*60+number,0)*1000;
  }
  let estimateTrack="",estimateMs=0,estimateAt=Date.now();
  function read(){
    const metadata=navigator.mediaSession?.metadata;
    const title=spotify?pick([
      '[data-testid="now-playing-widget"] [data-testid="context-item-info-title"]',
      '[data-testid="context-item-info-title"] a',
      '[data-testid="now-playing-widget"] a[href*="/track/"]'
    ]):youtube?pick([
      'ytmusic-player-bar .title',
      'ytmusic-player-bar [class*="title"]'
    ]):pick([
      '.playbackSoundBadge__titleLink',
      '.playbackSoundBadge__title a',
      '.playbackSoundBadge__title'
    ]);
    const artist=spotify?pick([
      '[data-testid="now-playing-widget"] [data-testid="context-item-info-artist"]',
      '[data-testid="context-item-info-artist"] a',
      '[data-testid="now-playing-widget"] a[href*="/artist/"]'
    ]):youtube?pick([
      'ytmusic-player-bar .byline a',
      'ytmusic-player-bar .byline'
    ]):pick([
      '.playbackSoundBadge__lightLink',
      '.playbackSoundBadge__usernameLink'
    ]);
    const fallbackTitle=metadata?.title||"";
    const fallbackArtist=metadata?.artist||"";
    const trackTitle=(title||fallbackTitle).trim().slice(0,140);
    const trackArtist=(artist||fallbackArtist).trim().slice(0,140);
    if(!trackTitle)return null;
    const playButton=spotify?
      document.querySelector('[data-testid="control-button-playpause"]'):
      youtube?document.querySelector('ytmusic-player-bar #play-pause-button'):
      document.querySelector('.playControl');
    const label=(playButton?.getAttribute("aria-label")||"").toLowerCase();
    let paused=(spotify||youtube)?/(^play$|^play\s|воспроизвести|spielen)/.test(label):
      !playButton?.classList.contains("playing");
    if(!playButton)paused=false;
    const progress=spotify?pick([
      '[data-testid="playback-position"]',
      '.playback-bar__progress-time',
      '[data-testid="playback-bar"] span:first-child'
    ]):youtube?pick([
      'ytmusic-player-bar .time-info',
      'ytmusic-player-bar .time-info span'
    ]).split('/')[0].trim():pick([
      '.playbackTimeline__timePassed span',
      '.playbackTimeline__timePassed'
    ]);
    let positionMs=timeMs(progress);
    if(positionMs===null && spotify){
      const bar=document.querySelector('[role="progressbar"][aria-valuenow]');
      const value=Number(bar?.getAttribute("aria-valuenow"));
      if(bar && Number.isFinite(value)&&value>=0)positionMs=value*1000;
    }
    const key=provider+"|"+trackArtist+"|"+trackTitle;
    const now=Date.now();
    if(positionMs===null){
      positionMs=key===estimateTrack?
        estimateMs+(paused?0:Math.max(0,now-estimateAt)):0;
    }
    estimateTrack=key;
    estimateMs=positionMs;
    estimateAt=now;
    return {provider,title:trackTitle,artist:trackArtist,positionMs,paused};
  }
  let lastKey="",lastSent=0;
  function poll(){
    if(document.hidden)return;
    const track=read();
    if(!track)return;
    const key=JSON.stringify(track);
    // Keep the local song clock synchronized, at most once per ~1.2 sec.
    if(key===lastKey && Date.now()-lastSent<4500)return;
    lastKey=key;lastSent=Date.now();
    try{chrome.runtime.sendMessage({type:"vision-track",track},()=>void chrome.runtime.lastError);}
    catch(ignored){}
  }
  setInterval(poll,1200);
  document.addEventListener("visibilitychange",poll);
  poll();
})();
