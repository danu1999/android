--
-- PostgreSQL database dump
--

\restrict NRJY6Ic8e98dQM8iMybeIj1nvNlVsFVQ4f3xU5ab50JeVQ5OgtuBOETfVICO67A

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
    "userName" text,
    "passwordHash" text,
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
\.


--
-- Data for Name: BmpAdmsDevice; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."BmpAdmsDevice" (id, "serialNumber", alias, "lastActivity", "createdAt") FROM stdin;
\.


--
-- Data for Name: BmpAttendanceLog; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."BmpAttendanceLog" (id, "deviceSN", "employeePIN", "verifyType", "verifyState", "logTime", "checkOutTime", "workDate", "lateMinutes", alasan, "createdAt") FROM stdin;
\.


--
-- Data for Name: BmpBahanNono; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."BmpBahanNono" (id, tanggal, nominal, notes, tagihan, "totalHarga", "createdAt") FROM stdin;
\.


--
-- Data for Name: BmpBahanNonoItem; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."BmpBahanNonoItem" (id, "bahanNonoId", "jenisBahan", kuantitas, unit, rate) FROM stdin;
\.


--
-- Data for Name: BmpCashFlow; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."BmpCashFlow" (id, "transactionDate", "transactionType", description, amount, "paymentRefId", "createdAt") FROM stdin;
\.


--
-- Data for Name: BmpClient; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."BmpClient" (id, "saldoTitipan", "clientName", "addressLine1", "clientLogo", province, "postalCode", "phoneNumber", "emailAddress", "taxNumber", "uniqueID", slug, "createdAt", "updatedAt") FROM stdin;
\.


--
-- Data for Name: BmpDeviceTenant; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."BmpDeviceTenant" (id, "serialNumber", "tenantId", "createdAt") FROM stdin;
1	BMP_DEV_001	bahteramulyap@gmail.com	2026-06-08 09:15:05.342
2	NHZ4254800403	bahteramulyap@gmail.com	2026-06-08 07:16:55.083
\.


--
-- Data for Name: BmpEmployee; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."BmpEmployee" (id, name, "position", "salaryAmount", "isActive", "fingerprintPIN", "createdAt", "updatedAt") FROM stdin;
\.


--
-- Data for Name: BmpInvoice; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."BmpInvoice" (id, title, number, "dueDate", "paymentTerms", status, notes, "clientId", "uniqueID", slug, "createdAt", "updatedAt") FROM stdin;
\.


--
-- Data for Name: BmpInvoicePayment; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."BmpInvoicePayment" (id, "invoiceId", "paymentDate", "paymentAmount", "paymentMethod", "createdAt") FROM stdin;
\.


--
-- Data for Name: BmpMachineBonusLog; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."BmpMachineBonusLog" (id, "employeeId", "machineName", "shiftType", "bonusAmount", "jumlahPerolehan", date, "createdAt") FROM stdin;
\.


--
-- Data for Name: BmpMasterProduct; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."BmpMasterProduct" (id, title, description, unit, price, "beratGram", "cycleTime", cavity, "rejectRate", "uniqueID", slug, "createdAt", "updatedAt") FROM stdin;
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
\.


--
-- Data for Name: BmpSettings; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."BmpSettings" (id, "clientName", "clientLogo", "addressLine1", province, "postalCode", "phoneNumber", "emailAddress", "taxNumber", "listrikBulanan", "jumlahMesin", "jumlahKaryawan", "gajiHarian", "hariKerjaSebulan", "biayaKarungPer1000", "hoursPerDay", "uniqueID", slug, "createdAt", "updatedAt") FROM stdin;
\.


--
-- Data for Name: Car; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."Car" (id, name, "plateNumber", type, "pricePerDay", status, "createdAt", "updatedAt") FROM stdin;
\.


--
-- Data for Name: Customer; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."Customer" (id, name, phone, address, "createdAt", "updatedAt") FROM stdin;
\.


