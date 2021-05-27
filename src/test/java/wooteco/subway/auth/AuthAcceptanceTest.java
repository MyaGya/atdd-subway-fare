package wooteco.subway.auth;

import static wooteco.subway.member.MemberAcceptanceTest.회원_생성을_요청;
import static wooteco.subway.member.MemberAcceptanceTest.회원_정보_조회됨;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import wooteco.subway.AcceptanceTest;
import wooteco.subway.auth.dto.TokenResponse;
import wooteco.subway.util.ExceptionCheck;

public class AuthAcceptanceTest extends AcceptanceTest {
    private static final String EMAIL = "email@email.com";
    private static final String PASSWORD = "password";
    private static final Integer AGE = 20;

    @DisplayName("Bearer Auth")
    @Test
    void myInfoWithBearerAuth() {
        // given
        회원_등록되어_있음(EMAIL, PASSWORD, AGE);
        TokenResponse tokenResponse = 로그인되어_있음(EMAIL, PASSWORD);

        // when
        ExtractableResponse<Response> response = 내_회원_정보_조회_요청(tokenResponse);

        // then
        회원_정보_조회됨(response, EMAIL, AGE);
    }

    @DisplayName("Bearer Auth 유효하지 않은 토큰")
    @Test
    void myInfoWithWrongBearerAuth() {
        TokenResponse tokenResponse = new TokenResponse("accesstoken");

        RestAssured
            .given().log().all()
            .auth().oauth2(tokenResponse.getAccessToken())
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .when().get("/members/me")
            .then().log().all()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @DisplayName("email 이 다른 경우")
    @Test
    void invalidEmail() {
        회원_등록되어_있음(EMAIL, PASSWORD, AGE);

        Map<String, String> params = new HashMap<>();
        params.put("email", "invalidEmail");
        params.put("password", PASSWORD);

        ExtractableResponse<Response> response = 로그인_결과_요청(params);

        ExceptionCheck.코드_400_응답됨(response);
        ExceptionCheck.에러_문구_확인(response, "INVALID_EMAIL");
    }

    @DisplayName("password 가 다른 경우")
    @Test
    void invalidPassword() {
        회원_등록되어_있음(EMAIL, PASSWORD, AGE);

        Map<String, String> params = new HashMap<>();
        params.put("email", EMAIL);
        params.put("password", "invalidPassword");

        ExtractableResponse<Response> response = 로그인_결과_요청(params);

        ExceptionCheck.코드_400_응답됨(response);
        ExceptionCheck.에러_문구_확인(response, "INVALID_PASSWORD");
    }

    public static ExtractableResponse<Response> 회원_등록되어_있음(String email, String password,
        Integer age) {
        return 회원_생성을_요청(email, password, age);
    }

    public static TokenResponse 로그인되어_있음(String email, String password) {
        ExtractableResponse<Response> response = 로그인_요청(email, password);
        return response.as(TokenResponse.class);
    }

    public static ExtractableResponse<Response> 로그인_요청(String email, String password) {
        Map<String, String> params = new HashMap<>();
        params.put("email", email);
        params.put("password", password);

        return RestAssured.given().log().all().
                contentType(MediaType.APPLICATION_JSON_VALUE).
                body(params).
                when().
                post("/login/token").
                then().
                log().all().
                statusCode(HttpStatus.OK.value()).
                extract();
    }

    public static ExtractableResponse<Response> 내_회원_정보_조회_요청(TokenResponse tokenResponse) {
        return RestAssured.given().log().all().
            auth().oauth2(tokenResponse.getAccessToken()).
            accept(MediaType.APPLICATION_JSON_VALUE).
            when().
            get("/members/me").
            then().
            log().all().
            statusCode(HttpStatus.OK.value()).
            extract();
    }

    private static ExtractableResponse<Response> 로그인_결과_요청(Map<String, String> params) {
        return RestAssured
            .given().log().all()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(params)
            .when().post("/login/token")
            .then().log().all()
            .extract();

    }
}
