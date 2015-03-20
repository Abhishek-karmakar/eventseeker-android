package com.freethinking.beats.sdk.data;

/**
 * Not wanting to duplicate the object in two places we simply extend it. We need this middle class
 * in order to maintain the correct object hierarchy in the class containing recommendations.
 */
public class PlaylistRecommendationContent extends Playlist implements RecommendationContent {
    public PlaylistRecommendationContent() {
        super();
        this.type = "playlist";
    }}
