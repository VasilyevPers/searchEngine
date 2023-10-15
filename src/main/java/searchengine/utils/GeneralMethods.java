package searchengine.utils;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;

import java.io.IOException;
import java.util.Map;

public class GeneralMethods {
    protected int pageResponseCode(String absUrlPage) {
        Connection.Response response;
        try {
            response = Jsoup.connect(absUrlPage).execute();
        } catch (IOException e) {
            return 0;
        }
        return response.statusCode();
    }

    protected boolean isAlienLink(String page) {
        return  page.contains("#") ||
                page.contains(".jpg") ||
                page.contains(".pdf") ||
                page.contains(".png") ||
                page.contains("page") ||
                page.contains("banner") ||
                page.contains("?") ||
                page.contains("video");
    }

    protected boolean isBeLongsToTheSite (String siteUrl, String path) {
        int start = siteUrl.indexOf(".") + 1;
        String nameSite = siteUrl.substring(start);
        return path.matches("https?://(w{3}\\.)?" + nameSite + ".+");
    }

    public String linkCorrection (String link) {
        if (link.contains("//www.")) return link;

    int placeOfInsertion = link.indexOf("/", link.indexOf("/") + 1) + 1;
        return link.substring(0, placeOfInsertion) + "www." + link.substring(placeOfInsertion);
    }

    private void createLemmaAndIndex (Page page) throws IOException {
        Map<String, Integer> lemmasOnThePage = new Lemmatization().collectLemmas(page.getContent());

        for (Map.Entry<String, Integer> entry : lemmasOnThePage.entrySet()) {
            Lemma lemma = new Lemma();
            Index index = new Index();
            lemma.setSiteId(page.getSiteId());
            lemma.setLemma(entry.getKey());
            lemma.setFrequency(1);

            index.setPageId(page.getId());
            index.setLemmaId(lemma.getId());
            index.setRank(entry.getValue());
        }
    }
}
