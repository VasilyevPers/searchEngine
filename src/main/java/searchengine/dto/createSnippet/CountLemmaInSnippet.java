package searchengine.dto.createSnippet;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CountLemmaInSnippet {
    private String snippet;
    private int countLemma;
    private int rankLemma;
}
