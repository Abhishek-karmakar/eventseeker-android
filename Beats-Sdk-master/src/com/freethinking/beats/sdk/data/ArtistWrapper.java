package com.freethinking.beats.sdk.data;

public class ArtistWrapper extends BaseJson {
    protected Artist data;
    protected String code;

    public ArtistWrapper() {
        this.data = new Artist();
    }

    public String getCode() {
        return code;
    }

    public Artist getData() {
        return data;
    }

    public Artist getArtist() {
        return data;
    }

    @Override
    public void fillIn(BaseJson parseJson) throws Exception {
        if (parseJson instanceof ArtistWrapper) {
            data = ((ArtistWrapper) parseJson).data;
            code = ((ArtistWrapper) parseJson).code;
        } else {
            throw new Exception();
        }
    }
}
