package jp.co.thinkethbank.kurikita.chisanpo.bean;

import com.parse.ParseClassName;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;

@ParseClassName("Treasure")
public class Treasure extends ParseObject {
    public void setTreasureId(int treasureId){
        put("treasureId", treasureId);
    }

    public int getTreasureId(){
        return getInt("treasureId");
    }

    public void setType(String type){
        put("type", type);
    }

    public String getType(){
        return getString("type");
    }

    public void setName(String name){
        put("name", name);
    }

    public String getName(){
        return getString("name");
    }

    public void setAddress(String address){
        put("address", address);
    }

    public String getAddress(){
        return getString("address");
    }

    public void setPosition(String position){
        put("position", position);
    }

    public ParseGeoPoint getPosition(){
        return getParseGeoPoint("position");
    }

    public void setFindGroupId(int findGroupId){
        put("findGroupId", findGroupId);
    }

    public int getFindGroupId(){
        return getInt("findGroupId");
    }

    public void setSourceId(int sourceId){
        put("sourceId", sourceId);
    }

    public int getSourceId(){
        return getInt("sourceId");
    }
}
