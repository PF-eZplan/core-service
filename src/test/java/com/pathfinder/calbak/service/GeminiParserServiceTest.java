package com.pathfinder.calbak.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.pathfinder.calbak.dto.ScheduleRecords.ParsedResponse;
import java.time.LocalDate;
import java.time.LocalTime;
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
    @DisplayName("텍스트만 입력 시 Gemini API 응답을 ParsedResponse 객체로 성공적으로 변환한다")
    void parseSchedule_TextOnly_Success() {
        // given: @Value로 주입되는 필드값을 강제로 세팅
        ReflectionTestUtils.setField(geminiParserService, "geminiApiUrl", "http://test-url");
        ReflectionTestUtils.setField(geminiParserService, "geminiApiKey", "test-key");

        String mockResponseBody = "{"
            + "\"candidates\": [{"
            + "\"content\": {"
            + "\"parts\": [{"
            + "\"text\": \"{\\\"title\\\": \\\"팀 회의\\\", \\\"content\\\": null, \\\"location\\\": \\\"강남역\\\","
            + "\\\"startDate\\\": \\\"2026-06-10\\\", \\\"startTime\\\": \\\"14:00:00\\\","
            + "\\\"endDate\\\": \\\"2026-06-10\\\", \\\"endTime\\\": \\\"15:00:00\\\", \\\"isAllDay\\\": false}\""
            + "}]"
            + "}"
            + "}]"
            + "}";

        ResponseEntity<String> mockResponse = new ResponseEntity<>(mockResponseBody, HttpStatus.OK);
        given(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
            .willReturn(mockResponse);

        // when: 텍스트만 전달 (images는 null)
        ParsedResponse response = geminiParserService.parseSchedule("내일 강남역에서 회의", null);

        // then: 파싱 결과 검증
        assertThat(response.title()).isEqualTo("팀 회의");
        assertThat(response.location()).isEqualTo("강남역");
        assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, 6, 10));
        assertThat(response.startTime()).isEqualTo(LocalTime.of(14, 0));
        assertThat(response.isAllDay()).isFalse();
    }
}
