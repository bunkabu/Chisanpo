package jp.co.thinkethbank.kurikita.chisanpo.bean;

import com.parse.ParseClassName;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;

@ParseClassName("Group")
public class Group extends ParseObject {
    public void setGroupId(int groupId){
        put("groupId", groupId);
    }

    public int getGroupId(){
        return getInt("groupId");
    }

    public void setName(String name){
        put("name", name);
    }

    public String getName(){
        return getString("name");
    }

    public void setEnable(boolean isEnable) {
        put("isEnable", isEnable);
    }

    public boolean getEnable(){
        return getBoolean("isEnable");
    }

    public void setGroupGeo(ParseGeoPoint groupGeo){
        put("groupGeo", groupGeo);
    }

    public ParseGeoPoint getGroupGeo(){
        return getParseGeoPoint("groupGeo");
    }

    public void setMemo(String memo){
        put("memo", memo);
    }

    public String getMemo(){
        return getString("memo");
    }
}
