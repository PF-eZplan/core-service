package com.pathfinder.calbak.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pathfinder.calbak.domain.entity.User;
import com.pathfinder.calbak.domain.enums.Enums;
import com.pathfinder.calbak.domain.enums.Enums.NotificationStatus;
import com.pathfinder.calbak.dto.UserAdditionalInfoRequest;
import com.pathfinder.calbak.dto.UserNicknameUpdateRequest;
import com.pathfinder.calbak.repository.UserRepository;
import com.pathfinder.calbak.security.JwtProvider;
import com.pathfinder.calbak.service.UserService;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@Import(UserService.class) // UserService 강제 로드
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtProvider jwtProvider;

    @Test
    @WithMockUser(username = "test@google.com")
    @DisplayName("유효한 온보딩 정보가 들어오면 200 OK와 성공 메시지를 반환한다.")
    void updateAdditionalInfo_Success() throws Exception {
        // given: 들어올 요청 데이터 세팅
        UserAdditionalInfoRequest request = new UserAdditionalInfoRequest(
            "새로운닉네임",
            Enums.Gender.MALE,
            Enums.AgeGroup.AGE_20S,
            "대학생,프리랜서",
            "수업 관리",
            LocalTime.of(23, 30),
            LocalTime.of(7, 0),
            NotificationStatus.NO
        );

        // given: 가짜 유저 객체 세팅
        User mockUser = User.builder()
            .email("test@google.com")
            .nickname("임시닉네임")
            .build();

        // given: userRepository가 호출될 때 DB 조회 대신 무조건 이 값을 반환하도록 설정
        given(userRepository.findByEmail("test@google.com"))
            .willReturn(Optional.of(mockUser));
        given(userRepository.existsByNickname(request.nickname()))
            .willReturn(false);

        // when & then: API 찌르고 결과 확인
        mockMvc.perform(patch("/api/users/additional-info")
                .with(csrf()) // 테스트 시 CSRF 방어 우회
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(content().string("추가 정보 등록이 완료되었습니다."));
    }

    @Test
    @WithMockUser(username = "test@google.com")
    @DisplayName("유효한 닉네임 변경 정보가 들어오면 200 OK와 성공 메시지를 반환한다.")
    void updateNickname_Success() throws Exception {
        // given: 들어올 요청 데이터 세팅
        UserNicknameUpdateRequest request = new UserNicknameUpdateRequest("슈퍼개발자");
        
        User mockUser = User.builder()
            .email("test@google.com")
            .nickname("기존닉네임")
            .build();

        given(userRepository.findByEmail("test@google.com"))
            .willReturn(Optional.of(mockUser));
        given(userRepository.existsByNickname(request.nickname()))
            .willReturn(false);

        // when & then: API 찌르고 결과 확인
        mockMvc.perform(patch("/api/users/nickname")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(content().string("닉네임이 성공적으로 변경되었습니다."));
    }
}
