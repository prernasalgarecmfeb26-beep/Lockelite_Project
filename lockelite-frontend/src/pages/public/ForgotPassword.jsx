import { useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../../services/api'

function AuthCard({ icon, title, sub, children }) {
  return (
    <div className="min-h-screen flex items-center justify-center px-4" style={{ background: 'var(--color-bg)' }}>
      <div className="bg-white rounded-3xl border border-slate-100 shadow-sm p-8 w-full max-w-sm">
        <div className="flex items-center gap-2.5 mb-6">
          <div className="w-8 h-8 rounded-xl bg-primary flex items-center justify-center text-white font-bold text-sm">LE</div>
          <span className="font-bold text-slate-900 tracking-widest text-xs">LOCKELITE</span>
        </div>
        <div className="text-3xl mb-4">{icon}</div>
        <h2 className="text-xl font-bold text-slate-900 mb-1">{title}</h2>
        <p className="text-sm text-slate-500 mb-6">{sub}</p>
        {children}
      </div>
    </div>
  )
}

export function ForgotPassword() {
  const [email, setEmail] = useState('')
  const [sent, setSent]   = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const submit = async (e) => {
    e.preventDefault(); setLoading(true); setError('')
    try { await api.post('/auth/forgot-password', { email }); setSent(true) }
    catch(ex) { setError(ex.response?.data?.message || 'Failed to send reset link.') }
    finally { setLoading(false) }
  }

  if (sent) return (
    <AuthCard icon="📬" title="Check your inbox" sub={`We sent a password reset link to ${email}. It's valid for 1 hour.`}>
      <Link to="/login" className="block text-center text-sm text-primary font-semibold hover:underline">← Back to sign in</Link>
    </AuthCard>
  )

  return (
    <AuthCard icon="🔑" title="Forgot password?" sub="Enter your email and we'll send a reset link.">
      {error && <div className="flex items-start gap-2 bg-red-50 border border-red-200 text-red-700 text-sm rounded-xl px-4 py-3 mb-4"><i className="ti ti-alert-circle flex-shrink-0 mt-0.5"/><span>{error}</span></div>}
      <form onSubmit={submit} className="space-y-4">
        <div>
          <label className="block text-xs font-semibold text-slate-500 mb-1.5 uppercase tracking-wide">Email address</label>
          <input type="email" value={email} onChange={e => setEmail(e.target.value)} required placeholder="you@example.com" className="input-field"/>
        </div>
        <button type="submit" disabled={loading} className="btn-primary w-full flex items-center justify-center gap-2">
          {loading ? <><div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"/><span>Sending...</span></> : <><i className="ti ti-mail"/><span>Send reset link</span></>}
        </button>
      </form>
      <p className="text-center text-xs text-slate-500 mt-5"><Link to="/login" className="text-primary hover:underline">← Back to sign in</Link></p>
    </AuthCard>
  )
}

export function ResetPassword() {
  const [pw, setPw]   = useState('')
  const [cpw, setCpw] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError]     = useState('')
  const [done, setDone]       = useState(false)
  const token = new URLSearchParams(window.location.search).get('token')

  const submit = async (e) => {
    e.preventDefault()
    if (pw !== cpw) { setError('Passwords do not match'); return }
    if (pw.length < 8) { setError('Password must be at least 8 characters'); return }
    setLoading(true); setError('')
    try { await api.post('/auth/reset-password', { token, newPassword: pw }); setDone(true) }
    catch(ex) { setError(ex.response?.data?.message || 'Reset failed. Link may have expired.') }
    finally { setLoading(false) }
  }

  if (done) return (
    <AuthCard icon="✅" title="Password reset!" sub="Your password has been changed. You can now sign in.">
      <Link to="/login" className="btn-primary flex items-center justify-center gap-2 w-full"><i className="ti ti-login"/><span>Sign in</span></Link>
    </AuthCard>
  )

  return (
    <AuthCard icon="🔐" title="Set new password" sub="Choose a strong password with at least 8 characters.">
      {error && <div className="flex items-start gap-2 bg-red-50 border border-red-200 text-red-700 text-sm rounded-xl px-4 py-3 mb-4"><i className="ti ti-alert-circle flex-shrink-0 mt-0.5"/><span>{error}</span></div>}
      <form onSubmit={submit} className="space-y-4">
        {[['New Password', pw, setPw], ['Confirm Password', cpw, setCpw]].map(([label, val, setter]) => (
          <div key={label}>
            <label className="block text-xs font-semibold text-slate-500 mb-1.5 uppercase tracking-wide">{label}</label>
            <input type="password" value={val} onChange={e => setter(e.target.value)} required placeholder="Min 8 characters" className="input-field"/>
          </div>
        ))}
        <button type="submit" disabled={loading} className="btn-primary w-full flex items-center justify-center gap-2">
          {loading ? <><div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"/><span>Saving...</span></> : <><i className="ti ti-shield-check"/><span>Set new password</span></>}
        </button>
      </form>
    </AuthCard>
  )
}

export function ChangePassword() {
  const [f, setF] = useState({ current: '', newP: '', confirm: '' })
  const [loading, setLoading] = useState(false)
  const [error, setError]     = useState('')
  const navigate = typeof window !== 'undefined' ? require('react-router-dom').useNavigate() : () => {}

  const submit = async (e) => {
    e.preventDefault()
    if (f.newP !== f.confirm) { setError('Passwords do not match'); return }
    if (f.newP.length < 8)    { setError('Password must be at least 8 characters'); return }
    setLoading(true); setError('')
    try {
      await api.post('/auth/change-password', { currentPassword: f.current, newPassword: f.newP })
      navigate('/employee/dashboard')
    } catch(ex) { setError(ex.response?.data?.message || 'Failed to change password.') }
    finally { setLoading(false) }
  }

  return (
    <AuthCard icon="🔑" title="Set your password" sub="You're using a temporary password. Please set a permanent one before continuing.">
      {error && <div className="flex items-start gap-2 bg-red-50 border border-red-200 text-red-700 text-sm rounded-xl px-4 py-3 mb-4"><i className="ti ti-alert-circle flex-shrink-0 mt-0.5"/><span>{error}</span></div>}
      <form onSubmit={submit} className="space-y-4">
        {[['current','Temporary Password','Current / temporary password'],['newP','New Password','Min 8 chars, letters + numbers'],['confirm','Confirm New Password','Repeat new password']].map(([k,l,p]) => (
          <div key={k}>
            <label className="block text-xs font-semibold text-slate-500 mb-1.5 uppercase tracking-wide">{l}</label>
            <input type="password" value={f[k]} onChange={e => setF({...f,[k]:e.target.value})} required placeholder={p} className="input-field"/>
          </div>
        ))}
        <button type="submit" disabled={loading} className="btn-primary w-full flex items-center justify-center gap-2">
          {loading ? <><div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"/><span>Saving...</span></> : <><i className="ti ti-shield-check"/><span>Set password & continue</span></>}
        </button>
      </form>
    </AuthCard>
  )
}

export default ForgotPassword
