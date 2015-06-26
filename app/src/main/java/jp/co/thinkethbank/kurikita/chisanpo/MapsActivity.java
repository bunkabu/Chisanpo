package jp.co.thinkethbank.kurikita.chisanpo;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.location.Location;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import jp.co.thinkethbank.kurikita.chisanpo.entity.SerialMarkerOptions;

public class MapsActivity extends FragmentActivity implements GoogleApiClient.OnConnectionFailedListener, LocationListener, GoogleApiClient.ConnectionCallbacks {
    /** カメラアクティビティの識別子 */
    static final int REQUEST_CAPTURE_IMAGE = 100;
    private static final float RESIZE_PICTURE_RATE = 0.5f;

    private static final int MENU_SAVE = 0;
    private static final int MENU_LOAD = 1;
    private static final int MENU_SET_GOAL = 2;
    private static final int MENU_TAKE_PHOTO = 3;

    private static final double VALUE_GOAL_RANGE = 900;

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private GoogleApiClient googleApiClient = null;
    /** 立てたマーカーをリスト化して保持する */
    private ArrayList<SerialMarkerOptions> markerList;

    private static final LocationRequest REQUEST = LocationRequest.create()
            .setInterval(20000) // 20 seconds
            .setFastestInterval(333) // 33ms = 30fps
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    /** 立てたフラグの本数 */
    private long flagCount;
    /** 前回の緯度 */
    private double oldLatitude;
    /** 前回の軽度 */
    private double oldLongitude;
    // TODO 後で消すよ
    private LatLng goal;
    private LatLng keepGoal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();

