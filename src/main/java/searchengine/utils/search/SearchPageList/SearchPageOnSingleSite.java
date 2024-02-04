package searchengine.utils.search.SearchPageList;

import searchengine.dto.SearchPage.SearchPage;
import searchengine.model.Index;
import searchengine.repositories.IndexRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchPageOnSingleSite {
    private IndexRepository indexRepository;
    private int siteId;
    private String rareLemma;
    private List<String> lemmaList;

    public SearchPageOnSingleSite (SearchPage searchPage) {
        indexRepository = searchPage.getIndexRepository();
        siteId = searchPage.getSiteId();
        rareLemma = searchPage.getRareLemma();
        lemmaList = searchPage.getLemmaList();
    }

    public Map<Integer, Double> searchPagesIdList () {
        List<Index> indexesFound = new ArrayList<>(indexRepository.findByLemmaAndSiteId(rareLemma, siteId));
        Map<Integer, Double> pagesIdMap = new HashMap<>();
        for (Index i : indexesFound) {
            pagesIdMap.put(i.getPageId(), (double) i.getRank());
        }

        for (String lemma : lemmaList) {
            if (lemma.equals(rareLemma)) continue;
           pagesIdMap = checksTheListsOfIndex(pagesIdMap, lemma);
        }

        return pagesIdMap;
    }
    private Map<Integer, Double> checksTheListsOfIndex (Map<Integer, Double> pagesMap, String lemma) {
        Map<Integer, Double> finalPagesList = new HashMap<>();
        List<Index> indexList = new ArrayList<>(indexRepository.findByLemmaAndSiteId(lemma, siteId));
        for (Map.Entry<Integer, Double> entry : pagesMap.entrySet()) {
            int rank = containsIndex(indexList, entry.getKey());
            if (rank != -1) {
                finalPagesList.put(entry.getKey(), entry.getValue() + rank);
            };
        }
        return finalPagesList;
    }

    private int containsIndex (List<Index> indexList, int pageId) {
        for (Index i : indexList) {
            if (i.getPageId() == pageId) return (int) i.getRank();
        }
        return -1;
    }
}
