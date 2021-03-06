package jp.co.thinkethbank.kurikita.chisanpo;


import android.os.AsyncTask;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class DownloadPicture extends AsyncTask<Object, Long, Boolean> {
    private MapsActivity mapsActivity;
    /** ドロップボックスのAPI */
    private DropboxAPI<AndroidAuthSession> dropboxAPI;
    private String type;
    private String outputFileName;
    /** 拡張子無し */
    private String downloadFileName;
    private Object[] params;

    public DownloadPicture(MapsActivity mapsActivity, String type, DropboxAPI<AndroidAuthSession> dropboxAPI,
                         String outputFileName, String downloadFileName){
        this.mapsActivity = mapsActivity;
        this.type = type;
        this.dropboxAPI = dropboxAPI;
        this.outputFileName = outputFileName;
        this.downloadFileName = downloadFileName;
    }

    @Override
    protected Boolean doInBackground(Object... params) {
        this.params = params;
        String fullFileName;
        switch(type){
            case "thumb":
                fullFileName = downloadFileName + ".thm";
                break;
            case "viewer":
                fullFileName = downloadFileName + ".jpg";
                break;
            default:
                fullFileName = downloadFileName;
        }

        File file = new File(outputFileName);
        FileOutputStream outputStream;
        try {
            outputStream = new FileOutputStream(file);
            dropboxAPI.getFile("/ChiSanphoto/" + fullFileName, null, outputStream, null);
        } catch (FileNotFoundException | DropboxException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        if(aBoolean) {
            switch (type) {
                case "thumb":
                    mapsActivity.setThumbMarkerList(downloadFileName, (String)params[0], (double)params[1], (double)params[2]);
                    break;
                case "viewer":
                    mapsActivity.makeImageViewDialog(outputFileName, (String)params[0], null);
                    break;
            }
        }
    }
}
