package net.matsudamper.gptclient

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import net.matsudamper.gptclient.app.R
import net.matsudamper.gptclient.datastore.SettingDataStore
import net.matsudamper.gptclient.entity.Calendar
import net.matsudamper.gptclient.entity.Emoji
import net.matsudamper.gptclient.entity.Money
import net.matsudamper.gptclient.entity.getProjectTitle
import net.matsudamper.gptclient.entity.projectUsageKey
import net.matsudamper.gptclient.room.AppDatabase
import net.matsudamper.gptclient.room.entity.BuiltinProjectId
import net.matsudamper.gptclient.room.entity.Project

const val EXTRA_SHORTCUT_BUILTIN_PROJECT_ID = "shortcutBuiltinProjectId"
const val EXTRA_SHORTCUT_PROJECT_ID = "shortcutProjectId"
const val EXTRA_SHORTCUT_PROJECT_TITLE = "shortcutProjectTitle"

class ProjectShortcutPublisher(
    private val context: Context,
    private val appDatabase: AppDatabase,
    private val settingDataStore: SettingDataStore,
) {
    fun start(scope: CoroutineScope) {
        scope.launch(Dispatchers.Default) {
            combine(
                appDatabase.projectDao().getAll(),
                settingDataStore.getProjectLastUsedAtFlow(),
            ) { projects, lastUsedAt ->
                createEntries(
                    projects = projects,
                    lastUsedAt = lastUsedAt,
                )
            }.distinctUntilChanged().collectLatest { entries ->
                publish(entries)
            }
        }
    }

    private fun createEntries(
        projects: List<Project>,
        lastUsedAt: Map<String, Long>,
    ): List<Entry> {
        val builtinEntries = listOf(
            BuiltinProjectId.Calendar,
            BuiltinProjectId.Money,
            BuiltinProjectId.Emoji,
        ).map { builtinProjectId ->
            Entry(
                key = projectUsageKey(builtinProjectId),
                label = builtinProjectId.getProjectTitle(),
                target = Entry.Target.Builtin(builtinProjectId),
            )
        }
        val projectEntries = projects.map { project ->
            Entry(
                key = projectUsageKey(project.id),
                label = project.name,
                target = Entry.Target.UserProject(project),
            )
        }
        val maxCount = minOf(
            MAX_SHORTCUT_COUNT,
            ShortcutManagerCompat.getMaxShortcutCountPerActivity(context),
        )
        return builtinEntries.plus(projectEntries)
            .sortedByDescending { lastUsedAt[it.key] ?: 0L }
            .take(maxCount)
    }

    private fun publish(entries: List<Entry>) {
        val shortcuts = entries.mapIndexed { index, entry ->
            val intent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra(EXTRA_SHORTCUT_PROJECT_TITLE, entry.label)
                when (val target = entry.target) {
                    is Entry.Target.Builtin -> {
                        putExtra(EXTRA_SHORTCUT_BUILTIN_PROJECT_ID, target.builtinProjectId.id)
                    }

                    is Entry.Target.UserProject -> {
                        putExtra(EXTRA_SHORTCUT_PROJECT_ID, target.project.id.id)
                    }
                }
            }
            ShortcutInfoCompat.Builder(context, entry.key)
                .setShortLabel(entry.label)
                .setLongLabel(entry.label)
                .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_app_icon))
                .setRank(index)
                .setIntent(intent)
                .build()
        }
        ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
    }

    private data class Entry(
        val key: String,
        val label: String,
        val target: Target,
    ) {
        sealed interface Target {
            data class Builtin(val builtinProjectId: BuiltinProjectId) : Target
            data class UserProject(val project: Project) : Target
        }
    }

    companion object {
        private const val MAX_SHORTCUT_COUNT = 4
    }
}
