package bot;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class BotUpdatesListener implements UpdatesListener {

    private static final String START_REQUEST = "/start";

    private static final String WAITING_FOR_EMAIL_RESPONSE = "Klondike Team thanks you for participating in the BIG Contest, it is very important for us! " +
            "To verify you and add to the list of all participants we need to ask you only TWO small and easy questions. \n" +
            "\n Please write your valid e-mail address. ";
    private static final String WAITING_FOR_ESTIMATION_RESPONSE = "Thank you for your answer! Before you finish" +
            " the process, we need you to give your PREDICTION about the Bitcoin Price at 10.07.2019 6 PM UTC time. " +
            "In other words, write your answer to the question - \"How much will Bitcoin cost at the mentioned date?\".         ";
    private static final String ESTIMATION_RECORDED_RESPONSE_1 = "Done! Thank you for filling this form. You have completed one of two steps needed" +
            " for joining the contest! If you still haven't done the second one - you can do it right now. " +
            "Register on the BitMEX exchange using our Affiliate Link: https://www.bitmex.com/register/bwaM1j. In case one of two steps won't be completed," +
            " we won't be able to consider you a competitor. \n" +
            "\n Want to remind you that we will pick the WINNERS on the 10th of July (6 PM UTC). ";
    private static final String ESTIMATION_RECORDED_RESPONSE_2 = "By the way, you can read Klondike Crypto Rush not only on Telegram but on Twitter " +
            "too! If you are interested in joining Klondike on TWITTER, here is the link: https://twitter.com/KlondikeRush";
    private static final String UNKNOWN_RESPONSE = "Sorry, unknown command, try /start\n" +
            "\n If you still have any questions - feel free to ask our admin @kankordio";

    private static final String BOT_STARTED_LOG = "UPDATE LISTENER ACTIVATED";
    private static final String UPDATES_RECEIVED_LOG = "UPDATES RECEIVED, COUNT: {}";
    private static final String UPDATE_LOG = "Update: messageText: \"{}\"; chatId: {}";
    private static final String START_REQUEST_RECEIVED_LOG = "Request is start request";
    private static final String USERNAME_SAVED_LOG = "Username saved: {}";
    private static final String ACTIVE_CHAT_CREATED_LOG = "Active chat created";
    private static final String DIFFERENT_REQUEST_RECEIVED_LOG = "Request is not start request";
    private static final String CHAT_IS_ACTIVE_FOR_REQUEST_LOG = "Active chat entry exists for request";
    private static final String CHAT_IS_NOT_ACTIVE_FOR_REQUEST_LOG = "Active chat entry does not exist for request";
    private static final String CHAT_STATUS_WAITING_FOR_EMAIL_LOG = "Chat status: waiting for email";
    private static final String EMAIL_RECORDED_LOG = "Email recorded";
    private static final String CHAT_STATUS_EMAIL_RECORDED_LOG = "Chat status: email recorded";
    private static final String ESTIMATION_RECORDED_LOG = "Estimation recorded. {} removed from activeChats and cachedData";
    private static final String RESPONSE_SENT_LOG = "RESPONSE SENT: chatId: {}";
    private static final String ALL_STEPS_COMPLETED_RESPONSE_SENT_LOG = "ALL STEPS COMPLETED. ADDITIONAL RESPONSE SENT: chatId: {}";
    private static final String MAP_SIZES = "activeChats: {}, cachedData: {}";

    private TelegramBot bot;
    private Map<Long, ChatStatus> activeChats;
    private Map<Long, CachedData> cachedData;
    private GoogleSheetsUtil googleSheetsUtil;

    BotUpdatesListener(TelegramBot bot) {
        try {
            this.bot = bot;
            activeChats = new HashMap<>();
            cachedData = new HashMap<>();
            googleSheetsUtil = new GoogleSheetsUtil();
            log.info(BOT_STARTED_LOG);
        } catch (Exception e) {
            log.error("Error in BotUpdatesListener constructor: ", e);
        }
    }

    @Override
    public int process(List<Update> updates) {
        try {
            log.info(UPDATES_RECEIVED_LOG, updates.size());

            for (Update update : updates) {
                if (update != null) {
                    Message message = update.message();
                    if (message != null) {
                        String messageText = message.text();
                        Long chatId = message.chat().id();
                        log.info(UPDATE_LOG, messageText, chatId);

                        boolean allStepsCompleted = false;
                        SendMessageWrapper sendMessage = null;
                        if (messageText.equals(START_REQUEST)) {
                            log.info(START_REQUEST_RECEIVED_LOG);
                            sendMessage = new SendMessageWrapper(chatId, WAITING_FOR_EMAIL_RESPONSE);
                            User from = message.from();
                            if (from != null) {
                                String username = from.username();
                                if (username != null) {
                                    cachedData.put(chatId, new CachedData(username));
                                    log.info(USERNAME_SAVED_LOG, username);
                                }
                            }
                            activeChats.put(chatId, ChatStatus.WAITING_FOR_EMAIL);
                            log.debug(ACTIVE_CHAT_CREATED_LOG);
                        } else {
                            log.info(DIFFERENT_REQUEST_RECEIVED_LOG);
                            if (activeChats.containsKey(chatId)) {
                                log.info(CHAT_IS_ACTIVE_FOR_REQUEST_LOG);
                                switch (activeChats.get(chatId)) {
                                    case WAITING_FOR_EMAIL:
                                        log.info(CHAT_STATUS_WAITING_FOR_EMAIL_LOG);
                                        sendMessage = new SendMessageWrapper(chatId, WAITING_FOR_ESTIMATION_RESPONSE);
                                        cachedData.get(chatId).email = messageText;
                                        activeChats.put(chatId, ChatStatus.WAITING_FOR_ESTIMATION);
                                        log.info(EMAIL_RECORDED_LOG);
                                        break;
                                    case WAITING_FOR_ESTIMATION:
                                        log.info(CHAT_STATUS_EMAIL_RECORDED_LOG);
                                        sendMessage = new SendMessageWrapper(chatId, ESTIMATION_RECORDED_RESPONSE_1);
                                        CachedData userInput = cachedData.get(chatId);
                                        googleSheetsUtil.saveUserInput(userInput.username, userInput.email, messageText);
                                        activeChats.remove(chatId);
                                        cachedData.remove(chatId);
                                        allStepsCompleted = true;
                                        log.info(ESTIMATION_RECORDED_LOG, chatId);
                                        break;
                                }
                            } else {
                                log.info(CHAT_IS_NOT_ACTIVE_FOR_REQUEST_LOG);
                                sendMessage = new SendMessageWrapper(chatId, UNKNOWN_RESPONSE);
                            }
                        }

                        bot.execute(sendMessage.sendMessage);
                        log.info(RESPONSE_SENT_LOG, sendMessage.chatId);
                        if (allStepsCompleted) {
                            bot.execute(new SendMessage(chatId, ESTIMATION_RECORDED_RESPONSE_2).parseMode(ParseMode.HTML));
                            log.info(ALL_STEPS_COMPLETED_RESPONSE_SENT_LOG, chatId);
                        }
                        log.info(MAP_SIZES, activeChats.size(), cachedData.size());
                    }
                }
            }

            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        } catch (Exception e) {
            log.error("Error in process() method: ", e);
            return UpdatesListener.CONFIRMED_UPDATES_NONE;
        }
    }

    enum ChatStatus {
        WAITING_FOR_EMAIL,
        WAITING_FOR_ESTIMATION
    }

    private static class CachedData {
        private String username;
        private String email;

        private CachedData(String username) {
            this.username = username;
        }
    }

    private static class SendMessageWrapper {
        private SendMessage sendMessage;
        private Long chatId;

        private SendMessageWrapper(Long chatId, String text) {
            sendMessage = new SendMessage(chatId, text).parseMode(ParseMode.HTML).disableWebPagePreview(true);
            this.chatId = chatId;
        }
    }
}
