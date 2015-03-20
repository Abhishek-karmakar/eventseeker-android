package com.freethinking.beats.sdk.data;

/**
 * Genre returns a Beats genre (eg. "Pop").
 * <p/>
 * Requires Auth.
 */
public class Genre extends BaseJson {
    protected String id;
    protected Boolean verified;
    protected String username;
    protected String name;

    @Override
    public void fillIn(BaseJson parseJson) throws Exception {
        if (parseJson instanceof Genre) {
            this.id = ((Genre) parseJson).id;
            this.verified = ((Genre) parseJson).verified;
            this.username = ((Genre) parseJson).username;
            this.name = ((Genre) parseJson).name;
        } else {
            throw new Exception();
        }
    }

    public String getId() {
        return id;
    }

    public Boolean getVerified() {
        return verified;
    }

    public String getUsername() {
        return username;
    }

    public String getName() {
        return name;
    }
}
