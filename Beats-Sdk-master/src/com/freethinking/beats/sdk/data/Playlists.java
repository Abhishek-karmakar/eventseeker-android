package com.freethinking.beats.sdk.data;

import java.util.ArrayList;
import java.util.List;

public class Playlists extends BaseJson {

    protected String code;
    protected PagingInfo info;
    protected List<Playlist> data;

    public Playlists() {
        this.data = new ArrayList<Playlist>();
        this.info = new PagingInfo();
    }

    @Override
    public void fillIn(BaseJson parseJson) throws Exception {
        if (parseJson instanceof Playlists) {
            List<Playlist> temp = ((Playlists) parseJson).data;
            this.data.addAll(temp);
            this.info = ((Playlists) parseJson).info;
            this.code = ((Playlists) parseJson).code;
        } else {
            throw new Exception();
        }
    }

    public String getCode() {
        return code;
    }

    public PagingInfo getInfo() {
        return info;
    }

    public List<Playlist> getData() {
        return data;
    }

    public List<Playlist> getPlaylists() {
        return data;
    }
}
