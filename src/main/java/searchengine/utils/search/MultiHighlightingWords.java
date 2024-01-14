package searchengine.utils.search;

import searchengine.dto.createSnippet.CountLemmaInSnippet;
import searchengine.dto.createSnippet.SearchLemma;
import searchengine.dto.createSnippet.WordPosition;
import searchengine.utils.lemmatization.Lemmatization;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class MultiHighlightingWords extends RecursiveAction {
    private static List<CountLemmaInSnippet> lemmaCountInTextElement;
    private String[] textElements;
    private List<String> lemmaList;
    private Lemmatization lemmatization;
    public MultiHighlightingWords (String[] textElements, List<String> lemmaList,
                                   Lemmatization lemmatization) {
        lemmaCountInTextElement = new CopyOnWriteArrayList<>();
        this.textElements = textElements;
        this.lemmaList = lemmaList;
        this.lemmatization = lemmatization;
    }
    @Override
    protected void compute() {
        if (textElements.length < 10) {
            highlightsWords(textElements, lemmaList);
        } else {
            String[] firstTextElement = Arrays.copyOfRange(textElements, 0, textElements.length/2);
            MultiHighlightingWords firstMultiHighlightingWords = new MultiHighlightingWords(firstTextElement, lemmaList, lemmatization);

            String[] secondTextElement = Arrays.copyOfRange(textElements, textElements.length/2, textElements.length);
            MultiHighlightingWords secondMultiHighlightingWords = new MultiHighlightingWords(secondTextElement, lemmaList, lemmatization);

            firstMultiHighlightingWords.fork();
            secondMultiHighlightingWords.fork();

            firstMultiHighlightingWords.join();
            secondMultiHighlightingWords.join();
        }
    }

    private void highlightsWords (String[] textElements, List<String> lemmaList) {
        for (String element : textElements) {
            CountLemmaInSnippet countLemmaInSnippet = new CountLemmaInSnippet();
            List<WordPosition> wordPositionList = new CreateSnippet().searchLemmaInText(element);
            List<WordPosition> allLemmaPositionList = new ArrayList<>();
            int countLemma = 0;
            int rankLemma = 0;
            int shiftpositionWord = 0;

            for (String lemma : lemmaList) {
                List<WordPosition> lemmaPosition = new ArrayList<>();
                SearchLemma searchLemma = new SearchLemma();
                searchLemma.setWordPositionList(wordPositionList.toArray(new WordPosition[0]));
                searchLemma.setLemmaForSearch(lemma);
                searchLemma.setLemmaPositionList(lemmaPosition);
                searchLemma.setLemmatization(lemmatization);

                new ForkJoinPool().invoke(new MultiSearchLemma(searchLemma));
                if (lemmaPosition.isEmpty()) continue;
                countLemma ++;
                allLemmaPositionList.addAll(lemmaPosition);
            }
            allLemmaPositionList.sort(Comparator.comparing(WordPosition::getWordPosition));

            for (WordPosition wordPosition : allLemmaPositionList) {
                String word = wordPosition.getWord();
                int position = wordPosition.getWordPosition() + shiftpositionWord;

                String firstElementSnippet = element.substring(0, position);
                String searchWord = "<b>" + word + "</b>";
                String secondElementSnippet = element.substring((position + word.length()));
                element = firstElementSnippet + searchWord + secondElementSnippet;

                shiftpositionWord +=7;
                rankLemma++;
            }
            countLemmaInSnippet.setCountLemma(countLemma);
            countLemmaInSnippet.setRankLemma(rankLemma);
            countLemmaInSnippet.setSnippet(element);
            lemmaCountInTextElement.add(countLemmaInSnippet);
        }
    }

    public List<CountLemmaInSnippet> getLemmaCountInTextElement() {
        return lemmaCountInTextElement;
    }
}
