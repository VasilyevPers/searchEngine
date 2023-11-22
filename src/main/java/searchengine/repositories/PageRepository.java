package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;

import java.util.List;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {
    @Query(value = "select path from pages where site_id=?",
           nativeQuery = true)
    List<String> findAllPathBySiteId (int siteId);
    Page findByPath (String path);
    int countBySiteId (int siteId);
    boolean existsByPath (String path);
    @Query(value = "SELECT p.* " +
            "FROM pages p " +
            "JOIN `index` i on i.page_id = p.id " +
            "JOIN lemmas l on i.lemma_id = l.id " +
            "WHERE l.id =?",
    nativeQuery = true)
    List<Page> findByLemmaId (int id);
    @Query(value = "SELECT p.* " +
            "FROM pages p " +
            "JOIN `index` i on i.page_id = p.id " +
            "JOIN lemmas l on i.lemma_id = l.id " +
            "WHERE l.id =? " +
            "LIMIT 30",
            nativeQuery = true)
    List<Page> findByLemmaIdAlternative (int id);
}
