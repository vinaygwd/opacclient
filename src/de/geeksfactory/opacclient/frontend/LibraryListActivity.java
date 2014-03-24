package de.geeksfactory.opacclient.frontend;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.acra.ACRA;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.app.ListFragment;
import org.holoeverywhere.widget.ArrayAdapter;
import org.holoeverywhere.widget.ListView;

import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.storage.AccountDataSource;

public class LibraryListActivity extends Activity {

	public static final int LEVEL_COUNTRY = 0;
	public static final int LEVEL_STATE = 1;
	public static final int LEVEL_CITY = 2;
	public static final int LEVEL_LIBRARY = 3;

	protected List<Library> libraries;
	protected LibraryListFragment fragment;
	protected LibraryListFragment fragment2;
	protected LibraryListFragment fragment3;
	protected LibraryListFragment fragment4;
	protected boolean visible;

	@Override
	protected void onPause() {
		visible = false;
		super.onPause();
	}

	@Override
	protected void onResume() {
		visible = true;
		super.onResume();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(null);
		setContentView(R.layout.activity_library_list);
		getSupportActionBar().setHomeButtonEnabled(false);

		try {
			libraries = ((OpacClient) getApplication()).getLibraries();
		} catch (IOException e) {
			ACRA.getErrorReporter().handleException(e);
			return;
		}
		final TextView tvLocateString = (TextView) findViewById(R.id.tvLocateString);
		final ImageView ivLocationIcon = (ImageView) findViewById(R.id.ivLocationIcon);
		final LinearLayout llLocate = (LinearLayout) findViewById(R.id.llLocate);

		showListCountries(false);

		final LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_COARSE); // no GPS
		final String provider = locationManager.getBestProvider(criteria, true);
		if (provider == null) // no geolocation available
			llLocate.setVisibility(View.GONE);

