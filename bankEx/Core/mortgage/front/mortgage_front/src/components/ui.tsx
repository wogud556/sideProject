import type { ReactNode } from 'react'
import { cardStyle } from './styles'

export function Card({ title, children, accent }: { title?: string; children: ReactNode; accent?: string }) {
  return (
    <section style={{ ...cardStyle, borderTop: accent ? `4px solid ${accent}` : undefined }}>
      {title && <h3 style={{ margin: '0 0 12px', fontSize: 16 }}>{title}</h3>}
      {children}
    </section>
  )
}

export function Rows({ rows }: { rows: [string, ReactNode][] }) {
  return (
    <table style={{ width: '100%', borderCollapse: 'collapse' }}>
      <tbody>
        {rows.map(([label, value]) => (
          <tr key={label}>
            <td style={{ color: '#5f6368', padding: '5px 0', width: '45%', fontSize: 14 }}>{label}</td>
            <td style={{ fontWeight: 600, textAlign: 'right', fontSize: 14 }}>{value}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

export function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label style={{ display: 'flex', flexDirection: 'column', gap: 4, fontSize: 13, color: '#5f6368', marginTop: 10 }}>
      {label}
      {children}
    </label>
  )
}

export function Badge({ color, children }: { color: string; children: ReactNode }) {
  return (
    <span style={{ display: 'inline-block', padding: '2px 8px', borderRadius: 10, fontSize: 12, fontWeight: 600, color: '#fff', background: color }}>
      {children}
    </span>
  )
}

export function ErrorText({ message }: { message: string }) {
  return message ? <p style={{ color: '#d93025', fontSize: 14 }}>{message}</p> : null
}
