package searchengine.utils.indexing;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.SiteIndexingImpl;
import searchengine.utils.conection.ConnectionUtils;
import searchengine.utils.lemmatization.CreateLemmaAndIndex;
import searchengine.utils.lemmatization.UpdateLemma;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

public class IndexingSite extends RecursiveAction {
    private ConnectionUtils connectionUtils = new ConnectionUtils();
    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private IndexRepository indexRepository;
    private LemmaRepository lemmaRepository;
    private  Site site;
    private  String linkForIndexing;

    private IndexingSite(SiteRepository siteRepository, PageRepository pageRepository,
                        IndexRepository indexRepository, LemmaRepository lemmaRepository,
                        Site site,
                        String absLinkForIndexing) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.indexRepository = indexRepository;
        this.lemmaRepository = lemmaRepository;
        this.site = site;
        this.linkForIndexing = connectionUtils.correctsTheLink(absLinkForIndexing);
    }

    @Override
    protected void compute() {
        Map<String, Page> elementsOnThePage = new HashMap<>();
        List<String> continuingIndexing = new ArrayList<>();
        List<String> allPathList = pageRepository.findAllPathBySiteId(site.getId());
        try {
            Document urlCode = Jsoup.connect(linkForIndexing).get();
            Thread.sleep(200);
            Elements elements = urlCode.select("a");
            if (SiteIndexingImpl.getStopIndexing().get())
                elements = new Elements();

            for (Element element : elements) {
                if (SiteIndexingImpl.getStopIndexing().get()) break;
                Page pageForSave = createPageForSaving(element, allPathList, elementsOnThePage);
                if (pageForSave == null) continue;
                String absUrl = element.absUrl("href");
                elementsOnThePage.put(absUrl, pageForSave);
            }
            saveBDAndContinuingIndexing(elementsOnThePage, continuingIndexing);
            if (!SiteIndexingImpl.getStopIndexing().get())
                ranRecursionIndexing(continuingIndexing);
        } catch (IOException | InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }

    private Page createPageForSaving (Element element, List<String> allPathList, Map<String, Page> elementsOnThePage) throws IOException {
        String pagePath = element.attr("href");
        String absUrl = element.absUrl("href");
        if (elementsOnThePage.containsKey(pagePath) || allPathList.contains(pagePath)) return null;
        if (!connectionUtils.isCheckAffiliationSite(site.getUrl(), absUrl) || connectionUtils.isRemovesUnnecessaryLinks(pagePath)) return null;
        Page pageForSave = new Page();
        pageForSave.setCode(connectionUtils.requestResponseCode(absUrl));
        if (pageForSave.getCode() != 200) return null;
        pageForSave.setContent(Jsoup.connect(absUrl).get().html());
        pageForSave.setPath(pagePath);
        pageForSave.setSite(site);

        return pageForSave;
    }

    private void saveBDAndContinuingIndexing (Map<String, Page> elementsOnThePage,  List<String> continuingIndexing) {
        List<Index> indexForSaving = new ArrayList<>();
        Map<String, Lemma> lemmaList = new HashMap<>();
        //synchronized (site.getUrl()) {
            List<String> allPathList = pageRepository.findAllPathBySiteId(site.getId());
            for (Map.Entry<String, Page> entry : elementsOnThePage.entrySet()) {
                if (allPathList.contains(entry.getValue().getPath())) continue;
                if (entry.getValue().getPath().length() > 1)
                    continuingIndexing.add(entry.getKey());
                try {
                    new CreateLemmaAndIndex().createLemmaAndIndex(entry.getValue(), indexForSaving, lemmaList);
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
           // }
            Map<String, Lemma> lemmaForSaving = new UpdateLemma().updateLemma(lemmaRepository, indexForSaving, lemmaList);
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
            lemmaRepository.saveAll(lemmaForSaving.values());
            indexRepository.saveAll(indexForSaving);
        }

    }

    private void ranRecursionIndexing (List<String> continuingIndexing) {
        List<IndexingSite> taskBranch = new ArrayList<>();
        for (String entry : continuingIndexing) {

            IndexingSite siteIndexing = new IndexingSite.IndexingSiteBuilding(site, entry)
                    .siteRepository(siteRepository)
                    .pageRepository(pageRepository)
                    .indexRepository(indexRepository)
                    .lemmaRepository(lemmaRepository)
                    .indexingSite();
            siteIndexing.fork();
            taskBranch.add(siteIndexing);

        }
        taskBranch.forEach(ForkJoinTask::join);
    }
    public static class IndexingSiteBuilding {
        private SiteRepository siteRepository;
        private PageRepository pageRepository;
        private IndexRepository indexRepository;
        private  LemmaRepository lemmaRepository;
        private final   Site site;
        private final   String linkForIndexing;

        public IndexingSiteBuilding (Site site, String linkForIndexing) {
            this.site = site;
            this.linkForIndexing = linkForIndexing;
        }
        public IndexingSiteBuilding siteRepository (SiteRepository siteRepository) {
            this.siteRepository = siteRepository;
            return this;
        }
        public IndexingSiteBuilding pageRepository (PageRepository pageRepository) {
            this.pageRepository = pageRepository;
            return this;
        }
        public IndexingSiteBuilding indexRepository(IndexRepository indexRepository) {
            this.indexRepository = indexRepository;
            return this;
        }
        public IndexingSiteBuilding lemmaRepository (LemmaRepository lemmaRepository) {
            this.lemmaRepository = lemmaRepository;
            return this;
        }
        public IndexingSite indexingSite () {
            return new IndexingSite(this);
        }
    }
    private IndexingSite (IndexingSiteBuilding indexingSiteBuilding) {
        siteRepository = indexingSiteBuilding.siteRepository;
        pageRepository = indexingSiteBuilding.pageRepository;
        indexRepository = indexingSiteBuilding.indexRepository;
        lemmaRepository = indexingSiteBuilding.lemmaRepository;
        site = indexingSiteBuilding.site;
        linkForIndexing = indexingSiteBuilding.linkForIndexing;
    }
}

