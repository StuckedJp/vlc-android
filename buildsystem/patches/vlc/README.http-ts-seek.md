# HTTP MPEG-TS duration and seeking (VLC 3)

The patch is also applied to the local ignored `libvlcjni/vlc` checkout.
It is preserved here because that independent repository is not tracked by
vlc-android. This directory is not automatically applied by compile.sh.

After initializing a fresh matching VLC 3 checkout, apply from the project root:

```sh
git -C libvlcjni/vlc apply --check ../../buildsystem/patches/vlc/http-ts-seek.patch
git -C libvlcjni/vlc apply ../../buildsystem/patches/vlc/http-ts-seek.patch
```

Do not reapply to an already patched checkout. For upstream integration, port
this change into libvlcjni/libvlc/patches using its get-vlc.sh / git-am workflow
and Message-Id convention. Tested revision pins are not changed here.

The default-enabled `ts-allow-slow-seek` option permits boundary probes and time
seeks only for HTTP(S) inputs reporting byte seeking and a known size. Disable
with `--no-ts-allow-slow-seek`. HTTP fast-seek capabilities remain unchanged.
Unknown-size/nonseekable inputs and fast-seekable local files keep their paths.
A growing recording with a reported size cannot be distinguished from a fixed
file here; only fixed recordings are supported by this feature.

Remote probes are attempted once per program. Time searches are limited to
32 seek attempts, 160000 TS packets and a 10-second deadline; boundary chunks
have a 5-second deadline and the existing 5000-packet outer limit. Deadlines
are checked between blocking I/O calls, not an HTTP timeout replacement.
Failed searches/probes attempt to restore the original byte position.

## Time-search convergence fix

Time searches use PCR exclusively when a valid PCR PID is available, avoiding
mixing it with decoder timestamps that can lead PCR by over 500 ms. When the
remote binary search converges without a sample in the 500 ms window, it may
restart at the closest preceding sample within two seconds. The existing
ES_OUT_SET_NEXT_DISPLAY_TIME request discards preroll to the target. This
fallback is restricted to the slow HTTP path and is disabled on cancellation
or budget exhaustion. Success and preroll distances are logged for device QA.

The captured Twonky recording has PCR 4303.188289 seconds followed by video
DTS 4304.145700 seconds in the sampled region. The old mixed-clock search can
therefore jump forward/backward according to which timestamp is encountered
first. The capture had four exhausted time searches despite valid HTTP 206
responses. The final device run completed all 15 logged time searches successfully.

Local VLC implementation commit: `4aca76bb1b1ab111fa5b34e2f20ef629b558ae5c`.

## Validation results (2026-10-21, Asia/Tokyo)

- Rebuilt local VLC 3 / LibVLC for armeabi-v7a with Android NDK 21.4.7075529.
- Built `:application:app:assembleDev` successfully, verified APK signature,
  and compared the APK's libvlc.so byte-for-byte with the rebuilt local library.
- Installed using `adb install -r` on Sony BRAVIA 4K VH2; installation succeeded.
- Twonky returned HTTP 206 and matching Content-Range for nonzero byte offsets.
- The initial patched run obtained 1,440,990 ms (about 24:01) duration but
  recorded four failed time searches. That prompted the PCR convergence fix.
- The final run recorded 15 successful time searches, no time-search failures,
  no position-restoration errors, and no FATAL EXCEPTION. The preceding-sample
  fallback was not needed (zero Time seek preroll messages).
- Right-key media-session positions: 163340 -> 178340 -> 193340 -> 208340 ms.
- Left-key media-session positions: 196701 -> 181701 -> 166701 ms.
  Both demonstrate 15000 ms steps. Rapid input can coalesce intermediate
  engine requests; each notified key step remains 15 seconds.
- The user confirmed normal seeking on the television after the final run.
- Final installed APK SHA-256:
  `869a364df9a711d8ce75adb7f96ed623a46fc77e5b8631d326cf9ed7e9f9f5bb`.

The application change fixes ordinary DPAD left/right seeks at +/-15 seconds;
modifier-key shortcuts, double-tap preferences and 360-degree navigation keep
their existing behavior. The APK is under
`application/app/build/outputs/apk/dev/VLC-Android-3.7.2-all.apk`; despite its
name, this build contains armeabi-v7a only. Binaries and raw device logs are
not committed.

Not yet validated: TVersity, other ABIs, paused seeking, file boundaries,
multiple programs, refused/malformed range responses, unknown size, connection
loss/cancellation, and post-change USB TS / HTTP MP4 regression checks.
USB TS seeking worked before this change, as reported by the user.
Use a local LibVLC Dev build: assembleDebug uses the published dependency.

Build warnings included existing signedness/deprecation warnings and inability
to strip libvlc.so (packaged unchanged); builds and signature verification passed.
