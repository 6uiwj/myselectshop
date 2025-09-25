package com.sparta.myselectshop.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.myselectshop.dto.KakaoUserInfoDto;
import com.sparta.myselectshop.entity.User;
import com.sparta.myselectshop.entity.UserRoleEnum;
import com.sparta.myselectshop.jwt.JwtUtil;
import com.sparta.myselectshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@Slf4j(topic = "KAKAO Login")
@Service
@RequiredArgsConstructor
public class KakaoService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final JwtUtil jwtUtil;

    public String kakaoLogin(String code) throws JsonProcessingException {
        // 1. "인가 코드"로 "액세스 토큰" 요청
        String accessToken = getToken(code); //토큰 받아옴

        // 2. 토큰으로 카카오 API 호출 : "액세스 토큰"으로 "카카오 사용자 정보" 가져오기
        KakaoUserInfoDto kakaoUserInfo = getKakaoUserInfo(accessToken);

        // 3. 필요시에 회원가입
        User kakaoUser = registerKakaoUserIfNeeded(kakaoUserInfo);

        //4. JWT 토큰 반환
        String createToken = jwtUtil.createToken(kakaoUser.getUsername(), kakaoUser.getRole());

        return createToken;
    }

    //인가 코드로 엑세스 토큰 요청하기
    private String getToken(String code) throws JsonProcessingException {
        // 요청 URL 만들기
        URI uri = UriComponentsBuilder
                .fromUriString("https://kauth.kakao.com")
                .path("/oauth/token")
                .encode()
                .build()
                .toUri();

        // HTTP Header 생성
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        // HTTP Body 생성
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", "45b7497ff5e56b1b45edec1ee2dd4d94"); //본인의 REST API Key
        body.add("redirect_uri", "http://localhost:8080/api/user/kakao/callback");
        body.add("code", code);

        RequestEntity<MultiValueMap<String, String>> requestEntity = RequestEntity
                .post(uri)
                .headers(headers)
                .body(body);

        // HTTP 요청 보내기
        ResponseEntity<String> response = restTemplate.exchange(
                requestEntity,
                String.class
        );

        /**
         * JsonNode는 JSON을 Java 객체로 바로 변환하지 않고 탐색 가능한 트리로 만든 것
         */

        // HTTP 응답 (JSON) -> 액세스 토큰 파싱 (인가코드로 토큰 요청 -> 반환 되는 스트링이 토큰형태로 되어있으므로, 파싱해야 함)
        //readTree : Json문자열을 JsonNode 트리 구조로 파싱
        //JsonNode는 JSON 객체의 노드(키/값)을 트리 형태로 탐색할 수 있게 해줌
        JsonNode jsonNode = new ObjectMapper().readTree(response.getBody());
        //루트 노드에서 "acess_token"이라는 키를 찾아 해당 값 노드(JsonNode)를 반환
        //.asText: jsonNode값을 문자열로 반환
        return jsonNode.get("access_token").asText();
    }


    //사용자 정보 요청
    private KakaoUserInfoDto getKakaoUserInfo(String accessToken) throws JsonProcessingException {

        // 요청 URL 만들기
        URI uri = UriComponentsBuilder
                .fromUriString("https://kapi.kakao.com")
                .path("/v2/user/me")
                .encode()
                .build()
                .toUri();

        // HTTP Header 생성
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        RequestEntity<MultiValueMap<String, String>> requestEntity = RequestEntity
                .post(uri)
                .headers(headers)
                .body(new LinkedMultiValueMap<>());

        // HTTP 요청 보내기
        ResponseEntity<String> response = restTemplate.exchange(
                requestEntity,
                String.class
        );

        JsonNode jsonNode = new ObjectMapper().readTree(response.getBody());
        Long id = jsonNode.get("id").asLong();
        String nickname = jsonNode.get("properties")
                .get("nickname").asText();
        String email = jsonNode.get("kakao_account")
                .get("email").asText();

        log.info("카카오 사용자 정보: " + id + ", " + nickname + ", " + email);
        //json으로 받아온 카카오 사용자 데이터를 파싱해서 dto에 넣어줌
        return new KakaoUserInfoDto(id, nickname, email);
    }

    private User registerKakaoUserIfNeeded(KakaoUserInfoDto kakaoUserInfo) {
        // DB 에 중복된 Kakao Id 가 있는지 확인
        Long kakaoId = kakaoUserInfo.getId(); //카카오에서 가져온 유저정보
        //User정보 테이블에 해당유저가 이미 kakaoId가 있는지 확인 (이미 카카오아이디로 회원가입을 했던 적이 있는지)
        User kakaoUser = userRepository.findByKakaoId(kakaoId).orElse(null);

        //DB에 kakaoId가 없다면
        if (kakaoUser == null) {
            // 카카오 사용자 email과 동일한 email로 가입한 회원이 있는지 확인
            String kakaoEmail = kakaoUserInfo.getEmail();
            User sameEmailUser = userRepository.findByEmail(kakaoEmail).orElse(null);
            if (sameEmailUser != null) { //기존 사이트 가입 이메일과 카카오이메일이 같으면
                kakaoUser = sameEmailUser; //이메일 통합
                // 기존 회원정보에 카카오 Id 추가
                kakaoUser = kakaoUser.kakaoIdUpdate(kakaoId);
            } else {
                // 신규 회원가입
                // password: random UUID
                String password = UUID.randomUUID().toString();
                String encodedPassword = passwordEncoder.encode(password);

                // email: kakao email
                String email = kakaoUserInfo.getEmail();

                kakaoUser = new User(kakaoUserInfo.getNickname(), encodedPassword, email, UserRoleEnum.USER, kakaoId);
            }
            //카카오 아이디가 이미 존재하면 카카오 유저정보를 반환(토큰 포함)
            userRepository.save(kakaoUser);
        }
        return kakaoUser;
    }


}
