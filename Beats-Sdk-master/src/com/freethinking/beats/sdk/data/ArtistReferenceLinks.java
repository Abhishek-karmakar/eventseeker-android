package com.freethinking.beats.sdk.data;

import java.util.ArrayList;
import java.util.List;

public class ArtistReferenceLinks extends BaseJson {
    protected List<ReferenceLink> similars;

    public ArtistReferenceLinks() {
        this.similars = new ArrayList<ReferenceLink>();
    }

    public List<ReferenceLink> getSimilars() {
        return similars;
    }

    public void fillIn(BaseJson parseJson) throws Exception {
        if (parseJson instanceof ArtistReferenceLinks) {
            List<ReferenceLink> temp = ((ArtistReferenceLinks) parseJson).similars;
            this.similars.addAll(temp);
        } else {
            throw new Exception();
        }
    }
}
