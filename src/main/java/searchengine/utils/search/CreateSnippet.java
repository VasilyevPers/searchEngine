package searchengine.utils.search;

import org.jsoup.Jsoup;
import searchengine.dto.createSnippet.IndexPositions;
import searchengine.utils.lemmatization.Lemmatization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CreateSnippet {

    public String createSnippet (String contentPage, String searchText) {

        String[] sentences = Jsoup.parse(contentPage).text().split("[.!?;]");
        List<String> wordSearch;
        try {
            wordSearch = new Lemmatization().createNormalWordTypeList(searchText);
        } catch (IOException e) {
            wordSearch = new ArrayList<>();
        }
        StringBuilder snippet = new StringBuilder();
        for (String sentence : sentences ) {
            if (sentence.split(" ").length < wordSearch.size()) continue;

            wordSearch.forEach(word -> {
                String snippetElement = searchSnippets(sentence, word);
                if (snippetElement != null) {
                    snippet.append(snippetElement).append("; ");
                }
            });
            if (snippet.length() > 200) break;
        }

        return String.valueOf(snippet);
    }

    private String searchSnippets (String sentence, String wordSearch) {
        if (!sentence.contains(wordSearch)) return null;

        IndexPositions indexPositions = new IndexPositions();
        indexPositions.setSentence(sentence);
        indexPositions.setWordSearch(wordSearch);
        indexPositions.setWordStart(sentence.indexOf(wordSearch));
        indexPositions.setWordFinish(indexPositions.getWordStart() + wordSearch.length());
        indexPositions.setSnippetStart(indexPositions.getWordStart() - 15);
        indexPositions.setSnippetFinish(indexPositions.getWordFinish() + 35);

        return highlightsSnippets(indexPositions);
    }

    private String highlightsSnippets (IndexPositions indexPositions) {
        String firstElementSnippet;
        try {
            firstElementSnippet = indexPositions.getSentence().substring(indexPositions.getSentence().indexOf(" ", indexPositions.getSnippetStart()),
                                                                         indexPositions.getWordStart());
        } catch (StringIndexOutOfBoundsException e) {
            firstElementSnippet = indexPositions.getSentence().substring(0, indexPositions.getWordStart() + 1);
        }
        String searchWord = "<b>" + indexPositions.getWordSearch() + "</b>";

        String secondElementSnippet;
        try {
            secondElementSnippet = indexPositions.getSentence().substring(indexPositions.getWordFinish(),
                                                                          indexPositions.getSentence().indexOf(" ", indexPositions.getSnippetFinish()));
        } catch (StringIndexOutOfBoundsException e) {
            secondElementSnippet = indexPositions.getSentence().substring(indexPositions.getWordFinish(), indexPositions.getSentence().length() - 1);
        }
        return firstElementSnippet + searchWord + secondElementSnippet;
    }
}
