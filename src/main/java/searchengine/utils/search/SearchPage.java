package searchengine.utils.search;

import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.config.SearchLimit;
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
    @Autowired
    private SearchLimit searchLimit = new SearchLimit();
    private String searchText;
    private String site;
    private int limit = searchLimit.getLimit();
    private List<String> lemmaList;
    private String rareLemma;

    private SearchPage(SearchPageBuilding searchPageBuilding) {
        siteRepository = searchPageBuilding.siteRepository;
        pageRepository = searchPageBuilding.pageRepository;
        indexRepository = searchPageBuilding.indexRepository;
        lemmaRepository = searchPageBuilding.lemmaRepository;
        searchText = searchPageBuilding.searchText;
        site = searchPageBuilding.site;
        lemmaList = createLemmasForSearch();
    }
        
    public SearchRequest search(int offset, int limit) {
        if (this.limit == 0) this.limit = 10;
        SearchRequest searchRequest = new SearchRequest();
        if (lemmaList.isEmpty()) return searchRequest;
        List<Integer> pageList = searchPagesId();
        searchRequest.setCount(pageList.size());
        if (pageList.isEmpty()) return searchRequest;
        searchRequest.setData(createFoundPages(pageList, offset, this.limit));

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

    private Lemma searchLemmaInSite () {
        Site siteInBD = siteRepository.findByUrl(site);
        List<Lemma> lemmaForSearch = new ArrayList<>();
        for (String l : lemmaList) {
            try {
                lemmaForSearch.add(lemmaRepository.findByLemmaAndSiteId(l, siteInBD.getId()));
            }catch (NullPointerException e) {
                return null;
            }
        }
        lemmaForSearch.sort(Comparator.comparing(Lemma::getFrequency));
        return lemmaForSearch.get(0);
    }

    private Lemma searchAllLemma() {
        List<Lemma> lemmaForSearch = new ArrayList<>();
        for (String l : lemmaList) {
            try {
                lemmaForSearch.addAll(lemmaRepository.findAllByLemma(l));
            } catch (NullPointerException e) {
                return null;
            }
        }
        lemmaForSearch.sort(Comparator.comparing(Lemma::getFrequency));
        return lemmaForSearch.get(0);


    }

    private List<Integer> searchPagesId () {
        List<Integer> finalPagesList = new ArrayList<>();
        Lemma rareLemma = (site == null ? searchAllLemma() : searchLemmaInSite());
        if (rareLemma == null)
            return finalPagesList;
        this.rareLemma = rareLemma.getLemma();
        List<Integer> pagesIdList = new ArrayList<>(pageRepository.findPageIdByLemmaId(rareLemma.getId()));
        for (int pageId : pagesIdList) {
            if (isContainLemma(pageId))
                finalPagesList.add(pageId);
        }
        return finalPagesList;
    }

    private boolean isContainLemma (int pageId) {
        boolean existsLemma = true;
        for (String l : lemmaList) {
            if (l.equals(rareLemma)) continue;
            existsLemma &= (indexRepository.findByPageIdAndLemma(pageId, l) != null);
        }
        return  existsLemma;
    }

    private Map<Integer, Double> createRelevanceAndLimitPageList(List<Integer> pageIdList, int offset, int limit) {
        Map<Integer, Double> rankOnPagesSort = createPageRelevance(pageIdList);

        Map<Integer, Double> lookingPagesId = new LinkedHashMap<>();
        int index = 0;
        for (Map.Entry<Integer, Double> entry : rankOnPagesSort.entrySet()) {
            if (index >= offset) lookingPagesId.put(entry.getKey(), entry.getValue());
            index++;
            if(lookingPagesId.size() == limit) break;
        }
        return lookingPagesId;
    }

    private Map<Integer, Double> createPageRelevance (List<Integer> pageIdList) {
        Map<Integer, Double> rankOnPages = new HashMap<>();
        for (Integer i : pageIdList) {
            rankOnPages.put(i, createRank(i));
        }

        Map<Integer, Double> rankOnPagesSort = rankOnPages.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));

        Double maxRank = new ArrayList<>(rankOnPagesSort.values()).get(0);
        for (Map.Entry<Integer, Double> entry : rankOnPagesSort.entrySet()) {
            entry.setValue(entry.getValue() / maxRank);
        }
        return rankOnPagesSort;
    }

    private List<FoundPage> createFoundPages (List<Integer> pageList, int offset, int limit) {
        Map<Integer, Double> lookingPagesId = createRelevanceAndLimitPageList(pageList, offset, limit);
        List<Page> pageForRequest = pageRepository.findAllByIdIn(lookingPagesId.keySet());

        List<FoundPage> foundPages = new ArrayList<>();
        for (Page page : pageForRequest) {
            FoundPage foundPage = new FoundPage();
            Site siteOnPath = siteRepository.findById(page.getSiteId());
            foundPage.setSite(editsTheLink(siteOnPath.getUrl()));
            foundPage.setSiteName(siteOnPath.getName());
            foundPage.setUri(editsThePath(page.getPath()));
            foundPage.setTitle(Jsoup.parse(page.getContent()).title());
            System.out.println("!!!!!!!!!!ПОИСК СНИППЕТА!!!!!!!!");

            long startTime = System.currentTimeMillis();
            foundPage.setSnippet(new CreateSnippet().createSnippet(page.getContent(), rareLemma, lemmaList));
            System.out.println("TIME SEARCH = " + (System.currentTimeMillis() - startTime));

            System.out.println("!!!!!!!!!!СНИППЕТ НАЙДЕН!!!!!!!!!");
            foundPage.setRelevance(lookingPagesId.get(page.getId()));
            foundPages.add(foundPage);
        }
        return foundPages;
    }

    private String editsTheLink (String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private String editsThePath (String path) {
        if (path.isEmpty()) path = "/";
        return path.startsWith("/") ? path : path.substring(path.indexOf("/", path.indexOf(".")));
    }

    private Double createRank (int pageId) {
        Double relevance = 0.0;
        for (String lemma : lemmaList) {
            try {
                relevance += indexRepository.findRankByPageIdAndLemma(pageId, lemma);
            } catch (NullPointerException e) {
                relevance += 0;
            }
        }
        return relevance;
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
