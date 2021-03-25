# Releasing

1. Run `./gradlew build`
2. Run `./gradlew buildPlugin`
3. Run `./gradlew publishPlugin` (Required system environment variable ORG_GRADLE_PROJECT_intellijPublishToken)
4. Visit https://plugins.jetbrains.com/plugin/10761-detekt to verify the plugin is approved and released
5. Run `./gradlew githubRelease` (Required gradle property `github.token`)
6. Visit https://github.com/detekt/detekt-intellij-plugin to verify the release is created on Github
7. Add a new entry in changelog.md