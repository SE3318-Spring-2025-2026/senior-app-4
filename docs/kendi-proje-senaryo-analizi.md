# Senior Proje Senaryo Analizi — SPMS (Sprint Project Management System)

> **Analiz Tarihi:** 4 Mayıs 2026  
> **Analiz Edilen Senaryo:** JIRA Entegrasyonu → GitHub Bağlantısı → Danışman Paneli'nde "Canlı Notlar" → Otomatik + Manuel Puanlama  
> Tüm dosya içerikleri okunarak hazırlanmıştır.

---

## 1. Proje Mimarisi (Klasör Yapısı)

```
senior-app-4/
├── backend/                         # Spring Boot 3 + PostgreSQL
│   └── src/main/java/com/spms/backend/
│       ├── annotation/              # @AuditableOperation custom annotation
│       ├── aop/                     # AuditLogAspect — otomatik audit loglama
│       ├── client/                  # Dış API istemcileri
│       │   ├── JiraApiClient.java   # JIRA REST API çağrıları
│       │   └── GithubApiClient.java # GitHub OAuth + Org doğrulama
│       ├── config/                  # Spring konfigürasyonları
│       │   ├── GithubProperties.java
│       │   ├── AsyncConfig.java     # @Async için thread pool
│       │   ├── SchedulingConfig.java # @Scheduled aktivasyonu
│       │   └── ...
│       ├── controller/              # REST Controller'lar
│       ├── converter/               # EncryptionConverter (AES ile PAT şifreleme)
│       ├── dto/                     # Request / Response DTO'ları
│       ├── exception/               # Özel exception sınıfları
│       ├── filter/                  # JWT auth filtresi
│       ├── model/                   # JPA Entity'leri
│       ├── repository/              # Spring Data JPA repository'leri
│       └── service/                 # İş mantığı (interface + impl)
│   └── src/main/resources/
│       ├── application.yml          # Uygulama konfigürasyonu
│       └── db/migration/            # Flyway migration SQL dosyaları
│           ├── V1__Create_Committee_Tables.sql
│           ├── V5__Create_Sprint_Table.sql
│           ├── V6__Create_Integration_Tables.sql
│           └── V7__Create_Student_Performance_Table.sql
├── frontend/                        # Next.js 14 (App Router) + TypeScript
│   ├── app/                         # Next.js sayfa route'ları
│   │   ├── groups/[groupId]/
│   │   │   ├── integrations/jira/page.tsx  # JIRA bağlantı sayfası
│   │   │   └── committee-grading/page.tsx  # Komite notlandırma sayfası
│   │   ├── settings/integrations/page.tsx  # Genel entegrasyon ayarları
│   │   └── professor/my-advisees/page.tsx  # Danışman paneli
│   ├── components/                  # Yeniden kullanılabilir bileşenler
│   │   ├── IntegrationStatusIndicator.tsx
│   │   ├── IntegrationSettingsForm.tsx
│   │   ├── LeaderboardTable.tsx     # Öğrenci performans sıralaması
│   │   └── CommitteeGradingDrawer.tsx
│   ├── hooks/
│   │   └── useIntegrationStatus.ts  # Entegrasyon durumu hook'u
│   └── lib/
│       ├── integrations-api.ts      # Entegrasyon API çağrıları
│       ├── groups-api.ts            # Grup API çağrıları
│       └── analytics-api.ts        # EKSIK DOSYA — import var ama dosya yok
└── docs/
    └── api/                         # Swagger/OpenAPI şema dosyaları
```

---

## 2. Bu Senaryoya Ait Dosyalar (Tam Liste)

### Backend — Model/Entity Katmanı
| Dosya | Açıklama |
|-------|----------|
| `model/JiraIntegration.java` | JIRA entegrasyonu entity'si |
| `model/JiraIntegrationStatus.java` | ACTIVE / INACTIVE / ERROR enum |
| `model/GithubIntegration.java` | GitHub entegrasyonu entity'si |
| `model/GithubIntegrationStatus.java` | ACTIVE / INACTIVE / ERROR enum |
| `model/Sprint.java` | Sprint varlığı (tarih, durum) |
| `model/StudentPerformance.java` | Öğrenci performans tablosu |
| `model/Group.java` | Grup entity'si (lider, danışman) |
| `model/SubmissionGrade.java` | Teslim notu |
| `model/GradingCriteria.java` | Notlandırma kriterleri |
| `converter/EncryptionConverter.java` | AES şifreleme dönüştürücü (PAT için) |

### Backend — Repository Katmanı
| Dosya | Açıklama |
|-------|----------|
| `repository/JiraIntegrationRepository.java` | `findByGroup_Id()` metodu |
| `repository/GithubIntegrationRepository.java` | `findByGroup_Id()` metodu |
| `repository/SprintRepository.java` | `findActiveSprintByDate()` custom sorgu |
| `repository/StudentPerformanceRepository.java` | `findByUser_UserId()` metodu |

