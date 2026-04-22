package com.tumbwe.scheduler;

import com.tumbwe.model.JobListing;
import com.tumbwe.service.JobAggregationService;
import com.tumbwe.service.EmailService;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@ApplicationScoped
public class JobScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(JobScheduler.class);

    @Inject
    JobAggregationService aggregationService;

    @Inject
    EmailService emailService;

    /**
     * Runs at 06:00, 12:00, and 18:00 Africa/Lusaka.
     * Note: Ensure the system timezone is set to Africa/Lusaka or configure the cron to offset accordingly.
     * Quartz cron: second minute hour dayMonth month dayWeek [year]
     */
    @Scheduled(cron = "0 0 6,12,18 * * ?")
    public void scheduledScrape() {
        LOG.info("Starting scheduled job scraping cycle...");
        try {
            List<JobListing> newJobs = aggregationService.collectAllJobs();
            LOG.info("Scraping complete. Found {} new matching jobs.", newJobs.size());
            emailService.sendDigest(newJobs);
        } catch (Exception e) {
            LOG.error("Critical error in scheduled scraping cycle: {}", e.getMessage(), e);
        }
    }
}
