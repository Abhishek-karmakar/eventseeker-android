package com.freethinking.beats.sdk.mappers;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.freethinking.beats.sdk.data.BaseJson;
import com.freethinking.beats.sdk.data.Highlights;

import java.io.IOException;

public class HighlightsMapper extends CommonMapper {

    @Override
    public BaseJson parseJson(String json) {
        Highlights highlights = new Highlights();

        try {
            jsonParser = jsonFactory.createParser(json);
            highlights = objMapper.readValue(jsonParser, Highlights.class);
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return highlights;
    }
}
