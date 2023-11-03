package searchengine.utils.lemmatization;

import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.repositories.LemmaRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UpdateLemma {
    public Map<String, Lemma> updateLemma (LemmaRepository lemmaRepository, List<Index> indexForSaving, Map<String, Lemma> lemmaList) {
        Map<String, Lemma> lemmaForSaving = new HashMap<>();
        for (Map.Entry<String, Lemma> lemma : lemmaList.entrySet()) {
            Lemma lemmaInBD = lemmaRepository.findByLemmaAndSiteId(lemma.getKey(), lemma.getValue().getSite().getId());
            if (lemmaInBD == null) {
                lemmaInBD = lemma.getValue();
            } else {
                lemmaInBD.setFrequency(lemmaInBD.getFrequency() + lemma.getValue().getFrequency());
            }
            Lemma finalLemmaInBD = lemmaInBD;
            indexForSaving.forEach(i -> {
                if(i.getLemma().getLemma().equals(finalLemmaInBD.getLemma())) {
                    i.setLemma(finalLemmaInBD);
                }
            });
            lemmaForSaving.put(finalLemmaInBD.getLemma(), finalLemmaInBD);
        }
        return lemmaForSaving;
    }
}
