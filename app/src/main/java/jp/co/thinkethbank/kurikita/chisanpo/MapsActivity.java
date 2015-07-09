package jp.co.thinkethbank.kurikita.chisanpo;

import android.content.res.AssetManager;
import android.graphics.Color;
import android.util.Xml;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Location;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AppKeyPair;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.CameraUpdate;

import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Comparator;

import jp.co.thinkethbank.kurikita.chisanpo.entity.DropboxUtils;
import jp.co.thinkethbank.kurikita.chisanpo.entity.SerialMarkerOptions;

/*
リソースの閲覧とか
http://androiddrawables.com/
*/


public class MapsActivity extends FragmentActivity implements GoogleApiClient.OnConnectionFailedListener, LocationListener, GoogleApiClient.ConnectionCallbacks {
    /** カメラアクティビティの識別子 */
    static final int REQUEST_CAPTURE_IMAGE = 100;
    private static final float RESIZE_PICTURE_RATE = 0.5f;

    private static final int MENU_SAVE = 0;
    private static final int MENU_LOAD = 1;
    private static final int MENU_TAKE_PHOTO = 2;
    private static final int MENU_MISC = 3;
    private static final int MENU_ADMIN_SUB_MENU = 4;

    private static final String SERVER_DATA_TYPE_INFORMATION = "information";
    private static final String SERVER_DATA_TYPE_POINT = "point";
    private static final String SERVER_DATA_TYPE_COMMENT = "comment";
    private static final String SERVER_DATA_TYPE_REFERENCE = "reference";

    private static final double VALUE_GOAL_RANGE = 900;

    /** memberId */
    private String user;
    private boolean isAdmin = false;

    /** ドロップボックスのAPI */
    private DropboxAPI<AndroidAuthSession> dropboxAPI;
    private DropboxUtils dropboxUtils;

    private GoogleMap gMap; // Might be null if Google Play services APK is not available.
    private GoogleApiClient googleApiClient = null;
    private TextView infoText;

    /** parse用 */
    ParseObject geoPosition;

    /** 立てたマーカーをリスト化して保持する */
    private ArrayList<SerialMarkerOptions> markerList;
    /** 現在位置を取得する頻度の設定 */
    private static final LocationRequest REQUEST = LocationRequest.create()
            .setInterval(20000) // 20 seconds
            .setFastestInterval(6000) // 6 seconds
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    /** 立てたフラグの本数 */
    private long flagCount;
    /** 前回の緯度 */
    private double oldLatitude;
    /** 前回の軽度 */
    private double oldLongitude;
    private LatLng keepGoal;

    private ArrayList<MarkerOptions> goals;
    private List<ParseObject> parseGoals;

