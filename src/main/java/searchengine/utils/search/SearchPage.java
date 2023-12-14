package searchengine.utils.search;

import org.jsoup.Jsoup;
import searchengine.dto.searchRequest.FoundPage;
import searchengine.dto.searchRequest.SearchRequest;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.utils.lemmatization.Lemmatization;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class SearchPage {
    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private IndexRepository indexRepository;
    private LemmaRepository lemmaRepository;

    private SearchPage(SiteRepository siteRepository, PageRepository pageRepository,
                       IndexRepository indexRepository, LemmaRepository lemmaRepository) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.indexRepository = indexRepository;
        this.lemmaRepository = lemmaRepository;
    }
        
    public SearchRequest search(String searchText, String site, int offset, int limit) {
        SearchRequest searchRequest = new SearchRequest();
        List<String> lemmasForSearch = createLemmas(searchText);
        if (lemmasForSearch.isEmpty()) return searchRequest;
        List<Lemma> lemmaList = searchLemmasInBD(site, lemmasForSearch);
        List<Page> pageList = searchPages(lemmaList, offset, limit);
        searchRequest.setCount(pageList.size());
        if (pageList.isEmpty()) return searchRequest;
        searchRequest.setData(createFoundPages(pageList, lemmaList, searchText));

        return searchRequest;
    }

    private List<String> createLemmas(String searchText) {
        List<String> lemmasForSearch;
        try {
            lemmasForSearch = new ArrayList<>(new Lemmatization().collectLemmasForSearch(searchText));
        } catch (IOException e) {
            lemmasForSearch = new ArrayList<>();
        }
        return lemmasForSearch;
    }

    public List<Lemma> searchLemmasInBD(String site, List<String> lemmasForSearch) {

        List<Lemma> lemmaList = new ArrayList<>();
        if (site == null) {
            for (String lemma : lemmasForSearch) {
                lemmaList.addAll(lemmaRepository.findAllByLemma(lemma));
            }
        } else {
            Site siteInBD = siteRepository.findByUrl(site);
            for (String lemma : lemmasForSearch) {
                Lemma lemma1 = lemmaRepository.findByLemmaAndSiteId(lemma, siteInBD.getId());
                if (lemma1 == null) continue;
                lemmaList.add(lemma1);
            }
        }
        lemmaList.sort(Comparator.comparing(Lemma::getFrequency));
        lemmaList.forEach(lemma -> System.out.println(lemma.getFrequency()));
        return lemmaList;
    }

    private List<Page> searchPages (List<Lemma> lemmaList, int offset, int limit) {
        List<Page> finalPagesList = new ArrayList<>();
        if (lemmaList.isEmpty()) return finalPagesList;
        List<Page> pagesList = new ArrayList<>(pageRepository.findByLemmaId(lemmaList.get(0).getId(), limit, offset));
        for (int i = 1; i < lemmaList.size(); i++) {
            if (lemmaList.get(i).getFrequency() > 3000) continue;
            List<Page> pageList = pageRepository.findAllByLemmaId(lemmaList.get(i).getId());
            for (Page page : pagesList) {
                if (pageList.contains(page)) finalPagesList.add(page);
            }
            pagesList = List.copyOf(finalPagesList);
            finalPagesList.clear();

        }
        return pagesList;
    }

    private List<FoundPage> createFoundPages (List<Page> pageList, List<Lemma> lemmaList, String searchText) {

        List<FoundPage> foundPages = new ArrayList<>();
        for (Page page : pageList) {
            FoundPage foundPage = new FoundPage();
            foundPage.setSite(editsTheLink(page.getSite().getUrl()));
            foundPage.setSiteName(page.getSite().getName());
            foundPage.setUri(editsThePath(page.getPath()));
            foundPage.setTitle(Jsoup.parse(page.getContent()).title());
            foundPage.setSnippet(new CreateSnippet().createSnippet(page.getContent(), searchText));
            foundPage.setRelevance(createRelevance(page.getId(), lemmaList));
            foundPages.add(foundPage);
        }
        Comparator<FoundPage> comparator = Comparator.comparing(FoundPage::getRelevance).reversed();
        foundPages.sort(comparator);
        double maxRelevance = foundPages.get(0).getRelevance();
        for (FoundPage foundPage : foundPages) {
            foundPage.setRelevance(foundPage.getRelevance() / maxRelevance);
        }
        foundPages.sort(comparator);
        return foundPages;
    }
    private String editsTheLink (String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private String editsThePath (String path) {
        if (path.isEmpty()) path = "/";
        return path.startsWith("/") ? path : path.substring(path.indexOf("/", path.indexOf(".")));
    }

    private double createRelevance (int pageId, List<Lemma> lemmaList) {
        int relevance = 0;
        for (Lemma lemma : lemmaList) {
            try {
                relevance += indexRepository.findRankByPageIdAndLemmaId(pageId, lemma.getId());
            } catch (NullPointerException e) {
                relevance += 0;
            }

        }
        return relevance;
    }

    public static class SearchPageBuilding {
        private SiteRepository siteRepository;
        private PageRepository pageRepository;
        private IndexRepository indexRepository;
        private LemmaRepository lemmaRepository;

        public SearchPageBuilding siteRepository(SiteRepository siteRepository) {
            this.siteRepository = siteRepository;
            return this;
        }

        public SearchPageBuilding pageRepository(PageRepository pageRepository) {
            this.pageRepository = pageRepository;
            return this;
        }

        public SearchPageBuilding indexRepository(IndexRepository indexRepository) {
            this.indexRepository = indexRepository;
            return this;
        }

        public SearchPageBuilding lemmaRepository(LemmaRepository lemmaRepository) {
            this.lemmaRepository = lemmaRepository;
            return this;
        }

        public SearchPage searchPage() {
            return new SearchPage(this);
        }
    }
    private SearchPage(SearchPageBuilding searchPageBuilding) {
        siteRepository = searchPageBuilding.siteRepository;
        pageRepository = searchPageBuilding.pageRepository;
        indexRepository = searchPageBuilding.indexRepository;
        lemmaRepository = searchPageBuilding.lemmaRepository;
    }
}
