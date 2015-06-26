package jp.co.thinkethbank.kurikita.chisanpo;

import android.os.AsyncTask;

public class ServerCommunicate extends AsyncTask<Object, Void, String> {
    private static final int SEND_TYPE_POSITION = 0;
    private static final int SEND_TYPE_GET_ITEM = 1;
    private static final int SEND_TYPE_OTHER_TEAM = 2;

    private OnReceiveFromServerCallback onReceiveFromServerCallback;

    public ServerCommunicate(OnReceiveFromServerCallback onReceiveFromServerCallback){
        super();
        this.onReceiveFromServerCallback = onReceiveFromServerCallback;
    }

    @Override
    protected String doInBackground(Object... params) {
        return null;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        onReceiveFromServerCallback.receiveCalc(s);
    }

    void sendPosition(String user, double latitude, double longitude){
        execute(SEND_TYPE_POSITION, user, latitude, longitude);
    }

    void sendGetItem(int teamId, int itemId){
        execute(SEND_TYPE_GET_ITEM, teamId, itemId);
    }

    void sendRequireOtherTeamRoute(int teamId, int otherTeamId){
        execute(SEND_TYPE_OTHER_TEAM, teamId, otherTeamId);
    }

    interface OnReceiveFromServerCallback{
        /** サーバーから情報を受け取った時に呼ばれるメソッド */
        void receiveCalc(String result);
    }
}
