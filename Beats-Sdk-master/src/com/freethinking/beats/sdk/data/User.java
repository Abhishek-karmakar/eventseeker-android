package com.freethinking.beats.sdk.data;

import com.fasterxml.jackson.annotation.JsonProperty;

public class User extends BaseJson {

    protected String id;
    protected String country;
    protected String username;
    @JsonProperty("full_name")
    protected String fullName;
    @JsonProperty("total_followed_by")
    protected Integer totalFollowedBy;
    @JsonProperty("total_follows")
    protected Integer totalFollows;

    @Override
    public void fillIn(BaseJson parseJson) throws Exception {
        if (parseJson instanceof User) {
            this.id = ((User) parseJson).id;
            this.country = ((User) parseJson).country;
            this.username = ((User) parseJson).username;
            this.fullName = ((User) parseJson).fullName;
            this.totalFollowedBy = ((User) parseJson).totalFollowedBy;
            this.totalFollows = ((User) parseJson).totalFollows;
        } else {
            throw new Exception();
        }
    }

    public String getId() {
        return id;
    }

    public String getCountry() {
        return country;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public Integer getTotalFollowedBy() {
        return totalFollowedBy;
    }

    public Integer getTotalFollows() {
        return totalFollows;
    }
}
