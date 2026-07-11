# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/amarthapa/Android/Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any custom rules here that might be specific to your project's logic.
# Keep Room entities and type converters
-keep class com.gmail.amarethapa.physicalalarm.data.room.** { *; }

# Keep BroadcastReceivers (referenced by name in AndroidManifest.xml)
-keep class com.gmail.amarethapa.physicalalarm.AlarmReceiver { *; }
-keep class com.gmail.amarethapa.physicalalarm.BootReceiver { *; }
