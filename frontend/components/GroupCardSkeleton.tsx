export default function GroupCardSkeleton() {
    return (
        <div className="animate-pulse rounded-2xl border border-gray-200 bg-white p-5 shadow-sm">
            <div className="mb-4 flex items-center justify-between">
                <div className="h-5 w-32 rounded bg-gray-200" />
                <div className="h-5 w-16 rounded-full bg-gray-200" />
            </div>

            <div className="space-y-3">
                <div className="flex justify-between">
                    <div className="h-4 w-20 rounded bg-gray-200" />
                    <div className="h-4 w-10 rounded bg-gray-200" />
                </div>

                <div className="flex justify-between">
                    <div className="h-4 w-20 rounded bg-gray-200" />
                    <div className="h-4 w-24 rounded bg-gray-200" />
                </div>

                <div className="flex gap-2 pt-2">
                    <div className="h-5 w-20 rounded-full bg-gray-200" />
                    <div className="h-5 w-20 rounded-full bg-gray-200" />
                </div>
            </div>
        </div>
    );
}