import React, { createContext, useContext } from 'react';

export const AuthContext = createContext(null);
export const DemoContext = createContext({ showDemoBlock: () => {} });

export const useAuth = () => useContext(AuthContext);
export const useDemoBlock = () => useContext(DemoContext);

/**
 * Role hierarchy:
 * OWNER  > ADMIN > KASIR
 *
 * KASIR  : Hanya bisa transaksi & lihat katalog (read-only)
 * ADMIN  : Tambah/edit produk, kelola pelanggan, lihat keuangan & dashboard
 * OWNER  : Semua akses ADMIN + kelola karyawan, hapus data sensitif
 */
export const ROLES = {
  KASIR: 'KASIR',
  ADMIN: 'ADMIN',
  OWNER: 'OWNER',
};

/** Cek apakah user punya role tertentu atau lebih tinggi */
export const hasRole = (userRole, requiredRole) => {
  // Normalisasi: CASHIER = alias lama dari KASIR
  const normalize = (r) => (r === 'CASHIER' ? 'KASIR' : r);
  const hierarchy = [ROLES.KASIR, ROLES.ADMIN, ROLES.OWNER];
  const userIdx = hierarchy.indexOf(normalize(userRole));
  const reqIdx  = hierarchy.indexOf(normalize(requiredRole));
  return userIdx >= reqIdx;
};

/** Shorthand hooks */
export const useIsOwner  = () => { const { user } = useAuth(); return user?.role === ROLES.OWNER; };
export const useIsAdmin  = () => { const { user } = useAuth(); return hasRole(user?.role, ROLES.ADMIN); };
export const useIsKasir  = () => { const { user } = useAuth(); return user?.role === ROLES.KASIR || user?.role === 'CASHIER'; };
export const useIsDemo   = () => { const { user } = useAuth(); return user?.isDemo === true; };

export const DEMO_LIMITS = {
  TRANSACTIONS: 20,
  PRODUCTS: 5,
  EMPLOYEES: 2,
  CUSTOMERS: 5
};
