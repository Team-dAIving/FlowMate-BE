package kr.co.daeun.flowmatebe;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.startsWith;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FlowMateBeApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void 인증_오류도_code와_message로_응답한다() throws Exception {
        mockMvc.perform(get("/api/teams"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."));
    }

    @Test
    void 토큰_교환_API는_인증_없이_호출할_수_있다() throws Exception {
        mockMvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"\",\"client\":\"web\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("code는 필수입니다."));
    }

    @Test
    void OAuth_시작_요청에_client와_state가_필수다() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/github"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("client는 desktop 또는 web이어야 합니다."));
    }

    @Test
    void 올바른_OAuth_시작_요청은_GitHub로_이동한다() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/github")
                        .queryParam("client", "web")
                        .queryParam("state", "frontend-random-state"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(
                        HttpHeaders.LOCATION,
                        startsWith("https://github.com/login/oauth/authorize")
                ));
    }

    @Test
    void Electron의_opaque_origin을_CORS에서_허용한다() throws Exception {
        mockMvc.perform(options("/api/teams")
                        .header(HttpHeaders.ORIGIN, "null")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "null"));
    }

    @Test
    @Test
    void 프록시의_HTTPS_헤더로_Github_OAuth_콜백을_생성한다() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/github")
                        .header("X-Forwarded-Proto", "https")
                        .header("X-Forwarded-Host", "flowmate.observer-v.cloud")
                        .header("X-Forwarded-Port", "443"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(
                        HttpHeaders.LOCATION,
                        org.hamcrest.Matchers.containsString(
                                "redirect_uri=https://flowmate.observer-v.cloud/login/oauth2/code/github"
                        )
                ));
    }

    @Test
    void 배포된_웹_프론트의_인증_헤더를_CORS에서_허용한다() throws Exception {
        mockMvc.perform(options("/api/teams")
                        .header(
                                HttpHeaders.ORIGIN,
                                "https://flow-mate-fe.vercel.app"
                        )
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(
                                HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                                "Authorization, Content-Type"
                        ))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "https://flow-mate-fe.vercel.app"
                ))
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                        "Authorization, Content-Type"
                ));
    }

}
