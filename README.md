# Range Slider Preference

This is a simple (yet very useful) library, based on <ins>com.google.android.material.slider.RangeSlider</ins> widget, to be used as Preference in Android's PreferenceScreen pages.

**The problem:**  
Android's built-in SeekBarPreference can only include a single slider, thus can show/set only one value per preference. However, if you need to set ranges in preferences, there's no built-in Slider Preference available.

**Range Slider Preference** is able to show/set several values per preference, covering SeekBarPreference's shortcoming.

**How to include**  
You can clone this repository using git, or simply add it to your project as a submodule (git submodule add https://github.com/siavash79/rangesliderpreference.git)

To use this preference in your apps, simply add this repository to your project as a module, and add the module to your app's dependencies.

You can then add it as a preference to your app settings page:

    <PreferenceScreen xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto">
    .
    .
    <sh.siava.rangesliderpreference.RangeSliderPreference
      android:key="MY_KEY"
      android:title="@string/the_title"
      android:summary="@string/the_summary"
      app:tickInterval=".5"        //value intervals
      app:valueCount="3"           //How many values shall be set - can be even 1
      app:minVal="0"	       //Slider start point
      app:maxVal="60"              //Slider end point
      app:defaultValue="3,12,46.5" //Default values, comma separated 
    />
    .
    .
    </PreferenceScreen>
In the above example, you are setting a slider that operates 3 values that must be between 0 to 60, increase every 0.5 unit, and if the preference is not yet set, defaults to 3, 12 and 46.5.

RangeSliderPreference is also able to parse the values of your previously-set preference, if it's of any of types Integer, Float, or JSON string (Name/value, or name/array)

to read the preference values in your app, simply use the static method included:

    List<Float> values = RangeSliderPreference.getValues(
                            myPrefrences        /*SharedPreferences*/,
                            "MY_KEY"            /* preference key */, 
                            2                   /* default value in case the preference was not found */);
and the value(s) will be presented in a List\<Float\> object.
