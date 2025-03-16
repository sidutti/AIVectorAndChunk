package com.sidutti.charlie.model.wiki;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.LinkedHashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "desktop",
        "mobile"
})

public class ContentUrls {

    @JsonIgnore

    private final Map<String, Object> additionalProperties = new LinkedHashMap<>();
    @JsonProperty("desktop")

    private Desktop desktop;
    @JsonProperty("mobile")

    private Mobile mobile;

    @JsonProperty("desktop")
    public Desktop getDesktop() {
        return desktop;
    }

    @JsonProperty("desktop")
    public void setDesktop(Desktop desktop) {
        this.desktop = desktop;
    }

    @JsonProperty("mobile")
    public Mobile getMobile() {
        return mobile;
    }

    @JsonProperty("mobile")
    public void setMobile(Mobile mobile) {
        this.mobile = mobile;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
