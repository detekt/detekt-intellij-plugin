import com.gradle.enterprise.gradleplugin.internal.extension.BuildScanExtensionWithHiddenFeatures

rootProject.name = "detekt-intellij-plugin"

// build scan plugin can only be applied in settings file
plugins {
    `gradle-enterprise`
    id("com.gradle.common-custom-user-data-gradle-plugin") version "1.6.5"
}

gradleEnterprise {
    val isCiBuild = System.getenv("CI") != null

    buildScan {
        publishAlways()

        // Publish to scans.gradle.com when `--scan` is used explicitly
        if (!gradle.startParameter.isBuildScan) {
            server = "https://ge.detekt.dev"
            this as BuildScanExtensionWithHiddenFeatures
            publishIfAuthenticated()
        }

        isUploadInBackground = !isCiBuild

        capture {
            isTaskInputFiles = true
        }
    }
}
