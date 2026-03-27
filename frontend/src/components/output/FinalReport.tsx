import Markdown from 'react-markdown';
import remarkGfm from 'remark-gfm';

interface Props {
  content: string;
}

export function FinalReport({ content }: Props) {
  if (!content) return null;

  return (
    <div className="rounded-lg border border-green-200 bg-white shadow-sm">
      <div className="border-b border-green-100 bg-green-50 px-4 py-3">
        <h2 className="text-lg font-bold text-green-800">Dein Reisebericht</h2>
      </div>
      <div className="prose prose-sm max-w-none px-6 py-4">
        <Markdown remarkPlugins={[remarkGfm]} skipHtml>{content}</Markdown>
      </div>
    </div>
  );
}
