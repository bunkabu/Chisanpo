package jp.co.thinkethbank.kurikita.chisanpo;

import android.os.AsyncTask;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.ProgressListener;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;

import java.io.ByteArrayInputStream;

public class UploadPicture extends AsyncTask<Void, Long, Boolean> {
    private MapsActivity mapsActivity;
    /** ドロップボックスのAPI */
    private DropboxAPI<AndroidAuthSession> dropboxAPI;
    private String name;
    private ByteArrayInputStream inputStream;
    private long dataLength;

    public UploadPicture(MapsActivity mapsActivity, DropboxAPI<AndroidAuthSession> dropboxAPI,
                         String name, ByteArrayInputStream inputStream, long dataLength){
        this.mapsActivity = mapsActivity;
        this.dropboxAPI = dropboxAPI;
        this.name = name;
        this.inputStream = inputStream;
        this.dataLength = dataLength;
    }

    @Override
    protected Boolean doInBackground(Void... params) {

        try {
            DropboxAPI.Entry response = dropboxAPI.putFile("/ChiSanphoto/" + name, inputStream, dataLength,
                    null, new DropboxUploadProgressListener());
            return response != null;
        } catch (DropboxException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        if(aBoolean) {
            mapsActivity.setInfoText(name + "のアップロードが完了しました");
        }else{
            mapsActivity.setInfoText(name + "のアップロードに失敗しました");
        }
    }

    private class DropboxUploadProgressListener extends ProgressListener{
        @Override
        public void onProgress(long process, long total) {
            mapsActivity.setInfoText("アップロード中(" + String.valueOf(process) + "/" + String.valueOf(total) + ")");
        }
    }
}
