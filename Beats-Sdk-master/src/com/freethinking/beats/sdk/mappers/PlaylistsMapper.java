package com.freethinking.beats.sdk.mappers;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.freethinking.beats.sdk.data.BaseJson;
import com.freethinking.beats.sdk.data.Playlists;

import java.io.IOException;

public class PlaylistsMapper extends CommonMapper {

    @Override
    public BaseJson parseJson(String json) {
        Playlists playlists = new Playlists();

        try {
            jsonParser = jsonFactory.createParser(json);
            playlists = objMapper.readValue(jsonParser, Playlists.class);
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return playlists;
    }
}
