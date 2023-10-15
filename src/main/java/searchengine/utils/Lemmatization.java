package searchengine.utils;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.*;
public class Lemmatization {
    private final LuceneMorphology rusLuceneMorph;
    private final LuceneMorphology enLuceneMorph;
    private final static String[] PARTICLES_NAMES = new String[]{"СОЮЗ", "ПРЕДЛ", "МЕЖД", "CONJ", "ARTICLE", "PART", "ADVERB"};

    public Lemmatization () throws IOException {
        rusLuceneMorph = new RussianLuceneMorphology();
        enLuceneMorph = new EnglishLuceneMorphology();
    }

    protected Map<String, Integer> collectLemmas (String htmlCode) {
        String text = parseHTML(htmlCode);
        String[] wordList = listSeparation(text);
        Map<String, Integer> lemmas = new HashMap<>();
        for (String word : wordList) {
            if (word.isBlank()) continue;

            LuceneMorphology luceneMorphology = isLanguageSelection(word);

            List<String> wordBaseForm = luceneMorphology.getMorphInfo(word);
            if (commonWords(wordBaseForm)) continue;

            List<String> normalForm = luceneMorphology.getNormalForms(word);
            if (normalForm.isEmpty()) continue;

            String normalWord = normalForm.get(0);

            if (lemmas.containsKey(normalWord)) lemmas.put(normalWord, lemmas.get(normalWord) + 1);
                else lemmas.put(normalWord, 1);
        }
        return lemmas;
    }

    private LuceneMorphology isLanguageSelection (String word) {
        return word.matches("([а-я])+") ? rusLuceneMorph : enLuceneMorph;
    }
    private boolean commonWords(List<String> wordBaseForms) {
        return wordBaseForms.stream().anyMatch(this::hasParticleProperty);
    }
    private boolean hasParticleProperty(String wordBase) {
        for (String property : PARTICLES_NAMES) {
            if (wordBase.equals(property)) {
                return true;
            }
        }
        return false;
    }

    private String[] listSeparation (String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[^а-яa-z\\s]", " ")
                .trim()
                .split("\\s+");
    }

    private String parseHTML (String html) {
        return Jsoup.parse(html).text();
    }
}
