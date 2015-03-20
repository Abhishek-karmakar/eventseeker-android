package com.freethinking.beats.sdk.mappers;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.freethinking.beats.sdk.data.ActivityWrapper;
import com.freethinking.beats.sdk.data.BaseJson;

import java.io.IOException;

public class ActivityMapper extends CommonMapper {

    @Override
    public BaseJson parseJson(String json) {
        ActivityWrapper activity = new ActivityWrapper();

        try {
            jsonParser = jsonFactory.createParser(json);
            activity = objMapper.readValue(jsonParser, ActivityWrapper.class);
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return activity.getData();
    }
}
