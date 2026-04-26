package com.tumbwe.scheduler;

import com.tumbwe.model.JobListing;
import com.tumbwe.service.JobAggregationService;
import com.tumbwe.service.EmailService;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class JobScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(JobScheduler.class);

    @Inject
    JobAggregationService aggregationService;

    @Inject
    EmailService emailService;

    @Inject
    NotificationScheduler notificationScheduler;

    /**
     * Runs every 30 minutes at 00 and 30 past the hour.
     * Note: Ensure the system timezone is set to Africa/Lusaka or configure the cron to offset accordingly.
     * Quartz cron: second minute hour dayMonth month dayWeek [year]
     */
    @Scheduled(cron = "0 0/10 * * * ?")
    public void scheduledScrape() {
        LOG.info("Starting scheduled job scraping cycle...");
        try {
            List<JobListing> newJobs = aggregationService.collectAllJobs();
            List<JobListing> adminDigestJobs = aggregationService.filterForAdmin(newJobs);
            LOG.info("Scraping complete. Found {} new jobs in total, {} match admin keywords.", newJobs.size(), adminDigestJobs.size());
            if (!adminDigestJobs.isEmpty()) {
                // emailService.sendDigest(adminDigestJobs); // Commented out for now
            }
            
            // Trigger on-demand notifications for users
            notificationScheduler.sendOnDemandNotifications();
            
        } catch (Exception e) {
            LOG.error("Critical error in scheduled scraping cycle: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 0 0 * * ?") // Run at midnight every day
    @jakarta.transaction.Transactional
    public void cleanupOldJobs() {
        LOG.info("Cleaning up jobs older than 36 hours...");
        LocalDateTime cutoff = LocalDateTime.now().minusHours(36);
        long deleted = com.tumbwe.model.JobEntity.delete("scrapedAt < ?1", cutoff);
        LOG.info("Deleted {} old jobs.", deleted);
    }
}
