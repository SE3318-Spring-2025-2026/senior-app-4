# Yapılan Değişiklikler ve Geri Alma Rehberi (Kerem)

Bu dosya, projede yapılan API düzeltmelerini ve değişiklikleri takip etmek için oluşturulmuştur. Herhangi bir sorunda değişiklikleri nasıl geri alabileceğin aşağıda belirtilmiştir.

---

## 1. Authorization (Yetkilendirme) Header Eklenmesi
**Tarih:** 28 Nisan 2026
**Dosya:** `frontend/lib/client.ts`
**Amaç:** Backend'e giden tüm isteklere kullanıcının JWT token'ını otomatik olarak eklemek. (Hata 1)

**Ne Değişti?**
Dosyanın başına `getToken` fonksiyonu eklendi ve `axios.interceptors.response.use` bloğundan hemen önce bir `request` interceptor eklendi.

**Nasıl Geri Alınır? (Eski Haline Döndürme):**
Aşağıdaki iki adımı silerek kodu eski haline döndürebilirsin:
1. `import { getToken } from './auth';` satırını sil.
2. Aşağıdaki bloğu tamamen sil:
```typescript
// Add Authorization Header to Requests
apiClient.interceptors.request.use(
  (config) => {
    const token = getToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);
```

---

## 2. API Yönlendirme (Proxy/Rewrites) Eklenmesi
**Tarih:** 28 Nisan 2026
**Dosya:** `frontend/next.config.ts`
**Amaç:** Next.js (port 3000) üzerinden yapılan `/api/*` isteklerini, Spring Boot backend'ine (port 8080) yönlendirmek. (Hata 2)

**Ne Değişti?**
`nextConfig` objesinin içine `rewrites` fonksiyonu eklendi.

**Nasıl Geri Alınır? (Eski Haline Döndürme):**
Dosyayı aşağıdaki orjinal haline döndürebilirsin:
```typescript
import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  turbopack: {
    root: process.cwd(),
  },
};

export default nextConfig;
```

---

## 3. Büyük/Küçük Harf (Case-Sensitivity) Uyumsuzluğunun Çözümü
**Tarih:** 28 Nisan 2026
**Amaç:** Veritabanındaki rol ("STUDENT", "student" vb.) kayıtlarının büyük/küçük harf fark etmeksizin bulunmasını sağlamak ve frontend'de sabit standart (Enum) kullanımına geçmek. (Hata 4 ve 5)

**Ne Değişti?**
1. **Backend (Esneklik):** `UserRepository` ve ilgili tüm servislerde `findAllByRole` metodunun adı `findAllByRoleIgnoreCase` olarak değiştirildi. Böylece Spring Boot aramaları büyük/küçük harfe duyarsız hale geldi.
2. **Frontend (Standart):** `frontend/types/enums.ts` oluşturuldu ve içine `UserRole` eklendi. `advisor-api.ts`, `coordinator/members/page.tsx` ve `coordinator/advisor-override/page.tsx` dosyalarında elle yazılan `role=STUDENT` veya `role=professor` yazıları silinip `role=${UserRole.STUDENT}` formatına dönüştürüldü.
3. **Hayalet Endpoint Düzeltildi:** `members/page.tsx` dosyasında yanlış yazılan `/students` endpoint çağrısı silinip doğru format olan `/users?role=${UserRole.STUDENT}` ile değiştirildi.

**Nasıl Geri Alınır? (Eski Haline Döndürme):**
1. Backend: `UserRepository`, `UserController` ve `SubmissionServiceImpl` içindeki `findAllByRoleIgnoreCase` kelimelerini tekrar `findAllByRole` yapabilirsin.
2. Frontend: Eklenen `import { UserRole }` satırlarını silip, API isteklerindeki `role=${UserRole.STUDENT}` yazılarını tekrar `role=STUDENT` yapabilirsin.

---

## 4. Yanlış Veri Tipi Gönderiminin Düzeltilmesi (String vs Number)
**Tarih:** 28 Nisan 2026
**Dosya:** `frontend/lib/advisor-api.ts`
**Amaç:** Danışman talebi oluştururken `teamId` verisinin backend'in beklediği gibi Sayı (Long) formatında gitmesini sağlamak. (Hata 3)

**Ne Değişti?**
`createAdvisorRequest` fonksiyonunun içinde yer alan `String(groupId)` dönüşümü kaldırılarak, değişken doğrudan sayı formatında (`groupId`) bırakıldı.

**Nasıl Geri Alınır? (Eski Haline Döndürme):**
Dosyanın içindeki şu kodu bulup:
`body: JSON.stringify({ teamId: groupId, professorId }),`
Tekrar şu şekilde String sarmalayıcısı ekleyebilirsin:
`body: JSON.stringify({ teamId: String(groupId), professorId }),`

---

## 5. Backend'de Karşılığı Olmayan Filtrelerin Düzeltilmesi (Dinamik Filtreleme)
**Tarih:** 28 Nisan 2026
**Dosya:** `GroupController.java`, `GroupService.java`, `GroupServiceImpl.java`, `GroupRepository.java`, `GroupSpecification.java`
**Amaç:** Frontend'den gelen `groupName`, `status` ve `advisorAssigned` arama/filtreleme parametrelerini backend'in anlayıp veritabanından süzerek getirmesini sağlamak (Hata 6).

