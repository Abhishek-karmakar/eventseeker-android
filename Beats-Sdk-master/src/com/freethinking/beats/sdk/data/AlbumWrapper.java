package com.freethinking.beats.sdk.data;

public class AlbumWrapper extends BaseJson {
    protected Album data;
    protected String code;

    public AlbumWrapper() {
        this.data = new Album();
    }

    public Album getData() {
        return data;
    }

    public String getCode() {
        return code;
    }

    public Album getAlbum() {
        return data;
    }

    @Override
    public void fillIn(BaseJson parseJson) throws Exception {
        if (parseJson instanceof AlbumWrapper) {
            data = ((AlbumWrapper) parseJson).data;
            code = ((AlbumWrapper) parseJson).code;
        } else {
            throw new Exception();
        }
    }
}
