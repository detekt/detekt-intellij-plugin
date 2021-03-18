# Releasing

1. Run `./gradlew build`
2. Run `./gradlew buildPlugin`
3. Run `./gradlew publishPlugin` (Required system environment variable ORG_GRADLE_PROJECT_intellijPublishToken)
4. Run `./gradlew githubRelease` (Required gradle property `github.token`)