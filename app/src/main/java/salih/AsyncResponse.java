package salih;

import com.google.android.gms.maps.model.LatLng;

import java.util.HashMap;
import java.util.List;

/**
 * Created by sceli on 7.05.2017.
 */

public interface AsyncResponse {
    void onProcessStart();
    void onProcessFinish(List<HashMap<String, String>> nearbyPlacesMapList);
}
