# Local VLC 3 program/service selection

The implementation is applied to the local VLC and libvlcjni repositories.
These patch files preserve changes to the ignored independent repositories.
They are not applied automatically by compile.sh. Initialize the matching VLC
checkout and apply the previous HTTP TS seeking patch first, then from the
vlc-android root:

```sh
git -C libvlcjni/vlc apply --check ../../buildsystem/patches/vlc/program-selection.patch
git -C libvlcjni/vlc apply ../../buildsystem/patches/vlc/program-selection.patch
git -C libvlcjni apply --check ../buildsystem/patches/vlc/libvlcjni-program-selection.patch
git -C libvlcjni apply ../buildsystem/patches/vlc/libvlcjni-program-selection.patch
```

Do not reapply patches to an already modified checkout. The get-vlc.sh
Message-Id patch workflow remains the integration path for upstream libvlcjni.

VLC exposes the program variable's service ID/name choices, current selection,
and validated asynchronous selection requests through three C APIs. A new
ProgramChanged event (0x11f) forwards INPUT_EVENT_PROGRAM to Java. Lists use
libvlc_track_description_list_release; input references are released on all
paths. JNI gracefully disables the extension when built against an unmodified
VLC header. Program requests use the input control queue; selected tracks and
program clocks stay managed by the existing VLC pipeline.

Android accesses this optional API through a compatibility bridge so published
LibVLC and VLC 4 dependencies can still compile. The existing ProGuard rule
keeps org.videolan.libvlc classes and members. Unsupported builds show no
channel selector. The tracks dialog offers Channels only for multiple services
and local playback (not casting). Its single-choice list shows names and IDs,
highlights the current service, and accepts DPAD navigation. Service/track
updates refresh the tracks dialog; listeners are removed on view destruction.
Program metadata remains a snapshot while the single-choice popup is open;
reopen it to pick up subsequent name changes. Selections are not persisted.

Independent implementation commits:

- VLC: `91d52f485a799f61c4e699b4fe90ae90287d76c4`
- LibVLC JNI: `c1383615c063992874e891b8e0e4cf288af57c23`

## Validation (2026-10-21, Asia/Tokyo)

- Rebuilt local armeabi-v7a VLC/LibVLC and JNI successfully.
- Built `:application:app:assembleDev`; verified APK signature and checked that
  packaged libvlc.so and libvlcjni.so match the local build byte-for-byte.
- Verified the three C program-selection symbols are exported.
- Installed on Sony BRAVIA 4K VH2 with `adb install -r` (Success).
- Final APK SHA-256:
  `50172f1363a0773c1366bd32c3d28d7cb33863fce8906ed7c50b42a7fcb806c9`.
- Checked diffs and verified both saved patches reverse-apply to the checkouts.
  Existing toolchain/deprecation warnings remain.

The user's multi-service TS was played on the device. Logs showed selection
requests and matching completed program selections for this sequence:

| Previous service | Requested service | Request time | Selection time |
| --- | --- | --- | --- |
| 23608 | 23610 | 10:19:41.826 | 10:19:42.626 |
| 23610 | 23609 | 10:19:47.938 | 10:19:48.692 |
| 23609 | 23992 | 10:19:52.813 | 10:19:53.534 |
| 23992 | 23993 | 10:19:59.891 | 10:20:00.702 |
| 23993 | 23608 | 10:20:06.837 | 10:20:07.618 |

All five requests completed (about 0.7-0.8 seconds each), including returning
to the original service. Each was followed by a matching DEMUX_SET_GROUP and
video/audio decoder startup. No FATAL EXCEPTION, UnsatisfiedLinkError or
NoSuchMethodError was recorded. MediaCodec capability-query warnings appeared,
but were followed by successful mediacodec decoder selection; these warnings
were not treated as failed switches.

SDT names decoded as null in the tested file. IDs identify the channels; the
user confirmed normal channel switching and explicitly accepted the current
service-ID-based behavior without requiring service-name improvements.
Raw logs and generated APKs are not committed.

Not yet verified: switching while paused, subtitle switching, timestamp
continuity and +/-15-second seeking after changing services, single-program
visibility, stop/reopen lifecycle, removed IDs, casting and unsupported builds
on a device, other ABIs, and VLC 4 compilation. Compatibility fallbacks for
unmodified LibVLC are implemented but have not been separately exercised.