**Ne Değişti?**
1. `GroupSpecification.java` adında yeni bir sınıf oluşturularak Spring Data JPA "Specification" (Kriter Bazlı Dinamik Sorgu) altyapısı kuruldu.
2. `GroupRepository` arayüzüne `JpaSpecificationExecutor<Group>` yeteneği eklendi.
3. `GroupController` içerisindeki `getGroups` metoduna 3 yeni `@RequestParam` eklendi.
4. `GroupServiceImpl` içerisinde `switch-case` ile yazılan statik sorgu mantığı, kullanıcının filtrelerine göre dinamik olarak şekillenen Specification sorgularıyla değiştirildi.

**Nasıl Geri Alınır? (Eski Haline Döndürme):**
Bu köklü bir mimari değişikliktir. Eğer eski (filtrelemeyen) haline dönmek istersen:
- `GroupController.java`'daki `getGroups` metodundan `@RequestParam` parametrelerini silmelisin.
- `GroupServiceImpl.java`'daki `getGroups` metodunda `GroupSpecification` kodunu silip eski `switch(role)` yapısını geri koymalısın.
- `GroupSpecification.java` dosyasını silebilirsin.

---

## 6. Backend Test Hatalarının Düzeltilmesi (JpaSpecificationExecutor)
**Tarih:** 28 Nisan 2026
**Dosya:** `AdvisorRequestDetailServiceTest.java`, `AdvisorAssignmentServiceImplTest.java`
**Amaç:** `GroupRepository`'ye `JpaSpecificationExecutor` eklendikten sonra, testlerde kullanılan sahte (stub) repository sınıflarının eksik abstract metodları yüzünden derlenememesini (`mvn clean compile test-compile` hatası) düzeltmek.

**Ne Değişti?**
Her iki test dosyasındaki `StubGroupRepository` sınıflarına Spring Data 3.x'in zorunlu kıldığı `findOne`, `findAll(Spec)`, `findAll(Spec, Sort)`, `findAll(Spec, Pageable)`, `count(Spec)`, `exists(Spec)`, `delete(Spec)` ve `findBy(Spec, Function)` abstract metodlarının boş (dummy) implementasyonları eklendi.

**Nasıl Geri Alınır? (Eski Haline Döndürme):**
Eklenen override metod bloklarını test dosyalarından silebilirsin. (Ancak bu durumda testler tekrar derlenmeyecektir).

---

## 7. GitHub OAuth Girişinde "role: null" ve "400 Bad Request" Sorununun Çözümü
**Tarih:** 28 Nisan 2026
**Dosya:** `StudentRegistrationService.java`, `JwtAuthFilter.java`
**Amaç:** Veritabanında role alanı eksik (null) olan eski kullanıcılar giriş yaptığında token'a rolün eksik yansımasını ve bunun controller'larda yanıltıcı `400 Bad Request` hatalarına (özellikle gruplar sayfasında) yol açmasını engellemek.

