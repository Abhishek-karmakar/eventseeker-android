package com.freethinking.beats.sdk.data;

public class PlaylistWrapper extends BaseJson {
    protected Playlist data;
    protected String code;

    public PlaylistWrapper() {
        this.data = new Playlist();
    }

    @Override
    public void fillIn(BaseJson parseJson) throws Exception {
        if (parseJson instanceof PlaylistWrapper) {
            data = ((PlaylistWrapper) parseJson).data;
            code = ((PlaylistWrapper) parseJson).code;
        } else {
            throw new Exception();
        }
    }

    public Playlist getData() {
        return data;
    }

    public String getCode() {
        return code;
    }
}
