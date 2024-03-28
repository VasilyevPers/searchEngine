package searchengine.utils.lemmatization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import searchengine.utils.lemmatization.Lemmatization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class LemmatizationTest {
    Lemmatization lemmatization;
    private String text;

    @BeforeEach
    public void connectLemmatization() {
        try {
            lemmatization = new Lemmatization();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String textForCollectLemmasForIndexingTest() {
        text = "<!doctype html><!--[if lt IE 7]><html lang=\"en-US\" prefix=\"og: http://ogp.me/ns#\" class=\"no-js lt-ie9 lt-ie8 lt-ie7\"><![endif]--><!--[if (IE 7)&!(IEMobile)]><html lang=\"en-US\" prefix=\"og: http://ogp.me/ns#\" class=\"no-js lt-ie9 lt-ie8\"><![endif]--><!--[if...\n" +
                "<!doctype html><!--[if lt IE 7]><html lang=\"en-US\" prefix=\"og: http://ogp.me/ns#\" class=\"no-js lt-ie9 lt-ie8 lt-ie7\"><![endif]--><!--[if (IE 7)&!(IEMobile)]><html lang=\"en-US\" prefix=\"og: http://ogp.me/ns#\" class=\"no-js lt-ie9 lt-ie8\"><![endif]--><!--[if (IE 8)&!(IEMobile)]><html lang=\"en-US\" prefix=\"og: http://ogp.me/ns#\" class=\"no-js lt-ie9\"><![endif]--><!--[if gt IE 8]><!-->\n" +
                " <html lang=\"en-US\" prefix=\"og: http://ogp.me/ns#\" class=\"no-js\">\n" +
                "  <!--<![endif]-->\n" +
                "   <section id=\"interstitials\">\n" +
                "<span style=\"font-family: Roboto-Regular,Helvetica,sans-serif; font-size: 14px; font-weight: 400; color: #000000; text-decoration: none;\">лес, Леса; кот. Kота/   кто.то на, ВышеЛ, чернейший коту/, </span>\n" +

                "    <div class=\"interstitial-container\" data-multiple-trigger=\"newsletter, news, idle-time\">\n" +
                "    </div>\n" +
                "   </section>\n" +
                " </html><!-- end of site. what a ride! -->";
                return text;
    }
    private String textForCollectLemmasForSearchTest() {
        text = "лес, Леса; кот. Kота/   кто.то на, ВышеЛ, чернейший коту/";
        return text;
    }

    @Test
    @DisplayName("Test of counting the number of unique lemmas from the html code")
    public void testCollectLemmasForIndexing () {
        Map<String, Integer> lemmas = new HashMap<>();
        lemmas.put("лес", 2);
        lemmas.put("кот", 2);
        lemmas.put("выйти", 1);
        lemmas.put("черный", 1);

        assertEquals(lemmas, lemmatization.collectLemmasForIndexing(textForCollectLemmasForIndexingTest()));
    }

    @Test
    @DisplayName("Test created unique lemma collection from the text")
    public void givenText_whenCollectLemmasForSearch_whenUniqueLemmasList () {
        List<String> lemmaList = new ArrayList<>();
        lemmaList.add("лес");
        lemmaList.add("кот");
        lemmaList.add("выйти");
        lemmaList.add("черный");

        assertEquals(lemmaList, lemmatization.collectLemmasForSearch(textForCollectLemmasForSearchTest()));
    }

    @Test
    @DisplayName("Test create normal form word with a short word")
    public void givenSHortWord_whenCreateNormalWordForm_thenNull () {
        assertNull(lemmatization.createNormalWordForm("я"));
    }

    @Test
    @DisplayName("Test create normal form word with a word in different languages")
    public void givenWordInDifferentLanguages_whenCreateNormalWordForm_thenNull () {
        assertNull(lemmatization.createNormalWordForm("Kот"));
    }

    @Test
    @DisplayName("Test create normal form word with a word of the speech")
    public void givenSpeech_whenCreateNormalWordForm_thenNull () {
        assertNull(lemmatization.createNormalWordForm("под"));
    }

    @Test
    @DisplayName("Test create normal form word with a normal word")
    public void givenNormalWord_whenCreateNormalWordForm_thenNormalWordForm () {
        assertEquals("лес", lemmatization.createNormalWordForm("леса"));
    }
}
