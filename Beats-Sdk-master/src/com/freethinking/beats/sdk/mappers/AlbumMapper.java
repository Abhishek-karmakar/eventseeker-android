package com.freethinking.beats.sdk.mappers;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.freethinking.beats.sdk.data.AlbumWrapper;
import com.freethinking.beats.sdk.data.BaseJson;

import java.io.IOException;

public class AlbumMapper extends CommonMapper {
    @Override
    public BaseJson parseJson(String json) {
        AlbumWrapper albumWrapper = new AlbumWrapper();

        try {
            jsonParser = jsonFactory.createParser(json);
            albumWrapper = objMapper.readValue(jsonParser, AlbumWrapper.class);
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return albumWrapper.getAlbum();
    }
}
