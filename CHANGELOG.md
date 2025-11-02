**Packed Packs 2.0.0**

- Added **Developer Mode** which adds more options in the context menu. Toggle with `F12` or `Ctrl` + `Shift` + `I`.
  See Description/Readme for more details.
- Moved config to the `config/packed_packs` directory. Your config will automatically be migrated in this version if the
  `config/packed_packs/config.json` does not exist, but `config/packed_packs.json` does.
- No longer remembers last viewed profile by default to avoid confusion. It's now added as an option.
- Fixed infinite screen loops with original screen and buttons added by other mods in the original screen.
- Various UI fixes.

---

- Removed 1.3.0-beta.1 automatic override migration. If you're still on 1.3.0-beta.1 and used overrides, your
  configuration will either reset or cause the game to crash.