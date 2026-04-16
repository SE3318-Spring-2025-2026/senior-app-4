# Contributing to S.P.M.S.

Öncelikle "Senior Project Managment System" projesine katkıda bulunduğunuz için teşekkür ederim <3. Bu doküman, takım içindeki geliştirme süreçlerimizi standartlaştırmak, kod kalitemizi artırmak ve sorunsuz bir inceleme (Review) süreci geçirmemizi sağlamak amacıyla hazırlanmıştır.

Lütfen herhangi bir kod yazmaya veya Pull Request (PR) açmaya başlamadan önce bu yönergeleri dikkatlice okuyun.

## 1. Branch (Dallanma) Stratejisi

Projemizde temiz ve anlaşılır bir Git geçmişi tutmak için aşağıdaki isimlendirme standartlarını kullanıyoruz:

* **Yeni Özellikler:** `feature/kisa-ozellik-adi` (Örn: `feature/github-oauth`, `feature/professor-login`)
* **Hata Çözümleri:** `bugfix/kisa-hata-adi` (Örn: `bugfix/jwt-token-error`)
* **Dokümantasyon:** `docs/guncellenen-belge` (Örn: `docs/api-yaml-update`)
* **Acil Düzeltmeler:** `hotfix/kritik-hata` (Sadece ana dalda çıkan acil durumlar için)

Lütfen doğrudan `main` veya `develop` dallarına kod **pushlamayın**.

## 2. Commit Mesajı Standartları

Commit mesajlarımızın yapay zeka entegrasyonumuz ve danışman incelemeleri tarafından rahatça okunabilmesi için [Conventional Commits](https://www.conventionalcommits.org/) standardını benimsiyoruz:

* `feat:` Yeni bir özellik eklendiğinde (Örn: `feat: add professor login page`)
* `fix:` Bir hata giderildiğinde (Örn: `fix: resolve db connection timeout`)
* `docs:` Sadece dokümantasyon değiştiğinde (Örn: `docs: update setup instructions`)
* `refactor:` Yeni bir özellik eklemeyen ve hata çözmeyen kod düzenlemeleri
* `style:` Kodun çalışmasını etkilemeyen format (boşluk, noktalı virgül vb.) değişiklikleri

## 3. Pull Request (PR) ve İnceleme (Review) Süreci

Sistemimizin notlandırma mekanizması (Evaluation Rubric) ve yapay zeka analiz araçları PR yorumlarına entegre çalıştığı için bu süreç kritik öneme sahiptir.

1. Branch'inizde işiniz bittiğinde `main` (veya `develop`) dalına bir Pull Request açın.
2. PR başlığınız net olmalı ve ilişkili GitHub Issue numarasını içermelidir (Örn: `feat: add initial password change form (Resolves #4)`).
3. PR açıklamasında (description) neleri değiştirdiğinizi ve nasıl test edileceğini kısaca açıklayın.
4. **Reviewer (İnceleyici)** olarak takım lideriniz ile iletişime geçin.
5. Reviewer onay vermeden ve tüm otomatik testler/kontroller geçmeden PR merge edilemez (birleştirilemez).


## 4. Geliştirme Ortamını Kurma (Local Setup)

Projeyi kendi bilgisayarınızda çalıştırmak için aşağıdaki adımları izleyin:

1. Repoyu klonlayın: `git clone <repo-url>`
2. Bağımlılıkları yükleyin: `npm install` (veya backend için ilgili komut)
3. Ortam değişkenlerini ayarlayın: `.env.example` dosyasını `.env` olarak kopyalayın ve gerekli değerleri doldurun.
4. Geliştirme sunucusunu başlatın: `npm run dev`

Herhangi bir sorunuz olursa, lütfen proje takım lideriyle iletişime geçin.