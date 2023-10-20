package searchengine.services;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.dto.statistics.ResponseMainRequest;
import searchengine.model.*;
import searchengine.config.SitesList;
import searchengine.utils.conections.ConnectionUtils;
import searchengine.utils.indexing.IndexingPage;
import searchengine.utils.indexing.IndexingSite;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class SiteIndexingImpl implements SiteIndexing {
    @Autowired
    private AllRepositories allRepositories = new AllRepositories();
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
        if (!isIndexingStatus()) {
            responseRequest.setError("Индексация уже запущена");
            return responseRequest;
        }
        service = Executors.newCachedThreadPool();
        allRepositories.getSiteRepository().deleteAll();
        for (SiteConfig siteForIndexing : allSitesForIndexing.getSites()) {
            searchengine.model.Site startSite = new searchengine.model.Site();
            startSite.setStatusTime(LocalDateTime.now());
            startSite.setName(siteForIndexing.getName());
            startSite.setUrl(connectionUtils.isCorrectsTheLink(siteForIndexing.getUrl()));
            startSite.setStatus(StatusIndexing.INDEXING);
            allRepositories.getSiteRepository().save(startSite);

            createAndRunTasks(allRepositories, startSite, startSite.getUrl());
        }
        responseRequest.setResult(true);
        return responseRequest;
    }
    private void createAndRunTasks(AllRepositories allRepositories, Site startSite, String linkForIndexing) {
        Runnable runnableTask = () ->{
            try {
                new ForkJoinPool().invoke(new IndexingSite(allRepositories, startSite, linkForIndexing));
                startSite.setStatus(StatusIndexing.INDEXED);
                startSite.setStatusTime(LocalDateTime.now());
                allRepositories.getSiteRepository().save(startSite);
            } catch (Exception e) {
                startSite.setStatusTime(LocalDateTime.now());
                startSite.setStatus(StatusIndexing.FAILED);
                startSite.setLastError(e.getMessage());
                allRepositories.getSiteRepository().save(startSite);
            }
        };
        Future<?> runTask = service.submit(runnableTask);
        taskList.add(runTask);
    }

    @Override
    public ResponseMainRequest stopIndexing() {
        responseRequest = new ResponseMainRequest();
        if (taskList.isEmpty() || isIndexingStatus()) {
            responseRequest.setError("Индексация не запущена");
            return responseRequest;
        }
        service.shutdownNow();
        stopIndexing.set(true);
        responseRequest.setResult(true);
        while (!isIndexingStatus()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
        }
        return responseRequest;
    }
    private boolean isIndexingStatus () {
        boolean allDone = true;
        for (Future<?> task : taskList) {
            allDone &= task.isDone();
        }
        return allDone;
    }

    @Override
    public ResponseMainRequest indexPage (String path) {
        responseRequest = new ResponseMainRequest();
        for (SiteConfig site : allSitesForIndexing.getSites()) {
            if (!connectionUtils.isCheckAffiliationSite(site.getUrl(), path)){
                continue;
            }
            new IndexingPage(allRepositories).indexPage(site, path);
            responseRequest.setResult(true);
            return responseRequest;
        }
        responseRequest.setError("Данная страница находится за пределами сайтов, " +
                "указанных в конфигурационном файле");
        return responseRequest;
    }
}