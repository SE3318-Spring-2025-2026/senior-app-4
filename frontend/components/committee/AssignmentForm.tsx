"use client";

type Professor = {
    id: number;
    name: string;
};

type Props = {
    professors: Professor[];
    onSubmit: (professorId: number) => void;
    type: "ADVISOR" | "JURY";
    error?: string;
};

export default function AssignmentForm({
    professors,
    onSubmit,
    type,
    error,
}: Props) {
    let selectedId = "";

    function handleSubmit(e: React.FormEvent) {
        e.preventDefault();

        const id = Number(selectedId);
        if (!id) return;

        onSubmit(id);
    }

    return (
        <form onSubmit={handleSubmit} className="space-y-4">
            {error && (
                <div
                    role="alert"
                    className="text-red-300 bg-red-500/10 border border-red-500/20 px-4 py-2 rounded-xl text-sm"
                >
                    {error}
                </div>
            )}

            <div>
                <label className="block text-sm text-gray-300 mb-2">
                    Select Professor
                </label>

                <select
                    onChange={(e) => (selectedId = e.target.value)}
                    className="w-full rounded-xl bg-gray-900 border border-white/10 px-4 py-3 text-white"
                >
                    <option value="">Select...</option>
                    {professors.map((p) => (
                        <option key={p.id} value={p.id}>
                            {p.name}
                        </option>
                    ))}
                </select>
            </div>

            <button
                type="submit"
                className="rounded-xl bg-blue-600 px-5 py-3 text-sm font-semibold text-white hover:bg-blue-500"
            >
                Assign
            </button>
        </form>
    );
}