import { NextResponse } from 'next/server';
import { supabase } from '@/lib/supabase';


export async function GET() {
  const { data, error } = await supabase.from('users').select('*');
  if (error) return NextResponse.json({ error: error.message }, { status: 500 });
  return NextResponse.json({ data, message: "Users retrieved successfully." }, { status: 200 });
}


export async function POST(request: Request) {
  try {
    const body = await request.json();
    const { fullName, email, studentId, githubUsername, role } = body;

    const validRoles = ['student', 'professor', 'coordinator'];
    if (!validRoles.includes(role)) {
      return NextResponse.json({ error: "Geçersiz rol." }, { status: 400 });
    }

    const { data, error } = await supabase
      .from('users')
      .insert([{ full_name: fullName, email, student_id: studentId, github_username: githubUsername, role }])
      .select()
      .single();

    if (error) throw error;
    return NextResponse.json({ data, message: "User created successfully." }, { status: 201 });
  } catch (error: any) {
    return NextResponse.json({ error: error.message }, { status: 400 });
  }
}