package com.tumbwe.scraper;

import com.tumbwe.model.JobListing;
import jakarta.enterprise.context.ApplicationScoped;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * A generic heuristic-based scraper for job boards.
 * It looks for common patterns in HTML to identify job listings.
 */
@ApplicationScoped
public class GenericScraper implements Scraper {

    private static final Logger LOG = LoggerFactory.getLogger(GenericScraper.class);

    @Override
    public List<JobListing> scrape(String url) {
        List<JobListing> jobs = new ArrayList<>();
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36")
                    .timeout(10_000)
                    .get();

            // Heuristic 1: Look for elements that likely contain a job listing
            // Common tags: article, div.job, li.job, div.listing
            Elements containers = doc.select("article, .job-item, .job_listing, .listing, .job, .post");
            
            if (containers.isEmpty()) {
                // Fallback: Look for links that might be job details
                Elements links = doc.select("a[href*='/job/'], a[href*='/jobs/'], a[href*='-vacancy/']");
                for (Element link : links) {
                    String title = link.text().trim();
                    if (title.length() > 5) { // Basic sanity check
                        String absoluteUrl = link.absUrl("href");
                        jobs.add(new JobListing(title, "Unknown Company", "Unknown Location", absoluteUrl, url, ""));
                    }
                }
            } else {
                for (Element item : containers) {
                    Element titleEl = item.selectFirst("h1, h2, h3, h4, .title, .job-title");
                    Element linkEl = item.selectFirst("a");
                    
                    if (titleEl != null && linkEl != null) {
                        String title = titleEl.text().trim();
                        String absoluteUrl = linkEl.absUrl("href");
                        
                        // Try to find company and location in the same container
                        String company = "Unknown Company";
                        Element companyEl = item.selectFirst(".company, .employer, .organization");
                        if (companyEl != null) company = companyEl.text().trim();
                        
                        String location = "Unknown Location";
                        Element locationEl = item.selectFirst(".location, .address, .city");
                        if (locationEl != null) location = locationEl.text().trim();

                        String description = "";
                        Element descEl = item.selectFirst("p, .description, .excerpt, .summary");
                        if (descEl != null) {
                            description = descEl.text().trim();
                            if (description.length() > 1900) {
                                description = description.substring(0, 1900) + "...";
                            }
                        }

                        if (!title.isEmpty() && !absoluteUrl.isEmpty()) {
                            jobs.add(new JobListing(title, company, location, absoluteUrl, url, description));
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Error in GenericScraper for {}: {}", url, e.getMessage());
        }
        return jobs;
    }

    @Override
    public boolean supports(String url) {
        // This is a catch-all for URLs not handled by specific scrapers
        return true;
    }

    @Override
    public boolean isGeneric() {
        return true;
    }
}
