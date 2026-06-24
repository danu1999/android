BEGIN;

INSERT INTO bmp_adms_devices (id, "serialNumber", alias, "lastActivity", "createdAt")
SELECT id, "serialNumber", alias, "lastActivity", "createdAt"
FROM "BmpAdmsDevice"
ON CONFLICT (id) DO UPDATE SET
  "serialNumber" = EXCLUDED."serialNumber",
  alias = EXCLUDED.alias,
  "lastActivity" = EXCLUDED."lastActivity";

INSERT INTO bmp_attendance_logs (id, "deviceSN", "employeePIN", "verifyType", "verifyState", "logTime", "checkOutTime", "workDate", "lateMinutes", alasan, "createdAt", "isSynced")
SELECT id, "deviceSN", "employeePIN", "verifyType", "verifyState", "logTime", "checkOutTime", "workDate", "lateMinutes", alasan, "createdAt", true
FROM "BmpAttendanceLog"
ON CONFLICT (id) DO UPDATE SET
  "deviceSN" = EXCLUDED."deviceSN",
  "employeePIN" = EXCLUDED."employeePIN",
  "logTime" = EXCLUDED."logTime";

COMMIT;
