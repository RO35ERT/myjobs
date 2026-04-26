package com.tumbwe.scraper;

import com.tumbwe.model.JobListing;
import java.util.List;

public interface Scraper {
    /**
     * Scrapes job listings from the given URL.
     * @param url The URL to scrape.
     * @return A list of JobListing objects.
     */
    List<JobListing> scrape(String url);

    /**
     * Returns true if this scraper supports the given URL.
     * @param url The URL to check.
     * @return True if supported, false otherwise.
     */
    boolean supports(String url);

    /**
     * Returns true if this is the generic catch-all scraper.
     * Used to correctly prioritise specialist scrapers over the generic one
     * without relying on brittle class-name string matching.
     */
    default boolean isGeneric() {
        return false;
    }
}
