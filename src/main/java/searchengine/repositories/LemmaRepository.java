package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;

import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    @Query(value = "SELECT SUM(`frequency`) FROM lemmas where site_id = ?",
    nativeQuery = true)
    Integer countLemmaInSite (int siteId);

    @Override
    void deleteById(Integer integer);

    Lemma findByLemmaAndSiteId (String lemma, int siteId);

    @Query(value = "SELECT l.* FROM lemmas l JOIN `index` i on i.lemma_id = l.id WHERE i.page_id =?",
    nativeQuery = true)
   List<Lemma> findAllByPageId (int pageId);

    List<Lemma> findAllByLemma (String lemma);
}
