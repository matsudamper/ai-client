package net.matsudamper.gptclient

import android.app.Application
import android.content.Context
import androidx.work.WorkManager
import net.matsudamper.gptclient.datastore.AndroidSettingsEncryptor
import net.matsudamper.gptclient.datastore.SettingDataStore
import net.matsudamper.gptclient.datastore.SettingsEncryptor
import net.matsudamper.gptclient.localmodel.LOCAL_MODEL_DOWNLOAD_NOTIFICATION_CHANNEL_ID
import net.matsudamper.gptclient.localmodel.localModelFeatureModule
import net.matsudamper.gptclient.room.AppDatabase
import net.matsudamper.gptclient.room.RoomPlatformBuilder
import net.matsudamper.gptclient.viewmodel.AddRequestUseCase
import net.matsudamper.gptclient.worker.AndroidWorkManagerScheduler
import org.koin.android.ext.android.get
import org.koin.core.context.startKoin
import org.koin.dsl.module

class Application : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            modules(
                module {
                    single<Context> { applicationContext }
                    single<AppDatabase> {
                        RoomPlatformBuilder.create(applicationContext)
                    }
                    single<SettingsEncryptor> {
                        AndroidSettingsEncryptor()
                    }
                    single<SettingDataStore> {
                        SettingDataStore(
                            storagePath = filesDir.resolve("setting.pb").absolutePath,
                            encryptor = get(),
                        )
                    }
                    single<AddRequestUseCase.WorkManagerScheduler> {
                        AndroidWorkManagerScheduler(WorkManager.getInstance(applicationContext))
                    }
                    single<PlatformRequest> {
                        AndroidPlatformRequest(
                            context = get(),
                        )
                    }
                },
                localModelFeatureModule(),
            )
        }
        val platformRequest = get<PlatformRequest>()
        platformRequest.createNotificationChannel(GPT_CLIENT_NOTIFICATION_CHANNEL_ID)
        platformRequest.createNotificationChannel(LOCAL_MODEL_DOWNLOAD_NOTIFICATION_CHANNEL_ID)
    }
}
