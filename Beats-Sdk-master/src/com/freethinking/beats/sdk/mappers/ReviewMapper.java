package com.freethinking.beats.sdk.mappers;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.freethinking.beats.sdk.data.BaseJson;
import com.freethinking.beats.sdk.data.ReviewWrapper;

import java.io.IOException;

public class ReviewMapper extends CommonMapper {

    @Override
    public BaseJson parseJson(String json) {
        ReviewWrapper reviewWrapper = new ReviewWrapper();

        try {
            jsonParser = jsonFactory.createParser(json);
            reviewWrapper = objMapper.readValue(jsonParser, ReviewWrapper.class);
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return reviewWrapper.getData();
    }
}
