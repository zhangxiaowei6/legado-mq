package io.legado.app

import io.legado.app.exception.NoStackTraceException
import io.legado.app.help.update.GithubAsset
import io.legado.app.help.update.GithubRelease
import io.legado.app.help.update.SemanticVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateTest {

    @Test
    fun semanticVersionOrdersPatchUpdates() {
        assertTrue(SemanticVersion.parse("1.0.1")!! > SemanticVersion.parse("1.0.0")!!)
    }

    @Test
    fun semanticVersionOrdersTwoDigitMinorUpdates() {
        assertTrue(SemanticVersion.parse("1.10.0")!! > SemanticVersion.parse("1.9.9")!!)
    }

    @Test
    fun invalidTagsAreRejected() {
        listOf("1.0.0", "v1.0", "v01.0.0", "v1.100.0", "beta").forEach {
            assertNull(SemanticVersion.parseTag(it))
        }
    }

    @Test
    fun validStableReleaseReturnsExactApk() {
        val info = release().toUpdateInfo("0.9.9")
        assertEquals("v1.0.0", info?.tagName)
        assertEquals("moqi-reader-v1.0.0.apk", info?.fileName)
    }

    @Test
    fun currentOrOlderReleaseIsIgnored() {
        assertNull(release().toUpdateInfo("1.0.0"))
        assertNull(release().toUpdateInfo("1.1.0"))
    }

    @Test
    fun draftAndPrereleaseAreIgnored() {
        assertNull(release(draft = true).toUpdateInfo("0.9.9"))
        assertNull(release(preRelease = true).toUpdateInfo("0.9.9"))
    }

    @Test
    fun missingOrWrongMimeApkIsRejected() {
        assertThrows(NoStackTraceException::class.java) {
            release(assets = emptyList()).toUpdateInfo("0.9.9")
        }
        assertThrows(NoStackTraceException::class.java) {
            release(assets = listOf(asset(contentType = "application/octet-stream")))
                .toUpdateInfo("0.9.9")
        }
    }

    private fun release(
        draft: Boolean = false,
        preRelease: Boolean = false,
        assets: List<GithubAsset> = listOf(asset()),
    ) = GithubRelease(
        tagName = "v1.0.0",
        name = "Moqi Reader v1.0.0",
        assets = assets,
        body = "Changes",
        draft = draft,
        preRelease = preRelease,
    )

    private fun asset(
        contentType: String = GithubRelease.ANDROID_APK_MIME,
    ) = GithubAsset(
        downloadUrl = "https://example.invalid/moqi-reader-v1.0.0.apk",
        contentType = contentType,
        name = "moqi-reader-v1.0.0.apk",
        state = "uploaded",
    )
}
