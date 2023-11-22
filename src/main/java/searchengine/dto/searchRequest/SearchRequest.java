package searchengine.dto.searchRequest;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Setter
@Getter
public class SearchRequest {
    private final boolean result = true;
    private int count;
    private List<FoundPage> data = new ArrayList<>();
}
