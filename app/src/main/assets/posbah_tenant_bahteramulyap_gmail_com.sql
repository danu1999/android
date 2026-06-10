--
-- PostgreSQL database dump
--

\restrict 7qLGNHsOPwnNFDVXfXFfzZtBkMOzjodI3A17S2Yb5JbgPUXOsb2mwljddyJVPRB

-- Dumped from database version 16.13 (Ubuntu 16.13-0ubuntu0.24.04.1)
-- Dumped by pg_dump version 16.13 (Ubuntu 16.13-0ubuntu0.24.04.1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: ActivityLog; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."ActivityLog" (
    id integer NOT NULL,
    action text NOT NULL,
    description text NOT NULL,
    date timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "employeeId" integer NOT NULL,
    "createdAt" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "appMode" text DEFAULT 'FNB'::text NOT NULL
);


--
-- Name: ActivityLog_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public."ActivityLog_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: ActivityLog_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public."ActivityLog_id_seq" OWNED BY public."ActivityLog".id;


--
-- Name: BmpAdmsDevice; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."BmpAdmsDevice" (
    id integer NOT NULL,
    "serialNumber" text NOT NULL,
    alias text,
    "lastActivity" timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: BmpAdmsDevice_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public."BmpAdmsDevice_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: BmpAdmsDevice_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public."BmpAdmsDevice_id_seq" OWNED BY public."BmpAdmsDevice".id;


--
-- Name: BmpAttendanceLog; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."BmpAttendanceLog" (
    id integer NOT NULL,
    "deviceSN" text,
    "employeePIN" text,
    "verifyType" integer NOT NULL,
    "verifyState" integer NOT NULL,
    "logTime" timestamp(3) without time zone NOT NULL,
    "checkOutTime" timestamp(3) without time zone,
    "workDate" timestamp(3) without time zone NOT NULL,
    "lateMinutes" integer DEFAULT 0 NOT NULL,
    alasan text,
    "createdAt" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: BmpAttendanceLog_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public."BmpAttendanceLog_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: BmpAttendanceLog_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public."BmpAttendanceLog_id_seq" OWNED BY public."BmpAttendanceLog".id;


--
-- Name: BmpBahanNono; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."BmpBahanNono" (
    id integer NOT NULL,
    tanggal timestamp(3) without time zone NOT NULL,
    nominal double precision DEFAULT 0 NOT NULL,
    notes text,
    tagihan text NOT NULL,
    "totalHarga" double precision DEFAULT 0 NOT NULL,
    "createdAt" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: BmpBahanNonoItem; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."BmpBahanNonoItem" (
    id integer NOT NULL,
    "bahanNonoId" integer NOT NULL,
    "jenisBahan" text NOT NULL,
    kuantitas double precision DEFAULT 0 NOT NULL,
    unit text DEFAULT 'Kg'::text NOT NULL,
    rate double precision DEFAULT 0 NOT NULL
);


--
-- Name: BmpBahanNonoItem_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public."BmpBahanNonoItem_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: BmpBahanNonoItem_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public."BmpBahanNonoItem_id_seq" OWNED BY public."BmpBahanNonoItem".id;


--
-- Name: BmpBahanNono_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public."BmpBahanNono_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: BmpBahanNono_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public."BmpBahanNono_id_seq" OWNED BY public."BmpBahanNono".id;


--
-- Name: BmpCashFlow; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."BmpCashFlow" (
    id integer NOT NULL,
    "transactionDate" timestamp(3) without time zone NOT NULL,
    "transactionType" text NOT NULL,
    description text NOT NULL,
    amount double precision DEFAULT 0 NOT NULL,
    "paymentRefId" integer,
    "createdAt" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: BmpCashFlow_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public."BmpCashFlow_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: BmpCashFlow_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public."BmpCashFlow_id_seq" OWNED BY public."BmpCashFlow".id;


--
-- Name: BmpClient; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."BmpClient" (
    id integer NOT NULL,
    "saldoTitipan" double precision DEFAULT 0 NOT NULL,
    "clientName" text NOT NULL,
    "addressLine1" text,
    "clientLogo" text,
    province text,
    "postalCode" text,
    "phoneNumber" text,
    "emailAddress" text,
    "taxNumber" text,
    "uniqueID" text,
    slug text,
    "createdAt" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: BmpClient_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public."BmpClient_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: BmpClient_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public."BmpClient_id_seq" OWNED BY public."BmpClient".id;


--
-- Name: BmpDeviceTenant; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."BmpDeviceTenant" (
    id integer NOT NULL,
    "serialNumber" text NOT NULL,
    "tenantId" text NOT NULL,
    "createdAt" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: BmpDeviceTenant_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public."BmpDeviceTenant_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: BmpDeviceTenant_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public."BmpDeviceTenant_id_seq" OWNED BY public."BmpDeviceTenant".id;


--
-- Name: BmpEmployee; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."BmpEmployee" (
    id integer NOT NULL,
    name text NOT NULL,
    "position" text,
    "salaryAmount" double precision NOT NULL,
    "isActive" boolean DEFAULT true NOT NULL,
    "fingerprintPIN" text,
    "createdAt" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: BmpEmployee_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public."BmpEmployee_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: BmpEmployee_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public."BmpEmployee_id_seq" OWNED BY public."BmpEmployee".id;


--
-- Name: BmpInvoice; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."BmpInvoice" (
    id integer NOT NULL,
    title text NOT NULL,
    number text NOT NULL,
    "dueDate" timestamp(3) without time zone,
    "paymentTerms" text DEFAULT '14 days'::text NOT NULL,
    status text DEFAULT 'DRAFT'::text NOT NULL,
    notes text,
    "clientId" integer,
    "uniqueID" text,
    slug text NOT NULL,
    "createdAt" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: BmpInvoicePayment; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."BmpInvoicePayment" (
    id integer NOT NULL,
    "invoiceId" integer NOT NULL,
    "paymentDate" timestamp(3) without time zone NOT NULL,
    "paymentAmount" double precision NOT NULL,
    "paymentMethod" text DEFAULT 'TRANSFER'::text NOT NULL,
    "createdAt" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: BmpInvoicePayment_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public."BmpInvoicePayment_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: BmpInvoicePayment_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public."BmpInvoicePayment_id_seq" OWNED BY public."BmpInvoicePayment".id;


--
-- Name: BmpInvoice_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public."BmpInvoice_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: BmpInvoice_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public."BmpInvoice_id_seq" OWNED BY public."BmpInvoice".id;


--
-- Name: BmpMachineBonusLog; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."BmpMachineBonusLog" (
    id integer NOT NULL,
    "employeeId" integer NOT NULL,
    "machineName" text NOT NULL,
    "shiftType" text NOT NULL,
    "bonusAmount" double precision NOT NULL,
    "jumlahPerolehan" integer DEFAULT 0 NOT NULL,
    date timestamp(3) without time zone NOT NULL,
    "createdAt" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: BmpMachineBonusLog_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public."BmpMachineBonusLog_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: BmpMachineBonusLog_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public."BmpMachineBonusLog_id_seq" OWNED BY public."BmpMachineBonusLog".id;


--
-- Name: BmpMasterProduct; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."BmpMasterProduct" (
    id integer NOT NULL,
    title text NOT NULL,
    description text,
    unit text DEFAULT 'Kg'::text NOT NULL,
    price double precision DEFAULT 0 NOT NULL,
    "beratGram" double precision DEFAULT 0 NOT NULL,
    "cycleTime" double precision DEFAULT 0 NOT NULL,
    cavity integer DEFAULT 1 NOT NULL,
    "rejectRate" double precision DEFAULT 0 NOT NULL,
    "uniqueID" text,
    slug text,
    "createdAt" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: BmpMasterProduct_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public."BmpMasterProduct_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: BmpMasterProduct_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public."BmpMasterProduct_id_seq" OWNED BY public."BmpMasterProduct".id;


--
-- Name: BmpPayroll; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."BmpPayroll" (
    id integer NOT NULL,
    "employeeId" integer NOT NULL,
    "paymentDate" timestamp(3) without time zone NOT NULL,
    amount double precision NOT NULL,
    "attendanceCount" integer DEFAULT 0 NOT NULL,
    "dailyRate" double precision NOT NULL,
    description text,
    "createdAt" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: BmpPayroll_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public."BmpPayroll_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: BmpPayroll_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public."BmpPayroll_id_seq" OWNED BY public."BmpPayroll".id;


--
-- Name: BmpPembayaran; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."BmpPembayaran" (
    id integer NOT NULL,
    "invoiceId" integer NOT NULL,
    "tanggalBayar" timestamp(3) without time zone NOT NULL,
    "jumlahBayar" double precision NOT NULL,
    keterangan text
);


--
-- Name: BmpPembayaran_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public."BmpPembayaran_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: BmpPembayaran_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public."BmpPembayaran_id_seq" OWNED BY public."BmpPembayaran".id;


--
-- Name: BmpPembelianBarang; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."BmpPembelianBarang" (
    id integer NOT NULL,
    supplier text NOT NULL,
    tanggal timestamp(3) without time zone NOT NULL,
    keterangan text,
    "totalHarga" double precision DEFAULT 0 NOT NULL,
    "caraBayar" text DEFAULT 'HUTANG'::text NOT NULL,
    "createdAt" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: BmpPembelianBarang_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public."BmpPembelianBarang_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: BmpPembelianBarang_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public."BmpPembelianBarang_id_seq" OWNED BY public."BmpPembelianBarang".id;


--
-- Name: BmpPembelianItem; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."BmpPembelianItem" (
    id integer NOT NULL,
    "pembelianId" integer NOT NULL,
    "namaBarang" text NOT NULL,
    "jumlahLusin" double precision DEFAULT 1 NOT NULL,
    kuantitas double precision DEFAULT 0 NOT NULL,
    unit text DEFAULT 'Pcs'::text NOT NULL,
    "hargaSatuan" double precision DEFAULT 0 NOT NULL
);


--
-- Name: BmpPembelianItem_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public."BmpPembelianItem_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: BmpPembelianItem_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public."BmpPembelianItem_id_seq" OWNED BY public."BmpPembelianItem".id;


--
-- Name: BmpProduct; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."BmpProduct" (
    id integer NOT NULL,
    "masterItemID" integer,
    title text NOT NULL,
    unit text DEFAULT 'pcs'::text NOT NULL,
    price double precision DEFAULT 0 NOT NULL,
    "jumlahLusin" double precision DEFAULT 1 NOT NULL,
    quantity double precision DEFAULT 0 NOT NULL,
    "isKhusus" boolean DEFAULT false NOT NULL,
    "hargaBeli" double precision DEFAULT 0 NOT NULL,
    currency text DEFAULT 'Rp'::text NOT NULL,
    "invoiceId" integer,
    "uniqueID" text,
    slug text,
    "createdAt" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: BmpProduct_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public."BmpProduct_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: BmpProduct_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public."BmpProduct_id_seq" OWNED BY public."BmpProduct".id;


--
-- Name: BmpSettings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."BmpSettings" (
    id integer NOT NULL,
    "clientName" text NOT NULL,
    "clientLogo" text,
    "addressLine1" text,
    province text,
    "postalCode" text,
    "phoneNumber" text,
    "emailAddress" text,
    "taxNumber" text,
    "listrikBulanan" double precision DEFAULT 30000000 NOT NULL,
    "jumlahMesin" integer DEFAULT 5 NOT NULL,
    "jumlahKaryawan" integer DEFAULT 19 NOT NULL,
    "gajiHarian" double precision DEFAULT 80000 NOT NULL,
    "hariKerjaSebulan" integer DEFAULT 26 NOT NULL,
    "biayaKarungPer1000" double precision DEFAULT 2100000 NOT NULL,
    "hoursPerDay" integer DEFAULT 24 NOT NULL,
    "uniqueID" text,
    slug text,
    "createdAt" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: BmpSettings_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public."BmpSettings_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: BmpSettings_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public."BmpSettings_id_seq" OWNED BY public."BmpSettings".id;


--
-- Name: Car; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."Car" (
    id integer NOT NULL,
    name text NOT NULL,
    "plateNumber" text NOT NULL,
    type text NOT NULL,
    "pricePerDay" double precision NOT NULL,
    status text DEFAULT 'AVAILABLE'::text NOT NULL,
    "createdAt" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: Car_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public."Car_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: Car_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public."Car_id_seq" OWNED BY public."Car".id;


--
-- Name: Customer; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."Customer" (
    id integer NOT NULL,
    name text NOT NULL,
    phone text,
    address text,
    "createdAt" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: Customer_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public."Customer_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: Customer_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public."Customer_id_seq" OWNED BY public."Customer".id;


--
-- Name: Employee; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."Employee" (
    id integer NOT NULL,
    name text NOT NULL,
    role text DEFAULT 'KASIR'::text NOT NULL,
    pin text NOT NULL,
    salary double precision DEFAULT 0 NOT NULL,
    "createdAt" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    email text,
    "outletId" integer
);


--
-- Name: Employee_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public."Employee_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: Employee_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public."Employee_id_seq" OWNED BY public."Employee".id;


--
-- Name: Finance; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."Finance" (
    id integer NOT NULL,
    type text NOT NULL,
    amount double precision NOT NULL,
    description text NOT NULL,
    date timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    status text DEFAULT 'PENDING'::text NOT NULL,
    "customerId" integer,
    "createdAt" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: Finance_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public."Finance_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: Finance_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public."Finance_id_seq" OWNED BY public."Finance".id;


--
-- Name: GoogleUser; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."GoogleUser" (
    id text NOT NULL,
    email text NOT NULL,
    "registeredAt" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "businessMode" text DEFAULT 'FNB'::text NOT NULL,
    "confirmToken" text,
    "confirmedAt" timestamp(3) without time zone,
    "demoExpiresAt" timestamp(3) without time zone,
    "isConfirmed" boolean DEFAULT false NOT NULL,
    "passwordHash" text,
    "userName" text,
    whatsapp text
);


--
-- Name: LaundryExpense; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."LaundryExpense" (
    id integer NOT NULL,
    kategori text NOT NULL,
    nominal double precision NOT NULL,
    keterangan text,
    tanggal timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "createdAt" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: LaundryExpense_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public."LaundryExpense_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: LaundryExpense_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public."LaundryExpense_id_seq" OWNED BY public."LaundryExpense".id;


--
-- Name: LaundryOrder; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."LaundryOrder" (
    id integer NOT NULL,
    "receiptNumber" text NOT NULL,
    "namaPelanggan" text NOT NULL,
    "noHp" text NOT NULL,
    "jenisLayanan" text NOT NULL,
    "jenisLaundry" text NOT NULL,
    "totalHarga" double precision NOT NULL,
    "statusBayar" text DEFAULT 'Belum Lunas'::text NOT NULL,
    status text DEFAULT 'Menunggu'::text NOT NULL,
    "tanggalMasuk" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "tanggalSelesai" timestamp(3) without time zone,
    selimut integer DEFAULT 0 NOT NULL,
    sprei integer DEFAULT 0 NOT NULL,
    boneka integer DEFAULT 0 NOT NULL,
    korden integer DEFAULT 0 NOT NULL,
    lokasi text,
    "employeeId" integer,
    "customerId" integer,
    "createdAt" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "outletId" integer
);


--
-- Name: LaundryOrder_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public."LaundryOrder_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: LaundryOrder_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public."LaundryOrder_id_seq" OWNED BY public."LaundryOrder".id;


--
-- Name: LaundryService; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."LaundryService" (
    id integer NOT NULL,
    kategori text NOT NULL,
    proses text NOT NULL,
    nama text NOT NULL,
    harga double precision NOT NULL,
    satuan text NOT NULL,
    waktu text NOT NULL,
    icon text DEFAULT '🧺'::text NOT NULL,
    "createdAt" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: LaundryService_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public."LaundryService_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: LaundryService_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public."LaundryService_id_seq" OWNED BY public."LaundryService".id;


--
-- Name: Outlet; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."Outlet" (
    id integer NOT NULL,
    name text NOT NULL,
    address text,
    phone text,
    "createdAt" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: Outlet_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public."Outlet_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: Outlet_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public."Outlet_id_seq" OWNED BY public."Outlet".id;


--
-- Name: PremiumUser; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."PremiumUser" (
    id text NOT NULL,
    email text NOT NULL,
    "passwordHash" text NOT NULL,
    name text NOT NULL,
    role text DEFAULT 'OWNER'::text NOT NULL,
    "registeredAt" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "tenantId" text,
    whatsapp text,
    "deletionScheduledAt" timestamp(3) without time zone,
    "lastPaymentConfirmedAt" timestamp(3) without time zone
);


--
-- Name: Product; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."Product" (
    id integer NOT NULL,
    name text NOT NULL,
    price double precision NOT NULL,
    "costPrice" double precision DEFAULT 0 NOT NULL,
    stock integer DEFAULT 0 NOT NULL,
    unit text DEFAULT 'pcs'::text NOT NULL,
    barcode text,
    category text DEFAULT 'Umum'::text NOT NULL,
    "wholesaleEnabled" boolean DEFAULT false NOT NULL,
    "wholesalePrices" text,
    variants text,
    image text,
    "createdAt" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "outletId" integer
);


--
-- Name: Product_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public."Product_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: Product_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public."Product_id_seq" OWNED BY public."Product".id;


--
-- Name: Promo; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."Promo" (
    id integer NOT NULL,
    name text NOT NULL,
    type text NOT NULL,
    value double precision NOT NULL,
    "isActive" boolean DEFAULT true NOT NULL,
    "createdAt" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: Promo_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public."Promo_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: Promo_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public."Promo_id_seq" OWNED BY public."Promo".id;


--
-- Name: PurchaseOrder; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."PurchaseOrder" (
    id integer NOT NULL,
    "supplierId" integer NOT NULL,
    date timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    status text DEFAULT 'DRAFT'::text NOT NULL,
    notes text,
    total double precision DEFAULT 0 NOT NULL,
    "createdAt" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: PurchaseOrderItem; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."PurchaseOrderItem" (
    id integer NOT NULL,
    "purchaseOrderId" integer NOT NULL,
    "productId" integer NOT NULL,
    quantity integer NOT NULL,
    "costPrice" double precision NOT NULL
);


--
-- Name: PurchaseOrderItem_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public."PurchaseOrderItem_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: PurchaseOrderItem_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public."PurchaseOrderItem_id_seq" OWNED BY public."PurchaseOrderItem".id;


--
-- Name: PurchaseOrder_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public."PurchaseOrder_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: PurchaseOrder_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public."PurchaseOrder_id_seq" OWNED BY public."PurchaseOrder".id;


--
-- Name: Rental; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."Rental" (
    id integer NOT NULL,
    "carId" integer NOT NULL,
    "customerId" integer,
    "customerName" text NOT NULL,
    "startDate" timestamp(3) without time zone NOT NULL,
    "endDate" timestamp(3) without time zone NOT NULL,
    "totalPrice" double precision NOT NULL,
    status text DEFAULT 'ACTIVE'::text NOT NULL,
    "actualReturnDate" timestamp(3) without time zone,
    "lateFee" double precision DEFAULT 0 NOT NULL,
    "employeeId" integer NOT NULL,
    "identityText" text,
    "createdAt" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: Rental_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public."Rental_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: Rental_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public."Rental_id_seq" OWNED BY public."Rental".id;


--
-- Name: Supplier; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."Supplier" (
    id integer NOT NULL,
    name text NOT NULL,
    phone text,
    address text,
    notes text,
    "createdAt" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: Supplier_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public."Supplier_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: Supplier_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public."Supplier_id_seq" OWNED BY public."Supplier".id;


--
-- Name: TenantLimit; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."TenantLimit" (
    "tenantId" text NOT NULL,
    "limit" integer DEFAULT 4 NOT NULL,
    "createdAt" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL
);


--
-- Name: Transaction; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."Transaction" (
    id integer NOT NULL,
    "receiptNumber" text NOT NULL,
    date timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    subtotal double precision DEFAULT 0 NOT NULL,
    "discountType" text,
    "discountInput" double precision DEFAULT 0 NOT NULL,
    "discountAmt" double precision DEFAULT 0 NOT NULL,
    total double precision NOT NULL,
    discount double precision DEFAULT 0 NOT NULL,
    "paymentMethod" text NOT NULL,
    "amountPaid" double precision,
    change double precision,
    status text DEFAULT 'COMPLETED'::text NOT NULL,
    type text DEFAULT 'SALES'::text NOT NULL,
    "orderStatus" text,
    "dpAmount" double precision DEFAULT 0 NOT NULL,
    "deliveryDate" timestamp(3) without time zone,
    "employeeId" integer NOT NULL,
    "customerId" integer,
    "customerName" text,
    "queueNumber" integer,
    notes text,
    "createdAt" timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updatedAt" timestamp(3) without time zone NOT NULL,
    "outletId" integer
);


--
-- Name: TransactionItem; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."TransactionItem" (
    id integer NOT NULL,
    "transactionId" integer NOT NULL,
    "productId" integer NOT NULL,
    "variantId" integer,
    "variantName" text,
    quantity integer NOT NULL,
    price double precision NOT NULL,
    "costPrice" double precision DEFAULT 0 NOT NULL,
    discount double precision DEFAULT 0 NOT NULL,
    note text
);


--
-- Name: TransactionItem_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public."TransactionItem_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: TransactionItem_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public."TransactionItem_id_seq" OWNED BY public."TransactionItem".id;


--
-- Name: Transaction_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public."Transaction_id_seq"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: Transaction_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public."Transaction_id_seq" OWNED BY public."Transaction".id;


--
-- Name: ActivityLog id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."ActivityLog" ALTER COLUMN id SET DEFAULT nextval('public."ActivityLog_id_seq"'::regclass);


--
-- Name: BmpAdmsDevice id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BmpAdmsDevice" ALTER COLUMN id SET DEFAULT nextval('public."BmpAdmsDevice_id_seq"'::regclass);


--
-- Name: BmpAttendanceLog id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BmpAttendanceLog" ALTER COLUMN id SET DEFAULT nextval('public."BmpAttendanceLog_id_seq"'::regclass);


--
-- Name: BmpBahanNono id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BmpBahanNono" ALTER COLUMN id SET DEFAULT nextval('public."BmpBahanNono_id_seq"'::regclass);


--
-- Name: BmpBahanNonoItem id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BmpBahanNonoItem" ALTER COLUMN id SET DEFAULT nextval('public."BmpBahanNonoItem_id_seq"'::regclass);


--
-- Name: BmpCashFlow id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BmpCashFlow" ALTER COLUMN id SET DEFAULT nextval('public."BmpCashFlow_id_seq"'::regclass);


--
-- Name: BmpClient id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BmpClient" ALTER COLUMN id SET DEFAULT nextval('public."BmpClient_id_seq"'::regclass);


--
-- Name: BmpDeviceTenant id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BmpDeviceTenant" ALTER COLUMN id SET DEFAULT nextval('public."BmpDeviceTenant_id_seq"'::regclass);


--
-- Name: BmpEmployee id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BmpEmployee" ALTER COLUMN id SET DEFAULT nextval('public."BmpEmployee_id_seq"'::regclass);


--
-- Name: BmpInvoice id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BmpInvoice" ALTER COLUMN id SET DEFAULT nextval('public."BmpInvoice_id_seq"'::regclass);


--
-- Name: BmpInvoicePayment id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BmpInvoicePayment" ALTER COLUMN id SET DEFAULT nextval('public."BmpInvoicePayment_id_seq"'::regclass);


--
-- Name: BmpMachineBonusLog id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BmpMachineBonusLog" ALTER COLUMN id SET DEFAULT nextval('public."BmpMachineBonusLog_id_seq"'::regclass);


--
-- Name: BmpMasterProduct id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BmpMasterProduct" ALTER COLUMN id SET DEFAULT nextval('public."BmpMasterProduct_id_seq"'::regclass);


--
-- Name: BmpPayroll id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BmpPayroll" ALTER COLUMN id SET DEFAULT nextval('public."BmpPayroll_id_seq"'::regclass);


--
-- Name: BmpPembayaran id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BmpPembayaran" ALTER COLUMN id SET DEFAULT nextval('public."BmpPembayaran_id_seq"'::regclass);


--
-- Name: BmpPembelianBarang id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BmpPembelianBarang" ALTER COLUMN id SET DEFAULT nextval('public."BmpPembelianBarang_id_seq"'::regclass);


--
-- Name: BmpPembelianItem id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BmpPembelianItem" ALTER COLUMN id SET DEFAULT nextval('public."BmpPembelianItem_id_seq"'::regclass);


--
-- Name: BmpProduct id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BmpProduct" ALTER COLUMN id SET DEFAULT nextval('public."BmpProduct_id_seq"'::regclass);


--
-- Name: BmpSettings id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BmpSettings" ALTER COLUMN id SET DEFAULT nextval('public."BmpSettings_id_seq"'::regclass);


--
-- Name: Car id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Car" ALTER COLUMN id SET DEFAULT nextval('public."Car_id_seq"'::regclass);


--
-- Name: Customer id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Customer" ALTER COLUMN id SET DEFAULT nextval('public."Customer_id_seq"'::regclass);


--
-- Name: Employee id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Employee" ALTER COLUMN id SET DEFAULT nextval('public."Employee_id_seq"'::regclass);


--
-- Name: Finance id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Finance" ALTER COLUMN id SET DEFAULT nextval('public."Finance_id_seq"'::regclass);


--
-- Name: LaundryExpense id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."LaundryExpense" ALTER COLUMN id SET DEFAULT nextval('public."LaundryExpense_id_seq"'::regclass);


--
-- Name: LaundryOrder id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."LaundryOrder" ALTER COLUMN id SET DEFAULT nextval('public."LaundryOrder_id_seq"'::regclass);


--
-- Name: LaundryService id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."LaundryService" ALTER COLUMN id SET DEFAULT nextval('public."LaundryService_id_seq"'::regclass);


--
-- Name: Outlet id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Outlet" ALTER COLUMN id SET DEFAULT nextval('public."Outlet_id_seq"'::regclass);


--
-- Name: Product id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Product" ALTER COLUMN id SET DEFAULT nextval('public."Product_id_seq"'::regclass);


--
-- Name: Promo id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Promo" ALTER COLUMN id SET DEFAULT nextval('public."Promo_id_seq"'::regclass);


--
-- Name: PurchaseOrder id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."PurchaseOrder" ALTER COLUMN id SET DEFAULT nextval('public."PurchaseOrder_id_seq"'::regclass);


--
-- Name: PurchaseOrderItem id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."PurchaseOrderItem" ALTER COLUMN id SET DEFAULT nextval('public."PurchaseOrderItem_id_seq"'::regclass);


--
-- Name: Rental id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Rental" ALTER COLUMN id SET DEFAULT nextval('public."Rental_id_seq"'::regclass);


--
-- Name: Supplier id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Supplier" ALTER COLUMN id SET DEFAULT nextval('public."Supplier_id_seq"'::regclass);


--
-- Name: Transaction id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Transaction" ALTER COLUMN id SET DEFAULT nextval('public."Transaction_id_seq"'::regclass);


--
-- Name: TransactionItem id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."TransactionItem" ALTER COLUMN id SET DEFAULT nextval('public."TransactionItem_id_seq"'::regclass);


--
-- Data for Name: ActivityLog; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."ActivityLog" (id, action, description, date, "employeeId", "createdAt", "appMode") FROM stdin;
1	CREATE_TRANSACTION	Membuat transaksi baru INV-1780545470629 senilai Rp 10.000	2026-06-04 03:57:50.68	1	2026-06-04 03:57:50.68	FNB
2	CREATE_TRANSACTION	Membuat transaksi baru INV-1780545677396 senilai Rp 150.000	2026-06-04 04:01:17.42	1	2026-06-04 04:01:17.42	FNB
3	CREATE_TRANSACTION	Membuat transaksi baru INV-1780545842233 senilai Rp 98.000	2026-06-04 04:04:02.277	1	2026-06-04 04:04:02.277	FNB
4	CREATE_LAUNDRY_SERVICE	Menambahkan layanan laundry baru: Klioan - Reguler (Rp 5.000)	2026-06-04 04:08:25.993	1	2026-06-04 04:08:25.993	LAUNDRY
5	CREATE_LAUNDRY_ORDER	Membuat pesanan laundry baru INV-LND-1780546132654 untuk jJ senilai Rp 30.000	2026-06-04 04:08:52.666	1	2026-06-04 04:08:52.666	LAUNDRY
6	UPDATE_LAUNDRY_PAYMENT	Mengubah pembayaran pesanan laundry INV-LND-1780546132654 menjadi: Lunas	2026-06-04 04:09:17.137	1	2026-06-04 04:09:17.137	LAUNDRY
7	UPDATE_LAUNDRY_STATUS	Mengubah status pesanan laundry INV-LND-1780546132654 menjadi: Selesai	2026-06-04 04:09:31.393	1	2026-06-04 04:09:31.393	LAUNDRY
8	UPDATE_LAUNDRY_STATUS	Mengubah status pesanan laundry INV-LND-1780546132654 menjadi: Diambil	2026-06-04 04:09:32.846	1	2026-06-04 04:09:32.846	LAUNDRY
9	UPDATE_LAUNDRY_STATUS	Mengubah status pesanan laundry INV-LND-1780546132654 menjadi: Selesai	2026-06-04 04:09:33.83	1	2026-06-04 04:09:33.83	LAUNDRY
10	CREATE_OUTLET	Membuat outlet baru: gedangan - kwangsan	2026-06-07 18:10:25.843	1	2026-06-07 18:10:25.843	FNB
11	CREATE_EMPLOYEE	Menambahkan karyawan baru syerli (Role: ADMIN)	2026-06-07 18:12:12.301	1	2026-06-07 18:12:12.301	FNB
12	UPDATE_OUTLET	Mengubah outlet: surabaya - rs. soetomo	2026-06-07 18:14:02.759	1	2026-06-07 18:14:02.759	FNB
13	CREATE_OUTLET	Membuat outlet baru: sidoarjo - RSUD	2026-06-07 18:14:30.524	1	2026-06-07 18:14:30.524	FNB
14	UPDATE_EMPLOYEE	Mengubah data karyawan syerli (Role: ADMIN)	2026-06-07 18:18:36.08	1	2026-06-07 18:18:36.08	FNB
\.


--
-- Data for Name: BmpAdmsDevice; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."BmpAdmsDevice" (id, "serialNumber", alias, "lastActivity", "createdAt") FROM stdin;
1	NHZ4254800403		2026-06-08 16:57:52.198	2026-05-12 14:41:04.402
\.


--
-- Data for Name: BmpAttendanceLog; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."BmpAttendanceLog" (id, "deviceSN", "employeePIN", "verifyType", "verifyState", "logTime", "checkOutTime", "workDate", "lateMinutes", alasan, "createdAt") FROM stdin;
269	NHZ4254800403	18	1	0	2026-05-29 06:57:47.732	\N	2026-05-28 07:00:00	0		2026-05-29 06:57:47.741
272	NHZ4254800403	3	1	1	2026-05-29 14:54:53.361	2026-05-29 23:25:06.634	2026-05-29 07:00:00	0		2026-05-29 14:54:53.368
270	NHZ4254800403	8	1	1	2026-05-29 14:51:57.984	2026-05-29 23:25:14.715	2026-05-29 07:00:00	0		2026-05-29 14:51:57.991
271	NHZ4254800403	7	1	1	2026-05-29 14:52:10.999	2026-05-29 23:25:23.454	2026-05-29 07:00:00	0		2026-05-29 14:52:11.006
273	NHZ4254800403	5	1	1	2026-05-29 14:55:00.432	2026-05-29 23:25:35.226	2026-05-29 07:00:00	0		2026-05-29 14:55:00.441
277	NHZ4254800403	18	1	1	2026-05-29 06:56:00	2026-05-29 15:10:00	2026-05-29 07:00:00	0		2026-05-29 15:13:21.436
278	NHZ4254800403	17	1	1	2026-05-29 06:52:00	2026-05-29 15:02:00	2026-05-29 07:00:00	0		2026-05-29 15:16:48.52
288		14	0	1	2026-05-30 06:56:00	2026-05-31 06:55:04.754	2026-05-30 07:00:00	0		2026-05-30 09:56:15.789
287		17	0	1	2026-05-30 06:55:00	2026-05-31 06:58:14.014	2026-05-30 07:00:00	0		2026-05-30 09:55:38.368
296	NHZ4254800403	15	1	1	2026-05-30 15:18:48.409	2026-05-31 06:56:07.935	2026-05-30 07:00:00	18		2026-05-30 15:18:48.42
289		18	0	1	2026-05-30 06:56:00	2026-05-31 06:57:32.229	2026-05-30 07:00:00	0		2026-05-30 09:56:56.65
305	NHZ4254800403	12	1	1	2026-05-31 14:51:45.126	2026-05-31 22:32:37.03	2026-05-31 07:00:00	0		2026-05-31 14:51:45.376
304	NHZ4254800403	7	1	1	2026-05-31 14:51:26.226	2026-06-01 06:56:21.157	2026-05-31 07:00:00	0		2026-05-31 14:51:26.475
315	NHZ4254800403	1	1	0	2026-06-01 11:41:09.294	\N	2026-05-31 07:00:00	0		2026-06-01 11:41:09.307
316		3	0	0	2026-06-01 07:00:00	\N	2026-06-01 07:00:00	0		2026-06-01 11:46:58.563
317		5	0	0	2026-06-01 06:58:00	\N	2026-06-01 07:00:00	0		2026-06-01 11:47:17.363
318		4	0	0	2026-06-01 06:59:00	\N	2026-06-01 07:00:00	0		2026-06-01 11:47:29.906
319		7	0	0	2026-06-01 06:47:00	\N	2026-06-01 07:00:00	0		2026-06-01 11:47:40.435
322		8	0	0	2026-06-01 06:44:00	\N	2026-06-01 07:00:00	0		2026-06-01 11:48:18.934
323		19	0	0	2026-06-01 07:00:00	\N	2026-06-01 07:00:00	0		2026-06-01 11:48:36.772
335	NHZ4254800403	3	1	0	2026-06-07 23:31:00	\N	2026-06-08 07:00:00	0	\N	2026-06-08 17:09:09.426
336	NHZ4254800403	4	1	0	2026-06-08 00:01:00	\N	2026-06-08 07:00:00	1	\N	2026-06-08 17:09:09.426
337	NHZ4254800403	5	1	0	2026-06-07 23:45:00	\N	2026-06-08 07:00:00	0	\N	2026-06-08 17:09:09.426
338	NHZ4254800403	7	1	0	2026-06-08 00:00:00	\N	2026-06-08 07:00:00	0	\N	2026-06-08 17:09:09.426
339	NHZ4254800403	8	1	0	2026-06-07 23:53:00	\N	2026-06-08 07:00:00	0	\N	2026-06-08 17:09:09.426
340	NHZ4254800403	9	1	0	2026-06-07 23:56:00	\N	2026-06-08 07:00:00	0	\N	2026-06-08 17:09:09.426
341	NHZ4254800403	19	1	0	2026-06-08 00:03:00	\N	2026-06-08 07:00:00	3	\N	2026-06-08 17:09:09.426
259	NHZ4254800403	10	1	1	2026-05-28 22:52:25	2026-05-29 07:03:10.818	2026-05-28 07:00:00	0		2026-05-28 22:52:33.361
257	NHZ4254800403	11	1	1	2026-05-28 22:51:50	2026-05-29 07:03:19.666	2026-05-28 07:00:00	0		2026-05-28 22:51:58.339
262	NHZ4254800403	12	1	1	2026-05-28 22:56:29	2026-05-29 07:03:59.895	2026-05-28 07:00:00	0		2026-05-28 22:56:37.545
285	NHZ4254800403	13	1	1	2026-05-29 22:59:08.417	2026-05-30 06:59:59.918	2026-05-29 07:00:00	0		2026-05-29 22:59:08.424
283	NHZ4254800403	20	1	1	2026-05-29 22:56:17.443	2026-05-30 07:00:07.987	2026-05-29 07:00:00	0		2026-05-29 22:56:17.449
281	NHZ4254800403	10	1	1	2026-05-29 22:51:41.738	2026-05-30 07:00:16.722	2026-05-29 07:00:00	0		2026-05-29 22:51:41.744
280	NHZ4254800403	11	1	1	2026-05-29 22:51:33.28	2026-05-30 07:00:22.859	2026-05-29 07:00:00	0		2026-05-29 22:51:33.288
284	NHZ4254800403	12	1	1	2026-05-29 22:56:27.42	2026-05-30 07:00:30.888	2026-05-29 07:00:00	0		2026-05-29 22:56:27.426
252	NHZ4254800403	15	1	1	2026-05-28 06:52:00	2026-05-28 15:03:00	2026-05-28 07:00:00	0		2026-05-28 15:21:07.129
255	NHZ4254800403	14	1	1	2026-05-28 06:52:00	2026-05-28 15:22:00	2026-05-28 07:00:00	0		2026-05-28 15:23:13.307
263	NHZ4254800403	13	1	1	2026-05-28 07:02:00	2026-05-28 15:02:00	2026-05-28 07:00:00	2		2026-05-28 22:59:02.763
253	NHZ4254800403	18	1	1	2026-05-28 06:22:00	2026-05-28 15:09:00	2026-05-28 07:00:00	0		2026-05-28 15:22:55.673
282	NHZ4254800403	6	1	1	2026-05-29 22:53:00	2026-05-30 07:04:00	2026-05-29 07:00:00	0		2026-05-29 22:53:04.874
275	NHZ4254800403	14	1	1	2026-05-29 06:52:00	2026-05-29 15:04:00	2026-05-29 07:00:00	0		2026-05-29 15:06:29.153
276	NHZ4254800403	16	1	1	2026-05-29 06:55:00	2026-05-29 15:09:00	2026-05-29 07:00:00	0		2026-05-29 15:12:55.469
258	NHZ4254800403	11	1	0	2026-05-28 22:52:13	\N	2026-05-28 07:00:00	0		2026-05-28 22:52:21.255
291	NHZ4254800403	22	1	1	2026-05-30 06:53:00	2026-05-31 06:57:25.074	2026-05-30 07:00:00	0		2026-05-30 12:53:47.998
302	NHZ4254800403	13	1	1	2026-05-30 22:59:19.192	2026-05-31 06:59:13.181	2026-05-30 07:00:00	0		2026-05-30 22:59:19.442
298	NHZ4254800403	11	1	1	2026-05-30 22:54:04.12	2026-05-31 06:59:34.999	2026-05-30 07:00:00	0		2026-05-30 22:54:04.37
300	NHZ4254800403	20	1	1	2026-05-30 22:58:00.3	2026-05-31 07:00:17.201	2026-05-30 07:00:00	0		2026-05-30 22:58:00.549
299	NHZ4254800403	10	1	1	2026-05-30 22:54:27.916	2026-05-31 07:00:41.006	2026-05-30 07:00:00	0		2026-05-30 22:54:28.166
301	NHZ4254800403	12	1	1	2026-05-30 22:58:15.216	2026-05-31 07:01:13.826	2026-05-30 07:00:00	0		2026-05-30 22:58:15.467
297	NHZ4254800403	6	1	1	2026-05-30 22:52:28.074	2026-05-31 07:01:35.865	2026-05-30 07:00:00	0		2026-05-30 22:52:28.449
290		16	0	1	2026-05-30 06:54:00	2026-05-31 07:03:13.839	2026-05-30 07:00:00	0		2026-05-30 12:49:49.71
307	NHZ4254800403	15	1	1	2026-05-31 15:08:21.431	2026-05-31 15:08:24.425	2026-05-31 07:00:00	8		2026-05-31 15:08:21.681
308	NHZ4254800403	14	1	0	2026-05-31 15:08:50.551	\N	2026-05-31 07:00:00	8		2026-05-31 15:08:50.801
303	NHZ4254800403	8	1	1	2026-05-31 14:51:13.176	2026-06-01 06:51:44.479	2026-05-31 07:00:00	0		2026-05-31 14:51:13.426
306	NHZ4254800403	3	1	1	2026-05-31 14:55:44.392	2026-06-01 06:56:10.029	2026-05-31 07:00:00	0		2026-05-31 14:55:44.644
260	NHZ4254800403	6	1	1	2026-05-28 22:52:41	2026-05-29 07:02:20.841	2026-05-28 07:00:00	0		2026-05-28 22:52:49.309
261	NHZ4254800403	20	1	1	2026-05-28 22:55:23	2026-05-29 07:02:53.989	2026-05-28 07:00:00	0		2026-05-28 22:55:31.395
247	NHZ4254800403	8	1	1	2026-05-28 14:51:42	2026-05-28 23:13:17	2026-05-28 07:00:00	0		2026-05-28 14:51:50.362
250	NHZ4254800403	3	1	1	2026-05-28 14:58:51	2026-05-28 23:13:22	2026-05-28 07:00:00	0		2026-05-28 14:58:57.639
249	NHZ4254800403	4	1	1	2026-05-28 14:56:37	2026-05-28 23:13:36	2026-05-28 07:00:00	0		2026-05-28 14:56:44.588
248	NHZ4254800403	7	1	1	2026-05-28 14:52:17	2026-05-28 23:13:47	2026-05-28 07:00:00	0		2026-05-28 14:52:24.333
251	NHZ4254800403	17	1	1	2026-05-28 06:54:00	2026-05-28 15:20:00	2026-05-28 07:00:00	0		2026-05-28 15:20:57.044
256	NHZ4254800403	16	1	1	2026-05-28 07:00:00	2026-05-28 15:00:00	2026-05-28 07:00:00	0		2026-05-28 15:48:55.569
274	NHZ4254800403	15	1	1	2026-05-29 06:56:00	2026-05-29 15:04:00	2026-05-29 07:00:00	0		2026-05-29 15:05:54.079
295	NHZ4254800403	5	1	1	2026-05-30 15:02:17.344	2026-05-30 23:07:50.462	2026-05-30 07:00:00	2		2026-05-30 15:02:17.353
294	NHZ4254800403	3	1	1	2026-05-30 14:56:28.132	2026-05-30 23:09:49.8	2026-05-30 07:00:00	0		2026-05-30 14:56:28.145
293	NHZ4254800403	4	1	1	2026-05-30 14:55:08.073	2026-05-30 23:10:19.627	2026-05-30 07:00:00	0		2026-05-30 14:55:08.082
292	NHZ4254800403	7	1	1	2026-05-30 14:51:46.473	2026-05-30 23:10:29.789	2026-05-30 07:00:00	0		2026-05-30 14:51:46.488
309	NHZ4254800403	22	1	0	2026-05-31 15:09:01.569	\N	2026-05-31 07:00:00	9		2026-05-31 15:09:01.818
310	NHZ4254800403	17	1	0	2026-05-31 15:09:42.61	\N	2026-05-31 07:00:00	9		2026-05-31 15:09:42.86
311	NHZ4254800403	18	1	1	2026-05-31 15:09:59.488	2026-05-31 15:10:02.19	2026-05-31 07:00:00	9		2026-05-31 15:09:59.738
325	NHZ4254800403	14	1	0	2026-06-08 08:53:47	\N	2026-06-08 07:00:00	0		2026-06-08 16:57:56.695
326	NHZ4254800403	22	1	0	2026-06-08 08:55:47	\N	2026-06-08 07:00:00	0		2026-06-08 16:57:56.702
327	NHZ4254800403	17	1	0	2026-06-08 08:58:22	\N	2026-06-08 07:00:00	0		2026-06-08 16:57:56.707
328	NHZ4254800403	16	1	0	2026-06-08 08:58:33	\N	2026-06-08 07:00:00	0		2026-06-08 16:57:56.711
329	NHZ4254800403	11	1	0	2026-06-08 08:59:04	\N	2026-06-08 07:00:00	0		2026-06-08 16:57:56.716
330	NHZ4254800403	12	1	0	2026-06-08 08:59:28	\N	2026-06-08 07:00:00	0		2026-06-08 16:57:56.72
331	NHZ4254800403	20	1	0	2026-06-08 08:59:39	\N	2026-06-08 07:00:00	0		2026-06-08 16:57:56.724
333	NHZ4254800403	10	1	0	2026-06-08 09:00:23	\N	2026-06-08 07:00:00	0		2026-06-08 16:57:56.731
334	NHZ4254800403	6	1	0	2026-06-08 09:00:29	\N	2026-06-08 07:00:00	0		2026-06-08 16:57:56.74
254	NHZ4254800403	18	1	1	2026-05-28 15:22:54	\N	2026-05-28 07:00:00	0		2026-05-28 15:23:01.508
312	NHZ4254800403	16	1	0	2026-05-31 15:10:11.12	\N	2026-05-31 07:00:00	10		2026-05-31 15:10:11.369
313	NHZ4254800403	19	1	0	2026-06-01 06:57:17.821	\N	2026-05-31 07:00:00	0		2026-06-01 06:57:17.83
314	NHZ4254800403	4	1	0	2026-06-01 07:09:54.654	\N	2026-05-31 07:00:00	9		2026-06-01 07:09:54.663
324	NHZ4254800403	8	1	0	2026-06-08 08:51:37	\N	2026-06-08 07:00:00	0		2026-06-08 16:57:56.689
332	NHZ4254800403	13	1	0	2026-06-08 08:59:46	\N	2026-06-08 07:00:00	0		2026-06-08 16:57:56.728
\.


--
-- Data for Name: BmpBahanNono; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."BmpBahanNono" (id, tanggal, nominal, notes, tagihan, "totalHarga", "createdAt") FROM stdin;
27	2026-04-10 07:00:00	0	setoran pagi	uploads/tagihan_nono/17761454502722252457073246195439_guzcr9	179440200	2026-04-14 12:44:47.992
28	2026-04-10 07:00:00	0	setoran pagi	uploads/tagihan_nono/17761455335001719376742123408505_r2beku	17632000	2026-04-14 12:45:59.594
34	2026-04-14 07:00:00	20000000	TF	uploads/tagihan_nono/17761464197856947976188751566836_eu43mv	0	2026-04-14 13:00:36.464
35	2026-04-10 07:00:00	20000000	TF	uploads/tagihan_nono/17761466431441880580606829538504_kyu7c3	0	2026-04-14 13:04:13.839
36	2026-04-13 07:00:00	50000000	TF	uploads/tagihan_nono/17761466834582792451293268839747_vavvbj	0	2026-04-14 13:04:53.406
37	2026-04-14 07:00:00	0	setoran sore	uploads/tagihan_nono/17761566957829027145017300715374_ri2mjn	18320000	2026-04-14 15:51:57.321
38	2026-04-15 07:00:00	0	setoran sore	uploads/tagihan_nono/17762390918807468112277313179839_roa3f6	18000000	2026-04-15 14:45:38.263
39	2026-04-16 07:00:00	0	setoran sore	uploads/tagihan_nono/17763268993418295397076887277854_fca9ge	18990400	2026-04-16 15:08:01.534
40	2026-04-16 07:00:00	40000000	TF BRI	uploads/tagihan_nono/IMG-20260416-WA0022_rc80m1	0	2026-04-16 18:43:06.213
41	2026-04-17 07:00:00	0	setoran sore	uploads/tagihan_nono/17764139289395154966230074889545_g5jwp6	20608900	2026-04-17 15:19:36.181
42	2026-04-20 07:00:00	30000000	TF	uploads/tagihan_nono/IMG-20260420-WA0012_keul4m	0	2026-04-20 19:11:18.969
43	2026-04-20 07:00:00	0	setoran sore	uploads/tagihan_nono/IMG_20260421_094629_hn571h	16500400	2026-04-21 09:48:16.837
44	2026-04-21 07:00:00	30000000	Tunai 	uploads/tagihan_nono/17767397404302738797771452715500_fwqow8	0	2026-04-21 09:49:29.604
45	2026-04-21 07:00:00	0	setoran sore	uploads/tagihan_nono/17767595408294200217397161003955_xnxj92	22874800	2026-04-21 15:19:36.562
46	2026-04-22 07:00:00	0	setoran sore	uploads/tagihan_nono/IMG-20260423-WA0005_iuxl1a	8300000	2026-04-22 15:49:09.567
47	2026-04-23 07:00:00	25000000	Tunai		0	2026-04-23 17:17:51.55
48	2026-04-24 07:00:00	40000000	TF BRI	uploads/tagihan_nono/IMG-20260424-WA0049_qqcodz	0	2026-04-24 11:16:45.718
49	2026-04-24 07:00:00	0	setoran sore	uploads/tagihan_nono/IMG_20260424_155131_rrioso	16467200	2026-04-24 18:01:57.423
50	2026-04-23 07:00:00	0	setoran sore	uploads/tagihan_nono/IMG-20260424-WA0067_trei8a	14491800	2026-04-24 18:02:48.074
51	2026-04-25 07:00:00	0	setoran sore	uploads/tagihan_nono/IMG-20260425-WA0027_gfrxxj	29904900	2026-04-25 14:49:28.48
52	2026-04-25 07:00:00	2000000	TF	uploads/tagihan_nono/IMG-20260425-WA0036_qzpvuu	0	2026-04-25 16:42:52.561
53	2026-04-27 07:00:00	0	Setoran Sore	uploads/tagihan_nono/IMG_20260427_145256_lhaqbk	14201300	2026-04-27 14:58:47.518
54	2026-04-28 07:00:00	35000000	TF BRI	uploads/tagihan_nono/IMG-20260428-WA0064_sh34o2	0	2026-04-28 12:11:09.209
25	2026-04-13 07:00:00	0		uploads/tagihan_nono/17761443724868552854407591220813_b9jlcj	21200000	2026-04-14 12:26:37.714
63	2026-05-01 07:00:00	0	Setoran Sore	https://res.cloudinary.com/dkkbizenf/image/upload/v1777865016/tagihan_nono/oruidmarofdscwr9irnb.jpg	12765400	2026-05-04 10:24:28.155
64	2026-04-30 07:00:00	0	Setoran Sore	https://res.cloudinary.com/dkkbizenf/image/upload/v1777865145/tagihan_nono/h0gawmv6qvojdpqbgvaa.jpg	13296600	2026-05-04 10:26:00.346
65	2026-04-29 07:00:00	0	setoran sore	https://res.cloudinary.com/dkkbizenf/image/upload/v1777865231/tagihan_nono/qfnsoxfzyhbtynkemxz8.jpg	18608600	2026-05-04 10:27:40.974
66	2026-04-29 07:00:00	40000000	TF BRI	https://res.cloudinary.com/dkkbizenf/image/upload/v1777865363/tagihan_nono/vho6xp24i5ea0gxqtjhk.jpg	0	2026-05-04 10:29:45.083
67	2026-05-05 07:00:00	0		https://res.cloudinary.com/dkkbizenf/image/upload/v1777988254/tagihan_nono/dfkma21izc64zzrye5ce.jpg	19499000	2026-05-05 20:37:48.886
74	2026-05-14 07:00:00	0	setoran sore	https://res.cloudinary.com/dkkbizenf/image/upload/v1778745773/tagihan_nono/ie3bkl6cgf5jmmvnrq1a.jpg	19329000	2026-05-14 15:03:12.906
61	2026-05-04 07:00:00	30000000	TF BCA	https://res.cloudinary.com/dkkbizenf/image/upload/v1777864490/tagihan_nono/gqpvmvadaetezls7gyh3.jpg	0	2026-05-04 10:14:59.151
68	2026-05-05 07:00:00	30000000	TF BCA	https://res.cloudinary.com/dkkbizenf/image/upload/v1777988311/tagihan_nono/hwfp5q9syxqpr5kkfmro.jpg	0	2026-05-05 20:38:46.91
69	2026-05-12 07:00:00	0	setoran sore	https://res.cloudinary.com/dkkbizenf/image/upload/v1778570447/tagihan_nono/wpu0i86vu5s37pwaatuz.jpg	20111000	2026-05-12 14:21:01.32
70	2026-05-12 07:00:00	0	setoran sore	https://res.cloudinary.com/dkkbizenf/image/upload/v1778583198/tagihan_nono/hi2wbvfyl2fdc2otyezw.jpg	20611500	2026-05-12 17:53:22.006
71	2026-05-13 07:00:00	0	setoran sore	https://res.cloudinary.com/dkkbizenf/image/upload/v1778660560/tagihan_nono/ts9bsemoaafcnu2msmyo.jpg	25823000	2026-05-13 15:22:55.675
72	2026-05-12 07:00:00	5000000	Tunai		0	2026-05-13 15:24:35.123
73	2026-05-08 07:00:00	0	setoran sore	https://res.cloudinary.com/dkkbizenf/image/upload/v1778745388/tagihan_nono/brdpbojnr1lmbd6ygua5.jpg	18649000	2026-05-14 14:56:44.344
75	2026-05-14 07:00:00	30000000	TF BCA	https://res.cloudinary.com/dkkbizenf/image/upload/v1778762536/tagihan_nono/z96rlbetvlac5ddxzhpa.jpg	0	2026-05-14 19:42:29.103
76	2026-05-15 07:00:00	0	setoran sore	https://res.cloudinary.com/dkkbizenf/image/upload/v1778832317/tagihan_nono/i84aifkefru7yxdflrzc.jpg	14552000	2026-05-15 15:05:41.099
77	2026-05-15 07:00:00	40000000	TF BRI	https://res.cloudinary.com/dkkbizenf/image/upload/v1778834418/tagihan_nono/pxsp1npidaedmfdo3imx.jpg	0	2026-05-15 15:40:32.888
78	2026-05-18 07:00:00	0	setoran sore	https://res.cloudinary.com/dkkbizenf/image/upload/v1779504211/tagihan_nono/jjpsoptmop6dcbhqmpgf.jpg	17001700	2026-05-23 09:43:55.221
79	2026-05-23 07:00:00	20000000		https://res.cloudinary.com/dkkbizenf/image/upload/v1779504673/tagihan_nono/dt8v7z7n4809l87m3ljv.jpg	0	2026-05-23 09:51:22.456
80	2026-05-23 07:00:00	6000000	BCA	https://res.cloudinary.com/dkkbizenf/image/upload/v1779505134/tagihan_nono/tq9gn0ou0zhmy9gdebrm.jpg	0	2026-05-23 09:59:15.566
81	2026-05-23 07:00:00	30000000	TF BRI	https://res.cloudinary.com/dkkbizenf/image/upload/v1779505199/tagihan_nono/owcvzg8pwtesyjactqps.jpg	0	2026-05-23 10:00:06.411
83	2026-05-21 07:00:00	0	setoran sore	https://res.cloudinary.com/dkkbizenf/image/upload/v1779507181/tagihan_nono/sfckbl3a2bicqyw4sovr.jpg	19975000	2026-05-23 10:33:22.886
84	2026-05-20 07:00:00	40000000	tunai		0	2026-05-23 10:44:51.919
85	2026-05-21 07:00:00	30000000	tunai		0	2026-05-23 10:45:41.336
86	2026-05-23 07:00:00	0	setoran sore	https://res.cloudinary.com/dkkbizenf/image/upload/v1779517390/tagihan_nono/ealfhbplapupfikxig8t.jpg	12010500	2026-05-23 13:23:38.024
82	2026-05-20 07:00:00	0	setoran sore	https://res.cloudinary.com/dkkbizenf/image/upload/v1779505471/tagihan_nono/smiiklwd2ayoeosoqjpt.jpg	10612400	2026-05-23 10:07:42.305
87	2026-05-11 07:00:00	0	setoran sore	https://res.cloudinary.com/dkkbizenf/image/upload/v1779517597/tagihan_nono/evb8cnxmwyqgk3p7otq5.jpg	11878000	2026-05-23 13:26:59.502
88	2026-05-07 07:00:00	0	setoran sore	https://res.cloudinary.com/dkkbizenf/image/upload/v1779518373/tagihan_nono/v2msqywhf3z0bvbli7js.jpg	15912000	2026-05-23 13:39:54.638
89	2026-05-06 07:00:00	0	setoran sore	https://res.cloudinary.com/dkkbizenf/image/upload/v1779518444/tagihan_nono/qzvyqonnkomkoi6qakjv.jpg	17153000	2026-05-23 13:40:58.953
90	2026-05-04 07:00:00	0	setoran sore	https://res.cloudinary.com/dkkbizenf/image/upload/v1779518500/tagihan_nono/dli6xqtfmllrg4eblqs1.jpg	14906200	2026-05-23 13:41:59.732
91	2026-05-28 07:00:00	30000000	setoran sore	https://res.cloudinary.com/dkkbizenf/image/upload/v1779944553/tagihan_nono/dkxeaaejapsuw1va4it9.jpg	0	2026-05-28 12:02:43.212
92	2026-05-30 07:00:00	0	setoran sore	https://res.cloudinary.com/dkkbizenf/image/upload/v1780128532/tagihan_nono/udh06a7dbjj3iruefyp4.jpg	20689000	2026-05-30 15:08:57.885
93	2026-05-31 07:00:00	40000000	TF BRI	https://res.cloudinary.com/dkkbizenf/image/upload/v1780231225/tagihan_nono/nbseay1g6jc4kklynf8w.jpg	0	2026-05-31 19:40:34.27
\.


--
-- Data for Name: BmpBahanNonoItem; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."BmpBahanNonoItem" (id, "bahanNonoId", "jenisBahan", kuantitas, unit, rate) FROM stdin;
40	63	Super A	1538	Kg	8300
41	64	Super A	1602	Kg	8300
42	65	Super A	2242	Kg	8300
43	67	Super A	2294	Kg	8500
44	69	Super A	2366	Kg	8500
45	70	Cerah A	1291	Kg	9500
46	70	Super A	982	Kg	8500
47	71	Super A	3038	Kg	8500
49	74	Super A	2274	Kg	8500
50	73	Super A	2194	Kg	8500
51	76	Super A	1712	Kg	8500
52	78	Super A	1933	Kg	8500
53	78	[JASA] Titip Giling	476	Kg	1200
80	83	Super A	2350	Kg	8500
81	86	Super A	1413	Kg	8500
82	82	Super A	1220	Kg	8500
83	82	[JASA] Titip Giling	202	Kg	1200
86	87	Super A	1300	Kg	8500
87	87	[JASA] Titip Giling	690	Kg	1200
88	88	Super A	1872	Kg	8500
89	89	Super A	2018	Kg	8500
92	90	Super A	1464	Kg	8300
93	90	[JASA] Titip Giling	290	Kg	9500
94	92	Super A	2434	Kg	8500
\.


--
-- Data for Name: BmpCashFlow; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."BmpCashFlow" (id, "transactionDate", "transactionType", description, amount, "paymentRefId", "createdAt") FROM stdin;
353	2026-04-28 07:00:00	KELUAR	jasa angkut - pak gito	70000	\N	2026-04-30 11:36:21.666
354	2026-04-28 07:00:00	KELUAR	uang saku - pak tono	520000	\N	2026-04-30 11:36:51.113
431	2026-05-05 07:00:00	MASUK	Pembayaran Faktur BMP-2605-002 (mas wiranto)	10000000	7	2026-05-05 16:50:09.64
439	2026-05-13 07:00:00	MASUK	Pembayaran Faktur BMP-0426-007 (abah ali)	53000000	12	2026-05-13 20:28:55.266
440	2026-05-12 07:00:00	KELUAR	kapasitor Ducati 10 kvar, 2 pcs	1786000	\N	2026-05-13 21:35:58.286
445	2026-05-15 07:00:00	MASUK	Pembayaran Faktur BMP-0426-010 (mas wiranto)	14835000	15	2026-05-15 11:10:44.351
450	2026-05-21 07:00:00	MASUK	Pembayaran Faktur BMP-0426-001 (abah ali)	31700000	18	2026-05-22 14:03:23.25
451	2026-05-15 07:00:00	KELUAR	Ongkir 	1100000	\N	2026-05-22 14:07:14.592
452	2026-05-19 07:00:00	KELUAR	Ongkir - Ko Hary	800000	\N	2026-05-22 14:07:48.639
453	2026-05-15 07:00:00	KELUAR	Jasa Angkut - Pak Sandi	250000	\N	2026-05-22 14:10:45.412
454	2026-05-19 07:00:00	KELUAR	Jasa Angkut - Ko Hary	250000	\N	2026-05-22 14:11:10.809
455	2026-05-21 07:00:00	KELUAR	Jasa Angkut - Ko Hary	300000	\N	2026-05-22 14:11:52.598
456	2026-05-21 07:00:00	KELUAR	Ongkir - Abah Kosi'in Grobogan	1620000	\N	2026-05-22 14:12:28.28
461	2026-05-23 07:00:00	MASUK	Pembayaran Faktur BMP-2605-013 (Mas Malvin)	20000000	23	2026-05-26 10:35:50.809
462	2026-05-26 07:00:00	MASUK	Pembayaran Faktur BMP-2605-013 (Mas Malvin)	13630000	24	2026-05-26 10:39:01.906
369	2026-04-30 07:00:00	KELUAR	ongkos kirim - pak tono	1250000	\N	2026-05-01 21:10:11.402
370	2026-05-01 07:00:00	KELUAR	jasa angkutan mas arip	20000	\N	2026-05-01 21:10:54.595
371	2026-04-17 07:00:00	KELUAR	Listrik PLN kWh 24364.0	26534828	\N	2026-05-01 21:13:30.873
432	2026-04-28 07:00:00	MASUK	Pembayaran Faktur BMP-0426-003 (abah aan)	20320000	8	2026-05-05 20:42:46.412
245	2026-04-28 07:00:00	MASUK	Pembayaran Borongan Faktur BMP-0426-048 (abah kosi'in)	40790000	\N	2026-04-29 15:24:03.137
247	2026-04-27 07:00:00	KELUAR	mesin fingerprint, LAN, Router SmartFren	3000000	\N	2026-04-29 15:24:06.123
248	2026-04-27 07:00:00	MASUK	Pembayaran Faktur BMP-0426-002 (abah ali)	33200000	\N	2026-04-29 15:24:07.355
249	2026-04-27 07:00:00	MASUK	Pembayaran Faktur BMP-0426-054 (Mas Malvin)	20000000	\N	2026-04-29 15:24:08.639
250	2026-04-25 07:00:00	KELUAR	jasa angkutan - mas adi	70000	\N	2026-04-29 15:24:10.014
251	2026-04-24 07:00:00	KELUAR	jasa angkut - mas adit	50000	\N	2026-04-29 15:24:11.447
252	2026-04-24 07:00:00	MASUK	Pembayaran Borongan Faktur BMP-0426-010 (mas wiranto)	200000	\N	2026-04-29 15:24:12.791
253	2026-04-24 07:00:00	MASUK	Pembayaran Borongan Faktur BMP-0426-006 (mas wiranto)	9655000	\N	2026-04-29 15:24:14.315
254	2026-04-24 07:00:00	MASUK	Pembayaran Borongan Faktur BMP-0426-005 (mas wiranto)	6395000	\N	2026-04-29 15:24:15.749
255	2026-04-24 07:00:00	MASUK	Pelunasan Faktur BMP-0426-052 (mas wiranto)	6150000	\N	2026-04-29 15:24:17.09
256	2026-04-23 07:00:00	MASUK	Pembayaran Borongan Faktur BMP-0426-048 (abah kosi'in)	30000000	\N	2026-04-29 15:24:18.513
257	2026-04-22 07:00:00	KELUAR	jasa angkut - mas adhi	50000	\N	2026-04-29 15:24:19.844
258	2026-04-22 07:00:00	KELUAR	ongkos kirim uang - Pak Tono	200000	\N	2026-04-29 15:24:21.176
259	2026-04-21 07:00:00	KELUAR	jasa pengiriman pak tono - Grobogan Pak Kosi'in	1630000	\N	2026-04-29 15:24:22.568
260	2026-04-21 07:00:00	KELUAR	jasa angkut - mas adhi	75000	\N	2026-04-29 15:24:23.941
261	2026-04-21 07:00:00	KELUAR	uang mesin neng tin	3400000	\N	2026-04-29 15:24:25.272
262	2026-04-21 07:00:00	KELUAR	jasa angkut - pak jito & pak sul (ALI)	300000	\N	2026-04-29 15:24:26.5
263	2026-04-20 07:00:00	MASUK	Pelunasan Faktur BMP-0426-039 (Mas Arylah)	8731000	\N	2026-04-29 15:24:27.729
264	2026-04-20 07:00:00	MASUK	Pelunasan Faktur BMP-0426-040 (mas wiranto)	4860000	\N	2026-04-29 15:24:28.922
265	2026-04-20 07:00:00	MASUK	Pelunasan Faktur BMP-0426-041 (Mas Eko Cahyono)	28755000	\N	2026-04-29 15:24:30.392
266	2026-04-20 07:00:00	MASUK	Pelunasan Faktur BMP-0426-042 (Linda Abadi)	28000000	\N	2026-04-29 15:24:32.435
267	2026-04-20 07:00:00	MASUK	Pelunasan Faktur BMP-0426-026 (mas kolis)	2825000	\N	2026-04-29 15:24:33.873
268	2026-04-20 07:00:00	MASUK	Pelunasan Faktur BMP-0426-044 (Pak Huda)	22160000	\N	2026-04-29 15:24:35.206
269	2026-04-20 07:00:00	MASUK	Pelunasan Faktur BMP-0426-043 (Umik Erna)	12000000	\N	2026-04-29 15:24:36.61
270	2026-04-20 07:00:00	KELUAR	jasa angkut - Pak Katiran	70000	\N	2026-04-29 15:24:38.072
271	2026-04-20 07:00:00	MASUK	Pelunasan Faktur BMP-0426-047 (pak katiran)	13805000	\N	2026-04-29 15:24:39.608
434	2026-05-05 07:00:00	MASUK	Pembayaran Faktur BMP-0426-024 (mas zahid)	10700000	10	2026-05-05 20:45:28.715
442	2026-05-14 07:00:00	KELUAR	Pembelian barang khusus untuk Faktur BMP-2605-008	6760000	\N	2026-05-14 08:52:21.031
443	2026-05-01 07:00:00	MASUK	Pembayaran Faktur BMP-2605-008 (mas wiranto)	10000000	13	2026-05-14 08:52:55.59
446	2026-05-15 07:00:00	MASUK	Pembayaran Faktur BMP-2605-010 (abah kosi'in)	80393000	16	2026-05-15 15:29:57.432
457	2026-05-19 07:00:00	MASUK	Pembayaran Faktur BMP-2605-013 (Mas Malvin)	14000000	19	2026-05-23 11:08:40.597
272	2026-04-20 07:00:00	MASUK	Pembayaran Faktur BMP-0426-025 (pak katiran)	17385000	\N	2026-04-29 15:24:40.922
273	2026-04-20 07:00:00	KELUAR	jasa angkut - mas adhi	50000	\N	2026-04-29 15:24:42.292
274	2026-04-20 07:00:00	KELUAR	Gaji Mas Dedi	5000000	\N	2026-04-29 15:24:43.499
275	2026-04-20 07:00:00	KELUAR	ongkir Pak Katiran	520000	\N	2026-04-29 15:24:44.83
276	2026-04-20 07:00:00	MASUK	Pelunasan Faktur BMP-0426-029 (mas zahid)	6015000	\N	2026-04-29 15:24:46.33
277	2026-04-20 07:00:00	MASUK	Pelunasan Faktur BMP-0426-028 (mas kolis)	780000	\N	2026-04-29 15:24:47.666
278	2026-04-20 07:00:00	MASUK	Pelunasan Faktur BMP-0426-046 (Mas Malvin)	44575000	\N	2026-04-29 15:24:49.131
279	2026-04-20 07:00:00	MASUK	Pelunasan Faktur BMP-0426-027 (mas kolis)	6850000	\N	2026-04-29 15:24:50.546
280	2026-04-20 07:00:00	MASUK	Pelunasan Faktur BMP-0426-045 (abah kosi'in)	51248000	\N	2026-04-29 15:24:51.998
281	2026-04-20 07:00:00	MASUK	Pembayaran Faktur BMP-0426-023 (mas kolis)	30000000	\N	2026-04-29 15:24:53.433
282	2026-04-20 07:00:00	MASUK	Pembayaran Faktur BMP-0426-009 (mas zahid)	13582500	\N	2026-04-29 15:24:54.858
283	2026-04-20 07:00:00	MASUK	Pembayaran Faktur BMP-0426-034 (mas wiranto)	16040000	\N	2026-04-29 15:24:56.299
284	2026-04-20 07:00:00	MASUK	Pelunasan Faktur BMP-0426-030 (mas kolis)	9882500	\N	2026-04-29 15:24:57.733
285	2026-04-20 07:00:00	MASUK	Pelunasan Faktur BMP-0426-031 (mas kolis)	15337500	\N	2026-04-29 15:24:59.122
286	2026-04-20 07:00:00	MASUK	Pelunasan Faktur BMP-0426-032 (mas kolis)	10325000	\N	2026-04-29 15:25:00.6
287	2026-04-20 07:00:00	MASUK	Pelunasan Faktur BMP-0426-033 (mas kolis)	15337500	\N	2026-04-29 15:25:01.931
288	2026-04-20 07:00:00	MASUK	Pelunasan Faktur BMP-0426-022 (mas wiranto)	290000	\N	2026-04-29 15:25:03.263
289	2026-04-20 07:00:00	MASUK	Pelunasan Faktur BMP-0426-035 (mas wiranto)	5984000	\N	2026-04-29 15:25:04.542
290	2026-04-20 07:00:00	MASUK	Pelunasan Faktur BMP-0426-036 (mas wiranto)	10211500	\N	2026-04-29 15:25:05.925
291	2026-04-20 07:00:00	MASUK	Pelunasan Faktur BMP-0426-037 (mas wiranto)	9182000	\N	2026-04-29 15:25:07.359
292	2026-04-20 07:00:00	MASUK	Pelunasan Faktur BMP-0426-038 (mas wiranto)	2940000	\N	2026-04-29 15:25:08.69
293	2026-04-19 07:00:00	KELUAR	uang mesin ibu	5000000	\N	2026-04-29 15:25:09.882
294	2026-04-17 07:00:00	KELUAR	beli inventaris kantor	8075000	\N	2026-04-29 15:25:11.352
295	2026-04-17 07:00:00	KELUAR	beli Kartu Smartfren	70000	\N	2026-04-29 15:25:12.888
296	2026-04-17 07:00:00	KELUAR	jasa angkut - mas adi	50000	\N	2026-04-29 15:25:14.195
297	2026-04-17 07:00:00	KELUAR	beli website	2000000	\N	2026-04-29 15:25:15.653
298	2026-04-17 07:00:00	MASUK	Pelunasan Faktur BMP-0426-017 (mas kolis)	14500000	\N	2026-04-29 15:25:16.782
299	2026-04-17 07:00:00	MASUK	Pelunasan Faktur BMP-0426-012 (mas kolis)	4125000	\N	2026-04-29 15:25:18.008
300	2026-04-17 07:00:00	MASUK	Pelunasan Faktur BMP-0426-018 (abah ali)	16520000	\N	2026-04-29 15:25:19.241
301	2026-04-17 07:00:00	MASUK	Pelunasan Faktur BMP-0426-019 (abah ali)	30600000	\N	2026-04-29 15:25:20.622
302	2026-04-17 07:00:00	MASUK	Pelunasan Faktur BMP-0426-020 (abah ali)	6560000	\N	2026-04-29 15:25:22.002
303	2026-04-17 07:00:00	MASUK	Pelunasan Faktur BMP-0426-021 (mas kolis)	13200000	\N	2026-04-29 15:25:23.538
304	2026-04-17 07:00:00	MASUK	Pelunasan Faktur BMP-0426-022 (mas wiranto)	12400000	\N	2026-04-29 15:25:24.887
305	2026-04-17 07:00:00	KELUAR	[FAKTUR BELI] Bengkel Sahabat - Kusmiantoro | Pembuatan Matras Baskom Rotan 14 (1,0Lsn x 1,0Qty), Pembuatan Matras Baskom Bahtera (1,0Lsn x 1,0Qty)	61000000	\N	2026-04-29 15:25:26.405
306	2026-04-17 07:00:00	KELUAR	[FAKTUR BELI] Bengkel Sahabat - Kusmiantoro | Krom Matras 2X (1,0Lsn x 1,0Qty)	4250000	\N	2026-04-29 15:25:27.839
307	2026-04-17 07:00:00	KELUAR	[FAKTUR BELI] Bengkel Sahabat - Kusmiantoro | servis Matras Bahtera (1,0Lsn x 1,0Qty)	2500000	\N	2026-04-29 15:25:29.222
308	2026-04-17 07:00:00	KELUAR	[FAKTUR BELI] Bengkel Sahabat - Kusmiantoro | Servis Matras & Krom (1,0Lsn x 1,0Qty)	5400000	\N	2026-04-29 15:25:30.604
309	2026-04-17 07:00:00	KELUAR	[FAKTUR BELI] Bengkel Sahabat - Kusmiantoro | servis Matras Baskom Panda (1,0Lsn x 1,0Qty)	2500000	\N	2026-04-29 15:25:32.14
310	2026-04-17 07:00:00	KELUAR	[FAKTUR BELI] Bengkel Sahabat - Kusmiantoro | Servis Matras (1,0Lsn x 1,0Qty)	2500000	\N	2026-04-29 15:25:33.49
311	2026-04-17 07:00:00	KELUAR	[FAKTUR BELI] Bengkel Sahabat - Kusmiantoro | krom Matras 2X (1,0Lsn x 1,0Qty)	3000000	\N	2026-04-29 15:25:34.802
312	2026-04-17 07:00:00	KELUAR	[FAKTUR BELI] Pak Hadi CNC | CNC Ulang Baskom Mawar (1,0Lsn x 1,0Qty)	8000000	\N	2026-04-29 15:25:36.236
313	2026-04-17 07:00:00	KELUAR	[FAKTUR BELI] Hwamda | Angsuran Mesin Hwamda (1,0Lsn x 1,0Qty)	20000000	\N	2026-04-29 15:25:37.669
314	2026-04-17 07:00:00	KELUAR	[FAKTUR BELI] Hwamda | Angsuran Mesin Hwamda (1,0Lsn x 1,0Qty)	20000000	\N	2026-04-29 15:25:39.103
315	2026-04-17 07:00:00	KELUAR	[FAKTUR BELI] Hwamda | Angsuran Mesin Hwamda (1,0Lsn x 1,0Qty)	20000000	\N	2026-04-29 15:25:40.639
316	2026-04-17 07:00:00	KELUAR	[FAKTUR BELI] Listrik PLN | kWh 19269.0 (1,0Lsn x 1,0Qty)	20858656	\N	2026-04-29 15:25:42.042
317	2026-04-17 07:00:00	KELUAR	Tiner 8 Liter	200000	\N	2026-04-29 15:25:43.506
318	2026-04-16 07:00:00	MASUK	Pelunasan Faktur BMP-0426-015 (mas wiranto)	3100000	\N	2026-04-29 15:25:44.94
319	2026-04-16 07:00:00	MASUK	Pelunasan Faktur BMP-0426-014 (mas wiranto)	5490000	\N	2026-04-29 15:25:46.332
320	2026-04-16 07:00:00	KELUAR	jasa angkut - mas adhi	50000	\N	2026-04-29 15:25:47.704
321	2026-04-16 07:00:00	KELUAR	Cleo Gelas 2 Box, kmbli 2K ksih ke orng krja	50000	\N	2026-04-29 15:25:48.933
322	2026-04-16 07:00:00	MASUK	Pelunasan Faktur BMP-0426-016 (ko hary)	14400000	\N	2026-04-29 15:25:50.367
323	2026-04-15 07:00:00	KELUAR	Bayar Server Railway	100000	\N	2026-04-29 15:25:51.596
324	2026-04-15 07:00:00	KELUAR	jasa angkut - mas adhi	50000	\N	2026-04-29 15:25:53.132
325	2026-04-15 07:00:00	MASUK	Pembayaran Faktur BMP-0426-004 (mas wiranto)	9012000	\N	2026-04-29 15:25:54.463
326	2026-04-15 07:00:00	MASUK	Pelunasan Faktur BMP-0426-013 (abah kosi'in)	52350000	\N	2026-04-29 15:25:55.962
327	2026-04-14 07:00:00	KELUAR	[FAKTUR BELI] Pak Kasnar | Wakul Telur (20,0Lsn x 50,0Qty), Wakul Telur Kotak (20,0Lsn x 100,0Qty), Karung Putih (1,0Lsn x 1,0Qty), Karung Kuning (1,0Lsn x 1,0Qty)	16400000	\N	2026-04-29 15:25:57.433
328	2026-04-14 07:00:00	MASUK	Pembayaran Faktur BMP-0426-008 (Linda Abadi)	52270000	\N	2026-04-29 15:25:58.969
329	2026-04-14 07:00:00	KELUAR	jasa angkut - mas adhi	50000	\N	2026-04-29 15:26:00.272
330	2026-04-14 07:00:00	KELUAR	jasa pengiriman pak tono - Blora & Bojonegoro	1750000	\N	2026-04-29 15:26:01.733
331	2026-04-13 07:00:00	KELUAR	jasa angkut - mas adi	50000	\N	2026-04-29 15:26:02.962
436	2026-04-23 07:00:00	KELUAR	beli kambing	3800000	\N	2026-05-05 22:47:12.6
332	2026-04-13 07:00:00	KELUAR	Uang Konsumsi Karyawan	300000	\N	2026-04-29 15:26:04.194
333	2026-04-13 07:00:00	KELUAR	jasa angkut - pak jito & pak sul (ALI)	260000	\N	2026-04-29 15:26:05.421
334	2026-04-10 07:00:00	KELUAR	[FAKTUR BELI] Bengkel Sahabat - Kusmiantoro | DP Matras Piring "8" (1,0Lsn x 1,0Qty)	8000000	\N	2026-04-29 15:26:06.644
335	2026-04-04 07:00:00	KELUAR	jasa pengiriman pak tono	1000000	\N	2026-04-29 15:26:07.835
336	2026-04-04 07:00:00	KELUAR	jasa angkut - pak jito	300000	\N	2026-04-29 15:26:09.106
337	2026-04-03 07:00:00	KELUAR	jasa pengiriman - arip (wiranto)	15000	\N	2026-04-29 15:26:10.335
338	2026-04-02 07:00:00	KELUAR	jasa angkut - arip (wiranto)	15000	\N	2026-04-29 15:26:11.574
339	2026-03-20 07:00:00	KELUAR	uang mesin ibu	5000000	\N	2026-04-29 15:26:12.793
340	2026-03-20 07:00:00	KELUAR	Gaji Mas Dedi	5000000	\N	2026-04-29 15:26:14.332
342	2026-03-09 07:00:00	KELUAR	jasa angkut - arip	15000	\N	2026-04-29 15:26:16.889
343	2026-03-05 07:00:00	KELUAR	jasa angkut - pak jito	300000	\N	2026-04-29 15:26:18.118
344	2026-03-05 07:00:00	KELUAR	jasa pengiriman pak tono	1000000	\N	2026-04-29 15:26:19.449
345	2026-02-28 07:00:00	KELUAR	jasa pengiriman mas hendrik	2000000	\N	2026-04-29 15:26:20.859
346	2026-02-28 07:00:00	KELUAR	jasa pengiriman pak tono	1000000	\N	2026-04-29 15:26:22.316
347	2026-02-28 07:00:00	KELUAR	jasa angkut - pak jito	300000	\N	2026-04-29 15:26:23.852
348	2026-02-26 07:00:00	KELUAR	jasa angkut - pak jito	300000	\N	2026-04-29 15:26:25.245
349	2026-02-26 07:00:00	KELUAR	jasa pengiriman pak tono	1000000	\N	2026-04-29 15:26:26.618
350	2026-02-20 07:00:00	KELUAR	Gaji Mas Dedi	5000000	\N	2026-04-29 15:26:28.051
351	2026-02-20 07:00:00	KELUAR	uang mesin ibu	5000000	\N	2026-04-29 15:26:29.452
352	2026-02-17 07:00:00	KELUAR	[FAKTUR BELI] Listrik PLN | Listrik PLN (1,0Lsn x 1,0Qty)	28246157	\N	2026-04-29 15:26:30.67
425	2026-05-04 07:00:00	MASUK	Pembayaran Faktur BMP-2605-001 (mas zahid)	12850000	1	2026-05-05 01:26:14.313
426	2026-05-04 07:00:00	MASUK	Pembayaran Borongan Faktur BMP-0426-054	12625000	2	2026-05-05 01:27:41.615
427	2026-05-04 07:00:00	MASUK	Pembayaran Borongan Faktur BMP-2605-001	21000000	3	2026-05-05 01:28:49.971
429	2026-05-05 07:00:00	MASUK	Pembayaran Borongan Faktur BMP-0426-055	12400000	5	2026-05-05 09:38:14.576
430	2026-05-05 07:00:00	MASUK	Pembayaran Borongan Faktur BMP-2605-001	16522000	6	2026-05-05 09:38:14.607
437	2026-05-08 07:00:00	KELUAR	beli oli pertamina 2 drum	12310000	\N	2026-05-09 12:06:54.472
438	2026-05-09 07:00:00	KELUAR	bayar angsuran mesin	20000000	\N	2026-05-09 12:07:48.642
444	2026-04-11 07:00:00	MASUK	Pembayaran Faktur BMP-2605-009 (abah ali)	34692500	14	2026-05-14 20:16:46.854
447	2026-05-14 07:00:00	MASUK	Pembayaran Faktur BMP-2605-013 (Mas Malvin)	20845000	17	2026-05-15 21:08:45.052
459	2026-05-23 07:00:00	MASUK	Pembayaran Borongan Faktur BMP-0426-011 (mas wiranto)	15039000	21	2026-05-23 16:24:27.848
460	2026-05-23 07:00:00	MASUK	Pembayaran Borongan Faktur BMP-0426-050 (mas wiranto)	3506000	22	2026-05-23 16:24:27.87
463	2026-06-04 00:00:00	MASUK	Pembayaran cicilan Faktur BMP-2605-018 (mas kolis)	8988000	25	2026-06-08 12:48:21.543
464	2026-06-04 00:00:00	MASUK	Pembayaran cicilan Faktur BMP-2606-001 (mas kolis)	5818000	26	2026-06-08 12:48:45.72
465	2026-06-05 00:00:00	MASUK	Pembayaran cicilan Faktur BMP-2605-014 (Mas Malvin)	68350000	27	2026-06-08 12:49:25.447
466	2026-05-22 00:00:00	MASUK	Pembayaran cicilan Faktur BMP-2605-006 (pak sandi)	688000	28	2026-06-08 12:51:04.201
467	2026-06-04 00:00:00	MASUK	Pembayaran cicilan Faktur BMP-2605-011 (pak sandi)	39705000	29	2026-06-08 12:52:03.566
468	2026-06-06 00:00:00	MASUK	Pembayaran cicilan Faktur BMP-2605-016 (Abah Hary)	14080000	30	2026-06-08 12:52:39.843
469	2026-06-01 00:00:00	MASUK	Pembayaran cicilan Faktur BMP-0426-056 (mas yeyen)	18280000	31	2026-06-08 12:53:38.934
470	2026-06-01 00:00:00	MASUK	Pembayaran cicilan Faktur BMP-0426-030 (mas kolis)	9128000	32	2026-06-08 12:56:55.333
471	2026-06-01 00:00:00	MASUK	Pembayaran cicilan Faktur BMP-0426-032 (mas kolis)	10335000	33	2026-06-08 12:57:11.265
472	2026-06-01 00:00:00	MASUK	Pembayaran cicilan Faktur BMP-0426-053 (Pak Huda)	30306000	34	2026-06-08 13:02:25.637
\.


--
-- Data for Name: BmpClient; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."BmpClient" (id, "saldoTitipan", "clientName", "addressLine1", "clientLogo", province, "postalCode", "phoneNumber", "emailAddress", "taxNumber", "uniqueID", slug, "createdAt", "updatedAt") FROM stdin;
8	0	mas kolis	mojojejer, jombang	default_logo.jpg	Jawa Timur	\N	\N	\N	\N	e61abc8929eb	mas-kolis-jawa-timur-e61abc8929eb	2026-03-14 14:36:27.816	2026-03-14 14:36:27.816
7	0	contoh	Jl. Cendrawasih No. 66 Kecamatan Gedangan Kabupaten Sidoarjo Ds. Punggul Rt. 05 Rw. 02	company_logos/8558_xa9uLKm.jpg	Jawa Timur	61254	6282652626237	muhammadmuizz8@gmail.com	\N	a563faa58c48	contoh-jawa-timur-a563faa58c48	2026-03-12 19:58:52.419	2026-03-24 02:21:12.654
12	0	abah ali	pasar turi		Jawa Timur	\N	\N	\N	\N	4491ce4544e6	abah-ali-jawa-timur-4491ce4544e6	2026-04-13 08:30:57.775	2026-04-13 08:30:57.775
13	0	abah aan	kudus		Jawa Timur	\N	\N	\N	\N	9a865fb86b3f	abah-aan-jawa-timur-9a865fb86b3f	2026-04-13 11:46:14.429	2026-04-13 11:46:14.429
14	0	mas wiranto	jombang			\N	\N	\N	\N	5f52d5627185	mas-wiranto-5f52d5627185	2026-04-13 13:21:31.989	2026-04-13 13:21:31.989
15	0	Linda Abadi	jl. Raya Sulang-Rembang RT 01 / RW 01			\N	082132939649	\N	\N	5382f6e3c63a	linda-abadi-5382f6e3c63a	2026-04-13 18:51:00.2	2026-04-13 18:51:00.2
16	0	mas zahid	krian		Jawa Timur	\N	\N	\N	\N	008a9246f228	mas-zahid-jawa-timur-008a9246f228	2026-04-14 12:14:35.43	2026-04-14 12:14:35.43
17	0	abah kosi'in	grobogan		Jawa Timur	\N	\N	\N	\N	fe67439feeda	abah-kosiin-jawa-timur-fe67439feeda	2026-04-15 17:35:21.008	2026-04-15 17:35:21.008
18	0	ko hary	surabaya		Jawa Timur	\N	\N	\N	\N	a63dde0f5aa7	ko-hary-jawa-timur-a63dde0f5aa7	2026-04-16 18:20:19.732	2026-04-16 18:20:19.732
19	0	pak katiran	\N			\N	\N	\N	\N	04f5fec83e9b	pak-katiran-04f5fec83e9b	2026-04-20 08:19:25.429	2026-04-20 08:19:25.429
20	0	Mas Arylah	\N			\N	\N	\N	\N	2262fc8fe4c4	mas-arylah-2262fc8fe4c4	2026-04-20 12:28:34.698	2026-04-20 12:28:34.698
22	0	Umik Erna	\N			\N	\N	\N	\N	1cfaa3143ccb	umik-erna-1cfaa3143ccb	2026-04-20 13:30:42.699	2026-04-20 13:30:42.699
23	0	Pak Huda	\N			\N	\N	\N	\N	0b3afccfc639	pak-huda-0b3afccfc639	2026-04-20 13:33:11.726	2026-04-20 13:33:11.726
24	0	Mas Malvin	\N			\N	\N	\N	\N	1ee859ca5484	mas-malvin-1ee859ca5484	2026-04-20 18:52:23.207	2026-04-20 18:52:23.207
25	0	Mas Eka	Jombang			\N	\N	\N	\N	6a80f3487e0f	mas-eka-6a80f3487e0f	2026-04-23 08:38:58.002	2026-04-23 08:38:58.004
26	0	mas yeyen	blitar		Jawa Timur	\N	\N	\N	\N	c89dad5e8148	mas-yeyen-jawa-timur-c89dad5e8148	2026-04-28 10:04:02.092	2026-04-28 10:04:02.092
27	0	Mas iyon			gersik					80231e6b		2026-04-30 11:34:36.279	2026-05-01 21:51:07.797
35	0	pak sandi			malang - jatim					08d3d704	pak-sandi-08d3d704	2026-05-05 20:32:18.843	2026-05-05 20:32:18.843
21	0	Mas Eko Cahyono			jawa tengah - cepu					a9ca31ee63a8	mas-eko-cahyono-a9ca31ee63a8	2026-04-20 12:37:53.961	2026-05-22 11:11:28.123
36	0	Abah Hary			Jawa Timur - Lumajang					09b6bef2	abah-hary-09b6bef2	2026-05-22 14:01:07.838	2026-05-22 14:01:07.838
\.


--
-- Data for Name: BmpDeviceTenant; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."BmpDeviceTenant" (id, "serialNumber", "tenantId", "createdAt") FROM stdin;
\.


--
-- Data for Name: BmpEmployee; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."BmpEmployee" (id, name, "position", "salaryAmount", "isActive", "fingerprintPIN", "createdAt", "updatedAt") FROM stdin;
22	muizz	admin	83300	t	1	2026-05-12 13:37:12.385	2026-05-12 14:53:49.293
4	mbak santi	operator	50000	t	3	2026-05-02 00:01:57.114	2026-05-13 11:49:55.365
10	mas ibnu	operator	55000	t	5	2026-05-02 00:04:31.23	2026-05-13 12:30:26.272
19	mas febri	operator	52500	t	4	2026-05-02 00:30:51.118	2026-05-13 12:30:44.835
15	mas vincent	operator	50000	t	6	2026-05-02 00:29:47.997	2026-05-13 12:34:25.999
6	mbak asih	operator	47500	t	7	2026-05-02 00:02:49.061	2026-05-13 12:35:41.664
5	mbak endah	operator	47500	t	8	2026-05-02 00:02:16.865	2026-05-13 12:46:34.551
11	mas wahyu	operator	67500	t	9	2026-05-02 00:28:42.225	2026-05-13 12:47:44.488
8	mas dimas	operator	75000	t	10	2026-05-02 00:03:59.158	2026-05-13 14:56:50.156
12	mas roby	operator	52500	t	11	2026-05-02 00:28:54.624	2026-05-13 14:57:49.508
2	mbak nur	operator	57500	t	12	2026-05-01 23:33:47.12	2026-05-13 14:58:41.963
13	mas farel	operator	52500	t	13	2026-05-02 00:29:14.805	2026-05-13 14:59:44.519
1	mbak lik	operator	60000	t	14	2026-05-01 23:11:45.72	2026-05-13 22:54:16.782
3	mbak eni	operator	52500	t	15	2026-05-02 00:01:36.667	2026-05-13 22:54:26.17
17	mas karem	operator	52500	t	16	2026-05-02 00:30:27.758	2026-05-13 23:01:22.991
9	mas agung	operator	75000	t	17	2026-05-02 00:04:14.571	2026-05-13 23:01:47.431
18	mas dian	operator	52500	t	18	2026-05-02 00:30:41.685	2026-05-13 23:02:10.967
14	mas candra	operator	52500	t	19	2026-05-02 00:29:37.919	2026-05-13 23:04:59.975
7	mbak vivin	operator	47500	t	20	2026-05-02 00:03:12.291	2026-05-14 12:31:03.791
23	noval	operator	50000	t	21	2026-05-22 16:38:12.228	2026-05-22 16:38:12.228
24	fareliyan	operator	50000	t	22	2026-05-30 12:54:56.962	2026-05-30 12:54:56.962
\.


--
-- Data for Name: BmpInvoice; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."BmpInvoice" (id, title, number, "dueDate", "paymentTerms", status, notes, "clientId", "uniqueID", slug, "createdAt", "updatedAt") FROM stdin;
132	Invoice	BMP-0426-008	2026-04-13 07:00:00	14 days	PAID	\N	15	ba484592d050	bmp-0426-008-ba484592d050	2026-04-13 18:58:55.23	2026-04-14 13:33:48.253
181	april	BMP-0426-031	2026-04-06 07:00:00	14 days	PAID		8	c1769dd67f90	bmp-0426-031-c1769dd67f90	2026-04-20 11:33:27.228	2026-04-20 11:35:57.155
183	april	BMP-0426-033	2026-04-06 07:00:00	14 days	PAID		8	f6c232361901	bmp-0426-033-f6c232361901	2026-04-20 11:41:06.415	2026-04-20 11:44:21.722
167	wiranto	BMP-0426-022	2026-02-18 07:00:00	14 days	PAID		14	0689e7a5ca37	bmp-0426-022-0689e7a5ca37	2026-04-17 15:48:50.179	2026-04-20 12:02:58.433
112	wiranto	BMP-0426-004	2026-03-09 07:00:00	14 days	PAID	jatuh tempo 09/03/2026	14	f22c2645b4ca	bmp-0426-004-f22c2645b4ca	2026-04-13 13:20:24.784	2026-04-15 09:09:30.363
155	wiranto	BMP-0426-014	2026-03-03 07:00:00	14 days	PAID		14	c229bb5b2901	bmp-0426-014-c229bb5b2901	2026-04-16 18:13:30.484	2026-04-16 18:15:41.821
184	april	BMP-0426-034	2026-02-23 07:00:00	14 days	PAID		14	2957164f1bd3	bmp-0426-034-2957164f1bd3	2026-04-20 12:04:11.706	2026-04-20 12:10:51.448
185	april	BMP-0426-035	2026-02-12 07:00:00	14 days	PAID		14	00c699cad969	bmp-0426-035-00c699cad969	2026-04-20 12:11:52.364	2026-04-20 12:13:40.699
186	april	BMP-0426-036	2026-02-12 07:00:00	14 days	PAID		14	f5567849cceb	bmp-0426-036-f5567849cceb	2026-04-20 12:13:59.365	2026-04-20 12:17:41.57
187	april	BMP-0426-037	2026-02-28 07:00:00	14 days	PAID		14	c824f3289e0e	bmp-0426-037-c824f3289e0e	2026-04-20 12:18:46.742	2026-04-20 12:20:58.332
188	april	BMP-0426-038	2026-03-10 07:00:00	14 days	PAID		14	41f87ea14ebc	bmp-0426-038-41f87ea14ebc	2026-04-20 12:25:10.849	2026-04-20 12:26:54.92
190	April	BMP-0426-039	2026-03-10 07:00:00	14 days	PAID		20	afbbd75f647b	bmp-0426-039-afbbd75f647b	2026-04-20 12:38:22.786	2026-04-20 12:42:13.4
191	april	BMP-0426-040	2026-03-13 07:00:00	14 days	PAID		14	5813288498f9	bmp-0426-040-5813288498f9	2026-04-20 12:47:39.781	2026-04-20 12:48:49.211
156	wiranto	BMP-0426-015	2026-03-04 07:00:00	14 days	PAID		14	9a1df5ac9fb8	bmp-0426-015-9a1df5ac9fb8	2026-04-16 18:17:13.082	2026-04-16 18:19:08.763
114	maret	BMP-0426-006	2026-04-03 07:00:00	14 days	PAID	jatuh tempo 03/04/2026	14	87902a8333fb	bmp-0426-006-87902a8333fb	2026-04-13 15:02:57.672	2026-04-24 12:49:23.838
150	ok	BMP-0426-013	2026-03-02 07:00:00	14 days	PAID		17	4626b369ace2	bmp-0426-013-4626b369ace2	2026-04-15 17:33:33.203	2026-04-15 17:56:55.863
192	april	BMP-0426-041	2026-03-14 07:00:00	14 days	PAID		21	6dbc817617bf	bmp-0426-041-6dbc817617bf	2026-04-20 12:48:55.721	2026-04-20 12:50:42.759
193	april	BMP-0426-042	2026-03-14 07:00:00	14 days	PAID		15	557f407bc957	bmp-0426-042-557f407bc957	2026-04-20 12:55:52.237	2026-04-20 12:56:37.946
194	April	BMP-0426-043	2026-03-14 07:00:00	14 days	PAID		22	965a80dd232f	bmp-0426-043-965a80dd232f	2026-04-20 13:30:49.863	2026-04-20 13:32:46.208
158	ko hary	BMP-0426-016	2026-03-05 07:00:00	14 days	PAID		18	8bafdd83eca9	bmp-0426-016-8bafdd83eca9	2026-04-16 18:20:27.756	2026-04-16 18:21:31.35
160	mas kolis	BMP-0426-017	2026-02-27 07:00:00	14 days	PAID		8	c5342542eb13	bmp-0426-017-c5342542eb13	2026-04-17 14:01:41.962	2026-04-17 14:03:38.037
172	april	BMP-0426-025	2026-04-20 07:00:00	14 days	PAID		19	550f8bbf3c75	bmp-0426-025-550f8bbf3c75	2026-04-20 08:19:53.55	2026-04-20 13:55:42.211
195	April	BMP-0426-044	2026-03-14 07:00:00	14 days	PAID		23	93b3381f7d46	bmp-0426-044-93b3381f7d46	2026-04-20 13:33:23.235	2026-04-20 18:44:34.312
149	kolis	BMP-0426-012	2026-03-02 07:00:00	14 days	PAID		8	6650e0ec148c	bmp-0426-012-6650e0ec148c	2026-04-15 17:29:37.716	2026-04-17 15:35:23.268
163	desember	BMP-0426-018	2025-12-22 07:00:00	14 days	PAID		12	949eed424074	bmp-0426-018-949eed424074	2026-04-17 15:35:46.073	2026-04-17 15:38:33.969
196	April	BMP-0426-045	2026-03-24 07:00:00	14 days	PAID		17	d4cb08234888	bmp-0426-045-d4cb08234888	2026-04-20 18:45:34.424	2026-04-20 18:50:53.454
164	ali	BMP-0426-019	2026-01-08 07:00:00	14 days	PAID		12	be9dc85c37fd	bmp-0426-019-be9dc85c37fd	2026-04-17 15:39:17.632	2026-04-17 15:40:24.045
165	ali	BMP-0426-020	2026-04-17 07:00:00	14 days	PAID		12	0c0a34480e9d	bmp-0426-020-0c0a34480e9d	2026-04-17 15:40:56.608	2026-04-17 15:43:05.573
197	Maret	BMP-0426-046	2026-03-29 07:00:00	14 days	PAID		24	0cf8359599e5	bmp-0426-046-0cf8359599e5	2026-04-20 18:51:35.612	2026-04-20 18:58:34.72
166	kolis	BMP-0426-021	2026-03-05 07:00:00	14 days	PAID		8	d8843b58024c	bmp-0426-021-d8843b58024c	2026-04-17 15:44:35.937	2026-04-17 15:45:41.924
198	April	BMP-0426-047	2026-04-09 07:00:00	14 days	PAID		19	824916832967	bmp-0426-047-824916832967	2026-04-20 19:03:38.904	2026-04-20 19:09:47.929
169	kolis	BMP-0426-023	2026-05-02 07:00:00	14 days	PAID	Jatuh Tempo 02/05/2026	8	4e51b15a3f2d	bmp-0426-023-4e51b15a3f2d	2026-04-18 08:51:36.411	2026-04-20 08:07:13.28
173	maret	BMP-0426-026	2026-04-01 07:00:00	14 days	PAID		8	585a7d38a91a	bmp-0426-026-585a7d38a91a	2026-04-20 08:34:33.625	2026-04-20 08:36:24.103
174	maret	BMP-0426-027	2026-03-07 07:00:00	14 days	PAID		8	3a1cb0e22771	bmp-0426-027-3a1cb0e22771	2026-04-20 08:36:57.14	2026-04-20 08:38:56.133
175	maret	BMP-0426-028	2026-03-11 07:00:00	14 days	PAID		8	13ec3348f9b4	bmp-0426-028-13ec3348f9b4	2026-04-20 08:39:23.885	2026-04-20 08:40:46.262
203	April	BMP-0426-051	2026-04-23 07:00:00	14 days	OVERDUE		25	0c9d954f1b9b	bmp-0426-051-0c9d954f1b9b	2026-04-23 08:39:03.785	2026-04-23 08:40:46.818
134	Invoice	BMP-0426-009	2026-04-11 07:00:00	14 days	PAID	\N	16	0d595fd9ce98	bmp-0426-009-0d595fd9ce98	2026-04-14 12:12:13.242	2026-04-20 08:59:57.471
135	Invoice	BMP-0426-010	2026-04-08 07:00:00	14 days	PAID		14	eb614695dac1	bmp-0426-010-eb614695dac1	2026-04-14 12:54:46.856	2026-05-15 11:10:44.379
170	april	BMP-0426-024	2026-05-18 07:00:00	30 days	PAID	Jatuh Tempo 18/05/2026	16	7e03092e65a8	bmp-0426-024-7e03092e65a8	2026-04-19 17:10:05.301	2026-05-05 20:45:28.732
115	maret	BMP-0426-007	2026-04-04 07:00:00	14 days	PAID	jatuh tempo 04/04/2026	12	b6327ab2bc52	bmp-0426-007-b6327ab2bc52	2026-04-13 15:26:21.098	2026-05-13 20:28:55.294
109	maret	BMP-0426-001	2026-03-05 07:00:00	14 days	PAID	jatuh tempo tanggal 05/03/2026	12	b1b1618544ab	bmp-0426-001-b1b1618544ab	2026-04-13 08:31:08.388	2026-05-22 14:03:23.273
138	Invoice	BMP-0426-011	2026-04-15 07:00:00	14 days	PAID		14	efb5b9e360c4	bmp-0426-011-efb5b9e360c4	2026-04-15 08:28:38.455	2026-05-23 16:24:27.854
179	maret	BMP-0426-029	2026-03-07 07:00:00	14 days	PAID		16	ad411ec1303a	bmp-0426-029-ad411ec1303a	2026-04-20 09:00:18.659	2026-04-20 09:02:09.019
113	februari	BMP-0426-005	2026-04-02 07:00:00	14 days	PAID	jatuh tempo 02/04/2026	14	0a50b6ceefc5	bmp-0426-005-0a50b6ceefc5	2026-04-13 13:42:37.684	2026-04-24 12:49:23.831
110	februari	BMP-0426-002	2026-02-26 07:00:00	14 days	PAID	jatuh tempo 26/02/2026	12	aa7ed98b7600	bmp-0426-002-aa7ed98b7600	2026-04-13 11:03:54.624	2026-04-27 15:53:52.862
200	april	BMP-0426-048	2026-04-28 07:00:00	14 days	PAID		17	4cbc1cd3a1d8	bmp-0426-048-4cbc1cd3a1d8	2026-04-21 16:43:06.159	2026-04-28 14:53:51.365
205	April	BMP-0426-052	2026-04-24 07:00:00	14 days	PAID		14	0aed2dbac456	bmp-0426-052-0aed2dbac456	2026-04-24 12:51:51.955	2026-04-24 12:52:57.998
251	Faktur Penjualan	BMP-2605-004	2026-05-03 07:00:00	14 days	PAID		16	9e707f6b	BMP-2605-004-9e707f6b	2026-05-05 01:25:53.261	2026-05-13 13:57:08.782
256	Faktur Penjualan	BMP-2605-007	2026-06-08 07:00:00	14 days	UNPAID		12	dd96ce2c	BMP-2605-007-dd96ce2c	2026-05-09 12:35:55.04	2026-05-13 13:57:13.491
262	Faktur Penjualan	BMP-2605-013	2026-05-29 07:00:00	14 days	PAID		24	4d43d330	BMP-2605-013-4d43d330	2026-05-15 21:07:39.335	2026-05-26 10:39:02.406
255	Faktur Penjualan	BMP-2605-006	2026-06-04 07:00:00	14 days	PARTIAL		35	35ca2747	BMP-2605-006-35ca2747	2026-05-05 20:49:45.339	2026-06-08 12:51:04.204
210	april	BMP-0426-056	2026-05-05 07:00:00	14 days	PAID		26	0ecaeb93fc2a	bmp-0426-056-0ecaeb93fc2a	2026-04-28 10:06:40.3	2026-06-08 12:53:38.937
180	april	BMP-0426-030	2026-04-11 07:00:00	14 days	PAID		8	7ed262bd1c5a	bmp-0426-030-7ed262bd1c5a	2026-04-20 11:21:27.699	2026-06-08 12:56:55.336
182	april	BMP-0426-032	2026-04-06 07:00:00	14 days	PAID		8	fd9bd5ccae75	bmp-0426-032-fd9bd5ccae75	2026-04-20 11:36:27.502	2026-06-08 12:57:11.267
206	April	BMP-0426-053	2026-05-09 07:00:00	14 days	PAID		23	4f44ed5e1757	bmp-0426-053-4f44ed5e1757	2026-04-27 09:51:29.664	2026-06-08 13:02:25.641
211	Faktur Penjualan	BMP-2605-001	2026-05-08 07:00:00	14 days	PAID		27	037c4106		2026-05-01 15:19:25.283	2026-05-05 01:28:49.975
257	Faktur Penjualan	BMP-2605-008	2026-06-13 07:00:00	14 days	PARTIAL		14	b971b6d0	BMP-2605-008-b971b6d0	2026-05-14 08:52:21.013	2026-05-26 16:06:27.597
209	april	BMP-0426-055	2026-04-28 07:00:00	14 days	PAID		8	465eaf53d49d	bmp-0426-055-465eaf53d49d	2026-04-28 08:44:29.088	2026-05-05 09:38:14.58
252	Faktur Penjualan	BMP-2605-005	2026-05-31 07:00:00	14 days	PAID		14	25268dae	BMP-2605-005-25268dae	2026-05-05 16:49:58.51	2026-05-14 08:45:18.505
111	februari	BMP-0426-003	2026-02-28 07:00:00	14 days	PAID	jatuh tempo 28/02/2026	13	217f12bce51b	bmp-0426-003-217f12bce51b	2026-04-13 11:45:02.023	2026-05-05 20:42:46.44
248	Faktur Penjualan	BMP-2605-002	2026-06-01 07:00:00	14 days	UNPAID		12	7ce5ac61	BMP-2605-002-7ce5ac61	2026-05-02 08:13:22.504	2026-05-13 13:57:06.044
250	Faktur Penjualan	BMP-2605-003	2026-05-16 07:00:00	14 days	PAID		8	d1aebb79	BMP-2605-003-d1aebb79	2026-05-02 16:00:45.574	2026-05-13 13:57:07.56
258	Faktur Penjualan	BMP-2605-009	2026-02-15 07:00:00	14 days	PAID		12	6bea2397	BMP-2605-009-6bea2397	2026-05-14 20:15:37.856	2026-05-14 20:16:46.88
264	Faktur Penjualan	BMP-2605-015	2026-06-05 07:00:00	14 days	UNPAID		21	88bb7619	BMP-2605-015-88bb7619	2026-05-22 11:31:06.961	2026-05-22 11:32:37.194
259	Faktur Penjualan	BMP-2605-010	2026-05-06 07:00:00	14 days	PAID		17	c94477e0	BMP-2605-010-c94477e0	2026-05-14 20:35:43.491	2026-05-26 16:10:18.713
261	Faktur Penjualan	BMP-2605-012	2026-06-14 07:00:00	14 days	UNPAID		14	006f3869	BMP-2605-012-006f3869	2026-05-15 09:35:08.347	2026-05-15 17:23:08.079
202	april	BMP-0426-050	2026-05-05 07:00:00	14 days	PARTIAL	Jatuh Tempo 05/05/2026	14	a0c9af198361	bmp-0426-050-a0c9af198361	2026-04-21 20:49:37.44	2026-05-23 16:24:27.873
266	Faktur Penjualan	BMP-2605-017	2026-06-24 07:00:00	14 days	UNPAID		12	00c25e20	BMP-2605-017-00c25e20	2026-05-25 11:32:42.654	2026-05-25 12:07:35.644
207	april	BMP-0426-054	2026-05-03 07:00:00	14 days	PARTIAL		24	065702d819fa	bmp-0426-054-065702d819fa	2026-04-27 10:33:28.906	2026-05-26 10:33:29.92
272	Faktur Penjualan	BMP-2606-002	2026-06-22 00:00:00	14 days	UNPAID		8	ec7e89b5	BMP-2606-002-ec7e89b5	2026-06-08 00:00:00	2026-06-08 10:48:35.248
270	Faktur Penjualan	BMP-2605-018	2026-06-13 07:00:00	14 days	PAID		8	915e3416	BMP-2605-018-915e3416	2026-05-30 09:00:44.531	2026-06-08 12:48:21.545
271	Faktur Penjualan	BMP-2606-001	2026-06-15 07:00:00	14 days	PAID		8	a849f0da	BMP-2606-001-a849f0da	2026-06-01 09:30:00.656	2026-06-08 12:48:45.722
263	Faktur Penjualan	BMP-2605-014	2026-06-05 07:00:00	14 days	PAID		24	13b59092	BMP-2605-014-13b59092	2026-05-22 11:25:42.696	2026-06-08 12:49:25.449
260	Faktur Penjualan	BMP-2605-011	2026-05-29 07:00:00	14 days	PAID		35	51099424	BMP-2605-011-51099424	2026-05-15 07:31:27.027	2026-06-08 12:52:03.569
265	Faktur Penjualan	BMP-2605-016	2026-06-02 07:00:00	14 days	PAID		36	3df6596f	BMP-2605-016-3df6596f	2026-05-22 14:05:15.77	2026-06-08 12:52:39.845
275	Faktur Penjualan	BMP-2606-007	2026-06-20 12:00:00	14 days	UNPAID		12	cf84e1b3	BMP-2606-007-cf84e1b3	2026-06-06 12:00:00	2026-06-06 12:00:00
\.


--
-- Data for Name: BmpInvoicePayment; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."BmpInvoicePayment" (id, "invoiceId", "paymentDate", "paymentAmount", "paymentMethod", "createdAt") FROM stdin;
15	135	2026-05-15 07:00:00	14835000	TRANSFER	2026-05-15 11:10:44.342
10	170	2026-05-05 07:00:00	10700000	TRANSFER	2026-05-05 20:45:28.711
12	115	2026-05-13 07:00:00	53000000	TRANSFER	2026-05-13 20:28:55.257
18	109	2026-05-21 07:00:00	31700000	TRANSFER	2026-05-22 14:03:23.242
21	138	2026-05-23 07:00:00	15039000	Borongan TRANSFER	2026-05-23 16:24:27.842
1	251	2026-05-04 07:00:00	12850000	TRANSFER	2026-05-05 01:26:14.304
24	262	2026-05-26 07:00:00	13630000	TRANSFER	2026-05-26 10:39:01.78
23	262	2026-05-23 07:00:00	20000000	TRANSFER	2026-05-26 10:35:50.559
19	262	2026-05-19 07:00:00	14000000	TRANSFER	2026-05-23 11:08:40.59
17	262	2026-05-14 07:00:00	20845000	TRANSFER	2026-05-15 21:08:45.043
3	211	2026-05-04 07:00:00	21000000	Borongan TRANSFER	2026-05-05 01:28:49.967
13	257	2026-05-01 07:00:00	10000000	TRANSFER	2026-05-14 08:52:55.581
5	209	2026-05-05 07:00:00	12400000	Borongan TRANSFER	2026-05-05 09:38:14.568
7	252	2026-05-05 07:00:00	10000000	TRANSFER	2026-05-05 16:50:09.63
8	111	2026-04-28 07:00:00	20320000	TRANSFER	2026-05-05 20:42:46.4
6	250	2026-05-05 07:00:00	16522000	Borongan TRANSFER	2026-05-05 09:38:14.602
14	258	2026-04-11 07:00:00	34692500	TRANSFER	2026-05-14 20:16:46.845
16	259	2026-05-15 07:00:00	80393000	TRANSFER	2026-05-15 15:29:57.428
22	202	2026-05-23 07:00:00	3506000	Borongan TRANSFER	2026-05-23 16:24:27.867
2	207	2026-05-04 07:00:00	12625000	Borongan TRANSFER	2026-05-05 01:27:41.611
25	270	2026-06-04 00:00:00	8988000	TRANSFER	2026-06-08 12:48:21.541
26	271	2026-06-04 00:00:00	5818000	TRANSFER	2026-06-08 12:48:45.718
27	263	2026-06-05 00:00:00	68350000	TRANSFER	2026-06-08 12:49:25.443
28	255	2026-05-22 00:00:00	688000	TRANSFER	2026-06-08 12:51:04.197
29	260	2026-06-04 00:00:00	39705000	TRANSFER	2026-06-08 12:52:03.562
30	265	2026-06-06 00:00:00	14080000	TRANSFER	2026-06-08 12:52:39.841
31	210	2026-06-01 00:00:00	18280000	TRANSFER	2026-06-08 12:53:38.931
32	180	2026-06-01 00:00:00	9128000	TRANSFER	2026-06-08 12:56:55.329
33	182	2026-06-01 00:00:00	10335000	TRANSFER	2026-06-08 12:57:11.262
34	206	2026-06-01 00:00:00	30306000	TRANSFER	2026-06-08 13:02:25.634
\.


--
-- Data for Name: BmpMachineBonusLog; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."BmpMachineBonusLog" (id, "employeeId", "machineName", "shiftType", "bonusAmount", "jumlahPerolehan", date, "createdAt") FROM stdin;
1	22	Baskom Mawar	Siang	5000	0	2026-05-15 07:00:00	2026-05-15 15:33:23.401
3	22	Baskom Panda	Pagi	10000	0	2026-05-16 07:00:00	2026-05-16 17:34:10.53
4	22	Baskom Panda	Pagi	10000	0	2026-05-17 07:00:00	2026-05-17 12:59:50.221
5	19	Baskom Panda	Pagi	10000	0	2026-05-17 07:00:00	2026-05-17 13:00:24.588
6	10	Baskom Panda	Pagi	10000	0	2026-05-17 07:00:00	2026-05-17 13:00:34.555
7	5	Baskom Panda	Pagi	10000	0	2026-05-17 07:00:00	2026-05-17 13:00:43.415
8	22	Wakul Moris	Pagi	5000	0	2026-05-18 07:00:00	2026-05-19 00:46:55.044
9	22	Baskom Panda	Sore	5000	50	2026-05-22 07:00:00	2026-05-22 16:22:26.964
10	12	Baskom Panda	Sore	5000	51	2026-05-22 07:00:00	2026-05-22 16:23:40.34
11	2	Baskom Jago	Sore	7000	51	2026-05-22 07:00:00	2026-05-22 16:24:22.728
12	13	BMP	Sore	5000	36	2026-05-22 07:00:00	2026-05-22 16:25:48.433
13	8	Wakul Moris	Sore	5000	60	2026-05-22 07:00:00	2026-05-22 16:26:20.839
14	15	Baskom Durian	Sore	5000	37	2026-05-22 07:00:00	2026-05-22 16:33:37.058
15	7	Baskom Panda	Sore	5000	99	2026-05-22 07:00:00	2026-05-22 16:34:28.19
16	17	Baskom Panda	Malam	5000	13	2026-05-22 07:00:00	2026-05-22 22:54:25.9
17	9	BMP	Malam	5000	10	2026-05-22 07:00:00	2026-05-22 22:57:47.731
18	3	Baskom Durian	Malam	5000	38	2026-05-22 07:00:00	2026-05-22 22:58:19.542
19	18	Wakul Moris	Malam	5000	52	2026-05-22 07:00:00	2026-05-22 22:58:58.507
20	23	Wakul Moris	Malam	5000	99	2026-05-22 07:00:00	2026-05-22 22:59:19.832
21	22	Bahtera TM	Pagi	5000	80	2026-05-29 07:00:00	2026-05-29 22:11:29.409
\.


--
-- Data for Name: BmpMasterProduct; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."BmpMasterProduct" (id, title, description, unit, price, "beratGram", "cycleTime", cavity, "rejectRate", "uniqueID", slug, "createdAt", "updatedAt") FROM stdin;
29	Baskom TM	\N	lusin	7000	40	9.5	1	0	\N	\N	2026-04-28 15:11:49.947	2026-04-28 15:11:49.947
38	Smile 14	\N	Lusin	8200	0	0	1	0	\N	\N	2026-04-28 15:11:49.947	2026-04-28 15:11:49.947
37	telor tali	\N	Lusin	3200	0	0	1	0	\N	\N	2026-04-28 15:11:49.947	2026-04-28 15:11:49.947
39	tradisi cerah	\N	Lusin	5200	0	0	1	0	\N	\N	2026-04-28 15:11:49.947	2026-04-28 15:11:49.947
40	Baskom Barca	\N	Lusin	7250	0	0	1	0	\N	\N	2026-04-28 15:11:49.947	2026-04-28 15:11:49.947
41	Wakul Tanggok	\N	Lusin	5800	0	0	1	0	\N	\N	2026-04-28 15:11:49.947	2026-04-28 15:11:49.947
42	Baskom Rotan	\N	Lusin	8400	0	0	1	0	\N	\N	2026-04-28 15:11:49.947	2026-04-28 15:11:49.947
43	Wakul Mawar Super	\N	Lusin	9000	0	0	1	0	\N	\N	2026-04-28 15:11:49.947	2026-04-28 15:11:49.947
44	Smile 14	\N	Lusin	8400	0	0	1	0	\N	\N	2026-04-28 15:11:49.947	2026-04-28 15:11:49.947
45	Wakul Morris Super	\N	Lusin	5700	0	0	1	0	\N	\N	2026-04-28 15:11:49.947	2026-04-28 15:11:49.947
46	BMP		Lusin	7000	0	0	1	0	44ba7f8a		2026-05-01 15:18:43.627	2026-05-01 15:18:43.627
15	contoh	\N	Lusin	5000	0	0	1	0	\N	\N	2026-04-28 15:11:49.947	2026-04-28 15:11:49.947
13	baskom panda	\N	Lusin	7000	0	0	1	0	\N	\N	2026-04-28 15:11:49.947	2026-04-28 15:11:49.947
16	baskom mawar	\N	Lusin	5800	0	0	1	0	\N	\N	2026-04-28 15:11:49.947	2026-04-28 15:11:49.947
17	baskom bahtera TM	\N	Lusin	6100	0	0	1	0	\N	\N	2026-04-28 15:11:49.947	2026-04-28 15:11:49.947
19	wakul moris	\N	Lusin	5500	0	0	1	0	\N	\N	2026-04-28 15:11:49.947	2026-04-28 15:11:49.947
20	baskom jago	\N	Lusin	5600	0	0	1	0	\N	\N	2026-04-28 15:11:49.947	2026-04-28 15:11:49.947
23	smile 12	\N	Lusin	5400	0	0	1	0	\N	\N	2026-04-28 15:11:49.947	2026-04-28 15:11:49.947
24	wakul rehana	\N	Lusin	4000	0	0	1	0	\N	\N	2026-04-28 15:11:49.947	2026-04-28 15:11:49.947
25	baskom mawar	\N	Lusin	5800	0	0	1	0	\N	\N	2026-04-28 15:11:49.947	2026-04-28 15:11:49.947
27	Wakul Rehana Super	\N	Lusin	4300	0	0	1	0	\N	\N	2026-04-28 15:11:49.947	2026-04-28 15:11:49.947
28	baskom jago 12	\N	Lusin	5700	0	0	1	0	\N	\N	2026-04-28 15:11:49.947	2026-04-28 15:11:49.947
31	wakul tradisi super	\N	Lusin	3600	0	0	1	0	\N	\N	2026-04-28 15:11:49.947	2026-04-28 15:11:49.947
32	Baskom Bahtera TB	\N	Lusin	7100	0	0	1	0	\N	\N	2026-04-28 15:11:49.947	2026-04-28 15:11:49.947
33	wakul kotak	\N	Lusin	5700	0	0	1	0	\N	\N	2026-04-28 15:11:49.947	2026-04-28 15:11:49.947
35	wakul telur	\N	Lusin	2300	0	0	1	0	\N	\N	2026-04-28 15:11:49.947	2026-04-28 15:11:49.947
36	telor japar	\N	Lusin	2100	0	0	1	0	\N	\N	2026-04-28 15:11:49.947	2026-04-28 15:11:49.947
26	Baskom Bahtera	\N	Lusin	5900	0	0	1	0	\N	\N	2026-04-28 15:11:49.947	2026-04-28 15:11:49.947
51	Baskom Panda Cerah		Lusin	10000	55	10	1	0	8ac53e80	Baskom Panda Cerah-8ac53e80	2026-05-15 08:25:46.436	2026-05-15 15:01:01.047
52	Test Product AI		Pcspcs	1000	0	0	1	0	dc023a4e	Test Product AI-dc023a4e	2026-05-16 11:48:27.025	2026-05-16 11:48:27.025
30	Baskom Durian	\N	Lusin	9200	50	11	1	0	\N	\N	2026-04-28 15:11:49.947	2026-05-16 11:52:29.008
18	bak kuping12	\N	Lusin	13000	72	15	1	0	\N	\N	2026-04-28 15:11:49.947	2026-05-31 13:31:11.603
34	Tradisi Super 30	\N	Lusin	3400	21	7	1	0	\N	\N	2026-04-28 15:11:49.947	2026-05-31 13:32:59.934
21	tradisi super 2	\N	Lusin	3100	21	7	1	0	\N	\N	2026-04-28 15:11:49.947	2026-05-31 13:34:09.68
22	baskom panda super	\N	Lusin	8400	52	10	1	0	\N	\N	2026-04-28 15:11:49.947	2026-05-31 13:35:49.413
\.


--
-- Data for Name: BmpPayroll; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."BmpPayroll" (id, "employeeId", "paymentDate", amount, "attendanceCount", "dailyRate", description, "createdAt") FROM stdin;
\.


--
-- Data for Name: BmpPembayaran; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."BmpPembayaran" (id, "invoiceId", "tanggalBayar", "jumlahBayar", keterangan) FROM stdin;
\.


--
-- Data for Name: BmpPembelianBarang; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."BmpPembelianBarang" (id, supplier, tanggal, keterangan, "totalHarga", "caraBayar", "createdAt") FROM stdin;
\.


--
-- Data for Name: BmpPembelianItem; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."BmpPembelianItem" (id, "pembelianId", "namaBarang", "jumlahLusin", kuantitas, unit, "hargaSatuan") FROM stdin;
\.


--
-- Data for Name: BmpProduct; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."BmpProduct" (id, "masterItemID", title, unit, price, "jumlahLusin", quantity, "isKhusus", "hargaBeli", currency, "invoiceId", "uniqueID", slug, "createdAt", "updatedAt") FROM stdin;
150	20	baskom jago	-	5400	50	110	f	0	Rp	150	0e02cbaec1ee	baskom-jago-0e02cbaec1ee	2026-04-15 17:37:01.527	2026-04-15 17:37:01.527
151	26	Bahkom Bahtera	-	5900	50	30	f	0	Rp	150	a5ec365d8011	bahkom-bahtera-a5ec365d8011	2026-04-15 17:37:01.531	2026-04-15 17:37:01.531
152	36	telor japar	-	2100	40	50	f	0	Rp	150	571b0c111d03	telor-japar-571b0c111d03	2026-04-15 17:56:55.868	2026-04-15 17:56:55.868
107	23	smile 12	-	5700	50	10	f	0	Rp	114	2c1b3ef8dbde	smile-12-2c1b3ef8dbde	2026-04-13 15:22:01.332	2026-04-17 13:58:46.135
141	34	Tradisi Super 30	-	3600	30	25	f	0	Rp	138	b470e494e550	tradisi-super-30-b470e494e550	2026-04-15 09:14:38.203	2026-04-21 21:32:03.855
91	18	bak kuping ANISA 12	-	12000	30	80	f	0	Rp	110	fdd3ae4ff1b0	bak-kuping-anisa-12-fdd3ae4ff1b0	2026-04-13 11:08:42.423	2026-04-13 11:08:42.423
92	19	wakul moris	-	5500	40	20	f	0	Rp	110	ae46e07b616e	wakul-moris-ae46e07b616e	2026-04-13 11:08:42.428	2026-04-13 11:08:42.428
89	18	bak kuping ANISA 12	-	12000	30	80	f	0	Rp	109	3df804a6b534	bak-kuping-anisa-12-3df804a6b534	2026-04-13 08:32:38.633	2026-04-13 08:32:38.633
90	16	baskom mawar	-	5800	50	10	f	0	Rp	109	d0ea04fea599	baskom-mawar-d0ea04fea599	2026-04-13 08:32:38.636	2026-04-13 08:32:38.636
94	20	baskom jago	-	5600	50	40	f	0	Rp	111	ef92c1609c19	baskom-jago-ef92c1609c19	2026-04-13 13:13:22.88	2026-04-13 13:13:22.88
95	19	wakul moris	-	5700	40	40	f	0	Rp	111	81593b97335a	wakul-moris-81593b97335a	2026-04-13 13:14:03.084	2026-04-13 13:14:03.084
96	21	tradisi super 2	-	3100	20	40	f	0	Rp	112	c6112d4dc3a6	tradisi-super-2-c6112d4dc3a6	2026-04-13 13:30:03.823	2026-04-13 13:30:03.823
97	22	baskom panda super	-	7200	40	4	f	0	Rp	112	f20ee407581a	baskom-panda-super-f20ee407581a	2026-04-13 13:30:58.318	2026-04-13 13:30:58.318
98	24	wakul rehana	-	4000	50	10	f	0	Rp	112	a98c073b3c99	wakul-rehana-a98c073b3c99	2026-04-13 13:31:50.532	2026-04-13 13:31:50.532
99	25	baskom mawar	-	5800	50	7	f	0	Rp	112	c87f8316e58e	baskom-mawar-c87f8316e58e	2026-04-13 13:36:43.58	2026-04-13 13:36:43.58
100	23	smile 12	-	5400	50	5	f	0	Rp	112	504614f38715	smile-12-504614f38715	2026-04-13 13:37:35.004	2026-04-13 13:37:35.004
101	26	Bahkom Bahtera	-	5900	50	5	f	0	Rp	113	43789f437e72	bahkom-bahtera-43789f437e72	2026-04-13 14:27:27.651	2026-04-13 14:27:27.651
102	17	baskom bahtera TM	-	5700	50	5	f	0	Rp	113	c4491786e907	baskom-bahtera-tm-c4491786e907	2026-04-13 14:27:27.658	2026-04-13 14:27:27.658
103	27	Wakul Rehana Super	-	4300	50	12	f	0	Rp	113	4d21f27a4126	wakul-rehana-super-4d21f27a4126	2026-04-13 14:27:27.666	2026-04-13 14:27:27.666
104	25	baskom mawar	-	6100	50	3	f	0	Rp	113	835e5bad1628	baskom-mawar-835e5bad1628	2026-04-13 14:27:27.67	2026-04-13 14:27:27.67
105	22	baskom panda super	-	7500	40	10	f	0	Rp	114	9f845c4e6274	baskom-panda-super-9f845c4e6274	2026-04-13 15:18:56.23	2026-04-13 15:18:56.23
108	28	baskom jago 12	-	5700	50	3	f	0	Rp	114	0b1a104ea3c4	baskom-jago-12-0b1a104ea3c4	2026-04-13 15:22:01.335	2026-04-13 15:22:01.335
109	18	bak kuping ANISA 12	-	12300	30	100	f	0	Rp	115	122b8a74011a	bak-kuping-anisa-12-122b8a74011a	2026-04-13 15:32:12.646	2026-04-13 15:32:12.646
110	16	baskom mawar	-	6100	50	10	f	0	Rp	115	461f504f375f	baskom-mawar-461f504f375f	2026-04-13 15:32:12.651	2026-04-13 15:32:12.651
111	29	Baskom TM	-	6100	50	10	f	0	Rp	115	2e8ee9881c1a	baskom-tm-2e8ee9881c1a	2026-04-13 15:32:12.654	2026-04-13 15:32:12.654
112	13	baskom panda	-	7800	40	10	f	0	Rp	115	65dfd24637b0	baskom-panda-65dfd24637b0	2026-04-13 15:32:12.656	2026-04-13 15:32:12.656
113	20	baskom jago	-	5800	50	4	f	0	Rp	115	9b67adb57beb	baskom-jago-9b67adb57beb	2026-04-13 15:32:12.659	2026-04-13 15:32:12.659
114	19	wakul moris	-	5800	40	10	f	0	Rp	115	f6696ce1c33a	wakul-moris-f6696ce1c33a	2026-04-13 15:32:12.661	2026-04-13 15:32:12.661
115	30	Baskom Durian	-	8500	40	10	f	0	Rp	115	334d50b49069	baskom-durian-334d50b49069	2026-04-13 15:33:50.62	2026-04-13 15:33:50.62
123	32	Baskom Bahtera TB	-	7100	50	50	f	0	Rp	132	865d5c6326f1	baskom-bahtera-tb-865d5c6326f1	2026-04-13 19:00:07.55	2026-04-13 19:00:07.55
124	31	wakul tradisi super	-	3600	20	10	f	0	Rp	132	5a684c3ab2b0	wakul-tradisi-super-5a684c3ab2b0	2026-04-13 19:00:07.558	2026-04-13 19:00:07.558
125	33	wakul kotak	-	5700	20	30	f	0	Rp	132	f295179a15ef	wakul-kotak-f295179a15ef	2026-04-13 19:06:24.403	2026-04-13 19:06:24.403
128	29	Baskom TM	-	6750	50	15	f	0	Rp	134	63020ceed7f9	baskom-tm-63020ceed7f9	2026-04-14 12:20:45.007	2026-04-14 12:20:45.007
129	25	baskom mawar	-	6800	50	20	f	0	Rp	134	5979eb87bffb	baskom-mawar-5979eb87bffb	2026-04-14 12:20:45.011	2026-04-14 12:20:45.011
130	30	Baskom Durian	-	8600	40	5	f	0	Rp	134	e9b11a19b1ec	baskom-durian-e9b11a19b1ec	2026-04-14 12:20:45.014	2026-04-14 12:20:45.014
131	29	Baskom TM	-	6500	50	10	f	0	Rp	135	f0b65eecc616	baskom-tm-f0b65eecc616	2026-04-14 12:57:57.807	2026-04-14 12:57:57.807
132	25	baskom mawar	-	7100	50	15	f	0	Rp	135	f7546516aa9d	baskom-mawar-f7546516aa9d	2026-04-14 12:57:57.816	2026-04-14 12:57:57.816
133	23	smile 12	-	6400	50	10	f	0	Rp	135	89817187abb3	smile-12-89817187abb3	2026-04-14 12:57:57.822	2026-04-14 12:57:57.822
134	34	Tradisi Super 30	-	3400	30	30	f	0	Rp	135	478139c40375	tradisi-super-30-478139c40375	2026-04-14 12:59:31.475	2026-04-14 12:59:31.475
135	20	baskom jago	-	6200	50	98	f	0	Rp	132	476a53a54f0d	baskom-jago-476a53a54f0d	2026-04-14 13:32:15.751	2026-04-14 13:32:15.751
137	23	smile 12	-	6700	50	20	f	0	Rp	138	3e510bc77ddc	smile-12-3e510bc77ddc	2026-04-15 08:42:08.767	2026-04-15 08:42:08.767
138	16	baskom mawar	-	7100	50	5	f	0	Rp	138	f21c0b239468	baskom-mawar-f21c0b239468	2026-04-15 08:42:34.979	2026-04-15 08:42:34.979
140	35	wakul telur	-	2300	20	84	f	0	Rp	138	0c5d77c6c877	wakul-telur-0c5d77c6c877	2026-04-15 08:44:12.601	2026-04-15 08:44:12.601
153	37	telor tali	-	3200	40	50	f	0	Rp	150	9048090f9ae6	telor-tali-9048090f9ae6	2026-04-15 17:56:55.873	2026-04-15 17:56:55.873
154	31	wakul tradisi super	-	3200	40	25	f	0	Rp	150	b2404a735812	wakul-tradisi-super-b2404a735812	2026-04-15 17:56:55.878	2026-04-15 17:56:55.878
158	31	wakul tradisi super	-	3100	30	30	f	0	Rp	155	bc6288361ed2	wakul-tradisi-super-bc6288361ed2	2026-04-16 18:15:41.827	2026-04-16 18:15:41.827
159	23	smile 12	-	5400	50	10	f	0	Rp	155	5e33f6cd553b	smile-12-5e33f6cd553b	2026-04-16 18:15:41.83	2026-04-16 18:15:41.83
160	21	tradisi super 2	-	3100	20	50	f	0	Rp	156	1201710281ea	tradisi-super-2-1201710281ea	2026-04-16 18:19:08.767	2026-04-16 18:19:08.767
161	22	baskom panda super	-	7200	40	50	f	0	Rp	158	ef4417723e65	baskom-panda-super-ef4417723e65	2026-04-16 18:21:31.358	2026-04-16 18:21:31.358
106	29	Baskom TM	-	5900	50	10	f	0	Rp	114	8cfe593e75af	baskom-tm-8cfe593e75af	2026-04-13 15:22:01.329	2026-04-17 13:57:48.793
162	17	baskom bahtera TM	-	5500	50	43	f	0	Rp	160	d15f0b4dd21e	baskom-bahtera-tm-d15f0b4dd21e	2026-04-17 14:03:38.041	2026-04-17 14:03:38.041
163	28	baskom jago 12	-	5300	50	5	f	0	Rp	160	082981805e0e	baskom-jago-12-082981805e0e	2026-04-17 14:03:38.048	2026-04-17 14:03:38.048
164	23	smile 12	-	5400	50	5	f	0	Rp	160	b92d8b1c6d32	smile-12-b92d8b1c6d32	2026-04-17 14:03:38.052	2026-04-17 14:03:38.052
165	18	bak kuping12	-	13000	30	5	f	0	Rp	149	9476a578ceb4	bak-kuping12-9476a578ceb4	2026-04-17 15:35:23.272	2026-04-17 15:35:23.272
166	23	smile 12	-	5400	50	5	f	0	Rp	149	ad30baa308c2	smile-12-ad30baa308c2	2026-04-17 15:35:23.276	2026-04-17 15:35:23.276
167	17	baskom bahtera TM	-	5500	50	3	f	0	Rp	149	f1326f4849a4	baskom-bahtera-tm-f1326f4849a4	2026-04-17 15:35:23.278	2026-04-17 15:35:23.279
168	17	baskom bahtera TM	-	5800	50	15	f	0	Rp	163	cba8915698ac	baskom-bahtera-tm-cba8915698ac	2026-04-17 15:38:33.973	2026-04-17 15:38:33.973
169	13	baskom panda	-	7500	40	15	f	0	Rp	163	506417ec2cb1	baskom-panda-506417ec2cb1	2026-04-17 15:38:33.976	2026-04-17 15:38:33.976
170	30	Baskom Durian	-	8200	40	15	f	0	Rp	163	c8689e9c597d	baskom-durian-c8689e9c597d	2026-04-17 15:38:33.979	2026-04-17 15:38:33.979
171	20	baskom jago	-	5500	50	10	f	0	Rp	163	f54f6d129637	baskom-jago-f54f6d129637	2026-04-17 15:38:33.981	2026-04-17 15:38:33.982
172	18	bak kuping12	-	12000	30	85	f	0	Rp	164	17b519f0b497	bak-kuping12-17b519f0b497	2026-04-17 15:40:24.049	2026-04-17 15:40:24.049
173	38	Smile 14	Lusin	8200	40	20	f	0	Rp	165	a14009a24085	smile-14-a14009a24085	2026-04-17 15:42:44.994	2026-04-17 15:42:44.994
174	17	baskom bahtera TM	-	5500	50	48	f	0	Rp	166	12da7dbc98b4	baskom-bahtera-tm-12da7dbc98b4	2026-04-17 15:45:41.93	2026-04-17 15:45:41.93
175	36	telor japar	-	2100	20	100	f	0	Rp	167	f05135291124	telor-japar-f05135291124	2026-04-17 15:51:58.479	2026-04-17 15:51:58.48
176	24	wakul rehana	-	4000	20	50	f	0	Rp	167	c3cbbac0aefe	wakul-rehana-c3cbbac0aefe	2026-04-17 15:51:58.491	2026-04-17 15:51:58.491
177	26	Bahkom Bahtera	-	5600	50	15	f	0	Rp	167	35a5e59e2ac0	bahkom-bahtera-35a5e59e2ac0	2026-04-17 15:51:58.499	2026-04-17 15:51:58.499
178	22	baskom panda super	-	8400	40	10	f	0	Rp	169	823558c7fc68	baskom-panda-super-823558c7fc68	2026-04-18 08:52:52.104	2026-04-18 09:10:56.738
179	20	baskom jago	-	7000	50	3	f	0	Rp	169	4bfe49304491	baskom-jago-4bfe49304491	2026-04-18 09:15:02.081	2026-04-18 09:15:02.081
180	17	baskom bahtera TM	-	7000	50	10	f	0	Rp	169	9654aa5cf13f	baskom-bahtera-tm-9654aa5cf13f	2026-04-18 09:15:02.091	2026-04-18 09:15:02.091
181	18	bak kuping12	-	14000	30	5	f	0	Rp	169	0d543cc9da0e	bak-kuping12-0d543cc9da0e	2026-04-18 09:15:02.099	2026-04-18 09:15:02.099
183	25	baskom mawar	-	7200	50	20	f	0	Rp	170	0deb43ebab06	baskom-mawar-0deb43ebab06	2026-04-19 17:28:56.841	2026-04-19 17:28:56.841
184	17	baskom bahtera TM	-	7000	50	10	f	0	Rp	170	ebdc66900d09	baskom-bahtera-tm-ebdc66900d09	2026-04-19 17:28:56.847	2026-04-19 17:28:56.847
182	39	tradisi cerah	Lusin	5000	30	5	f	0	Rp	169	ce30a26aac50	tradisi-cerah-ce30a26aac50	2026-04-18 09:16:41.961	2026-04-20 08:17:20.461
185	22	baskom panda super	-	8400	40	30	f	0	Rp	172	41ff5cfe41d2	baskom-panda-super-41ff5cfe41d2	2026-04-20 08:33:34.144	2026-04-20 08:33:34.144
186	30	Baskom Durian	-	8900	40	5	f	0	Rp	172	8265cd64a155	baskom-durian-8265cd64a155	2026-04-20 08:33:34.152	2026-04-20 08:33:34.153
187	26	Bahkom Bahtera	-	7500	50	5	f	0	Rp	172	dd28fb01e30d	bahkom-bahtera-dd28fb01e30d	2026-04-20 08:33:34.16	2026-04-20 08:33:34.16
188	16	baskom mawar	-	7300	50	10	f	0	Rp	172	0607a011be7c	baskom-mawar-0607a011be7c	2026-04-20 08:33:34.169	2026-04-20 08:33:34.169
189	26	Bahkom Bahtera	-	5900	50	5	f	0	Rp	173	2191bca1fba5	bahkom-bahtera-2191bca1fba5	2026-04-20 08:36:24.117	2026-04-20 08:36:24.117
190	17	baskom bahtera TM	-	5400	50	5	f	0	Rp	173	5eb4714b25e9	baskom-bahtera-tm-5eb4714b25e9	2026-04-20 08:36:24.128	2026-04-20 08:36:24.128
191	17	baskom bahtera TM	-	5500	50	10	f	0	Rp	174	94bb9f524eff	baskom-bahtera-tm-94bb9f524eff	2026-04-20 08:38:56.143	2026-04-20 08:38:56.143
192	28	baskom jago 12	-	5300	50	10	f	0	Rp	174	af103ff79e47	baskom-jago-12-af103ff79e47	2026-04-20 08:38:56.15	2026-04-20 08:38:56.15
193	25	baskom mawar	-	5800	50	5	f	0	Rp	174	875c56a13840	baskom-mawar-875c56a13840	2026-04-20 08:38:56.159	2026-04-20 08:38:56.159
194	18	bak kuping12	-	13000	30	2	f	0	Rp	175	6a52b3e7b1d0	bak-kuping12-6a52b3e7b1d0	2026-04-20 08:40:46.273	2026-04-20 08:40:46.273
195	17	baskom bahtera TM	-	5700	50	11	f	0	Rp	179	97e6f63fb8be	baskom-bahtera-tm-97e6f63fb8be	2026-04-20 09:02:09.024	2026-04-20 09:02:09.024
196	22	baskom panda super	-	7200	40	10	f	0	Rp	179	ef4eb8c60b12	baskom-panda-super-ef4eb8c60b12	2026-04-20 09:02:09.029	2026-04-20 09:02:09.029
200	17	baskom bahtera TM	-	6750	50	15	f	0	Rp	181	3eebf179353f	baskom-bahtera-tm-3eebf179353f	2026-04-20 11:35:57.164	2026-04-20 11:35:57.164
201	16	baskom mawar	-	7050	50	15	f	0	Rp	181	4cf850dc514b	baskom-mawar-4cf850dc514b	2026-04-20 11:35:57.174	2026-04-20 11:35:57.174
202	23	smile 12	-	6650	50	15	f	0	Rp	181	7b4b20315509	smile-12-7b4b20315509	2026-04-20 11:35:57.18	2026-04-20 11:35:57.18
207	17	baskom bahtera TM	-	6750	50	15	f	0	Rp	183	303e213e8e0f	baskom-bahtera-tm-303e213e8e0f	2026-04-20 11:44:21.727	2026-04-20 11:44:21.727
208	16	baskom mawar	-	7050	50	15	f	0	Rp	183	a866051992c6	baskom-mawar-a866051992c6	2026-04-20 11:44:21.737	2026-04-20 11:44:21.737
209	23	smile 12	-	6650	50	15	f	0	Rp	183	e3e195d2982f	smile-12-e3e195d2982f	2026-04-20 11:44:21.741	2026-04-20 11:44:21.741
210	41	Wakul Tanggok	Lusin	5800	50	1	f	0	Rp	167	9a655df17f85	wakul-tanggok-9a655df17f85	2026-04-20 12:02:58.44	2026-04-20 12:02:58.44
211	26	Bahkom Bahtera	-	5600	50	5	f	0	Rp	184	c7b7875afece	bahkom-bahtera-c7b7875afece	2026-04-20 12:06:48.811	2026-04-20 12:06:48.811
212	31	wakul tradisi super	-	3100	40	30	f	0	Rp	184	a06e62231689	wakul-tradisi-super-a06e62231689	2026-04-20 12:06:48.818	2026-04-20 12:06:48.818
213	39	tradisi cerah	Lusin	4200	30	20	f	0	Rp	184	44c24ef50126	tradisi-cerah-44c24ef50126	2026-04-20 12:06:48.822	2026-04-20 12:06:48.822
214	36	telor japar	-	2100	20	200	f	0	Rp	184	2b87c93df5c7	telor-japar-2b87c93df5c7	2026-04-20 12:06:48.829	2026-04-20 12:06:48.829
215	21	tradisi super 2	-	3100	40	32	f	0	Rp	185	26fb97ac4a8e	tradisi-super-2-26fb97ac4a8e	2026-04-20 12:13:40.707	2026-04-20 12:13:40.707
216	13	baskom panda	-	7200	40	7	f	0	Rp	185	9dadc44071ca	baskom-panda-9dadc44071ca	2026-04-20 12:13:40.712	2026-04-20 12:13:40.712
217	21	tradisi super 2	-	3100	40	32	f	0	Rp	186	e95865d37e47	tradisi-super-2-e95865d37e47	2026-04-20 12:17:41.578	2026-04-20 12:17:41.578
218	22	baskom panda super	-	7200	40	7	f	0	Rp	186	92709b9abbf0	baskom-panda-super-92709b9abbf0	2026-04-20 12:17:41.583	2026-04-20 12:17:41.583
219	17	baskom bahtera TM	-	5400	50	5	f	0	Rp	186	c014fe9a0f3e	baskom-bahtera-tm-c014fe9a0f3e	2026-04-20 12:17:41.587	2026-04-20 12:17:41.588
220	24	wakul rehana	-	4000	50	10	f	0	Rp	186	684a6b175579	wakul-rehana-684a6b175579	2026-04-20 12:17:41.592	2026-04-20 12:17:41.592
221	16	baskom mawar	-	5850	50	3	f	0	Rp	186	d8c4b4a3ff49	baskom-mawar-d8c4b4a3ff49	2026-04-20 12:17:41.596	2026-04-20 12:17:41.596
222	23	smile 12	-	5400	50	15	f	0	Rp	187	f4209cb9a028	smile-12-f4209cb9a028	2026-04-20 12:20:58.37	2026-04-20 12:20:58.371
223	17	baskom bahtera TM	-	5400	50	10	f	0	Rp	187	2482fb8db375	baskom-bahtera-tm-2482fb8db375	2026-04-20 12:20:58.376	2026-04-20 12:20:58.376
224	22	baskom panda super	-	7200	50	4	f	0	Rp	187	1874a2da90fb	baskom-panda-super-1874a2da90fb	2026-04-20 12:20:58.38	2026-04-20 12:20:58.38
225	31	wakul tradisi super	-	3100	40	8	f	0	Rp	187	62a36314824d	wakul-tradisi-super-62a36314824d	2026-04-20 12:20:58.385	2026-04-20 12:20:58.385
226	37	telor tali	Lusin	2100	20	70	f	0	Rp	188	6f632df678b9	telor-tali-6f632df678b9	2026-04-20 12:26:54.926	2026-04-20 12:26:54.926
227	18	bak kuping12	-	13000	50	3	f	0	Rp	190	b8bdc41abbda	bak-kuping12-b8bdc41abbda	2026-04-20 12:42:13.408	2026-04-20 12:42:13.408
228	42	Baskom Rotan	Lusin	8400	40	6	f	0	Rp	190	00ab3688529e	baskom-rotan-00ab3688529e	2026-04-20 12:42:13.419	2026-04-20 12:42:13.419
229	26	Bahkom Bahtera	-	6500	50	5	f	0	Rp	190	7b309b9ba89c	bahkom-bahtera-7b309b9ba89c	2026-04-20 12:42:13.425	2026-04-20 12:42:13.425
230	20	baskom jago	-	5800	50	5	f	0	Rp	190	9c977df0da96	baskom-jago-9c977df0da96	2026-04-20 12:42:13.433	2026-04-20 12:42:13.433
231	23	smile 12	-	5800	50	5	f	0	Rp	190	81d091f02c40	smile-12-81d091f02c40	2026-04-20 12:42:13.438	2026-04-20 12:42:13.438
232	36	telor japar	-	2400	20	5	f	0	Rp	190	7ff0960a8d5d	telor-japar-7ff0960a8d5d	2026-04-20 12:42:13.442	2026-04-20 12:42:13.442
233	17	baskom bahtera TM	-	5400	50	15	f	0	Rp	191	38130e3940a8	baskom-bahtera-tm-38130e3940a8	2026-04-20 12:48:49.217	2026-04-20 12:48:49.217
234	23	smile 12	-	5400	50	3	f	0	Rp	191	fe7b27e62c29	smile-12-fe7b27e62c29	2026-04-20 12:48:49.223	2026-04-20 12:48:49.223
235	26	Bahkom Bahtera	-	6100	50	51	f	0	Rp	192	8299d82592bd	bahkom-bahtera-8299d82592bd	2026-04-20 12:50:42.764	2026-04-20 12:50:42.764
236	31	wakul tradisi super	-	3300	40	100	f	0	Rp	192	820b76d28798	wakul-tradisi-super-820b76d28798	2026-04-20 12:50:42.769	2026-04-20 12:50:42.769
237	20	baskom jago	-	5600	50	100	f	0	Rp	193	b3dca834903e	baskom-jago-b3dca834903e	2026-04-20 12:56:37.952	2026-04-20 12:56:37.952
238	36	telor japar	-	2100	20	50	f	0	Rp	194	79f00e14a83e	telor-japar-79f00e14a83e	2026-04-20 13:32:46.214	2026-04-20 13:32:46.215
239	31	wakul tradisi super	-	3300	20	150	f	0	Rp	194	e6aeeb53643f	wakul-tradisi-super-e6aeeb53643f	2026-04-20 13:32:46.219	2026-04-20 13:32:46.219
240	43	Wakul Mawar Super	Lusin	3600	50	20	f	0	Rp	195	36617920434e	wakul-mawar-super-36617920434e	2026-04-20 18:36:57.254	2026-04-20 18:36:57.254
241	38	Smile 14	Lusin	8400	40	15	f	0	Rp	195	5683209e656b	smile-14-5683209e656b	2026-04-20 18:39:54.391	2026-04-20 18:39:54.391
242	45	Wakul Morris Super	Lusin	5700	40	15	f	0	Rp	195	8b72a72a5ff2	wakul-morris-super-8b72a72a5ff2	2026-04-20 18:44:34.319	2026-04-20 18:44:34.319
243	22	baskom panda super	-	7250	40	10	f	0	Rp	195	722a459f52fc	baskom-panda-super-722a459f52fc	2026-04-20 18:44:34.324	2026-04-20 18:44:34.324
244	17	baskom bahtera TM	-	5800	50	15	f	0	Rp	195	995634e6e91c	baskom-bahtera-tm-995634e6e91c	2026-04-20 18:44:34.33	2026-04-20 18:44:34.33
245	23	smile 12	-	5700	50	10	f	0	Rp	195	30b03b79b75c	smile-12-30b03b79b75c	2026-04-20 18:44:34.334	2026-04-20 18:44:34.334
246	20	baskom jago	-	5400	50	107	f	0	Rp	196	6bf28d847a43	baskom-jago-6bf28d847a43	2026-04-20 18:50:53.459	2026-04-20 18:50:53.459
247	26	Bahkom Bahtera	-	5900	50	30	f	0	Rp	196	29c011670396	bahkom-bahtera-29c011670396	2026-04-20 18:50:53.464	2026-04-20 18:50:53.464
248	13	baskom panda	-	7200	40	10	f	0	Rp	196	db9e2b55b9e3	baskom-panda-db9e2b55b9e3	2026-04-20 18:50:53.47	2026-04-20 18:50:53.47
249	36	telor japar	-	2100	20	58	f	0	Rp	196	5291ee737b25	telor-japar-5291ee737b25	2026-04-20 18:50:53.474	2026-04-20 18:50:53.474
250	37	telor tali	Lusin	3200	20	128	f	0	Rp	196	68d146af2205	telor-tali-68d146af2205	2026-04-20 18:50:53.478	2026-04-20 18:50:53.478
252	17	baskom bahtera TM	-	5850	50	50	f	0	Rp	197	0e543178f0a6	baskom-bahtera-tm-0e543178f0a6	2026-04-20 18:54:32.516	2026-04-20 18:54:42.676
253	16	baskom mawar	-	5900	50	20	f	0	Rp	197	545c4d71743d	baskom-mawar-545c4d71743d	2026-04-20 18:55:47.483	2026-04-20 18:55:47.483
254	45	Wakul Morris Super	Lusin	5750	40	25	f	0	Rp	197	a4934cfc75e9	wakul-morris-super-a4934cfc75e9	2026-04-20 18:56:46.601	2026-04-20 18:56:46.601
255	26	Baskom Bahtera	-	6100	50	60	f	0	Rp	197	102cf89c7ff7	baskom-bahtera-102cf89c7ff7	2026-04-20 18:58:34.725	2026-04-20 18:58:34.725
256	23	smile 12	-	6700	50	25	f	0	Rp	198	8fbe43f563e4	smile-12-8fbe43f563e4	2026-04-20 19:05:36.457	2026-04-20 19:05:36.457
257	16	baskom mawar	-	7100	50	5	f	0	Rp	198	898a83a2c8e4	baskom-mawar-898a83a2c8e4	2026-04-20 19:05:36.462	2026-04-20 19:06:11.213
258	26	Baskom Bahtera	-	7500	50	5	f	0	Rp	198	a58a2b686524	baskom-bahtera-a58a2b686524	2026-04-20 19:08:53.784	2026-04-20 19:08:53.784
259	30	Baskom Durian	-	8900	40	5	f	0	Rp	198	2c574d987ff7	baskom-durian-2c574d987ff7	2026-04-20 19:08:53.789	2026-04-20 19:08:53.789
260	36	telor japar	-	2600	20	70	f	0	Rp	200	9a62ba0ba35b	telor-japar-9a62ba0ba35b	2026-04-21 16:51:03.164	2026-04-21 16:51:03.164
261	37	telor tali	Lusin	3700	20	80	f	0	Rp	200	652d2a1988fd	telor-tali-652d2a1988fd	2026-04-21 16:51:03.168	2026-04-21 16:51:03.168
262	23	smile 12	-	6900	50	2	f	0	Rp	200	88657a23c015	smile-12-88657a23c015	2026-04-21 16:51:03.171	2026-04-21 16:51:03.172
263	26	Baskom Bahtera	-	7300	50	50	f	0	Rp	200	f13f2946b435	baskom-bahtera-f13f2946b435	2026-04-21 16:51:03.174	2026-04-21 16:51:03.174
264	20	baskom jago	-	7000	50	115	f	0	Rp	200	bae39e72b60e	baskom-jago-bae39e72b60e	2026-04-21 16:51:03.177	2026-04-21 16:51:03.177
265	22	baskom panda super	-	8500	40	6	f	0	Rp	200	d5f87a9f71cc	baskom-panda-super-d5f87a9f71cc	2026-04-21 16:51:03.182	2026-04-21 16:51:03.182
266	34	Tradisi Super 30	-	3600	30	30	f	0	Rp	202	4b2b85bf759e	tradisi-super-30-4b2b85bf759e	2026-04-21 20:51:42.9	2026-04-21 20:51:42.9
267	36	telor japar	-	2500	20	27	f	0	Rp	202	cddfe957b585	telor-japar-cddfe957b585	2026-04-21 20:51:42.911	2026-04-21 20:51:42.911
268	17	baskom bahtera TM	-	7000	50	5	f	0	Rp	202	342b9668ae8a	baskom-bahtera-tm-342b9668ae8a	2026-04-21 20:51:42.921	2026-04-21 20:51:42.921
269	13	baskom panda	-	8400	40	5	f	0	Rp	202	9ea97c5ab9a9	baskom-panda-9ea97c5ab9a9	2026-04-21 20:51:42.929	2026-04-21 20:51:42.929
270	26	Baskom Bahtera	-	7700	50	4	f	0	Rp	203	b6b5190a86ef	baskom-bahtera-b6b5190a86ef	2026-04-23 08:40:46.826	2026-04-23 08:40:46.826
271	16	baskom mawar	-	7500	50	1	f	0	Rp	203	abd3bd257f88	baskom-mawar-abd3bd257f88	2026-04-23 08:40:46.838	2026-04-23 08:40:46.838
272	24	wakul rehana	-	4400	50	20	f	0	Rp	205	3e8ab106b3e5	wakul-rehana-3e8ab106b3e5	2026-04-24 12:52:58.003	2026-04-24 12:52:58.003
273	17	baskom bahtera TM	-	7000	50	5	f	0	Rp	205	365380d361ed	baskom-bahtera-tm-365380d361ed	2026-04-24 12:52:58.007	2026-04-24 12:52:58.007
274	18	bak kuping12	-	14300	30	14	f	0	Rp	206	76a146dbd17d	bak-kuping12-76a146dbd17d	2026-04-27 10:01:50.767	2026-04-27 10:01:50.767
275	16	baskom mawar	-	7600	50	25	f	0	Rp	206	3f7c0b20829c	baskom-mawar-3f7c0b20829c	2026-04-27 10:01:50.771	2026-04-27 10:01:50.771
276	17	baskom bahtera TM	-	9250	40	40	f	0	Rp	206	8b6b7939857c	baskom-bahtera-tm-8b6b7939857c	2026-04-27 10:01:50.776	2026-04-27 10:01:50.776
277	17	baskom bahtera TM	-	7250	50	90	f	0	Rp	207	980706cda58b	baskom-bahtera-tm-980706cda58b	2026-04-27 10:37:35.958	2026-04-27 10:41:15.134
278	16	baskom mawar	-	7200	50	15	f	0	Rp	209	8aa523519b37	baskom-mawar-8aa523519b37	2026-04-28 08:46:32.735	2026-04-28 08:46:32.735
279	17	baskom bahtera TM	-	7000	50	10	f	0	Rp	209	660c6aac3430	baskom-bahtera-tm-660c6aac3430	2026-04-28 08:46:32.739	2026-04-28 08:46:32.739
280	20	baskom jago	-	7000	50	10	f	0	Rp	209	3ef6efd54def	baskom-jago-3ef6efd54def	2026-04-28 08:46:32.742	2026-04-28 08:46:32.742
281	26	Baskom Bahtera	-	7700	50	20	f	0	Rp	210	296064e108e5	baskom-bahtera-296064e108e5	2026-04-28 10:09:55.252	2026-04-28 10:09:55.253
282	13	baskom panda	-	8600	40	20	f	0	Rp	210	7ae71a14be1f	baskom-panda-7ae71a14be1f	2026-04-28 10:09:55.256	2026-04-28 10:09:55.256
283	16	baskom mawar	-	7400	50	10	f	0	Rp	210	ec45f602afe9	baskom-mawar-ec45f602afe9	2026-04-28 10:09:55.259	2026-04-28 10:09:55.259
284	46	BMP	Lusin	7000	50	60	f	0	Rp	211	b67d475c	slug_284_36010001-617e-47b9-abbd-e22cc2bd2bf3	2026-05-01 15:19:25.31	2026-05-01 15:19:25.31
475	20	baskom jago	Lusin	7000	50	120	f	0	Rp	259	56ee39b5	BMP-2605-010-c94477e0-012d413b	2026-05-26 16:10:18.658	2026-05-26 16:10:18.658
315	18	bak kuping12	-	13000	30	50	f	0	Rp	248	f934cfbf	BMP-2605-001-88774ad6	2026-05-02 08:13:22.511	2026-05-02 08:13:22.511
316	16	baskom mawar	-	7400	50	15	f	0	Rp	248	ac8ecac7	BMP-2605-001-431c12b9	2026-05-02 08:13:22.521	2026-05-02 08:13:22.521
317	46	BMP	Lusin	7200	50	15	f	0	Rp	248	72221160	BMP-2605-001-654e7f6c	2026-05-02 08:13:22.529	2026-05-02 08:13:22.529
318	20	baskom jago	-	7000	50	7	f	0	Rp	248	c13d582a	BMP-2605-001-f789cb57	2026-05-02 08:13:22.535	2026-05-02 08:13:22.535
476	26	Baskom Bahtera	Lusin	7300	50	20	f	0	Rp	259	bc72e2ff	BMP-2605-010-c94477e0-04f90fb5	2026-05-26 16:10:18.666	2026-05-26 16:10:18.666
477	23	smile 12	Lusin	6900	50	15	f	0	Rp	259	4e847528	BMP-2605-010-c94477e0-adfe71b3	2026-05-26 16:10:18.674	2026-05-26 16:10:18.674
478	38	Smile 14	Lusin	9300	50	36	f	0	Rp	259	8cced3e9	BMP-2605-010-c94477e0-90526fb3	2026-05-26 16:10:18.683	2026-05-26 16:10:18.683
479	51	Baskom Panda Cerah	Lusin	10000	40	5	f	0	Rp	259	16ceba72	BMP-2605-010-c94477e0-2bf44bea	2026-05-26 16:10:18.691	2026-05-26 16:10:18.691
480	37	telor tali	Lusin	3700	20	97	t	0	Rp	259	e67f381d	BMP-2605-010-c94477e0-faf102af	2026-05-26 16:10:18.699	2026-05-26 16:10:18.699
343	17	baskom bahtera TM	-	7000	50	15	f	0	Rp	250	524e0cf0	BMP-2605-001-d1aebb79-165326fe	2026-05-02 16:05:33.135	2026-05-02 16:05:33.135
344	20	baskom jago	-	7000	50	10	f	0	Rp	250	b4e743dc	BMP-2605-001-d1aebb79-34fe2793	2026-05-02 16:05:33.144	2026-05-02 16:05:33.144
345	23	smile 12	-	7000	50	10	f	0	Rp	250	51b24081	BMP-2605-001-d1aebb79-1fb46617	2026-05-02 16:05:33.152	2026-05-02 16:05:33.152
346	16	baskom mawar	-	7200	50	10	f	0	Rp	250	1156f6b7	BMP-2605-001-d1aebb79-148c1cf5	2026-05-02 16:05:33.161	2026-05-02 16:05:33.161
347	13	baskom panda	-	8400	40	2	f	0	Rp	250	c4a211ee	BMP-2605-001-d1aebb79-f3280fef	2026-05-02 16:05:33.169	2026-05-02 16:05:33.169
348	25	baskom mawar	-	7400	50	25	f	0	Rp	251	42ab698a	BMP-2605-001-9e707f6b-1fd01cd4	2026-05-05 01:25:53.28	2026-05-05 01:25:53.28
349	17	baskom bahtera TM	-	7200	50	10	f	0	Rp	251	9bc3d733	BMP-2605-001-9e707f6b-1738edf0	2026-05-05 01:25:53.293	2026-05-05 01:25:53.293
357	22	baskom panda super	-	8600	40	20	f	0	Rp	255	45102ee9	BMP-2605-004-35ca2747-8d4090ec	2026-05-05 20:49:45.358	2026-05-05 20:49:45.358
358	18	bak kuping12	-	13000	30	70	f	0	Rp	256	35c0ffb0	BMP-2605-005-dd96ce2c-9c61d5a1	2026-05-09 12:35:55.051	2026-05-09 12:35:55.051
359	28	baskom jago 12	-	7000	50	15	f	0	Rp	256	9df322ef	BMP-2605-005-dd96ce2c-6a646098	2026-05-09 12:35:55.059	2026-05-09 12:35:55.059
360	22	baskom panda super	-	8600	40	5	f	0	Rp	256	339a1b57	BMP-2605-005-dd96ce2c-937b6ade	2026-05-09 12:35:55.064	2026-05-09 12:35:55.064
365	13	baskom panda	-	8400	40	10	f	0	Rp	252	f4a3dff9	BMP-2605-005-25268dae-af882674	2026-05-14 08:45:18.477	2026-05-14 08:45:18.477
366	25	baskom mawar	-	7200	50	5	f	0	Rp	252	2f188033	BMP-2605-005-25268dae-3c81e59d	2026-05-14 08:45:18.486	2026-05-14 08:45:18.486
367	39	tradisi cerah	Lusin	4900	30	10	f	0	Rp	252	67a04202	BMP-2605-005-25268dae-e2d7dd3c	2026-05-14 08:45:18.493	2026-05-14 08:45:18.493
372	16	baskom mawar	-	6050	50	10	f	0	Rp	182	754fc320	bmp-0426-032-fd9bd5ccae75-ff5fa4f7	2026-05-14 20:04:16.483	2026-05-14 20:04:16.483
373	17	baskom bahtera TM	-	5750	50	10	f	0	Rp	182	853743f1	bmp-0426-032-fd9bd5ccae75-eb650e79	2026-05-14 20:04:16.497	2026-05-14 20:04:16.497
374	28	baskom jago 12	-	5550	50	10	f	0	Rp	182	0bb896e5	bmp-0426-032-fd9bd5ccae75-6dc183ed	2026-05-14 20:04:16.506	2026-05-14 20:04:16.506
375	30	Baskom Durian	-	6640	50	5	f	0	Rp	182	0d0480d7	bmp-0426-032-fd9bd5ccae75-a7e03515	2026-05-14 20:04:16.515	2026-05-14 20:04:16.515
376	18	bak kuping12	-	12000	30	50	f	0	Rp	258	f887db04	BMP-2605-009-6bea2397-7aaadbf6	2026-05-14 20:15:37.87	2026-05-14 20:15:37.87
377	16	baskom mawar	-	5800	50	15	f	0	Rp	258	11ef4174	BMP-2605-009-6bea2397-069e1bdb	2026-05-14 20:15:37.878	2026-05-14 20:15:37.878
378	20	baskom jago	-	5500	50	15	f	0	Rp	258	d5be10b7	BMP-2605-009-6bea2397-2244ccc7	2026-05-14 20:15:37.887	2026-05-14 20:15:37.887
379	30	Baskom Durian	-	8200	40	10	f	0	Rp	258	df0f82d2	BMP-2605-009-6bea2397-ce5d4c27	2026-05-14 20:15:37.895	2026-05-14 20:15:37.895
380	17	baskom bahtera TM	-	5800	50	10	f	0	Rp	258	f3d11fd9	BMP-2605-009-6bea2397-d99d24ef	2026-05-14 20:15:37.904	2026-05-14 20:15:37.904
381	19	wakul moris	-	4400	50	5	f	0	Rp	258	685969ff	BMP-2605-009-6bea2397-17907574	2026-05-14 20:15:37.912	2026-05-14 20:15:37.912
382	43	Wakul Mawar Super	Lusin	3750	50	5	f	0	Rp	258	7dac131c	BMP-2605-009-6bea2397-fa74a784	2026-05-14 20:15:37.921	2026-05-14 20:15:37.921
390	22	baskom panda super	-	8600	40	70	f	0	Rp	260	0ce18d3a	BMP-2605-011-51099424-b5fd0a1e	2026-05-15 08:27:26.26	2026-05-15 08:27:26.26
391	29	Baskom TM	lusin	7250	50	10	f	0	Rp	260	efd9670d	BMP-2605-011-51099424-df92eb09	2026-05-15 08:27:26.271	2026-05-15 08:27:26.271
392	51	Baskom Panda Cerah	Lusin	10000	40	30	f	0	Rp	260	57904c99	BMP-2605-011-51099424-fbfc1ec2	2026-05-15 08:27:26.282	2026-05-15 08:27:26.282
407	28	baskom jago 12	Lusin	7000	50	15	f	0	Rp	261	e49cec45	BMP-2605-012-006f3869-ed333c03	2026-05-15 17:23:08.041	2026-05-15 17:23:08.041
408	16	baskom mawar	Lusin	7100	50	5	f	0	Rp	261	2ec25522	BMP-2605-012-006f3869-144759b8	2026-05-15 17:23:08.058	2026-05-15 17:23:08.058
409	17	baskom bahtera TM	Lusin	7000	50	10	f	0	Rp	261	a1d23d45	BMP-2605-012-006f3869-b02e4c44	2026-05-15 17:23:08.067	2026-05-15 17:23:08.067
410	29	Baskom TM	lusin	7500	50	100	f	0	Rp	262	a0c9fed5	BMP-2605-013-4d43d330-615d9ed4	2026-05-15 21:07:39.351	2026-05-15 21:07:39.351
411	46	BMP	Lusin	7500	50	50	f	0	Rp	262	012cebe8	BMP-2605-013-4d43d330-85e761fc	2026-05-15 21:07:39.364	2026-05-15 21:07:39.364
412	51	Baskom Panda Cerah	Lusin	10000	40	10	f	0	Rp	262	7973a827	BMP-2605-013-4d43d330-89328e02	2026-05-15 21:07:39.372	2026-05-15 21:07:39.372
413	16	baskom mawar	Lusin	7500	50	15	f	0	Rp	262	cce6c380	BMP-2605-013-4d43d330-2fba67dc	2026-05-15 21:07:39.381	2026-05-15 21:07:39.381
414	43	Wakul Mawar Super	Lusin	4000	50	13	f	0	Rp	262	d2f8f071	BMP-2605-013-4d43d330-c90a75da	2026-05-15 21:07:39.389	2026-05-15 21:07:39.389
415	46	BMP	Lusin	7400	50	120	f	0	Rp	263	79954f3f	BMP-2605-014-13b59092-9cdf3c60	2026-05-22 11:25:42.715	2026-05-22 11:25:42.715
416	29	Baskom TM	lusin	7350	50	20	f	0	Rp	263	05344c53	BMP-2605-014-13b59092-a65a1e2e	2026-05-22 11:25:42.727	2026-05-22 11:25:42.727
417	16	baskom mawar	Lusin	7400	50	25	f	0	Rp	263	31e68a6b	BMP-2605-014-13b59092-21285ec2	2026-05-22 11:25:42.736	2026-05-22 11:25:42.736
418	19	wakul moris	Lusin	6700	50	10	f	0	Rp	263	6da2d879	BMP-2605-014-13b59092-466add53	2026-05-22 11:25:42.744	2026-05-22 11:25:42.744
419	51	Baskom Panda Cerah	Lusin	10000	40	10	f	0	Rp	263	db109d9a	BMP-2605-014-13b59092-da1a9c8f	2026-05-22 11:25:42.752	2026-05-22 11:25:42.752
422	46	BMP	Lusin	7500	50	50	f	0	Rp	264	109a6ce4	BMP-2605-015-88bb7619-9b11e1ba	2026-05-22 11:32:37.17	2026-05-22 11:32:37.17
423	33	wakul kotak	Lusin	5500	20	20	t	5100	Rp	264	85b9fbd7	BMP-2605-015-88bb7619-ebeb09a3	2026-05-22 11:32:37.178	2026-05-22 11:32:37.178
424	17	baskom bahtera TM	Lusin	7200	50	20	f	0	Rp	265	08e7429e	BMP-2605-016-3df6596f-84a66aae	2026-05-22 14:05:15.778	2026-05-22 14:05:15.778
425	13	baskom panda	Lusin	8600	40	20	f	0	Rp	265	c2beea11	BMP-2605-016-3df6596f-92654896	2026-05-22 14:05:15.786	2026-05-22 14:05:15.786
499	17	baskom bahtera TM	Lusin	6800	50	15	f	0	Rp	180	e0f2e987	bmp-0426-030-7ed262bd1c5a-489fc572	2026-05-29 22:22:02.739	2026-05-29 22:22:02.739
465	35	wakul telur	Lusin	2600	20	130	t	0	Rp	257	3c8d1c7c	BMP-2605-008-b971b6d0-05d6414f	2026-05-26 16:06:27.538	2026-05-26 16:06:27.538
466	13	baskom panda	Lusin	8400	40	10	f	0	Rp	257	ff7a7dbb	BMP-2605-008-b971b6d0-de1c9f75	2026-05-26 16:06:27.558	2026-05-26 16:06:27.558
467	16	baskom mawar	Lusin	7200	50	5	f	0	Rp	257	052dd66e	BMP-2605-008-b971b6d0-c126f13d	2026-05-26 16:06:27.569	2026-05-26 16:06:27.569
468	39	tradisi cerah	Lusin	4900	30	10	f	0	Rp	257	dd078c93	BMP-2605-008-b971b6d0-030f5413	2026-05-26 16:06:27.578	2026-05-26 16:06:27.578
500	18	bak kuping12	Lusin	13600	30	6	f	0	Rp	180	99d6a338	bmp-0426-030-7ed262bd1c5a-b4df63cc	2026-05-29 22:22:02.744	2026-05-29 22:22:02.744
501	13	baskom panda	Lusin	7900	40	5	f	0	Rp	180	7a346a9d	bmp-0426-030-7ed262bd1c5a-9667de4b	2026-05-29 22:22:02.75	2026-05-29 22:22:02.75
505	46	BMP	Lusin	7100	50	6	f	0	Rp	270	458b2d66	BMP-2605-018-915e3416-60cee0ac	2026-05-30 09:01:22.161	2026-05-30 09:01:22.161
506	20	baskom jago	Lusin	7000	50	15	f	0	Rp	270	a43bf349	BMP-2605-018-915e3416-9def8d61	2026-05-30 09:01:22.17	2026-05-30 09:01:22.17
507	45	Wakul Morris Super	Lusin	6700	40	6	f	0	Rp	270	4f80c9d6	BMP-2605-018-915e3416-62b9a648	2026-05-30 09:01:22.177	2026-05-30 09:01:22.177
508	18	bak kuping12	Lusin	14000	30	3	f	0	Rp	271	ee2330b3	BMP-2606-001-a849f0da-363a9b02	2026-06-01 09:30:00.673	2026-06-01 09:30:00.673
509	46	BMP	Lusin	7100	50	10	f	0	Rp	271	3d755088	BMP-2606-001-a849f0da-292ae7cc	2026-06-01 09:30:00.687	2026-06-01 09:30:00.687
510	22	baskom panda super	Lusin	8400	40	3	f	0	Rp	271	26f27608	BMP-2606-001-a849f0da-f40f59e6	2026-06-01 09:30:00.696	2026-06-01 09:30:00.696
514	18	bak kuping12	Lusin	14000	30	13	f	0	Rp	272	27901606	BMP-2606-002-ec7e89b5-944f807a	2026-06-08 12:47:18.336	2026-06-08 12:47:18.336
515	46	BMP	Lusin	7000	50	10	f	0	Rp	272	0d426204	BMP-2606-002-ec7e89b5-0da73f0b	2026-06-08 12:47:18.341	2026-06-08 12:47:18.341
516	20	baskom jago	Lusin	7000	50	3	f	0	Rp	272	f5db5b1e	BMP-2606-002-ec7e89b5-3699b1a1	2026-06-08 12:47:18.345	2026-06-08 12:47:18.345
517	30	Baskom Durian	Lusin	9100	40	27	f	0	Rp	266	34a92135	BMP-2605-017-00c25e20-ce1e3fae	2026-06-08 13:00:39.019	2026-06-08 13:00:39.019
518	22	baskom panda super	Lusin	8600	40	16	f	0	Rp	266	fda193df	BMP-2605-017-00c25e20-ede033c5	2026-06-08 13:00:39.026	2026-06-08 13:00:39.026
519	46	BMP	Lusin	7200	50	8	f	0	Rp	266	266faa07	BMP-2605-017-00c25e20-971d61c6	2026-06-08 13:00:39.032	2026-06-08 13:00:39.032
520	25	baskom mawar	Lusin	7400	50	5	f	0	Rp	266	a269bbbd	BMP-2605-017-00c25e20-a9468b9c	2026-06-08 13:00:39.039	2026-06-08 13:00:39.039
528	18	bak kuping12	-	13000	30	50	f	0	Rp	275	18d76dc3609ac13e052df0008c54f172	BMP-2605-001-88774ad6-fa0c10d8	2026-06-06 12:00:00	2026-06-06 12:00:00
529	16	baskom mawar	-	7400	50	15	f	0	Rp	275	b78eeb642e5c38bd6e769567825402bf	BMP-2605-001-431c12b9-d5bb84f3	2026-06-06 12:00:00	2026-06-06 12:00:00
530	46	BMP	Lusin	7200	50	15	f	0	Rp	275	446fe3fffd25da53d6c522788653791c	BMP-2605-001-654e7f6c-22d772c5	2026-06-06 12:00:00	2026-06-06 12:00:00
531	20	baskom jago	-	7000	50	7	f	0	Rp	275	b8dd4d84d47ac0f2a9d10b962c1122cd	BMP-2605-001-f789cb57-3a772a08	2026-06-06 12:00:00	2026-06-06 12:00:00
\.


--
-- Data for Name: BmpSettings; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."BmpSettings" (id, "clientName", "clientLogo", "addressLine1", province, "postalCode", "phoneNumber", "emailAddress", "taxNumber", "listrikBulanan", "jumlahMesin", "jumlahKaryawan", "gajiHarian", "hariKerjaSebulan", "biayaKarungPer1000", "hoursPerDay", "uniqueID", slug, "createdAt", "updatedAt") FROM stdin;
1	CV. BAHTERA MULYA PLASTIK		jl. arimbi, RT04 RW 01 Desa Ngrimbi	Jatim		088986084722	bahteramulyap@gmail.com		36000000	5	21	85000	26	1700000	24		main-settings	2026-04-29 02:22:16.281	2026-05-30 15:29:08.965
\.


--
-- Data for Name: Car; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."Car" (id, name, "plateNumber", type, "pricePerDay", status, "createdAt", "updatedAt") FROM stdin;
1	avanza	B 76272 GN	MPV	350000	AVAILABLE	2026-05-24 04:45:53.822	2026-05-24 05:06:22.155
\.


--
-- Data for Name: Customer; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: Employee; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."Employee" (id, name, role, pin, salary, "createdAt", "updatedAt", email, "outletId") FROM stdin;
1	Nama Customer	OWNER		0	2026-06-04 03:57:50.621	2026-06-04 03:57:50.621	bahteramulyap@gmail.com	\N
2	syerli	ADMIN	5819ef0d24208780b75c18009f0f69400eb933916f800ae980b778820cda595e3151de8600a0c325711f0e9641b5a72f393008868913578601ba0fa0d4c9ad93	1000000	2026-06-07 18:12:10.384	2026-06-07 18:18:36.064	syerlirahma7@gmail.com	2
\.


--
-- Data for Name: Finance; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: GoogleUser; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."GoogleUser" (id, email, "registeredAt", "updatedAt", "businessMode", "confirmToken", "confirmedAt", "demoExpiresAt", "isConfirmed", "passwordHash", "userName", whatsapp) FROM stdin;
\.


--
-- Data for Name: LaundryExpense; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."LaundryExpense" (id, kategori, nominal, keterangan, tanggal, "createdAt", "updatedAt") FROM stdin;
\.


--
-- Data for Name: LaundryOrder; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."LaundryOrder" (id, "receiptNumber", "namaPelanggan", "noHp", "jenisLayanan", "jenisLaundry", "totalHarga", "statusBayar", status, "tanggalMasuk", "tanggalSelesai", selimut, sprei, boneka, korden, lokasi, "employeeId", "customerId", "createdAt", "updatedAt", "outletId") FROM stdin;
\.


--
-- Data for Name: LaundryService; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."LaundryService" (id, kategori, proses, nama, harga, satuan, waktu, icon, "createdAt", "updatedAt") FROM stdin;
1	Klioan	Cuci >> Kering 	Reguler	5000	Kg	3 Hari	🧺	2026-06-04 04:08:25.979	2026-06-04 04:08:25.979
\.


--
-- Data for Name: Outlet; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."Outlet" (id, name, address, phone, "createdAt", "updatedAt") FROM stdin;
1	surabaya - rs. soetomo	jl. demo 	082245077959	2026-06-07 18:10:25.827	2026-06-07 18:14:02.736
2	sidoarjo - RSUD	jl. majapahit	082245077889	2026-06-07 18:14:30.513	2026-06-07 18:14:30.513
\.


--
-- Data for Name: PremiumUser; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."PremiumUser" (id, email, "passwordHash", name, role, "registeredAt", "updatedAt", "tenantId", whatsapp, "deletionScheduledAt", "lastPaymentConfirmedAt") FROM stdin;
\.


--
-- Data for Name: Product; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: Promo; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."Promo" (id, name, type, value, "isActive", "createdAt", "updatedAt") FROM stdin;
\.


--
-- Data for Name: PurchaseOrder; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."PurchaseOrder" (id, "supplierId", date, status, notes, total, "createdAt", "updatedAt") FROM stdin;
\.


--
-- Data for Name: PurchaseOrderItem; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."PurchaseOrderItem" (id, "purchaseOrderId", "productId", quantity, "costPrice") FROM stdin;
\.


--
-- Data for Name: Rental; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."Rental" (id, "carId", "customerId", "customerName", "startDate", "endDate", "totalPrice", status, "actualReturnDate", "lateFee", "employeeId", "identityText", "createdAt", "updatedAt") FROM stdin;
2	1	\N	rizky	2026-05-24 00:00:00	2026-05-25 00:00:00	350000	RETURNED	2026-05-24 00:00:00	0	2	data:image/jpeg;base64,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	2026-05-24 05:05:09.8	2026-05-31 07:05:56.507
\.


--
-- Data for Name: Supplier; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."Supplier" (id, name, phone, address, notes, "createdAt", "updatedAt") FROM stdin;
\.


--
-- Data for Name: TenantLimit; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."TenantLimit" ("tenantId", "limit", "createdAt", "updatedAt") FROM stdin;
\.


--
-- Data for Name: Transaction; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: TransactionItem; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Name: ActivityLog_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."ActivityLog_id_seq"', 14, true);


--
-- Name: BmpAdmsDevice_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."BmpAdmsDevice_id_seq"', 1, true);


--
-- Name: BmpAttendanceLog_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."BmpAttendanceLog_id_seq"', 341, true);


--
-- Name: BmpBahanNonoItem_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."BmpBahanNonoItem_id_seq"', 94, true);


--
-- Name: BmpBahanNono_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."BmpBahanNono_id_seq"', 93, true);


--
-- Name: BmpCashFlow_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."BmpCashFlow_id_seq"', 472, true);


--
-- Name: BmpClient_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."BmpClient_id_seq"', 36, true);


--
-- Name: BmpDeviceTenant_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."BmpDeviceTenant_id_seq"', 1, false);


--
-- Name: BmpEmployee_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."BmpEmployee_id_seq"', 24, true);


--
-- Name: BmpInvoicePayment_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."BmpInvoicePayment_id_seq"', 34, true);


--
-- Name: BmpInvoice_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."BmpInvoice_id_seq"', 275, true);


--
-- Name: BmpMachineBonusLog_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."BmpMachineBonusLog_id_seq"', 21, true);


--
-- Name: BmpMasterProduct_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."BmpMasterProduct_id_seq"', 52, true);


--
-- Name: BmpPayroll_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."BmpPayroll_id_seq"', 1, true);


--
-- Name: BmpPembayaran_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."BmpPembayaran_id_seq"', 1, true);


--
-- Name: BmpPembelianBarang_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."BmpPembelianBarang_id_seq"', 1, false);


--
-- Name: BmpPembelianItem_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."BmpPembelianItem_id_seq"', 1, false);


--
-- Name: BmpProduct_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."BmpProduct_id_seq"', 531, true);


--
-- Name: BmpSettings_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."BmpSettings_id_seq"', 1, true);


--
-- Name: Car_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."Car_id_seq"', 1, true);


--
-- Name: Customer_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."Customer_id_seq"', 2, true);


--
-- Name: Employee_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."Employee_id_seq"', 2, true);


--
-- Name: Finance_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."Finance_id_seq"', 42, true);


--
-- Name: LaundryExpense_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."LaundryExpense_id_seq"', 1, false);


--
-- Name: LaundryOrder_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."LaundryOrder_id_seq"', 1, true);


--
-- Name: LaundryService_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."LaundryService_id_seq"', 1, true);


--
-- Name: Outlet_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."Outlet_id_seq"', 2, true);


--
-- Name: Product_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."Product_id_seq"', 26, true);


--
-- Name: Promo_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."Promo_id_seq"', 1, false);


--
-- Name: PurchaseOrderItem_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."PurchaseOrderItem_id_seq"', 1, false);


--
-- Name: PurchaseOrder_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."PurchaseOrder_id_seq"', 1, false);


--
-- Name: Rental_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."Rental_id_seq"', 2, true);


--
-- Name: Supplier_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."Supplier_id_seq"', 1, false);


--
-- Name: TransactionItem_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."TransactionItem_id_seq"', 100, true);


--
-- Name: Transaction_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."Transaction_id_seq"', 103, true);


--
-- Name: ActivityLog ActivityLog_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."ActivityLog"
    ADD CONSTRAINT "ActivityLog_pkey" PRIMARY KEY (id);


--
-- Name: BmpAdmsDevice BmpAdmsDevice_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BmpAdmsDevice"
    ADD CONSTRAINT "BmpAdmsDevice_pkey" PRIMARY KEY (id);


--
-- Name: BmpAttendanceLog BmpAttendanceLog_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BmpAttendanceLog"
    ADD CONSTRAINT "BmpAttendanceLog_pkey" PRIMARY KEY (id);


--
-- Name: BmpBahanNonoItem BmpBahanNonoItem_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BmpBahanNonoItem"
    ADD CONSTRAINT "BmpBahanNonoItem_pkey" PRIMARY KEY (id);


--
-- Name: BmpBahanNono BmpBahanNono_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BmpBahanNono"
    ADD CONSTRAINT "BmpBahanNono_pkey" PRIMARY KEY (id);


--
-- Name: BmpCashFlow BmpCashFlow_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BmpCashFlow"
    ADD CONSTRAINT "BmpCashFlow_pkey" PRIMARY KEY (id);


--
-- Name: BmpClient BmpClient_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BmpClient"
    ADD CONSTRAINT "BmpClient_pkey" PRIMARY KEY (id);


--
-- Name: BmpDeviceTenant BmpDeviceTenant_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BmpDeviceTenant"
    ADD CONSTRAINT "BmpDeviceTenant_pkey" PRIMARY KEY (id);


--
-- Name: BmpEmployee BmpEmployee_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BmpEmployee"
    ADD CONSTRAINT "BmpEmployee_pkey" PRIMARY KEY (id);


--
-- Name: BmpInvoicePayment BmpInvoicePayment_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BmpInvoicePayment"
    ADD CONSTRAINT "BmpInvoicePayment_pkey" PRIMARY KEY (id);


--
-- Name: BmpInvoice BmpInvoice_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BmpInvoice"
    ADD CONSTRAINT "BmpInvoice_pkey" PRIMARY KEY (id);


--
-- Name: BmpMachineBonusLog BmpMachineBonusLog_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BmpMachineBonusLog"
    ADD CONSTRAINT "BmpMachineBonusLog_pkey" PRIMARY KEY (id);


--
-- Name: BmpMasterProduct BmpMasterProduct_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BmpMasterProduct"
    ADD CONSTRAINT "BmpMasterProduct_pkey" PRIMARY KEY (id);


--
-- Name: BmpPayroll BmpPayroll_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BmpPayroll"
    ADD CONSTRAINT "BmpPayroll_pkey" PRIMARY KEY (id);


--
-- Name: BmpPembayaran BmpPembayaran_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BmpPembayaran"
    ADD CONSTRAINT "BmpPembayaran_pkey" PRIMARY KEY (id);


--
-- Name: BmpPembelianBarang BmpPembelianBarang_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BmpPembelianBarang"
    ADD CONSTRAINT "BmpPembelianBarang_pkey" PRIMARY KEY (id);


--
-- Name: BmpPembelianItem BmpPembelianItem_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BmpPembelianItem"
    ADD CONSTRAINT "BmpPembelianItem_pkey" PRIMARY KEY (id);


--
-- Name: BmpProduct BmpProduct_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BmpProduct"
    ADD CONSTRAINT "BmpProduct_pkey" PRIMARY KEY (id);


--
-- Name: BmpSettings BmpSettings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BmpSettings"
    ADD CONSTRAINT "BmpSettings_pkey" PRIMARY KEY (id);


--
-- Name: Car Car_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Car"
    ADD CONSTRAINT "Car_pkey" PRIMARY KEY (id);


--
-- Name: Customer Customer_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Customer"
    ADD CONSTRAINT "Customer_pkey" PRIMARY KEY (id);


--
-- Name: Employee Employee_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Employee"
    ADD CONSTRAINT "Employee_pkey" PRIMARY KEY (id);


--
-- Name: Finance Finance_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Finance"
    ADD CONSTRAINT "Finance_pkey" PRIMARY KEY (id);


--
-- Name: GoogleUser GoogleUser_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."GoogleUser"
    ADD CONSTRAINT "GoogleUser_pkey" PRIMARY KEY (id);


--
-- Name: LaundryExpense LaundryExpense_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."LaundryExpense"
    ADD CONSTRAINT "LaundryExpense_pkey" PRIMARY KEY (id);


--
-- Name: LaundryOrder LaundryOrder_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."LaundryOrder"
    ADD CONSTRAINT "LaundryOrder_pkey" PRIMARY KEY (id);


--
-- Name: LaundryService LaundryService_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."LaundryService"
    ADD CONSTRAINT "LaundryService_pkey" PRIMARY KEY (id);


--
-- Name: Outlet Outlet_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Outlet"
    ADD CONSTRAINT "Outlet_pkey" PRIMARY KEY (id);


--
-- Name: PremiumUser PremiumUser_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."PremiumUser"
    ADD CONSTRAINT "PremiumUser_pkey" PRIMARY KEY (id);


--
-- Name: Product Product_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Product"
    ADD CONSTRAINT "Product_pkey" PRIMARY KEY (id);


--
-- Name: Promo Promo_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Promo"
    ADD CONSTRAINT "Promo_pkey" PRIMARY KEY (id);


--
-- Name: PurchaseOrderItem PurchaseOrderItem_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."PurchaseOrderItem"
    ADD CONSTRAINT "PurchaseOrderItem_pkey" PRIMARY KEY (id);


--
-- Name: PurchaseOrder PurchaseOrder_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."PurchaseOrder"
    ADD CONSTRAINT "PurchaseOrder_pkey" PRIMARY KEY (id);


--
-- Name: Rental Rental_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Rental"
    ADD CONSTRAINT "Rental_pkey" PRIMARY KEY (id);


--
-- Name: Supplier Supplier_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Supplier"
    ADD CONSTRAINT "Supplier_pkey" PRIMARY KEY (id);


--
-- Name: TenantLimit TenantLimit_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."TenantLimit"
    ADD CONSTRAINT "TenantLimit_pkey" PRIMARY KEY ("tenantId");


--
-- Name: TransactionItem TransactionItem_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."TransactionItem"
    ADD CONSTRAINT "TransactionItem_pkey" PRIMARY KEY (id);


--
-- Name: Transaction Transaction_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Transaction"
    ADD CONSTRAINT "Transaction_pkey" PRIMARY KEY (id);


--
-- Name: BmpAdmsDevice_serialNumber_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "BmpAdmsDevice_serialNumber_key" ON public."BmpAdmsDevice" USING btree ("serialNumber");


--
-- Name: BmpDeviceTenant_serialNumber_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "BmpDeviceTenant_serialNumber_key" ON public."BmpDeviceTenant" USING btree ("serialNumber");


--
-- Name: BmpInvoice_slug_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "BmpInvoice_slug_key" ON public."BmpInvoice" USING btree (slug);


--
-- Name: Car_plateNumber_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "Car_plateNumber_key" ON public."Car" USING btree ("plateNumber");


--
-- Name: GoogleUser_confirmToken_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "GoogleUser_confirmToken_key" ON public."GoogleUser" USING btree ("confirmToken");


--
-- Name: GoogleUser_email_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "GoogleUser_email_key" ON public."GoogleUser" USING btree (email);


--
-- Name: LaundryOrder_receiptNumber_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "LaundryOrder_receiptNumber_key" ON public."LaundryOrder" USING btree ("receiptNumber");


--
-- Name: PremiumUser_email_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "PremiumUser_email_key" ON public."PremiumUser" USING btree (email);


--
-- Name: Product_barcode_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "Product_barcode_key" ON public."Product" USING btree (barcode);


--
-- Name: Transaction_receiptNumber_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX "Transaction_receiptNumber_key" ON public."Transaction" USING btree ("receiptNumber");


--
-- Name: ActivityLog ActivityLog_employeeId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."ActivityLog"
    ADD CONSTRAINT "ActivityLog_employeeId_fkey" FOREIGN KEY ("employeeId") REFERENCES public."Employee"(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: BmpBahanNonoItem BmpBahanNonoItem_bahanNonoId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BmpBahanNonoItem"
    ADD CONSTRAINT "BmpBahanNonoItem_bahanNonoId_fkey" FOREIGN KEY ("bahanNonoId") REFERENCES public."BmpBahanNono"(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: BmpCashFlow BmpCashFlow_paymentRefId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BmpCashFlow"
    ADD CONSTRAINT "BmpCashFlow_paymentRefId_fkey" FOREIGN KEY ("paymentRefId") REFERENCES public."BmpInvoicePayment"(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: BmpInvoicePayment BmpInvoicePayment_invoiceId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BmpInvoicePayment"
    ADD CONSTRAINT "BmpInvoicePayment_invoiceId_fkey" FOREIGN KEY ("invoiceId") REFERENCES public."BmpInvoice"(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: BmpInvoice BmpInvoice_clientId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BmpInvoice"
    ADD CONSTRAINT "BmpInvoice_clientId_fkey" FOREIGN KEY ("clientId") REFERENCES public."BmpClient"(id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: BmpMachineBonusLog BmpMachineBonusLog_employeeId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BmpMachineBonusLog"
    ADD CONSTRAINT "BmpMachineBonusLog_employeeId_fkey" FOREIGN KEY ("employeeId") REFERENCES public."BmpEmployee"(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: BmpPayroll BmpPayroll_employeeId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BmpPayroll"
    ADD CONSTRAINT "BmpPayroll_employeeId_fkey" FOREIGN KEY ("employeeId") REFERENCES public."BmpEmployee"(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: BmpPembayaran BmpPembayaran_invoiceId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BmpPembayaran"
    ADD CONSTRAINT "BmpPembayaran_invoiceId_fkey" FOREIGN KEY ("invoiceId") REFERENCES public."BmpInvoice"(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: BmpPembelianItem BmpPembelianItem_pembelianId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BmpPembelianItem"
    ADD CONSTRAINT "BmpPembelianItem_pembelianId_fkey" FOREIGN KEY ("pembelianId") REFERENCES public."BmpPembelianBarang"(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: BmpProduct BmpProduct_invoiceId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BmpProduct"
    ADD CONSTRAINT "BmpProduct_invoiceId_fkey" FOREIGN KEY ("invoiceId") REFERENCES public."BmpInvoice"(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: BmpProduct BmpProduct_masterItemID_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."BmpProduct"
    ADD CONSTRAINT "BmpProduct_masterItemID_fkey" FOREIGN KEY ("masterItemID") REFERENCES public."BmpMasterProduct"(id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: Employee Employee_outletId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Employee"
    ADD CONSTRAINT "Employee_outletId_fkey" FOREIGN KEY ("outletId") REFERENCES public."Outlet"(id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: Finance Finance_customerId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Finance"
    ADD CONSTRAINT "Finance_customerId_fkey" FOREIGN KEY ("customerId") REFERENCES public."Customer"(id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: LaundryOrder LaundryOrder_customerId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."LaundryOrder"
    ADD CONSTRAINT "LaundryOrder_customerId_fkey" FOREIGN KEY ("customerId") REFERENCES public."Customer"(id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: LaundryOrder LaundryOrder_employeeId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."LaundryOrder"
    ADD CONSTRAINT "LaundryOrder_employeeId_fkey" FOREIGN KEY ("employeeId") REFERENCES public."Employee"(id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: LaundryOrder LaundryOrder_outletId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."LaundryOrder"
    ADD CONSTRAINT "LaundryOrder_outletId_fkey" FOREIGN KEY ("outletId") REFERENCES public."Outlet"(id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: Product Product_outletId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Product"
    ADD CONSTRAINT "Product_outletId_fkey" FOREIGN KEY ("outletId") REFERENCES public."Outlet"(id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: PurchaseOrderItem PurchaseOrderItem_productId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."PurchaseOrderItem"
    ADD CONSTRAINT "PurchaseOrderItem_productId_fkey" FOREIGN KEY ("productId") REFERENCES public."Product"(id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: PurchaseOrderItem PurchaseOrderItem_purchaseOrderId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."PurchaseOrderItem"
    ADD CONSTRAINT "PurchaseOrderItem_purchaseOrderId_fkey" FOREIGN KEY ("purchaseOrderId") REFERENCES public."PurchaseOrder"(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: PurchaseOrder PurchaseOrder_supplierId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."PurchaseOrder"
    ADD CONSTRAINT "PurchaseOrder_supplierId_fkey" FOREIGN KEY ("supplierId") REFERENCES public."Supplier"(id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: Rental Rental_carId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Rental"
    ADD CONSTRAINT "Rental_carId_fkey" FOREIGN KEY ("carId") REFERENCES public."Car"(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: Rental Rental_customerId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Rental"
    ADD CONSTRAINT "Rental_customerId_fkey" FOREIGN KEY ("customerId") REFERENCES public."Customer"(id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: Rental Rental_employeeId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Rental"
    ADD CONSTRAINT "Rental_employeeId_fkey" FOREIGN KEY ("employeeId") REFERENCES public."Employee"(id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: TransactionItem TransactionItem_productId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."TransactionItem"
    ADD CONSTRAINT "TransactionItem_productId_fkey" FOREIGN KEY ("productId") REFERENCES public."Product"(id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: TransactionItem TransactionItem_transactionId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."TransactionItem"
    ADD CONSTRAINT "TransactionItem_transactionId_fkey" FOREIGN KEY ("transactionId") REFERENCES public."Transaction"(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: Transaction Transaction_customerId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Transaction"
    ADD CONSTRAINT "Transaction_customerId_fkey" FOREIGN KEY ("customerId") REFERENCES public."Customer"(id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: Transaction Transaction_employeeId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Transaction"
    ADD CONSTRAINT "Transaction_employeeId_fkey" FOREIGN KEY ("employeeId") REFERENCES public."Employee"(id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: Transaction Transaction_outletId_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."Transaction"
    ADD CONSTRAINT "Transaction_outletId_fkey" FOREIGN KEY ("outletId") REFERENCES public."Outlet"(id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- PostgreSQL database dump complete
--

\unrestrict 7qLGNHsOPwnNFDVXfXFfzZtBkMOzjodI3A17S2Yb5JbgPUXOsb2mwljddyJVPRB

