"use client";

import Sidebar from "@/components/Sidebar";
import Link from "next/link";
import { useEffect, useState, useRef } from "react"; // useRef eklendi
import { fetchCommittees } from "@/lib/committees-api";
import { Committee } from "@/lib/committee-types";
import { showToast } from "@/components/toast/ToastContext";
import { useRouter, useSearchParams, usePathname } from "next/navigation";

export default function CommitteesPage() {
    const [committees, setCommittees] = useState<Committee[]>([]);
    const [loading, setLoading] = useState(true);

    const router = useRouter();
    const searchParams = useSearchParams();
    const pathname = usePathname();

    const initialStatus = searchParams.get("status") || "ALL";
    const initialSearch = searchParams.get("search") || "";
    const initialSort = searchParams.get("sort") || "created_desc";
    const initialPage = Number(searchParams.get("page") || "1");
    const initialPageSize = Number(searchParams.get("size") || "10");

    const [statusFilter, setStatusFilter] = useState(initialStatus);
    const [searchInput, setSearchInput] = useState(initialSearch);
    const [searchQuery, setSearchQuery] = useState(initialSearch);
    const [sortOption, setSortOption] = useState(initialSort);
    const [page, setPage] = useState(initialPage);
    const [pageSize, setPageSize] = useState(initialPageSize);
    const [totalPages, setTotalPages] = useState(1);

    // YENİ: Yarış durumunu (race condition) önlemek için her isteğe bir ID vereceğiz
    const fetchIdRef = useRef(0);

    // YENİ: Senin özel renk tercihlerine göre güncellenmiş yardımcı fonksiyon
function getStatusColor(status: string | undefined) {
    switch (status) {
        case "ACTIVE":
            // turkuaz
            return "border-teal-500/20 bg-teal-500/10 text-teal-400";
            
        case "INACTIVE":
            // koyu pembe
            return "border-pink-500/20 bg-pink-500/10 text-pink-400";
            
        case "COMPLETED":
            // turuncu
            return "border-orange-500/20 bg-orange-500/10 text-orange-400";
            
        default:
            // Beklenmedik bir durum gelirse standart Gri tonu
            return "border-gray-500/20 bg-gray-500/10 text-gray-400";
    }
}
    function updateUrlParams(updates: Record<string, string | number>) {
        const params = new URLSearchParams(searchParams.toString());

        Object.entries(updates).forEach(([key, value]) => {
            if (
                value === "" ||
                value === "ALL" ||
                value === "created_desc" ||
                value === 1 ||
                value === 10
            ) {
                params.delete(key);
            } else {
                params.set(key, String(value));
            }
        });

        const query = params.toString();
        router.replace(query ? `${pathname}?${query}` : pathname);
    }

    async function loadCommittees(
        nextPage = page,
        nextStatus = statusFilter,
        nextSearch = searchQuery,
        nextSort = sortOption,
        nextSize = pageSize
    ) {
        setLoading(true);
        
        // YENİ: Her yeni istekte ID'yi 1 artırıyoruz
        const currentFetchId = ++fetchIdRef.current;

        try {
            const response = await fetchCommittees(
                nextPage - 1,
                nextSize,
                nextStatus,
                nextSearch,
                nextSort
            );

            // YENİ: Eğer bu istekten sonra başka bir istek atıldıysa, bu eski gelen cevabı YOKSAY!
            if (currentFetchId !== fetchIdRef.current) {
                return;
            }

            setCommittees(response.content);
            setTotalPages(response.totalPages);
        } catch (err) {
            // Hatayı sadece en son istekte göster
            if (currentFetchId === fetchIdRef.current) {
                showToast(
                    err instanceof Error ? err.message : "Failed to load committees.",
                    "error"
                );
            }
        } finally {
            // Yüklenme durumunu sadece en son istekte kapat
            if (currentFetchId === fetchIdRef.current) {
                setLoading(false);
            }
        }
    }
// Arama kutusuna yazı yazıldığında anında (0 gecikme ile) arama işlemi
    useEffect(() => {
        // setTimeout'u tamamen kaldırdık! Tuşa basıldığı an state güncellenir ve arama başlar.
        setPage(1);
        setSearchQuery(searchInput);
        updateUrlParams({ search: searchInput, page: 1 });
    }, [searchInput]);

    useEffect(() => {
        loadCommittees(1, statusFilter, searchQuery, sortOption, pageSize);
    }, [statusFilter, searchQuery, sortOption, pageSize]);

    function handlePageChange(nextPage: number) {
        if (nextPage < 1 || nextPage > totalPages) return;

        setPage(nextPage);
        updateUrlParams({ page: nextPage });
        loadCommittees(nextPage, statusFilter, searchQuery, sortOption, pageSize);
    }

    return (
        <div className="min-h-screen bg-gray-950 flex">
            <Sidebar activePage="committees" />

            <main className="flex-1 min-w-0 px-6 py-10 text-white">
                <div className="mx-auto max-w-6xl space-y-8">
                    <div>
                        <h1 className="text-3xl font-bold">Committees</h1>
                        <p className="mt-2 text-gray-400">
                            Browse committees and view their current assignments.
                        </p>
                    </div>

                    <div className="rounded-2xl border border-white/10 bg-gray-900/70 p-6">
                        <div className="mb-6 grid grid-cols-1 gap-4 md:grid-cols-4">
                            <input
                                type="text"
                                placeholder="Search committees..."
                                value={searchInput}
                                onChange={(e) => setSearchInput(e.target.value)}
                                className="rounded-xl border border-white/10 bg-gray-950 px-4 py-3 text-sm text-white placeholder:text-gray-500 outline-none focus:border-blue-500"
                            />

                            <select
                                value={statusFilter}
                                onChange={(e) => {
                                    setPage(1);
                                    setStatusFilter(e.target.value);
                                    updateUrlParams({ status: e.target.value, page: 1 });
                                }}
                                className="rounded-xl border border-white/10 bg-gray-950 px-4 py-3 text-sm text-white outline-none focus:border-blue-500"
                            >
                                <option value="ALL">All Statuses</option>
                                <option value="ACTIVE">ACTIVE</option>
                                <option value="INACTIVE">INACTIVE</option>
                                <option value="COMPLETED">COMPLETED</option>
                            </select>

                            <select
                                value={sortOption}
                                onChange={(e) => {
                                    setPage(1);
                                    setSortOption(e.target.value);
                                    updateUrlParams({ sort: e.target.value, page: 1 });
                                }}
                                className="rounded-xl border border-white/10 bg-gray-950 px-4 py-3 text-sm text-white outline-none focus:border-blue-500"
                            >
                                <option value="created_desc">Created Date ↓</option>
                                <option value="created_asc">Created Date ↑</option>
                                <option value="name_asc">Name A-Z</option>
                                <option value="name_desc">Name Z-A</option>
                            </select>

                            <select
                                value={pageSize}
                                onChange={(e) => {
                                    const nextSize = Number(e.target.value);
                                    setPage(1);
                                    setPageSize(nextSize);
                                    updateUrlParams({ size: nextSize, page: 1 });
                                }}
                                className="rounded-xl border border-white/10 bg-gray-950 px-4 py-3 text-sm text-white outline-none focus:border-blue-500"
                            >
                                <option value={10}>10 / page</option>
                                <option value={20}>20 / page</option>
                                <option value={50}>50 / page</option>
                            </select>
                        </div>

                        {loading ? (
                            <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
                                {Array.from({ length: 6 }).map((_, index) => (
                                    <div
                                        key={index}
                                        className="h-44 animate-pulse rounded-2xl border border-white/10 bg-white/5"
                                    />
                                ))}
                            </div>
                        ) : committees.length === 0 ? (
                            <div className="rounded-2xl border border-dashed border-white/10 bg-white/5 px-6 py-16 text-center">
                                <p className="text-gray-400">
                                    No committees found. Try adjusting your filters.
                                </p>
                            </div>
                        ) : (
                            <>
                                <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
                                    {committees.map((committee) => (
                                        <Link
                                            key={committee.committeeId}
                                            href={`/committees/${committee.committeeId}`}
                                        >
                                            <div className="h-full cursor-pointer rounded-2xl border border-white/10 bg-gray-950 p-5 transition hover:border-blue-500/40 hover:bg-white/5">
                                                <div className="flex items-start justify-between gap-3">
                                                    <div>
                                                        <h3 className="font-semibold text-white">
                                                            {committee.committeeName}
                                                        </h3>
                                                        <p className="mt-1 text-sm text-gray-400">
                                                            {committee.description || "No description"}
                                                        </p>
                                                    </div>

                                                    <span className={`rounded-full border px-3 py-1 text-xs ${getStatusColor(committee.status)}`}>
                                                         {committee.status} {/*dinamik renklendirme eklendi dk */}
                                                    </span>
                                                </div>

                                                <div className="mt-4 grid grid-cols-3 gap-2 text-center text-xs text-gray-400">
                                                    <div className="rounded-lg bg-white/5 p-2">
                                                        <p className="text-white">
                                                            {committee.advisorCount ?? 0}
                                                        </p>
                                                        Advisors
                                                    </div>

                                                    <div className="rounded-lg bg-white/5 p-2">
                                                        <p className="text-white">
                                                            {committee.juryCount ?? 0}
                                                        </p>
                                                        Jury
                                                    </div>

                                                    <div className="rounded-lg bg-white/5 p-2">
                                                        <p className="text-white">
                                                            {committee.groupCount ?? 0}
                                                        </p>
                                                        Groups
                                                    </div>
                                                </div>

                                            </div>
                                        </Link>
                                    ))}
                                </div>

                                <div className="mt-8 flex items-center justify-center gap-3">
                                    <button
                                        onClick={() => handlePageChange(page - 1)}
                                        disabled={page <= 1}
                                        className="rounded-xl border border-white/10 bg-gray-900 px-4 py-2 text-sm text-gray-300 transition-colors disabled:cursor-not-allowed disabled:opacity-40 hover:bg-white/5"
                                    >
                                        Previous
                                    </button>

                                    <span className="text-sm text-gray-400">
                                        Page <span className="text-white">{page}</span> /{" "}
                                        <span className="text-white">{totalPages}</span>
                                    </span>

                                    <button
                                        onClick={() => handlePageChange(page + 1)}
                                        disabled={page >= totalPages}
                                        className="rounded-xl border border-white/10 bg-gray-900 px-4 py-2 text-sm text-gray-300 transition-colors disabled:cursor-not-allowed disabled:opacity-40 hover:bg-white/5"
                                    >
                                        Next
                                    </button>
                                </div>
                            </>
                        )}
                    </div>
                </div>
            </main>
        </div>
    );
}