package it.unisalento.iot.autobluapp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;


@JsonIgnoreProperties(ignoreUnknown = true)
public class Beacon {

    @JsonProperty("selfId")
    private String selfId;

    @JsonProperty("address")
    private String address;


    public String getSelfId() {
        return this.selfId;
    }

    public String getAddress() {
        return this.address;
    }

}
