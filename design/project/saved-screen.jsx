// SavedScreen — Inbox of posts shared into the app from the system share-sheet.
// Implements the simplified delete pattern from DESIGN_SYSTEM.md §4.4:
//   · Swipe a card left → Delete (red) panel
//   · ⋯ button per card → bottom action sheet (Send / Copy / Delete)
//
// No destructive button is ever drawn on the card itself.
//
// The leading 14dp tile in the meta row shows the post's primary category
// initial in monochrome (1dp cardEdge border, inkSoft letter) — not a per-
// source brand letter, because the source name already appears in the text
// to the right. Falls back to a middle dot `·` while categorization is
// pending so the slot is never empty.
//
// Props:
//   themeKey  — 'light' | 'dark'
//   state     — 'resting' | 'swiped' | 'sheet'  (controls the demo state)
//
// Depends on themes.jsx (THEMES, SERIF, SERIF_READ, SANS, MONO).

const SAVED_POSTS_DATA = [
  { id: 1, author: 'haileymocaixi', source: 'Threads',
    when: '4:20 PM', unread: true, categories: ['AI'],
    body: 'The best way to predict the future is to read research papers from five years ago. Almost everything in today\u2019s AI hype cycle was sketched out around 2019\u20132020.' },
  { id: 2, author: '@somebody', source: 'X',
    when: '3:44 PM', unread: true, categories: [],   // pending categorization -> `·`
    body: 'Third one — just to see how this card looks with very little body text.' },
  { id: 3, author: 'sama', source: 'OpenAI',
    when: '3:43 PM', unread: true, categories: ['Learn'],
    body: 'Second test post — pure text, no URL.' },
  { id: 4, author: 'yingrushih', source: 'Threads',
    when: 'May 17', unread: false, categories: ['Design'],
    body: 'Working on a calmer feed reader. The trick is filtering at save-time, not read-time — that\u2019s where intent lives.' },
];

const SAVED_FILTERS = [
  { id: 'all',  label: 'All'  },
  { id: 'misc', label: 'Misc' },
];

const savedBtnReset = {
  background: 'transparent', border: 'none', cursor: 'pointer', padding: 0, lineHeight: 0,
};

// ─── Icons ──────────────────────────────────────────────────────────────
function SavedIcon({ name, size = 16, w = 1.6 }) {
  const s = { width: size, height: size, display: 'block', flexShrink: 0 };
  const p = { fill: 'none', stroke: 'currentColor', strokeWidth: w, strokeLinecap: 'round', strokeLinejoin: 'round' };
  switch (name) {
    case 'back':    return <svg style={s} viewBox="0 0 24 24"><path {...p} d="M15 5l-7 7 7 7"/></svg>;
    case 'search':  return <svg style={s} viewBox="0 0 24 24"><circle {...p} cx="11" cy="11" r="6.5"/><path {...p} d="M16 16l4 4"/></svg>;
    case 'arrow':   return <svg style={s} viewBox="0 0 24 24"><path {...p} d="M5 12h14M13 5l7 7-7 7"/></svg>;
    case 'archive': return <svg style={s} viewBox="0 0 24 24"><rect {...p} x="3" y="5" width="18" height="4" rx="1"/><path {...p} d="M5 9v10h14V9M10 13h4"/></svg>;
    case 'trash':   return <svg style={s} viewBox="0 0 24 24"><path {...p} d="M4 7h16M9 7V4h6v3M6 7l1 13h10l1-13M10 11v6M14 11v6"/></svg>;
    case 'more':    return <svg style={s} viewBox="0 0 24 24"><circle cx="5" cy="12" r="1.5" fill="currentColor"/><circle cx="12" cy="12" r="1.5" fill="currentColor"/><circle cx="19" cy="12" r="1.5" fill="currentColor"/></svg>;
    case 'link':    return <svg style={s} viewBox="0 0 24 24"><path {...p} d="M10 14a4 4 0 005.66 0l3-3a4 4 0 10-5.66-5.66L11.5 7M14 10a4 4 0 00-5.66 0l-3 3a4 4 0 105.66 5.66L12.5 17"/></svg>;
    case 'send':    return <svg style={s} viewBox="0 0 24 24"><path {...p} d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z"/></svg>;
    default: return null;
  }
}

// ─── Atoms ──────────────────────────────────────────────────────────────
// Leading 14dp tile shows the post's primary category initial. Monochrome
// (1dp cardEdge border, inkSoft letter) — no per-source brand color, because
// the source name already lives in the text segment to the right. Falls back
// to `·` while categorization is pending so the slot is never empty.
function categoryGlyph(p) {
  if (!p.categories || p.categories.length === 0) return '·';
  const first = p.categories[0];
  return first ? first.charAt(0).toUpperCase() : '·';
}

