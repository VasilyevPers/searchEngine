package searchengine.dto.searchResponse;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class FoundPage {
    private String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private double relevance;

    @Override
    public String toString() {
        return "FoundPage" +
                "site = " + site + '\n' +
                "siteName = " + siteName + '\n' +
                "uri = " + uri + '\n' +
                "title = " + title + '\n' +
                "snippet = " + snippet + '\n' +
                "relevance = " + relevance + '\n';
    }
}
