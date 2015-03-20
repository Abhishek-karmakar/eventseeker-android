package com.freethinking.beats.sdk.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Returns a list of genres.
 */
public class Genres extends BaseJson {
    protected List<Genre> data;
    protected String code;
    protected PagingInfo info;

    public Genres() {
        this.data = new ArrayList<Genre>();
        this.info = new PagingInfo();
    }

    @Override
    public void fillIn(BaseJson parseJson) throws Exception {
        if (parseJson instanceof Genres) {
            List<Genre> temp = ((Genres) parseJson).data;
            this.data.addAll(temp);
            this.info = ((Genres) parseJson).info;
            this.code = ((Genres) parseJson).code;
        } else {
            throw new Exception();
        }
    }

    public List<Genre> getGenres() {
        return data;
    }

    public List<Genre> getData() {
        return data;
    }

    public String getCode() {
        return code;
    }

    public PagingInfo getInfo() {
        return info;
    }
}
