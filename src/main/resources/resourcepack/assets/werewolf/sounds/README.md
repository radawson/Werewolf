# Werewolf resource-pack audio

Drop Ogg Vorbis (`.ogg`, mono recommended) files here. `../sounds.json` maps them to
in-game sound events the plugin plays:

| File (this folder)      | sounds.json event | Played by the plugin as | When |
|-------------------------|-------------------|-------------------------|------|
| `transform.ogg`         | `transform`       | `werewolf:transform`    | on transformation |
| `howl.ogg`              | `howl`            | `werewolf:howl`         | (available for future use) |

Notes:
- The plugin already calls `werewolf:transform` on transform — as soon as `transform.ogg`
  exists here and the pack is rebuilt, players who accepted the pack will hear it.
- Missing files simply produce no sound (no error), so it's safe to ship without them.
- To add more sounds: add the `.ogg` here and a matching event block in `../sounds.json`.

## Adding other content (textures / item models)
- Item textures / custom-model-data models go under `assets/minecraft/` (e.g.
  `assets/minecraft/models/item/…`, `assets/minecraft/textures/…`).
- Everything under `src/main/resources/resourcepack/` is zipped into `resourcepack.zip`
  at runtime and served on the pack HTTP server.
- Keep `pack.mcmeta`'s `pack_format` aligned with the server's Minecraft version.
