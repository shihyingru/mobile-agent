// SettingsScreen + TimePicker — v4
// Bring-your-own keys (Gemini / Notion), provider picker, behavior toggles.
// Form fields are flat (rounded inputs, not cards). Only repeating list rows
// render as cards — matching the Home rule.
//
// Depends on themes.jsx (THEMES + type stacks).

// Each provider lists its selectable models. First entry is the default.
// Each model has an `id`, `label`, and a short `note` describing trade-offs.
const PROVIDERS = [
  { id: 'gemini',   name: 'Gemini',   glyph: 'G', bg: '#4285F4', fg: '#FFFFFF',
    models: [
      { id: '2.5-pro',   label: 'gemini-2.5-pro',   note: 'Best reasoning'  },
      { id: '2.5-flash', label: 'gemini-2.5-flash', note: 'Fast · cheap'    },
      { id: '2.0-flash', label: 'gemini-2.0-flash', note: 'Legacy'          },
    ] },
  { id: 'claude',   name: 'Claude',   glyph: 'C', bg: '#D97757', fg: '#FFFFFF',
    models: [
      { id: 'sonnet-4',  label: 'claude-sonnet-4',  note: 'Balanced'        },
      { id: 'opus-4',    label: 'claude-opus-4',    note: 'Deepest'         },
      { id: 'haiku-4',   label: 'claude-haiku-4',   note: 'Fast · cheap'    },
    ] },
  { id: 'openai',   name: 'OpenAI',   glyph: 'O', bg: '#0A0A0A', fg: '#FFFFFF',
    models: [
      { id: 'gpt-4.1',   label: 'gpt-4.1',          note: 'General'         },
      { id: 'gpt-4o',    label: 'gpt-4o',           note: 'Multimodal'      },
      { id: 'o4-mini',   label: 'o4-mini',          note: 'Cheap reasoning' },
    ] },
  { id: 'mistral',  name: 'Mistral',  glyph: 'M', bg: '#FA520F', fg: '#FFFFFF',
    models: [
      { id: 'large-2',   label: 'mistral-large-2',  note: 'Flagship'        },
      { id: 'medium-3',  label: 'mistral-medium-3', note: 'Balanced'        },
      { id: 'small-3',   label: 'mistral-small-3',  note: 'Fast'            },
    ] },
  { id: 'deepseek', name: 'DeepSeek', glyph: 'D', bg: '#4D6BFE', fg: '#FFFFFF',
    models: [
      { id: 'v3',        label: 'deepseek-v3',      note: 'General'         },
      { id: 'r1',        label: 'deepseek-r1',      note: 'Reasoning'       },
    ] },
  { id: 'llama',    name: 'Llama',    glyph: 'L', bg: '#0866FF', fg: '#FFFFFF',
    models: [
      { id: '3.3-70b',   label: 'llama-3.3-70b',    note: 'Flagship'        },
      { id: '3.2-11b',   label: 'llama-3.2-11b',    note: 'Small · fast'    },
    ] },
];

function SettingsInput({ label, value, theme }) {
  const t = theme;
  return (
    <div style={{
      background: t.card,
      border: `1px solid ${t.cardEdge}`,
      borderRadius: 12,
      padding: '10px 14px',
      position: 'relative',
    }}>
      <div style={{
        fontFamily: SANS, fontSize: 11, fontWeight: 500,
        color: t.inkMute, lineHeight: 1.2,
      }}>{label}</div>
      <div style={{
        marginTop: 4,
        display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8,
      }}>
        <div style={{
          fontFamily: value ? MONO : SANS,
          fontSize: 14,
          color: value ? t.ink : t.inkMute,
          lineHeight: 1.3,
          whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
          flex: 1, minWidth: 0,
          letterSpacing: value ? '0.4px' : 0,
        }}>{value || label}</div>
      </div>
    </div>
  );
}

