package in.androidapps.parv.digitalleash_child;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;


public class MainActivity extends AppCompatActivity {

    private static String LATITUDE = "latitude";
    private static String LONGITUDE = "longitude";
    private static String TIMESTAMP = "timestamp";
    private static String ERROR = "error";
    private static String CODE = "code";

    EditText parent_name, child_name;
    TextView error_message;
    Button request_button;

    private static final int REQUEST_CODE = 1;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Location location;

    String urlString = "https://turntotech.firebaseio.com/digitalleash/users/";
    private String provider_name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        parent_name = findViewById(R.id.parent);
        child_name = findViewById(R.id.child);
        error_message = findViewById(R.id.errorText);

        request_button = findViewById(R.id.retrive);

        request_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isNetworkAvailable()) {
                    locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    getLocations(locationManager);

                } else {
                    Toast.makeText(getApplicationContext(), "NO NETWORK", Toast.LENGTH_LONG).show();
                    error_message.setTextColor(Color.BLUE);
                    error_message.setText(ConnectivityManager.EXTRA_NO_CONNECTIVITY.toUpperCase());
                }

            }
        });
    } // onCreate ends here --------------

    private void getLocations(LocationManager locationManager1) {

        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE);
            }
            return;
        }
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                createData(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                error_message.setText("status changed");
            }

            @Override
            public void onProviderEnabled(String provider) {
                error_message.setText("provider enabled");
            }

            @Override
            public void onProviderDisabled(String provider) {
                error_message.setText("provider disabled");
            }


        };

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setCostAllowed(false);
        if(locationManager1 !=null) {
                if ((locationManager1.isProviderEnabled(LocationManager.GPS_PROVIDER)) ||
                        (locationManager1.isProviderEnabled(LocationManager.NETWORK_PROVIDER))) {
                    provider_name = locationManager1.getBestProvider(criteria, true);
                }
            if (provider_name != null) {
                Location location_lastKnown = locationManager.getLastKnownLocation(provider_name);
                if (location_lastKnown != null) {
                    createData(location_lastKnown);                                                                                                                 // creating the json object for lat and long

                } else {
                    locationManager1.requestLocationUpdates(provider_name, 1000, 5, this.locationListener);
                }
            } else {
                error_message.setText("no provider available");
            }
        }else{
            error_message.setText("location manager is null");
        }

    }  // location ftn ends here --------

    public void createData(Location location_values) {

        try {

            JSONObject json_location = new JSONObject();                                                                                              //building the json object
            json_location.put(LATITUDE, location_values.getLatitude());
            json_location.put(LONGITUDE, location_values.getLongitude());
            json_location.put(TIMESTAMP, new Date().getTime());

            String final_result = json_location.toString();                                                                                           // converting json to string so as to pass it on to server using async task

            new writeData().execute(final_result);                                                                                                    // defining class that extends the asyncTask for the next set of functions to be done. passing the json to http

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }  // createData ends here -------------

    private class writeData extends AsyncTask<String, JSONObject, JSONObject> {                                                                         //Posting it to the server function

        String jsonStr = "";

        @Override
        protected JSONObject doInBackground(String... strings) {

            jsonStr = strings[0];
            String parent_value = parent_name.getText().toString();
            String child_value = child_name.getText().toString();

            String concatUrl = urlString + parent_value + "/" + child_value + ".json";

            Log.d("url testing :", concatUrl);

            HttpURLConnection httpCon = null;
            JSONObject object = new JSONObject();
            try {
                URL url = new URL(concatUrl);

                httpCon = (HttpURLConnection) url.openConnection();

                httpCon.setDoOutput(true);
                httpCon.setDoInput(true);

                httpCon.setRequestMethod("PUT");
                httpCon.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                httpCon.setRequestProperty("Accept", "application/json");
                                                                                                                                                         // add json content to put request body
                Log.d("Method testing:", httpCon.getRequestMethod());

                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(httpCon.getOutputStream());
                outputStreamWriter.write(jsonStr);

                Log.i(MainActivity.class.toString(), jsonStr);

                outputStreamWriter.flush();
                outputStreamWriter.close();

                object.put(CODE, httpCon.getResponseCode());
                Log.d("Code Check :", String.valueOf(httpCon.getResponseCode()));

                object.put(ERROR, httpCon.getResponseMessage());

            } catch (Exception e) {
                e.printStackTrace();
            }

            return object;
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {

            try {
                if (jsonObject.has(CODE)) {
                    if ((jsonObject.getInt(CODE) > 399)
                            || (!(jsonObject.getInt(CODE) < 399))) { //error code range
                        error_message.setTextColor(Color.RED);
                        error_message.setText("Unsuccesfull in reporting the locations");
                        Log.d("ERROR REPORTED", jsonObject.getString(ERROR));

                    } else {
                        error_message.setTextColor(Color.GREEN);
                        error_message.setText("Successfully Reported the locations");
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

    }// writeData ends here -------------

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
            }
        }
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        //internet connection status easily by using getActiveNetworkInfo() method of ConnectivityManager object.
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();//NetworkInfo object to know the type of internet connection.
        if (activeNetworkInfo != null && activeNetworkInfo.isConnected() && activeNetworkInfo.isAvailable()) {
            return true;
        } else {
            return false;

        }
    }

}



