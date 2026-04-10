"use client";

import { useState } from "react";
import { signIn } from "next-auth/react";

export default function StudentLoginPage() {
  const [studentId, setStudentId] = useState("");
  const [error, setError] = useState("");
  const [isValidated, setIsValidated] = useState(false);

  const handleValidate = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");

    // 11 haneli numeric kontrolü
    if (!/^\d{11}$/.test(studentId)) {
      setError("Lütfen 11 haneli geçerli bir öğrenci numarası giriniz.");
      return;
    }

    try {
      // API Entegrasyonu (YAML: POST /students/validate)
      // CORS için localhost:8080 absolute URL'i eklendi!
      const response = await fetch("http://localhost:8080/api/v1/students/validate", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ studentId }),
      });

      const data = await response.json();

      if (response.ok && data.valid) {
        setIsValidated(true); // Başarılıysa GitHub butonunu göster
      } else {
        setError(data.message || "Öğrenci numarası sistemde bulunamadı.");
      }
    } catch (err) {
      setError("Bağlantı hatası oluştu. Lütfen sunucunun çalıştığından emin olun.");
    }
  };

  return (
    <div className="flex flex-col items-center justify-center min-h-screen bg-white text-black">
      <div className="border p-10 rounded-lg shadow-sm w-full max-w-sm">
        <h1 className="text-xl font-bold mb-6 text-center">Öğrenci Doğrulama</h1>
        {!isValidated ? (
          <form onSubmit={handleValidate} className="space-y-4">
            <input
              type="text"
              placeholder="Öğrenci No"
              className="w-full p-3 border rounded border-gray-300"
              value={studentId}
              onChange={(e) => setStudentId(e.target.value)}
            />
            {error && <p className="text-red-500 text-xs">{error}</p>}
            <button type="submit" className="w-full bg-blue-600 text-white p-3 rounded font-medium">
              Devam Et
            </button>
          </form>
        ) : (
          <div className="space-y-4">
            <p className="text-green-600 text-sm text-center font-medium">✓ Numara Onaylandı</p>
            <button 
              onClick={() => signIn("github")}
              className="w-full bg-black text-white p-3 rounded font-medium disabled:opacity-50"
            >
              GitHub ile Giriş Yap
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
