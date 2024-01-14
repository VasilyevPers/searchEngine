package searchengine.utils.lemmatization;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import java.io.IOException;
import java.util.*;
public class Lemmatization {
    private final LuceneMorphology rusLuceneMorph;
    private final LuceneMorphology enLuceneMorph;
    private final static String[] PARTICLES_NAMES = new String[]{" СОЮЗ", " ПРЕДЛ", " МЕЖД"," ЧАСТ", "вопр", " МС-П", " ПРЕДК", " МС", "CONJ", "ARTICLE", "PART", "ADVERB"};

    public Lemmatization () throws IOException {
        rusLuceneMorph = new RussianLuceneMorphology();
        enLuceneMorph = new EnglishLuceneMorphology();
    }

    public Map<String, Integer> collectLemmasForIndexing(String htmlCode) {
        String text = parseHTML(htmlCode);
        String[] wordList = listSeparating(text);
        Map<String, Integer> lemmas = new HashMap<>();
        for (String word : wordList) {

            word = removesUnnecessaryCharacters(word);

            String normalWord = createNormalWordForm(word);
            if (normalWord == null) continue;

            if (lemmas.containsKey(normalWord)) lemmas.put(normalWord, lemmas.get(normalWord) + 1);
                else lemmas.put(normalWord, 1);
        }
        return lemmas;
    }

    public List<String> collectLemmasForSearch (String searchText) {
        List<String> wordsForSearch = new ArrayList<>();
        String[] words = listSeparating(searchText);
        for (String word : words) {
            word = removesUnnecessaryCharacters(word);
            String wordForSearch = createNormalWordForm(word);
            if (wordForSearch == null) continue;
            wordsForSearch.add(wordForSearch);
        }
        return wordsForSearch;
    }

    public String createNormalWordForm(String word)  {
        if (word.isBlank() || word.length() < 2) return null;

        word = removesUnnecessaryCharacters(word);

        LuceneMorphology luceneMorphology = languageSelection(word);
        if (luceneMorphology == null) return null;

        List<String> wordBaseForm = luceneMorphology.getMorphInfo(word);
        if (isChecksWordType(wordBaseForm)) return null;

        List<String> normalForm = luceneMorphology.getNormalForms(word);
        if (normalForm.isEmpty()) return null;

        return normalForm.get(0);
    }

    private String removesUnnecessaryCharacters (String word) {
        word = word.toLowerCase(Locale.ROOT);
        word = word.replaceAll("ё", "e");
        word = word.replaceAll("[^а-яa-z]", " ").strip();

        return word;
    }

    private LuceneMorphology languageSelection(String word) {
        int wordLength = word.length();
        if (word.matches("[а-я]" + "{" + wordLength + "}"))
            return rusLuceneMorph;

        if (word.matches("[a-z]" + "{" + wordLength + "}"))
            return enLuceneMorph;

        return null;
    }
    private boolean isChecksWordType(List<String> wordBaseForms) {
        return wordBaseForms.stream().anyMatch(this::hasParticleProperty);
    }
    private boolean hasParticleProperty(String wordBase) {
        for (String property : PARTICLES_NAMES) {
            if (wordBase.contains(property)) {
                return true;
            }
        }
        return false;
    }

    private String[] listSeparating(String text) {
        return text.split("\\s+");
    }

    private String parseHTML (String html) {
        return Jsoup.clean(html, Safelist.none());
    }
}
