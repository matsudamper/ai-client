package net.matsudamper.gptclient.localmodel

import android.content.Context
import androidx.work.WorkManager
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun localModelFeatureModule(): Module =
    module {
        single<LocalModelRepository> {
            LocalModelRepositoryImpl(
                context = get(),
                workManager = WorkManager.getInstance(get<Context>()),
            )
        }
        single<LocalModelClientFactory> {
            AndroidLocalModelClientFactory(context = get())
        }
    }
