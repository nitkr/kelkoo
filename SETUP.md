# Kelkoo setup

Personal Android music app (`app.kelkoo.music`). JDK 21 + Android SDK required.

```bash
git clone https://github.com/nitkr/kelkoo.git
cd kelkoo
# set sdk.dir in local.properties
./gradlew assembleUniversalFossDebug
```

FOSS debug package: `app.kelkoo.music.debug`. Prefer FOSS for personal sideload.

Derived from Echo Music (GPL-3.0) — see `LICENSE` / `NOTICE`.
