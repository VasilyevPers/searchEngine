package searchengine.utils.search.SearchPageList;

import searchengine.dto.SearchPage.SearchPage;
import searchengine.model.Index;
import searchengine.repositories.IndexRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchPageOnAllSites {
    private IndexRepository indexRepository;
    private String rareLemma;
    private List<String> lemmaList;

    public SearchPageOnAllSites (SearchPage searchPage) {
        indexRepository = searchPage.getIndexRepository();
        rareLemma = searchPage.getRareLemma();
        lemmaList = searchPage.getLemmaList();
    }

    public Map<Integer, Double> searchPagesIdList () {
        List<Index> indexesFound = new ArrayList<>(indexRepository.findByLemma(rareLemma));
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
        List<Index> indexList = new ArrayList<>(indexRepository.findByLemma(lemma));
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
