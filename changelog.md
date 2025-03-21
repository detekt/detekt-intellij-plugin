### Changelog

#### 2.4.3

- Based on detekt 1.23.8
- Ignore AI snippets - #528

#### 2.4.2

- Based on detekt 1.23.7

#### 2.4.1

- Based on detekt 1.23.6
- Fix error notifications showing detekt does not override update thread function - [#513](https://github.com/detekt/detekt-intellij-plugin/issues/513)

#### 2.4.0

- Based on detekt 1.23.6
- Add support for IntelliJ 2024.1
- Drop support for IntelliJ 2022.1

#### 2.3.0

- Based on detekt 1.23.4
- Workaround transitive logging classloader problem by constraining the dependency to a newer fixed version - [#501](https://github.com/detekt/detekt-intellij-plugin/issues/501)

#### 2.2.0

- Based on detekt 1.23.3

#### 2.1.0

- Based on detekt 1.23.1
- Make detekt plugin opt-in per-project - [#490](https://github.com/detekt/detekt-intellij-plugin/issues/490)

#### 2.0.1

- Based on detekt 1.23.1
- Plugin intentions/quick fixes have now lower priority - [#487](https://github.com/detekt/detekt-intellij-plugin/issues/487)

#### 2.0.0

- Based on detekt 1.23.0
- Support file and directory analysis from project explorer, editor and tool window - [#469](https://github.com/detekt/detekt-intellij-plugin/pull/469)
- `Enable detekt` was renamed to `Enable background analysis` to make it clearer that it activates the on-the-fly analysis while coding - [#469](https://github.com/detekt/detekt-intellij-plugin/pull/469)
- Introduce `detekt doc` tool window for quick access to detekt's documentation.
  - Introduce `Show detekt Rules Documentation` action to open tool window with style rules set pre-selected
  - Introduce `Show detek Changelog` action to open tool window with the current changelog.
- Allow to redirect detekt's output to IntelliJ log file.
- Drop support for Android Studio 2021.3. Android Studio 2022.1.1 Patch 2 is the new minimal supported IntelliJ Platform version.
- Minimal supported IntelliJ version is now 2022.1.4.
- Drop support for settings migration from version < 1.21.1. Make sure to update from 1.21.2+ or be prepared to renew your settings - [#469](https://github.com/detekt/detekt-intellij-plugin/pull/469)
- Drop naming scheme based on included detekt version - [#469](https://github.com/detekt/detekt-intellij-plugin/pull/469)
  - This makes sure the IntelliJ plugin can make feature release with proper semantic versioning.
  - The included detekt version will always appear in the current changelog entry for reference.

#### 1.22.2

- Support ordered configuration files to be able to override rules deterministically - [#385](https://github.com/detekt/detekt-intellij-plugin/issues/385)
- Make paths sortable, add migration - [#460](https://github.com/detekt/detekt-intellij-plugin/pull/460)

#### 1.22.1

- Deactivate error reporting for now due to unsolved unrelated reporting of Kotlin issues - [#271](https://github.com/detekt/detekt-intellij-plugin/issues/271)

#### 1.22.0

- Update to detekt 1.22.0
- Fix logging process cancellation exceptions - [#311](https://github.com/detekt/detekt-intellij-plugin/issues/311)
- Allow configuration files with yaml extension - [#300](https://github.com/detekt/detekt-intellij-plugin/issues/300)

#### 1.21.3

Please do not report issues anymore where IntelliJ/Android Studio blames the detekt plugin for an exception 
without any detekt stacktrace. This is a known issue and will take some time to investigate - [#271](https://github.com/detekt/detekt-intellij-plugin/issues/271)

- Fix a situation where it could happen that the config or plugin jar paths got corrupt and reference the containing directory - [#272](https://github.com/detekt/detekt-intellij-plugin/pull/272)

#### 1.21.2

- Do not allow autocorrect action on read-only files - [#268](https://github.com/detekt/detekt-intellij-plugin/pull/268)
- Introduce some debug logging to find out if stack traces without detekt package are thrown by our plugin - [#268](https://github.com/detekt/detekt-intellij-plugin/pull/268)

#### 1.21.1

- Settings UI overhaul - [#240](https://github.com/detekt/detekt-intellij-plugin/pull/240)
- File paths should be platform-independent - [#231](https://github.com/detekt/detekt-intellij-plugin/issues/231)
- Store configuration file path as relative to the project root - [#135](https://github.com/detekt/detekt-intellij-plugin/issues/135)
- Multiple configuration files are not supported - [#117](https://github.com/detekt/detekt-intellij-plugin/issues/117)

#### 1.21.0

- Update to detekt 1.21.0 - [#155](https://github.com/detekt/detekt-intellij-plugin/pull/175)
- IntelliJ Detekt plugin should be enabled by default - [115](https://github.com/detekt/detekt-intellij-plugin/issues/115)
- Introduce file level annotations to not mark the whole file - [173](https://github.com/detekt/detekt-intellij-plugin/issues/173)
- Introduce an autoCorrect action - [142](https://github.com/detekt/detekt-intellij-plugin/issues/142)
- Fix some errors when reading or writing source code - [159](https://github.com/detekt/detekt-intellij-plugin/issues/159)
- More finding position fixes from detekt core
- Using a yaml configuration file generated by a detekt version higher than the IntelliJ plugin version should not throw an InvalidConfigurationError anymore

#### 1.20.1

- Fix crash when formatting rule set was used - [#164](https://github.com/detekt/detekt-intellij-plugin/pull/164)
- Support for reporting plugin errors - [#162](https://github.com/detekt/detekt-intellij-plugin/pull/162)
- Allow to enable detekt through the action menu via a toggle action - [#166](https://github.com/detekt/detekt-intellij-plugin/pull/166)

#### 1.20.0
- Update to detekt 1.20.0 - [#155](https://github.com/detekt/detekt-intellij-plugin/pull/155)
- Update to Gradle 7.4.2 - [#155](https://github.com/detekt/detekt-intellij-plugin/pull/155)
- Update Gradle plugins - [#155](https://github.com/detekt/detekt-intellij-plugin/pull/155)
- Update dependencies - [#155](https://github.com/detekt/detekt-intellij-plugin/pull/155)

#### 1.19.0
- Update to detekt 1.19.0 - [#144](https://github.com/detekt/detekt-intellij-plugin/pull/144)
- Update to Gradle 7.3.1 - [#144](https://github.com/detekt/detekt-intellij-plugin/pull/144)
- Update Gradle plugins - [#144](https://github.com/detekt/detekt-intellij-plugin/pull/144)
- Fixed warnings in plugin.xml - [#144](https://github.com/detekt/detekt-intellij-plugin/pull/144)
- Migrating to the [version catalog](https://docs.gradle.org/current/userguide/platforms.html#sec:sharing-catalogs) - [#144](https://github.com/detekt/detekt-intellij-plugin/pull/144)
- Added plugin icon ([source](https://github.com/detekt/detekt/blob/main/media/icon.svg)) - [#144](https://github.com/detekt/detekt-intellij-plugin/pull/144)

#### 1.18.1

- Update to detekt 1.18.1 - [#133](https://github.com/detekt/detekt-intellij-plugin/pull/133)
- Update to Gradle 7.2 - [#133](https://github.com/detekt/detekt-intellij-plugin/pull/133)
- Update to IntelliJ 2021.2.1 - [#133](https://github.com/detekt/detekt-intellij-plugin/pull/133)
- Update Gradle plugins - [#133](https://github.com/detekt/detekt-intellij-plugin/pull/133)
- Update to JVM 11

#### 1.17.1

- Update to detekt 1.17.1
- Update to Gradle 7.0.2
- Update to IntelliJ 2021.1

#### 1.16.0
- Upgrade to detekt 1.16.0

#### 1.15.0
- Update to detekt 1.15.0
- Update to Gradle 6.8
- Update to IntelliJ 2020.3
- Align the major and minor version with detekt to follow semantic versions

#### 1.6.1

- Update to detekt 1.14.2

#### 1.6.0

- Update to detekt 1.14.1

#### 1.5.0

- Update to detekt 1.11.0

#### 1.4.0

##### Changelog

- Update to detekt 1.11.0-RC2 - [#94](https://github.com/detekt/detekt-intellij-plugin/pull/94)

See all issues at: [1.4.0](https://github.com/detekt/detekt-intellij-plugin/milestone/10)

#### 1.3.0

##### Changelog

- Update to detekt 1.10.0 - [#93](https://github.com/detekt/detekt-intellij-plugin/pull/93)

See all issues at: [1.3.0](https://github.com/detekt/detekt-intellij-plugin/milestone/9)

#### 1.2.0

##### Changelog

- Update intellij plugin - [#91](https://github.com/detekt/detekt-intellij-plugin/pull/91)
- Update to detekt 1.9.1 - [#90](https://github.com/detekt/detekt-intellij-plugin/pull/90)
- Make compatible with Android Studio 3.6.3 - [#88](https://github.com/detekt/detekt-intellij-plugin/pull/88)
- Not compatible with Android Studio since version 1.0.0 - [#87](https://github.com/detekt/detekt-intellij-plugin/issues/87)
- TooManyFunctions - Plugin highlights whole file - [#83](https://github.com/detekt/detekt-intellij-plugin/issues/83)

See all issues at: [1.2.0](https://github.com/detekt/detekt-intellij-plugin/milestone/8)

#### 1.1.0

- Update to detekt 1.8.0 - [#84](https://github.com/detekt/detekt-intellij-plugin/pull/84)
- Plugin highlights whole object - [#81](https://github.com/detekt/detekt-intellij-plugin/issues/81)

See all issues at: [1.1.0](https://github.com/detekt/detekt-intellij-plugin/milestone/7)

#### 1.0.0

- Remove println statements - [#76](https://github.com/detekt/detekt-intellij-plugin/pull/76)
- Add testcases for different plugin config options - [#75](https://github.com/detekt/detekt-intellij-plugin/pull/75)
- Simplify internal detekt usage for 1.0 - [#74](https://github.com/detekt/detekt-intellij-plugin/pull/74)
- Make plugin fit for dynamic plugin requirements - [#73](https://github.com/detekt/detekt-intellij-plugin/pull/73)
- Use Github actions for CI - [#72](https://github.com/detekt/detekt-intellij-plugin/pull/72)
- Update to detekt 1.7.4 - [#71](https://github.com/detekt/detekt-intellij-plugin/pull/71)
- Allow configuration files with 'yaml' extension - [#69](https://github.com/detekt/detekt-intellij-plugin/issues/69)

See all issues at: [1.0.0](https://github.com/detekt/detekt-intellij-plugin/milestone/6)

#### 1.0.0-RC2

- I can`t open the settings after update to 1.0.0-RC1 - [#67](https://github.com/detekt/detekt-intellij-plugin/issues/67)
- Fix path filters due to missing directories of the analyzed file - [#65](https://github.com/detekt/detekt-intellij-plugin/pull/65)
- Make sure that java classes are also compiled to 1.8 bytecode - #62 - [#64](https://github.com/detekt/detekt-intellij-plugin/pull/64)
- Difference in how exclude paths are evaluated after upgrade to 1.0.0-RC1 - [#63](https://github.com/detekt/detekt-intellij-plugin/issues/63)
- Plugin not working in Android Studio 3.6 - [#62](https://github.com/detekt/detekt-intellij-plugin/issues/62)

See all issues at: [1.0.0-RC2](https://github.com/detekt/detekt-intellij-plugin/milestone/5)

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
