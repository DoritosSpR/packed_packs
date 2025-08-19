# 📦 Packed Packs
Pack resource and data packs into profiles with multiple selection, drag and drop, and extended mouse and keyboard controls.

![packed_packs_demo_compressed](https://github.com/user-attachments/assets/6f7995cc-665d-4989-976c-d07ee2ca1ecf)

## ✨ Features
-   Save and load custom profiles.
-   Select multiple packs at once.
-   Drag and drop selection between columns.
-   Folder Packs.
-   Search by title.
-   Filter out incompatible packs.
-   Sort alphabetically or by last updated.
-   Additional mouse and keyboard controls.
-   History (undo and redo).

### 🖱️ Mouse Controls
- Hold <kbd>Shift</kbd> while clicking to select range.
- Hold <kbd>Ctrl</kbd> while clicking to add/remove from selection.
- Double click to quickly transfer a single entry.
- Click backwards side button to undo.
- Click forwards side button to redo.

### ⌨️ Keyboard Controls
- Navigate entries with <kbd>↑</kbd> and <kbd>↓</kbd> arrow keys.
- Navigate out of entries with <kbd>Tab</kbd>.
- Press <kbd>Space</kbd> or <kbd>Enter</kbd> to transfer selection.
- Hold <kbd>Shift</kbd> with <kbd>↑</kbd> and <kbd>↓</kbd> arrow keys to select range.
- Hold <kbd>Ctrl</kbd> or <kbd>Alt</kbd> with <kbd>↑</kbd> and <kbd>↓</kbd> arrow keys to move selection.
- Press <kbd>Ctrl</kbd> + <kbd>Z</kbd> to undo.
- Press <kbd>Ctrl</kbd> + <kbd>Shift</kbd> + <kbd>Z</kbd> or <kbd>Ctrl</kbd> + <kbd>Y</kbd> to redo.
- Press <kbd>Enter</kbd> to open a single folder pack. <kbd>Space</kbd> to transfer.
- Press <kbd>Escape</kbd> to close a folder pack.
- Type any character to automatically focus search bar.

### 📂 Folder Packs
- Any folder in the root pack directory containing packs, without a `pack.mcmeta`, will be treated as a folder pack.
- Folder packs behave like regular packs and can be moved between rows and columns to toggle multiple packs at once.
- They can be opened to view and reorder their contents. The order is saved to `packed_packs.folderpack.json` in the
  folder root.
- Add a `pack.png` at the folder root to set a custom icon.

### ⚙️ Configuration
- Apply resource packs automatically on close.
- Replace the default resourcepack & datapack screens.
- Remove the red background on incompatible packs.
