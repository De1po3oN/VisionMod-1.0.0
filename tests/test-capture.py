"""Tests only the screenshot verifier, using synthetic fixtures, not game visuals."""
import os
import subprocess
import sys
import tempfile
import time
from pathlib import Path
from PIL import Image, ImageDraw

script=Path(__file__).with_name('capture-editors.py').resolve()
for blank in (False,True):
    with tempfile.TemporaryDirectory() as directory:
        root=Path(directory)
        (root/'run/screenshots').mkdir(parents=True)
        environment=dict(os.environ,VISION_CAPTURE_TIMEOUT='2')
        process=subprocess.Popen([sys.executable,str(script)],cwd=root,env=environment,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
        time.sleep(.3)
        for stage in range(8):
            frame=Image.new('RGB',(320,240),(0,0,0) if blank else (20,28,48))
            if not blank: ImageDraw.Draw(frame).rectangle((30,30,180,130),fill=(180,160,230))
            frame.save(root/f'run/screenshots/vision-editor-{stage:02d}.png')
        output=process.communicate(timeout=5)[0]
        assert (process.returncode!=0)==blank,output
        if not blank: assert len(list((root/'editor-captures').glob('*.png')))==8
print('PASS: capture verifier accepts complete frames and rejects blank images (synthetic fixtures)')
