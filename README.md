<div align="center">
  <h1>📅 PF-eZplan Core Service</h1>
  <p>사용자 자연어 입력 및 이미지 인식을 통한 지능형 스케줄링 시스템</p>
</div>

<hr />

## 📖 Introduction

**eZplan**은 복잡한 일정 등록 과정을 AI로 자동화하고 관리해 주는 스마트 캘린더 백엔드 서비스입니다. 텍스트나 이미지를 전송하면 AI가 문맥을 분석하여 일정을 자동으로 파싱 및 분류하며, 구글 OAuth2
기반의 안전한 인증을 제공합니다.

### ✨ Key Features (현재 구현 완료)

- **AI 지능형 파싱:** Gemini 3.1 Flash-Lite를 활용하여 자연어 및 이미지에서 일정(날짜, 시간, 장소, 목적) 자동 추출
- **반복 일정 자동화:** 대학 시간표(학기 기준일 계산), 매주 회의 등 주기적인 반복 일정 자동 세팅
- **맞춤형 온보딩:** 사용자 직업, 수면 시간 등을 반영한 커스텀 유저 프로필 관리
- **보안 및 인증:** Google OAuth2 및 HttpOnly 쿠키 기반의 JWT 인증 처리

---

## 🏗 System Architecture

- **4-Layer Architecture:** Web (Controller) → Service → Domain (Entity) → Infrastructure (Repository)
- **RESTful API:** 프론트엔드(Web)와 완전히 분리된 클라이언트-서버 아키텍처

---

## 🗄 Database Schema (ERD)

공통 추상화 클래스(`BaseEntity`)를 포함한 eZplan의 핵심 데이터베이스 설계입니다.

```mermaid
erDiagram
    USER ||--o{ CATEGORY : owns
    USER ||--o{ SCHEDULE : creates
    USER ||--o{ TEAM_MEMBER : has
    TEAM ||--o{ TEAM_MEMBER : includes
    TEAM ||--o{ SCHEDULE : contains

    USER {
        UUID id PK
        String email UK
        String password
        String provider "LOCAL, GOOGLE"
        String name
        String nickname UK
        String gender
        String age_group
        String job
        String usage_purpose
        Time sleep_time
        Time wake_up_time
        String notification_status
        String system_role "USER, ADMIN"
        String status "ACTIVE, SUSPENDED, WITHDRAWN"
        Timestamp created_at
        Timestamp updated_at
    }

    CATEGORY {
        UUID id PK
        UUID user_id FK
        String name
        String color_code
        Timestamp created_at
    }

    SCHEDULE {
        UUID id PK
        UUID user_id FK
        UUID category_id FK
        UUID team_id FK "nullable"
        UUID group_id
        String repeat_pattern "NONE, DAILY, WEEKLY..."
        Date repeat_end_date
        String title
        String content
        String location
        Date start_date
        Time start_time
        Date end_date
        Time end_time
        Boolean is_all_day
        String status "ACTIVE, COMPLETED, DELETED"
        Integer reminder_minutes
        Timestamp created_at
    }

    TEAM {
        UUID id PK
        String name
        String description
        String invite_code UK
        Timestamp created_at
    }

    TEAM_MEMBER {
        UUID id PK
        UUID team_id FK
        UUID user_id FK
        String team_role "LEADER, MEMBER"
    }
```

---

## 🔄 Detailed Workflows (흐름도 및 시퀀스 다이어그램)

> **💡 아래의 각 항목을 클릭하여 상세 다이어그램을 확인할 수 있습니다.**

<details>
<summary><b>1. 전체 서비스 플로우차트</b></summary>
<br/>

