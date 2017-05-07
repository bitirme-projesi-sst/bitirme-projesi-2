package salih;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;

import java.util.HashMap;
import java.util.List;

/**
 * Created by sceli on 7.05.2017.
 */

public class NearbyNew {
    private AsyncResponse asyncResponseListener;
    private NearbyNew2 nearbyNew2;
    private Object[] params;
    private LatLng origin;
    private String googlePlacesData;
    private String url;
    private GoogleMap mMap;

    public NearbyNew(AsyncResponse asyncResponseListener, LatLng origin, Object... params) {
        this.asyncResponseListener = asyncResponseListener;
        this.origin = origin;
        this.params = params;
    }

    public void execute() {
        if(nearbyNew2 != null){
            nearbyNew2.cancel(true);
        }
        nearbyNew2 = new NearbyNew2();
        nearbyNew2.execute(params);
    }


    public class NearbyNew2 extends AsyncTask<Object, String, String> {

        private String googlePlacesData;
        private String url;
        private GoogleMap mMap;
        private LatLng origin;
        private LatLng destination;


        @Override
        protected String doInBackground(Object... params) {
            try {
                Log.d("GetNearbyPlacesData", "doInBackground entered");
                mMap = (GoogleMap) params[0];
                url = (String) params[1];
                DownloadUrl downloadUrl = new DownloadUrl();
                googlePlacesData = downloadUrl.readUrl(url);
                Log.d("GooglePlacesReadTask", "doInBackground Exit");
            } catch (Exception e) {
                Log.d("GooglePlacesReadTask", e.toString());
            }
            return googlePlacesData;
        }

        @Override
        protected void onPostExecute(String result) {
            Log.d("GooglePlacesReadTask", "onPostExecute Entered");
            List<HashMap<String, String>> nearbyPlacesList = null;
            DataParser dataParser = new DataParser();
            nearbyPlacesList = dataParser.parse(result);
            asyncResponseListener.onProcessFinish(nearbyPlacesList);
        }
    }

}
