### Changelog

#### 1.0.0-RC1

- Close ProcessingSettings after usage - [#61](https://github.com/detekt/detekt-intellij-plugin/pull/61)
- Analyze cached PsiFile instead of old version on disk - [#59](https://github.com/detekt/detekt-intellij-plugin/pull/59)
- Warning ranges not consistent with editor content - [#57](https://github.com/detekt/detekt-intellij-plugin/issues/57)

See all issues at: [1.0.0-RC1](https://github.com/detekt/detekt-intellij-plugin/milestone/4)

#### 0.4.1

##### Notable Changes

- Set JVM target version to Java 8

#### 0.4.0

##### Notable Changes

- The plugin now does not auto correct issues as this conflicts with idea and other plugins.
Use the new `AutoCorrect by Detekt Rules` action for this.
This change also fixes inconvenient errors in the event log when debugging or commiting.
- Custom rule sets can be applied using the new `pluginPaths` config option.

##### Changelog

- Remove running 'AutoCorrect' automatically on background and assign it to IDEA action - [#55](https://github.com/detekt/detekt-intellij-plugin/pull/55)
- Add pluginPaths option (#43) - [#54](https://github.com/detekt/detekt-intellij-plugin/pull/54)
- Run autocorrect only when save is trigerred - [#53](https://github.com/detekt/detekt-intellij-plugin/issues/53)
- Set current project directory for the "Baseline File" and "Configuration File" browse file dialog window - [#51](https://github.com/detekt/detekt-intellij-plugin/pull/51)
- Save Actions does not work when detekt plugin is also enabled - [#50](https://github.com/detekt/detekt-intellij-plugin/issues/50)
- Custom rules integration to IntelliJ plugin? - [#43](https://github.com/detekt/detekt-intellij-plugin/issues/43)
- java.lang.IllegalArgumentException: Given path /fragment.kt does not exist! - [#23](https://github.com/detekt/detekt-intellij-plugin/issues/23)
- Must not start write action from within read action in the other thread - deadlock is coming - [#21](https://github.com/detekt/detekt-intellij-plugin/issues/21)

See all issues at: [0.4.0](https://github.com/detekt/detekt-intellij-plugin/milestone/3)

#### 0.3.2

- Add baseline option - [#48](https://github.com/arturbosch/detekt-intellij-plugin/pull/48)
- Put detekt plugin config in a separate file to ease sharing - [#47](https://github.com/arturbosch/detekt-intellij-plugin/pull/47)
- Handle relative path configuration file - [#46](https://github.com/arturbosch/detekt-intellij-plugin/pull/46)
- Show notification if provided config is invalid - [#45](https://github.com/arturbosch/detekt-intellij-plugin/pull/45)
- Add autocorrect option - [#44](https://github.com/arturbosch/detekt-intellij-plugin/pull/44)
- [IntelliJ plugin] Platform error if configuration file is removed - [#42](https://github.com/arturbosch/detekt-intellij-plugin/issues/42)
- IDEA plugin doesn't handle relative config file path - [#29](https://github.com/arturbosch/detekt-intellij-plugin/issues/29)
- no possibility to specify the baseline.xml - [#24](https://github.com/arturbosch/detekt-intellij-plugin/issues/24)
- Uncaught exception  in the intellij plugin when switching to a branch that does not have the config.yml - [#22](https://github.com/arturbosch/detekt-intellij-plugin/issues/22)
- Feature request: shared configuration - [#13](https://github.com/arturbosch/detekt-intellij-plugin/issues/13)

See all issues at: [0.3.2](https://github.com/arturbosch/detekt-intellij-plugin/milestone/2)

#### 0.3.1

- Upgrade detekt engine to 1.0.0
- Gradle Plugin: `autoCorrect` property is now allowed on the detekt extension. No need to create a new task anymore.
- Formatting: updated to KtLint 0.34.2 which removed the two rules `NoItParamInMultilineLambda` and `SpacingAroundUnaryOperators`. 

#### 0.3.0

- Upgrade detekt engine to 1.0.0-RC16
- Integrate fail fast config option to enable all detekt rules regardless of the configured `active` properties.
- Allow to reuse the default detekt configuration as baseline (checkbox)
- Remove `analyzeTestCode` in favor of config based `excludes` on detekt RC15+ 

#### 0.2.2

- Upgrade detekt engine to 1.0.0-RC14

#### 0.2.1

- Upgrade detekt engine to 1.0.0-RC13

#### 0.2.0

- Uses detekt 1.0.0-RC10 as engine baseline
- Support `detekt-formatting` (`enableFormatting` in settings)
- Force save for accurate issue reporting

#### 0.1.2

- Indicate warning name in error message - [#6](https://github.com/arturbosch/detekt-intellij-plugin/issues/6)
- Publish plugin to JetBrains repository - [#5](https://github.com/arturbosch/detekt-intellij-plugin/issues/5)
- Add ability to change severity of the inspections - [#2](https://github.com/arturbosch/detekt-intellij-plugin/issues/2)

See all issues at: [0.1.2](https://github.com/arturbosch/detekt-intellij-plugin/milestone/1)
