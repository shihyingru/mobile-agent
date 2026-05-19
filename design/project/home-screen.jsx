// HomeScreen — v4 card layout, time-aware
// Depends on themes.jsx (THEMES, SLOTS, TASKS, QUOTES, useTimeSlot, type stacks).

// ─────────────────────────────────────────────────────────────
// Icons — minimal, currentColor strokes.
// ─────────────────────────────────────────────────────────────
function Icon({ name, size = 18, stroke = 1.6 }) {
  const s = { width: size, height: size, display: 'block', flexShrink: 0 };
  const p = { fill: 'none', stroke: 'currentColor', strokeWidth: stroke, strokeLinecap: 'round', strokeLinejoin: 'round' };
  switch (name) {
    case 'sunrise':
      return (<svg style={s} viewBox="0 0 24 24"><circle {...p} cx="12" cy="14" r="3.5" /><path {...p} d="M3 18h18M12 6V3M5.6 9.6L4 8M18.4 9.6L20 8" /></svg>);
    case 'refresh':
      return (<svg style={s} viewBox="0 0 24 24"><path {...p} d="M20 11A8 8 0 006.3 6.3M4 13a8 8 0 0013.7 4.7" /><path {...p} d="M20 4v5h-5M4 20v-5h5" /></svg>);
    case 'check':
      return (<svg style={s} viewBox="0 0 24 24"><path {...p} d="M5 12.5l4.5 4.5L19 7.5" /></svg>);
    case 'moon':
      return (<svg style={s} viewBox="0 0 24 24"><path {...p} d="M20 14.5A8 8 0 119.5 4a6.5 6.5 0 0010.5 10.5z" /></svg>);
    case 'chevron':
      return (<svg style={s} viewBox="0 0 24 24"><path {...p} d="M9 6l6 6-6 6" /></svg>);
    case 'settings':
      return (<svg style={s} viewBox="0 0 24 24"><circle {...p} cx="12" cy="12" r="2.6" /><path {...p} d="M19.4 15a1.8 1.8 0 00.4 2l.1.1a2 2 0 11-2.9 2.9l-.1-.1a1.8 1.8 0 00-2-.4 1.8 1.8 0 00-1 1.7V21a2 2 0 11-4 0v-.1A1.8 1.8 0 008 19.4a1.8 1.8 0 00-2 .4l-.1.1a2 2 0 11-2.9-2.9l.1-.1a1.8 1.8 0 00.4-2 1.8 1.8 0 00-1.7-1H1.8a2 2 0 110-4h.1A1.8 1.8 0 003.6 8a1.8 1.8 0 00-.4-2l-.1-.1a2 2 0 112.9-2.9l.1.1a1.8 1.8 0 002 .4H8a1.8 1.8 0 001-1.7V1.8a2 2 0 114 0v.1a1.8 1.8 0 001 1.7 1.8 1.8 0 002-.4l.1-.1a2 2 0 112.9 2.9l-.1.1a1.8 1.8 0 00-.4 2v.2a1.8 1.8 0 001.7 1h.1a2 2 0 110 4h-.1a1.8 1.8 0 00-1.7 1z" /></svg>);
    default: return null;
  }
}

// ─────────────────────────────────────────────────────────────
// SourceLogo — small brand chip.
// ─────────────────────────────────────────────────────────────
function SourceLogo({ source = 'Notion', size = 18, theme }) {
  const map = {
    Notion: { bg: '#FFFFFF', fg: '#231C12', glyph: 'N' },
    Linear: { bg: '#5E6AD2', fg: '#FFFFFF', glyph: 'L' },
    GitHub: { bg: '#1F2328', fg: '#F0F6FC', glyph: 'G' },
  };
  const c = map[source] || { bg: theme.ink, fg: theme.bg, glyph: source[0] };
  return (
    <div title={source} style={{
      width: size, height: size, borderRadius: 4,
      background: c.bg, color: c.fg,
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      fontFamily: SANS, fontWeight: 700, fontSize: size * 0.58,
      letterSpacing: '-0.5px',
      boxShadow: `0 0 0 1px ${theme.cardEdge}`,
      flexShrink: 0,
    }}>{c.glyph}</div>
  );
}

// ─────────────────────────────────────────────────────────────
// Card primitive
// ─────────────────────────────────────────────────────────────
function Card({ theme, children, style = {}, pad = 18 }) {
  return (
    <div style={{
      background: theme.card,
      border: `1px solid ${theme.cardEdge}`,
      borderRadius: 18,
      padding: pad,
      boxShadow: theme.cardShadow,
      ...style,
    }}>{children}</div>
  );
}

