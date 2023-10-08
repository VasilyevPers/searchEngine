package searchengine.services;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.ResponseMainRequest;
import searchengine.model.Site;
import searchengine.config.SitesList;
import searchengine.model.Page;
import searchengine.model.StatusIndexing;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class SiteIndexingImpl extends RecursiveAction implements SiteIndexing {
    @Autowired
    private AllRepositories allRepositories;
    @Autowired
    private final SitesList allSitesForIndexing = new SitesList();
    private String linkForIndexing;
    private Site site;
    private ExecutorService service;
    private ResponseMainRequest responseRequest;
    private final static AtomicBoolean STOP_INDEXING = new AtomicBoolean();
    private List<Future<?>> taskList = new ArrayList<>();

    public SiteIndexingImpl (AllRepositories allRepositories, Site site, String linkForIndexing) {
        this.allRepositories = allRepositories;
        this.site = site;
        this.linkForIndexing = linkForIndexingInitialization(linkForIndexing);
    }
    public SiteIndexingImpl () {}

    private String linkForIndexingInitialization(String linkForIndexing) {
        System.out.println("ЗАПРОС ID САЙТА ПО: " + linkForIndexing);
        return linkForIndexing.equals(site.getUrl()) ? linkForIndexing
               : site.getUrl() + linkForIndexing;
    }

    @Override
    protected void compute() {
        Map<String,Page> elementsOnThePage = new HashMap<>();
        Map<String, Page> continuingIndexing = new HashMap<>();
        List<String> allPathList = allRepositories.getPageRepository().findAllPathBySiteId(site.getId());
        try {
            Document urlCode = Jsoup.connect(linkForIndexing).get();
            Thread.sleep(200);
            Elements elements = urlCode.select("a");

            for (Element element : elements) {
                if (STOP_INDEXING.get()) throw new RuntimeException("Индексация остановлена пользователем");
                String pagePath = element.attr("href");
                String absUrl = element.absUrl("href");
                if (elementsOnThePage.containsKey(pagePath) || allPathList.contains(pagePath)) continue;
                if (!isBeLongsToTheSite(site.getUrl(), absUrl) || isAlienLink(pagePath)) continue;
                Page pageForSave = new Page();
                pageForSave.setCode(siteResponseCode(absUrl));
                if (pageForSave.getCode() != 200) continue;

                pageForSave.setContent(Jsoup.connect(absUrl).get().html());
                pageForSave.setPath(pagePath);
                pageForSave.setSiteId(site.getId());
                elementsOnThePage.put(pagePath, pageForSave);
            }
            saveBDAndContinuingIndexing(elementsOnThePage, continuingIndexing);
            if (STOP_INDEXING.get()) throw new RuntimeException("Индексация остановлена пользователем");
            ranRecursionIndexing(continuingIndexing);
        } catch (IOException | InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }

    private boolean isIndexingStatus () {
        boolean allDone = true;
        for (Future<?> task : taskList) {
            allDone &= task.isDone();
        }
        return allDone;
    }

    @Override
    public ResponseMainRequest fullIndexingSite() {
        STOP_INDEXING.set(false);
        responseRequest = new ResponseMainRequest();
        if (!isIndexingStatus()) {
            responseRequest.setError("Индексация уже запущена");
            return responseRequest;
        }
        service = Executors.newCachedThreadPool();
        allRepositories.getSiteRepository().deleteAll();
        for (searchengine.config.Site siteForIndexing : allSitesForIndexing.getSites()) {
            searchengine.model.Site startSite = new searchengine.model.Site();
            startSite.setStatusTime(LocalDateTime.now());
            startSite.setName(siteForIndexing.getName());
            startSite.setUrl(siteForIndexing.getUrl());
            startSite.setStatus(StatusIndexing.INDEXING);
            allRepositories.getSiteRepository().save(startSite);

            createAndRunTasks(allRepositories, startSite, startSite.getUrl());
        }
        responseRequest.setResult(true);
        return responseRequest;
    }

    public ResponseMainRequest stopIndexing() {
        responseRequest = new ResponseMainRequest();
        if (taskList.isEmpty() || isIndexingStatus()) {
            responseRequest.setError("Индексация не запущена");
            return responseRequest;
        }
        service.shutdown();
        STOP_INDEXING.set(true);
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

    public ResponseMainRequest indexPage (String path) {
        responseRequest = new ResponseMainRequest();
        List<String> allSiteUrl = allRepositories.getSiteRepository().findAllUrl();
        for (String siteUrl : allSiteUrl) {
            if (isBeLongsToTheSite(siteUrl, path)) {

                /** Реализовать код индексации указанной страницы!**/

                responseRequest.setResult(true);
                return responseRequest;
            }
        }
        responseRequest.setResult(false);
        responseRequest.setError("Данная страница находится за пределами сайтов," +
                                 "указанных в конфигурационном файле");
        return responseRequest;
    }

    private void createAndRunTasks(AllRepositories allRepositories, Site startSite, String linkForIndexing) {
        Runnable runnableTask = () ->{
            try {
                new ForkJoinPool().invoke(new SiteIndexingImpl(allRepositories, startSite, linkForIndexing));
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

    private int siteResponseCode (String site) {
        Connection.Response response;
        try {
            response = Jsoup.connect(site).execute();
        } catch (IOException e) {
            return 0;
        }
        return response.statusCode();
    }

    private boolean isAlienLink(String page) {
        return  page.contains("#") ||
                page.contains(".jpg") ||
                page.contains(".pdf") ||
                page.contains(".png") ||
                //page.length() < 2 ||
                page.contains("page") ||
                page.contains("banner") ||
                page.contains("?") ||
                page.contains("video");
    }

    private boolean isBeLongsToTheSite (String siteUrl, String path) {
        int start = siteUrl.indexOf(".") + 1;
        String nameSite = siteUrl.substring(start);
        return path.matches("https?://(w{3}\\.)?" + nameSite + ".+");
    }

    private synchronized void saveBDAndContinuingIndexing (Map<String, Page> elementsOnThePage, Map<String, Page> continuingIndexing) {
        List<String> allPathList = allRepositories.getPageRepository().findAllPathBySiteId(site.getId());
        List<Page> pageForSaving = new ArrayList<>();
        for (Map.Entry<String, Page> entry : elementsOnThePage.entrySet()) {
            if (!allPathList.contains(entry.getKey())) {
                site.setStatusTime(LocalDateTime.now());
                entry.getValue().setSite(site);
                pageForSaving.add(entry.getValue());

                if (entry.getValue().getPath().startsWith("/") && entry.getValue().getPath().length() > 1)
                    continuingIndexing.put(entry.getKey(), entry.getValue());
            }
        }
        allRepositories.getSiteRepository().save(site);
        allRepositories.getPageRepository().saveAll(pageForSaving);
    }

    private void ranRecursionIndexing (Map<String, Page> continuingIndexing) {
        List<SiteIndexingImpl> srg = new ArrayList<>();
        for (Map.Entry<String, Page> entry : continuingIndexing.entrySet()) {
            if (entry.getValue().getCode() == 200) {
                SiteIndexingImpl siteIndexing = new SiteIndexingImpl(allRepositories, site, entry.getKey());
                siteIndexing.fork();
                srg.add(siteIndexing);
            }
        }
        srg.forEach(ForkJoinTask::join);
    }
}