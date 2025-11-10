package lx.project.dementia_care.dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatBotDTO {

	// 요청 필드
	private Long userId; // 환자 식별용(없으면도 동작 가능)
	private String text; // 사용자가 말한 문장

	// 응답 필드
	private List<Message> messages; // 대화 버블 (user/assistant)

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class Message {
		private String role; // "user" | "assistant"
		private String text;
	}
}