// Priority chip — high uses accent, medium uses gold, low is muted.
function PriorityChip({ level, theme }) {
  const palette = level === 'high'
    ? { bg: theme.accentSoft, fg: theme.accent, label: 'High' }
    : level === 'med'
      ? { bg: 'rgba(165,141,102,0.16)', fg: theme.gold, label: 'Medium' }
      : { bg: 'rgba(138,149,160,0.16)', fg: theme.inkMute, label: 'Low' };
  return (
    <span style={{
      display: 'inline-flex', alignItems: 'center', gap: 6,
      padding: '3px 9px', borderRadius: 999,
      background: palette.bg, color: palette.fg,
      fontFamily: SANS, fontSize: 11, fontWeight: 600,
      letterSpacing: '0.2px',
    }}>
      <span style={{
        width: 5, height: 5, borderRadius: '50%', background: palette.fg,
      }} />
      {palette.label}
    </span>
  );
}

// Status dot — animated only when live.
function StatusDot({ state, theme }) {
  const color = state === 'live' ? theme.statusGood
    : state === 'scheduled' ? theme.accent
    : state === 'sleeping' ? theme.inkMute
    : theme.gold;
  return (
    <span style={{ position: 'relative', display: 'inline-block', width: 10, height: 10 }}>
      <span style={{
        position: 'absolute', inset: 0, borderRadius: '50%', background: color,
      }} />
      {state === 'live' && (
        <span style={{
          position: 'absolute', inset: -4, borderRadius: '50%',
          background: color, opacity: 0.25, animation: 'pulse 1.6s ease-out infinite',
        }} />
      )}
    </span>
  );
}

