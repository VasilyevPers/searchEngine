package searchengine.utils.search;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.dto.searchRequest.FoundPage;
import searchengine.dto.searchRequest.LemmaCount;
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

public class AlternativeSearchPage {
    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private IndexRepository indexRepository;
    private LemmaRepository lemmaRepository;

    private AlternativeSearchPage(SiteRepository siteRepository, PageRepository pageRepository,
                                  IndexRepository indexRepository, LemmaRepository lemmaRepository) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.indexRepository = indexRepository;
        this.lemmaRepository = lemmaRepository;
    }

    public SearchRequest search(String searchText, String site) {
        SearchRequest searchRequest = new SearchRequest();
        List<String> lemmasForSearch = createLemmas(searchText);
        if (lemmasForSearch.isEmpty()) return searchRequest;
        List<Lemma> lemmaList = searchLemmasInBD(site, lemmasForSearch);
        Map<Page, LemmaCount> pageList = searchPages(lemmaList);
        searchRequest.setCount(pageList.size());
        searchRequest.setData(createFoundPages(pageList, searchText));

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
        return lemmaList;
    }

    private String editsTheLink (String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private String editsThePath (String path) {
        if (path.isEmpty()) path = "/";
        return path.startsWith("/") ? path : path.substring(path.indexOf("/", path.indexOf(".")));
    }

    private String createSnippet (String contentPage, String searchText) {

        String[] sentences = Jsoup.parse(contentPage).text().split("\\. ");
        List<String> wordSearch;
        try {
            wordSearch = new Lemmatization().createNormalWordTypeList(searchText);
        } catch (IOException e) {
            wordSearch = new ArrayList<>();
        }
        Map<String, Integer> wordCountInSentence = new HashMap<>();
        for (String sentence : sentences ) {
            if (sentence.split(" ").length < wordSearch.size()) continue;
            int wordCount = 0;
            for (String word : wordSearch) {
                if (sentence.contains(word)) wordCount += 1;
            }
            wordCountInSentence.put(sentence + ".", wordCount);
        }
        Map<String, Integer> sortedWordCountInSentence = wordCountInSentence.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue, LinkedHashMap::new));

        return createSnippet(sortedWordCountInSentence.keySet().stream().toList());
    }

    private String createSnippet (List<String> snippetList) {
        String snippet = snippetList.get(0);
        try {
            snippet = snippet.substring(0, snippet.indexOf(" ", 250));
        } catch (StringIndexOutOfBoundsException e) {
            return snippet;
        }
        return snippet;
    }

    private List<FoundPage> createFoundPages (Map<Page, LemmaCount> sortedPageMap, String searchText) {

        sortedPageMap.forEach((k, v) -> System.out.println(v.getLemmaCount() + " " + v.getRank()));

        List<FoundPage> foundPages = new ArrayList<>();
        for (Map.Entry<Page, LemmaCount> page : sortedPageMap.entrySet()) {
            FoundPage foundPage = new FoundPage();
            foundPage.setSite(editsTheLink(page.getKey().getSite().getUrl()));
            foundPage.setSiteName(page.getKey().getSite().getName());
            foundPage.setUri(editsThePath(page.getKey().getPath()));
            foundPage.setTitle(Jsoup.parse(page.getKey().getContent()).title());
            foundPage.setSnippet(createSnippet(page.getKey().getContent(), searchText));
            foundPage.setRelevance(page.getValue().getRank());
            foundPages.add(foundPage);
        }
//        Comparator<FoundPage> comparator = Comparator.comparing(FoundPage::getRelevance).reversed();
//        foundPages.sort(comparator);
//        double maxRelevance = foundPages.get(0).getRelevance();
//        for (FoundPage foundPage : foundPages) {
//            foundPage.setRelevance(foundPage.getRelevance() / maxRelevance);
//        }
//        foundPages.sort(comparator);
        return foundPages;
    }

    private Map<Page, LemmaCount> searchPages (List<Lemma> lemmaList) {
        Map<Page, LemmaCount> pageMap = new HashMap<>();
        for (Lemma lemma : lemmaList) {
            createPageMap(pageMap, pageRepository.findByLemmaIdAlternative(lemma.getId()), lemma);
        }

        return pageMap.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue((Comparator.comparing(LemmaCount::getLemmaCount).thenComparing(LemmaCount::getRank)).reversed()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue, LinkedHashMap::new));
    }

    private void createPageMap(Map<Page, LemmaCount> pageMap, List<Page> pageList, Lemma lemma) {

        for (Page page : pageList) {
            if (pageMap.containsKey(page)) {
                pageMap.get(page).setLemmaCount(pageMap.get(page).getLemmaCount() + 1);
                pageMap.get(page).setRank(pageMap.get(page).getRank() + indexRepository.findRankByPageIdAndLemmaId(page.getId(), lemma.getId()));
            } else {
                LemmaCount lemmaCount = new LemmaCount();
                lemmaCount.setLemmaCount(1);
                lemmaCount.setRank(indexRepository.findRankByPageIdAndLemmaId(page.getId(), lemma.getId()));
                pageMap.put(page, lemmaCount);
            }
        }
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

        public AlternativeSearchPage searchPage() {
            return new AlternativeSearchPage(this);
        }
    }
    private AlternativeSearchPage(SearchPageBuilding searchPageBuilding) {
        siteRepository = searchPageBuilding.siteRepository;
        pageRepository = searchPageBuilding.pageRepository;
        indexRepository = searchPageBuilding.indexRepository;
        lemmaRepository = searchPageBuilding.lemmaRepository;
    }
}
