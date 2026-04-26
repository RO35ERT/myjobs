package com.tumbwe.bot;

import com.tumbwe.model.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class BotService {
    private static final Logger LOG = LoggerFactory.getLogger(BotService.class);


    private static final String HELP_MESSAGE =
            "Here's what you can do:\n\n" +
            "👤 /me — View your profile and preferences\n" +
            "✏️ /edit — Update your job preferences or notification frequency\n" +
            "🔄 /start — Restart your registration from scratch\n\n" +
            "You'll receive job alerts based on your preferences. Stay tuned! 🚀";

    private static final String FREQUENCY_PROMPT =
            "How often do you want to be notified?\n\n" +
            "1 - When found (every ~30 mins)\n" +
            "2 - Every 1 hour\n" +
            "3 - Every 6 hours\n" +
            "4 - Every 12 hours (once a day)";

    @Transactional
    public String processMessage(Long chatId, String text) {
        TelegramUser user = TelegramUser.findByChatId(chatId);
        
        if (text == null) return "⚠️ Empty message.";
        if (text.length() > 500) {
            return "⚠️ Your message is too long. Please keep it under 500 characters.";
        }

        String trimmedText = text.trim();

        // ── Global commands (work at any state) ────────────────────────────
        if ("/start".equals(trimmedText)) {
            if (user == null) {
                user = new TelegramUser();
                user.chatId = chatId;
                user.registeredAt = LocalDateTime.now();
                user.lastNotifiedAt = LocalDateTime.now();
            }
            user.state = UserState.AWAITING_NAME;
            user.name = null;
            user.frequency = null;
            user.persist();
            UserPreference.delete("user.chatId", chatId);
            return "👋 Welcome! What should we call you?";
        }

        if (user == null) {
            return "👋 Hi! Send /start to get set up and start receiving job alerts.";
        }

        if ("/me".equals(trimmedText)) {
            return buildProfile(user, chatId);
        }

        if ("/edit".equals(trimmedText)) {
            if (user.state != UserState.REGISTERED
                    && user.state != UserState.EDITING_JOBS
                    && user.state != UserState.EDITING_FREQUENCY) {
                return "⚠️ Please finish your registration first before editing.";
            }
            user.state = UserState.EDITING_JOBS;
            user.persist();
            return "✏️ *Edit Job Preferences*\n\nSend your new job list separated by commas.\n" +
                   "Example: Java Developer, Driver, Database Administrator\n\n" +
                   "Your current preferences: " + buildKeywordList(chatId);
        }

        if ("/help".equals(trimmedText)) {
            return HELP_MESSAGE;
        }

        // ── State machine ───────────────────────────────────────────────────
        switch (user.state) {
            case AWAITING_NAME:
                user.name = trimmedText;
                user.state = UserState.AWAITING_JOBS;
                user.persist();
                return "Thank you " + trimmedText + " for signing up! 🎉\n\n" +
                       "Please list the jobs you are looking for, separated by commas.\n" +
                       "Example: Java Developer, Driver, Database Administrator";

            case AWAITING_JOBS:
                saveKeywords(user, chatId, trimmedText);
                user.state = UserState.AWAITING_FREQUENCY;
                user.persist();
                return FREQUENCY_PROMPT;

            case AWAITING_FREQUENCY:
                String freqError = applyFrequency(user, trimmedText);
                if (freqError != null) return freqError;
                user.state = UserState.REGISTERED;
                user.persist();
                return "✅ You're all set up, " + user.name + "!\n\n" + HELP_MESSAGE;

            case EDITING_JOBS:
                UserPreference.delete("user.chatId", chatId);
                saveKeywords(user, chatId, trimmedText);
                user.state = UserState.EDITING_FREQUENCY;
                user.persist();
                return "✅ Job preferences updated!\n\n" + FREQUENCY_PROMPT;

            case EDITING_FREQUENCY:
                String editFreqError = applyFrequency(user, trimmedText);
                if (editFreqError != null) return editFreqError;
                user.state = UserState.REGISTERED;
                user.persist();
                return "✅ All done! Your preferences have been updated.\n\n" + buildProfile(user, chatId);

            case REGISTERED:
                return "You're all set! Use /help to see available commands.";

            default:
                return "Something went wrong. Send /start to begin again.";
        }
    }

    private void saveKeywords(TelegramUser user, Long chatId, String text) {
        String[] jobs = text.split(",");
        for (String job : jobs) {
            String kw = job.trim();
            if (!kw.isBlank()) {
                UserPreference pref = new UserPreference();
                pref.user = user;
                pref.keyword = kw;
                pref.persist();
            }
        }
    }

    private String applyFrequency(TelegramUser user, String text) {
        switch (text.trim()) {
            case "1": user.frequency = NotificationFrequency.WHEN_FOUND;   break;
            case "2": user.frequency = NotificationFrequency.ONE_HOUR;     break;
            case "3": user.frequency = NotificationFrequency.SIX_HOURS;    break;
            case "4": user.frequency = NotificationFrequency.TWELVE_HOURS; break;
            default:  return "⚠️ Invalid option. Please reply with 1, 2, 3, or 4.";
        }
        return null;
    }

    private String buildProfile(TelegramUser user, Long chatId) {
        if (user.state != UserState.REGISTERED) {
            return "👤 *Profile (Incomplete)*\n\n" +
                   "You haven't finished setting up your profile yet! Send /start to complete your registration.\n\n" +
                   "🏷️ Name: " + (user.name != null ? user.name : "Not set") + "\n" +
                   "🔍 Job Preferences: " + buildKeywordList(chatId);
        }

        String keywords = buildKeywordList(chatId);
        String freq = user.frequency != null ? friendlyFrequency(user.frequency) : "Not set";
        return "👤 *Your Profile*\n\n" +
               "🏷️ Name: " + user.name + "\n" +
               "🔍 Job Preferences: " + keywords + "\n" +
               "🔔 Notification Frequency: " + freq;
    }

    private String buildKeywordList(Long chatId) {
        List<UserPreference> prefs = UserPreference.findByChatId(chatId);
        if (prefs.isEmpty()) return "None";
        return prefs.stream()
                    .map(p -> p.keyword)
                    .collect(Collectors.joining(", "));
    }

    private String friendlyFrequency(NotificationFrequency f) {
        return switch (f) {
            case WHEN_FOUND   -> "When found (~30 mins)";
            case ONE_HOUR     -> "Every 1 hour";
            case SIX_HOURS    -> "Every 6 hours";
            case TWELVE_HOURS -> "Every 12 hours";
        };
    }
}
