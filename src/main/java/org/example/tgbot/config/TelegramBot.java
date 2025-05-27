package org.example.tgbot.config;

import org.example.tgbot.service.BotService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class TelegramBot extends TelegramLongPollingBot {
    private final BotService botService;
    private final String botUsername;

    public TelegramBot(@Value("${bot.token}") String botToken, @Value("${bot.username}") String botUsername, BotService botService) {
        super(botToken);
        this.botUsername = botUsername;
        this.botService = botService;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            try {
                botService.handleUpdate(update, this);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }
}