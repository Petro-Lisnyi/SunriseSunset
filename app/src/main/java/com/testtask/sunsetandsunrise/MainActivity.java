package com.testtask.sunsetandsunrise;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.PlaceLikelihoodBufferResponse;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private final String TAG = this.getClass().getSimpleName();

    private final int PERMISSIONS_REQUEST_LOCATION = 1;
    private boolean mLocationPermission;

    private final static int PLACE_AUTOCOMPLETE_REQUEST_CODE = 0;


    private String mCityName;
    private float mCurrentLat;
    private float mCurrentLng;

    private Toolbar mToolbar;

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
    private Button mButtonTryAgain;
    private FloatingActionButton mFabRefresh;

    protected GeoDataClient mGeoDataClient;
    protected PlaceDetectionClient mPlaceDetectionClient;

    private SunriseSunsetApi mSunriseSunsetApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        initUiElements();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && isRestrictedLocationPermission()) {
            requestLocationPermission();
        }

        createSunriseSunsetApi();

        // Construct a GeoDataClient.
        mGeoDataClient = Places.getGeoDataClient(this);

        // Construct a PlaceDetectionClient.
        mPlaceDetectionClient = Places.getPlaceDetectionClient(this);

        initCurrentPlace();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = PlaceAutocomplete.getPlace(this, data);
                Log.i(TAG, "Current place: " + place.getName() + " with latitude - " +
                        place.getLatLng().latitude + ", longitude - " + place.getLatLng().longitude);
                // TODO: Handle the result.
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this, data);
                // TODO: Handle the error.
                Log.e(TAG, status.getStatusMessage());

            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_LOCATION) {
            mLocationPermission =
                    !ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.ACCESS_FINE_LOCATION);
            if (mLocationPermission || isRestrictedLocationPermission())
                requestLocationPermission();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_search) callPlaceAutocomplete();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_try_again:
                initCurrentPlace();
                break;
            case R.id.fab_refresh:
                initCurrentPlace();
                break;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestLocationPermission() {
        final String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};

        if (mLocationPermission) {
            View.OnClickListener listener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                }
            };
            Snackbar.make(mToolbar, R.string.ask_location_permissions,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, listener)
                    .show();
            return;
        }
        Log.w(TAG, "Location permission is not granted. Requesting permission");
        requestPermissions(permissions, PERMISSIONS_REQUEST_LOCATION);
    }

    private void initUiElements() {
        mConstraintMainInfo = findViewById(R.id.constraint_main_info);
        mLinearLoadingInfo = findViewById(R.id.linear_load_info);
        mLinearErrorInfo = findViewById(R.id.linear_error_info);

        mTextLocation = findViewById(R.id.text_city_name);
        mTextDate = findViewById(R.id.text_date);
        mTextTwilightBegin = findViewById(R.id.text_twilight_begin);
        mTextSunrise = findViewById(R.id.text_sunrise_time_title);
        mTextSunriseTime = findViewById(R.id.text_sunrise_time);
        mTextSunset = findViewById(R.id.text_sunset_time_title);
        mTextSunsetTime = findViewById(R.id.text_sunset_time);
        mTextTwilightEnd = findViewById(R.id.text_twilight_end);
        mTextDayLength = findViewById(R.id.text_day_length);
        mTextTimeZoneWarn = findViewById(R.id.id_text_warning);
        mTextLoading = findViewById(R.id.text_loading_info);
        mTextError = findViewById(R.id.text_error_info);

        mButtonTryAgain = findViewById(R.id.button_try_again);
        mFabRefresh = findViewById(R.id.fab_refresh);

        mButtonTryAgain.setOnClickListener(this);
        mFabRefresh.setOnClickListener(this);
    }

    private boolean isRestrictedLocationPermission() {
        return !(ActivityCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
    }

    /**
     * Initialise current place
     */
    private void initCurrentPlace() {
        //Adjust the UI
        mConstraintMainInfo.setVisibility(View.GONE);
        mLinearLoadingInfo.setVisibility(View.VISIBLE);
        mLinearErrorInfo.setVisibility(View.GONE);
        mFabRefresh.setVisibility(View.GONE);

        mTextLoading.setText(R.string.loading_location);

        @SuppressLint("MissingPermission")
        Task<PlaceLikelihoodBufferResponse> placeResult =
                mPlaceDetectionClient.getCurrentPlace(null);
        placeResult.addOnCompleteListener(new OnCompleteListener<PlaceLikelihoodBufferResponse>() {
            @Override
            public void onComplete(@NonNull Task<PlaceLikelihoodBufferResponse> task) {
                try {
                    Place place = task.getResult().get(0).getPlace();
                    mCurrentLat = (float) place.getLatLng().latitude;
                    mCurrentLng = (float) place.getLatLng().longitude;
                    mCityName = place.getAddress()+ "";
                    Log.i(TAG, "Current place: " + place.getName() + " with latitude - " +
                            mCurrentLat + ", longitude - " + mCurrentLng + ".");

                    //downloading data
                    mTextLoading.setText(R.string.loading_data);
                    mSunriseSunsetApi.getResult(mCurrentLat, mCurrentLng).enqueue(mResultCallback);

                } catch (SecurityException e) {
                    showError(e.getLocalizedMessage());
                    Log.e(TAG, e.getMessage());
                } catch (Exception e) {
                    showError(e.getLocalizedMessage());
                    Log.e(TAG, e.getMessage());
                }
            }
        });
    }

    /**
     * Show dialog for searching any city
     */
    private void callPlaceAutocomplete() {
        try {
            Intent intent = new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY)
                    .build(this);
            startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
        } catch (GooglePlayServicesRepairableException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        } catch (GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }
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
            Log.e(TAG, "Call: " + call.toString() + "\n Throwable: " + t.getLocalizedMessage());
        }
    };

    /**
     * Initialise REST API client
     */
    private void createSunriseSunsetApi() {
        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(SunriseSunsetApi.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        mSunriseSunsetApi = retrofit.create(SunriseSunsetApi.class);
    }

    /**
     * Show results about current city
     */
    private void showResults(Result result) {
        mConstraintMainInfo.setVisibility(View.VISIBLE);
        mLinearLoadingInfo.setVisibility(View.GONE);
        mLinearErrorInfo.setVisibility(View.GONE);
        mFabRefresh.setVisibility(View.VISIBLE);

        String str  = getString(R.string.sunrise_and_sunset_times_in) + " " + mCityName;
        mTextLocation.setText(str);

        str = getString(R.string.today) + " " + getDate();
        mTextDate.setText(str);

        str = getString(R.string.first_light_at) + " " + result.getResults().getCivilTwilightBegin();
        mTextTwilightBegin.setText(str);

        str = getString(R.string.last_light_at) + " " + result.getResults().getCivilTwilightEnd();
        mTextTwilightEnd.setText(str);

        mTextSunrise.setText(R.string.sunrise_time);
        mTextSunriseTime.setText(result.getResults().getSunrise());

        mTextDayLength.setText(R.string.sunset_time);
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
        mFabRefresh.setVisibility(View.GONE);

        mTextError.setText(msg);
    }

    private String getDate(){
        return  new SimpleDateFormat("EEE, MMM d, yyyy, HH:mm",
                getResources().getConfiguration().locale).format(Calendar.getInstance().getTime());
    }
}