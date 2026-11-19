"""Probe public catalogs, retaining only metadata and counts, never lyrics or recordings."""
import json
from concurrent.futures import ThreadPoolExecutor
from urllib.parse import urlencode
from urllib.request import Request, urlopen


def get(url, **params):
    request = Request(url + '?' + urlencode(params, doseq=True), headers={
        'User-Agent': 'VisionMod/2.0.0 (https://github.com/De1po3oN/VisionMod-2.0.0)'
    })
    with urlopen(request, timeout=14) as reply:
        return json.load(reply)


def lrclib(query):
    tracks = get('https://lrclib.net/api/search', q=query)
    return [{key: item.get(key) for key in ('id', 'trackName', 'artistName', 'albumName', 'duration', 'instrumental')}
            | {'timedLines': len((item.get('syncedLyrics') or '').splitlines()), 'plain': bool(item.get('plainLyrics'))}
            for item in tracks]


def commons(query):
    result = get('https://commons.wikimedia.org/w/api.php', action='query', generator='search',
                 gsrsearch=query, gsrnamespace=6, gsrlimit=8, prop='imageinfo',
                 iiprop='url|mime|extmetadata', iiextmetadatafilter='LicenseShortName|LicenseUrl', format='json')
    return [{'title': page.get('title'), 'audio': [{key: info.get(key) for key in ('url', 'mime', 'extmetadata')}
             for info in page.get('imageinfo', [])]} for page in result.get('query', {}).get('pages', {}).values()]


def archive():
    result = get('https://archive.org/advancedsearch.php', q='(title:"Священная война" OR title:"The Sacred War") AND mediatype:audio',
                 output='json', rows=12, **{'fl[]': ['identifier', 'title', 'creator', 'licenseurl']})
    return result.get('response', {}).get('docs', [])


def itunes():
    result = get('https://itunes.apple.com/search', term='Священная война', limit=12, entity='song', country='US')
    return [{key: track.get(key) for key in ('trackName', 'artistName', 'collectionName', 'trackTimeMillis', 'trackViewUrl')}
            for track in result.get('results', [])]


def probe(item):
    name, operation = item
    try:
        return name, operation()
    except Exception as error:
        return name, {'error': str(error)}


with ThreadPoolExecutor(max_workers=6) as pool:
    result = dict(pool.map(probe, {
        'lrclib_ru': lambda: lrclib('Священная война'),
        'lrclib_en': lambda: lrclib('The Sacred War'),
        'lrclib_transliterated': lambda: lrclib('Svyashchennaya Voyna'),
        'commons_ru': lambda: commons('filetype:audio "Священная война"'),
        'commons_en': lambda: commons('filetype:audio "Sacred War"'),
        'archive': archive,
        'itunes': itunes,
    }.items()))
print(json.dumps(result, ensure_ascii=False, indent=2))
