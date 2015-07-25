package jp.co.thinkethbank.kurikita.chisanpo;

import android.app.ProgressDialog;
import android.content.res.AssetManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Xml;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
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
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.CameraUpdate;

import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import jp.co.thinkethbank.kurikita.chisanpo.bean.Event;
import jp.co.thinkethbank.kurikita.chisanpo.bean.Group;
import jp.co.thinkethbank.kurikita.chisanpo.bean.Member;
import jp.co.thinkethbank.kurikita.chisanpo.bean.Treasure;
import jp.co.thinkethbank.kurikita.chisanpo.entity.Refuge;
import jp.co.thinkethbank.kurikita.chisanpo.entity.SerialMarkerOptions;


public class MapsActivity extends FragmentActivity implements GoogleApiClient.OnConnectionFailedListener, LocationListener, GoogleApiClient.ConnectionCallbacks {
    private final int[] groupIconResources = new int[]{
            R.drawable.ic_action_user_black, R.drawable.ic_action_user_blue, R.drawable.ic_action_user_green,
            R.drawable.ic_action_user_holo, R.drawable.ic_action_user_purple, R.drawable.ic_action_user_red,
            R.drawable.ic_action_user_yellow, R.drawable.ic_action_user_blue_l, R.drawable.ic_action_user_green_l,
            R.drawable.ic_action_user_holo_l
    };

    private final int[] groupColor = new int[]{
            0xA0000000, 0xA0000080, 0xA0008000, 0xA0FFFFFF, 0xA0FF00FF,
            0xA0FF0000, 0xA0808000, 0xA00000FF, 0xA000FF00, 0xA0808080
    };

    private final int[] groupReverseColor = new int[]{
            0x30FFFFFF, 0x30808000, 0x30800080, 0x30000000, 0x3000FF00,
            0x3000FFFF, 0x30000080, 0x30FFFF00, 0x30FF00FF, 0x30000000
    };

    /** カメラアクティビティの識別子 */
    static final int REQUEST_CAPTURE_IMAGE = 100;

    private static final int MENU_SAVE = 0;
    private static final int MENU_LOAD = 1;
    private static final int MENU_TAKE_PHOTO = 2;
    private static final int MENU_FOOTPRINT = 3;
    private static final int MENU_EVENT_SELECT = 4;
    private static final int MENU_INFORMATION = 5;
    private static final int MENU_FINISH = 6;
    private static final int MENU_ADMIN_SUB_MENU = 7;

    private static final int MAX_GROUP_NUM = 10;
    private static final double VALUE_GOAL_RANGE = 900;

    private ArrayAdapter<String> informationList;

    private ProgressDialog progressDialog;

    /** memberId */
    private String user;
    private boolean isAdmin = false;

    /** キャッシュディレクトリ */
    private File cacheDir;

    /** ドロップボックスのAPI */
    private DropboxAPI<AndroidAuthSession> dropboxAPI;
    private DropboxUtils dropboxUtils;

    /** googleMap API */
    private GoogleMap gMap; // Might be null if Google Play services APK is not available.
    private GoogleApiClient googleApiClient = null;
    private TextView infoText;

    /** parse API */
    /** イベント情報 */
    private Event event;

    /** memberIdとpositionとmillisecUpdateしか持ってないよ */
    private Member geoPosition;

    /** 立てたマーカーをリスト化して保持する */
    private ArrayList<SerialMarkerOptions> markerList;
    /** 足跡情報を保持する */
    private Polyline[] groupFootprints = new Polyline[MAX_GROUP_NUM];
    private ArrayList<Polyline> lineList = new ArrayList<>();
    private Circle touchPoint;
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
    private Marker[] groupIcons = new Marker[MAX_GROUP_NUM];
    private MarkerOptions[] groupIconOptions = new MarkerOptions[MAX_GROUP_NUM];

