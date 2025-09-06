# 📦 Packed Packs
Pack resource and data packs into profiles with multiple selection, drag and drop, and extended mouse and keyboard controls.

![packed_packs_demo_compressed](https://github.com/user-attachments/assets/6f7995cc-665d-4989-976c-d07ee2ca1ecf)

## ✨ Features
-   Save and load custom profiles.
-   Select multiple packs at once.
-   Drag and drop selection between columns.
-   Additional Folders.
-   Folder Packs.
-   Search by title.
-   Filter out incompatible packs.
-   Sort alphabetically or by last updated.
-   Mouse and keyboard controls.
-   History (undo and redo).

### 📂 Additional Folders
- Add extra folders for pack discovery.
- Configure in `config/packed_packs.json` by adding paths under the `additionalFolders` array inside `resourcepacks` or `datapacks`.
- If the array doesn’t exist, create it manually or open and close the Packed Packs screen to update the config.
- Paths can be absolute or relative to the game directory.
- Correctly added folders appear as a context menu option under **Open Pack Folder**.
- **Requires game restart to apply.**

### 📂📦 Folder Packs
- Any folder in the root pack directory containing packs, without a `pack.mcmeta`, will be treated as a folder pack.
- Folder packs behave like regular packs and can be moved between rows and columns to toggle multiple packs at once.
- They can be opened to view and reorder their contents. The order is saved to `packed_packs.folderpack.json` in the
  folder root.
- Add a `pack.png` at the folder root to set a custom icon.

### 🖱️ Mouse Controls
- Hold <kbd>Shift</kbd> while clicking to select range.
- Hold <kbd>Ctrl</kbd> while clicking to add/remove from selection.
- Double click to quickly transfer a single entry.
- Click backwards side button to undo.
- Click forwards side button to redo.

### ⌨️ Keyboard Controls
- Navigate entries — <kbd>↑</kbd> / <kbd>↓</kbd>
- Navigate out of entries — <kbd>Tab</kbd>
- Transfer selection — <kbd>Space</kbd> / <kbd>Enter</kbd>
- Select range — <kbd>Shift</kbd> + <kbd>↑</kbd> / <kbd>↓</kbd>
- Move selection — <kbd>Ctrl</kbd> / <kbd>Alt</kbd> + <kbd>↑</kbd> / <kbd>↓</kbd>
- Undo — <kbd>Ctrl</kbd> + <kbd>Z</kbd>
- Redo — <kbd>Ctrl</kbd> + <kbd>Shift</kbd> + <kbd>Z</kbd> / <kbd>Ctrl</kbd> + <kbd>Y</kbd>
- Open folder pack — <kbd>Enter</kbd>
- Close folder pack — <kbd>Escape</kbd>
- Delete file — <kbd>Delete</kbd>
- Rename file — <kbd>Ctrl</kbd> + <kbd>R</kbd> / <kbd>F2</kbd> (if not bound to screenshot)
- Open file — <kbd>Ctrl</kbd> + <kbd>Enter</kbd>
- Show in file manager — <kbd>Alt</kbd> + <kbd>Shift</kbd> + <kbd>R</kbd>
- Refresh packs — <kbd>F5</kbd>
- Focus search bar — type any character

### ⚙️ Configuration
- Apply resource packs automatically on close.
- Replace the default resourcepack & datapack screens.
- Remove the red background on incompatible packs.
