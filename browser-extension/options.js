const input=document.getElementById("token"),status=document.getElementById("status");
chrome.storage.local.get("token").then(({token})=>{if(token)input.value=token;});
document.getElementById("save").addEventListener("click",async()=>{
  const value=input.value.trim().toLowerCase();
  if(!/^[0-9a-f]{32}$/.test(value)){
    status.textContent="Ошибка: ключ должен содержать ровно 32 шестнадцатеричных символа.";
    return;
  }
  await chrome.storage.local.set({token:value});
  status.textContent="Сопряжение сохранено. Открой Spotify / SoundCloud и запусти песню.";
});
