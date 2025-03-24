package searchengine.utils.indexing;

import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import searchengine.config.SiteConfig;
import searchengine.dto.responseRequest.RequestStatus;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.utils.CustomExceptions;
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
    private RequestStatus requestStatus = new RequestStatus();
    public static final Logger pageLog = LoggerFactory.getLogger(IndexingPage.class);

    private IndexingPage(SiteRepository siteRepository, PageRepository pageRepository,
                        IndexRepository indexRepository, LemmaRepository lemmaRepository) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.indexRepository = indexRepository;
        this.lemmaRepository = lemmaRepository;
    }

    public final RequestStatus indexPage (SiteConfig siteConfig, String path) {
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
        try {
            page = createPage(site, path);
            createLemmaAndIndex.createLemmaAndIndex(page);
        } catch (CustomExceptions.PageConnectException e) {
            requestStatus.setError(e.getMessage() + " Проверьте правильность написания адреса страницы или попробуйте позже.");
            return requestStatus;
        } catch (CustomExceptions.LemmatizationConnectException e) {
            requestStatus.setError(e.getMessage() + " Невозможно установить соединение с сервером. Попробуйте позже.");
            pageLog.warn("Ошибка индексации страницы: {} {}", path, e.getMessage());
            return requestStatus;
        } catch (CustomExceptions.ContentRequestException e) {
            requestStatus.setError(e.getMessage() + "Не удалось получить данные с запрашиваемой страницы. Попробуйте позже.");
            return requestStatus;
        }
        createLemmaAndIndex.createListLemmaAndIndex(page, indexForSaving, lemmaList);

        Map<String, Lemma> lemmaForSaving = new UpdateLemma().updateLemma(lemmaRepository, indexForSaving, lemmaList);
        site.setStatus(Site.StatusIndexing.INDEXED);
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
        lemmaRepository.saveAll(lemmaForSaving.values());
        indexRepository.saveAll(indexForSaving);

        requestStatus.setStatus(true);
        return requestStatus;
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

    private Page createPage(Site site, String path) throws CustomExceptions.PageConnectException, CustomExceptions.ContentRequestException {
        int pageResponseCode = connectionUtils.requestResponseCode(path);
        String errorMessageForLog = "Ошибка индексации страницы: ";
        if (pageResponseCode != 200) {
            String errorMessageForException = "Страница не доступна!";
            pageLog.warn("{} {} {} Код ответа: {}", errorMessageForLog, path,errorMessageForException, pageResponseCode);
            throw new CustomExceptions.PageConnectException(errorMessageForException);
        }
        Page pageForReindexing = new Page();
        pageForReindexing.setSite(site);
        pageForReindexing.setCode(pageResponseCode);
        try {
            pageForReindexing.setContent(Jsoup.connect(path).get().html());
        } catch (IOException e) {
            String errorMessageForException = "Ошибка при получении контента!";
            pageLog.warn("{} {} {}", errorMessageForLog, path, errorMessageForException);
            throw new CustomExceptions.ContentRequestException(errorMessageForException);
        }

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
