package searchengine.dto.createSnippet;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class IndexPositions {
    private String sentence;
    private String wordSearch;
    private int snippetStart;
    private int snippetFinish;
    private int wordStart;
    private int wordFinish;
}
