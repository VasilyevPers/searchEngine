package searchengine.services;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.ResponseMainRequest;
import searchengine.model.*;
import searchengine.config.SitesList;
import searchengine.utils.GeneralMethods;
import searchengine.utils.IndexingPage;
import searchengine.utils.IndexingSite;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class SiteIndexingImpl implements SiteIndexing {
    @Autowired
    private final AllRepositories ALL_REPOSITORIES = new AllRepositories();
    @Autowired
    private final SitesList ALL_SITES_FOR_INDEXING = new SitesList();
    private ExecutorService service;
    private ResponseMainRequest responseRequest;
    @Getter
    private static AtomicBoolean stopIndexing = new AtomicBoolean();
    private List<Future<?>> taskList = new ArrayList<>();
    public SiteIndexingImpl () {}

    @Override
    public ResponseMainRequest fullIndexingSite() {
        stopIndexing.set(false);
        responseRequest = new ResponseMainRequest();
        if (!isIndexingStatus()) {
            responseRequest.setError("Индексация уже запущена");
            return responseRequest;
        }
        service = Executors.newCachedThreadPool();
        ALL_REPOSITORIES.getSiteRepository().deleteAll();
        for (searchengine.config.Site siteForIndexing : ALL_SITES_FOR_INDEXING.getSites()) {
            searchengine.model.Site startSite = new searchengine.model.Site();
            startSite.setStatusTime(LocalDateTime.now());
            startSite.setName(siteForIndexing.getName());
            startSite.setUrl(new GeneralMethods().linkCorrection(siteForIndexing.getUrl()));
            startSite.setStatus(StatusIndexing.INDEXING);
            ALL_REPOSITORIES.getSiteRepository().save(startSite);

            createAndRunTasks(ALL_REPOSITORIES, startSite, startSite.getUrl());
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
            } catch (RuntimeException e) {
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
        for (searchengine.config.Site site : ALL_SITES_FOR_INDEXING.getSites()) {
            IndexingPage indexingPage = new IndexingPage(ALL_REPOSITORIES, site, path);
            if (indexingPage.isResultError()) continue;
            if (indexingPage.isCodeError()) {
                responseRequest.setError("Данная страница не доступна");
                return responseRequest;
            }
            responseRequest.setResult(true);
            return responseRequest;
        }
        responseRequest.setError("Данная страница находится за пределами сайтов," +
                "указанных в конфигурационном файле");
        return responseRequest;
    }
}