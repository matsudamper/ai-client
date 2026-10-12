package net.matsudamper.gptclient.localmodel

import java.io.File

internal object LiteRtModelOptionsStore {
    fun read(modelFile: File): LiteRtModelOptions? {
        val optionsFile = optionsFile(modelFile)
        if (!optionsFile.isFile) {
            return null
        }
        return parse(optionsFile.readText())
    }

    fun write(modelFile: File, options: LiteRtModelOptions) {
        optionsFile(modelFile).writeText(format(options))
    }

    fun delete(modelFile: File) {
        optionsFile(modelFile).delete()
    }

    fun readOrLoad(modelFile: File): LiteRtModelOptions {
        val saved = read(modelFile)
        if (saved != null) {
            return saved
        }
        val options = LiteRtModelOptionsReader.readOrNull(modelFile) ?: return LiteRtModelOptions(
            supportsThinking = false,
            supportsVision = false,
        )
        write(modelFile, options)
        return options
    }

    private fun optionsFile(modelFile: File): File =
        File(modelFile.parentFile, "${modelFile.name}.options")

    private fun format(options: LiteRtModelOptions): String =
        "supportsThinking=${options.supportsThinking}\nsupportsVision=${options.supportsVision}\n"

    private fun parse(text: String): LiteRtModelOptions? {
        val values = text.lineSequence()
            .mapNotNull { line ->
                val separatorIndex = line.indexOf('=').takeIf { it >= 0 } ?: return@mapNotNull null
                line.substring(0, separatorIndex) to line.substring(separatorIndex + 1)
            }
            .toMap()
        val supportsThinking = values["supportsThinking"]?.toBooleanStrictOrNull() ?: return null
        val supportsVision = values["supportsVision"]?.toBooleanStrictOrNull() ?: return null
        return LiteRtModelOptions(
            supportsThinking = supportsThinking,
            supportsVision = supportsVision,
        )
    }
}
