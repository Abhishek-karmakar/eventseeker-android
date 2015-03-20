package com.freethinking.beats.sdk.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Returns a list of tracks.
 */
public class Tracks extends BaseJson {
    protected List<Track> data;
    protected String code;
    protected PagingInfo info;

    public Tracks() {
        this.data = new ArrayList<Track>();
        this.info = new PagingInfo();
    }

    @Override
    public void fillIn(BaseJson parseJson) throws Exception {
        if (parseJson instanceof Tracks) {
            List<Track> temp = ((Tracks) parseJson).data;
            this.data.addAll(temp);
            this.info = ((Tracks) parseJson).info;
            this.code = ((Tracks) parseJson).code;
        } else {
            throw new Exception();
        }
    }

    public List<Track> getTracks() {
        return data;
    }

    public List<Track> getData() {
        return data;
    }

    public String getCode() {
        return code;
    }

    public PagingInfo getInfo() {
        return info;
    }
}
