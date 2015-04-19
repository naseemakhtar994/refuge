package com.moac.android.refuge.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.moac.android.refuge.R;
import com.moac.android.refuge.RefugeApplication;
import com.moac.android.refuge.database.RefugeeDataStore;
import com.moac.android.refuge.fragment.NavigationDrawerFragment;
import com.moac.android.refuge.importer.DataFileImporter;
import com.moac.android.refuge.importer.LoadDataRunnable;
import com.moac.android.refuge.model.Country;
import com.moac.android.refuge.model.DisplayedStore;
import com.moac.android.refuge.util.DoOnce;
import com.moac.android.refuge.util.Visualizer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import rx.Observable;
import rx.functions.Action1;

;

public class MainActivity extends Activity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks,
        NavigationDrawerFragment.FragmentContainer {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String LOAD_DATA_TASK_TAG = "LOAD_DATA_TASK";
    private static final String DISPLAYED_COUNTRIES_KEY = "DISPLAYED_COUNTRIES";

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    @Inject
    RefugeeDataStore mRefugeeDataStore;

    private GoogleMap mMapFragment;
    private SearchView mSearchView;
    private DisplayedStore mDisplayedCountriesStore;
    private Map<Long, Integer> mColorMap = new HashMap<Long, Integer>();
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RefugeApplication.from(this).inject(this);
        mDisplayedCountriesStore = new DisplayedStore();
        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.drawable.ic_drawer, R.string.open_drawer_hint, R.string.close_drawer_hint) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);


        // Get a reference to the Map Fragment
        mMapFragment = ((MapFragment) getFragmentManager()
                .findFragmentById(R.id.map)).getMap();

        // We are using single top mode, so this will not contain
        // search intents as the SearchView is operating on its host
        // Activity - instead see onNewIntent()
        handleIntent(getIntent());

        // TODO This should be cleaned up. Import pre-populated or do in background
        String assetFile = "UNDataExport2012.xml";
        String countriesLatLongFile = "CountriesLatLong.csv";

        try {
            boolean attemptedToLoad = DoOnce.doOnce(this, LOAD_DATA_TASK_TAG, new LoadDataRunnable(new DataFileImporter(mRefugeeDataStore), getAssets().open(assetFile), getAssets().open(countriesLatLongFile)));
            Log.i(TAG, "Attempted to load data: " + attemptedToLoad);
        } catch (IOException e) {
            Log.e(TAG, "Failed to open the data file: " + assetFile, e);
            finish();
        }

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    private Observable<List<Long>> initCountryList(Bundle savedInstanceState) {
        List<Long> countryIdList = new ArrayList<Long>(); //Collections.<Long>emptyList();
        if (savedInstanceState != null) {
            long[] countryIds = savedInstanceState.getLongArray(DISPLAYED_COUNTRIES_KEY);
            if (countryIds != null) {
                for (long id : countryIds) {
                    countryIdList.add(id);
                }
                return Observable.just(countryIdList);
            }
        }
        return Observable.empty();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mDisplayedCountriesStore.getDisplayedCountries().subscribe(new Action1<List<Long>>() {
            @Override
            public void call(List<Long> countryIds) {
                mMapFragment.clear();
                double scaling = 0.0;
                // TODO This should be moved to background
                for (Long id : countryIds) {
                    // TODO Surely there's a build-in function for this
                    scaling = Math.max(scaling, mRefugeeDataStore.getTotalRefugeeFlowTo(id));
                }
                Visualizer.drawCountries(mRefugeeDataStore, mMapFragment, countryIds, mColorMap, scaling);
            }
        });
    }

    @Override
    public void onCountryItemSelected(long countryId, boolean isSelected) {
        // TODO
    }

    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(R.string.app_name);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);

            // Get the SearchView and set the searchable configuration
            SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            mSearchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
            mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            mSearchView.setIconifiedByDefault(true);
//            mSearchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
//                @Override
//                public void onFocusChange(View v, boolean hasFocus) {
//                    if (v == mSearchView && !hasFocus) mSearchView.setIconified(true);
//                }
//            });
            // Note: I don't register callbacks to invoke the search query - use the Intents instead.

            restoreActionBar();
            return true;
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        int id = item.getItemId();
        if (id == R.id.action_about) {
            return true;
        } else if (id == R.id.action_clear) {
            mDisplayedCountriesStore.clear();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onNewIntent(Intent _intent) {
        Log.i(TAG, "onNewIntent - received intent");
        handleIntent(_intent);
    }

    private void handleIntent(Intent _intent) {
        if (Intent.ACTION_SEARCH.equals(_intent.getAction())) {
            String query = _intent.getStringExtra(SearchManager.QUERY).trim();
            // TODO Enforce limits some how
//            if (mDisplayedCountriesStore. == 5) {
//                Toast.makeText(this, "Displaying max number of countries on map.", Toast.LENGTH_SHORT).show();
//                // add more colors!
//                return;
//            }
            Country country = mRefugeeDataStore.getCountry(query);
            if (country != null) {
                addCountry(country);
            } else {
                Toast.makeText(this, "Country not found", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void addCountry(Country country) {
        // TODO Add to Observable
        mDisplayedCountriesStore.add(country.getId());
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        // TODO
//        if (mDisplayedCountriesStore != null) {
//            long[] displayedCountries = new long[mDisplayedCountriesStore.size()];
//            for (int i = 0; i < mDisplayedCountriesStore.size(); i++) {
//                displayedCountries[i] = mDisplayedCountriesStore.get(i);
//            }
//            outState.putLongArray(DISPLAYED_COUNTRIES_KEY, displayedCountries);
//        }
    }

    @Override
    public Observable<List<Long>> getDisplayedCountries() {
        return mDisplayedCountriesStore.getDisplayedCountries();
    }

    @Override
    public Map<Long, Integer> getColorMap() {
        return mColorMap;
    }

    @Override
    public RefugeeDataStore getRefugeeDataStore() {
        return mRefugeeDataStore;
    }
}
