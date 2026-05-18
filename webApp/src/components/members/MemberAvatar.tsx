/**
 * MemberAvatar — 36×36 (or configurable) circle.
 * If avatarUrl is set, renders an img. Otherwise renders initials on a single
 * brand gradient (mensa-blue → mensa-cyan) shared by every member, so the
 * registry reads as a consistent grid instead of a colour patchwork.
 */
import { useState } from "react";
import type { MensaWebMember } from "../../lib/mensa";

const BRAND_GRADIENT =
  "linear-gradient(135deg, oklch(38% 0.16 263), oklch(78% 0.13 222))";

interface Props {
  member: Pick<MensaWebMember, "avatarUrl" | "firstName" | "lastName" | "name">;
  size?: number;
}

// URLs that the legacy backend serves as "no real photo" placeholders.
// These are old WinXP-era default user icons; we'd rather render initials.
const PLACEHOLDER_AVATAR = /(no[_-]?avatar|no[_-]?image|default[_-]?user|placeholder|silhouette|generic[_-]?user|user[_-]?default|anonymous|guest[_-]?icon)/i;

function isPlaceholderUrl(url: string): boolean {
  if (!url) return true;
  return PLACEHOLDER_AVATAR.test(url);
}

export function MemberAvatar({ member, size = 36 }: Props) {
  const { avatarUrl, firstName, lastName, name } = member;
  const [imgFailed, setImgFailed] = useState(false);

  const showImage = !!avatarUrl && !isPlaceholderUrl(avatarUrl) && !imgFailed;

  if (showImage) {
    return (
      <img
        src={avatarUrl}
        alt=""
        loading="lazy"
        onError={() => setImgFailed(true)}
        style={{
          width: size,
          height: size,
          borderRadius: "50%",
          objectFit: "cover",
          flexShrink: 0,
          display: "block",
        }}
      />
    );
  }

  const initials =
    ((firstName?.[0] ?? name[0] ?? "?").toUpperCase() +
     (lastName?.[0] ?? "").toUpperCase()) || "?";

  const fontSize = Math.round(size * 0.36);

  return (
    <span
      aria-hidden="true"
      style={{
        display: "inline-flex",
        alignItems: "center",
        justifyContent: "center",
        width: size,
        height: size,
        borderRadius: "50%",
        background: BRAND_GRADIENT,
        color: "oklch(98% 0.005 263)",
        fontWeight: 700,
        fontSize: fontSize,
        letterSpacing: "-0.01em",
        flexShrink: 0,
        userSelect: "none",
        fontFamily: "var(--font-display)",
      }}
    >
      {initials}
    </span>
  );
}
