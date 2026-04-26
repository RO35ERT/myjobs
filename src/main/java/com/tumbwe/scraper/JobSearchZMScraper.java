package com.tumbwe.scraper;

import com.tumbwe.model.JobListing;
import jakarta.enterprise.context.ApplicationScoped;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class JobSearchZMScraper implements Scraper {

    private static final Logger LOG = LoggerFactory.getLogger(JobSearchZMScraper.class);

    @Override
    public List<JobListing> scrape(String url) {
        List<JobListing> jobs = new ArrayList<>();
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36")
                    .timeout(10_000)
                    .get();

            // Typical WP Job Manager structure
            Elements containers = doc.select("li.job_listing");
            
            for (Element item : containers) {
                Element titleEl = item.selectFirst("h3");
                if (titleEl == null) continue;

                String title = titleEl.text().trim();
                
                // The link is usually a direct child or parent a tag
                Element linkEl = item.selectFirst("a");
                if (linkEl == null) continue;
                String link = linkEl.absUrl("href");
                
                String company = "Unknown Company";
                Element companyEl = item.selectFirst("strong");
                if (companyEl != null) company = companyEl.text().trim();

                String location = "Unknown Location";
                Element locationEl = item.selectFirst(".location");
                if (locationEl != null) location = locationEl.text().trim();

                String description = "";
                // WP Job Manager sometimes has a small summary or we can use the company/location line
                Element summaryEl = item.selectFirst(".meta");
                if (summaryEl != null) description = summaryEl.text().trim();

                jobs.add(new JobListing(title, company, location, link, url, description));
            }
        } catch (Exception e) {
            LOG.error("Error scraping JobSearchZM from {}: {}", url, e.getMessage());
        }
        return jobs;
    }

    @Override
    public boolean supports(String url) {
        return url.contains("jobsearchzm.com");
    }
}
