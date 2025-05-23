package searchengine.dto.searchResponse;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SearchResponseFalse {
    private final boolean result = false;
    private String error;
}
