package course.labs.locationlab;

import java.util.Date;

import android.app.ListActivity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.Toast;

public class PlaceViewActivity extends ListActivity implements LocationListener {
	private static final long FIVE_MINS = 5 * 60 * 1000;
	private static final String TAG = "Lab-Location";

	// False if you don't have network access
	public static boolean sHasNetwork = false;

	private Location mLastLocationReading;
	private PlaceViewAdapter mAdapter;
	private LocationManager mLocationManager;
	private boolean mMockLocationOn = false;

	// default minimum time between new readings
	private long mMinTime = 5000;

	// default minimum distance between old and new readings.
	private float mMinDistance = 1000.0f;

	// A fake location provider used for testing
	private MockLocationProvider mMockLocationProvider;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set up the app's user interface. This class is a ListActivity,
        // so it has its own ListView. ListView's adapter should be a PlaceViewAdapter

		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		ListView placesListView = getListView();


        View footerView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.footer_view, null, false);

		footerView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Log.i(TAG, "Entered footerView.OnClickListener.onClick()");

                if (mLastLocationReading != null) {
                 	for (PlaceRecord el : mAdapter.getList()) {
                		if (el.intersects(mLastLocationReading)) {	
                			Toast.makeText(getApplicationContext(), "You already have this location badge", Toast.LENGTH_LONG).show();
                				return;
                		}
                	}
                	PlaceDownloaderTask t = new PlaceDownloaderTask(PlaceViewActivity.this, sHasNetwork);
                	t.execute(mLastLocationReading);
                	
                }
                
                
			}

		});
		//footerView.setClickable(false);
		placesListView.addFooterView(footerView);
		mAdapter = new PlaceViewAdapter(getApplicationContext());
		setListAdapter(mAdapter);

	}

	@Override
	protected void onResume() {
		super.onResume();

		startMockLocationManager();
		if (mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) != null) {
			if (ageInMilliseconds(mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)) > FIVE_MINS) {
		        mLastLocationReading = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
	
			}
		}
        	
        

        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, mMinTime, mMinDistance, this);
        
        
	}

	@Override
	protected void onPause() {


        mLocationManager.removeUpdates(this);
		shutdownMockLocationManager();
		super.onPause();
	}

	// Callback method used by PlaceDownloaderTask
	public void addNewPlace(PlaceRecord place) {
		
		
		Log.i(TAG, "Entered addNewPlace()");
		if (place != null) {
			if (place.getCountryName().isEmpty()) {
				Toast.makeText(getApplicationContext(), R.string.no_country_string, Toast.LENGTH_LONG).show();
				return;
			}
	        for (PlaceRecord el : mAdapter.getList()) {
	        	if (el.intersects(place.getLocation())) {
	        		Toast.makeText(getApplicationContext(), R.string.duplicate_location_string, Toast.LENGTH_LONG).show();
	        		return;
	        	}
	        }
		        mAdapter.add(place);
		} else {
			Toast.makeText(getApplicationContext(), "PlaceBadge could not be acquired", Toast.LENGTH_LONG).show();
		}
        
	}

	// LocationListener methods
	@Override
	public void onLocationChanged(Location currentLocation) {



        if (mLastLocationReading != null) {
        	if (currentLocation.getTime() < mLastLocationReading.getTime()) {
        		return;
        	}
        }
        
        mLastLocationReading = currentLocation;      
	}

	@Override
	public void onProviderDisabled(String provider) {
		// not implemented
	}

	@Override
	public void onProviderEnabled(String provider) {
		// not implemented
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// not implemented
	}

	// Returns age of location in milliseconds
	private long ageInMilliseconds(Location location) {
		return System.currentTimeMillis() - location.getTime();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.delete_badges:
			mAdapter.removeAllViews();
			return true;
		case R.id.place_one:
			mMockLocationProvider.pushLocation(37.422, -122.084);
			return true;
		case R.id.place_no_country:
			mMockLocationProvider.pushLocation(0, 0);
			return true;
		case R.id.place_two:
			mMockLocationProvider.pushLocation(38.996667, -76.9275);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void shutdownMockLocationManager() {
		if (mMockLocationOn) {
			mMockLocationProvider.shutdown();
		}
	}

	private void startMockLocationManager() {
		if (!mMockLocationOn) {
			mMockLocationProvider = new MockLocationProvider(
					LocationManager.NETWORK_PROVIDER, this);
		}
	}
}
