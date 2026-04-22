package com.tumbwe.service;

import brevoApi.TransactionalEmailsApi;
import brevoModel.SendSmtpEmail;
import brevoModel.SendSmtpEmailSender;
import brevoModel.SendSmtpEmailTo;
import brevo.ApiClient;
import brevo.ApiException;
import brevo.Configuration;
import brevo.auth.ApiKeyAuth;
import com.tumbwe.model.JobListing;
import io.quarkus.qute.Template;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class EmailService {

    private static final Logger LOG = LoggerFactory.getLogger(EmailService.class);

    @Inject
    @ConfigProperty(name = "job.scraper.email.recipient")
    String recipient;

    @Inject
    @ConfigProperty(name = "brevo.sender.email")
    String senderEmail;

    @Inject
    @ConfigProperty(name = "brevo.sender.name", defaultValue = "My Job Scraper")
    String senderName;

    @Inject
    @ConfigProperty(name = "brevo.api.key")
    String apiKey;

    @Inject
    Template JobDigestTemplate;

    private TransactionalEmailsApi apiInstance;

    @PostConstruct
    void init() {
        if (apiKey == null || apiKey.isBlank()) {
            LOG.error("BREVO_API_KEY is not set! Please check your .env file and application.properties.");
            return;
        }

        if (apiKey.startsWith("xsmtpsib-")) {
            LOG.warn("It looks like you're using an SMTP key (starts with 'xsmtpsib-') for the Brevo API. " +
                     "Please use a v3 API Key (starts with 'xkeysib-') from https://app.brevo.com/settings/keys/api");
        }

        ApiClient client = new ApiClient();
        ApiKeyAuth apiKeyAuth = (ApiKeyAuth) client.getAuthentication("api-key");
        apiKeyAuth.setApiKey(apiKey);
        apiInstance = new TransactionalEmailsApi(client);
        LOG.debug("Brevo API client initialized with key starting with: {}", apiKey.substring(0, Math.min(apiKey.length(), 10)));
    }

    public void sendDigest(List<JobListing> jobs) {
        if (jobs.isEmpty()) {
            LOG.info("No new jobs to send. Skipping email.");
            return;
        }

        String html = JobDigestTemplate
                .data("jobs", jobs)
                .data("now", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .render();

        SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
        sendSmtpEmail.setSender(new SendSmtpEmailSender().name(senderName).email(senderEmail));
        sendSmtpEmail.setTo(Collections.singletonList(new SendSmtpEmailTo().email(recipient)));
        sendSmtpEmail.setSubject("Daily Software Job Digest");
        sendSmtpEmail.setHtmlContent(html);

        try {
            brevoModel.CreateSmtpEmail result = apiInstance.sendTransacEmail(sendSmtpEmail);
            LOG.info("Email digest sent successfully to {} via Brevo API. Message ID: {}", recipient, result.getMessageId());
        } catch (ApiException e) {
            LOG.error("Failed to send email digest via Brevo API: {} (Status: {}, Body: {})", 
                e.getMessage(), e.getCode(), e.getResponseBody());
        }
    }
}
