/**
 * Minimal RFC 5545 ICS builder + download trigger.
 * No React dependency — top-level helper.
 */

function icsDate(ms: number): string {
  const d = new Date(ms);
  const pad = (n: number) => String(n).padStart(2, "0");
  return (
    d.getUTCFullYear().toString() +
    pad(d.getUTCMonth() + 1) +
    pad(d.getUTCDate()) +
    "T" +
    pad(d.getUTCHours()) +
    pad(d.getUTCMinutes()) +
    pad(d.getUTCSeconds()) +
    "Z"
  );
}

function foldLine(line: string): string {
  // RFC 5545: fold at 75 octets, continuation lines begin with CRLF + SPACE
  const result: string[] = [];
  let remaining = line;
  while (remaining.length > 75) {
    result.push(remaining.slice(0, 75));
    remaining = " " + remaining.slice(75);
  }
  result.push(remaining);
  return result.join("\r\n");
}

function escape(s: string): string {
  return s
    .replace(/\\/g, "\\\\")
    .replace(/;/g, "\\;")
    .replace(/,/g, "\\,")
    .replace(/\n/g, "\\n")
    .replace(/\r/g, "");
}

export interface IcsEvent {
  uid: string;
  summary: string;
  description: string;
  location: string;
  startsMs: number;
  endsMs: number;
}

export function buildIcs(event: IcsEvent): string {
  const lines = [
    "BEGIN:VCALENDAR",
    "VERSION:2.0",
    "PRODID:-//Mensa Italia//Web//IT",
    "CALSCALE:GREGORIAN",
    "METHOD:PUBLISH",
    "BEGIN:VEVENT",
    `UID:${event.uid}@mensa.it`,
    `DTSTAMP:${icsDate(Date.now())}`,
    `DTSTART:${icsDate(event.startsMs)}`,
    `DTEND:${event.endsMs > event.startsMs ? icsDate(event.endsMs) : icsDate(event.startsMs + 3600_000)}`,
    foldLine(`SUMMARY:${escape(event.summary)}`),
    foldLine(`DESCRIPTION:${escape(event.description)}`),
    foldLine(`LOCATION:${escape(event.location)}`),
    "END:VEVENT",
    "END:VCALENDAR",
  ];
  return lines.join("\r\n");
}

export function downloadIcs(event: IcsEvent): void {
  const content = buildIcs(event);
  const blob = new Blob([content], { type: "text/calendar;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `${event.summary.replace(/[^a-z0-9]/gi, "-").toLowerCase()}.ics`;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}
