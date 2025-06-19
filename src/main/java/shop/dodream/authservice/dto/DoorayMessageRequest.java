package shop.dodream.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoorayMessageRequest {
    private String botName;
    private String text;
    private List<Attachment> attachments;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Attachment {
        private String title;
        private String text;
        private String titleLink;
        private String botIconImage;
        private String color;
    }
}
