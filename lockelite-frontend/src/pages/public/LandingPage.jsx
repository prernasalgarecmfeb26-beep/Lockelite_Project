import { useState, useEffect, useCallback } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'

const SLIDES = [
  {
    badge: 'Bank-Grade Security',
    title: 'Your valuables\ndeserve a vault.',
    sub: 'Modern locker management with real-time visibility, digital KYC, dual-officer approval, and immutable audit trails.',
    bg: 'from-slate-950 via-slate-900 to-slate-800',
    accent: 'var(--color-primary)',
    icon: '🔐',
    stat: ['10,000+ lockers', '50+ banks', '99.9% uptime'],
  },
  {
    badge: 'KYC in 2 Minutes',
    title: 'Digital verification.\nZero paperwork.',
    sub: 'Aadhaar PDF + PAN verified via Setu API in seconds. No queues, no manual documents, seamless onboarding.',
    bg: 'from-blue-950 via-blue-900 to-indigo-900',
    accent: '#60a5fa',
    icon: '✅',
    stat: ['Setu verified', 'Aadhaar + PAN', 'Real-time'],
  },
  {
    badge: 'Smart Branch Finder',
    title: 'Nearest branch,\ninstantly located.',
    sub: 'We detect your location and sort branches by distance with real-time locker availability — no guessing.',
    bg: 'from-emerald-950 via-emerald-900 to-teal-900',
    accent: '#34d399',
    icon: '📍',
    stat: ['GPS sorting', 'Live availability', 'Multi-bank'],
  },
  {
    badge: 'Four-Eyes Policy',
    title: 'Double approval.\nZero compromise.',
    sub: 'Large and extra-large lockers require two different officer sign-offs. No single point of failure — ever.',
    bg: 'from-purple-950 via-purple-900 to-violet-900',
    accent: '#a78bfa',
    icon: '🛡️',
    stat: ['Dual sign-off', 'SHA-256 chain', 'Tamper-proof'],
  },
]

const FEATURES = [
  { icon: 'ti-building-bank', t: 'Vault-style Locker Browser', d: 'Browse lockers floor-by-floor like a real bank vault. Click to inspect size, price, and status.' },
  { icon: 'ti-id-badge',      t: 'Aadhaar & PAN KYC',          d: 'Upload your Aadhaar PDF and enter PAN — verified via Setu API in under 2 minutes.' },
  { icon: 'ti-map-pin',       t: 'Nearest Branch Finder',       d: 'Share your location and we sort branches by distance with live locker availability.' },
  { icon: 'ti-shield-check',  t: 'Four-Eyes Dual Approval',     d: 'Large lockers need two officer sign-offs — no single point of failure.' },
  { icon: 'ti-bell',          t: 'Smart Rent Reminders',        d: 'Automated cron jobs track lease expiry and send email alerts before overdue.' },
  { icon: 'ti-lock',          t: 'SHA-256 Audit Trail',         d: 'Every action is cryptographically chained in an immutable audit log.' },
  { icon: 'ti-robot',         t: 'AI Log Scanner',              d: 'One-click AI analysis flags suspicious access patterns and anomalies.' },
  { icon: 'ti-palette',       t: 'Multi-Bank White-Label',      d: 'Each bank gets its own colors, layout, and branding — one platform, many identities.' },
]

