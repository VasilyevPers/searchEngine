package searchengine.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@Table(name = "`index`")
public class Index {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "page_id", nullable = false, updatable = false, insertable = false)
    private int pageId;

    @Column(name = "lemma_id", nullable = false, updatable = false, insertable = false)
    private int lemmaId;

    @Column(name = "`rank`", columnDefinition = "FLOAT", nullable = false)
    private float rank;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "page_id", referencedColumnName = "id")
    private Page page;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "lemma_id", referencedColumnName = "id")
    private Lemma lemma;

}
