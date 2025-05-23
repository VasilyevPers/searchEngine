package searchengine.dto.searchResponse;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class SearchResponseTrue {
    private final boolean result = true;
    private int count;
    private List<FoundPage> data = new ArrayList<>();
}
