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
}
