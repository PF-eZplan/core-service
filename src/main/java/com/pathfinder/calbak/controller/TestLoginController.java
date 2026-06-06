package com.pathfinder.calbak.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TestLoginController {
    @GetMapping("/") // 로컬호스트 첫 화면 접속 시
    public String loginPage() {
        return "login"; // login.html 렌더링
    }
}
