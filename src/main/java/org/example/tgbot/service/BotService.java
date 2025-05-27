package org.example.tgbot.service;

import org.example.tgbot.model.Response;
import org.example.tgbot.repository.ResponseRepository;
import org.example.tgbot.model.User;
import org.example.tgbot.repository.UserRepository;
import org.example.tgbot.utils.BotConstants;
import org.example.tgbot.utils.TelegramApiHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BotService {
    private static final Logger log = LoggerFactory.getLogger(BotService.class);
    private final UserRepository userRepository;
    private final ResponseRepository responseRepository;
    private final ReportService reportService;

    private static final int FORM_TIMEOUT_MINUTES = 1;

    public BotService(UserRepository userRepository, ResponseRepository responseRepository, ReportService reportService) {
        this.userRepository = userRepository;
        this.responseRepository = responseRepository;
        this.reportService = reportService;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void handleUpdate(Update update, AbsSender bot) throws TelegramApiException {
        Long chatId = update.getMessage().getChatId();
        String messageText = update.getMessage().getText();
        log.info("Starting transaction for chatId: {}, message: {}", chatId, messageText);

        User user = userRepository.findById(chatId).orElseGet(() -> {
            log.info("Creating new user for chatId: {}", chatId);
            User newUser = new User();
            newUser.setChatId(chatId);
            newUser.setUsername(update.getMessage().getFrom().getUserName());
            newUser.setState(BotConstants.STATE_IDLE);
            return userRepository.saveAndFlush(newUser);
        });

        log.info("User state for chatId: {} is {}", chatId, user.getState());

        try {
            if (messageText.startsWith("/")) {
                handleCommand(chatId, messageText, user, bot);
            } else {
                handleFormInput(chatId, messageText, user, bot);
            }
        } catch (Exception e) {
            log.error("Error processing update for chatId: {}", chatId, e);
            TelegramApiHelper.sendMessage(bot, chatId, "Произошла ошибка. Попробуйте позже.");
        }

        log.info("Transaction completed for chatId: {}", chatId);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    void handleCommand(Long chatId, String command, User user, AbsSender bot) throws TelegramApiException {
        log.info("Handling command: {} for chatId: {}", command, chatId);
        try {
            switch (command) {
                case "/start":
                    user.setState(BotConstants.STATE_IDLE);
                    userRepository.saveAndFlush(user);
                    TelegramApiHelper.sendMessageWithKeyboard(bot, chatId, BotConstants.WELCOME_MESSAGE);
                    break;
                case "/form":
                    user.setState(BotConstants.STATE_AWAITING_NAME);
                    userRepository.saveAndFlush(user);
                    TelegramApiHelper.sendMessage(bot, chatId, BotConstants.ASK_NAME_MESSAGE);
                    break;
                case "/report":
                    user.setState(BotConstants.STATE_IDLE);
                    userRepository.saveAndFlush(user);
                    TelegramApiHelper.sendMessage(bot, chatId, BotConstants.REPORT_GENERATING_MESSAGE);
                    reportService.generateReport(chatId).thenAccept(file -> {
                        try {
                            TelegramApiHelper.sendDocument(bot, chatId, file);
                        } catch (TelegramApiException e) {
                            log.error("Failed to send report to chatId: {}", chatId, e);
                            try {
                                TelegramApiHelper.sendMessage(bot, chatId, BotConstants.REPORT_ERROR_MESSAGE);
                            } catch (TelegramApiException ex) {
                                log.error("Failed to send error message to chatId: {}", chatId, ex);
                            }
                        }
                    });
                    break;
                default:
                    TelegramApiHelper.sendMessage(bot, chatId, BotConstants.UNKNOWN_COMMAND_MESSAGE);
            }
        } catch (TelegramApiException e) {
            log.error("Telegram API error for chatId: {}", chatId, e);
            throw e;
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    void handleFormInput(Long chatId, String input, User user, AbsSender bot) throws TelegramApiException {
        log.info("Handling form input for chatId: {}, state: {}, input: {}", chatId, user.getState(), input);

        if (isFormExpired(chatId)) {
            resetForm(chatId, user, bot);
            return;
        }

        try {
            switch (user.getState()) {
                case BotConstants.STATE_AWAITING_NAME:
                    handleNameInput(chatId, input, user, bot);
                    break;
                case BotConstants.STATE_AWAITING_EMAIL:
                    handleEmailInput(chatId, input, user, bot);
                    break;
                case BotConstants.STATE_AWAITING_RATING:
                    handleRatingInput(chatId, input, user, bot);
                    break;
                default:
                    handleDefaultInput(chatId, input, user, bot);
            }
        } catch (Exception e) {
            log.error("Error processing form input for chatId: {}", chatId, e);
            TelegramApiHelper.sendMessage(bot, chatId, "Произошла ошибка при обработке формы.");
        }
    }

    private boolean isFormExpired(Long chatId) {
        List<Response> responses = responseRepository.findByUserIdOrderByIdDesc(chatId);
        if (!responses.isEmpty()) {
            Response latestResponse = responses.get(0);
            return !latestResponse.isCompleted() && LocalDateTime.now().isAfter(latestResponse.getCreatedAt().plusMinutes(FORM_TIMEOUT_MINUTES));
        }
        return false;
    }

    private void resetForm(Long chatId, User user, AbsSender bot) throws TelegramApiException {
        List<Response> responses = responseRepository.findByUserIdOrderByIdDesc(chatId);
        if (!responses.isEmpty()) {
            responseRepository.delete(responses.get(0));
        }
        user.setState(BotConstants.STATE_AWAITING_NAME);
        userRepository.saveAndFlush(user);
        TelegramApiHelper.sendMessage(bot, chatId, BotConstants.TIME_EXPIRED_MESSAGE);
    }

    private void handleNameInput(Long chatId, String input, User user, AbsSender bot) throws TelegramApiException {
        if ("\uD83D\uDCDD Заполнить форму".equals(input)) {
            user.setState(BotConstants.STATE_AWAITING_NAME);
            userRepository.saveAndFlush(user);
            TelegramApiHelper.sendMessage(bot, chatId, BotConstants.ASK_NAME_MESSAGE);
            return;
        }
        if ("\uD83D\uDCCA Отчет".equals(input)) {
            handleCommand(chatId, "/report", user, bot);
            return;
        }
        Response response = new Response();
        response.setUserId(chatId);
        response.setName(input);
        response.setCreatedAt(LocalDateTime.now());
        Response savedResponse = responseRepository.saveAndFlush(response);
        log.info("Saved response with name for chatId: {}, responseId: {}", chatId, savedResponse.getId());
        user.setState(BotConstants.STATE_AWAITING_EMAIL);
        userRepository.saveAndFlush(user);
        TelegramApiHelper.sendMessage(bot, chatId, BotConstants.ASK_EMAIL_MESSAGE);
    }

    private void handleEmailInput(Long chatId, String input, User user, AbsSender bot) throws TelegramApiException {
        if ("\uD83D\uDCDD Заполнить форму".equals(input)) {
            user.setState(BotConstants.STATE_AWAITING_NAME);
            userRepository.saveAndFlush(user);
            TelegramApiHelper.sendMessage(bot, chatId, BotConstants.ASK_NAME_MESSAGE);
            return;
        }
        if ("\uD83D\uDCCA Отчет".equals(input)) {
            handleCommand(chatId, "/report", user, bot);
            return;
        }
        if (!BotConstants.EMAIL_PATTERN.matcher(input).matches()) {
            TelegramApiHelper.sendMessage(bot, chatId, BotConstants.INVALID_EMAIL_MESSAGE);
            return;
        }
        List<Response> responses = responseRepository.findByUserIdOrderByIdDesc(chatId);
        if (responses.isEmpty() || isFormExpired(chatId)) {
            resetForm(chatId, user, bot);
            return;
        }
        Response latestResponse = responses.get(0);
        latestResponse.setEmail(input);
        responseRepository.saveAndFlush(latestResponse);
        user.setState(BotConstants.STATE_AWAITING_RATING);
        userRepository.saveAndFlush(user);
        TelegramApiHelper.sendMessage(bot, chatId, BotConstants.ASK_RATING_MESSAGE);
    }

    private void handleRatingInput(Long chatId, String input, User user, AbsSender bot) throws TelegramApiException {
        if ("\uD83D\uDCDD Заполнить форму".equals(input)) {
            user.setState(BotConstants.STATE_AWAITING_NAME);
            userRepository.saveAndFlush(user);
            TelegramApiHelper.sendMessage(bot, chatId, BotConstants.ASK_NAME_MESSAGE);
            return;
        }
        if ("\uD83D\uDCCA Отчет".equals(input)) {
            handleCommand(chatId, "/report", user, bot);
            return;
        }
        try {
            int rating = Integer.parseInt(input);
            if (rating < 1 || rating > 10) {
                TelegramApiHelper.sendMessage(bot, chatId, BotConstants.INVALID_RATING_MESSAGE);
                return;
            }
            List<Response> responses = responseRepository.findByUserIdOrderByIdDesc(chatId);
            if (responses.isEmpty() || isFormExpired(chatId)) {
                resetForm(chatId, user, bot);
                return;
            }
            Response latestResponse = responses.get(0);
            latestResponse.setRating(rating);
            latestResponse.setCompleted(true);
            responseRepository.saveAndFlush(latestResponse);
            user.setState(BotConstants.STATE_IDLE);
            userRepository.saveAndFlush(user);
            TelegramApiHelper.sendMessageWithKeyboard(bot, chatId, BotConstants.FORM_COMPLETED_MESSAGE);
            log.info("Current responses in database: {}", responseRepository.findAll());
        } catch (NumberFormatException e) {
            TelegramApiHelper.sendMessage(bot, chatId, BotConstants.ENTER_NUMBER_MESSAGE);
        }
    }

    private void handleDefaultInput(Long chatId, String input, User user, AbsSender bot) throws TelegramApiException {
        if ("\uD83D\uDCDD Заполнить форму".equals(input)) {
            user.setState(BotConstants.STATE_AWAITING_NAME);
            userRepository.saveAndFlush(user);
            TelegramApiHelper.sendMessage(bot, chatId, BotConstants.ASK_NAME_MESSAGE);
        } else if ("\uD83D\uDCCA Отчет".equals(input)) {
            handleCommand(chatId, "/report", user, bot);
        } else {
            TelegramApiHelper.sendMessage(bot, chatId, BotConstants.UNKNOWN_COMMAND_MESSAGE);
        }
    }

    @Scheduled(fixedRate = 60000) // Если за минуту юзер не заполнил форму, то форма дропается из бд
    @Transactional
    public void cleanupIncompleteForms() {
        log.info("Starting cleanup of incomplete forms");
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(FORM_TIMEOUT_MINUTES);
        List<Response> incompleteForms = responseRepository.findByIsCompletedFalseAndCreatedAtBefore(threshold);
        for (Response response : incompleteForms) {
            log.info("Deleting incomplete form for userId: {}, responseId: {}", response.getUserId(), response.getId());
            responseRepository.delete(response);
        }
        log.info("Completed cleanup, deleted {} incomplete forms", incompleteForms.size());
    }
}