package searchengine.utils.indexing;

import org.jsoup.Jsoup;
import searchengine.config.SiteConfig;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.utils.conection.ConnectionUtils;
import searchengine.utils.lemmatization.CreateLemmaAndIndex;
import searchengine.utils.lemmatization.UpdateLemma;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

public class IndexingPage {
    private CreateLemmaAndIndex createLemmaAndIndex = new CreateLemmaAndIndex();
    private ConnectionUtils connectionUtils = new ConnectionUtils();
    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private IndexRepository indexRepository;
    private LemmaRepository lemmaRepository;

    private IndexingPage(SiteRepository siteRepository, PageRepository pageRepository,
                        IndexRepository indexRepository, LemmaRepository lemmaRepository) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.indexRepository = indexRepository;
        this.lemmaRepository = lemmaRepository;

    }

    public final void indexPage (SiteConfig siteConfig, String path) {
        List<Index> indexForSaving = new ArrayList<>();
        Map<String, Lemma> lemmaList = new HashMap<>();
        path = connectionUtils.correctsTheLink(path);
        Site site = siteRepository.findByUrl(siteConfig.getUrl());
        if (site == null) {
            site = new Site();
            site.setLastError(null);
            site.setStatus(StatusIndexing.INDEXING);
            site.setName(siteConfig.getName());
            site.setUrl(siteConfig.getUrl());
            site.setStatusTime(LocalDateTime.now());
        }
        Page page = searchPageInBD(path);
        try {
            if (page != null) {
                deletesOrUpdatesPageData(page.getId());
            }
            page = createPage(site, path);

            createLemmaAndIndex.createLemmaAndIndex(page);
            createLemmaAndIndex.createListLemmaAndIndex(page, indexForSaving, lemmaList);

            Map<String, Lemma> lemmaForSaving = new UpdateLemma().updateLemma(lemmaRepository, indexForSaving, lemmaList);
            site.setStatus(StatusIndexing.INDEXED);
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
            lemmaRepository.saveAll(lemmaForSaving.values());
            indexRepository.saveAll(indexForSaving);
        } catch (IOException e) {
            System.out.println("страница не доступна");
        }
    }

    private Page searchPageInBD(String path) {
        Page page = pageRepository.findByPath(path);

        if (page == null) {
            path = path.substring(0, path.indexOf("w")) + path.substring(path.indexOf(".") + 1);
            page = pageRepository.findByPath(path);
        }
        if (page == null) {
            path = path.substring(path.indexOf("/", path.indexOf(".")));
            page = pageRepository.findByPath(path);
        }
        return page;
    }

    private Page createPage(Site site, String path) throws IOException {
        Page pageForReindexing = new Page();
        pageForReindexing.setSite(site);
        pageForReindexing.setCode(connectionUtils.requestResponseCode(path));
        pageForReindexing.setContent(Jsoup.connect(path).get().html());
        pageForReindexing.setPath(path);

        return pageForReindexing;
    }

    private void deletesOrUpdatesPageData (int pageId) {
    List<Lemma> lemmaOnThePage = lemmaRepository.findAllByPageId(pageId);
    List<Lemma> lemmaForSave = new ArrayList<>();
    List<Lemma> lemmaForDelete = new ArrayList<>();
    for (Lemma lemma : lemmaOnThePage) {
        if (lemma.getFrequency() > 1) {
            lemma.setFrequency(lemma.getFrequency() - 1);
            lemmaForSave.add(lemma);
        }
        if (lemma.getFrequency() == 1) {
            lemmaForDelete.add(lemma);
        }
    }
    pageRepository.deleteById(pageId);
    lemmaRepository.saveAll(lemmaForSave);
    lemmaRepository.deleteAllInBatch(lemmaForDelete);
    }

    public static class IndexingPageBuilding {
        private SiteRepository siteRepository;
        private PageRepository pageRepository;
        private IndexRepository indexRepository;
        private LemmaRepository lemmaRepository;

        public IndexingPageBuilding siteRepository(SiteRepository siteRepository) {
            this.siteRepository = siteRepository;
            return this;
        }

        public IndexingPageBuilding pageRepository(PageRepository pageRepository) {
            this.pageRepository = pageRepository;
            return this;
        }

        public IndexingPageBuilding indexRepository(IndexRepository indexRepository) {
            this.indexRepository = indexRepository;
            return this;
        }

        public IndexingPageBuilding lemmaRepository(LemmaRepository lemmaRepository) {
            this.lemmaRepository = lemmaRepository;
            return this;
        }

        public IndexingPage indexingPage() {
            return new IndexingPage(this);
        }
    }
    private IndexingPage(IndexingPageBuilding indexingPageBuilding) {
        siteRepository = indexingPageBuilding.siteRepository;
        pageRepository = indexingPageBuilding.pageRepository;
        indexRepository = indexingPageBuilding.indexRepository;
        lemmaRepository = indexingPageBuilding.lemmaRepository;
    }
}
