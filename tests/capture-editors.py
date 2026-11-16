"""Validate and retain native Minecraft framebuffer screenshots (not X11 root)."""
import shutil
import time
from pathlib import Path
from PIL import Image, ImageStat

source=Path('run/screenshots')
output=Path('editor-captures')
output.mkdir(exist_ok=True)
seen=set()
started=time.time()
end=time.monotonic()+90
while time.monotonic()<end:
    for stage in range(8):
        image=source/f'vision-editor-{stage:02d}.png'
        if stage in seen or not image.exists() or image.stat().st_mtime<started:
            continue
        try:
            with Image.open(image) as frame:
                if frame.width<160 or frame.height<100 or max(ImageStat.Stat(frame.convert('RGB')).var)<5:
                    continue
                frame.verify()
            shutil.copy2(image,output/f'{stage:02d}.png')
            seen.add(stage)
        except (OSError,ValueError):
            pass  # The asynchronous screenshot writer may still own this file.
    if len(seen)==8:
        print('PASS: 8 nonblank Minecraft framebuffer screenshots')
        break
    time.sleep(.15)
else:
    raise SystemExit(f'Only {len(seen)} of 8 nonblank editor captures; missing {sorted(set(range(8))-seen)}')
