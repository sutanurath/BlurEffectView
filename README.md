## BlurEffectView


Blur effect view like ios.


### Screenshots

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

Add dependencies in your /app/build.gradle file

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

If it doesn't work, please send me a email, sutanurath@gmail.com


Add proguard rules if require.

```
-keep class android.support.v8.renderscript.** { *; }
```

### License

```
Copyright (C) 2014 sutanurath

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

