rootProject.name = "detekt-intellij-plugin"

// build scan plugin can only be applied in settings file
plugins {
    id("com.gradle.develocity") version "4.3"
    id("com.gradle.common-custom-user-data-gradle-plugin") version "2.4.0"
}

develocity {
    val isCiBuild = System.getenv("CI") != null
    buildScan {
        publishing {
            onlyIf {
                // Publish to scans.gradle.com when `--scan` is used explicitly
                if (!gradle.startParameter.isBuildScan) {
                    it.isAuthenticated
                } else {
                    true
                }
            }
        }

        if (!gradle.startParameter.isBuildScan) {
            server = "https://ge.detekt.dev"
        }

        uploadInBackground = !isCiBuild
    }
}
