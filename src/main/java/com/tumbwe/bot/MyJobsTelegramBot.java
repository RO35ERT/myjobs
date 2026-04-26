package com.tumbwe.bot;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@ApplicationScoped
public class MyJobsTelegramBot extends TelegramLongPollingBot {
    private static final Logger LOG = LoggerFactory.getLogger(MyJobsTelegramBot.class);

    private final String botUsername;
    private final String botToken;
    private final BotService botService;

    // Required for Quarkus CDI Proxy
    protected MyJobsTelegramBot() {
        super("dummy");
        this.botUsername = null;
        this.botToken = null;
        this.botService = null;
    }

    @Inject
    public MyJobsTelegramBot(
            @ConfigProperty(name = "telegram.bot.token", defaultValue = "dummy") String botToken,
            @ConfigProperty(name = "telegram.bot.username", defaultValue = "dummy_bot") String botUsername,
            BotService botService) {
        super(botToken);
        this.botToken = botToken;
        this.botUsername = botUsername;
        this.botService = botService;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();

            try {
                String response = botService.processMessage(chatId, text);
                sendMessage(chatId, response);
            } catch (Exception e) {
                LOG.error("Error processing message", e);
                sendMessage(chatId, "An error occurred while processing your request.");
            }
        }
    }

    public void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode("Markdown");
        try {
            execute(message);
        } catch (TelegramApiException e) {
            LOG.error("Failed to send message to {}", chatId, e);
        }
    }
}