function Toggle({ on, theme, isLight }) {
  const t = theme;
  return (
    <div style={{
      width: 44, height: 26, borderRadius: 13, flexShrink: 0,
      background: on ? t.accent : (isLight ? 'rgba(8,58,79,0.18)' : 'rgba(229,225,221,0.20)'),
      position: 'relative', transition: 'background 200ms',
    }}>
      <div style={{
        position: 'absolute', top: 2, left: on ? 20 : 2,
        width: 22, height: 22, borderRadius: '50%',
        background: '#FFFFFF',
        boxShadow: '0 1px 3px rgba(0,0,0,0.18), 0 0 0 1px rgba(0,0,0,0.04)',
        transition: 'left 200ms',
      }} />
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// SettingsScreen
//
// Props:
//   themeKey       — 'light' | 'dark'
//   state          — 'empty' | 'saved' | 'typing'
//   provider       — id from PROVIDERS (default 'gemini')
//   scrollTo       — 'top' | 'bottom' (auto-scroll on mount)
//   showTimePicker — render the modal over this screen
// ─────────────────────────────────────────────────────────────
function SettingsScreen({
  themeKey = 'light', state = 'empty', provider = 'gemini', model,
  scrollTo = 'top', showTimePicker = false,
}) {
  const activeProvider = PROVIDERS.find(p => p.id === provider) || PROVIDERS[0];
  const activeModelId  = model || activeProvider.models[0].id;
  const activeModel    = activeProvider.models.find(m => m.id === activeModelId) || activeProvider.models[0];
  const t = THEMES[themeKey];
  const isLight = t.isLight;
  const scrollRef = React.useRef(null);
  React.useEffect(() => {
    if (scrollRef.current && scrollTo === 'bottom') {
      scrollRef.current.scrollTop = 9999;
    }
  }, [scrollTo]);

  const values = state === 'saved'
    ? { apiKey: '••••k7Qa', notionToken: '•••••••9F2c', notionDb: 'abc123def456789012345678901234ab' }
    : state === 'typing'
      ? { apiKey: '•'.repeat(16), notionToken: '', notionDb: 'https://www.notion.so/Tasks-abc123def4' }
      : { apiKey: '', notionToken: '', notionDb: '' };

  const hasChanges = state === 'typing';
  const justSaved  = state === 'saved';

  const sectionLabel = {
    fontFamily: MONO, fontSize: 10, fontWeight: 600,
    letterSpacing: '2px', textTransform: 'uppercase',
    color: t.inkMute,
    margin: '4px 0 10px',
  };

  return (
    <div ref={scrollRef} style={{
      width: '100%', height: '100%',
      background: t.bg,
      color: t.ink,
      fontFamily: SANS,
      overflowY: 'auto', overflowX: 'hidden',
      position: 'relative',
    }}>
      {/* Wash */}
      <div style={{
        position: 'absolute', inset: 0, pointerEvents: 'none',
        background: `radial-gradient(120% 60% at 50% 0%, ${t.bgWash} 0%, ${t.bg} 60%)`,
      }} />

      <div style={{ position: 'relative', padding: '18px 18px 36px', display: 'flex', flexDirection: 'column', gap: 8 }}>

        {/* Header */}
        <div style={{
          display: 'flex', alignItems: 'center', gap: 10,
          padding: '4px 0 6px',
        }}>
          <button style={{
            background: 'transparent', border: 'none', cursor: 'pointer',
            color: t.ink, padding: 4, lineHeight: 0, marginLeft: -4,
          }}>
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
              <path d="M15 5l-7 7 7 7" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
          </button>
          <div style={{
            fontFamily: SERIF, fontSize: 26, fontWeight: 400,
            color: t.ink, letterSpacing: '-0.3px', lineHeight: 1,
          }}>Settings</div>
        </div>

        {/* Intro */}
        <div style={{
          fontFamily: SERIF_READ, fontStyle: 'italic',
          fontSize: 14, lineHeight: 1.45, color: t.inkSoft,
          padding: '0 2px 8px',
          textWrap: 'pretty',
        }}>Bring your own keys. They’re stored encrypted on this device — they never leave your phone.</div>

        {/* Provider picker */}
        <div style={sectionLabel}>AI Provider</div>
        <div style={{
          display: 'flex', gap: 10,
          overflowX: 'auto', overflowY: 'hidden',
          margin: '0 -18px',
          padding: '4px 18px 12px',
          scrollbarWidth: 'none',
        }}>
          <style>{`.providerScroll::-webkit-scrollbar { display: none; }`}</style>
          {PROVIDERS.map(p => {
            const active = p.id === provider;
            // The label under each provider tile = the currently-selected model
            // when active, otherwise the default model.
            const tileModel = active ? activeModel.label : p.models[0].label;
            return (
              <button key={p.id} style={{
                flexShrink: 0,
                minWidth: 124,
                padding: '12px 14px',
                background: active
                  ? (isLight ? 'rgba(64,126,140,0.08)' : 'rgba(192,213,214,0.10)')
                  : 'transparent',
                border: active ? `1.5px solid ${t.accent}` : `1px solid ${t.cardEdge}`,
                borderRadius: 14,
                cursor: 'pointer',
                display: 'flex', flexDirection: 'column', alignItems: 'flex-start', gap: 8,
                textAlign: 'left',
                position: 'relative',
              }}>
                <div style={{
                  display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                  width: '100%',
                }}>
                  <div style={{
                    width: 26, height: 26, borderRadius: 7,
                    background: p.bg, color: p.fg,
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    fontFamily: SANS, fontWeight: 700, fontSize: 14,
                    letterSpacing: '-0.5px',
                  }}>{p.glyph}</div>
                  <div style={{
                    width: 16, height: 16, borderRadius: '50%',
                    border: `1.5px solid ${active ? t.accent : t.inkMute}`,
                    background: active ? t.accent : 'transparent',
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    transition: 'all 180ms',
                  }}>
                    {active && (
                      <svg width="9" height="9" viewBox="0 0 24 24" fill="none">
                        <path d="M5 12.5l4.5 4.5L19 7.5" stroke={t.onAccent} strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" />
                      </svg>
                    )}
                  </div>
                </div>
                <div style={{ width: '100%' }}>
                  <div style={{
                    fontFamily: SANS, fontSize: 13.5, fontWeight: 600,
                    color: t.ink, lineHeight: 1.2,
                  }}>{p.name}</div>
                  <div style={{
                    marginTop: 2,
                    display: 'flex', alignItems: 'center', gap: 4,
                    fontFamily: MONO, fontSize: 10, fontWeight: 400,
                    color: active ? t.accent : t.inkMute, letterSpacing: '0.2px',
                    whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
                  }}>
                    <span style={{
                      overflow: 'hidden', textOverflow: 'ellipsis', minWidth: 0,
                    }}>{tileModel}</span>
                    {p.models.length > 1 && (
                      <span style={{
                        fontFamily: SANS, fontSize: 9, fontWeight: 600,
                        padding: '1px 5px', borderRadius: 999,
                        background: active ? t.accentSoft : (isLight ? 'rgba(8,58,79,0.06)' : 'rgba(229,225,221,0.08)'),
                        color: active ? t.accent : t.inkMute,
                        letterSpacing: '0.3px', flexShrink: 0,
                      }}>+{p.models.length - 1}</span>
                    )}
                  </div>
                </div>
              </button>
            );
          })}
        </div>

        {/* Model picker — segmented chips for the active provider */}
        <div style={{
          display: 'flex', alignItems: 'baseline', justifyContent: 'space-between',
          marginTop: 4, marginBottom: 8,
        }}>
          <div style={sectionLabel}>Model</div>
          <div style={{
            fontFamily: MONO, fontSize: 9.5, fontWeight: 500,
            color: t.inkMute, letterSpacing: '0.4px',
          }}>{activeProvider.name.toLowerCase()} · {activeProvider.models.length} options</div>
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {activeProvider.models.map(m => {
            const isOn = m.id === activeModel.id;
            return (
              <button key={m.id} style={{
                appearance: 'none', textAlign: 'left',
                background: isOn ? t.accentSoft : t.card,
                border: `1px solid ${isOn ? t.accent : t.cardEdge}`,
                borderRadius: 12,
                padding: '10px 14px',
                cursor: 'pointer',
                display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 10,
              }}>
                <div style={{ minWidth: 0 }}>
                  <div style={{
                    fontFamily: MONO, fontSize: 13, fontWeight: 500,
                    color: t.ink, letterSpacing: '0.2px',
                    whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
                  }}>{m.label}</div>
                  <div style={{
                    marginTop: 2,
                    fontFamily: SERIF_READ, fontStyle: 'italic',
                    fontSize: 12, color: t.inkSoft, lineHeight: 1.3,
                  }}>{m.note}</div>
                </div>
                <div style={{
                  width: 18, height: 18, borderRadius: '50%',
                  border: `1.5px solid ${isOn ? t.accent : t.inkMute}`,
                  background: isOn ? t.accent : 'transparent',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  flexShrink: 0,
                }}>
                  {isOn && (
                    <svg width="10" height="10" viewBox="0 0 24 24" fill="none">
                      <path d="M5 12.5l4.5 4.5L19 7.5" stroke={t.onAccent} strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" />
                    </svg>
                  )}
                </div>
              </button>
            );
          })}
        </div>

        {/* Credentials */}
        <div style={{ ...sectionLabel, marginTop: 14 }}>Credentials</div>
        <SettingsInput label={`${activeProvider.name} API key`} value={values.apiKey} theme={t} />
        <SettingsInput label="Notion integration token" value={values.notionToken} theme={t} />

        {/* Data source */}
        <div style={{ ...sectionLabel, marginTop: 14 }}>Data source</div>
        <SettingsInput label="Notion database (URL or ID)" value={values.notionDb} theme={t} />

        {/* Save */}
        <div style={{ marginTop: 14, display: 'flex', flexDirection: 'column', gap: 10 }}>
          <button style={{
            height: 46,
            background: hasChanges ? t.accent : (isLight ? 'rgba(8,58,79,0.10)' : 'rgba(192,213,214,0.12)'),
            color: hasChanges ? t.onAccent : t.inkMute,
            border: 'none', borderRadius: 14,
            fontFamily: SANS, fontSize: 14, fontWeight: 600,
            letterSpacing: '0.1px', cursor: hasChanges ? 'pointer' : 'default',
            boxShadow: hasChanges ? `0 6px 18px ${t.accent}33` : 'none',
          }}>Save</button>

          {justSaved && (
            <div style={{
              display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6,
              fontFamily: SANS, fontSize: 12.5, fontWeight: 600,
              color: t.statusGood, letterSpacing: '0.2px',
            }}>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                <path d="M5 12.5l4.5 4.5L19 7.5" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
              <span>Saved</span>
            </div>
          )}

          <button style={{
            background: 'transparent', border: 'none', cursor: 'pointer',
            padding: '4px 0',
            fontFamily: SANS, fontSize: 13, fontWeight: 600,
            color: t.accent, letterSpacing: '0.1px',
          }}>Test Notion connection</button>
        </div>

        {/* Behavior */}
        <div style={{ ...sectionLabel, marginTop: 18 }}>Behavior</div>
        <div style={{ borderTop: `1px solid ${t.cardEdge}` }}>
          {[
            { kind: 'toggle', title: 'Auto-run on launch',
              sub: 'Run the agent as soon as you open the app.',
              on: true },
            { kind: 'toggle', title: 'Daily briefing notification',
              sub: 'Push the briefing when it’s ready.',
              on: true },
            { kind: 'value', title: 'Briefing time',
              sub: 'Wakes the agent and delivers the briefing.',
              value: '9:00 AM', indent: true },
            { kind: 'toggle', title: 'Quiet hours',
              sub: 'Pause background runs 9:00 PM — 7:00 AM.',
              on: false },
          ].map(row => (
            <div key={row.title} style={{
              padding: row.indent ? '12px 2px 12px 18px' : '14px 2px',
              borderBottom: `1px solid ${t.cardEdge}`,
              borderLeft: row.indent ? `2px solid ${t.accentSoft}` : 'none',
              marginLeft: row.indent ? 2 : 0,
              display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 14,
              cursor: row.kind === 'value' ? 'pointer' : 'default',
            }}>
              <div style={{ minWidth: 0 }}>
                <div style={{
                  fontFamily: SANS, fontSize: 14.5, fontWeight: 500, color: t.ink, lineHeight: 1.25,
                }}>{row.title}</div>
                <div style={{
                  marginTop: 3,
                  fontFamily: SERIF_READ, fontStyle: 'italic',
                  fontSize: 13, color: t.inkSoft, lineHeight: 1.4,
                }}>{row.sub}</div>
              </div>

              {row.kind === 'toggle' && <Toggle on={row.on} theme={t} isLight={isLight} />}

              {row.kind === 'value' && (
                <div style={{
                  display: 'flex', alignItems: 'center', gap: 6,
                  fontFamily: MONO, fontSize: 14, fontWeight: 500,
                  color: t.accent, letterSpacing: '0.2px', flexShrink: 0,
                }}>
                  <span>{row.value}</span>
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                    <path d="M9 6l6 6-6 6" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
                  </svg>
                </div>
              )}
            </div>
          ))}
        </div>

        {/* Send test notification */}
        <button style={{
          marginTop: 18,
          alignSelf: 'center',
          background: 'transparent', border: 'none', cursor: 'pointer',
          padding: '6px 0',
          fontFamily: SANS, fontSize: 13, fontWeight: 600,
          color: t.accent, letterSpacing: '0.1px',
        }}>Send test notification</button>
      </div>

      {showTimePicker && <TimePicker themeKey={themeKey} />}
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// TimePicker — modal dialog (analog clock + AM/PM stack)
// ─────────────────────────────────────────────────────────────
function TimePicker({ themeKey = 'light', hour = 9, minute = 0, ampm = 'AM' }) {
  const t = THEMES[themeKey];
  const isLight = t.isLight;

  const surface = isLight ? '#F6F3EF' : '#0E3D52';
  const tile = isLight ? 'rgba(8,58,79,0.05)' : 'rgba(255,255,255,0.04)';
  const tileActive = t.accentSoft;

  const clockSize = 240;
  const cx = clockSize / 2;
  const cy = clockSize / 2;
  const r = clockSize / 2 - 24;
  const handR = r * 0.62;

  const angleFor = (h) => ((h % 12) - 3) * (Math.PI / 6);
  const handX = cx + handR * Math.cos(angleFor(hour));
  const handY = cy + handR * Math.sin(angleFor(hour));

  const HourSlot = ({ value, active }) => (
    <div style={{
      flex: 1,
      height: 76,
      borderRadius: 14,
      background: active ? tileActive : tile,
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      fontFamily: SERIF, fontSize: 52, fontWeight: 400,
      color: active ? t.accent : t.ink,
      letterSpacing: '-1px', lineHeight: 1,
    }}>{String(value).padStart(2, '0')}</div>
  );

  const AMPMTile = ({ label, active }) => (
    <div style={{
      flex: 1,
      borderRadius: 10,
      background: active ? tileActive : 'transparent',
      color: active ? t.accent : t.inkSoft,
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      fontFamily: SANS, fontSize: 14, fontWeight: 600,
      letterSpacing: '0.5px',
    }}>{label}</div>
  );

  return (
    <div style={{
      position: 'absolute', inset: 0,
      background: 'rgba(4,22,30,0.55)',
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      padding: 16,
      zIndex: 10,
      backdropFilter: 'blur(2px)',
    }}>
      <div style={{
        width: '100%',
        background: surface,
        borderRadius: 22,
        padding: '22px 18px 14px',
        boxShadow: '0 30px 60px rgba(0,0,0,0.35), 0 0 0 1px ' + t.cardEdge,
        display: 'flex', flexDirection: 'column', gap: 18,
      }}>
        <div style={{
          fontFamily: SERIF, fontSize: 22, fontWeight: 400,
          color: t.ink, letterSpacing: '-0.2px', lineHeight: 1,
        }}>Pick briefing time</div>

        <div style={{ display: 'flex', alignItems: 'stretch', gap: 8 }}>
          <HourSlot value={hour} active />
          <div style={{
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontFamily: SERIF, fontSize: 44, color: t.inkSoft, padding: '0 2px',
          }}>:</div>
          <HourSlot value={minute} />
          <div style={{
            width: 56, padding: 4, borderRadius: 12,
            background: tile,
            display: 'flex', flexDirection: 'column', gap: 4,
          }}>
            <AMPMTile label="AM" active={ampm === 'AM'} />
            <AMPMTile label="PM" active={ampm === 'PM'} />
          </div>
        </div>

        <div style={{
          alignSelf: 'center', position: 'relative',
          width: clockSize, height: clockSize,
          borderRadius: '50%',
          background: tile,
          margin: '4px 0',
        }}>
          <svg width={clockSize} height={clockSize} style={{ position: 'absolute', inset: 0 }}>
            <line x1={cx} y1={cy} x2={handX} y2={handY}
              stroke={t.accent} strokeWidth="2" strokeLinecap="round" />
            <circle cx={cx} cy={cy} r="3" fill={t.accent} />
            <circle cx={handX} cy={handY} r="20" fill={t.accent} fillOpacity="0.92" />
          </svg>

          {Array.from({ length: 12 }, (_, i) => {
            const num = i + 1;
            const a = angleFor(num);
            const x = cx + r * Math.cos(a);
            const y = cy + r * Math.sin(a);
            const isActive = num === hour;
            return (
              <div key={num} style={{
                position: 'absolute',
                left: x, top: y,
                transform: 'translate(-50%, -50%)',
                fontFamily: SANS, fontSize: 17, fontWeight: 500,
                color: isActive ? t.onAccent : t.ink,
                pointerEvents: 'none',
                zIndex: 1,
              }}>{num}</div>
            );
          })}
        </div>

        <div style={{
          display: 'flex', justifyContent: 'flex-end', gap: 28,
          padding: '4px 8px 2px',
        }}>
          <button style={{
            background: 'transparent', border: 'none', cursor: 'pointer',
            fontFamily: SANS, fontSize: 14, fontWeight: 600,
            color: t.inkSoft, letterSpacing: '0.3px', padding: '6px 0',
          }}>Cancel</button>
          <button style={{
            background: 'transparent', border: 'none', cursor: 'pointer',
            fontFamily: SANS, fontSize: 14, fontWeight: 700,
            color: t.accent, letterSpacing: '0.3px', padding: '6px 0',
          }}>Set</button>
        </div>
      </div>
    </div>
  );
}

Object.assign(window, { SettingsScreen, TimePicker, PROVIDERS });
