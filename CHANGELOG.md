- Added **Context Menus**. Right-click anywhere on the screen to open the context menu. Right-click on certain elements
  to change the context. Options include:
    - **Rename** a pack file.
    - **Delete** a pack file.
    - Refresh packs.
- Added **Additional Folders** ([#18](https://github.com/fishstiz/packed_packs/issues/18)). Packs can be discovered from
  other directories by adding its path to `config/packed_packs.json` array:
    - `datapacks.additionalFolders` for datapacks
    - `resourcepacks.additionalFolders` for resource packs
    - Requires restart to apply!
- Switched to polling for real-time file watching due to Windows directory locking (JDK-6972833). Performance may take a
  hit.
- Fixed packs' metadata not updating in real-time.
- Fixed packs inside Folder Packs not being watched for updates.
- Fixed packs not refreshing when entering the screen.
- Fixed packs unnecessarily refreshing when applying changes.
- Fixed pack entries showing hover state even when overlapped by another component.
- Fixed dead zone between pack entries.
- Fixed Folder Packs being rejected when dragged in from outside the game window.

### **WARNING**:

This is a beta version! Things may break or change. Please report any issues and share your feedback, thanks.