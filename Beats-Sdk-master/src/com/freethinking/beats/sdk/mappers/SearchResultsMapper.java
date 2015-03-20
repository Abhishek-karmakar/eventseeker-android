package com.freethinking.beats.sdk.mappers;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.freethinking.beats.sdk.data.BaseJson;
import com.freethinking.beats.sdk.data.SearchResults;

import java.io.IOException;

public class SearchResultsMapper extends CommonMapper {
    @Override
    public BaseJson parseJson(String json) {
        SearchResults searchResults = new SearchResults();

        try {
            jsonParser = jsonFactory.createParser(json);
            searchResults = objMapper.readValue(jsonParser, SearchResults.class);
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return searchResults;
    }
}
