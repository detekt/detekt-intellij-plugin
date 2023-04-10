# IntelliJ Detekt Plugin

[![Join the chat at https://kotlinlang.slack.com/messages/C88E12QH4/convo/C0BQ5GZ0S-1511956674.000289/](https://img.shields.io/badge/chat-on_slack-red.svg?style=flat-square)](https://kotlinlang.slack.com/messages/C88E12QH4/convo/C0BQ5GZ0S-1511956674.000289/)
![Pre Merge Checks](https://github.com/detekt/detekt-intellij-plugin/workflows/Pre%20Merge%20Checks/badge.svg)
[![JetBrains Plugins](https://img.shields.io/jetbrains/plugin/v/10761-detekt.svg)](https://plugins.jetbrains.com/plugin/10761-detekt)
[![FOSSA Status](https://app.fossa.io/api/projects/git%2Bgithub.com%2Farturbosch%2Fdetekt-intellij-plugin.svg?type=small)](https://app.fossa.io/projects/git%2Bgithub.com%2Farturbosch%2Fdetekt-intellij-plugin?ref=badge_large)
[![License](https://img.shields.io/github/license/detekt/detekt-intellij-plugin.svg)](LICENSE)

Integrates _detekt_, a static code analysis tool for the Kotlin programming language, into IntelliJ.

![detekt in action](./img/detekt.png "detekt in action")

The plugin can be downloaded from the [Jetbrains plugin repository](https://plugins.jetbrains.com/plugin/10761-detekt).

## Enabling the plugin

- `Settings -> Plugins -> Marketplace -> Search for detekt -> Install`
- Configure the plugin via `Settings -> Tools -> detekt`

## Configuration Options

![configuration](./img/configuration.png "configuration")

That's it. Detekt issues will be annotated on-the-fly while coding.

## Autocorrection

You may optionally click `Refactor` -> `AutoCorrect by detekt rules` to auto correct detekt violations if possible.

## Building / developing the plugin

For building the project, the [Gradle IntelliJ plugin](https://github.com/JetBrains/gradle-intellij-plugin)
is used.

```bash
# linux & macOS
./gradlew buildPlugin
# windows
gradlew buildPlugin
```

To test your development, use task `runIde` which will automatically run an Intellij instance to test your new version of detekt plugin.
```bash
# linux & macOS
./gradlew runIde
# windows
gradlew runIde
```

Also install the current `Detekt IntelliJ Plugin` version  to verify you do not introduce new issues.
