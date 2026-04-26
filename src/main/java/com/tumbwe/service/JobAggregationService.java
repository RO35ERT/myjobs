package com.tumbwe.service;

import com.tumbwe.model.JobListing;
import com.tumbwe.scraper.Scraper;
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

import com.tumbwe.model.JobEntity;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class JobAggregationService {

    private static final Logger LOG = LoggerFactory.getLogger(JobAggregationService.class);

    @Inject
    Instance<Scraper> scrapers;

    @ConfigProperty(name = "job.scraper.sites-file", defaultValue = "sites.txt")
    String sitesFilePath;

    @ConfigProperty(name = "job.scraper.keywords")
    List<String> keywords;

    @Retry(maxRetries = 3, delay = 1000)
    public List<JobListing> collectAllJobs() {
        List<String> urls = loadSites();
        List<JobListing> newlyInsertedJobs = new ArrayList<>();

        for (String url : urls) {
            Scraper scraper = findScraper(url);
            LOG.info("Scraping {} using {}", url, scraper.getClass().getSimpleName());
            
            List<JobListing> scrapedJobs = scraper.scrape(url);
            
            for (JobListing job : scrapedJobs) {
                if (persistJobIfNotExists(job)) {
                    newlyInsertedJobs.add(job);
                }
            }
        }

        return newlyInsertedJobs;
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public boolean persistJobIfNotExists(JobListing job) {
        if (JobEntity.findByLink(job.link()) == null) {
            JobEntity entity = new JobEntity();
            entity.title = job.title();
            entity.company = job.company();
            entity.location = job.location();
            entity.link = job.link();
            entity.sourceUrl = job.sourceUrl();
            entity.description = job.description();
            entity.scrapedAt = java.time.LocalDateTime.now();
            entity.persist();
            return true;
        }
        return false;
    }

    private List<String> loadSites() {
        try {
            List<String> lines;
            Path path = Path.of(sitesFilePath);
            if (Files.exists(path)) {
                lines = Files.readAllLines(path);
            } else {
                // Fallback to classpath resource
                try (var is = getClass().getClassLoader().getResourceAsStream(sitesFilePath)) {
                    if (is == null) {
                        LOG.error("Could not find sites file at {} on filesystem or classpath.", sitesFilePath);
                        return List.of();
                    }
                    lines = new java.io.BufferedReader(new java.io.InputStreamReader(is))
                            .lines().collect(Collectors.toList());
                    LOG.info("Loaded sites from classpath: {}", sitesFilePath);
                }
            }
            return lines.stream()
                    .map(line -> line.replaceAll("[^\\x20-\\x7E]", "").trim())
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOG.error("Error reading sites file {}: {}", sitesFilePath, e.getMessage());
            return List.of();
        }
    }

    private Scraper findScraper(String url) {
        Scraper generic = null;
        for (Scraper s : scrapers) {
            if (s.isGeneric()) {
                generic = s;
            } else if (s.supports(url)) {
                return s; // Found a specialized scraper
            }
        }
        if (generic == null) {
            throw new IllegalStateException("No scrapers found and GenericScraper is missing");
        }
        return generic;
    }

    public List<JobListing> filterForAdmin(List<JobListing> jobs) {
        if (keywords == null || keywords.isEmpty()) return jobs;
        return jobs.stream()
                .filter(job -> keywords.stream()
                        .anyMatch(kw -> job.title().toLowerCase().contains(kw.toLowerCase())))
                .collect(Collectors.toList());
    }
}
