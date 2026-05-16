import React from 'react';
import { Link } from 'react-router-dom';
import { Wallet, Contact, Users, Globe } from 'lucide-react';

export default function Dashboard() {
  const menuItems = [
    { path: '/keuangan', label: 'Keuangan & Laporan', icon: <Wallet size={48} />, color: 'text-green-500', bg: 'bg-green-100', border: 'border-green-500' },
    { path: '/pelanggan', label: 'Pelanggan', icon: <Contact size={48} />, color: 'text-blue-500', bg: 'bg-blue-100', border: 'border-blue-500' },
    { path: '/karyawan', label: 'Karyawan', icon: <Users size={48} />, color: 'text-orange-500', bg: 'bg-orange-100', border: 'border-orange-500' },
    { path: '/toko-online', label: 'Toko Online', icon: <Globe size={48} />, color: 'text-purple-500', bg: 'bg-purple-100', border: 'border-purple-500' },
  ];

  return (
    <div className="page-container">
      <div className="header-actions">
        <h1>Dashboard</h1>
        <p className="text-gray-500">Kelola bisnis Anda dari satu tempat terpusat.</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mt-6">
        {menuItems.map((item, index) => (
          <Link to={item.path} key={index} className={`glass-panel p-6 flex flex-col items-center justify-center text-center gap-4 hover:shadow-lg transition-all border-b-4 ${item.border} group`}>
            <div className={`w-20 h-20 ${item.bg} ${item.color} rounded-full flex items-center justify-center group-hover:scale-110 transition-transform`}>
              {item.icon}
            </div>
            <h3 className="font-bold text-lg text-gray-800">{item.label}</h3>
          </Link>
        ))}
      </div>
    </div>
  );
}
