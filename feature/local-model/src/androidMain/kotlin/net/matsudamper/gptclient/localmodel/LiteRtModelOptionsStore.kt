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
        val options = LiteRtModelOptionsReader.readOrNull(modelFile) ?: return unavailableLiteRtModelOptions()
        write(modelFile, options)
        return options
    }

    private fun optionsFile(modelFile: File): File =
        File(modelFile.parentFile, "${modelFile.name}.options")

    private fun format(options: LiteRtModelOptions): String =
        buildString {
            appendLine("supportsThinking=${options.supportsThinking}")
            appendLine("supportsVision=${options.supportsVision}")
            appendLine("textBackends=${formatBackends(options.textBackends)}")
            appendLine("visionBackends=${formatBackends(options.visionBackends)}")
            appendLine("textSocName=${options.textSocName}")
            appendLine("visionSocName=${options.visionSocName}")
        }

    private fun formatBackends(backends: Set<LiteRtBackendKind>): String =
        LiteRtBackendKind.entries.filter { it in backends }.joinToString(",") { it.name }

    private fun parse(text: String): LiteRtModelOptions? {
        val values = text.lineSequence()
            .mapNotNull { line ->
                val separatorIndex = line.indexOf('=').takeIf { it >= 0 } ?: return@mapNotNull null
                line.substring(0, separatorIndex) to line.substring(separatorIndex + 1)
            }
            .toMap()
        val supportsThinking = values["supportsThinking"]?.toBooleanStrictOrNull() ?: return null
        val supportsVision = values["supportsVision"]?.toBooleanStrictOrNull() ?: return null
        val textBackends = parseBackends(values["textBackends"]) ?: return null
        val visionBackends = parseBackends(values["visionBackends"]) ?: return null
        val textSocName = values["textSocName"] ?: return null
        val visionSocName = values["visionSocName"] ?: return null
        return LiteRtModelOptions(
            supportsThinking = supportsThinking,
            supportsVision = supportsVision,
            textBackends = textBackends,
            visionBackends = visionBackends,
            textSocName = textSocName,
            visionSocName = visionSocName,
        )
    }

    private fun parseBackends(value: String?): Set<LiteRtBackendKind>? {
        if (value == null) {
            return null
        }
        if (value.isEmpty()) {
            return setOf()
        }
        return value.split(",").map { token ->
            LiteRtBackendKind.entries.firstOrNull { it.name == token } ?: return null
        }.toSet()
    }
}
