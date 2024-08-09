package com.sidutti.charlie.model.wiki;

import com.fasterxml.jackson.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "canonical",
        "normalized",
        "display"
})

public class Titles {

    @JsonProperty("canonical")
    private String canonical;
    @JsonProperty("normalized")
    private String normalized;
    @JsonProperty("display")
    private String display;
    @JsonIgnore

    private final Map<String, Object> additionalProperties = new LinkedHashMap<>();

    @JsonProperty("canonical")
    public String getCanonical() {
        return canonical;
    }

    @JsonProperty("canonical")
    public void setCanonical(String canonical) {
        this.canonical = canonical;
    }

    @JsonProperty("normalized")
    public String getNormalized() {
        return normalized;
    }

    @JsonProperty("normalized")
    public void setNormalized(String normalized) {
        this.normalized = normalized;
    }

    @JsonProperty("display")
    public String getDisplay() {
        return display;
    }

    @JsonProperty("display")
    public void setDisplay(String display) {
        this.display = display;
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
