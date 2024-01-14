package searchengine.utils.search;

import searchengine.dto.createSnippet.SearchLemma;
import searchengine.dto.createSnippet.WordPosition;
import searchengine.utils.lemmatization.Lemmatization;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.RecursiveAction;

public class MultiSearchLemma extends RecursiveAction {
    private WordPosition[] wordPositionList;
    private String lemmaForSearch;
    private List<WordPosition> lemmaPositionList;
    private Lemmatization lemmatization;
    public MultiSearchLemma (SearchLemma searchLemma) {
        this.wordPositionList = searchLemma.getWordPositionList();
        this.lemmaForSearch = searchLemma.getLemmaForSearch();
        lemmaPositionList = searchLemma.getLemmaPositionList();
        this.lemmatization = searchLemma.getLemmatization();
    }

    @Override
    protected void compute() {
        if (wordPositionList.length < 100) {
            searchLemma(wordPositionList, lemmaForSearch);
        } else {
            WordPosition[] firstWordPositionList = Arrays.copyOfRange(wordPositionList, 0, wordPositionList.length/2);
            SearchLemma firstSearchLemma = new SearchLemma();
            firstSearchLemma.setLemmaForSearch(lemmaForSearch);
            firstSearchLemma.setLemmaPositionList(lemmaPositionList);
            firstSearchLemma.setWordPositionList(firstWordPositionList);
            firstSearchLemma.setLemmatization(lemmatization);
            MultiSearchLemma firstMultiSearchLemma = new MultiSearchLemma(firstSearchLemma);

            WordPosition[] secondWordPositionList = Arrays.copyOfRange(wordPositionList, wordPositionList.length/2, wordPositionList.length);
            SearchLemma secondSearchLemma = new SearchLemma();
            secondSearchLemma.setLemmaForSearch(lemmaForSearch);
            secondSearchLemma.setLemmaPositionList(lemmaPositionList);
            secondSearchLemma.setWordPositionList(secondWordPositionList);
            secondSearchLemma.setLemmatization(lemmatization);
            MultiSearchLemma secondMultiSearchLemma = new MultiSearchLemma(secondSearchLemma);

            firstMultiSearchLemma.fork();
            secondMultiSearchLemma.fork();

            firstMultiSearchLemma.join();
            secondMultiSearchLemma.join();
        }
    }

    private void searchLemma (WordPosition[] wordPositionList, String lemmaForSearch) {
        for (WordPosition wordPosition : wordPositionList) {

            String lemma = lemmatization.createNormalWordForm(wordPosition.getWord());

            if (lemma == null) {
                continue;
            }
            if (lemma.equals(lemmaForSearch)){
                lemmaPositionList.add(wordPosition);
            }
        }
    }
}
