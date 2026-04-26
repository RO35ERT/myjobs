package com.tumbwe.bot;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@ApplicationScoped
public class BotRegistration {
    private static final Logger LOG = LoggerFactory.getLogger(BotRegistration.class);

    @Inject
    MyJobsTelegramBot myJobsTelegramBot;

    void onStart(@Observes StartupEvent ev) {
        String token = myJobsTelegramBot.getBotToken();
        if (token == null || "dummy".equals(token) || "your_bot_token_here".equals(token) || token.isBlank()) {
            LOG.warn("Telegram Bot token is not set (found placeholder). Bot will not be registered. Please update your .env file with TELEGRAM_BOT_TOKEN.");
            return;
        }
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(myJobsTelegramBot);
            LOG.info("Telegram Bot registered successfully");
        } catch (Exception e) {
            LOG.error("Failed to register Telegram Bot: {}", e.getMessage(), e);
        }
    }
}
