package com.freethinking.beats.sdk.mappers;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.freethinking.beats.sdk.data.ArtistWrapper;
import com.freethinking.beats.sdk.data.BaseJson;

import java.io.IOException;

public class ArtistMapper extends CommonMapper {
    @Override
    public BaseJson parseJson(String json) {
        ArtistWrapper artistWrapper = new ArtistWrapper();

        try {
            jsonParser = jsonFactory.createParser(json);
            artistWrapper = objMapper.readValue(jsonParser, ArtistWrapper.class);
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return artistWrapper.getArtist();
    }
}
