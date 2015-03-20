package com.freethinking.beats.sdk.mappers;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.freethinking.beats.sdk.data.BaseJson;
import com.freethinking.beats.sdk.data.TrackWrapper;

import java.io.IOException;

public class TrackMapper extends CommonMapper {
    @Override
    public BaseJson parseJson(String json) {
        TrackWrapper trackWrapper = new TrackWrapper();

        try {
            jsonParser = jsonFactory.createParser(json);
            trackWrapper = objMapper.readValue(jsonParser, TrackWrapper.class);
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return trackWrapper.getData();
    }
}