export default function LandingPage() {
  const [cur, setCur]   = useState(0)
  const [fading, setFading] = useState(false)
  const { user, redirectPath } = useAuth()
  const navigate = useNavigate()

  const go = useCallback((idx) => {
    setFading(true)
    setTimeout(() => { setCur(idx); setFading(false) }, 250)
  }, [])

  useEffect(() => {
    const t = setInterval(() => go((cur + 1) % SLIDES.length), 5000)
    return () => clearInterval(t)
  }, [cur, go])

  const s = SLIDES[cur]

  return (
    <div className="min-h-screen bg-white">
      {/* Navbar */}
      <nav className="fixed top-0 inset-x-0 z-50 bg-white/95 backdrop-blur-sm border-b border-slate-100">
        <div className="max-w-6xl mx-auto px-6 h-16 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-xl bg-primary flex items-center justify-center text-white font-bold text-sm">LE</div>
            <span className="font-bold text-slate-900 tracking-widest text-sm">LOCKELITE</span>
          </div>
          <div className="hidden md:flex items-center gap-6 text-sm text-slate-500">
            <a href="#features" className="hover:text-slate-800 transition-colors">Features</a>
            <a href="#how"      className="hover:text-slate-800 transition-colors">How it works</a>
          </div>
          <div className="flex items-center gap-2">
            <Link to="/login" className="px-4 py-2 text-sm text-slate-600 hover:text-slate-900 font-medium transition-colors">Sign in</Link>
            <Link to="/register" className="px-4 py-2 rounded-xl bg-primary text-white text-sm font-semibold hover:bg-orange-600 transition-colors">Sign up</Link>
          </div>
        </div>
      </nav>

      {/* Hero Slider */}
      <section className={`relative pt-16 min-h-[600px] bg-gradient-to-br ${s.bg} overflow-hidden`}>
        {/* Animated background dots */}
        <div className="absolute inset-0 opacity-[0.03] pointer-events-none select-none">
          {[...Array(20)].map((_, i) => (
            <div key={i} className="absolute text-white text-5xl"
              style={{ left: `${(i * 13 + 5) % 100}%`, top: `${(i * 17 + 3) % 100}%` }}>🔒</div>
          ))}
        </div>

        <div className={`relative max-w-6xl mx-auto px-6 py-20 md:py-28 transition-all duration-300 ${fading ? 'opacity-0 translate-y-2' : 'opacity-100 translate-y-0'}`}>
          <div className="max-w-2xl">
            <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full border border-white/20 bg-white/10 text-white/80 text-xs font-medium mb-6">
              <div className="w-1.5 h-1.5 rounded-full animate-pulse" style={{ background: s.accent }}/>
              {s.badge}
            </div>
            <h1 className="text-4xl md:text-5xl lg:text-6xl font-bold text-white leading-tight mb-5 whitespace-pre-line">{s.title}</h1>
            <p className="text-lg text-white/60 mb-8 leading-relaxed max-w-lg">{s.sub}</p>
            <div className="flex flex-wrap gap-3 mb-10">
              <button
                onClick={() => navigate('/register')}
                className="px-6 py-3 rounded-xl font-semibold text-sm text-white transition-all hover:scale-[1.03] active:scale-[0.97]"
                style={{ background: s.accent }}>
                Get started free <i className="ti ti-arrow-right ml-1"/>
              </button>
              <Link to="/login" className="px-6 py-3 rounded-xl text-white/70 border border-white/20 text-sm font-medium hover:bg-white/5 transition-all">
                Sign in
              </Link>
            </div>
            <div className="flex gap-6">
              {s.stat.map(st => (
                <div key={st} className="flex items-center gap-1.5 text-xs text-white/50">
                  <div className="w-1 h-1 rounded-full" style={{ background: s.accent }}/>
                  {st}
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Slide controls */}
        <div className="absolute bottom-8 left-0 right-0 flex justify-center gap-2">
          {SLIDES.map((_, i) => (
            <button key={i} onClick={() => go(i)}
              className={`rounded-full transition-all duration-300 ${i === cur ? 'w-8 h-2' : 'w-2 h-2 bg-white/30 hover:bg-white/50'}`}
              style={i === cur ? { background: s.accent } : {}}/>
          ))}
        </div>
        <button onClick={() => go((cur - 1 + SLIDES.length) % SLIDES.length)}
          className="absolute left-4 top-1/2 -translate-y-1/2 w-10 h-10 rounded-full bg-white/10 border border-white/20 text-white flex items-center justify-center hover:bg-white/20 transition-all text-lg">
          <i className="ti ti-chevron-left"/>
        </button>
        <button onClick={() => go((cur + 1) % SLIDES.length)}
          className="absolute right-4 top-1/2 -translate-y-1/2 w-10 h-10 rounded-full bg-white/10 border border-white/20 text-white flex items-center justify-center hover:bg-white/20 transition-all text-lg">
          <i className="ti ti-chevron-right"/>
        </button>
      </section>

      {/* Stats bar */}
      <div className="bg-slate-950 py-5">
        <div className="max-w-6xl mx-auto px-6 grid grid-cols-4 gap-4 text-center">
          {[['10,000+','Lockers managed'],['50+','Bank partners'],['99.9%','Uptime SLA'],['2 min','Avg KYC time']].map(([v,l])=>(
            <div key={l}><div className="text-xl font-bold text-primary">{v}</div><div className="text-xs text-white/40 mt-0.5">{l}</div></div>
          ))}
        </div>
      </div>

      {/* Features */}
      <section id="features" className="max-w-6xl mx-auto px-6 py-20">
        <div className="text-center mb-12">
          <div className="text-xs font-bold text-primary tracking-widest mb-3">PLATFORM FEATURES</div>
          <h2 className="text-3xl md:text-4xl font-bold text-slate-900 mb-3">Everything a bank locker needs</h2>
          <p className="text-slate-500 max-w-lg mx-auto">Built for banks, branches, and customers who value security above everything.</p>
        </div>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-5">
          {FEATURES.map(f => (
            <div key={f.t} className="bg-white rounded-2xl p-5 border border-slate-100 hover:border-orange-200 hover:-translate-y-1 hover:shadow-lg transition-all duration-200 cursor-default">
              <div className="w-10 h-10 rounded-xl bg-orange-50 flex items-center justify-center mb-3">
                <i className={`ti ${f.icon} text-lg text-primary`}/>
              </div>
              <div className="text-sm font-semibold text-slate-900 mb-1">{f.t}</div>
              <div className="text-xs text-slate-500 leading-relaxed">{f.d}</div>
            </div>
          ))}
        </div>
      </section>

      {/* How it works */}
      <section id="how" className="bg-slate-950 py-20">
        <div className="max-w-6xl mx-auto px-6">
          <div className="text-center mb-12">
            <div className="text-xs font-bold text-primary tracking-widest mb-3">HOW IT WORKS</div>
            <h2 className="text-3xl md:text-4xl font-bold text-white mb-3">From signup to secured locker</h2>
          </div>
          <div className="grid md:grid-cols-4 gap-6">
            {[
              ['01','Register & verify','Create your account and verify email with a 6-digit OTP.'],
              ['02','Choose bank & branch','Pick your bank and nearest branch using GPS-based sorting.'],
              ['03','Complete KYC','Upload Aadhaar PDF and enter PAN — verified via Setu in seconds.'],
              ['04','Book your locker','Browse the vault, pick a locker, and get approved.'],
            ].map(([n,t,d]) => (
              <div key={n} className="text-center">
                <div className="w-12 h-12 rounded-2xl bg-primary text-white font-bold text-lg flex items-center justify-center mx-auto mb-4">{n}</div>
                <div className="text-sm font-semibold text-white mb-1">{t}</div>
                <div className="text-xs text-white/40 leading-relaxed">{d}</div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="max-w-6xl mx-auto px-6 py-20 text-center">
        <h2 className="text-3xl md:text-4xl font-bold text-slate-900 mb-3">Ready to secure what matters?</h2>
        <p className="text-slate-500 mb-8">Join thousands of customers who trust LockElite for their valuables.</p>
        <button onClick={() => user ? navigate(redirectPath(user.role)) : navigate('/register')}
          className="px-8 py-4 rounded-2xl bg-primary text-white font-bold text-sm hover:bg-orange-600 hover:scale-[1.02] transition-all active:scale-[0.98]">
          Create your account — it's free <i className="ti ti-arrow-right ml-2"/>
        </button>
      </section>

      {/* Footer */}
      <footer className="bg-slate-950 py-8">
        <div className="max-w-6xl mx-auto px-6 flex flex-col md:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <div className="w-7 h-7 rounded-lg bg-primary flex items-center justify-center text-white font-bold text-xs">LE</div>
            <span className="text-white font-bold text-sm tracking-widest">LOCKELITE</span>
          </div>
          <p className="text-white/30 text-xs">© 2026 LockElite. All rights reserved.</p>
        </div>
      </footer>
    </div>
  )
}
