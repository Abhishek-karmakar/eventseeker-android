package com.freethinking.beats.sdk.mappers;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.freethinking.beats.sdk.data.BaseJson;
import com.freethinking.beats.sdk.data.Genres;

import java.io.IOException;

/**
 * Maps the list of genres.
 */
public class GenresMapper extends CommonMapper {

    @Override
    public BaseJson parseJson(String json) {
        Genres genres = new Genres();

        try {
            jsonParser = jsonFactory.createParser(json);
            genres = objMapper.readValue(jsonParser, Genres.class);
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return genres;
    }
}
