package com.moac.android.refuge.util;

import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.moac.android.refuge.database.RefugeeDataStore;
import com.moac.android.refuge.model.Country;
import com.moac.android.refuge.model.RefugeeFlow;

import java.util.List;
import java.util.Map;

public class Visualizer {

    private static final String TAG = Visualizer.class.getSimpleName();

    private static final long MAX_RADIUS = 1500000; // 1500 kms
    private static final double MAX_AREA = 3.14 * MAX_RADIUS * MAX_RADIUS;

    private static final int[] FILL_COLORS = {0x660066cc, 0x66D6B331, 0x66663399, 0x55FF6600,
            0x66669900};

    private static final int[] STROKE_COLORS = {0xDD0066cc, 0xFFD6B331, 0xDD663399, 0xFFFF6600,
            0xDD669900};

    public static void drawCountries(RefugeeDataStore refugeeDataStore, GoogleMap map, List<Long> countryIds
            , Map<Long, Integer> colorMap, double scaling) {
        Log.d(TAG, "drawCountries() - Draw TO countries: " + countryIds);

        int colorIndex = 0;
        // Maximum radius is defined
        for (Long countryId : countryIds) {
            int strokeColor = STROKE_COLORS[colorIndex];
            int fillColor = FILL_COLORS[colorIndex];
            colorMap.put(countryId, strokeColor);
            drawAllFromCircles(refugeeDataStore, map, countryId, strokeColor, scaling);
            drawToCircle(refugeeDataStore,
                    map,
                    countryId,
                    strokeColor, fillColor,
                    (refugeeDataStore.getTotalRefugeeFlowTo(countryId) / scaling));
            colorIndex++;
        }
        Log.d(TAG, "drawCountries() - Using: " + scaling);
    }

    private static void drawAllFromCircles(RefugeeDataStore refugeeDataStore, GoogleMap map, long toCountryId, int strokeColor, double maxCount) {
        Log.d(TAG, "drawAllFromCircles() - toCountryId: " + toCountryId + " toCountryColor: " + strokeColor + " maxFlow: " + maxCount);
        List<RefugeeFlow> flows = refugeeDataStore.getRefugeeFlowsTo(toCountryId);
        for (RefugeeFlow flow : flows) {
            Country fromCountry = refugeeDataStore.getCountry(flow.getFromCountry().getId());
            Log.d(TAG, "drawAllFromCircles() - Drawing flow from: " + fromCountry.getName() + " count: " + flow.getRefugeeCount() + " / " + maxCount);
            drawScaledCircle(map, fromCountry.getLatLng(), (flow.getRefugeeCount() / maxCount), strokeColor, 0);
        }
    }

    private static void drawToCircle(RefugeeDataStore refugeeDataStore, GoogleMap map, long toCountryId, int strokeColor, int fillColor, double percent) {
        Country toCountry = refugeeDataStore.getCountry(toCountryId);
        drawScaledCircle(map, toCountry.getLatLng(), percent, strokeColor, fillColor);
    }

    public static Circle drawScaledCircle(GoogleMap map,
                                          LatLng coordinates,
                                          double percent,
                                          int strokeColor, int fillColor) {
        Log.d(TAG, "drawScaledCircle() - percent: " + percent);
        double circleArea = percent * MAX_AREA;
        double radius = Math.sqrt(circleArea / 3.14);
        Log.d(TAG, "drawScaledCircle() - radius (m): " + radius + " circleArea: " + circleArea + " percent: " + percent + " max Area: " + MAX_AREA);
        CircleOptions circleOptions = new CircleOptions()
                .center(coordinates)
                .radius(radius)
                .fillColor(fillColor)
                .strokeColor(strokeColor)
                .strokeWidth(5);
        return map.addCircle(circleOptions);
    }
}
