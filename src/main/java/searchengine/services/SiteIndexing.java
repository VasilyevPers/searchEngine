package searchengine.services;
import searchengine.dto.responseRequest.ResponseMainRequest;

public interface SiteIndexing {
     ResponseMainRequest fullIndexingSite();
     ResponseMainRequest stopIndexing();
     ResponseMainRequest indexPage(String path);
     Object search(String searchText, String site);

}
