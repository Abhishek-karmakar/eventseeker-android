package com.freethinking.beats.sdk.data;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Album extends BaseJson {

    protected String type;
    protected String id;
    protected String title;
    protected Integer duration;
    @JsonProperty("parental_advisory")
    protected Boolean parentalAdvisory;
    @JsonProperty("release_date")
    protected String releaseDate;
    @JsonProperty("release_format")
    protected String releaseFormat;
    protected Integer rating;
    protected Integer popularity;
    protected Boolean streamable;
    @JsonProperty("artist_display_name")
    protected String artistDisplayName;
    protected Boolean canonical;
    @JsonProperty("total_companion_albums")
    protected Integer totalCompanionAlbums;
    @JsonProperty("total_tracks")
    protected Integer totalTracks;
    protected Boolean essential;

    protected AlbumReferenceLinks refs;

    public Album() {
        refs = new AlbumReferenceLinks();
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

    public Integer getDuration() {
        return duration;
    }

    public Boolean getParentalAdvisory() {
        return parentalAdvisory;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public String getReleaseFormat() {
        return releaseFormat;
    }

    public Integer getRating() {
        return rating;
    }

    public Integer getPopularity() {
        return popularity;
    }

    public Boolean getStreamable() {
        return streamable;
    }

    public String getArtistDisplayName() {
        return artistDisplayName;
    }

    public Boolean getCanonical() {
        return canonical;
    }

    public Integer getTotalCompanionAlbums() {
        return totalCompanionAlbums;
    }

    public Integer getTotalTracks() {
        return totalTracks;
    }

    public Boolean getEssential() {
        return essential;
    }

    public AlbumReferenceLinks getRefs() {
        return refs;
    }

    @Override
    public void fillIn(BaseJson parseJson) throws Exception {
        if (parseJson instanceof Album) {
            this.type = ((Album) parseJson).type;
            this.id = ((Album) parseJson).id;
            this.title = ((Album) parseJson).title;
            this.duration = ((Album) parseJson).duration;
            this.parentalAdvisory = ((Album) parseJson).parentalAdvisory;
            this.releaseDate = ((Album) parseJson).releaseDate;
            this.releaseFormat = ((Album) parseJson).releaseFormat;
            this.rating = ((Album) parseJson).rating;
            this.popularity = ((Album) parseJson).popularity;
            this.streamable = ((Album) parseJson).streamable;
            this.artistDisplayName = ((Album) parseJson).artistDisplayName;
            this.refs = ((Album) parseJson).refs;
            this.canonical = ((Album) parseJson).canonical;
            this.totalCompanionAlbums = ((Album) parseJson).totalCompanionAlbums;
            this.totalTracks = ((Album) parseJson).totalTracks;
            this.essential = ((Album) parseJson).essential;
        } else {
            throw new Exception();
        }
    }
}
