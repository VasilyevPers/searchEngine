package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;

import java.util.Collection;
import java.util.List;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {
    @Query(value = "select path from pages where site_id=?",
           nativeQuery = true)
    List<String> findAllPathBySiteId (int siteId);

    Page findByPath (String path);

    int countBySiteId (int siteId);

    @Query(value = "SELECT p.id " +
            "FROM pages p " +
            "JOIN `index` i on i.page_id = p.id " +
            "JOIN lemmas l on i.lemma_id = l.id " +
            "WHERE l.lemma = :lemma AND l.site_id = :siteId",
            nativeQuery = true)
    List<Integer> findPageIdByLemmaAndSiteId (@Param(value = "lemma") String lemma,
                                              @Param(value = "siteId") int siteId);


    @Query(value = "SELECT p.id " +
            "FROM pages p " +
            "JOIN `index` i on i.page_id = p.id " +
            "JOIN lemmas l on i.lemma_id = l.id " +
            "WHERE l.lemma =? ",
            nativeQuery = true)
    List<Integer> findPageIdByLemma (String lemma);


    List<Page> findAllByIdIn (Collection<Integer> id);
}
