import { useEffect, useMemo, useRef, useState, type ChangeEvent, type ReactNode } from "react";
import {
  ArrowLeft,
  Check,
  ChevronRight,
  Globe,
  Image as ImageIcon,
  Loader2,
  Lock,
  PlayCircle,
  Search,
  Sparkles,
  Trash2,
  Users,
  X,
} from "lucide-react";
import { toast } from "sonner";
import {
  createPost,
  getPostTopics,
  updatePost,
  uploadMedia,
  type PostTopic,
  type Privacy,
} from "@/services/post/postService";

interface EditablePost {
  postId: number;
  content: string;
  imageUrl: string | null;
  topicSlugs?: string[] | null;
  privacy?: Privacy | null;
}

interface CreatePostModalProps {
  isOpen: boolean;
  onClose: () => void;
  currentUserAvatar?: string;
  currentUsername?: string;
  mode?: "create" | "edit";
  initialPost?: EditablePost | null;
  onPostCreated?: () => void;
  onPostUpdated?: (post: {
    postId: number;
    content: string;
    imageUrl: string | null;
    topicSlugs: string[];
    privacy: Privacy;
    updatedAt: string;
  }) => void;
  parentPostId?: number | null;
  parentPostAuthor?: string | null;
  parentPostContent?: string | null;
}

const PRIVACY_OPTIONS: { value: Privacy; label: string; description: string; icon: React.FC<{ className?: string }> }[] = [
  { value: "PUBLIC", label: "Công khai", description: "Mọi người có thể xem bài viết", icon: Globe },
  { value: "FRIENDS_ONLY", label: "Người theo dõi", description: "Chỉ những người theo dõi bạn mới có thể xem", icon: Users },
  { value: "PRIVATE", label: "Chỉ mình tôi", description: "Chỉ mình bạn có thể nhìn thấy", icon: Lock },
];

const MAX_CHARS = 5000;
const MAX_TOTAL_SIZE_MB = 25;
const MAX_FILES = 4;
const MAX_TOPICS = 5;

