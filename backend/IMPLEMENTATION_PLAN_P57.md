# Implementation Plan — P5.7: Committee Notification Service + WebSocket

## 1. Genel Bakış

Bu issue, commit ortamlarına (committee) yapılan atamalar (P5.2 advisor, P5.3 jury, P5.4 group) sonrasında ilgili üyelere bildirim gönderilmesini (P5.7) kapsar. Bildirimler D10 veri deposunda (mevcut `notifications` tablosu) saklanacak ve `/ws` WebSocket endpoint'i üzerinden gerçek zamanlı iletilecektir.

---

## 2. Mevcut Kodun Durumu (Analiz Özeti)

| Katman | Dosya | Durum |
|---|---|---|
| Model | `Notification.java` | ✅ Mevcut, kullanılabilir |
| Model | `NotificationType` enum | ⚠️ Eksik: ADVISOR_ASSIGNMENT, JURY_ASSIGNMENT vb. yok |
| Model | `CommitteeAdvisor.java` | ✅ Mevcut |
| Model | `CommitteeJury.java` | ✅ Mevcut |
| Model | `Committee.java` | ✅ Mevcut |
| Repository | `NotificationRepository.java` | ⚠️ Eksik: committeeId bazlı sorgular yok |
| Service | `NotificationService.java` (interface) | ⚠️ Yeni metotlar eklenecek |
| Service | `NotificationServiceImpl.java` | ⚠️ Yeni metotlar eklenecek |
| Controller | `NotificationController.java` | ⚠️ Yeni endpoint'ler eklenecek |
| Config | WebSocket config | ❌ Yok — oluşturulacak |
| Config | CORS | ⚠️ WebSocket için güncellenmeli |
| pom.xml | WebSocket bağımlılığı | ❌ Yok — eklenecek |

---

## 3. Yapılacak Değişiklikler (Dosya Bazlı)

### 3.1 pom.xml — WebSocket Bağımlılığı Ekle

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

> **Not:** Bilgisayarına EKSTRA BİR ŞEY KURMAN GEREKMEZ. Spring Boot starter tüm bağımlılıkları Maven üzerinden otomatik indirir.

---

### 3.2 `NotificationType.java` — Yeni Enum Değerleri Ekle

**Dosya:** `model/notification/NotificationType.java`

```java
// MEVCUT:
MEMBERSHIP_INVITE, ADVISOR_REQUEST, ADVISOR_DECISION, SYSTEM_ALERT, GROUP_DISBANDED

// EKLENECEK:
ADVISOR_ASSIGNMENT, JURY_ASSIGNMENT, GROUP_ASSIGNMENT, SCHEDULE_CHANGE, MEETING_REMINDER, GENERAL
```

---

### 3.3 `NotificationRepository.java` — Yeni Query Metotları

**Dosya:** `repository/NotificationRepository.java`

Eklenecek metotlar:

```java
// Committee bazlı tüm bildirimler (en yeniden eskiye)
List<Notification> findByCommitteeIdOrderByCreatedAtDesc(Long committeeId);

// Kullanıcı + committeeId bazlı
List<Notification> findByToUser_UserIdAndCommitteeIdOrderByCreatedAtDesc(Long userId, Long committeeId);
```

> **Not:** `Notification.java` entity'sine `committeeId` alanı eklenmeli.

---

### 3.4 `Notification.java` — committeeId Alanı Ekle

**Dosya:** `model/notification/Notification.java`

```java
@Column(name = "committee_id")
private Long committeeId;

// getter + setter
```

> **Veritabanı:** `notifications` tablosuna `committee_id` kolonu eklenecek.
> `spring.jpa.hibernate.ddl-auto=validate` olduğu için Supabase'de bu kolonu **manuel olarak** eklemelisin:
> ```sql
> ALTER TABLE notifications ADD COLUMN IF NOT EXISTS committee_id BIGINT;
> ```

---

