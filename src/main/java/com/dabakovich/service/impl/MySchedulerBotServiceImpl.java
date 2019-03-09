package com.dabakovich.service.impl;

import com.dabakovich.entity.*;
import com.dabakovich.service.DayPlaneService;
import com.dabakovich.service.MySchedulerBotService;
import com.dabakovich.service.ReaderService;
import com.dabakovich.service.utils.KeyboardBuilder;
import com.dabakovich.telegram.BotState;
import com.dabakovich.telegram.scheduler.MessageState;
import com.dabakovich.telegram.scheduler.MySchedulerBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.data.domain.Page;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.telegram.telegrambots.api.interfaces.BotApiObject;
import org.telegram.telegrambots.api.methods.BotApiMethod;
import org.telegram.telegrambots.api.methods.ParseMode;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.api.objects.CallbackQuery;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.User;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by dabak on 14.09.2017, 22:37.
 */
@Service
public class MySchedulerBotServiceImpl implements MySchedulerBotService {

    private Logger logger = LoggerFactory.getLogger(MySchedulerBotServiceImpl.class);

    private final BotState state;
    private final MySchedulerBot mySchedulerBot;
    private final ReaderService readerService;
    private final DayPlaneService dayPlaneService;
    private final ReloadableResourceBundleMessageSource MS;

    @Autowired
    public MySchedulerBotServiceImpl(BotState state,
                                     MySchedulerBot mySchedulerBot,
                                     ReaderService readerService,
                                     DayPlaneService dayPlaneService,
                                     ReloadableResourceBundleMessageSource MS) {
        this.state = state;
        this.mySchedulerBot = mySchedulerBot;
        this.readerService = readerService;
        this.dayPlaneService = dayPlaneService;
        this.MS = MS;
    }

    @Scheduled(cron = "0 0 6 * * ?", zone = "GMT+2")
    private void scheduledSendingOfDayPlanes() {
        logger.info("Started daily scheduling...");
        List<Reader> readers = readerService.getReadersByScheduleIsNotNull();

        readers.stream()
                .filter(r -> r.getSchedule().isActive())
                .forEach(this::sendDayPlaneForReader);
    }

    @Override
    public BotApiMethod handleUpdate(Update update) {
        BotApiMethod message;

        if (update.hasCallbackQuery()) {
            message = handleCallbackQuery(update.getCallbackQuery());
        } else if (update.hasMessage()) {
            message = handleMessageUpdate(update.getMessage());
        } else {
            logger.warn("Handled unknown update type: {}", update);
            message = null;
//            new SendMessage().setChatId(update.getMessage().getChatId()).setText("Sorry, I cannot receive that message yet.");
        }

        return message;
    }

    @Override
    public BotApiMethod handleRequestException(BotApiMethod method, Integer errorCode) {
        switch (errorCode) {
            case 400: {
                String chatId = "";
                int messageId = 0;

                if (method instanceof DeleteMessage) {
                    DeleteMessage deleteMessage = (DeleteMessage) method;
                    chatId = deleteMessage.getChatId();
                    messageId = deleteMessage.getMessageId();
                } else if (method instanceof EditMessageText) {
                    EditMessageText editMessageText = (EditMessageText) method;
                    chatId = editMessageText.getChatId();
                    messageId = editMessageText.getMessageId();
                }

                Locale locale = state.getLocale(Integer.valueOf(chatId));
                return new EditMessageText()
                        .setChatId(chatId)
                        .setMessageId(messageId)
                        .setText(MS.getMessage("telegram.ui.message.exited", null, locale));

            }
            default:
                return null;
        }


    }

    private BotApiMethod handleCallbackQuery(CallbackQuery callbackQuery) {
        if (isNewUser(callbackQuery.getFrom().getId()))
            return getNewUserMessage(callbackQuery.getFrom(), callbackQuery.getMessage().getChatId());

        String data = callbackQuery.getData();
        UriComponents URI = UriComponentsBuilder.fromUriString(data).build();
        MultiValueMap<String, String> params = URI.getQueryParams();
        String path = URI.getPath();
//        String[] path = data.split(":");

        assert path != null;
        switch (path) {
            case "/":
                return getMainMenuMessage(callbackQuery);
            case "/exit":
                return getDeleteMessageWithCallbackQuery(callbackQuery);
            case "/create":
                return createPath(callbackQuery, params);
            case "/settings":
                return settingsPath(callbackQuery, params);
            case "/settings/stop":
                return settingsStopPath(callbackQuery, params);
            case "/settings/language":
                return settingsLanguagePath(callbackQuery, params);
            case "/passages/get-next":
                return getTodayPassagesMessage(callbackQuery);
            default:
                return defaultMessage(callbackQuery);
        }
    }