export default function CreatePostModal({
  isOpen,
  onClose,
  currentUserAvatar,
  currentUsername,
  mode = "create",
  initialPost,
  onPostCreated,
  onPostUpdated,
  parentPostId,
  parentPostAuthor,
  parentPostContent,
}: CreatePostModalProps) {
  const [view, setView] = useState<"COMPOSER" | "PRIVACY" | "TOPICS">("COMPOSER");
  const [content, setContent] = useState("");
  const [privacy, setPrivacy] = useState<Privacy>("PUBLIC");
  const [topicSlugs, setTopicSlugs] = useState<string[]>([]);
  const [topics, setTopics] = useState<PostTopic[]>([]);
  const [topicSearch, setTopicSearch] = useState("");
  const [existingMediaUrls, setExistingMediaUrls] = useState<string[]>([]);
  const [selectedFiles, setSelectedFiles] = useState<File[]>([]);
  const [filePreviews, setFilePreviews] = useState<{ url: string; type: string }[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);

  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (!isOpen) return;

    getPostTopics().then((res) => {
      if (res.ok && res.data) setTopics(res.data);
      else toast.error(res.message ?? "Không tải được danh sách chủ đề.");
    });
  }, [isOpen]);

  useEffect(() => {
    if (!isOpen) return;

    setView("COMPOSER");
    setContent(mode === "edit" && initialPost ? initialPost.content : "");
    setPrivacy((mode === "edit" && initialPost?.privacy) || "PUBLIC");
    setTopicSlugs((mode === "edit" && initialPost?.topicSlugs?.length ? initialPost.topicSlugs : []).filter(Boolean));
    setExistingMediaUrls(splitMediaUrls(mode === "edit" ? initialPost?.imageUrl : null));
    setSelectedFiles([]);
    filePreviews.forEach((preview) => URL.revokeObjectURL(preview.url));
    setFilePreviews([]);
    setUploadProgress(0);
    setTopicSearch("");
  }, [isOpen, mode, initialPost?.postId]);

  const selectedPrivacy = PRIVACY_OPTIONS.find((option) => option.value === privacy) ?? PRIVACY_OPTIONS[0];
  const selectedTopicLabels = topicSlugs
    .map((slug) => topics.find((topic) => topic.slug === slug)?.label ?? slug)
    .join(", ");

  const filteredTopics = useMemo(() => {
    const query = topicSearch.trim().toLowerCase();
    if (!query) return topics;
    return topics.filter((topic) =>
      `${topic.label} ${topic.slug} ${topic.category}`.toLowerCase().includes(query)
    );
  }, [topicSearch, topics]);

  const groupedTopics = useMemo(() => {
    return filteredTopics.reduce<Record<string, PostTopic[]>>((acc, topic) => {
      acc[topic.category] = acc[topic.category] ?? [];
      acc[topic.category].push(topic);
      return acc;
    }, {});
  }, [filteredTopics]);

  const dynamicFontSize = (() => {
    const effectiveLength = content.length + (content.split("\n").length - 1) * 60;
    if (effectiveLength <= 25) return 24;
    if (effectiveLength <= 50) return 20;
    if (effectiveLength <= 90) return 18;
    if (effectiveLength <= 160) return 15;
    if (effectiveLength <= 280) return 13;
    return Math.max(10, Math.round(13 * Math.sqrt(280 / effectiveLength)));
  })();

  if (!isOpen) return null;

  const charCount = content.length;
  const isOverLimit = charCount > MAX_CHARS;
  const totalMediaCount = existingMediaUrls.length + selectedFiles.length;
  const canPost =
    (content.trim().length > 0 || totalMediaCount > 0) &&
    topicSlugs.length > 0 &&
    !isOverLimit &&
    !isSubmitting;

  const handleClose = () => {
    if (isSubmitting) return;
    filePreviews.forEach((preview) => URL.revokeObjectURL(preview.url));
    onClose();
  };

  const handleFileSelect = (event: ChangeEvent<HTMLInputElement>) => {
    if (!event.target.files) return;
    const newFiles = Array.from(event.target.files);

    if (totalMediaCount + newFiles.length > MAX_FILES) {
      toast.error(`Tối đa ${MAX_FILES} ảnh hoặc video.`);
      return;
    }

    const totalSize = [...selectedFiles, ...newFiles].reduce((sum, file) => sum + file.size, 0);
    if (totalSize > MAX_TOTAL_SIZE_MB * 1024 * 1024) {
      toast.error(`Tổng dung lượng tệp mới không được vượt quá ${MAX_TOTAL_SIZE_MB}MB.`);
      return;
    }

    setSelectedFiles((prev) => [...prev, ...newFiles]);
    setFilePreviews((prev) => [
      ...prev,
      ...newFiles.map((file) => ({ url: URL.createObjectURL(file), type: file.type })),
    ]);

    if (fileInputRef.current) fileInputRef.current.value = "";
  };

  const removeNewFile = (index: number) => {
    setSelectedFiles((prev) => prev.filter((_, i) => i !== index));
    setFilePreviews((prev) => {
      URL.revokeObjectURL(prev[index].url);
      return prev.filter((_, i) => i !== index);
    });
  };

  const toggleTopic = (slug: string) => {
    setTopicSlugs((prev) => {
      if (prev.includes(slug)) return prev.filter((item) => item !== slug);
      if (prev.length >= MAX_TOPICS) {
        toast.error(`Tối đa ${MAX_TOPICS} chủ đề cho một bài viết.`);
        return prev;
      }
      return [...prev, slug];
    });
  };

  const handleSubmit = async () => {
    if (!canPost) return;

    setIsSubmitting(true);
    setUploadProgress(10);
    try {
      const uploadedUrls: string[] = [];
      if (selectedFiles.length > 0) {
        setUploadProgress(35);
        const uploadResults = await Promise.all(selectedFiles.map((file) => uploadMedia(file)));
        for (const result of uploadResults) {
          if (!result.ok || !result.data) {
            toast.error(result.message || "Tải lên thất bại.");
            setUploadProgress(0);
            return;
          }
          uploadedUrls.push(result.data);
        }
      }

      setUploadProgress(80);
      const mediaUrls = [...existingMediaUrls, ...uploadedUrls];
      const payload = {
        content: content.trim(),
        imageUrl: mediaUrls.length > 0 ? mediaUrls.join(",") : null,
        imagePublicId: null,
        topicSlugs,
        privacy,
        parentPostId: parentPostId || null,
      };

      if (mode === "edit" && initialPost) {
        const result = await updatePost(initialPost.postId, payload);
        if (!result.ok || !result.data) {
          toast.error(result.message ?? "Cập nhật bài viết thất bại.");
          setUploadProgress(0);
          return;
        }

        setUploadProgress(100);
        toast.success("Đã cập nhật bài viết.");
        onPostUpdated?.({
          postId: initialPost.postId,
          content: result.data.content,
          imageUrl: result.data.imageUrl,
          topicSlugs: result.data.topicSlugs ?? [],
          privacy: result.data.privacy,
          updatedAt: result.data.updatedAt,
        });
        handleClose();
        return;
      }

      const result = await createPost(payload);
      if (!result.ok) {
        toast.error(result.message ?? "Đăng bài thất bại.");
        setUploadProgress(0);
        return;
      }

      setUploadProgress(100);
      toast.success("Đã đăng bài viết.");
      onPostCreated?.();
      handleClose();
    } catch (err) {
      console.error(err);
      toast.error("Đã có lỗi xảy ra.");
      setUploadProgress(0);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/55 backdrop-blur-sm" onClick={handleClose} />
      <div className="relative w-full max-w-2xl h-[620px] bg-white dark:bg-[#1a1a1a] rounded-xl shadow-2xl overflow-hidden flex flex-col animate-in zoom-in-95 duration-200">
        <div className="flex h-full w-[300%] transition-transform duration-300 ease-in-out" style={{ transform: view === "COMPOSER" ? "translateX(0)" : view === "PRIVACY" ? "translateX(-33.3333%)" : "translateX(-66.6666%)" }}>
          <div className="w-1/3 shrink-0 flex flex-col h-full relative">
            <ModalHeader title={mode === "edit" ? "Chỉnh sửa bài viết" : parentPostId ? "Chia sẻ bài viết" : "Tạo bài viết"} onClose={handleClose} />

            <div className="flex-1 overflow-y-auto px-6 pb-6">
              <div className="py-3 flex items-center gap-3">
                {currentUserAvatar ? (
                  <img src={currentUserAvatar} alt={currentUsername ?? "You"} className="w-10 h-10 rounded-full object-cover bg-gray-200 dark:bg-gray-800 shrink-0" />
                ) : (
                  <div className="w-10 h-10 rounded-full bg-slate-200 dark:bg-neutral-800" />
                )}
                <div className="flex flex-col gap-1">
                  <span className="text-sm font-semibold text-gray-900 dark:text-gray-100">{currentUsername ?? "Bạn"}</span>
                  <div className="flex flex-wrap gap-1.5">
                    <button onClick={() => setView("PRIVACY")} className="flex items-center gap-1.5 px-2 py-0.5 rounded bg-gray-100 dark:bg-gray-800 hover:bg-gray-200 dark:hover:bg-gray-700 text-xs font-medium text-gray-700 dark:text-gray-300">
                      <selectedPrivacy.icon className="w-3.5 h-3.5" />
                      {selectedPrivacy.label}
                      <ChevronRight className="w-3.5 h-3.5" />
                    </button>
                    <button onClick={() => setView("TOPICS")} className={`flex items-center gap-1.5 px-2 py-0.5 rounded text-xs font-medium ${topicSlugs.length ? "bg-blue-50 text-blue-700 dark:bg-blue-500/10 dark:text-blue-300" : "bg-amber-50 text-amber-700 dark:bg-amber-500/10 dark:text-amber-300"}`}>
                      {topicSlugs.length ? `${topicSlugs.length} chủ đề` : "Chọn chủ đề"}
                      <ChevronRight className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
              </div>

              <textarea
                value={content}
                onChange={(event) => setContent(event.target.value)}
                placeholder={parentPostId ? "Thêm bình luận cho chia sẻ này..." : "Bạn đang nghĩ gì thế?"}
                rows={4}
                style={{ fontSize: `${dynamicFontSize}px`, overflowWrap: "anywhere", wordBreak: "break-word" }}
                className="w-full min-h-[120px] bg-transparent text-gray-900 dark:text-gray-100 placeholder-gray-500 dark:placeholder-gray-400 leading-normal outline-none border-none resize-none"
              />

              {parentPostId && (
                <div className="mt-1 mb-4 p-4 rounded-xl border border-slate-200/80 dark:border-neutral-800 bg-slate-50/50 dark:bg-neutral-900/30">
                  <div className="flex items-center gap-2 mb-1.5">
                    <span className="text-xs font-bold text-slate-700 dark:text-neutral-300">@{parentPostAuthor}</span>
                    <span className="text-[10px] text-slate-400">Bài viết gốc</span>
                  </div>
                  <p className="text-sm text-slate-600 dark:text-neutral-400 line-clamp-3 whitespace-pre-line break-words leading-relaxed">
                    {parentPostContent}
                  </p>
                </div>
              )}

              {selectedTopicLabels && (
                <div className="mb-3 flex flex-wrap gap-2">
                  {topicSlugs.map((slug) => (
                    <span key={slug} className="rounded-full bg-blue-50 dark:bg-blue-500/10 px-3 py-1 text-xs font-semibold text-blue-700 dark:text-blue-300">
                      {topics.find((topic) => topic.slug === slug)?.label ?? slug}
                    </span>
                  ))}
                </div>
              )}

              {(existingMediaUrls.length > 0 || filePreviews.length > 0) && (
                <div className={`grid gap-2 mt-4 ${existingMediaUrls.length + filePreviews.length <= 1 ? "grid-cols-1" : "grid-cols-2"}`}>
                  {existingMediaUrls.map((url, index) => (
                    <MediaPreview key={url} url={url} onRemove={() => setExistingMediaUrls((prev) => prev.filter((_, i) => i !== index))} />
                  ))}
                  {filePreviews.map((preview, index) => (
                    <MediaPreview key={preview.url} url={preview.url} type={preview.type} onRemove={() => removeNewFile(index)} />
                  ))}
                </div>
              )}
            </div>

            <div className="px-6 py-4 border-t border-gray-200 dark:border-gray-800 flex items-center justify-between bg-white dark:bg-[#1a1a1a] shrink-0">
              <div className="flex items-center gap-1">
                <input type="file" ref={fileInputRef} onChange={handleFileSelect} accept="image/*,video/*" multiple className="hidden" />
                <button onClick={() => fileInputRef.current?.click()} disabled={totalMediaCount >= MAX_FILES} className="p-2 rounded-full text-green-500 hover:bg-green-50 dark:hover:bg-green-500/10 transition-colors disabled:opacity-50 disabled:cursor-not-allowed" title="Thêm ảnh/video">
                  <ImageIcon className="w-5 h-5" />
                </button>
              </div>

              <div className="flex items-center gap-4">
                {charCount > 0 && <span className={`text-xs font-medium ${isOverLimit ? "text-red-500" : "text-gray-500 dark:text-gray-400"}`}>{charCount}/{MAX_CHARS}</span>}
                <button onClick={handleSubmit} disabled={!canPost} className={`flex items-center justify-center gap-2 h-9 px-6 rounded-lg font-semibold transition-all ${canPost ? "bg-blue-600 hover:bg-blue-700 text-white shadow-md shadow-blue-500/20" : "bg-gray-200 dark:bg-gray-800 text-gray-500 cursor-not-allowed"}`}>
                  {isSubmitting ? <Loader2 className="w-4 h-4 animate-spin" /> : <span>{mode === "edit" ? "Lưu" : "Đăng"}</span>}
                </button>
              </div>
            </div>
            {isSubmitting && <div className="absolute bottom-0 left-0 h-1 bg-blue-500 transition-all duration-300 z-20" style={{ width: `${uploadProgress}%` }} />}
          </div>

          <SelectionPane title="Đối tượng của bài viết" onBack={() => setView("COMPOSER")}>
            <p className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-4 px-2">Ai có thể xem bài viết của bạn?</p>
            <div className="flex flex-col gap-1">
              {PRIVACY_OPTIONS.map((option) => (
                <button key={option.value} onClick={() => { setPrivacy(option.value); setView("COMPOSER"); }} className="flex items-center gap-4 p-3 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors text-left">
                  <div className="w-12 h-12 rounded-full bg-gray-100 dark:bg-gray-800 flex items-center justify-center shrink-0">
                    <option.icon className="w-6 h-6 text-gray-700 dark:text-gray-300" />
                  </div>
                  <div className="flex-1">
                    <p className="text-base font-semibold text-gray-900 dark:text-gray-100">{option.label}</p>
                    <p className="text-sm text-gray-500 dark:text-gray-400">{option.description}</p>
                  </div>
                  <div className="w-6 h-6 rounded-full border-2 border-gray-300 dark:border-gray-600 flex items-center justify-center shrink-0">
                    {privacy === option.value && <div className="w-3 h-3 rounded-full bg-blue-600" />}
                  </div>
                </button>
              ))}
            </div>
          </SelectionPane>

          <SelectionPane
            title="Chọn chủ đề"
            onBack={() => setView("COMPOSER")}
            fixedContent={
              <div className="shrink-0 border-b border-gray-100 bg-white pb-4 dark:border-gray-800 dark:bg-[#1a1a1a]">
              <p className="text-sm text-gray-500 dark:text-gray-400 mb-3">Chọn 1 đến {MAX_TOPICS} chủ đề để feed và AI ranking hiểu đúng ngữ cảnh bài viết.</p>
              <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                <input value={topicSearch} onChange={(event) => setTopicSearch(event.target.value)} placeholder="Tìm chủ đề: công nghệ, gia đình, thể thao..." className="w-full rounded-xl border border-gray-200 dark:border-gray-800 bg-gray-50 dark:bg-neutral-950 py-2.5 pl-10 pr-3 text-sm outline-none focus:border-blue-500 dark:text-white" />
              </div>
              </div>
            }
          >
            <div className="space-y-5">
              {Object.entries(groupedTopics).map(([category, items]) => (
                <section key={category}>
                  <h3 className="mb-2 text-xs font-extrabold uppercase tracking-wider text-gray-400">{category}</h3>
                  <div className="flex flex-wrap gap-2">
                    {items.map((topic) => {
                      const selected = topicSlugs.includes(topic.slug);
                      return (
                        <button key={topic.slug} onClick={() => toggleTopic(topic.slug)} className={`flex items-center gap-1.5 rounded-full border px-3 py-1.5 text-sm font-semibold transition-colors ${selected ? "border-blue-500 bg-blue-50 text-blue-700 dark:bg-blue-500/10 dark:text-blue-300" : "border-gray-200 text-gray-600 hover:border-blue-300 hover:text-blue-600 dark:border-gray-800 dark:text-gray-300"}`}>
                          {selected && <Check className="w-3.5 h-3.5" />}
                          {topic.label}
                        </button>
                      );
                    })}
                  </div>
                </section>
              ))}
            </div>
          </SelectionPane>
        </div>
      </div>
    </div>
  );
}