### 3.5 `CommitteeRepository.java` — YENİ DOSYA

**Dosya:** `repository/CommitteeRepository.java`

```java
package com.spms.backend.repository;

import com.spms.backend.model.Committee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommitteeRepository extends JpaRepository<Committee, Long> {}
```

---

### 3.6 `CommitteeNotificationService.java` — YENİ DOSYA (Interface)

**Dosya:** `service/CommitteeNotificationService.java`

```java
package com.spms.backend.service;

import com.spms.backend.dto.request.CommitteeNotifyRequestDto;
import com.spms.backend.dto.response.NotificationDto;
import java.util.List;

public interface CommitteeNotificationService {

    // POST /api/v1/committees/{committeeId}/notify
    Long sendCommitteeNotification(Long committeeId, CommitteeNotifyRequestDto request, Long senderUserId);

    // GET /api/v1/committees/{committeeId}/notifications
    List<NotificationDto> getCommitteeNotifications(Long committeeId);

    // GET /api/v1/committees/notifications  (caller'ın aldığı bildirimler)
    List<NotificationDto> getMyCommitteeNotifications(Long userId);

    // P5.2 trigger: Advisor atandığında çağrılır
    void notifyAdvisorAssignment(Long committeeId, Long assignedUserId, Long actorUserId);

    // P5.3 trigger: Jury atandığında çağrılır
    void notifyJuryAssignment(Long committeeId, Long assignedUserId, Long actorUserId);

    // P5.4 trigger: Group atandığında çağrılır
    void notifyGroupAssignment(Long committeeId, Long groupId, Long actorUserId);
}
```

---

### 3.7 `CommitteeNotificationServiceImpl.java` — YENİ DOSYA

**Dosya:** `service/impl/CommitteeNotificationServiceImpl.java`

Temel mantık:

```java
@Service
@Transactional
public class CommitteeNotificationServiceImpl implements CommitteeNotificationService {

    // Dependencies: CommitteeRepository, NotificationRepository,
    //               UserRepository, SimpMessagingTemplate (WebSocket)

    @Override
    public Long sendCommitteeNotification(Long committeeId, CommitteeNotifyRequestDto req, Long senderUserId) {
        // 1. Committee'yi bul → 404 if not found
        // 2. notificationType enum validation
        // 3. message length: 1-1000 chars
        // 4. recipients: req.recipients() null/empty → tüm committee üyeleri
        // 5. Her recipient için Notification oluştur → kaydet
        // 6. Her kayıt sonrası WebSocket'e push yap
        // 7. İlk bildirimin ID'sini döndür
    }

    @Override
    public List<NotificationDto> getCommitteeNotifications(Long committeeId) {
        // committeeId ile repository'den çek → DTO map → döndür
    }

    @Override
    public List<NotificationDto> getMyCommitteeNotifications(Long userId) {
        // userId + committee notification tiplerini filtrele
    }

    private void pushWebSocket(Long toUserId, NotificationDto dto) {
        // SimpMessagingTemplate.convertAndSendToUser(...)
        // Hedef topic: /user/{userId}/queue/notifications
    }

    private List<User> resolveRecipients(Committee committee, List<Long> recipientIds) {
        if (recipientIds == null || recipientIds.isEmpty()) {
            // Tüm advisor + jury üyeleri
        } else {
            // Sadece belirtilen ID'ler
        }
    }
}
```

---

### 3.8 DTOs — YENİ DOSYALAR

#### `CommitteeNotifyRequestDto.java`
**Dosya:** `dto/request/CommitteeNotifyRequestDto.java`

```java
public record CommitteeNotifyRequestDto(
    @NotBlank @Size(min=1, max=1000) String message,
    @NotNull String notificationType,  // enum validation service'de
    List<Long> recipients              // optional
) {}
```

#### `CommitteeNotificationResponse.java`
**Dosya:** `dto/response/CommitteeNotificationResponse.java`

