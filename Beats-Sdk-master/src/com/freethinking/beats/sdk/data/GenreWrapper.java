package com.freethinking.beats.sdk.data;

public class GenreWrapper extends BaseJson {
    protected Genre data;
    protected String code;

    public GenreWrapper() {
        this.data = new Genre();
    }

    @Override
    public void fillIn(BaseJson parseJson) throws Exception {
        if (parseJson instanceof GenreWrapper) {
            data = ((GenreWrapper) parseJson).data;
            code = ((GenreWrapper) parseJson).code;
        } else {
            throw new Exception();
        }
    }

    public Genre getData() {
        return data;
    }

    public String getCode() {
        return code;
    }
}
