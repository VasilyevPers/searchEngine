package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Index;

import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<Index, Integer> {
    @Query(value = "SELECT i.* " +
            "FROM index i " +
            "JOIN lemmas l ON i.lemma_id = l.id " +
            "WHERE l.lemma = :lemma",
    nativeQuery = true)
    List<Index> findByLemma (@Param(value = "lemma") String lemma);

    @Query(value = "SELECT i.* " +
            "FROM index i " +
            "JOIN lemmas l ON i.lemma_id = l.id " +
            "WHERE l.lemma = :lemma AND l.site_id = :siteId",
            nativeQuery = true)
    List<Index> findByLemmaAndSiteId (@Param(value = "lemma") String lemma,
                                      @Param(value = "siteId") int siteId);
}
