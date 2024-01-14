package searchengine.dto.createSnippet;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class WordPosition {
    private String word;
    private int wordPosition;
}
