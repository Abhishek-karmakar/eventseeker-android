package com.freethinking.beats.sdk.data;

import java.util.ArrayList;
import java.util.List;

public class TrackReferenceLinks extends BaseJson {
    protected List<ReferenceLink> artists;
    protected ReferenceLink album;

    public TrackReferenceLinks() {
        this.artists = new ArrayList<ReferenceLink>();
        this.album = new ReferenceLink();
    }

    @Override
    public void fillIn(BaseJson parseJson) throws Exception {
        if (parseJson instanceof TrackReferenceLinks) {
            this.album = ((TrackReferenceLinks) parseJson).album;
            List<ReferenceLink> temp = ((TrackReferenceLinks) parseJson).artists;
            this.artists.addAll(temp);
        } else {
            throw new Exception();
        }
    }

    public List<ReferenceLink> getArtists() {
        return artists;
    }

    public ReferenceLink getAlbum() {
        return album;
    }
}