**Ne Değişti?**
1. **StudentRegistrationService:** `reconcileExistingStudent` metodunda, veritabanından çekilen mevcut kullanıcının rolü null/boş ise, otomatik olarak `"student"` rolü atanarak güncellendi ve bu haliyle token üretilmesi sağlandı.
2. **JwtAuthFilter:** Token içinden okunan `role` veya `userId` eksik (null) ise, controller aşamasına geçmeden önce isteğin erken aşamada `401 Unauthorized` ile reddedilmesi sağlandı (Daha önce null rol request'ten tamamen silindiği için `400 Bad Request` fırlatıyordu).

**Nasıl Geri Alınır? (Eski Haline Döndürme):**
1. `StudentRegistrationService` içindeki `if (!StringUtils.hasText(existingUser.getRole())) { existingUser.setRole(STUDENT_ROLE); dirty = true; }` bloğunu silebilirsin.
2. `JwtAuthFilter` içindeki `if (userId == null || role == null || role.toString().isBlank()) { sendUnauthorized... }` iflasını ve return satırını silebilirsin.

---

## 8. Gruplar Sayfasında "Danışman Durumu" Filtresinin Çalışmama Sorunu
**Tarih:** 28 Nisan 2026
**Dosya:** `frontend/lib/groups-api.ts`
**Amaç:** "No Advisor" veya "Has Advisor" seçilmesine rağmen filtrenin uygulanmayıp tüm grupların listelenmesi sorununu çözmek.

**Ne Değişti?**
Frontend, backend'e "no_advisor" değerini gönderirken bunu `"false"` kelimesine çeviriyordu. Backend'in `GroupSpecification` sınıfı ise `"false"` kelimesini tanımadığı için filtreyi pas geçiyordu. Frontend'deki `advisorAssigned === "has_advisor" ? "true" : "false"` çevirimi kaldırılarak, değerin ("no_advisor" veya "has_advisor" olarak) orijinal haliyle backend'e iletilmesi sağlandı.

**Nasıl Geri Alınır? (Eski Haline Döndürme):**
`groups-api.ts` içindeki `params.append("advisorAssigned", advisorAssigned);` satırını tekrar `params.append("advisorAssigned", advisorAssigned === "has_advisor" ? "true" : "false");` olarak değiştirebilirsin.

---

## 9. Öğrenci Listesi "Data" Objensi Uyumsuzluğunun Giderilmesi (Members)
**Tarih:** 28 Nisan 2026
**Dosya:** `frontend/app/coordinator/members/page.tsx`
**Amaç:** Koordinatör panelindeki Members (Öğrenci Yönetimi) sayfasında, veritabanında öğrenci bulunmasına rağmen listenin boş (`[]`) gelmesi hatasını (Hata 1) çözmek. Backend'den gelen cevabın içindeki array ismi yanlış okunduğu için sayfada tablolar dolmuyordu.

**Ne Değişti?**
API'den dönen cevaptaki dizi `res.data.data` altındayken, UI kodu bunu `res.data.content` olarak okumaya çalışıyordu. İlgili satırdaki `.content` kelimesi `.data` olarak değiştirildi.

**Nasıl Geri Alınır? (Eski Haline Döndürme):**
`page.tsx` dosyasındaki şu satırı bulup:
`const data = Array.isArray(res.data) ? res.data : res.data.data || [];`
Tekrar şu şekilde hatalı hale döndürebilirsin:
`const data = Array.isArray(res.data) ? res.data : res.data.content || [];`

---

## 10. Öğrenci Tip Uyuşmazlığı ve React Key Hatası (Hata 2)
**Tarih:** 28 Nisan 2026
**Dosya:** `frontend/app/coordinator/members/page.tsx`
**Amaç:** Backend'den dönen cevaptaki `userId` alanı yerine frontend'in yanlışlıkla `id` beklemesi sonucu oluşan "unique key prop" React çökme hatasını gidermek.

**Ne Değişti?**
`Student` tipindeki `id` alanı `userId` olarak değiştirildi ve backend'den gelen `studentId` (okul numarası) da tipe eklendi. Tabloda öğrenci listelenirken `student.id` yazan tüm yerler `student.userId` yapıldı. Böylece React'ın `undefined` key hatası vermesi engellendi.

**Nasıl Geri Alınır? (Eski Haline Döndürme):**
`page.tsx` dosyasındaki `userId` ve `studentId` tanımlamalarını silip tekrar sadece `id: number;` bırakarak sayfayı eski kırık haline çevirebilirsin.

---

## 11. Öğrenciyi Gruptan Çıkaramama / Yanlış ID (Hata 3)
**Tarih:** 28 Nisan 2026
**Dosya:** `frontend/app/coordinator/members/page.tsx`
**Amaç:** Koordinatörün bir öğrenciyi gruba ekleme (Assign) veya gruptan çıkarma (Remove) işlemlerinin, backend API'sinin beklediği format yüzünden 404/Bad Request hatası vermesini önlemek.

**Ne Değişti?**
Eskiden Remove ve Assign işlemlerinde frontend, backend'e öğrencinin veritabanı ID'sini (`userId` örneğin 42) gönderiyordu. Oysa backend `GroupServiceImpl` içinde gruptan çıkarma/ekleme yaparken bunu okul numarası (`studentId`, örneğin "11070001000") olarak bekliyordu. Bu yüzden `handleRemove` ve `handleAssign` fonksiyonları güncellenerek backend'e sayfa üzerinden `student.studentId` bilgisinin yollanması sağlandı. Ayrıca Members sayfası görünür olması için `Sidebar.tsx` bileşenine eklendi.

**Nasıl Geri Alınır? (Eski Haline Döndürme):**
`page.tsx` içinde `handleRemove` fonksiyonunda yollanan `${studentId}` parametresini tekrar `userId` değerini yollayacak şekilde geri alabilirsin. `Sidebar.tsx` dosyasından `<NavItem label="Members" .../>` kodunu silebilirsin.

---

## 12. Members Sayfasında Yanlış Menü Seçimi ve Ekrana Vuran Fetch Hatası
**Tarih:** 28 Nisan 2026
**Dosya:** `frontend/app/coordinator/members/page.tsx`, `frontend/app/coordinator/student-ids/page.tsx`
**Amaç:** Members sayfasına girildiğinde sol menüde "Members" yerine yanlışlıkla "Student IDs" sekmesinin mavi (aktif) yanması sorununu çözmek ve sayfa geçişlerinde alakasız `Failed to fetch users` kırmızı ekran hatasının (throw Error) kullanıcıya yansımasını engellemek.

**Ne Değişti?**
1. `members/page.tsx` dosyasında `<Sidebar activePage="student-ids" />` kodu, sayfayı doğru temsil etmesi için `<Sidebar activePage="members" />` olarak güncellendi.
2. `student-ids/page.tsx` dosyasında API 404/401 döndüğünde React'ı çökerten `throw new Error("Failed to fetch users");` satırı kaldırılarak, hata sadece konsola `console.warn` ile düşecek ve sistemi kilitlemeyecek şekilde (graceful handling) değiştirildi. Backend ellenmeden frontend üzerinden çözüldü.

**Nasıl Geri Alınır? (Eski Haline Döndürme):**
`members/page.tsx` dosyasında `activePage="student-ids"` değişikliğini geri alabilir ve `student-ids/page.tsx` dosyasında `console.warn` yerine tekrar `throw new Error(...)` yazabilirsin.

---

## 13. Arama/Filtreleme İşleminin Çalışmaması ve Fallback Hatası (Hata 6 ve 7)
**Tarih:** 28 Nisan 2026
**Dosya:** `frontend/app/coordinator/members/page.tsx`
**Amaç:** Backend'in arama çubuğundan gelen `search=abc` parametresini tanımaması nedeniyle, ekrandaki öğrenci listesinde filtreleme yapılamaması sorununu frontend tarafında anlık filtreleme (client-side filtering) yöntemiyle çözmek. Ayrıca bu durumdan kaynaklanan işlevsiz fallback (alternatif API denemesi) hatalarını temizlemek.

**Ne Değişti?**
1. Sayfaya konulan butona basarak backend'e gereksiz API isteği atan eski `handleSearch` metodu tamamen silindi.
2. Sayfa açılır açılmaz (`useEffect` içinde) çalışacak bir `fetchStudents()` fonksiyonu yazıldı, böylece artık sayfaya girince tüm öğrenciler anında listeleniyor.
3. Kullanıcı arama kutusuna yazı yazdığı anda (`onChange`), `students` listesindeki veriler frontend üzerinde JavaScript kullanılarak `filteredStudents` adıyla anlık olarak filtrelendi. Tablo da bu `filteredStudents` üzerinden oluşturuldu. (Eski "Search" butonu kaldırıldı).

**Nasıl Geri Alınır? (Eski Haline Döndürme):**
`page.tsx` içindeki `useEffect` bloklarını ve `fetchStudents` metodunu geri alıp yerine tekrar `handleSearch` fonksiyonunu koyarak ve tabloda `filteredStudents.map` yerine `students.map` döngüsünü kullanarak kodu eski haline döndürebilirsin.

---

## 14. Öğrenciyi Gruba Atama (Assign) İşleminde "403 Forbidden" Hatası
**Tarih:** 29 Nisan 2026
**Dosya:** `backend/src/main/java/com/spms/backend/controller/GroupController.java`, `frontend/app/coordinator/members/page.tsx`
**Amaç:** Koordinatörün bir öğrenciyi gruba eklemek istediğinde yetki hatası (403 Forbidden) almasını sağlayan eksik/yanlış endpoint sorununu gidermek.

**Ne Değişti?**
1. Frontend'in `POST /groups/{groupId}/members` adresine attığı istek aslında **"Grup Liderinin Öğrenci Davet Etmesi"** endpoint'ine gidiyordu. Bu endpoint, isteği atanın "o grubun lideri" olup olmadığını kontrol ediyordu. Koordinatör lider olmadığı için sistem isteği engelliyor ve 403 fırlatıyordu.
2. Backend'deki `GroupServiceImpl.java` dosyasında zaten yazılı olan `addMember` (direkt atama) fonksiyonu `GroupController.java` dosyasına eksik yazılmıştı (hiç dışarı açılmamıştı). `POST /groups/{groupId}/members/{studentId}` adıyla bu yeni endpoint eklendi.
3. Frontend'in de `handleAssign` fonksiyonu güncellenerek bu yeni, güvenli ve doğrudan atama yapan endpoint'i kullanması sağlandı.

**Nasıl Geri Alınır? (Eski Haline Döndürme):**
Backend `GroupController.java` içinden `@PostMapping("/{groupId}/members/{studentId}")` ile başlayan `addMember` metodunu silebilir ve frontend'de `apiClient.post(...)` metodunun içine tekrar eski `/groups/${groupId}/members` URL'sini koyabilirsin.

---

## 15. Teslim Tarihi (Deadline) ve Gecikme (Overdue) Bilgilerinin Eklenmesi (Hata 4)
**Tarih:** 29 Nisan 2026
**Dosya:** `backend/src/main/java/com/spms/backend/dto/response/SubmissionListResponse.java`, `backend/src/main/java/com/spms/backend/service/impl/SubmissionServiceImpl.java`
**Amaç:** Koordinatör ve öğrencilerin teslimler listesinde her teslim için "Son Teslim Tarihi (deadline)" ve "Gecikti mi? (isOverdue)" bilgilerinin gelmemesi sorununu çözmek.

**Ne Değişti?**
1. `SubmissionListResponse.java` dosyasındaki `SubmissionSummary` Java record'una `deadline` (LocalDateTime) ve `isOverdue` (Boolean) alanları eklendi.
2. `SubmissionServiceImpl.java` dosyasındaki `listSubmissions` metoduna, dökümanları listelemeden önce sistemin takvimini (`scheduleRepository.findTopByOrderByIdDesc()`) çeken bir satır eklendi.
3. Aynı dosyaya `resolveDeadline(DeliverableType, Schedule)` adıyla yeni bir yardımcı (private) metod eklendi. Bu metod, teslim edilen döküman tipine göre ilgili deadline'ı takvimden okur:
   - `PROPOSAL` / `REVISED_PROPOSAL` → `proposalRevisionDeadline`
   - `STATEMENT_OF_WORK` / `DEMONSTRATION` → `gradingDeadline`
4. Her bir teslim için `createdAt` (öğrencinin teslim ettiği zaman) bu deadline ile karşılaştırılarak `isOverdue = true/false` olarak hesaplanıp API cevabına eklendi.

**Nasıl Geri Alınır? (Eski Haline Döndürme):**
`SubmissionListResponse.java` dosyasındaki `SubmissionSummary` record'unda son iki alanı (`deadline`, `isOverdue`) silebilir ve `SubmissionServiceImpl.java` içindeki `schedule` değişkeni ile `resolveDeadline` metodunu kaldırıp `new SubmissionSummary(...)` çağrısından son iki parametreyi çıkarabilirsin.

---

## 16. Deadline Filtrelemesi (deadlineStatus) Çalışmama Sorununun Çözümü (Hata 5)
**Tarih:** 29 Nisan 2026
**Dosya:** `backend/src/main/java/com/spms/backend/service/SubmissionService.java`, `backend/.../controller/SubmissionController.java`, `backend/.../service/impl/SubmissionServiceImpl.java`
**Amaç:** Koordinatörün teslimler (Submissions) sayfasında "Overdue" veya "Approaching" filtresini seçmesine rağmen filtrenin uygulanmayıp tüm teslimlerin listelenmesi sorununu çözmek.

**Ne Değişti?**
1. `SubmissionService.java` (interface) içindeki `listSubmissions` metoduna `String deadlineStatus` parametresi eklendi.
2. `SubmissionController.java` dosyasındaki `listSubmissions` endpoint'ine `@RequestParam(required = false) String deadlineStatus` eklenerek frontend'in gönderdiği parametre artık okunuyor. Bu değer de service metoduna iletildi.
3. `SubmissionServiceImpl.java` içindeki `listSubmissions` metoduna şu mantık eklendi:
   - Veritabanından tüm sonuçlar getirilip her biri için `isOverdue` hesaplandıktan sonra, `deadlineStatus` parametresine göre in-memory filtreleme yapılıyor.
   - `OVERDUE` seçilmişse: yalnızca `isOverdue == true` olanlar döndürülür.
   - `APPROACHING` seçilmişse: deadline'ı belirlenmiş ama henüz geçmemiş (overdue olmayan) teslimler döndürülür.
   - Filtre boşsa: tüm teslimler döndürülür (mevcut davranış korunur).

**Nasıl Geri Alınır? (Eski Haline Döndürme):**
- `SubmissionService.java`'dan `deadlineStatus` parametresini silin.
- `SubmissionController.java`'dan `@RequestParam String deadlineStatus` satırını ve servis çağrısındaki bu parametreyi silin.
- `SubmissionServiceImpl.java`'dan `filtered` değişkeni ve deadline filtre bloğunu kaldırıp `return new SubmissionListResponse("success", items, meta)` olarak geri alın.



---

## 17. Members Sayfasında "Current Group" ve Remove Butonunun Görünmeme Sorunu
**Tarih:** 29 Nisan 2026
**Dosya:** `backend/src/main/java/com/spms/backend/dto/response/UserResponse.java`, `backend/.../controller/UserController.java`
**Amaç:** Koordinatör panelindeki Members sayfasında tüm öğrencilerin "No group" göstermesi ve Remove butonunun hiç çıkmaması sorununu çözmek.

**Sorunun Kökü:**
`GET /users?role=student` endpoint'i kullanıcıları listelerken `UserResponse.UserData` içinde `groupId` ve `groupName` alanları yoktu. Frontend bu alanları göremeyince her öğrencinin grupsuz olduğunu sanıyordu.

**Ne Değişti?**
1. `UserResponse.UserData` record'una `Long groupId` ve `String groupName` alanları eklendi.
2. `UserController.getAllUsers` metoduna `GroupMemberRepository` inject edildi.
3. Her kullanıcı listelenirken `groupMemberRepository.findTopByUser_UserId(userId)` ile o kullanıcının grubu sorgulanıp sonuç API cevabına eklendi.
4. Frontend zaten bu alanları kullandığından frontend'de değişiklik gerekmedi.

**Nasıl Geri Alınır?**
`UserResponse.java`'daki `groupId` ve `groupName` alanlarını ve `UserController.java`'daki `GroupMemberRepository` injection + lookup kodunu kaldır.

---

## 18. Test Altyapısı ve Java 25 Uyumluluk Düzenlemeleri
**Tarih:** 29 Nisan 2026
**Dosya:** `backend/pom.xml`, `backend/.../service/impl/GroupServiceImpl.java`, `backend/.../controller/UserControllerTest.java`, `backend/.../service/SubmissionServiceTest.java`
**Amaç:** Backend testlerinin yeni eklenen özelliklerle uyumlu çalışmasını sağlamak, Java 25 kaynaklı Mockito hatalarını gidermek ve yerel dil (Turkish locale) kaynaklı bugları temizlemek.

**Yapılan Teknik Düzenlemeler:**
1.  **Java 25 & Byte Buddy Uyumluluğu:** Kullanılan Java versiyonunun (Java 25) Mockito'nun alt bileşeni olan Byte Buddy ile resmi olarak desteklenmemesi nedeniyle oluşan `IllegalStateException` hatası, `pom.xml` dosyasına eklenen `maven-surefire-plugin` konfigürasyonu (`-Dnet.bytebuddy.experimental=true`) ile çözüldü.
2.  **Turkish Locale Bug Fix:** `GroupServiceImpl.java` içerisinde Enum isimlerini küçük harfe çevirirken (`.toLowerCase()`) sistem dilinden kaynaklanan "I -> ı" (noktasız i) dönüşümü (`ACTIVE` -> `actıve`), `java.util.Locale.ROOT` parametresi eklenerek (`.toLowerCase(Locale.ROOT)`) standart hale getirildi. Bu sayede testlerin her sistemde (Türkçe macOS dahil) geçmesi sağlandı.
3.  **Metot İmzası Güncellemeleri (Refactoring):**
    *   `SubmissionService.listSubmissions` metoduna eklenen `deadlineStatus` parametresi nedeniyle bozulan `SubmissionServiceTest` içerisindeki tüm test çağrıları güncellendi.
    *   `UserController` constructor'ına eklenen `GroupMemberRepository` bağımlılığı nedeniyle bozulan `UserControllerTest` setup aşaması Mockito `mock()` kullanılarak modernize edildi.
4.  **LazyInitializationException (500 Hatası) Çözümü:** `UserController.getAllUsers` metodunda kullanıcıların grup bilgilerine erişilirken alınan 500 hatası, metoda `@Transactional(readOnly = true)` annotasyonu eklenerek ve transaction açık tutularak çözüldü.

**Sonuç:**
Tüm backend testleri (özellikle `SubmissionServiceTest` ve `UserControllerTest`) şu an Java 25 üzerinde hatasız ve stabil şekilde çalışmaktadır. Görsel IDE uyarıları ve gerçek çalışma hataları tamamen giderilmiştir.

---

## 19. Performans: Groups Sayfasında N+1 API Döngüsünün Kaldırılması
**Tarih:** 29 Nisan 2026
**Dosyalar:**
- `frontend/app/groups/page.tsx`
- `frontend/lib/groups-api.ts`
- `backend/.../controller/UserController.java`

**Sorun:**
Öğrenci rolüyle gruplar sayfası açıldığında, sayfadaki 6 grup için arka arkaya (sıralı, paralel değil) `GET /groups/{id}` isteği gönderiliyordu. Bu istek, öğrencinin kendi grubunu tespit etmek için yapılıyordu. Worst-case: 6 × ~150ms = **~900ms ekstra gecikme**. Filtreler her değiştiğinde bu döngü yeniden başlıyordu.

**Neden Yapıldı:**
6 sıralı HTTP isteği yerine tek bir `GET /api/v1/users/me` isteğiyle öğrencinin `groupId`'si doğrudan alınabilir. Backend zaten `UserResponse.UserData`'ya `groupId` alanını eklemiş durumdaydı — sadece `getMe()` endpoint'i bu alanı `null` gönderiyordu.

**Ne Değişti:**
1. **Backend `UserController.getMe()`:** Artık `GroupMemberRepository.findTopByUser_UserId()` ile öğrencinin grubu sorgulanıp `groupId` ve `groupName` dolu olarak dönüyor. `@Transactional(readOnly = true)` eklendi (lazy loading için gerekli).
2. **`groups-api.ts`:** `fetchCurrentUserGroupId()` adlı yeni yardımcı fonksiyon eklendi. Tek bir `/users/me` isteği atar, `data.groupId`'yi döndürür.
3. **`groups/page.tsx`:**
   - `fetchGroupDetail` içeren `for` döngüsü tamamen kaldırıldı.
   - `fetchCurrentUserGroupId()` tek satırla çağrılıyor.
   - `decodeToken`, `getToken`, `currentUserId` artık kullanılmadığından kaldırıldı.
   - `useEffect` bağımlılık dizisinden `currentUserId` kaldırıldı.

**Sonuç:**
`6 sıralı GET /groups/{id}` isteği → `1 adet GET /users/me` isteği.

**Nasıl Geri Alınır:**
`groups/page.tsx` içine eski `for` döngüsünü geri koy ve `fetchGroupDetail` import'unu ekle. `groups-api.ts`'den `fetchCurrentUserGroupId()` fonksiyonunu sil. `UserController.getMe()`'deki membership lookup kodunu ve `@Transactional` annotasyonunu kaldır.

---

## 20. Performans: Backend N+1 SQL Sorgusu — `mapToSimpleDto`
**Tarih:** 29 Nisan 2026
**Dosyalar:**
- `backend/.../model/Group.java`
- `backend/.../model/User.java`
- `backend/.../service/impl/GroupServiceImpl.java`

**Sorun:**
`getGroups()` endpoint'i 6 grup döndürürken `mapToSimpleDto()` metodu her grup için ayrı ayrı SQL sorgusu tetikliyordu:
- `group.getLeader().getUserId()` → leader lazy load → 1 SQL
- `group.getAdvisor().getUserId()` → advisor lazy load → 1 SQL
- `group.getMembers().size()` → members koleksiyonu lazy load → 1 SQL

6 grup için = **6×3 = 18 ekstra SQL sorgusu** her istekte.

**Neden Yapıldı:**
Her grubun üye sayısı için tüm `group_members` koleksiyonunu bellekte yüklemek ve leader/advisor'ı ayrı sorgularla çekmek yerine daha verimli yaklaşımlar mevcut.

**Ne Değişti:**

1. **`Group.java` — `@Formula` ile `memberCount`:**
   - `@Formula("(SELECT COUNT(*) FROM group_members gm WHERE gm.group_id = id)")` alanı eklendi.
   - Hibernate bu alt sorguyu ana SQL'e dahil eder → ayrı bir `SELECT members` sorgusu **hiç atılmaz**.
   - `getMemberCount()` getter'ı eklendi.

2. **`User.java` — `@BatchSize(size = 50)`:**
   - Sınıfa `@BatchSize(size = 50)` annotasyonu eklendi.
   - Hibernate, `leader` ve `advisor` lazy load'larını artık tek tek değil **toplu** (batch IN sorgusu) çeker.
   - 6 grup için: 6×2 = 12 ayrı SQL yerine → **2 toplu SQL** (`SELECT ... WHERE id IN (...)`)

3. **`GroupServiceImpl.mapToSimpleDto()`:**
   - `group.getMembers().size()` → `group.getMemberCount()` olarak değiştirildi.
   - Artık members koleksiyonu bu yoldan hiç yüklenmez.

**Sonuç:**
6 grup için yaklaşık 18 SQL sorgusu → yaklaşık 3 SQL sorgusu (1 ana sorgu + 2 batch User sorgusu).

**Nasıl Geri Alınır:**
- `Group.java`'dan `@Formula` alanını ve `getMemberCount()` getter'ını sil, `import org.hibernate.annotations.Formula;` satırını kaldır.
- `User.java`'dan `@BatchSize(size = 50)` ve `import org.hibernate.annotations.BatchSize;` satırlarını kaldır.
- `GroupServiceImpl.mapToSimpleDto()`'da `group.getMemberCount()` → `group.getMembers().size()` olarak geri al.

---

## 21. Performans: `findAllWithStudentGroupFirst` Pagination Bug — Duplicate Rows
**Tarih:** 29 Nisan 2026
**Dosya:** `backend/.../repository/GroupRepository.java`

**Sorun:**
Öğrenci rolüyle filtre uygulanmadan gruplar listelendiğinde kullanılan `findAllWithStudentGroupFirst` sorgusu `LEFT JOIN g.members m` içeriyordu. Bu JOIN, her grup için `group_members` tablosundaki kadar satır üretiyordu (örneğin 3 üyeli bir grup sonuç setinde 3 kez tekrar ediyordu).

Spring Data JPA'nın `Pageable` pagination'ı (LIMIT/OFFSET) bu ham SQL satırlarına uygulandığı için:
- `size=6` ile istenen sayfa sadece 2-3 grup döndürebiliyordu (satır limitine göre).
- `totalPages` yanlış hesaplanıyordu (toplam satır / 6, gerçek grup sayısı / 6 değil).
- Hibernate bu durumda `HHH90003004` uyarısı loglar ve pagination'ı bellekte yapar — tüm tabloyu RAM'e çeker.

**Neden Yapıldı:**
Öğrencinin kendi grubunu sıralamada öne almak için JOIN yerine EXISTS alt sorgusu kullanılabilir. Alt sorgu sadece koşulu kontrol eder, satır çoğaltmaz.

**Ne Değişti:**
`GroupRepository.java` içindeki `findAllWithStudentGroupFirst` sorgusu:

Eski (hatalı):
```java
@Query("SELECT g FROM Group g LEFT JOIN g.members m " +
       "ORDER BY CASE WHEN m.user.userId = :studentId THEN 0 ELSE 1 END, g.id ASC")
```

Yeni (doğru):
```java
@Query("SELECT g FROM Group g " +
       "ORDER BY CASE WHEN EXISTS " +
       "(SELECT 1 FROM GroupMember m WHERE m.group = g AND m.user.userId = :studentId) " +
       "THEN 0 ELSE 1 END, g.id ASC")
```

**Teknik Fark:**
- `LEFT JOIN g.members m` → her üye için ayrı satır → N üyeli grup N kez gelir → pagination bozulur
- `EXISTS (SELECT 1 FROM GroupMember ...)` → sadece boolean kontrol → her grup tam olarak 1 kez gelir → pagination doğru çalışır

**Sonuç:**
Sayfa başına gerçekten 6 grup gelir. `totalPages` doğru hesaplanır. Hibernate bellekte filtreleme yapmaz.

**Nasıl Geri Alınır:**
`GroupRepository.java`'da `findAllWithStudentGroupFirst` sorgusunu eski haliyle (`LEFT JOIN g.members m`) değiştir.

---

## 22. Performans: `getAllUsers` N+1 SQL — Toplu Membership Sorgusu
**Tarih:** 29 Nisan 2026
**Dosyalar:**
- `backend/.../repository/GroupMemberRepository.java`
- `backend/.../controller/UserController.java`

**Sorun:**
`GET /api/v1/users` endpoint'i her kullanıcı için ayrı ayrı `findTopByUser_UserId(userId)` çağırıyordu. 50 kullanıcı → 50 ek SQL sorgusu. Koordinatör sayfası her açıldığında bu istek gönderildiğinden ciddi gecikmeye neden oluyordu.

**Neden Yapıldı:**
Tüm `GroupMember` kayıtlarını tek bir sorguda çekip `userId → GroupMember` map'i kurarsak, kullanıcı başına SQL atma ihtiyacı ortadan kalkar.

**Ne Değişti:**

1. **`GroupMemberRepository.java` — yeni sorgu eklendi:**
```java
@Query("SELECT gm FROM GroupMember gm JOIN FETCH gm.user JOIN FETCH gm.group")
List<GroupMember> findAllWithUserAndGroup();
```
`JOIN FETCH` ile hem `user` hem `group` ilişkisi tek sorguda eager yüklenir — ayrıca lazy load SQL atılmaz.

2. **`UserController.getAllUsers()` — N+1 döngüsü kaldırıldı:**

Eski (N+1):
```java
// her kullanıcı için ayrı SQL
Optional<GroupMember> membership = groupMemberRepository.findTopByUser_UserId(u.getUserId());
```

Yeni (1 sorgu):
```java
// 1 sorgu: tüm üyelikler
Map<Long, GroupMember> membershipMap = groupMemberRepository.findAllWithUserAndGroup()
    .stream()
    .collect(Collectors.toMap(gm -> gm.getUser().getUserId(), gm -> gm, (a, b) -> a));

// döngüde map lookup — SQL yok
GroupMember membership = membershipMap.get(u.getUserId());
```

`(a, b) -> a` merge fonksiyonu: aynı kullanıcıya ait birden fazla üyelik kaydı varsa ilkini korur (tutarlılık için).

**Sonuç:**
N kullanıcı için N+1 SQL → **2 SQL** (1 kullanıcı listesi + 1 toplu üyelik sorgusu).

**Nasıl Geri Alınır:**
- `GroupMemberRepository.java`'dan `findAllWithUserAndGroup()` metodunu sil.
- `UserController.getAllUsers()`'daki `membershipMap` bloğunu kaldır, eski `findTopByUser_UserId` çağrısına dön.
- `UserController.java`'dan `import java.util.Map;` satırını kaldır.

---

## 23. Performans: Koordinatör İsteklerinde Gereksiz `countGroupsByStatus()` Sorgusunun Kaldırılması
**Tarih:** 29 Nisan 2026
**Dosya:** `backend/.../service/impl/GroupServiceImpl.java`

**Sorun:**
Koordinatör rolüyle filtre uygulanmadan gruplar her listelendiğinde (`GET /groups` — filtre yok), `getGroups()` metodu ana sorgunun yanına bir de şunu atıyordu:

```java
List<Object[]> counts = groupRepository.countGroupsByStatus();
counts.forEach(result -> log.info("Coordinator Summary - Status: {} Count: {}", result[0], result[1]));
```

Bu `SELECT g.status, COUNT(g) FROM Group g GROUP BY g.status` sorgusu sadece **server log'una** yazmak için atılıyordu. Kullanıcıya hiçbir şey dönmüyor, frontend hiçbir şekilde bu veriyi kullanmıyordu.

**Neden Yapıldı:**
Koordinatör gruplar sayfasını her açtığında ekstra bir `GROUP BY` SQL sorgusu tetikleniyordu. Üretim ortamında gruplar tablosu büyüdükçe bu sorgu yavaşlar; üstelik sağladığı değer sıfır (sadece log).

**Ne Değişti:**
`GroupServiceImpl.getGroups()` içindeki `if ("coordinator".equals(role) && !hasFilters)` bloğunun tamamı kaldırıldı. `countGroupsByStatus()` metodu `GroupRepository`'de bırakıldı (başka yerden kullanılıyor olabilir).

**Sonuç:**
Koordinatör her gruplar sayfasını açtığında atılan 1 gereksiz `GROUP BY` SQL kaldırıldı.

**Nasıl Geri Alınır:**
`GroupServiceImpl.getGroups()` içinde `groupRepository.findAll(spec, pageable);` satırının hemen ardına şu bloğu geri ekle:
```java
if ("coordinator".equals(role) && !hasFilters) {
    try {
        List<Object[]> counts = groupRepository.countGroupsByStatus();
        counts.forEach(result -> log.info("Coordinator Summary - Status: {} Count: {}", result[0], result[1]));
    } catch (Exception e) {
        log.error("Status summary count failed", e);
    }
}
```

---

## 24. Performans: Members Sayfasında Sıralı API İstekleri ve Gereksiz Liste Yenileme
**Tarih:** 29 Nisan 2026
**Dosya:** `frontend/app/coordinator/members/page.tsx`

**Sorunlar:**

**Sorun 1 — Sayfa açılışında sıralı (sequential) API istekleri:**
`useEffect` içinde önce `/users?role=student`, ardından `/groups?size=1000` isteği atılıyordu. Bu iki istek birbirinin sonucuna bağlı olmadığı halde sırayla bekliyordu. Toplam yükleme süresi: `t(users) + t(groups)`. Paralel yapılsaydı sadece `max(t(users), t(groups))` olurdu.

**Sorun 2 — Her Assign/Remove işlemi sonrası tüm liste yeniden yükleniyordu:**
`handleAssign` ve `handleRemove` başarıyla tamamlandığında `fetchStudents()` çağrılıyordu. Bu, tüm öğrenci listesini sunucudan baştan çekiyordu (yüzlerce öğrenci varsa yüzlerce satır). Oysa sadece o tek öğrencinin satırını güncellemek yeterliydi.

**Ne Değişti:**

1. **Paralel istek — `Promise.all`:**
```typescript
// Eski: sıralı
apiClient.get('/groups...').then(...)   // bekle...
fetchStudents();                        // sonra bu

// Yeni: paralel
Promise.all([
    apiClient.get(`/users?role=student`),
    apiClient.get('/groups?size=1000&page=0'),
]).then(([studentsRes, groupsRes]) => { ... })
```

2. **Local state güncelleme:**
```typescript
// Eski: tüm listeyi yeniden çek
fetchStudents();

// Yeni — Assign sonrası: sadece o öğrencinin satırını güncelle
setStudents(prev => prev.map(s =>
    s.userId === userId
        ? { ...s, groupId: Number(groupId), groupName: assignedGroup?.groupName }
        : s
));

// Yeni — Remove sonrası:
setStudents(prev => prev.map(s =>
    s.userId === userId ? { ...s, groupId: null, groupName: null } : s
));
```

**Sonuç:**
- Sayfa açılış süresi: `t(users) + t(groups)` → `max(t(users), t(groups))` (yaklaşık yarıya indi)
- Her assign/remove sonrası: 1 tam liste isteği → 0 istek (anlık UI güncellemesi)

**Nasıl Geri Alınır:**
`useEffect`'i eski `fetchStudents()` + ayrı groups isteği şeklinde ayır. `handleAssign` ve `handleRemove`'daki `setStudents(...)` bloklarını kaldırıp `fetchStudents()` çağrılarını geri ekle.
