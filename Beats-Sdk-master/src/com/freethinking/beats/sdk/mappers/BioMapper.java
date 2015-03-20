package com.freethinking.beats.sdk.mappers;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.freethinking.beats.sdk.data.BaseJson;
import com.freethinking.beats.sdk.data.BioWrapper;

import java.io.IOException;

public class BioMapper extends CommonMapper {
    @Override
    public BaseJson parseJson(String json) {
        BioWrapper bioWrapper = new BioWrapper();

        try {
            jsonParser = jsonFactory.createParser(json);
            bioWrapper = objMapper.readValue(jsonParser, BioWrapper.class);
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bioWrapper;
    }
}
