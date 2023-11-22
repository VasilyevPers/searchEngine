package searchengine.services;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.dto.responseRequest.ResponseMainRequest;
import searchengine.dto.searchRequest.SearchRequest;
import searchengine.model.*;
import searchengine.config.SitesList;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.utils.indexing.ConnectionUtils;
import searchengine.utils.indexing.IndexingPage;
import searchengine.utils.indexing.IndexingSite;
import searchengine.utils.search.AlternativeSearchPage;
import searchengine.utils.search.SearchPage;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class SiteIndexingImpl implements SiteIndexing {
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private IndexRepository indexRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private SitesList allSitesForIndexing = new SitesList();
    private ConnectionUtils connectionUtils = new ConnectionUtils();
    private ExecutorService service;
    private ResponseMainRequest responseRequest;
    @Getter
    private static AtomicBoolean stopIndexing = new AtomicBoolean();
    private List<Future<?>> taskList = new ArrayList<>();

    @Override
    public ResponseMainRequest fullIndexingSite() {
        stopIndexing.set(false);
        responseRequest = new ResponseMainRequest();
        if (!isCheckIndexingStatus()) {
            responseRequest.setError("Индексация уже запущена");
            return responseRequest;
        }
        service = Executors.newCachedThreadPool();
        siteRepository.deleteAll();
        for (SiteConfig siteForIndexing : allSitesForIndexing.getSites()) {
            Site startSite = new Site();
            startSite.setStatusTime(LocalDateTime.now());
            startSite.setName(siteForIndexing.getName());
            startSite.setUrl(connectionUtils.correctsTheLink(siteForIndexing.getUrl()));
            startSite.setStatus(StatusIndexing.INDEXING);
            siteRepository.save(startSite);

            createAndRunTask(startSite, startSite.getUrl());
        }
        responseRequest.setResult(true);
        return responseRequest;
    }
    private void createAndRunTask(Site startSite, String linkForIndexing) {
        Runnable runnableTask = () ->{
            try {
                new ForkJoinPool().invoke(new IndexingSite.IndexingSiteBuilding(startSite, linkForIndexing)
                        .siteRepository(siteRepository)
                        .pageRepository(pageRepository)
                        .indexRepository(indexRepository)
                        .lemmaRepository(lemmaRepository)
                        .indexingSite());
                if (stopIndexing.get()) {
                    String error = "Индексация остановлена пользователем";
                    updatesSiteWithErrors(startSite, error);
                } else updatesSiteWithOk(startSite);
            } catch (Exception error) {
                updatesSiteWithErrors(startSite, error.toString());
                System.out.println(Arrays.toString(error.getStackTrace()));
                throw new RuntimeException();
            }
        };
        Future<?> runTask = service.submit(runnableTask);
        taskList.add(runTask);
    }

    private void updatesSiteWithOk (Site startSite) {
        startSite.setStatus(StatusIndexing.INDEXED);
        startSite.setStatusTime(LocalDateTime.now());
        siteRepository.save(startSite);
    }

    private void updatesSiteWithErrors (Site startSite, String error) {
        startSite.setStatusTime(LocalDateTime.now());
        startSite.setStatus(StatusIndexing.FAILED);
        startSite.setLastError(error);
        siteRepository.save(startSite);
    }

    @Override
    public ResponseMainRequest stopIndexing() {
        responseRequest = new ResponseMainRequest();
        if (taskList.isEmpty() || isCheckIndexingStatus()) {
            responseRequest.setError("Индексация не запущена");
            return responseRequest;
        }
        stopIndexing.set(true);
        responseRequest.setResult(true);
        while (!isCheckIndexingStatus()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
        }
        return responseRequest;
    }
    public boolean isCheckIndexingStatus() {
        boolean allDone = true;
        for (Future<?> task : taskList) {
            allDone &= task.isDone();
        }
        return allDone;
    }

    @Override
    public ResponseMainRequest indexPage (String path)  {
        path = path.substring(path.indexOf("h"));
        responseRequest = new ResponseMainRequest();
        for (SiteConfig site : allSitesForIndexing.getSites()) {
            if (!connectionUtils.isCheckAffiliationSite(site.getUrl(), path)){
                continue;
            }
            new IndexingPage.IndexingPageBuilding().siteRepository(siteRepository)
                    .pageRepository(pageRepository)
                    .indexRepository(indexRepository)
                    .lemmaRepository(lemmaRepository)
                    .indexingPage().indexPage(site, path);
            responseRequest.setResult(true);
            return responseRequest;
        }
        responseRequest.setError("Данная страница находится за пределами сайтов, " +
                "указанных в конфигурационном файле");
        return responseRequest;
    }

    @Override
    public SearchRequest search(String searchText, String site) {

        return new SearchPage.SearchPageBuilding().siteRepository(siteRepository)
                .pageRepository(pageRepository)
                .indexRepository(indexRepository)
                .lemmaRepository(lemmaRepository)
                .searchPage().search(searchText, site);

//        return new AlternativeSearchPage.SearchPageBuilding().siteRepository(siteRepository)
//                .pageRepository(pageRepository)
//                .indexRepository(indexRepository)
//                .lemmaRepository(lemmaRepository)
//                .searchPage().search(searchText, site);
    }
}