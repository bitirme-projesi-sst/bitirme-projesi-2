package modules;


import android.os.AsyncTask;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.jsoup.Jsoup.parse;


public class DirectionFinder {
    private static final String DIRECTION_URL_API = "https://maps.googleapis.com/maps/api/directions/json?";
    private static final String GOOGLE_API_KEY = "AIzaSyAew7I6t4QHkRfqzLI8Kz8QSAxhLUNRzPg";
    private DirectionFinderListener listener;
    private LatLng origin;
    private LatLng destination;

    public DirectionFinder(DirectionFinderListener listener, LatLng origin, LatLng destination) {
        this.listener = listener;
        this.origin = origin;
        this.destination = destination;
    }

    public void execute() throws UnsupportedEncodingException {
        listener.onDirectionFinderStart();
        new DownloadRawData().execute(createUrl());
    }

    private String createUrl() throws UnsupportedEncodingException {
        //String urlOrigin = URLEncoder.encode(origin, "utf-8");
        //String urlDestination = URLEncoder.encode(destination, "utf-8");

        //return DIRECTION_URL_API + "origin=" + origin + "&destination=" + destination + "&key=" + GOOGLE_API_KEY;
        //return DIRECTION_URL_API + "origin=41.063617,28.991122&destination=41.025688,29.045044&mode=transit&language=tr&key=" + GOOGLE_API_KEY;   //MECİDİYE
//maps.googleapis.com/maps/api/directions/json?origin=41.006141,%2028.796586&destination=41.000650,%2028.808677&mode=transit&key=AIzaSyAew7I6t4QHkRfqzLI8Kz8QSAxhLUNRzPg
        return DIRECTION_URL_API + "origin=41.017030,28.790306&destination=40.978689,29.078858&mode=transit&language=tr&key=" + GOOGLE_API_KEY; //https://www.google.com.tr/maps/dir/41.006776,+28.796066/41.004929,+28.800251/@41.0058312,28.7971383,18z/am=t/data=!4m9!4m8!1m3!2m2!1d28.796066!2d41.006776!1m3!2m2!1d28.800251!2d41.004929
    }

    private class DownloadRawData extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            String link = params[0];
            try {
                URL url = new URL(link);
                InputStream is = url.openConnection().getInputStream();
                StringBuilder builder = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));

                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append("\n");
                }
                return builder.toString();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String res) {
            try {
                parseJSon(res);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void parseJSon(String data) throws JSONException {
        ArrayList<String> allSteps = new ArrayList<>();
        if (data == null)
            return;

        List<Route> routes = new ArrayList<Route>();
        JSONObject jsonData = new JSONObject(data);
        JSONArray jsonRoutes = jsonData.getJSONArray("routes");
        for (int i = 0; i < jsonRoutes.length(); i++) {
            JSONObject jsonRoute = jsonRoutes.getJSONObject(i);
            Route route = new Route();

            JSONObject overview_polylineJson = jsonRoute.getJSONObject("overview_polyline");
            JSONArray jsonLegs = jsonRoute.getJSONArray("legs");
            JSONObject jsonLeg = jsonLegs.getJSONObject(0);
            JSONArray jsonSteps = jsonLeg.getJSONArray("steps");
            for (int j = 0; j < jsonSteps.length(); j++) {
                JSONObject jsonStep = jsonSteps.getJSONObject(j);
                String instruction = getInstructions(jsonStep);
                allSteps.add(parse(instruction).text() + "\n\n");
                if (jsonStep.has("steps")) {
                    JSONArray jsonInnerSteps = jsonStep.getJSONArray("steps");
                    for (int z = 0; z < jsonInnerSteps.length(); z++) {
                        JSONObject jsonInnerStep = jsonInnerSteps.getJSONObject(z);
                        String e = getInstructions(jsonInnerStep);
                        allSteps.add(parse(e).text() + "\n\n");
                    }
                }
            }
            JSONObject jsonStep = jsonSteps.getJSONObject(0);
            JSONObject jsonDistance = jsonLeg.getJSONObject("distance");
            JSONObject jsonDuration = jsonLeg.getJSONObject("duration");
            JSONObject jsonEndLocation = jsonLeg.getJSONObject("end_location");
            JSONObject jsonStartLocation = jsonLeg.getJSONObject("start_location");
            route.distance = new Distance(jsonDistance.getString("text"), jsonDistance.getInt("value"));
            route.duration = new Duration(jsonDuration.getString("text"), jsonDuration.getInt("value"));
            route.endAddress = jsonLeg.getString("end_address");
            route.startAddress = jsonLeg.getString("start_address");
            route.startLocation = new LatLng(jsonStartLocation.getDouble("lat"), jsonStartLocation.getDouble("lng"));
            route.endLocation = new LatLng(jsonEndLocation.getDouble("lat"), jsonEndLocation.getDouble("lng"));
            route.points = decodePolyLine(overview_polylineJson.getString("points"));
            route.steps = new Steps(allSteps);

            routes.add(route);
        }

        listener.onDirectionFinderSuccess(routes);
    }

    private String getInstructions(JSONObject jsonStep) throws JSONException {
        String result = "";
        if (jsonStep.getString("travel_mode").equalsIgnoreCase("TRANSIT")) {
            JSONObject jsonTransitDetails = jsonStep.getJSONObject("transit_details");
            JSONObject jsonTransitLine = jsonTransitDetails.getJSONObject("line");
            String busName;
            if (jsonTransitLine.has("short_name")) {
                busName = jsonTransitLine.getString("short_name");
            } else {
                busName = jsonTransitLine.getString("name");
            }
            String busStops = jsonTransitDetails.getString("num_stops");
            result = busName + " otobüsü ile " + busStops + " durak gidiniz.";
        } else {
            String distance = jsonStep.getJSONObject("distance").getString("text");
            String instruction;
            if (jsonStep.has("html_instructions")) {
                instruction = jsonStep.getString("html_instructions");
                result = distance + " sonra " + instruction;

            } else {
                instruction = "ilerleyiniz.";
                result = distance + " boyunca ilerleyiniz.";

            }

        }
        return result;
    }

    private List<LatLng> decodePolyLine(final String poly) {
        int len = poly.length();
        int index = 0;
        List<LatLng> decoded = new ArrayList<LatLng>();
        int lat = 0;
        int lng = 0;

        while (index < len) {
            int b;
            int shift = 0;
            int result = 0;
            do {
                b = poly.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = poly.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            decoded.add(new LatLng(
                    lat / 100000d, lng / 100000d
            ));
        }

        return decoded;
    }
}