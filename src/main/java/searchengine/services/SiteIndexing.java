package searchengine.services;
import searchengine.dto.statistics.ResponseMainRequest;

public interface SiteIndexing {
     ResponseMainRequest fullIndexingSite();
     ResponseMainRequest stopIndexing();
     ResponseMainRequest indexPage(String path);

}
