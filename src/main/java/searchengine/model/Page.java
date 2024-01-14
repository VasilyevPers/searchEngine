package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import javax.persistence.Index;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "pages", indexes = {@Index(name = "index", columnList = "path, site_id", unique = true)})
@Getter
@Setter
@NoArgsConstructor
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "site_id", nullable = false, insertable = false, updatable = false)
    private int siteId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String path;

    @Column(nullable = false)
    private int code;

    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE,
            orphanRemoval = true,
            mappedBy = "page")
    private List<searchengine.model.Index> indexList = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY,cascade = CascadeType.MERGE, optional = false)
    @JoinColumn(name = "site_id", referencedColumnName = "id")
    private Site site;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Page page)) return false;

        if (siteId != page.siteId) return false;
        return Objects.equals(path, page.path);
    }

    @Override
    public int hashCode() {
        int result = siteId;
        result = 31 * result + (path != null ? path.hashCode() : 0);
        return result;
    }
}
