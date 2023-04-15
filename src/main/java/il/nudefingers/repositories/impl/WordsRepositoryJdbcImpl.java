package il.nudefingers.repositories.impl;

import il.nudefingers.models.Word;
import il.nudefingers.repositories.WordsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class WordsRepositoryJdbcImpl implements WordsRepository {
    private final JdbcTemplate jdbcTemplate;

    //language=SQL
    private static final String SQL_SELECT_ALL = "select * from vocabulary";
    //language=SQL
    private static final String SQL_SELECT_ALL_WITH_COUNTERS = "SELECT vocabulary.*, " +
            "COALESCE(answers.total, 0) AS total, " +
            "COALESCE(answers.correct, 0) AS correct, " +
            "answers.user_id " +
            "FROM vocabulary " +
            "LEFT OUTER JOIN answers ON vocabulary.id = answers.id_vocabulary " +
            "WHERE answers.user_id = ?";
    //language=SQL
    private static final String SQL_SELECT_ALL_NEW = "SELECT *, " +
            "0 AS total, " +
            "0 AS correct " +
            "FROM vocabulary " +
            "WHERE id NOT IN " +
            "(SELECT DISTINCT id_vocabulary FROM answers WHERE answers.user_id = ?)";

    //language=SQL
    private static final String SQL_INSERT = "insert into vocabulary(word, translation) values (?, ?)";
    //language=SQL
    private static final String SQL_UPDATE_ANSWERS = "update answers set total = ?, correct = ? where id_vocabulary = ? and user_id = ?";
    private static final String SQL_EXISTS_ANSWERS = "SELECT EXISTS(SELECT * FROM answers WHERE id_vocabulary = ? and user_id = ?)";
    //language=SQL
    private static final String SQL_INSERT_INTO_ANSWERS = "insert into answers(id_vocabulary, total, correct, user_id) values (?, ?, ?, ?)";

    private final static RowMapper<Word> wordRowMapper = (row, rowNum) ->
            Word.builder()
                    .id(row.getLong("id"))
                    .meaning(row.getString("word"))
                    .translation(row.getString("translation"))
                    .total(row.getInt("total"))
                    .correct(row.getInt("correct"))
                    .build();

    @Override
    public List<Word> findAll() {
        return jdbcTemplate.query(SQL_SELECT_ALL, wordRowMapper);
    }

    @Override
    public List<Word> findAllWithCounters(long userId) {
        return jdbcTemplate.query(
                SQL_SELECT_ALL_WITH_COUNTERS,
                        new Object[] {userId},
                        wordRowMapper);
    }

    @Override
    public List<Word> findAllNew(long userId) {
        return jdbcTemplate.query(
                SQL_SELECT_ALL_NEW,
                        new Object[] {userId},
                        wordRowMapper);
    }

    @Override
    public void save(Word newWord) {
        jdbcTemplate.update(SQL_INSERT, newWord.getMeaning(), newWord.getTranslation());
    }

    @Override
    public void update(Word word, long userId, Boolean answerCounted) {
        if(jdbcTemplate.queryForObject(SQL_EXISTS_ANSWERS, Boolean.class, word.getId(), userId)) {

            jdbcTemplate.update(
                    SQL_UPDATE_ANSWERS,
                    word.getTotal() + 1,
                    answerCounted ? word.getCorrect() + 1 : word.getCorrect(),
                    word.getId(),
                    userId);
        } else {

            jdbcTemplate.update(
                    SQL_INSERT_INTO_ANSWERS,
                    word.getId(),
                    1,
                    answerCounted ? 1 : 0,
                    userId);
        }
    }
}