// ─────────────────────────────────────────────────────────────
// HomeScreen — card layout.
// Props:
//   themeKey      — 'light' | 'dark' (default 'light')
//   overrideHour  — 0..23 (optional). When set, pins the time slot.
// ─────────────────────────────────────────────────────────────
function HomeScreen({ themeKey = 'light', overrideHour }) {
  const t = THEMES[themeKey];
  const { slot, dateLine } = useTimeSlot(overrideHour);

  return (
    <ThemeContext.Provider value={t}>
      <div style={{
        width: '100%', height: '100%',
        background: t.bg,
        color: t.ink,
        fontFamily: SANS,
        overflowY: 'auto', overflowX: 'hidden',
        position: 'relative',
      }}>
        <style>{`
          @keyframes pulse {
            0%   { transform: scale(0.6); opacity: 0.5; }
            80%  { transform: scale(1.6); opacity: 0; }
            100% { transform: scale(1.6); opacity: 0; }
          }
          @keyframes slotFade {
            from { opacity: 0; transform: translateY(-3px); }
            to   { opacity: 1; transform: translateY(0); }
          }
        `}</style>

        {/* Background wash — subtle radial tint at top */}
        <div style={{
          position: 'absolute', inset: 0, pointerEvents: 'none',
          background: `radial-gradient(120% 60% at 50% 0%, ${t.bgWash} 0%, ${t.bg} 60%)`,
        }} />

        <div style={{ position: 'relative', padding: '22px 18px 32px', display: 'flex', flexDirection: 'column', gap: 14 }}>

          {/* Top bar — wordmark + settings */}
          <div style={{
            display: 'flex', alignItems: 'center', justifyContent: 'space-between',
            padding: '0 4px',
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <div style={{
                width: 10, height: 10, borderRadius: '50%',
                background: t.accent,
                boxShadow: `0 0 0 3px ${t.accentSoft}`,
              }} />
              <span style={{
                fontFamily: SERIF, fontSize: 20, color: t.ink, letterSpacing: '-0.2px',
              }}>Agent</span>
            </div>
            <button style={{
              background: 'transparent', border: 'none', cursor: 'pointer',
              color: t.inkSoft, padding: 6, lineHeight: 0, opacity: 0.8,
            }}>
              <Icon name="settings" size={18} stroke={1.4} />
            </button>
          </div>

          {/* Greeting block (time-aware) */}
          <div key={slot.key} style={{ padding: '6px 4px 2px', animation: 'slotFade 380ms ease-out' }}>
            <div style={{
              fontFamily: MONO, fontSize: 10, fontWeight: 500,
              letterSpacing: '2px', textTransform: 'uppercase',
              color: t.inkMute,
            }}>{dateLine}</div>
            <div style={{
              marginTop: 10,
              fontFamily: SERIF, fontSize: 34, fontWeight: 400,
              letterSpacing: '-0.6px', lineHeight: 1.05, color: t.ink,
            }}>{slot.greeting}</div>
            <div style={{
              marginTop: 6,
              fontFamily: SERIF_READ, fontStyle: 'italic',
              fontSize: 15, lineHeight: 1.4, color: t.inkSoft,
            }}>{slot.sub}</div>
          </div>

          {/* Status / Run — inline */}
          <div style={{
            padding: '8px 4px 4px',
            display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12,
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12, minWidth: 0 }}>
              <StatusDot state={slot.statusDot} theme={t} />
              <div style={{ minWidth: 0 }}>
                <div key={slot.key + '-status'} style={{
                  fontFamily: SANS, fontSize: 14, fontWeight: 600, color: t.ink, lineHeight: 1.2,
                  animation: 'slotFade 380ms ease-out',
                }}>{slot.statusLabel}</div>
                <div style={{
                  marginTop: 2,
                  fontFamily: MONO, fontSize: 10.5, fontWeight: 400,
                  color: t.inkMute, letterSpacing: '0.3px',
                }}>Next run · {slot.nextRun}</div>
              </div>
            </div>
            <button key={slot.key + '-btn'} style={{
              display: 'inline-flex', alignItems: 'center', gap: 8,
              height: 38, padding: '0 14px 0 12px',
              background: t.accent, color: t.onAccent,
              border: 'none', borderRadius: 19,
              fontFamily: SANS, fontSize: 13, fontWeight: 600,
              letterSpacing: '0.1px', cursor: 'pointer',
              boxShadow: `0 4px 14px ${t.accent}33`,
              flexShrink: 0,
              animation: 'slotFade 380ms ease-out',
            }}>
              <Icon name={slot.runIcon} size={16} stroke={1.8} />
              <span>{slot.runLabel}</span>
            </button>
          </div>

          {/* Divider */}
          <div style={{ height: 1, background: t.cardEdge, margin: '4px 4px' }} />

          {/* Briefing — inline */}
          <div style={{ padding: '0 4px' }}>
            <div style={{
              display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 10,
              marginBottom: 10,
            }}>
              <div style={{
                fontFamily: MONO, fontSize: 10, fontWeight: 600,
                letterSpacing: '1.8px', textTransform: 'uppercase',
                color: t.accent,
              }}>Briefing</div>
              <div style={{
                fontFamily: MONO, fontSize: 9.5, fontWeight: 400,
                color: t.inkMute, letterSpacing: '0.4px',
              }}>gemini-2.5 · 312 tok</div>
            </div>

            <div key={slot.key + '-quote'} style={{
              fontFamily: SERIF_READ, fontSize: 18, lineHeight: 1.45,
              color: t.ink, textWrap: 'pretty', letterSpacing: '-0.1px',
              paddingLeft: 12,
              borderLeft: `2px solid ${t.accent}`,
              animation: 'slotFade 420ms ease-out',
            }}>{QUOTES[slot.key]}</div>
          </div>

          {/* Section label */}
          <div style={{
            display: 'flex', alignItems: 'baseline', justifyContent: 'space-between',
            padding: '6px 4px 0',
          }}>
            <div style={{
              fontFamily: SERIF, fontSize: 22, fontWeight: 400,
              color: t.ink, letterSpacing: '-0.2px', lineHeight: 1,
            }}>Today</div>
            <div style={{
              fontFamily: MONO, fontSize: 10, fontWeight: 500,
              letterSpacing: '1.6px', textTransform: 'uppercase',
              color: t.inkMute,
            }}>{TASKS.length} items</div>
          </div>

          {/* Task cards */}
          {TASKS.map(task => (
            <Card key={task.id} theme={t} pad={16}>
              <div style={{
                display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                gap: 10, marginBottom: 10,
              }}>
                <PriorityChip level={task.priority} theme={t} />
                <div style={{
                  display: 'flex', alignItems: 'center', gap: 8,
                  fontFamily: MONO, fontSize: 10.5, color: t.inkMute, letterSpacing: '0.3px',
                }}>
                  <span>{task.est}</span>
                  <span style={{ opacity: 0.4 }}>·</span>
                  <SourceLogo source={task.src} size={16} theme={t} />
                </div>
              </div>

              <div style={{
                fontFamily: SERIF_READ, fontSize: 18, fontWeight: 500,
                color: t.ink, lineHeight: 1.25, letterSpacing: '-0.1px',
              }}>{task.title}</div>

              <div style={{
                marginTop: 8,
                fontFamily: SERIF_READ, fontStyle: 'italic',
                fontSize: 14, lineHeight: 1.5, color: t.inkSoft,
                textWrap: 'pretty',
              }}>{task.tip}</div>

              <div style={{
                marginTop: 12,
                paddingTop: 10,
                borderTop: `1px solid ${t.cardEdge}`,
                display: 'flex', alignItems: 'center', justifyContent: 'space-between',
              }}>
                <span style={{
                  fontFamily: SANS, fontSize: 12, fontWeight: 600,
                  color: t.accent, letterSpacing: '0.1px',
                }}>Open task</span>
                <span style={{ color: t.accent, opacity: 0.7 }}>
                  <Icon name="chevron" size={14} stroke={2} />
                </span>
              </div>
            </Card>
          ))}

          {/* Footer */}
          <div style={{
            marginTop: 4,
            textAlign: 'center',
            fontFamily: MONO, fontSize: 9.5, fontWeight: 500,
            letterSpacing: '2px', textTransform: 'uppercase',
            color: t.inkMute,
          }}>
            Powered by Gemini · Notion MCP
          </div>
        </div>
      </div>
    </ThemeContext.Provider>
  );
}

Object.assign(window, {
  HomeScreen, Card, PriorityChip, StatusDot, Icon, SourceLogo,
});
