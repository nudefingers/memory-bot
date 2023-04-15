package il.nudefingers.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Word {

    private Long id;
    private String meaning;
    private String translation;
    private int total;
    private int correct;

}