```mermaid
flowchart TD
    %% 색상 정의 (클래스)
    classDef frontend fill:#E3F2FD,stroke:#1E88E5,stroke-width:2px,color:#000;
    classDef backend fill:#E8F5E9,stroke:#43A047,stroke-width:2px,color:#000;
    classDef database fill:#FFF3E0,stroke:#FB8C00,stroke-width:2px,color:#000;
    classDef ai fill:#F3E5F5,stroke:#8E24AA,stroke-width:2px,color:#000;
    classDef api fill:#FFFDE7,stroke:#FBC02D,stroke-width:2px,color:#D32F2F,font-weight:bold;

    subgraph Auth [1. 인증 및 초기 설정]
        A[서비스 접속]:::frontend --> B{로그인 여부 확인}:::frontend
        B -- No --> C[Google OAuth 로그인]:::frontend
        C --> D{추가 정보 존재 여부}:::backend
        D -- No 최초가입 --> E[성별/연령대/직업/용도<br>수면-기상시간/알림 수신 여부<br>입력 폼 렌더링]:::frontend
        E --> F_API(["회원 추가 정보 등록 API<br>PATCH /api/users/additional-info"]):::api
        F_API --> F[(DB에 추가 정보 업데이트)]:::database
        D -- Yes 기존회원 --> G[메인 대시보드 리다이렉트]:::frontend
        F --> G
        B -- Yes --> G
    end

    subgraph Settings [6. 회원 정보 및 카테고리 관리]
        G --> Mypage[설정 및 마이페이지 진입]:::frontend
        Mypage --> EditNick(["닉네임 단건 수정 API<br>PATCH /api/users/nickname"]):::api
        Mypage --> EditInfo(["온보딩 추가 정보 수정 API<br>PATCH /api/users/additional-info"]):::api
        Mypage --> EditCategory(["카테고리 이름, 색상 수정 API<br>PATCH /api/categories/{categoryId}"]):::api
        EditNick --> DB_Setting[(DB 유저 및 카테고리<br>정보 업데이트)]:::database
        EditInfo --> DB_Setting
        EditCategory --> DB_Setting
        DB_Setting -->|새로고침 및 상태 업데이트| G
    end

    subgraph Manage [5. 일정 관리 및 수정]
        G --> Y[기존 일정 상세 클릭]:::frontend
        Y --> Z{액션 선택}:::frontend
        
        Z -- 상태 변경 --> AA(["완료 또는 삭제 API<br>PATCH /api/schedules/id/complete<br>PATCH /api/schedules/id/delete"]):::api
        AA --> AB[(DB 상태 업데이트<br>COMPLETED 또는 DELETED)]:::database
        
        Z -- 수동 수정 --> AC(["수정 폼 입력 및 API<br>PATCH /api/schedules/id"]):::api
        
        Z -- AI 자동 수정 --> AD(["텍스트/이미지 첨부 및 API<br>PATCH /api/schedules/id/parse-and-update"]):::api
        
        AD --> AE[Gemini API 프롬프팅 및 호출]:::backend
        AE --> AE_AI((Gemini AI 모델)):::ai
        AE_AI --> AF_JSON[JSON 형태로 파싱하여 반환]:::backend
        AF_JSON --> AF{파싱 결과 확인}:::backend
        
        AF -- 0건 또는 2건 이상 --> AG[예외 발생 Fail-Fast 방어<br>400 Bad Request]:::backend
        AF -- 정상 1건 --> AH[엔티티 정합성 검증<br>validateUpdateArguments]:::backend
        
        AC --> AH
        AH --> AI[(DB 일정 정보 업데이트)]:::database
    end

    AB -->|새로고침 및 상태 업데이트| G
    AI -->|새로고침 및 상태 업데이트| G
    AG --> Y

    subgraph Monthly [4. 메인 달력 렌더링]
        G --> U(["월간 달력 API<br>GET /api/schedules/monthly"]):::api
        U --> V[(해당 월의 ACTIVE 및<br>COMPLETED 전체 일정 조회)]:::database
        V --> W[월간 데이터 JSON 배열 반환]:::backend
        W --> X[프론트엔드 자체 로직:<br>타겟 주간은 상세 리스트 렌더링<br>나머지 주간은 점으로 렌더링]:::frontend
        X -- 다른 주 클릭 시 화면 갱신 --> X
    end

    subgraph Daily [3. 24시간 생활계획표 렌더링]
        G --> Q(["일간 타임테이블 API<br>GET /api/schedules/daily/export"]):::api
        Q --> Q_DB[(DB 조회: ACTIVE 및<br>COMPLETED 상태 포함)]:::database
        Q_DB --> R[0~23시 시간대별<br>Map 데이터 구조 매핑]:::backend
        R --> S[24시간 배열 프론트로 반환]:::backend
        S --> T[프론트에서 원형 계획표 렌더링]:::frontend
    end

    subgraph Input [2. AI 일정 파싱 및 신규 등록]
        G --> H[일정 추가 창 렌더링]:::frontend
        H --> I[텍스트, 이미지 또는 둘 다 동시 입력<br>multipart/form-data]:::frontend
        I --> J(["AI 파싱 및 자동 생성 API<br>POST /api/schedules/parse-and-create"]):::api
        J --> L[Gemini API 프롬프팅 및 호출<br>기간 및 단일 일정 규칙 적용]:::backend
        L --> M((Gemini AI 모델)):::ai
        M --> N[JSON 형태로 파싱하여 반환]:::backend
        N --> O[(DB에 일정 객체로 자동 저장)]:::database
        O --> P[저장 완료 응답 반환]:::backend
    end

    %% 상태 변경 후 화면 갱신 연결
    P -->|새로고침 및 상태 업데이트| G
```

