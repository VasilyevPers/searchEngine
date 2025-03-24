package searchengine.utils.lemmatization;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.utils.CustomExceptions;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateLemmaAndIndex {

    public void createLemmaAndIndex (Page page) throws CustomExceptions.LemmatizationConnectException {
        Map<String, Integer> lemmasOnThePage = new HashMap<>();
        try {
            lemmasOnThePage = new Lemmatization().collectLemmasForIndexing(page.getContent());
        } catch (IOException e) {
            throw new CustomExceptions.LemmatizationConnectException("Ошибка подключения к библиотеке лемматизации");
        }

        /* В случае, если список индексов пустой, это значит что при подключении
         * к лемматизатору возникла ошибка и данную ситуацию нужно передать в логи
         * для дальнейшей обработки. */


        for (Map.Entry<String, Integer> entry : lemmasOnThePage.entrySet()) {

            Lemma lemma = new Lemma();
            lemma.setLemma(entry.getKey());
            lemma.setFrequency(1);
            lemma.setSite(page.getSite());

            Index index = new Index();
            index.setRank(entry.getValue());
            index.setLemma(lemma);
            index.setPage(page);

            page.getIndexList().add(index);
        }
    }

    public void createListLemmaAndIndex (Page page, List<Index> indexForSaving, Map<String, Lemma> lemmaList) {
        for (Index index : page.getIndexList()) {
            if (lemmaList.containsKey(index.getLemma().getLemma())) {
                lemmaList.get(index.getLemma().getLemma())
                         .setFrequency(lemmaList.get(index.getLemma().getLemma()).getFrequency() + 1);
            } else {
                lemmaList.put(index.getLemma().getLemma(), index.getLemma());
            }
        }
        indexForSaving.addAll(page.getIndexList());
        page.getIndexList().clear();
    }
}
