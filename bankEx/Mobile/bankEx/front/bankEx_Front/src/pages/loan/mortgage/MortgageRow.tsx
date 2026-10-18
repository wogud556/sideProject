export default function MortgageRow({ label, value, highlight }: { label: string; value: string; highlight?: boolean }) {
  return (
    <div style={{ display: 'flex', justifyContent: 'space-between', gap: '12px', padding: '10px 0', borderBottom: '1px solid #f0f0f0' }}>
      <span style={{ color: '#888', fontSize: '14px', whiteSpace: 'nowrap' }}>{label}</span>
      <span style={{ fontWeight: highlight ? 'bold' : 'normal', color: highlight ? '#2c7be5' : '#333', textAlign: 'right' }}>{value}</span>
    </div>
  )
}