    private BotApiMethod handleMessageUpdate(Message message) {
        if (isNewUser(message.getFrom().getId())) return getNewUserMessage(message.getFrom(), message.getChatId());

        switch (message.getText()) {
            case "/menu":
                return menuCommand(message);
            case "/start":
                return startCommand(message);
            case "/help":
                return helpCommand(message);
        }

        switch (state.getMessageState(message.getFrom().getId())) {
            case DEFAULT:
                return handleMainMenuMessageUpdate(message);
            case CHOOSE_START_PASSAGE:
                return onEnteredStartPassage(message);
        }

        return new SendMessage()
                .setChatId(message.getChatId())
                .setText(message.getText())
                .setReplyMarkup(getMainMenuReplyKeyboardMarkup(message.getChatId()));
    }

    private BotApiMethod getNewUserMessage(User from, Long chatId) {
        Reader reader = readerService.saveTelegramUser(from);
        Locale locale = state.getLocale(from.getId());
        InlineKeyboardMarkup keyboardMarkup = KeyboardBuilder.createInlineKeyboard()
                .addCallbackButton(MS.getMessage("telegram.ui.button.yes", null, locale), "/create")
                .addCallbackButton(MS.getMessage("telegram.ui.button.no", null, locale), "/")
                .addRow()
                .addCallbackButton(MS.getMessage("telegram.ui.button.change_language", null, locale), "/settings/language")
                .build();

        return new SendMessage()
                .setChatId(chatId)
                .setText(MS.getMessage("telegram.ui.message.welcome_message", new Object[]{reader.getName()}, locale))
                .setReplyMarkup(keyboardMarkup);
    }

    private BotApiMethod getMainMenuMessage(BotApiObject botApiObject) {
        Integer telegramId;
        if (botApiObject instanceof Message) telegramId = ((Message) botApiObject).getFrom().getId();
        else telegramId = ((CallbackQuery) botApiObject).getFrom().getId();
        Locale locale = state.getLocale(telegramId);
        Reader reader = readerService.getByTelegramId(telegramId);
        KeyboardBuilder.InlineKeyboardBuilder inlineKeyboard = KeyboardBuilder.createInlineKeyboard();
        if (reader.getSchedule() == null)
            inlineKeyboard.addCallbackButton(MS.getMessage("telegram.ui.button.create", null, locale), "/create");
        else if (reader.getSchedule().getDayNumber() >= 0)
            inlineKeyboard.addCallbackButton(MS.getMessage("telegram.ui.button.get_next_passages", null, locale), "/passages/get-next");
        inlineKeyboard
                .addCallbackButton(MS.getMessage("telegram.ui.button.settings", null, locale), "/settings")
                .addRow()
                .addCallbackButton(MS.getMessage("telegram.ui.button.exit", null, locale), "/exit");

        if (botApiObject instanceof Message) {
            Message message = (Message) botApiObject;
            return new SendMessage()
                    .setChatId(message.getChatId())
                    .setText(MS.getMessage("telegram.ui.message.main_menu", null, locale))
                    .setReplyMarkup(inlineKeyboard.build());
        } else {
            CallbackQuery callbackQuery = (CallbackQuery) botApiObject;
            return new EditMessageText()
                    .setMessageId(callbackQuery.getMessage().getMessageId())
                    .setChatId(callbackQuery.getMessage().getChatId())
                    .setText(MS.getMessage("telegram.ui.message.main_menu", null, locale))
                    .setReplyMarkup(inlineKeyboard.build());
        }
    }

    // FOR CREATING:
    private BotApiMethod createPath(CallbackQuery callbackQuery, MultiValueMap<String, String> params) {
        if (params.size() == 0) return getCreateTypeEditMessage(callbackQuery);
        if (params.containsKey("type")) return createTypeHandler(callbackQuery, params.get("type").get(0));
        if (params.containsKey("when")) return createWhenHandler(callbackQuery, params.get("when").get(0));
        if (params.containsKey("page")) return createPageHandler(callbackQuery, params.get("page").get(0));

        return defaultMessage(callbackQuery);
    }

