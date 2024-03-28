package searchengine.utils.lemmatization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.utils.lemmatization.CreateLemmaAndIndex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CreateLemmaAndIndexTest {
    private CreateLemmaAndIndex createLemmaAndIndex = new CreateLemmaAndIndex();
    private Page Page = new Page();
    private String htmlCode;
    private Map<String, Lemma> expectedLemmaMap = new HashMap<>();

    private String createHtmlCode () {
        htmlCode = "<!doctype html><!--[if lt IE 7]><html lang=\"en-US\" prefix=\"og: http://ogp.me/ns#\" class=\"no-js lt-ie9 lt-ie8 lt-ie7\"><![endif]--><!--[if (IE 7)&!(IEMobile)]><html lang=\"en-US\" prefix=\"og: http://ogp.me/ns#\" class=\"no-js lt-ie9 lt-ie8\"><![endif]--><!--[if...\n" +
                "<!doctype html><!--[if lt IE 7]><html lang=\"en-US\" prefix=\"og: http://ogp.me/ns#\" class=\"no-js lt-ie9 lt-ie8 lt-ie7\"><![endif]--><!--[if (IE 7)&!(IEMobile)]><html lang=\"en-US\" prefix=\"og: http://ogp.me/ns#\" class=\"no-js lt-ie9 lt-ie8\"><![endif]--><!--[if (IE 8)&!(IEMobile)]><html lang=\"en-US\" prefix=\"og: http://ogp.me/ns#\" class=\"no-js lt-ie9\"><![endif]--><!--[if gt IE 8]><!-->\n" +
                " <html lang=\"en-US\" prefix=\"og: http://ogp.me/ns#\" class=\"no-js\">\n" +
                "  <!--<![endif]-->\n" +
                "   <section id=\"interstitials\">\n" +
                "<span style=\"font-family: Roboto-Regular,Helvetica,sans-serif; font-size: 14px; font-weight: 400; color: #000000; text-decoration: none;\">лес, Леса; кот. Kота/   кто.то на, ВышеЛ, чернейший коту/, </span>\n" +

                "    <div class=\"interstitial-container\" data-multiple-trigger=\"newsletter, news, idle-time\">\n" +
                "    </div>\n" +
                "   </section>\n" +
                " </html><!-- end of site. what a ride! -->";
        return htmlCode;
    }

    @BeforeEach
    public void createPage() {

        Lemma lemma1 = new Lemma();
        lemma1.setLemma("выйти");
        lemma1.setFrequency(1);
        Index index1 = new Index();
        index1.setLemma(lemma1);
        index1.setRank(1);
        index1.setPage(Page);
        Page.getIndexList().add(index1);

        Lemma lemma2 = new Lemma();
        lemma2.setLemma("лес");
        lemma2.setFrequency(1);
        Index index2 = new Index();
        index2.setLemma(lemma2);
        index2.setRank(2);
        index2.setPage(Page);
        Page.getIndexList().add(index2);

        Lemma lemma3 = new Lemma();
        lemma3.setLemma("черный");
        lemma3.setFrequency(1);
        Index index3 = new Index();
        index3.setLemma(lemma3);
        index3.setRank(1);
        index3.setPage(Page);
        Page.getIndexList().add(index3);

        Lemma lemma4 = new Lemma();
        lemma4.setLemma("кот");
        lemma4.setFrequency(1);
        Index index4 = new Index();
        index4.setLemma(lemma4);
        index4.setRank(2);
        index4.setPage(Page);
        Page.getIndexList().add(index4);
    }

    private void createExpectedLemmaMap () {
        Lemma lemma1 = new Lemma();
        lemma1.setLemma("выйти");
        lemma1.setFrequency(1);
        expectedLemmaMap.put(lemma1.getLemma(), lemma1);

        Lemma lemma2 = new Lemma();
        lemma2.setLemma("лес");
        lemma2.setFrequency(1);
        expectedLemmaMap.put(lemma2.getLemma(), lemma2);

        Lemma lemma3 = new Lemma();
        lemma3.setLemma("черный");
        lemma3.setFrequency(1);
        expectedLemmaMap.put(lemma3.getLemma(), lemma3);

        Lemma lemma4 = new Lemma();
        lemma4.setLemma("кот");
        lemma4.setFrequency(2);
        expectedLemmaMap.put(lemma4.getLemma(), lemma4);
    }

    @Test
    @DisplayName("Test create index list in page")
    public void givenPage_whenCreateLemmaAndIndex_thenIndexListInPage () {
        Page actualPage = new Page();
        actualPage.setContent(createHtmlCode());

        try {
            createLemmaAndIndex.createLemmaAndIndex(actualPage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        assertArrayEquals(new List[]{Page.getIndexList()}, new List[]{actualPage.getIndexList()});
    }

    @Test
    @DisplayName("Test of filling in the index list and the lemma map")
    public void givenAbsolutPage_whenCreateListLemmaAndIndex_thenFiledIndexListAndLemmaMap () {
        List<Index> indexList = new ArrayList<>();
        Map<String, Lemma> lemmaMap = new HashMap<>();
        Lemma lemma = new Lemma();
        lemma.setLemma("кот");
        lemma.setFrequency(1);
        lemmaMap.put(lemma.getLemma(), lemma);
        createLemmaAndIndex.createListLemmaAndIndex(Page, indexList, lemmaMap);
        createExpectedLemmaMap();

        assertFalse(indexList.isEmpty());
        assertEquals(expectedLemmaMap, lemmaMap);
    }
}
