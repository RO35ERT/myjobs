package com.tumbwe.model;

import io.quarkus.qute.TemplateData;

@TemplateData
public record JobListing(
    String title,
    String company,
    String location,
    String link,
    String sourceUrl
) {
}
