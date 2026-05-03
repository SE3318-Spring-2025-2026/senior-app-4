type Props = {
    status: "forming" | "formed" | "advised" | "disbanded";
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