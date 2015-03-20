package com.freethinking.beats.sdk.data;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SearchResult extends BaseJson {

    protected String type;
    @JsonProperty("result_type")
    protected String resultType;
    protected String id;
    protected String display;
    protected String detail;
    protected ReferenceLink related;

    public SearchResult() {
        related = new ReferenceLink();
    }

    @Override
    public void fillIn(BaseJson parseJson) throws Exception {
        if (parseJson instanceof SearchResult) {
            this.related = ((SearchResult) parseJson).related;
            this.type = ((SearchResult) parseJson).type;
            this.resultType = ((SearchResult) parseJson).resultType;
            this.id = ((SearchResult) parseJson).id;
            this.display = ((SearchResult) parseJson).display;
            this.detail = ((SearchResult) parseJson).detail;
        } else {
            throw new Exception();
        }
    }

    public String getType() {
        return type;
    }

    public String getResultType() {
        return resultType;
    }

    public String getId() {
        return id;
    }

    public String getDisplay() {
        return display;
    }

    public String getDetail() {
        return detail;
    }

    public ReferenceLink getRelated() {
        return related;
    }
}
