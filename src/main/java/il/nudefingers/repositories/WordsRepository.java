package il.nudefingers.repositories;

import il.nudefingers.models.Word;

import java.util.List;

public interface WordsRepository {
    List<Word> findAll();

    List<Word> findAllWithCounters(long userId);

    List<Word> findAllNew(long userId);

    void save(Word newWord);

    void update(Word word, long userId, Boolean answerCounted);
}
