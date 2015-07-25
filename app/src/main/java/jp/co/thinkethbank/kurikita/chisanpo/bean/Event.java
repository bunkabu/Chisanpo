package jp.co.thinkethbank.kurikita.chisanpo.bean;

import com.parse.GetCallback;
import com.parse.ParseClassName;
import com.parse.ParseObject;
import com.parse.ParseQuery;

@ParseClassName("Events")
public class Event extends ParseObject {
    public static void findByEnable(GetCallback<Event> getCallback){
        ParseQuery<Event> query = ParseQuery.getQuery(Event.class);
        query.whereEqualTo("isEnable", true);
        query.orderByDescending("priority");
        query.getFirstInBackground(getCallback);
    }

    public void setEventId(String eventId){
        put("eventId", eventId);
    }

    /** イベント固有のID */
    public String getEventId(){
        return getString("eventId");
    }

    public void setName(String name){
        put("name", name);
    }

    /** イベントの名前 */
    public String getName(){
        return getString("name");
    }

    public void setEnable(boolean isEnable) {
        put("isEnable", isEnable);
    }

    /** イベントが有効かどうか。ユーザーの保有するイベントが無効の場合位置情報の同期などが行われず、写真を保存することもでき無い */
    public boolean getEnable(){
        return getBoolean("isEnable");
    }

    public void setPriority(int priority){
        put("priority", priority);
    }

    /** イベントの優先順位。有効ないべんとが複数あった場合、この値が大きいイベントを優先して取得する */
    public int getPriority(){
        return getInt("priority");
    }

    public void setMemo(String memo){
        put("memo", memo);
    }

    /** イベント説明・補足 */
    public String getMemo(){
        return getString("memo");
    }
}
