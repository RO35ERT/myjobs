package com.tumbwe.service;

import com.tumbwe.model.JobListing;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.reactive.ReactiveMailer;
import io.quarkus.qute.Template;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@ApplicationScoped
public class EmailService {

    private static final Logger LOG = LoggerFactory.getLogger(EmailService.class);

    @Inject
    ReactiveMailer mailer;

    @Inject
    @ConfigProperty(name = "job.scraper.email.recipient")
    String recipient;

    @Inject
    @ConfigProperty(name = "quarkus.mailer.from")
    String fromAddress;

    @Inject
    Template JobDigestTemplate;

    public void sendDigest(List<JobListing> jobs) {
        if (jobs.isEmpty()) {
            LOG.info("No new jobs to send. Skipping email.");
            return;
        }

        String html = JobDigestTemplate
                .data("jobs", jobs)
                .data("now", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .render();

        Mail mail = Mail.withHtml(recipient, "Daily Software Job Digest", html)
                .setFrom("My Job Board <" + fromAddress + ">");
        
        mailer.send(mail)
                .subscribe().with(
                        success -> LOG.info("Email digest sent successfully to {}", recipient),
                        failure -> LOG.error("Failed to send email digest: {}", failure.getMessage())
                );
    }
}
