package searchengine.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Table(name = "lemmas")
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "site_id", nullable = false, updatable = false, insertable = false)
    private int siteId;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String lemma;

    @Column(nullable = false)
    private int frequency;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id", referencedColumnName = "id")
    private Site site;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.REFRESH,
            orphanRemoval = true,
            mappedBy = "lemma")
    private List<Index> index;

//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (!(o instanceof Lemma lemma1)) return false;
//
//        if (siteId != lemma1.siteId) return false;
//        return lemma.equals(lemma1.lemma);
//    }
//
//    @Override
//    public int hashCode() {
//        int result = siteId;
//        result = 31 * result + lemma.hashCode();
//        return result;
//    }
}
