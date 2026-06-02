/* eslint-disable @typescript-eslint/no-explicit-any */
import React, { useEffect, useState, useCallback } from 'react';
import api from '../services/api';
import { Plus, Trash2, Edit2, Save, X, Users, ChevronDown, ChevronUp, RefreshCw, Zap, DollarSign, Clock, CheckSquare, AlertTriangle } from 'lucide-react';

const Payroll: React.FC = () => {
    const [employees, setEmployees] = useState<any[]>([]);
    const [attendanceLogs, setAttendanceLogs] = useState<any[]>([]);
    const [logFilterDate, setLogFilterDate] = useState<string>('');
    const [logFilterName, setLogFilterName] = useState<string>('');
    const [bonusMap, setBonusMap] = useState<Record<number, number>>({});
    const [activeTab, setActiveTab] = useState<'pay' | 'employees' | 'logs'>('pay');
    const [expandedEmpId, setExpandedEmpId] = useState<number | null>(null);
    const [selectedEmployeeForLog, setSelectedEmployeeForLog] = useState<any | null>(null);
    const [editingLog, setEditingLog] = useState<any | null>(null); // log yang sedang diedit
    const [editLogForm, setEditLogForm] = useState({ log_time: '', check_out_time: '', work_date: '' });
    const days = ['Sen', 'Sel', 'Rab', 'Kam', 'Jum', 'Sab', 'Min'];

    const defaultEntry = { employee_id: '', amount: '', notes: '', attendance: [false, false, false, false, false, false, false] };
    const [entries, setEntries] = useState([{ ...defaultEntry }]);
    const [newEmp, setNewEmp] = useState({ name: '', position: '', salary: '', pin: '' });
    const [showAddEmp, setShowAddEmp] = useState(false);
    const [editingId, setEditingId] = useState<number | null>(null);
    const [editData, setEditData] = useState({ Name: '', Position: '', SalaryAmount: 0, FingerprintPIN: '' });

    const fetchEmployees = useCallback(() => {
        api.get('/payroll/employees').then(res => setEmployees(res.data.data || []));
    }, []);

    const fetchAttendanceLogs = useCallback(() => {
        api.get('/payroll/attendance-logs').then(res => setAttendanceLogs(res.data.data || []));
    }, []);

    const fetchBonusLogs = useCallback(() => {
        api.get('/bonus/logs').then(res => {
            const totals = res.data?.totals || {};
            setBonusMap(totals);
        }).catch(() => { });
    }, []);

    useEffect(() => {
        fetchEmployees();
        fetchAttendanceLogs();
        fetchBonusLogs();
    }, [fetchEmployees, fetchAttendanceLogs, fetchBonusLogs]);

    const calculateTotal = (entry: any) => {
        const totalAtt = entry.attendance.filter(Boolean).length;
        return (Number(entry.amount) || 0) * totalAtt;
    };

    const handleEntryChange = (index: number, field: string, value: any) => {
        const newEntries = [...entries];
        (newEntries[index] as any)[field] = value;
        setEntries(newEntries);
    };

    const handleAttendanceChange = (index: number, dayIndex: number) => {
        const newEntries = [...entries];
        newEntries[index].attendance[dayIndex] = !newEntries[index].attendance[dayIndex];
        setEntries(newEntries);
    };

    const paySalary = async (e: React.FormEvent) => {
        e.preventDefault();
        const validEntries = entries.filter(ent => ent.employee_id !== '' && calculateTotal(ent) > 0);
        if (validEntries.length === 0) return alert("Pilih karyawan dan minimal 1 hari kehadiran!");
        try {
            for (const ent of validEntries) {
                await api.post('/payroll/pay', {
                    employee_id: Number(ent.employee_id),
                    amount: calculateTotal(ent),
                    date: new Date().toISOString().split('T')[0],
                    notes: ent.notes
                });
            }
            const empIds = validEntries.map(e => e.employee_id).join(',');
            const token = localStorage.getItem('token');
            const apiUrl = import.meta.env.VITE_API_URL || 'https://bmp.up.railway.app/api';
            const absoluteApiUrl = apiUrl.startsWith('http') ? apiUrl : `${window.location.origin}${apiUrl}`;
            const pdfUrl = `${absoluteApiUrl}/payroll/pdf?token=${token}&ids=${empIds}`;
            if ((window as any).Capacitor) {
                window.open(pdfUrl, '_system');
            } else {
                window.open(pdfUrl, '_blank');
            }
            alert(`Gaji ${validEntries.length} karyawan berhasil dibayar & Slip sedang dibuka!`);
            setEntries([{ ...defaultEntry }]);
        } catch {
            alert("Gagal membayar gaji");
        }
    };

    const addEmployee = async () => {
        if (!newEmp.name || !newEmp.salary) return;
        try {
            await api.post('/payroll/employees', { Name: newEmp.name, Position: newEmp.position, SalaryAmount: Number(newEmp.salary), FingerprintPIN: newEmp.pin });
            setNewEmp({ name: '', position: '', salary: '', pin: '' });
            setShowAddEmp(false);
            fetchEmployees();
        } catch { alert("Gagal menambah karyawan"); }
    };

    const deleteEmployee = async (id: number) => {
        if (!window.confirm("Yakin hapus karyawan?")) return;
        try {
            await api.delete(`/payroll/employees/${id}`);
            fetchEmployees();
        } catch { alert("Gagal menghapus"); }
    };

    const startEdit = (emp: any) => {
        setEditingId(emp.ID);
        setExpandedEmpId(emp.ID);
        setEditData({ Name: emp.Name, Position: emp.Position, SalaryAmount: emp.SalaryAmount, FingerprintPIN: emp.FingerprintPIN || '' });
    };

    const saveEdit = async (id: number) => {
        try {
            await api.put(`/payroll/employees/${id}`, editData);
            setEditingId(null);
            fetchEmployees();
        } catch { alert("Gagal update"); }
    };

    const autoFillFromLogs = () => {
        if (attendanceLogs.length === 0) {
            alert("Belum ada log absensi yang termuat!");
            return;
        }
        const getLateMinutes = (logTime: Date, pin: string) => {
            const hours = logTime.getHours();
            const minutes = logTime.getMinutes();
            const totalMinutes = hours * 60 + minutes;
            if (pin === '1') {
                if (totalMinutes >= 240 && totalMinutes < 720) return Math.max(0, totalMinutes - 480);
                return 0;
            }
            if (totalMinutes >= 180 && totalMinutes < 660) return Math.max(0, totalMinutes - 420);
            if (totalMinutes >= 660 && totalMinutes < 1140) return Math.max(0, totalMinutes - 900);
            if (totalMinutes >= 1140 || totalMinutes < 180) {
                if (totalMinutes >= 1140 && totalMinutes <= 1380) return 0;
                else if (totalMinutes > 1380) return totalMinutes - 1380;
                else return (1440 - 1380) + totalMinutes;
            }
            return 0;
        };
        const now = new Date();
        const dayOfWeek = now.getDay() || 7;
        const startOfWeek = new Date(now);
        startOfWeek.setDate(now.getDate() - dayOfWeek + 1);
        startOfWeek.setHours(0, 0, 0, 0);
        const newEntries: any[] = [];
        employees.forEach(emp => {
            if (!emp.FingerprintPIN) return;
            const empLogs = attendanceLogs.filter(log => {
                if (String(log.EmployeePIN) !== String(emp.FingerprintPIN)) return false;
                return new Date(log.LogTime) >= startOfWeek;
            });
            if (empLogs.length > 0) {
                const attendance = [false, false, false, false, false, false, false];
                let totalLateMinutes = 0;
                const sortedLogs = [...empLogs].sort((a, b) => new Date(a.LogTime).getTime() - new Date(b.LogTime).getTime());
                let i = 0;
                while (i < sortedLogs.length) {
                    let checkInTime = new Date(sortedLogs[i].LogTime);
                    const jsDay = checkInTime.getDay();
                    const ourIndex = jsDay === 0 ? 6 : jsDay - 1;
                    attendance[ourIndex] = true;
                    let late = sortedLogs[i].LateMinutes !== undefined && sortedLogs[i].LateMinutes !== null ? sortedLogs[i].LateMinutes : getLateMinutes(checkInTime, String(emp.FingerprintPIN));
                    totalLateMinutes += late;
                    let nextI = i + 1;
                    while (nextI < sortedLogs.length) {
                        let diffHours = (new Date(sortedLogs[nextI].LogTime).getTime() - checkInTime.getTime()) / (1000 * 60 * 60);
                        if (diffHours < 12) nextI++;
                        else break;
                    }
                    i = nextI;
                }
                let totalGaji = emp.SalaryAmount;
                let notes = 'Auto-fill (Minggu ini)';
                let amountToPay = totalGaji;
                const daysAttended = attendance.filter(Boolean).length;
                if (daysAttended > 0 && totalLateMinutes > 0) {
                    const potongan = totalLateMinutes * 1000;
                    const totalGajiKotor = totalGaji * daysAttended;
                    const totalGajiBersih = totalGajiKotor - potongan;
                    amountToPay = totalGajiBersih / daysAttended;
                    notes = `Telat ${totalLateMinutes} menit, Potong Rp ${potongan.toLocaleString('id-ID')}`;
                }
                newEntries.push({ employee_id: emp.ID, amount: amountToPay > 0 ? amountToPay : 0, notes, attendance });
            }
        });
        if (newEntries.length === 0) alert("Tidak ada data absen karyawan minggu ini!");
        else { setEntries(newEntries); alert(`Berhasil memuat otomatis ${newEntries.length} karyawan!`); }
    };

    const getLocalDateString = (timeStr: string) => {
        // Selalu gunakan WIB (Asia/Jakarta) agar tanggal tidak bergantung timezone browser
        const d = new Date(timeStr);
        const wibDateStr = d.toLocaleDateString('sv-SE', { timeZone: 'Asia/Jakarta' }); // format: YYYY-MM-DD
        return wibDateStr;
    };

    const getLogWorkDate = (log: any) => {
        if (log.WorkDate && !log.WorkDate.startsWith('0001-01-01')) {
            return log.WorkDate.split('T')[0];
        }
        return getLocalDateString(log.LogTime);
    };

    const getShiftLabel = (logTimeStr: string) => {
        const d = new Date(logTimeStr);
        // Dapatkan jam dan menit di Asia/Jakarta (WIB)
        const timeStr = d.toLocaleTimeString('en-US', { hour12: false, hour: '2-digit', minute: '2-digit', timeZone: 'Asia/Jakarta' });
        const [h, m] = timeStr.split(':').map(Number);
        const totalMinutes = h * 60 + m;
        
        // Shift Pagi: 06:01 - 07:30 (361 s.d. 450)
        if (totalMinutes >= 361 && totalMinutes <= 450) return 'Shift Pagi';
        // Shift Sore: 14:01 - 15:30 (841 s.d. 930)
        if (totalMinutes >= 841 && totalMinutes <= 930) return 'Shift Sore';
        // Shift Malam: 22:01 - 23:30 (1321 s.d. 1410)
        if (totalMinutes >= 1321 && totalMinutes <= 1410) return 'Shift Malam';
        
        // Fallback jika di luar window ketat (misal data dummy manual)
        if (h >= 5 && h < 13) return 'Shift Pagi';
        if (h >= 13 && h < 21) return 'Shift Sore';
        return 'Shift Malam';
    };

    const handleAlasanChange = async (date: string, pin: string, value: string) => {
        try {
            await api.post('/payroll/attendance-logs/reason', { date, employee_pin: pin, alasan: value });
            fetchAttendanceLogs();
        } catch { alert("Gagal menyimpan alasan absensi"); }
    };

    const deleteGroupedLogs = async (ids: number[]) => {
        if (!window.confirm("Yakin ingin menghapus semua scan hari ini?")) return;
        try {
            for (const id of ids) await api.delete(`/payroll/attendance-logs/${id}`);
            fetchAttendanceLogs();
        } catch { alert("Gagal menghapus log"); }
    };

    const handleDeleteSingleLog = async (id: number) => {
        if (!window.confirm("Yakin hapus log absensi ini?")) return;
        try {
            await api.delete(`/payroll/attendance-logs/${id}`);
            fetchAttendanceLogs();
        } catch { alert("Gagal menghapus log"); }
    };

    const openEditLog = (log: any, employeePin?: string, date?: string) => {
        if (log && log.ID) {
            const wibOpts = { timeZone: 'Asia/Jakarta', hour: '2-digit' as const, minute: '2-digit' as const, hour12: false };
            const logIn = new Date(log.LogTime).toLocaleTimeString('en-US', wibOpts);
            const logOut = log.CheckOutTime ? new Date(log.CheckOutTime).toLocaleTimeString('en-US', wibOpts) : '';
            const workDate = log.WorkDate && !log.WorkDate.startsWith('0001') 
                ? log.WorkDate.split('T')[0]
                : new Date(log.LogTime).toLocaleDateString('sv-SE', { timeZone: 'Asia/Jakarta' });
            setEditingLog(log);
            setEditLogForm({ log_time: logIn, check_out_time: logOut, work_date: workDate });
        } else {
            setEditingLog({
                ID: 0,
                EmployeePIN: employeePin || '',
                WorkDate: date || '',
            });
            setEditLogForm({ log_time: '', check_out_time: '', work_date: date || '' });
        }
    };

    const saveEditLog = async () => {
        if (!editingLog) return;
        try {
            if (editingLog.ID === 0) {
                await api.post('/payroll/attendance-logs', {
                    employee_pin: editingLog.EmployeePIN,
                    log_time: editLogForm.log_time,
                    check_out_time: editLogForm.check_out_time,
                    work_date: editLogForm.work_date,
                });
            } else {
                await api.put(`/payroll/attendance-logs/${editingLog.ID}`, {
                    log_time: editLogForm.log_time,
                    check_out_time: editLogForm.check_out_time,
                    work_date: editLogForm.work_date,
                });
            }
            setEditingLog(null);
            fetchAttendanceLogs();
            alert('Log absensi berhasil diperbarui!');
        } catch { alert('Gagal memperbarui log'); }
    };

    const getGroupedAttendance = () => {
        const dateSet = new Set<string>();
        for (let i = 0; i < 7; i++) {
            const d = new Date();
            d.setDate(d.getDate() - i);
            dateSet.add(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`);
        }
        attendanceLogs.forEach(log => { if (log.LogTime) dateSet.add(getLogWorkDate(log)); });
        const sortedDates = Array.from(dateSet).sort((a, b) => b.localeCompare(a));
        const rows: any[] = [];
        sortedDates.forEach(date => {
            employees.forEach(emp => {
                if (!emp.FingerprintPIN) return;
                const empLogs = attendanceLogs.filter(log => {
                    if (String(log.EmployeePIN) !== String(emp.FingerprintPIN)) return false;
                    return log.LogTime && getLogWorkDate(log) === date;
                });
                let scan1 = '', scan2 = '', alasan = '';
                let logIds: number[] = [];
                if (empLogs.length > 0) {
                    const sorted = [...empLogs].sort((a, b) => new Date(a.LogTime).getTime() - new Date(b.LogTime).getTime());
                    logIds = sorted.map(l => l.ID);
                    const logWithAlasan = sorted.find(l => l.Alasan);
                    if (logWithAlasan) alasan = logWithAlasan.Alasan;
                    const WIB = { timeZone: 'Asia/Jakarta' };
                    
                    // Cek jika ada log dengan CheckOutTime langsung
                    const logWithCheckout = sorted.find(l => l.CheckOutTime);
                    
                    if (logWithCheckout) {
                        scan1 = new Date(logWithCheckout.LogTime).toLocaleTimeString('id-ID', { hour: '2-digit', minute: '2-digit', ...WIB });
                        scan2 = new Date(logWithCheckout.CheckOutTime).toLocaleTimeString('id-ID', { hour: '2-digit', minute: '2-digit', ...WIB });
                    } else if (sorted.length >= 2) {
                        scan1 = new Date(sorted[0].LogTime).toLocaleTimeString('id-ID', { hour: '2-digit', minute: '2-digit', ...WIB });
                        scan2 = new Date(sorted[sorted.length - 1].LogTime).toLocaleTimeString('id-ID', { hour: '2-digit', minute: '2-digit', ...WIB });
                    } else if (sorted.length === 1) {
                        const singleLog = sorted[0];
                        const logTime = new Date(singleLog.LogTime);
                        const hourWIB = parseInt(logTime.toLocaleString('sv-SE', { hour: '2-digit', ...WIB }).split(' ')[1]?.split(':')[0] ?? '0');
                        const timeStr = logTime.toLocaleTimeString('id-ID', { hour: '2-digit', minute: '2-digit', ...WIB });
                        if (singleLog.VerifyState === 1 || (singleLog.VerifyState !== 0 && hourWIB >= 12)) scan2 = timeStr;
                        else scan1 = timeStr;
                    }
                }
                rows.push({ date, employee: emp, scan1, scan2, alasan, logIds });
            });
        });
        return { rows, sortedDates };
    };

    const getEmployeeLogsGrouped = (employeePIN: string) => {
        const empLogs = attendanceLogs.filter(log => String(log.EmployeePIN) === String(employeePIN));
        const dateSet = new Set<string>();
        for (let i = 0; i < 7; i++) {
            const d = new Date();
            d.setDate(d.getDate() - i);
            dateSet.add(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`);
        }
        empLogs.forEach(log => {
            if (log.LogTime) dateSet.add(getLogWorkDate(log));
        });

        return Array.from(dateSet).map(dateStr => {
            const logs = empLogs.filter(log => log.LogTime && getLogWorkDate(log) === dateStr);
            const sortedLogs = [...logs].sort((a, b) => new Date(a.LogTime).getTime() - new Date(b.LogTime).getTime());
            
            const logWithCheckout = sortedLogs.find(l => l.CheckOutTime);
            
            const checkInLog = sortedLogs.find(l => l.VerifyState === 0) || sortedLogs[0];
            const checkOutLog = sortedLogs.length > 1 ? sortedLogs[sortedLogs.length - 1] : (checkInLog && checkInLog.VerifyState === 1 ? checkInLog : null);
            
            const hasCheckIn = checkInLog && checkInLog.VerifyState === 0;
            const hasCheckOut = checkOutLog && checkOutLog !== checkInLog;
            
            let scan1 = '';
            let scan2 = '';
            
            if (logWithCheckout) {
                scan1 = new Date(logWithCheckout.LogTime).toLocaleTimeString('id-ID', { hour: '2-digit', minute: '2-digit', timeZone: 'Asia/Jakarta' });
                scan2 = new Date(logWithCheckout.CheckOutTime).toLocaleTimeString('id-ID', { hour: '2-digit', minute: '2-digit', timeZone: 'Asia/Jakarta' });
            } else {
                scan1 = hasCheckIn ? new Date(checkInLog.LogTime).toLocaleTimeString('id-ID', { hour: '2-digit', minute: '2-digit', timeZone: 'Asia/Jakarta' }) : '';
                scan2 = hasCheckOut ? new Date(checkOutLog.LogTime).toLocaleTimeString('id-ID', { hour: '2-digit', minute: '2-digit', timeZone: 'Asia/Jakarta' }) : '';
            }
            
            let shift = 'Shift Pagi';
            let late = 0;
            let alasan = '';
            
            if (checkInLog) {
                shift = getShiftLabel(checkInLog.LogTime);
                late = checkInLog.LateMinutes || 0;
                alasan = checkInLog.Alasan || '';
            } else if (checkOutLog) {
                shift = getShiftLabel(checkOutLog.LogTime);
                alasan = checkOutLog.Alasan || '';
            }

            return {
                date: dateStr,
                scan1,
                scan2,
                shift,
                late,
                alasan,
                logs: sortedLogs
            };
        }).sort((a, b) => b.date.localeCompare(a.date));
    };

    const totalGajiSemua = entries.reduce((sum, e) => sum + calculateTotal(e), 0);
    const totalBonusSemua = entries.reduce((sum, e) => sum + ((e as any).bonusAmount || bonusMap[Number(e.employee_id)] || 0), 0);

    // ─── Styles ───────────────────────────────────────────────────
    const tabStyle = (active: boolean): React.CSSProperties => ({
        flex: 1,
        padding: '10px 4px',
        background: active ? '#0d6efd' : 'transparent',
        color: active ? 'white' : '#6c757d',
        border: 'none',
        borderRadius: '8px',
        fontWeight: active ? 700 : 500,
        cursor: 'pointer',
        fontSize: '12px',
        transition: 'all 0.2s',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        gap: '4px',
        whiteSpace: 'nowrap',
    });

    const inputStyle: React.CSSProperties = {
        padding: '10px 12px',
        borderRadius: '8px',
        border: '1px solid #e2e8f0',
        width: '100%',
        boxSizing: 'border-box',
        fontSize: '14px',
        background: 'white',
        outline: 'none',
    };

    const cardStyle: React.CSSProperties = {
        background: 'white',
        borderRadius: '16px',
        padding: '16px',
        boxShadow: '0 1px 8px rgba(0,0,0,0.08)',
        marginBottom: '16px',
        border: '1px solid #f1f5f9',
    };

    return (
        <div style={{ padding: '12px', maxWidth: '700px', margin: '0 auto', fontFamily: 'system-ui, sans-serif', width: '100%', boxSizing: 'border-box', overflowX: 'hidden' }}>

            {/* Header */}
            <div style={{ marginBottom: '16px' }}>
                <h2 style={{ margin: 0, fontSize: '20px', fontWeight: 800, color: '#1e293b' }}>Manajemen Penggajian</h2>
                <p style={{ margin: '4px 0 0', color: '#64748b', fontSize: '13px' }}>{employees.length} karyawan aktif</p>
            </div>

            {/* Tab Navigation */}
            <div style={{ display: 'flex', gap: '4px', background: '#f1f5f9', borderRadius: '12px', padding: '4px', marginBottom: '16px', width: '100%', maxWidth: '100%', boxSizing: 'border-box', overflowX: 'hidden' }}>
                <button style={tabStyle(activeTab === 'pay')} onClick={() => setActiveTab('pay')}>
                    <DollarSign size={14} /> Bayar Gaji
                </button>
                <button style={tabStyle(activeTab === 'employees')} onClick={() => setActiveTab('employees')}>
                    <Users size={14} /> Karyawan
                </button>
                <button style={tabStyle(activeTab === 'logs')} onClick={() => setActiveTab('logs')}>
                    <Clock size={14} /> Log Absen
                </button>
            </div>

            {/* ═══════════════════════════════════════
                TAB 1: BAYAR GAJI
            ═══════════════════════════════════════ */}
            {activeTab === 'pay' && (
                <div>
                    {/* Header aksi */}
                    <div style={{ display: 'flex', gap: '8px', marginBottom: '14px', width: '100%', maxWidth: '100%', boxSizing: 'border-box', overflowX: 'hidden', flexWrap: 'wrap' }}>
                        <button
                            type="button"
                            onClick={autoFillFromLogs}
                            style={{ flex: 1, minWidth: '150px', background: 'linear-gradient(135deg, #f59e0b, #d97706)', color: 'white', border: 'none', borderRadius: '10px', padding: '11px', fontSize: '13px', cursor: 'pointer', fontWeight: 700, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px' }}
                        >
                            <Zap size={15} /> Auto-Isi dari Mesin
                        </button>
                        {entries.length < 10 && (
                            <button
                                type="button"
                                onClick={() => setEntries([...entries, { ...defaultEntry }])}
                                style={{ background: '#e0e7ff', color: '#3730a3', border: 'none', borderRadius: '10px', padding: '11px 14px', fontSize: '13px', cursor: 'pointer', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '4px' }}
                            >
                                <Plus size={15} /> Baris
                            </button>
                        )}
                    </div>

                    <form onSubmit={paySalary}>
                        {entries.map((entry, index) => {
                            const emp = employees.find(e => e.ID === Number(entry.employee_id));
                            const totalGaji = calculateTotal(entry);
                            const bonus = (entry as any).bonusAmount || bonusMap[Number(entry.employee_id)] || 0;
                            const totalDenganBonus = totalGaji + bonus;
                            const daysCount = entry.attendance.filter(Boolean).length;
                            const hasLate = String(entry.notes).includes('Telat') || String(entry.notes).includes('Potong');

                            const hapusDenda = () => {
                                const newEntries = [...entries];
                                newEntries[index] = { ...newEntries[index], amount: emp?.SalaryAmount || newEntries[index].amount, notes: 'Denda dihapus (manual)' };
                                setEntries(newEntries);
                            };

                            return (
                                <div key={index} style={{
                                    ...cardStyle,
                                    border: hasLate ? '1px solid #fbbf24' : '1px solid #e2e8f0',
                                    background: hasLate ? '#fffbf0' : 'white',
                                    position: 'relative'
                                }}>
                                    {/* Badge nomor + tombol hapus baris */}
                                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
                                        <span style={{ background: hasLate ? '#fbbf24' : '#0d6efd', color: 'white', borderRadius: '20px', padding: '2px 10px', fontSize: '12px', fontWeight: 700 }}>
                                            Karyawan {index + 1}
                                        </span>
                                        {entries.length > 1 && (
                                            <button type="button" onClick={() => setEntries(entries.filter((_, i) => i !== index))}
                                                style={{ background: '#fee2e2', color: '#dc2626', border: 'none', borderRadius: '8px', padding: '4px 8px', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '3px', fontSize: '12px' }}>
                                                <X size={14} /> Hapus
                                            </button>
                                        )}
                                    </div>

                                    {/* Pilih karyawan + nominal - 2 kolom */}
                                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px', marginBottom: '12px' }}>
                                        <div>
                                            <label style={{ fontSize: '11px', color: '#64748b', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.5px' }}>Karyawan</label>
                                            <select style={{ ...inputStyle, marginTop: '4px' }} value={entry.employee_id} onChange={e => {
                                                const empFound = employees.find(x => x.ID == Number(e.target.value));
                                                handleEntryChange(index, 'employee_id', e.target.value);
                                                handleEntryChange(index, 'amount', empFound?.SalaryAmount || '');
                                            }} required>
                                                <option value="">-- Pilih --</option>
                                                {employees.map(e => <option key={e.ID} value={e.ID}>{e.Name}</option>)}
                                            </select>
                                            {entry.employee_id && (
                                                <button
                                                    type="button"
                                                    onClick={() => {
                                                        const empFound = employees.find(x => x.ID == Number(entry.employee_id));
                                                        if (empFound) setSelectedEmployeeForLog(empFound);
                                                    }}
                                                    style={{
                                                        marginTop: '6px',
                                                        padding: '5px 8px',
                                                        background: 'linear-gradient(135deg, #f1f5f9, #e2e8f0)',
                                                        border: '1px solid #cbd5e1',
                                                        borderRadius: '6px',
                                                        fontSize: '11px',
                                                        color: '#475569',
                                                        fontWeight: 700,
                                                        cursor: 'pointer',
                                                        display: 'inline-flex',
                                                        alignItems: 'center',
                                                        gap: '4px',
                                                        boxShadow: '0 1px 2px rgba(0,0,0,0.05)',
                                                    }}
                                                >
                                                    <Clock size={12} /> Log Absen
                                                </button>
                                            )}
                                        </div>
                                        <div>
                                            <label style={{ fontSize: '11px', color: '#64748b', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.5px' }}>Gaji / Hari (Rp)</label>
                                            <input type="number" style={{ ...inputStyle, marginTop: '4px' }} value={entry.amount} onChange={e => handleEntryChange(index, 'amount', e.target.value)} required />
                                        </div>
                                    </div>

                                    {/* Kehadiran - Checkbox grid dengan label jelas */}
                                    <div style={{ marginBottom: '12px' }}>
                                        <label style={{ fontSize: '11px', color: '#64748b', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.5px', display: 'block', marginBottom: '8px' }}>
                                            <CheckSquare size={12} style={{ display: 'inline', marginRight: '4px' }} />
                                            Kehadiran ({daysCount} hari)
                                        </label>
                                        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: '4px' }}>
                                            {days.map((day, dayIndex) => {
                                                const checked = entry.attendance[dayIndex];
                                                return (
                                                    <label key={day} style={{
                                                        display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '4px',
                                                        background: checked ? '#dcfce7' : '#f8fafc',
                                                        border: checked ? '1.5px solid #22c55e' : '1.5px solid #e2e8f0',
                                                        borderRadius: '8px', padding: '6px 2px', cursor: 'pointer',
                                                        transition: 'all 0.15s'
                                                    }}>
                                                        <span style={{ fontSize: '11px', fontWeight: 700, color: checked ? '#16a34a' : '#94a3b8' }}>{day}</span>
                                                        <input
                                                            type="checkbox"
                                                            checked={checked}
                                                            onChange={() => handleAttendanceChange(index, dayIndex)}
                                                            style={{ width: '16px', height: '16px', cursor: 'pointer', accentColor: '#22c55e' }}
                                                        />
                                                    </label>
                                                );
                                            })}
                                        </div>
                                    </div>

                                    {/* Catatan */}
                                    <div style={{ marginBottom: '10px' }}>
                                        <label style={{ fontSize: '11px', color: '#64748b', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.5px' }}>Catatan</label>
                                        <div style={{ display: 'flex', gap: '6px', marginTop: '4px' }}>
                                            <input
                                                placeholder="Catatan pembayaran..."
                                                style={{ ...inputStyle, flex: 1, background: hasLate ? '#fff8e1' : 'white', borderColor: hasLate ? '#fbbf24' : '#e2e8f0' }}
                                                value={entry.notes}
                                                onChange={e => handleEntryChange(index, 'notes', e.target.value)}
                                            />
                                            {hasLate && (
                                                <button type="button" onClick={hapusDenda}
                                                    style={{ padding: '8px 10px', background: '#dc2626', color: 'white', border: 'none', borderRadius: '8px', fontSize: '12px', fontWeight: 700, cursor: 'pointer', whiteSpace: 'nowrap' }}>
                                                    ✕ Denda
                                                </button>
                                            )}
                                        </div>
                                    </div>

                                    {/* Info denda */}
                                    {hasLate && emp && (
                                        <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: '6px', background: '#fff3cd', border: '1px solid #fbbf24', borderRadius: '8px', padding: '8px 10px', fontSize: '12px', marginBottom: '10px' }}>
                                            <span style={{ color: '#92400e', fontWeight: 600 }}>⚠️ Asli: Rp {emp.SalaryAmount?.toLocaleString('id-ID')}/hari</span>
                                            <span style={{ color: '#dc2626', fontWeight: 700 }}>→ Rp {Number(entry.amount).toLocaleString('id-ID')}/hari (sudah dipotong)</span>
                                        </div>
                                    )}

                                    {/* Total per karyawan */}
                                    <div style={{ background: '#f0fdf4', border: '1px solid #bbf7d0', borderRadius: '10px', padding: '10px 14px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                        <div style={{ fontSize: '12px', color: '#64748b' }}>
                                            <div>Gaji: Rp {totalGaji.toLocaleString('id-ID')}</div>
                                            {bonus > 0 && <div style={{ color: '#0d6efd' }}>🏭 Bonus: Rp {bonus.toLocaleString('id-ID')}</div>}
                                        </div>
                                        <div style={{ fontWeight: 800, fontSize: '18px', color: '#15803d' }}>
                                            Rp {totalDenganBonus.toLocaleString('id-ID')}
                                        </div>
                                    </div>
                                </div>
                            );
                        })}

                        {/* Summary total semua */}
                        {entries.length > 1 && (
                            <div style={{ background: 'linear-gradient(135deg, #1e293b, #334155)', borderRadius: '14px', padding: '14px 16px', marginBottom: '12px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                <div>
                                    <div style={{ color: '#94a3b8', fontSize: '12px' }}>Total {entries.length} Karyawan</div>
                                    {totalBonusSemua > 0 && <div style={{ color: '#60a5fa', fontSize: '12px' }}>+ Bonus Rp {totalBonusSemua.toLocaleString('id-ID')}</div>}
                                </div>
                                <div style={{ color: '#4ade80', fontWeight: 900, fontSize: '20px' }}>
                                    Rp {(totalGajiSemua + totalBonusSemua).toLocaleString('id-ID')}
                                </div>
                            </div>
                        )}

                        <button type="submit" style={{
                            width: '100%', padding: '15px', background: 'linear-gradient(135deg, #16a34a, #15803d)',
                            color: 'white', border: 'none', borderRadius: '12px', fontWeight: 800, cursor: 'pointer',
                            fontSize: '16px', boxShadow: '0 4px 12px rgba(22,163,74,0.4)', letterSpacing: '0.3px'
                        }}>
                            💰 Bayar & Cetak Slip ({entries.length} Karyawan)
                        </button>
                    </form>
                </div>
            )}

            {/* ═══════════════════════════════════════
                TAB 2: DATA KARYAWAN
            ═══════════════════════════════════════ */}
            {activeTab === 'employees' && (
                <div>
                    {/* Tombol tambah */}
                    <button
                        onClick={() => setShowAddEmp(!showAddEmp)}
                        style={{ width: '100%', padding: '12px', background: showAddEmp ? '#f1f5f9' : 'linear-gradient(135deg, #0d6efd, #2563eb)', color: showAddEmp ? '#475569' : 'white', border: showAddEmp ? '1px dashed #cbd5e1' : 'none', borderRadius: '12px', fontWeight: 700, cursor: 'pointer', fontSize: '14px', marginBottom: '14px', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px' }}
                    >
                        {showAddEmp ? <><X size={16} /> Batal</> : <><Plus size={16} /> Tambah Karyawan Baru</>}
                    </button>

                    {/* Form tambah */}
                    {showAddEmp && (
                        <div style={{ ...cardStyle, border: '2px dashed #93c5fd', background: '#eff6ff' }}>
                            <h4 style={{ margin: '0 0 12px', color: '#1d4ed8', fontSize: '14px' }}>Data Karyawan Baru</h4>
                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px', marginBottom: '8px' }}>
                                <div>
                                    <label style={{ fontSize: '11px', color: '#475569', fontWeight: 600 }}>Nama Lengkap *</label>
                                    <input placeholder="Nama karyawan" style={{ ...inputStyle, marginTop: '4px' }} value={newEmp.name} onChange={e => setNewEmp({ ...newEmp, name: e.target.value })} />
                                </div>
                                <div>
                                    <label style={{ fontSize: '11px', color: '#475569', fontWeight: 600 }}>Jabatan</label>
                                    <input placeholder="Posisi / Jabatan" style={{ ...inputStyle, marginTop: '4px' }} value={newEmp.position} onChange={e => setNewEmp({ ...newEmp, position: e.target.value })} />
                                </div>
                                <div>
                                    <label style={{ fontSize: '11px', color: '#475569', fontWeight: 600 }}>Gaji Harian (Rp) *</label>
                                    <input type="number" placeholder="Misal: 100000" style={{ ...inputStyle, marginTop: '4px' }} value={newEmp.salary} onChange={e => setNewEmp({ ...newEmp, salary: e.target.value })} />
                                </div>
                                <div>
                                    <label style={{ fontSize: '11px', color: '#475569', fontWeight: 600 }}>PIN Mesin</label>
                                    <input placeholder="Angka PIN" style={{ ...inputStyle, marginTop: '4px' }} value={newEmp.pin} onChange={e => setNewEmp({ ...newEmp, pin: e.target.value })} />
                                </div>
                            </div>
                            <button onClick={addEmployee}
                                style={{ width: '100%', padding: '12px', background: '#0d6efd', color: 'white', border: 'none', borderRadius: '10px', fontWeight: 700, cursor: 'pointer', fontSize: '14px' }}>
                                <Plus size={16} style={{ display: 'inline', marginRight: '6px' }} /> Simpan Karyawan
                            </button>
                        </div>
                    )}

                    {/* List karyawan sebagai cards */}
                    {employees.map(emp => (
                        <div key={emp.ID} style={{ ...cardStyle, border: editingId === emp.ID ? '2px solid #0d6efd' : '1px solid #e2e8f0' }}>
                            {editingId === emp.ID ? (
                                /* Mode Edit */
                                <div>
                                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
                                        <span style={{ fontWeight: 700, color: '#0d6efd', fontSize: '14px' }}>✏️ Edit Karyawan</span>
                                        <button onClick={() => setEditingId(null)} style={{ background: 'none', border: 'none', color: '#dc2626', cursor: 'pointer', padding: 0 }}><X size={18} /></button>
                                    </div>
                                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px', marginBottom: '8px' }}>
                                        <div>
                                            <label style={{ fontSize: '11px', color: '#475569', fontWeight: 600 }}>Nama</label>
                                            <input value={editData.Name} onChange={e => setEditData({ ...editData, Name: e.target.value })} style={{ ...inputStyle, marginTop: '4px' }} />
                                        </div>
                                        <div>
                                            <label style={{ fontSize: '11px', color: '#475569', fontWeight: 600 }}>Jabatan</label>
                                            <input value={editData.Position} onChange={e => setEditData({ ...editData, Position: e.target.value })} style={{ ...inputStyle, marginTop: '4px' }} />
                                        </div>
                                        <div>
                                            <label style={{ fontSize: '11px', color: '#475569', fontWeight: 600 }}>Gaji Harian (Rp)</label>
                                            <input type="number" value={editData.SalaryAmount} onChange={e => setEditData({ ...editData, SalaryAmount: Number(e.target.value) })} style={{ ...inputStyle, marginTop: '4px' }} />
                                        </div>
                                        <div>
                                            <label style={{ fontSize: '11px', color: '#475569', fontWeight: 600 }}>PIN Mesin</label>
                                            <input value={editData.FingerprintPIN} onChange={e => setEditData({ ...editData, FingerprintPIN: e.target.value })} style={{ ...inputStyle, marginTop: '4px' }} placeholder="PIN" />
                                        </div>
                                    </div>
                                    <button onClick={() => saveEdit(emp.ID)}
                                        style={{ width: '100%', padding: '11px', background: '#16a34a', color: 'white', border: 'none', borderRadius: '10px', fontWeight: 700, cursor: 'pointer', fontSize: '14px', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px' }}>
                                        <Save size={15} /> Simpan Perubahan
                                    </button>
                                </div>
                            ) : (
                                /* Mode View */
                                <div>
                                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                                        <div style={{ flex: 1 }}>
                                            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
                                                {emp.FingerprintPIN && (
                                                    <span style={{ background: '#e0e7ff', color: '#3730a3', borderRadius: '6px', padding: '2px 8px', fontSize: '12px', fontWeight: 700 }}>
                                                        PIN: {emp.FingerprintPIN}
                                                    </span>
                                                )}
                                                {!emp.IsActive && (
                                                    <span style={{ background: '#fee2e2', color: '#dc2626', borderRadius: '6px', padding: '2px 8px', fontSize: '11px' }}>Nonaktif</span>
                                                )}
                                            </div>
                                            <div style={{ fontWeight: 700, fontSize: '15px', color: '#1e293b' }}>{emp.Name}</div>
                                            <div style={{ color: '#64748b', fontSize: '13px' }}>{emp.Position || 'Tanpa Jabatan'}</div>
                                        </div>
                                        <div style={{ textAlign: 'right' }}>
                                            <div style={{ fontWeight: 800, fontSize: '16px', color: '#15803d' }}>Rp {emp.SalaryAmount?.toLocaleString('id-ID')}</div>
                                            <div style={{ color: '#94a3b8', fontSize: '11px' }}>/hari</div>
                                        </div>
                                    </div>
                                    <div style={{ display: 'flex', gap: '6px', marginTop: '12px', flexWrap: 'wrap' }}>
                                        <button onClick={() => setSelectedEmployeeForLog(emp)}
                                            style={{ flex: 1, minWidth: '80px', padding: '8px 10px', background: '#f1f5f9', color: '#475569', border: '1px solid #cbd5e1', borderRadius: '8px', cursor: 'pointer', fontWeight: 600, fontSize: '12px', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '4px' }}>
                                            <Clock size={14} /> Log Absen
                                        </button>
                                        <button onClick={() => startEdit(emp)}
                                            style={{ flex: 1, minWidth: '70px', padding: '8px 10px', background: '#eff6ff', color: '#2563eb', border: '1px solid #bfdbfe', borderRadius: '8px', cursor: 'pointer', fontWeight: 600, fontSize: '12px', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '4px' }}>
                                            <Edit2 size={14} /> Edit
                                        </button>
                                        <button onClick={() => deleteEmployee(emp.ID)}
                                            style={{ flex: 1, minWidth: '70px', padding: '8px 10px', background: '#fef2f2', color: '#dc2626', border: '1px solid #fecaca', borderRadius: '8px', cursor: 'pointer', fontWeight: 600, fontSize: '12px', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '4px' }}>
                                            <Trash2 size={14} /> Hapus
                                        </button>
                                    </div>
                                </div>
                            )}
                        </div>
                    ))}

                    {employees.length === 0 && (
                        <div style={{ textAlign: 'center', padding: '40px 20px', color: '#94a3b8' }}>
                            <Users size={40} style={{ opacity: 0.3, marginBottom: '10px' }} />
                            <div>Belum ada karyawan. Tambah karyawan pertama!</div>
                        </div>
                    )}
                </div>
            )}

            {/* ═══════════════════════════════════════
                TAB 3: LOG ABSENSI
            ═══════════════════════════════════════ */}
            {activeTab === 'logs' && (
                <div>
                    {/* Filter bar */}
                    <div style={{ ...cardStyle, display: 'flex', gap: '8px', flexWrap: 'wrap', padding: '12px' }}>
                        <select style={{ ...inputStyle, flex: 1, minWidth: '140px' }} value={logFilterDate} onChange={e => setLogFilterDate(e.target.value)}>
                            <option value="">📅 Semua Tanggal</option>
                            {getGroupedAttendance().sortedDates.map(d => (
                                <option key={d} value={d}>{new Date(d + 'T00:00:00').toLocaleDateString('id-ID', { day: '2-digit', month: 'short', year: 'numeric' })}</option>
                            ))}
                        </select>
                        <input placeholder="🔍 Cari nama..." style={{ ...inputStyle, flex: 1, minWidth: '130px' }} value={logFilterName} onChange={e => setLogFilterName(e.target.value)} />
                        <button onClick={fetchAttendanceLogs}
                            style={{ padding: '10px 14px', background: '#f1f5f9', border: '1px solid #e2e8f0', borderRadius: '8px', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '4px', fontSize: '13px', color: '#475569', fontWeight: 600 }}>
                            <RefreshCw size={14} /> Refresh
                        </button>
                    </div>

                    {/* Log cards — tidak ada scroll horizontal! */}
                    {(() => {
                        const filtered = getGroupedAttendance().rows.filter(row => {
                            if (logFilterDate && row.date !== logFilterDate) return false;
                            if (logFilterName && !row.employee.Name.toLowerCase().includes(logFilterName.toLowerCase())) return false;
                            return true;
                        });

                        if (filtered.length === 0) {
                            return (
                                <div style={{ textAlign: 'center', padding: '40px 20px', color: '#94a3b8', background: 'white', borderRadius: '16px', border: '1px solid #f1f5f9' }}>
                                    <Clock size={40} style={{ opacity: 0.3, marginBottom: '10px' }} />
                                    <div>Belum ada log dari mesin absen</div>
                                </div>
                            );
                        }

                        // Group by date
                        const byDate: Record<string, any[]> = {};
                        filtered.forEach(row => {
                            if (!byDate[row.date]) byDate[row.date] = [];
                            byDate[row.date].push(row);
                        });

                        return Object.entries(byDate).map(([date, rows]) => (
                            <div key={date} style={{ marginBottom: '12px' }}>
                                {/* Date header */}
                                <div style={{ background: '#1e293b', color: 'white', borderRadius: '10px 10px 0 0', padding: '8px 14px', fontSize: '13px', fontWeight: 700 }}>
                                    📅 {new Date(date + 'T00:00:00').toLocaleDateString('id-ID', { weekday: 'long', day: '2-digit', month: 'long', year: 'numeric' })}
                                </div>
                                <div style={{ background: 'white', borderRadius: '0 0 10px 10px', border: '1px solid #e2e8f0', borderTop: 'none', overflow: 'hidden' }}>
                                    {rows.map((row, ri) => {
                                        const hadir = !!row.scan1;
                                        return (
                                            <div key={ri} style={{ padding: '12px 14px', borderBottom: ri < rows.length - 1 ? '1px solid #f1f5f9' : 'none' }}>
                                                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                                                    {/* Status dot */}
                                                    <div style={{
                                                        width: '10px', height: '10px', borderRadius: '50%', flexShrink: 0,
                                                        background: hadir ? '#22c55e' : (row.alasan ? '#f59e0b' : '#e2e8f0')
                                                    }} />
                                                    <div style={{ flex: 1, minWidth: 0 }}>
                                                        <div style={{ fontWeight: 700, fontSize: '14px', color: '#1e293b', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                                                            {row.employee.Name}
                                                        </div>
                                                        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px', marginTop: '4px' }}>
                                                            {row.scan1 && (
                                                                <span style={{ background: '#dcfce7', color: '#16a34a', borderRadius: '6px', padding: '2px 8px', fontSize: '12px', fontWeight: 600 }}>
                                                                    Masuk: {row.scan1}
                                                                </span>
                                                            )}
                                                            {row.scan2 && (
                                                                <span style={{ background: '#dbeafe', color: '#1d4ed8', borderRadius: '6px', padding: '2px 8px', fontSize: '12px', fontWeight: 600 }}>
                                                                    Pulang: {row.scan2}
                                                                </span>
                                                            )}
                                                            {!hadir && !row.scan2 && (
                                                                row.alasan ? (
                                                                    <span style={{ background: '#fff3cd', color: '#92400e', borderRadius: '6px', padding: '2px 8px', fontSize: '12px' }}>
                                                                        {row.alasan}
                                                                    </span>
                                                                ) : (
                                                                    <select
                                                                        value={row.alasan}
                                                                        onChange={e => handleAlasanChange(row.date, row.employee.FingerprintPIN, e.target.value)}
                                                                        style={{ padding: '3px 8px', borderRadius: '6px', border: '1px solid #fbbf24', fontSize: '12px', background: '#fffbf0', color: '#92400e', cursor: 'pointer' }}
                                                                    >
                                                                        <option value="">-- Alasan Absen --</option>
                                                                        <option value="Sakit">Sakit</option>
                                                                        <option value="Mesin Error">Mesin Error</option>
                                                                        <option value="Izin">Izin</option>
                                                                        <option value="Tanpa Keterangan">Tanpa Keterangan</option>
                                                                    </select>
                                                                )
                                                            )}
                                                        </div>
                                                    </div>
                                                    {/* Tombol Edit & Hapus */}
                                                    <div style={{ display: 'flex', gap: '4px', flexShrink: 0 }}>
                                                        <button
                                                            onClick={() => {
                                                                const firstLog = row.logIds.length > 0 
                                                                    ? attendanceLogs.find((l: any) => l.ID === row.logIds[0]) 
                                                                    : null;
                                                                openEditLog(firstLog, row.employee.FingerprintPIN, row.date);
                                                            }}
                                                            title="Edit jam masuk/pulang"
                                                            style={{ background: '#eff6ff', border: '1px solid #bfdbfe', color: '#2563eb', cursor: 'pointer', padding: '5px 8px', borderRadius: '6px', display: 'flex', alignItems: 'center', gap: '3px', fontSize: '12px', fontWeight: 600 }}
                                                        >
                                                            <Edit2 size={13} /> Edit
                                                        </button>
                                                        {row.logIds.length > 0 && (
                                                            <button
                                                                onClick={() => deleteGroupedLogs(row.logIds)}
                                                                title="Hapus semua scan hari ini"
                                                                style={{ background: '#fef2f2', border: '1px solid #fecaca', color: '#dc2626', cursor: 'pointer', padding: '5px 8px', borderRadius: '6px', display: 'flex', alignItems: 'center', gap: '3px', fontSize: '12px', fontWeight: 600 }}
                                                            >
                                                                <Trash2 size={13} /> Hapus
                                                            </button>
                                                        )}
                                                    </div>
                                                    {/* Expand toggle */}
                                                    <button
                                                        onClick={() => setExpandedEmpId(expandedEmpId === row.employee.ID ? null : row.employee.ID)}
                                                        style={{ background: 'none', border: 'none', color: '#94a3b8', cursor: 'pointer', padding: '4px', flexShrink: 0 }}
                                                    >
                                                        {expandedEmpId === row.employee.ID ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
                                                    </button>
                                                </div>
                                            </div>
                                        );
                                    })}
                                </div>
                            </div>
                        ));
                    })()}
                </div>
            )}

            {/* Modal Log Absen Pop-up */}
            {selectedEmployeeForLog && (
                <div style={{
                    position: 'fixed',
                    top: 0,
                    left: 0,
                    right: 0,
                    bottom: 0,
                    backgroundColor: 'rgba(15, 23, 42, 0.6)',
                    display: 'flex',
                    justifyContent: 'center',
                    alignItems: 'center',
                    zIndex: 2000,
                    backdropFilter: 'blur(4px)',
                    padding: '16px',
                    boxSizing: 'border-box'
                }}>
                    <div style={{
                        backgroundColor: 'white',
                        borderRadius: '16px',
                        width: '100%',
                        maxWidth: '450px',
                        maxHeight: '85vh',
                        display: 'flex',
                        flexDirection: 'column',
                        boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04)',
                        border: '1px solid #e2e8f0',
                        overflow: 'hidden',
                    }}>
                        {/* Header */}
                        <div style={{
                            padding: '16px',
                            borderBottom: '1px solid #f1f5f9',
                            display: 'flex',
                            justifyContent: 'space-between',
                            alignItems: 'center',
                            background: '#f8fafc'
                        }}>
                            <div>
                                <h3 style={{ margin: 0, fontSize: '15px', fontWeight: 800, color: '#0f172a' }}>
                                    Riwayat Absensi Karyawan
                                </h3>
                                <p style={{ margin: '2px 0 0', fontSize: '12px', color: '#64748b', fontWeight: 500 }}>
                                    {selectedEmployeeForLog.Name} {selectedEmployeeForLog.Position ? `(${selectedEmployeeForLog.Position})` : ''}
                                </p>
                            </div>
                            <button
                                onClick={() => setSelectedEmployeeForLog(null)}
                                style={{
                                    background: '#fee2e2',
                                    color: '#ef4444',
                                    border: 'none',
                                    borderRadius: '50%',
                                    width: '28px',
                                    height: '28px',
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    cursor: 'pointer',
                                    transition: 'all 0.2s'
                                }}
                            >
                                <X size={16} />
                            </button>
                        </div>

                        {/* Body dengan independent vertical scroll */}
                        <div style={{
                            padding: '16px',
                            overflowY: 'auto',
                            maxHeight: '60vh',
                            flex: 1,
                            backgroundColor: '#f8fafc'
                        }}>
                            {(() => {
                                const logsGrouped = getEmployeeLogsGrouped(selectedEmployeeForLog.FingerprintPIN);
                                if (logsGrouped.length === 0) {
                                    return (
                                        <div style={{ textAlign: 'center', padding: '30px 10px', color: '#94a3b8' }}>
                                            <Clock size={32} style={{ opacity: 0.3, marginBottom: '8px' }} />
                                            <div style={{ fontSize: '13px' }}>Tidak ada riwayat absensi ditemukan untuk PIN {selectedEmployeeForLog.FingerprintPIN}</div>
                                        </div>
                                    );
                                }

                                return logsGrouped.map((g, idx) => {
                                    const hasHadir = !!g.scan1;
                                    const isPagi = g.shift.includes('Pagi');
                                    const isSore = g.shift.includes('Sore');
                                    const shiftColor = isPagi ? '#dcfce7' : (isSore ? '#dbeafe' : '#f3e8ff');
                                    const shiftTextColor = isPagi ? '#15803d' : (isSore ? '#1d4ed8' : '#7e22ce');
                                    const lateColor = g.late > 0 ? '#fef2f2' : '#f0fdf4';
                                    const lateTextColor = g.late > 0 ? '#991b1b' : '#16a34a';

                                    return (
                                        <div key={idx} style={{
                                            backgroundColor: 'white',
                                            borderRadius: '12px',
                                            padding: '12px 14px',
                                            marginBottom: '10px',
                                            border: '1px solid #e2e8f0',
                                            boxShadow: '0 1px 3px rgba(0,0,0,0.02)'
                                        }}>
                                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                                                <span style={{ fontSize: '13px', fontWeight: 700, color: '#1e293b' }}>
                                                    {new Date(g.date + 'T00:00:00').toLocaleDateString('id-ID', { weekday: 'long', day: '2-digit', month: 'short', year: 'numeric' })}
                                                </span>
                                                <span style={{
                                                    backgroundColor: shiftColor,
                                                    color: shiftTextColor,
                                                    borderRadius: '6px',
                                                    padding: '2px 8px',
                                                    fontSize: '11px',
                                                    fontWeight: 700
                                                }}>
                                                    {g.shift}
                                                </span>
                                            </div>

                                            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px', alignItems: 'center' }}>
                                                {g.scan1 ? (
                                                    <span style={{ background: '#ecfdf5', color: '#065f46', borderRadius: '6px', padding: '4px 8px', fontSize: '11px', fontWeight: 600 }}>
                                                        Masuk: {g.scan1}
                                                    </span>
                                                ) : (
                                                    <span style={{ background: '#f8fafc', color: '#64748b', borderRadius: '6px', padding: '4px 8px', fontSize: '11px', fontStyle: 'italic' }}>
                                                        Belum Masuk
                                                    </span>
                                                )}
                                                
                                                {g.scan2 ? (
                                                    <span style={{ background: '#eff6ff', color: '#1e40af', borderRadius: '6px', padding: '4px 8px', fontSize: '11px', fontWeight: 600 }}>
                                                        Pulang: {g.scan2}
                                                    </span>
                                                ) : (
                                                    <span style={{ background: '#f8fafc', color: '#64748b', borderRadius: '6px', padding: '4px 8px', fontSize: '11px', fontStyle: 'italic' }}>
                                                        Belum Pulang
                                                    </span>
                                                )}
                                            </div>
                                            {/* Info & Aksi */}
                                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '6px', flexWrap: 'wrap', marginTop: '8px', borderTop: '1px dashed #f1f5f9', paddingTop: '8px' }}>
                                                <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
                                                    {hasHadir && (
                                                        <span style={{
                                                            backgroundColor: lateColor,
                                                            color: lateTextColor,
                                                            borderRadius: '6px',
                                                            padding: '2px 8px',
                                                            fontSize: '11px',
                                                            fontWeight: 600
                                                        }}>
                                                            {g.late > 0 ? `Terlambat: ${g.late} menit` : 'Tepat Waktu'}
                                                        </span>
                                                    )}
                                                    {g.alasan && (
                                                        <span style={{
                                                            backgroundColor: '#fff3cd',
                                                            color: '#92400e',
                                                            borderRadius: '6px',
                                                            padding: '2px 8px',
                                                            fontSize: '11px',
                                                            fontWeight: 600
                                                        }}>
                                                            Alasan: {g.alasan}
                                                        </span>
                                                    )}
                                                </div>
                                                
                                                <div style={{ display: 'flex', gap: '4px' }}>
                                                    <button
                                                        onClick={() => {
                                                            const firstLog = g.logs && g.logs.length > 0 ? g.logs[0] : null;
                                                            openEditLog(firstLog, selectedEmployeeForLog.FingerprintPIN, g.date);
                                                        }}
                                                        title="Edit jam masuk/pulang"
                                                        style={{ background: '#eff6ff', border: '1px solid #bfdbfe', color: '#2563eb', cursor: 'pointer', padding: '4px 8px', borderRadius: '6px', display: 'flex', alignItems: 'center', gap: '3px', fontSize: '11px', fontWeight: 600 }}
                                                    >
                                                        <Edit2 size={12} /> Edit
                                                    </button>
                                                    {g.logs && g.logs.length > 0 && (
                                                        <button
                                                            onClick={() => {
                                                                const logIds = g.logs.map((l: any) => l.ID);
                                                                deleteGroupedLogs(logIds);
                                                            }}
                                                            title="Hapus semua scan hari ini"
                                                            style={{ background: '#fef2f2', border: '1px solid #fecaca', color: '#dc2626', cursor: 'pointer', padding: '4px 8px', borderRadius: '6px', display: 'flex', alignItems: 'center', gap: '3px', fontSize: '11px', fontWeight: 600 }}
                                                        >
                                                            <Trash2 size={12} /> Hapus
                                                        </button>
                                                    )}
                                                </div>
                                            </div>
                                        </div>
                                    );
                                });
                            })()}
                        </div>

                        {/* Footer */}
                        <div style={{
                            padding: '12px 16px',
                            borderTop: '1px solid #f1f5f9',
                            display: 'flex',
                            justifyContent: 'flex-end',
                            backgroundColor: '#f8fafc'
                        }}>
                            <button
                                onClick={() => setSelectedEmployeeForLog(null)}
                                style={{
                                    padding: '8px 16px',
                                    background: 'linear-gradient(135deg, #0f172a, #1e293b)',
                                    color: 'white',
                                    border: 'none',
                                    borderRadius: '8px',
                                    fontSize: '13px',
                                    fontWeight: 700,
                                    cursor: 'pointer'
                                }}
                            >
                                Tutup
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* ═══════════════════════════════════════
                MODAL EDIT LOG ABSENSI
            ═══════════════════════════════════════ */}
            {editingLog && (
                <div style={{
                    position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
                    backgroundColor: 'rgba(15, 23, 42, 0.7)',
                    display: 'flex', justifyContent: 'center', alignItems: 'center',
                    zIndex: 3000, backdropFilter: 'blur(4px)', padding: '16px', boxSizing: 'border-box'
                }}>
                    <div style={{
                        backgroundColor: 'white', borderRadius: '20px', width: '100%', maxWidth: '400px',
                        boxShadow: '0 25px 50px rgba(0,0,0,0.25)', overflow: 'hidden',
                        border: '1px solid #e2e8f0'
                    }}>
                        {/* Header */}
                        <div style={{ background: 'linear-gradient(135deg, #1e293b, #334155)', padding: '16px 20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                            <div>
                                <h3 style={{ margin: 0, fontSize: '15px', fontWeight: 800, color: 'white' }}>✏️ Edit Log Absensi</h3>
                                <p style={{ margin: '3px 0 0', fontSize: '12px', color: '#94a3b8' }}>PIN: {editingLog.EmployeePIN} — Ubah jam masuk &amp; pulang</p>
                            </div>
                            <button onClick={() => setEditingLog(null)} style={{ background: 'rgba(255,255,255,0.1)', border: 'none', color: 'white', borderRadius: '50%', width: '30px', height: '30px', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                                <X size={16} />
                            </button>
                        </div>

                        {/* Body */}
                        <div style={{ padding: '20px' }}>
                            {/* Peringatan */}
                            <div style={{ background: '#fff3cd', border: '1px solid #fbbf24', borderRadius: '10px', padding: '10px 14px', marginBottom: '16px', display: 'flex', alignItems: 'flex-start', gap: '8px' }}>
                                <AlertTriangle size={16} style={{ color: '#d97706', flexShrink: 0, marginTop: '1px' }} />
                                <p style={{ margin: 0, fontSize: '12px', color: '#92400e', lineHeight: '1.5' }}>
                                    Gunakan fitur ini hanya jika mesin error. Perubahan akan langsung tersimpan ke database.
                                </p>
                            </div>

                            {/* Tanggal Kerja */}
                            <div style={{ marginBottom: '14px' }}>
                                <label style={{ fontSize: '11px', color: '#475569', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.5px', display: 'block', marginBottom: '6px' }}>Tanggal Kerja</label>
                                <input
                                    id="edit-log-work-date"
                                    type="date"
                                    value={editLogForm.work_date}
                                    onChange={e => setEditLogForm({ ...editLogForm, work_date: e.target.value })}
                                    style={{ ...inputStyle, fontWeight: 600 }}
                                />
                            </div>

                            {/* Jam Masuk & Jam Pulang */}
                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', marginBottom: '20px' }}>
                                <div>
                                    <label style={{ fontSize: '11px', color: '#16a34a', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.5px', display: 'block', marginBottom: '6px' }}>⏰ Jam Masuk (WIB)</label>
                                    <input
                                        id="edit-log-checkin"
                                        type="time"
                                        value={editLogForm.log_time}
                                        onChange={e => setEditLogForm({ ...editLogForm, log_time: e.target.value })}
                                        style={{ ...inputStyle, borderColor: '#86efac', background: '#f0fdf4', fontWeight: 700, fontSize: '16px' }}
                                    />
                                    <div style={{ fontSize: '10px', color: '#64748b', marginTop: '4px' }}>Kosongkan = tidak diubah</div>
                                </div>
                                <div>
                                    <label style={{ fontSize: '11px', color: '#1d4ed8', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.5px', display: 'block', marginBottom: '6px' }}>⏰ Jam Pulang (WIB)</label>
                                    <input
                                        id="edit-log-checkout"
                                        type="time"
                                        value={editLogForm.check_out_time}
                                        onChange={e => setEditLogForm({ ...editLogForm, check_out_time: e.target.value })}
                                        style={{ ...inputStyle, borderColor: '#93c5fd', background: '#eff6ff', fontWeight: 700, fontSize: '16px' }}
                                    />
                                    <div style={{ fontSize: '10px', color: '#64748b', marginTop: '4px' }}>Kosongkan = hapus jam pulang</div>
                                </div>
                            </div>

                            {/* Tombol aksi */}
                            <div style={{ display: 'flex', gap: '10px' }}>
                                <button
                                    onClick={() => setEditingLog(null)}
                                    style={{ flex: 1, padding: '12px', background: '#f1f5f9', color: '#475569', border: '1px solid #e2e8f0', borderRadius: '10px', fontWeight: 700, cursor: 'pointer', fontSize: '14px' }}
                                >
                                    Batal
                                </button>
                                <button
                                    onClick={saveEditLog}
                                    style={{ flex: 2, padding: '12px', background: 'linear-gradient(135deg, #16a34a, #15803d)', color: 'white', border: 'none', borderRadius: '10px', fontWeight: 800, cursor: 'pointer', fontSize: '14px', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px', boxShadow: '0 4px 12px rgba(22,163,74,0.3)' }}
                                >
                                    <Save size={15} /> Simpan Perubahan
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default Payroll;