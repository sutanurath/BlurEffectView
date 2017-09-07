## BlurEffectView


Blur effect view like ios.


![Alt text](https://i.imgur.com/7Q0lPmS.png "BlurEffectView")


Put the BlurEffectView in the layout xml

```
<com.sutanu.blureffectview.BlurEffectView
        android:layout_width="300dp"
        android:layout_height="300dp"
        app:blurEffectViewRadius="3dp"
        android:layout_centerInParent="true"
        app:blurEffectViewOverlayColor="#80C4A8A8" />
```

### Add to project

Add dependencies in your app level build.gradle file

```
dependencies {
	    compile 'com.sutanu.blureffectview:blureffectview:1.0.1'
	}
	android {
		buildToolsVersion '24.0.2'                
		defaultConfig {
			minSdkVersion 15
			renderscriptTargetApi 19
			renderscriptSupportModeEnabled true   
		}
	}
```

Add proguard rules if require.

```
-keep class android.support.v8.renderscript.** { *; }
```


