package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Site;

import java.util.List;

@Repository
public interface SiteRepository extends JpaRepository<Site, Integer> {
    @Query(value = "select url from sites",
           nativeQuery = true)
    List<String> findAllUrl ();
}
