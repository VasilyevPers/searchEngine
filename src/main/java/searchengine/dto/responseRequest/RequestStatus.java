package searchengine.dto.responseRequest;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RequestStatus {
    private boolean status;
    private String error;
}
