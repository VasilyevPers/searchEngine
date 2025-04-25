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
import searchengine.utils.lemmatization.CreateLemmaAndIndex;
import searchengine.utils.lemmatization.Lemmatization;
import searchengine.utils.lemmatization.UpdateLemma;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import static searchengine.utils.indexing.IndexingPage.pageLog;

public class IndexingSite extends RecursiveAction {
    private ConnectionUtils connectionUtils = new ConnectionUtils();
    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private IndexRepository indexRepository;
    private LemmaRepository lemmaRepository;
    private Site site;
    private  String linkForIndexing;

    private IndexingSite (IndexingSiteBuilding indexingSiteBuilding) {
        siteRepository = indexingSiteBuilding.siteRepository;
        pageRepository = indexingSiteBuilding.pageRepository;
        indexRepository = indexingSiteBuilding.indexRepository;
        lemmaRepository = indexingSiteBuilding.lemmaRepository;
        site = indexingSiteBuilding.site;
        linkForIndexing = connectionUtils.correctsTheLink(indexingSiteBuilding.linkForIndexing);
    }

    @Override
    protected void compute() throws RuntimeException {
        Map<String, Page> elementsOnThePage = new HashMap<>();
        List<String> continuingIndexing = new ArrayList<>();
        List<String> allPathList = new ArrayList<>();
        if (!SiteIndexingImpl.getStopIndexing().get()) {
            synchronized (site.getUrl()){
                allPathList = pageRepository.findAllPathBySiteId(site.getId());
            }
        }
        Elements elements;
        try {
            Document urlCode = Jsoup.connect(linkForIndexing).get();
            Thread.sleep(100);
            elements = urlCode.select("a");
        } catch (IOException e) {
            throw new RuntimeException(this.getClass().getName() + " " +
                                       StackWalker.getInstance().walk(frames -> frames
                                           .findFirst()
                                           .map(StackWalker.StackFrame::getMethodName)).get() +
                                       "Ошибка индексации страницы: " +
                                       linkForIndexing +
                                       " Не удалось получить доступ к странице для получения HTML данных!");
        } catch (InterruptedException e) {
            throw new RuntimeException( this.getClass().getName() + " " +
                                        StackWalker.getInstance().walk(frames -> frames
                                            .findFirst()
                                            .map(StackWalker.StackFrame::getMethodName)).get() +
                                        " Ошибка индексации страницы: " +
                                        linkForIndexing +
                                        " Поток был прерван другим потоком!");
        }
        if (SiteIndexingImpl.getStopIndexing().get()) {
            elements.clear();
        }
        for (Element element : elements) {
            if (SiteIndexingImpl.getStopIndexing().get())
                break;
            Page pageForSave = createPageForSaving(element, allPathList, elementsOnThePage);
            if (pageForSave == null) continue;
            String absUrl = element.absUrl("href");
            elementsOnThePage.put(absUrl, pageForSave);
        }
        if (!SiteIndexingImpl.getStopIndexing().get()) {
            saveBDAndContinuingIndexing(elementsOnThePage, continuingIndexing);
            ranRecursionIndexing(continuingIndexing);
        }
    }

    private Page createPageForSaving (Element element, List<String> allPathList, Map<String, Page> elementsOnThePage) {
        String pagePath = element.attr("href");
        String absUrl = element.absUrl("href");
        if (elementsOnThePage.containsKey(pagePath) || allPathList.contains(pagePath))
            return null;
        if (!connectionUtils.isCheckAffiliationSite(site.getUrl(), absUrl) ||
             connectionUtils.isRemovesUnnecessaryLinks(pagePath))
            return null;
        Page pageForSave = new Page();
        try {
            pageForSave.setCode(connectionUtils.requestResponseCode(absUrl));
            pageForSave.setContent(connectionUtils.createPageContent(absUrl));
        } catch (ConnectionUtils.PageConnectException |
                 ConnectionUtils.ContentRequestException ex) {
            pageLog.warn("{}: {}. Ошибка индексации страницы: {}. {}", this.getClass().getName(),
                                                                       StackWalker.getInstance().walk(frames -> frames
                                                                           .findFirst()
                                                                           .map(StackWalker.StackFrame::getMethodName)).get(),
                                                                       absUrl,
                                                                       ex.getMessage());
            return null;
        }
        pageForSave.setPath(pagePath);
        pageForSave.setSite(site);

        return pageForSave;
    }

    private void saveBDAndContinuingIndexing (Map<String, Page> elementsOnThePage,  List<String> continuingIndexing) {
        List<Index> indexForSaving = new ArrayList<>();
        Map<String, Lemma> lemmaList = new HashMap<>();

        for (Map.Entry<String, Page> entry : elementsOnThePage.entrySet()) {
            try {
                new CreateLemmaAndIndex().createLemmaAndIndex(entry.getValue());
            } catch (Lemmatization.LemmatizationConnectException ex) {
                pageLog.warn("{} {} Ошибка индексации страницы: {}{}. {}", this.getClass().getName(),
                                                                           StackWalker.getInstance().walk(frames -> frames
                                                                               .findFirst()
                                                                               .map(StackWalker.StackFrame::getMethodName)).get(),
                                                                           entry.getValue().getSite(),
                                                                           entry.getValue().getPath(),
                                                                           ex.getMessage());
            }
        }
        synchronized (site.getUrl()) {
            List<String> allPathList = pageRepository.findAllPathBySiteId(site.getId());
            for (Map.Entry<String, Page> entry : elementsOnThePage.entrySet()) {
                if (allPathList.contains(entry.getValue().getPath()) ||
                    entry.getValue().getIndexList().isEmpty()) continue;
                if (entry.getValue().getPath().length() > 1) continuingIndexing.add(entry.getKey());
                new CreateLemmaAndIndex().createListLemmaAndIndex(entry.getValue(), indexForSaving, lemmaList);
            }
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

            IndexingSite siteIndexing = new IndexingSite.IndexingSiteBuilding(site, connectionUtils.correctsTheLink(entry))
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

}

