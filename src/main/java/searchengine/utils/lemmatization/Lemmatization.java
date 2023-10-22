package searchengine.utils.lemmatization;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.*;
public class Lemmatization {
    private final LuceneMorphology rusLuceneMorph;
    private final LuceneMorphology enLuceneMorph;
    private final static String[] PARTICLES_NAMES = new String[]{" СОЮЗ", " ПРЕДЛ", " МЕЖД"," ЧАСТ", " МС-П", " ПРЕДК", " ИНФИНИТИВ", " МС", "CONJ", "ARTICLE", "PART", "ADVERB"};

    public Lemmatization () throws IOException {
        rusLuceneMorph = new RussianLuceneMorphology();
        enLuceneMorph = new EnglishLuceneMorphology();
    }

    public final Map<String, Integer> collectLemmas (String htmlCode) {
        String text = parseHTML(htmlCode);
        String[] wordList = listSeparating(text);
        Map<String, Integer> lemmas = new HashMap<>();
        for (String word : wordList) {
            if (word.isBlank() || word.length() < 3) continue;

            LuceneMorphology luceneMorphology = languageSelection(word);
            if (luceneMorphology == null) continue;

            List<String> wordBaseForm = luceneMorphology.getMorphInfo(word);
            if (isChecksWordType(wordBaseForm)) continue;

            List<String> normalForm = luceneMorphology.getNormalForms(word);
            if (normalForm.isEmpty()) continue;

            String normalWord = normalForm.get(0);

            if (lemmas.containsKey(normalWord)) lemmas.put(normalWord, lemmas.get(normalWord) + 1);
                else lemmas.put(normalWord, 1);
        }
        return lemmas;
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
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[^а-яa-z\\s]", " ")
                .trim()
                .split("\\s+");
    }

    private String parseHTML (String html) {
        return Jsoup.parse(html).text();
    }
}
