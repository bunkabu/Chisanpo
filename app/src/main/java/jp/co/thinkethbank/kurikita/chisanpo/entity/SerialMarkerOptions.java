package jp.co.thinkethbank.kurikita.chisanpo.entity;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.Serializable;

/** マーカーの情報を保持するためのクラス。永続化 */
public class SerialMarkerOptions implements Serializable {
    public String title;
    public double latitude;
    public double longitude;

    public SerialMarkerOptions(MarkerOptions options){
        title = options.getTitle();
        latitude = options.getPosition().latitude;
        longitude = options.getPosition().longitude;
    }

    /**
     * 持っている情報からMarkerOptionsを生成する
     * @return MarkerOptionを返す
     */
    public MarkerOptions createMarkerOptions(){
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.title(title);
        markerOptions.position(new LatLng(latitude, longitude));
        return markerOptions;
    }
}
