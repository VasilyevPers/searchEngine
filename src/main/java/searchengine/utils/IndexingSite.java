package searchengine.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.services.AllRepositories;
import searchengine.services.SiteIndexingImpl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

public class IndexingSite extends RecursiveAction {
    GeneralMethods generalMethods = new GeneralMethods();
    private final AllRepositories allRepositories;
    private final Site site;
    private final String linkForIndexing;

    public IndexingSite(AllRepositories allRepositories, Site site, String absLinkForIndexing) {
        this.allRepositories = allRepositories;
        this.site = site;
        this.linkForIndexing = generalMethods.linkCorrection(absLinkForIndexing);
    }

    @Override
    protected void compute() {
        Map<String, Page> elementsOnThePage = new HashMap<>();
        Map<String, Page> continuingIndexing = new HashMap<>();
        List<String> allPathList = allRepositories.getPageRepository().findAllPathBySiteId(site.getId());
        try {
            Document urlCode = Jsoup.connect(linkForIndexing).get();
            Thread.sleep(200);
            Elements elements = urlCode.select("a");

            for (Element element : elements) {
                if (SiteIndexingImpl.getStopIndexing().get()) throw new RuntimeException("Индексация остановлена пользователем");
                String pagePath = element.attr("href");
                String absUrl = element.absUrl("href");
                if (elementsOnThePage.containsKey(pagePath) || allPathList.contains(pagePath)) continue;
                if (!generalMethods.isBeLongsToTheSite(site.getUrl(), absUrl) || generalMethods.isAlienLink(pagePath)) continue;
                Page pageForSave = new Page();
                pageForSave.setCode(generalMethods.pageResponseCode(absUrl));
                if (pageForSave.getCode() != 200) continue;

                pageForSave.setContent(Jsoup.parse(absUrl).html());
                pageForSave.setPath(pagePath);
                elementsOnThePage.put(pagePath, pageForSave);
            }
            saveBDAndContinuingIndexing(elementsOnThePage, continuingIndexing);
            if (SiteIndexingImpl.getStopIndexing().get()) throw new RuntimeException("Индексация остановлена пользователем");
            ranRecursionIndexing(continuingIndexing);
        } catch (IOException | InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }

    private synchronized void saveBDAndContinuingIndexing (Map<String, Page> elementsOnThePage, Map<String, Page> continuingIndexing) {
        List<String> allPathList = allRepositories.getPageRepository().findAllPathBySiteId(site.getId());
        List<Page> pageForSaving = new ArrayList<>();
        for (Map.Entry<String, Page> entry : elementsOnThePage.entrySet()) {
            if (allPathList.contains(entry.getKey())) continue;
            pageForSaving.add(entry.getValue());
            entry.getValue().setSite(site);

            if (entry.getValue().getPath().length() > 1)
                continuingIndexing.put(entry.getKey(), entry.getValue());

        }
        site.setStatusTime(LocalDateTime.now());
        allRepositories.getSiteRepository().save(site);
        allRepositories.getPageRepository().saveAll(pageForSaving);
    }

    private void ranRecursionIndexing (Map<String, Page> continuingIndexing) {
        List<IndexingSite> taskBranch = new ArrayList<>();
        for (Map.Entry<String, Page> entry : continuingIndexing.entrySet()) {
            if (entry.getValue().getCode() == 200) {
                IndexingSite siteIndexing = new IndexingSite(allRepositories, site, site.getUrl() + entry.getKey());
                siteIndexing.fork();
                taskBranch.add(siteIndexing);
            }
        }
        taskBranch.forEach(ForkJoinTask::join);
    }

}
