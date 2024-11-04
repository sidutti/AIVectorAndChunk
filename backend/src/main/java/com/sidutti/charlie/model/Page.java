package com.sidutti.charlie.model;

import java.util.List;
import java.util.Map;

public record Page(List<Paragraph> paragraphs, Map<String,String> formFields) {
}
