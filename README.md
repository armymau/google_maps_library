# Google Maps for Android

Android library to manage the Google Maps Android API. 

You can add maps based on Google Maps data to your application in easy way and automatically. 

### Installing

**Step 1**. Add the JitPack repository to your build file.

Add it in your root build.gradle at the end of repositories:

```
  allprojects {
  	repositories {	
      ...
		  maven { url 'https://jitpack.io' }
	  }
  }
```

**Step 2**. Add the dependency

```	
  dependencies {
  	implementation 'com.github.armymau:google_maps_library:v1.6'
  }
```

**Step 3**. You Extend your AppCompatActivity with GoogleMapsActivity (or MapFragment extends GoogleMapsFragment) class and you can implements onLocationRetrieved(location : Location) method to get automatically the device position.



For more complete documentation 
https://developers.google.com/maps/documentation/android-api/intro

  
## Authors
Armando Mennini  - [armymau](https://github.com/armymau)
