package com.freethinking.beats.sdk.data;

public class Authorization extends BaseJson {

    protected String code;
    protected int id;
    protected String jsonrpc;
    protected AuthorizationResult result;

    public Authorization() {
        this.result = new AuthorizationResult();
    }

    @Override
    public void fillIn(BaseJson parseJson) throws Exception {
        if (parseJson instanceof Authorization) {
            id = ((Authorization) parseJson).id;
            code = ((Authorization) parseJson).code;
            jsonrpc = ((Authorization) parseJson).jsonrpc;
            result = ((Authorization) parseJson).result;
        } else {
            throw new Exception();
        }
    }

    public String getCode() {
        return code;
    }

    public int getId() {
        return id;
    }

    public String getJsonrpc() {
        return jsonrpc;
    }

    public AuthorizationResult getResult() {
        return result;
    }
}
