package net.matsudamper.gptclient.entity

import net.matsudamper.gptclient.room.entity.BuiltinProjectId
import net.matsudamper.gptclient.room.entity.ProjectId

fun projectUsageKey(builtinProjectId: BuiltinProjectId): String = "builtin:${builtinProjectId.id}"

fun projectUsageKey(projectId: ProjectId): String = "project:${projectId.id}"
