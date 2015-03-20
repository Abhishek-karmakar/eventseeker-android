package com.freethinking.beats.sdk.data;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MeResult extends BaseJson {

    @JsonProperty("client_id")
    protected String clientId;
    @JsonProperty("token_type")
    protected String tokenType;
    @JsonProperty("grant_type")
    protected String grantType;
    protected String expires;
    @JsonProperty("user_context")
    protected String userContext;
    protected String scope;
    protected Boolean extended;

    @Override
    public void fillIn(BaseJson parseJson) throws Exception {
        if (parseJson instanceof MeResult) {
            clientId = ((MeResult) parseJson).clientId;
            tokenType = ((MeResult) parseJson).tokenType;
            grantType = ((MeResult) parseJson).grantType;
            expires = ((MeResult) parseJson).expires;
            userContext = ((MeResult) parseJson).userContext;
            scope = ((MeResult) parseJson).scope;
            extended = ((MeResult) parseJson).extended;
        } else {
            throw new Exception();
        }
    }

    public String getClientId() {
        return clientId;
    }

    public String getTokenType() {
        return tokenType;
    }

    public String getGrantType() {
        return grantType;
    }

    public String getExpires() {
        return expires;
    }

    public String getUserContext() {
        return userContext;
    }

    public String getScope() {
        return scope;
    }

    public Boolean getExtended() {
        return extended;
    }
}
