package net.matsudamper.gptclient.localmodel

import org.koin.core.module.Module

const val LOCAL_MODEL_DOWNLOAD_NOTIFICATION_CHANNEL_ID = "local_model_download_notifications"
const val EXTRA_OPEN_LOCAL_MODEL_SETTINGS = "openLocalModelSettings"

expect fun localModelFeatureModule(): Module