### Backend — Service Katmanı
| Dosya | Açıklama |
|-------|----------|
| `service/impl/GroupServiceImpl.java` | `bindJiraIntegration()`, `bindGithubIntegration()`, `testIntegrations()` |
| `service/impl/JiraMetricsServiceImpl.java` | JIRA issue sorgulama, temizleme, detay çekme |
| `service/GithubDiscoveryService.java` | Branch - JIRA ID eşleştirme |
| `service/impl/ScrumSyncServiceImpl.java` | Senkronizasyon tetikleme (STUB — içi boş) |
| `service/ScrumSyncScheduler.java` | Günlük saat 02:00 cron zamanlayıcı |
| `service/impl/ActiveSprintServiceImpl.java` | Aktif sprint bulma |
| `service/IntegrationCredentialsService.java` | Token/key döndürme servisi |
| `service/impl/AnalyticsServiceImpl.java` | Liderboard + performans yeniden hesaplama |

### Backend — Controller Katmanı
| Dosya | Açıklama |
|-------|----------|
| `controller/GroupController.java` | `/api/v1/groups/{id}/integrations/jira|github` |
| `controller/JiraMetricsController.java` | `/api/v1/jira-metrics/*` |
| `controller/GithubDiscoveryController.java` | `/api/v1/github/branch-query` |
| `controller/GithubWebhookController.java` | `/api/v1/github/webhook/pr-data` |
| `controller/PrVerificationController.java` | `/api/v1/pr-verification/verify` |
| `controller/StoryPointController.java` | `/api/v1/story-points/validate` |
| `controller/ScrumSyncController.java` | `/api/v1/scrum-sync/trigger` |
| `controller/ScheduleController.java` | `/api/v1/schedules/active-sprint` |
| `controller/SprintDataController.java` | `/api/v1/sprint-data/*` |
| `controller/AnalyticsController.java` | `/api/v1/analytics/leaderboard` |
| `controller/IntegrationController.java` | `/api/v1/integrations/{teamId}/credentials` |

### Backend — Client Katmanı
| Dosya | Açıklama |
|-------|----------|
| `client/JiraApiClient.java` | JIRA REST API v2/v3 istemcisi |
| `client/GithubApiClient.java` | GitHub API OAuth + Org doğrulama |

### Veritabanı Migrations
| Dosya | Açıklama |
|-------|----------|
| `V5__Create_Sprint_Table.sql` | `sprint` tablosu |
| `V6__Create_Integration_Tables.sql` | `github_integrations` + `jira_integrations` tabloları |
| `V7__Create_Student_Performance_Table.sql` | `student_performances` tablosu |

### Frontend
| Dosya | Açıklama |
|-------|----------|
| `app/groups/[groupId]/integrations/jira/page.tsx` | JIRA bağlantı sayfası |
| `app/settings/integrations/page.tsx` | GitHub + JIRA ayarlar sayfası |
| `app/professor/my-advisees/page.tsx` | Danışman listesi (entegrasyon göstergeli) |
| `app/groups/[groupId]/committee-grading/page.tsx` | Komite notlandırma (MOCK veri kullanıyor) |
| `components/IntegrationStatusIndicator.tsx` | Bağlantı durumu göstergesi |
| `components/IntegrationSettingsForm.tsx` | GitHub + JIRA form + Sync Now butonu |
| `components/LeaderboardTable.tsx` | Öğrenci performans tablosu |
| `components/CommitteeGradingDrawer.tsx` | Not giriş drawer'ı |
| `hooks/useIntegrationStatus.ts` | Grup için entegrasyon durumu hook'u |
| `lib/integrations-api.ts` | Tüm entegrasyon API fonksiyonları |

---

## 3. Entity'ler ve Tablolar (Kolonlar Dahil)

### `jira_integrations` tablosu
```sql
id               BIGSERIAL PRIMARY KEY
group_id         BIGINT NOT NULL UNIQUE  → groups(id) FK
jira_space_url   VARCHAR(255) NOT NULL   -- örn: https://team.atlassian.net
api_key          TEXT                    -- AES şifrelenmiş Personal Access Token
project_key      VARCHAR(255) NOT NULL   -- örn: PROJ
status           VARCHAR(50) DEFAULT 'ACTIVE'  -- ACTIVE / INACTIVE / ERROR
last_error       TEXT                    -- son hata mesajı
created_at       TIMESTAMP WITH TIME ZONE
updated_at       TIMESTAMP WITH TIME ZONE
```

### `github_integrations` tablosu
```sql
id                   BIGSERIAL PRIMARY KEY
group_id             BIGINT NOT NULL UNIQUE  → groups(id) FK
organization_name    VARCHAR(255)            -- GitHub org adı (örn: my-org)
github_pat_encrypted TEXT                   -- AES şifrelenmiş PAT
status               VARCHAR(50) DEFAULT 'INACTIVE'  -- ACTIVE / INACTIVE / ERROR
last_error           TEXT
created_at           TIMESTAMP WITH TIME ZONE
updated_at           TIMESTAMP WITH TIME ZONE
```