    private BotApiMethod getCreateTypeEditMessage(CallbackQuery callbackQuery) {
        Locale locale = state.getLocale(callbackQuery.getFrom().getId());
        String[] scheduleTypesStrings = MS.getMessage("scheduler.schedule_types", null, locale).split(",");
        Map<String, String> scheduleTypes = new LinkedHashMap<>();
        for (String scheduleTypeString : scheduleTypesStrings) {
            String[] pair = scheduleTypeString.split(":");
            scheduleTypes.put(pair[0], pair[1]);
        }
//
//        Map<String, String> scheduleTypes = Arrays
//                .stream(scheduleTypesString.split(","))
//                .map(s -> s.split(":"))
//                .collect(Collectors.toMap(s -> s[0], s -> s[1]));

        KeyboardBuilder.InlineKeyboardBuilder keyboardBuilder = KeyboardBuilder.createInlineKeyboard();
        scheduleTypes.forEach((k, v) -> keyboardBuilder.addCallbackButton(v, "/create?type=" + k));

        InlineKeyboardMarkup keyboardMarkup = keyboardBuilder
                .addRow()
                .addCallbackButton(MS.getMessage("telegram.ui.button.return", null, locale), "/create?type=return")
                .build();

        return new EditMessageText()
                .setChatId(callbackQuery.getMessage().getChatId())
                .setMessageId(callbackQuery.getMessage().getMessageId())
                .setText(MS.getMessage("telegram.ui.message.choose_schedule_type", null, locale))
                .setReplyMarkup(keyboardMarkup);
    }

    private BotApiMethod createTypeHandler(CallbackQuery callbackQuery, String argument) {
        if (argument.equals("return")) return getMainMenuMessage(callbackQuery);

        ScheduleType type = ScheduleType.valueOf(argument);
        Schedule schedule = new Schedule();
        schedule.setScheduleType(type);

        readerService.saveScheduleForTelegramId(schedule, callbackQuery.getFrom().getId());

        return getCreateWhenEditMessage(callbackQuery);
    }

    private BotApiMethod getCreateWhenEditMessage(CallbackQuery callbackQuery) {
        Locale locale = state.getLocale(callbackQuery.getFrom().getId());
        InlineKeyboardMarkup keyboardMarkup = KeyboardBuilder.createInlineKeyboard()
                .addCallbackButton(MS.getMessage("telegram.ui.button.from_beginning", null, locale), "/create?when=from-beginning")
                .addRow()
                .addCallbackButton(MS.getMessage("telegram.ui.button.choose_start_passages", null, locale), "/create?page=0")
                .addRow()
                .addCallbackButton(MS.getMessage("telegram.ui.button.return", null, locale), "/create?when=return")
                .build();

        return new EditMessageText()
                .setMessageId(callbackQuery.getMessage().getMessageId())
                .setChatId(callbackQuery.getMessage().getChatId())
                .setText(MS.getMessage("telegram.ui.message.where_start_schedule", null, locale))
                .setReplyMarkup(keyboardMarkup);
    }

