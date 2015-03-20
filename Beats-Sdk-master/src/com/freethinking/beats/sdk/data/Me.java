package com.freethinking.beats.sdk.data;

public class Me extends BaseJson {

    protected String code;
    protected Integer id;
    protected String jsonrpc;
    protected MeResult result;

    public Me() {
        this.result = new MeResult();
    }

    @Override
    public void fillIn(BaseJson parseJson) throws Exception {
        if (parseJson instanceof Me) {
            id = ((Me) parseJson).id;
            code = ((Me) parseJson).code;
            jsonrpc = ((Me) parseJson).jsonrpc;
            result = ((Me) parseJson).result;
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

    public MeResult getResult() {
        return result;
    }
}
