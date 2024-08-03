package com.sidutti.charlie.sidutti.model.wiki;

import com.fasterxml.jackson.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
                "page",
                "revisions",
                "edit",
                "talk"
})

public class Desktop {

        @JsonProperty("page")
        private String page;
        @JsonProperty("revisions")
        private String revisions;
        @JsonProperty("edit")
        private String edit;
        @JsonProperty("talk")
        private String talk;
        @JsonIgnore

        private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

        @JsonProperty("page")
        public String getPage() {
                return page;
        }

        @JsonProperty("page")
        public void setPage(String page) {
                this.page = page;
        }

        @JsonProperty("revisions")
        public String getRevisions() {
                return revisions;
        }

        @JsonProperty("revisions")
        public void setRevisions(String revisions) {
                this.revisions = revisions;
        }

        @JsonProperty("edit")
        public String getEdit() {
                return edit;
        }

        @JsonProperty("edit")
        public void setEdit(String edit) {
                this.edit = edit;
        }

        @JsonProperty("talk")
        public String getTalk() {
                return talk;
        }

        @JsonProperty("talk")
        public void setTalk(String talk) {
                this.talk = talk;
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
