package bot;

import com.pengrad.telegrambot.TelegramBot;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {

    public static final String BOT_TOKEN = ""; // API key

    public static final String BOT_CREATED_LOG = "BOT CREATED";

    public static void main(String[] args) {
        try {
            TelegramBot bot = new TelegramBot(BOT_TOKEN);
            log.info(BOT_CREATED_LOG);
            bot.setUpdatesListener(new BotUpdatesListener(bot));
        } catch (Exception e) {
            log.error("Error in main method: ", e);
        }
    }
}
