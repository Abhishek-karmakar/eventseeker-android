package com.freethinking.beats.sdk.data;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Returns a single track.
 */
public class Track extends BaseJson {

    protected String type;
    protected String id;
    protected String title;
    @JsonProperty("artist_display_name")
    protected String artistDisplayName;
    protected Long duration;
    @JsonProperty("parental_advisory")
    protected Boolean parentalAdvisory;
    @JsonProperty("edited_version")
    protected Boolean editedVersion;
    protected Long popularity;
    @JsonProperty("track_position")
    protected Integer trackPosition;
    @JsonProperty("disc_number")
    protected Integer diskNumber;
    protected Boolean streamable;
    protected TrackReferenceLinks refs;

    public Track() {
        this.refs = new TrackReferenceLinks();
    }

    @Override
    public void fillIn(BaseJson parseJson) throws Exception {
        if (parseJson instanceof Track) {
            this.type = ((Track) parseJson).type;
            this.id = ((Track) parseJson).id;
            this.title = ((Track) parseJson).title;
            this.artistDisplayName = ((Track) parseJson).artistDisplayName;
            this.duration = ((Track) parseJson).duration;
            this.parentalAdvisory = ((Track) parseJson).parentalAdvisory;
            this.editedVersion = ((Track) parseJson).editedVersion;
            this.popularity = ((Track) parseJson).popularity;
            this.trackPosition = ((Track) parseJson).trackPosition;
            this.diskNumber = ((Track) parseJson).diskNumber;
            this.streamable = ((Track) parseJson).streamable;
            this.refs = ((Track) parseJson).refs;
        } else {
            throw new Exception();
        }
    }

    public String getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtistDisplayName() {
        return artistDisplayName;
    }

    public Long getDuration() {
        return duration;
    }

    public Boolean getParentalAdvisory() {
        return parentalAdvisory;
    }

    public Boolean getEditedVersion() {
        return editedVersion;
    }

    public Long getPopularity() {
        return popularity;
    }

    public Integer getTrackPosition() {
        return trackPosition;
    }

    public Integer getDiskNumber() {
        return diskNumber;
    }

    public Boolean getStreamable() {
        return streamable;
    }

    public TrackReferenceLinks getRefs() {
        return refs;
    }
}
