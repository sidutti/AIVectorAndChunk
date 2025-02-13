package com.sidutti.charlie.model;

import java.util.List;

public record Section(List<Element> elements, String sectionOrder, int sectionNumber) {
}
