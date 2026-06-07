package com.pathfinder.calbak.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.pathfinder.calbak.dto.ScheduleRecords.ParsedResponse;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class GeminiParserServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private GeminiParserService geminiParserService;

    @Test
    @DisplayName("텍스트만 입력 시 Gemini API 응답을 List<ParsedResponse> 배열 객체로 성공적으로 변환한다")
    void parseSchedule_TextOnly_Success() {
        // given: @Value로 주입되는 필드값을 강제로 세팅
        ReflectionTestUtils.setField(geminiParserService, "geminiApiUrl", "http://test-url");
        ReflectionTestUtils.setField(geminiParserService, "geminiApiKey", "test-key");

        String mockResponseBody = "{"
            + "\"candidates\": [{"
            + "\"content\": {"
            + "\"parts\": [{"
            + "\"text\": \"[ {\\\"title\\\": \\\"팀 회의\\\", \\\"content\\\": null, \\\"location\\\": \\\"강남역\\\","
            + "\\\"startDate\\\": \\\"2026-06-10\\\", \\\"startTime\\\": \\\"14:00:00\\\","
            + "\\\"endDate\\\": \\\"2026-06-10\\\", \\\"endTime\\\": \\\"15:00:00\\\", \\\"isAllDay\\\": false} ]\""
            + "}]"
            + "}"
            + "}]"
            + "}";

        ResponseEntity<String> mockResponse = new ResponseEntity<>(mockResponseBody, HttpStatus.OK);
        given(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
            .willReturn(mockResponse);

        // when: 텍스트만 전달 (images는 null)
        List<ParsedResponse> responses = geminiParserService.parseSchedule("내일 강남역에서 회의", null);

        // then: 파싱 결과 검증
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).title()).isEqualTo("팀 회의");
        assertThat(responses.get(0).location()).isEqualTo("강남역");
        assertThat(responses.get(0).startDate()).isEqualTo(LocalDate.of(2026, 6, 10));
        assertThat(responses.get(0).startTime()).isEqualTo(LocalTime.of(14, 0));
        assertThat(responses.get(0).isAllDay()).isFalse();
    }

    @Test
    @DisplayName("마크다운 코드펜스가 포함된 응답도 정상 파싱된다 (Regression 피드백 1)")
    void parseScheduleText_MarkdownCodeFence() {
        ReflectionTestUtils.setField(geminiParserService, "geminiApiUrl", "http://test-url");
        ReflectionTestUtils.setField(geminiParserService, "geminiApiKey", "test-key");

        // 유니코드 치환으로 UI 및 JSON 파싱 에러 방지
        String mockResponseBody = "{"
            + "\"candidates\": [{"
            + "\"content\": {"
            + "\"parts\": [{"
            + "\"text\": \"\\u0060\\u0060\\u0060json\\n[ { \\\"title\\\": \\\"마크다운 테스트\\\", \\\"startDate\\\": \\\"2026-06-10\\\", \\\"endDate\\\": \\\"2026-06-10\\\", \\\"isAllDay\\\": true } ]\\n\\u0060\\u0060\\u0060\""
            + "}]"
            + "}"
            + "}]"
            + "}";

        given(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
            .willReturn(new ResponseEntity<>(mockResponseBody, HttpStatus.OK));

        List<ParsedResponse> responses = geminiParserService.parseSchedule("회의", null);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).title()).isEqualTo("마크다운 테스트");
    }

    @Test
    @DisplayName("endDate가 null인 경우 startDate와 동일하게 fallback 처리할 수 있다 (Regression 피드백 1)")
    void parseScheduleText_NullEndDateFallback() {
        ReflectionTestUtils.setField(geminiParserService, "geminiApiUrl", "http://test-url");
        ReflectionTestUtils.setField(geminiParserService, "geminiApiKey", "test-key");

        String mockResponseBody = "{"
            + "\"candidates\": [{"
            + "\"content\": {"
            + "\"parts\": [{"
            + "\"text\": \"[ { \\\"title\\\": \\\"폴백 테스트\\\", \\\"startDate\\\": \\\"2026-06-10\\\", \\\"endDate\\\": null, \\\"isAllDay\\\": true } ]\""
            + "}]"
            + "}"
            + "}]"
            + "}";

        given(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
            .willReturn(new ResponseEntity<>(mockResponseBody, HttpStatus.OK));

        List<ParsedResponse> responses = geminiParserService.parseSchedule("테스트", null);
        ParsedResponse res = responses.get(0);

        assertThat(res.startDate()).isEqualTo(LocalDate.of(2026, 6, 10));
        assertThat(res.endDate()).isEqualTo(LocalDate.of(2026, 6, 10));
    }
}
