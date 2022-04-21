# Releasing

- Add a new entry in changelog.md as well as plugin.xml
- Run `./gradlew build`
- Run `./gradlew buildPlugin`
- Run `./gradlew publishPlugin` (Required system environment variable ORG_GRADLE_PROJECT_intellijPublishToken)
- Visit https://plugins.jetbrains.com/plugin/10761-detekt to verify the plugin is approved and released
- Run `./gradlew githubRelease` (Required gradle property `github.token`)
- Visit https://github.com/detekt/detekt-intellij-plugin to verify the release is created on Github