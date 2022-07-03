package sh.siava.rangesliderpreference;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.JsonReader;
import android.util.JsonWriter;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.google.android.material.slider.RangeSlider;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@SuppressWarnings("ConstantConditions")
public class RangeSliderPreference extends Preference {
    @SuppressWarnings("unused")
    private static final String TAG = "Range Slider Preference";
    private final float valueFrom;
    private final float valueTo;
    private final float tickInterval;
    private final float defaultValue;
    RangeSlider slider;
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

        TypedArray a =context.obtainStyledAttributes(attrs, R.styleable.RangeSliderPreference);
        updateConstantly = a.getBoolean(R.styleable.RangeSliderPreference_updatesContinuously, false);
        valueCount = a.getInteger(R.styleable.RangeSliderPreference_valueCount, 1);
        valueFrom = a.getFloat(R.styleable.RangeSliderPreference_minVal, 0f);
        valueTo = a.getFloat(R.styleable.RangeSliderPreference_maxVal, 100f);
        tickInterval = a.getFloat(R.styleable.RangeSliderPreference_tickInterval, 1f);
        defaultValue = a.getFloat(R.styleable.Preference_defaultValue, valueFrom);

        a.recycle();

    }

    public void savePrefs()
    {
        try {
            List<Float> values = slider.getValues();

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

            getSharedPreferences().edit().putString(getKey(), jsonString).apply();

        }catch (Exception ignored){}
    }

    public void syncState() {
        List<Float> values = getValues(getSharedPreferences(), getKey(), defaultValue);

        for (float v : values) {
            if (v < slider.getValueFrom() || v > slider.getValueTo() || v % slider.getStepSize() != 0) {
                values.remove(v);
            }
        }
        while (values.size() < valueCount) {
            values.add(defaultValue);
        }

        try {
            slider.setValues(values);
        } catch (Throwable t) {
            values.clear();
        }
    }

    RangeSlider.OnChangeListener changeListener = (slider, value, fromUser) -> {
        if(updateConstantly && fromUser)
        {
            savePrefs();
        }
    };

    RangeSlider.OnSliderTouchListener sliderTouchListener = new RangeSlider.OnSliderTouchListener() {
        @Override
        public void onStartTrackingTouch(@NonNull RangeSlider slider) {}

        @Override
        public void onStopTrackingTouch(@NonNull RangeSlider slider) {
            if(!updateConstantly)
            {
                savePrefs();
            }
        }
    };

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder)
    {
        super.onBindViewHolder(holder);

        slider = (RangeSlider) holder.findViewById(R.id.slider);

        slider.addOnSliderTouchListener(sliderTouchListener);
        slider.addOnChangeListener(changeListener);


        slider.setValueFrom(valueFrom);
        slider.setValueTo(valueTo);
        slider.setStepSize(tickInterval);

        syncState();
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
        jsonReader.nextName();
        jsonReader.beginArray();

        while (jsonReader.hasNext()) {
            values.add((float) jsonReader.nextDouble());
        }

        return values;
    }
}