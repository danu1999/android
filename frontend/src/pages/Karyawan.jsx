import React, { useState, useEffect } from 'react';
import { Plus, Edit2, Trash2, Shield, Users, Crown, Eye, UserCog, Banknote, CheckCircle, Clock, ChevronLeft, ChevronRight } from 'lucide-react';
import api from '../api';
import { useAuth, useIsOwner, useDemoBlock } from '../AuthContext';

const MONTH_NAMES = ['Januari','Februari','Maret','April','Mei','Juni','Juli','Agustus','September','Oktober','November','Desember'];
const MONTH_SHORT = ['Jan','Feb','Mar','Apr','Mei','Jun','Jul','Agu','Sep','Okt','Nov','Des'];

const ROLE_CFG = {
  OWNER: { grad:'linear-gradient(135deg,#F59E0B,#D97706)', soft:'linear-gradient(135deg,#FEF3C7,#FDE68A)', border:'#FCD34D', text:'#78350F', badge:{bg:'#FEF3C7',color:'#92400E'}, icon:<Crown size={22}/>, label:'👑 Owner' },
  ADMIN: { grad:'linear-gradient(135deg,#6D28D9,#4F46E5)', soft:'linear-gradient(135deg,#EDE9FE,#E0E7FF)', border:'#A78BFA', text:'#3730A3', badge:{bg:'#EDE9FE',color:'#4C1D95'}, icon:<Shield size={22}/>, label:'🛡️ Admin' },
  KASIR: { grad:'linear-gradient(135deg,#3B82F6,#2563EB)', soft:'linear-gradient(135deg,#EFF6FF,#DBEAFE)', border:'#93C5FD', text:'#1E3A8A', badge:{bg:'#DBEAFE',color:'#1E40AF'}, icon:<Users size={22}/>, label:'🧾 Kasir' },
};
const cfg = (role) => ROLE_CFG[role] || ROLE_CFG.KASIR;
const fmt = (n) => Number(n||0).toLocaleString('id-ID');

