package com.freethinking.beats.sdk.data;

import java.util.ArrayList;
import java.util.List;

public class Highlights extends BaseJson {
    protected List<Highlight> data;
    protected String code;
    protected PagingInfo info;

    public Highlights() {
        this.data = new ArrayList<Highlight>();
        this.info = new PagingInfo();
    }

    @Override
    public void fillIn(BaseJson parseJson) throws Exception {
        if (parseJson instanceof Highlights) {
            List<Highlight> temp = ((Highlights) parseJson).data;
            this.data.addAll(temp);
            this.info = ((Highlights) parseJson).info;
            this.code = ((Highlights) parseJson).code;
        } else {
            throw new Exception();
        }
    }

    public List<Highlight> getHighlights() {
        return data;
    }

    public List<Highlight> getData() {
        return data;
    }

    public String getCode() {
        return code;
    }

    public PagingInfo getInfo() {
        return info;
    }
}
