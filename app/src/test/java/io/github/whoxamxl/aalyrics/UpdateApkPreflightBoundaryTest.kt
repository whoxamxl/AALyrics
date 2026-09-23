package io.github.whoxamxl.aalyrics

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class UpdateApkPreflightBoundaryTest {
    private val root = createTempDirectory("aalyrics-install-preflight").toFile()
    private val stagingRoot = root.resolve("cache/updates")
    private val verifiedRoot = root.resolve("no-backup/updates")

    @AfterTest
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun `canonical retained APK delegates to package inspection`() {
        val retained = retainedApk()
        var inspectionCount = 0
        val boundary = boundary(
            inspector = UpdateApkPackageInspector {
                inspectionCount += 1
                validInspection()
            },
        )

        assertEquals(
            UpdateApkPreflightResult.Ready,
            boundary.evaluate(
                retainedApk = retained,
                expectedVersionName = "0.2.0-alpha.2",
            ),
        )
        assertEquals(1, inspectionCount)
    }

    @Test
    fun `missing retained APK fails before package inspection`() {
        var inspectionCount = 0
        val boundary = boundary(
            inspector = UpdateApkPackageInspector {
                inspectionCount += 1
                validInspection()
            },
        )

        assertEquals(
            UpdateApkPreflightResult.Rejected(
                UpdateApkPreflightRejection.FILE_MISSING,
            ),
            boundary.evaluate(
                retainedApk = verifiedRoot.resolve("AALyrics-v0.2.0-alpha.2.apk"),
                expectedVersionName = "0.2.0-alpha.2",
            ),
        )
        assertEquals(0, inspectionCount)
    }

    @Test
    fun `non canonical file fails before package inspection`() {
        retainedApk()
        val outside = root.resolve("outside.apk").apply {
            writeText("other")
        }
        var inspectionCount = 0
        val boundary = boundary(
            inspector = UpdateApkPackageInspector {
                inspectionCount += 1
                validInspection()
            },
        )

        assertEquals(
            UpdateApkPreflightResult.Rejected(
                UpdateApkPreflightRejection.NOT_CANONICAL_RETAINED_ARTIFACT,
            ),
            boundary.evaluate(
                retainedApk = outside,
                expectedVersionName = "0.2.0-alpha.2",
            ),
        )
        assertEquals(0, inspectionCount)
    }

    @Test
    fun `unavailable package inspection fails closed`() {
        val retained = retainedApk()
        val boundary = boundary(
            inspector = UpdateApkPackageInspector { null },
        )

        assertEquals(
            UpdateApkPreflightResult.Rejected(
                UpdateApkPreflightRejection.ARCHIVE_UNREADABLE,
            ),
            boundary.evaluate(
                retainedApk = retained,
                expectedVersionName = "0.2.0-alpha.2",
            ),
        )
    }

    private fun retainedApk(): File {
        verifiedRoot.mkdirs()
        return verifiedRoot.resolve("AALyrics-v0.2.0-alpha.2.apk").apply {
            writeText("verified")
        }
    }

    private fun boundary(
        inspector: UpdateApkPackageInspector,
    ) = UpdateApkPreflightBoundary(
        fileStore = UpdateDownloadFileStore(
            stagingDirectory = stagingRoot,
            verifiedDirectory = verifiedRoot,
        ),
        packageInspector = inspector,
    )

    private fun validInspection() = UpdateApkPackageInspection(
        archivePackageName = "io.github.whoxamxl.aalyrics",
        archiveVersionCode = 41L,
        archiveVersionName = "0.2.0-alpha.2",
        installedPackageName = "io.github.whoxamxl.aalyrics",
        installedVersionCode = 40L,
        signingIdentityCompatible = true,
    )
}
