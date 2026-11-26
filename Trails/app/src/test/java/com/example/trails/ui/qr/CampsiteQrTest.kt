// AI assistance: used ai to understand and modify lines of codes to tailor to my requirements
// OWNER: Neaz (NeazMZ06) — QR codes
// USER STORIES: 02.02.01, 01.12.01
// PURPOSE: Unit tests for what a campsite QR code holds, how a scan is read back, and that
//   the drawn code can really be scanned.
// Outstanding issues: none.
package com.example.trails.ui.qr

import com.example.trails.data.fake.TestCampsites
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Checks [campsiteQrText], [campsiteIdFromQr], [qrScanOutcome], [qrMatrix] and [qrFileName].
 */
class CampsiteQrTest {

    @Test
    fun qrText_isTheTrailsLinkForTheCampsite() {
        assertEquals("trails://campsite/campsite-banff-tunnel", campsiteQrText(TestCampsites.BANFF_ID))
    }

    @Test
    fun qrText_thenRead_givesTheSameIdBack() {
        TestCampsites.all.forEach { campsite ->
            assertEquals(campsite.id, campsiteIdFromQr(campsiteQrText(campsite.id)))
        }
    }

    @Test
    fun differentCampsites_getDifferentCodes() {
        val codes = TestCampsites.all.map { campsiteQrText(it.id) }
        assertEquals(codes.size, codes.toSet().size)
        assertNotEquals(campsiteQrText(TestCampsites.BANFF_ID), campsiteQrText(TestCampsites.JASPER_ID))
    }

    @Test(expected = IllegalArgumentException::class)
    fun qrText_forBlankId_isRejected() {
        campsiteQrText("  ")
    }

    @Test
    fun read_ignoresSpacesCaseAndExtras() {
        assertEquals("abc123", campsiteIdFromQr("  trails://campsite/abc123  "))
        assertEquals("abc123", campsiteIdFromQr("TRAILS://CAMPSITE/abc123"))
        assertEquals("abc123", campsiteIdFromQr("trails://campsite/abc123/"))
        assertEquals("abc123", campsiteIdFromQr("trails://campsite/abc123?from=poster"))
        assertEquals("abc123", campsiteIdFromQr("trails://campsite/abc123#top"))
    }

    @Test
    fun read_rejectsCodesThatAreNotTrailsCampsites() {
        assertNull(campsiteIdFromQr(null))
        assertNull(campsiteIdFromQr(""))
        assertNull(campsiteIdFromQr("https://example.com/campsite/abc123"))
        assertNull(campsiteIdFromQr("trails://lottery/abc123"))
        assertNull(campsiteIdFromQr("trails://campsite/"))
        assertNull(campsiteIdFromQr("trails://campsite/a/b"))
        assertNull(campsiteIdFromQr("trails://campsite/a b"))
        assertNull(campsiteIdFromQr("hello world"))
    }

    @Test
    fun scanOutcome_coversCancelledWrongCodeAndCampsite() {
        assertEquals(QrScanOutcome.Cancelled, qrScanOutcome(null))
        assertEquals(QrScanOutcome.NotTrails, qrScanOutcome("https://example.com"))
        assertEquals(
            QrScanOutcome.Campsite(TestCampsites.HINTON_ID),
            qrScanOutcome(campsiteQrText(TestCampsites.HINTON_ID))
        )
    }

    @Test
    fun matrix_isTheRequestedSize() {
        val matrix = qrMatrix(campsiteQrText(TestCampsites.BANFF_ID), 300)
        assertEquals(300, matrix.width)
        assertEquals(300, matrix.height)
    }

    @Test
    fun drawnCode_scansBackToTheSameLink() {
        TestCampsites.all.forEach { campsite ->
            val text = campsiteQrText(campsite.id)
            assertEquals(text, decode(text))
        }
    }

    @Test
    fun fileName_isSafeAndNamesTheCampsite() {
        assertEquals("trails-qr-campsite-banff-tunnel.png", qrFileName(TestCampsites.BANFF_ID))
        assertEquals("trails-qr-a_b_c.png", qrFileName("a/b c"))
    }

    /** Draws [text] as a QR code and reads it back the way a scanner would. */
    private fun decode(text: String): String {
        val matrix = qrMatrix(text, 300)
        val w = matrix.width
        val pixels = IntArray(w * matrix.height) { i ->
            if (matrix[i % w, i / w]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }
        val source = RGBLuminanceSource(w, matrix.height, pixels)
        return QRCodeReader().decode(BinaryBitmap(HybridBinarizer(source))).text
    }
}
