package com.freethinking.beats.sdk.data;

public class UserWrapper extends BaseJson {

    protected String code;
    protected User data;

    public UserWrapper() {
        this.data = new User();
    }

    @Override
    public void fillIn(BaseJson parseJson) throws Exception {
        if (parseJson instanceof UserWrapper) {
            data = ((UserWrapper) parseJson).data;
            code = ((UserWrapper) parseJson).code;
        } else {
            throw new Exception();
        }
    }

    public String getCode() {
        return code;
    }

    public User getData() {
        return data;
    }

    public User getUser() {
        return data;
    }
}
