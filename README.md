# Villagers Reborn Fabric 1.20.1

An unofficial native Fabric port of Villagers Reborn by JaviC (Javieljefe). This is not the newer upstream source tree: the starting point was the exact Forge Minecraft 1.20.1 1.0.9b release, remapped and decompiled, then adapted for Fabric. The author's current repository linked by the project is not the source of this exact port. Mod ID: `slimpatch`.

## Build

Requires JDK 21 to run Gradle/Loom, JDK 17 for compilation, and network access for dependencies and the pinned upstream Forge artifact:

```sh
JAVA21_HOME=/path/to/jdk-21 JAVA17_HOME=/path/to/jdk-17 ./build.sh :villagers_reborn:build
```

The remapped JAR is produced in `ports/villagers_reborn/build/libs/`. It contains the original entity textures extracted from the pinned upstream JAR. No prebuilt mod is distributed here. This build fails rather than substitute missing textures if the download, SHA-512, or expected asset count is wrong. The output can contain assets with separate rights; do not assume you may redistribute the resulting JAR.

## Provenance and rights

The author's [project listing](https://api.modrinth.com/v2/project/villagers-reborn) identifies the project license as MIT. The MIT terms for the author's code are in `LICENSE`; this is the author-primary project declaration, not the MIT license for NeoForge's unrelated MDK template. The Forge artifact's `mods.toml` says “All Rights Reserved”; the original port's `fabric.mod.json` retains the conflicting notice. The project listing additionally says Skindex skins belong to their original creators and permissions were confirmed where available. It does not grant us blanket redistribution rights for those skins. See the original [`SKIN_CREDITS.md`](SKIN_CREDITS.md) and [upstream credits](https://github.com/Javieljefe/Villagers-Reborn/blob/main/SKIN_CREDITS.md); the credits describe the mod as non-commercial and shared free. Keep those separate rights intact. Villagers Reborn's project credits lead art to Elin R. (Modge), animation to Astrid, Russian translation to Max Snegir and Portuguese translation to Felipe Campos.

Conservatively **all 914 files** in `ports/villagers_reborn/src/main/resources/assets/slimpatch/textures/entity/**` are omitted from this source repository, including any entity textures that may not be Skindex skins. The build fetches the exact [Forge 1.0.9b artifact](https://cdn.modrinth.com/data/cS9I57mu/versions/4fDzNnav/villagersreborn-1.0.9b.jar) ([version metadata](https://api.modrinth.com/v2/version/4fDzNnav)), verifies SHA-512 `dc34ec6bb0ae00bf13c614d49050e7fb47d56b6e929a88156e35a42db763584a40b97b12b067224d5ef81dbf27aab65b797cb7de2d3898b1994ae3a80b5040ec`, and extracts only that subtree to an ignored build directory. At preparation all excluded files matched the pinned artifact byte-for-byte; other port resources remain in source. Rights to the extracted assets stay with their respective owners; fetching them grants no new license. Build locally only where you are entitled to use them.
