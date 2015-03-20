package com.freethinking.beats.sdk.mappers;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.freethinking.beats.sdk.data.Authorization;
import com.freethinking.beats.sdk.data.BaseJson;

import java.io.IOException;

public class AuthorizationMapper extends CommonMapper {

    @Override
    public BaseJson parseJson(String json) {
        Authorization authorization = new Authorization();

        try {
            jsonParser = jsonFactory.createParser(json);
            authorization = objMapper.readValue(jsonParser, Authorization.class);
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return authorization;
    }
}
