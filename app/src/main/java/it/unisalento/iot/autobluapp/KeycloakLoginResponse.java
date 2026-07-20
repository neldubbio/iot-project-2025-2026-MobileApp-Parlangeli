package it.unisalento.iot.autobluapp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;


@JsonIgnoreProperties(ignoreUnknown = true)
public class KeycloakLoginResponse {

    @JsonProperty("access_token")
    private String jwt;

    @JsonProperty("expires_in")
    private Integer expiresIn;

    public String getJwt() {
        return this.jwt;
    }

    public Integer getExpiresIn() {
        return this.expiresIn;
    }

}
