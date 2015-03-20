package com.freethinking.beats.sdk.data;

import java.util.ArrayList;
import java.util.List;

public class AlbumReferenceLinks extends BaseJson {
    protected List<ReferenceLink> artists;
    protected ReferenceLink label;
    protected List<ReferenceLink> tracks;

    public AlbumReferenceLinks() {
        this.artists = new ArrayList<ReferenceLink>();
        this.label = new ReferenceLink();
        this.tracks = new ArrayList<ReferenceLink>();
    }

    public List<ReferenceLink> getArtists() {
        return artists;
    }

    public ReferenceLink getLabel() {
        return label;
    }

    public List<ReferenceLink> getTracks() {
        return tracks;
    }

    public void fillIn(BaseJson parseJson) throws Exception {
        if (parseJson instanceof AlbumReferenceLinks) {
            List<ReferenceLink> temp = ((AlbumReferenceLinks) parseJson).artists;
            this.artists.addAll(temp);
            this.label = ((AlbumReferenceLinks) parseJson).label;
            temp = ((AlbumReferenceLinks) parseJson).tracks;
            this.tracks.addAll(temp);
        } else {
            throw new Exception();
        }
    }
}
