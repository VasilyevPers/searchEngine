package searchengine.services;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
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
import searchengine.utils.search.SearchPage;

import java.io.IOException;
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
        log.info("{}: {}. Подготовка к полной индексации. Представлено {} сайта(ов).",this.getClass().getName(),
                                                                                      StackWalker.getInstance().walk(frames -> frames
                                                                                              .findFirst()
                                                                                              .map(StackWalker.StackFrame::getMethodName)).get(),
                                                                                      allSitesForIndexing.getSites().size());
        stopIndexing.set(false);
        responseRequest = new ResponseMainRequest();
        if (!isCheckIndexingStatus()) {
            responseRequest.setError("Индексация уже запущена");
            log.warn("{}: {}. Ошибка полной индексации, {}", this.getClass().getName(),
                                                             StackWalker.getInstance().walk(frames -> frames
                                                                .findFirst()
                                                                .map(StackWalker.StackFrame::getMethodName)).get(),
                                                             responseRequest.getError());
            return responseRequest;
        }
        siteRepository.deleteAll();
        service = Executors.newCachedThreadPool();
        for (SiteConfig siteForIndexing : allSitesForIndexing.getSites()) {
            Site startSite = new Site();
            startSite.setStatusTime(LocalDateTime.now());
            startSite.setName(siteForIndexing.getName());
            startSite.setUrl(connectionUtils.correctsTheLink(siteForIndexing.getUrl()));

            try {
                connectionUtils.requestResponseCode(startSite.getUrl());
                startSite.setStatus(Site.StatusIndexing.INDEXING);
                createAndRunTask(startSite, startSite.getUrl());
            } catch (ConnectionUtils.PageConnectException | SecurityException ex) {
                updatesSiteWithErrors(startSite, ex.getMessage());
                log.error("{}: {}. {} {} для индексации.",this.getClass().getName(),
                                                          StackWalker.getInstance().walk(frames -> frames
                                                            .findFirst()
                                                            .map(StackWalker.StackFrame::getMethodName)).get(),
                                                          startSite.getLastError(),
                                                          siteForIndexing.getUrl());
                continue;
            }
            siteRepository.save(startSite);
        }
        if (taskList.isEmpty()) {
            responseRequest.setError("Не удалось Запустить полную индексацию, Указанные сайты недоступны!");
            log.warn("{}: {}. {}",this.getClass().getName(),
                                  StackWalker.getInstance().walk(frames -> frames
                                    .findFirst()
                                    .map(StackWalker.StackFrame::getMethodName)).get(),
                                  responseRequest.getError());
            return responseRequest;
        }
        responseRequest.setResult(true);
        log.info("{}: {}. Полная индексация {} сайта(ов) запущена!",this.getClass().getName(),
                                                                    StackWalker.getInstance().walk(frames -> frames
                                                                        .findFirst()
                                                                        .map(StackWalker.StackFrame::getMethodName)).get(),
                                                                    taskList.size());
        return responseRequest;
    }

    private void createAndRunTask(Site startSite, String linkForIndexing) throws SecurityException {
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
                    log.info("{}: {}. {}, {}",this.getClass().getName(),
                                              StackWalker.getInstance().walk(frames -> frames
                                                .findFirst()
                                                .map(StackWalker.StackFrame::getMethodName)).get(),
                                              startSite.getUrl(),
                                              error);
                } else {
                    updatesSiteWithOk(startSite);
                    log.info("{}: {}. {} Индексация успешно завершена.", this.getClass().getName(),
                                                                         StackWalker.getInstance().walk(frames -> frames
                                                                            .findFirst()
                                                                            .map(StackWalker.StackFrame::getMethodName)).get(),
                                                                         startSite.getUrl());
                }
            } catch (SecurityException ex) {
                updatesSiteWithErrors(startSite, ex.getMessage());
                throw new SecurityException(this.getClass().getName() + " " +
                                            StackWalker.getInstance().walk(frames -> frames
                                                .findFirst()
                                                .map(StackWalker.StackFrame::getMethodName)).get() + " " +
                                            Exception.class.getName() +
                                            "Ошибка индексации сайта: " + startSite.getUrl() +
                                            "Ошибка при создании потока: " +
                                            ex.getMessage());
            } catch (RuntimeException ex) {
                pageLog.warn("{} {} {}", this.getClass().getName(),
                                         StackWalker.getInstance().walk(frames -> frames
                                             .findFirst()
                                             .map(StackWalker.StackFrame::getMethodName)).get(),
                                         ex.getMessage());
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
        log.info("{}: {}. Запрос на остановку индексации от пользователя.", this.getClass().getName(),
                                                                            StackWalker.getInstance().walk(frames -> frames
                                                                                .findFirst()
                                                                                .map(StackWalker.StackFrame::getMethodName)).get());
        responseRequest = new ResponseMainRequest();
        if (taskList.isEmpty() || isCheckIndexingStatus()) {
            responseRequest.setError("Индексация не запущена");
            log.warn("{}: {}. Ошибка остановки индексации! {}", this.getClass().getName(),
                                                                StackWalker.getInstance().walk(frames -> frames
                                                                    .findFirst()
                                                                    .map(StackWalker.StackFrame::getMethodName)).get(),
                                                                responseRequest.getError());
            return responseRequest;
        }
        stopIndexing.set(true);
        responseRequest.setResult(true);
        while (!isCheckIndexingStatus()) {
            try {
                sleep(500);
            } catch (InterruptedException e) {
                log.warn("{}: {}. Попытка прерывания потока остановки индексации!", this.getClass().getName(),
                                                                                    StackWalker.getInstance().walk(frames -> frames
                                                                                        .findFirst()
                                                                                        .map(StackWalker.StackFrame::getMethodName)).get());
                Thread.currentThread().interrupt();
            }
        }
        log.info("{}: {}. Индексация успешно остановлена пользователем", this.getClass().getName(),
                                                                         StackWalker.getInstance().walk(frames -> frames
                                                                         .findFirst()
                                                                         .map(StackWalker.StackFrame::getMethodName)).get());
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
        pageLog.info("{}: {}. Получен запрос для индексации отдельной страницы: {}", this.getClass().getName(),
                                                                                     StackWalker.getInstance().walk(frames -> frames
                                                                                        .findFirst()
                                                                                        .map(StackWalker.StackFrame::getMethodName)).get(),
                                                                                     path);
        responseRequest = new ResponseMainRequest();
        for (SiteConfig site : allSitesForIndexing.getSites()) {
            if (!connectionUtils.isCheckAffiliationSite(site.getUrl(), path))
                continue;
            try {
                responseRequest = new IndexingPage.IndexingPageBuilding()
                                                  .siteRepository(siteRepository)
                                                  .pageRepository(pageRepository)
                                                  .indexRepository(indexRepository)
                                                  .lemmaRepository(lemmaRepository)
                                                  .indexingPage().indexPage(site, path);
            } catch (IOException ex) {
                responseRequest.setError(ex.getMessage());
                pageLog.error("{}: {}. Ошибка индексации отдельной страницы: {}. {}: {}", this.getClass().getName(),
                                                                                          StackWalker.getInstance().walk(frames -> frames
                                                                                             .findFirst()
                                                                                             .map(StackWalker.StackFrame::getMethodName)).get(),
                                                                                          path,
                                                                                          Exception.class.getName(),
                                                                                          ex.getMessage());
                return responseRequest;
            }
            responseRequest.setResult(true);
            return responseRequest;
        }
        responseRequest.setError("Страница находится за пределами сайтов!");
        pageLog.info("{}: {}. {}", this.getClass().getName(),
                                   StackWalker.getInstance().walk(frames -> frames
                                      .findFirst()
                                      .map(StackWalker.StackFrame::getMethodName)).get(),
                                   responseRequest.getError());
        return responseRequest;
    }

    @Override
    public SearchRequest search(String searchText, String site, int offset, int limit) {

        return new SearchPage.SearchPageBuilding(searchText, site).siteRepository(siteRepository)
                .pageRepository(pageRepository)
                .indexRepository(indexRepository)
                .lemmaRepository(lemmaRepository)
                .searchPage().search(offset, limit);

    }
}