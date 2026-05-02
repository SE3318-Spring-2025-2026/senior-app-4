# P5.7 — Yapılan Değişiklikler

## Genel Özet

| | |
|---|---|
| **Derleme** | ✅ BUILD SUCCESS (main + test) |
| **Yeni dosya** | 6 adet |
| **Değiştirilen dosya** | 6 adet |
| **Silinen dosya** | 0 adet |

---

## 1. `pom.xml` — WebSocket bağımlılığı eklendi

**Değişiklik türü:** Güncelleme

Eklenen kod:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

**Neden:** Gerçek zamanlı bildirim gönderimi için Spring STOMP/WebSocket altyapısı gerekiyordu.
Bilgisayara ekstra kurulum gerekmez — Maven bağımlılığı ile otomatik gelir.

---

## 2. `NotificationType.java` — 6 yeni enum değeri eklendi

**Dosya:** `model/notification/NotificationType.java`
**Değişiklik türü:** Güncelleme

```java
// Öncesi:
MEMBERSHIP_INVITE, ADVISOR_REQUEST, ADVISOR_DECISION, SYSTEM_ALERT, GROUP_DISBANDED

// Sonrası (yeni eklenenler):
ADVISOR_ASSIGNMENT, JURY_ASSIGNMENT, GROUP_ASSIGNMENT,
SCHEDULE_CHANGE, MEETING_REMINDER, GENERAL
```

**Neden:** Issue P5.7, committee bildirimlerinin bu tipleri desteklemesini zorunlu kılıyor.

---

## 3. `Notification.java` — `committeeId` alanı eklendi

**Dosya:** `model/notification/Notification.java`
**Değişiklik türü:** Güncelleme

Eklenen kod:
```java
@Column(name = "committee_id")
private Long committeeId;

public Long getCommitteeId() { return committeeId; }
public void setCommitteeId(Long committeeId) { this.committeeId = committeeId; }
```

**Neden:** Hangi bildirimin hangi committee'ye ait olduğunu D10'da (notifications tablosu) tutmak için.

> ⚠️ **Supabase'de manuel SQL gereklidir:**
> ```sql
> ALTER TABLE notifications ADD COLUMN IF NOT EXISTS committee_id BIGINT;
> ```

---

## 4. `NotificationRepository.java` — 2 yeni sorgu metodu eklendi

**Dosya:** `repository/NotificationRepository.java`
**Değişiklik türü:** Güncelleme

Eklenen metodlar:
```java
List<Notification> findByCommitteeIdOrderByCreatedAtDesc(Long committeeId);

List<Notification> findByToUser_UserIdAndCommitteeIdIsNotNullOrderByCreatedAtDesc(Long userId);
```

**Neden:**
- `findByCommitteeIdOrderByCreatedAtDesc` → `GET /{committeeId}/notifications` için
- `findByToUser_UserIdAndCommitteeIdIsNotNullOrderByCreatedAtDesc` → `GET /committees/notifications` için

---

## 5. `CommitteeRepository.java` — YENİ DOSYA

**Dosya:** `repository/CommitteeRepository.java`

```java
public interface CommitteeRepository extends JpaRepository<Committee, Long> {
    Optional<Committee> findByCommitteeId(Long committeeId);
}
```

**Neden:** Service katmanında committee varlığını doğrulamak (404 kontrolü) için.

---

## 6. `CommitteeNotifyRequestDto.java` — YENİ DOSYA

**Dosya:** `dto/request/CommitteeNotifyRequestDto.java`

```java
public record CommitteeNotifyRequestDto(
    @NotBlank @Size(min=1, max=1000) String message,
    @NotBlank String notificationType,
    List<Long> recipients   // opsiyonel — boşsa tüm committee üyeleri
) {}
```

**Neden:** `POST /api/v1/committees/{committeeId}/notify` endpoint'inin request body'si.

---

## 7. `CommitteeNotificationResponse.java` — YENİ DOSYA

**Dosya:** `dto/response/CommitteeNotificationResponse.java`

```java
public record CommitteeNotificationResponse(
    Long notificationId,
    String status,
    int recipientCount,
    Instant sentAt
) {}
```

**Neden:** `POST /notify` endpoint'inin response body'si.

---

## 8. `CommitteeNotificationService.java` — YENİ DOSYA (Interface)

**Dosya:** `service/CommitteeNotificationService.java`

Tanımlanan metodlar:

