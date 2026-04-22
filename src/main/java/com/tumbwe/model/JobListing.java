package com.tumbwe.model;

public record JobListing(
    String title,
    String company,
    String location,
    String link,
    String sourceUrl
) {
}
