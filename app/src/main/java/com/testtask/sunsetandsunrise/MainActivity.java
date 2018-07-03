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
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.PlaceLikelihoodBufferResponse;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class MainActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getSimpleName();

    private final int PERMISSIONS_REQUEST_LOCATION = 1;
    private boolean mLocationPermission;

    private float mCurrentLat;
    private float mCurrentLng;

    private Toolbar mToolbar;

    protected GeoDataClient mGeoDataClient;
    protected PlaceDetectionClient mPlaceDetectionClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && isLocationPermissionRestricted()) {
            requestLocationPermission();
        }

        // Construct a GeoDataClient.
        mGeoDataClient = Places.getGeoDataClient(this);

        // Construct a PlaceDetectionClient.
        mPlaceDetectionClient = Places.getPlaceDetectionClient(this);

        initCurrentPlace();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View view) {

//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_LOCATION) {
            mLocationPermission =
                    !ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.ACCESS_FINE_LOCATION);
            if (mLocationPermission || isLocationPermissionRestricted())
                requestLocationPermission();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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

    private boolean isLocationPermissionRestricted() {
        return !(ActivityCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
    }

    private void initCurrentPlace() {
        @SuppressLint("MissingPermission")
        Task<PlaceLikelihoodBufferResponse> placeResult =
                mPlaceDetectionClient.getCurrentPlace(null);
        placeResult.addOnCompleteListener(new OnCompleteListener<PlaceLikelihoodBufferResponse>() {
            @Override
            public void onComplete(@NonNull Task<PlaceLikelihoodBufferResponse> task) {
                try {
                    Place pl = task.getResult().get(0).getPlace();
                    mCurrentLat = (float) pl.getLatLng().latitude;
                    mCurrentLng = (float) pl.getLatLng().longitude;

                    Log.i(TAG, "Current place: " + pl.getName() + " with latitude - " +
                    mCurrentLat + ", longitude - " + mCurrentLng + ".");
                } catch (SecurityException e) {
                    Log.e(TAG, e.getMessage());
                    Toast.makeText(getApplicationContext(), e.getLocalizedMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
