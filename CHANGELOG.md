- Added option to unfix fixed position of pack.
- Enabled fixed and required overrides for the available packs list.
- Disabled option to unrequire a pack on non-default profiles. (as it gets added anyway if removed)
- Updating the required override now moves the packs where it should.
- Fixed unsaved changes being discarded when toggling the lock on the current profile.
- Fixed overrides being cleared when disabling the pack.
- Config format for overrides has changed. If you used overrides in 1.3.0-beta.1, the format will automatically be
  converted in this update.
    - **WARNING**: the automatic conversion for overrides will be removed on stable release, and may cause your config
      to reset or game to crash if your overrides hasn't been converted by then.  
- **This is a beta version, things may break or change!** Submit your feedback [here](https://github.com/fishstiz/packed_packs/issues).