// LaunchScreen — v4 paper / Waterworks
// Sand paper, Navy ink, Teal pigment orb (light)  ·
// Deep navy ground, Sand wordmark, Aqua orb (dark).
//
// Depends on themes.jsx for THEMES + type stacks.

function BreathingOrb({ size = 110, theme }) {
  const accent = theme.accent;
  const deep   = theme.accentDeep;
  const isLight = theme.isLight;

  return (
    <div style={{
      position: 'relative', width: size, height: size,
      display: 'flex', alignItems: 'center', justifyContent: 'center',
    }}>
      {/* Soft halo */}
      <div style={{
        position: 'absolute', inset: -32, borderRadius: '50%',
        background: `radial-gradient(circle, ${accent}33 0%, ${accent}10 50%, transparent 72%)`,
        filter: 'blur(8px)',
        animation: 'orbBreathe 2600ms ease-in-out infinite',
      }} />

      {/* Pigment ring — single accent, gentle bloom */}
      <div style={{
        position: 'absolute', width: size + 6, height: size + 6, borderRadius: '50%',
        background: `radial-gradient(circle at 35% 30%, ${accent} 0%, ${deep} 70%, ${deep} 100%)`,
        filter: 'blur(6px)', opacity: 0.85,
        animation: 'orbBreathe 2600ms ease-in-out infinite',
      }} />

      {/* Core — pearl that takes the pigment underneath */}
      <div style={{
        position: 'absolute', width: size, height: size, borderRadius: '50%',
        background: isLight
          ? 'radial-gradient(circle at 35% 30%, rgba(255,255,255,0.95) 0%, rgba(255,255,255,0.45) 22%, rgba(255,255,255,0.12) 50%, transparent 72%)'
          : 'radial-gradient(circle at 35% 30%, rgba(229,225,221,0.85) 0%, rgba(229,225,221,0.30) 22%, rgba(229,225,221,0.08) 50%, transparent 72%)',
        mixBlendMode: 'screen',
        animation: 'orbBreathe 2600ms ease-in-out infinite',
      }} />

      {/* Specular highlight */}
      <div style={{
        position: 'absolute',
        top: size * 0.18, left: size * 0.24,
        width: size * 0.30, height: size * 0.20,
        borderRadius: '50%',
        background: 'radial-gradient(ellipse, rgba(255,255,255,0.55) 0%, rgba(255,255,255,0) 70%)',
        filter: 'blur(2px)',
      }} />
    </div>
  );
}

function LoadingDots({ theme }) {
  return (
    <div style={{ display: 'flex', gap: 8, alignItems: 'center', justifyContent: 'center' }}>
      {[0, 1, 2].map((i) => (
        <div key={i} style={{
          width: 4, height: 4, borderRadius: '50%',
          background: theme.inkMute,
          animation: `dotPulse 1400ms ease-in-out ${i * 200}ms infinite`,
        }} />
      ))}
    </div>
  );
}

function LaunchScreen({ themeKey = 'light' }) {
  const t = THEMES[themeKey];

  return (
    <div style={{
      width: '100%', height: '100%',
      position: 'relative',
      fontFamily: SANS,
      color: t.ink,
      overflow: 'hidden',
      background: t.bg,
    }}>
      <style>{`
        @keyframes orbBreathe {
          0%, 100% { transform: scale(1.0); }
          50%      { transform: scale(1.06); }
        }
        @keyframes dotPulse {
          0%, 100% { opacity: 0.25; transform: scale(0.8); }
          50%      { opacity: 1;    transform: scale(1); }
        }
        @keyframes fadeInUp {
          from { opacity: 0; transform: translateY(8px); }
          to   { opacity: 1; transform: translateY(0); }
        }
      `}</style>

      {/* Paper wash — radial tint at top, matching the Home screen wash */}
      <div style={{
        position: 'absolute', inset: 0, pointerEvents: 'none',
        background: `radial-gradient(120% 70% at 50% 32%, ${t.bgWash} 0%, ${t.bg} 65%)`,
      }} />

      {/* Center column */}
      <div style={{
        position: 'absolute',
        top: '38%', left: '50%',
        transform: 'translate(-50%, -50%)',
        display: 'flex', flexDirection: 'column', alignItems: 'center',
      }}>
        <BreathingOrb size={110} theme={t} />

        <div style={{
          margin: '56px 0 0',
          fontFamily: SERIF, fontSize: 38, fontWeight: 400,
          letterSpacing: '-0.6px', lineHeight: 1, color: t.ink,
          textAlign: 'center',
          animation: 'fadeInUp 400ms ease-out 500ms both',
        }}>Morning Agent</div>

        <div style={{
          marginTop: 12,
          fontFamily: SERIF_READ, fontStyle: 'italic',
          fontSize: 15, color: t.inkSoft, letterSpacing: '0.1px',
          animation: 'fadeInUp 400ms ease-out 800ms both',
        }}>Your day, prepared.</div>
      </div>

      {/* Loading dots near bottom */}
      <div style={{
        position: 'absolute',
        bottom: 64, left: 0, right: 0,
        display: 'flex', justifyContent: 'center',
        animation: 'fadeInUp 300ms ease-out 1000ms both',
      }}>
        <LoadingDots theme={t} />
      </div>

      {/* Subtle MCP credit */}
      <div style={{
        position: 'absolute', bottom: 28, left: 0, right: 0,
        textAlign: 'center',
        fontFamily: MONO, fontSize: 9.5, fontWeight: 500,
        letterSpacing: '2px', textTransform: 'uppercase',
        color: t.inkMute,
        animation: 'fadeInUp 300ms ease-out 1200ms both',
      }}>Gemini · Notion MCP</div>
    </div>
  );
}

Object.assign(window, { LaunchScreen, BreathingOrb, LoadingDots });