function SavedSourceChip({ p, t }) {
  return (
    <span style={{
      display: 'inline-flex', alignItems: 'center', gap: 7,
      fontFamily: SANS, fontSize: 12, fontWeight: 500, color: t.inkSoft,
      minWidth: 0, overflow: 'hidden',
    }}>
      <span style={{
        width: 14, height: 14, borderRadius: 4,
        border: `1px solid ${t.cardEdge}`,
        color: t.inkSoft,
        display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
        fontFamily: SANS, fontWeight: 600, fontSize: 8.5,
        letterSpacing: '-0.3px', flexShrink: 0,
      }}>{categoryGlyph(p)}</span>
      <span style={{
        whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', minWidth: 0,
      }}>{p.source} · {p.author}</span>
    </span>
  );
}

function SavedPostMeta({ p, t, showOverflow }) {
  return (
    <div style={{
      display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 10,
    }}>
      <SavedSourceChip p={p} t={t} />
      <div style={{
        display: 'flex', alignItems: 'center', gap: 8, flexShrink: 0,
        fontFamily: MONO, fontSize: 10.5, fontWeight: 500,
        color: t.inkMute, letterSpacing: '0.3px',
      }}>
        <span>{p.when}</span>
        {showOverflow && (
          <button style={{
            ...savedBtnReset, color: t.inkMute,
            padding: 4, margin: '-4px -6px -4px 0',
            display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
            width: 26, height: 22, borderRadius: 6,
          }}>
            <SavedIcon name="more" size={16} w={1.8} />
          </button>
        )}
      </div>
    </div>
  );
}

function SavedPostText({ p, t }) {
  return (
    <div style={{
      fontFamily: SERIF_READ, fontSize: 15.5, lineHeight: 1.45,
      color: t.ink, textWrap: 'pretty', letterSpacing: '-0.05px',
    }}>{p.body}</div>
  );
}

// ─── PostCard — with swipe-reveal + overflow ────────────────────────────
function SavedPostCard({ p, t, revealed = false }) {
  const offset = revealed ? 88 : 0;
  return (
    <div style={{ position: 'relative', borderRadius: 16, overflow: 'hidden' }}>
      {/* Underlay action panel (revealed when card swipes left) */}
      <div style={{
        position: 'absolute', inset: 0, display: 'flex', justifyContent: 'flex-end',
      }}>
        <div style={{
          width: 88, background: '#E5484D',
          display: 'flex', flexDirection: 'column',
          alignItems: 'center', justifyContent: 'center', gap: 4,
          color: '#FFFFFF',
        }}>
          <SavedIcon name="trash" size={18} w={1.7} />
          <span style={{
            fontFamily: SANS, fontSize: 10.5, fontWeight: 700, letterSpacing: '0.3px',
          }}>Delete</span>
        </div>
      </div>

      {/* Card surface */}
      <div style={{
        transform: `translateX(-${offset}px)`,
        transition: 'transform 280ms cubic-bezier(.2,.7,.2,1)',
        position: 'relative',
      }}>
        <div style={{
          position: 'relative',
          background: t.card,
          border: `1px solid ${t.cardEdge}`,
          borderRadius: 16,
          padding: '14px 14px 14px 16px',
          boxShadow: t.cardShadow,
        }}>
          {p.unread && (
            <div style={{
              position: 'absolute', left: 0, top: 14, bottom: 14, width: 2,
              background: t.accent, borderRadius: 2,
            }} />
          )}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 9 }}>
            <SavedPostMeta p={p} t={t} showOverflow />
            <SavedPostText p={p} t={t} />
          </div>
        </div>
      </div>
    </div>
  );
}

// ─── Pending sync strip (gold) ──────────────────────────────────────────
function SavedSyncStrip({ t }) {
  return (
    <button style={{
      ...savedBtnReset,
      display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 10,
      width: '100%',
      padding: '10px 12px 10px 14px',
      background: 'rgba(165,141,102,0.10)',
      border: `1px solid rgba(165,141,102,0.32)`,
      borderRadius: 12,
      textAlign: 'left',
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 11, minWidth: 0 }}>
        <span style={{
          width: 8, height: 8, borderRadius: '50%', background: t.gold, flexShrink: 0,
        }} />
        <div style={{ minWidth: 0 }}>
          <div style={{
            fontFamily: SANS, fontSize: 13, fontWeight: 600, color: t.ink, lineHeight: 1.2,
          }}>4 saves waiting</div>
          <div style={{
            marginTop: 2,
            fontFamily: SERIF_READ, fontStyle: 'italic',
            fontSize: 12, color: t.inkSoft, lineHeight: 1.3,
          }}>Finish Notion sync to flush</div>
        </div>
      </div>
      <span style={{
        color: t.gold, display: 'inline-flex', alignItems: 'center', gap: 6,
        fontFamily: SANS, fontSize: 12, fontWeight: 600, letterSpacing: '0.1px', flexShrink: 0,
      }}>
        Set up <SavedIcon name="arrow" size={13} w={1.8}/>
      </span>
    </button>
  );
}

// ─── Search row (passive) ───────────────────────────────────────────────
function SavedSearchRow({ t }) {
  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 10,
      padding: '10px 12px',
      background: t.card,
      border: `1px solid ${t.cardEdge}`,
      borderRadius: 12,
      color: t.inkMute,
      height: 40,
    }}>
      <SavedIcon name="search" size={15} w={1.7} />
      <span style={{ fontFamily: SANS, fontSize: 13, color: t.inkMute }}>
        Search content, author, source
      </span>
    </div>
  );
}

