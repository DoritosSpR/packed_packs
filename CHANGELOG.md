**WARNING**: This update will deselect/disable any packs under `additionalFolders`.
<br>

- Added Reset to Enabled option in context menu, which resets your packs to what is currently enabled.
- Added Show in File Manager option in context menu.
- Added keyboard shortcuts:
    - Delete File: `Delete`
    - Rename File: `Ctrl` + `R` or `F2` (if unbound from screenshot)
    - Open File: `Ctrl` + `Enter`
    - Show in File Manager: `Alt` + `Shift` + `R`
    - Refresh Packs: `F5`
- Deleted files will attempt to move to trash first instead of permanently deleting.
- Added the pencil button from VTDownloader ([#6](https://github.com/fishstiz/packed_packs/issues/6)).
- Replace original screen option should always replace original screen regardless of where the screen is
  opened. ([#23](https://github.com/fishstiz/packed_packs/issues/23))
- Replace original screen default value set to `true`.
- Removed unique prefix in additional folder pack ids. This allows packs to easily be movable across other additional
  folders, but you'll have to resolve duplicate pack ids yourself.
- Fixed vanilla sort not accounting for feature packs.
- Fixed being able to register base folder as an additional folder causing packs to be discovered twice on said folder.
