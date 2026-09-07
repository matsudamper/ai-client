# ML Kit Structured Output
# https://developers.google.com/ml-kit/genai/prompt/android/structured-output
-keep class net.matsudamper.gptclient.localmodel.CalendarOutput { *; }
-keep class net.matsudamper.gptclient.localmodel.CalendarItem { *; }
-keep class net.matsudamper.gptclient.localmodel.MoneyOutput { *; }
-keep class net.matsudamper.gptclient.localmodel.MoneyItem { *; }
-keep class net.matsudamper.gptclient.localmodel.EmojiOutput { *; }
-keep class net.matsudamper.gptclient.localmodel.**_GeneratedProvider { *; }
-keep class * implements com.google.mlkit.genai.schema.guided.GenerableProvider { *; }

# LiteRT-LM JNI
# https://github.com/google-ai-edge/LiteRT-LM/issues/2406
-keep class com.google.ai.edge.litertlm.** { *; }
-keepclasseswithmembernames class com.google.ai.edge.litertlm.** {
    native <methods>;
}
-keep class com.google.ai.edge.litertlm.LiteRtLmJni$JniMessageCallback { *; }
-keep class com.google.ai.edge.litertlm.LiteRtLmJni$JniInferenceCallback { *; }
-keep class * implements com.google.ai.edge.litertlm.LiteRtLmJni$JniMessageCallback { *; }
-keep class * implements com.google.ai.edge.litertlm.LiteRtLmJni$JniInferenceCallback { *; }
