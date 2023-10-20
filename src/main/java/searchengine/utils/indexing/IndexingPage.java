package searchengine.utils.indexing;

import org.jsoup.Jsoup;
import searchengine.config.SiteConfig;
import searchengine.model.Index;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.StatusIndexing;
import searchengine.services.AllRepositories;
import searchengine.utils.conections.ConnectionUtils;
import searchengine.utils.lemmatization.CreateLemmaAndIndex;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

public class IndexingPage {
    private CreateLemmaAndIndex createLemmaAndIndex = new CreateLemmaAndIndex();
    private ConnectionUtils connectionUtils = new ConnectionUtils();
    private AllRepositories allRepositories;

    public IndexingPage (AllRepositories allRepositories) {
        this.allRepositories = allRepositories;
    }

    public final void indexPage (SiteConfig siteConfig, String path) {
        Map<String, List<Index>> indexList = new HashMap<>();
        path = connectionUtils.isCorrectsTheLink(path);
        Site site = allRepositories.getSiteRepository().findByUrl(siteConfig.getUrl());
        if (site == null) {
            site = new Site();
            site.setLastError(null);
            site.setStatus(StatusIndexing.INDEXING);
            site.setName(siteConfig.getName());
            site.setUrl(siteConfig.getUrl());
            site.setStatusTime(LocalDateTime.now());
        }
        Page page = isSearchPageInBD(path);
        try {
            if (page == null) {
                page = isCreatePage(site, path);
            }
            createLemmaAndIndex.createLemmaAndIndex(page, indexList);
            site.setStatus(StatusIndexing.INDEXED);
            site.setStatusTime(LocalDateTime.now());
            allRepositories.getSiteRepository().save(site);
            allRepositories.getPageRepository().save(page);
            allRepositories.getIndexRepository().saveAll(indexList.get(page.getPath()));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private Page isSearchPageInBD(String path) {
        Page page = allRepositories.getPageRepository().findByPath(path);

        if (page == null) {
            path = path.substring(0, path.indexOf("w")) + path.substring(path.indexOf(".") + 1);
            page = allRepositories.getPageRepository().findByPath(path);
        }
        if (page == null) {
            path = path.substring(path.indexOf("/"), path.indexOf("."));
            page = allRepositories.getPageRepository().findByPath(path);
        }
        return page;
    }

    private Page isCreatePage(Site site, String path) throws IOException {
        Page pageForReindexing = new Page();
        pageForReindexing.setSite(site);
        pageForReindexing.setCode(connectionUtils.isRequestResponseCode(path));
        pageForReindexing.setContent(Jsoup.connect(path).get().html());
        pageForReindexing.setPath(path);

        return pageForReindexing;
    }
}
