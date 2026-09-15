package io.legado.app.help.update

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import io.legado.app.exception.NoStackTraceException

data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
) : Comparable<SemanticVersion> {

    override fun compareTo(other: SemanticVersion): Int =
        compareValuesBy(this, other, SemanticVersion::major, SemanticVersion::minor, SemanticVersion::patch)

    companion object {
        private val VERSION_PATTERN = Regex("^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)$")
        private val TAG_PATTERN = Regex("^v(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)$")

        fun parse(value: String): SemanticVersion? = parse(value, VERSION_PATTERN)

        fun parseTag(value: String): SemanticVersion? = parse(value, TAG_PATTERN)

        private fun parse(value: String, pattern: Regex): SemanticVersion? {
            val match = pattern.matchEntire(value) ?: return null
            val parts = match.groupValues.takeLast(3).map { it.toIntOrNull() ?: return null }
            if (parts[1] !in 0..99 || parts[2] !in 0..99) return null
            return SemanticVersion(parts[0], parts[1], parts[2])
        }
    }
}

@Keep
data class GithubRelease(
    @SerializedName("tag_name")
    val tagName: String,
    val name: String?,
    val assets: List<GithubAsset>?,
    val body: String?,
    val draft: Boolean,
    @SerializedName("prerelease")
    val preRelease: Boolean,
) {
    fun toUpdateInfo(currentVersionName: String): AppUpdate.UpdateInfo? {
        if (draft || preRelease) return null
        val remoteVersion = SemanticVersion.parseTag(tagName)
            ?: throw NoStackTraceException("发行标签格式无效")
        val currentVersion = SemanticVersion.parse(currentVersionName)
            ?: throw NoStackTraceException("当前版本格式无效")
        if (remoteVersion <= currentVersion) return null

        val expectedName = "moqi-reader-$tagName.apk"
        val apk = assets.orEmpty().singleOrNull {
            it.name == expectedName &&
                it.state == "uploaded" &&
                it.contentType == ANDROID_APK_MIME
        } ?: throw NoStackTraceException("发行版缺少有效安装包 $expectedName")

        return AppUpdate.UpdateInfo(
            tagName = tagName,
            updateLog = body.orEmpty(),
            downloadUrl = apk.downloadUrl,
            fileName = apk.name,
        )
    }

    companion object {
        const val ANDROID_APK_MIME = "application/vnd.android.package-archive"
    }
}

@Keep
data class GithubAsset(
    @SerializedName("browser_download_url")
    val downloadUrl: String,
    @SerializedName("content_type")
    val contentType: String,
    val name: String,
    val state: String,
)
