package com.freethinking.beats.sdk.data;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Recommendations come in many types: artist, track, playlist, album
 *
 * This class holds the logic to correctly deserialze the contained content based on a specific value
 * of the "type" attribute. Depending on the type a different class is used to deserialze so that
 * we can properly automatically map the right fields.
 */

/**
 * Here we state what variable inside the class contains the forking data
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
/**
 * Here we control what class gets loaded based on the content of the variable
 */
@JsonSubTypes({
        @JsonSubTypes.Type(value = AlbumRecommendationContent.class, name = "album"),
        @JsonSubTypes.Type(value = PlaylistRecommendationContent.class, name = "playlist"),
        @JsonSubTypes.Type(value = TrackRecommendationContent.class, name = "track"),
        @JsonSubTypes.Type(value = ArtistRecommendationContent.class, name = "artist")
})
public interface RecommendationContent {
    public void fillIn(BaseJson parseJson) throws Exception;
    public String getType();
}
