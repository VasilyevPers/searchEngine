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

    public int requestResponseCode(String absPageUrl) throws PageConnectException {
        Connection.Response response;
        try {
            response = Jsoup.connect(absPageUrl).execute();
        } catch (IOException e) {
            throw new PageConnectException(this.getClass().getName() + " " +
                    this.getClass().getEnclosingMethod().getName() + " " +
                    Exception.class.getName() +
                    "Не удалось получить доступ к странице!");
        }
        int statusCode = response.statusCode();
        if (statusCode != 200) {
            throw new PageConnectException(this.getClass().getName() + " " +
                    this.getClass().getEnclosingMethod().getName() + " " +
                    Exception.class.getName() +
                    "Страница не доступна, код ответа: " + statusCode);
        }
        return statusCode;
    }
    public String createPageContent(String absPageUrl) throws ContentRequestException {
        String content;
        try {
            content = Jsoup.connect(absPageUrl).get().html();
        } catch (IOException ex) {
            throw new ContentRequestException(this.getClass().getName() +
                                        this.getClass().getEnclosingMethod().getName() +
                                        Exception.class.getName() +
                                        "Ошибка при получении HTML данных!");
        }
        return content;
    }

    public static class PageConnectException extends IOException {
        public PageConnectException(String message) {
            super(message);
        }
    }
    public static class ContentRequestException extends IOException {
        public ContentRequestException (String message) {
            super(message);
        }
    }
}
