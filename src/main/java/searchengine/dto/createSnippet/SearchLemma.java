package searchengine.dto.createSnippet;

import lombok.Getter;
import lombok.Setter;
import searchengine.utils.lemmatization.Lemmatization;

import java.util.List;

@Setter
@Getter
public class SearchLemma {
    private WordPosition[] wordPositionList;
    private String lemmaForSearch;
    private List<WordPosition> lemmaPositionList;
    private Lemmatization lemmatization;
}
