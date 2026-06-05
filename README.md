<!-- PF-Calbak Core Service Documentation -->
<div align="center">
  <h1>📅 PF-Calbak Core Service</h1>
  <p>사용자 자연어 입력 및 이미지 인식을 통한 지능형 스케줄링 시스템</p>
</div>

<hr />

## 🏗 System Architecture (추후 MSA & CQRS)
<ul>
  <li><strong>4-Layer Architecture:</strong> Web (Controller) → Service → Domain (Entity) → Infrastructure (Repository)</li>
  <li><strong>CQRS:</strong> PostgreSQL (Command/Write)와 Redis/MongoDB (Query/Read)의 분리 설계</li>
  <li><strong>Event-Driven:</strong> 일정 생성 시 AI 분석 및 알림 서비스로 비동기 이벤트 발행</li>
</ul>

## 🗄 Database Schema (PostgreSQL)
수정필요
<pre>
Table User {
  id: uuid [pk]
  email: varchar [unique]
  gender: varchar
  age_range: varchar
  created_at: timestamp
}

Table Schedule {
  id: bigint [pk]
  user_id: uuid [ref: > User.id]
  title: varchar
  content: text
  start_at: timestamp
  end_at: timestamp
  location: varchar
  category_id: int
  is_completed: boolean [default: false]
  is_deleted: boolean [default: false]
  repeat_group_id: uuid [note: '반복 일정 그룹화']
}
</pre>

## 🔄 Sequence Diagram (자연어 일정 생성)
<ol>
  <li><strong>User:</strong> 단일/다중 일정 문장 입력 (ex: "6월 7일 3시 강남역 약속")</li>
  <li><strong>Core-Service:</strong> 입력값 검증 및 AI 전송</li>
  <li><strong>Gemini 3.1 Flash-Lite:</strong> 엔티티 추출 (날짜, 시간, 장소, 목적)</li>
  <li><strong>Core-Service:</strong> 03시/15시 모호성 일단 그대로 처리 및 DB 저장</li>
  <li><strong>Core-Service:</strong> 시간순 정렬 데이터 응답</li>
</ol>

## 🚀 DevOps & CI/CD
<ul>
  <li><strong>CI:</strong> GitHub Actions - Gradle Build & Test</li>
  <li><strong>CD:</strong> Docker Image Push → AWS EC2 SSH Access → Docker Compose Up</li>
  <li><strong>Infrastructure:</strong> AWS EC2 (t3.micro), RDS (PostgreSQL), S3</li>
</ul>

## 🛠 Tech Stack
<p>
  <img src="https://img.shields.io/badge/Spring%20Boot-6DB33F?style=flat-square&logo=Spring%20Boot&logoColor=white"/>
  <img src="https://img.shields.io/badge/Java%2017-ED8B00?style=flat-square&logo=java&logoColor=white"/>
  <img src="https://img.shields.io/badge/PostgreSQL-4169E1?style=flat-square&logo=PostgreSQL&logoColor=white"/>
  <img src="https://img.shields.io/badge/Redis-DC382D?style=flat-square&logo=Redis&logoColor=white"/>
  <img src="https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=Docker&logoColor=white"/>
  <img src="https://img.shields.io/badge/AWS-232F3E?style=flat-square&logo=Amazon%20AWS&logoColor=white"/>
</p>
