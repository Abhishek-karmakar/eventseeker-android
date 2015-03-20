package com.freethinking.beats.sdk.mappers;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.freethinking.beats.sdk.data.Activities;
import com.freethinking.beats.sdk.data.BaseJson;

import java.io.IOException;

public class ActivitiesMapper extends CommonMapper {

    @Override
    public BaseJson parseJson(String json) {
        Activities activities = new Activities();

        try {
            jsonParser = jsonFactory.createParser(json);
            activities = objMapper.readValue(jsonParser, Activities.class);
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return activities;
    }
}
