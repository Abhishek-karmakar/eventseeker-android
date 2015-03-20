package com.freethinking.beats.sdk.data;

import java.util.ArrayList;
import java.util.List;

public class BioWrapper extends BaseJson {
    protected List<Bio> data;
    protected String code;
    protected PagingInfo info;

    public BioWrapper() {
        this.data = new ArrayList<Bio>();
        this.info = new PagingInfo();
    }

    @Override
    public void fillIn(BaseJson parseJson) throws Exception {
        if (parseJson instanceof BioWrapper) {
            data = ((BioWrapper) parseJson).data;
            code = ((BioWrapper) parseJson).code;
            info = ((BioWrapper) parseJson).info;

        } else {
            throw new Exception();
        }
    }

    public List<Bio> getData() {
        return data;
    }

    public PagingInfo getInfo() {
        return info;
    }

    public String getCode() {
        return code;
    }
}