    /** 神社リスト用 */
    private List<Refuge> mRefugeList = new ArrayList<>();
    /** サムネイルのマーカーリスト */
    private ArrayList<Marker> thumbMarkerList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        /** 画面をスリープにしない処理を追加 */
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
        ListView informationListView = (ListView) findViewById(R.id.informationList);
        informationList = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<String>());
        informationListView.setAdapter(informationList);
        setupMapIfNeeded();

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
        setupMapIfNeeded();
        googleApiClient.connect();

        if(dropboxAPI.getSession().authenticationSuccessful()) {
            dropboxAPI.getSession().finishAuthentication();
            dropboxUtils.storeOauth2AccessToken(dropboxAPI.getSession().getOAuth2AccessToken());

            Toast.makeText(this, "Dropboxの認証に成功しました", Toast.LENGTH_SHORT).show();
            informationList.insert("Dropboxの認証に成功しました", 0);
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
        menu.add(Menu.NONE, MENU_FOOTPRINT, Menu.NONE, "足跡");
        menu.add(Menu.NONE, MENU_EVENT_SELECT, Menu.NONE, "イベントの選択");
        menu.add(Menu.NONE, MENU_INFORMATION, Menu.NONE, "情報の取得");
        menu.add(Menu.NONE, MENU_FINISH, Menu.NONE, "アプリの終了");
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
            case MENU_FOOTPRINT:
                dispGroupFootprints();
                return true;
            case MENU_EVENT_SELECT:
                makeSelectEventDialog();
                return true;
            case MENU_INFORMATION:
                downloadInformation();
                return true;
            case MENU_FINISH:
                finish();
                return true;
            case MENU_ADMIN_SUB_MENU:
                if(isAdmin) {
                    AlertDialog.Builder listDialog = new AlertDialog.Builder(this);
                    listDialog
                            .setTitle("管理者メニュー")
                            .setItems(new String[]{"ゴールの設定(工事中)", "アカウント削除して終了", "ズーム13.7"}, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    switch(which){
                                        case 0:
                                            makeSetGoalDialog();
                                            break;
                                        case 1:
                                            // ローカルデータからユーザー情報を抹消
                                            ParseQuery<Member> query = ParseQuery.getQuery(Member.class);
                                            query.fromLocalDatastore();
                                            query.findInBackground(new FindCallback<Member>() {
                                                @Override
                                                public void done(List<Member> list, ParseException e) {
                                                    if (e == null) {
                                                        if (list.size() != 0) {
                                                            Member member = list.get(0);
                                                            member.unpinInBackground();
                                                            Toast.makeText(MapsActivity.this, "保存されているユーザー情報を削除しました", Toast.LENGTH_SHORT).show();
                                                            finish();
                                                        } else {
                                                            Toast.makeText(MapsActivity.this, "保存されているユーザー情報がありません", Toast.LENGTH_SHORT).show();
                                                        }
                                                    }
                                                }
                                            });
                                            break;
                                        case 2:
                                            gMap.moveCamera(CameraUpdateFactory.zoomTo(13.7f));
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
            Bitmap thumb = BitmapEditor.thumbnail(takePic);
            takePic = BitmapEditor.resizePicture(takePic, 640);

            String fileName = String.valueOf(oldLatitude);
            String comment = "シンクスバンク夏のイベント";

            MarkerOptions thumbMark = new MarkerOptions()
                    .title(fileName)
                    .position(keepGoal)
                    .icon(BitmapDescriptorFactory.fromBitmap(thumb))
                    .snippet(comment);
            gMap.addMarker(thumbMark);

            // ************ ローカルに保存 ************
            File jpegFile = new File(cacheDir.getAbsolutePath(), fileName + ".jpg");
            byte[] jpegStream = BitmapEditor.compressJpeg(takePic, 90);

            File thumbFile = new File(cacheDir.getAbsolutePath(), fileName + ".thm");
            byte[] thumbStream = BitmapEditor.compressJpeg(thumb, 70);

            if(jpegStream != null) {
                try {
                    FileOutputStream saveFile = new FileOutputStream(jpegFile);
                    saveFile.write(jpegStream, 0, jpegStream.length);
                    saveFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if(thumbStream != null) {
                try {
                    FileOutputStream saveFile = new FileOutputStream(thumbFile);
                    saveFile.write(thumbStream, 0, thumbStream.length);
                    saveFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // ************ 写真アップロード ************
            if(jpegStream != null) {
                UploadPicture uploadPicture = new UploadPicture(this, dropboxAPI, fileName + ".jpg",
                        new ByteArrayInputStream(jpegStream), jpegStream.length);
                uploadPicture.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }

            if(thumbStream != null) {
                UploadPicture uploadPicture = new UploadPicture(this, dropboxAPI, fileName + ".thm",
                        new ByteArrayInputStream(thumbStream), thumbStream.length);
                uploadPicture.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }

            // ************ parseにファイル名登録 ************
            if(geoPosition != null) {
                ParseObject imageFile = new ParseObject("ImageFile");
                imageFile.put("fileName", fileName);
                imageFile.put("memberId", geoPosition.get("memberId"));
                imageFile.put("comment", comment);
                imageFile.put("position", new ParseGeoPoint(oldLatitude, oldLongitude));
                imageFile.saveInBackground();
            }
        }
    }

    private void setupParse2(){
        Event.findByEnable(new GetCallback<Event>() {
            @Override
            public void done(Event event, ParseException e) {
                MapsActivity.this.event = event;
                informationList.insert("イベント:" + event.getName() + "の情報を取得します", 0);

                setGeoPosition();
                setThumbnail();
                // putGoal();
            }
        });
    }

    private void setupAdmin(){
        isAdmin = true;
    }

    /** 各パラメータの取得 */
    private void setupParams(){
        cacheDir = getCacheDir();
    }

    private void saveParams(){
        // SharedPreferences.Editor editor =  params.edit();
        if(user != null){
            // editor.putString(PARAMS_USER_NAME, user);
            Log.i("saveParams", "execute save");
        }
        // editor.apply();
    }

    private void setupMapIfNeeded() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        if (gMap == null) {
            gMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
            if (gMap != null) {
                setupMap();
            }
        }
    }

    /** グーグルマップの初期設定 */
    private void setupMap() {
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

        gMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if(thumbMarkerList != null && thumbMarkerList.contains(marker)){
                    String fileName = marker.getTitle();
                    if(fileName != null){
                        File file = new File(cacheDir, fileName + ".jpg");
                        // ローカルに既にあった場合
                        if(file.exists()){
                            makeImageViewDialog(file.getAbsolutePath(), marker.getSnippet(), null);
                        }else{
                            // Dropboxから取得する
                            DownloadPicture dl = new DownloadPicture(MapsActivity.this, "viewer", dropboxAPI,
                                    file.getAbsolutePath(), fileName);
                            dl.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, marker.getSnippet());
                        }
                    }
                }
                return false;
            }
        });
    }

    private void addLine(LatLng point){
        if(touchPoint != null){
            touchPoint.remove();
        }
        CircleOptions circleOptions = new CircleOptions()
                .center(point)
                .radius(3);
        touchPoint = gMap.addCircle(circleOptions);

        for(Polyline pl : lineList){
            pl.remove();
        }
        lineList.clear();

        for (Refuge refuge : mRefugeList) {
            if (refuge != null) {
                if (refuge.isNear()) {
                    PolylineOptions polyOptions = new PolylineOptions();
                    polyOptions.add(point);
                    polyOptions.add(new LatLng(refuge.getLat(), refuge.getLng()));
                    polyOptions.color(Color.GRAY);
                    polyOptions.width(3);
                    polyOptions.geodesic(true); //true:大圏コース,false:直線
                    lineList.add(gMap.addPolyline(polyOptions));
                }
            }
        }
    }

    private void updateMaker() {
        int i = 0;
        for (Refuge refuge : mRefugeList) {
            if (refuge != null) {
                MarkerOptions options = new MarkerOptions();
                options.position(new LatLng(refuge.getLat(), refuge.getLng()));
                options.title(refuge.getName() + " " + refuge.getDistance() + "m");
                options.snippet(refuge.getAddress());

                BitmapDescriptor icon;
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

    private void sortRefugeList() {
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
            AndroidAuthSession session = new AndroidAuthSession(new AppKeyPair(DropboxUtils.APP_KEY, DropboxUtils.APP_KEY_SECRET));
            dropboxAPI = new DropboxAPI<>(session);
            dropboxAPI.getSession().startOAuth2Authentication(this);
        }else{
            dropboxAPI = new DropboxAPI<>(dropboxUtils.loadAndroidAuthSession());
        }
    }

    /** Parseの初期化とログイン処理 */
    private void setupParse(){
        ParseObject.registerSubclass(Member.class);
        ParseObject.registerSubclass(Treasure.class);
        ParseObject.registerSubclass(Event.class);
        ParseObject.registerSubclass(Group.class);
        Parse.enableLocalDatastore(this);
        Parse.initialize(this, getResources().getString(R.string.parse_app_id), getResources().getString(R.string.parse_app_key));

        // ローカルデータストアからユーザー情報を取得
        final ParseQuery<Member> query = ParseQuery.getQuery(Member.class);
        query.fromLocalDatastore();
        query.findInBackground(new FindCallback<Member>() {
            public void done(List<Member> memberList, ParseException e) {
                if (e != null) {
                    Log.e("Get User", "Error: " + e.getMessage());
                    return;
                }

                if(memberList.size() > 0){
                    Member member = memberList.get(0);
                    user = member.getMemberId();

                    // 権限者だった場合
                    if (member.getRank() == 9) {
                        setupAdmin();
                    }
                    Toast.makeText(MapsActivity.this, "ログインしました:" + user, Toast.LENGTH_SHORT).show();
                    setupParse2();
                    return;
                }

                // ***** ローカルに保存されていない場合は登録 *****
                final EditText et = new EditText(MapsActivity.this);
                et.setInputType(InputType.TYPE_CLASS_TEXT);
                et.setHint("サイボウズのID");
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

                        if(inputMember.isEmpty()){
                            return;
                        }

                        ParseQuery<Member> query = ParseQuery.getQuery(Member.class);
                        // 入力した名前の人がいるかどうか
                        query.whereEqualTo("memberId", inputMember);
                        query.getFirstInBackground(new GetCallback<Member>() {
                            public void done(Member cloudMember, ParseException e) {
                                if(e != null){
                                    Log.e("compare name", "Error: " + e.getMessage());
                                    return;
                                }

                                if(cloudMember == null){
                                    dialog.setMessage("存在しない名前です");
                                    return;
                                }

                                user = cloudMember.getMemberId();
                                // 権限者だった場合
                                if (cloudMember.getRank() == 9) {
                                    setupAdmin();
                                }
                                cloudMember.pinInBackground();

                                dialog.dismiss();
                                Toast.makeText(MapsActivity.this, "ログインしました", Toast.LENGTH_SHORT).show();
                                setupParse2();
                            }
                        });
                    }
                });
            }
        });
    }

    /** Parseの機能を利用してアプリを利用するユーザーとグループの位置情報を保管するオブジェクトを生成 */
    private void setGeoPosition(){
        ParseQuery<Member> query = ParseQuery.getQuery(Member.class);
        query.selectKeys(Arrays.asList("memberId", "position", "millisecUpdate", "previousInfo"));
        query.whereEqualTo("memberId", user);
        query.findInBackground(new FindCallback<Member>() {
            @Override
            public void done(List<Member> list, ParseException e) {
                if (list.size() != 0) {
                    geoPosition = list.get(0);
                    if (event != null) {
                        geoPosition.setEvent(event);
                        geoPosition.saveInBackground();
                    }
                } else {
                    Toast.makeText(MapsActivity.this, "Parseから位置情報データの取得に失敗しました:" + user, Toast.LENGTH_LONG).show();
                }
            }
        });

        ParseQuery<Group> queryGroup = ParseQuery.getQuery(Group.class);
        queryGroup.selectKeys(Arrays.asList("groupId", "groupGeo", "name"));
        queryGroup.findInBackground(new FindCallback<Group>() {
            @Override
            public void done(List<Group> list, ParseException e) {
                if (e == null) {
                    for (Group gp : list) {
                        int groupId = gp.getGroupId();
                        ParseGeoPoint groupGeo = gp.getGroupGeo();

                        if (groupId >= MAX_GROUP_NUM) continue;

                        groupIconOptions[groupId] = new MarkerOptions()
                                .icon(BitmapDescriptorFactory.fromResource(groupIconResources[groupId]))
                                .position(new LatLng(groupGeo.getLatitude(), groupGeo.getLongitude()))
                                .title(gp.getName());
                        groupIcons[groupId] = gMap.addMarker(groupIconOptions[groupId]);
                    }
                } else {
                    Toast.makeText(MapsActivity.this, "Parseからグループの位置情報データの取得に失敗しました", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void downloadInformation(){
        ParseQuery<ParseObject> query = ParseQuery.getQuery("InformationLog");
        query.whereGreaterThan("millisecUpdate", geoPosition.getPreviousInfo());
        query.addAscendingOrder("millisecUpdate");
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> list, ParseException e) {
                if(e != null){
                    return;
                }

                if(list == null || list.size() == 0){
                    return;
                }

                for(ParseObject info : list){
                    switch(info.getString("model")){
                        case "ImageFile":               // 写真を撮った情報
                            if(event == null || !event.getEnable()){
                                continue;
                            }
                            informationList.add("新しく写真が撮影されました(" + info.getString("value1") + ")");
                            setThumbnail();
                            break;
                    }
                }

                geoPosition.setMillisecUpdate(list.get(list.size() - 1).getLong("millisecUpdate"));
                geoPosition.saveInBackground();
            }
        });
    }

    private void setThumbnail(){
        if(dropboxAPI == null){
            Toast.makeText(MapsActivity.this, "Dropboxが未設定ためサムネイルの取得に失敗しました", Toast.LENGTH_LONG).show();
            return;
        }

        if(thumbMarkerList != null && thumbMarkerList.size() != 0){
            for(Marker m : thumbMarkerList){
                m.remove();
            }
        }

        // サムネイルファイルの取得
        ParseQuery<ParseObject> query = ParseQuery.getQuery("ImageFile");
        if(event != null) {
            query.whereEqualTo("event", event);
        }
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> list, ParseException e) {
                informationList.insert("このイベントで撮った写真の枚数:" + list.size() + "枚", 0);
                for (ParseObject imageFile : list) {
                    String fileName = (String) imageFile.get("fileName");
                    ParseGeoPoint pos = imageFile.getParseGeoPoint("position");
                    // ファイル名があった場合
                    if (fileName != null) {
                        File file = new File(cacheDir, fileName + ".thm");
                        // ローカルに既にあった場合
                        if (file.exists()) {
                            Bitmap bmpImage = BitmapEditor.decodeThumbnail(file.getAbsolutePath());
                            if (bmpImage == null) {
                                Log.w("decode thumb", "can't decode thumbnail!! name:" + file.getName());
                                continue;
                            }
                            BitmapDescriptor iconImage = BitmapDescriptorFactory.fromBitmap(bmpImage);

                            MarkerOptions thumbOptions = new MarkerOptions()
                                    .title(fileName)
                                    .position(new LatLng(pos.getLatitude(), pos.getLongitude()))
                                    .snippet((String) imageFile.get("comment"));
                            thumbOptions.icon(iconImage);

                            Marker tMarker = gMap.addMarker(thumbOptions);
                            thumbMarkerList.add(tMarker);
                        }
                        // ローカルに無い場合はダウンロード
                        else {
                            // Dropboxから取得する
                            DownloadPicture dl = new DownloadPicture(MapsActivity.this, "thumb", dropboxAPI,
                                    file.getAbsolutePath(), fileName);
                            dl.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, imageFile.get("comment"),
                                    pos.getLatitude(), pos.getLongitude());
                        }
                    }
                }
            }
        });
    }

    /**
     * 他のクラスから呼び出す用。非同期にサムネイルマーカーをマップに配置する
     * @param name ファイル名。拡張子無し
     * @param value コメントとか
     * @param lat 緯度
     * @param lng 経度
     */
    void setThumbMarkerList(String name, String value, double lat, double lng){
        File file = new File(cacheDir, name + ".thm");
        Bitmap bitmap = BitmapEditor.decodeThumbnail(file.getAbsolutePath());
        if(bitmap == null){
            Log.w("decode thumb", "can't decode thumbnail!! name:" + file.getName());
            return;
        }
        MarkerOptions thumbOptions = new MarkerOptions()
                .title(name)
                .position(new LatLng(lat, lng))
                .snippet(value)
                .icon(BitmapDescriptorFactory.fromBitmap(bitmap));
        thumbMarkerList.add(gMap.addMarker(thumbOptions));
    }

    /**
     * 画像を表示するダイアログの作成
     * @param fileName 画像のファイル名
     * @param comment 表示するコメント
     * @param list 他の人のコメント（未実装）
     */
    void makeImageViewDialog(String fileName, String comment, List<ParseObject> list){
        Bitmap bitmap = BitmapFactory.decodeFile(fileName);

        LayoutInflater inflater = (LayoutInflater)this.getSystemService(LAYOUT_INFLATER_SERVICE);
        final View layout = inflater.inflate(R.layout.image_view_dialog,
                (ViewGroup)findViewById(R.id.layout_root));

        ImageView image = (ImageView)layout.findViewById(R.id.imageView);
        image.setImageBitmap(bitmap);

        // アラーとダイアログ を生成
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(comment);
        builder.setView(layout);
        builder.setPositiveButton("閉じる", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        // 表示
        builder.create().show();
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

    /** サーバーからアイテム情報を取得してマップに表示する */
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
        BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.mipmap.chi);
        String title = String.valueOf(flagCount) + "本目";
        MarkerOptions options = new MarkerOptions();
        options.icon(icon);
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
        String title;
        String message;
        String btn;
        keepGoal = position;

        if(event == null || !event.getEnable()){
            Toast.makeText(this, "イベントが既に終了しています", Toast.LENGTH_LONG).show();
            event.fetchIfNeededInBackground();
            return;
        }

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

        if(itemId == 0){
            title = "写真でも撮りますか";
            message = "この場所で撮影します";
            btn = "撮ります";
        }else{
            title = "おめでとう！";
            message = goalTitle + "に到着しました";
            btn = "記念写真撮るよ";
        }

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("ｷｬﾝｾﾙ", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setNegativeButton(btn, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent( MediaStore.ACTION_IMAGE_CAPTURE);
                        startActivityForResult(intent, REQUEST_CAPTURE_IMAGE);
                    }
                })
                .show();
    }

    /** 各グループの足跡を表示する */
    private void dispGroupFootprints(){
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Footprints");
        if(event != null){
            query.whereEqualTo("event", event);
        }
        query.orderByAscending("createdAt");
        query.setLimit(1000);
        makeProgressDialog("各グループの足跡情報取得中");
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> list, ParseException e) {
                progressDialog.dismiss();
                if (e == null && list != null) {
                    PolylineOptions[] pOptions = new PolylineOptions[MAX_GROUP_NUM];
                    PolylineOptions[] bOptions = new PolylineOptions[MAX_GROUP_NUM];
                    for (int i = 0; i < MAX_GROUP_NUM; i++) {
                        pOptions[i] = new PolylineOptions().width(6).color(groupColor[i]);
                        bOptions[i] = new PolylineOptions().width(18).color(groupReverseColor[i]);
                    }

                    if (groupFootprints != null) {
                        for (Polyline gLine : groupFootprints) {
                            if (gLine != null) {
                                gLine.remove();
                            }
                        }
                    } else {
                        groupFootprints = new Polyline[MAX_GROUP_NUM];
                    }

                    for (ParseObject footPrint : list) {
                        int groupId = (int) footPrint.get("groupId");
                        ParseGeoPoint p = (ParseGeoPoint) footPrint.get("position");
                        LatLng latLng1 = new LatLng(p.getLatitude(), p.getLongitude());
                        LatLng latLng2 = new LatLng(p.getLatitude(), p.getLongitude());
                        pOptions[groupId].add(latLng1);
                        bOptions[groupId].add(latLng2);
                    }

                    for (int i = 0; i < MAX_GROUP_NUM; i++) {
                        if (pOptions[i].getPoints().size() > 0) {
                            gMap.addPolyline(bOptions[i]);
                            groupFootprints[i] = gMap.addPolyline(pOptions[i]);
                        }
                    }
                    informationList.insert("各グループの足跡情報を表示しました", 0);
                } else {
                    Toast.makeText(MapsActivity.this, "グループID：" + user + "の足跡の取得に失敗しました", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    /**
     * 自分の位置をサーバーに送ると同時にサーバーからグループの位置を取得する
     * @param latitude 緯度
     * @param longitude 経度
     */
    private void syncPosition(double latitude, double longitude){
        if(event == null || !event.getEnable()){
            return;
        }

        // ***** 自分の位置情報を送信 *****
        if(geoPosition != null) {
            ParseGeoPoint pos = geoPosition.getPosition();
            pos.setLatitude(latitude);
            pos.setLongitude(longitude);
            geoPosition.setMillisecUpdate(new Date().getTime());

            // 標準時の時間
            geoPosition.saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    if(e != null){
                        Toast.makeText(MapsActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });
        }

        // ***** グループの位置情報を取得 *****
        ParseQuery<Group> queryGroup = ParseQuery.getQuery(Group.class);
        queryGroup.selectKeys(Arrays.asList("groupId", "groupGeo"));
        queryGroup.findInBackground(new FindCallback<Group>() {
            @Override
            public void done(List<Group> list, ParseException e) {
                if(e != null){
                    Toast.makeText(MapsActivity.this, "Parseからグループの位置情報データの取得に失敗しました", Toast.LENGTH_LONG).show();
                    return;
                }

                for(Group gp : list){
                    int groupId = gp.getGroupId();
                    ParseGeoPoint groupGeo = gp.getGroupGeo();

                    if(groupIcons[groupId] != null) {
                        groupIcons[groupId].remove();
                        groupIconOptions[groupId].position(new LatLng(groupGeo.getLatitude(), groupGeo.getLongitude()));
                    }else{
                        groupIconOptions[groupId] = new MarkerOptions().position(new LatLng(groupGeo.getLatitude(), groupGeo.getLongitude()));
                    }
                    groupIcons[groupId] = gMap.addMarker(groupIconOptions[groupId]);
                }
            }
        });
    }

    /** イベント選択ダイアログの表示。選択したイベント情報を取得する */
    void makeSelectEventDialog(){
        ParseQuery<Event> query = ParseQuery.getQuery(Event.class);
        makeProgressDialog("イベントリストの取得中");
        query.findInBackground(new FindCallback<Event>() {
            @Override
            public void done(final List<Event> eventList, ParseException e) {
                progressDialog.dismiss();
                String[] itemList = new String[eventList.size()];

                for (int i = 0; i < eventList.size(); i++) {
                    Event event = eventList.get(i);
                    itemList[i] = event.getName();
                }

                AlertDialog dialog = new AlertDialog.Builder(MapsActivity.this)
                        .setTitle("イベントの選択")
                        .setItems(itemList, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                final Event selectedEvent = eventList.get(which);
                                // イベントが有効ではない時
                                if (!selectedEvent.getEnable()) {
                                    new AlertDialog.Builder(MapsActivity.this)
                                            .setTitle(selectedEvent.getName() + "は既に終了しています")
                                            .setNegativeButton("構わない", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    setEvent(selectedEvent);
                                                }
                                            })
                                            .setPositiveButton("やめます", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {

                                                }
                                            }).show();
                                    return;
                                }
                                setEvent(selectedEvent);
                            }

                            private void setEvent(final Event settingEvent) {
                                geoPosition.setEvent(settingEvent);
                                geoPosition.saveInBackground(new SaveCallback() {
                                    @Override
                                    public void done(ParseException e) {
                                        if (e != null) {
                                            Toast.makeText(MapsActivity.this, "なんかイベントの変更に失敗しました。" + e.getMessage(), Toast.LENGTH_LONG).show();
                                        }
                                        event = settingEvent;
                                        informationList.insert("イベント:" + settingEvent.getName() + "の情報を取得します", 0);
                                        setThumbnail();
                                    }
                                });
                            }
                        })
                        .setPositiveButton("ｷｬﾝｾﾙ", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .create();
                dialog.show();
            }
        });


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

    private void makeProgressDialog(String waitFor){
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(waitFor);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.show();
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

        // ***** カメラとかの設定 *****
        // あまり動いていない場合は処理を実行しない
        if(length_2 < 100){
            return;
        }else if(length_2 > 90000){
            length_2 = 90000;
        }
        float zoomCoefficient = 12f + (float)(60 / (Math.pow(length_2, 0.3) + 4d));
        CameraPosition currentPlace = new CameraPosition.Builder()
                .target(new LatLng(latitude, longitude)).zoom(zoomCoefficient)
                .bearing(location.getBearing()).build();
        gMap.animateCamera(CameraUpdateFactory.newCameraPosition(currentPlace));

        // ***** 雑多な処理 *****
        // マーカーの作成
        putMarker(latitude, longitude);
        // サーバーに位置情報の送信
        syncPosition(latitude, longitude);

        // ***** 目的地到着判定 *****
        if(mRefugeList != null){
            for(int i = 0; i < mRefugeList.size(); i++){
                Refuge goal = mRefugeList.get(i);

                double diffGoalLatitude = (goal.getLat() - latitude) / 0.000008983148616;
                double diffGoalLongitude = (goal.getLng() - longitude) / 0.000010966382364;

                if (diffGoalLatitude * diffGoalLatitude + diffGoalLongitude * diffGoalLongitude < VALUE_GOAL_RANGE) {
                    reachPoint((int)parseGoals.get(i).get("treasureId"), new LatLng(goal.getLat(), goal.getLng()));
                    break;
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
}