| Metod | Açıklama |
|---|---|
| `sendCommitteeNotification` | POST /notify endpoint'i için |
| `getCommitteeNotifications` | GET /{committeeId}/notifications için |
| `getMyCommitteeNotifications` | GET /committees/notifications için |
| `notifyAdvisorAssignment` | P5.2 tetiklenince çağrılır |
| `notifyJuryAssignment` | P5.3 tetiklenince çağrılır |
| `notifyGroupAssignment` | P5.4 tetiklenince çağrılır |

---

## 9. `CommitteeNotificationServiceImpl.java` — YENİ DOSYA

**Dosya:** `service/impl/CommitteeNotificationServiceImpl.java`

İş mantığı:

| Metod | Ne Yapar |
|---|---|
| `sendCommitteeNotification` | Committee bulur → tip doğrular → recipients boşsa tüm üyeleri alır → D10'a kaydet → WebSocket push |
| `getCommitteeNotifications` | committeeId ile D10'dan bildirimleri çeker, en yeniden eskiye sıralar |
| `getMyCommitteeNotifications` | Kullanıcının aldığı committee bildirimlerini çeker |
| `notifyAdvisorAssignment` | P5.2 tetiklenince tüm committee üyelerine ADVISOR_ASSIGNMENT bildirimi gönderir |
| `notifyJuryAssignment` | P5.3 tetiklenince tüm committee üyelerine JURY_ASSIGNMENT bildirimi gönderir |
| `notifyGroupAssignment` | P5.4 tetiklenince tüm committee üyelerine GROUP_ASSIGNMENT bildirimi gönderir |
| `pushWebSocket` | `SimpMessagingTemplate` ile `/user/{userId}/queue/notifications` kanalına push yapar |
| `resolveRecipients` | recipients null/boş ise tüm advisor + jury üyelerini döner |

---

## 10. `WebSocketConfig.java` — YENİ DOSYA

**Dosya:** `config/WebSocketConfig.java`

```
Endpoint     : /ws
Fallback     : SockJS (HTTP polling — eski tarayıcılar için)
Broker       : In-memory (/topic, /queue)
User prefix  : /user/{id}/queue/notifications
```

**Frontend bağlantı örneği:**
```javascript
const client = new Client({
    webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
    onConnect: () => {
        client.subscribe('/user/queue/notifications', (msg) => {
            console.log(JSON.parse(msg.body));
        });
    }
});
client.activate();
```

---

## 11. `CorsConfig.java` — WebSocket için güncellendi

**Dosya:** `config/CorsConfig.java`
**Değişiklik türü:** Güncelleme

Eklenen mapping:
```java
registry.addMapping("/ws/**")
    .allowedOrigins("http://localhost:3000")
    .allowedMethods("GET", "POST", "OPTIONS")
    .allowedHeaders("*")
    .allowCredentials(true);
```

**Neden:** SockJS bağlantısı HTTP üzerinden başladığı için CORS politikasına dahil edilmesi gerekiyor.

---

## 12. `CommitteeNotificationController.java` — YENİ DOSYA

**Dosya:** `controller/CommitteeNotificationController.java`

| Method | URL | Yetki | Açıklama |
|---|---|---|---|
| `POST` | `/api/v1/committees/{committeeId}/notify` | coordinator | Bildirim gönder |
| `GET` | `/api/v1/committees/{committeeId}/notifications` | authenticated | Committee bildirimlerini getir |
| `GET` | `/api/v1/committees/notifications` | authenticated | Kendi aldığım bildirimleri getir |

**Hata senaryoları:**

| Durum | HTTP Kodu |
|---|---|
| committeeId bulunamadı | 404 |
| Geçersiz notificationType | 400 |
| Mesaj boş veya 1000+ karakter | 400 |
| Coordinator değil göndermeye çalışıyor | 403 |
| Token yok | 401 |

---

## 13. `AdvisorRequestDetailServiceTest.java` — Test stub güncellendi

**Dosya:** `src/test/.../AdvisorRequestDetailServiceTest.java`
**Değişiklik türü:** Test stub'ına yalnızca 2 boş override eklendi

Eklenen satırlar:
```java
@Override public List<Notification> findByCommitteeIdOrderByCreatedAtDesc(Long committeeId) { return List.of(); }
@Override public List<Notification> findByToUser_UserIdAndCommitteeIdIsNotNullOrderByCreatedAtDesc(Long userId) { return List.of(); }
```

> Test mantığına (test metotlarına, assertion'lara) hiç dokunulmadı.

---

## Kalan Manuel Adım

Supabase SQL Editor'da şu komutu çalıştır:

```sql
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS committee_id BIGINT;
```

Ardından uygulamayı yeniden başlat:

```bash
mvn spring-boot:run
```
