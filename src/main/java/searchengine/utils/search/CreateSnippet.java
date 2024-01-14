package searchengine.utils.search;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import searchengine.dto.createSnippet.CountLemmaInSnippet;
import searchengine.dto.createSnippet.SearchLemma;
import searchengine.dto.createSnippet.WordPosition;
import searchengine.utils.lemmatization.Lemmatization;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

public class CreateSnippet {
    public String createSnippet (String contentPage,String rareLemma, List<String> lemmaList) {
        String errorCreateSnippet = "сниппет не найден";
        if (contentPage == null) return errorCreateSnippet;
        Lemmatization lemmatization;
        try {
            lemmatization = new Lemmatization();
        } catch (IOException e) {
            return errorCreateSnippet;
        }
        String clearedText = Jsoup.clean(contentPage, Safelist.none());

        Set<String> textElements = createFragmentsList(clearedText, rareLemma, lemmatization);

        MultiHighlightingWords multiHighlightingWords = new MultiHighlightingWords(textElements.toArray(new String[0]), lemmaList, lemmatization);
        new ForkJoinPool().invoke(multiHighlightingWords);
        List<CountLemmaInSnippet> snippetList = multiHighlightingWords.getLemmaCountInTextElement();

        snippetList.sort(Comparator.comparing(CountLemmaInSnippet::getCountLemma)
                .thenComparing(CountLemmaInSnippet::getRankLemma)
                .thenComparing(CountLemmaInSnippet::getSnippet)
                .reversed());
        return snippetList.get(0).getSnippet();
    }

    private Set<String> createFragmentsList(String clearedText, String rareLemma, Lemmatization lemmatization) {

        List<WordPosition> wordPositionList = searchLemmaInText(clearedText);
        List<WordPosition> lemmaPositionList = new ArrayList<>();

        SearchLemma searchLemma = new SearchLemma();
        searchLemma.setLemmaForSearch(rareLemma);
        searchLemma.setWordPositionList(wordPositionList.toArray(new WordPosition[0]));
        searchLemma.setLemmaPositionList(lemmaPositionList);
        searchLemma.setLemmatization(lemmatization);

        new ForkJoinPool().invoke(new MultiSearchLemma(searchLemma));

        return createsTextElements(lemmaPositionList, clearedText);
    }

    protected List<WordPosition> searchLemmaInText (String textForSearch) {
        List<WordPosition> wordPositionList = new ArrayList<>();
        boolean cycleFlag = true;
        int indexSearchPosition = 0;
        while (cycleFlag) {
            String elementText;
            try {
                elementText = textForSearch.substring(indexSearchPosition, textForSearch.indexOf(" ", indexSearchPosition));
            } catch (StringIndexOutOfBoundsException e) {
                elementText = textForSearch.substring(indexSearchPosition);
                cycleFlag = false;
            }

            String word = elementText.replaceAll("ё", "е")
                    .replaceAll("[^а-яa-z]", " ")
                    .strip();
            if (word.length() < 2 || word.contains(" ")) {
                indexSearchPosition = changesStartIndex(indexSearchPosition, elementText);
                continue;
            }
            WordPosition wordPosition = new WordPosition();
            wordPosition.setWordPosition(indexSearchPosition);
            wordPosition.setWord(elementText);
            wordPositionList.add(wordPosition);
            indexSearchPosition = changesStartIndex(indexSearchPosition, elementText);
        }
        return wordPositionList;
    }

    private Set<String> createsTextElements(List<WordPosition> lemmaPositionList, String text) {
        Set<String> textElements = new TreeSet<>();
        for (WordPosition wordPosition : lemmaPositionList) {
            int wordStartPosition = wordPosition.getWordPosition();

            int fragmentStart = wordStartPosition - 50;
            int fragmentFinish = wordStartPosition + 160;

            if (fragmentStart < 0) {
                fragmentFinish += (fragmentStart * -1);
                fragmentStart = 0;
                if (fragmentFinish > text.length() - 1) fragmentFinish = text.length() - 1;
            }

            if (fragmentFinish > text.length() - 1) {
                fragmentStart -= fragmentFinish - (text.length() - 1);
                fragmentFinish = text.length() - 1;
                if (fragmentStart < 0) fragmentStart = 0;
            }

            String firstElementFragment;
            if (fragmentStart == 0) {
                firstElementFragment = text.substring(fragmentStart, wordStartPosition);
            } else {
                firstElementFragment = text.substring(text.indexOf(" ", fragmentStart) + 1, wordStartPosition);
            }

            String secondElementFragment;
            if (fragmentFinish == text.length() - 1) {
                secondElementFragment = text.substring(wordStartPosition);
            } else {
                try {
                    secondElementFragment = text.substring(wordStartPosition, text.indexOf(" ", fragmentFinish));
                } catch (StringIndexOutOfBoundsException e) {
                    secondElementFragment = text.substring(wordStartPosition);
                }
            }
            textElements.add(firstElementFragment + secondElementFragment);
        }
        return textElements;
    }

    private int changesStartIndex (int startIndex, String word) {
        return startIndex + word.length() + 1;
    }
}
