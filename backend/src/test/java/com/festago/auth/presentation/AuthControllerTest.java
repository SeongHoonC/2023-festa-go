package com.festago.auth.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.festago.auth.application.AuthFacadeService;
import com.festago.auth.domain.Role;
import com.festago.auth.domain.SocialType;
import com.festago.auth.dto.LoginRequest;
import com.festago.auth.dto.LoginResponse;
import com.festago.fcm.application.MemberFCMService;
import com.festago.presentation.AuthController;
import com.festago.support.CustomWebMvcTest;
import com.festago.support.WithMockAuth;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@CustomWebMvcTest(AuthController.class)
@DisplayNameGeneration(ReplaceUnderscores.class)
@SuppressWarnings("NonAsciiCharacters")
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    AuthFacadeService authFacadeService;

    @MockBean
    MemberFCMService memberFCMService;

    @Test
    void OAuth2_로그인을_한다() throws Exception {
        // given
        LoginResponse expected = new LoginResponse("accesstoken", "nickname", true);
        given(authFacadeService.login(any(), any()))
            .willReturn(expected);
        LoginRequest request = new LoginRequest(SocialType.FESTAGO, "code", "fcmToken");

        // when & then
        String response = mockMvc.perform(post("/auth/oauth2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andDo(print())
            .andReturn()
            .getResponse()
            .getContentAsString();
        LoginResponse actual = objectMapper.readValue(response, LoginResponse.class);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void 로그인을_하지_않고_탈퇴를_하면_예외() throws Exception {
        mockMvc.perform(delete("/auth")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockAuth(role = Role.ADMIN)
    void 멤버_권한이_아닌데_탈퇴하면_예외() throws Exception {
        mockMvc.perform(delete("/auth")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockAuth
    void 회원_탈퇴를_한다() throws Exception {
        mockMvc.perform(delete("/auth")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    void 회원가입_시_유저의_FCM_를_저장한다() throws Exception {
        // given
        String fcmToken = "fcmToken";
        String accessToken = "accessToken";
        boolean isNewMember = true;
        LoginResponse expected = new LoginResponse(accessToken, "nickname", isNewMember);
        given(authFacadeService.login(any(), any()))
            .willReturn(expected);
        LoginRequest request = new LoginRequest(SocialType.FESTAGO, "code", fcmToken);

        // when & then
        String response = mockMvc.perform(post("/auth/oauth2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andDo(print())
            .andReturn()
            .getResponse()
            .getContentAsString();
        LoginResponse actual = objectMapper.readValue(response, LoginResponse.class);

        assertThat(actual).isEqualTo(expected);
        verify(memberFCMService, times(1))
            .saveMemberFCM(isNewMember, accessToken, fcmToken);
    }

    @Test
    void 로그인_시_유저의_FCM_를_저장한다() throws Exception {
        // given
        String fcmToken = "fcmToken";
        String accessToken = "accessToken";
        boolean isNewMember = false;
        LoginResponse expected = new LoginResponse(accessToken, "nickname", isNewMember);
        given(authFacadeService.login(any(), any()))
            .willReturn(expected);
        LoginRequest request = new LoginRequest(SocialType.FESTAGO, "code", fcmToken);

        // when & then
        String response = mockMvc.perform(post("/auth/oauth2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andDo(print())
            .andReturn()
            .getResponse()
            .getContentAsString();
        LoginResponse actual = objectMapper.readValue(response, LoginResponse.class);

        assertThat(actual).isEqualTo(expected);
        verify(memberFCMService, times(1))
            .saveMemberFCM(isNewMember, accessToken, fcmToken);
    }
}
