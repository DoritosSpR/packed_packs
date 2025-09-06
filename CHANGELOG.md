- **Context Menus**: Right-click anywhere to open the context menu. Perform action such as:
    - Renaming, Deleting, Opening, and Refreshing packs.
- **Additional Folders** ([#18](https://github.com/fishstiz/packed_packs/issues/18)): Lets you add an additional folder
  for pack discovery.
    - An additional folder can be added by going to the config `config/packed_packs.json`and adding a path to the folder
      in the `additionalFolders` array under `resourcepacks` or `datapacks`.
    - If the `additionalFolders` array does not exist, you may add it yourself or Open and Close the Packed Packs Screen
      to update the config.
    - Path can be absolute or relative to the game directory.
    - Shows up as a context menu action under "Open Pack Folder" if done correctly.
    - **Requires restart to apply**!
- Added keyboard shortcuts for the context menu actions:
    - Delete File: `Delete`
    - Rename File: `Ctrl` + `R` or `F2` (if unbound from screenshot)
    - Open File: `Ctrl` + `Enter`
    - Show in File Manager: `Alt` + `Shift` + `R`
    - Refresh Packs: `F5`
- Added real-time file watching in the Packed Packs screen for folder packs, additional folders, and other folder
  sources/providers (`FolderRepositorySource`/`FileResourcePackProvider`) that may be registered by other mods.
- Added the pencil button from VTDownloader ([#6](https://github.com/fishstiz/packed_packs/issues/6)).
- Updated Ukrainian Translation ([#21](https://github.com/fishstiz/packed_packs/pull/21)).
- Now replaces the original screen by default.
- Fixed bugs (see beta changelog).
