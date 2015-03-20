package com.freethinking.beats.sdk.data;

public class ReviewWrapper extends BaseJson {
    protected Review data;
    protected String code;

    public ReviewWrapper() {
        this.data = new Review();
    }

    @Override
    public void fillIn(BaseJson parseJson) throws Exception {
        if (parseJson instanceof ReviewWrapper) {
            data = ((ReviewWrapper) parseJson).data;
            code = ((ReviewWrapper) parseJson).code;

        } else {
            throw new Exception();
        }
    }

    public Review getData() {
        return data;
    }

    public String getCode() {
        return code;
    }
}
