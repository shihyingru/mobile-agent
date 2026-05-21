// Themes — Light & Dark, SOHO Waterworks palette
// One screen, two themes (light = day, dark = dusk/night).
// Greeting + status + button label rotate by time of day (5 slots).
//
// Owned by v4. Consumed by home-screen.jsx, launch-screen.jsx,
// Morning Agent.html.

// ─────────────────────────────────────────────────────────────
// Type stacks (CJK fallbacks kept for safety)
// ─────────────────────────────────────────────────────────────
const SERIF      = "'Instrument Serif', 'Noto Serif SC', 'Songti SC', Georgia, serif";
const SERIF_READ = "'Newsreader', 'Noto Serif SC', 'Songti SC', Georgia, serif";
const SANS       = "'Inter', 'Noto Sans SC', 'PingFang SC', system-ui, sans-serif";
const MONO       = "'IBM Plex Mono', 'Sarasa Mono SC', ui-monospace, monospace";

// ─────────────────────────────────────────────────────────────
// Themes — Light (Sand + Navy + Teal) and Dark (Navy + Sand + Aqua)
// ─────────────────────────────────────────────────────────────
const THEMES = {
  light: {
    name: 'Light',
    bg:         '#E5E1DD',                          // SAND
    bgWash:     '#D8D3CC',
    card:       '#F6F3EF',
    cardEdge:   'rgba(8,58,79,0.08)',
    cardShadow: '0 1px 0 rgba(255,255,255,0.6) inset, 0 1px 2px rgba(8,58,79,0.04), 0 12px 28px -16px rgba(8,58,79,0.18)',
    ink:        '#083A4F',                          // NAVY
    inkSoft:    '#3F6273',
    inkMute:    '#8A95A0',
    accent:     '#407E8C',                          // TEAL
    accentDeep: '#1E5765',
    accentSoft: 'rgba(64,126,140,0.14)',
    onAccent:   '#F6F3EF',
    gold:       '#A58D66',
    statusGood: '#5BB58F',
    isLight:    true,
  },
  dark: {
    name: 'Dark',
    bg:         '#04222F',                          // deepened navy
    bgWash:     '#072D3D',
    card:       '#0E3D52',
    cardEdge:   'rgba(192,213,214,0.10)',
    cardShadow: '0 1px 0 rgba(192,213,214,0.05) inset, 0 12px 32px -16px rgba(0,0,0,0.6)',
    ink:        '#E5E1DD',                          // SAND text
    inkSoft:    '#C0D5D6',                          // AQUA
    inkMute:    'rgba(229,225,221,0.45)',
    accent:     '#C0D5D6',                          // AQUA
    accentDeep: '#7DA9AE',
    accentSoft: 'rgba(192,213,214,0.12)',
    onAccent:   '#04222F',
    gold:       '#C8B083',
    statusGood: '#5BB58F',
    isLight:    false,
  },
};

// ─────────────────────────────────────────────────────────────
// Time slots — drive greeting + run button + status copy.
// hourRange is [startInclusive, endExclusive).
// ─────────────────────────────────────────────────────────────
const SLOTS = {
  dawn: {
    hours: [5, 9],
    greeting: 'Good morning, Luna.',
    sub: 'Fresh start. Three things matter today.',
    runLabel: 'Begin the day',
    runIcon: 'sunrise',
    statusLabel: 'Briefing ready',
    statusDot: 'live',
    nextRun: 'Now',
    // Default theme for this slot when the host doesn't specify.
    suggestedTheme: 'light',
  },
  midday: {
    hours: [9, 12],
    greeting: 'Mid-morning, Luna.',
    sub: 'Halfway through your sharpest hours.',
    runLabel: 'Refresh briefing',
    runIcon: 'refresh',
    statusLabel: 'Last run 09:02',
    statusDot: 'idle',
    nextRun: 'on demand',
    suggestedTheme: 'light',
  },
  afternoon: {
    hours: [12, 17],
    greeting: 'Good afternoon, Luna.',
    sub: 'Tidy the loose threads before dusk.',
    runLabel: 'Refresh briefing',
    runIcon: 'refresh',
    statusLabel: 'Resting',
    statusDot: 'idle',
    nextRun: 'tomorrow, 9:00',
    suggestedTheme: 'light',
  },
  evening: {
    hours: [17, 21],
    greeting: 'Good evening, Luna.',
    sub: 'Wind down. Tomorrow is set.',
    runLabel: 'Confirm tomorrow',
    runIcon: 'check',
    statusLabel: 'Set for 09:00',
    statusDot: 'scheduled',
    nextRun: 'tomorrow, 9:00',
    suggestedTheme: 'dark',
  },
  night: {
    hours: [21, 29],                                // wraps past midnight (29 == 5 next day)
    greeting: "It's late, Luna.",
    sub: 'Sleep on it. The agent has tomorrow.',
    runLabel: 'Plan tomorrow',
    runIcon: 'moon',
    statusLabel: 'Quiet hours',
    statusDot: 'sleeping',
    nextRun: 'tomorrow, 9:00',
    suggestedTheme: 'dark',
  },
};

