"""Build static OFL UI font instances. Development utility; not a mod dependency."""
from pathlib import Path
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont

root = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/visionmod/font'
for filename, weight, subfamily in [('menu.ttf', 450, 'Regular'), ('menu-heading.ttf', 650, 'Semibold')]:
    font = TTFont(root / 'inter.ttf', recalcTimestamp=False)
    instantiateVariableFont(font, {'opsz': 20, 'wght': weight}, inplace=True)
    for record in font['name'].names:
        replacement = {1: 'Vision UI', 2: subfamily, 3: 'VisionUI-' + subfamily,
                       4: 'Vision UI ' + subfamily, 6: 'VisionUI-' + subfamily,
                       16: 'Vision UI', 17: subfamily, 25: 'VisionUI'}.get(record.nameID)
        if replacement is not None:
            record.string = replacement.encode(record.getEncoding(), errors='replace')
    font.save(root / filename)
    print(filename, (root / filename).stat().st_size)