    /** 神社リスト用 */
    private List<Refuge> mRefugeList = new ArrayList<Refuge>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        /**
         *画面をスリープにしない処理を追加
         */
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // ***** レイアウトの設定 *****
        ImageButton settingButton = (ImageButton)findViewById(R.id.settingButton);
        settingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openOptionsMenu();
            }
        });
        infoText = (TextView)findViewById(R.id.infoText);
        infoText.setVisibility(View.INVISIBLE);
        setUpMapIfNeeded();

        // ***** 各初期化処理 *****
        flagCount = 1;
        oldLatitude = 0;
        oldLongitude = 0;
        markerList = new ArrayList<>();
        setupParams();
        setupDropBox();
        setupParse();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        googleApiClient.connect();

        if(dropboxAPI.getSession().authenticationSuccessful()) {
            dropboxAPI.getSession().finishAuthentication();
            dropboxUtils.storeOauth2AccessToken(dropboxAPI.getSession().getOAuth2AccessToken());

            Toast.makeText(this, "Dropboxの認証に成功しました", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        googleApiClient.disconnect();
        saveParams();
        super.onStop();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, MENU_SAVE, Menu.NONE, "保存");
        menu.add(Menu.NONE, MENU_LOAD, Menu.NONE, "読込");
        menu.add(Menu.NONE, MENU_TAKE_PHOTO, Menu.NONE, "撮影");
        menu.add(Menu.NONE, MENU_MISC, Menu.NONE, "雑務");
        if(isAdmin){
            menu.add(Menu.NONE, MENU_ADMIN_SUB_MENU, Menu.NONE, "管理者メニュー");
        }
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
            case MENU_TAKE_PHOTO:
                if(markerList.size() != 0){
                    SerialMarkerOptions serialMarkerOptions = markerList.get(markerList.size() - 1);
                    reachPoint(0, new LatLng(serialMarkerOptions.latitude, serialMarkerOptions.longitude));
                }
                return true;
            case MENU_MISC:
                Toast.makeText(this, "今は何もないよ", Toast.LENGTH_SHORT).show();
                return true;
            case MENU_ADMIN_SUB_MENU:
                if(isAdmin) {
                    AlertDialog.Builder listDialog = new AlertDialog.Builder(this);
                    listDialog
                            .setTitle("管理者メニュー")
                            .setItems(new String[]{"ゴールの設定(工事中)", "アカウント削除して終了", "ズーム13.7", "ズーム20"}, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    switch(which){
                                        case 0:
                                            makeSetGoalDialog();
                                            break;
                                        case 1:
                                            // ローカルデータからユーザー情報を抹消
                                            ParseQuery<ParseObject> query = ParseQuery.getQuery("Members");
                                            query.fromLocalDatastore();
                                            query.findInBackground(new FindCallback<ParseObject>() {
                                                @Override
                                                public void done(List<ParseObject> list, ParseException e) {
                                                    if(e == null){
                                                        if(list.size() != 0){
                                                            ParseObject member = list.get(0);
                                                            member.unpinInBackground();
                                                            Toast.makeText(MapsActivity.this, "保存されているユーザー情報を削除しました", Toast.LENGTH_SHORT).show();
                                                            finish();
                                                        }else{
                                                            Toast.makeText(MapsActivity.this, "保存されているユーザー情報がありません", Toast.LENGTH_SHORT).show();
                                                        }
                                                    }
                                                }
                                            });
                                            break;
                                        case 2:
                                            gMap.moveCamera(CameraUpdateFactory.zoomTo(13.7f));
                                            break;
                                        case 3:
                                            gMap.moveCamera(CameraUpdateFactory.zoomTo(20));
                                            break;
                                    }
                                }
                            })
                            .show();
                }
                return true;
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // カメラ
        if(REQUEST_CAPTURE_IMAGE == requestCode && resultCode == Activity.RESULT_OK ){
            // 撮影した写真
            Bitmap takePic = (Bitmap)data.getExtras().get("data");
            takePic = BitmapEditor.resizePicture(takePic, 640);
            float takePicWidth = takePic.getWidth() * RESIZE_PICTURE_RATE;
            float takePicHeight = takePic.getHeight() * RESIZE_PICTURE_RATE;

            BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(takePic);
            // 貼り付ける設定
            GroundOverlayOptions options = new GroundOverlayOptions();
            options.image(descriptor);
            options.anchor(0.5f, 0.5f);
            options.position(keepGoal, takePicWidth, takePicHeight);
            // マップに貼り付け・アルファを設定
            GroundOverlay overlay = gMap.addGroundOverlay(options);
            // 透過率の設定
            overlay.setTransparency(0.3F);

            // ************ ここから写真アップロード ************
            byte[] jpegStream = BitmapEditor.compressJpeg(takePic, 80);
            if(jpegStream != null) {
                UploadPicture uploadPicture = new UploadPicture(this, dropboxAPI, String.valueOf(oldLatitude) + ".jpg",
                        new ByteArrayInputStream(jpegStream), jpegStream.length);
                uploadPicture.execute();
            }
        }
    }

    private void setup(){
        setGeoPosition();
        putGoal();
    }

    private void setupAdmin(){
        isAdmin = true;
    }

    /** 各パラメータの取得 */
    private void setupParams(){
        //params = PreferenceManager.getDefaultSharedPreferences(this);
        // user = params.getString(PARAMS_USER_NAME, null);
    }

    private void saveParams(){
        // SharedPreferences.Editor editor =  params.edit();
        if(user != null){
            // editor.putString(PARAMS_USER_NAME, user);

        }
        // editor.apply();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #gMap} is not null.
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

        if (gMap == null) {
            gMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
            if (gMap != null) {
                setUpMap();
            }
        }
    }

    /** グーグルマップの初期設定 */
    private void setUpMap() {
        gMap.setMyLocationEnabled(true);
        // カメラの初期位置の設定
        gMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                new CameraPosition(new LatLng(35.688224, 139.6985579), 15f, 0f, 0f)
        ));

        UiSettings settings = gMap.getUiSettings();
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

        mRefugeList.clear();
        parseXML();
        addMaker();

        Refuge refuge = mRefugeList.get(0);
        if (refuge != null) {
            CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(new LatLng(refuge.getLat(), refuge.getLng()), 15);
            gMap.moveCamera(cu);
        }
        gMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                calcDistance(latLng);
                sortRefugeList();
                updateMaker();
                addLine(latLng);
            }
        });
    }

    private void addLine(LatLng point){

        CircleOptions circleOptions = new CircleOptions()
                .center(point)
                        //.fillColor(Color.LTGRAY)
                .radius(3);
        gMap.addCircle(circleOptions);
        for (Refuge refuge : mRefugeList) {
            if (refuge != null) {
                if (refuge.isNear()) {
                    PolylineOptions polyOptions = new PolylineOptions();
                    polyOptions.add(point);
                    polyOptions.add(new LatLng(refuge.getLat(), refuge.getLng()));
                    polyOptions.color(Color.GRAY);
                    polyOptions.width(3);
                    polyOptions.geodesic(true); //true:大圏コース,false:直線
                    gMap.addPolyline(polyOptions);
                }
            }
        }
    }

    private void updateMaker() {
        int i = 0;
        gMap.clear();
        for (Refuge refuge : mRefugeList) {
            if (refuge != null) {
                MarkerOptions options = new MarkerOptions();
                options.position(new LatLng(refuge.getLat(), refuge.getLng()));
                options.title(refuge.getName() + " " + refuge.getDistance() + "m");
                options.snippet(refuge.getAddress());
                //アイコンを地井さんに設定する
                BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.mipmap.chi);
                if (i > 2) {
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA);
                    refuge.setNear(false);
                } else {
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
                    refuge.setNear(true);
                }
                options.icon(icon);
                Marker marker = gMap.addMarker(options);
                if (i == 0) {
                    marker.showInfoWindow();
                }
                i++;
            }
        }
    }
    private void sortRefugeList(){
        Collections.sort(mRefugeList, new Comparator<Refuge>() {
            @Override
            public int compare(Refuge lhs, Refuge rhs) {
                return lhs.getDistance() - rhs.getDistance();
            }
        });
    }
    private void calcDistance(LatLng point){
        // タッチした場所と避難所の距離を求める
        double startLat = point.latitude;
        double startLng = point.longitude;
        // 結果を格納するための配列
        float[] results = new float[3];
        for (Refuge refuge : mRefugeList) {
            if (refuge != null) {
                Location.distanceBetween(startLat, startLng, refuge.getLat(), refuge.getLng(), results);
                refuge.setDistance(results[0]);
            }
        }


    }
    private void parseXML() {
        // AssetManagerの呼び出し
        AssetManager assetManager = getResources().getAssets();
        try {

            // XMLファイルのストリーム情報を取得
            InputStream is = assetManager.open("refuge_nonoichi.xml");
            InputStreamReader inputStreamReader = new InputStreamReader(is);
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(inputStreamReader);
            String title="";
            String address="";
            String lat = "";
            String lon = "";

            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        String tag = parser.getName();
                        if ("marker".equals(tag)) {
                            title = parser.getAttributeValue(null,"title");
                            address = parser.getAttributeValue(null,"adress");
                            lat = parser.getAttributeValue(null,"lat");
                            lon = parser.getAttributeValue(null,"lng");
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        String endTag = parser.getName();
                        if ("marker".equals(endTag)) {
                            newRefuge(title, address,Double.valueOf(lat), Double.valueOf(lon));
                        }
                        break;
                    case XmlPullParser.TEXT:
                        break;
                }
                eventType = parser.next();
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    private void newRefuge(String title,String address,double lat,double lon) {
        Refuge refuge;
        refuge = new Refuge(title,address,lat,lon);
        mRefugeList.add(refuge);

    }
    private void addMaker() {
        for (Refuge refuge : mRefugeList) {
            if (refuge != null) {
                MarkerOptions options = new MarkerOptions();
                options.position(new LatLng(refuge.getLat(),refuge.getLng()));
                options.title(refuge.getName());
                options.snippet(refuge.getAddress());
                gMap.addMarker(options);
            }
        }
    }

    private void setupDropBox(){
        dropboxUtils = new DropboxUtils(this);

        if(!dropboxUtils.hasLoadAndroidAuthSession()){
            AndroidAuthSession session = new AndroidAuthSession(new AppKeyPair("flu2b0urtc18egm", "kroennf800x63fa"));
            dropboxAPI = new DropboxAPI<>(session);
            dropboxAPI.getSession().startOAuth2Authentication(this);
        }else{
            dropboxAPI = new DropboxAPI<>(dropboxUtils.loadAndroidAuthSession());
        }
    }

    private void setupParse(){
        Parse.enableLocalDatastore(this);
        Parse.initialize(this, "aXB8YYKBnz7JG6Jdhi2vhdjo2PkkdFDZU9CPCHYO", "Tl2OJP50LvPluTJ2sxvJpdazFebQRfB0sTsLT8wa");

        // ローカルデータストアからユーザー情報を取得
        final ParseQuery<ParseObject> query = ParseQuery.getQuery("Members");
        query.fromLocalDatastore();
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> memberList, ParseException e) {
                if (e == null) {
                    Log.d("score", "Retrieved " + memberList.size() + " scores");

                    // ローカルに保存されていな場合は登録
                    if (memberList.size() == 0) {
                        final EditText et = new EditText(MapsActivity.this);
                        et.setInputType(InputType.TYPE_CLASS_TEXT);
                        et.setHint("サイボウズのあれ");
                        final AlertDialog dialog = new AlertDialog.Builder(MapsActivity.this).setTitle("ユーザー名の入力").setView(et)
                                .setMessage("ユーザー名を入力してください")
                                .setCancelable(false)
                                .setPositiveButton("OK", null)
                                .show();

                        // OKボタンが押された時
                        Button button = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                        button.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String inputMember = et.getText().toString();
                                if (!inputMember.isEmpty()) {
                                    ParseQuery<ParseObject> query = ParseQuery.getQuery("Members");
                                    // 入力した名前の人がいるかどうか
                                    query.whereEqualTo("memberId", inputMember);
                                    query.findInBackground(new FindCallback<ParseObject>() {
                                        public void done(List<ParseObject> cloudMemberList, ParseException e) {
                                            if (e == null) {
                                                Log.d("score", "Retrieved " + cloudMemberList.size() + " scores");
                                                // 存在しない場合
                                                if (cloudMemberList.size() == 0) {
                                                    dialog.setMessage("存在しない名前です");
                                                } else {
                                                    ParseObject cloudMember = cloudMemberList.get(0);
                                                    user = (String) cloudMember.get("memberId");
                                                    // 権限者だった場合
                                                    if ((int) cloudMember.get("rank") == 9) {
                                                        setupAdmin();
                                                    }
                                                    cloudMember.pinInBackground();

                                                    dialog.dismiss();
                                                    Toast.makeText(MapsActivity.this, "ログインしました", Toast.LENGTH_SHORT).show();
                                                    setup();
                                                }
                                            } else {
                                                Log.d("member name", "Error: " + e.getMessage());
                                            }
                                        }
                                    });
                                }
                            }
                        });
                    } else {
                        ParseObject member = memberList.get(0);
                        user = (String) member.get("memberId");

                        if(member.get("rank") == null){
                            member.unpinInBackground();
                            finish();
                        }else {
                            // 権限者だった場合
                            if ((int) member.get("rank") == 9) {
                                setupAdmin();
                            }
                            Toast.makeText(MapsActivity.this, "ログインしました:" + user, Toast.LENGTH_SHORT).show();
                            setup();
                        }
                    }
                } else {
                    Log.d("score", "Error: " + e.getMessage());
                }
            }
        });
    }

    /** Parseの機能を利用してアプリを利用するユーザーの位置情報を保管するオブジェクトを生成 */
    private void setGeoPosition(){
        ParseQuery<ParseObject> query = ParseQuery.getQuery("GeoPosition");
        query.whereEqualTo("memberId", user);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> list, ParseException e) {
                if (list.size() != 0) {
                    geoPosition = list.get(0);
                } else {
                    Toast.makeText(MapsActivity.this, "Parseから位置情報データの取得に失敗しました:" + user, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    // TODO 仕様が大幅に変更なる
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
                        putGoal();
                    }
                });
        dialog.show();
    }

    private void refreshMap(){
        if(gMap == null)    return;

        // マップオブジェクトを取得していた場合はマーカーをセットする
        gMap.clear();
        for(SerialMarkerOptions smo : markerList){
            gMap.addMarker(smo.createMarkerOptions());
        }
        putGoal();
        flagCount = markerList.size() + 1;
    }

    private void putGoal(){
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Treasure");
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> list, ParseException e) {
                if(e == null) {
                    if(goals == null){
                        goals = new ArrayList<>();
                    }else{
                        goals.clear();
                    }

                    parseGoals = list;

                    for(ParseObject treasure : list){
                        ParseGeoPoint point = (ParseGeoPoint)treasure.get("position");
                        String title = (String)treasure.get("name");
                        int type = (int)treasure.get("typeId");
                        int resourceId;

                        switch(type){
                            case 1:
                                resourceId = R.drawable.ic_action_achievement_blue;
                                break;
                            case 2:
                                resourceId = R.drawable.ic_action_achievement_green;
                                break;
                            case 3:
                                resourceId = R.drawable.ic_action_achievement_red;
                                break;
                            default:
                                resourceId = R.drawable.ic_action_achievement_yellow;
                                break;
                        }

                        MarkerOptions marker = new MarkerOptions()
                                .position(new LatLng(point.getLatitude(), point.getLongitude()))
                                .title(title)
                                .icon(BitmapDescriptorFactory.fromResource(resourceId));
                        goals.add(marker);
                        gMap.addMarker(marker);
                    }


                    if (list.size() != 0) {
                        geoPosition = list.get(0);
                    } else {
                        Toast.makeText(MapsActivity.this, "Parseから位置情報データの取得に失敗しました:" + user, Toast.LENGTH_LONG).show();
                    }
                }else{
                    Toast.makeText(MapsActivity.this, "目的地データの取得に失敗しました", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    /**
     * 指定した位置にマーカーを作成する
     * @param latitude 緯度
     * @param longitude 軽度
     */
    private void putMarker(double latitude, double longitude){
        String title = String.valueOf(flagCount) + "本目";
        MarkerOptions options = new MarkerOptions();
        options.position(new LatLng(latitude, longitude));
        options.title(title);
        gMap.addMarker(options);
        markerList.add(new SerialMarkerOptions(options));
        flagCount++;
    }

    /**
     * 目標地点に到達した時
     * @param itemId 目標アイテムID
     * @param position 到達位置
     */
    private void reachPoint(int itemId, LatLng position){
        String goalTitle = null;
        keepGoal = position;

        if(parseGoals != null){
            for(ParseObject goal : parseGoals){
                if((int)goal.get("treasureId") == itemId){
                    goalTitle = (String)goal.get("name");
                }
            }
            if(goalTitle == null){
                goalTitle = "不明な目的地";
            }
        }else{
            goalTitle = "不明な目的地";
        }

        new AlertDialog.Builder(this)
                .setTitle("おめでとう！")
                .setMessage(goalTitle + "に到着しました")
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

    void setInfoText(String message){
        // infoText.setVisibility(View.VISIBLE);
        // infoText.setText(message);
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

            refreshMap();
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

        double diffLatitude = (latitude - oldLatitude) / 0.000008983148616;
        double diffLongitude = (longitude - oldLongitude) / 0.000010966382364;
        double length_2 = diffLatitude * diffLatitude + diffLongitude * diffLongitude;

        // あまり動いていない場合は処理を実行しない
        if(length_2 < 100){
            return;
        }else if(length_2 > 90000){
            length_2 = 90000;
        }

        float zoomCoefficient = 12f + (float)(60 / (Math.pow(length_2, 0.3) + 4d));

        Toast.makeText(this, location.toString() + "\nlength:" + length_2 + "\nzoom:" + zoomCoefficient, Toast.LENGTH_SHORT).show();
        CameraPosition currentPlace = new CameraPosition.Builder()
                .target(new LatLng(latitude, longitude)).zoom(zoomCoefficient)
                .bearing(location.getBearing()).build();
        gMap.animateCamera(CameraUpdateFactory.newCameraPosition(currentPlace));
        // マーカーの作成
        putMarker(latitude, longitude);

        // ***** サーバーに送信 *****
        if(geoPosition != null) {
            ParseGeoPoint pos = (ParseGeoPoint) geoPosition.get("position");
            pos.setLatitude(latitude);
            pos.setLongitude(longitude);
            geoPosition.saveInBackground();
        }

        // ***** 目的地到着判定 *****
        if(goals != null){
            for(int i = 0; i < goals.size(); i++){
                MarkerOptions goal = goals.get(i);

                double diffGoalLatitude = (goal.getPosition().latitude - latitude) * 100000;
                double diffGoalLongitude = (goal.getPosition().longitude - longitude) * 100000;

                if (diffGoalLatitude * diffGoalLatitude + diffGoalLongitude * diffGoalLongitude < VALUE_GOAL_RANGE) {
                    reachPoint((int)parseGoals.get(i).get("treasureId"), goal.getPosition());
                }
            }
        }

        oldLatitude = latitude;
        oldLongitude = longitude;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(this, "なんかGoogleMapへの接続に失敗しました", Toast.LENGTH_LONG).show();
    }

    /** TODO サーバーから情報を受け取った時に呼ばれる
     * {"status" : "true",
     *  "data" : [{"type" : "information", "division" : "news", "message" : "アイテム発見！"},
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

                for(int i = 0; i < serverData.length(); i++) {
                    JSONObject block = serverData.getJSONObject(i);

                    String type = block.getString("type");
                    switch (type) {
                        case SERVER_DATA_TYPE_INFORMATION: {
                            String division = block.getString("division");
                            String message = block.getString("message");

                            if(division == null){
                                setInfoText("ERROR : " + message);
                            }else {
                                setInfoText(message);
                            }
                            break;
                        }
                        case SERVER_DATA_TYPE_POINT: {
                            JSONArray points = block.getJSONArray("points");

                            break;
                        }
                        case SERVER_DATA_TYPE_COMMENT: {
                            JSONArray comments = block.getJSONArray("comments");

                            break;
                        }
                        case SERVER_DATA_TYPE_REFERENCE: {
                            String division = block.getString("division");
                            String value = block.getString("value");
                            break;
                        }
                        default:
                            Toast.makeText(MapsActivity.this, "不明コマンドタイプ:" + type, Toast.LENGTH_SHORT).show();
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(MapsActivity.this, "Jsonのパースに失敗しました", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