### `sprint` tablosu
```sql
id           BIGSERIAL PRIMARY KEY
sprint_name  VARCHAR(255) NOT NULL
start_date   DATE NOT NULL
end_date     DATE NOT NULL
status       VARCHAR(50) NOT NULL DEFAULT 'Active'
created_at   TIMESTAMP WITH TIME ZONE
updated_at   TIMESTAMP WITH TIME ZONE
```

### `student_performances` tablosu
```sql
id               BIGSERIAL PRIMARY KEY
user_id          BIGINT UNIQUE NOT NULL  → users(user_id) FK
assigned_sp      INTEGER                -- atanan story point
accomplished_sp  INTEGER                -- tamamlanan story point
performance_ratio DOUBLE PRECISION      -- oran (0.0 - 1.0)
last_updated_at  TIMESTAMP
```

### `groups` tablosu (senaryo ile ilgili kolonlar)
```sql
id          BIGSERIAL PRIMARY KEY
group_name  VARCHAR(100)
leader_id   BIGINT  → users(user_id) FK
advisor_id  BIGINT  → users(user_id) FK (NULL ise danışman atanmamış)
status      VARCHAR(50)  -- FORMING / FORMED / ADVISED / DISBANDED
version     BIGINT       -- optimistic locking
```

### `grades` tablosu (SubmissionGrade entity'si)
```sql
id             BIGSERIAL PRIMARY KEY
submission_id  BIGINT NOT NULL
professor_id   BIGINT NOT NULL
score          DOUBLE PRECISION NOT NULL  -- 0-100
feedback       TEXT
graded_at      TIMESTAMP NOT NULL
```

### `grading_criteria` tablosu
```sql
id               BIGSERIAL PRIMARY KEY
deliverable_type VARCHAR(50)   -- enum: P1/P2/... 
grading_type     VARCHAR(20)   -- enum: SCRUM_PERFORMANCE / CODE_REVIEW / ...
name             VARCHAR(255)
description      TEXT
weight           DOUBLE PRECISION
created_at       TIMESTAMP
created_by       BIGINT
```

---

## 4. API Endpoint'leri (Request / Response Detayları)

### A. JIRA Entegrasyonu Endpoint'leri

#### `POST /api/v1/groups/{groupId}/integrations/jira`
**Açıklama:** Grup liderinin JIRA bağlaması  
**Yetki:** JWT (sadece grup lideri)  
**Request Body:**
```json
{
  "jiraSpaceUrl": "https://team.atlassian.net",
  "apiKey": "ATATT3...",
  "projectKey": "PROJ"
}
```
**Ne Yapar:** JIRA API'sine bağlantı doğrulaması yapar (`/rest/api/3/project/{key}`), başarılıysa şifreleyerek `jira_integrations` tablosuna kaydeder.  
**Response:** `{ "success": true, "message": "JIRA space bound successfully" }`

#### `GET /api/v1/groups/{groupId}/integrations/jira`
**Response:**
```json
{
  "success": true,
  "data": {
    "status": "active",
    "jiraSpaceUrl": "https://...",
    "projectKey": "PROJ",
    "connectedAt": "2026-05-01T10:00:00Z",
    "message": null
  }
}
```

#### `DELETE /api/v1/groups/{groupId}/integrations/jira`
JIRA entegrasyonunu siler.

#### `POST /api/v1/jira-metrics/initialize`
**Açıklama:** Grup ID ile kayıtlı JIRA bağlantısını test eder  
**Request:** `{ "groupId": 1 }`  
**Response:** `{ "connected": true, "jiraSpaceUrl": "...", "projectKey": "...", "message": "..." }`

#### `POST /api/v1/jira-metrics/issue-query`
**Açıklama:** Kayıtlı JIRA bağlantısını kullanarak JQL sorgusu çalıştırır  
**Request:** `{ "groupId": 1, "jql": "project = PROJ AND sprint in openSprints()" }`  
**Response:** `{ "total": 5, "issueKeys": ["PROJ-101", "PROJ-102", ...] }`

#### `POST /api/v1/jira-metrics/issue-details`
**Açıklama:** Issue key listesini alıp JIRA'dan detayları çeker  
**Request:** `{ "jiraSpaceUrl": "...", "apiKey": "...", "issueKeys": ["PROJ-101"] }`  
**Response:**
```json
{
  "total": 1,
  "fetched": 1,
  "issues": [
    { "issueKey": "PROJ-101", "assigneeName": "Ali", "status": "Done", "storyPoints": 5 }
  ]
}
```

#### `POST /api/v1/jira-metrics/callback`
**Açıklama:** Ham JIRA webhook payload'ını temizler  
**Request:** `{ "issues": [ { "key": "PROJ-101", "fields": { ... } } ] }`  
**Response:** `{ "count": 1, "issues": [...] }`

---

### B. GitHub Entegrasyonu Endpoint'leri

#### `POST /api/v1/groups/{groupId}/integrations/github`
**Açıklama:** GitHub PAT + Org adı ile bağlantı kurar  
**Request:** `{ "githubPat": "ghp_...", "organizationName": "my-org" }`  
**Ne Yapar:** `GET /orgs/{org}` ile doğrulama yapar, başarılıysa şifreleyerek kaydeder.  
**Response:** 200 OK (boş gövde)

