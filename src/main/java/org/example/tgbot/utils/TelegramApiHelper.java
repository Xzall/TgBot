package org.example.tgbot.utils;

import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TelegramApiHelper {
    public static void sendMessage(AbsSender bot, Long chatId, String text) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        bot.execute(message);
    }

    public static void sendMessageWithKeyboard(AbsSender bot, Long chatId, String text) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton("\uD83D\uDCDD Заполнить форму"));
        row.add(new KeyboardButton("\uD83D\uDCCA Отчет"));
        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setIsPersistent(true);

        message.setReplyMarkup(keyboardMarkup);
        bot.execute(message);
    }

    public static void sendDocument(AbsSender bot, Long chatId, File file) throws TelegramApiException {
        SendDocument document = new SendDocument();
        document.setChatId(chatId.toString());
        document.setDocument(new InputFile(file));
        bot.execute(document);
        if (file.delete()) {
            System.out.println("Deleted temporary file: " + file.getAbsolutePath());
        } else {
            System.out.println("Failed to delete temporary file: " + file.getAbsolutePath());
        }
    }
}