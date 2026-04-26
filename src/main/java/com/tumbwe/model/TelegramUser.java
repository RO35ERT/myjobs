package com.tumbwe.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "telegram_users")
public class TelegramUser extends PanacheEntityBase {
    @Id
    public Long chatId;

    public String name;

    @Enumerated(EnumType.STRING)
    public UserState state;

    @Enumerated(EnumType.STRING)
    public NotificationFrequency frequency;

    public LocalDateTime lastNotifiedAt;
    public LocalDateTime registeredAt;

    public static TelegramUser findByChatId(Long chatId) {
        return find("chatId", chatId).firstResult();
    }
}
