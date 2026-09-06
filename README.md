# Kelkoo

Personal Android music player for YouTube Music — phone + Android Auto. Sideload APK only (`app.kelkoo.music`). Not published to Play Store.

## What it is

Kelkoo is a personal fork oriented around driving use: search, library, playlists, queue, and Android Auto browse (Library / Playlists / Recents / Search) with Now Playing metadata.

## Scope (MVP)

- Android phone app + Android Auto
- Sideload install (enable Auto **Unknown sources** for car use)
- FOSS build flavor for personal use

**Out of scope for now:** CarPlay, macOS, Play Store listing.

## Build

Requires JDK 21 and Android SDK. Then:

```bash
./gradlew assembleUniversalFossDebug
```

Debug package id: `app.kelkoo.music.debug`

See `SETUP.md` only if you need SDK path notes; treat product naming in this README as authoritative (Kelkoo).


## Contributors

<!-- readme: contributors -start -->
<table>
<tr><td align="center"><a href="https://github.com/nitkr"><img src="https://github.com/nitkr.png" width="60" height="60" alt="nitkr" /><br/>nitkr</a></td></tr>
</table>
<!-- readme: contributors -end -->

Owner: [nitkr](https://github.com/nitkr). Automation commits may appear as `github-actions[bot]`.

## Credits

Kelkoo is derived from [Echo Music](https://github.com/EchoMusicApp/Echo-Music) (GPL-3.0). See `LICENSE` and `NOTICE` for full licensing and attribution. Upstream Echo branding, screenshots, Discord, and donation links are not part of Kelkoo.

## License

GPL-3.0 — see `LICENSE`.
