package com.freethinking.beats.sdk.data;

public class ReferenceLink extends BaseJson {
    protected String refType;
    protected String id;
    protected String display;

    @Override
    public void fillIn(BaseJson parseJson) throws Exception {
        if (parseJson instanceof ReferenceLink) {
            this.refType = ((ReferenceLink) parseJson).refType;
            this.id = ((ReferenceLink) parseJson).id;
            this.display = ((ReferenceLink) parseJson).display;
        } else {
            throw new Exception();
        }
    }

    public String getRefType() {
        return refType;
    }

    public String getId() {
        return id;
    }

    public String getDisplay() {
        return display;
    }
}
