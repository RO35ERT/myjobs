package com.tumbwe.service;

import com.tumbwe.model.JobListing;
import com.tumbwe.scraper.Scraper;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.smallrye.faulttolerance.api.RateLimit;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class JobAggregationService {

    private static final Logger LOG = LoggerFactory.getLogger(JobAggregationService.class);

    @Inject
    Instance<Scraper> scrapers;

    @Inject
    @CacheName("job-deduplication")
    Cache deduplicationCache;

    @ConfigProperty(name = "job.scraper.sites-file", defaultValue = "sites.txt")
    String sitesFilePath;

    @ConfigProperty(name = "job.scraper.keywords")
    List<String> keywords;

    @Retry(maxRetries = 3, delay = 1000)
    public List<JobListing> collectAllJobs() {
        List<String> urls = loadSites();
        List<JobListing> allNewJobs = new ArrayList<>();

        for (String url : urls) {
            Scraper scraper = findScraper(url);
            LOG.info("Scraping {} using {}", url, scraper.getClass().getSimpleName());
            
            List<JobListing> scrapedJobs = scraper.scrape(url);
            
            for (JobListing job : scrapedJobs) {
                if (isNewAndMatches(job)) {
                    allNewJobs.add(job);
                    // Mark as seen
                    deduplicationCache.get(job.link(), k -> true).await().indefinitely();
                }
            }
        }

        return allNewJobs;
    }

    private List<String> loadSites() {
        try {
            return Files.readAllLines(Path.of(sitesFilePath))
                    .stream()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOG.error("Could not read sites file at {}: {}", sitesFilePath, e.getMessage());
            return List.of();
        }
    }

    private Scraper findScraper(String url) {
        // Find a specialized scraper first
        for (Scraper s : scrapers) {
            if (!(s.getClass().getSimpleName().contains("GenericScraper")) && s.supports(url)) {
                return s;
            }
        }
        // Fallback to Generic
        return scrapers.stream()
                .filter(s -> s.getClass().getSimpleName().contains("GenericScraper"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No scrapers found"));
    }

    private boolean isNewAndMatches(JobListing job) {
        // 1. Check Keywords
        boolean matches = keywords.stream()
                .anyMatch(kw -> job.title().toLowerCase().contains(kw.toLowerCase()));
        
        if (!matches) return false;

        // 2. Check Deduplication Cache
        // We use a marker to detect if the loader was called (meaning it's new)
        java.util.concurrent.atomic.AtomicBoolean isNew = new java.util.concurrent.atomic.AtomicBoolean(false);
        deduplicationCache.get(job.link(), k -> {
            isNew.set(true);
            return Boolean.TRUE;
        }).await().indefinitely();
        
        return isNew.get();
    }
}
