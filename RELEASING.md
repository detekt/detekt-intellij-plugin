# Releasing

- Add a new entry in changelog.md as well as plugin.xml
- Manually verify the plugin through `./gradlew runIde`
- Run `./gradlew build`
- Run `./gradlew publishPlugin` (Required system environment variable ORG_GRADLE_PROJECT_intellijPublishToken)
- Visit [Marketplace](https://plugins.jetbrains.com/plugin/10761-detekt) to verify the plugin is approved and released
- Run `./gradlew githubRelease` (Required gradle property `github.token`)
- Visit [Github](https://github.com/detekt/detekt-intellij-plugin) to verify the release is created on Github
