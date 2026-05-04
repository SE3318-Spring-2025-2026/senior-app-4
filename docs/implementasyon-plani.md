# Senaryo Implementasyon Planı — Senior App 4

> **Tarih:** 2026-05-04  
> **Amaç:** JIRA Entegrasyonu → GitHub Branch/PR Kontrolü → Danışman Veri Gösterim Paneli → Otomatik Senkronizasyon senaryosunu bu projede çalışır hale getirmek.  
> **Referans:** senior-app-6-senaryo-analizi.md (iş mantığı kaynağı)  
> **Kural:** Referans projenin isimlerini, klasörlerini, entity'lerini kopyalamak yok. Bu projenin mevcut yapısına adapte et.

---

## Mevcut Durumun Özeti

Projede şu anda:
- JIRA ve GitHub bağlantısını kaydetme / silme → **çalışıyor**
- Danışman panelinde "bağlı mı?" göstergesi → **çalışıyor ama buggy**
- JIRA'dan issue key çekme (manuel endpoint) → **kısmen çalışıyor**
- GitHub'da branch eşleştirme → **kısmen çalışıyor, token kullanmıyor**
- Senkronizasyon pipeline'ı (`ScrumSyncServiceImpl`) → **tamamen boş**
- Danışman panelinde veri gösterim (canlı veriler, öğrenci bazlı breakdown) → **hiç yok**

---

## Plan Yapısı

| Faz | Başlık | Öncelik |
|-----|--------|---------|
| 0 | Kırık Parçaları Düzelt | ÖNCE YAPILMALI |
| 1 | Sprint Issue Takip Tablosu (Yeni) | Temel altyapı |
| 2 | Senkronizasyon Pipeline'ını Gerçekleştir | Çekirdek iş mantığı |
| 3 | GitHub PR Kontrol Yeteneğini Ekle | Pipeline için gerekli |
| 4 | Danışman Panelini Genişlet (Veri Gösterim) | Frontend |
| 5 | Eksik Frontend Dosyasını Oluştur | Derleme hatası düzeltme |

---

## FAZ 0 — Kırık Parçaları Düzelt

Bu faz, mevcut kodu düzeltir. Yeni özellik değil, bugfix.

---

### 0.1 Frontend JIRA Formu Alan Adı Uyumsuzluğu

**Sorun:** `/groups/[groupId]/integrations/jira/page.tsx` backend'in beklediği alanları yanlış gönderiyor.

**Mevcut (hatalı):**
```tsx
apiClient.post(`/groups/${groupId}/integrations/jira`, {
    spaceUrl: spaceUrl.trim(),   // ← hatalı alan adı
    apiKey: apiKey.trim()        // ← projectKey eksik
});
```

**Olması gereken:**
```tsx
apiClient.post(`/groups/${groupId}/integrations/jira`, {
    jiraSpaceUrl: spaceUrl.trim(),
    apiKey: apiKey.trim(),
    projectKey: projectKey.trim()   // ← form'a bu input eklenmeli
});
```

**Yapılacak:**
- `page.tsx`'e `projectKey` state'i ve input alanı ekle
- Gönderilen objedeki `spaceUrl` → `jiraSpaceUrl` olarak düzelt
- `projectKey` gönder

---

### 0.2 useIntegrationStatus Hook — Alan Adı Uyumsuzluğu

**Sorun:** Hook `connectedAt` bekliyor, backend `createdAt` döndürüyor. "Never connected" her zaman görünüyor.

**Dosya:** `frontend/hooks/useIntegrationStatus.ts`

**Yapılacak seçeneklerden biri:**
- Ya backend `GithubIntegrationResponse` ve `JiraIntegrationResponse`'da alan adını `connectedAt` yap
- Ya da hook'u `github.data?.createdAt` okuyacak şekilde güncelle

Backend tarafı (tercih edilir — tutarlılık için):  
`GithubIntegrationResponse` içindeki `createdAt` → `connectedAt` olarak yeniden adlandır.  
`JiraIntegrationResponse` için de aynısı.

---

### 0.3 GithubDiscoveryService — Kayıtlı PAT Kullan

**Sorun:** `GithubDiscoveryService.matchBranchesWithJiraIds()` anonim HTTP isteği atıyor. GitHub'ın anonim rate limiti 60 istek/saat — bu production'da çalışmaz.

