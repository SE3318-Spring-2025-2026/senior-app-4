export function formatDate(date?: string | null): string {
    if (!date) return "-";
    return new Date(date).toLocaleString("en-US", {
        dateStyle: "medium",
        timeStyle: "short",
    });
}
