# Lyrics service protocols

VisionMod implements its own small HTTP clients using the JDK. No third-party music SDK, application binary, browser profile, or browser extension is bundled.

- LRCLIB: https://lrclib.net/docs — exact and structured/free-text searches. Metadata and duration are checked before using timed or plain lyrics.
- lyrics.ovh: https://lyricsovh.docs.apiary.io/ — optional plain-text fallback, after the timed providers have been tried.
- Yandex Music: the track search and authenticated `/tracks/{id}/lyrics` protocol is described by the independent yandex-music-api project's documentation: https://yandex-music.readthedocs.io/ . Its source reference is https://github.com/MarshalX/yandex-music-api/blob/main/yandex_music/_client/tracks.py and request-signature documentation is https://github.com/MarshalX/yandex-music-api/blob/main/yandex_music/utils/sign_request.py . That project is licensed LGPL-3.0; its Python implementation is not copied, linked, or distributed here. The Java client independently constructs the protocol's standard HMAC-SHA256 request signature using its public client compatibility constant. This constant is separate from the user's OAuth credential.

Yandex Music's endpoints are not a stable, officially supported third-party lyrics API. Availability may change. A valid user OAuth token and whatever account/content access Yandex requires are necessary; a denied request is handled as unavailable and does not bypass provider restrictions. Signed content URLs are fetched without forwarding the account token. The token is masked in the GUI, kept only in the running JVM, and omitted from configs and profiles.

No service guarantees lyrics for every recording or provides timestamps for every text. Plain words are retained as plain words and have explicit manual line controls; timing is never fabricated from song duration. Failed searches are retried rather than cached as permanent misses. Lyrics content is not printed in CI logs.
