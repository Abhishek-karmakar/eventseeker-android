package com.freethinking.beats.sdk.data;

import java.util.ArrayList;
import java.util.List;

public class Artists extends BaseJson {
    protected List<Artist> data;
    protected String code;
    protected PagingInfo info;

    public Artists() {
        this.data = new ArrayList<Artist>();
        this.info = new PagingInfo();
    }

    @Override
    public void fillIn(BaseJson parseJson) throws Exception {
        if (parseJson instanceof Artists) {
            List<Artist> temp = ((Artists) parseJson).data;
            this.data.addAll(temp);
            this.info = ((Artists) parseJson).info;
            this.code = ((Artists) parseJson).code;
        } else {
            throw new Exception();
        }
    }

    public List<Artist> getArtists() {
        return data;
    }

    public List<Artist> getData() {
        return data;
    }

    public String getCode() {
        return code;
    }

    public PagingInfo getInfo() {
        return info;
    }
}
