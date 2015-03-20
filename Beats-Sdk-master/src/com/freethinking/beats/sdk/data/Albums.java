package com.freethinking.beats.sdk.data;

import java.util.ArrayList;
import java.util.List;

public class Albums extends BaseJson {

    protected String code;
    protected PagingInfo info;
    protected List<Album> data;

    public Albums() {
        this.data = new ArrayList<Album>();
        this.info = new PagingInfo();
    }

    public void fillIn(BaseJson parseJson) throws Exception {
        if (parseJson instanceof Albums) {
            List<Album> temp = ((Albums) parseJson).data;
            this.data.addAll(temp);
            this.info = ((Albums) parseJson).info;
            this.code = ((Albums) parseJson).code;
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

    public List<Album> getData() {
        return data;
    }

    public List<Album> getAlbums() {
        return data;
    }

    public List<String> getCoverArt() {
        List<String> coverArtUrls = new ArrayList<String>();

        for (Album album : data) {
            coverArtUrls.add(album.id);
        }

        return coverArtUrls;
    }
}
