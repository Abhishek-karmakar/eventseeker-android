package com.freethinking.beats.sdk.data;

import java.util.ArrayList;
import java.util.List;

public class SearchResults extends BaseJson {
    protected String code;
    protected PagingInfo info;
    protected List<SearchResult> data;

    public SearchResults() {
        data = new ArrayList<SearchResult>();
        info = new PagingInfo();
    }

    public List<SearchResult> getSearchResults() {
        return data;
    }

    public void fillIn(BaseJson parseJson) throws Exception {
        if (parseJson instanceof SearchResults) {
            List<SearchResult> temp = ((SearchResults) parseJson).data;
            data.addAll(temp);
            this.code = ((SearchResults) parseJson).code;
            this.info = ((SearchResults) parseJson).info;
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

    public List<SearchResult> getData() {
        return data;
    }
}
