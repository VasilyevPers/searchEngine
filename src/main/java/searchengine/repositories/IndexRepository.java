package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Index;

@Repository
public interface IndexRepository extends JpaRepository<Index, Integer> {
    @Query(value = "SELECT `rank` FROM `index` WHERE page_id =? AND lemma_id =?",
    nativeQuery = true)
    Integer findRankByPageIdAndLemmaId (int pageId, int LemmaId);
}
