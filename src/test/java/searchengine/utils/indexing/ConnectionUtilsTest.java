package searchengine.utils.indexing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ConnectionUtilsTest {
    private ConnectionUtils connectionUtils = new ConnectionUtils();
    private final String testSite = "http://www.site.ru";
    private final String actualLinkRegex = "https?://www\\..+";

    @Test
    @DisplayName("Test verification link")
    public void givenNormalLink_whenCorrectTheLink_thenNormalLink () {
        String linkForVerification = "https://www.link.ru/path";
        String expectedVerificationLink = connectionUtils.correctsTheLink(linkForVerification);

        assertTrue(expectedVerificationLink.matches(actualLinkRegex));
    }

    @Test
    @DisplayName("Test correction link")
    public void givenInvalidLink_whenCorrectTheLink_thenNormalLink () {
        String linkForCorrection = "http://link.ru/path";
        String expectedCorrectionLink = connectionUtils.correctsTheLink(linkForCorrection);

        assertTrue(expectedCorrectionLink.matches(actualLinkRegex));
    }

    @Test
    @DisplayName("Site membership test")
    public void givenPathOwnedBySite_whenIsCheckAffiliationSite_thenTrue () {
        String testNormalPath = "http://site.ru/name/path/";
        boolean expectedResponseTrue = connectionUtils.isCheckAffiliationSite(testSite, testNormalPath);

        assertTrue(expectedResponseTrue);
    }

    @Test
    @DisplayName("Site membership test")
    public void givenAlienPath_whenIsCheckAffiliationSite_thenFalse () {
        String testAlienPath = "http://newSite.ru/name/path/";
        boolean expectedResponseFalse = connectionUtils.isCheckAffiliationSite(testSite, testAlienPath);

        assertFalse(expectedResponseFalse);
    }

    @Test
    @DisplayName("Test request response code where site is normal")
    public void givenNormalSite_whenRequestResponseCode_thenCode200 () {
        String testSite = "https://skillbox.ru/";
        int numberCode;
        try {
            numberCode = connectionUtils.requestResponseCode(testSite);
        } catch (ConnectionUtils.PageConnectException e) {
            numberCode = 0;
        }

        assertTrue(numberCode >= 200 && numberCode < 300);
    }

    @Test
    @DisplayName("Test request response code where site is unavailable")
    public void givenUnavailableSite_whenRequestResponseCode_thenErrorCode () {
        String testSite = "https://www.skillboxx.ru";
        int numberCode;
        try {
            numberCode = connectionUtils.requestResponseCode(testSite);
        } catch (ConnectionUtils.PageConnectException e) {
            numberCode = 0;
        }
        assertEquals(0,numberCode);
    }

    @Test
    @DisplayName(" ")
    public void givenPathList_whenIsRemovesUnnecessaryLinks_thenTheCorrectedMap () {
        List<String> testPathList = new LinkedList<>();
        createTestPathList(testPathList);
        Map<String, Boolean> theCorrectedMap = new HashMap<>();


        for (String path : testPathList) {
            theCorrectedMap.put(path, connectionUtils.isRemovesUnnecessaryLinks(path));
        }

        assertEquals(createExpectedMap(), theCorrectedMap);
    }
    private void createTestPathList (List<String> testPathList) {
        testPathList.add("https://site.ru/text#");
        testPathList.add("https://site.ru/text.jpg");
        testPathList.add("https://site.ru/text.pdf");
        testPathList.add("https://site.ru/text.png");
        testPathList.add("https://site.ru/textpage");
        testPathList.add("https://site.ru/textbanner");
        testPathList.add("https://site.ru/text?");
        testPathList.add("https://site.ru/textvideo");
        testPathList.add("https://site.ru/text");
    }
    private Map<String, Boolean> createExpectedMap () {
        Map<String, Boolean> expectedMap = new HashMap<>();
        expectedMap.put("https://site.ru/text#", true);
        expectedMap.put("https://site.ru/text.jpg", true);
        expectedMap.put("https://site.ru/text.pdf", true);
        expectedMap.put("https://site.ru/text.png", true);
        expectedMap.put("https://site.ru/textpage", true);
        expectedMap.put("https://site.ru/textbanner", true);
        expectedMap.put("https://site.ru/text?", true);
        expectedMap.put("https://site.ru/textvideo", true);
        expectedMap.put("https://site.ru/text", false);
        return expectedMap;
    }
}
