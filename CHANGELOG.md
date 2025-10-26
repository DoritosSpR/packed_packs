We're jumping straight to v2, this is due to breaking changes in the config file. Don't worry, your existing config
will be migrated to the new version, although this may be removed in a future update.

- Config changes
    - Moved to `/config/packed_packs/config.json`
    - Each profile is now saved in its own file, allowing modpack devs to selectively update profiles.
    - The default profile reference is now saved to `/config/packed_packs/config.meta.json`.
- Added **Copy to Clipboard** option to context menu in dev mode, which copies the pack ID.
- **Pack Aliases**
    - Add aliases for specific pack IDs, accessible via the context menu in dev mode.
    - Designed to help modpack devs migrate resource/data packs when updates are needed without affecting the user's
      configured pack selection and order.
    - Saved to `/config/packed_packs/config.meta.json`
    - Only used if the pack is selected but doesn't exist. Matching starts from the top of the alias map
      and resolves to the first match, see config file.
    - Supports regex (must be prefixed with `regex:`), the text color will change in the _Edit Aliases_ dialog.
    - Exact matches always take priority over regex matches, regardless of their position in the alias map (e.g., a pack
      ID of `file/test-v1.2` will always match `"file/test-v1.2": "file/test-v1.4"` over `"regex:file\\/test-v\\d*": "file/test-v2"`).
    - Aliases are never cleared automatically, even if it points to a pack that no longer exists. They are only removed
      manually from the in-game GUI or the config file.
    - While aliases should work for data packs, I generally don't recommend it as it could break worlds. Test
      thoroughly.
- Slightly improved initial startup time of the Packed Packs screen, and made other small optimizations.
- Updated Ukrainian translation ([#31](https://github.com/fishstiz/packed_packs/pull/31) by StarmanMine142)
- Updated Russian translation ([#32](https://github.com/fishstiz/packed_packs/pull/32) by iceban)
- As usual, this is a beta version, things may break or change. If no more feedback or issues are reported,
  a stable version may be released next week (still keeping the auto migration of the config).