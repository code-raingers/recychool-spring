package com.app.recychool.api.publicapi;

import com.app.recychool.domain.dto.ApiResponseDTO;
import com.app.recychool.domain.entity.User;
import com.app.recychool.repository.UserRepository;
import com.app.recychool.service.SmsService;
import com.app.recychool.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users/*")
public class UserApi {

    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final SmsService smsService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<ApiResponseDTO> register(@RequestBody User user){
        String encodedPassword = passwordEncoder.encode(user.getUserPassword());
        user.setUserPassword(encodedPassword);
        userService.register(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDTO.of("회원가입이 완료되었습니다")); // 201
    }

    //  이메일 찾기
    @PostMapping("/finds/email")
    public ResponseEntity<ApiResponseDTO> findEmail(@RequestBody User user,HttpSession session){
        // 문자로 인증코드 전송
//        1, 받은 유저의 정보 이름 + 전화번호로 유저를 찾는다.
//        회원 정보가 없을 때
//        3. 해당 유저가 없으면 없음 응답.
        List<String> userEmails = userService.getUserEmailsByNameAndPhone(user);
        if(userEmails.isEmpty()){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponseDTO.of("등록된 회원이 아닙니다."));
        }
//        2. 해당 유저가 있을 때 문자를 보낸 후 응답.
        ApiResponseDTO smsResponse = smsService.sendAuthentificationCodeBySms(user.getUserPhone(), session);
        
        // 응답 데이터에 이메일 정보 포함
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("emails", userEmails);
        if(smsResponse.getData() != null) {
            responseData.put("sms", smsResponse.getData());
        }
        
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponseDTO.of(smsResponse.getMessage() != null ? smsResponse.getMessage() : "인증코드가 전송되었습니다.", responseData));
    }

    // 회원 수정 (이메일 + 패스워드만 받아서 수정)
    @PostMapping("/modify")
    public ResponseEntity<ApiResponseDTO> modify(@RequestBody User user){
        // 이메일로 회원 조회
        Optional<User> foundUser = userRepository.findByUserEmail(user.getUserEmail());

        if (foundUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponseDTO.of("등록된 회원이 아닙니다."));
        }

        // 찾은 회원에 패스워드 인코딩해서 설정
        User existingUser = foundUser.get();
        String encodedPassword = passwordEncoder.encode(user.getUserPassword());
        existingUser.setUserPassword(encodedPassword);
        
        // save
        userRepository.save(existingUser);
        
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponseDTO.of("비밀번호가 변경되었습니다"));
    }

}
