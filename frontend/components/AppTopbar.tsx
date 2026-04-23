"use client";

import Link from "next/link";

type Props = {
    title?: string;
};
export default function AppTopbar({
    title = "SPMS",
}: Props) {
    return (
        <header className="mb-8" />
    );
}