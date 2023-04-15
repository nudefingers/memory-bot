package il.nudefingers.bot.components;

import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;

import java.util.List;

public interface CommandMenu {
    List<BotCommand> LIST_OF_COMMANDS = List.of(
            new BotCommand("/start", "start bot"),
            new BotCommand("/help", "bot info"),
            new BotCommand("/add", "new words")
    );

    String HELP_TEXT = "This bot will help to count the number of messages in the chat. " +
            "The following commands are available to you:\n\n" +
            "/start - start the bot\n" +
            "/add - add new words\n" +
            "/help - help menu";
}
