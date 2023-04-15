package il.nudefingers.services;

import il.nudefingers.models.Word;

import java.util.List;
import java.util.Map;

public interface WordsService {
    List<Word> getWords(long userId);

    void putWord(Word word);

    void putCounters(Map<Word, Boolean> result, long userId);

}
