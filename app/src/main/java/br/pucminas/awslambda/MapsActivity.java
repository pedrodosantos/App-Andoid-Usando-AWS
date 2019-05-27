package br.pucminas.awslambda;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.lambdainvoker.LambdaFunctionException;
import com.amazonaws.mobileconnectors.lambdainvoker.LambdaInvokerFactory;
import com.amazonaws.regions.Regions;
import com.google.maps.android.SphericalUtil;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters
    private static final long MIN_TIME_BW_UPDATES = 1; // 1 minute
    CognitoCachingCredentialsProvider cognitoProvider;
    MyInterface myInterface;
    RequestClass request = new RequestClass("GPS", "shulambs");
    private  LocationCallback locationCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Initialize the Amazon Cognito credentials provider
        cognitoProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "us-east-2:f310acb3-f15c-4a73-afce-521bad38bdb4", // Identity pool ID
                Regions.US_EAST_2 // Region
        );


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION},
                    1);
        }

        // Create LambdaInvokerFactory, to be used to instantiate the Lambda proxy.
        LambdaInvokerFactory factory = new LambdaInvokerFactory(this.getApplicationContext(),
                Regions.US_EAST_2, cognitoProvider);


        // Create the Lambda proxy object with a default Json data binder.
        // You can provide your own data binder by implementing
        // LambdaDataBinder.
        myInterface = factory.build(MyInterface.class);

        RequestClass request = new RequestClass("GPS", "1234");
        // The Lambda function invocation results in a network call.
        // Make sure it is not called from the main thread.
        new AsyncTask<RequestClass, Void, ResponseClass>() {
            @Override
            protected ResponseClass doInBackground(RequestClass... params) {
                // invoke "echo" method. In case it fails, it will throw a
                // LambdaFunctionException.
                try {
                    return myInterface.androidAWSlambda(params[0]);
                } catch (LambdaFunctionException lfe) {
                    Log.e("Tag", "Failed to invoke echo", lfe);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(ResponseClass result) {
                if (result == null) {
                    return;
                }

                // Do a toast
                Toast.makeText(MapsActivity.this, result.getResponseString(), Toast.LENGTH_LONG).show();
            }
        }.execute(request);


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);



    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


        Location location; // location
        double latitude=0; // latitude
        double longitude=0; // longitude
        String sensorName = "NOT AVAILABLE";
        String sensorValue = "NONE";


        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (Build.VERSION.SDK_INT >= 21 &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // getting GPS status
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        Log.v("isGPSEnabled", "=" + isGPSEnabled);
        // getting network status
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        Log.v("isNetworkEnabled", "=" + isNetworkEnabled);

        if (isNetworkEnabled) {
            location = null;
            if (locationManager != null) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                sensorName = "NETWORK";
                if (location != null) {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    sensorValue = "lat: " + latitude + ", long: " + longitude;
                }
            }
        }

        if (isGPSEnabled) {
            location = null;
            if (locationManager != null) {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                sensorName = "GPS";
                if (location != null) {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    sensorValue = "lat: " + latitude + ", long: " + longitude;
                }
            }
        }

        RequestClass request = new RequestClass(sensorName, sensorValue);

        new AsyncTask<RequestClass, Void, ResponseClass>() {
            @Override
            protected ResponseClass doInBackground(RequestClass... params) {
                try {
                    return myInterface.androidAWSlambda(params[0]);
                } catch (LambdaFunctionException lfe) {
                    Log.e("Tag", "Failed to invoke echo", lfe);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(ResponseClass result) {
                if (result == null) {
                    return;
                }

                // Do a toast
                Toast.makeText(MapsActivity.this, result.getResponseString(), Toast.LENGTH_LONG).show();
            }
        }.execute(request);

        // Adiciona o marcador ao mapa da localizacao do celular e da um zoom minimo
        LatLng minhaPosicao = new LatLng(latitude, longitude);
        mMap.addMarker(new MarkerOptions().position(minhaPosicao).title("Estou aqui"));
        mMap.setMinZoomPreference(15.0f);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(minhaPosicao));
    }


    private void mostrarMensagem( Location location){

        double latitude=0; // latitude
        double longitude=0; // longitude
        String sensorName = "NOT AVAILABLE";
        String sensorValue = "NONE";


        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (Build.VERSION.SDK_INT >= 21 &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // getting GPS status
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        Log.v("isGPSEnabled", "=" + isGPSEnabled);
        // getting network status
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        Log.v("isNetworkEnabled", "=" + isNetworkEnabled);

        if (isNetworkEnabled) {

            if (locationManager != null) {

                sensorName = "NETWORK";
                if (location != null) {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    sensorValue = "lat: " + latitude + ", long: " + longitude;
                }
            }
        }

        if (isGPSEnabled) {

            if (locationManager != null) {

                sensorName = "GPS";
                if (location != null) {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    sensorValue = "lat: " + latitude + ", long: " + longitude;
                }
            }
        }

        RequestClass request = new RequestClass(sensorName, sensorValue);

        new AsyncTask<RequestClass, Void, ResponseClass>() {
            @Override
            protected ResponseClass doInBackground(RequestClass... params) {
                try {
                    return myInterface.androidAWSlambda(params[0]);
                } catch (LambdaFunctionException lfe) {
                    Log.e("Tag", "Failed to invoke echo", lfe);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(ResponseClass result) {
                if (result == null) {
                    return;
                }

                // Do a toast
                Toast.makeText(MapsActivity.this, result.getResponseString(), Toast.LENGTH_LONG).show();
            }
        }.execute(request);

        // Adiciona o marcador ao mapa da localizacao do celular e da um zoom minimo
        LatLng minhaPosicao = new LatLng(latitude, longitude);
        mMap.addMarker(new MarkerOptions().position(minhaPosicao).title("Estou aqui"));
        mMap.setMinZoomPreference(15.0f);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(minhaPosicao));

        calcularDistancia(minhaPosicao);
    }

    private void calcularDistancia(LatLng minhaPosicao )
    {
        LatLng pucSaoGabriel = new LatLng(-19.856956, -43.919178);
        LatLng pucPracaLiberdade = new LatLng(-19.933118, -43.937105);
        LatLng pucCorel = new LatLng(-19.924798, -43.992633);
        LatLng pucBarreiro = new LatLng(-19.976734, -44.026180);


        double distance = SphericalUtil.computeDistanceBetween(minhaPosicao, pucSaoGabriel);
        if(distance<= 100) {
            Log.i("LOG", "Bem vindo a PUC São Gabriel!");
            return;
        }

        distance = SphericalUtil.computeDistanceBetween(minhaPosicao, pucPracaLiberdade);
        if(distance<= 100) {
            Log.i("LOG", "Bem vindo a PUC Praça da Liberdade!");
            return;
        }

        distance = SphericalUtil.computeDistanceBetween(minhaPosicao, pucCorel);
        if(distance<= 100) {
            Log.i("LOG", "Bem vindo a PUC Coração Eucaristico!");
            return;
        }

        distance = SphericalUtil.computeDistanceBetween(minhaPosicao, pucBarreiro);
        if(distance<= 100)
            Log.i("LOG","Bem vindo a PUC Barreiro!");
        return;





    }

    private String formatNumber(double distance) {
        String unit = "m";
        if (distance > 1000) {
            distance /= 1000;
            unit = "km";
        }

        return String.format("%4.3f%s", distance, unit);
    }
}
