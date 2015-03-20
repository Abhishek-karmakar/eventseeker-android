package com.freethinking.beats.sdk.mappers;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.freethinking.beats.sdk.data.Artists;
import com.freethinking.beats.sdk.data.BaseJson;

import java.io.IOException;

public class ArtistsMapper extends CommonMapper {
    @Override
    public BaseJson parseJson(String json) {
        Artists artists = new Artists();

        try {
            jsonParser = jsonFactory.createParser(json);
            artists = objMapper.readValue(jsonParser, Artists.class);
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return artists;
    }
}
