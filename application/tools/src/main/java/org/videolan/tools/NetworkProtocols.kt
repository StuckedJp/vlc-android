package org.videolan.tools

/** SMB aliases supported by LibVLC but deliberately unavailable in the Android app. */
fun String?.isSmbScheme(): Boolean =
    equals("smb", ignoreCase = true) || equals("smb2", ignoreCase = true) || equals("cifs", ignoreCase = true)
