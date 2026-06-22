package com.pathfinder.calbak.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pathfinder.calbak.dto.ScheduleRecords.ParsedResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiParserService {

    private final RestTemplate restTemplate;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    /**
     * 텍스트와 이미지를 Gemini API로 전송하여 일정 데이터를 파싱 (다중 일정 배열 반환)
     */
    public List<ParsedResponse> parseSchedule(String rawText, List<MultipartFile> images) {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        // 오늘 날짜를 프롬프트에 포함: "이번 주 금요일", "다음 달" 같은 상대 표현 해석에 필요
        String today = LocalDate.now().toString();

        String prompt = """
            오늘 날짜는 %s 입니다.
            제공된 텍스트와 이미지(전단지, 포스터, 자필 메모 등)를 분석하여 일정 데이터를 추출해줘.
            이미지가 있다면 이미지 속의 날짜, 시간, 장소, 제목을 우선적으로 추출해.
            단, 이미지가 채팅, 메신저, 문자, 카카오톡, 디스코드, 슬랙 등의 화면인 경우, 메시지 전송 시각, 읽음 표시, 프로필 정보, 상태바 시간 등은 일정 정보로 간주하지 마.
            실제 일정을 설명하는 메시지 내용에 포함된 날짜, 시간, 장소만 추출해.
            여러 이미지가 있다면 모든 이미지의 내용을 종합하여 하나의 일정으로 파싱해.
            단, 콤마 등으로 여러 개의 일정이 나열되어 있다면 반드시 배열 형태로 모두 추출해.
            "이번 주", "다음 주", "오늘", "내일" 같은 상대적 날짜 표현은 오늘 날짜를 기준으로 계산해.
            
            [반복 일정 규칙]
            - "매주 수요일 저녁 7시 동아리"와 같은 반복 일정이 입력되면, '가장 가까운 미래의 수요일'을 계산해서 `startDate`로 설정해.
            - 반복 일정인 경우 `repeatPattern`을 DAILY, WEEKLY, MONTHLY, YEARLY 중 하나로 설정하고, 아니면 NONE으로 해.
            - 언제까지 반복할지 모르면 `repeatEndDate`는 반드시 null로 비워둬.
            
            [기간 일정 특별 규칙]
            - "6/15~6/20 신청기간", "접수기간", "응시기간" 등 마감일이 중요한 이벤트는 기간 전체가 아닌, 반드시 '마지막 날(종료일)'을 startDate와 endDate로 하는 하루짜리 단일 일정으로 추출해 (예: startDate="2026-06-20", endDate="2026-06-20").
            - 단, "6/25~6/27 여행", "휴가", "출장", "방학" 등 실제로 해당 기간 내내 진행되는 이벤트는 기존처럼 startDate와 endDate를 각각 시작일과 종료일로 하는 기간 일정으로 추출해.
            
            [대학 시간표 특별 규칙]
            - 이미지나 텍스트가 "시간표"이고 "연도와 학기(예: 2026년 1학기)" 정보가 명시되어 있다면, 각 수업을 16주간 반복되는 일정(`repeatPattern`: "WEEKLY")으로 추출해.
            - 1학기의 시작일은 해당 연도의 3월 2일, 2학기의 시작일은 해당 연도의 9월 1일로 기준을 잡아 (만약 기준일이 주말이거나 공휴일이면 그 직후의 가장 빠른 평일을 학기 시작일로 간주).
            - 각 수업의 `startDate`는 위에서 구한 학기 시작일 이후 '가장 먼저 도래하는 해당 수업의 요일' 날짜로 설정해.
            - 각 수업의 `repeatEndDate`는 해당 수업의 `startDate`로부터 정확히 16주(112일) 뒤의 날짜로 설정해.
            - 연도와 학기 정보가 명확하지 않다면 이 시간표 규칙을 무시하고 기존처럼 단일 일정으로 파싱해.
            
            부가적인 설명 없이 오직 JSON 배열만 반환해.
            - startDate, endDate 형식: YYYY-MM-DD
            - startTime, endTime 형식: HH:mm:ss (시간이 없으면 null)
            - isAllDay: 시간이 없으면 true, 있으면 false
            - endDate: 종료일이 명시되지 않은 단일 일정이면 startDate와 동일한 값으로 설정
            - endTime: 종료 시간이 없으면 null
            
            [
              {
                "title": "일정 제목",
                "content": "추가 메모 (없으면 null)",
                "location": "장소 (없으면 null)",
                "startDate": "YYYY-MM-DD",
                "startTime": "HH:mm:ss",
                "endDate": "YYYY-MM-DD",
                "endTime": "HH:mm:ss",
                "isAllDay": true/false,
                "repeatPattern": "NONE/DAILY/WEEKLY/MONTHLY/YEARLY",
                "repeatEndDate": "YYYY-MM-DD 또는 null"
              }
            ]
            """.formatted(today);

        List<Map<String, Object>> parts = new ArrayList<>();

        // 1. 프롬프트 + 사용자 텍스트를 하나의 text 파트로 결합
        String combinedText = prompt;
        if (rawText != null && !rawText.isBlank()) {
            combinedText += "\n\n사용자 입력: " + rawText;
        }
        parts.add(Map.of("text", combinedText));

        // 2. 이미지들을 Base64로 인코딩하여 파트에 추가 (여러 장 모두 처리)
        if (images != null && !images.isEmpty()) {
            log.info("업로드 이미지 개수: {}", images.size());
            for (MultipartFile image : images) {

                log.info(
                    "이미지명={}, 크기={} bytes, 타입={}",
                    image.getOriginalFilename(),
                    image.getSize(),
                    image.getContentType()
                );

                try {
                    String base64Image = Base64.getEncoder().encodeToString(image.getBytes());
                    parts.add(Map.of("inline_data", Map.of(
                        "mime_type", image.getContentType() != null ? image.getContentType() : "image/jpeg",
                        "data", base64Image
                    )));
                } catch (Exception e) {
                    log.error("이미지 인코딩 실패: {}", image.getOriginalFilename(), e);
                    throw new RuntimeException("이미지를 처리하는 중 오류가 발생했습니다.");
                }
            }
        }

        Map<String, Object> contentMap = Map.of("parts", parts);
        Map<String, Object> requestBody = Map.of("contents", List.of(contentMap));

        log.info("Gemini 요청 part 개수: {}", parts.size());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        String requestUrl = geminiApiUrl + "?key=" + geminiApiKey;

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(requestUrl, requestEntity, String.class);

            JsonNode rootNode = objectMapper.readTree(response.getBody());
            JsonNode candidates = rootNode.path("candidates");

            if (candidates.isMissingNode() || !candidates.isArray() || candidates.isEmpty()) {
                throw new RuntimeException("Gemini API 응답에서 일정 데이터를 찾을 수 없습니다.");
            }

            JsonNode partsNode = candidates.get(0)
                .path("content")
                .path("parts");
            if (partsNode.isMissingNode() || !partsNode.isArray() || partsNode.isEmpty()) {
                throw new RuntimeException("Gemini API 응답 구조가 올바르지 않습니다.");
            }

            JsonNode textNode = partsNode.get(0).path("text");
            if (textNode.isMissingNode() || textNode.asText().isBlank()) {
                throw new RuntimeException("Gemini API 텍스트 응답이 비어있습니다.");
            }

            String responseText = textNode.asText();

            // Gemini가 ```json ... ``` 마크다운 블록으로 감싸서 반환하는 경우 제거
            String cleanJson = responseText
                .replaceAll("(?s)```json\\s*", "")
                .replaceAll("(?s)```\\s*", "")
                .trim();

            List<ParsedResponse> parsedList = objectMapper.readValue(cleanJson,
                new TypeReference<List<ParsedResponse>>() {
                });
            List<ParsedResponse> fallbackList = new ArrayList<>();

            // endDate 누락 시 startDate로 강제 Fallback 처리
            for (ParsedResponse parsed : parsedList) {
                if (parsed.endDate() == null && parsed.startDate() != null) {
                    parsed = new ParsedResponse(
                        parsed.title(),
                        parsed.content(),
                        parsed.location(),
                        parsed.startDate(),
                        parsed.startTime(),
                        parsed.startDate(), // endDate = startDate
                        parsed.endTime(),
                        parsed.isAllDay(),
                        parsed.repeatPattern(),
                        parsed.repeatEndDate()
                    );
                }
                fallbackList.add(parsed);
            }

            return fallbackList;

        } catch (HttpClientErrorException e) {
            String rawBody = e.getResponseBodyAsString();
            String sanitizedBody = rawBody.length() > 150 ? rawBody.substring(0, 150) + "...[REDACTED]" : rawBody;

            log.error("Gemini API 4xx 오류 - 상태코드: {}", e.getStatusCode());
            log.error("응답본문 요약: {}", sanitizedBody);

            throw new RuntimeException("일정 데이터를 분석하는데 실패했습니다.");
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gemini API 파싱 실패", e);
            throw new RuntimeException("일정 데이터를 분석하는데 실패했습니다.");
        }
    }
}
