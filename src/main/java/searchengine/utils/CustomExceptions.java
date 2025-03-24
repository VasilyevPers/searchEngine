package searchengine.utils;

import java.io.IOException;

public class CustomExceptions {
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
    public static class LemmatizationConnectException extends IOException {
        public LemmatizationConnectException(String message) {
            super(message);
        }
    }
}
