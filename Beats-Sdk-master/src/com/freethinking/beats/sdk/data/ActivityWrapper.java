package com.freethinking.beats.sdk.data;

public class ActivityWrapper extends BaseJson {
    protected Activity data;
    protected String code;

    public ActivityWrapper() {
        this.data = new Activity();
    }

    @Override
    public void fillIn(BaseJson parseJson) throws Exception {
        if (parseJson instanceof ActivityWrapper) {
            data = ((ActivityWrapper) parseJson).data;
            code = ((ActivityWrapper) parseJson).code;
        } else {
            throw new Exception();
        }
    }

    public Activity getData() {
        return data;
    }

    public String getCode() {
        return code;
    }
}
