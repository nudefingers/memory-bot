package il.nudefingers.services.impl;

import il.nudefingers.models.Word;
import il.nudefingers.repositories.WordsRepository;
import il.nudefingers.services.WordsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@PropertySource("application.properties")
public class WordsServiceImpl implements WordsService {
    private final WordsRepository wordsRepository;
    private List<Word> questionnaire;
    @Value("${questionnaire.all}") int allWordsCount;
    @Value("${questionnaire.practiced}") int practicedWordsCount;
    @Value("${questionnaire.learned}") int learnedWordsCount;
    private final static Double LEARNING_CRITERION = 0.5;

    @Override
    public List<Word> getWords(long userId) {
        questionnaire = new ArrayList<>(allWordsCount);

        System.out.println(wordsRepository
                .findAllWithCounters(userId).toString());

        List<Word> learnedWords = wordsRepository
                .findAllWithCounters(userId)
                .stream()
                .filter(w -> w.getCorrect() / w.getTotal() >= LEARNING_CRITERION)
                .collect(Collectors.toList());

        addToQuestionnaire(learnedWords, learnedWordsCount);


        List<Word> practicedWords = wordsRepository
                .findAllWithCounters(userId)
                .stream()
                .filter(w -> w.getCorrect() / w.getTotal() < LEARNING_CRITERION)
                .collect(Collectors.toList());

        addToQuestionnaire(practicedWords, practicedWordsCount);

        questionnaire.addAll(wordsRepository
                .findAllNew(userId)
                .stream()
                .limit(allWordsCount - questionnaire.size())
                .toList());

        return questionnaire;
    }

    @Override
    public void putWord(Word word) {
        wordsRepository.save(word);
    }

    @Override
    public void putCounters(Map<Word, Boolean> result, long userId) {
        for (Map.Entry<Word, Boolean> entry : result.entrySet()) {
            wordsRepository.update(entry.getKey(), userId, entry.getValue());
        }
    }

    private void addToQuestionnaire(List<Word> words, int count) {
        questionnaire.addAll(words
                .stream()
                .sorted(Comparator.comparingDouble(e -> Math.random()))
                .limit(Math.min(count, words.size()))
                .toList());
    }
}
