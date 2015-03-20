package com.freethinking.beats.sdk.data;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class BaseJson implements Serializable {
    public abstract void fillIn(BaseJson parseJson) throws Exception;
}
