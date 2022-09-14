# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keep class com.ss.ugc.android.editor.track.TrackPanel {*;}
-keep interface com.ss.ugc.android.editor.track.ITrackPanel {*;}
-keep class com.ss.ugc.android.editor.track.CurrentSlotInfo {*;}
-keep class com.ss.ugc.android.editor.track.PlayPositionState {*;}
-keep class com.ss.ugc.android.editor.track.SeekInfo {*;}
-keep class com.ss.ugc.android.editor.track.TrackPanelActionListener {*;}
-keep class com.ss.ugc.android.editor.track.TrackState {*;}
-keep class com.ss.ugc.android.editor.track.widget.TrackConfig {*;}
-keep class com.ss.ugc.android.editor.track.utils.VideoFrameUtil {*;}
-keep class com.ss.ugc.android.editor.track.widget.MultiTrackLayout$TrackStyle {*;}
-keep class com.ss.ugc.android.editor.track.widget.MultiTrackLayout$LabelType {*;}

