package org.example.tgbot.utils;

import java.util.regex.Pattern;

public final class BotConstants {
    private BotConstants() {}

    // State of user
    public static final String STATE_IDLE = "IDLE";
    public static final String STATE_AWAITING_NAME = "AWAITING_NAME";
    public static final String STATE_AWAITING_EMAIL = "AWAITING_EMAIL";
    public static final String STATE_AWAITING_RATING = "AWAITING_RATING";

    // Messages
    public static final String WELCOME_MESSAGE = "Добро пожаловать! Используйте кнопки ниже или команды /form и /report.";
    public static final String ASK_NAME_MESSAGE = "Пожалуйста, введите ваше имя:";
    public static final String ASK_EMAIL_MESSAGE = "Пожалуйста, введите ваш email:";
    public static final String ASK_RATING_MESSAGE = "Пожалуйста, введите оценку (1-10):";
    public static final String FORM_COMPLETED_MESSAGE = "Форма заполнена! Используйте кнопки ниже для продолжения.";
    public static final String INVALID_EMAIL_MESSAGE = "Неверный формат email. Попробуйте еще раз:";
    public static final String INVALID_RATING_MESSAGE = "Оценка должна быть от 1 до 10. Попробуйте еще раз:";
    public static final String ENTER_NUMBER_MESSAGE = "Пожалуйста, введите число от 1 до 10:";
    public static final String TIME_EXPIRED_MESSAGE = "Время заполнения вышло, попробуйте еще раз.";
    public static final String UNKNOWN_COMMAND_MESSAGE = "Неизвестная команда. Используйте /start, /form или /report.";
    public static final String REPORT_GENERATING_MESSAGE = "Генерация отчета...";
    public static final String REPORT_ERROR_MESSAGE = "Ошибка при генерации отчета.";

    // Email
    public static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
    public static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);
}