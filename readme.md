# IntelliJ Detekt Plugin

[![Join the chat at https://kotlinlang.slack.com/messages/C88E12QH4/convo/C0BQ5GZ0S-1511956674.000289/](https://img.shields.io/badge/chat-on_slack-red.svg?style=flat-square)](https://kotlinlang.slack.com/messages/C88E12QH4/convo/C0BQ5GZ0S-1511956674.000289/)
[![build status](https://travis-ci.org/arturbosch/detekt-intellij-plugin.svg?branch=master)](https://travis-ci.org/arturbosch/detekt-intellij-plugin)
[![build status windows](https://ci.appveyor.com/api/projects/status/5pfg19cd9qxhsj02/branch/master?svg=true)](https://ci.appveyor.com/project/arturbosch/detekt-intellij-plugin)

Integrates _detekt_, a static code analysis tool for the Kotlin programming language, into IntelliJ.

The plugin can be downloaded from the [Jetbrains plugin repository](https://plugins.jetbrains.com/plugin/10761-detekt-intellij-plugin).

## Build plugin

For building the project, the [Gradle IntelliJ plugin](https://github.com/JetBrains/gradle-intellij-plugin)
is used.

```bash
# linux & macOS
./gradlew buildPlugin
# windows
gradlew buildPlugin
```
