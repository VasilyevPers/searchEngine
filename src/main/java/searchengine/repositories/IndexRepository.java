package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Index;

@Repository
public interface IndexRepository extends JpaRepository<Index, Integer> {
    @Query(value = "SELECT i.`rank` " +
            "FROM `index` i " +
            "JOIN lemmas l ON i.lemma_id = l.id " +
            "WHERE i.page_id =:pageId and l.lemma = :lemma",
    nativeQuery = true)
    Integer findRankByPageIdAndLemma (@Param(value = "pageId") int pageId,
                                      @Param(value = "lemma") String lemma);

    @Query(value = "SELECT i.* " +
            "FROM `index` i " +
            "JOIN lemmas l ON i.lemma_id = l.id " +
            "WHERE i.page_id = :pageId and l.lemma = :lemma",
    nativeQuery = true)
    Index findByPageIdAndLemma (@Param(value = "pageId") int pageId,
                                @Param(value = "lemma") String lemma);
}
