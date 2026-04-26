package com.tumbwe.bot;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Registers all Telegram Bot API model classes for reflection,
 * required for Jackson deserialization in GraalVM native image.
 */
@RegisterForReflection(targets = {
    // Telegram Bot infrastructure (instantiated via reflection)
    org.telegram.telegrambots.updatesreceivers.DefaultBotSession.class,
    org.telegram.telegrambots.facilities.TelegramHttpClientBuilder.class,
    org.telegram.telegrambots.meta.generics.BotSession.class,
    // Telegram API model classes (Jackson deserialization)
    org.telegram.telegrambots.meta.api.objects.ApiResponse.class,
    org.telegram.telegrambots.meta.api.objects.Update.class,
    org.telegram.telegrambots.meta.api.objects.Message.class,
    org.telegram.telegrambots.meta.api.objects.Chat.class,
    org.telegram.telegrambots.meta.api.objects.User.class,
    org.telegram.telegrambots.meta.api.objects.MessageEntity.class,
    org.telegram.telegrambots.meta.api.objects.PhotoSize.class,
    org.telegram.telegrambots.meta.api.objects.Document.class,
    org.telegram.telegrambots.meta.api.objects.CallbackQuery.class,
    org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup.class,
    org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup.class,
    org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton.class,
    org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow.class,
    org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton.class,
    org.telegram.telegrambots.meta.api.objects.ChatMemberUpdated.class,
    org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberAdministrator.class,
    org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberBanned.class,
    org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberLeft.class,
    org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberMember.class,
    org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberOwner.class,
    org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberRestricted.class,
    org.telegram.telegrambots.meta.api.objects.chatmember.serialization.ChatMemberDeserializer.class,
    org.telegram.telegrambots.meta.api.methods.send.SendMessage.class,
    org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethodMessage.class,
    org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethodBoolean.class,
    org.telegram.telegrambots.meta.api.methods.BotApiMethod.class,
    org.telegram.telegrambots.meta.api.methods.updates.GetUpdates.class,
    org.telegram.telegrambots.meta.api.methods.updates.SetWebhook.class,
    org.telegram.telegrambots.meta.api.methods.updates.DeleteWebhook.class,
    org.telegram.telegrambots.meta.api.methods.GetMe.class,
    org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands.class,
    com.tumbwe.bot.MyJobsTelegramBot.class
}, registerFullHierarchy = true)
public class TelegramReflectionConfig {
}
