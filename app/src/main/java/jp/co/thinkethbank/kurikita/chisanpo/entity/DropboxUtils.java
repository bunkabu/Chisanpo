package jp.co.thinkethbank.kurikita.chisanpo.entity;

import android.content.Context;
import android.content.SharedPreferences;

import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AppKeyPair;

/** Dropboxの認証情報を保持するクラス */
public class DropboxUtils {
    private static final String TOKEN = "token";
    private static final String PREF_NAME = "dropbox";
    public static final String APP_KEY = "flu2b0urtc18egm";
    public static final String APP_KEY_SECRET = "kroennf800x63fa";
    private Context context;

    public DropboxUtils(Context context) {
        this.context = context;
    }

    public void storeOauth2AccessToken(String secret){
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(TOKEN, secret);
        editor.apply();
    }

    public AndroidAuthSession loadAndroidAuthSession() {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String token = preferences.getString(TOKEN, null);
        if (token != null) {
            AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_KEY_SECRET);
            return new AndroidAuthSession(appKeys,token);
        } else {

            return null;
        }
    }

    public boolean hasLoadAndroidAuthSession() {
        return loadAndroidAuthSession() != null;
    }
}
