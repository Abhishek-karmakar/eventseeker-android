package com.freethinking.beats.sdk.data;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Playlist extends BaseJson {

    protected String type;
    protected String id;
    protected String name;
    protected String description;
    @JsonProperty("user_display_name")
    protected String userDisplayName;
    @JsonProperty("total_tracks")
    protected String totalTracks;
    @JsonProperty("total_subscribers")
    protected String totalSubscribers;
    protected Long duration;
    protected Long createdAt;
    protected Long updatedAt;
    @JsonProperty("parental_advisory")
    protected Boolean parentalAdvisory;
    protected PlaylistReferenceLinks refs;

    public Playlist() {
        this.refs = new PlaylistReferenceLinks();
    }

    @Override
    public void fillIn(BaseJson parseJson) throws Exception {
        if (parseJson instanceof Playlist) {
            this.type = ((Playlist) parseJson).type;
            this.id = ((Playlist) parseJson).id;
            this.name = ((Playlist) parseJson).name;
            this.description = ((Playlist) parseJson).description;
            this.userDisplayName = ((Playlist) parseJson).userDisplayName;
            this.totalTracks = ((Playlist) parseJson).totalTracks;
            this.totalSubscribers = ((Playlist) parseJson).totalSubscribers;
            this.duration = ((Playlist) parseJson).duration;
            this.createdAt = ((Playlist) parseJson).createdAt;
            this.updatedAt = ((Playlist) parseJson).updatedAt;
            this.parentalAdvisory = ((Playlist) parseJson).parentalAdvisory;
            this.refs = ((Playlist) parseJson).refs;
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

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getUserDisplayName() {
        return userDisplayName;
    }

    public String getTotalTracks() {
        return totalTracks;
    }

    public String getTotalSubscribers() {
        return totalSubscribers;
    }

    public Long getDuration() {
        return duration;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public Boolean getParentalAdvisory() {
        return parentalAdvisory;
    }

    public PlaylistReferenceLinks getRefs() {
        return refs;
    }
}
