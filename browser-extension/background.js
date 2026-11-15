// Runs only in the browser extension service worker. The secret is stored in
// chrome.storage.local; no Spotify/SoundCloud content page can read it.
const ENDPOINT="http://127.0.0.1:38567/track";
let lastError="";
chrome.runtime.onMessage.addListener((message,sender,reply)=>{
  if(message?.type!=="vision-track")return false;
  const url=sender.tab?.url||"";
  if(!/^https:\/\/(open\.spotify\.com|(?:www\.)?soundcloud\.com)\//.test(url)){
    reply({ok:false,error:"Untrusted tab"});return false;
  }
  (async()=>{
    const {token}=await chrome.storage.local.get("token");
    if(!/^[0-9a-f]{32}$/.test(token||"")) {
      reply({ok:false,error:"Open extension options and paste pairing token"});
      return;
    }
    const t=message.track;
    if(typeof t?.title!=="string"||!t.title.trim()) {
      reply({ok:false,error:"No playing track detected"});return;
    }
    const valid={
      provider:["Spotify","SoundCloud","YouTube Music"].includes(t.provider)?t.provider:"SoundCloud",
      title:t.title.slice(0,140),artist:String(t.artist||"").slice(0,140),
      positionMs:Math.min(86400000,Math.max(0,Number(t.positionMs)||0)),
      paused:Boolean(t.paused)
    };
    try{
      const controller=new AbortController();
      const cancel=setTimeout(()=>controller.abort(),2500);
      const response=await fetch(ENDPOINT,{
        method:"POST",headers:{"Content-Type":"application/json","X-Vision-Token":token},
        body:JSON.stringify(valid),signal:controller.signal
      });
      clearTimeout(cancel);
      lastError=response.ok?"":("Bridge HTTP "+response.status);
      reply({ok:response.ok,error:lastError});
    }catch(e){lastError=String(e);reply({ok:false,error:lastError});}
  })();
  return true;
});
