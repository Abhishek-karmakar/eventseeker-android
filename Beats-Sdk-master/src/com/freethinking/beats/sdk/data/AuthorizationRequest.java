package com.freethinking.beats.sdk.data;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthorizationRequest extends BaseJson {

    @JsonProperty("client_secret")
    protected String clientSecret;
    @JsonProperty("client_id")
    protected String clientId;
    @JsonProperty("redirect_uri")
    protected String redirectUri;
    protected String code;
    @JsonProperty("refresh_token")
    protected String refreshToken;
    @JsonProperty("grant_type")
    protected String grantType;

    public AuthorizationRequest(String clientSecret, String clientId, String redirectUri, String code) {
        this(clientSecret, clientId, redirectUri, code, "authorization_code", false);
    }

    public AuthorizationRequest(String clientSecret, String clientId, String redirectUri, String code, String grantType, boolean refresh) {
        this.clientSecret = clientSecret;
        this.clientId = clientId;
        this.redirectUri = redirectUri;
        if (refresh) {
            this.refreshToken = code;
        } else {
            this.code = code;
        }
        this.grantType = grantType;
    }

    @Override
    public void fillIn(BaseJson parseJson) throws Exception {

    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}
