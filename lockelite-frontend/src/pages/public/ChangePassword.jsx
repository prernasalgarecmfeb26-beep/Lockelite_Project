import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../../services/api'
import { useAuth } from '../../context/AuthContext'

export default function ChangePassword() {
  const [f, setF]   = useState({ current: '', newP: '', confirm: '' })
  const [loading, setLoading] = useState(false)
  const [error, setError]     = useState('')
  const navigate = useNavigate()
  const { loadUser } = useAuth()

  const submit = async (e) => {
    e.preventDefault()
    if (f.newP !== f.confirm) { setError('Passwords do not match'); return }
    if (f.newP.length < 8)    { setError('Password must be at least 8 characters'); return }
    setLoading(true); setError('')
    try {
      const res = await api.post('/auth/change-password', { currentPassword: f.current, newPassword: f.newP })
      if (res.data?.token) {
        localStorage.setItem('le_access_token', res.data.token)
      }
      loadUser()
      navigate('/employee/dashboard')
    } catch(ex) { setError(ex.response?.data?.message || 'Failed to change password.') }
    finally { setLoading(false) }
  }

  return (
    <div className="min-h-screen flex items-center justify-center px-4" style={{ background: 'var(--color-bg)' }}>
      <div className="bg-white rounded-3xl border border-slate-100 shadow-sm p-8 w-full max-w-sm">
        <div className="flex items-center gap-2.5 mb-6">
          <div className="w-8 h-8 rounded-xl bg-primary flex items-center justify-center text-white font-bold text-sm">LE</div>
          <span className="font-bold text-slate-900 tracking-widest text-xs">LOCKELITE</span>
        </div>
        <div className="text-3xl mb-4">🔑</div>
        <h2 className="text-xl font-bold text-slate-900 mb-1">Set your password</h2>
        <p className="text-sm text-slate-500 mb-6">You're using a temporary password. Please set a permanent one before continuing.</p>
        {error && <div className="flex items-start gap-2 bg-red-50 border border-red-200 text-red-700 text-sm rounded-xl px-4 py-3 mb-4"><i className="ti ti-alert-circle flex-shrink-0 mt-0.5"/><span>{error}</span></div>}
        <form onSubmit={submit} className="space-y-4">
          {[['current','Temporary Password','Enter your temp password'],['newP','New Password','Min 8 chars'],['confirm','Confirm Password','Repeat new password']].map(([k,l,p]) => (
            <div key={k}>
              <label className="block text-xs font-semibold text-slate-500 mb-1.5 uppercase tracking-wide">{l}</label>
              <input type="password" value={f[k]} onChange={e => setF({...f,[k]:e.target.value})} required placeholder={p} className="input-field"/>
            </div>
          ))}
          <button type="submit" disabled={loading} className="btn-primary w-full flex items-center justify-center gap-2">
            {loading ? <><div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"/><span>Saving...</span></> : <><i className="ti ti-shield-check"/><span>Set password & continue</span></>}
          </button>
        </form>
      </div>
    </div>
  )
}
