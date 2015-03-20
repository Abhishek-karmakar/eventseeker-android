package com.freethinking.beats.sdk.data;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Artist extends BaseJson {
    protected String type;
    protected String id;
    protected String name;
    protected int popularity;
    protected Boolean streamable;
    @JsonProperty("total_albums")
    protected String totalAlbums;
    @JsonProperty("total_singles")
    protected String totalSingles;
    @JsonProperty("total_eps")
    protected String totalEps;
    @JsonProperty("total_lps")
    protected String totalLps;
    @JsonProperty("total_freeplays")
    protected Integer totalFreeplays;
    @JsonProperty("total_compilations")
    protected String totalCompilations;
    @JsonProperty("total_tracks")
    protected String totalTracks;
    protected ArtistReferenceLinks refs;
    protected Boolean verified;
    @JsonProperty("total_follows")
    protected Integer totalFollows;
    @JsonProperty("total_followed_by")
    protected Integer totalFollowedBy;

    public String getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getPopularity() {
        return popularity;
    }

    public Boolean getStreamable() {
        return streamable;
    }

    public String getTotalAlbums() {
        return totalAlbums;
    }

    public String getTotalSingles() {
        return totalSingles;
    }

    public String getTotalEps() {
        return totalEps;
    }

    public String getTotalLps() {
        return totalLps;
    }

    public Integer getTotalFreeplays() {
        return totalFreeplays;
    }

    public String getTotalCompilations() {
        return totalCompilations;
    }

    public String getTotalTracks() {
        return totalTracks;
    }

    public ArtistReferenceLinks getRefs() {
        return refs;
    }

    public Boolean getVerified() {
        return verified;
    }

    public Integer getTotalFollows() {
        return totalFollows;
    }

    public Integer getTotalFollowedBy() {
        return totalFollowedBy;
    }

    @Override
    public void fillIn(BaseJson parseJson) throws Exception {
        if (parseJson instanceof Artist) {
            this.type = ((Artist) parseJson).type;
            this.id = ((Artist) parseJson).id;
            this.name = ((Artist) parseJson).name;
            this.popularity = ((Artist) parseJson).popularity;
            this.streamable = ((Artist) parseJson).streamable;
            this.totalAlbums = ((Artist) parseJson).totalAlbums;
            this.totalSingles = ((Artist) parseJson).totalSingles;
            this.totalEps = ((Artist) parseJson).totalEps;
            this.totalLps = ((Artist) parseJson).totalLps;
            this.totalFreeplays = ((Artist) parseJson).totalFreeplays;
            this.totalCompilations = ((Artist) parseJson).totalCompilations;
            this.totalTracks = ((Artist) parseJson).totalTracks;
            this.refs = ((Artist) parseJson).refs;
            this.verified = ((Artist) parseJson).verified;
            this.totalFollows = ((Artist) parseJson).totalFollows;
            this.totalFollowedBy = ((Artist) parseJson).totalFollowedBy;
        } else {
            throw new Exception();
        }

    }
}
