package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.Site;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.sql.Timestamp.valueOf;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    @Autowired
    private final SiteRepository siteRepository;
    @Autowired
    private final PageRepository pageRepository;
    @Autowired
    private final LemmaRepository lemmaRepository;
    private final SiteIndexingImpl siteIndexing = new SiteIndexingImpl();


    @Override
    public StatisticsResponse getStatistics() {
        List<Site> sites = siteRepository.findAll();
        StatisticsData data = new StatisticsData();
        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.size());
        total.setPages((int) pageRepository.count());
        total.setLemmas((int) lemmaRepository.count());
        total.setIndexing(!siteIndexing.isCheckIndexingStatus());

        data.setTotal(total);
        List<DetailedStatisticsItem> detailedList = new ArrayList<>();
        for (Site site : sites) {
            detailedList.add(createSiteDetail(site));
        }
        data.setDetailed(detailedList);

        StatisticsResponse response = new StatisticsResponse();
        response.setResult(true);
        response.setStatistics(data);
        return response;
    }

    private DetailedStatisticsItem createSiteDetail (Site site) {
        DetailedStatisticsItem detailed = new DetailedStatisticsItem();
        detailed.setUrl(site.getUrl());
        detailed.setName(site.getName());
        detailed.setStatus(String.valueOf(site.getStatus()));
        detailed.setStatusTime(createTimestamp(site.getStatusTime()));
        detailed.setError(site.getLastError());
        detailed.setPages(pageRepository.countBySiteId(site.getId()));
        detailed.setLemmas(findCountLemmas(site.getId()));
        return detailed;
    }

    private int findCountLemmas (int siteId) {
        Integer countLemmas = lemmaRepository.countLemmaInSite(siteId);

        return countLemmas == null ? 0 : countLemmas;
    }

    private long createTimestamp (LocalDateTime time) {
        return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