```java
public record CommitteeNotificationResponse(
    Long notificationId,
    String status,
    int recipientCount,
    Instant sentAt
) {}
```

---

### 3.9 `CommitteeNotificationController.java` — YENİ DOSYA

**Dosya:** `controller/CommitteeNotificationController.java`

```java
@RestController
@RequestMapping("/api/v1/committees")
public class CommitteeNotificationController {

    // POST /{committeeId}/notify
    // Yetki: sadece coordinator (jwt_role = "coordinator")
    @PostMapping("/{committeeId}/notify")
    public ResponseEntity<CommitteeNotificationResponse> notify(...) { }

    // GET /{committeeId}/notifications
    // Yetki: authenticated user
    @GetMapping("/{committeeId}/notifications")
    public ResponseEntity<List<NotificationDto>> getCommitteeNotifications(...) { }

    // GET /notifications  (benim aldıklarım)
    // Yetki: authenticated user
    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationDto>> getMyCommitteeNotifications(...) { }
}
```

---

### 3.10 `WebSocketConfig.java` — YENİ DOSYA

**Dosya:** `config/WebSocketConfig.java`

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("http://localhost:3000", "http://localhost:*")
                .withSockJS();  // SockJS fallback (eski tarayıcılar için)
    }
}
```

---

### 3.11 `CorsConfig.java` — Güncelleme

**Dosya:** `config/CorsConfig.java`

```java
// Mevcut /api/** mapping'e ek olarak:
registry.addMapping("/ws/**")
    .allowedOrigins("http://localhost:3000")
    .allowedMethods("GET", "POST", "OPTIONS")
    .allowCredentials(true);
```

---

## 4. Klasör Yapısı (Değişiklik Sonrası)

```
backend/src/main/java/com/spms/backend/
├── config/
│   ├── CorsConfig.java              ← GÜNCELLEME
│   └── WebSocketConfig.java         ← YENİ
├── controller/
│   └── CommitteeNotificationController.java  ← YENİ
├── dto/
│   ├── request/
│   │   └── CommitteeNotifyRequestDto.java    ← YENİ
│   └── response/
│       └── CommitteeNotificationResponse.java ← YENİ
├── model/
│   └── notification/
│       ├── Notification.java        ← GÜNCELLEME (committeeId alanı)
│       └── NotificationType.java    ← GÜNCELLEME (yeni enum değerleri)
├── repository/
│   ├── CommitteeRepository.java     ← YENİ
│   └── NotificationRepository.java  ← GÜNCELLEME (yeni query metotları)
├── service/
│   ├── CommitteeNotificationService.java     ← YENİ
│   └── impl/
│       └── CommitteeNotificationServiceImpl.java  ← YENİ
```

---

## 5. Uygulama Adımları (Sıra Önemlidir)

1. `pom.xml`'e WebSocket bağımlılığını ekle
2. Supabase'de SQL komutu çalıştır: `ALTER TABLE notifications ADD COLUMN IF NOT EXISTS committee_id BIGINT;`
3. `NotificationType.java`'ya yeni enum değerlerini ekle
4. `Notification.java`'ya `committeeId` alanını ekle
5. `CommitteeRepository.java` oluştur
6. `NotificationRepository.java`'ya yeni query metotlarını ekle
7. `CommitteeNotifyRequestDto.java` ve `CommitteeNotificationResponse.java` oluştur
8. `CommitteeNotificationService.java` interface'ini oluştur
9. `CommitteeNotificationServiceImpl.java` oluştur
10. `WebSocketConfig.java` oluştur
11. `CorsConfig.java`'yı güncelle
12. `CommitteeNotificationController.java` oluştur
13. `mvn clean compile` ile derlemeyi doğrula
14. `mvn spring-boot:run` ile uygulamayı başlat ve test et

---

## 6. WebSocket: Frontend Tarafında Nasıl Bağlanılır

Frontend (JavaScript/React):

```javascript
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

