package org.example.tgbot.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Component
public class BotLauncher implements CommandLineRunner {
    private final TelegramBot telegramBot;

    public BotLauncher(TelegramBot telegramBot) {
        this.telegramBot = telegramBot;
    }

    @Override
    public void run(String... args) throws Exception {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(telegramBot);
        System.out.println("Telegram бот успешно запущен!");
    }
}