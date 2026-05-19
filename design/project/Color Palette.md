# Mobile Agent App — Color Palette

Two themes built on the **SOHO Waterworks** swatch card. The light theme reads as Sand paper with Navy ink and a Teal accent; the dark theme inverts to a deep Navy ground with Sand text and an Aqua accent. Gold appears as a secondary "medium-priority" tone in both themes.

---

## Source swatches

| Name | Hex       | RGB              | Role                                  |
|------|-----------|------------------|---------------------------------------|
| Navy | `#083A4F` | 8 · 58 · 79      | Primary ink (light) / surface (dark)  |
| Gold | `#A58D66` | 165 · 141 · 102  | Secondary accent (medium priority)    |
| Aqua | `#C0D5D6` | 192 · 213 · 214  | Accent (dark)                         |
| Teal | `#407E8C` | 64 · 126 · 140   | Accent (light)                        |
| Sand | `#E5E1DD` | 229 · 225 · 221  | Page (light) / text (dark)            |

---

## Light theme — Sand · Navy · Teal

| Token            | Hex                            | Usage                                              |
|------------------|--------------------------------|----------------------------------------------------|
| `bg`             | `#E5E1DD`                      | Page background (Sand)                             |
| `bgWash`         | `#D8D3CC`                      | Top radial wash for depth                          |
| `card`           | `#F6F3EF`                      | Card / input surface                               |
| `cardEdge`       | `rgba(8, 58, 79, 0.08)`        | Card border, hairline dividers                     |
| `ink`            | `#083A4F`                      | Primary text (Navy)                                |
| `inkSoft`        | `#3F6273`                      | Secondary text, italic asides                      |
| `inkMute`        | `#8A95A0`                      | Meta, placeholders, mono labels                    |
| `accent`         | `#407E8C`                      | Primary accent (Teal) — buttons, dots, links       |
| `accentDeep`     | `#1E5765`                      | Accent hover / pressed                             |
| `accentSoft`     | `rgba(64, 126, 140, 0.14)`     | Accent tint — chip backgrounds, hover wash         |
| `onAccent`       | `#F6F3EF`                      | Text on accent surfaces                            |
| `gold`           | `#A58D66`                      | Medium-priority chips                              |
| `cardShadow`     | `0 1px 0 rgba(255,255,255,.6) inset, 0 1px 2px rgba(8,58,79,.04), 0 12px 28px -16px rgba(8,58,79,.18)` | Card elevation |

---

## Dark theme — Navy · Sand · Aqua

| Token            | Hex                              | Usage                                              |
|------------------|----------------------------------|----------------------------------------------------|
| `bg`             | `#04222F`                        | Page background (deepened Navy)                    |
| `bgWash`         | `#072D3D`                        | Top radial wash                                    |
| `card`           | `#0E3D52`                        | Card / input surface                               |
| `cardEdge`       | `rgba(192, 213, 214, 0.10)`      | Card border, hairline dividers                     |
| `ink`            | `#E5E1DD`                        | Primary text (Sand)                                |
| `inkSoft`        | `#C0D5D6`                        | Secondary text (Aqua tint)                         |
| `inkMute`        | `rgba(229, 225, 221, 0.45)`      | Meta, placeholders, mono labels                    |
| `accent`         | `#C0D5D6`                        | Primary accent (Aqua)                              |
| `accentDeep`     | `#7DA9AE`                        | Accent hover / pressed                             |
| `accentSoft`     | `rgba(192, 213, 214, 0.12)`      | Accent tint — chip backgrounds, hover wash         |
| `onAccent`       | `#04222F`                        | Text on accent surfaces                            |
| `gold`           | `#C8B083`                        | Medium-priority chips (warmed for dark ground)     |
| `cardShadow`     | `0 1px 0 rgba(192,213,214,.05) inset, 0 12px 32px -16px rgba(0,0,0,.6)` | Card elevation |

---

## Semantic colors (shared)

| Token            | Hex       | Usage                              |
|------------------|-----------|------------------------------------|
| `priority.high`  | accent    | High-priority chip (uses theme accent) |
| `priority.med`   | `gold`    | Medium-priority chip               |
| `priority.low`   | `inkMute` | Low-priority chip                  |
| `status.live`    | `#5BB58F` | Active / live status dot           |
| `status.saved`   | `#5BB58F` | "Saved" confirmation               |
| `status.scheduled` | accent  | Scheduled / set status             |
| `status.idle`    | `gold`    | Idle / resting status              |
| `status.sleeping`| `inkMute` | Quiet hours / sleeping             |

---

## Source-logo brand colors (used as-is in both themes)

| Source  | Hex       |
|---------|-----------|
| Notion  | `#FFFFFF` on `#231C12` text |
| Linear  | `#5E6AD2` |
| GitHub  | `#1F2328` |
| Gemini  | `#4285F4` |
| Claude  | `#D97757` |
| OpenAI  | `#0A0A0A` |
| Mistral | `#FA520F` |
| DeepSeek| `#4D6BFE` |
| Llama   | `#0866FF` |
