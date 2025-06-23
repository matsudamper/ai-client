package net.matsudamper.gptclient

import android.app.Application
import androidx.work.WorkManager
import net.matsudamper.gptclient.datastore.SettingDataStore
import net.matsudamper.gptclient.room.AppDatabase
import net.matsudamper.gptclient.room.RoomPlatformBuilder
import net.matsudamper.gptclient.viewmodel.AddRequestUseCase
import net.matsudamper.gptclient.worker.AndroidWorkManagerScheduler
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.dsl.module

class Application : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            loadKoinModules(
                module = module {
                    single<AppDatabase> {
                        RoomPlatformBuilder.create(applicationContext)
                    }
                    single<SettingDataStore> {
                        SettingDataStore(filesDir.resolve("setting").absolutePath)
                    }
                    single<AddRequestUseCase.WorkManagerScheduler> {
                        AndroidWorkManagerScheduler(WorkManager.getInstance(applicationContext))
                    }
                },
            )
        }
    }
}