const client = new Client({
    webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
    onConnect: () => {
        client.subscribe('/user/queue/notifications', (message) => {
            const notification = JSON.parse(message.body);
            console.log('Yeni bildirim:', notification);
        });
    }
});
client.activate();
```

> Frontend için ekstra kurulum: `npm install @stomp/stompjs sockjs-client`

---

## 7. Doğrulama ve Test Planı

### 7.1 Derleme Doğrulama

```bash
cd backend
mvn clean compile
# BUILD SUCCESS görülmeli
```

### 7.2 Uygulama Başlatma

```bash
mvn spring-boot:run
# Started SpmsBackendApplication successfully
```

### 7.3 REST Endpoint Testleri (curl)

**Bildirim Gönder:**
```bash
curl -X POST http://localhost:8080/api/v1/committees/1/notify \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <coordinator_token>" \
  -d '{"message":"Advisor atandı","notificationType":"ADVISOR_ASSIGNMENT"}'
# Beklenen: 201 Created + { "notificationId": ..., "recipientCount": ..., "status": "sent" }
```

**Committee Bildirimlerini Getir:**
```bash
curl http://localhost:8080/api/v1/committees/1/notifications \
  -H "Authorization: Bearer <token>"
# Beklenen: 200 OK + bildirim listesi
```

**Kendi Bildirimlerimi Getir:**
```bash
curl http://localhost:8080/api/v1/committees/notifications \
  -H "Authorization: Bearer <token>"
# Beklenen: 200 OK + benim aldığım bildirimler
```

**404 Testi:**
```bash
curl http://localhost:8080/api/v1/committees/99999/notifications \
  -H "Authorization: Bearer <token>"
# Beklenen: 404 Not Found
```

### 7.4 WebSocket Testi (Postman)

1. Postman → New Request → WebSocket
2. URL: `ws://localhost:8080/ws`
3. Bağlan → Subscribe `/user/queue/notifications`
4. Başka terminalde POST `/api/v1/committees/1/notify` çalıştır
5. WebSocket'te mesajın 1 saniye içinde geldiğini doğrula

### 7.5 Hata Senaryoları

| Senaryo | Beklenen HTTP Kodu |
|---|---|
| committeeId bulunamadı | 404 |
| Geçersiz notificationType | 400 |
| Message boş veya 1000+ karakter | 400 |
| Coordinator değil göndermeye çalışıyor | 403 |
| Token yok | 401 |

---

## 8. Sık Karşılaşılan Sorunlar ve Çözümleri

| Sorun | Neden | Çözüm |
|---|---|---|
| `Failed to load ApplicationContext` | DB bağlantısı yok | application.properties'teki DB_URL doğru mu? |
| `Column committee_id not found` | DB şeması güncellenmemiş | Supabase'de ALTER TABLE çalıştır |
| `WebSocket 403` | CORS ayarı eksik | CorsConfig'e `/ws/**` mapping ekle |
| `SimpMessagingTemplate not found` | WebSocket starter eksik | pom.xml'e bağımlılık ekle |
| `NotificationType has no constant ADVISOR_ASSIGNMENT` | Enum güncellenmemiş | NotificationType.java'yı güncelle |

---

## 9. Önemli Notlar

- **Ekstra kurulum GEREKMEZ.** Spring Boot WebSocket tamamen Maven bağımlılığı ile gelir. Hiçbir şey yüklemen gerekmez.
- `ddl-auto=validate` ayarı olduğu için Supabase'de `committee_id` kolonunu **elle** eklemelisin.
- WebSocket authentication için mevcut JWT filter'ı WebSocket handshake'e uyarlamak gerekebilir. İlk aşamada origin-based güvenlik yeterlidir.
- `SimpMessagingTemplate` bean'i `WebSocketConfig.java` oluşturulduktan sonra otomatik Spring context'e eklenir — ayrıca bir Bean tanımı yazmana gerek yok.
