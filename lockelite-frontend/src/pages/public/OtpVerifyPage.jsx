import { useState, useRef, useEffect } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import api from '../../services/api'
import { useToast } from '../../context/ToastContext'
import { useAuth } from '../../context/AuthContext'

export default function OtpVerifyPage() {
  const [otp, setOtp]       = useState(Array(6).fill(''))
  const [error, setError]   = useState('')
  const [loading, setLoading] = useState(false)
  const [timer, setTimer]   = useState(30)
  const refs = useRef([])
  const navigate  = useNavigate()
  const location  = useLocation()
  const { show }  = useToast()
  const { loadUser } = useAuth()
  const email = location.state?.email || ''

  useEffect(() => {
    if (timer > 0) { const t = setTimeout(() => setTimer(s => s - 1), 1000); return () => clearTimeout(t) }
  }, [timer])

  const handleChange = (i, val) => {
    if (!/^\d?$/.test(val)) return
    const next = [...otp]; next[i] = val; setOtp(next)
    if (val && i < 5) refs.current[i + 1]?.focus()
  }
  const handleKeyDown = (i, e) => {
    if (e.key === 'Backspace' && !otp[i] && i > 0) refs.current[i - 1]?.focus()
  }

  const submit = async (e) => {
    e.preventDefault()
    const code = otp.join('')
    if (code.length < 6) { setError('Please enter the complete 6-digit OTP'); return }
    setLoading(true); setError('')
    try {
      const res = await api.post('/auth/verify-otp', { email, otp: code })
      if (res.data?.token) {
        localStorage.setItem('le_access_token', res.data.token)
        loadUser()
        show('Email verified successfully!', 'success')
        navigate('/select-bank')
      } else {
        show('Email verified! Please sign in.', 'success')
        navigate('/login')
      }
    } catch(ex) { setError(ex.response?.data?.message || 'Invalid OTP. Please try again.') }
    finally { setLoading(false) }
  }

  const resend = async () => {
    try { await api.post('/auth/resend-otp', { email }); setTimer(30); show('OTP resent!', 'info') }
    catch { setError('Failed to resend OTP.') }
  }

  return (
    <div className="min-h-screen flex items-center justify-center px-4" style={{ background: 'var(--color-bg)' }}>
      <div className="bg-white rounded-3xl border border-slate-100 shadow-sm p-8 w-full max-w-sm text-center">
        <div className="w-16 h-16 rounded-2xl bg-orange-50 flex items-center justify-center text-3xl mx-auto mb-5">📧</div>
        <h2 className="text-xl font-bold text-slate-900 mb-1">Verify your email</h2>
        <p className="text-sm text-slate-500 mb-6">6-digit OTP sent to<br/><strong className="text-slate-800">{email}</strong></p>

        {error && (
          <div className="flex items-start gap-2 bg-red-50 border border-red-200 text-red-700 text-sm rounded-xl px-4 py-3 mb-4 text-left">
            <i className="ti ti-alert-circle flex-shrink-0 mt-0.5"/><span>{error}</span>
          </div>
        )}

        <form onSubmit={submit}>
          <div className="flex gap-2 justify-center mb-6">
            {otp.map((d, i) => (
              <input key={i} ref={el => refs.current[i] = el}
                type="text" inputMode="numeric" maxLength={1} value={d}
                onChange={e => handleChange(i, e.target.value)}
                onKeyDown={e => handleKeyDown(i, e)}
                className="w-11 h-13 text-center text-xl font-bold rounded-xl border-2 border-slate-200 bg-slate-50 focus:outline-none focus:border-primary focus:bg-white transition-all"
                style={{ height: '52px' }}
              />
            ))}
          </div>
          <button type="submit" disabled={loading} className="btn-primary w-full flex items-center justify-center gap-2 mb-4">
            {loading ? <><div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"/><span>Verifying...</span></> : <><i className="ti ti-shield-check"/><span>Verify OTP</span></>}
          </button>
        </form>

        <div className="text-sm text-slate-500">
          Didn't receive it?{' '}
          {timer > 0
            ? <span className="text-slate-400">Resend in {timer}s</span>
            : <button onClick={resend} className="text-primary font-semibold hover:underline">Resend OTP</button>
          }
        </div>
      </div>
    </div>
  )
}
