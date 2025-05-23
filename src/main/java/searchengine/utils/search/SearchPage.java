package searchengine.utils.search;

import org.jsoup.Jsoup;
import searchengine.dto.searchResponse.FoundPage;
import searchengine.dto.searchResponse.SearchResponseTrue;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.utils.lemmatization.Lemmatization;
import searchengine.utils.search.SearchPageList.SearchPageOnAllSites;
import searchengine.utils.search.SearchPageList.SearchPageOnSingleSite;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class SearchPage {
    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private IndexRepository indexRepository;
    private LemmaRepository lemmaRepository;
    private static String searchText;
    private String site;
    private List<String> lemmaList;
    private String rareLemma;
    searchengine.dto.SearchPage.SearchPage searchPage = new searchengine.dto.SearchPage.SearchPage();

    private SearchPage(SearchPageBuilding searchPageBuilding) {
        siteRepository = searchPageBuilding.siteRepository;
        pageRepository = searchPageBuilding.pageRepository;
        indexRepository = searchPageBuilding.indexRepository;
        lemmaRepository = searchPageBuilding.lemmaRepository;
        searchText = searchPageBuilding.searchText;
        site = searchPageBuilding.site;
        lemmaList = createLemmasForSearch();
        this.rareLemma = site == null ? searchAllLemma() : searchLemmaInSite();
    }
        
    public SearchResponseTrue search(int offset, int limit) {
        SearchResponseTrue searchRequest = new SearchResponseTrue();
        if (lemmaList.isEmpty()) return searchRequest;
        searchPage.setIndexRepository(indexRepository);
        searchPage.setLemmaList(lemmaList);
        searchPage.setRareLemma(rareLemma);
        Map<Integer, Double> foundPagesIdList = site == null ? new SearchPageOnAllSites(searchPage).searchPagesIdList()
                                              : new SearchPageOnSingleSite(searchPage).searchPagesIdList();
        searchRequest.setCount(foundPagesIdList.size());
        if (foundPagesIdList.isEmpty()) return searchRequest;
        searchRequest.setData(createFoundPages(foundPagesIdList, offset, limit));

        return searchRequest;
    }

    private List<String> createLemmasForSearch () {
        List<String> lemmaList;
        try {
            lemmaList = new Lemmatization().collectLemmasForSearch(searchText);
        } catch (IOException e) {
            return new ArrayList<>();
        }
        return lemmaList;
    }

    private String searchLemmaInSite () {
        Site siteInBD = siteRepository.findByUrl(site);
        searchPage.setSiteId(siteInBD.getId());
        List<Lemma> lemmaForSearch = new ArrayList<>();
        for (String l : lemmaList) {
            lemmaForSearch.add(lemmaRepository.findByLemmaAndSiteId(l, siteInBD.getId()));
        }
        try {
            lemmaForSearch.sort(Comparator.comparing(Lemma::getFrequency));
        } catch (NullPointerException ex) {
            return null;
        }
        return lemmaForSearch.get(0).getLemma();
    }

    private String searchAllLemma() {
        Map<String, Integer> lemmaForSearch = new HashMap<>();
        for (String l : lemmaList) {
            List<Lemma> allLemmaList = new ArrayList<>(lemmaRepository.findAllByLemma(l));
            try {
                allLemmaList.forEach(lemma -> {
                    if (lemmaForSearch.containsKey(lemma.getLemma())) {
                        lemmaForSearch.put(lemma.getLemma(), lemmaForSearch.get(lemma.getLemma()) + lemma.getFrequency());
                    }
                    else lemmaForSearch.put(lemma.getLemma(), lemma.getFrequency());
                });
            } catch (NullPointerException e) {
                return null;
            }
        }
        if(lemmaForSearch.isEmpty()) return null;
        return lemmaForSearch.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .get()
                .getKey();
    }

    private Map<Integer, Double> createRelevanceAndLimitPageList(Map<Integer, Double> foundPageIdList, int offset, int limit) {
        Map<Integer, Double> rankOnPagesSort = createPageRelevance(foundPageIdList);

        Map<Integer, Double> lookingPagesId = new LinkedHashMap<>();
        int index = 0;
        for (Map.Entry<Integer, Double> entry : rankOnPagesSort.entrySet()) {
            if (index >= offset) lookingPagesId.put(entry.getKey(), entry.getValue());
            index++;
            if(lookingPagesId.size() == limit) break;
        }
        return lookingPagesId;
    }

    private Map<Integer, Double> createPageRelevance (Map<Integer, Double> foundPageIdList) {

        double maxRank = foundPageIdList.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .get()
                .getValue();

        for (Map.Entry<Integer, Double> entry : foundPageIdList.entrySet()) {
            entry.setValue(entry.getValue() / maxRank);
        }
        return foundPageIdList.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));
    }

    private List<FoundPage> createFoundPages (Map<Integer, Double> foundPagesIdList, int offset, int limit) {
        Map<Integer, Double> lookingPagesId = createRelevanceAndLimitPageList(foundPagesIdList, offset, limit);
        List<Page> pageForRequest = pageRepository.findAllByIdIn(lookingPagesId.keySet());

        Lemmatization lemmatization;

        try {
            lemmatization = new Lemmatization();
        } catch (IOException e) {
            lemmatization = null;
        }


        List<FoundPage> foundPages = new ArrayList<>();
        for (Page page : pageForRequest) {
            FoundPage foundPage = new FoundPage();
            Site siteOnPath = siteRepository.findById(page.getSiteId());
            foundPage.setSite(editsTheLink(siteOnPath.getUrl()));
            foundPage.setSiteName(siteOnPath.getName());
            foundPage.setUri(editsThePath(page.getPath()));
            foundPage.setTitle(Jsoup.parse(page.getContent()).title());
            foundPage.setSnippet(lemmatization == null ? "Сниппет не найден." :
                                                         new CreateSnippet().createSnippet(page.getContent(), rareLemma, lemmaList, lemmatization));
            foundPage.setRelevance(lookingPagesId.get(page.getId()));
            foundPages.add(foundPage);
        }
        foundPages.sort(Comparator.comparing(FoundPage::getRelevance).reversed());
        return foundPages;
    }

    private String editsTheLink (String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private String editsThePath (String path) {
        if (path.isEmpty()) path = "/";
        return path.startsWith("/") ? path : path.substring(path.indexOf("/", path.indexOf(".")));
    }

    public static class SearchPageBuilding {
        private String searchText;
        private String site;
        private SiteRepository siteRepository;
        private PageRepository pageRepository;
        private IndexRepository indexRepository;
        private LemmaRepository lemmaRepository;

        public SearchPageBuilding (String searchText, String site) {
            this.searchText = searchText;
            this.site = site;
        }

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
}
