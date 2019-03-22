package in.androidapps.parv.digitalleash_parent;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemLongClickListener {

    private EditText parent_latitude_etext, parent_longitude_etext, parentUsername, radius, childName;
    private Button location_update, add_child;

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    private static final String PERSON_NAME = "nameKey";
    private static final String RADIUS = "radiusKey";
    private static final String CHILD_NAME = "childKey";
    private static final String STORAGE = "data_stored";
    private static String parent_latitude_value = null;
    private static String parent_longitude_value = null;

    private ListView listView;
    private List<String> childList = new ArrayList<>();
    private ArrayAdapter<String> listAdapter;
    private Set<String> newSet = new HashSet<String>();

    private static String url_string = "https://turntotech.firebaseio.com/digitalleash/users/";
    private static final int REQUEST_CODE = 3;

    private LocationManager locationManager;
    private LocationListener locationListener;

    JSONObject json_location = new JSONObject();
    private String provider_name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setup();
        setEventListeners();
        readSharedPref();

    }//OnCreate method ends here------------------------------------------------------------------------------------------------------------------------------------------

    private void setup() {
        setContentView(R.layout.activity_main);

        parentUsername = findViewById(R.id.username);                                                                             // view setup
        radius = findViewById(R.id.radius);
        childName = findViewById(R.id.child);
        add_child = findViewById(R.id.addChildButton);                                                                            // Get reference of widgets from XML layout
        location_update = findViewById(R.id.updateButton);
        listView = findViewById(R.id.listNames);
        parent_latitude_etext = findViewById(R.id.latitude);
        parent_longitude_etext = findViewById(R.id.longitude);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        sharedPreferences = getSharedPreferences(STORAGE, MODE_PRIVATE);


    } // setup----------------------------------------------------------------------------------------------

    private void setEventListeners() {                                                                                          //method for retrieving the latitude and longitude --locations

        location_update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {                                                                             //update button

                if (isNetworkAvailable()) {
                    getLocations(MainActivity.this, locationManager);
                } else {
                    Toast.makeText(getApplicationContext(), "NO NETWORK", Toast.LENGTH_LONG).show();
                }
            }
        });

        add_child.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {                                                                                   //add child button
                getListView();
            }
        });

        listView.setOnItemLongClickListener(this);                                                                            //long click

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {                                              // child selected
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (isNetworkAvailable()) {                                                                                   //onItemClick
                    String child_chosen = (String) listView.getItemAtPosition(position);
                    getChildLocation(child_chosen);
                } else {
                    Toast.makeText(getApplicationContext(), "NO NETWORK AVAILABLE", Toast.LENGTH_LONG).show();
                }
            }
        });
    }// eventlisteners----------------------------------------------------------------------------------------------------

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        childList.remove(position);
        listAdapter.notifyDataSetChanged();
        return true;
    }//-----------------------------------------------------------------------------------------------

    private void getLocations(MainActivity mainActivity, LocationManager locationManager1) {                              //getLocation of parent
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
                parentSetLocation(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                Toast.makeText(getApplicationContext(), "status changed", Toast.LENGTH_LONG);
            }

            @Override
            public void onProviderEnabled(String provider) {
                Toast.makeText(getApplicationContext(), "provider enabled", Toast.LENGTH_LONG);
            }

            @Override
            public void onProviderDisabled(String provider) {
                Toast.makeText(getApplicationContext(), "provider disabled", Toast.LENGTH_LONG);
            }
        };
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setCostAllowed(false);
        if (locationManager1 != null) {
            if ((locationManager1.isProviderEnabled(LocationManager.GPS_PROVIDER)) ||
                    (locationManager1.isProviderEnabled(LocationManager.NETWORK_PROVIDER))) {
                provider_name = locationManager1.getBestProvider(criteria, true);
            }
            if (provider_name != null) {
                Location location_lastKnown = locationManager.getLastKnownLocation(provider_name);
                if (location_lastKnown != null) {
                    parentSetLocation(location_lastKnown);                                                                                                        // creating the json object for lat and long

                } else {
                    locationManager1.requestLocationUpdates(provider_name, 1000, 5, this.locationListener);
                }
            } else {
                Toast.makeText(getApplicationContext(), "no provider available", Toast.LENGTH_LONG);
            }
        } else {
            Toast.makeText(getApplicationContext(), "location manager is null", Toast.LENGTH_LONG);
        }


    }//getLocations method ends here---------------------------------------

    private void parentSetLocation(Location location_lastKnown) {
        parent_latitude_value = String.valueOf(location_lastKnown.getLatitude());
        parent_longitude_value = String.valueOf(location_lastKnown.getLongitude());

        parent_latitude_etext.setText(parent_latitude_value);                                                                     // lat and long values of parent
        parent_longitude_etext.setText(parent_longitude_value);
    }

    private void getChildLocation(String child_select) {                                                                                     //get child location
        new writeUrl().execute(child_select);

    }


    protected void getListView() {
        String get_childValue = childName.getText().toString();                                                                              // listView of the children
        if (childList.contains(get_childValue)) {
            Toast.makeText(getApplicationContext(), "This Name:  " + get_childValue + "  already exists.", Toast.LENGTH_LONG).show();
        } else {
            childList.add(get_childValue);
        }
        listView.setAdapter(listAdapter);

        editor = sharedPreferences.edit();
        Set<String> set = new HashSet<String>(childList);
        editor.putStringSet(CHILD_NAME, (set));
        editor.commit();
        listAdapter.notifyDataSetChanged();                                                                                                 // Notifies the attached observers that the underlying data
                                                                                                                                            // has been changed and any View reflecting the data set should refresh itself.

    }//getList----------------------------------------------------------------------------------------------------------

    private void readSharedPref() {

        String parent_retrieved = sharedPreferences.getString(PERSON_NAME, null);                                                  //retrieveing the parent and radius values
        String radius_retrieved = sharedPreferences.getString(RADIUS, null);

        parentUsername.setText(parent_retrieved);                                                                                            //setText(int resi) String resource identifier
        radius.setText(radius_retrieved);

        newSet = sharedPreferences.getStringSet(CHILD_NAME, new HashSet<String>());                                                             //retrieveing the list
        childList = new ArrayList<String>(newSet);
        listAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_expandable_list_item_1, childList);
        listView.setAdapter(listAdapter);

    }// readShared--------------------------------------------------------------------------------------------------------------


    public void save(View view) {                                                                                                              //save the values of the user and radius

        String person_name = parentUsername.getText().toString();
        String radius_value = radius.getText().toString();                                                                                       //get the names and radius

        editor = sharedPreferences.edit();                                                                                                     //you need to get the values in the shared edit for that you use the get(key,mode)
        editor.putString(PERSON_NAME, person_name);
        editor.putString(RADIUS, radius_value);

        editor.commit();                                                                                                                      // don forget

    }// save button ftn-------------------------------------------------------------------------------

    public void clear(View view) {
        childName.setText("");
    }

    private class writeUrl extends AsyncTask<String, Void, String> {

        private String selectedChild;

        @Override
        protected String doInBackground(String... strings) {
            String child_loc = null;
            selectedChild = strings[0];
            JSONObject jsonObject = new JSONObject();
            HttpURLConnection httpURLConnection = null;
            try {
                final String child_to_parent = parentUsername.getText().toString();
                StringBuffer whole_url = new StringBuffer(url_string);
                whole_url.append(child_to_parent + "/");                                                                                   //appending the url
                whole_url.append(selectedChild);
                whole_url.append(".json");
                Log.d("url testing", whole_url.toString());

                URL url = new URL(whole_url.toString());
                httpURLConnection = (HttpURLConnection) url.openConnection();                                                             //open connection - http
                httpURLConnection.setDoInput(true);
                httpURLConnection.setRequestMethod("GET");

                InputStream in = httpURLConnection.getInputStream();                                                                      //getting the values
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                StringBuffer sb = new StringBuffer();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                    Log.d("result testing", sb.toString());
                }
                child_loc = sb.toString();
                Log.d("child location ", child_loc);

            } catch (Exception e) {
                e.printStackTrace();

            }
            try {
                json_location.put("value", child_loc);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return child_loc;


        }

        @Override
        protected void onPostExecute(String child_detail) {
            if (child_detail.matches("null")) {
                Toast.makeText(getApplicationContext(), "child din't report yet", Toast.LENGTH_LONG).show();
            } else {
                findingDistance(child_detail);
            }
        }
    }

    private void findingDistance(String child_locDetails) {                                                                                //finding distance

        try {

            JSONObject jsonObject = new JSONObject(child_locDetails);

            Double child_lat = jsonObject.getDouble("latitude");
            Double child_long = jsonObject.getDouble("longitude");

            Location child_loctn = new Location("");
            child_loctn.setLatitude(child_lat);                                                                                           // child lat and long
            child_loctn.setLongitude(child_long);

            Location parent_loctn = new Location("");
            Double parent_lat = Double.valueOf(parent_latitude_etext.getText().toString());
            Double parent_long = Double.valueOf(parent_longitude_etext.getText().toString());

            parent_loctn.setLatitude(parent_lat);
            parent_loctn.setLongitude(parent_long);                                                                                       //parent lat and long

            Double par_Rad = Double.valueOf(radius.getText().toString());
            Double parent_radius = par_Rad;
            Log.d("parent radiu", parent_radius.toString());

            Float final_distance = Float.valueOf((parent_loctn.distanceTo(child_loctn)));
            Log.d("final dist", final_distance.toString());

            if (final_distance < parent_radius) {                                                                                        //distance check
                Toast.makeText(getApplicationContext(), "Your child is nearby - SAFE within your range", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, SafeActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(getApplicationContext(), "Your child is far - AWAY from your range", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, DangerActivity.class);
                startActivity(intent);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(), "Granted", Toast.LENGTH_SHORT);                                                                   // We can now safely use the API we requested access to
            } else {
                Toast.makeText(getApplicationContext(), "Denied", Toast.LENGTH_SHORT);                                                                   // Permission was denied or request was cancelled
            }
        }
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetworkInfo != null && activeNetworkInfo.isConnected() && activeNetworkInfo.isAvailable()) {
            return true;
        } else {
            return false;

        }
    }
}//MainActivity ends here