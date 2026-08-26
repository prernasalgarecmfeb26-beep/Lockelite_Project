import { createContext, useContext, useState, useEffect, useCallback } from 'react'
import { jwtDecode } from 'jwt-decode'
import api from '../services/api'

const AuthContext = createContext(null)

export const AuthProvider = ({ children }) => {
  const [user, setUser]       = useState(null)
  const [loading, setLoading] = useState(true)

  const TOKEN_KEY = 'le_access_token'

  const loadUser = useCallback(() => {
    const params = new URLSearchParams(window.location.search)
    let token = params.get('token')
    if (token) {
      localStorage.setItem(TOKEN_KEY, token)
      window.history.replaceState({}, document.title, window.location.pathname)
    } else {
      token = localStorage.getItem(TOKEN_KEY)
    }

    if (token) {
      try {
        const decoded = jwtDecode(token)
        if (decoded.exp * 1000 > Date.now()) {
          setUser(decoded)
        } else {
          localStorage.removeItem(TOKEN_KEY)
        }
      } catch {
        localStorage.removeItem(TOKEN_KEY)
      }
    }
    setLoading(false)
  }, [])

  useEffect(() => { loadUser() }, [loadUser])

  const login = async (emailOrUsername, password) => {
    const res = await api.post('/auth/login', { emailOrUsername, password })
    const { token, role, passwordChanged, branchId } = res.data
    localStorage.setItem(TOKEN_KEY, token)
    const decoded = jwtDecode(token)
    setUser(decoded)
    return { role, passwordChanged, branchId, decoded }
  }

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY)
    setUser(null)
  }, [])

  const getToken = () => localStorage.getItem(TOKEN_KEY)

  const redirectPath = (role) => ({
    CUSTOMER: '/customer/dashboard',
    EMPLOYEE: '/employee/dashboard',
    ADMIN:    '/admin/dashboard',
  }[role] || '/')

  return (
    <AuthContext.Provider value={{ user, loading, login, logout, getToken, redirectPath, loadUser }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
