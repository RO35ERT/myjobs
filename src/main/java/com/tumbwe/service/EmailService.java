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
        if (apiInstance == null) {
            LOG.error("Brevo API client is not initialized; skipping email send. Check brevo.api.key / BREVO_API_KEY.");
            return;
        }

        if (jobs.isEmpty()) {
            LOG.info("No new jobs to send. Skipping email.");
            return;
        }

        String resolvedRecipient = recipient == null ? "" : recipient.trim();
        String resolvedSenderEmail = senderEmail == null ? "" : senderEmail.trim();
        String resolvedSenderName = senderName == null ? "" : senderName.trim();

        if (resolvedRecipient.isBlank()) {
            LOG.error("Email recipient is missing/blank; skipping digest. Set job.scraper.email.recipient / MYJOBS_EMAIL_RECIPIENT.");
            return;
        }

        if (resolvedSenderEmail.isBlank()) {
            LOG.error("Brevo sender email is missing/blank; skipping digest. Set brevo.sender.email (env: BREVO_SENDER_EMAIL or MYJOBS_EMAIL_FROM).");
            return;
        }

        if (resolvedSenderName.isBlank()) {
            resolvedSenderName = "My Job Scraper";
        }

        String html = JobDigestTemplate
                .data("jobs", jobs)
                .data("now", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .render();

        SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
        sendSmtpEmail.setSender(new SendSmtpEmailSender().name(resolvedSenderName).email(resolvedSenderEmail));
        sendSmtpEmail.setTo(Collections.singletonList(new SendSmtpEmailTo().email(resolvedRecipient)));
        sendSmtpEmail.setSubject("Daily Software Job Digest");
        sendSmtpEmail.setHtmlContent(html);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Sending Brevo digest: jobs={}, from=\"{}\" <{}>, to={}", jobs.size(), resolvedSenderName, resolvedSenderEmail, resolvedRecipient);
        }

        try {
            brevoModel.CreateSmtpEmail result = apiInstance.sendTransacEmail(sendSmtpEmail);
            LOG.info("Email digest sent successfully to {} via Brevo API. Message ID: {}", recipient, result.getMessageId());
        } catch (ApiException e) {
            LOG.error("Failed to send email digest via Brevo API: {} (Status: {}, Body: {})", 
                e.getMessage(), e.getCode(), e.getResponseBody());
        }
    }

    public void sendPlainText(String subject, String body) {
        if (apiInstance == null) {
            LOG.error("Brevo API client is not initialized; skipping plain text email.");
            return;
        }

        String resolvedRecipient = recipient == null ? "" : recipient.trim();
        String resolvedSenderEmail = senderEmail == null ? "" : senderEmail.trim();
        String resolvedSenderName = senderName == null ? "" : senderName.trim();

        if (resolvedRecipient.isBlank() || resolvedSenderEmail.isBlank()) {
            LOG.error("Missing recipient or sender email for plain text email.");
            return;
        }
        if (resolvedSenderName.isBlank()) {
            resolvedSenderName = "My Job Scraper";
        }

        SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
        sendSmtpEmail.setSender(new SendSmtpEmailSender().name(resolvedSenderName).email(resolvedSenderEmail));
        sendSmtpEmail.setTo(Collections.singletonList(new SendSmtpEmailTo().email(resolvedRecipient)));
        sendSmtpEmail.setSubject(subject);
        sendSmtpEmail.setTextContent(body);

        try {
            brevoModel.CreateSmtpEmail result = apiInstance.sendTransacEmail(sendSmtpEmail);
            LOG.info("Plain text email sent successfully to {}. Message ID: {}", recipient, result.getMessageId());
        } catch (ApiException e) {
            LOG.error("Failed to send plain text email: {} (Status: {}, Body: {})", 
                e.getMessage(), e.getCode(), e.getResponseBody());
        }
    }

    public void sendHtml(String subject, String html) {
        if (apiInstance == null) {
            LOG.error("Brevo API client is not initialized; skipping HTML email.");
            return;
        }

        String resolvedRecipient = recipient == null ? "" : recipient.trim();
        String resolvedSenderEmail = senderEmail == null ? "" : senderEmail.trim();
        String resolvedSenderName = senderName == null ? "" : senderName.trim();

        if (resolvedRecipient.isBlank() || resolvedSenderEmail.isBlank()) {
            LOG.error("Missing recipient or sender email for HTML email.");
            return;
        }
        if (resolvedSenderName.isBlank()) {
            resolvedSenderName = "My Job Scraper";
        }

        SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
        sendSmtpEmail.setSender(new SendSmtpEmailSender().name(resolvedSenderName).email(resolvedSenderEmail));
        sendSmtpEmail.setTo(Collections.singletonList(new SendSmtpEmailTo().email(resolvedRecipient)));
        sendSmtpEmail.setSubject(subject);
        sendSmtpEmail.setHtmlContent(html);

        try {
            brevoModel.CreateSmtpEmail result = apiInstance.sendTransacEmail(sendSmtpEmail);
            LOG.info("HTML email sent successfully to {}. Message ID: {}", recipient, result.getMessageId());
        } catch (ApiException e) {
            LOG.error("Failed to send HTML email: {} (Status: {}, Body: {})", 
                e.getMessage(), e.getCode(), e.getResponseBody());
        }
    }
}
