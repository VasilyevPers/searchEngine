package searchengine.model;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "sites")
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private int id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusIndexing status;

    @Column(name = "status_time", columnDefinition = "TIMESTAMP", nullable = false)
    private LocalDateTime statusTime;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String url;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String name;

    @OneToMany(fetch = FetchType.LAZY,
            orphanRemoval = true,
            mappedBy = "site")
    private List<Lemma> lemmasList = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY,cascade = CascadeType.REMOVE,
            orphanRemoval = true,
            mappedBy = "site")
    private List<Page> pages = new ArrayList<>();

    public enum StatusIndexing {
        INDEXING,
        INDEXED,
        FAILED;
    }
}
