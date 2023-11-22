package searchengine.dto.searchRequest;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LemmaCount {
    private int lemmaCount;
    private int rank;
}
