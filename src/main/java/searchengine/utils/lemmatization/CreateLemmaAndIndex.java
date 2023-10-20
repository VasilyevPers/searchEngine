package searchengine.utils.lemmatization;

import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CreateLemmaAndIndex {
    public final void createLemmaAndIndex (Page page, Map<String, List<Index>> indexList) throws IOException {
        Map<String, Integer> lemmasOnThePage = new Lemmatization().collectLemmas(page.getContent());
        List<Index> indexInPage = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : lemmasOnThePage.entrySet()) {
            Lemma lemma = new Lemma();
            Index index = new Index();
            lemma.setLemma(entry.getKey());
            lemma.setSite(page.getSite());
            lemma.setFrequency(1);

            index.setRank(entry.getValue());
            index.setLemma(lemma);
            index.setPage(page);

            indexInPage.add(index);
        }
        indexList.put(page.getPath(), indexInPage);
    }
}
