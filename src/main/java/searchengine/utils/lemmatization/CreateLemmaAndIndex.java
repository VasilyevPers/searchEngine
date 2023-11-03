package searchengine.utils.lemmatization;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class CreateLemmaAndIndex {
    public final void createLemmaAndIndex (Page page, List<Index> indexForSaving, Map<String, Lemma> lemmaList) throws IOException {
        Map<String, Integer> lemmasOnThePage = new Lemmatization().collectLemmas(page.getContent());

        for (Map.Entry<String, Integer> entry : lemmasOnThePage.entrySet()) {
            Index index = new Index();
            if (lemmaList.containsKey(entry.getKey())) {
                lemmaList.get(entry.getKey()).setFrequency(lemmaList.get(entry.getKey()).getFrequency() + 1);
                index.setLemma(lemmaList.get(entry.getKey()));
            } else {
                Lemma lemma = new Lemma();
                lemma.setLemma(entry.getKey());
                lemma.setFrequency(1);
                lemma.setSite(page.getSite());
                lemmaList.put(entry.getKey(), lemma);
                index.setLemma(lemma);
            }

            index.setRank(entry.getValue());
            index.setPage(page);
            indexForSaving.add(index);
        }
    }
}
