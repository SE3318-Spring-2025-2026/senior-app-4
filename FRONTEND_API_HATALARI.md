# 🚀 Frontend API Hata Analizi ve Çözüm Rehberi

Merhaba! Projenin frontend kodlarını incelerken bazı API entegrasyon hataları tespit ettik. Bu hatalar, frontend ile backend'in birbiriyle doğru konuşamamasına sebep oluyor. 

Aşağıda bu hataların **ne olduğunu**, **neden sorun yarattığını** ve **nasıl çözebileceğimizi** senin için basit ve anlaşılır bir şekilde özetledim. Bu rehberi takip ederek hataları sırayla çözebilirsin. Kolay gelsin! 💪

---

## 🔴 KRİTİK HATALAR (Uygulamanın çalışmasını doğrudan engeller)

### 1. Authorization (Yetkilendirme) Header Eksikliği
**📍 Nerede:** `frontend/lib/client.ts` (Axios `apiClient` yapılandırması)

* **Hata Nedir?**
  Axios ile oluşturduğumuz `apiClient` içine sadece gelen cevapları yakalayan (response interceptor) bir kod yazılmış. Ancak bizim backend'e giden isteklere **"Ben şu kullanıcıyım"** diyebilmek için token (kimlik kartı) eklememiz gerekiyor. (Request interceptor eksik).
* **Neden Sorun Yaratır?**
  İsteklerimizde JWT token olmadığı için backend kim olduğumuzu bilemiyor. Haklı olarak bize `400 Bad Request` veya yetki hatası döndürüyor.
* **Nasıl Çözeriz?**
  `client.ts` dosyasına bir `axios.interceptors.request.use` eklemeliyiz. Bu kod, her istek gitmeden önce araya girip `Authorization` header'ına (başlığına) kullanıcının token'ını (`Bearer ...` şeklinde) eklemeli.

### 2. Yanlış Porta Giden İstekler (baseURL Hatası)
**📍 Nerede:** `frontend/lib/client.ts`

* **Hata Nedir?**
  `baseURL: '/api/v1'` olarak ayarlanmış. Bu şekilde bırakıldığında istekler, uygulamanın çalıştığı port olan `localhost:3000`'e (Next.js sunucusuna) gider. Oysa bizim backend'imiz `localhost:8080`'de çalışıyor.
* **Neden Sorun Yaratır?**
  Next.js sunucusu bu isteklerin ne anlama geldiğini bilmediği için aradığımız API'yi bulamaz ve `404 Not Found` hatası verir.
* **Nasıl Çözeriz?**
  `next.config.ts` dosyasında bir "Proxy (Yönlendirme)" kuralı yazmalıyız. Yani Next.js'e şunu demeliyiz: *"Eğer bir istek `/api/v1` ile başlıyorsa, bunu alıp otomatik olarak `http://localhost:8080`'e gönder."* Veya baseURL'i doğrudan `http://localhost:8080/api/v1` olarak değiştirebiliriz.

### 3. Yanlış Veri Tipi Gönderimi (String vs Number)
**📍 Nerede:** `frontend/lib/advisor-api.ts` (Satır 93)

* **Hata Nedir?**
  Kodu yazarken `teamId: String(groupId)` diyerek grup ID'sini zorla bir metne (String) çevirmişiz. 
* **Neden Sorun Yaratır?**
  Backend tarafında `teamId` bir sayı (`Long` veri tipi) olarak bekleniyor. Backend sayı beklerken biz ona metin (string) gönderdiğimizde, veriyi işleyemez ve "Bana yanlış formatta veri gönderdin" diyerek `400 Bad Request` hatası verir.
* **Nasıl Çözeriz?**
  Çok basit! `String()` çevirme işlemini kaldırıp değişkeni olduğu gibi sayı formatında göndermemiz yeterli: `teamId: groupId`

---

## 🟠 ÖNEMLİ HATALAR (Belirli sayfaların veya işlevlerin bozulmasına yol açar)

### 4. Büyük/Küçük Harf (Case-Sensitivity) Uyuşmazlığı
**📍 Nerede:** `frontend/app/coordinator/members/page.tsx` (Satır 100)

* **Hata Nedir?**
  Kullanıcıları getirirken URL'e rol parametresini `role=STUDENT` şeklinde büyük harflerle yazarak gönderiyoruz. Diğer sayfalarda ise `role=professor` şeklinde küçük harf kullanılmış.
* **Neden Sorun Yaratır?**
  Backend sistemleri büyük/küçük harf konusunda çok hassastır. Backend küçük harfle "student" bekliyorsa, büyük harfle "STUDENT" gönderdiğimizde eşleşme bulamaz. Bu da listelerin boş dönmesine neden olur.
* **Nasıl Çözeriz?**
  Parametreyi küçük harfe çevirmeliyiz: `role=student`

### 5. Olmayan (Hayalet) Endpoint Çağrısı
**📍 Nerede:** `frontend/app/coordinator/members/page.tsx` (Satır 106)

* **Hata Nedir?**
  Sistemde öğrencileri getirmek için `/students` diye bir adrese istek atılmış. 
* **Neden Sorun Yaratır?**
  Backend'de aslında `/students` diye bir yol (endpoint) tanımlanmamış. Öğrencileri getirmek için `/users` adresini kullanmalıyız. Olmayan bir adrese istek attığımız için hep başarısız olur ve veri alamayız.
* **Nasıl Çözeriz?**
  İsteği backend'in tanıdığı adrese çevirmeliyiz: `/users?role=student`

---

## 🟡 ORTA ÖNCELİKLİ HATALAR (Hatalı çalışır ama uygulamayı çökertmez)

### 6. Backend'de Karşılığı Olmayan Filtreler
**📍 Nerede:** `frontend/lib/groups-api.ts` ve `frontend/app/groups/page.tsx`

* **Hata Nedir?**
  Frontend tarafında grupları çekerken URL'e `status=FORMING`, `groupName=abc`, `advisorAssigned=true` gibi filtreleme kriterleri ekleyip gönderiyoruz.
* **Neden Sorun Yaratır?**
  Backend kodlarına (GroupController) baktığımızda, backend'in sadece "hangi sayfa" (`page`) ve "kaç adet" (`size`) verilerini okuduğunu görüyoruz. Gönderdiğimiz filtreler backend tarafından tamamen görmezden geliniyor. Sonuç olarak filtreleme çalışmıyor, kullanıcı arama yapsa bile hep bütün gruplar listeleniyor.
* **Nasıl Çözeriz?**
  İki seçeneğimiz var:
  1. **(Tavsiye Edilen)** Backend geliştiricisi ile konuşup bu filtreleri karşılayacak kodu (örneğin Spring Data JPA'da spesifikasyonlar veya queryler) backend'e eklemek.
  2. Frontend'de arayüzü geçici olarak sadeleştirip çalışmayan filtre seçeneklerini kullanıcıdan gizlemek.

---

### 💡 Geliştirici Tavsiyesi (Nasıl İlerlemelisin?)
Hataları düzeltmeye her zaman **🔴 Kritik** olanlardan başla. Özellikle `client.ts` içindeki Authorization ve Port (baseURL) hatalarını çözdüğünde, şu an çalışmayan birçok sayfanın aniden çalışmaya başladığını göreceksin! Başarılar dilerim.🚀
