package com.tumbwe.scheduler;

import com.tumbwe.bot.MyJobsTelegramBot;
import com.tumbwe.model.JobListing;
import com.tumbwe.model.NotificationFrequency;
import com.tumbwe.model.TelegramUser;
import com.tumbwe.model.UserPreference;
import com.tumbwe.model.UserState;
import com.tumbwe.service.JobSearchService;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class NotificationScheduler {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationScheduler.class);
    private static final int MAX_JOBS_PER_MESSAGE = 10;

    @Inject
    MyJobsTelegramBot bot;

    @Inject
    JobSearchService searchService;

    @Inject
    NotificationProcessor processor; // Inject self-proxy for REQUIRES_NEW transactions

    // Run every 30 mins at HH:05 and HH:35 to give scraper time to finish
    @Scheduled(cron = "0 5,35 * * * ?")
    public void sendNotifications() {
        LOG.info("Checking for user notifications...");
        // Fetch users in a short transaction
        List<TelegramUser> users = processor.getRegisteredUsers();
        LocalDateTime now = LocalDateTime.now();

        for (TelegramUser user : users) {
            try {
                if (user.frequency != NotificationFrequency.WHEN_FOUND && isDue(user, now)) {
                    processor.processUser(user.chatId, now);
                }
            } catch (Exception e) {
                LOG.error("Failed to process notifications for user {}", user.chatId, e);
            }
        }
    }

    public void sendOnDemandNotifications() {
        LOG.info("Checking for ON DEMAND user notifications...");
        List<TelegramUser> users = processor.getRegisteredUsers();
        LocalDateTime now = LocalDateTime.now();

        for (TelegramUser user : users) {
            try {
                if (user.frequency == NotificationFrequency.WHEN_FOUND) {
                    processor.processUser(user.chatId, now);
                }
            } catch (Exception e) {
                LOG.error("Failed to process ON DEMAND notifications for user {}", user.chatId, e);
            }
        }
    }

    private boolean isDue(TelegramUser user, LocalDateTime now) {
        if (user.lastNotifiedAt == null) return true;
        long hoursSinceLast = java.time.Duration.between(user.lastNotifiedAt, now).toHours();
        long minsSinceLast = java.time.Duration.between(user.lastNotifiedAt, now).toMinutes();

        return switch (user.frequency) {
            case WHEN_FOUND -> minsSinceLast >= 25; // using 25 to account for slight schedule offsets
            case ONE_HOUR -> hoursSinceLast >= 1;
            case SIX_HOURS -> hoursSinceLast >= 6;
            case TWELVE_HOURS -> hoursSinceLast >= 12;
            default -> false;
        };
    }

    @ApplicationScoped
    public static class NotificationProcessor {
        
        @Inject
        MyJobsTelegramBot bot;

        @Inject
        JobSearchService searchService;

        @Transactional
        public List<TelegramUser> getRegisteredUsers() {
            return TelegramUser.list("state", UserState.REGISTERED);
        }

        @Transactional(Transactional.TxType.REQUIRES_NEW)
        public void processUser(Long chatId, LocalDateTime now) {
            TelegramUser user = TelegramUser.findByChatId(chatId);
            if (user == null || user.state != UserState.REGISTERED) return;

            List<UserPreference> prefs = UserPreference.findByChatId(user.chatId);
            if (prefs.isEmpty()) return;

            List<String> keywords = prefs.stream().map(p -> p.keyword).collect(Collectors.toList());
            
            LocalDateTime maxSince = switch (user.frequency) {
                case WHEN_FOUND -> now.minusMinutes(35);
                case ONE_HOUR -> now.minusHours(1);
                case SIX_HOURS -> now.minusHours(6);
                case TWELVE_HOURS -> now.minusHours(12);
                default -> now.minusDays(1);
            };
            
            LocalDateTime since = user.lastNotifiedAt != null && user.lastNotifiedAt.isAfter(maxSince) 
                    ? user.lastNotifiedAt 
                    : maxSince;
            
            // Fetch up to 10 jobs plus 1 to know if there's more
            List<JobListing> results = searchService.searchJobs(keywords, since, MAX_JOBS_PER_MESSAGE + 1);
            
            boolean jobsFound = !results.isEmpty();

            if (jobsFound) {
                int displayCount = Math.min(results.size(), MAX_JOBS_PER_MESSAGE);
                boolean hasMore = results.size() > MAX_JOBS_PER_MESSAGE;
                
                StringBuilder message = new StringBuilder("Found " + (hasMore ? "more than " : "") + displayCount + " new jobs for you!\n\n");
                
                for (int i = 0; i < displayCount; i++) {
                    JobListing job = results.get(i);
                    message.append("🔹 ").append(job.title()).append(" at ").append(job.company()).append("\n");
                    message.append("📍 ").append(job.location()).append("\n");
                    message.append("🔗 ").append(job.link()).append("\n\n");
                }
                
                if (hasMore) {
                    message.append("...and more! Check the websites for the rest.\n");
                }
                
                try {
                    bot.sendMessage(user.chatId, message.toString());
                } catch (Exception e) {
                    LoggerFactory.getLogger(NotificationScheduler.class).error("Failed to send Telegram message", e);
                    return; // Abort transaction if send fails, so lastNotifiedAt doesn't update and we retry later
                }
            }
            
            // Only update lastNotifiedAt for WHEN_FOUND if jobs were actually found
            // For periodic schedules, always update it so they don't get spammed every 30 mins
            if (jobsFound || user.frequency != NotificationFrequency.WHEN_FOUND) {
                user.lastNotifiedAt = now;
                user.persist();
            }
        }
    }
}