// Briefing quotes — one per slot, slot-aware.
const QUOTES = {
  dawn:      'Pick one win this morning, then build momentum. Start with the SDK doc — your mind is sharpest now, and it unblocks two downstream tasks.',
  midday:    'Two of three still untouched. Close the PR before lunch — Maya is online, and a quick approval lets the next deploy go out on time.',
  afternoon: 'Don\u2019t start anything new. Close the PR review and the design replies — both are quick, and you\u2019ll feel lighter by four.',
  evening:   'Tomorrow is set — three high-priority items, queued. The SDK doc has the longest tail; everything else fits around it.',
  night:     'Put it down. You closed six of nine today, including the auth refactor. The doc can wait one more sleep.',
};

// ─────────────────────────────────────────────────────────────
// Tasks — the today queue. Constant across slots.
// ─────────────────────────────────────────────────────────────
const TASKS = [
  { id: 1, priority: 'high', title: 'Write SDK documentation',
    tip: 'Public API surface first. Skip examples until structure is solid.',
    est: '2h', src: 'Notion' },
  { id: 2, priority: 'high', title: 'Review PR #482 — auth refactor',
    tip: 'Token refresh path needs your eyes. Maya flagged a race condition in retry.',
    est: '45m', src: 'Linear' },
  { id: 3, priority: 'med', title: 'Reply to design feedback',
    tip: 'Three threads in #design-review. Batch them so blockers surface at standup.',
    est: '20m', src: 'Notion' },
];

// ─────────────────────────────────────────────────────────────
// Slot resolution + clock hook
// ─────────────────────────────────────────────────────────────
function resolveSlot(hour) {
  // night wraps; normalize 0–4 as 24–28 for compare.
  const h = hour < 5 ? hour + 24 : hour;
  for (const key of ['dawn', 'midday', 'afternoon', 'evening', 'night']) {
    const [a, b] = SLOTS[key].hours;
    if (h >= a && h < b) return { key, ...SLOTS[key] };
  }
  return { key: 'afternoon', ...SLOTS.afternoon };
}

function withHour(d, h) {
  const x = new Date(d);
  x.setHours(h, h === 0 ? 0 : Math.min(58, d.getMinutes()), 0, 0);
  return x;
}

function formatDate(d) {
  const days = ['Sunday','Monday','Tuesday','Wednesday','Thursday','Friday','Saturday'];
  const months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
  const hh = d.getHours();
  const mm = String(d.getMinutes()).padStart(2, '0');
  const ampm = hh >= 12 ? 'PM' : 'AM';
  const h12 = ((hh + 11) % 12) + 1;
  return `${days[d.getDay()]} · ${months[d.getMonth()]} ${d.getDate()} · ${h12}:${mm} ${ampm}`;
}

// Live-clock hook. Accepts overrideHour (0–23) to pin a slot.
function useTimeSlot(overrideHour) {
  const [now, setNow] = React.useState(() => new Date());
  React.useEffect(() => {
    if (overrideHour != null) return;
    const id = setInterval(() => setNow(new Date()), 30_000);
    return () => clearInterval(id);
  }, [overrideHour]);
  const hour = overrideHour != null ? overrideHour : now.getHours();
  const slot = resolveSlot(hour);
  const dateLine = formatDate(overrideHour != null
    ? withHour(now, overrideHour)
    : now);
  return { slot, dateLine, hour };
}

const ThemeContext = React.createContext(THEMES.light);
const useTheme = () => React.useContext(ThemeContext);

Object.assign(window, {
  THEMES, SLOTS, QUOTES, TASKS,
  SERIF, SERIF_READ, SANS, MONO,
  resolveSlot, useTimeSlot, formatDate, withHour,
  ThemeContext, useTheme,
});