--
-- Data for Name: Employee; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."Employee" (id, name, role, pin, salary, "createdAt", "updatedAt", email, "outletId") FROM stdin;
1	muizz	OWNER	81124f1214aaa83fac7eff7c6c81dfeef39d9a1b65a1938e0feb2d93831a32e01747099d7f502d72c8bf23daabbab6a06624decbd680a4c6db397bed5e5791e6	0	2026-05-30 16:39:36.747	2026-06-08 12:22:23.725	\N	\N
\.


--
-- Data for Name: Finance; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."Finance" (id, type, amount, description, date, status, "customerId", "createdAt", "updatedAt") FROM stdin;
\.


--
-- Data for Name: GoogleUser; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."GoogleUser" (id, email, "registeredAt", "updatedAt", "businessMode", "confirmToken", "confirmedAt", "demoExpiresAt", "isConfirmed", "userName", "passwordHash", whatsapp) FROM stdin;
hanafiariful@gmail.com	hanafiariful@gmail.com	2026-06-01 18:03:32.112	2026-06-01 18:03:55.578	FNB	\N	2026-06-01 18:03:55.577	2026-06-03 18:03:32.11	t	PISANG KEJU RAMAYANA	20710a82f8d6b458af10d49fbb1f985ac8aaf696e6b32e776d4f4ebbc30d08565e2bb5e1902ace18297d8db47ad35e49c086669125b1d6ac867c0d2d7e265e50	\N
bahteramulyap@gmail.com	bahteramulyap@gmail.com	2026-06-08 03:27:47.487	2026-06-08 03:27:47.615	BMP	\N	\N	\N	t	CV Bahtera Mulya Plastik	\N	\N
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
\.


--
-- Data for Name: Outlet; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."Outlet" (id, name, address, phone, "createdAt", "updatedAt") FROM stdin;
\.


--
-- Data for Name: PremiumUser; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."PremiumUser" (id, email, "passwordHash", name, role, "registeredAt", "updatedAt", "tenantId", whatsapp, "deletionScheduledAt", "lastPaymentConfirmedAt") FROM stdin;
hanafiariful@gmail.com	hanafiariful@gmail.com	20710a82f8d6b458af10d49fbb1f985ac8aaf696e6b32e776d4f4ebbc30d08565e2bb5e1902ace18297d8db47ad35e49c086669125b1d6ac867c0d2d7e265e50	PISANG KEJU RAMAYANA	OWNER	2026-06-01 18:03:55.583	2026-06-06 17:44:04.494	hanafiariful@gmail.com	\N	\N	2026-06-06 17:44:04.493
bahteramulyap@gmail.com	bahteramulyap@gmail.com	8a0ff1f8926195dfde55af7e68c028591602dacc30dc3c7caef27a949ca45142b25514004cf4540c46eca830100d06517c6facc0faf77fc57140e9df5fe5ffc7	Nama Customer	OWNER	2026-05-31 05:31:54.694	2026-06-06 17:44:33.45	\N	\N	\N	2026-06-06 17:44:33.449
fahrup22@gmail.com	fahrup22@gmail.com	63e71711d1481b6da8b756e114aa2ac71a704929c0accf46f419706a5c1416ae1a312899ae84d3d8e33d255811e98fd4d17e59371a08e2f9c21c01d1b1c13a8d	FahriP	ADMIN	2026-06-05 14:39:41.548	2026-06-06 17:44:43.331	hanafiariful@gmail.com	\N	\N	2026-06-06 17:44:43.329
alfarisirosi40@gmail.com	alfarisirosi40@gmail.com	a10301e4a133374bddc5f4f246aead30ba95b4f60c65df80418df2c6338141c9606262b07348fb0ee75964d460de3a459377217afa4b85b7bde3f8572d3b791c	Mamet PKR	KASIR	2026-06-06 16:58:52.003	2026-06-06 17:44:50.1	hanafiariful@gmail.com	\N	\N	2026-06-06 17:44:50.099
syerlirahma7@gmail.com	syerlirahma7@gmail.com	5819ef0d24208780b75c18009f0f69400eb933916f800ae980b778820cda595e3151de8600a0c325711f0e9641b5a72f393008868913578601ba0fa0d4c9ad93	syerli	ADMIN	2026-06-07 18:12:10.393	2026-06-07 18:18:36.07	bahteramulyap@gmail.com	\N	\N	\N
\.


