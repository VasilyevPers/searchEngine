package searchengine.services;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.*;

public class Lemmatization {

    public static void main(String[] args) throws IOException {
        LuceneMorphology luceneMorph =
                new RussianLuceneMorphology();
        List<String> wordBaseForms =
                luceneMorph.getMorphInfo("и");
        wordBaseForms.forEach(System.out::println);
    }






    private final LuceneMorphology luceneMorph;
    private final static String[] PARTICLES_NAMES = new String[]{"СОЮЗ", "ПРЕДЛ", "МЕЖД"};

    public Lemmatization () throws IOException {
        luceneMorph = new RussianLuceneMorphology();
    }

    public Map<String, Integer> collectLemmas (String absUrl) {
        String text = parseHTML(absUrl);
        String[] wordList = listSeparation(text);
        Map<String, Integer> lemmas = new HashMap<>();
        for (String word : wordList) {
            if (word.isBlank()) continue;

            List<String> wordBaseForm = luceneMorph.getMorphInfo(word);
            if (commonWords(wordBaseForm)) continue;

            List<String> normalForm = luceneMorph.getNormalForms(word);
            if (normalForm.isEmpty()) continue;

            String normalWord = normalForm.get(0);

            if (lemmas.containsKey(normalWord)) lemmas.put(normalWord, lemmas.get(normalWord) + 1);
                else lemmas.put(normalWord, 1);
        }
        return lemmas;
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
                .replaceAll("[^а-я\\s]", " ")
                .trim()
                .split("\\s+");
    }

    private String parseHTML (String path) {
        return Jsoup.parse(path).text();
    }
}
