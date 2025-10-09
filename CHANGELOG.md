### Developer Mode
- Press `Ctrl` + `Shift` + `I` to toggle Developer Mode.
- Additional options will appear in the context menu when in developer mode.
- Lock profiles. ([#29](https://github.com/fishstiz/packed_packs/issues/29))
- **Override pack properties** in profiles.
- Set a **Default Profile** for resource and data packs.
- Toggle the visibility of specific widgets under **Preferences**.

#### Profile Pack Overrides
- Only enabled for the profile unless its set as the **default**.
- Override required and position properties of packs.
- Hide packs.

#### Default Profiles
- **Pack overrides** under the default profile are enabled **globally**.
- Packs under the default profile are automatically marked as compatible.
- Resource Packs
  - Applies when the `options.txt` file is missing.
- Data Packs
  - Applies when creating a new world.

#### Preferences
- Toggle visibility of:
    - original screen button
    - options button
    - action bar button
    - hide incompatible button
    - show packs button in folder packs
    - ETF button (if installed)
    - Respackopts button (if installed)
    - VTD button and pencil (if installed)

### Other Changes
- No longer opens the last viewed profile when opening the Packed Packs screen.
- 1.21.9:
  - Fixed list not scrolling in keyboard navigation.
  - Fixed double click detection not localized to widget level.
  - Fixed being able to disable a required pack by double-clicking.
