package com.freethinking.beats.sdk.data;

import java.util.ArrayList;
import java.util.List;

public class Activities extends BaseJson {

    protected String code;
    protected PagingInfo info;
    protected List<Activity> data;

    public Activities() {
        this.data = new ArrayList<Activity>();
        this.info = new PagingInfo();
    }

    @Override
    public void fillIn(BaseJson parseJson) throws Exception {
        if (parseJson instanceof Activities) {
            List<Activity> temp = ((Activities) parseJson).data;
            this.data.addAll(temp);
            this.info = ((Activities) parseJson).info;
            this.code = ((Activities) parseJson).code;
        } else {
            throw new Exception();
        }
    }

    public String getCode() {
        return code;
    }

    public PagingInfo getInfo() {
        return info;
    }

    public List<Activity> getData() {
        return data;
    }

    public List<Activity> getActivities() {
        return data;
    }
}
