package com.freethinking.beats.sdk.data;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthorizationResult extends BaseJson {

    @JsonProperty("return_type")
    protected String returnType;
    @JsonProperty("access_token")
    protected String accessToken;
    @JsonProperty("token_type")
    protected String tokenType;
    @JsonProperty("refresh_token")
    protected String refreshToken;
    @JsonProperty("expires_in")
    protected Integer expiresIn;
    protected String scope;
    protected String state;
    protected String uri;
    protected Boolean extended;

    @Override
    public void fillIn(BaseJson parseJson) throws Exception {
        if (parseJson instanceof AuthorizationResult) {
            returnType = ((AuthorizationResult) parseJson).returnType;
            accessToken = ((AuthorizationResult) parseJson).accessToken;
            tokenType = ((AuthorizationResult) parseJson).tokenType;
            refreshToken = ((AuthorizationResult) parseJson).refreshToken;
            scope = ((AuthorizationResult) parseJson).scope;
            state = ((AuthorizationResult) parseJson).state;
            uri = ((AuthorizationResult) parseJson).uri;
            extended = ((AuthorizationResult) parseJson).extended;
        } else {
            throw new Exception();
        }
    }

    public String getReturnType() {
        return returnType;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getScope() {
        return scope;
    }

    public String getState() {
        return state;
    }

    public String getUri() {
        return uri;
    }

    public Boolean getExtended() {
        return extended;
    }

    public Integer getExpiresIn() {
        return expiresIn;
    }
}