**Mevcut:**
```java
response = restTemplate.exchange(url, HttpMethod.GET, null, ...);
```

**Yapılacak:**
```java
// 1. groupId parametresi ekle
// 2. githubIntegrationRepository.findByGroup_Id(groupId) ile kayıtlı entegrasyonu bul
// 3. EncryptionConverter ile PAT'ı çöz
// 4. Authorization: Bearer {pat} header'ı ile istek at
HttpEntity<Void> entity = new HttpEntity<>(buildGithubHeaders(decryptedPat));
response = restTemplate.exchange(url, HttpMethod.GET, entity, ...);
```

**Etkilenen dosyalar:**
- `GithubDiscoveryService.java` — metod imzasına `groupId` ekle
- `GithubDiscoveryController.java` — `groupId` query param ekle
- `EncryptionConverter.java` — bir sonraki maddede düzeltiliyor

---

### 0.4 EncryptionConverter — Sabit Anahtarı Konfigürasyona Taşı

**Sorun:** AES anahtarı kod içinde hardcoded.

**Mevcut:**
```java
private static final byte[] KEY = "MySuperSecretKey1234567890123456".getBytes();
```

**Yapılacak:**
`EncryptionConverter` Spring bean haline getirilmeli (şu an `@Converter` + `autoApply` kullanıyor, `@Value` enjekte edilemiyor).

Çözüm: `EncryptionService` adında ayrı bir Spring `@Service` yaz, şifreleme/çözme mantığını buraya taşı. `application.yml`'dan key oku:
```yaml
app:
  encryption:
    key: ${ENCRYPTION_KEY}  # Base64 encoded 32 byte key
```

`EncryptionConverter`, `EncryptionService`'i çağıracak şekilde güncelle.

---

### 0.5 GithubIntegration — PAT Şifreleme Kontrolü

**Sorun:** `GithubIntegration` entity'sinde `@Convert(converter = EncryptionConverter.class)` annotation var, ama service'te:
```java
integration.setGithubPatEncrypted(request.githubPat().trim()); // şifreleme annotation'a bırakılmış
```

0.4 tamamlandıktan sonra bu doğru çalışacak. Ancak bir kez kontrol et — eğer PAT düz metin kaydedildiyse mevcut kayıtlar silinip tekrar bağlanmalı.

---

## FAZ 1 — Sprint Issue Takip Tablosu

Bu projede senkronizasyon pipeline'ının sonuçlarını saklayacak tablo yok.

### 1.1 Yeni Flyway Migration: `V8__Create_Sprint_Issue_Tracking_Table.sql`

```sql
CREATE TABLE sprint_issue_tracking (
    id                      BIGSERIAL PRIMARY KEY,
    group_id                BIGINT NOT NULL REFERENCES groups(id),
    sprint_id               BIGINT NOT NULL REFERENCES sprint(id),
    issue_key               VARCHAR(50) NOT NULL,
    story_points            INTEGER,
    assignee_github_username VARCHAR(100),
    pr_number               BIGINT,
    pr_merged               BOOLEAN,       -- null=branch yok, false=PR açık/yok, true=merged
    synced_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_sprint_issue UNIQUE (group_id, sprint_id, issue_key)
);

CREATE INDEX idx_sit_group_sprint ON sprint_issue_tracking(group_id, sprint_id);
```

