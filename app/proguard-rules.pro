# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/amarthapa/Android/Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any custom rules here that might be specific to your project's logic.
# For example, if you're using Room, you might need to keep certain classes:
-keep class com.gmail.amarethapa.physicalalarm.data.room.** { *; }
