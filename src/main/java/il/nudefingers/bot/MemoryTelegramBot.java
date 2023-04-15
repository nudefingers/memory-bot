package il.nudefingers.bot;

import il.nudefingers.bot.components.Button;
import il.nudefingers.bot.config.BotConfig;
import il.nudefingers.models.Word;
import il.nudefingers.services.WordsService;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.apache.commons.text.similarity.JaccardSimilarity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static il.nudefingers.bot.components.CommandMenu.HELP_TEXT;
import static il.nudefingers.bot.components.CommandMenu.LIST_OF_COMMANDS;

@Slf4j
@Component
public class MemoryTelegramBot extends TelegramLongPollingBot {
    @Autowired
    private final WordsService wordsService;
    final BotConfig config;
    boolean isAdding = false;
    boolean isStarting = false;
    List<Word> questionnaire;
    int questionNumber = 0;
    Map<Word, Boolean> result = new HashMap<>();

    private final static String BANANA_EMOJI = "\uD83C\uDF4C";
    private final static String STAR_EMOJI = "\ud83c\udf1f";
    private final static String BRICK_EMOJI = "\u26d4";

    public MemoryTelegramBot(BotConfig config,  WordsService wordsService) {
        this.config = config;
        this.wordsService = wordsService;
        try {
            this.execute(new SetMyCommands(LIST_OF_COMMANDS, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e){
            log.error(e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }
    @Override
    public String getBotToken() {
        return config.getToken();
    }
    @Override
    public void onUpdateReceived(@NotNull Update update) {
        long chatId;
        long userId;
        String userName;
        String receivedMessage;

        //если получено сообщение текстом
        if(update.hasMessage()) {
            chatId = update.getMessage().getChatId();
            userId = update.getMessage().getFrom().getId();
            userName = update.getMessage().getFrom().getFirstName();

            if (update.getMessage().hasText()) {
                receivedMessage = update.getMessage().getText();
                botAnswerUtils(receivedMessage, userId, chatId, userName);
            }

            //если нажата одна из кнопок бота
        } else if (update.hasCallbackQuery()) {
            chatId = update.getCallbackQuery().getMessage().getChatId();
            userId = update.getCallbackQuery().getFrom().getId();
            userName = update.getCallbackQuery().getFrom().getFirstName();
            receivedMessage = update.getCallbackQuery().getData();

            botAnswerUtils(receivedMessage, userId, chatId, userName);
        }
    }

    private void botAnswerUtils(String receivedMessage, long userId, long chatId, String userName) {
        switch (receivedMessage) {
            case "/start" -> startBot(userId, chatId, userName);
            case "/help" -> sendHelpText(chatId);
            case "/add" -> startAdding(chatId);
            default -> {
                if (isAdding) {
                    addNewWords(chatId, receivedMessage);
                }
                if (isStarting) {
                    makeTask(userId, chatId, receivedMessage);
                }
            }
        }
    }

    private void startBot(long userId, long chatId, String userName) {
        isStarting = !isStarting;
        isAdding = false;

        SendMessage message = new SendMessage();
        message.setChatId(chatId);

        questionnaire = wordsService.getWords(userId);

        message.setText("Hi, " + userName + "! Lets start!. First question i-i-i-i-is...'\n\n" + questionnaire.get(questionNumber).getMeaning());
        message.setReplyMarkup(Button.inlineMarkup());

        try {
            execute(message);
            log.info("Reply sent");
        } catch (TelegramApiException e){
            log.error(e.getMessage());
        }
    }

    private void sendHelpText(long chatId){
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(HELP_TEXT);

        try {
            execute(message);
            log.info("Reply sent");
        } catch (TelegramApiException e){
            log.error(e.getMessage());
        }
    }

    private void startAdding(long chatId){
        isAdding = !isAdding;
        isStarting = false;

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Внесите новые слова с переводом через тире, экранированное пробелами. Каждое новое слово с новой строки");

        try {
            execute(message);
            log.info("Reply sent");
        } catch (TelegramApiException e){
            log.error(e.getMessage());
        }
    }

    private void makeTask(long userId, long chatId, String text) {
        StringBuilder forAnswer = new StringBuilder();

        JaccardSimilarity similarity = new JaccardSimilarity();
        double similarityValue = similarity.apply(text.toLowerCase(), questionnaire.get(questionNumber).getTranslation().toLowerCase());

        if (similarityValue >= 0.9) {
            result.put(questionnaire.get(questionNumber), true);
            forAnswer
                    .append(STAR_EMOJI)
                    .append("\n\n");

        } else {
            result.put(questionnaire.get(questionNumber), false);
            forAnswer
                    .append(BRICK_EMOJI)
                    .append(" ")
                    .append(questionnaire.get(questionNumber).getTranslation())
                    .append("\n\n");
        }

        questionNumber++;

        if (questionnaire.size() > questionNumber) {
            forAnswer.append(questionnaire.get(questionNumber).getMeaning());
        } else {
            forAnswer
                    .append(BANANA_EMOJI)
                    .append(result.values().stream().filter(x -> x).count())
                    .append(" / ")
                    .append(questionnaire.size())
                    .append(BANANA_EMOJI);

            wordsService.putCounters(result, userId);
            isStarting =! isStarting;
        }

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(forAnswer.toString());

        try {
            execute(message);
            log.info("Reply sent");
        } catch (TelegramApiException e){
            log.error(e.getMessage());
        }
    }

    private void addNewWords(long chatId, String text){
        // механизм загрузки слов в базу
        List<String> vocabulary = Stream.of(text.split("\n"))
                .toList();

        for (String word : vocabulary) {
            if (word.split(" - ").length >= 2) {
                Word newWord = Word.builder()
                        .meaning(word.split(" - ")[0])
                        .translation(word.split(" - ")[1])
                        .build();

                wordsService.putWord(newWord);
            }
        }

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Done!");
        isAdding = !isAdding;

        try {
            execute(message);
            log.info("Reply sent");
        } catch (TelegramApiException e){
            log.error(e.getMessage());
        }
    }
}