</details>

<details>
<summary><b>2. Google OAuth 및 온보딩 시퀀스</b></summary>
<br/>

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant Browser as Web Browser (Frontend)
    participant Backend as Spring Boot API
    participant Google as Google OAuth Server
    participant DB as PostgreSQL

    User->>Browser: 구글 로그인 버튼 클릭
    Browser->>Backend: 리다이렉트 (GET /oauth2/authorization/google)
    Backend->>Google: 구글 로그인 페이지 리다이렉트
    Google-->>User: 로그인 화면 제공 및 인증 진행
    User->>Google: 아이디/비밀번호 입력
    Google->>Backend: 인증 성공 (Auth Code 전달)
    
    rect rgb(232, 245, 233)
        note right of Backend: User DB 저장 및 카테고리 세팅
        Backend->>Google: 유저 정보(Email) 요청
        Google-->>Backend: 유저 정보 반환
        Backend->>DB: 기존 회원 여부 조회
        alt 신규 회원인 경우
            Backend->>DB: User 정보 및 기본 카테고리 INSERT
        end
    end
    
    rect rgb(255, 243, 224)
        note right of Backend: 쿠키 굽기 및 웹 리다이렉트
        Backend->>Backend: JWT (Access Token) 생성
        Backend->>Browser: Set-Cookie: access_token=... (HttpOnly)<br/>302 Redirect to /oauth-redirect
    end
    
    Browser->>Browser: 브라우저에 쿠키 자동 저장
    Browser-->>User: 온보딩(추가 정보 입력) 화면 제공
    
    User->>Browser: 성별, 수면시간 등 추가 정보 입력
    Browser->>Backend: PATCH /api/users/additional-info<br/>(Cookie 자동 전송)
    Backend->>DB: User 정보 UPDATE
    Backend-->>Browser: 200 OK
    Browser-->>User: 메인 대시보드 화면으로 이동
```

</details>

<details>
<summary><b>3. Gemini AI 기반 일정 파싱 및 생성 시퀀스</b></summary>
<br/>

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant Browser as Web Browser (Frontend)
    participant Backend as Spring Boot API
    participant Gemini as Gemini AI API
    participant DB as PostgreSQL

    User->>Browser: 텍스트 입력 및 이미지 첨부 (일정 파싱 요청)
    Browser->>Browser: FormData 객체 생성 (text, images)
    
    alt 파싱 결과만 미리보기 (1-A, 1-B, 1-C)
        Browser->>Backend: POST /api/schedules/parse<br/>(multipart/form-data, HttpOnly Cookie 자동 전송)
        Backend->>Gemini: 프롬프트 + 텍스트/이미지 데이터 전송
        Gemini-->>Backend: JSON 형태의 파싱 결과 반환
        Backend-->>Browser: 파싱된 일정 데이터 응답 (200 OK)
        Browser-->>User: 파싱 결과 미리보기 화면 렌더링
    end

    alt 파싱 후 즉시 DB 저장 (2번)
        Browser->>Backend: POST /api/schedules/parse-and-create<br/>(multipart/form-data, HttpOnly Cookie 자동 전송)
        Backend->>Gemini: 프롬프트 + 데이터 전송
        Gemini-->>Backend: JSON 형태의 파싱 결과 반환
        Backend->>DB: 사용자 식별 및 Category 매핑 후 일정 INSERT
        Backend-->>Browser: DB 저장 완료된 일정 객체 응답 (200 OK)
        Browser-->>User: 일정 생성 완료 알림 (Toast)
    end
```

</details>

<details>
<summary><b>4. 메인 달력, 타임테이블 조회 및 CRUD 시퀀스</b></summary>
<br/>