export default function Karyawan() {
  const { user } = useAuth();
  const isOwner  = useIsOwner();
  const { showDemoBlock, isDemo } = useDemoBlock();

  const [tab, setTab]           = useState('karyawan');
  const [employees, setEmp]     = useState([]);
  const [payHistory, setHist]   = useState([]);
  const [isModalOpen, setModal] = useState(false);
  const [isViewOnly, setView]   = useState(false);
  const [payModal, setPayModal] = useState(null); // {employee}
  const [payNote, setPayNote]   = useState('');
  const [payAmt, setPayAmt]     = useState('');
  const [selMonth, setSelMonth] = useState(new Date().getMonth()+1);
  const [selYear,  setSelYear]  = useState(new Date().getFullYear());
  const [formData, setForm]     = useState({ id:null, name:'', role:'KASIR', pin:'', salary:'' });

  useEffect(()=>{ fetchEmp(); }, []);
  useEffect(()=>{ if(tab==='penggajian') fetchHistory(); }, [tab, selMonth, selYear]);

  const fetchEmp = async()=>{ try{ const r=await api.get('/employees'); setEmp(r.data); }catch(_){} };
  const fetchHistory = async()=>{ try{ const r=await api.get(`/payroll/history?month=${selMonth}&year=${selYear}`); setHist(r.data); }catch(_){} };

  const paidIds = new Set(payHistory.map(h => {
    const m = h.description?.match(/ID:(\d+)/); return m ? Number(m[1]) : null;
  }).filter(Boolean));

  const handleOpenModal = (emp=null, viewOnly=false) => {
    if(employees.length>=10 && !emp){ alert('Batas 10 karyawan.'); return; }
    setForm(emp ? {...emp, salary: emp.salary||''} : {id:null,name:'',role:'KASIR',pin:'',salary:''});
    setView(viewOnly); setModal(true);
  };

  const handleSave = async(e) => {
    e.preventDefault();
    if(isDemo){ showDemoBlock('Mengelola karyawan hanya tersedia di akun berbayar.'); return; }
    if(!isOwner) return;
    try{
      const payload = {...formData, salary: Number(formData.salary||0)};
      if(formData.id) await api.put(`/employees/${formData.id}`, payload);
      else            await api.post('/employees', payload);
      setModal(false); fetchEmp();
    }catch(err){ alert(err.response?.data?.error||'Gagal menyimpan.'); }
  };

  const handleDelete = async(id) => {
    if(isDemo){ showDemoBlock('Menghapus karyawan hanya tersedia di akun berbayar.'); return; }
    if(!isOwner||id===user?.id) return;
    if(window.confirm('Yakin hapus karyawan ini?')){
      try{ await api.delete(`/employees/${id}`); setModal(false); fetchEmp(); }
      catch(err){ alert(err.response?.data?.error||'Gagal menghapus.'); }
    }
  };

  const handlePaySalary = async(emp) => {
    if(isDemo){ showDemoBlock('Fitur penggajian hanya tersedia di akun berbayar.'); return; }
    setPayModal(emp); setPayNote(''); setPayAmt(emp.salary||'');
  };

  const confirmPay = async() => {
    try{
      await api.post('/payroll/pay',{ employeeId:payModal.id, month:selMonth, year:selYear, amount:payAmt, note:payNote });
      setPayModal(null); fetchHistory();
    }catch(err){ alert(err.response?.data?.error||'Gagal bayar gaji.'); }
  };

  const totalGaji   = employees.filter(e=>e.role!=='OWNER').reduce((s,e)=>s+Number(e.salary||0),0);
  const totalDibayar= payHistory.reduce((s,h)=>s+h.amount,0);
  const sudahBayar  = paidIds.size;

  const changeMonth = (dir) => {
    let m=selMonth+dir, y=selYear;
    if(m>12){m=1;y++;} if(m<1){m=12;y--;}
    setSelMonth(m); setSelYear(y);
  };

  const availableRoles = isOwner?['KASIR','ADMIN','OWNER']:['KASIR'];

  return (
    <div className="page-container">
      {/* ── Header ── */}
      <div style={{background:'linear-gradient(135deg,#4F46E5,#7C3AED)',borderRadius:20,padding:'16px 20px',marginBottom:16,display:'flex',justifyContent:'space-between',alignItems:'center',flexWrap:'wrap',gap:10}}>
        <div style={{display:'flex',alignItems:'center',gap:12}}>
          <div style={{width:44,height:44,borderRadius:12,background:'rgba(255,255,255,0.2)',display:'flex',alignItems:'center',justifyContent:'center'}}>
            <UserCog size={24} color="white"/>
          </div>
          <div>
            <h1 style={{margin:0,color:'white',fontSize:'1.2rem',fontWeight:900}}>Manajemen Karyawan</h1>
            <div style={{color:'rgba(255,255,255,0.75)',fontSize:12}}>{employees.length}/10 karyawan · Total gaji Rp {fmt(totalGaji)}/bln</div>
          </div>
        </div>
        {isOwner&&<button onClick={()=>handleOpenModal()} disabled={employees.length>=10} style={{display:'flex',alignItems:'center',gap:8,background:employees.length>=10?'rgba(255,255,255,0.1)':'white',color:employees.length>=10?'rgba(255,255,255,0.4)':'#4F46E5',border:'none',borderRadius:12,padding:'9px 16px',fontWeight:800,fontSize:13,cursor:employees.length>=10?'not-allowed':'pointer'}}><Plus size={15}/> Tambah</button>}
      </div>

      {/* ── Tabs ── */}
      <div style={{display:'flex',gap:8,marginBottom:16}}>
        {[{id:'karyawan',label:'👥 Daftar Karyawan'},...(isOwner?[{id:'penggajian',label:'💰 Penggajian'}]:[])].map(t=>(
          <button key={t.id} onClick={()=>setTab(t.id)} style={{padding:'9px 18px',borderRadius:12,border:'none',fontWeight:700,fontSize:13,cursor:'pointer',background:tab===t.id?'linear-gradient(135deg,#4F46E5,#7C3AED)':'white',color:tab===t.id?'white':'#6B7280',boxShadow:tab===t.id?'0 4px 12px rgba(79,70,229,0.3)':'0 1px 4px rgba(0,0,0,0.08)',transition:'all 0.2s'}}>{t.label}</button>
        ))}
      </div>

      {/* ══ TAB: KARYAWAN ══ */}
      {tab==='karyawan'&&(
        <>
          {/* Summary bar */}
          <div style={{display:'grid',gridTemplateColumns:'repeat(3,1fr)',gap:10,marginBottom:16}}>
            {['OWNER','ADMIN','KASIR'].map(role=>{
              const c=cfg(role); const count=employees.filter(e=>e.role===role).length;
              return(
                <div key={role} style={{background:c.soft,border:`1.5px solid ${c.border}`,borderRadius:14,padding:'10px 14px',display:'flex',alignItems:'center',gap:10}}>
                  <div style={{background:c.grad,borderRadius:10,padding:8,display:'flex',color:'white',flexShrink:0}}>{c.icon}</div>
                  <div><div style={{fontSize:22,fontWeight:900,color:c.text,lineHeight:1}}>{count}</div><div style={{fontSize:10,fontWeight:700,color:c.text,opacity:0.7}}>{role}</div></div>
                </div>
              );
            })}
          </div>

          {/* Cards */}
          <div style={{display:'grid',gridTemplateColumns:'repeat(auto-fill,minmax(230px,1fr))',gap:14}}>
            {employees.length>0?employees.map(emp=>{
              const c=cfg(emp.role); const isSelf=emp.id===user?.id;
              return(
                <div key={emp.id} style={{background:'white',border:`1.5px solid ${c.border}`,borderRadius:18,overflow:'hidden',boxShadow:'0 2px 10px rgba(0,0,0,0.06)',transition:'transform 0.18s,box-shadow 0.18s'}}
                  onMouseEnter={e=>{e.currentTarget.style.transform='translateY(-3px)';e.currentTarget.style.boxShadow='0 8px 22px rgba(0,0,0,0.12)'}}
                  onMouseLeave={e=>{e.currentTarget.style.transform='';e.currentTarget.style.boxShadow='0 2px 10px rgba(0,0,0,0.06)'}}>
                  <div style={{height:5,background:c.grad}}/>
                  <div style={{padding:'14px 16px'}}>
                    <div style={{display:'flex',alignItems:'center',gap:12,marginBottom:10}}>
                      <div style={{width:48,height:48,borderRadius:13,background:c.grad,display:'flex',alignItems:'center',justifyContent:'center',color:'white',flexShrink:0,boxShadow:`0 4px 10px ${c.border}`}}>{c.icon}</div>
                      <div>
                        <div style={{display:'flex',alignItems:'center',gap:6,flexWrap:'wrap'}}>
                          <span style={{fontWeight:800,fontSize:14,color:'#111827'}}>{emp.name}</span>
                          {isSelf&&<span style={{fontSize:10,background:'#EEF2FF',color:'#4F46E5',padding:'1px 7px',borderRadius:99,fontWeight:700}}>Saya</span>}
                        </div>
                        <span style={{display:'inline-block',marginTop:3,fontSize:11,fontWeight:800,background:c.badge.bg,color:c.badge.color,padding:'2px 10px',borderRadius:99}}>{c.label}</span>
                      </div>
                    </div>
                    {isOwner && emp.role !== 'OWNER' && (
                      <div style={{display:'flex',justifyContent:'space-between',background:'#F8FAFC',borderRadius:10,padding:'6px 12px',marginBottom:10,border:'1px solid #F1F5F9'}}>
                        <span style={{fontSize:11,color:'#6B7280',fontWeight:600}}>Gaji Pokok</span>
                        <span style={{fontSize:12,fontWeight:800,color:emp.salary?'#059669':'#9CA3AF'}}>{emp.salary?`Rp ${fmt(emp.salary)}`:'Belum diset'}</span>
                      </div>
                    )}
                    <div style={{display:'flex',gap:8}}>
                      {isOwner?(
                        <>
                          <button onClick={()=>handleOpenModal(emp,false)} style={{flex:1,display:'flex',alignItems:'center',justifyContent:'center',gap:5,background:c.soft,border:`1px solid ${c.border}`,color:c.text,borderRadius:10,padding:'7px',fontWeight:700,fontSize:12,cursor:'pointer'}}><Edit2 size={12}/>Edit</button>
                          {!isSelf&&<button onClick={()=>handleDelete(emp.id)} style={{display:'flex',alignItems:'center',justifyContent:'center',background:'#FEF2F2',border:'1px solid #FECACA',color:'#DC2626',borderRadius:10,padding:'7px 11px',cursor:'pointer'}}><Trash2 size={12}/></button>}
                        </>
                      ):(
                        <button onClick={()=>handleOpenModal(emp,true)} style={{flex:1,display:'flex',alignItems:'center',justifyContent:'center',gap:5,background:'#F9FAFB',border:'1px solid #E5E7EB',color:'#6B7280',borderRadius:10,padding:'7px',fontWeight:600,fontSize:12,cursor:'pointer'}}><Eye size={12}/>Lihat</button>
                      )}
                    </div>
                  </div>
                </div>
              );
            }):(
              <div style={{gridColumn:'1/-1',textAlign:'center',padding:48,color:'#9CA3AF'}}><Users size={36} style={{marginBottom:10,opacity:0.3}}/><div>Belum ada karyawan.</div></div>
            )}
          </div>
        </>
      )}

      {/* ══ TAB: PENGGAJIAN ══ */}
      {tab==='penggajian'&&(
        <>
          {/* Month nav */}
          <div style={{background:'linear-gradient(135deg,#059669,#10B981)',borderRadius:18,padding:'14px 20px',marginBottom:14,display:'flex',justifyContent:'space-between',alignItems:'center',color:'white'}}>
            <button onClick={()=>changeMonth(-1)} style={{background:'rgba(255,255,255,0.2)',border:'none',borderRadius:8,padding:'6px 10px',color:'white',cursor:'pointer'}}><ChevronLeft size={18}/></button>
            <div style={{textAlign:'center'}}>
              <div style={{fontWeight:900,fontSize:18}}>{MONTH_NAMES[selMonth-1]} {selYear}</div>
              <div style={{fontSize:11,opacity:0.8}}>Periode Penggajian</div>
            </div>
            <button onClick={()=>changeMonth(1)} style={{background:'rgba(255,255,255,0.2)',border:'none',borderRadius:8,padding:'6px 10px',color:'white',cursor:'pointer'}}><ChevronRight size={18}/></button>
          </div>

          {/* Summary */}
          <div style={{display:'grid',gridTemplateColumns:'repeat(3,1fr)',gap:10,marginBottom:14}}>
            {[
              {label:'Total Gaji',val:`Rp ${fmt(totalGaji)}`,bg:'linear-gradient(135deg,#EFF6FF,#DBEAFE)',border:'#93C5FD',color:'#1E3A8A',icon:<Banknote size={16}/>},
              {label:'Sudah Dibayar',val:`${sudahBayar}/${employees.length} org`,bg:'linear-gradient(135deg,#ECFDF5,#D1FAE5)',border:'#6EE7B7',color:'#065F46',icon:<CheckCircle size={16}/>},
              {label:'Total Dibayar',val:`Rp ${fmt(totalDibayar)}`,bg:'linear-gradient(135deg,#FEF3C7,#FDE68A)',border:'#FCD34D',color:'#78350F',icon:<Clock size={16}/>},
            ].map((s,i)=>(
              <div key={i} style={{background:s.bg,border:`1.5px solid ${s.border}`,borderRadius:14,padding:'10px 12px'}}>
                <div style={{display:'flex',alignItems:'center',gap:6,marginBottom:4,color:s.color,fontSize:10,fontWeight:700}}>{s.icon}{s.label}</div>
                <div style={{fontWeight:900,color:s.color,fontSize:13,lineHeight:1.2}}>{s.val}</div>
              </div>
            ))}
          </div>

          {/* Payroll list */}
          <div style={{display:'flex',flexDirection:'column',gap:10}}>
            {employees.filter(e=>e.role!=='OWNER').map(emp=>{
              const c=cfg(emp.role); const paid=paidIds.has(emp.id);
              const histItem=payHistory.find(h=>h.description?.includes(`ID:${emp.id}`));
              return(
                <div key={emp.id} style={{background:'white',border:`1.5px solid ${paid?'#6EE7B7':c.border}`,borderRadius:16,padding:'14px 16px',display:'flex',alignItems:'center',gap:14,boxShadow:'0 2px 8px rgba(0,0,0,0.05)',transition:'all 0.2s'}}>
                  <div style={{width:44,height:44,borderRadius:12,background:paid?'linear-gradient(135deg,#10B981,#059669)':c.grad,display:'flex',alignItems:'center',justifyContent:'center',color:'white',flexShrink:0}}>
                    {paid?<CheckCircle size={20}/>:c.icon}
                  </div>
                  <div style={{flex:1,minWidth:0}}>
                    <div style={{display:'flex',alignItems:'center',gap:8,flexWrap:'wrap'}}>
                      <span style={{fontWeight:800,fontSize:14,color:'#111827'}}>{emp.name}</span>
                      <span style={{fontSize:10,fontWeight:700,background:c.badge.bg,color:c.badge.color,padding:'1px 8px',borderRadius:99}}>{c.label}</span>
                      <span style={{fontSize:10,fontWeight:700,background:paid?'#D1FAE5':'#FEF3C7',color:paid?'#065F46':'#92400E',padding:'1px 8px',borderRadius:99}}>
                        {paid?'✓ Dibayar':'⏳ Belum'}
                      </span>
                    </div>
                    <div style={{fontSize:12,color:'#6B7280',marginTop:2}}>
                      Gaji: <b style={{color:'#111827'}}>{emp.salary?`Rp ${fmt(emp.salary)}`:'Belum diset'}</b>
                      {paid&&histItem&&<span style={{marginLeft:8,color:'#059669'}}>· Dibayar Rp {fmt(histItem.amount)}</span>}
                    </div>
                  </div>
                  {isOwner&&!paid&&(
                    <button onClick={()=>handlePaySalary(emp)} style={{display:'flex',alignItems:'center',gap:6,background:'linear-gradient(135deg,#10B981,#059669)',color:'white',border:'none',borderRadius:10,padding:'8px 14px',fontWeight:700,fontSize:12,cursor:'pointer',flexShrink:0,boxShadow:'0 3px 10px rgba(16,185,129,0.35)'}}>
                      <Banknote size={14}/> Bayar
                    </button>
                  )}
                  {paid&&(
                    <div style={{fontSize:11,color:'#059669',fontWeight:700,flexShrink:0}}>✓ Lunas</div>
                  )}
                </div>
              );
            })}
          </div>
        </>
      )}

      {/* ══ Modal Edit/Tambah Karyawan ══ */}
      {isModalOpen&&(
        <div className="modal-overlay">
          <div className="modal-content glass-panel" style={{maxWidth:420,width:'100%'}}>
            <div style={{background:cfg(formData.role).grad,margin:'-2rem -2rem 1.5rem',padding:'16px 22px',borderRadius:'16px 16px 0 0',display:'flex',alignItems:'center',gap:12,color:'white'}}>
              <div style={{background:'rgba(255,255,255,0.2)',borderRadius:10,padding:8,display:'flex'}}>
                {isViewOnly?<Eye size={18}/>:formData.id?<Edit2 size={18}/>:<Plus size={18}/>}
              </div>
              <div>
                <div style={{fontWeight:800,fontSize:15}}>{isViewOnly?'Detail Karyawan':formData.id?'Edit Karyawan':'Tambah Karyawan'}</div>
                {isViewOnly&&<div style={{fontSize:11,opacity:0.8}}>Mode hanya lihat</div>}
              </div>
            </div>
            <form onSubmit={isViewOnly?(e)=>{e.preventDefault();setModal(false);}:handleSave}>
              <div className="form-group">
                <label style={{fontWeight:700}}>Nama Karyawan</label>
                <input type="text" value={formData.name} onChange={e=>setForm({...formData,name:e.target.value})} required disabled={isViewOnly} style={{border:`1.5px solid ${cfg(formData.role).border}`}}/>
              </div>
              <div className="form-group">
                <label style={{fontWeight:700}}>Peran (Role)</label>
                <select value={formData.role} onChange={e=>setForm({...formData,role:e.target.value})} disabled={isViewOnly} style={{border:`1.5px solid ${cfg(formData.role).border}`}}>
                  {availableRoles.map(r=>(
                    <option key={r} value={r}>{r==='KASIR'?'🧾 Kasir':r==='ADMIN'?'🛡️ Admin':'👑 Owner'}</option>
                  ))}
                </select>
              </div>
              {formData.role !== 'OWNER' && (
                <div className="form-group">
                  <label style={{fontWeight:700}}>💰 Gaji Pokok (Rp/bulan)</label>
                  <input type="number" value={formData.salary} onChange={e=>setForm({...formData,salary:e.target.value})} placeholder="0" disabled={isViewOnly} style={{border:`1.5px solid ${cfg(formData.role).border}`}}/>
                </div>
              )}
              {!isViewOnly&&(
                <div className="form-group">
                  <label style={{fontWeight:700}}>PIN {formData.id?'(kosong = tidak diubah)':'(untuk Login)'}</label>
                  <input type="text" maxLength="6" value={formData.pin} onChange={e=>setForm({...formData,pin:e.target.value})} required={!formData.id} placeholder="Contoh: 123456" style={{border:`1.5px solid ${cfg(formData.role).border}`,letterSpacing:4,fontWeight:700}}/>
                </div>
              )}
              <div className="modal-actions" style={{display:'flex',gap:8,alignItems:'center'}}>
                {isOwner&&!isViewOnly&&formData.id&&(
                  <button type="button" onClick={()=>handleDelete(formData.id)} style={{display:'flex',alignItems:'center',gap:5,background:'#FEE2E2',color:'#DC2626',border:'none',borderRadius:10,padding:'9px 13px',fontWeight:700,fontSize:13,cursor:'pointer',marginRight:'auto'}}><Trash2 size={13}/>Hapus</button>
                )}
                <button type="button" className="btn btn-secondary" onClick={()=>setModal(false)}>{isViewOnly?'Tutup':'Batal'}</button>
                {isOwner&&!isViewOnly&&(
                  <button type="submit" style={{display:'flex',alignItems:'center',gap:6,background:cfg(formData.role).grad,color:'white',border:'none',borderRadius:10,padding:'9px 18px',fontWeight:800,fontSize:13,cursor:'pointer'}}>Simpan</button>
                )}
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ══ Modal Bayar Gaji ══ */}
      {payModal&&(
        <div className="modal-overlay">
          <div className="modal-content glass-panel" style={{maxWidth:400,width:'100%'}}>
            <div style={{background:'linear-gradient(135deg,#059669,#10B981)',margin:'-2rem -2rem 1.5rem',padding:'16px 22px',borderRadius:'16px 16px 0 0',display:'flex',alignItems:'center',gap:12,color:'white'}}>
              <div style={{background:'rgba(255,255,255,0.2)',borderRadius:10,padding:8,display:'flex'}}><Banknote size={20}/></div>
              <div>
                <div style={{fontWeight:800,fontSize:15}}>Bayar Gaji</div>
                <div style={{fontSize:11,opacity:0.8}}>{payModal.name} · {MONTH_SHORT[selMonth-1]} {selYear}</div>
              </div>
            </div>
            <div className="form-group">
              <label style={{fontWeight:700}}>💰 Nominal Gaji (Rp)</label>
              <input type="number" value={payAmt} onChange={e=>setPayAmt(e.target.value)} placeholder="Masukkan nominal" style={{border:'1.5px solid #6EE7B7',fontSize:16,fontWeight:700}}/>
              {payModal.salary>0&&<div style={{fontSize:11,color:'#059669',marginTop:4}}>Gaji pokok: Rp {fmt(payModal.salary)}</div>}
            </div>
            <div className="form-group">
              <label style={{fontWeight:700}}>Catatan (opsional)</label>
              <input type="text" value={payNote} onChange={e=>setPayNote(e.target.value)} placeholder="mis. Transfer BCA, Cash..." style={{border:'1.5px solid #6EE7B7'}}/>
            </div>
            <div style={{background:'#ECFDF5',border:'1px solid #6EE7B7',borderRadius:10,padding:'10px 14px',fontSize:12,color:'#065F46',marginBottom:16}}>
              ✅ Pembayaran akan otomatis tercatat sebagai <b>Pengeluaran</b> di Keuangan & Laporan.
            </div>
            <div className="modal-actions" style={{display:'flex',gap:8}}>
              <button type="button" className="btn btn-secondary" onClick={()=>setPayModal(null)}>Batal</button>
              <button type="button" onClick={confirmPay} disabled={!payAmt||Number(payAmt)<=0} style={{flex:1,display:'flex',alignItems:'center',justifyContent:'center',gap:6,background:'linear-gradient(135deg,#059669,#10B981)',color:'white',border:'none',borderRadius:10,padding:'10px',fontWeight:800,fontSize:14,cursor:'pointer',opacity:!payAmt||Number(payAmt)<=0?0.5:1}}>
                <Banknote size={16}/> Konfirmasi Bayar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
