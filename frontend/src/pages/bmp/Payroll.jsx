const _jsxFileName = "C:\\Users\\danus\\Documents\\antigravity\\invoice-bmp-go\\golang-frontend\\src\\pages\\Payroll.tsx"; function _nullishCoalesce(lhs, rhsFn) { if (lhs != null) { return lhs; } else { return rhsFn(); } } function _optionalChain(ops) { let lastAccessLHS = undefined; let value = ops[0]; let i = 1; while (i < ops.length) { const op = ops[i]; const fn = ops[i + 1]; i += 2; if ((op === 'optionalAccess' || op === 'optionalCall') && value == null) { return undefined; } if (op === 'access' || op === 'optionalAccess') { lastAccessLHS = value; value = fn(value); } else if (op === 'call' || op === 'optionalCall') { value = fn((...args) => value.call(lastAccessLHS, ...args)); lastAccessLHS = undefined; } } return value; }/* eslint-disable @typescript-eslint/no-explicit-any */
import React, { useEffect, useState, useCallback } from 'react';
import api, { API_URL } from '../../services/apiBmp';
import { Plus, Trash2, Edit2, Save, X, Users, ChevronDown, ChevronUp, RefreshCw, Zap, DollarSign, Clock, CheckSquare, AlertTriangle } from 'lucide-react';

