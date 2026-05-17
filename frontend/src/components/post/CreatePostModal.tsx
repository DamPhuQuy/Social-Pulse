import { useRef, useState, type ChangeEvent } from "react";
import { 
  X, Globe, Users, Lock, ChevronRight, 
  Image as ImageIcon, Loader2, PlayCircle, 
  Trash2, Sparkles, ArrowLeft
} from "lucide-react";
import { createPost, uploadMedia, type Privacy } from "@/services/post/postService";
import { toast } from "sonner";

interface CreatePostModalProps {
  isOpen: boolean;
  onClose: () => void;
  currentUserAvatar?: string;
  currentUsername?: string;
  onPostCreated?: () => void;
}

const PRIVACY_OPTIONS: { value: Privacy; label: string; description: string; icon: React.FC<{ className?: string }> }[] = [
  { value: "PUBLIC", label: "Công khai", description: "Bất kỳ ai trên hoặc ngoài SocialPulse", icon: Globe },
  { value: "FRIENDS_ONLY", label: "Bạn bè", description: "Bạn bè của bạn trên SocialPulse", icon: Users },
  { value: "PRIVATE", label: "Chỉ mình tôi", description: "Chỉ mình bạn có thể nhìn thấy", icon: Lock },
];

const MAX_CHARS = 5000;
const MAX_TOTAL_SIZE_MB = 25;
const MAX_FILES = 4;

