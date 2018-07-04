package com.testtask.sunsetandsunrise;

import android.content.Context;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.testtask.sunsetandsunrise.data.Result;
import com.testtask.sunsetandsunrise.data.SunriseSunsetApi;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class FoundCityFragment extends DialogFragment {

    private static final String LATITUDE_KEY = "latitude_key";
    private static final String LONGITUDE_KEY = "longitude_key";
    private static final String CITY_ADDRESS_KEY = "city_address_key";

    private String mCityAddress;
    private float mCurrentLat;
    private float mCurrentLng;

    private ConstraintLayout mConstraintMainInfo;
    private LinearLayout mLinearLoadingInfo;
    private LinearLayout mLinearErrorInfo;

    private TextView mTextLocation;
    private TextView mTextDate;
    private TextView mTextTwilightBegin;
    private TextView mTextSunrise;
    private TextView mTextSunriseTime;
    private TextView mTextSunset;
    private TextView mTextSunsetTime;
    private TextView mTextTwilightEnd;
    private TextView mTextDayLength;
    private TextView mTextTimeZoneWarn;

    private TextView mTextLoading;
    private TextView mTextError;

    public FoundCityFragment() {
        // Required empty public constructor
    }

    public static FoundCityFragment newInstance(float lat, float lng, String address) {
        FoundCityFragment fragment = new FoundCityFragment();
        Bundle args = new Bundle();
        args.putFloat(LATITUDE_KEY, lat);
        args.putFloat(LONGITUDE_KEY, lng);
        args.putString(CITY_ADDRESS_KEY, address);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mCurrentLat = getArguments().getFloat(LATITUDE_KEY);
            mCurrentLng = getArguments().getFloat(LONGITUDE_KEY);
            mCityAddress = getArguments().getString(CITY_ADDRESS_KEY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_found_city, container, false);
        initUiElements(v);
        initFoundPlace();
        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private void initUiElements(View view) {
        mConstraintMainInfo = view.findViewById(R.id.constraint_main_info);
        mLinearLoadingInfo = view.findViewById(R.id.linear_load_info);
        mLinearErrorInfo = view.findViewById(R.id.linear_error_info);

        mTextLocation = view.findViewById(R.id.text_city_name);
        mTextDate = view.findViewById(R.id.text_date);
        mTextTwilightBegin = view.findViewById(R.id.text_twilight_begin);
        mTextSunrise = view.findViewById(R.id.text_sunrise_time_title);
        mTextSunriseTime = view.findViewById(R.id.text_sunrise_time);
        mTextSunset = view.findViewById(R.id.text_sunset_time_title);
        mTextSunsetTime = view.findViewById(R.id.text_sunset_time);
        mTextTwilightEnd = view.findViewById(R.id.text_twilight_end);
        mTextDayLength = view.findViewById(R.id.text_day_length);
        mTextTimeZoneWarn = view.findViewById(R.id.id_text_warning);
        mTextLoading = view.findViewById(R.id.text_loading_info);
        mTextError = view.findViewById(R.id.text_error_info);

        Button buttonTryAgain = view.findViewById(R.id.button_try_again);

        buttonTryAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initFoundPlace();
            }
        });
    }

    /**
     * Initialise found place
     */
    private void initFoundPlace() {
        //Adjust the UI
        mConstraintMainInfo.setVisibility(View.GONE);
        mLinearLoadingInfo.setVisibility(View.VISIBLE);
        mLinearErrorInfo.setVisibility(View.GONE);

        //downloading data
        mTextLoading.setText(R.string.loading_data);

        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(SunriseSunsetApi.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        SunriseSunsetApi sunriseSunsetApi = retrofit.create(SunriseSunsetApi.class);
        sunriseSunsetApi.getResult(mCurrentLat, mCurrentLng).enqueue(mResultCallback);
    }

    /**
     * Return result about current city
     */
    private Callback<Result> mResultCallback = new Callback<Result>() {
        @Override
        public void onResponse(Call<Result> call, Response<Result> response) {
            Result result = response.body();
            showResults(result);
        }

        @Override
        public void onFailure(Call<Result> call, Throwable t) {
            showError(t.getLocalizedMessage());
            Log.e(getTag(), "Call: " + call.toString() + "\n Throwable: " + t.getLocalizedMessage());
        }
    };

    /**
     * Show results about current city
     */
    private void showResults(Result result) {
        mConstraintMainInfo.setVisibility(View.VISIBLE);
        mLinearLoadingInfo.setVisibility(View.GONE);
        mLinearErrorInfo.setVisibility(View.GONE);

        String str;
        if (result.getResults().getCityAddress() == null)
            str = getString(R.string.sunrise_and_sunset_times_in) + " " + mCityAddress;
        else str = getString(R.string.sunrise_and_sunset_times_in) + " " +
                result.getResults().getCityAddress();

        mTextLocation.setText(str);

        str = getString(R.string.today) + " " + getDate();
        mTextDate.setText(str);

        str = getString(R.string.first_light_at) + " " + result.getResults().getCivilTwilightBegin();
        mTextTwilightBegin.setText(str);

        str = getString(R.string.last_light_at) + " " + result.getResults().getCivilTwilightEnd();
        mTextTwilightEnd.setText(str);

        mTextSunrise.setText(R.string.sunrise_time);
        mTextSunriseTime.setText(result.getResults().getSunrise());

        mTextSunset.setText(R.string.sunset_time);
        mTextSunsetTime.setText(result.getResults().getSunset());

        str = getString(R.string.day_length) + " " + result.getResults().getDayLength();
        mTextDayLength.setText(str);

        mTextTimeZoneWarn.setText(R.string.time_zone_warning);
    }

    /**
     * Show error message
     */
    private void showError(String msg) {
        mConstraintMainInfo.setVisibility(View.GONE);
        mLinearLoadingInfo.setVisibility(View.GONE);
        mLinearErrorInfo.setVisibility(View.VISIBLE);

        mTextError.setText(msg);
    }


    private String getDate() {
        return new SimpleDateFormat("EEE, MMM d, yyyy, HH:mm",
                getResources().getConfiguration().locale).format(Calendar.getInstance().getTime());
    }
}
