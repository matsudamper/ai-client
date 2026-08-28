# ML Kit Structured Output
# https://developers.google.com/ml-kit/genai/prompt/android/structured-output
-keep class net.matsudamper.gptclient.localmodel.CalendarOutput { *; }
-keep class net.matsudamper.gptclient.localmodel.CalendarItem { *; }
-keep class net.matsudamper.gptclient.localmodel.MoneyOutput { *; }
-keep class net.matsudamper.gptclient.localmodel.MoneyItem { *; }
-keep class net.matsudamper.gptclient.localmodel.EmojiOutput { *; }
-keep class net.matsudamper.gptclient.localmodel.**_GeneratedProvider { *; }
-keep class * implements com.google.mlkit.genai.schema.guided.GenerableProvider { *; }
