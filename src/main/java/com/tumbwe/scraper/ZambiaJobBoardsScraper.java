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
public class ZambiaJobBoardsScraper implements Scraper {

    private static final Logger LOG = LoggerFactory.getLogger(ZambiaJobBoardsScraper.class);

    @Override
    public List<JobListing> scrape(String url) {
        List<JobListing> jobs = new ArrayList<>();
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36")
                    .timeout(10_000)
                    .get();

            // Each job is in a white card div
            Elements containers = doc.select("div:has(> div > div > p > a[href^='/jobs/'])");
            
            for (Element item : containers) {
                Element titleLink = item.selectFirst("p > a[href^='/jobs/']");
                if (titleLink == null) continue;

                String title = titleLink.text().trim();
                String link = titleLink.absUrl("href");
                
                String company = "Unknown Company";
                Element companyEl = item.selectFirst("a[href^='/companies/']");
                if (companyEl != null) company = companyEl.text().trim();

                String location = "Unknown Location";
                Element locationEl = item.selectFirst("span:contains(Location:) + span");
                if (locationEl != null) location = locationEl.text().trim();

                String description = "";
                // Sometimes the excerpt is in a p tag that is not the title
                Element descEl = item.selectFirst("p:not(:has(a[href^='/jobs/']))");
                if (descEl != null) description = descEl.text().trim();

                jobs.add(new JobListing(title, company, location, link, url, description));
            }
        } catch (Exception e) {
            LOG.error("Error scraping ZambiaJobBoards from {}: {}", url, e.getMessage());
        }
        return jobs;
    }

    @Override
    public boolean supports(String url) {
        return url.contains("zambiajobboards.com");
    }
}
