#!/usr/bin/env bash
# CI-only output branch; never writes the development branch or prints credentials.
set -euo pipefail
kind=$1
case "$kind" in build|smoke) ;; *) exit 2 ;; esac
python3 - "$kind" <<'PY'
import json, os, shutil, sys
from pathlib import Path
kind=sys.argv[1]
report=Path('.qa-evidence')
if not (report/'.git').exists(): raise SystemExit('Evidence checkout missing')
folder=report/kind
if folder.exists(): shutil.rmtree(folder)
folder.mkdir()
(folder/'source.json').write_text(json.dumps({'sha':os.environ['GITHUB_SHA'], 'run':os.environ['GITHUB_RUN_ID'], 'attempt':os.environ['GITHUB_RUN_ATTEMPT']},indent=2)+'\n')
if kind=='build':
    shutil.copy2('build/libs/visionmod-2.0.0.jar',folder/'visionmod-2.0.0.jar')
else:
    for name in ['smoke-client.log','smoke-status.txt','capture-editors.log','game-exit-status.txt']:
        if Path(name).is_file(): shutil.copy2(name,folder/name)
    for image in Path('editor-captures').glob('*.png'): shutil.copy2(image,folder/image.name)
(report/'README.md').write_text('VisionMod CI evidence only. Source: codex/visual-refresh.\nEach folder records its exact source SHA and workflow run.\n')
PY
git -C .qa-evidence config user.name 'github-actions[bot]'
git -C .qa-evidence config user.email '41898282+github-actions[bot]@users.noreply.github.com'
git -C .qa-evidence add -- README.md "$kind"
if git -C .qa-evidence diff --cached --quiet; then exit 0; fi
git -C .qa-evidence commit -m "Record $kind evidence for $GITHUB_SHA"
for attempt in 1 2 3; do
  git -C .qa-evidence pull --rebase origin codex/visual-evidence
  if git -C .qa-evidence push origin HEAD:codex/visual-evidence; then exit 0; fi
done
exit 1