		llLocate.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (fragment instanceof LocatedLibraryListFragment) {
					showListCountries(true);
					tvLocateString.setText(R.string.geolocate);
					ivLocationIcon.setImageResource(R.drawable.ic_locate);
				} else {
					tvLocateString.setText(R.string.geolocate_progress);
					ivLocationIcon.setImageResource(R.drawable.ic_locate);
					showListGeo();
				}
			}
		});
	}

	public void showListGeo() {
		final TextView tvLocateString = (TextView) findViewById(R.id.tvLocateString);
		final ImageView ivLocationIcon = (ImageView) findViewById(R.id.ivLocationIcon);

		final LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_COARSE); // no GPS
		final String provider = locationManager.getBestProvider(criteria, true);

		if (provider == null)
			return;
		locationManager.requestLocationUpdates(provider, 0, 0,
				new LocationListener() {
					@Override
					public void onStatusChanged(String provider, int status,
							Bundle extras) {
					}

					@Override
					public void onProviderEnabled(String provider) {
					}

					@Override
					public void onProviderDisabled(String provider) {
					}

					@Override
					public void onLocationChanged(Location location) {
						if (!visible)
							return;
						fragment = new LocatedLibraryListFragment();
						Bundle args = new Bundle();
						args.putInt("level", LEVEL_LIBRARY);
						fragment.setArguments(args);

						if (location != null) {
							double lat = location.getLatitude();
							double lon = location.getLongitude();
							// Calculate distances
							List<Library> distancedlibs = new ArrayList<Library>();
							for (Library lib : libraries) {
								float[] result = new float[1];
								double[] geo = lib.getGeo();
								if (geo == null)
									continue;
								Location.distanceBetween(lat, lon, geo[0],
										geo[1], result);
								lib.setGeo_distance(result[0]);
								distancedlibs.add(lib);
							}
							Collections.sort(distancedlibs,
									new DistanceComparator());
							distancedlibs = distancedlibs.subList(0, 20);

							LibraryAdapter adapter = new LibraryAdapter(
									LibraryListActivity.this,
									R.layout.listitem_library, R.id.tvTitle,
									distancedlibs);
							fragment.setListAdapter(adapter);
							getSupportFragmentManager()
									.beginTransaction()
									.addToBackStack(null)
									.setTransition(
											FragmentTransaction.TRANSIT_FRAGMENT_FADE)
									.replace(R.id.container, fragment).commit();
							if (fragment2 != null)
								getSupportFragmentManager().beginTransaction()
										.detach(fragment2).commit();
							if (fragment3 != null)
								getSupportFragmentManager().beginTransaction()
										.detach(fragment3).commit();
							if (fragment4 != null)
								getSupportFragmentManager().beginTransaction()
										.detach(fragment4).commit();

							tvLocateString.setText(R.string.alphabetic_list);
							ivLocationIcon.setImageResource(R.drawable.ic_list);
						}
					}
				});
	}

	public void showListCountries(boolean fade) {
		fragment = new LibraryListFragment();
		Bundle args = new Bundle();
		args.putInt("level", LEVEL_COUNTRY);
		fragment.setArguments(args);
		Set<String> data = new HashSet<String>();
		for (Library lib : libraries) {
			if (!data.contains(lib.getCountry())) {
				data.add(lib.getCountry());
			}
		}
		List<String> list = new ArrayList<String>(data);
		Collections.sort(list);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				R.layout.listitem_simple, R.id.text1,
				list.toArray(new String[] {}));
		fragment.setListAdapter(adapter);
		if (findViewById(R.id.llFragments) != null) {
			getSupportFragmentManager().beginTransaction()
					.replace(R.id.container, fragment).commit();
			if (fragment2 != null)
				getSupportFragmentManager().beginTransaction()
						.detach(fragment2).commit();
			if (fragment3 != null)
				getSupportFragmentManager().beginTransaction()
						.detach(fragment3).commit();
			if (fragment4 != null)
				getSupportFragmentManager().beginTransaction()
						.detach(fragment4).commit();
		} else {
			if (fade)
				getSupportFragmentManager()
						.beginTransaction()
						.setTransition(
								FragmentTransaction.TRANSIT_FRAGMENT_FADE)
						.replace(R.id.container, fragment).commit();
			else
				getSupportFragmentManager().beginTransaction()
						.replace(R.id.container, fragment).commit();
		}

	}

	public void showListStates(String country) {
		LibraryListFragment fragment = new LibraryListFragment();
		Bundle args = new Bundle();
		args.putInt("level", LEVEL_STATE);
		args.putString("country", country);
		fragment.setArguments(args);
		Set<String> data = new HashSet<String>();
		for (Library lib : libraries) {
			if (country.equals(lib.getCountry())
					&& !data.contains(lib.getState())) {
				data.add(lib.getState());
			}
		}
		List<String> list = new ArrayList<String>(data);
		if (data.size() == 1) {
			showListCities(country, list.get(0));
		}
		Collections.sort(list);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				R.layout.listitem_simple, R.id.text1, list);
		fragment.setListAdapter(adapter);
		if (findViewById(R.id.llFragments) != null) {
			fragment2 = fragment;
			getSupportFragmentManager().beginTransaction()
					.replace(R.id.container2, fragment2).commit();
			if (fragment3 != null)
				getSupportFragmentManager().beginTransaction()
						.detach(fragment3).commit();
			if (fragment4 != null)
				getSupportFragmentManager().beginTransaction()
						.detach(fragment4).commit();
		} else if (data.size() > 1) {
			this.fragment = fragment;
			getSupportFragmentManager().beginTransaction()
					.replace(R.id.container, fragment).addToBackStack(null)
					.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
					.commit();
		}
	}

	public void showListCities(String country, String state) {
		LibraryListFragment fragment = new LibraryListFragment();
		Bundle args = new Bundle();
		args.putInt("level", LEVEL_CITY);
		args.putString("country", country);
		args.putString("state", state);
		fragment.setArguments(args);
		Set<String> data = new HashSet<String>();
		for (Library lib : libraries) {
			if (country.equals(lib.getCountry())
					&& state.equals(lib.getState())
					&& !data.contains(lib.getCity())) {
				data.add(lib.getCity());
			}
		}
		List<String> list = new ArrayList<String>(data);
		if (data.size() == 1 && list.get(0).equals(state)) { // City states
			showListLibraries(country, state, list.get(0));
		}
		Collections.sort(list);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				R.layout.listitem_simple, R.id.text1, list);
		fragment.setListAdapter(adapter);
		if (findViewById(R.id.llFragments) != null) {
			fragment3 = fragment;
			getSupportFragmentManager().beginTransaction()
					.replace(R.id.container3, fragment3).commit();
			if (fragment4 != null)
				getSupportFragmentManager().beginTransaction()
						.detach(fragment4).commit();
		} else if (data.size() > 1) {
			this.fragment = fragment;
			getSupportFragmentManager().beginTransaction()
					.replace(R.id.container, fragment).addToBackStack(null)
					.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
					.commit();
		}
	}

	public void showListLibraries(String country, String state, String city) {
		LibraryListFragment fragment = new LibraryListFragment();
		Bundle args = new Bundle();
		args.putInt("level", LEVEL_LIBRARY);
		args.putString("country", country);
		args.putString("state", state);
		args.putString("city", city);
		fragment.setArguments(args);
		Set<Library> data = new HashSet<Library>();
		for (Library lib : libraries) {
			if (country.equals(lib.getCountry())
					&& state.equals(lib.getState())
					&& city.equals(lib.getCity()) && !data.contains(lib)) {
				data.add(lib);
			}
		}
		List<Library> list = new ArrayList<Library>(data);
		Collections.sort(list);
		LibraryAdapter adapter = new LibraryAdapter(this,
				R.layout.listitem_library_in_city, R.id.tvTitle, list);
		fragment.setListAdapter(adapter);
		if (findViewById(R.id.llFragments) != null) {
			fragment4 = fragment;
			getSupportFragmentManager().beginTransaction()
					.replace(R.id.container4, fragment4).commit();
		} else {
			this.fragment = fragment;
			getSupportFragmentManager().beginTransaction()
					.replace(R.id.container, fragment).addToBackStack(null)
					.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
					.commit();
		}
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class LibraryListFragment extends ListFragment {
		int level;

		@Override
		public void onListItemClick(ListView l, View v, int position, long id) {
			super.onListItemClick(l, v, position, id);
			switch (level) {
			case LEVEL_COUNTRY:
				((LibraryListActivity) getActivity())
						.showListStates((String) getListAdapter().getItem(
								position));
				break;
			case LEVEL_STATE:
				((LibraryListActivity) getActivity()).showListCities(
						getArguments().getString("country"),
						(String) getListAdapter().getItem(position));
				break;
			case LEVEL_CITY:
				((LibraryListActivity) getActivity()).showListLibraries(
						getArguments().getString("country"), getArguments()
								.getString("state"), (String) getListAdapter()
								.getItem(position));
				break;
			case LEVEL_LIBRARY:
				Library lib = (Library) getListAdapter().getItem(position);
				AccountDataSource data = new AccountDataSource(getActivity());
				data.open();
				Account acc = new Account();
				acc.setLibrary(lib.getIdent());
				acc.setLabel(getActivity().getString(
						R.string.default_account_name));
				long insertedid = data.addAccount(acc);
				data.close();

				((OpacClient) getActivity().getApplication())
						.setAccount(insertedid);

				Intent i = new Intent(getActivity(), AccountEditActivity.class);
				i.putExtra("id", insertedid);
				i.putExtra("adding", true);
				if (getActivity().getIntent().hasExtra("welcome"))
					i.putExtra("welcome", true);
				getActivity().startActivity(i);
				getActivity().finish();
				break;
			}
		}

		public LibraryListFragment() {
			super();
		}

		@Override
		public View onCreateView(org.holoeverywhere.LayoutInflater inflater,
				ViewGroup container, Bundle savedInstanceState) {
			level = getArguments().getInt("level", LEVEL_COUNTRY);
			setRetainInstance(true);
			return super.onCreateView(inflater, container, savedInstanceState);
		}
	}

	public static class LocatedLibraryListFragment extends LibraryListFragment {

		public LocatedLibraryListFragment() {
			super();
		}

	}

	public static class LibraryAdapter extends ArrayAdapter<Library> {

		private Context context;
		private int resource;

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			if (convertView == null) {
				view = LayoutInflater.from(context).inflate(resource, parent,
						false);
			} else {
				view = convertView;
			}
			Library item = getItem(position);

			TextView tvTitle = (TextView) view.findViewById(R.id.tvTitle);
			TextView tvSupport = (TextView) view.findViewById(R.id.tvSupport);
			tvTitle.setText(item.getTitle());
			tvSupport.setText(item.getSupport());
			if (view.findViewById(R.id.tvCity) != null) {
				TextView tvCity = (TextView) view.findViewById(R.id.tvCity);
				tvCity.setText(item.getCity());
			}
			return view;
		}

		public LibraryAdapter(Context context, int resource,
				int textViewResourceId, List<Library> objects) {
			super(context, resource, textViewResourceId, objects);
			this.context = context;
			this.resource = resource;
		}

	}

	public static class DistanceComparator implements Comparator<Library> {
		@Override
		public int compare(Library o1, Library o2) {
			return ((Float) o1.getGeo_distance()).compareTo(o2
					.getGeo_distance());
		}
	}

}