// ─── Filter chips ───────────────────────────────────────────────────────
function SavedFilterRow({ t, active = 'all' }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
      {SAVED_FILTERS.map(f => {
        const on = f.id === active;
        return (
          <button key={f.id} style={{
            ...savedBtnReset,
            display: 'inline-flex', alignItems: 'center',
            padding: '8px 14px', borderRadius: 999,
            background: on ? t.accentSoft : 'transparent',
            border: `1px solid ${on ? t.accent : t.cardEdge}`,
            color: on ? t.accent : t.inkSoft,
            fontFamily: SANS, fontSize: 12.5, fontWeight: 600, letterSpacing: '0.1px',
          }}>
            {f.label}
          </button>
        );
      })}
    </div>
  );
}

// ─── Action sheet (D — overflow target) ─────────────────────────────────
function SavedActionSheet({ t }) {
  const rows = [
    { icon: 'send', label: 'Send to agent', hint: 'Add to today\u2019s briefing' },
    { icon: 'link', label: 'Copy link',     hint: 'Original post URL' },
    { icon: 'trash', label: 'Delete',       destructive: true },
  ];
  return (
    <div style={{
      position: 'absolute', left: 0, right: 0, bottom: 0,
      background: t.card,
      borderTopLeftRadius: 22, borderTopRightRadius: 22,
      padding: '10px 14px 22px',
      boxShadow: '0 -20px 40px rgba(0,0,0,0.35), 0 -1px 0 ' + t.cardEdge,
      zIndex: 10,
    }}>
      <div style={{
        width: 36, height: 4, borderRadius: 2,
        background: t.inkMute, opacity: 0.4,
        margin: '4px auto 14px',
      }} />
      <div style={{
        fontFamily: SERIF_READ, fontStyle: 'italic',
        fontSize: 13, color: t.inkSoft, padding: '0 4px 12px', lineHeight: 1.4,
      }}>“The best way to predict the future…”</div>
      <div style={{ display: 'flex', flexDirection: 'column' }}>
        {rows.map((r, i) => (
          <button key={r.label} style={{
            ...savedBtnReset,
            display: 'flex', alignItems: 'center', gap: 14,
            padding: '14px 4px',
            borderTop: i ? `1px solid ${t.cardEdge}` : 'none',
            color: r.destructive ? '#E5484D' : t.ink,
            fontFamily: SANS, fontSize: 14.5, fontWeight: 500,
            textAlign: 'left',
          }}>
            <SavedIcon name={r.icon} size={18} w={1.7} />
            <span style={{ flex: 1 }}>{r.label}</span>
            {r.hint && (
              <span style={{
                fontFamily: SERIF_READ, fontStyle: 'italic',
                fontSize: 12, color: t.inkMute, fontWeight: 400,
              }}>{r.hint}</span>
            )}
          </button>
        ))}
      </div>
    </div>
  );
}

// ─── SavedScreen ────────────────────────────────────────────────────────
function SavedScreen({ themeKey = 'dark', state = 'resting' }) {
  const t = THEMES[themeKey];
  const showSheet = state === 'sheet';

  return (
    <div style={{
      width: '100%', height: '100%',
      background: t.bg, color: t.ink, fontFamily: SANS,
      overflowY: 'auto', overflowX: 'hidden',
      position: 'relative',
    }}>
      {/* Background wash */}
      <div style={{
        position: 'absolute', inset: 0, pointerEvents: 'none',
        background: `radial-gradient(120% 60% at 50% 0%, ${t.bgWash} 0%, ${t.bg} 60%)`,
      }} />

      <div style={{
        position: 'relative', padding: '18px 18px 32px',
        display: 'flex', flexDirection: 'column', gap: 12,
      }}>
        {/* Header */}
        <div style={{
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <button style={{ ...savedBtnReset, color: t.ink, padding: 4, marginLeft: -4 }}>
              <SavedIcon name="back" size={20} w={1.8} />
            </button>
            <span style={{
              fontFamily: SERIF, fontSize: 26, color: t.ink, letterSpacing: '-0.3px', lineHeight: 1,
            }}>Saved</span>
          </div>
        </div>

        <SavedSyncStrip t={t} />
        <SavedSearchRow t={t} />
        <SavedFilterRow t={t} />

        <div style={{ display: 'flex', flexDirection: 'column', gap: 10, marginTop: 2 }}>
          {SAVED_POSTS_DATA.map((p, idx) => (
            <SavedPostCard
              key={p.id}
              p={p} t={t}
              revealed={state === 'swiped' && idx === 0}
            />
          ))}
        </div>
      </div>

      {showSheet && <SavedActionSheet t={t} />}
    </div>
  );
}

Object.assign(window, {
  SavedScreen, SavedPostCard, SavedActionSheet, SavedSyncStrip,
  SavedSearchRow, SavedFilterRow,
  SAVED_POSTS_DATA, SAVED_FILTERS,
});
