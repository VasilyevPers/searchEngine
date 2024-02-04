package searchengine.dto.SearchPage;

import lombok.Getter;
import lombok.Setter;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.PageRepository;

import java.util.List;

@Setter
@Getter
public class SearchPage {
    private IndexRepository indexRepository;
    private int siteId;
    private String rareLemma;
    private List<String> lemmaList;
}
