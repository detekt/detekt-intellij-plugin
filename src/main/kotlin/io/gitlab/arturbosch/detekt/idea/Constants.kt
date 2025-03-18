package io.gitlab.arturbosch.detekt.idea

import com.intellij.openapi.util.Key

const val DETEKT = "detekt"
const val NOTIFICATION_GROUP_ID = DETEKT
const val FORMATTING_RULE_SET_ID = "formatting"

const val SPECIAL_FILENAME_FOR_DEBUGGING = "/fragment.kt"
const val SPECIAL_FILENAME_AI_SNIPPED = "/AIAssistantSnippet."

val SPECIAL_FILES_TO_IGNORE = listOf(SPECIAL_FILENAME_FOR_DEBUGGING, SPECIAL_FILENAME_AI_SNIPPED)

val KOTLIN_FILE_EXTENSIONS = setOf("kt", "kts")

// Note this is a hack to allow tests to not use KotlinLanguage class due to classloading problems with IJ and detekt.
val TEST_KOTLIN_LANGUAGE_ID_KEY = Key.create<String>("__tests_only_kotlin_id__")
