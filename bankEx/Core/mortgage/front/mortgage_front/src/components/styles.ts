import type { CSSProperties } from 'react'

export const inputStyle: CSSProperties = { padding: '8px 10px', border: '1px solid #dadce0', borderRadius: 6, fontSize: 14 }

export function buttonStyle(bg: string, disabled = false): CSSProperties {
  return {
    padding: '9px 20px', background: bg, color: '#fff', border: 'none', borderRadius: 8,
    cursor: disabled ? 'not-allowed' : 'pointer', fontSize: 14, opacity: disabled ? 0.5 : 1, marginTop: 12,
  }
}

export const cardStyle: CSSProperties = {
  background: '#fff', borderRadius: 12, padding: '18px 22px', boxShadow: '0 1px 6px rgba(0,0,0,0.08)', marginTop: 16,
}

export const tableStyle: CSSProperties = { width: '100%', borderCollapse: 'collapse', fontSize: 14 }
export const thStyle: CSSProperties = { textAlign: 'left', padding: '8px 6px', borderBottom: '2px solid #e8eaed', color: '#5f6368', fontWeight: 600 }
export const tdStyle: CSSProperties = { padding: '8px 6px', borderBottom: '1px solid #f1f3f4' }
