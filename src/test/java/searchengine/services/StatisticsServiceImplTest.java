package searchengine.services;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class StatisticsServiceImplTest {
    @LocalServerPort
    private Integer port;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private IndexRepository indexRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    private TestRestTemplate template = new TestRestTemplate();
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14");
    public static MysqlContainer<?> container = new MysqlContainer<>();

    @BeforeAll
    public static void beforeAll () {
        postgres.start();
    }

    @AfterAll
    public static void afterAll () {
        postgres.stop();
    }

    @DynamicPropertySource
    public static void configureProperties (DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeEach
    public void fillingDatabase () {
        Site site = new Site();
        site.setStatus(StatusIndexing.INDEXED);
        site.setUrl("https://www.site.ru");
        site.setName("Site");
        site.setStatusTime(LocalDateTime.now());
        site.setLastError(null);
        siteRepository.save(site);

        List<Page> pageList = createPageList(site);
        List<Lemma> lemmaList = createLemmaList(site);
        List<Index> indexList = createIndexList(pageList, lemmaList);

        lemmaRepository.saveAll(lemmaList);
        indexRepository.saveAll(indexList);
    }

    private List<Page> createPageList (Site site) {
        List<Page> pageList = new ArrayList<>();
        for(int i = 1; i <= 2; i++) {
            Page page = new Page();
            page.setContent("content");
            page.setPath("/path:" + i + "/");
            page.setSite(site);
            page.setCode(200);
        }
        return pageList;
    }

    private List<Lemma> createLemmaList (Site site) {
        List<Lemma> lemmaList = new ArrayList<>();
        int pageNumber = 1;
        int lemmaNumber = 1;
        for (int i = 1; i <= 4; i++) {
            Lemma lemma = new Lemma();
            lemma.setLemma("page: " + pageNumber + " lemma: " + lemmaNumber);
            lemma.setSite(site);
            lemma.setFrequency(1);
            lemmaNumber++;
            if (lemmaNumber == 3) {
                pageNumber++;
                lemmaNumber = 1;
            }
        }
        return lemmaList;
    }

    private List<Index> createIndexList (List<Page> pageList, List<Lemma> lemmaList) {
        List<Index> indexList = new ArrayList<>();
        for(Page page : pageList) {
            for(int i = 1; i <= 2; i++) {
                Index index = new Index();
                index.setPage(page);
                index.setRank(1);
                index.setLemma(lemmaList.get(i - 1));
            }
        }
        return indexList;
    }

    @AfterEach
    public void clearDatabase () {
        siteRepository.deleteAll();
    }

    @Test
    public void getStatisticsTest () {
        ResponseEntity<StatisticsResponse> response = template.getRestTemplate()
                .getForEntity("http://lacalhost:" + port + "/statistics", StatisticsResponse.class);

        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(1, response.getBody().getStatistics().getTotal().getSites());
        assertEquals(2, response.getBody().getStatistics().getTotal().getPages());
        assertEquals(4, response.getBody().getStatistics().getTotal().getLemmas());
    }
}
