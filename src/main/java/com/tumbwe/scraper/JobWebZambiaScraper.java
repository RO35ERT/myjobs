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
public class JobWebZambiaScraper implements Scraper {

    private static final Logger LOG = LoggerFactory.getLogger(JobWebZambiaScraper.class);

    @Override
    public List<JobListing> scrape(String url) {
        List<JobListing> jobs = new ArrayList<>();
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36")
                    .timeout(10_000)
                    .get();

            Elements containers = doc.select("div.item-job");
            
            for (Element item : containers) {
                Element titleLink = item.selectFirst("h2 a");
                if (titleLink == null) continue;

                String title = titleLink.text().trim();
                String link = titleLink.absUrl("href");
                
                String company = "Unknown Company";
                // In JobWebZambia, company is often part of the title or in a specific span if present
                // Heuristic: check if there's a .company or similar
                Element companyEl = item.selectFirst(".company, .employer");
                if (companyEl != null) company = companyEl.text().trim();

                String location = "Zambia";
                Element locationEl = item.selectFirst(".location");
                if (locationEl != null) location = locationEl.text().trim().replace("Location:", "").trim();

                String description = "";
                Element descEl = item.selectFirst("p");
                if (descEl != null) description = descEl.text().trim();

                jobs.add(new JobListing(title, company, location, link, url, description));
            }
        } catch (Exception e) {
            LOG.error("Error scraping JobWebZambia from {}: {}", url, e.getMessage());
        }
        return jobs;
    }

    @Override
    public boolean supports(String url) {
        return url.contains("jobwebzambia.com");
    }
}
