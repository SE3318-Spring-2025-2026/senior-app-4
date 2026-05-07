"use client";

import { useEditor, EditorContent } from "@tiptap/react";
import StarterKit from "@tiptap/starter-kit";
import { Image } from "@tiptap/extension-image";
import { Link } from "@tiptap/extension-link";
import { Highlight } from "@tiptap/extension-highlight";
import { Underline } from "@tiptap/extension-underline";
import { TextAlign } from "@tiptap/extension-text-align";
import { Table } from "@tiptap/extension-table";
import { TableRow } from "@tiptap/extension-table-row";
import { TableHeader } from "@tiptap/extension-table-header";
import { TableCell } from "@tiptap/extension-table-cell";
import { CodeBlock } from "@tiptap/extension-code-block";
import { Placeholder } from "@tiptap/extension-placeholder";
import { useCallback, useRef } from "react";
import { getToken } from "@/lib/auth";

interface MarkdownEditorProps {
  content: string;
  onChange: (html: string) => void;
  disabled?: boolean;
  placeholder?: string;
}

export default function MarkdownEditor({
  content,
  onChange,
  disabled = false,
  placeholder = "Start writing your document here...",
}: MarkdownEditorProps) {
  const imageInputRef = useRef<HTMLInputElement>(null);

  const editor = useEditor({
    immediatelyRender: false,
    extensions: [
      StarterKit.configure({ codeBlock: false }),
      CodeBlock,
      Image.configure({ inline: false, allowBase64: true }),
      Link.configure({ openOnClick: false }),
      Highlight,
      Underline,
      TextAlign.configure({ types: ["heading", "paragraph"] }),
      Table.configure({ resizable: true }),
      TableRow,
      TableHeader,
      TableCell,
      Placeholder.configure({ placeholder }),
    ],
    content,
    editable: !disabled,
    onUpdate({ editor }) {
      onChange(editor.getHTML());
    },
  });

  const uploadImage = useCallback(
    async (file: File) => {
      if (!editor) return;

      // If file is small enough use base64 inline, otherwise upload to backend
      if (file.size <= 2 * 1024 * 1024) {
        const reader = new FileReader();
        reader.onload = (e) => {
          const src = e.target?.result as string;
          editor.chain().focus().setImage({ src }).run();
        };
        reader.readAsDataURL(file);
        return;
      }

      const token = getToken();
      const formData = new FormData();
      formData.append("file", file);

      try {
        const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/files/images`, {
          method: "POST",
          headers: { Authorization: `Bearer ${token}` },
          body: formData,
        });
        if (res.ok) {
          const { url } = await res.json();
          editor.chain().focus().setImage({ src: url }).run();
        }
      } catch {
        // fallback: still embed as base64
        const reader = new FileReader();
        reader.onload = (e) => {
          const src = e.target?.result as string;
          editor.chain().focus().setImage({ src }).run();
        };
        reader.readAsDataURL(file);
      }
    },
    [editor]
  );

  const handleImageFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) uploadImage(file);
    e.target.value = "";
  };

  const handleEditorDrop = useCallback(
    (e: React.DragEvent) => {
      const file = e.dataTransfer.files?.[0];
      if (file && file.type.startsWith("image/")) {
        e.preventDefault();
        uploadImage(file);
      }
    },
    [uploadImage]
  );

  if (!editor) return null;

  return (
    <div className="rounded-xl border border-white/10 bg-gray-950 overflow-hidden flex flex-col">
      {/* Toolbar */}
      <div className="flex flex-wrap items-center gap-0.5 border-b border-white/8 bg-gray-900 px-2 py-1.5">
        <ToolbarGroup>
          <ToolBtn
            onClick={() => editor.chain().focus().toggleBold().run()}
            active={editor.isActive("bold")}
            title="Bold (Ctrl+B)"
          >
            <strong>B</strong>
          </ToolBtn>
          <ToolBtn
            onClick={() => editor.chain().focus().toggleItalic().run()}
            active={editor.isActive("italic")}
            title="Italic (Ctrl+I)"
          >
            <em>I</em>
          </ToolBtn>
          <ToolBtn
            onClick={() => editor.chain().focus().toggleUnderline().run()}
            active={editor.isActive("underline")}
            title="Underline (Ctrl+U)"
          >
            <span className="underline">U</span>
          </ToolBtn>
          <ToolBtn
            onClick={() => editor.chain().focus().toggleStrike().run()}
            active={editor.isActive("strike")}
            title="Strikethrough"
          >
            <span className="line-through">S</span>
          </ToolBtn>
          <ToolBtn
            onClick={() => editor.chain().focus().toggleHighlight().run()}
            active={editor.isActive("highlight")}
            title="Highlight"
          >
            <span className="bg-yellow-300 text-black rounded px-0.5 text-xs">H</span>
          </ToolBtn>
        </ToolbarGroup>

        <Divider />

        <ToolbarGroup>
          {([1, 2, 3] as const).map((level) => (
            <ToolBtn
              key={level}
              onClick={() => editor.chain().focus().toggleHeading({ level }).run()}
              active={editor.isActive("heading", { level })}
              title={`Heading ${level}`}
            >
              <span className="text-xs font-bold">H{level}</span>
            </ToolBtn>
          ))}
          <ToolBtn
            onClick={() => editor.chain().focus().setParagraph().run()}
            active={editor.isActive("paragraph")}
            title="Paragraph"
          >
            <span className="text-xs">¶</span>
          </ToolBtn>
        </ToolbarGroup>

        <Divider />

        <ToolbarGroup>
          <ToolBtn
            onClick={() => editor.chain().focus().setTextAlign("left").run()}
            active={editor.isActive({ textAlign: "left" })}
            title="Align left"
          >
            <AlignLeftIcon />
          </ToolBtn>
          <ToolBtn
            onClick={() => editor.chain().focus().setTextAlign("center").run()}
            active={editor.isActive({ textAlign: "center" })}
            title="Align center"
          >
            <AlignCenterIcon />
          </ToolBtn>
          <ToolBtn
            onClick={() => editor.chain().focus().setTextAlign("right").run()}
            active={editor.isActive({ textAlign: "right" })}
            title="Align right"
          >
            <AlignRightIcon />
          </ToolBtn>
        </ToolbarGroup>

        <Divider />

        <ToolbarGroup>
          <ToolBtn
            onClick={() => editor.chain().focus().toggleBulletList().run()}
            active={editor.isActive("bulletList")}
            title="Bullet list"
          >
            <ListIcon />
          </ToolBtn>
          <ToolBtn
            onClick={() => editor.chain().focus().toggleOrderedList().run()}
            active={editor.isActive("orderedList")}
            title="Ordered list"
          >
            <OrderedListIcon />
          </ToolBtn>
          <ToolBtn
            onClick={() => editor.chain().focus().toggleBlockquote().run()}
            active={editor.isActive("blockquote")}
            title="Blockquote"
          >
            <span className="text-sm">"</span>
          </ToolBtn>
          <ToolBtn
            onClick={() => editor.chain().focus().toggleCodeBlock().run()}
            active={editor.isActive("codeBlock")}
            title="Code block"
          >
            <CodeIcon />
          </ToolBtn>
        </ToolbarGroup>

        <Divider />

        <ToolbarGroup>
          <ToolBtn
            onClick={() =>
              editor
                .chain()
                .focus()
                .insertTable({ rows: 3, cols: 3, withHeaderRow: true })
                .run()
            }
            active={false}
            title="Insert table"
          >
            <TableIcon />
          </ToolBtn>
          <ToolBtn
            onClick={() => {
              if (!disabled) imageInputRef.current?.click();
            }}
            active={false}
            title="Insert image"
          >
            <ImageIcon />
          </ToolBtn>
          <ToolBtn
            onClick={() => {
              const url = window.prompt("Enter URL:");
              if (url) editor.chain().focus().setLink({ href: url }).run();
            }}
            active={editor.isActive("link")}
            title="Insert link"
          >
            <LinkIcon />
          </ToolBtn>
        </ToolbarGroup>

        <Divider />

        <ToolbarGroup>
          <ToolBtn onClick={() => editor.chain().focus().undo().run()} active={false} title="Undo (Ctrl+Z)">
            <UndoIcon />
          </ToolBtn>
          <ToolBtn onClick={() => editor.chain().focus().redo().run()} active={false} title="Redo (Ctrl+Y)">
            <RedoIcon />
          </ToolBtn>
        </ToolbarGroup>
      </div>

      {/* Editor area */}
      <input
        ref={imageInputRef}
        type="file"
        accept="image/*"
        className="hidden"
        onChange={handleImageFileChange}
      />

      <div
        className="min-h-[320px] overflow-y-auto p-5"
        onDrop={handleEditorDrop}
        onDragOver={(e) => e.preventDefault()}
      >
        <EditorContent
          editor={editor}
          className="prose prose-invert prose-sm max-w-none focus:outline-none [&_.ProseMirror]:outline-none [&_.ProseMirror]:min-h-[280px]"
        />
      </div>

      <style>{`
        .ProseMirror p.is-editor-empty:first-child::before {
          color: #4b5563;
          content: attr(data-placeholder);
          float: left;
          height: 0;
          pointer-events: none;
        }
        .ProseMirror table {
          border-collapse: collapse;
          width: 100%;
        }
        .ProseMirror th,
        .ProseMirror td {
          border: 1px solid rgba(255,255,255,0.15);
          padding: 6px 10px;
          min-width: 60px;
        }
        .ProseMirror th {
          background: rgba(255,255,255,0.06);
          font-weight: 600;
        }
        .ProseMirror pre {
          background: rgba(0,0,0,0.4);
          border-radius: 6px;
          padding: 12px 16px;
          color: #e2e8f0;
          font-size: 0.85em;
        }
        .ProseMirror blockquote {
          border-left: 3px solid rgba(99,102,241,0.5);
          padding-left: 16px;
          color: #9ca3af;
        }
        .ProseMirror img {
          max-width: 100%;
          border-radius: 8px;
        }
        .ProseMirror a {
          color: #60a5fa;
          text-decoration: underline;
        }
      `}</style>
    </div>
  );
}

function ToolbarGroup({ children }: { children: React.ReactNode }) {
  return <div className="flex items-center gap-0.5">{children}</div>;
}

function Divider() {
  return <div className="mx-1 h-5 w-px bg-white/10" />;
}

function ToolBtn({
  children,
  onClick,
  active,
  title,
}: {
  children: React.ReactNode;
  onClick: () => void;
  active: boolean;
  title: string;
}) {
  return (
    <button
      type="button"
      onMouseDown={(e) => {
        e.preventDefault();
        onClick();
      }}
      title={title}
      className={[
        "flex h-7 w-7 items-center justify-center rounded text-xs transition-colors",
        active
          ? "bg-blue-600/30 text-blue-300"
          : "text-gray-400 hover:bg-white/8 hover:text-white",
      ].join(" ")}
    >
      {children}
    </button>
  );
}

function AlignLeftIcon() {
  return (
    <svg className="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}>
      <line x1="3" y1="6" x2="21" y2="6" /><line x1="3" y1="12" x2="15" y2="12" /><line x1="3" y1="18" x2="18" y2="18" />
    </svg>
  );
}
function AlignCenterIcon() {
  return (
    <svg className="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}>
      <line x1="3" y1="6" x2="21" y2="6" /><line x1="6" y1="12" x2="18" y2="12" /><line x1="4" y1="18" x2="20" y2="18" />
    </svg>
  );
}
function AlignRightIcon() {
  return (
    <svg className="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}>
      <line x1="3" y1="6" x2="21" y2="6" /><line x1="9" y1="12" x2="21" y2="12" /><line x1="6" y1="18" x2="21" y2="18" />
    </svg>
  );
}
function ListIcon() {
  return (
    <svg className="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}>
      <line x1="9" y1="6" x2="20" y2="6" /><line x1="9" y1="12" x2="20" y2="12" /><line x1="9" y1="18" x2="20" y2="18" />
      <circle cx="4" cy="6" r="1" fill="currentColor" /><circle cx="4" cy="12" r="1" fill="currentColor" /><circle cx="4" cy="18" r="1" fill="currentColor" />
    </svg>
  );
}
function OrderedListIcon() {
  return (
    <svg className="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}>
      <line x1="10" y1="6" x2="21" y2="6" /><line x1="10" y1="12" x2="21" y2="12" /><line x1="10" y1="18" x2="21" y2="18" />
      <path d="M4 6h1v4" /><path d="M4 10h2" /><path d="M6 18H4c0-1 2-2 2-3s-1-1.5-2-1" />
    </svg>
  );
}
function CodeIcon() {
  return (
    <svg className="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}>
      <polyline points="16 18 22 12 16 6" /><polyline points="8 6 2 12 8 18" />
    </svg>
  );
}
function TableIcon() {
  return (
    <svg className="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}>
      <rect x="3" y="3" width="18" height="18" rx="2" /><line x1="3" y1="9" x2="21" y2="9" /><line x1="3" y1="15" x2="21" y2="15" /><line x1="12" y1="3" x2="12" y2="21" />
    </svg>
  );
}
function ImageIcon() {
  return (
    <svg className="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}>
      <rect x="3" y="3" width="18" height="18" rx="2" /><circle cx="8.5" cy="8.5" r="1.5" /><polyline points="21 15 16 10 5 21" />
    </svg>
  );
}
function LinkIcon() {
  return (
    <svg className="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}>
      <path d="M10 13a5 5 0 007.54.54l3-3a5 5 0 00-7.07-7.07l-1.72 1.71" /><path d="M14 11a5 5 0 00-7.54-.54l-3 3a5 5 0 007.07 7.07l1.71-1.71" />
    </svg>
  );
}
function UndoIcon() {
  return (
    <svg className="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}>
      <polyline points="9 14 4 9 9 4" /><path d="M20 20v-7a4 4 0 00-4-4H4" />
    </svg>
  );
}
function RedoIcon() {
  return (
    <svg className="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}>
      <polyline points="15 14 20 9 15 4" /><path d="M4 20v-7a4 4 0 014-4h12" />
    </svg>
  );
}
