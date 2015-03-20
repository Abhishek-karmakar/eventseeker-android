package com.freethinking.beats.sdk.data;

public class TrackWrapper extends BaseJson {
    protected Activity data;
    protected String code;

    public TrackWrapper() {
        this.data = new Activity();
    }

    @Override
    public void fillIn(BaseJson parseJson) throws Exception {
        if (parseJson instanceof TrackWrapper) {
            data = ((TrackWrapper) parseJson).data;
            code = ((TrackWrapper) parseJson).code;
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