```mermaid
sequenceDiagram
    autonumber
    participant Browser as Web Browser (Frontend)
    participant Backend as Spring Boot API
    participant DB as PostgreSQL

    rect rgb(255, 243, 224)
        note right of Browser: 화면 렌더링을 위한 데이터 조회 (GET)
        Browser->>Backend: 월간 달력 조회<br/>GET /api/schedules/monthly?year=2026&month=6<br/>(Cookie 자동 전송)
        Backend->>DB: 해당 유저의 해당 월 전체 일정 SELECT
        Backend-->>Browser: JSON 배열 반환
        Browser->>Browser: 프론트 자체 로직: 특정 주차 상세 렌더링, 나머지는 점 표시
        
        Browser->>Backend: 24시간 타임테이블 내보내기<br/>GET /api/schedules/daily/export?date=2026-06-10<br/>(Cookie 자동 전송)
        Backend->>DB: 해당 유저의 특정 일자 일정 SELECT
        Backend->>Backend: 0시~23시 슬롯으로 데이터 매핑 로직 수행
        Backend-->>Browser: 24시간 배열 데이터 반환
        Browser->>Browser: 원형 생활계획표 렌더링
    end

    rect rgb(243, 229, 245)
        note right of Browser: 일정 및 카테고리 수동 관리 (POST, PATCH, PUT)
        Browser->>Backend: 수동 일정 생성<br/>POST /api/schedules
        Backend->>DB: Schedule INSERT
        Backend-->>Browser: 200 OK (생성된 ID 반환)

        Browser->>Backend: 일정 내용 전체 수정 (개발 예정)<br/>PUT /api/schedules/{id}
        Backend->>DB: Schedule UPDATE
        Backend-->>Browser: 200 OK
        
        Browser->>Backend: 일정 상태 변경 (완료/삭제)<br/>PATCH /api/schedules/{id}/complete (또는 delete)
        Backend->>DB: Status 컬럼 UPDATE
        Backend-->>Browser: 200 OK

        Browser->>Backend: 카테고리 수정<br/>PATCH /api/categories/{id}
        Backend->>DB: 해당 유저의 카테고리 중복 이름 검증 (SELECT)
        alt 이름 중복 시
            Backend-->>Browser: 400 Bad Request (중복 에러 반환)
        else 정상일 시
            Backend->>DB: Category UPDATE
            Backend-->>Browser: 200 OK
        end
    end
```

</details>

---

## 🚀 DevOps & CI/CD

- **CI:** GitHub Actions - Gradle Build & Test
- **CD:** Docker Image Push → AWS EC2 SSH Access → Docker Compose Up
- **Infrastructure:** AWS EC2 (t3.micro), RDS (PostgreSQL), S3

---

## 🛠 Tech Stack

<p>
  <img src="https://img.shields.io/badge/Spring%20Boot-6DB33F?style=flat-square&logo=springboot&logoColor=white"/>
  <img src="https://img.shields.io/badge/Java%2017-ED8B00?style=flat-square&logo=openjdk&logoColor=white"/>
  <img src="https://img.shields.io/badge/PostgreSQL-4169E1?style=flat-square&logo=postgresql&logoColor=white"/>
  <img src="https://img.shields.io/badge/Redis-DC382D?style=flat-square&logo=redis&logoColor=white"/>
  <img src="https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white"/>
  <img src="https://img.shields.io/badge/AWS-232F3E?style=flat-square&logo=amazonwebservices&logoColor=white"/>
  <img src="https://img.shields.io/badge/Google%20Gemini-8E75B2?style=flat-square&logo=googlegemini&logoColor=white"/>
</p>

---

## 📚 API Documentation

본 프로젝트는 무거운 외부 API 문서화 도구(Swagger, Postman) 대신, IDE 내장 **HTTP Client (`.http`)**를 활용하여 API 명세 및 통신 테스트를 코드로 관리합니다.

* **API 테스트 및 명세 파일 위치:** 프로젝트 내 `.http` 확장자 파일 참고 (예: `schedules.http`, `users.http`, `categories.http`)
* **사용 방법:** IntelliJ IDEA 등의 환경에서 해당 파일을 열고, `@auth_token` 등의 환경 변수를 세팅한 뒤 즉시 API 호출 및 스펙 확인이 가능합니다.

---

## 📌 Future Scope & Known Issues (추후 개선 과제)

- **MSA & CQRS 도입:** PostgreSQL(Command/Write)와 Redis/MongoDB(Query/Read) 분리 설계를 통한 읽기 성능 최적화
- **Event-Driven 아키텍처 전환:** 일정 생성 시 AI 파싱 작업 및 푸시 알림 서비스를 비동기 이벤트(Kafka 등)로 발행하여 결합도 낮추기
