"use client";

import { useState } from "react";

export default function AdminProfessorRegisterPage() {
  const [formData, setFormData] = useState({
    fullName: "",
    email: "",
    password: "",
    role: "PROFESSOR", // Dropdown için varsayılan değer
  });
  
  const [message, setMessage] = useState({ type: "", text: "" });
  const [isLoading, setIsLoading] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setMessage({ type: "", text: "" });

    // 1. İster (Client-side validation): Geçerli Email Formatı
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(formData.email)) {
      setMessage({ type: "error", text: "Lütfen geçerli bir e-posta adresi giriniz." });
      return;
    }

    // 2. İster (Client-side validation): Güçlü Şifre Kontrolü (Min 8 karakter, 1 Büyük, 1 Küçük, 1 Sayı)
    const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[a-zA-Z\d\w\W]{8,}$/;
    if (!passwordRegex.test(formData.password)) {
      setMessage({ 
        type: "error", 
        text: "Şifre en az 8 karakter olmalı, en az bir büyük harf, bir küçük harf ve bir sayı içermelidir." 
      });
      return;
    }

    setIsLoading(true);

    try {
      // 3. İster: Backend Endpoint Entegrasyonu
      const response = await fetch("/api/v1/professors/register", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(formData),
      });

      const data = await response.json();

      if (response.ok) {
        // 4. İster: Başarı mesajı göster ve formu temizle
        setMessage({ type: "success", text: "Akademisyen/Koordinatör başarıyla kaydedildi!" });
        setFormData({ fullName: "", email: "", password: "", role: "PROFESSOR" });
      } else {
        // 5. İster: Backend hatalarını göster (Örn: Email already exists)
        setMessage({ type: "error", text: data.message || "Kayıt sırasında bir hata oluştu. E-posta zaten kayıtlı olabilir." });
      }
    } catch (err) {
      setMessage({ type: "error", text: "Sunucuya bağlanılamadı. Lütfen API bağlantısını kontrol edin." });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="flex flex-col items-center justify-center min-h-screen bg-gray-50 text-black p-4">
      <div className="bg-white border p-8 rounded-xl shadow-md w-full max-w-md">
        <div className="mb-6 border-b pb-4 text-center">
          <h1 className="text-2xl font-bold text-gray-800">Personel Ekle</h1>
          {/* Güvenlik Notu: Yetki kontrolü (Coordinator role) ideal olarak Next.js Middleware veya Layout seviyesinde yapılır */}
          <span className="inline-block mt-2 px-3 py-1 bg-blue-100 text-blue-800 text-xs font-semibold rounded-full">
            Sadece Koordinatör Yetkisi
          </span>
        </div>

        {message.text && (
          <div className={`p-4 mb-6 rounded-md text-sm font-medium ${message.type === "error" ? "bg-red-50 text-red-700 border border-red-200" : "bg-green-50 text-green-700 border border-green-200"}`}>
            {message.text}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Ad Soyad</label>
            <input
              type="text"
              name="fullName"
              required
              className="w-full p-3 border rounded-md border-gray-300 focus:ring-2 focus:ring-blue-500 outline-none"
              placeholder="Prof. Dr. İsim Soyisim"
              value={formData.fullName}
              onChange={handleChange}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">E-posta Adresi</label>
            <input
              type="email"
              name="email"
              required
              className="w-full p-3 border rounded-md border-gray-300 focus:ring-2 focus:ring-blue-500 outline-none"
              placeholder="ornek@universite.edu.tr"
              value={formData.email}
              onChange={handleChange}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Geçici Şifre</label>
            <input
              type="password"
              name="password"
              required
              className="w-full p-3 border rounded-md border-gray-300 focus:ring-2 focus:ring-blue-500 outline-none"
              placeholder="••••••••"
              value={formData.password}
              onChange={handleChange}
            />
            <p className="text-xs text-gray-500 mt-1">Min 8 karakter, 1 Büyük, 1 Küçük harf ve 1 Sayı.</p>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Rol</label>
            <select
              name="role"
              className="w-full p-3 border rounded-md border-gray-300 focus:ring-2 focus:ring-blue-500 outline-none bg-white"
              value={formData.role}
              onChange={handleChange}
            >
              <option value="PROFESSOR">Professor (Akademisyen)</option>
              <option value="COORDINATOR">Coordinator (Koordinatör)</option>
            </select>
          </div>

          <button 
            type="submit" 
            disabled={isLoading}
            className={`w-full text-white p-3 rounded-md font-medium transition-colors mt-2 ${isLoading ? "bg-blue-400 cursor-not-allowed" : "bg-blue-600 hover:bg-blue-700"}`}
          >
            {isLoading ? "İşleniyor..." : "Sisteme Kaydet"}
          </button>
        </form>
      </div>
    </div>
  );
}