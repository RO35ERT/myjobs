package com.tumbwe.scheduler;

import com.tumbwe.model.TelegramUser;
import com.tumbwe.service.EmailService;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

@ApplicationScoped
public class AdminEmailScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(AdminEmailScheduler.class);

    @Inject
    EmailService emailService;

    // Send emails at 04:00, 10:00, and 16:00 UTC (corresponding to 06:00, 12:00, and 18:00 Zambia time)
    @Scheduled(cron = "0 0 4,10,16 * * ?")
    @Transactional
    public void sendAdminReports() {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour(); // This will be UTC hour
        
        LocalDateTime startTime;
        String periodName;

        if (hour == 4) { // 06:00 Zambia
            startTime = now.minusHours(12); // From 18:00 yesterday to 06:00 today
            periodName = "18:00 to 06:00 (Zambia)";
        } else if (hour == 10) { // 12:00 Zambia
            startTime = now.minusHours(6);  // From 06:00 to 12:00
            periodName = "06:00 to 12:00 (Zambia)";
        } else { // 16:00 UTC = 18:00 Zambia
            startTime = now.minusHours(6);  // From 12:00 to 18:00
            periodName = "12:00 to 18:00 (Zambia)";
        }

        long newUsers = TelegramUser.count("registeredAt >= ?1 AND registeredAt < ?2", startTime, now);
        
        String subject1 = "New Users Report: " + periodName;
        String body1 = "<html><body>" +
                       "<h3>New Users Report</h3>" +
                       "<p>Period: <b>" + periodName + "</b></p>" +
                       "<p>Number of new users: <b>" + newUsers + "</b></p>" +
                       "</body></html>";
        
        LOG.info("Prepared Admin HTML Email -> Subject: {}, Body: {}", subject1, body1);
        emailService.sendHtml(subject1, body1);

        if (hour == 16) { // 18:00 Zambia
            long totalUsers = TelegramUser.count();
            String subject2 = "System Report: Total Users";
            String body2 = "<html><body>" +
                           "<h3>System Report</h3>" +
                           "<p>Total number of users on the system: <b>" + totalUsers + "</b></p>" +
                           "</body></html>";
            
            LOG.info("Prepared Admin HTML Email -> Subject: {}, Body: {}", subject2, body2);
            emailService.sendHtml(subject2, body2);
        }
    }
}