#### `GET /api/v1/groups/{groupId}/integrations/github`
**Response:**
```json
{
  "success": true,
  "data": {
    "status": "active",
    "organizationName": "my-org",
    "connectedAt": "2026-05-01T10:00:00Z",
    "message": "Connected successfully"
  }
}
```

#### `DELETE /api/v1/groups/{groupId}/integrations/github`
GitHub entegrasyonunu siler.

#### `GET /api/v1/github/branch-query`
**Açıklama:** GitHub'da JIRA ID'ye uyan branch'leri bulur  
**Query Params:** `owner`, `repo`, `jiraIds=PROJ-101,PROJ-102`  
**Ne Yapar:** `GET /repos/{owner}/{repo}/branches` çağırır, her branch'i regex ile JIRA ID ile karşılaştırır.  
**Response:** `[{ "branchName": "feature/PROJ-101-login", "jiraId": "PROJ-101" }, ...]`

#### `POST /api/v1/github/webhook/pr-data`
**Açıklama:** GitHub webhook alıcısı (HMAC SHA-256 imza doğrulamalı)  
**Header:** `X-Hub-Signature-256: sha256=...`  
**Response:** `{ "processed": true/false, "reason": "..." }`

---

### C. Senkronizasyon Endpoint'leri

#### `POST /api/v1/scrum-sync/trigger`
**Açıklama:** Manuel senkronizasyon tetikler (async)  
**Response:** `202 Accepted`
```json
{
  "status": "STARTED",
  "message": "Synchronization pipeline started",
  "syncId": "uuid-...",
  "startedAt": "2026-05-04T10:00:00Z"
}
```
**Önemli Not:** Gerçek senkronizasyon içi **tamamen boş/TODO** — sadece log yazıyor.

#### `GET /api/v1/schedules/active-sprint`
**Açıklama:** Bugünün tarihine göre aktif sprint'i döner  
**Response:** `{ "sprintId": 1, "sprintName": "Sprint 3", "startDate": "...", "endDate": "...", "status": "Active" }`  
**Hata:** 404 — aktif sprint bulunamazsa

---

### D. PR ve Story Point Endpoint'leri

#### `POST /api/v1/pr-verification/verify`
**Request:** `{ "merged": true/false, "status": "closed" }`  
**Response:** `{ "verified": true, "reason": "PR successfully merged." }`

#### `POST /api/v1/story-points/validate`
**Request:** `{ "completedSP": 8, "targetSP": 10 }`  
**Response:** `{ "performanceRatio": 0.8 }`  
**Kurallar:** targetSP=0 → ratio=0.0; ratio>1.0 → 1.0 olarak kırpılır; negatif değerler → 400

#### `POST /api/v1/story-points/merge-status`
**Request:** `{ "merged": true }`  
**Response:** `{ "eligibleForPoints": true, "message": "Task is merged. Points will be calculated." }`

---

### E. Analytics / Liderboard

#### `GET /api/v1/analytics/leaderboard`
**Params:** Pageable (page, size, sort)  
**Response:** Page of `StudentPerformanceDto`
```json
{
  "content": [
    { "studentId": 1, "name": "Ali", "assignedSp": 10, "accomplishedSp": 8, "ratio": 0.8 }
  ],
  "totalPages": 3,
  ...
}
```

#### `POST /api/v1/analytics/recalculate`
**Açıklama:** Tüm öğrencilerin performansını yeniden hesaplar.  
**Önemli Not:** Gerçek veri okuma mantığı **STUB** — her öğrenci için assigned=0, accomplished=0 yazıyor.

---

### F. Entegrasyon Durumu

#### `POST /api/v1/groups/{groupId}/integrations/test`
**Açıklama:** Hem GitHub hem JIRA bağlantısını canlı olarak test eder  
**Response:**
```json
{
  "github": { "connected": true, "message": "Connected" },
  "jira": { "connected": false, "message": "Jira credentials invalid" }
}
```

#### `GET /api/v1/integrations/{teamId}/credentials`
**Header:** `X-Team-Token: ...` (gerekli)  
**Response:** `[{ "type": "JIRA", "token": "...", "projectKey": "...", "organizationName": null }, ...]`  
**Not:** Token doğrulama mantığı çok basit — sadece "EXPIRED_TOKEN" string kontrolü.

---

## 5. İş Mantığı Akışı (Adım Adım Mevcut Durum)

### Adım 1: JIRA Entegrasyonu Bağlama
1. Grup lideri `/settings/integrations` sayfasına girer
2. `IntegrationSettingsForm` bileşeni üzerinden JIRA Space URL + API Token + Project Key girer
3. `bindJiraIntegration()` fonksiyonu `POST /groups/{groupId}/integrations/jira` çağırır
4. `GroupController` → `GroupServiceImpl.bindJiraIntegration()` çağırır
5. Service, `JiraApiClient.validateSpaceConnection()` ile JIRA'ya `GET /rest/api/3/project/{key}` isteği atar
6. Doğrulama başarılıysa `EncryptionConverter` ile AES şifrelenmiş `api_key` `jira_integrations` tablosuna yazılır
7. Başarı bildirimi oluşturulur (system alert)

