package searchengine.services;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.dto.responseRequest.RequestStatus;
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
import searchengine.utils.search.SearchPage;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Thread.sleep;
import static searchengine.utils.indexing.IndexingPage.pageLog;

@Service
@Slf4j
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
        log.info("Подготовка к полной индексации. Представлено {} сайта(ов).",allSitesForIndexing.getSites().size());
        stopIndexing.set(false);
        responseRequest = new ResponseMainRequest();
        if (!isCheckIndexingStatus()) {
            responseRequest.setError("Индексация уже запущена");
            log.warn("Ошибка полной индексации, {}", responseRequest.getError());
            return responseRequest;
        }
        siteRepository.deleteAll();
        log.info("База данный успешно удалена.");
        service = Executors.newCachedThreadPool();
        for (SiteConfig siteForIndexing : allSitesForIndexing.getSites()) {
            Site startSite = new Site();
            startSite.setStatusTime(LocalDateTime.now());
            startSite.setName(siteForIndexing.getName());
            startSite.setUrl(connectionUtils.correctsTheLink(siteForIndexing.getUrl()));
            int statusCode = connectionUtils.requestResponseCode(startSite.getUrl());
            if (statusCode != 200) {
                startSite.setStatus(Site.StatusIndexing.FAILED);
                startSite.setLastError("Не удалось получить доступ к сайту.");
                log.warn("{} {} для индексации. Код ответа: {}",startSite.getLastError(), siteForIndexing.getUrl(), statusCode);
            } else {
                startSite.setStatus(Site.StatusIndexing.INDEXING);
                createAndRunTask(startSite, startSite.getUrl());
            }
            siteRepository.save(startSite);
        }
        if (taskList.isEmpty()) {
            responseRequest.setError("Не удалось Запустить полную индексацию, Указанные сайты недоступны!");
            log.warn("{}", responseRequest.getError());
            return responseRequest;
        }
        responseRequest.setResult(true);
        log.info("Полная индексация {} сайта(ов) запущена!", taskList.size());
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

                log.info("Запущена индексация сайта: {}", startSite.getUrl());
                if (stopIndexing.get()) {
                    String error = "Индексация остановлена пользователем";
                    updatesSiteWithErrors(startSite, error);
                    log.info("{}, {}",startSite.getUrl(), error);
                } else {
                    updatesSiteWithOk(startSite);
                    log.info("{} Индексация успешно завершена.", startSite.getUrl());
                }
            } catch (SecurityException error) {
                updatesSiteWithErrors(startSite, error.getMessage());
                log.error("Ошибка индексации сайта: {} Ошибка создания ForkJoinPool {}", startSite.getUrl(), error.getMessage());
            }
        };
        Future<?> runTask = service.submit(runnableTask);
        taskList.add(runTask);
    }

    private void updatesSiteWithOk (Site startSite) {
        startSite.setStatus(Site.StatusIndexing.INDEXED);
        startSite.setStatusTime(LocalDateTime.now());
        siteRepository.save(startSite);
    }

    private void updatesSiteWithErrors (Site startSite, String error) {
        startSite.setStatusTime(LocalDateTime.now());
        startSite.setStatus(Site.StatusIndexing.FAILED);
        startSite.setLastError(error);
        siteRepository.save(startSite);
    }

    @Override
    public ResponseMainRequest stopIndexing() {
        log.info("Запрос на остановку индексации от пользователя");
        responseRequest = new ResponseMainRequest();
        if (taskList.isEmpty() || isCheckIndexingStatus()) {
            responseRequest.setError("Индексация не запущена");
            log.warn("Ошибка остановки индексации! {}",responseRequest.getError());
            return responseRequest;
        }
        stopIndexing.set(true);
        responseRequest.setResult(true);
        while (!isCheckIndexingStatus()) {
            try {
                sleep(500);
            } catch (InterruptedException e) {
                log.warn("Ошибка остановки индексации! {}", e.getMessage());
            }
        }
        log.info("Индексация успешно остановлена пользователем");
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
        pageLog.info("Получен запрос для индексации отдельной страницы: {}",path);
        responseRequest = new ResponseMainRequest();
        for (SiteConfig site : allSitesForIndexing.getSites()) {
            if (!connectionUtils.isCheckAffiliationSite(site.getUrl(), path))
                continue;

            RequestStatus requestStatus = new IndexingPage.IndexingPageBuilding()
                    .siteRepository(siteRepository)
                    .pageRepository(pageRepository)
                    .indexRepository(indexRepository)
                    .lemmaRepository(lemmaRepository)
                    .indexingPage().indexPage(site, path);

            if (requestStatus.isStatus()) {
                responseRequest.setResult(requestStatus.isStatus());
                pageLog.info("Индексация страницы {} успешно завершена.", path);
            } else {
                responseRequest.setError(requestStatus.getError());
            }
            return responseRequest;
        }
        responseRequest.setError("Данная страница находится за пределами сайтов, " +
                "указанных в конфигурационном файле");
        pageLog.warn("Ошибка индексации страницы: {} {}",path, responseRequest.getError());
        return responseRequest;
    }

    @Override
    public SearchRequest search(String searchText, String site, int offset, int limit) {

        SearchRequest searchRequest = new SearchPage.SearchPageBuilding(searchText, site).siteRepository(siteRepository)
                .pageRepository(pageRepository)
                .indexRepository(indexRepository)
                .lemmaRepository(lemmaRepository)
                .searchPage().search(offset, limit);
        return searchRequest;

    }
}