    @SuppressWarnings("unchecked")
    private BotApiMethod createWhenHandler(CallbackQuery callbackQuery, String argument) {
        Schedule schedule = readerService.getScheduleByTelegramId(callbackQuery.getFrom().getId());

        switch (argument) {
            case "from-beginning":
                schedule.setDayNumber(0);
                break;
            case "manual":
                return getCreateWhenManualEditMessage(callbackQuery);
            case "return": {
                state.putMessageState(callbackQuery.getFrom().getId(), MessageState.DEFAULT);
                return getCreateTypeEditMessage(callbackQuery);
            }
        }
        readerService.saveScheduleForTelegramId(schedule, callbackQuery.getFrom().getId());
        try {
            mySchedulerBot.execute(getDeleteMessageWithCallbackQuery(callbackQuery));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        return getScheduleCreatedMessage(callbackQuery, schedule.getDayNumber());
    }

    private BotApiMethod getCreateWhenManualEditMessage(CallbackQuery callbackQuery) {
        Locale locale = state.getLocale(callbackQuery.getFrom().getId());
        InlineKeyboardMarkup keyboardMarkup = KeyboardBuilder.createInlineKeyboard()
                .addCallbackButton(MS.getMessage("telegram.ui.button.return", null, locale), "/create?when=return")
                .build();
        state.putMessageState(callbackQuery.getFrom().getId(), MessageState.CHOOSE_START_PASSAGE);

        return new EditMessageText()
                .setMessageId(callbackQuery.getMessage().getMessageId())
                .setChatId(callbackQuery.getMessage().getChatId())
                .setText(MS.getMessage("telegram.ui.message.enter_passage_number", null, locale))
                .setReplyMarkup(keyboardMarkup);
    }

    private BotApiMethod createPageHandler(CallbackQuery callbackQuery, String argument) {
        if (argument.equals("return"))
            return getCreateWhenEditMessage(callbackQuery);

        int page = Integer.parseInt(argument);
        Locale locale = state.getLocale(callbackQuery.getFrom().getId());
        ScheduleType scheduleType = readerService
                .getByTelegramId(callbackQuery.getFrom().getId())
                .getSchedule()
                .getScheduleType();
        Page<DayPlane> dayPlanePage = dayPlaneService.findDayPlanePage(scheduleType, page,50);
        state.putMessageState(callbackQuery.getFrom().getId(), MessageState.CHOOSE_START_PASSAGE);

        return new EditMessageText().setChatId(callbackQuery.getMessage().getChatId())
                .setMessageId(callbackQuery.getMessage().getMessageId())
                .setText(getDayPlanePageString(dayPlanePage, page, locale))
                .setReplyMarkup(getPaginationButtons(page, dayPlanePage.getTotalPages(), locale))
                .setParseMode(ParseMode.MARKDOWN);
    }

    private BotApiMethod onEnteredStartPassage(Message message) {
        Integer dayNumber;
        Schedule schedule = readerService.getScheduleByTelegramId(message.getFrom().getId());
        int countOfPassages = dayPlaneService.countByScheduleType(schedule.getScheduleType());
        try {
            dayNumber = Integer.valueOf(message.getText());
            if (dayNumber < 1 || dayNumber > countOfPassages) {
                logger.error("Bad number format: out of range");
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            Locale locale = state.getLocale(message.getFrom().getId());
            return new SendMessage()
//                    .setMessageId(message.getMessageId())
                    .setChatId(message.getChatId())
                    .setText(MS.getMessage("telegram.ui.message.invalid_number", null, locale))
                    .setReplyMarkup(KeyboardBuilder.createInlineKeyboard()
                            .addCallbackButton(MS.getMessage("telegram.ui.button.return", null, locale), "/create?when=return")
                            .build());
        }

        state.putMessageState(message.getFrom().getId(), MessageState.DEFAULT);
        schedule.setDayNumber(dayNumber - 1);
        readerService.saveScheduleForTelegramId(schedule, message.getFrom().getId());

        return getScheduleCreatedMessage(message, schedule.getDayNumber());
//        return null;
    }

    private BotApiMethod getScheduleCreatedMessage(BotApiObject botApiObject, int sequenceNumber) {
        Integer telegramId;
        SendMessage sendMessage = new SendMessage();
        if (botApiObject instanceof CallbackQuery) {
            CallbackQuery callbackQuery = (CallbackQuery) botApiObject;
            telegramId = callbackQuery.getFrom().getId();
            sendMessage
                    .setChatId(callbackQuery.getMessage().getChatId())
                    .setReplyMarkup(getMainMenuReplyKeyboardMarkup(callbackQuery.getMessage().getChatId()));
        }
        else {
            Message message = (Message) botApiObject;
            telegramId = (message).getFrom().getId();
            sendMessage
                    .setChatId(message.getChatId())
                    .setReplyMarkup(getMainMenuReplyKeyboardMarkup(message.getChatId()));
        }
        Locale locale = state.getLocale(telegramId);
        KeyboardBuilder.InlineKeyboardBuilder inlineKeyboard = KeyboardBuilder
                .createInlineKeyboard();

        if (sequenceNumber >= 0) inlineKeyboard
                .addCallbackButton(MS.getMessage("telegram.ui.button.get_next_passages", null, locale), "/passages/get-next")
                .addRow();
        inlineKeyboard
                .addCallbackButton(MS.getMessage("telegram.ui.button.exit", null, locale), "/exit");

        String[] createdMessages = MS.getMessage("telegram.ui.message.created_schedule", null, locale).split("\\|");

//        if ()

        Reader reader = readerService.getByTelegramId(telegramId);
        if (!reader.isReadWelcomeMessage()) {
            for (String createdMessage : createdMessages) {
                try {
                    Integer millis = createdMessage.length() * 50;
                    mySchedulerBot.execute(sendMessage.setText(createdMessage));
                    Thread.sleep(millis);
                } catch (TelegramApiException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
            reader.setReadWelcomeMessage(true);
            readerService.save(reader);
        }

        return sendMessage
                .setText(MS.getMessage("telegram.ui.message.get_first_passages",
                        new Object[]{MS.getMessage("telegram.ui.button.get_next_passages", null, locale)},
                        locale))
                .setParseMode(ParseMode.MARKDOWN)
                .setReplyMarkup(inlineKeyboard.build());
    }

    // FOR SETTINGS:
    private BotApiMethod settingsPath(CallbackQuery callbackQuery, MultiValueMap<String, String> params) {
        if (params.size() == 0) return getSettingsMenuEditMessage(callbackQuery);
        if (params.containsKey("active")) return settingsActiveHandler(callbackQuery, params.get("active").get(0));
        return defaultMessage(callbackQuery);
    }

    private BotApiMethod getSettingsMenuEditMessage(CallbackQuery callbackQuery) {
        Integer telegramId = callbackQuery.getFrom().getId();

        Locale locale = state.getLocale(telegramId);
        Reader reader = readerService.getByTelegramId(telegramId);

        KeyboardBuilder.InlineKeyboardBuilder inlineKeyboard = KeyboardBuilder.createInlineKeyboard();
        if (reader.getSchedule() != null) {
            if (reader.getSchedule().isActive()) inlineKeyboard
                    .addCallbackButton(MS.getMessage("telegram.ui.button.pause", null, locale), "/settings?active=false");
            else inlineKeyboard
                    .addCallbackButton(MS.getMessage("telegram.ui.button.resume", null, locale), "/settings?active=true");

            inlineKeyboard
                    .addCallbackButton(MS.getMessage("telegram.ui.button.stop", null, locale), "/settings/stop")
                    .addRow();
        }
        inlineKeyboard
                .addCallbackButton(MS.getMessage("telegram.ui.button.language", null, locale), "/settings/language")
                .addCallbackButton(MS.getMessage("telegram.ui.button.return", null, locale), "/settings?return");

        return new EditMessageText()
                .setMessageId(callbackQuery.getMessage().getMessageId())
                .setChatId(callbackQuery.getMessage().getChatId())
                .setText(MS.getMessage("telegram.ui.message.settings_menu", null, locale))
                .setReplyMarkup(inlineKeyboard.build());

    }

    private BotApiMethod settingsActiveHandler(CallbackQuery callbackQuery, String argument) {
        Reader reader = readerService.getByTelegramId(callbackQuery.getFrom().getId());
        reader.getSchedule().setActive(Boolean.parseBoolean(argument));
        readerService.save(reader);
        return getSettingsMenuEditMessage(callbackQuery);
    }

    private BotApiMethod settingsStopPath(CallbackQuery callbackQuery, MultiValueMap<String, String> params) {
        if (params.size() == 0) return getSettingsStopConfirmMessage(callbackQuery);
        if (params.containsKey("confirmed")) return settingsStopHandler(callbackQuery, params.get("confirmed").get(0));
        return defaultMessage(callbackQuery);
    }

    private BotApiMethod getSettingsStopConfirmMessage(CallbackQuery callbackQuery) {
        Locale locale = state.getLocale(callbackQuery.getFrom().getId());
        InlineKeyboardMarkup keyboardMarkup = KeyboardBuilder.createInlineKeyboard()
                .addCallbackButton(MS.getMessage("telegram.ui.button.yes", null, locale), "/settings/stop?confirmed=true")
                .addCallbackButton(MS.getMessage("telegram.ui.button.no", null, locale), "/settings/stop?confirmed=false")
                .build();
        return new EditMessageText()
                .setMessageId(callbackQuery.getMessage().getMessageId())
                .setChatId(callbackQuery.getMessage().getChatId())
                .setText(MS.getMessage("telegram.ui.message.confirm_stop_schedule", null, locale))
                .setReplyMarkup(keyboardMarkup);
    }

    private BotApiMethod settingsStopHandler(CallbackQuery callbackQuery, String argument) {
        boolean confirmed = Boolean.parseBoolean(argument);
        if (confirmed) {
            Reader reader = readerService.getByTelegramId(callbackQuery.getFrom().getId());
            reader.setSchedule(null);
            readerService.save(reader);
            return getMainMenuMessage(callbackQuery);
        } else return getSettingsMenuEditMessage(callbackQuery);
    }

    private BotApiMethod settingsLanguagePath(CallbackQuery callbackQuery, MultiValueMap<String, String> params) {
        if (params.size() == 0) return getSettingsLanguagesEditMessage(callbackQuery);
        if (params.containsKey("language"))
            return settingsLanguageHandler(callbackQuery, params.get("language").get(0));
        return defaultMessage(callbackQuery);
    }

    private BotApiMethod getSettingsLanguagesEditMessage(CallbackQuery callbackQuery) {
        Locale locale = state.getLocale(callbackQuery.getFrom().getId());
        KeyboardBuilder.InlineKeyboardBuilder inlineKeyboard = KeyboardBuilder
                .createInlineKeyboard();
        Map<String, String> languages = state.getAvailableLanguages();
        languages.forEach((tag, name) -> inlineKeyboard
                .addCallbackButton(name, "/settings/language?language=" + tag)
                .addRow());
        inlineKeyboard.addCallbackButton(
                MS.getMessage("telegram.ui.button.return", null, locale),
                "/settings/language?language=return");

        return new EditMessageText()
                .setMessageId(callbackQuery.getMessage().getMessageId())
                .setChatId(callbackQuery.getMessage().getChatId())
                .setText(MS.getMessage("telegram.ui.message.select_language", null, locale))
                .setReplyMarkup(inlineKeyboard.build());
    }

    private BotApiMethod settingsLanguageHandler(CallbackQuery callbackQuery, String argument) {
        if (argument.equals("return")) return getSettingsMenuEditMessage(callbackQuery);

        Map<String, String> languages = state.getAvailableLanguages();
        if (languages.containsKey(argument)) {
            Reader reader = readerService.getByTelegramId(callbackQuery.getFrom().getId());
            reader.setLanguageTag(argument);
            readerService.save(reader);
        }
        return getMainMenuMessage(callbackQuery);
    }

    // FOR PASSAGES:
    private void sendDayPlaneForReader(Reader reader) {
        SendMessage message = getTodayPassagesMessage(reader);

        try {
            mySchedulerBot.execute(message);
            logger.info("Successfully sent passages for {}", reader.getName());

            if ((reader.getSchedule().getDayNumber() + 1) >= dayPlaneService.countByScheduleType(reader.getSchedule().getScheduleType())) {
                reader.setSchedule(null);
                readerService.save(reader);
                logger.info("{} successfully finished scheduling", reader.getName());
            } else {
                readerService.save(reader.forwardScheduling());
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private BotApiMethod getTodayPassagesMessage(CallbackQuery callbackQuery) {
        sendDayPlaneForReader(readerService.getByTelegramId(callbackQuery.getFrom().getId()));

        return getDeleteMessageWithCallbackQuery(callbackQuery);
    }

    private SendMessage getTodayPassagesMessage(Reader reader) {
        DayPlane dayPlane = dayPlaneService.getByScheduleTypeAndSequenceNumber(
                reader.getSchedule().getScheduleType(),
                reader.getSchedule().getDayNumber());
        LocalDate now = LocalDate.now();
        Locale locale = state.getLocale(Math.toIntExact(reader.getTelegramId()));
        StringBuilder text = new StringBuilder(MS.getMessage("scheduler.title", new Object[]{now.format(DateTimeFormatter.ofPattern("dd.MM.yy"))}, locale));
        String url = getUrl(dayPlane.getPassages(), locale);
        text.append(MS.getMessage("scheduler.text", new Object[]{url, reader.getSchedule().getDayNumber() + 1}, locale));

        for (Passage passage : dayPlane.getPassages()) {
            text.append(MS.getMessage("book." + passage.getBook(), null, locale))
                    .append(" ").append(passage.getVerses()).append("\n");
        }

        return new SendMessage()
                .setChatId(reader.getTelegramId())
                .setText(text.toString())
                .setParseMode(ParseMode.MARKDOWN)
                .setReplyMarkup(getMainMenuReplyKeyboardMarkup(reader.getTelegramId()))
                .disableWebPagePreview();
    }

    // COMMAND HANDLERS:
    private BotApiMethod menuCommand(Message message) {
        return getMainMenuMessage(message);
    }

    private BotApiMethod startCommand(Message message) {
        readerService.deleteReader(message.getFrom().getId());
        readerService.saveTelegramUser(message.getFrom());
        return getNewUserMessage(message.getFrom(), message.getChatId());
    }

    private BotApiMethod helpCommand(Message message) {
        Locale locale = state.getLocale(message.getFrom().getId());
        return new SendMessage()
                .setChatId(message.getChatId())
                .setText(MS.getMessage("telegram.ui.message.help", null, locale))
                .setReplyMarkup(getMainMenuReplyKeyboardMarkup(message.getChatId()));
    }

    // UTILS:
    private BotApiMethod handleMainMenuMessageUpdate(Message message) {
        Locale locale = state.getLocale(message.getFrom().getId());
        String text = message.getText();
        if (text.equals(MS.getMessage("telegram.ui.button.open_menu", null, locale)))
            return getMainMenuMessage(message);

        return new SendMessage()
                .setChatId(message.getChatId())
                .setText(message.getText())
                .setReplyMarkup(getMainMenuReplyKeyboardMarkup(message.getChatId()));
    }

    private BotApiMethod defaultMessage(BotApiObject botApiObject) {
        return getMainMenuMessage(botApiObject);
    }

    private BotApiMethod getDeleteMessageWithCallbackQuery(CallbackQuery callbackQuery) {
        return new DeleteMessage()
                .setChatId(callbackQuery.getMessage().getChatId().toString())
                .setMessageId(callbackQuery.getMessage().getMessageId());
    }

    private boolean isNewUser(Integer id) {
        Reader reader = readerService.getByTelegramId(id);
        return reader == null;
    }

    private ReplyKeyboardMarkup getMainMenuReplyKeyboardMarkup(Long chatId) {
        Locale locale = state.getLocale(Math.toIntExact(chatId));
        return KeyboardBuilder
                .createReplyKeyboard()
                .addButton(MS.getMessage("telegram.ui.button.open_menu", null, locale))
                .setResizeKeyboard(true)
                .setSelective(true)
                .build();
    }

    private String getDayPlanePageString(Page<DayPlane> dayPlanePage, int page, Locale locale) {
        StringBuilder builder = new StringBuilder();
        builder.append(MS.getMessage("telegram.ui.message.enter_passage_number",
                null,
                locale));
        dayPlanePage.getContent().forEach(dp -> {
            builder.append(dp.getSequenceNumber() + 1).append(". ");
            dp.getPassages().forEach(p -> builder
                    .append(MS.getMessage("book.short." + p.getBook(), null, locale))
                    .append(" ")
                    .append(p.getVerses())
                    .append("; "));
            builder.append("\n");
        });
        builder.append(MS.getMessage("telegram.ui.message.page_count",
                new Object[]{page + 1, dayPlanePage.getTotalPages()},
                locale));
        return builder.toString();
    }

    private InlineKeyboardMarkup getPaginationButtons(int page, int totalPages, Locale locale) {
        KeyboardBuilder.InlineKeyboardBuilder builder = KeyboardBuilder.createInlineKeyboard();
        if (page != 0) builder.addCallbackButton("<", "/create?page=" + (page - 1));
        if (page != (totalPages - 1)) builder.addCallbackButton(">", "/create?page=" + (page + 1));

        builder.addRow()
                .addCallbackButton(MS.getMessage("telegram.ui.button.return", null, locale), "/create?page=return");

        return builder.build();
    }

    private String getUrl(List<Passage> passages, Locale locale) {
        String url = MS.getMessage("wol.url.search", null, locale);
        StringBuilder parameters = new StringBuilder();
        int last = passages.size() - 1;
        for (int i = 0; i < passages.size(); i++) {
            parameters.append(MS.getMessage("book.short." + passages.get(i).getBook(), null, locale))
                    .append(" ")
                    .append(passages.get(i).getVerses());
            if (i < last) parameters.append("; ");
        }
        try {
            return url + "?q=" + URLEncoder.encode(parameters.toString(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return url + "?q=" + parameters.toString();
        }
    }
}
