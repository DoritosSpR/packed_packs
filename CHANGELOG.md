#### 📂 Folder Packs ([#14](https://github.com/fishstiz/packed_packs/issues/14))

- Any folder in the root pack directory containing packs, without a `pack.mcmeta`, will be treated as a folder pack.
- Folder packs behave like regular packs and can be moved between rows and columns to toggle multiple packs at once.
- They can be opened to view and reorder their contents. The order is saved to `packed_packs.folderpack.json` in the
  folder root.
- Add a `pack.png` at the folder root to set a custom icon.

---

- Added Ukrainian translation ([#17](https://github.com/fishstiz/packed_packs/pull/17) by StarmanMine142)
- Added keyboard shortcut to open folder packs with `Enter`. Pressing `Space` still transfers the folder
  pack.
    - Folder packs can be closed with Escape or by navigating to the other pack list.
- Child packs enabled individually from the Vanilla Screen now cause their folder pack to appear in both columns in the
  Packed Packs Screen.
- Renamed `packed_packs.manifest.json` → `packed_packs.folderpack.json`.
- Updated the default icon for folder packs.
- Fixed pack list focus jumping around after closing a folder pack.
- Fixed minor resource leak when loading folder packs.
