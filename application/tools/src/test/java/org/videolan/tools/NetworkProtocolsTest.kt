package org.videolan.tools

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkProtocolsTest {
    @Test fun rejectsAllNativeSmbAliasesRegardlessOfCase() {
        for (scheme in listOf("smb", "smb2", "cifs", "SMB", "SmB2", "CIFS")) {
            assertTrue(scheme, scheme.isSmbScheme())
        }
    }

    @Test fun keepsOtherNetworkAndLocalProtocolsAvailable() {
        for (scheme in listOf("upnp", "UPNP", "http", "https", "ftp", "ftps", "ftpes", "sftp", "ssh", "nfs", "file", "content", "fd", "rtsp")) {
            assertFalse(scheme, scheme.isSmbScheme())
        }
    }

    @Test fun doesNotBlockLocalPathsOrUnrelatedProtocolNames() {
        for (scheme in listOf(null, "", "smb-backup", "cifs-extra")) {
            assertFalse(scheme, scheme.isSmbScheme())
        }
    }
}
