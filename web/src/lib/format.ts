import { format, formatDistanceToNow, parseISO } from 'date-fns';

export function money(n?: number | null): string {
  if (n == null) return '—';
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(n);
}

export function dateTime(iso?: string | null): string {
  if (!iso) return '—';
  try {
    return format(parseISO(iso), 'dd MMM yyyy, HH:mm');
  } catch {
    return iso;
  }
}

export function dateOnly(iso?: string | null): string {
  if (!iso) return '—';
  try {
    return format(parseISO(iso), 'dd MMM yyyy');
  } catch {
    return iso;
  }
}

export function fromNow(iso?: string | null): string {
  if (!iso) return '—';
  try {
    return formatDistanceToNow(parseISO(iso), { addSuffix: true });
  } catch {
    return iso;
  }
}

export function titleCase(s?: string | null): string {
  if (!s) return '—';
  return s.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase());
}
