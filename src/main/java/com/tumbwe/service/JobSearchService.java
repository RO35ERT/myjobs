package com.tumbwe.service;

import com.tumbwe.model.JobListing;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class JobSearchService {

    @Inject
    EntityManager em;

    public List<JobListing> searchJobs(List<String> keywords, LocalDateTime since, int limit) {
        if (keywords == null || keywords.isEmpty()) return List.of();

        // Split by commas (from UserPreference) into OR clauses, 
        // and split by spaces into AND clauses for FTS5
        String matchQuery = keywords.stream()
                .map(kw -> kw.replace("\"", "").trim())
                .filter(kw -> !kw.isEmpty())
                .map(kw -> {
                    String[] words = kw.split("\\s+");
                    return "(" + java.util.Arrays.stream(words)
                            .map(w -> "\"" + w + "\"")
                            .collect(Collectors.joining(" AND ")) + ")";
                })
                .collect(Collectors.joining(" OR "));

        if (matchQuery.isEmpty()) return List.of();

        String sql = "SELECT j.title, j.company, j.location, j.link, j.sourceUrl, j.description " +
                     "FROM jobs j " +
                     "JOIN jobs_fts f ON j.id = f.id " +
                     "WHERE jobs_fts MATCH :matchQuery AND j.scrapedAt > :since " +
                     "ORDER BY j.scrapedAt DESC LIMIT :limit";

        Query query = em.createNativeQuery(sql);
        query.setParameter("matchQuery", matchQuery);
        query.setParameter("since", since);
        query.setParameter("limit", limit);

        List<Object[]> results = query.getResultList();
        List<JobListing> jobs = new ArrayList<>();
        
        for (Object[] row : results) {
            jobs.add(new JobListing(
                    (String) row[0], // title
                    (String) row[1], // company
                    (String) row[2], // location
                    (String) row[3], // link
                    (String) row[4], // sourceUrl
                    (String) row[5]  // description
            ));
        }
        return jobs;
    }
}
