package searchengine.utils;

import lombok.Getter;
import org.jsoup.Jsoup;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.services.AllRepositories;

public class IndexingPage {
    private final GeneralMethods GENERAL_METHODS = new GeneralMethods();
    @Getter
    private boolean resultError;
    @Getter
    private boolean codeError;

    public IndexingPage (AllRepositories allRepositories, searchengine.config.Site configSite, String path){
        resultError = indexPage(allRepositories, configSite, path);
    }

    private boolean indexPage (AllRepositories allRepositories, searchengine.config.Site configSite,  String path) {
        if (!GENERAL_METHODS.isBeLongsToTheSite(configSite.getUrl(), path)) return true;
        if (GENERAL_METHODS.pageResponseCode(path) != 200) return codeError = true;

        Page page = allRepositories.getPageRepository().findByPath(path);
        path = path.substring(path.indexOf("/", path.indexOf(".")));

        if (page == null) {
            page = allRepositories.getPageRepository().findByPath(path);
        }

        if (page == null) {
             Site modelSite = allRepositories.getSiteRepository().findByUrl(configSite.getUrl());
            codeError = reindexingPage(allRepositories, modelSite, path);

        } else {
            allRepositories.getPageRepository().delete(page);
            reindexingPage(allRepositories, page.getSite(), path);
        }
        return false;
    }

    private boolean reindexingPage(AllRepositories allRepositories, Site site, String path) {
        Page pageForReindexing = new Page();
        pageForReindexing.setSite(site);
        pageForReindexing.setCode(GENERAL_METHODS.pageResponseCode(site.getUrl() + path));
        pageForReindexing.setContent(Jsoup.parse(site.getUrl() + path).html());
        pageForReindexing.setPath(path);

        allRepositories.getPageRepository().save(pageForReindexing);

        return true;
    }
}
