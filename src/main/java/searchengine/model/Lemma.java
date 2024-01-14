package searchengine.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Table(name = "lemmas", indexes = {@javax.persistence.Index(name = "'index'",columnList = "lemma, site_id", unique = true)})
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

    @OneToMany(fetch = FetchType.LAZY,
            orphanRemoval = true,
            mappedBy = "lemma")
    private List<Index> index;

    @Override
    public String toString() {
        return "id = " + id + "\n" +
                "siteId = " + siteId + "\n" +
                "lemma = " + lemma + '\n' +
                "frequency = " + frequency;
    }
}
