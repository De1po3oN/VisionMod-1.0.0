"""Capture actual editor frames inside the smoke test's Xvfb display."""
import os
import time
from pathlib import Path
from PIL import ImageGrab

log = Path('smoke-client.log')
output = Path('editor-captures')
output.mkdir(exist_ok=True)
seen = set()
end = time.monotonic() + 90
while time.monotonic() < end:
    text = log.read_text(errors='replace') if log.exists() else ''
    for stage in range(8):
        if stage not in seen and f'[VisionMod][VISUAL] {stage}' in text:
            time.sleep(.5)
            ImageGrab.grab(xdisplay=os.environ['DISPLAY']).save(output / f'{stage:02d}.png')
            seen.add(stage)
    if len(seen) == 8:
        print('Captured 8 real Minecraft editor frames')
        break
    time.sleep(.15)
else:
    raise SystemExit(f'Only captured {len(seen)} of 8 editors')
