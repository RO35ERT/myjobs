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
    private static final long STARTUP_TIME = System.currentTimeMillis() / 1000;
    
    // Simple deduplication map: ChatId -> LastText:Timestamp
    private static final java.util.Map<Long, String> LAST_MESSAGE = new java.util.concurrent.ConcurrentHashMap<>();

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
            Integer messageDate = update.getMessage().getDate();
            Integer updateId = update.getUpdateId();

            // 1. Ignore messages sent before the bot started (backlog)
            if (messageDate != null && messageDate < STARTUP_TIME - 10) {
                LOG.info("Ignoring backlog message from {} (UpdateID: {}): '{}' (msgDate: {}, startup: {}, diff: {})", 
                        chatId, updateId, text, messageDate, STARTUP_TIME, (STARTUP_TIME - messageDate));
                return;
            }

            // 2. Simple deduplication: ignore same message from same user if it arrives within 2 seconds
            String currentKey = text + ":" + (System.currentTimeMillis() / 2000); // 2-sec window
            String lastKey = LAST_MESSAGE.put(chatId, currentKey);
            if (currentKey.equals(lastKey)) {
                return; // Skip duplicate
            }

            try {
                LOG.info("Processing message from {} (UpdateID: {}): '{}'", chatId, updateId, text);
                String response = botService.processMessage(chatId, text);
                sendMessage(chatId, response);
            } catch (Exception e) {
                LOG.error("Error processing message", e);
                sendMessage(chatId, "An error occurred while processing your request.");
            }
        }
    }

    public void sendMessage(Long chatId, String text) {
        if (text == null || text.isBlank()) {
            LOG.warn("Refusing to send empty/blank message to chat {}", chatId);
            return;
        }
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