--
-- Data for Name: Product; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."Product" (id, name, price, "costPrice", stock, unit, barcode, category, "wholesaleEnabled", "wholesalePrices", variants, image, "createdAt", "updatedAt", "outletId") FROM stdin;
\.


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

COPY public."Transaction" (id, "receiptNumber", date, subtotal, "discountType", "discountInput", "discountAmt", total, discount, "paymentMethod", "amountPaid", change, status, type, "orderStatus", "dpAmount", "deliveryDate", "employeeId", "customerId", "customerName", "queueNumber", notes, "createdAt", "updatedAt", "outletId") FROM stdin;
\.


--
-- Data for Name: TransactionItem; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public."TransactionItem" (id, "transactionId", "productId", "variantId", "variantName", quantity, price, "costPrice", discount, note) FROM stdin;
\.


--
-- Name: ActivityLog_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."ActivityLog_id_seq"', 1, false);


--
-- Name: BmpAdmsDevice_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."BmpAdmsDevice_id_seq"', 1, false);


--
-- Name: BmpAttendanceLog_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."BmpAttendanceLog_id_seq"', 1, false);


--
-- Name: BmpBahanNonoItem_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."BmpBahanNonoItem_id_seq"', 1, false);


--
-- Name: BmpBahanNono_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."BmpBahanNono_id_seq"', 1, false);


--
-- Name: BmpCashFlow_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."BmpCashFlow_id_seq"', 1, false);


--
-- Name: BmpClient_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."BmpClient_id_seq"', 1, false);


--
-- Name: BmpDeviceTenant_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."BmpDeviceTenant_id_seq"', 2, true);


--
-- Name: BmpEmployee_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."BmpEmployee_id_seq"', 1, false);


--
-- Name: BmpInvoicePayment_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."BmpInvoicePayment_id_seq"', 1, false);


--
-- Name: BmpInvoice_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."BmpInvoice_id_seq"', 1, false);


--
-- Name: BmpMachineBonusLog_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."BmpMachineBonusLog_id_seq"', 1, false);


--
-- Name: BmpMasterProduct_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."BmpMasterProduct_id_seq"', 1, false);


--
-- Name: BmpPayroll_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."BmpPayroll_id_seq"', 1, false);


--
-- Name: BmpPembayaran_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."BmpPembayaran_id_seq"', 1, false);


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

SELECT pg_catalog.setval('public."BmpProduct_id_seq"', 1, false);


--
-- Name: BmpSettings_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."BmpSettings_id_seq"', 1, false);


--
-- Name: Car_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."Car_id_seq"', 1, false);


--
-- Name: Customer_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."Customer_id_seq"', 1, false);


--
-- Name: Employee_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."Employee_id_seq"', 1, true);


--
-- Name: Finance_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."Finance_id_seq"', 1, false);


--
-- Name: LaundryExpense_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."LaundryExpense_id_seq"', 1, false);


--
-- Name: LaundryOrder_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."LaundryOrder_id_seq"', 1, false);


--
-- Name: LaundryService_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."LaundryService_id_seq"', 1, false);


--
-- Name: Outlet_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."Outlet_id_seq"', 1, false);


--
-- Name: Product_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."Product_id_seq"', 1, false);


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

SELECT pg_catalog.setval('public."Rental_id_seq"', 1, false);


--
-- Name: Supplier_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."Supplier_id_seq"', 1, false);


--
-- Name: TransactionItem_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."TransactionItem_id_seq"', 1, false);


--
-- Name: Transaction_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public."Transaction_id_seq"', 1, false);


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

\unrestrict NRJY6Ic8e98dQM8iMybeIj1nvNlVsFVQ4f3xU5ab50JeVQ5OgtuBOETfVICO67A