        // ***** 各初期化処理 *****
        flagCount = 1;
        oldLatitude = 0;
        oldLongitude = 0;
        markerList = new ArrayList<>();
        putGoal(35.6647104, 139.7435281);
    }

    @Override
    protected void onResume() {
        super.onResume();
        googleApiClient.connect();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        googleApiClient.disconnect();
        super.onStop();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, MENU_SAVE, Menu.NONE, "保存");
        menu.add(Menu.NONE, MENU_LOAD, Menu.NONE, "読込");
        menu.add(Menu.NONE, MENU_SET_GOAL, Menu.NONE, "目的地設定");
        menu.add(Menu.NONE, MENU_TAKE_PHOTO, Menu.NONE, "撮影");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_SAVE:
                saveMarkerList();
                return true;
            case MENU_LOAD:
                loadMarkerList();
                return true;
            case MENU_SET_GOAL:
                makeSetGoalDialog();
                return true;
            case MENU_TAKE_PHOTO:
                if(markerList.size() != 0){
                    SerialMarkerOptions serialMarkerOptions = markerList.get(markerList.size() - 1);
                    reachPoint(0, new LatLng(serialMarkerOptions.latitude, serialMarkerOptions.longitude));
                }
                return true;
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(REQUEST_CAPTURE_IMAGE == requestCode && resultCode == Activity.RESULT_OK ){
            // 撮影した写真
            Bitmap takePic = (Bitmap)data.getExtras().get("data");
            takePic = resizePicture(takePic, 480);
            float takePicWidth = takePic.getWidth() * RESIZE_PICTURE_RATE;
            float takePicHeight = takePic.getHeight() * RESIZE_PICTURE_RATE;

            BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(takePic);
            // 貼り付ける設定
            GroundOverlayOptions options = new GroundOverlayOptions();
            options.image(descriptor);
            options.anchor(0.5f, 0.5f);
            options.position(keepGoal, takePicWidth, takePicHeight);
            // マップに貼り付け・アルファを設定
            GroundOverlay overlay = mMap.addGroundOverlay(options);
            // 透過率の設定
            overlay.setTransparency(0.3F);
        }
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    private void setUpMap() {
        mMap.setMyLocationEnabled(true);
        UiSettings settings = mMap.getUiSettings();
        // コンパスの有効化
        settings.setCompassEnabled(true);
        // 現在位置に移動するボタンの有効化
        settings.setMyLocationButtonEnabled(true);
        // 回転ジェスチャーの有効化
        settings.setRotateGesturesEnabled(true);
        // スクロールジェスチャーの有効化
        settings.setScrollGesturesEnabled(true);
        // ズームジェスチャー(ピンチイン・アウト)の有効化
        settings.setZoomGesturesEnabled(true);
    }

    private void makeSetGoalDialog(){
        final EditText editText1 = new EditText(this);
        editText1.setInputType(InputType.TYPE_CLASS_NUMBER);
        editText1.setHint("緯度");
        final EditText editText2 = new EditText(this);
        editText2.setInputType(InputType.TYPE_CLASS_NUMBER);
        editText2.setHint("経度");
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.addView(editText1, new LinearLayout.LayoutParams(300, ViewGroup.LayoutParams.WRAP_CONTENT));
        layout.addView(editText2, new LinearLayout.LayoutParams(300, ViewGroup.LayoutParams.WRAP_CONTENT));

        AlertDialog.Builder dialog = new AlertDialog.Builder(this).setTitle("目的地の設定").setView(layout)
                .setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String s1 = editText1.getText().toString();
                        String s2 = editText2.getText().toString();

                        if(s1.isEmpty() || s2.isEmpty()){
                            return;
                        }
                        putGoal(Double.parseDouble(s1), Double.parseDouble(s2));
                    }
                });
        dialog.show();
    }

    private void updateMap(){
        if(mMap == null)    return;

        mMap.addMarker(new MarkerOptions()
                .position(goal)
                .title("ゴールだよ！")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_goal)));
        // マップオブジェクトを取得していた場合はマーカーをセットする
        mMap.clear();
        for(SerialMarkerOptions smo : markerList){
            mMap.addMarker(smo.createMarkerOptions());
        }
        flagCount = markerList.size() + 1;
    }

    private void putGoal(double latitude, double longitude){
        goal = new LatLng(latitude, longitude);
        mMap.addMarker(new MarkerOptions()
                .position(goal)
                .title("ゴールだよ！")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_goal)));
    }

    /**
     * 指定した位置にマーカーを作成する
     * @param latitude 緯度
     * @param longitude 軽度
     */
    private void putFlag(double latitude, double longitude){
        String title = String.valueOf(flagCount) + "本目";
        MarkerOptions options = new MarkerOptions();
        options.position(new LatLng(latitude, longitude));
        options.title(title);
        mMap.addMarker(options);
        markerList.add(new SerialMarkerOptions(options));
        flagCount++;
    }

    /**
     * 目標地点に到達した時
     * @param itemId 目標のID
     * @param position 到達位置
     */
    private void reachPoint(int itemId, LatLng position){
        keepGoal = position;
        goal = null;
        new AlertDialog.Builder(this)
                .setTitle("おめでとう！！")
                .setMessage("到着だよ")
                .setPositiveButton("そうだよ", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setNegativeButton("記念写真撮るよ", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent( MediaStore.ACTION_IMAGE_CAPTURE);
                        startActivityForResult(intent, REQUEST_CAPTURE_IMAGE);
                    }
                })
                .show();
    }

    /**
     * Bitmapをリサイズする
     * @param src スケーリングするBitmap
     * @param shortLength ソース画像の短い方の辺のスケーリング後のサイズ
     * @return リサイズ後のBitmap
     */
    private Bitmap resizePicture(Bitmap src, int shortLength){
        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();
        float scaleRate;

        if(srcWidth < srcHeight){
            scaleRate = (float)shortLength / srcWidth;
        }else{
            scaleRate = (float)shortLength / srcHeight;
        }
        Matrix matrix = new Matrix();
        matrix.postScale(scaleRate, scaleRate);

        return Bitmap.createBitmap(src, 0, 0, srcWidth, srcHeight, matrix, true);
    }

    private void saveMarkerList(){
        try {
            FileOutputStream fos = openFileOutput("LocalList.dat", MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(markerList);
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void loadMarkerList(){
        try {
            FileInputStream fis = openFileInput("LocalList.dat");
            ObjectInputStream ois = new ObjectInputStream(fis);
            markerList = (ArrayList<SerialMarkerOptions>)ois.readObject();
            ois.close();

            updateMap();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, REQUEST, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        double diffLatitude = (latitude - oldLatitude) * 100000;
        double diffLongitude = (longitude - oldLongitude) * 100000;

        // あまり動いていない場合は処理を実行しない
        if(diffLatitude * diffLatitude + diffLongitude * diffLongitude < 100){
            return;
        }

        Toast.makeText(this, location.toString(), Toast.LENGTH_SHORT).show();
        CameraPosition currentPlace = new CameraPosition.Builder()
                .target(new LatLng(latitude, longitude)).zoom(15.5f)
                .bearing(location.getBearing()).build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(currentPlace));
        // マーカーの作成
        putFlag(latitude, longitude);

        // ***** 目的地到着判定 *****
        if(goal != null) {
            double diffGoalLatitude = (goal.latitude - latitude) * 100000;
            double diffGoalLongitude = (goal.longitude - longitude) * 100000;
            if (diffGoalLatitude * diffGoalLatitude + diffGoalLongitude * diffGoalLongitude < VALUE_GOAL_RANGE) {
                reachPoint(0, goal);
            }
        }

        oldLatitude = latitude;
        oldLongitude = longitude;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    /** TODO サーバーから情報を受け取った時に呼ばれる
     * {"status" : "true",
     *  "data" : [{"type" : "information", "division" : "news", "value" : "アイテム発見！"},
     *            {"type" : "point", "points" : [{"team" : "honda", "lat" : "35.55555", "long" : "132.33333"},
     *                                           {"team" : "nike", "lat" : "35.55555", "long" : "132.33333"}]},
     *            {"type" : "comment", "comments" : []},
     *            {"type" : "reference", "division" : "item", "value" : "true"}]} */
    private class InformationReceive implements ServerCommunicate.OnReceiveFromServerCallback{
        @Override
        public void receiveCalc(String result) {
            try {
                JSONObject json = new JSONObject(result);
                if(!json.getBoolean("status")){
                    Toast.makeText(MapsActivity.this, "Jsonのパースに失敗しました", Toast.LENGTH_SHORT).show();
                    return;
                }
                JSONArray serverData = json.getJSONArray("data");

            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(MapsActivity.this, "Jsonのパースに失敗しました", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
