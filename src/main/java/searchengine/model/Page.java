package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "pages")
@Getter
@Setter
@NoArgsConstructor
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private int id;

    @Column(name = "site_id", nullable = false, insertable = false, updatable = false)
    private int siteId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String path;

    @Column(nullable = false)
    private int code;

    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;

    //@Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.REFRESH)
    @JoinColumn(name = "site_id", referencedColumnName = "id")
    private Site site;

    @Override
    public String toString() {
        return "id = " + id +
                ", siteId = " + siteId +
                ", path = " + path +
                ", code= " + code +
                ", content = " + content;
    }

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
