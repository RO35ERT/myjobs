package com.tumbwe.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "user_preferences")
public class UserPreference extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id")
    public TelegramUser user;

    public String keyword;

    public static List<UserPreference> findByChatId(Long chatId) {
        return find("user.chatId", chatId).list();
    }
}
