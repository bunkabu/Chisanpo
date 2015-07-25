package jp.co.thinkethbank.kurikita.chisanpo.bean;

import com.parse.ParseClassName;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;

@ParseClassName("Members")
public class Member extends ParseObject {
    public void setMemberId(String memberId){
        put("memberId", memberId);
    }

    public String getMemberId(){
        return getString("memberId");
    }

    public void setGroup(Group group){
        put("group", group);
    }

    public Group getGroup(){
        return (Group)get("group");
    }

    public void setRank(int rank){
        put("rank", rank);
    }

    public int getRank(){
        return getInt("rank");
    }

    public void setName(String name){
        put("name", name);
    }

    public String getName(){
        return getString("name");
    }

    public void setPriority(int priority){
        put("priority", priority);
    }

    public int getPriority(){
        return getInt("priority");
    }

    public void setPosition(ParseGeoPoint position){
        put("position", position);
    }

    public ParseGeoPoint getPosition(){
        return getParseGeoPoint("position");
    }

    public void setPreviousInfo(long previousInfo){
        put("previousInfo", previousInfo);
    }

    public long getPreviousInfo(){
        return getLong("previousInfo");
    }

    public void setMillisecUpdate(long millisecUpdate){
        put("millisecUpdate", millisecUpdate);
    }

    public long getMillisecUpdate(){
        return getLong("millisecUpdate");
    }

    public void setEvent(Event event){
        put("event", event);
    }
}
