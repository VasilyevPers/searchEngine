package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.responseRequest.ResponseMainRequest;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.SiteIndexing;
import searchengine.services.StatisticsService;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

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
    public ResponseEntity<ResponseMainRequest> indexPage (@RequestBody String path) {
        return ResponseEntity.ok(siteIndexing.indexPage(URLDecoder.decode(path, StandardCharsets.UTF_8)));
    }

    @GetMapping(value = "/search")
    public ResponseEntity<?> search (String query, String site, int offset, int limit) {
        return ResponseEntity.ok(siteIndexing.search(query, site, offset, limit));
    }
}
