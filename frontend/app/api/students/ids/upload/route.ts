import { NextResponse } from 'next/server';
import { supabase } from '@/lib/supabase';

export async function POST(request: Request) {
  try {
    const formData = await request.formData();
    const file = formData.get('file') as File;

    if (!file) {
      return NextResponse.json({ error: "Dosya bulunamadı." }, { status: 400 });
    }

    const text = await file.text();
    const lines = text.split('\n').map(line => line.trim()).filter(line => line !== '');
    
    const insertData = lines.map(id => ({ student_id: id, status: 'valid' }));

    const { error } = await supabase.from('valid_student_ids').upsert(insertData, { onConflict: 'student_id' });

    if (error) throw error;

    return NextResponse.json({
      message: "Öğrenci ID'leri başarıyla yüklendi.",
      totalRecords: lines.length,
      validRecords: lines.length,
      invalidRecords: 0
    }, { status: 200 });

  } catch (error: any) {
    return NextResponse.json({ error: "Dosya işlenirken hata oluştu." }, { status: 400 });
  }
}