---
version: 2.0
name: DevPulse-Design-System
description: Modern, high-legibility developer community & tech content hub design specification inspired by daily.dev and GitHub dark design languages. Focuses on content hierarchy, source credibility badges, monospace accents, and distraction-free developer reading.
---

# DevPulse Design System (v2.0)

## 1. Design Principles

1. **Content First, Zero Fluff**: Developer cards emphasize the headline, source publisher (domain/favicon), reading time, and tech tags over intrusive social decorations.
2. **Dark-First Elegance**: Crafted with a deep slate/zinc neutral scale (`#0f172a` canvas, `#18181b` card surface) with crisp high-contrast border definition (`#27272a` / `#334155`).
3. **Typography & Monospace Accents**: Inter/system sans-serif for high reading velocity; monospace (`JetBrains Mono`, `Fira Code`, system mono) for tech tags, code snippets, and domain badges.
4. **Intuitive Developer Signals**:
   - `▲ Upvote`: Clean chevron with instant counter increment.
   - `💬 Comments`: Threaded technical discussions.
   - `🔖 Bookmark`: Personal reading list save/unsave toggle.
   - `🌐 Domain Badge`: Verified publisher domain (`github.com`, `dev.to`, `martinfowler.com`).

---

## 2. Color Palette

### Neutrals (Dark Mode - Primary)
- **Canvas / Background**: `#0f172a` (Slate 900)
- **Card Surface**: `#18181b` (Zinc 900)
- **Elevated / Modal**: `#1e293b` (Slate 800)
- **Subtle Borders**: `#27272a` (Zinc 800) / `#334155` (Slate 700)
- **Dividers**: `#1e293b` (Slate 800)

### Text Hierarchy (Dark Mode)
- **Primary Text (Headlines, Titles)**: `#f8fafc` (Slate 50)
- **Secondary Text (Excerpts, Descriptions)**: `#94a3b8` (Slate 400)
- **Tertiary / Metadata (Dates, Domains)**: `#64748b` (Slate 500)

### Accents & Brand
- **Brand Blue**: `#2563eb` (Blue 600) — Primary CTA, Upvote Active, Badges
- **Brand Blue Hover**: `#1d4ed8` (Blue 700)
- **Brand Blue Soft**: `#1e3a8a` / `#172554` (Dark mode badge backgrounds)
- **Success / Karma**: `#10b981` (Emerald 500)
- **Flame / Trending**: `#f59e0b` (Amber 500)
- **Danger / Delete**: `#f43f5e` (Rose 500)

---

## 3. Component Specs

### 3.1 DevCard (`<DevCard />`)
- **Container**: `rounded-2xl`, `border border-slate-200/80 dark:border-zinc-800`, `p-5`, `bg-white dark:bg-[#18181b]`, `hover:border-zinc-700`.
- **Publisher Header**:
  - Source Domain Badge: `font-mono text-[11px]`, `bg-slate-100 dark:bg-zinc-800/80`, `text-slate-700 dark:text-zinc-300`, `rounded-md px-2.5 py-1`.
  - Recency: `text-xs text-slate-500 dark:text-zinc-400`.
  - External Link Icon: `ExternalLink` 12px with hover underline.
- **Headline**: `text-base sm:text-lg font-bold leading-snug text-slate-900 dark:text-white`, `hover:text-blue-600 dark:hover:text-blue-400`.
- **Excerpt / TL;DR**: `text-sm text-slate-600 dark:text-zinc-400 line-clamp-2 leading-relaxed`.
- **Tags**: Monospace pill badges (`#react`, `#rust`, `#docker`) with `text-[11px] font-mono`.
- **Action Bar**:
  - Upvote: `▲` ChevronUp + count, highlighted in Blue 600 on active.
  - Discussion: MessageSquare + comment count.
  - Bookmark: `🔖` Bookmark icon (filled on saved state).

### 3.2 Feed Navigation (`<FeedTabs />`)
- **Pill Tabs**:
  - Active: `bg-slate-900 text-white dark:bg-white dark:text-zinc-900 shadow-sm`.
  - Inactive: `bg-white dark:bg-zinc-900 text-slate-600 dark:text-zinc-300 border hover:bg-slate-50 dark:hover:bg-zinc-800`.
- **Topic Filter Chip**: Dropdown with search filter and quick follow/unfollow toggle.

---

## 4. Typography Scale

- **App Wordmark**: 18px / ExtraBold / tracking -0.025em
- **Post Title**: 18px (1.125rem) / Bold / leading 1.4
- **Card Excerpt**: 14px (0.875rem) / Regular / leading 1.6
- **Source & Tags**: 11px (0.6875rem) / Monospace / Medium
- **Metadata**: 12px (0.75rem) / Regular
