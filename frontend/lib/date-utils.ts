export const isOverlapping = (
    startA: string | Date | null,
    endA: string | Date | null,
    startB: string | Date | null,
    endB: string | Date | null
): boolean => {
    if (!startA || !endA || !startB || !endB) return false;

    try {
        const sA = new Date(startA).getTime();
        const eA = new Date(endA).getTime();
        const sB = new Date(startB).getTime();
        const eB = new Date(endB).getTime();

        // Geçersiz tarih kontrolü (NaN)
        if (isNaN(sA) || isNaN(eA) || isNaN(sB) || isNaN(eB)) return false;

        return sA < eB && eA > sB;
    } catch (e) {
        return false;
    }
};