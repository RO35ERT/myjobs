package com.tumbwe.service;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class DatabaseInitializer {
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseInitializer.class);

    @Inject
    EntityManager em;

    @Transactional
    void onStart(@Observes StartupEvent ev) {
        LOG.info("Initializing SQLite: WAL mode, tables, FTS5, and triggers...");
        try {
            // Enable WAL mode for better concurrent access
            em.createNativeQuery("PRAGMA journal_mode=WAL").getResultList();
            em.createNativeQuery("PRAGMA busy_timeout=5000").getResultList();

            // Create base tables if they don't exist
            em.createNativeQuery("CREATE TABLE IF NOT EXISTS jobs (id VARCHAR(36) PRIMARY KEY, title VARCHAR(255), company VARCHAR(255), location VARCHAR(255), link VARCHAR(255) UNIQUE, sourceUrl VARCHAR(255), description VARCHAR(2000), scrapedAt TIMESTAMP)").executeUpdate();
            em.createNativeQuery("CREATE TABLE IF NOT EXISTS telegram_users (chatId BIGINT PRIMARY KEY, name VARCHAR(255), state VARCHAR(50), frequency VARCHAR(50), lastNotifiedAt TIMESTAMP, registeredAt TIMESTAMP)").executeUpdate();
            em.createNativeQuery("CREATE TABLE IF NOT EXISTS user_preferences (id VARCHAR(36) PRIMARY KEY, chat_id BIGINT, keyword VARCHAR(255), FOREIGN KEY(chat_id) REFERENCES telegram_users(chatId))").executeUpdate();

            // Create FTS5 virtual table
            em.createNativeQuery("CREATE VIRTUAL TABLE IF NOT EXISTS jobs_fts USING fts5(id UNINDEXED, title, company, location, link UNINDEXED, description)").executeUpdate();
            
            // Triggers for syncing jobs -> jobs_fts
            em.createNativeQuery("CREATE TRIGGER IF NOT EXISTS jobs_ai AFTER INSERT ON jobs BEGIN " +
                    "INSERT INTO jobs_fts(id, title, company, location, link, description) VALUES (new.id, new.title, new.company, new.location, new.link, new.description); " +
                    "END;").executeUpdate();
                    
            em.createNativeQuery("CREATE TRIGGER IF NOT EXISTS jobs_ad AFTER DELETE ON jobs BEGIN " +
                    "DELETE FROM jobs_fts WHERE id = old.id; " +
                    "END;").executeUpdate();
                    
            em.createNativeQuery("CREATE TRIGGER IF NOT EXISTS jobs_au AFTER UPDATE ON jobs BEGIN " +
                    "DELETE FROM jobs_fts WHERE id = old.id; " +
                    "INSERT INTO jobs_fts(id, title, company, location, link, description) VALUES (new.id, new.title, new.company, new.location, new.link, new.description); " +
                    "END;").executeUpdate();
                    
            LOG.info("FTS5 setup complete.");
        } catch (Exception e) {
            LOG.error("Failed to initialize FTS5: " + e.getMessage(), e);
        }
    }
}
