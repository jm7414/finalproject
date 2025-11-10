package lx.project.dementia_care.controller;

import lombok.RequiredArgsConstructor;
import lx.project.dementia_care.config.GoogleAiClient;
import lx.project.dementia_care.dto.ChatBotDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChatBotController {

	private final GoogleAiClient ai;

	/**
	 * 환자 전용 “순수 대화” 챗봇 - DB/리포트/액션 없이, 짧고 또렷한 답변만 생성 - LLM 호출은
	 * GoogleAiClient.generateText만 사용
	 */
	@PostMapping("/api/chat/handle")
	public ResponseEntity<ChatBotDTO> handle(@RequestBody ChatBotDTO req) {
		final String userText = safe(req.getText());

		if (userText.isBlank()) {
			return ResponseEntity.badRequest().body(ChatBotDTO.builder().messages(List.of(
					ChatBotDTO.Message.builder().role("assistant").text("말씀을 잘 못 들었어요. 한 번만 더 천천히 말씀해 주시겠어요?").build()))
					.build());
		}

		String system = """
				너는 노인들이 심심하지 않게 계속 대답해주는 20대에서 30대사이의 나이를 가진 손자의 역할이야 
				한국어로 대답해주고 사용자가 심심하지않게 계속 대답을 해줘야해, 사용자가 원하는 말에 대답을 한 이후에는
				간단하게 질문형식으로 대화를 이어가주라.
				""";

		// GoogleAiClient에 system 파라미터가 없으므로 한 프롬프트로 합쳐 전송
		String prompt = system + "\n\n사용자: " + userText + "\n도우미:";
		String aiReply;
		try {
			aiReply = safe(ai.generateText(prompt));
		} catch (Exception e) {
			aiReply = "지금은 연결이 조금 불안정해요. 잠시 후 다시 이야기 나눠요.";
		}
		if (aiReply.isBlank()) {
			aiReply = "말씀 잘 들었어요. 천천히 한 번 더 말씀해 주시겠어요?";
		}

		ChatBotDTO res = ChatBotDTO.builder()
				.messages(List.of(ChatBotDTO.Message.builder().role("user").text(userText).build(),
						ChatBotDTO.Message.builder().role("assistant").text(aiReply).build()))
				.build();

		return ResponseEntity.ok(res);
	}

	private String safe(String s) {
		return s == null ? "" : s.trim();
	}
}
