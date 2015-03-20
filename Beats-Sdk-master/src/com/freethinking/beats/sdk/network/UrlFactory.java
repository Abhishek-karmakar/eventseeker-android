package com.freethinking.beats.sdk.network;


import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;

import java.util.HashMap;

public class UrlFactory {
    public static final String BASE_URL = "https://partner.api.beatsmusic.com/v1";
    public static final String SEARCH_PREDICTIVE = "/api/search/predictive";

    public final int OFFSET_DEFAULT = 0;
    public final int LIMIT_DEFAULT = 20;
    public final String[] FIELDS_DEFAULT = new String[0];
    public final String[] REFS_DEFAULT = new String[0];
    public final String ORDER_BY_DEFAULT = "popularity desc";
    public final HashMap<String, Boolean> STREAMABILITY_FILTERS_DEFAULT = new HashMap<String, Boolean>();
    public final HashMap<String, String> FILTERS_DEFAULT = new HashMap<String, String>();
    public final String[] IDS_DEFAULT = new String[0];

    @SuppressWarnings("SpellCheckingInspection")
    public static String clientID(Context context) {
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            return ai.metaData.getString("com.freethinking.beats.sdk.id");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (NullPointerException e) {
            e.printStackTrace();
            return null;
        }
    }

    @SuppressWarnings("SpellCheckingInspection")
    public static String clientSecret(Context context) {
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            return ai.metaData.getString("com.freethinking.beats.sdk.secret");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (NullPointerException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Requires Auth
    public static String obtainToken() {
        return "https://partner.api.beatsmusic.com/oauth2/token";
    }

    // Requires Auth
    public static String me() {
        return BASE_URL + "/api/me";
    }

    // Requires Auth
    public static String profile(String id) {
        return BASE_URL + "/api/users/" + id;
    }

    public static String artistList(Context context) {
        return BASE_URL + "/api/artists" + "?client_id=" + clientID(context);
    }

    public static String artistList(Context context, CollectionQueryParams params) {
        return BASE_URL + "/api/artists" + "?client_id=" + clientID(context) + '&' + params.toString();
    }

    public static String artist(Context context, String id) {
        return BASE_URL + "/api/artists/" + id + "?client_id=" + clientID(context);
    }

    public static String artist(Context context, String id, LookupQueryParams params) {
        return BASE_URL + "/api/artists/" + id + "?client_id=" + clientID(context) + '&' + params.toString();
    }

    public static String artistBio(Context context, String id) {
        return BASE_URL + "/api/artists/" + id + "/bios" + "?client_id=" + clientID(context);
    }

    public static String artistBio(Context context, String id, LookupQueryParams params) {
        return BASE_URL + "/api/artists/" + id + "/bios" + "?client_id=" + clientID(context) + '&' + params.toString();
    }

    public static String artistAlbums(Context context, String id) {
        return BASE_URL + "/api/artists/" + id + "/albums" + "?client_id=" + clientID(context);
    }

    public static String artistAlbums(Context context, String id, CollectionQueryParams params) {
        return BASE_URL + "/api/artists/" + id + "/albums" + "?client_id=" + clientID(context) + '&' + params.toString();
    }

    public static String artistEssentialAlbums(Context context, String id) {
        return BASE_URL + "/api/artists/" + id + "/essential_albums" + "?client_id=" + clientID(context);
    }

    public static String artistEssentialAlbums(Context context, String id, CollectionQueryParams params) {
        return BASE_URL + "/api/artists/" + id + "/essential_albums" + "?client_id=" + clientID(context) + '&' + params.toString();
    }

    public static String artistTracks(Context context, String id) {
        return BASE_URL + "/api/artists/" + id + "/tracks" + "?client_id=" + clientID(context);
    }

    public static String artistTracks(Context context, String id, CollectionQueryParams params) {
        return BASE_URL + "/api/artists/" + id + "/tracks" + "?client_id=" + clientID(context) + '&' + params.toString();
    }

    public static String artistPlaylists(Context context, String id) {
        return BASE_URL + "/api/artists/" + id + "/playlists" + "?client_id=" + clientID(context);
    }

    public static String artistPlaylists(Context context, String id, CollectionQueryParams params) {
        return BASE_URL + "/api/artists/" + id + "/playlists" + "?client_id=" + clientID(context) + '&' + params.toString();
    }

    public static String activitiesList(Context context) {
        return BASE_URL + "/api/activities" + "?client_id=" + clientID(context);
    }

    public static String activitiesList(Context context, CollectionQueryParams params) {
        return BASE_URL + "/api/activities" + "?client_id=" + clientID(context) + '&' + params.toString();
    }

    public static String activity(Context context, String id) {
        return BASE_URL + "/api/activities/" + id + "?client_id=" + clientID(context);
    }

    public static String activity(Context context, String id, LookupQueryParams params) {
        return BASE_URL + "/api/activities/" + id + "?client_id=" + clientID(context) + '&' + params.toString();
    }

    public static String activityEditorialPlaylists(Context context, String id) {
        return BASE_URL + "/api/activities/" + id + "/editorial_playlists" + "?client_id=" + clientID(context);
    }

    public static String activityEditorialPlaylists(Context context, String id, CollectionQueryParams params) {
        return BASE_URL + "/api/activities/" + id + "/editorial_playlists" + "?client_id=" + clientID(context) + '&' + params.toString();
    }

    public static String albumList(Context context) {
        return BASE_URL + "/api/albums" + "?client_id=" + clientID(context);
    }

    public static String albumList(Context context, CollectionQueryParams params) {
        return BASE_URL + "/api/albums" + "?client_id=" + clientID(context) + '&' + params.toString();
    }

    public static String album(Context context, String id) {
        return BASE_URL + "/api/albums/" + id + "?client_id=" + clientID(context);
    }

    public static String album(Context context, String id, LookupQueryParams params) {
        return BASE_URL + "/api/albums/" + id + "?client_id=" + clientID(context) + '&' + params.toString();
    }

    public static String albumArtists(Context context, String id) {
        return BASE_URL + "/api/albums/" + id + "/artists" + "?client_id=" + clientID(context);
    }

    public static String albumArtists(Context context, String id, CollectionQueryParams params) {
        return BASE_URL + "/api/albums/" + id + "/artists" + "?client_id=" + clientID(context) + '&' + params.toString();
    }

    public static String albumTracks(Context context, String id) {
        return BASE_URL + "/api/albums/" + id + "/tracks" + "?client_id=" + clientID(context);
    }

    public static String albumTracks(Context context, String id, CollectionQueryParams params) {
        return BASE_URL + "/api/albums/" + id + "/tracks" + "?client_id=" + clientID(context) + '&' + params.toString();
    }

    public static String albumReview(Context context, String id) {
        return BASE_URL + "/api/albums/" + id + "/review" + "?client_id=" + clientID(context);
    }

    public static String albumReviews(Context context, String id, CollectionQueryParams params) {
        return BASE_URL + "/api/albums/" + id + "/reviews" + "?client_id=" + clientID(context) + '&' + params.toString();
    }

    public static String albumCompanionAlbums(Context context, String id) {
        return BASE_URL + "/api/albums/" + id + "/companion_albums" + "?client_id=" + clientID(context);
    }

    public static String albumCompanionAlbums(Context context, String id, CollectionQueryParams params) {
        return BASE_URL + "/api/albums/" + id + "/companion_albums" + "?client_id=" + clientID(context) + '&' + params.toString();
    }

    public static String imageUrl(Context context, String id, EntityType entity, ImageType type) {
        return imageUrl(context, id, entity, type, ImageSize.MEDIUM);
    }

    public static String imageUrl(Context context, String id, EntityType entity, ImageType type, ImageSize size) {
        if (!imageTypeEntityValidation(type, entity)) {
            throw new IllegalArgumentException(entity + " does not have image type of " + type);
        } else {
            return BASE_URL + "/api/" + entity.toString() + "/" + id + "/images/" + type.toString() + "?client_id=" + clientID(context) + "&size=" + size.toString();
        }
    }

    private static boolean imageTypeEntityValidation(ImageType type, EntityType entity) {
        if (ImageType.COVER.equals(type)) {
            if (EntityType.GENRE.equals(entity) || EntityType.USER.equals(entity)) {
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    public static String highlightsFeatured(Context context) {
        return BASE_URL + "/api/discoveries/featured" + "?client_id=" + clientID(context);
    }

    public static String highlightsFeatured(Context context, CollectionQueryParams params) {
        return BASE_URL + "/api/discoveries/featured" + "?client_id=" + clientID(context) + '&' + params.toString();
    }

    public static String highlightsEditorPicks(Context context) {
        return BASE_URL + "/api/discoveries/editor_picks" + "?client_id=" + clientID(context);
    }

    public static String highlightsEditorPicks(Context context, CollectionQueryParams params) {
        return BASE_URL + "/api/discoveries/editor_picks" + "?client_id=" + clientID(context) + '&' + params.toString();
    }

    public static String trackList(Context context) {
        return BASE_URL + "/api/tracks" + "?client_id=" + clientID(context);
    }

    public static String trackList(Context context, CollectionQueryParams params) {

        return BASE_URL + "/api/tracks" + "?client_id=" + clientID(context) + '&' + params.toString();
    }

    public static String track(Context context, String id) {
        return BASE_URL + "/api/tracks/" + id + "?client_id=" + clientID(context);
    }

    public static String track(Context context, String id, LookupQueryParams params) {
        return BASE_URL + "/api/tracks/" + id + "?client_id=" + clientID(context) + '&' + params.toString();
    }

    public static String trackArtists(Context context, String id) {
        return BASE_URL + "/api/tracks/" + id + "/artists" + "?client_id=" + clientID(context);
    }

    public static String trackArtists(Context context, String id, CollectionQueryParams params) {
        return BASE_URL + "/api/tracks/" + id + "/artists" + "?client_id=" + clientID(context) + '&' + params.toString();
    }

    public static String playlistList(CollectionQueryParams params) {
        if (params.ids == null || params.ids.length == 0) {
            throw new IllegalArgumentException("Ids are required for lookup");
        }
        return BASE_URL + "/api/playlists?" + params.toString();
    }

    public static String playlist(String id) {
        return BASE_URL + "/api/playlists/" + id;
    }

    public static String playlist(String id, LookupQueryParams params) {
        return BASE_URL + "/api/playlists/" + id + '?' + params.toString();
    }

    public static String playlistSubscribers(String id) {
        return BASE_URL + "/api/playlists/" + id + "/subscribers";
    }

    public static String playlistSubscribers(String id, CollectionQueryParams params) {
        return BASE_URL + "/api/playlists/" + id + "/subscribers" + '?' + params.toString();
    }

    public static String playlistTracks(String id) {
        return BASE_URL + "/api/playlists/" + id + "/tracks";
    }

    public static String playlistTracks(String id, CollectionQueryParams params) {
        return BASE_URL + "/api/playlists/" + id + "/tracks" + '?' + params.toString();
    }

    public static String playlistArtists(String id) {
        return BASE_URL + "/api/playlists/" + id + "/artists";
    }

    public static String playlistArtists(String id, CollectionQueryParams params) {
        return BASE_URL + "/api/playlists/" + id + "/artists" + '?' + params.toString();
    }

    public static String usersPlaylists(String id) {
        return BASE_URL + "/api/users/" + id + "/playlists";
    }

    public static String usersPlaylists(String id, CollectionQueryParams params) {
        return BASE_URL + "/api/users/" + id + "/playlists" + '?' + params.toString();
    }

    public static String usersSubscribedPlaylists(String id) {
        return BASE_URL + "/api/users/" + id + "/playlist_subscriptions";
    }

    public static String usersSubscribedPlaylists(String id, CollectionQueryParams params) {
        return BASE_URL + "/api/users/" + id + "/playlist_subscriptions" + '?' + params.toString();
    }
    
    public static String usersMusicLibraryArtists(String id, String accessToken) {
    	return BASE_URL + "/api/users/" + id + "/mymusic/artists" + '?' + "access_token=" + accessToken;
    }

    public static String searchPredictive(Context context, String searchText) {
        return BASE_URL + SEARCH_PREDICTIVE + "?q=" + searchText + "&client_id=" + clientID(context);
    }

    public static String genre(String id) {
        return BASE_URL + "/api/genres/" + id;
    }

    public static String genre(String id, LookupQueryParams params) {
        return BASE_URL + "/api/genres/" + id + '?' + params.toString();
    }

    public static String genreEditorPicks(String id) {
        return BASE_URL + "/api/genres/" + id + "/editor_picks";
    }

    public static String genreEditorPicks(String id, CollectionQueryParams params) {
        return BASE_URL + "/api/genres/" + id + "/editor_picks" + '?' + params.toString();
    }

    public static String genreFeatured(String id) {
        return BASE_URL + "/api/genres/" + id + "/featured";
    }

    public static String genreFeatured(String id, CollectionQueryParams params) {
        return BASE_URL + "/api/genres/" + id + "/featured" + '?' + params.toString();
    }

    public static String genreNewReleases(String id) {
        return BASE_URL + "/api/genres/" + id + "/new_releases";
    }

    public static String genreNewReleases(String id, CollectionQueryParams params) {
        return BASE_URL + "/api/genres/" + id + "/new_releases" + '?' + params.toString();
    }

    public static String genreBios(String id) {
        return BASE_URL + "/api/genres/" + id + "/bios";
    }

    public static String genreBios(String id, CollectionQueryParams params) {
        return BASE_URL + "/api/genres/" + id + "/bios" + '?' + params.toString();
    }

    public static String genrePlaylists(String id) {
        return BASE_URL + "/api/genres/" + id + "/playlists";
    }

    public static String genrePlaylists(String id, CollectionQueryParams params) {
        return BASE_URL + "/api/genres/" + id + "/playlists" + '?' + params.toString();
    }

    public static String genresCollection(Context context) {
        return BASE_URL + "/api/genres" + "?client_id=" + clientID(context);
    }

    public static String genresCollection(Context context, CollectionQueryParams params) {
        return BASE_URL + "/api/genres" + "?client_id=" + clientID(context) + '&' + params.toString();
    }

    public enum EntityType {
        GENRE("genres"),
        ARTIST("artists"),
        PLAYLIST("playlists"),
        ALBUM("albums"),
        TRACK("tracks"),
        USER("users");

        private final String type;

        private EntityType(final String type) {
            this.type = type;
        }

        public String toString() {
            return type;
        }
    }

    public enum ImageType {
        DEFAULT("default"),
        COVER("cover");

        private final String type;

        private ImageType(final String type) {
            this.type = type;
        }

        public String toString() {
            return type;
        }
    }

    public enum ImageSize {
        THUMB("thumb"),
        SMALL("small"),
        MEDIUM("medium"),
        LARGE("large");

        private final String type;

        private ImageSize(final String type) {
            this.type = type;
        }

        public String toString() {
            return type;
        }
    }

    public class LookupQueryParams {
        protected String[] fields;
        protected String[] refs;

        public LookupQueryParams() {
            this(new String[0], new String[0]);
        }

        public LookupQueryParams(String[] fields, String[] refs) {
            this.fields = fields;
            this.refs = refs;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();

            if (fields.length > 0) {
                for (String field : fields) {
                    sb.append("fields=").append(field).append('&');
                }
            }

            if (refs.length > 0) {
                for (String ref : refs) {
                    sb.append("refs=").append(ref).append('&');
                }
            }

            return sb.toString();
        }
    }

    public class CollectionQueryParams {

        protected int offset;
        protected int limit;
        protected String[] fields;
        protected String[] refs;
        protected String orderBy;
        protected HashMap<String, Boolean> streamabilityFilters;
        protected HashMap<String, String> filters;
        protected String[] ids;

        public CollectionQueryParams() {
            this(OFFSET_DEFAULT, LIMIT_DEFAULT, FIELDS_DEFAULT, REFS_DEFAULT, ORDER_BY_DEFAULT, STREAMABILITY_FILTERS_DEFAULT, FILTERS_DEFAULT, IDS_DEFAULT);
        }

        public CollectionQueryParams(int offset, int limit, String[] fields, String[] refs, String orderBy, HashMap<String, Boolean> streamabilityFilters, HashMap<String, String> filters, String[] ids) {
            this.offset = offset;
            this.limit = limit;
            this.fields = fields;
            this.refs = refs;
            this.orderBy = orderBy;
            this.streamabilityFilters = streamabilityFilters;
            this.filters = filters;
            this.ids = ids;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();

            if (ids.length == 0) {
                sb.append("offset=").append(offset).append('&');
                sb.append("limit=").append(limit).append('&');

                if (fields.length > 0) {
                    for (String field : fields) {
                        sb.append("fields=").append(field).append('&');
                    }
                }

                if (refs.length > 0) {
                    for (String ref : refs) {
                        sb.append("refs=").append(ref).append('&');
                    }
                }

                sb.append("order_by=").append(Uri.encode(orderBy)).append('&');

                if (!streamabilityFilters.isEmpty()) {
                    for (String key : streamabilityFilters.keySet()) {
                        sb.append("filters=").append(key).append('=').append(streamabilityFilters.get(key)).append('&');
                    }
                }

                if (!filters.isEmpty()) {
                    for (String key : filters.keySet()) {
                        sb.append("filters=").append(key).append('=').append(filters.get(key)).append('&');
                    }
                }
            } else {
                for (String id : ids) {
                    sb.append("ids=").append(id).append('&');
                }
            }

            return sb.toString();
        }
    }
}