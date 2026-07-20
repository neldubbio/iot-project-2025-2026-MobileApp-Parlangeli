package it.unisalento.iot.autobluapp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;


@JsonIgnoreProperties(ignoreUnknown = true)
public class User {

    @JsonProperty("beacons")
    private List<String> beacons;

    public List<String> getBeacons() {
        return this.beacons;
    }

}
