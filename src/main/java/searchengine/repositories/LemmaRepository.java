package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    @Override
    @Query(value = "insert into entity on duplicate key update frequency = frequency + 1", nativeQuery = true)
    <S extends Lemma> S save(S entity);

    @Query(value = "SELECT SUM(`frequency`) FROM lemmas where site_id = ?",
    nativeQuery = true)
    Integer countLemmaInSite (int siteId);
}