function ModalHeader({ title, onClose }: { title: string; onClose: () => void }) {
  return (
    <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200 dark:border-gray-800 shrink-0 bg-white dark:bg-[#1a1a1a] z-10">
      <div className="flex items-center gap-2">
        <Sparkles className="w-5 h-5 text-blue-500" />
        <h2 className="text-lg font-bold text-gray-900 dark:text-gray-100">{title}</h2>
      </div>
      <button onClick={onClose} className="p-2 rounded-full hover:bg-gray-100 dark:hover:bg-gray-800 text-gray-500 dark:text-gray-400 transition-colors">
        <X className="w-5 h-5" />
      </button>
    </div>
  );
}

function SelectionPane({
  title,
  onBack,
  fixedContent,
  children,
}: {
  title: string;
  onBack: () => void;
  fixedContent?: ReactNode;
  children: ReactNode;
}) {
  return (
    <div className="w-1/3 shrink-0 flex flex-col h-full bg-white dark:bg-[#1a1a1a]">
      <div className="flex items-center px-6 py-4 border-b border-gray-200 dark:border-gray-800 relative">
        <button onClick={onBack} className="p-2 rounded-full hover:bg-gray-100 dark:hover:bg-gray-800 text-gray-500 dark:text-gray-400 transition-colors absolute left-4">
          <ArrowLeft className="w-5 h-5" />
        </button>
        <h2 className="text-lg font-bold text-gray-900 dark:text-gray-100 w-full text-center">{title}</h2>
      </div>
      {fixedContent ? <div className="px-6 pt-6">{fixedContent}</div> : null}
      <div className={`flex-1 overflow-y-auto px-6 ${fixedContent ? "py-5" : "py-6"}`}>{children}</div>
    </div>
  );
}

function MediaPreview({ url, type, onRemove }: { url: string; type?: string; onRemove: () => void }) {
  const isVideo = (type?.startsWith("video/") ?? false) || /\.(mp4|webm|ogg|mov)$/i.test(url) || url.includes("video/upload");

  return (
    <div className="relative group rounded-lg overflow-hidden bg-gray-100 dark:bg-gray-800 border border-gray-200 dark:border-gray-700">
      {isVideo ? (
        <div className="w-full h-full relative aspect-video">
          <video src={url} className="w-full h-full object-cover" />
          <PlayCircle className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-10 h-10 text-white/80" />
        </div>
      ) : (
        <div className="aspect-square">
          <img src={url} alt="preview" className="w-full h-full object-cover" />
        </div>
      )}
      <button onClick={onRemove} className="absolute top-2 right-2 w-8 h-8 bg-black/60 hover:bg-red-500 rounded-full flex items-center justify-center text-white transition-all opacity-0 group-hover:opacity-100">
        <Trash2 className="w-4 h-4" />
      </button>
    </div>
  );
}

function splitMediaUrls(value?: string | null): string[] {
  return value ? value.split(",").map((url) => url.trim()).filter(Boolean) : [];
}
