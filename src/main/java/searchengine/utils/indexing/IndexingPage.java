package searchengine.utils.indexing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import searchengine.config.SiteConfig;
import searchengine.dto.responseRequest.ResponseMainRequest;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.utils.lemmatization.CreateLemmaAndIndex;
import searchengine.utils.lemmatization.Lemmatization;
import searchengine.utils.lemmatization.UpdateLemma;

import java.time.LocalDateTime;
import java.util.*;

public class IndexingPage {
    private CreateLemmaAndIndex createLemmaAndIndex = new CreateLemmaAndIndex();
    private ConnectionUtils connectionUtils = new ConnectionUtils();
    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private IndexRepository indexRepository;
    private LemmaRepository lemmaRepository;
    public static final Logger pageLog = LoggerFactory.getLogger(IndexingPage.class);

    private IndexingPage(SiteRepository siteRepository, PageRepository pageRepository,
                        IndexRepository indexRepository, LemmaRepository lemmaRepository) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.indexRepository = indexRepository;
        this.lemmaRepository = lemmaRepository;
    }

    public final ResponseMainRequest indexPage (SiteConfig siteConfig, String path) throws ConnectionUtils.PageConnectException,
                                                                                           Lemmatization.LemmatizationConnectException,
                                                                                           ConnectionUtils.ContentRequestException {
        List<Index> indexForSaving = new ArrayList<>();
        Map<String, Lemma> lemmaList = new HashMap<>();
        path = connectionUtils.correctsTheLink(path);
        Site site = siteRepository.findByUrl(siteConfig.getUrl());
        if (site == null) {
            site = new Site();
            site.setLastError(null);
            site.setStatus(Site.StatusIndexing.INDEXING);
            site.setName(siteConfig.getName());
            site.setUrl(siteConfig.getUrl());
            site.setStatusTime(LocalDateTime.now());
        }
        Page page = searchPageInBD(path);

        if (page != null) {
            deletesOrUpdatesPageData(page.getId());
        }
        page = createPage(site, path);
        createLemmaAndIndex.createLemmaAndIndex(page);
        createLemmaAndIndex.createListLemmaAndIndex(page, indexForSaving, lemmaList);

        Map<String, Lemma> lemmaForSaving = new UpdateLemma().updateLemma(lemmaRepository, indexForSaving, lemmaList);
        site.setStatus(Site.StatusIndexing.INDEXED);
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
        lemmaRepository.saveAll(lemmaForSaving.values());
        indexRepository.saveAll(indexForSaving);

        ResponseMainRequest responseRequest = new ResponseMainRequest();
        responseRequest.setResult(true);
        return responseRequest;
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

    private Page createPage(Site site, String path) throws ConnectionUtils.PageConnectException, ConnectionUtils.ContentRequestException {
        Page pageForReindexing = new Page();
        try {
            pageForReindexing.setCode(connectionUtils.requestResponseCode(path));
            pageForReindexing.setContent(connectionUtils.createPageContent(path));
        } catch (ConnectionUtils.PageConnectException ex) {
            throw new ConnectionUtils.PageConnectException(this.getClass().getName() + " " +
                                                           this.getClass().getEnclosingMethod().getName() + " " +
                                                           "Ошибка подключения " +
                                                           ex.getMessage());
        } catch (ConnectionUtils.ContentRequestException ex) {
            throw new ConnectionUtils.ContentRequestException(this.getClass().getName() + " " +
                                                              this.getClass().getEnclosingMethod().getName() + " " +
                                                              "Ошибка при обработке данных " +
                                                              ex.getMessage());
        }
        pageForReindexing.setSite(site);
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
