package com.freethinking.beats.sdk.mappers;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.freethinking.beats.sdk.data.BaseJson;
import com.freethinking.beats.sdk.data.UserWrapper;

import java.io.IOException;

public class UserMapper extends CommonMapper {
    @Override
    public BaseJson parseJson(String json) {
        UserWrapper userWrapper = new UserWrapper();

        try {
            jsonParser = jsonFactory.createParser(json);
            userWrapper = objMapper.readValue(jsonParser, UserWrapper.class);
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return userWrapper.getData();
    }
}
