package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.statistics.ResponseMainRequest;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.SiteIndexing;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final SiteIndexing siteIndexing;

    public ApiController(StatisticsService statisticsService, SiteIndexing siteIndexing) {
        this.statisticsService = statisticsService;
        this.siteIndexing = siteIndexing;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<ResponseMainRequest> startIndexing () {
        return ResponseEntity.ok(siteIndexing.fullIndexingSite());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<ResponseMainRequest> stopIndexing() {
        return ResponseEntity.ok(siteIndexing.stopIndexing());
    }

    @PostMapping(value = "/indexPage")
    public ResponseEntity<ResponseMainRequest> indexPage (@RequestParam String path) {
        return ResponseEntity.ok(siteIndexing.indexPage(path));
    }


}
