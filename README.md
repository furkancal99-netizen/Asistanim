# Asistanım 1.0 — Android başlangıç projesi

Bu proje Kotlin + Jetpack Compose ile hazırlanmış Android Studio başlangıç uygulamasıdır.

## İlk sürümde bulunanlar
- Türkçe sohbet arayüzü
- Android konuşma tanıma ile sesli giriş (cihazın tanıma servisine bağlı)
- Android Text-to-Speech ile sesli cevap
- Yerel not kaydetme ve listeleme (SharedPreferences)
- Saat sorgusu
- YouTube, WhatsApp, Chrome ve Ayarlar uygulamalarını açma (yüklü ve erişilebilir olmalarına bağlı)
- Bağlantı durumunu kontrol etme

## Açma
1. Android Studio'yu aç.
2. `Asistanim_Android` klasörünü Open ile seç.
3. Gradle senkronizasyonunun bitmesini bekle.
4. Android telefonunda geliştirici seçenekleri ve USB hata ayıklamayı aç veya emülatör kullan.
5. Run düğmesine bas.

## Önemli: Bulut yapay zekâ bağlantısı
Uygulamada henüz gerçek bir LLM/API bağlantısı yoktur. Güvenlik nedeniyle API anahtarı Android uygulamasına gömülmemelidir. Sonraki aşamada HTTPS kullanan bir backend uç noktası eklenmelidir. `assistantReply()` fonksiyonu bunun bağlanacağı yerdir.

## Çevrimdışı sınırı
İlk sürüm genel bilgi sorularına çevrimdışı cevap veren büyük bir model içermez. Notlar, saat ve uygulama açma gibi komutlar yerel çalışır. Tamamen yerel sohbet için ayrıca lisansı uygun bir küçük dil modeli ve cihaz uyumluluğu entegrasyonu gerekir.

## İzinler ve gizlilik
Mikrofon izni yalnızca kullanıcı mikrofon düğmesine bastığında istenir. Uygulama arka planda gizli dinleme yapmaz. Telefon işlemleri Android'in normal uygulama başlatma mekanizmasıyla yapılır.
