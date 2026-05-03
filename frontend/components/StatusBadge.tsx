type Props = {
    status: "forming" | "formed" | "advised" | "disbanded" | "ACTIVE" | "INACTIVE" | "COMPLETED";
};

export default function StatusBadge({ status }: Props) {
    const styles = {
        forming:
            "bg-yellow-400/15 text-yellow-300 border border-yellow-400/20 shadow-yellow-400/20",
        formed:
            "bg-blue-500/15 text-blue-300 border border-blue-500/20 shadow-blue-500/20",
        advised:
            "bg-green-500/15 text-green-300 border border-green-500/20 shadow-green-500/20",
        disbanded:
            "bg-red-500/15 text-red-300 border border-red-500/20 shadow-red-500/20",
        // Issue 320 Statuses (Preserving backdrop-blur but using AC hex codes via CSS Variables)
        ACTIVE:
            "bg-[#28A745]/15 text-[#28A745] border border-[#28A745]/30 shadow-[#28A745]/20",
        INACTIVE:
            "bg-[#6C757D]/15 text-[#6C757D] border border-[#6C757D]/30 shadow-[#6C757D]/20",
        COMPLETED:
            "bg-[#FFC107]/15 text-[#FFC107] border border-[#FFC107]/30 shadow-[#FFC107]/20",
    };

    return (
        <span
            className={[
                "px-3 py-1 text-xs font-medium rounded-full capitalize",
                "backdrop-blur-md transition-all duration-200",
                "hover:scale-105",
                "shadow-sm",
                styles[status],
            ].join(" ")}
        >
            {status}
        </span>
    );
}