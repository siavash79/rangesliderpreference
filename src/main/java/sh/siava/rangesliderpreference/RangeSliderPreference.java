package sh.siava.rangesliderpreference;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.google.android.material.slider.LabelFormatter;
import com.google.android.material.slider.RangeSlider;

import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;


public class RangeSliderPreference extends Preference {
    @SuppressWarnings("unused")
    private static final String TAG = "Range Slider Preference";
    private float valueFrom;
    private float valueTo;
    private final float tickInterval;
    private final List<Float> defaultValue = new ArrayList<>();
    private RangeSlider slider;
    int valueCount;

    boolean updateConstantly;

    @SuppressWarnings("unused")
    public RangeSliderPreference(Context context, AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public RangeSliderPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setLayoutResource(R.layout.range_slider);

        try(TypedArray a =context.obtainStyledAttributes(attrs, R.styleable.RangeSliderPreference)) {

            updateConstantly = a.getBoolean(R.styleable.RangeSliderPreference_updatesContinuously, false);
            valueCount = a.getInteger(R.styleable.RangeSliderPreference_valueCount, 1);
            valueFrom = a.getFloat(R.styleable.RangeSliderPreference_minVal, 0f);
            valueTo = a.getFloat(R.styleable.RangeSliderPreference_maxVal, 100f);
            tickInterval = a.getFloat(R.styleable.RangeSliderPreference_tickInterval, 1f);
            String defaultValueStr = a.getString(R.styleable.Preference_defaultValue);

            try {
                Scanner scanner = new Scanner(defaultValueStr);
                scanner.useDelimiter(",");
                scanner.useLocale(Locale.ENGLISH);

                while (scanner.hasNext()) {
                    defaultValue.add(scanner.nextFloat());
                }
            } catch (Exception ignored) {
                Log.e(TAG, String.format("RangeSliderPreference: Error parsing default values for key: %s", getKey()));
            }
        }
    }
    public void savePrefs(List<Float> values)
    {
        setValues(getSharedPreferences(), getKey(), values);
    }

    @SuppressWarnings("UnusedReturnValue")
    public static boolean setValues(SharedPreferences sharedPreferences, String key, List<Float> values)
    {
        try {
            StringWriter writer = new StringWriter();
            JsonWriter jsonWriter = new JsonWriter(writer);
            jsonWriter.beginObject();
            jsonWriter.name("");
            jsonWriter.beginArray();

            for (float value : values) {
                jsonWriter.value(value);
            }
            jsonWriter.endArray();
            jsonWriter.endObject();
            jsonWriter.close();
            String jsonString = writer.toString();

            sharedPreferences.edit().putString(key, jsonString).apply();

            return true;

        }catch (Exception ignored)
        {
            return false;
        }
    }

    public void syncState() {
        List<Float> values = getValues(getSharedPreferences(), getKey(), valueFrom);

        boolean needsCommit = cleanupValues(values);

        if(needsCommit) savePrefs(values);

        try {
            slider.setValues(values);
        } catch (Throwable t) {
            values.clear();
        }
    }

    RangeSlider.OnChangeListener changeListener = (slider, value, fromUser) -> {
        if(!getKey().equals(slider.getTag())) return;

        if(updateConstantly && fromUser)
        {
            savePrefs(slider.getValues());
        }
    };

    public boolean cleanupValues(List<Float> values)
    {
        boolean needsCommit = false;

        BigDecimal step = new BigDecimal(String.valueOf(tickInterval)); //float and double are not accurate when it comes to decimal points

        for(int i = 0; i < values.size(); i++)
        {
            BigDecimal round = new BigDecimal(Math.round(values.get(i)/tickInterval));
            double  v = Math.min(Math.max(step.multiply(round).doubleValue(), valueFrom), valueTo);
            if(v != values.get(i))
            {
                values.set(i, (float)v);
                needsCommit = true;
            }
        }
        if(values.size() < valueCount)
        {
            needsCommit = true;
            values.clear();
            values.addAll(defaultValue);
            while (values.size() < valueCount) {
                values.add(valueFrom);
            }
        }
        else if (values.size() > valueCount)
        {
            needsCommit = true;
            while(values.size() > valueCount)
            {
                values.remove(values.size()-1);
            }
        }

        return needsCommit;
    }

    RangeSlider.OnSliderTouchListener sliderTouchListener = new RangeSlider.OnSliderTouchListener() {
        @Override
        public void onStartTrackingTouch(@NonNull RangeSlider slider) {}

        @Override
        public void onStopTrackingTouch(@NonNull RangeSlider slider) {
            if(!getKey().equals(slider.getTag())) return;

            if(!updateConstantly)
            {
                savePrefs(slider.getValues());
            }
        }
    };

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder)
    {
        super.onBindViewHolder(holder);

        slider = (RangeSlider) holder.findViewById(R.id.slider);
        slider.setTag(getKey());

        slider.addOnSliderTouchListener(sliderTouchListener);
        slider.addOnChangeListener(changeListener);

        slider.setValueFrom(valueFrom);
        slider.setValueTo(valueTo);
        slider.setStepSize(tickInterval);

        syncState();
    }

    public void setMin(float value)
    {
        valueFrom = value;
        slider.setValueFrom(value);
    }

    public void setMax(float value)
    {
        valueTo = value;
        slider.setValueTo(value);
    }

    public static List<Float> getValues(SharedPreferences prefs, String key, float defaultValue)
    {
        List<Float> values;

        try {
            String JSONString = prefs.getString(key, "");
            values = getValues(JSONString);
        }
        catch (Exception ignored)
        {
            try {
                float value = prefs.getFloat(key, defaultValue);
                values = Collections.singletonList(value);
            }
            catch (Exception ignored2)
            {
                try {
                    int value = prefs.getInt(key, Math.round(defaultValue));
                    values = Collections.singletonList((float)value);
                }catch (Exception ignored3)
                {
                    values = Collections.singletonList(defaultValue);
                }
            }
        }
        return values;
    }
    public static List<Float> getValues(String JSONString) throws Exception
    {
        List<Float> values = new ArrayList<>();

        if(JSONString.trim().isEmpty()) return values;

        JsonReader jsonReader = new JsonReader(new StringReader(JSONString));

        jsonReader.beginObject();
        try {
            jsonReader.nextName();
            jsonReader.beginArray();
        }catch (Exception ignored){}

        while (jsonReader.hasNext()) {
            try
            {
                jsonReader.nextName();
            }catch (Exception ignored){}
            values.add((float) jsonReader.nextDouble());
        }

        return values;
    }

    public static float getSingleFloatValue(SharedPreferences prefs, String key, float defaultValue)
    {
        float result = defaultValue;

        try
        {
            result = getValues(prefs, key, defaultValue).get(0);
        }
        catch (Throwable ignored){}

        return result;
    }

    public static int getSingleIntValue(SharedPreferences prefs, String key, int defaultValue)
    {
        return Math.round(getSingleFloatValue(prefs, key, defaultValue));
    }

    public void setLabelFormatter(LabelFormatter formatter)
    {
        if(slider != null) {
            slider.setLabelFormatter(formatter);
        }
    }

    public float getFirstValue() {
        return valueFrom;
    }
}
