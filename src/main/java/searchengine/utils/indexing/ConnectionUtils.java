package searchengine.utils.indexing;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;

public class ConnectionUtils {
    public boolean isRemovesUnnecessaryLinks(String page) {
        return  page.contains("#") ||
                page.contains(".jpg") ||
                page.contains(".pdf") ||
                page.contains(".png") ||
                page.contains("page") ||
                page.contains("banner") ||
                page.contains("?") ||
                page.contains("video");
    }

    public final boolean isCheckAffiliationSite(String siteUrl, String path) {
        siteUrl = correctsTheLink(siteUrl);
        int start = siteUrl.indexOf(".") + 1;
        String nameSite = siteUrl.substring(start);
        return path.matches("https?://(w{3}\\.)?" + nameSite + ".+");
    }

    public final String correctsTheLink(String link) {
        if (link.contains("//www.")) return link;

        int placeOfInsertion = link.indexOf("/", link.indexOf("/") + 1) + 1;
        return link.substring(0, placeOfInsertion) + "www." + link.substring(placeOfInsertion);
    }

    public int requestResponseCode(String absUrlPage) {
        Connection.Response response;
        try {
            response = Jsoup.connect(absUrlPage).execute();
        } catch (IOException e) {
            return 0;
        }
        return response.statusCode();
    }
}
