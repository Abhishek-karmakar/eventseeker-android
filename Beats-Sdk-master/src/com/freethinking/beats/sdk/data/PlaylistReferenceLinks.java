package com.freethinking.beats.sdk.data;

import java.util.ArrayList;
import java.util.List;

public class PlaylistReferenceLinks extends BaseJson {
    protected List<ReferenceLink> tracks;
    protected ReferenceLink user;
    protected ReferenceLink author;

    public PlaylistReferenceLinks() {
        this.tracks = new ArrayList<ReferenceLink>();
        this.user = new ReferenceLink();
        this.author = new ReferenceLink();
    }

    public void fillIn(BaseJson parseJson) throws Exception {
        if (parseJson instanceof PlaylistReferenceLinks) {
            List<ReferenceLink> temp = ((PlaylistReferenceLinks) parseJson).tracks;
            this.tracks.addAll(temp);
            this.user = ((PlaylistReferenceLinks) parseJson).user;
            this.author = ((PlaylistReferenceLinks) parseJson).author;
        } else {
            throw new Exception();
        }
    }

    public List<ReferenceLink> getTracks() {
        return tracks;
    }

    public ReferenceLink getUser() {
        return user;
    }

    public ReferenceLink getAuthor() {
        return author;
    }
}
