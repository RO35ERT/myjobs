package com.tumbwe.api;

import com.tumbwe.service.JobAggregationService;
import com.tumbwe.service.EmailService;
import com.tumbwe.model.JobListing;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/scrape")
public class ScraperResource {

    @Inject
    JobAggregationService aggregationService;

    @Inject
    EmailService emailService;

    @GET
    @Path("/test")
    @Produces(MediaType.TEXT_PLAIN)
    public String triggerTest() {
        try {
            List<JobListing> jobs = aggregationService.collectAllJobs();
            emailService.sendDigest(jobs);
            return "Test scrape completed. Found " + jobs.size() + " new jobs. Check logs for email status.";
        } catch (Exception e) {
            return "Error during test: " + e.getMessage();
        }
    }

    @GET
    @Path("/test-email")
    @Produces(MediaType.TEXT_PLAIN)
    public String sendTestEmail() {
        try {
            JobListing testJob = new JobListing(
                "Test Software Engineer", 
                "Quarkus Lab", 
                "Remote", 
                "https://example.com/test-job-" + System.currentTimeMillis(), 
                "Test Source"
            );
            emailService.sendDigest(List.of(testJob));
            return "Test email sent. Check your inbox and logs.";
        } catch (Exception e) {
            return "Error sending test email: " + e.getMessage();
        }
    }
}
