package com.freethinking.beats.sdk.mappers;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.freethinking.beats.sdk.data.BaseJson;
import com.freethinking.beats.sdk.data.GenreWrapper;

import java.io.IOException;

public class GenreMapper extends CommonMapper {

    @Override
    public BaseJson parseJson(String json) {
        GenreWrapper genre = new GenreWrapper();

        try {
            jsonParser = jsonFactory.createParser(json);
            genre = objMapper.readValue(jsonParser, GenreWrapper.class);
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return genre.getData();
    }
}
