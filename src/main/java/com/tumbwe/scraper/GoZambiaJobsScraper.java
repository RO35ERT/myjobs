package com.tumbwe.scraper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tumbwe.model.JobListing;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class GoZambiaJobsScraper implements Scraper {

    private static final Logger LOG = LoggerFactory.getLogger(GoZambiaJobsScraper.class);
    private static final String BASE_URL = "https://gozambiajobs.com";
    private static final Pattern JSON_PATTERN = Pattern.compile("window\\.jobsList\\.concat\\((\\[.*?\\])\\);", Pattern.DOTALL);

    @Inject
    ObjectMapper objectMapper;

    @Override
    public List<JobListing> scrape(String url) {
        List<JobListing> jobs = new ArrayList<>();
        try {
            Document doc = Jsoup.connect(url).timeout(10_000).get();
            Elements scripts = doc.select("script");
            
            for (Element script : scripts) {
                String html = script.html();
                if (html.contains("window.jobsList")) {
                    Matcher matcher = JSON_PATTERN.matcher(html);
                    if (matcher.find()) {
                        String jsonArrayString = matcher.group(1);
                        JsonNode jsonArray = objectMapper.readTree(jsonArrayString);
                        
                        for (JsonNode node : jsonArray) {
                            String title = node.get("title").asText();
                            String company = node.path("employer").path("name").asText("N/A");
                            String location = node.get("location").asText("N/A");
                            String detailsPath = node.get("job_details_path").asText();
                            String link = BASE_URL + detailsPath;
                            String description = node.path("excerpt").asText("");
                            
                            jobs.add(new JobListing(title, company, location, link, url, description));
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Error scraping GoZambiaJobs from {}: {}", url, e.getMessage());
        }
        return jobs;
    }

    @Override
    public boolean supports(String url) {
        return url.contains("gozambiajobs.com");
    }
}
