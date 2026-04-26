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
public class ZambiaJobScraper implements Scraper {

    private static final Logger LOG = LoggerFactory.getLogger(ZambiaJobScraper.class);

    @Override
    public List<JobListing> scrape(String url) {
        List<JobListing> jobs = new ArrayList<>();
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36")
                    .timeout(10_000)
                    .get();

            Elements containers = doc.select("div.search-result");
            
            for (Element item : containers) {
                Element titleLink = item.selectFirst("h3 a");
                if (titleLink == null) continue;

                String title = titleLink.text().trim();
                String link = titleLink.absUrl("href");
                
                String company = "Unknown Company";
                Element companyEl = item.selectFirst(".company-name");
                if (companyEl != null) company = companyEl.text().trim();

                String location = "Unknown Location";
                Element locationEl = item.selectFirst(".job-recruiter");
                if (locationEl != null) {
                    location = locationEl.text().trim();
                    // Clean up: "Region of : Lusaka" -> "Lusaka"
                    if (location.contains(":")) {
                        location = location.split(":")[1].trim();
                    }
                }

                String description = "";
                Element descEl = item.selectFirst(".job-description");
                if (descEl != null) description = descEl.text().trim();

                jobs.add(new JobListing(title, company, location, link, url, description));
            }
        } catch (Exception e) {
            LOG.error("Error scraping ZambiaJob from {}: {}", url, e.getMessage());
        }
        return jobs;
    }

    @Override
    public boolean supports(String url) {
        return url.contains("zambiajob.com");
    }
}
