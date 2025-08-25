### **WARNING**:

**This is a beta version!** Things may break or change. Please report any issues and share your feedback, thanks.

#### **Context Menus**

Right-click anywhere on the screen to open the context menu. Right-click on certain elements to change the context.
Options include:

- **Rename** a pack file.
- **Delete** a pack file.
- **Open** a pack file.
- Refresh packs.
- Open Pack Folder/s (more below)

#### **Additional Folders** ([#18](https://github.com/fishstiz/packed_packs/issues/18)).

**IMPORTANT**: As this is a beta version, do not yet use on datapacks you don't want to lose. Pack IDs under these
folders have been modified in an attempt to avoid ID conflicts from other folders. This is not final and may
unexpectedly disable datapacks when updating.

Packs can be discovered from other directories by adding its path (absolute or relative to the game directory) to
`config/packed_packs.json` string array:

- `datapacks.additionalFolders` for datapacks.
- `resourcepacks.additionalFolders` for resource packs.
- You may add the keys yourself if they are missing, or open and close the Packed Packs screen to update the config.
- Requires restart to apply!

---

- Switched to polling for real-time file watching due to Windows directory locking (JDK-6972833). May impact
  performance.
- Fixed packs' metadata not updating in real-time.
- Fixed packs inside Folder Packs not being watched for updates.
- Fixed packs not refreshing when entering the screen.
- Fixed packs unnecessarily refreshing when applying changes.
- Fixed pack entries showing hover state even when overlapped by another component.
- Fixed dead zone between pack entries.
- Fixed Folder Packs being rejected when dragged in from outside the game window.