export default function CreatePostModal({
  isOpen,
  onClose,
  currentUserAvatar,
  currentUsername,
  onPostCreated,
}: CreatePostModalProps) {
  const [view, setView] = useState<"COMPOSER" | "PRIVACY">("COMPOSER");
  const [content, setContent] = useState("");
  const [privacy, setPrivacy] = useState<Privacy>("PUBLIC");
  
  const [selectedFiles, setSelectedFiles] = useState<File[]>([]);
  const [filePreviews, setFilePreviews] = useState<{ url: string; type: string }[]>([]);
  
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  
  const fileInputRef = useRef<HTMLInputElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  // Mathematically scale the font size down dynamically to guarantee that
  // the entire text remains fully visible within the visible area as the user types more content.
  const dynamicFontSize = (() => {
    const newlineCount = content.split('\n').length - 1;
    // Each newline is equivalent to ~60 characters of visual space
    const effectiveLength = content.length + newlineCount * 60;
    
    if (effectiveLength <= 25) return 24; // text-2xl
    if (effectiveLength <= 50) return 20; // text-xl
    if (effectiveLength <= 90) return 18; // text-lg
    if (effectiveLength <= 160) return 15; // text-base-ish
    if (effectiveLength <= 280) return 13; // text-sm-ish
    
    // Smoothly scale down for very long texts
    const scaledSize = 13 * Math.sqrt(280 / effectiveLength);
    return Math.max(10, Math.round(scaledSize)); // Cap minimum at 10px to ensure legibility
  })();

  // No JS height calculation needed - using pure CSS Grid for flawless auto-resizing

  if (!isOpen) return null;

  const charCount = content.length;
  const isOverLimit = charCount > MAX_CHARS;
  const canPost = (content.trim().length > 0 || selectedFiles.length > 0) && !isOverLimit && !isSubmitting;

  // Dynamic font size is handled mathematically and applied as an inline style.

  const selectedPrivacy = PRIVACY_OPTIONS.find((o) => o.value === privacy)!;

  const handleClose = () => {
    if (isSubmitting) return;
    setContent("");
    setSelectedFiles([]);
    filePreviews.forEach(p => URL.revokeObjectURL(p.url));
    setFilePreviews([]);
    setPrivacy("PUBLIC");
    setView("COMPOSER");
    setUploadProgress(0);
    onClose();
  };

  const handleFileSelect = (e: ChangeEvent<HTMLInputElement>) => {
    if (!e.target.files) return;
    const newFiles = Array.from(e.target.files);
    
    if (selectedFiles.length + newFiles.length > MAX_FILES) {
      toast.error(`Tối đa ${MAX_FILES} tệp.`);
      return;
    }

    const totalSize = [...selectedFiles, ...newFiles].reduce((acc, file) => acc + file.size, 0);
    if (totalSize > MAX_TOTAL_SIZE_MB * 1024 * 1024) {
      toast.error(`Tổng dung lượng > ${MAX_TOTAL_SIZE_MB}MB.`);
      return;
    }

    const previews = newFiles.map(file => ({
      url: URL.createObjectURL(file),
      type: file.type
    }));

    setSelectedFiles(prev => [...prev, ...newFiles]);
    setFilePreviews(prev => [...prev, ...previews]);
    if (fileInputRef.current) fileInputRef.current.value = "";
  };

  const removeFile = (index: number) => {
    setSelectedFiles(prev => prev.filter((_, i) => i !== index));
    setFilePreviews(prev => {
      URL.revokeObjectURL(prev[index].url);
      return prev.filter((_, i) => i !== index);
    });
  };

  const handleSubmit = async () => {
    if (!canPost) return;
    setIsSubmitting(true);
    setUploadProgress(10);
    try {
      let uploadedUrls: string[] = [];
      if (selectedFiles.length > 0) {
        setUploadProgress(30);
        const uploadResults = await Promise.all(selectedFiles.map(file => uploadMedia(file)));
        for (const res of uploadResults) {
          if (!res.ok || !res.data) {
            toast.error(res.message || "Tải lên thất bại");
            setIsSubmitting(false);
            setUploadProgress(0);
            return;
          }
          uploadedUrls.push(res.data);
        }
      }
      setUploadProgress(80);
      const result = await createPost({
        content: content.trim(),
        imageUrl: uploadedUrls.length > 0 ? uploadedUrls.join(",") : null,
        imagePublicId: null,
        privacy,
      });
      if (result.ok) {
        setUploadProgress(100);
        setTimeout(() => {
          toast.success("Đã đăng bài viết!");
          onPostCreated?.();
          handleClose();
        }, 300);
      } else {
        toast.error(result.message ?? "Đăng bài thất bại.");
        setUploadProgress(0);
      }
    } catch (err) {
      toast.error("Đã có lỗi xảy ra.");
      setUploadProgress(0);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center p-4">
      {/* Backdrop */}
      <div className="absolute inset-0 bg-black/50 backdrop-blur-sm transition-opacity" onClick={handleClose} />

      {/* Modal Container: Spacious layout to maximize user input space and prevent scrolling */}
      <div className="relative w-full max-w-2xl h-[520px] sm:h-[580px] bg-white dark:bg-[#1a1a1a] rounded-xl shadow-2xl overflow-hidden flex flex-col animate-in zoom-in-95 duration-200">
        
        {/* Sliding Wrapper */}
        <div 
          className="flex h-full w-[200%] transition-transform duration-300 ease-in-out"
          style={{ transform: view === "COMPOSER" ? "translateX(0)" : "translateX(-50%)" }}
        >
          
          {/* =========================================================================
              VIEW 1: COMPOSER
              ========================================================================= */}
          <div className="w-1/2 shrink-0 flex flex-col h-full relative">
            {/* Header */}
            <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200 dark:border-gray-800 shrink-0 bg-white dark:bg-[#1a1a1a] z-10">
              <div className="flex items-center gap-2">
                <Sparkles className="w-5 h-5 text-blue-500" />
                <h2 className="text-lg font-bold text-gray-900 dark:text-gray-100">Tạo bài viết</h2>
              </div>
              <button 
                onClick={handleClose} 
                className="p-2 rounded-full hover:bg-gray-100 dark:hover:bg-gray-800 text-gray-500 dark:text-gray-400 transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Scrollable Content Area */}
            <div className="flex-1 flex flex-col overflow-y-auto px-6 pb-6 [&::-webkit-scrollbar]:w-2 [&::-webkit-scrollbar-thumb]:bg-gray-300 dark:[&::-webkit-scrollbar-thumb]:bg-gray-600 [&::-webkit-scrollbar-thumb]:rounded-full hover:[&::-webkit-scrollbar-thumb]:bg-gray-400 dark:hover:[&::-webkit-scrollbar-thumb]:bg-gray-500">
              {/* User Info & Privacy */}
              <div className="py-3 flex items-center gap-3">
                <img
                  src={currentUserAvatar ?? `https://api.dicebear.com/7.x/avataaars/svg?seed=${currentUsername ?? "user"}`}
                  alt={currentUsername ?? "You"}
                  className="w-10 h-10 rounded-full object-cover bg-gray-200 dark:bg-gray-800 shrink-0"
                />
                <div className="flex flex-col">
                  <span className="text-sm font-semibold text-gray-900 dark:text-gray-100">
                    {currentUsername ?? "Alex Henderson"}
                  </span>

                  <button
                    onClick={() => setView("PRIVACY")}
                    className="flex items-center gap-1.5 px-2 py-0.5 mt-0.5 rounded bg-gray-100 dark:bg-gray-800 hover:bg-gray-200 dark:hover:bg-gray-700 text-xs font-medium text-gray-700 dark:text-gray-300 transition-colors w-fit"
                  >
                    <selectedPrivacy.icon className="w-3.5 h-3.5" />
                    {selectedPrivacy.label}
                    <ChevronRight className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>

              {/* Pure CSS Grid Auto-expanding container - 100% immune to JS scrollHeight bugs! */}
              <div className="grid w-full mt-2 relative">
                {/* Ghost element matches textarea typography precisely to expand the grid row */}
                <div
                  aria-hidden="true"
                  style={{ fontSize: `${dynamicFontSize}px`, overflowWrap: 'anywhere', wordBreak: 'break-all' }}
                  className="col-start-1 col-end-2 row-start-1 row-end-2 w-full min-h-[40px] pt-2 pb-8 invisible whitespace-pre-wrap leading-normal pointer-events-none"
                >
                  {content + ' '}
                </div>
                
                {/* The actual textarea, perfectly overlaid in the same grid cell */}
                <textarea
                  ref={textareaRef}
                  value={content}
                  onChange={(e) => setContent(e.target.value)}
                  placeholder="Bạn đang nghĩ gì thế?"
                  rows={1}
                  style={{ fontSize: `${dynamicFontSize}px`, overflowWrap: 'anywhere', wordBreak: 'break-all' }}
                  className="col-start-1 col-end-2 row-start-1 row-end-2 w-full h-full min-h-[40px] pt-2 pb-8 bg-transparent text-gray-900 dark:text-gray-100 placeholder-gray-500 dark:placeholder-gray-400 leading-normal outline-none border-none resize-none overflow-hidden"
                />
              </div>

              {/* Image/Video Previews */}
              {filePreviews.length > 0 && (
                <div className={`grid gap-2 mt-4 shrink-0 ${filePreviews.length <= 1 ? 'grid-cols-1' : 'grid-cols-2'}`}>
                  {filePreviews.map((preview, idx) => (
                    <div key={idx} className="relative group rounded-lg overflow-hidden bg-gray-100 dark:bg-gray-800 border border-gray-200 dark:border-gray-700">
                      {preview.type.startsWith("video/") ? (
                        <div className="w-full h-full relative aspect-video">
                          <video src={preview.url} className="w-full h-full object-cover" />
                          <PlayCircle className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-10 h-10 text-white/80" />
                        </div>
                      ) : (
                        <div className="aspect-square">
                          <img src={preview.url} alt="preview" className="w-full h-full object-cover" />
                        </div>
                      )}
                      <button
                        onClick={() => removeFile(idx)}
                        className="absolute top-2 right-2 w-8 h-8 bg-black/60 hover:bg-red-500 rounded-full flex items-center justify-center text-white transition-all opacity-0 group-hover:opacity-100"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Footer Actions */}
            <div className="px-6 py-4 border-t border-gray-200 dark:border-gray-800 flex items-center justify-between bg-white dark:bg-[#1a1a1a] shrink-0 w-full z-10">
              <div className="flex items-center gap-1">
                <input type="file" ref={fileInputRef} onChange={handleFileSelect} accept="image/*,video/*" multiple className="hidden" />
                <button
                  onClick={() => fileInputRef.current?.click()}
                  disabled={selectedFiles.length >= MAX_FILES}
                  className="p-2 rounded-full text-green-500 hover:bg-green-50 dark:hover:bg-green-500/10 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                  title="Thêm Ảnh/Video"
                >
                  <ImageIcon className="w-5 h-5" />
                </button>
              </div>

              <div className="flex items-center gap-4">
                {charCount > 0 && (
                  <span className={`text-xs font-medium ${isOverLimit ? "text-red-500" : "text-gray-500 dark:text-gray-400"}`}>
                    {charCount}/{MAX_CHARS}
                  </span>
                )}

                <button
                  onClick={handleSubmit}
                  disabled={!canPost}
                  className={`flex items-center justify-center gap-2 h-9 px-6 rounded-lg font-semibold transition-all duration-200
                    ${canPost 
                      ? "bg-blue-600 hover:bg-blue-700 text-white shadow-md shadow-blue-500/20" 
                      : "bg-gray-200 dark:bg-gray-800 text-gray-500 dark:text-gray-500 cursor-not-allowed"}
                  `}
                >
                  {isSubmitting ? <Loader2 className="w-4 h-4 animate-spin" /> : <span>Đăng</span>}
                </button>
              </div>
            </div>
            
            {/* Upload Progress Overlay within Composer */}
            {isSubmitting && (
              <div className="absolute bottom-0 left-0 h-1 bg-blue-500 transition-all duration-300 w-full z-20" style={{ width: `${uploadProgress}%` }} />
            )}
          </div>

          {/* =========================================================================
              VIEW 2: PRIVACY SELECTOR
              ========================================================================= */}
          <div className="w-1/2 shrink-0 flex flex-col h-full bg-white dark:bg-[#1a1a1a]">
            {/* Header */}
            <div className="flex items-center px-6 py-4 border-b border-gray-200 dark:border-gray-800 relative">
              <button 
                onClick={() => setView("COMPOSER")} 
                className="p-2 rounded-full hover:bg-gray-100 dark:hover:bg-gray-800 text-gray-500 dark:text-gray-400 transition-colors absolute left-4"
              >
                <ArrowLeft className="w-5 h-5" />
              </button>
              <h2 className="text-lg font-bold text-gray-900 dark:text-gray-100 w-full text-center">Đối tượng của bài viết</h2>
            </div>
            
            {/* Content Area */}
            <div className="flex-1 overflow-y-auto px-6 py-6">
              <p className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-4 px-2">Ai có thể xem bài viết của bạn?</p>
              
              <div className="flex flex-col gap-1">
                {PRIVACY_OPTIONS.map((opt) => (
                  <button
                    key={opt.value}
                    onClick={() => {
                      setPrivacy(opt.value);
                      setView("COMPOSER");
                    }}
                    className="flex items-center gap-4 p-3 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors text-left"
                  >
                    <div className="w-12 h-12 rounded-full bg-gray-100 dark:bg-gray-800 flex items-center justify-center shrink-0">
                      <opt.icon className="w-6 h-6 text-gray-700 dark:text-gray-300" />
                    </div>
                    <div className="flex-1">
                      <p className="text-base font-semibold text-gray-900 dark:text-gray-100">{opt.label}</p>
                      <p className="text-sm text-gray-500 dark:text-gray-400">{opt.description}</p>
                    </div>
                    <div className="w-6 h-6 rounded-full border-2 border-gray-300 dark:border-gray-600 flex items-center justify-center shrink-0">
                      {privacy === opt.value && (
                        <div className="w-3 h-3 rounded-full bg-blue-600" />
                      )}
                    </div>
                  </button>
                ))}
              </div>
            </div>
          </div>

        </div>
      </div>
    </div>
  );
}