const Payroll = () => {
    const [employees, setEmployees] = useState([]);
    const [attendanceLogs, setAttendanceLogs] = useState([]);
    const [logFilterDate, setLogFilterDate] = useState('');
    const [logFilterName, setLogFilterName] = useState('');
    const [bonusMap, setBonusMap] = useState({});
    const [activeTab, setActiveTab] = useState('pay');
    const [expandedEmpId, setExpandedEmpId] = useState(null);
    const [selectedEmployeeForLog, setSelectedEmployeeForLog] = useState(null);
    const [editingLog, setEditingLog] = useState(null); // log yang sedang diedit
    const [editLogForm, setEditLogForm] = useState({ log_time: '', check_out_time: '', work_date: '' });
    const days = ['Sen', 'Sel', 'Rab', 'Kam', 'Jum', 'Sab', 'Min'];

    const defaultEntry = { employee_id: '', amount: '', notes: '', attendance: [false, false, false, false, false, false, false] };
    const [entries, setEntries] = useState([{ ...defaultEntry }]);
    const [newEmp, setNewEmp] = useState({ name: '', position: '', salary: '', pin: '' });
    const [showAddEmp, setShowAddEmp] = useState(false);
    const [editingId, setEditingId] = useState(null);
    const [editData, setEditData] = useState({ Name: '', Position: '', SalaryAmount: 0, FingerprintPIN: '' });

    const fetchEmployees = useCallback(() => {
        api.get('/payroll/employees').then(res => setEmployees(res.data.data || []));
    }, []);

    const fetchAttendanceLogs = useCallback(() => {
        api.get('/payroll/attendance-logs').then(res => setAttendanceLogs(res.data.data || []));
    }, []);

    const fetchBonusLogs = useCallback(() => {
        api.get('/bonus/logs').then(res => {
            const totals = _optionalChain([res, 'access', _2 => _2.data, 'optionalAccess', _3 => _3.totals]) || {};
            setBonusMap(totals);
        }).catch(() => { });
    }, []);

    useEffect(() => {
        fetchEmployees();
        fetchAttendanceLogs();
        fetchBonusLogs();
    }, [fetchEmployees, fetchAttendanceLogs, fetchBonusLogs]);

    const calculateTotal = (entry) => {
        const totalAtt = entry.attendance.filter(Boolean).length;
        return (Number(entry.amount) || 0) * totalAtt;
    };

    const handleEntryChange = (index, field, value) => {
        const newEntries = [...entries];
        (newEntries[index] )[field] = value;
        setEntries(newEntries);
    };

    const handleAttendanceChange = (index, dayIndex) => {
        const newEntries = [...entries];
        newEntries[index].attendance[dayIndex] = !newEntries[index].attendance[dayIndex];
        setEntries(newEntries);
    };

    const paySalary = async (e) => {
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
            const apiUrl = API_URL;
            const absoluteApiUrl = apiUrl.startsWith('http') ? apiUrl : `${window.location.origin}${apiUrl}`;
            const pdfUrl = `${absoluteApiUrl}/payroll/pdf?token=${token}&ids=${empIds}`;
            if ((window ).Capacitor) {
                window.open(pdfUrl, '_system');
            } else {
                window.open(pdfUrl, '_blank');
            }
            alert(`Gaji ${validEntries.length} karyawan berhasil dibayar & Slip sedang dibuka!`);
            setEntries([{ ...defaultEntry }]);
        } catch (e2) {
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
        } catch (e3) { alert("Gagal menambah karyawan"); }
    };

    const deleteEmployee = async (id) => {
        if (!window.confirm("Yakin hapus karyawan?")) return;
        try {
            await api.delete(`/payroll/employees/${id}`);
            fetchEmployees();
        } catch (e4) { alert("Gagal menghapus"); }
    };

    const startEdit = (emp) => {
        setEditingId(emp.ID);
        setExpandedEmpId(emp.ID);
        setEditData({ Name: emp.Name, Position: emp.Position, SalaryAmount: emp.SalaryAmount, FingerprintPIN: emp.FingerprintPIN || '' });
    };

    const saveEdit = async (id) => {
        try {
            await api.put(`/payroll/employees/${id}`, editData);
            setEditingId(null);
            fetchEmployees();
        } catch (e5) { alert("Gagal update"); }
    };

    const autoFillFromLogs = () => {
        if (attendanceLogs.length === 0) {
            alert("Belum ada log absensi yang termuat!");
            return;
        }
        const getLateMinutes = (logTime, pin) => {
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
        const newEntries = [];
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

    const getLocalDateString = (timeStr) => {
        // Selalu gunakan WIB (Asia/Jakarta) agar tanggal tidak bergantung timezone browser
        const d = new Date(timeStr);
        const wibDateStr = d.toLocaleDateString('sv-SE', { timeZone: 'Asia/Jakarta' }); // format: YYYY-MM-DD
        return wibDateStr;
    };

    const getLogWorkDate = (log) => {
        if (log.WorkDate && !log.WorkDate.startsWith('0001-01-01')) {
            return log.WorkDate.split('T')[0];
        }
        return getLocalDateString(log.LogTime);
    };

    const getShiftLabel = (logTimeStr) => {
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

    const handleAlasanChange = async (date, pin, value) => {
        try {
            await api.post('/payroll/attendance-logs/reason', { date, employee_pin: pin, alasan: value });
            fetchAttendanceLogs();
        } catch (e6) { alert("Gagal menyimpan alasan absensi"); }
    };

    const deleteGroupedLogs = async (ids) => {
        if (!window.confirm("Yakin ingin menghapus semua scan hari ini?")) return;
        try {
            for (const id of ids) await api.delete(`/payroll/attendance-logs/${id}`);
            fetchAttendanceLogs();
        } catch (e7) { alert("Gagal menghapus log"); }
    };

    const handleDeleteSingleLog = async (id) => {
        if (!window.confirm("Yakin hapus log absensi ini?")) return;
        try {
            await api.delete(`/payroll/attendance-logs/${id}`);
            fetchAttendanceLogs();
        } catch (e8) { alert("Gagal menghapus log"); }
    };

    const openEditLog = (log, employeePin, date) => {
        if (log && log.ID) {
            const wibOpts = { timeZone: 'Asia/Jakarta', hour: '2-digit' , minute: '2-digit' , hour12: false };
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
        } catch (e9) { alert('Gagal memperbarui log'); }
    };

    const getGroupedAttendance = () => {
        const dateSet = new Set();
        for (let i = 0; i < 7; i++) {
            const d = new Date();
            d.setDate(d.getDate() - i);
            dateSet.add(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`);
        }
        attendanceLogs.forEach(log => { if (log.LogTime) dateSet.add(getLogWorkDate(log)); });
        const sortedDates = Array.from(dateSet).sort((a, b) => b.localeCompare(a));
        const rows = [];
        sortedDates.forEach(date => {
            employees.forEach(emp => {
                if (!emp.FingerprintPIN) return;
                const empLogs = attendanceLogs.filter(log => {
                    if (String(log.EmployeePIN) !== String(emp.FingerprintPIN)) return false;
                    return log.LogTime && getLogWorkDate(log) === date;
                });
                let scan1 = '', scan2 = '', alasan = '';
                let logIds = [];
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
                        const hourWIB = parseInt(_nullishCoalesce(_optionalChain([logTime, 'access', _4 => _4.toLocaleString, 'call', _5 => _5('sv-SE', { hour: '2-digit', ...WIB }), 'access', _6 => _6.split, 'call', _7 => _7(' '), 'access', _8 => _8[1], 'optionalAccess', _9 => _9.split, 'call', _10 => _10(':'), 'access', _11 => _11[0]]), () => ( '0')));
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

    const getEmployeeLogsGrouped = (employeePIN) => {
        const empLogs = attendanceLogs.filter(log => String(log.EmployeePIN) === String(employeePIN));
        const dateSet = new Set();
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
    const totalBonusSemua = entries.reduce((sum, e) => sum + ((e ).bonusAmount || bonusMap[Number(e.employee_id)] || 0), 0);

    // ─── Styles ───────────────────────────────────────────────────
    const tabStyle = (active) => ({
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

    const inputStyle = {
        padding: '10px 12px',
        borderRadius: '8px',
        border: '1px solid #e2e8f0',
        width: '100%',
        boxSizing: 'border-box',
        fontSize: '14px',
        background: 'white',
        outline: 'none',
    };

    const cardStyle = {
        background: 'white',
        borderRadius: '16px',
        padding: '16px',
        boxShadow: '0 1px 8px rgba(0,0,0,0.08)',
        marginBottom: '16px',
        border: '1px solid #f1f5f9',
    };

    return (
        React.createElement('div', { style: { padding: '12px', maxWidth: '700px', margin: '0 auto', fontFamily: 'system-ui, sans-serif', width: '100%', boxSizing: 'border-box', overflowX: 'hidden' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 451}}

            /* Header */
            , React.createElement('div', { style: { marginBottom: '16px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 454}}
                , React.createElement('h2', { style: { margin: 0, fontSize: '20px', fontWeight: 800, color: '#1e293b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 455}}, "Manajemen Penggajian" )
                , React.createElement('p', { style: { margin: '4px 0 0', color: '#64748b', fontSize: '13px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 456}}, employees.length, " karyawan aktif"  )
            )

            /* Tab Navigation */
            , React.createElement('div', { style: { display: 'flex', gap: '4px', background: '#f1f5f9', borderRadius: '12px', padding: '4px', marginBottom: '16px', width: '100%', maxWidth: '100%', boxSizing: 'border-box', overflowX: 'hidden' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 460}}
                , React.createElement('button', { style: tabStyle(activeTab === 'pay'), onClick: () => setActiveTab('pay'), __self: this, __source: {fileName: _jsxFileName, lineNumber: 461}}
                    , React.createElement(DollarSign, { size: 14, __self: this, __source: {fileName: _jsxFileName, lineNumber: 462}} ), " Bayar Gaji"
                )
                , React.createElement('button', { style: tabStyle(activeTab === 'employees'), onClick: () => setActiveTab('employees'), __self: this, __source: {fileName: _jsxFileName, lineNumber: 464}}
                    , React.createElement(Users, { size: 14, __self: this, __source: {fileName: _jsxFileName, lineNumber: 465}} ), " Karyawan"
                )
                , React.createElement('button', { style: tabStyle(activeTab === 'logs'), onClick: () => setActiveTab('logs'), __self: this, __source: {fileName: _jsxFileName, lineNumber: 467}}
                    , React.createElement(Clock, { size: 14, __self: this, __source: {fileName: _jsxFileName, lineNumber: 468}} ), " Log Absen"
                )
            )

            /* ═══════════════════════════════════════
                TAB 1: BAYAR GAJI
            ═══════════════════════════════════════ */
            , activeTab === 'pay' && (
                React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 476}}
                    /* Header aksi */
                    , React.createElement('div', { style: { display: 'flex', gap: '8px', marginBottom: '14px', width: '100%', maxWidth: '100%', boxSizing: 'border-box', overflowX: 'hidden', flexWrap: 'wrap' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 478}}
                        , React.createElement('button', {
                            type: "button",
                            onClick: autoFillFromLogs,
                            style: { flex: 1, minWidth: '150px', background: 'linear-gradient(135deg, #f59e0b, #d97706)', color: 'white', border: 'none', borderRadius: '10px', padding: '11px', fontSize: '13px', cursor: 'pointer', fontWeight: 700, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 479}}

                            , React.createElement(Zap, { size: 15, __self: this, __source: {fileName: _jsxFileName, lineNumber: 484}} ), " Auto-Isi dari Mesin"
                        )
                        , entries.length < 10 && (
                            React.createElement('button', {
                                type: "button",
                                onClick: () => setEntries([...entries, { ...defaultEntry }]),
                                style: { background: '#e0e7ff', color: '#3730a3', border: 'none', borderRadius: '10px', padding: '11px 14px', fontSize: '13px', cursor: 'pointer', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '4px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 487}}

                                , React.createElement(Plus, { size: 15, __self: this, __source: {fileName: _jsxFileName, lineNumber: 492}} ), " Baris"
                            )
                        )
                    )

                    , React.createElement('form', { onSubmit: paySalary, __self: this, __source: {fileName: _jsxFileName, lineNumber: 497}}
                        , entries.map((entry, index) => {
                            const emp = employees.find(e => e.ID === Number(entry.employee_id));
                            const totalGaji = calculateTotal(entry);
                            const bonus = (entry ).bonusAmount || bonusMap[Number(entry.employee_id)] || 0;
                            const totalDenganBonus = totalGaji + bonus;
                            const daysCount = entry.attendance.filter(Boolean).length;
                            const hasLate = String(entry.notes).includes('Telat') || String(entry.notes).includes('Potong');

                            const hapusDenda = () => {
                                const newEntries = [...entries];
                                newEntries[index] = { ...newEntries[index], amount: _optionalChain([emp, 'optionalAccess', _12 => _12.SalaryAmount]) || newEntries[index].amount, notes: 'Denda dihapus (manual)' };
                                setEntries(newEntries);
                            };

                            return (
                                React.createElement('div', { key: index, style: {
                                    ...cardStyle,
                                    border: hasLate ? '1px solid #fbbf24' : '1px solid #e2e8f0',
                                    background: hasLate ? '#fffbf0' : 'white',
                                    position: 'relative'
                                }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 513}}
                                    /* Badge nomor + tombol hapus baris */
                                    , React.createElement('div', { style: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 520}}
                                        , React.createElement('span', { style: { background: hasLate ? '#fbbf24' : '#0d6efd', color: 'white', borderRadius: '20px', padding: '2px 10px', fontSize: '12px', fontWeight: 700 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 521}}, "Karyawan "
                                             , index + 1
                                        )
                                        , entries.length > 1 && (
                                            React.createElement('button', { type: "button", onClick: () => setEntries(entries.filter((_, i) => i !== index)),
                                                style: { background: '#fee2e2', color: '#dc2626', border: 'none', borderRadius: '8px', padding: '4px 8px', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '3px', fontSize: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 525}}
                                                , React.createElement(X, { size: 14, __self: this, __source: {fileName: _jsxFileName, lineNumber: 527}} ), " Hapus"
                                            )
                                        )
                                    )

                                    /* Pilih karyawan + nominal - 2 kolom */
                                    , React.createElement('div', { style: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px', marginBottom: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 533}}
                                        , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 534}}
                                            , React.createElement('label', { style: { fontSize: '11px', color: '#64748b', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.5px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 535}}, "Karyawan")
                                            , React.createElement('select', { style: { ...inputStyle, marginTop: '4px' }, value: entry.employee_id, onChange: e => {
                                                const empFound = employees.find(x => x.ID == Number(e.target.value));
                                                handleEntryChange(index, 'employee_id', e.target.value);
                                                handleEntryChange(index, 'amount', _optionalChain([empFound, 'optionalAccess', _13 => _13.SalaryAmount]) || '');
                                            }, required: true, __self: this, __source: {fileName: _jsxFileName, lineNumber: 536}}
                                                , React.createElement('option', { value: "", __self: this, __source: {fileName: _jsxFileName, lineNumber: 541}}, "-- Pilih --"  )
                                                , employees.map(e => React.createElement('option', { key: e.ID, value: e.ID, __self: this, __source: {fileName: _jsxFileName, lineNumber: 542}}, e.Name))
                                            )
                                            , entry.employee_id && (
                                                React.createElement('button', {
                                                    type: "button",
                                                    onClick: () => {
                                                        const empFound = employees.find(x => x.ID == Number(entry.employee_id));
                                                        if (empFound) setSelectedEmployeeForLog(empFound);
                                                    },
                                                    style: {
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
                                                    }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 545}}

                                                    , React.createElement(Clock, { size: 12, __self: this, __source: {fileName: _jsxFileName, lineNumber: 567}} ), " Log Absen"
                                                )
                                            )
                                        )
                                        , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 571}}
                                            , React.createElement('label', { style: { fontSize: '11px', color: '#64748b', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.5px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 572}}, "Gaji / Hari (Rp)"   )
                                            , React.createElement('input', { type: "number", style: { ...inputStyle, marginTop: '4px' }, value: entry.amount, onChange: e => handleEntryChange(index, 'amount', e.target.value), required: true, __self: this, __source: {fileName: _jsxFileName, lineNumber: 573}} )
                                        )
                                    )

                                    /* Kehadiran - Checkbox grid dengan label jelas */
                                    , React.createElement('div', { style: { marginBottom: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 578}}
                                        , React.createElement('label', { style: { fontSize: '11px', color: '#64748b', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.5px', display: 'block', marginBottom: '8px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 579}}
                                            , React.createElement(CheckSquare, { size: 12, style: { display: 'inline', marginRight: '4px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 580}} ), "Kehadiran ("
                                             , daysCount, " hari)"
                                        )
                                        , React.createElement('div', { style: { display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: '4px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 583}}
                                            , days.map((day, dayIndex) => {
                                                const checked = entry.attendance[dayIndex];
                                                return (
                                                    React.createElement('label', { key: day, style: {
                                                        display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '4px',
                                                        background: checked ? '#dcfce7' : '#f8fafc',
                                                        border: checked ? '1.5px solid #22c55e' : '1.5px solid #e2e8f0',
                                                        borderRadius: '8px', padding: '6px 2px', cursor: 'pointer',
                                                        transition: 'all 0.15s'
                                                    }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 587}}
                                                        , React.createElement('span', { style: { fontSize: '11px', fontWeight: 700, color: checked ? '#16a34a' : '#94a3b8' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 594}}, day)
                                                        , React.createElement('input', {
                                                            type: "checkbox",
                                                            checked: checked,
                                                            onChange: () => handleAttendanceChange(index, dayIndex),
                                                            style: { width: '16px', height: '16px', cursor: 'pointer', accentColor: '#22c55e' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 595}}
                                                        )
                                                    )
                                                );
                                            })
                                        )
                                    )

                                    /* Catatan */
                                    , React.createElement('div', { style: { marginBottom: '10px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 608}}
                                        , React.createElement('label', { style: { fontSize: '11px', color: '#64748b', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.5px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 609}}, "Catatan")
                                        , React.createElement('div', { style: { display: 'flex', gap: '6px', marginTop: '4px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 610}}
                                            , React.createElement('input', {
                                                placeholder: "Catatan pembayaran..." ,
                                                style: { ...inputStyle, flex: 1, background: hasLate ? '#fff8e1' : 'white', borderColor: hasLate ? '#fbbf24' : '#e2e8f0' },
                                                value: entry.notes,
                                                onChange: e => handleEntryChange(index, 'notes', e.target.value), __self: this, __source: {fileName: _jsxFileName, lineNumber: 611}}
                                            )
                                            , hasLate && (
                                                React.createElement('button', { type: "button", onClick: hapusDenda,
                                                    style: { padding: '8px 10px', background: '#dc2626', color: 'white', border: 'none', borderRadius: '8px', fontSize: '12px', fontWeight: 700, cursor: 'pointer', whiteSpace: 'nowrap' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 618}}, "✕ Denda"

                                                )
                                            )
                                        )
                                    )

                                    /* Info denda */
                                    , hasLate && emp && (
                                        React.createElement('div', { style: { display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: '6px', background: '#fff3cd', border: '1px solid #fbbf24', borderRadius: '8px', padding: '8px 10px', fontSize: '12px', marginBottom: '10px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 628}}
                                            , React.createElement('span', { style: { color: '#92400e', fontWeight: 600 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 629}}, "⚠️ Asli: Rp "   , _optionalChain([emp, 'access', _14 => _14.SalaryAmount, 'optionalAccess', _15 => _15.toLocaleString, 'call', _16 => _16('id-ID')]), "/hari")
                                            , React.createElement('span', { style: { color: '#dc2626', fontWeight: 700 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 630}}, "→ Rp "  , Number(entry.amount).toLocaleString('id-ID'), "/hari (sudah dipotong)"  )
                                        )
                                    )

                                    /* Total per karyawan */
                                    , React.createElement('div', { style: { background: '#f0fdf4', border: '1px solid #bbf7d0', borderRadius: '10px', padding: '10px 14px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 635}}
                                        , React.createElement('div', { style: { fontSize: '12px', color: '#64748b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 636}}
                                            , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 637}}, "Gaji: Rp "  , totalGaji.toLocaleString('id-ID'))
                                            , bonus > 0 && React.createElement('div', { style: { color: '#0d6efd' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 638}}, "🏭 Bonus: Rp "   , bonus.toLocaleString('id-ID'))
                                        )
                                        , React.createElement('div', { style: { fontWeight: 800, fontSize: '18px', color: '#15803d' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 640}}, "Rp "
                                             , totalDenganBonus.toLocaleString('id-ID')
                                        )
                                    )
                                )
                            );
                        })

                        /* Summary total semua */
                        , entries.length > 1 && (
                            React.createElement('div', { style: { background: 'linear-gradient(135deg, #1e293b, #334155)', borderRadius: '14px', padding: '14px 16px', marginBottom: '12px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 650}}
                                , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 651}}
                                    , React.createElement('div', { style: { color: '#94a3b8', fontSize: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 652}}, "Total " , entries.length, " Karyawan" )
                                    , totalBonusSemua > 0 && React.createElement('div', { style: { color: '#60a5fa', fontSize: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 653}}, "+ Bonus Rp "   , totalBonusSemua.toLocaleString('id-ID'))
                                )
                                , React.createElement('div', { style: { color: '#4ade80', fontWeight: 900, fontSize: '20px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 655}}, "Rp "
                                     , (totalGajiSemua + totalBonusSemua).toLocaleString('id-ID')
                                )
                            )
                        )

                        , React.createElement('button', { type: "submit", style: {
                            width: '100%', padding: '15px', background: 'linear-gradient(135deg, #16a34a, #15803d)',
                            color: 'white', border: 'none', borderRadius: '12px', fontWeight: 800, cursor: 'pointer',
                            fontSize: '16px', boxShadow: '0 4px 12px rgba(22,163,74,0.4)', letterSpacing: '0.3px'
                        }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 661}}, "💰 Bayar & Cetak Slip ("
                                 , entries.length, " Karyawan)"
                        )
                    )
                )
            )

            /* ═══════════════════════════════════════
                TAB 2: DATA KARYAWAN
            ═══════════════════════════════════════ */
            , activeTab === 'employees' && (
                React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 676}}
                    /* Tombol tambah */
                    , React.createElement('button', {
                        onClick: () => setShowAddEmp(!showAddEmp),
                        style: { width: '100%', padding: '12px', background: showAddEmp ? '#f1f5f9' : 'linear-gradient(135deg, #0d6efd, #2563eb)', color: showAddEmp ? '#475569' : 'white', border: showAddEmp ? '1px dashed #cbd5e1' : 'none', borderRadius: '12px', fontWeight: 700, cursor: 'pointer', fontSize: '14px', marginBottom: '14px', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 678}}

                        , showAddEmp ? React.createElement(React.Fragment, null, React.createElement(X, { size: 16, __self: this, __source: {fileName: _jsxFileName, lineNumber: 682}} ), " Batal" ) : React.createElement(React.Fragment, null, React.createElement(Plus, { size: 16, __self: this, __source: {fileName: _jsxFileName, lineNumber: 682}} ), " Tambah Karyawan Baru"   )
                    )

                    /* Form tambah */
                    , showAddEmp && (
                        React.createElement('div', { style: { ...cardStyle, border: '2px dashed #93c5fd', background: '#eff6ff' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 687}}
                            , React.createElement('h4', { style: { margin: '0 0 12px', color: '#1d4ed8', fontSize: '14px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 688}}, "Data Karyawan Baru"  )
                            , React.createElement('div', { style: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px', marginBottom: '8px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 689}}
                                , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 690}}
                                    , React.createElement('label', { style: { fontSize: '11px', color: '#475569', fontWeight: 600 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 691}}, "Nama Lengkap *"  )
                                    , React.createElement('input', { placeholder: "Nama karyawan" , style: { ...inputStyle, marginTop: '4px' }, value: newEmp.name, onChange: e => setNewEmp({ ...newEmp, name: e.target.value }), __self: this, __source: {fileName: _jsxFileName, lineNumber: 692}} )
                                )
                                , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 694}}
                                    , React.createElement('label', { style: { fontSize: '11px', color: '#475569', fontWeight: 600 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 695}}, "Jabatan")
                                    , React.createElement('input', { placeholder: "Posisi / Jabatan"  , style: { ...inputStyle, marginTop: '4px' }, value: newEmp.position, onChange: e => setNewEmp({ ...newEmp, position: e.target.value }), __self: this, __source: {fileName: _jsxFileName, lineNumber: 696}} )
                                )
                                , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 698}}
                                    , React.createElement('label', { style: { fontSize: '11px', color: '#475569', fontWeight: 600 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 699}}, "Gaji Harian (Rp) *"   )
                                    , React.createElement('input', { type: "number", placeholder: "Misal: 100000" , style: { ...inputStyle, marginTop: '4px' }, value: newEmp.salary, onChange: e => setNewEmp({ ...newEmp, salary: e.target.value }), __self: this, __source: {fileName: _jsxFileName, lineNumber: 700}} )
                                )
                                , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 702}}
                                    , React.createElement('label', { style: { fontSize: '11px', color: '#475569', fontWeight: 600 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 703}}, "PIN Mesin" )
                                    , React.createElement('input', { placeholder: "Angka PIN" , style: { ...inputStyle, marginTop: '4px' }, value: newEmp.pin, onChange: e => setNewEmp({ ...newEmp, pin: e.target.value }), __self: this, __source: {fileName: _jsxFileName, lineNumber: 704}} )
                                )
                            )
                            , React.createElement('button', { onClick: addEmployee,
                                style: { width: '100%', padding: '12px', background: '#0d6efd', color: 'white', border: 'none', borderRadius: '10px', fontWeight: 700, cursor: 'pointer', fontSize: '14px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 707}}
                                , React.createElement(Plus, { size: 16, style: { display: 'inline', marginRight: '6px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 709}} ), " Simpan Karyawan"
                            )
                        )
                    )

                    /* List karyawan sebagai cards */
                    , employees.map(emp => (
                        React.createElement('div', { key: emp.ID, style: { ...cardStyle, border: editingId === emp.ID ? '2px solid #0d6efd' : '1px solid #e2e8f0' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 716}}
                            , editingId === emp.ID ? (
                                /* Mode Edit */
                                React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 719}}
                                    , React.createElement('div', { style: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 720}}
                                        , React.createElement('span', { style: { fontWeight: 700, color: '#0d6efd', fontSize: '14px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 721}}, "✏️ Edit Karyawan"  )
                                        , React.createElement('button', { onClick: () => setEditingId(null), style: { background: 'none', border: 'none', color: '#dc2626', cursor: 'pointer', padding: 0 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 722}}, React.createElement(X, { size: 18, __self: this, __source: {fileName: _jsxFileName, lineNumber: 722}} ))
                                    )
                                    , React.createElement('div', { style: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px', marginBottom: '8px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 724}}
                                        , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 725}}
                                            , React.createElement('label', { style: { fontSize: '11px', color: '#475569', fontWeight: 600 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 726}}, "Nama")
                                            , React.createElement('input', { value: editData.Name, onChange: e => setEditData({ ...editData, Name: e.target.value }), style: { ...inputStyle, marginTop: '4px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 727}} )
                                        )
                                        , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 729}}
                                            , React.createElement('label', { style: { fontSize: '11px', color: '#475569', fontWeight: 600 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 730}}, "Jabatan")
                                            , React.createElement('input', { value: editData.Position, onChange: e => setEditData({ ...editData, Position: e.target.value }), style: { ...inputStyle, marginTop: '4px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 731}} )
                                        )
                                        , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 733}}
                                            , React.createElement('label', { style: { fontSize: '11px', color: '#475569', fontWeight: 600 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 734}}, "Gaji Harian (Rp)"  )
                                            , React.createElement('input', { type: "number", value: editData.SalaryAmount, onChange: e => setEditData({ ...editData, SalaryAmount: Number(e.target.value) }), style: { ...inputStyle, marginTop: '4px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 735}} )
                                        )
                                        , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 737}}
                                            , React.createElement('label', { style: { fontSize: '11px', color: '#475569', fontWeight: 600 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 738}}, "PIN Mesin" )
                                            , React.createElement('input', { value: editData.FingerprintPIN, onChange: e => setEditData({ ...editData, FingerprintPIN: e.target.value }), style: { ...inputStyle, marginTop: '4px' }, placeholder: "PIN", __self: this, __source: {fileName: _jsxFileName, lineNumber: 739}} )
                                        )
                                    )
                                    , React.createElement('button', { onClick: () => saveEdit(emp.ID),
                                        style: { width: '100%', padding: '11px', background: '#16a34a', color: 'white', border: 'none', borderRadius: '10px', fontWeight: 700, cursor: 'pointer', fontSize: '14px', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 742}}
                                        , React.createElement(Save, { size: 15, __self: this, __source: {fileName: _jsxFileName, lineNumber: 744}} ), " Simpan Perubahan"
                                    )
                                )
                            ) : (
                                /* Mode View */
                                React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 749}}
                                    , React.createElement('div', { style: { display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 750}}
                                        , React.createElement('div', { style: { flex: 1 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 751}}
                                            , React.createElement('div', { style: { display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 752}}
                                                , emp.FingerprintPIN && (
                                                    React.createElement('span', { style: { background: '#e0e7ff', color: '#3730a3', borderRadius: '6px', padding: '2px 8px', fontSize: '12px', fontWeight: 700 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 754}}, "PIN: "
                                                         , emp.FingerprintPIN
                                                    )
                                                )
                                                , !emp.IsActive && (
                                                    React.createElement('span', { style: { background: '#fee2e2', color: '#dc2626', borderRadius: '6px', padding: '2px 8px', fontSize: '11px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 759}}, "Nonaktif")
                                                )
                                            )
                                            , React.createElement('div', { style: { fontWeight: 700, fontSize: '15px', color: '#1e293b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 762}}, emp.Name)
                                            , React.createElement('div', { style: { color: '#64748b', fontSize: '13px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 763}}, emp.Position || 'Tanpa Jabatan')
                                        )
                                        , React.createElement('div', { style: { textAlign: 'right' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 765}}
                                            , React.createElement('div', { style: { fontWeight: 800, fontSize: '16px', color: '#15803d' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 766}}, "Rp " , _optionalChain([emp, 'access', _17 => _17.SalaryAmount, 'optionalAccess', _18 => _18.toLocaleString, 'call', _19 => _19('id-ID')]))
                                            , React.createElement('div', { style: { color: '#94a3b8', fontSize: '11px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 767}}, "/hari")
                                        )
                                    )
                                    , React.createElement('div', { style: { display: 'flex', gap: '6px', marginTop: '12px', flexWrap: 'wrap' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 770}}
                                        , React.createElement('button', { onClick: () => setSelectedEmployeeForLog(emp),
                                            style: { flex: 1, minWidth: '80px', padding: '8px 10px', background: '#f1f5f9', color: '#475569', border: '1px solid #cbd5e1', borderRadius: '8px', cursor: 'pointer', fontWeight: 600, fontSize: '12px', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '4px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 771}}
                                            , React.createElement(Clock, { size: 14, __self: this, __source: {fileName: _jsxFileName, lineNumber: 773}} ), " Log Absen"
                                        )
                                        , React.createElement('button', { onClick: () => startEdit(emp),
                                            style: { flex: 1, minWidth: '70px', padding: '8px 10px', background: '#eff6ff', color: '#2563eb', border: '1px solid #bfdbfe', borderRadius: '8px', cursor: 'pointer', fontWeight: 600, fontSize: '12px', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '4px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 775}}
                                            , React.createElement(Edit2, { size: 14, __self: this, __source: {fileName: _jsxFileName, lineNumber: 777}} ), " Edit"
                                        )
                                        , React.createElement('button', { onClick: () => deleteEmployee(emp.ID),
                                            style: { flex: 1, minWidth: '70px', padding: '8px 10px', background: '#fef2f2', color: '#dc2626', border: '1px solid #fecaca', borderRadius: '8px', cursor: 'pointer', fontWeight: 600, fontSize: '12px', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '4px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 779}}
                                            , React.createElement(Trash2, { size: 14, __self: this, __source: {fileName: _jsxFileName, lineNumber: 781}} ), " Hapus"
                                        )
                                    )
                                )
                            )
                        )
                    ))

                    , employees.length === 0 && (
                        React.createElement('div', { style: { textAlign: 'center', padding: '40px 20px', color: '#94a3b8' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 790}}
                            , React.createElement(Users, { size: 40, style: { opacity: 0.3, marginBottom: '10px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 791}} )
                            , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 792}}, "Belum ada karyawan. Tambah karyawan pertama!"     )
                        )
                    )
                )
            )

            /* ═══════════════════════════════════════
                TAB 3: LOG ABSENSI
            ═══════════════════════════════════════ */
            , activeTab === 'logs' && (
                React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 802}}
                    /* Filter bar */
                    , React.createElement('div', { style: { ...cardStyle, display: 'flex', gap: '8px', flexWrap: 'wrap', padding: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 804}}
                        , React.createElement('select', { style: { ...inputStyle, flex: 1, minWidth: '140px' }, value: logFilterDate, onChange: e => setLogFilterDate(e.target.value), __self: this, __source: {fileName: _jsxFileName, lineNumber: 805}}
                            , React.createElement('option', { value: "", __self: this, __source: {fileName: _jsxFileName, lineNumber: 806}}, "📅 Semua Tanggal"  )
                            , getGroupedAttendance().sortedDates.map(d => (
                                React.createElement('option', { key: d, value: d, __self: this, __source: {fileName: _jsxFileName, lineNumber: 808}}, new Date(d + 'T00:00:00').toLocaleDateString('id-ID', { day: '2-digit', month: 'short', year: 'numeric' }))
                            ))
                        )
                        , React.createElement('input', { placeholder: "🔍 Cari nama..."  , style: { ...inputStyle, flex: 1, minWidth: '130px' }, value: logFilterName, onChange: e => setLogFilterName(e.target.value), __self: this, __source: {fileName: _jsxFileName, lineNumber: 811}} )
                        , React.createElement('button', { onClick: fetchAttendanceLogs,
                            style: { padding: '10px 14px', background: '#f1f5f9', border: '1px solid #e2e8f0', borderRadius: '8px', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '4px', fontSize: '13px', color: '#475569', fontWeight: 600 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 812}}
                            , React.createElement(RefreshCw, { size: 14, __self: this, __source: {fileName: _jsxFileName, lineNumber: 814}} ), " Refresh"
                        )
                    )

                    /* Log cards — tidak ada scroll horizontal! */
                    , (() => {
                        const filtered = getGroupedAttendance().rows.filter(row => {
                            if (logFilterDate && row.date !== logFilterDate) return false;
                            if (logFilterName && !row.employee.Name.toLowerCase().includes(logFilterName.toLowerCase())) return false;
                            return true;
                        });

                        if (filtered.length === 0) {
                            return (
                                React.createElement('div', { style: { textAlign: 'center', padding: '40px 20px', color: '#94a3b8', background: 'white', borderRadius: '16px', border: '1px solid #f1f5f9' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 828}}
                                    , React.createElement(Clock, { size: 40, style: { opacity: 0.3, marginBottom: '10px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 829}} )
                                    , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 830}}, "Belum ada log dari mesin absen"     )
                                )
                            );
                        }

                        // Group by date
                        const byDate = {};
                        filtered.forEach(row => {
                            if (!byDate[row.date]) byDate[row.date] = [];
                            byDate[row.date].push(row);
                        });

                        return Object.entries(byDate).map(([date, rows]) => (
                            React.createElement('div', { key: date, style: { marginBottom: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 843}}
                                /* Date header */
                                , React.createElement('div', { style: { background: '#1e293b', color: 'white', borderRadius: '10px 10px 0 0', padding: '8px 14px', fontSize: '13px', fontWeight: 700 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 845}}, "📅 "
                                     , new Date(date + 'T00:00:00').toLocaleDateString('id-ID', { weekday: 'long', day: '2-digit', month: 'long', year: 'numeric' })
                                )
                                , React.createElement('div', { style: { background: 'white', borderRadius: '0 0 10px 10px', border: '1px solid #e2e8f0', borderTop: 'none', overflow: 'hidden' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 848}}
                                    , rows.map((row, ri) => {
                                        const hadir = !!row.scan1;
                                        return (
                                            React.createElement('div', { key: ri, style: { padding: '12px 14px', borderBottom: ri < rows.length - 1 ? '1px solid #f1f5f9' : 'none' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 852}}
                                                , React.createElement('div', { style: { display: 'flex', alignItems: 'center', gap: '10px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 853}}
                                                    /* Status dot */
                                                    , React.createElement('div', { style: {
                                                        width: '10px', height: '10px', borderRadius: '50%', flexShrink: 0,
                                                        background: hadir ? '#22c55e' : (row.alasan ? '#f59e0b' : '#e2e8f0')
                                                    }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 855}} )
                                                    , React.createElement('div', { style: { flex: 1, minWidth: 0 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 859}}
                                                        , React.createElement('div', { style: { fontWeight: 700, fontSize: '14px', color: '#1e293b', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 860}}
                                                            , row.employee.Name
                                                        )
                                                        , React.createElement('div', { style: { display: 'flex', flexWrap: 'wrap', gap: '6px', marginTop: '4px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 863}}
                                                            , row.scan1 && (
                                                                React.createElement('span', { style: { background: '#dcfce7', color: '#16a34a', borderRadius: '6px', padding: '2px 8px', fontSize: '12px', fontWeight: 600 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 865}}, "Masuk: "
                                                                     , row.scan1
                                                                )
                                                            )
                                                            , row.scan2 && (
                                                                React.createElement('span', { style: { background: '#dbeafe', color: '#1d4ed8', borderRadius: '6px', padding: '2px 8px', fontSize: '12px', fontWeight: 600 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 870}}, "Pulang: "
                                                                     , row.scan2
                                                                )
                                                            )
                                                            , !hadir && !row.scan2 && (
                                                                row.alasan ? (
                                                                    React.createElement('span', { style: { background: '#fff3cd', color: '#92400e', borderRadius: '6px', padding: '2px 8px', fontSize: '12px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 876}}
                                                                        , row.alasan
                                                                    )
                                                                ) : (
                                                                    React.createElement('select', {
                                                                        value: row.alasan,
                                                                        onChange: e => handleAlasanChange(row.date, row.employee.FingerprintPIN, e.target.value),
                                                                        style: { padding: '3px 8px', borderRadius: '6px', border: '1px solid #fbbf24', fontSize: '12px', background: '#fffbf0', color: '#92400e', cursor: 'pointer' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 880}}

                                                                        , React.createElement('option', { value: "", __self: this, __source: {fileName: _jsxFileName, lineNumber: 885}}, "-- Alasan Absen --"   )
                                                                        , React.createElement('option', { value: "Sakit", __self: this, __source: {fileName: _jsxFileName, lineNumber: 886}}, "Sakit")
                                                                        , React.createElement('option', { value: "Mesin Error" , __self: this, __source: {fileName: _jsxFileName, lineNumber: 887}}, "Mesin Error" )
                                                                        , React.createElement('option', { value: "Izin", __self: this, __source: {fileName: _jsxFileName, lineNumber: 888}}, "Izin")
                                                                        , React.createElement('option', { value: "Tanpa Keterangan" , __self: this, __source: {fileName: _jsxFileName, lineNumber: 889}}, "Tanpa Keterangan" )
                                                                    )
                                                                )
                                                            )
                                                        )
                                                    )
                                                    /* Tombol Edit & Hapus */
                                                    , React.createElement('div', { style: { display: 'flex', gap: '4px', flexShrink: 0 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 896}}
                                                        , React.createElement('button', {
                                                            onClick: () => {
                                                                const firstLog = row.logIds.length > 0 
                                                                    ? attendanceLogs.find((l) => l.ID === row.logIds[0]) 
                                                                    : null;
                                                                openEditLog(firstLog, row.employee.FingerprintPIN, row.date);
                                                            },
                                                            title: "Edit jam masuk/pulang"  ,
                                                            style: { background: '#eff6ff', border: '1px solid #bfdbfe', color: '#2563eb', cursor: 'pointer', padding: '5px 8px', borderRadius: '6px', display: 'flex', alignItems: 'center', gap: '3px', fontSize: '12px', fontWeight: 600 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 897}}

                                                            , React.createElement(Edit2, { size: 13, __self: this, __source: {fileName: _jsxFileName, lineNumber: 907}} ), " Edit"
                                                        )
                                                        , row.logIds.length > 0 && (
                                                            React.createElement('button', {
                                                                onClick: () => deleteGroupedLogs(row.logIds),
                                                                title: "Hapus semua scan hari ini"    ,
                                                                style: { background: '#fef2f2', border: '1px solid #fecaca', color: '#dc2626', cursor: 'pointer', padding: '5px 8px', borderRadius: '6px', display: 'flex', alignItems: 'center', gap: '3px', fontSize: '12px', fontWeight: 600 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 910}}

                                                                , React.createElement(Trash2, { size: 13, __self: this, __source: {fileName: _jsxFileName, lineNumber: 915}} ), " Hapus"
                                                            )
                                                        )
                                                    )
                                                    /* Expand toggle */
                                                    , React.createElement('button', {
                                                        onClick: () => setExpandedEmpId(expandedEmpId === row.employee.ID ? null : row.employee.ID),
                                                        style: { background: 'none', border: 'none', color: '#94a3b8', cursor: 'pointer', padding: '4px', flexShrink: 0 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 920}}

                                                        , expandedEmpId === row.employee.ID ? React.createElement(ChevronUp, { size: 14, __self: this, __source: {fileName: _jsxFileName, lineNumber: 924}} ) : React.createElement(ChevronDown, { size: 14, __self: this, __source: {fileName: _jsxFileName, lineNumber: 924}} )
                                                    )
                                                )
                                            )
                                        );
                                    })
                                )
                            )
                        ));
                    })()
                )
            )

            /* Modal Log Absen Pop-up */
            , selectedEmployeeForLog && (
                React.createElement('div', { style: {
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
                }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 939}}
                    , React.createElement('div', { style: {
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
                    }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 954}}
                        /* Header */
                        , React.createElement('div', { style: {
                            padding: '16px',
                            borderBottom: '1px solid #f1f5f9',
                            display: 'flex',
                            justifyContent: 'space-between',
                            alignItems: 'center',
                            background: '#f8fafc'
                        }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 967}}
                            , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 975}}
                                , React.createElement('h3', { style: { margin: 0, fontSize: '15px', fontWeight: 800, color: '#0f172a' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 976}}, "Riwayat Absensi Karyawan"

                                )
                                , React.createElement('p', { style: { margin: '2px 0 0', fontSize: '12px', color: '#64748b', fontWeight: 500 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 979}}
                                    , selectedEmployeeForLog.Name, " " , selectedEmployeeForLog.Position ? `(${selectedEmployeeForLog.Position})` : ''
                                )
                            )
                            , React.createElement('button', {
                                onClick: () => setSelectedEmployeeForLog(null),
                                style: {
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
                                }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 983}}

                                , React.createElement(X, { size: 16, __self: this, __source: {fileName: _jsxFileName, lineNumber: 999}} )
                            )
                        )

                        /* Body dengan independent vertical scroll */
                        , React.createElement('div', { style: {
                            padding: '16px',
                            overflowY: 'auto',
                            maxHeight: '60vh',
                            flex: 1,
                            backgroundColor: '#f8fafc'
                        }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1004}}
                            , (() => {
                                const logsGrouped = getEmployeeLogsGrouped(selectedEmployeeForLog.FingerprintPIN);
                                if (logsGrouped.length === 0) {
                                    return (
                                        React.createElement('div', { style: { textAlign: 'center', padding: '30px 10px', color: '#94a3b8' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1015}}
                                            , React.createElement(Clock, { size: 32, style: { opacity: 0.3, marginBottom: '8px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1016}} )
                                            , React.createElement('div', { style: { fontSize: '13px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1017}}, "Tidak ada riwayat absensi ditemukan untuk PIN "       , selectedEmployeeForLog.FingerprintPIN)
                                        )
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
                                        React.createElement('div', { key: idx, style: {
                                            backgroundColor: 'white',
                                            borderRadius: '12px',
                                            padding: '12px 14px',
                                            marginBottom: '10px',
                                            border: '1px solid #e2e8f0',
                                            boxShadow: '0 1px 3px rgba(0,0,0,0.02)'
                                        }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1032}}
                                            , React.createElement('div', { style: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1040}}
                                                , React.createElement('span', { style: { fontSize: '13px', fontWeight: 700, color: '#1e293b' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1041}}
                                                    , new Date(g.date + 'T00:00:00').toLocaleDateString('id-ID', { weekday: 'long', day: '2-digit', month: 'short', year: 'numeric' })
                                                )
                                                , React.createElement('span', { style: {
                                                    backgroundColor: shiftColor,
                                                    color: shiftTextColor,
                                                    borderRadius: '6px',
                                                    padding: '2px 8px',
                                                    fontSize: '11px',
                                                    fontWeight: 700
                                                }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1044}}
                                                    , g.shift
                                                )
                                            )

                                            , React.createElement('div', { style: { display: 'flex', flexWrap: 'wrap', gap: '8px', alignItems: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1056}}
                                                , g.scan1 ? (
                                                    React.createElement('span', { style: { background: '#ecfdf5', color: '#065f46', borderRadius: '6px', padding: '4px 8px', fontSize: '11px', fontWeight: 600 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1058}}, "Masuk: "
                                                         , g.scan1
                                                    )
                                                ) : (
                                                    React.createElement('span', { style: { background: '#f8fafc', color: '#64748b', borderRadius: '6px', padding: '4px 8px', fontSize: '11px', fontStyle: 'italic' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1062}}, "Belum Masuk"

                                                    )
                                                )

                                                , g.scan2 ? (
                                                    React.createElement('span', { style: { background: '#eff6ff', color: '#1e40af', borderRadius: '6px', padding: '4px 8px', fontSize: '11px', fontWeight: 600 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1068}}, "Pulang: "
                                                         , g.scan2
                                                    )
                                                ) : (
                                                    React.createElement('span', { style: { background: '#f8fafc', color: '#64748b', borderRadius: '6px', padding: '4px 8px', fontSize: '11px', fontStyle: 'italic' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1072}}, "Belum Pulang"

                                                    )
                                                )
                                            )
                                            /* Info & Aksi */
                                            , React.createElement('div', { style: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '6px', flexWrap: 'wrap', marginTop: '8px', borderTop: '1px dashed #f1f5f9', paddingTop: '8px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1078}}
                                                , React.createElement('div', { style: { display: 'flex', gap: '6px', flexWrap: 'wrap' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1079}}
                                                    , hasHadir && (
                                                        React.createElement('span', { style: {
                                                            backgroundColor: lateColor,
                                                            color: lateTextColor,
                                                            borderRadius: '6px',
                                                            padding: '2px 8px',
                                                            fontSize: '11px',
                                                            fontWeight: 600
                                                        }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1081}}
                                                            , g.late > 0 ? `Terlambat: ${g.late} menit` : 'Tepat Waktu'
                                                        )
                                                    )
                                                    , g.alasan && (
                                                        React.createElement('span', { style: {
                                                            backgroundColor: '#fff3cd',
                                                            color: '#92400e',
                                                            borderRadius: '6px',
                                                            padding: '2px 8px',
                                                            fontSize: '11px',
                                                            fontWeight: 600
                                                        }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1093}}, "Alasan: "
                                                             , g.alasan
                                                        )
                                                    )
                                                )

                                                , React.createElement('div', { style: { display: 'flex', gap: '4px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1106}}
                                                    , React.createElement('button', {
                                                        onClick: () => {
                                                            const firstLog = g.logs && g.logs.length > 0 ? g.logs[0] : null;
                                                            openEditLog(firstLog, selectedEmployeeForLog.FingerprintPIN, g.date);
                                                        },
                                                        title: "Edit jam masuk/pulang"  ,
                                                        style: { background: '#eff6ff', border: '1px solid #bfdbfe', color: '#2563eb', cursor: 'pointer', padding: '4px 8px', borderRadius: '6px', display: 'flex', alignItems: 'center', gap: '3px', fontSize: '11px', fontWeight: 600 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1107}}

                                                        , React.createElement(Edit2, { size: 12, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1115}} ), " Edit"
                                                    )
                                                    , g.logs && g.logs.length > 0 && (
                                                        React.createElement('button', {
                                                            onClick: () => {
                                                                const logIds = g.logs.map((l) => l.ID);
                                                                deleteGroupedLogs(logIds);
                                                            },
                                                            title: "Hapus semua scan hari ini"    ,
                                                            style: { background: '#fef2f2', border: '1px solid #fecaca', color: '#dc2626', cursor: 'pointer', padding: '4px 8px', borderRadius: '6px', display: 'flex', alignItems: 'center', gap: '3px', fontSize: '11px', fontWeight: 600 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1118}}

                                                            , React.createElement(Trash2, { size: 12, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1126}} ), " Hapus"
                                                        )
                                                    )
                                                )
                                            )
                                        )
                                    );
                                });
                            })()
                        )

                        /* Footer */
                        , React.createElement('div', { style: {
                            padding: '12px 16px',
                            borderTop: '1px solid #f1f5f9',
                            display: 'flex',
                            justifyContent: 'flex-end',
                            backgroundColor: '#f8fafc'
                        }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1138}}
                            , React.createElement('button', {
                                onClick: () => setSelectedEmployeeForLog(null),
                                style: {
                                    padding: '8px 16px',
                                    background: 'linear-gradient(135deg, #0f172a, #1e293b)',
                                    color: 'white',
                                    border: 'none',
                                    borderRadius: '8px',
                                    fontSize: '13px',
                                    fontWeight: 700,
                                    cursor: 'pointer'
                                }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1145}}
, "Tutup"

                            )
                        )
                    )
                )
            )

            /* ═══════════════════════════════════════
                MODAL EDIT LOG ABSENSI
            ═══════════════════════════════════════ */
            , editingLog && (
                React.createElement('div', { style: {
                    position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
                    backgroundColor: 'rgba(15, 23, 42, 0.7)',
                    display: 'flex', justifyContent: 'center', alignItems: 'center',
                    zIndex: 3000, backdropFilter: 'blur(4px)', padding: '16px', boxSizing: 'border-box'
                }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1169}}
                    , React.createElement('div', { style: {
                        backgroundColor: 'white', borderRadius: '20px', width: '100%', maxWidth: '400px',
                        boxShadow: '0 25px 50px rgba(0,0,0,0.25)', overflow: 'hidden',
                        border: '1px solid #e2e8f0'
                    }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1175}}
                        /* Header */
                        , React.createElement('div', { style: { background: 'linear-gradient(135deg, #1e293b, #334155)', padding: '16px 20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1181}}
                            , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 1182}}
                                , React.createElement('h3', { style: { margin: 0, fontSize: '15px', fontWeight: 800, color: 'white' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1183}}, "✏️ Edit Log Absensi"   )
                                , React.createElement('p', { style: { margin: '3px 0 0', fontSize: '12px', color: '#94a3b8' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1184}}, "PIN: " , editingLog.EmployeePIN, " — Ubah jam masuk & pulang"      )
                            )
                            , React.createElement('button', { onClick: () => setEditingLog(null), style: { background: 'rgba(255,255,255,0.1)', border: 'none', color: 'white', borderRadius: '50%', width: '30px', height: '30px', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1186}}
                                , React.createElement(X, { size: 16, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1187}} )
                            )
                        )

                        /* Body */
                        , React.createElement('div', { style: { padding: '20px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1192}}
                            /* Peringatan */
                            , React.createElement('div', { style: { background: '#fff3cd', border: '1px solid #fbbf24', borderRadius: '10px', padding: '10px 14px', marginBottom: '16px', display: 'flex', alignItems: 'flex-start', gap: '8px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1194}}
                                , React.createElement(AlertTriangle, { size: 16, style: { color: '#d97706', flexShrink: 0, marginTop: '1px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1195}} )
                                , React.createElement('p', { style: { margin: 0, fontSize: '12px', color: '#92400e', lineHeight: '1.5' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1196}}, "Gunakan fitur ini hanya jika mesin error. Perubahan akan langsung tersimpan ke database."

                                )
                            )

                            /* Tanggal Kerja */
                            , React.createElement('div', { style: { marginBottom: '14px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1202}}
                                , React.createElement('label', { style: { fontSize: '11px', color: '#475569', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.5px', display: 'block', marginBottom: '6px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1203}}, "Tanggal Kerja" )
                                , React.createElement('input', {
                                    id: "edit-log-work-date",
                                    type: "date",
                                    value: editLogForm.work_date,
                                    onChange: e => setEditLogForm({ ...editLogForm, work_date: e.target.value }),
                                    style: { ...inputStyle, fontWeight: 600 }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1204}}
                                )
                            )

                            /* Jam Masuk & Jam Pulang */
                            , React.createElement('div', { style: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', marginBottom: '20px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1214}}
                                , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 1215}}
                                    , React.createElement('label', { style: { fontSize: '11px', color: '#16a34a', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.5px', display: 'block', marginBottom: '6px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1216}}, "⏰ Jam Masuk (WIB)"   )
                                    , React.createElement('input', {
                                        id: "edit-log-checkin",
                                        type: "time",
                                        value: editLogForm.log_time,
                                        onChange: e => setEditLogForm({ ...editLogForm, log_time: e.target.value }),
                                        style: { ...inputStyle, borderColor: '#86efac', background: '#f0fdf4', fontWeight: 700, fontSize: '16px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1217}}
                                    )
                                    , React.createElement('div', { style: { fontSize: '10px', color: '#64748b', marginTop: '4px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1224}}, "Kosongkan = tidak diubah"   )
                                )
                                , React.createElement('div', {__self: this, __source: {fileName: _jsxFileName, lineNumber: 1226}}
                                    , React.createElement('label', { style: { fontSize: '11px', color: '#1d4ed8', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.5px', display: 'block', marginBottom: '6px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1227}}, "⏰ Jam Pulang (WIB)"   )
                                    , React.createElement('input', {
                                        id: "edit-log-checkout",
                                        type: "time",
                                        value: editLogForm.check_out_time,
                                        onChange: e => setEditLogForm({ ...editLogForm, check_out_time: e.target.value }),
                                        style: { ...inputStyle, borderColor: '#93c5fd', background: '#eff6ff', fontWeight: 700, fontSize: '16px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1228}}
                                    )
                                    , React.createElement('div', { style: { fontSize: '10px', color: '#64748b', marginTop: '4px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1235}}, "Kosongkan = hapus jam pulang"    )
                                )
                            )

                            /* Tombol aksi */
                            , React.createElement('div', { style: { display: 'flex', gap: '10px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1240}}
                                , React.createElement('button', {
                                    onClick: () => setEditingLog(null),
                                    style: { flex: 1, padding: '12px', background: '#f1f5f9', color: '#475569', border: '1px solid #e2e8f0', borderRadius: '10px', fontWeight: 700, cursor: 'pointer', fontSize: '14px' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1241}}
, "Batal"

                                )
                                , React.createElement('button', {
                                    onClick: saveEditLog,
                                    style: { flex: 2, padding: '12px', background: 'linear-gradient(135deg, #16a34a, #15803d)', color: 'white', border: 'none', borderRadius: '10px', fontWeight: 800, cursor: 'pointer', fontSize: '14px', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px', boxShadow: '0 4px 12px rgba(22,163,74,0.3)' }, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1247}}

                                    , React.createElement(Save, { size: 15, __self: this, __source: {fileName: _jsxFileName, lineNumber: 1251}} ), " Simpan Perubahan"
                                )
                            )
                        )
                    )
                )
            )
        )
    );
};

export default Payroll;