### Adım 2: GitHub Entegrasyonu Bağlama
1. Aynı form üzerinden GitHub PAT + Org adı girilir
2. `bindGithubIntegration()` → `POST /groups/{groupId}/integrations/github`
3. `GithubApiClient.validateOrganizationAccess()` ile `GET /orgs/{org}` doğrulanır
4. Başarılıysa `github_integrations` tablosuna yazılır

### Adım 3: Danışman Paneli'nde Entegrasyon Durumu Görüntüleme
1. Danışman `/professor/my-advisees` sayfasına girer
2. Her grup satırında `useIntegrationStatus(teamId)` hook'u çağrılır
3. Bu hook `fetchGithubIntegration(groupId)` ve `fetchJiraIntegration(groupId)` çağırır
4. `IntegrationStatusIndicator` bileşeni "Connected" / "No Connection" gösterir

### Adım 4: JIRA'dan Active Sprint Issue'larını Çekme (KISMİ)
1. `POST /api/v1/jira-metrics/issue-query` ile JQL sorgusu gönderilebilir
2. Service kayıtlı JIRA token'ını kullanarak JIRA API'sine sorgu atar
3. Issue key listesi döner (ör: `["PROJ-101", "PROJ-102"]`)
4. `POST /api/v1/jira-metrics/issue-details` ile her key için detaylar alınabilir
5. **NOT:** Bu endpoint'ler bağımsız çalışıyor — otomatik sprint bağlantısı yok

### Adım 5: GitHub Branch Sorgusu (KISMİ)
1. `GET /api/v1/github/branch-query?owner=...&repo=...&jiraIds=PROJ-101`
2. `GithubDiscoveryService` GitHub API'den tüm branch listesini çeker
3. Her branch için regex ile JIRA ID varlığını kontrol eder
4. Eşleşen `BranchMatchDto` listesini döner
5. **NOT:** PR durumu (merged/not merged) bu endpoint'te kontrol edilmiyor

### Adım 6: Senkronizasyon Tetikleme (STUB)
1. Kullanıcı "Sync Now" butonuna tıklar veya günlük saat 02:00'da cron çalışır
2. `ScrumSyncController` → `ScrumSyncServiceImpl.triggerSync()` çağrılır
3. Async olarak `executeSyncPipeline()` çalışır
4. **GERÇEK İÇERİK YOK** — Sadece logger çıktısı + 4 adet TODO comment

### Adım 7: Performans Hesaplama (STUB)
1. `POST /api/v1/analytics/recalculate` çağrılır
2. `AnalyticsServiceImpl.recalculateAllPerformances()` tüm student'ları gezar
3. **GERÇEK JIRA/GitHub VERİSİ YOK** — Her öğrenci için `assigned=0, accomplished=0` yazıyor
4. Ratio her zaman `0.0` olarak hesaplanır

### Adım 8: Liderboard Görüntüleme
1. `LeaderboardTable` bileşeni `GET /api/v1/analytics/leaderboard` çağırır
2. `StudentPerformance` tablosundaki veriler sayfalı olarak gösterilir
3. Sıralama: ratio / assignedSp / accomplishedSp / name
4. **Veri kalitesi sorunu:** Recalculate stub olduğu için tüm değerler 0

### Adım 9: Manuel Not Girme (Danışman)
1. Danışman `/groups/{groupId}/committee-grading` sayfasına girer
2. **UYARI:** Bu sayfa gerçek API kullanmıyor — `mockGroups` ve `mockCommitteeSubmissions` mock verileri kullanıyor
3. Grading Drawer açılıyor, not giriliyor
4. `POST /api/v1/submissions/{id}/grades` gerçek API çağrısı yapılıyor (tek gerçek nokta)
5. **SENARYO EKSİKLİĞİ:** Scrum Performansı ve Code Review için A/B/C/D/F girişi yok — sadece 0-100 sayısal not

---

## 6. Eksikler (Neler Henüz Yok)