**Neden bu tablo gerekli:**
- Pipeline her çalıştığında JIRA'dan + GitHub'dan çekilen veriyi buraya yazar
- Danışman paneli bu tabloyu okur → "canlı not" gösterir
- Öğrenci performansı bu tablodan hesaplanır (merged PR'ların SP toplamı)
- `student_performances` tablosu bu tablonun özeti/aggregation'ı olur

---

### 1.2 Yeni Entity: `SprintIssueTracking.java`

**Paket:** `com.spms.backend.model`

```java
@Entity
@Table(name = "sprint_issue_tracking")
public class SprintIssueTracking {
    @Id @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "sprint_id", nullable = false)
    private Sprint sprint;

    @Column(name = "issue_key", nullable = false)
    private String issueKey;

    @Column(name = "story_points")
    private Integer storyPoints;

    @Column(name = "assignee_github_username")
    private String assigneeGithubUsername;

    @Column(name = "pr_number")
    private Long prNumber;

    @Column(name = "pr_merged")
    private Boolean prMerged;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;
}
```

---

### 1.3 Yeni Repository: `SprintIssueTrackingRepository.java`

```java
public interface SprintIssueTrackingRepository extends JpaRepository<SprintIssueTracking, Long> {
    List<SprintIssueTracking> findByGroup_IdAndSprint_Id(Long groupId, Long sprintId);
    void deleteByGroup_IdAndSprint_Id(Long groupId, Long sprintId);
}
```

---

## FAZ 2 — Senkronizasyon Pipeline'ını Gerçekleştir

Bu fazdaki tüm değişiklikler `ScrumSyncServiceImpl.executeSyncPipeline()` içine gider. Şu an dört TODO var; bunların hepsi implemente edilecek.

---

### 2.1 Pipeline Giriş Noktası

**Dosya:** `service/impl/ScrumSyncServiceImpl.java`

Pipeline şu adımları takip eder:

```
executeSyncPipeline()
  ↓
1. Aktif sprint'i bul
  ↓
2. Her grup için (JIRA + GitHub bağlı olanlar):
    a. JIRA'dan sprint issue'larını çek
    b. Her issue için GitHub'da branch ara
    c. Branch için PR durumunu kontrol et
    d. sprint_issue_tracking tablosuna yaz
```

---

### 2.2 Adım 1 — Aktif Sprint Bulma

Mevcut `ActiveSprintServiceImpl.findActiveSprint()` kullanılır:
```java
Sprint activeSprint = activeSprintService.findActiveSprint()
    .orElseThrow(() -> new IllegalStateException("No active sprint found"));
```

---

### 2.3 Adım 2a — JIRA'dan Sprint Issue'larını Çekme

Mevcut `JiraApiClient` genişletilmeli. Şu an `/rest/api/2/search` ile JQL sorgusu yapabiliyor. Buna ek olarak **Agile API üzerinden aktif sprint issue'ları** çekmek gerekiyor.

**`JiraApiClient`'a eklenecek metod:**
```java
public List<JiraIssueData> fetchActiveSprintIssues(String baseUrl, String email, String apiToken, String projectKey) {
    // Adım 1: Board ID bul
    // GET {baseUrl}/rest/agile/1.0/board?projectKeyOrId={projectKey}
    // → boardId = response.values[0].id

    // Adım 2: Aktif sprint ID bul
    // GET {baseUrl}/rest/agile/1.0/board/{boardId}/sprint?state=active
    // → jiraSprintId = response.values[0].id

    // Adım 3: Sprint issue'larını sayfalı çek
    // GET {baseUrl}/rest/agile/1.0/sprint/{jiraSprintId}/issue?startAt={n}
    // Her issue'dan: key, fields.customfield_10016 (story points), fields.assignee.displayName
}
```

**Auth header:** `Basic Base64(email:token)` — mevcut `JiraApiClient.buildAuthHeader()` benzeri.

**Yeni DTO:** `JiraIssueData` (record veya class)
```java
record JiraIssueData(
    String issueKey,        // ör. "PROJ-101"
    Integer storyPoints,    // customfield_10016, null olabilir
    String assigneeEmail    // sadece referans, asıl eşleştirme GitHub PR'dan gelir
)
```

**Güvenlik notu:** JIRA'dan boş liste gelirse mevcut `sprint_issue_tracking` kayıtları **silinmez** (JIRA arıza koruması).

---

### 2.4 Adım 2b — GitHub'da Branch Arama

Mevcut `GithubDiscoveryService.matchBranchesWithJiraIds()` bunu yapıyor ama:
- Anonim istek atıyor (0.3'te düzeltilecek)
- Sadece tek issue key için değil, liste için çalışıyor

Pipeline içinde şöyle kullanılır:
```java
// Her issue key için branch bul
Optional<String> branchName = githubDiscoveryService.findBranchForIssueKey(
    orgName, repoName, issueKey, decryptedPat
);
```

**`GithubDiscoveryService`'e eklenecek:** `findBranchForIssueKey()` metodu.  
Eşleştirme kuralları (mevcut regex genişletilir):
- `branchName.contains(issueKey + "-")`
- `branchName.contains(issueKey + "/")`
- `branchName.endsWith(issueKey)`
- `branchName.equals(issueKey)`

---

### 2.5 Adım 2c — PR Durumu Kontrol Etme

**Bu yetenek şu an yok.** Faz 3'te `GithubApiClient`'a eklenecek.

Pipeline'da şöyle kullanılır:
```java
Optional<PrCheckResult> pr = githubApiClient.findMergedPrForBranch(
    orgName, repoName, branchName, decryptedPat
);
```

`PrCheckResult` record'u:
```java
record PrCheckResult(
    Long prNumber,
    boolean merged,         // state=="closed" && merged_at!=null
    String authorGithubUsername  // PR yazarının GitHub kullanıcı adı
)
```

---

### 2.6 Adım 2d — sprint_issue_tracking Tablosuna Yazma

Her grup + sprint kombinasyonu için:
```java
// Önceki kayıtları sil (idempotency)
trackingRepository.deleteByGroup_IdAndSprint_Id(group.getId(), sprint.getId());

// Her issue için yeni kayıt oluştur
for (JiraIssueData issue : jiraIssues) {
    SprintIssueTracking log = new SprintIssueTracking();
    log.setGroup(group);
    log.setSprint(sprint);
    log.setIssueKey(issue.issueKey());
    log.setStoryPoints(issue.storyPoints());
    log.setSyncedAt(Instant.now());

    // Branch bul
    Optional<String> branch = githubDiscoveryService.findBranchForIssueKey(...);
    if (branch.isPresent()) {
        // PR durumunu kontrol et
        Optional<PrCheckResult> pr = githubApiClient.findMergedPrForBranch(...);
        pr.ifPresent(p -> {
            log.setPrNumber(p.prNumber());
            log.setPrMerged(p.merged());
            log.setAssigneeGithubUsername(p.authorGithubUsername());
        });
    }

    logs.add(log);
}
trackingRepository.saveAll(logs);
```

---

### 2.7 Transaction Yönetimi

Her grup `@Transactional(propagation = REQUIRES_NEW)` ile ayrı transaction'da çalışmalı. Bir grup başarısız olursa diğerleri etkilenmesin.

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
private void processSingleGroup(Group group, Sprint sprint) { ... }
```

---

## FAZ 3 — GitHub PR Kontrol Yeteneği

### 3.1 GithubApiClient'a PR Sorgusu Ekle

**Dosya:** `client/GithubApiClient.java`

**Eklenecek metod:**
```java
public Optional<PrCheckResult> findMergedPrForBranch(
        String orgName, String repoName, String branchName, String pat) {

    // GET https://api.github.com/repos/{orgName}/{repoName}/pulls
    //     ?head={orgName}:{branchName}&base=main&state=closed

    // Header'lar:
    // Authorization: Bearer {pat}
    // Accept: application/vnd.github+json
    // X-GitHub-Api-Version: 2022-11-28

    // İlk PR'ı al:
    // prNumber = response[0].number
    // merged = (state == "closed") && (merged_at != null)
    // authorGithubUsername = response[0].user.login
}
```

**Neden `authorGithubUsername` önemli:** JIRA'daki assignee email güvenilir değil. PR'ı kimin yazdığı (GitHub `user.login`) gerçek sahibi belirler. Bu alan `assignee_github_username` kolonuna yazılır ve öğrenci başına breakdown için kullanılır.

---

### 3.2 Mevcut PrVerificationController Güncelleme

`PrVerificationController.getBranchList()` şu an hardcoded mock döndürüyor. Bu endpoint artık anlamlı değil — silinmeli ya da gerçek GitHub API çağrısına bağlanmalı.

Öneri: Bu endpoint'i kaldır. PR kontrolü artık senkronizasyon pipeline'ının içinde yapılıyor.

---

## FAZ 4 — Danışman Paneli Genişletme (Veri Gösterim)

### 4.1 Backend: Danışman için Sprint Özeti Endpoint'leri

Şu an `/professor/my-advisees` sayfasında sadece entegrasyon bağlantı durması görünüyor. Aşağıdaki endpoint'ler eklenmeli — **sadece veri okuma, not giriş yok**.

#### 4.1.0 Yeni Controller: `AdvisorSprintController.java`

```java
@RestController
@RequestMapping("/api/v1/advisor")
@RequiredArgsConstructor
public class AdvisorSprintController {

    private final SprintIssueTrackingRepository trackingRepository;
    private final GroupRepository groupRepository;
    private final ActiveSprintServiceImpl activeSprintService;
    private final UserRepository userRepository;

    @GetMapping("/sprint-summary")
    public ResponseEntity<GroupSprintSummaryResponse> getSprintSummary(
            @RequestHeader("Authorization") String token) {
        // 1. JWT'den danışman user'ını al
        // 2. Danışmanın tüm gruplarını sor
        // 3. Aktif sprint'i bul
        // 4. Her grup için sprint_issue_tracking'den veri oku
        // 5. GroupSprintSummaryResponse nesnesi dön
    }

    @GetMapping("/groups/{groupId}/sprint-tracking")
    public ResponseEntity<GroupTrackingDetailResponse> getGroupTrackingDetails(
            @PathVariable Long groupId,
            @RequestHeader("Authorization") String token) {
        // 1. JWT'den danışman user'ını al
        // 2. Grup danışmanı kontrolü yap
        // 3. Aktif sprint'i bul
        // 4. sprint_issue_tracking.group_id = groupId AND sprint_id = activeSprintId sorgusu
        // 5. GroupTrackingDetailResponse nesnesi dön
    }
}
```

---

**Endpoint A: Danışmanın aktif sprint özeti**

```
GET /api/v1/advisor/sprint-summary
```

**Yetki:** Danışman JWT  
**Açıklama:** Danışmanın tüm gruplarının aktif sprint'teki durumunu gösterir.

**Response:**
```json
{
  "activeSprint": {
    "sprintId": 2,
    "sprintName": "Sprint 3",
    "startDate": "2026-03-01",
    "endDate": "2026-03-14",
    "daysRemaining": 5
  },
  "groups": [
    {
      "groupId": 1,
      "groupName": "Team Alpha",
      "totalIssues": 8,
      "mergedPRCount": 5,
      "syncedAt": "2026-05-04T02:00:00Z",
      "perStudentSummary": [
        {
          "githubUsername": "ali-dev",
          "completedStoryPoints": 13,
          "totalAssignedStoryPoints": 15
        },
        {
          "githubUsername": "fatih-dev",
          "completedStoryPoints": 8,
          "totalAssignedStoryPoints": 10
        }
      ]
    }
  ]
}
```

**Nasıl hesaplanır:**
- `totalIssues`: `sprint_issue_tracking.group_id = X AND sprint_id = activeSprintId` satır sayısı
- `mergedPRCount`: aynı sorguda `pr_merged = true` olan sayısı
- `perStudentSummary`: `assignee_github_username`'e göre gruplandırılmış SP toplamları

---

#### 4.1.1 Yeni DTO'lar

**`GroupSprintSummaryResponse.java`**
```java
@Data
public class GroupSprintSummaryResponse {
    private ActiveSprintInfo activeSprint;
    private List<GroupSummaryDto> groups;

    @Data
    public static class ActiveSprintInfo {
        private Long sprintId;
        private String sprintName;
        private LocalDate startDate;
        private LocalDate endDate;
        private Long daysRemaining;
    }

    @Data
    public static class GroupSummaryDto {
        private Long groupId;
        private String groupName;
        private Integer totalIssues;
        private Integer mergedPRCount;
        private LocalDateTime syncedAt;
        private List<PerStudentSummaryDto> perStudentSummary;
    }
}
```

**`PerStudentSummaryDto.java`**
```java
@Data
public class PerStudentSummaryDto {
    private String githubUsername;          // GitHub username
    private Integer completedStoryPoints;   // Merged PR'lardan gelen SP
    private Integer totalAssignedStoryPoints; // Atanan tüm issue'ların SP
}
```

**`GroupTrackingDetailResponse.java`**
```java
@Data
public class GroupTrackingDetailResponse {
    private Long groupId;
    private String groupName;
    private Long sprintId;
    private LocalDateTime syncedAt;
    private List<IssueTrackingDto> issues;

    @Data
    public static class IssueTrackingDto {
        private String issueKey;           // ör. "PROJ-101"
        private Integer storyPoints;       // null olabilir
        private String assigneeGithubUsername; // null olabilir
        private Long prNumber;             // null olabilir
        private Boolean prMerged;          // null = branch yok, false = PR açık, true = merged
    }
}
```

---

#### 4.1.2 Service: `AdvisorSprintService.java` (Yeni)

```java
@Service
@RequiredArgsConstructor
public class AdvisorSprintService {

    private final SprintIssueTrackingRepository trackingRepository;
    private final GroupRepository groupRepository;
    private final ActiveSprintServiceImpl activeSprintService;

    public GroupSprintSummaryResponse getAdvisorSprintSummary(Long advisorId) {
        // 1. Danışmanın tüm gruplarını bul
        List<Group> advisorGroups = groupRepository.findByAdvisor_Id(advisorId);

        // 2. Aktif sprint'i bul
        Sprint activeSprint = activeSprintService.findActiveSprint()
            .orElseThrow(() -> new IllegalStateException("No active sprint"));

        // 3. Her grup için sprint_issue_tracking'den verileri oku
        List<GroupSprintSummaryResponse.GroupSummaryDto> groupSummaries = 
            advisorGroups.stream()
                .map(group -> buildGroupSummary(group, activeSprint))
                .collect(Collectors.toList());

        GroupSprintSummaryResponse response = new GroupSprintSummaryResponse();
        response.setActiveSprint(buildActiveSprintInfo(activeSprint));
        response.setGroups(groupSummaries);
        return response;
    }

    // Helper methods: buildGroupSummary(), buildActiveSprintInfo(), buildIssueTrackingDto()
    // Per-student summary: assignee_github_username'e göre grupla, merged PR'lar için SP topla
}
```

---

#### 4.1.3 Endpoint Detayları

**Endpoint A: Danışmanın aktif sprint özeti**

```
GET /api/v1/advisor/sprint-summary
```

**Yetki:** Danışman JWT  
**Açıklama:** Danışmanın tüm gruplarının aktif sprint'teki durumunu gösterir.

**Response:**
```json
{
  "activeSprint": {
    "sprintId": 2,
    "sprintName": "Sprint 3",
    "startDate": "2026-03-01",
    "endDate": "2026-03-14",
    "daysRemaining": 5
  },
  "groups": [
    {
      "groupId": 1,
      "groupName": "Team Alpha",
      "totalIssues": 8,
      "mergedPRCount": 5,
      "syncedAt": "2026-05-04T02:00:00Z",
      "perStudentSummary": [
        {
          "githubUsername": "ali-dev",
          "completedStoryPoints": 13,
          "totalAssignedStoryPoints": 15
        },
        {
          "githubUsername": "fatih-dev",
          "completedStoryPoints": 8,
          "totalAssignedStoryPoints": 10
        }
      ]
    }
  ]
}
```

---

**Endpoint B: Grup bazlı detaylı issue tracking**

```
GET /api/v1/advisor/groups/{groupId}/sprint-tracking
```

**Yetki:** Danışman JWT + Grup danışmanı kontrolü  
**Response:**
```json
{
  "groupId": 1,
  "groupName": "Team Alpha",
  "sprintId": 2,
  "syncedAt": "2026-05-04T02:00:00Z",
  "issues": [
    {
      "issueKey": "PROJ-101",
      "storyPoints": 5,
      "assigneeGithubUsername": "ali-dev",
      "prNumber": 42,
      "prMerged": true
    },
    {
      "issueKey": "PROJ-102",
      "storyPoints": 3,
      "assigneeGithubUsername": "fatih-dev",
      "prNumber": 43,
      "prMerged": false
    },
    {
      "issueKey": "PROJ-103",
      "storyPoints": 4,
      "assigneeGithubUsername": null,
      "prNumber": null,
      "prMerged": null
    }
  ]
}
```

---

### 4.2 Frontend: Danışman Paneli Güncelleme

**Dosya:** `app/professor/my-advisees/page.tsx`

Mevcut sayfada `useIntegrationStatus` hook'u ile "Connected/No Connection" gösteriliyor. Buna ek olarak aşağıdakiler eklenecek — **sadece veri gösterimi, hiçbir form yok**.

**Eklenmesi gerekenler:**

1. **Aktif Sprint Bilgisi Kartı:** 
   - Sprint adı, başlama/bitiş tarihleri, kalan gün sayısı

2. **Grup Özet Listesi (Accordion/Expandable):** Her grup için:
   - Grup adı
   - Toplam issue sayısı
   - Merged PR sayısı / Toplam issue sayısı
   - Son sync zamanı
   - Per-student summary tablo:
     - GitHub username
     - Completed SP / Total assigned SP
     - (örn: "ali-dev: 13 / 15")

3. **Grup Detay (Genişletildiğinde):** Gruba tıklayınca:
   - Issue detay tablosu:
     - Issue Key (ör. PROJ-101)
     - Story Points
     - Assignee GitHub username
     - PR Number (varsa)
     - PR Merged? (Yes/No/—)

**NOT:** ❌ Hiçbir not giriş formu, dropdown veya buton **yok**. Sadece **okunabilir veri gösterimi**.

**Yeni API çağrı fonksiyonları (`lib/` altında):**
```typescript
// lib/sprint-tracking-api.ts (yeni dosya)
export async function fetchAdvisorSprintSummary(token: string): Promise<SprintSummaryResponse>
export async function fetchGroupTrackingDetails(groupId: number, token: string): Promise<GroupTrackingDetailResponse>
```

---

## FAZ 5 — Eksik analytics-api.ts Dosyası

**Sorun:** `LeaderboardTable.tsx` import ediyor ama dosya yok → derleme hatası.

**Oluşturulacak dosya:** `frontend/lib/analytics-api.ts`

**NOT:** Bu dosya şu an için **boş kalabilir** — Leaderboard veri gösterimi FAZ 2 (pipeline) sonrası, `sprint_issue_tracking` tablosu dolduktan sonra geri dönülecek.

```typescript
// Şu an için basic interface'ler ve placeholder fonksiyon

export interface StudentPerformanceDto {
  studentId: number;
  name: string;
  assignedSp: number;
  accomplishedSp: number;
  ratio: number;
}

export interface LeaderboardResponse {
  content: StudentPerformanceDto[];
  totalPages: number;
  totalElements: number;
  number: number;
}

export async function fetchLeaderboard(
  page: number = 0,
  size: number = 20,
  token: string
): Promise<LeaderboardResponse> {
  const res = await fetch(
    `${process.env.NEXT_PUBLIC_API_URL}/api/v1/analytics/leaderboard?page=${page}&size=${size}`,
    { headers: { Authorization: `Bearer ${token}` } }
  );
  if (!res.ok) throw new Error("Leaderboard fetch failed");
  return res.json();
}
```

**Hatırlatma:** `AnalyticsServiceImpl.recalculateAllPerformances()` şu an stub durumdadır. Pipeline (FAZ 2) başarılı çalıştıktan sonra bu service revize edilecek — `sprint_issue_tracking` tablosundan verileri okuması sağlanacak.

---

## Implementasyon Sırası ve Bağımlılıklar

```
FAZ 0.4 (EncryptionService)
  ↓
FAZ 0.3 (GithubDiscovery token kullan)
FAZ 0.1 (Frontend JIRA formu düzelt)    ← FAZ 0.2 ile paralel
FAZ 0.2 (connectedAt düzelt)            ← FAZ 0.1 ile paralel
  ↓
FAZ 1 (Sprint Issue Tracking tablosu + entity)
  ↓
FAZ 3 (GitHub PR metodu — GithubApiClient)
  ↓
FAZ 2 (ScrumSyncServiceImpl pipeline)   ← FAZ 1 + FAZ 3 gerekli
  ↓
FAZ 4 (Danışman paneli frontend)        ← FAZ 2 gerekli (tablo dolu olmalı)
FAZ 5 (analytics-api.ts)               ← bağımsız, her zaman yapılabilir
```

---

## Dosya Değişiklik Özeti

### Yeni Oluşturulacaklar

| Dosya | Tür | Faz |
|-------|-----|-----|
| `db/migration/V8__Create_Sprint_Issue_Tracking_Table.sql` | SQL | 1 |
| `model/SprintIssueTracking.java` | Entity | 1 |
| `repository/SprintIssueTrackingRepository.java` | Repository | 1 |
| `dto/JiraIssueData.java` | Record/DTO | 2 |
| `dto/PrCheckResult.java` | Record/DTO | 3 |
| `dto/GroupSprintSummaryResponse.java` | DTO | 4 |
| `dto/GroupTrackingDetailResponse.java` | DTO | 4 |
| `dto/PerStudentSummaryDto.java` | DTO | 4 |
| `service/EncryptionService.java` | Service | 0.4 |
| `frontend/lib/analytics-api.ts` | Frontend | 5 |
| `frontend/lib/sprint-tracking-api.ts` | Frontend | 4 |

### Değiştirilecekler

| Dosya | Ne Değişecek | Faz |
|-------|-------------|-----|
| `client/JiraApiClient.java` | `fetchActiveSprintIssues()` metodu eklenir | 2 |
| `client/GithubApiClient.java` | `findMergedPrForBranch()` metodu eklenir | 3 |
| `service/impl/ScrumSyncServiceImpl.java` | Pipeline tamamen yazılır | 2 |
| `service/GithubDiscoveryService.java` | PAT ile istek, `findBranchForIssueKey()` eklenir | 0.3 |
| `converter/EncryptionConverter.java` | `EncryptionService`'i çağıracak şekilde güncellenir | 0.4 |
| `controller/AdvisorSprintController.java` | `/api/v1/advisor/sprint-summary` ve `/api/v1/advisor/groups/{id}/sprint-tracking` endpoint'leri | 4 |
| `app/professor/my-advisees/page.tsx` | Sprint özeti accordion/drawer eklenir (sadece veri gösterimi, not giriş yok) | 4 |
| `app/groups/[groupId]/integrations/jira/page.tsx` | Alan adı + projectKey düzeltme | 0.1 |
| `hooks/useIntegrationStatus.ts` | `connectedAt` → `createdAt` düzeltme | 0.2 |
| `controller/PrVerificationController.java` | Hardcoded mock endpoint kaldırılır | 3 |

---

## Kritik Tasarım Kararları

### 1. JIRA Assignee Yerine GitHub PR Yazarı

JIRA'da assignee email tutulur, ama bu email'in GitHub username'iyle eşleşmesi güvenilir değil. Bu projede de aynı tasarım kararı:

> `assignee_github_username` kolonu JIRA'daki assignee'den değil, GitHub PR'ındaki `user.login`'den doldurulur.

### 2. Boş JIRA Sonucunda Silme Yok

Pipeline çalışırken JIRA API boş liste döndürürse, mevcut `sprint_issue_tracking` kayıtları korunur. Sadece başarılı çekmede delete + insert yapılır.

### 3. sprint_issue_tracking Idempotency

Her pipeline çalışmasında önce `deleteByGroup_IdAndSprint_Id()` yapılır, sonra yeniden yazılır. Bu sayede birden fazla kez tetiklenmek güvenlidir.

### 4. Güvenlik: EncryptionService

`EncryptionConverter`'daki hardcoded key, `EncryptionService` ayrıştırmasıyla `application.yml`'dan (`${ENCRYPTION_KEY}`) okunacak hale getirilir. Mevcut şifrelenmiş kayıtlar, key değişmediği sürece etkilenmez.

---

## Kısaca: Hangi Senaryo Adımı Nerede Çözülüyor

| Senaryo | Çözüm |
|---------|-------|
| Takım lideri JIRA PAT + Space girer | Faz 0.1 — frontend düzeltmesi |
| JIRA bağlantısı aktif sprint issue'larını çeker | Faz 2 — `JiraApiClient.fetchActiveSprintIssues()` |
| Issue Key ile GitHub'da branch aranır | Faz 0.3 + Faz 2 — `GithubDiscoveryService` + pipeline |
| Branch'in PR'ı var mı, merged mı? | Faz 3 — `GithubApiClient.findMergedPrForBranch()` |
| Sonuçlar veritabanına yazılır | Faz 1 — `sprint_issue_tracking` tablosu |
| Danışman panelinde veri görüntülenmesi | Faz 4 — yeni endpoint'ler + frontend (sadece okuma) |
| Günlük otomatik senkronizasyon | Faz 2 — `ScrumSyncScheduler` + dolu pipeline |
