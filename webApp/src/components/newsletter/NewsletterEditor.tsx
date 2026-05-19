import { useRef } from "react";
import EmailEditor, { type EditorRef } from "react-email-editor";

interface Props {
  initialDesign?: object | null;
}

export function NewsletterEditor({ initialDesign }: Props) {
  const editorRef = useRef<EditorRef>(null);

  function handleExport() {
    editorRef.current?.editor?.exportHtml((data) => {
      console.log("Exported HTML:", data);
      alert("Esportato! Controlla la console del browser per il codice HTML.");
    });
  }

  function handleReady() {
    if (initialDesign && editorRef.current?.editor) {
      editorRef.current.editor.loadDesign(initialDesign as Parameters<typeof editorRef.current.editor.loadDesign>[0]);
    }
  }

  return (
    <div className="nl-editor">
      <EmailEditor
        ref={editorRef}
        onReady={handleReady}
        style={{ height: "720px", border: "1px solid var(--color-border-subtle, #e2e8f0)", borderRadius: "6px" }}
        minHeight="720px"
      />
      <div className="nl-editor__actions">
        <button type="button" className="nl-editor__btn nl-editor__btn--ghost" onClick={() => alert("Bozza salvata (demo).")}>
          Salva bozza
        </button>
        <button type="button" className="nl-editor__btn nl-editor__btn--primary" onClick={handleExport}>
          Esporta HTML
        </button>
      </div>

      <style>{`
        .nl-editor { display: grid; gap: 0; }
        .nl-editor__actions {
          display: flex;
          gap: 8px;
          padding-top: 12px;
          justify-content: flex-end;
        }
        .nl-editor__btn {
          padding: 7px 18px;
          border-radius: 6px;
          font-size: 13px;
          font-weight: 500;
          cursor: pointer;
          border: 1px solid #e2e8f0;
        }
        .nl-editor__btn--ghost { background: #fff; color: #374151; }
        .nl-editor__btn--ghost:hover { background: #f8fafc; }
        .nl-editor__btn--primary { background: #1e40af; color: #fff; border-color: #1e40af; }
        .nl-editor__btn--primary:hover { background: #1d3a9e; }
      `}</style>
    </div>
  );
}
