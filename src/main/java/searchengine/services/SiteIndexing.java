package searchengine.services;

import org.springframework.http.ResponseEntity;
import searchengine.dto.statistics.ResponseMainRequest;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

public interface SiteIndexing {
     ResponseMainRequest fullIndexingSite();
     ResponseMainRequest stopIndexing();
     ResponseMainRequest indexPage(String path);

}
