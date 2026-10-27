# Native SMB removal

`0001-disable-native-smb.patch` changes the LibVLC Android build script. The
parent `buildsystem/compile.sh` applies it after fetching libvlcjni, or skips it
when the reverse check confirms that it is already present. A conflicting
checkout fails explicitly rather than silently building with SMB enabled.

The patch was developed against libvlcjni `a24a8d7` (VLC 3). Other revisions,
including VLC 4, must pass the patch applicability check and be built/tested
before use. Tested dependency pins are unchanged.

The build disables libdsm and libsmb2 in contrib, and configures VLC with
`--disable-smbclient --disable-dsm --disable-smb2`. This removes SMB1/SMB2 access
and libdsm's SMB service discovery, including the `smb`, `smb2`, and `cifs` URI
aliases. The parent script also applies `../vlc/remove-smb-discovery.patch`
after fetching VLC. It removes only `_smb._tcp.local` from the common mDNS
protocol table, keeping FTP, NFS, SFTP, RTSP, and Chromecast discovery.
UPnP, HTTP, and MPEG-TS sources are unchanged.

Existing VLC configurations are regenerated if they lack these options. Old
SMB module archives are also blacklisted when generating the static module
registry. The link package list ignores leftover libdsm, libsmb2, and libtasn1
pkg-config files, so an old or prebuilt contrib prefix cannot reintroduce them.
Iconv is retained because other features use it. Cached source archives and
previous intermediate outputs are not deleted.

Build from the repository root with the SDK/NDK environment configured:

```sh
./buildsystem/compile.sh -a armeabi-v7a
```

For an existing patched checkout, the native-only step can also be run directly:

```sh
./libvlcjni/buildsystem/compile-libvlc.sh -a armeabi-v7a
./gradlew :application:app:assembleDev
```

Rebuild each ABI that will be packaged. Verify the generated static module
registry and the resulting `libvlc.so`: SMB/DSM entry points must be absent,
while UPnP, HTTP, and TS module entry points remain. Check that the APK contains
that rebuilt library. Device QA should cover UPnP discovery, TS seeking,
program selection, consecutive playback, and remote key handling.

This affects Dev and other builds using the local LibVLC project. Debug and
Release still use the published LibVLC dependency unless their dependency
configuration is changed separately.

## Verification (2026-10-27)

- Rebuilt native VLC 3 / armeabi-v7a with NDK 21, then assembled Dev.
- Configure selects none of `smb`, `smb2`, or `dsm`.
- Static module registration and defined symbols contain none of those modules;
  SMB client symbols and `_smb._tcp.local` are absent from the resulting library.
- UPnP, HTTP(S), TS, and microdns module entry points remain. FTP/NFS/SFTP and
  Chromecast mDNS service strings, TS options, and program-selection APIs remain.
- APK `lib/armeabi-v7a/libvlc.so` matches the rebuilt library byte-for-byte.
- The 14 Android-side protocol and playback-resume unit tests pass.
- Both patches pass forward/reverse checks; automatic already-applied detection
  was exercised for both checkouts. Device playback has not been tested here.