### 6.1 ScrumSyncService Gerçek İmplementasyonu
`ScrumSyncServiceImpl.executeSyncPipeline()` tamamen boş (sadece TODO'lar):
```java
// TODO: Implement active sprint lookup from database
// TODO: Use jiraApiClient to fetch sprint issues  
// TODO: Use githubApiClient to fetch repository data
// TODO: Implement data merge and persistence logic
```
**Beklenen:** Her grup için kayıtlı JIRA'dan aktif sprint issue'larını çekip, her issue key için GitHub'da branch araması yapıp, PR durumunu (merged/not merged) kontrol edip, `StudentPerformance` tablosuna yazması gerekiyor.

### 6.2 AnalyticsService Gerçek İmplementasyonu
`recalculateAllPerformances()` içinde gerçek veri okuma yok:
```java
int assigned = 0;     // Sabit
int accomplished = 0; // Sabit
```
**Beklenen:** JIRA'daki atanan story point + GitHub'da merged PR'lara karşılık gelen tamamlanan story point ile güncellenmeli.

### 6.3 Danışman Paneli "Canlı Not" Ekranı
- Senaryo: Danışman panelinde "live grades" gösterilmesi bekleniyor
- Mevcut durum: Yalnızca entegrasyon bağlantı durumu (Connected/No Connection) gösteriliyor
- **Eksik:** Story point tamamlanma oranı, PR merge durumu, otomatik "katsayı" (coefficient), her öğrencinin yaptığı iş — bunların hiçbiri danışman panelinde gösterilmiyor

### 6.4 PR Status Check (PR Merge Durumu GitHub'dan Çekme)
- `GithubDiscoveryService` sadece branch eşleştiriyor
- Branch'e ait PR var mı, merged mı kontrol etmiyor
- GitHub API `GET /repos/{owner}/{repo}/pulls` veya `GET /repos/{owner}/{repo}/commits/{sha}/check-runs` çağrısı yok

### 6.5 Scrum Performansı ve Code Review Notu (A/B/C/D/F)
- `GradingCriteria` entity'si var, `GradingType` enum'u var ama danışman için A/B/C/D/F formatında özel not girişi yok
- Frontend'de sadece 0-100 sayısal not girişi var
- **Beklenen:** `grading_type = SCRUM_PERFORMANCE` ve `grading_type = CODE_REVIEW` için ayrı A/B/C/D/F dropdown'ları

### 6.6 Otomatik Katsayı (Coefficient/Allowance) Hesaplama
- Story point tamamlanma oranına göre takım notuna uygulanacak "allowance" hesabı yok
- `StoryPointController.validate()` ratio hesaplıyor ama bu değer hiçbir nota bağlanmıyor
- Grup notuna çarpılacak katsayı mantığı hiçbir yerde yok

### 6.7 `analytics-api.ts` Frontend Dosyası
- `LeaderboardTable.tsx`: `import { fetchLeaderboard, StudentPerformanceDto, LeaderboardResponse } from "@/lib/analytics-api"`
- **`/frontend/lib/analytics-api.ts` dosyası mevcut değil** — bu import hata verecek

### 6.8 Group-Specific Sprint Bağlantısı
- `sprint` tablosu var ama `group_id` kolonu yok
- Sprint ile grup arasında ilişki tanımlanmamış
- Hangi grup hangi sprint'te hangi JIRA projesinden issue alıyor bilgisi bulunmuyor

### 6.9 Advisor Grading Dashboard (Advisor Paneli Tam Görünümü)
- Advisor panelinde sadece "entegrasyon bağlı mı" gösteriliyor
- Beklenen: Her grup için sprint ilerleme durumu, kaç story point tamamlandı, PR merge yüzdesi, öğrenci başına performans

---

## 7. Yarım Kalanlar (Kısmen Yapılmış)

### 7.1 GithubApiClient — Branch/PR Sorgusu Eksik
`GithubApiClient.java` sadece şunları yapıyor:
- OAuth code → access token dönüşümü
- GitHub kullanıcı bilgisi çekme
- Org erişim doğrulama

**Eksik metodlar:**
- `fetchBranchesForRepo(owner, repo, pat)` — PR query için gerekli
- `fetchPullRequestByBranch(owner, repo, branchName, pat)` — PR merged mi?

### 7.2 GithubDiscoveryService — Token Kullanmıyor
```java
// Mevcut (anonim istek — rate limit sorunlu):
response = restTemplate.exchange(url, HttpMethod.GET, null, ...);
```
GitHub API token olmadan saniyede 60 istek limiti. Kayıtlı GitHub PAT kullanılmıyor.

### 7.3 IntegrationController — Token Doğrulama Sahte
```java
private boolean isValidTeamToken(String token) {
    return token != null && !token.isEmpty() && !token.equalsIgnoreCase("EXPIRED_TOKEN");
}
```
Gerçek bir token doğrulaması yok — "EXPIRED_TOKEN" string'i dışındaki her değer geçerli kabul ediliyor.

### 7.4 JiraApiClient — Sadece v2 ve v3 Karışık Kullanım
- `validateSpaceConnection` → `/rest/api/3/project/{key}` (v3)
- `fetchIssuesBatch` → `/rest/api/2/search` (v2)
- `searchIssuesByJql` → `/rest/api/2/search` (v2)

JIRA Cloud artık v3'e geçiş yapıyor, v2 bazı müşterilerde çalışmayabilir.

### 7.5 EncryptionConverter — Sabit Anahtar (Güvenlik Açığı)
```java
private static final byte[] KEY = "MySuperSecretKey1234567890123456".getBytes();
```
Yorum satırında "Gerçek projede @Value ile application.yml'den al" diyor ama implementasyon yapılmamış. Key hardcoded durumda.

### 7.6 CommitteeGradingPage — Mock Veri Kullanıyor
```tsx
// TODO(#74): replace mock submissions with real Process 3 professor-facing
// list/detail endpoints when submission retrieval is implemented.
const submissions = mockCommitteeSubmissions.filter(...)
```
Sayfa tamamen mock data ile çalışıyor, gerçek API çağrısı yok.

### 7.7 ScrumSyncScheduler — Konfigürasyonu Yanlış Okuyor
```java
@Value("${scrum.sync.cron:0 0 2 * * *}")
private String syncCronExpression;
```
`syncCronExpression` field'ı okunuyor ama `@Scheduled` annotation'daki cron expression doğrudan `${scrum.sync.cron:0 0 2 * * *}` ile property'den okunuyor. Field hiç kullanılmıyor. Tutarsızlık ama çalışıyor.

---

## 8. Yanlışlar veya Tutarsızlıklar

### 8.1 JiraIntegration — `jira_space_url` + `project_key` Gönderilmiyor Frontend'den
`JiraBindingRequest` record'u şunları bekliyor:
```java
String jiraSpaceUrl,   // zorunlu
String apiKey,         // isteğe bağlı
String projectKey      // zorunlu
```

Ancak `/groups/[groupId]/integrations/jira/page.tsx` sadece `spaceUrl` ve `apiKey` gönderiyor:
```tsx
const res = await apiClient.post(`/groups/${groupId}/integrations/jira`, {
    spaceUrl: spaceUrl.trim(),     // hatalı alan adı → jiraSpaceUrl olmalı
    apiKey: apiKey.trim()          // projectKey eksik!
});
```
**Sonuç:** Bu sayfa üzerinden bağlama işlemi çalışmıyor. `IntegrationSettingsForm.tsx` doğru `{ jiraSpaceUrl, apiKey, projectKey }` gönderiyor.

### 8.2 GithubIntegration — PAT "Şifrelenmiş" Ama Açık Yazıldı
```java
integration.setGithubPatEncrypted(request.githubPat().trim()); // şifreleme yok
```
Alan adı `githubPatEncrypted` ama `EncryptionConverter` uygulanmamış. JiraIntegration `api_key` için `@Convert(converter = EncryptionConverter.class)` annotation'ı var, GithubIntegration için de var ama aşağıdaki kolonda:
```java
@Convert(converter = EncryptionConverter.class)
@Column(name = "github_pat_encrypted")
private String githubPatEncrypted;
```
Bu doğru görünüyor — hem JIRA hem GitHub için AES şifreleme annotation'ı mevcut. Ancak `EncryptionConverter`'daki sabit KEY güvenlik açığı bu ikisi için de geçerli.

### 8.3 SprintDataController — Sprint ile İlgisi Yok
`SprintDataController` adı "sprint verisi" olduğunu düşündürüyor, ama aslında `ValidStudentId` tablosunu güncelleme (bulk update) ve listeleme yapıyor. Sprint tablosuyla hiç alakası yok. İsimlendirme yanıltıcı.

### 8.4 `GithubDiscoveryController.startDiscovery()` — Dummy Endpoint
```java
@PostMapping("/github-discovery/start")
public ResponseEntity<DiscoveryResponse> startDiscovery() {
    return ResponseEntity.ok(new DiscoveryResponse("STARTED", "Discovery workflow initiated."));
}
```
Herhangi bir iş mantığı yok — her zaman "STARTED" döner. `DiscoveryRequest` parametresi bile alınmıyor.

### 8.5 `PrVerificationController.getBranchList()` — Hardcoded Mock
```java
@PostMapping("/branch-list")
public ResponseEntity<?> getBranchList() {
    return ResponseEntity.ok(Map.of(
        "branches", List.of("main", "feature/PROJ-123", "bugfix/PROJ-456")
    ));
}
```
Hiçbir parametreye bağlı olmayan, her zaman aynı 3 branch döndüren mock endpoint.

### 8.6 `AnalyticsServiceImpl.recalculateAllPerformances()` — Issue #14 Referansı Tutarsız
```java
// Real integration logic will go here
int assigned = 0; 
int accomplished = 0;
```
Kod yorumu "Issue #14 Math Engine Core Logic" diyor ama math engine (`StoryPointController.validate()`) gerçekten çalışıyor. Sorun math engine değil, math engine'e beslenecek verinin hiç çekilmemesi.

### 8.7 `useIntegrationStatus` Hook — `connectedAt` Alanı Backend'den Gelmiyor
Hook `github.data?.connectedAt` arıyor, ama `GithubIntegrationResponse` içinde:
```java
new GithubIntegrationResponse.GithubIntegrationData(
    github.getStatus()...,
    github.getOrganizationName(),
    github.getCreatedAt().toString(),   // ← "connectedAt" değil, "createdAt"
    ...
)
```
Frontend `connectedAt` bekliyor, backend `createdAt` döndürüyor. Alan adı uyuşmazlığı nedeniyle "Never connected" görüntüleniyor.

### 8.8 `GradingCriteria` — Advisor için Özel Notlar Tanımlanmamış
`GradingType` enum'u var ama içeriği bilinmiyor (dosya verilmedi). `GradeSubmissionRequest` sadece `Double grade` (0-100) alıyor, A/B/C/D/F formatına destek yok.

---

## 9. Mevcut vs Beklenen Karşılaştırma Tablosu

| Senaryo Adımı | Beklenen | Mevcut Durum | Durum |
|---------------|----------|--------------|-------|
| Takım lideri JIRA PAT + Space girer | Form + doğrulama + şifreli kayıt | `IntegrationSettingsForm.tsx` çalışıyor; `/groups/{id}/integrations/jira` endpoint'i çalışıyor | ✅ ÇALIŞIYOR |
| JIRA bağlantısı doğrulanır | API'ye `/rest/api/3/project/{key}` atılır | `JiraApiClient.validateSpaceConnection()` mevcut | ✅ ÇALIŞIYOR |
| GitHub PAT + Org bağlanır | PAT ile org erişim doğrulanır | `GithubApiClient.validateOrganizationAccess()` + `bindGithubIntegration()` mevcut | ✅ ÇALIŞIYOR |
| Aktif sprint issue'ları çekilir | Günlük otomatik veya manuel tetikleme ile JIRA'dan sprint issue key'leri alınır | `issue-query` endpoint'i çalışıyor ama senkronizasyon pipeline'a bağlı değil | ⚠️ KISMİ |
| Her issue key için GitHub'da branch aranır | `feature/PROJ-101-*` gibi branch'ler tespit edilir | `GithubDiscoveryService.matchBranchesWithJiraIds()` çalışıyor ama token kullanmıyor | ⚠️ KISMİ |
| PR durumu (merged/not merged) kontrol edilir | PR açık mı, merged mı? | `PrVerificationController.verifyPrStatus()` manuel veri ile çalışıyor, GitHub'dan otomatik çekmüyor | ❌ EKSİK |
| Tamamlanan story point hesaplanır | Merged PR → SP puanına dahil edilir, merge edilmemiş → dahil değil | `StoryPointController.validate()` math çalışıyor ama gerçek veri beslenmiyor | ⚠️ KISMİ |
| Senkronizasyon pipeline çalışır | JIRA + GitHub veri birleştirilir, DB'ye yazılır | `ScrumSyncServiceImpl` tamamen STUB — sadece TODO'lar | ❌ EKSİK |
| Öğrenci performansı hesaplanır | `student_performances` tablosu güncellenir | `recalculateAllPerformances()` her şeyi 0 yazıyor | ❌ EKSİK |
| Danışman panelinde canlı notlar gösterilir | SP tamamlanma, PR durumu, öğrenci başına breakdown | Sadece "Connected/No Connection" gösteriliyor | ❌ EKSİK |
| Otomatik katsayı (allowance) hesabı | SP oranına göre grup notuna çarpan hesabı | Hiçbir yerde yok | ❌ EKSİK |
| Danışman Scrum Performans notu girer (A/B/C/D/F) | Advisor panelinde A-F dropdown | Yok — sadece 0-100 sayısal not | ❌ EKSİK |
| Danışman Code Review notu girer (A/B/C/D/F) | Advisor panelinde A-F dropdown | Yok | ❌ EKSİK |
| Liderboard öğrenci sıralaması | Gerçek SP verisiyle güncel tablo | Tablo UI çalışıyor ama veriler hep 0 | ⚠️ KISMİ |
| Günlük otomatik senkronizasyon | Her gece 02:00'da JIRA+GitHub sync | Scheduler çalışıyor ama pipeline boş | ⚠️ KISMİ |
| frontend `analytics-api.ts` | `fetchLeaderboard` fonksiyonu | **Dosya mevcut değil** — import hatası | ❌ EKSİK |

---

## Özet Bulgular

**Sağlıklı Çalışan Parçalar:**
- JIRA ve GitHub entegrasyonunu kaydetme / silme akışı
- Entegrasyon bağlantı doğrulama (JiraApiClient + GithubApiClient)
- Danışman panelinde entegrasyon bağlantı göstergesi (Connected/No Connection)
- JIRA'dan JQL ile issue key listesi çekme (manuel endpoint)
- GitHub branch araması JIRA ID ile eşleştirme (manuel endpoint)
- PR merge durumu doğrulama mantığı (manuel endpoint, gerçek veritabanı bağlantısı yok)
- Story Point ratio hesaplama (math engine doğru)
- Liderboard UI bileşeni (veri kalitesi sorunu var)

**Kritik Eksikler:**
1. `ScrumSyncServiceImpl.executeSyncPipeline()` — tamamen boş
2. `AnalyticsServiceImpl.recalculateAllPerformances()` — gerçek veri bağlantısı yok
3. Danışman paneli "canlı not" görünümü yok
4. A/B/C/D/F formatında Scrum Performans + Code Review notu
5. `frontend/lib/analytics-api.ts` dosyası eksik
6. PR status otomatik kontrol yok
7. Otomatik katsayı hesabı yok
