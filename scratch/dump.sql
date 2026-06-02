--
-- PostgreSQL database dump
--

\restrict W634R46Xl8oqxGBYnfYKN3p6d6bRkNGyNFKl8U9i5R0Rr98HKSMtZCW97A2uFhN

-- Dumped from database version 18.3 (Debian 18.3-1.pgdg13+1)
-- Dumped by pg_dump version 18.4

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

ALTER TABLE IF EXISTS ONLY public.products DROP CONSTRAINT IF EXISTS fk_products_master_item;
ALTER TABLE IF EXISTS ONLY public.products DROP CONSTRAINT IF EXISTS fk_products_invoice;
ALTER TABLE IF EXISTS ONLY public.pembelian_items DROP CONSTRAINT IF EXISTS fk_pembelian_items_pembelian;
ALTER TABLE IF EXISTS ONLY public.pembelian_items DROP CONSTRAINT IF EXISTS fk_pembelian_barangs_items;
ALTER TABLE IF EXISTS ONLY public.pembayarans DROP CONSTRAINT IF EXISTS fk_pembayarans_invoice;
ALTER TABLE IF EXISTS ONLY public.payrolls DROP CONSTRAINT IF EXISTS fk_payrolls_employee;
ALTER TABLE IF EXISTS ONLY public.machine_bonus_logs DROP CONSTRAINT IF EXISTS fk_machine_bonus_logs_employee;
ALTER TABLE IF EXISTS ONLY public.invoice_payments DROP CONSTRAINT IF EXISTS fk_invoices_payments;
ALTER TABLE IF EXISTS ONLY public.invoices DROP CONSTRAINT IF EXISTS fk_invoices_client;
ALTER TABLE IF EXISTS ONLY public.cash_flows DROP CONSTRAINT IF EXISTS fk_cash_flows_payment_ref;
ALTER TABLE IF EXISTS ONLY public.bahan_nono_items DROP CONSTRAINT IF EXISTS fk_bahan_nonos_items;
ALTER TABLE IF EXISTS ONLY public.bahan_nono_items DROP CONSTRAINT IF EXISTS fk_bahan_nono_items_bahan_nono;
DROP INDEX IF EXISTS public.idx_users_username;
DROP INDEX IF EXISTS public.idx_users_deleted_at;
DROP INDEX IF EXISTS public.idx_settings_slug;
DROP INDEX IF EXISTS public.idx_settings_is_demo;
DROP INDEX IF EXISTS public.idx_settings_deleted_at;
DROP INDEX IF EXISTS public.idx_products_slug;
DROP INDEX IF EXISTS public.idx_products_deleted_at;
DROP INDEX IF EXISTS public.idx_pembelian_items_deleted_at;
DROP INDEX IF EXISTS public.idx_pembelian_barangs_is_demo;
DROP INDEX IF EXISTS public.idx_pembelian_barangs_deleted_at;
DROP INDEX IF EXISTS public.idx_pembayarans_deleted_at;
DROP INDEX IF EXISTS public.idx_payrolls_is_demo;
DROP INDEX IF EXISTS public.idx_payrolls_deleted_at;
DROP INDEX IF EXISTS public.idx_master_products_slug;
DROP INDEX IF EXISTS public.idx_master_products_is_demo;
DROP INDEX IF EXISTS public.idx_master_products_deleted_at;
DROP INDEX IF EXISTS public.idx_machine_bonus_logs_is_demo;
DROP INDEX IF EXISTS public.idx_machine_bonus_logs_deleted_at;
DROP INDEX IF EXISTS public.idx_invoices_slug;
DROP INDEX IF EXISTS public.idx_invoices_is_demo;
DROP INDEX IF EXISTS public.idx_invoices_deleted_at;
DROP INDEX IF EXISTS public.idx_invoice_payments_deleted_at;
DROP INDEX IF EXISTS public.idx_employees_is_demo;
DROP INDEX IF EXISTS public.idx_employees_deleted_at;
DROP INDEX IF EXISTS public.idx_clients_slug;
DROP INDEX IF EXISTS public.idx_clients_is_demo;
DROP INDEX IF EXISTS public.idx_clients_deleted_at;
DROP INDEX IF EXISTS public.idx_cash_flows_is_demo;
DROP INDEX IF EXISTS public.idx_cash_flows_deleted_at;
DROP INDEX IF EXISTS public.idx_bahan_nonos_is_demo;
DROP INDEX IF EXISTS public.idx_bahan_nonos_deleted_at;
DROP INDEX IF EXISTS public.idx_bahan_nono_items_deleted_at;
DROP INDEX IF EXISTS public.idx_attendance_logs_work_date;
DROP INDEX IF EXISTS public.idx_attendance_logs_log_time;
DROP INDEX IF EXISTS public.idx_attendance_logs_is_demo;
DROP INDEX IF EXISTS public.idx_attendance_logs_deleted_at;
DROP INDEX IF EXISTS public.idx_attendance_logs_check_out_time;
DROP INDEX IF EXISTS public.idx_adms_devices_serial_number;
DROP INDEX IF EXISTS public.idx_adms_devices_is_demo;
DROP INDEX IF EXISTS public.idx_adms_devices_deleted_at;
ALTER TABLE IF EXISTS ONLY public.users DROP CONSTRAINT IF EXISTS users_pkey;
ALTER TABLE IF EXISTS ONLY public.settings DROP CONSTRAINT IF EXISTS settings_pkey;
ALTER TABLE IF EXISTS ONLY public.products DROP CONSTRAINT IF EXISTS products_pkey;
ALTER TABLE IF EXISTS ONLY public.pembelian_items DROP CONSTRAINT IF EXISTS pembelian_items_pkey;
ALTER TABLE IF EXISTS ONLY public.pembelian_barangs DROP CONSTRAINT IF EXISTS pembelian_barangs_pkey;
ALTER TABLE IF EXISTS ONLY public.pembayarans DROP CONSTRAINT IF EXISTS pembayarans_pkey;
ALTER TABLE IF EXISTS ONLY public.payrolls DROP CONSTRAINT IF EXISTS payrolls_pkey;
ALTER TABLE IF EXISTS ONLY public.master_products DROP CONSTRAINT IF EXISTS master_products_pkey;
ALTER TABLE IF EXISTS ONLY public.machine_bonus_logs DROP CONSTRAINT IF EXISTS machine_bonus_logs_pkey;
ALTER TABLE IF EXISTS ONLY public.invoices DROP CONSTRAINT IF EXISTS invoices_pkey;
ALTER TABLE IF EXISTS ONLY public.invoice_payments DROP CONSTRAINT IF EXISTS invoice_payments_pkey;
ALTER TABLE IF EXISTS ONLY public.employees DROP CONSTRAINT IF EXISTS employees_pkey;
ALTER TABLE IF EXISTS ONLY public.clients DROP CONSTRAINT IF EXISTS clients_pkey;
ALTER TABLE IF EXISTS ONLY public.cash_flows DROP CONSTRAINT IF EXISTS cash_flows_pkey;
ALTER TABLE IF EXISTS ONLY public.bahan_nonos DROP CONSTRAINT IF EXISTS bahan_nonos_pkey;
ALTER TABLE IF EXISTS ONLY public.bahan_nono_items DROP CONSTRAINT IF EXISTS bahan_nono_items_pkey;
ALTER TABLE IF EXISTS ONLY public.attendance_logs DROP CONSTRAINT IF EXISTS attendance_logs_pkey;
ALTER TABLE IF EXISTS ONLY public.adms_devices DROP CONSTRAINT IF EXISTS adms_devices_pkey;
ALTER TABLE IF EXISTS public.users ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.settings ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.products ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.pembelian_items ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.pembelian_barangs ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.pembayarans ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.payrolls ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.master_products ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.machine_bonus_logs ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.invoices ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.invoice_payments ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.employees ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.clients ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.cash_flows ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.bahan_nonos ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.bahan_nono_items ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.attendance_logs ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.adms_devices ALTER COLUMN id DROP DEFAULT;
DROP SEQUENCE IF EXISTS public.users_id_seq;
DROP TABLE IF EXISTS public.users;
DROP SEQUENCE IF EXISTS public.settings_id_seq;
DROP TABLE IF EXISTS public.settings;
DROP SEQUENCE IF EXISTS public.products_id_seq;
DROP TABLE IF EXISTS public.products;
DROP SEQUENCE IF EXISTS public.pembelian_items_id_seq;
DROP TABLE IF EXISTS public.pembelian_items;
DROP SEQUENCE IF EXISTS public.pembelian_barangs_id_seq;
DROP TABLE IF EXISTS public.pembelian_barangs;
DROP SEQUENCE IF EXISTS public.pembayarans_id_seq;
DROP TABLE IF EXISTS public.pembayarans;
DROP SEQUENCE IF EXISTS public.payrolls_id_seq;
DROP TABLE IF EXISTS public.payrolls;
DROP SEQUENCE IF EXISTS public.master_products_id_seq;
DROP TABLE IF EXISTS public.master_products;
DROP SEQUENCE IF EXISTS public.machine_bonus_logs_id_seq;
DROP TABLE IF EXISTS public.machine_bonus_logs;
DROP SEQUENCE IF EXISTS public.invoices_id_seq;
DROP TABLE IF EXISTS public.invoices;
DROP SEQUENCE IF EXISTS public.invoice_payments_id_seq;
DROP TABLE IF EXISTS public.invoice_payments;
DROP SEQUENCE IF EXISTS public.employees_id_seq;
DROP TABLE IF EXISTS public.employees;
DROP SEQUENCE IF EXISTS public.clients_id_seq;
DROP TABLE IF EXISTS public.clients;
DROP SEQUENCE IF EXISTS public.cash_flows_id_seq;
DROP TABLE IF EXISTS public.cash_flows;
DROP SEQUENCE IF EXISTS public.bahan_nonos_id_seq;
DROP TABLE IF EXISTS public.bahan_nonos;
DROP SEQUENCE IF EXISTS public.bahan_nono_items_id_seq;
DROP TABLE IF EXISTS public.bahan_nono_items;
DROP SEQUENCE IF EXISTS public.attendance_logs_id_seq;
DROP TABLE IF EXISTS public.attendance_logs;
DROP SEQUENCE IF EXISTS public.adms_devices_id_seq;
DROP TABLE IF EXISTS public.adms_devices;
SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: adms_devices; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.adms_devices (
    id bigint NOT NULL,
    created_at timestamp with time zone,
    updated_at timestamp with time zone,
    deleted_at timestamp with time zone,
    serial_number character varying(100) NOT NULL,
    alias character varying(100),
    last_activity timestamp with time zone,
    is_demo boolean DEFAULT false
);


ALTER TABLE public.adms_devices OWNER TO postgres;

--
-- Name: adms_devices_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.adms_devices_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.adms_devices_id_seq OWNER TO postgres;

--
-- Name: adms_devices_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.adms_devices_id_seq OWNED BY public.adms_devices.id;


--
-- Name: attendance_logs; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.attendance_logs (
    id bigint NOT NULL,
    created_at timestamp with time zone,
    updated_at timestamp with time zone,
    deleted_at timestamp with time zone,
    device_sn character varying(100),
    employee_pin character varying(100),
    verify_type bigint,
    verify_state bigint,
    log_time timestamp with time zone,
    alasan character varying(255),
    is_demo boolean DEFAULT false,
    work_date date,
    late_minutes integer DEFAULT 0 NOT NULL,
    check_out_time timestamp with time zone
);


ALTER TABLE public.attendance_logs OWNER TO postgres;

--
-- Name: attendance_logs_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.attendance_logs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.attendance_logs_id_seq OWNER TO postgres;

--
-- Name: attendance_logs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.attendance_logs_id_seq OWNED BY public.attendance_logs.id;


--
-- Name: bahan_nono_items; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.bahan_nono_items (
    id bigint NOT NULL,
    created_at timestamp with time zone,
    updated_at timestamp with time zone,
    deleted_at timestamp with time zone,
    bahan_nono_id bigint,
    jenis_bahan character varying(50),
    kuantitas numeric DEFAULT 0,
    unit character varying(20) DEFAULT 'Kg'::character varying,
    rate numeric DEFAULT 0
);


ALTER TABLE public.bahan_nono_items OWNER TO postgres;

--
-- Name: bahan_nono_items_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.bahan_nono_items_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.bahan_nono_items_id_seq OWNER TO postgres;

--
-- Name: bahan_nono_items_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.bahan_nono_items_id_seq OWNED BY public.bahan_nono_items.id;


--
-- Name: bahan_nonos; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.bahan_nonos (
    id bigint NOT NULL,
    created_at timestamp with time zone,
    updated_at timestamp with time zone,
    deleted_at timestamp with time zone,
    tanggal date,
    nominal numeric DEFAULT 0,
    notes text,
    tagihan character varying(255),
    total_harga numeric DEFAULT 0,
    date_created timestamp with time zone,
    is_demo boolean DEFAULT false
);


ALTER TABLE public.bahan_nonos OWNER TO postgres;

--
-- Name: bahan_nonos_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.bahan_nonos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.bahan_nonos_id_seq OWNER TO postgres;

--
-- Name: bahan_nonos_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.bahan_nonos_id_seq OWNED BY public.bahan_nonos.id;


--
-- Name: cash_flows; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.cash_flows (
    id bigint NOT NULL,
    created_at timestamp with time zone,
    updated_at timestamp with time zone,
    deleted_at timestamp with time zone,
    transaction_date date,
    transaction_type character varying(10),
    description character varying(255),
    amount numeric DEFAULT 0,
    payment_ref_id bigint,
    date_created timestamp with time zone,
    tanggal text,
    jenis_transaksi text,
    keterangan text,
    nominal numeric,
    is_demo boolean DEFAULT false
);


ALTER TABLE public.cash_flows OWNER TO postgres;

--
-- Name: cash_flows_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.cash_flows_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.cash_flows_id_seq OWNER TO postgres;

--
-- Name: cash_flows_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.cash_flows_id_seq OWNED BY public.cash_flows.id;


--
-- Name: clients; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.clients (
    id bigint NOT NULL,
    created_at timestamp with time zone,
    updated_at timestamp with time zone,
    deleted_at timestamp with time zone,
    saldo_titipan numeric(15,2) DEFAULT 0,
    client_name character varying(200),
    address_line1 character varying(200),
    client_logo character varying(255),
    province character varying(100),
    postal_code character varying(10),
    phone_number character varying(100),
    email_address character varying(100),
    tax_number character varying(100),
    unique_id character varying(100),
    slug character varying(500),
    date_created timestamp with time zone,
    last_updated timestamp with time zone,
    is_demo boolean DEFAULT false
);


ALTER TABLE public.clients OWNER TO postgres;

--
-- Name: clients_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.clients_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.clients_id_seq OWNER TO postgres;

--
-- Name: clients_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.clients_id_seq OWNED BY public.clients.id;


--
-- Name: employees; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.employees (
    id bigint NOT NULL,
    created_at timestamp with time zone,
    updated_at timestamp with time zone,
    deleted_at timestamp with time zone,
    name character varying(255) NOT NULL,
    "position" character varying(100),
    salary_amount numeric(15,2),
    is_active boolean DEFAULT true,
    fingerprint_pin character varying(100),
    is_demo boolean DEFAULT false
);


ALTER TABLE public.employees OWNER TO postgres;

--
-- Name: employees_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.employees_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.employees_id_seq OWNER TO postgres;

--
-- Name: employees_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.employees_id_seq OWNED BY public.employees.id;


--
-- Name: invoice_payments; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.invoice_payments (
    id bigint NOT NULL,
    created_at timestamp with time zone,
    updated_at timestamp with time zone,
    deleted_at timestamp with time zone,
    invoice_id bigint,
    payment_date date,
    payment_amount bigint,
    payment_method character varying(50),
    date_created timestamp with time zone
);


ALTER TABLE public.invoice_payments OWNER TO postgres;

--
-- Name: invoice_payments_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.invoice_payments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.invoice_payments_id_seq OWNER TO postgres;

--
-- Name: invoice_payments_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.invoice_payments_id_seq OWNED BY public.invoice_payments.id;


--
-- Name: invoices; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.invoices (
    id bigint NOT NULL,
    created_at timestamp with time zone,
    updated_at timestamp with time zone,
    deleted_at timestamp with time zone,
    title character varying(100),
    number character varying(100),
    due_date timestamp with time zone,
    payment_terms character varying(100) DEFAULT '14 days'::character varying,
    status character varying(100) DEFAULT 'DRAFT'::character varying,
    notes text,
    client_id bigint,
    unique_id character varying(100),
    slug character varying(500),
    date_created timestamp with time zone,
    last_updated timestamp with time zone,
    is_demo boolean DEFAULT false
);


ALTER TABLE public.invoices OWNER TO postgres;

--
-- Name: invoices_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.invoices_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.invoices_id_seq OWNER TO postgres;

--
-- Name: invoices_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.invoices_id_seq OWNED BY public.invoices.id;


--
-- Name: machine_bonus_logs; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.machine_bonus_logs (
    id bigint NOT NULL,
    created_at timestamp with time zone,
    updated_at timestamp with time zone,
    deleted_at timestamp with time zone,
    employee_id bigint,
    machine_name character varying(100),
    shift_type character varying(50),
    bonus_amount numeric(15,2),
    date date,
    jumlah_perolehan bigint DEFAULT 0,
    is_demo boolean DEFAULT false
);


ALTER TABLE public.machine_bonus_logs OWNER TO postgres;

--
-- Name: machine_bonus_logs_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.machine_bonus_logs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.machine_bonus_logs_id_seq OWNER TO postgres;

--
-- Name: machine_bonus_logs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.machine_bonus_logs_id_seq OWNED BY public.machine_bonus_logs.id;


--
-- Name: master_products; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.master_products (
    id bigint NOT NULL,
    created_at timestamp with time zone,
    updated_at timestamp with time zone,
    deleted_at timestamp with time zone,
    title character varying(100),
    description text,
    unit character varying(50),
    price numeric DEFAULT 0,
    berat_gram numeric DEFAULT 0,
    cycle_time numeric DEFAULT 0,
    unique_id character varying(100),
    slug character varying(500),
    date_created timestamp with time zone,
    last_updated timestamp with time zone,
    is_demo boolean DEFAULT false,
    cavity bigint DEFAULT 1,
    reject_rate numeric DEFAULT 0
);


ALTER TABLE public.master_products OWNER TO postgres;

--
-- Name: master_products_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.master_products_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.master_products_id_seq OWNER TO postgres;

--
-- Name: master_products_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.master_products_id_seq OWNED BY public.master_products.id;


--
-- Name: payrolls; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.payrolls (
    id bigint NOT NULL,
    created_at timestamp with time zone,
    updated_at timestamp with time zone,
    deleted_at timestamp with time zone,
    employee_id bigint,
    payment_date date,
    amount numeric(15,2),
    description character varying(255),
    attendance_count integer DEFAULT 0,
    daily_rate numeric(15,2),
    is_demo boolean DEFAULT false
);


ALTER TABLE public.payrolls OWNER TO postgres;

--
-- Name: payrolls_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.payrolls_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.payrolls_id_seq OWNER TO postgres;

--
-- Name: payrolls_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.payrolls_id_seq OWNED BY public.payrolls.id;


--
-- Name: pembayarans; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.pembayarans (
    id bigint NOT NULL,
    created_at timestamp with time zone,
    updated_at timestamp with time zone,
    deleted_at timestamp with time zone,
    invoice_id bigint,
    tanggal_bayar timestamp with time zone,
    jumlah_bayar numeric(15,2),
    keterangan character varying(255)
);


ALTER TABLE public.pembayarans OWNER TO postgres;

--
-- Name: pembayarans_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.pembayarans_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.pembayarans_id_seq OWNER TO postgres;

--
-- Name: pembayarans_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.pembayarans_id_seq OWNED BY public.pembayarans.id;


--
-- Name: pembelian_barangs; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.pembelian_barangs (
    id bigint NOT NULL,
    created_at timestamp with time zone,
    updated_at timestamp with time zone,
    deleted_at timestamp with time zone,
    supplier character varying(255),
    tanggal date,
    keterangan text,
    total_harga numeric DEFAULT 0,
    cara_bayar character varying(20) DEFAULT 'HUTANG'::character varying,
    is_demo boolean DEFAULT false
);


ALTER TABLE public.pembelian_barangs OWNER TO postgres;

--
-- Name: pembelian_barangs_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.pembelian_barangs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.pembelian_barangs_id_seq OWNER TO postgres;

--
-- Name: pembelian_barangs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.pembelian_barangs_id_seq OWNED BY public.pembelian_barangs.id;


--
-- Name: pembelian_items; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.pembelian_items (
    id bigint NOT NULL,
    created_at timestamp with time zone,
    updated_at timestamp with time zone,
    deleted_at timestamp with time zone,
    pembelian_id bigint,
    nama_barang character varying(255),
    jumlah_lusin numeric DEFAULT 1,
    kuantitas numeric DEFAULT 0,
    unit character varying(20) DEFAULT 'Pcs'::character varying,
    harga_satuan numeric DEFAULT 0
);


ALTER TABLE public.pembelian_items OWNER TO postgres;

--
-- Name: pembelian_items_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.pembelian_items_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.pembelian_items_id_seq OWNER TO postgres;

--
-- Name: pembelian_items_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.pembelian_items_id_seq OWNED BY public.pembelian_items.id;


--
-- Name: products; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.products (
    id bigint NOT NULL,
    created_at timestamp with time zone,
    updated_at timestamp with time zone,
    deleted_at timestamp with time zone,
    master_item_id bigint,
    title character varying(100),
    unit character varying(50),
    price numeric DEFAULT 0,
    jumlah_lusin numeric DEFAULT 1,
    quantity numeric DEFAULT 0,
    currency character varying(100) DEFAULT 'Rp'::character varying,
    invoice_id bigint,
    unique_id character varying(100),
    slug character varying(500),
    date_created timestamp with time zone,
    last_updated timestamp with time zone,
    is_khusus boolean DEFAULT false,
    harga_beli numeric DEFAULT 0
);


ALTER TABLE public.products OWNER TO postgres;

--
-- Name: products_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.products_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.products_id_seq OWNER TO postgres;

--
-- Name: products_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.products_id_seq OWNED BY public.products.id;


--
-- Name: settings; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.settings (
    id bigint NOT NULL,
    created_at timestamp with time zone,
    updated_at timestamp with time zone,
    deleted_at timestamp with time zone,
    client_name character varying(200),
    client_logo character varying(255),
    address_line1 character varying(200),
    province character varying(100),
    postal_code character varying(10),
    phone_number character varying(100),
    email_address character varying(100),
    tax_number character varying(100),
    listrik_bulanan numeric DEFAULT 30000000,
    jumlah_mesin bigint DEFAULT 5,
    jumlah_karyawan bigint DEFAULT 19,
    gaji_harian numeric DEFAULT 80000,
    hari_kerja_sebulan bigint DEFAULT 26,
    biaya_karung_per1000 numeric DEFAULT 2100000,
    unique_id character varying(100),
    slug character varying(500),
    date_created timestamp with time zone,
    last_updated timestamp with time zone,
    is_demo boolean DEFAULT false,
    hours_per_day bigint DEFAULT 24
);


ALTER TABLE public.settings OWNER TO postgres;

--
-- Name: settings_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.settings_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.settings_id_seq OWNER TO postgres;

--
-- Name: settings_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.settings_id_seq OWNED BY public.settings.id;


--
-- Name: users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.users (
    id bigint NOT NULL,
    created_at timestamp with time zone,
    updated_at timestamp with time zone,
    deleted_at timestamp with time zone,
    username text NOT NULL,
    password text NOT NULL
);


ALTER TABLE public.users OWNER TO postgres;

--
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.users_id_seq OWNER TO postgres;

--
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.users_id_seq OWNED BY public.users.id;


--
-- Name: adms_devices id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.adms_devices ALTER COLUMN id SET DEFAULT nextval('public.adms_devices_id_seq'::regclass);


--
-- Name: attendance_logs id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.attendance_logs ALTER COLUMN id SET DEFAULT nextval('public.attendance_logs_id_seq'::regclass);


--
-- Name: bahan_nono_items id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bahan_nono_items ALTER COLUMN id SET DEFAULT nextval('public.bahan_nono_items_id_seq'::regclass);


--
-- Name: bahan_nonos id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bahan_nonos ALTER COLUMN id SET DEFAULT nextval('public.bahan_nonos_id_seq'::regclass);


--
-- Name: cash_flows id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.cash_flows ALTER COLUMN id SET DEFAULT nextval('public.cash_flows_id_seq'::regclass);


--
-- Name: clients id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.clients ALTER COLUMN id SET DEFAULT nextval('public.clients_id_seq'::regclass);


--
-- Name: employees id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.employees ALTER COLUMN id SET DEFAULT nextval('public.employees_id_seq'::regclass);


--
-- Name: invoice_payments id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.invoice_payments ALTER COLUMN id SET DEFAULT nextval('public.invoice_payments_id_seq'::regclass);


--
-- Name: invoices id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.invoices ALTER COLUMN id SET DEFAULT nextval('public.invoices_id_seq'::regclass);


--
-- Name: machine_bonus_logs id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.machine_bonus_logs ALTER COLUMN id SET DEFAULT nextval('public.machine_bonus_logs_id_seq'::regclass);


--
-- Name: master_products id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.master_products ALTER COLUMN id SET DEFAULT nextval('public.master_products_id_seq'::regclass);


--
-- Name: payrolls id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.payrolls ALTER COLUMN id SET DEFAULT nextval('public.payrolls_id_seq'::regclass);


--
-- Name: pembayarans id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.pembayarans ALTER COLUMN id SET DEFAULT nextval('public.pembayarans_id_seq'::regclass);


--
-- Name: pembelian_barangs id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.pembelian_barangs ALTER COLUMN id SET DEFAULT nextval('public.pembelian_barangs_id_seq'::regclass);


--
-- Name: pembelian_items id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.pembelian_items ALTER COLUMN id SET DEFAULT nextval('public.pembelian_items_id_seq'::regclass);


--
-- Name: products id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.products ALTER COLUMN id SET DEFAULT nextval('public.products_id_seq'::regclass);


--
-- Name: settings id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.settings ALTER COLUMN id SET DEFAULT nextval('public.settings_id_seq'::regclass);


--
-- Name: users id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users ALTER COLUMN id SET DEFAULT nextval('public.users_id_seq'::regclass);


--
-- Data for Name: adms_devices; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.adms_devices (id, created_at, updated_at, deleted_at, serial_number, alias, last_activity, is_demo) VALUES (1, '2026-05-12 07:41:04.402433+00', '2026-05-29 04:54:05.742616+00', NULL, 'NHZ4254800403', '', '2026-05-29 04:54:05.738753+00', false);


--
-- Data for Name: attendance_logs; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (76, '2026-05-17 23:38:46.225038+00', '2026-05-17 23:38:46.225038+00', NULL, 'NHZ4254800403', '10', 1, 1, '2026-05-17 23:38:46.212733+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (77, '2026-05-17 23:45:33.595353+00', '2026-05-17 23:45:33.595353+00', NULL, 'NHZ4254800403', '11', 1, 1, '2026-05-17 23:45:33.587147+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (78, '2026-05-17 23:50:54.9851+00', '2026-05-17 23:50:54.9851+00', NULL, 'NHZ4254800403', '20', 1, 1, '2026-05-17 23:50:54.977324+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (79, '2026-05-17 23:52:13.219284+00', '2026-05-17 23:52:13.219284+00', NULL, 'NHZ4254800403', '6', 1, 1, '2026-05-17 23:52:13.211374+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (80, '2026-05-17 23:52:50.282966+00', '2026-05-17 23:52:50.282966+00', NULL, 'NHZ4254800403', '13', 1, 1, '2026-05-17 23:52:50.274703+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (81, '2026-05-17 23:58:18.769586+00', '2026-05-17 23:58:18.769586+00', NULL, 'NHZ4254800403', '12', 1, 1, '2026-05-17 23:58:18.757404+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (82, '2026-05-18 07:52:15.390303+00', '2026-05-18 07:52:15.390303+00', NULL, 'NHZ4254800403', '14', 1, 1, '2026-05-18 07:52:15.377583+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (83, '2026-05-18 07:52:31.664865+00', '2026-05-18 07:52:31.664865+00', NULL, 'NHZ4254800403', '15', 1, 1, '2026-05-18 07:52:31.657179+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (84, '2026-05-18 07:52:33.707747+00', '2026-05-18 07:52:33.707747+00', NULL, 'NHZ4254800403', '15', 1, 1, '2026-05-18 07:52:33.697656+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (269, '2026-05-28 23:57:47.741552+00', '2026-05-28 23:57:47.741552+00', NULL, 'NHZ4254800403', '18', 1, 0, '2026-05-28 23:57:47.732435+00', '', false, '2026-05-28', 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (86, '2026-05-18 07:59:06.2787+00', '2026-05-18 07:59:06.2787+00', NULL, 'NHZ4254800403', '19', 1, 1, '2026-05-18 07:59:06.271287+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (87, '2026-05-18 07:59:55.136143+00', '2026-05-18 07:59:55.136143+00', NULL, 'NHZ4254800403', '18', 1, 1, '2026-05-18 07:59:55.127337+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (88, '2026-05-18 08:00:03.012322+00', '2026-05-18 08:00:03.012322+00', NULL, 'NHZ4254800403', '16', 1, 1, '2026-05-18 08:00:03.003542+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (89, '2026-05-18 08:00:27.07722+00', '2026-05-18 08:00:27.07722+00', NULL, 'NHZ4254800403', '10', 1, 1, '2026-05-18 08:00:27.069351+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (90, '2026-05-18 08:00:35.240268+00', '2026-05-18 08:00:35.240268+00', NULL, 'NHZ4254800403', '6', 1, 1, '2026-05-18 08:00:35.233733+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (91, '2026-05-18 08:00:43.311869+00', '2026-05-18 08:00:43.311869+00', NULL, 'NHZ4254800403', '20', 1, 1, '2026-05-18 08:00:43.303723+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (92, '2026-05-18 08:00:56.293658+00', '2026-05-18 08:00:56.293658+00', NULL, 'NHZ4254800403', '13', 1, 1, '2026-05-18 08:00:56.285745+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (93, '2026-05-18 08:01:32.445189+00', '2026-05-18 08:01:32.445189+00', NULL, 'NHZ4254800403', '11', 1, 1, '2026-05-18 08:01:32.437753+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (94, '2026-05-18 08:02:46.643159+00', '2026-05-18 08:02:46.643159+00', NULL, 'NHZ4254800403', '12', 1, 1, '2026-05-18 08:02:46.635575+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (95, '2026-05-18 15:52:45.110886+00', '2026-05-18 15:52:45.110886+00', NULL, 'NHZ4254800403', '3', 1, 1, '2026-05-18 15:52:45.102594+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (96, '2026-05-18 15:52:58.189043+00', '2026-05-18 15:52:58.189043+00', NULL, 'NHZ4254800403', '8', 1, 1, '2026-05-18 15:52:58.181115+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (97, '2026-05-18 15:58:29.630117+00', '2026-05-18 15:58:29.630117+00', NULL, 'NHZ4254800403', '4', 1, 1, '2026-05-18 15:58:29.622338+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (98, '2026-05-18 16:00:21.934943+00', '2026-05-18 16:00:21.934943+00', NULL, 'NHZ4254800403', '5', 1, 1, '2026-05-18 16:00:21.927037+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (99, '2026-05-18 16:00:38.011441+00', '2026-05-18 16:00:38.011441+00', NULL, 'NHZ4254800403', '15', 1, 1, '2026-05-18 16:00:38.003123+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (102, '2026-05-18 16:01:16.671887+00', '2026-05-18 16:01:16.671887+00', NULL, 'NHZ4254800403', '18', 1, 1, '2026-05-18 16:01:16.663933+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (103, '2026-05-18 16:01:33.025887+00', '2026-05-18 16:01:33.025887+00', NULL, 'NHZ4254800403', '19', 1, 1, '2026-05-18 16:01:33.017846+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (104, '2026-05-18 16:01:47.305642+00', '2026-05-18 16:01:47.305642+00', NULL, 'NHZ4254800403', '16', 1, 1, '2026-05-18 16:01:47.296664+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (105, '2026-05-18 16:02:01.603697+00', '2026-05-18 16:02:01.603697+00', NULL, 'NHZ4254800403', '14', 1, 1, '2026-05-18 16:02:01.595621+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (106, '2026-05-18 23:51:20.965216+00', '2026-05-18 23:51:20.965216+00', NULL, 'NHZ4254800403', '6', 1, 1, '2026-05-18 23:51:20.956203+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (107, '2026-05-18 23:53:38.747002+00', '2026-05-18 23:53:38.747002+00', NULL, 'NHZ4254800403', '20', 1, 1, '2026-05-18 23:53:38.736732+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (108, '2026-05-18 23:55:47.030289+00', '2026-05-18 23:55:47.030289+00', NULL, 'NHZ4254800403', '11', 1, 1, '2026-05-18 23:55:47.021024+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (109, '2026-05-18 23:56:45.477651+00', '2026-05-18 23:56:45.477651+00', NULL, 'NHZ4254800403', '12', 1, 1, '2026-05-18 23:56:45.469953+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (110, '2026-05-19 00:01:38.875585+00', '2026-05-19 00:01:38.875585+00', NULL, 'NHZ4254800403', '13', 1, 1, '2026-05-19 00:01:38.867895+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (112, '2026-05-19 00:24:13.504101+00', '2026-05-19 00:24:13.504101+00', NULL, 'NHZ4254800403', '3', 1, 1, '2026-05-19 00:24:13.496351+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (113, '2026-05-19 00:24:20.633553+00', '2026-05-19 00:24:20.633553+00', NULL, 'NHZ4254800403', '8', 1, 1, '2026-05-19 00:24:20.626129+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (114, '2026-05-19 00:25:06.596069+00', '2026-05-19 00:25:06.596069+00', NULL, 'NHZ4254800403', '5', 1, 1, '2026-05-19 00:25:06.588457+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (115, '2026-05-19 00:26:48.934708+00', '2026-05-19 00:26:48.934708+00', NULL, 'NHZ4254800403', '4', 1, 1, '2026-05-19 00:26:48.926915+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (116, '2026-05-19 07:52:09.128561+00', '2026-05-19 07:52:09.128561+00', NULL, 'NHZ4254800403', '14', 1, 1, '2026-05-19 07:52:09.120834+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (117, '2026-05-19 07:52:30.200109+00', '2026-05-19 07:52:30.200109+00', NULL, 'NHZ4254800403', '15', 1, 1, '2026-05-19 07:52:30.192183+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (118, '2026-05-19 07:58:26.708453+00', '2026-05-19 07:58:26.708453+00', NULL, 'NHZ4254800403', '16', 1, 1, '2026-05-19 07:58:26.700611+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (119, '2026-05-19 07:58:37.007257+00', '2026-05-19 07:58:37.007257+00', NULL, 'NHZ4254800403', '16', 1, 0, '2026-05-19 07:58:36.999572+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (120, '2026-05-19 07:58:59.085922+00', '2026-05-19 07:58:59.085922+00', NULL, 'NHZ4254800403', '18', 1, 0, '2026-05-19 07:58:59.077901+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (121, '2026-05-19 07:59:28.545775+00', '2026-05-19 07:59:28.545775+00', NULL, 'NHZ4254800403', '18', 1, 0, '2026-05-19 07:59:28.537764+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (122, '2026-05-19 07:59:42.352972+00', '2026-05-19 07:59:42.352972+00', NULL, 'NHZ4254800403', '16', 1, 0, '2026-05-19 07:59:42.344444+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (123, '2026-05-19 08:00:12.52928+00', '2026-05-19 08:00:12.52928+00', NULL, 'NHZ4254800403', '11', 1, 1, '2026-05-19 08:00:12.521468+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (124, '2026-05-19 08:00:20.580047+00', '2026-05-19 08:00:20.580047+00', NULL, 'NHZ4254800403', '20', 1, 1, '2026-05-19 08:00:20.571968+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (125, '2026-05-19 08:00:28.9045+00', '2026-05-19 08:00:28.9045+00', NULL, 'NHZ4254800403', '6', 1, 1, '2026-05-19 08:00:28.896667+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (126, '2026-05-19 08:00:40.283302+00', '2026-05-19 08:00:40.283302+00', NULL, 'NHZ4254800403', '12', 1, 1, '2026-05-19 08:00:40.273064+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (127, '2026-05-19 08:00:49.271174+00', '2026-05-19 08:00:49.271174+00', NULL, 'NHZ4254800403', '13', 1, 1, '2026-05-19 08:00:49.26339+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (128, '2026-05-19 15:52:53.020771+00', '2026-05-19 15:52:53.020771+00', NULL, 'NHZ4254800403', '19', 1, 0, '2026-05-19 15:52:53.012173+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (129, '2026-05-19 15:53:06.00843+00', '2026-05-19 15:53:06.00843+00', NULL, 'NHZ4254800403', '7', 1, 0, '2026-05-19 15:53:05.996922+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (130, '2026-05-19 15:54:01.378037+00', '2026-05-19 15:54:01.378037+00', NULL, 'NHZ4254800403', '8', 1, 0, '2026-05-19 15:54:01.370536+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (131, '2026-05-19 15:54:19.727918+00', '2026-05-19 15:54:19.727918+00', NULL, 'NHZ4254800403', '3', 1, 0, '2026-05-19 15:54:19.720334+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (132, '2026-05-19 15:57:44.064064+00', '2026-05-19 15:57:44.064064+00', NULL, 'NHZ4254800403', '5', 1, 0, '2026-05-19 15:57:44.054846+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (133, '2026-05-19 16:02:46.509062+00', '2026-05-19 16:02:46.509062+00', NULL, 'NHZ4254800403', '18', 1, 1, '2026-05-19 16:02:46.498938+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (134, '2026-05-19 16:03:12.459182+00', '2026-05-19 16:03:12.459182+00', NULL, 'NHZ4254800403', '16', 1, 1, '2026-05-19 16:03:12.451209+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (135, '2026-05-19 16:03:16.704809+00', '2026-05-19 16:03:16.704809+00', NULL, 'NHZ4254800403', '16', 1, 1, '2026-05-19 16:03:16.696984+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (136, '2026-05-19 16:03:39.750386+00', '2026-05-19 16:03:39.750386+00', NULL, 'NHZ4254800403', '14', 1, 1, '2026-05-19 16:03:39.742155+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (137, '2026-05-19 16:04:24.327975+00', '2026-05-19 16:04:24.327975+00', NULL, 'NHZ4254800403', '15', 1, 1, '2026-05-19 16:04:24.320234+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (138, '2026-05-19 16:07:13.802121+00', '2026-05-19 16:07:13.802121+00', NULL, 'NHZ4254800403', '9', 1, 1, '2026-05-19 16:07:13.794381+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (272, '2026-05-29 07:54:53.368995+00', '2026-05-29 16:25:06.643417+00', NULL, 'NHZ4254800403', '3', 1, 1, '2026-05-29 07:54:53.361316+00', '', false, '2026-05-29', 0, '2026-05-29 16:25:06.634704+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (270, '2026-05-29 07:51:57.991726+00', '2026-05-29 16:25:14.721639+00', NULL, 'NHZ4254800403', '8', 1, 1, '2026-05-29 07:51:57.984009+00', '', false, '2026-05-29', 0, '2026-05-29 16:25:14.715638+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (271, '2026-05-29 07:52:11.006637+00', '2026-05-29 16:25:23.459982+00', NULL, 'NHZ4254800403', '7', 1, 1, '2026-05-29 07:52:10.999005+00', '', false, '2026-05-29', 0, '2026-05-29 16:25:23.454892+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (273, '2026-05-29 07:55:00.441496+00', '2026-05-29 16:25:35.231622+00', NULL, 'NHZ4254800403', '5', 1, 1, '2026-05-29 07:55:00.432715+00', '', false, '2026-05-29', 0, '2026-05-29 16:25:35.226733+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (151, '2026-05-20 10:16:10.602859+00', '2026-05-20 10:16:10.602859+00', NULL, 'NHZ4254800403', '9', 1, 1, '2026-05-20 00:00:00+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (139, '2026-05-20 10:16:10.450396+00', '2026-05-20 10:16:10.450396+00', NULL, 'NHZ4254800403', '10', 1, 1, '2026-05-20 00:00:00+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (140, '2026-05-20 10:16:10.463728+00', '2026-05-20 10:16:10.463728+00', NULL, 'NHZ4254800403', '11', 1, 1, '2026-05-20 00:00:00+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (141, '2026-05-20 10:16:10.476077+00', '2026-05-20 10:16:10.476077+00', NULL, 'NHZ4254800403', '11', 1, 0, '2026-05-20 00:00:00+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (142, '2026-05-20 10:16:10.488602+00', '2026-05-20 10:16:10.488602+00', NULL, 'NHZ4254800403', '6', 1, 0, '2026-05-20 00:00:00+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (277, '2026-05-29 08:13:21.436569+00', '2026-05-30 05:44:42.399758+00', NULL, 'NHZ4254800403', '18', 1, 1, '2026-05-28 23:56:00+00', '', false, '2026-05-29', 0, '2026-05-29 08:10:00+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (278, '2026-05-29 08:16:48.520372+00', '2026-05-30 05:46:10.172721+00', NULL, 'NHZ4254800403', '17', 1, 1, '2026-05-28 23:52:00+00', '', false, '2026-05-29', 0, '2026-05-29 08:02:00+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (288, '2026-05-30 02:56:15.789358+00', '2026-05-30 23:55:05.004006+00', NULL, '', '14', 0, 1, '2026-05-29 23:56:00+00', '', false, '2026-05-30', 0, '2026-05-30 23:55:04.754251+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (287, '2026-05-30 02:55:38.368418+00', '2026-05-30 23:58:14.26477+00', NULL, '', '17', 0, 1, '2026-05-29 23:55:00+00', '', false, '2026-05-30', 0, '2026-05-30 23:58:14.014415+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (296, '2026-05-30 08:18:48.420326+00', '2026-05-30 23:56:08.18615+00', NULL, 'NHZ4254800403', '15', 1, 1, '2026-05-30 08:18:48.409205+00', '', false, '2026-05-30', 18, '2026-05-30 23:56:07.935697+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (289, '2026-05-30 02:56:56.650236+00', '2026-05-30 23:57:32.479728+00', NULL, '', '18', 0, 1, '2026-05-29 23:56:00+00', '', false, '2026-05-30', 0, '2026-05-30 23:57:32.229331+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (183, '2026-05-21 07:54:28.333583+00', '2026-05-21 07:54:28.333583+00', NULL, 'NHZ4254800403', '15', 1, 0, '2026-05-21 07:54:28.322003+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (184, '2026-05-21 07:54:40.456785+00', '2026-05-21 07:54:40.456785+00', NULL, 'NHZ4254800403', '14', 1, 0, '2026-05-21 07:54:40.447159+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (185, '2026-05-21 07:58:14.730786+00', '2026-05-21 07:58:14.730786+00', NULL, 'NHZ4254800403', '16', 1, 0, '2026-05-21 07:58:14.721092+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (186, '2026-05-21 07:58:41.686454+00', '2026-05-21 07:58:41.686454+00', NULL, 'NHZ4254800403', '18', 1, 0, '2026-05-21 07:58:41.676233+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (187, '2026-05-21 07:58:57.390613+00', '2026-05-21 07:58:57.390613+00', NULL, 'NHZ4254800403', '17', 1, 0, '2026-05-21 07:58:57.380395+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (188, '2026-05-21 08:00:14.67071+00', '2026-05-21 08:00:14.67071+00', NULL, 'NHZ4254800403', '13', 1, 1, '2026-05-21 08:00:14.660215+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (189, '2026-05-21 08:01:05.820702+00', '2026-05-21 08:01:05.820702+00', NULL, 'NHZ4254800403', '10', 1, 1, '2026-05-21 08:01:05.809735+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (190, '2026-05-21 08:01:23.126387+00', '2026-05-21 08:01:23.126387+00', NULL, 'NHZ4254800403', '11', 1, 1, '2026-05-21 08:01:23.116675+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (191, '2026-05-21 08:01:28.301559+00', '2026-05-21 08:01:28.301559+00', NULL, 'NHZ4254800403', '6', 1, 1, '2026-05-21 08:01:28.291295+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (192, '2026-05-21 08:02:43.373863+00', '2026-05-21 08:02:43.373863+00', NULL, 'NHZ4254800403', '20', 1, 1, '2026-05-21 08:02:43.364399+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (193, '2026-05-21 08:03:02.508459+00', '2026-05-21 08:03:02.508459+00', NULL, 'NHZ4254800403', '12', 1, 1, '2026-05-21 08:03:02.498678+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (194, '2026-05-21 15:52:49.256287+00', '2026-05-21 15:52:49.256287+00', NULL, 'NHZ4254800403', '8', 1, 0, '2026-05-21 15:52:49.24744+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (195, '2026-05-21 15:53:03.283912+00', '2026-05-21 15:53:03.283912+00', NULL, 'NHZ4254800403', '7', 1, 0, '2026-05-21 15:53:03.275564+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (196, '2026-05-21 15:55:29.649644+00', '2026-05-21 15:55:29.649644+00', NULL, 'NHZ4254800403', '3', 1, 0, '2026-05-21 15:55:29.641251+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (197, '2026-05-21 15:55:43.849788+00', '2026-05-21 15:55:43.849788+00', NULL, 'NHZ4254800403', '5', 1, 0, '2026-05-21 15:55:43.840896+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (198, '2026-05-21 15:56:06.499914+00', '2026-05-21 15:56:06.499914+00', NULL, 'NHZ4254800403', '19', 1, 0, '2026-05-21 15:56:06.490955+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (199, '2026-05-21 15:59:59.878492+00', '2026-05-21 15:59:59.878492+00', NULL, 'NHZ4254800403', '9', 1, 0, '2026-05-21 15:59:59.86991+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (200, '2026-05-21 16:02:32.207397+00', '2026-05-21 16:02:32.207397+00', NULL, 'NHZ4254800403', '17', 1, 1, '2026-05-21 16:02:32.199316+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (201, '2026-05-21 16:02:44.222023+00', '2026-05-21 16:02:44.222023+00', NULL, 'NHZ4254800403', '16', 1, 1, '2026-05-21 16:02:44.213501+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (202, '2026-05-21 16:07:51.911697+00', '2026-05-21 16:07:51.911697+00', NULL, 'NHZ4254800403', '15', 1, 1, '2026-05-21 16:07:51.902993+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (203, '2026-05-21 16:08:03.096671+00', '2026-05-21 16:08:03.096671+00', NULL, 'NHZ4254800403', '14', 1, 1, '2026-05-21 16:08:03.087494+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (204, '2026-05-21 16:10:40.475182+00', '2026-05-21 16:10:40.475182+00', NULL, 'NHZ4254800403', '18', 1, 1, '2026-05-21 16:10:40.467045+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (205, '2026-05-21 23:50:46.558762+00', '2026-05-21 23:50:46.558762+00', NULL, 'NHZ4254800403', '12', 1, 0, '2026-05-21 23:50:46.550669+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (206, '2026-05-21 23:53:29.78202+00', '2026-05-21 23:53:29.78202+00', NULL, 'NHZ4254800403', '11', 1, 0, '2026-05-21 23:53:29.773892+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (207, '2026-05-21 23:53:42.721993+00', '2026-05-21 23:53:42.721993+00', NULL, 'NHZ4254800403', '6', 1, 0, '2026-05-21 23:53:42.714024+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (208, '2026-05-21 23:53:53.938741+00', '2026-05-21 23:53:53.938741+00', NULL, 'NHZ4254800403', '10', 1, 0, '2026-05-21 23:53:53.930853+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (209, '2026-05-21 23:56:03.035159+00', '2026-05-21 23:56:03.035159+00', NULL, 'NHZ4254800403', '13', 1, 0, '2026-05-21 23:56:03.025892+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (210, '2026-05-21 23:56:33.293362+00', '2026-05-21 23:56:33.293362+00', NULL, 'NHZ4254800403', '20', 1, 0, '2026-05-21 23:56:33.285515+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (211, '2026-05-22 00:04:53.765945+00', '2026-05-22 00:04:53.765945+00', NULL, 'NHZ4254800403', '9', 1, 1, '2026-05-22 00:04:53.758+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (212, '2026-05-22 00:05:05.769804+00', '2026-05-22 00:05:05.769804+00', NULL, 'NHZ4254800403', '5', 1, 1, '2026-05-22 00:05:05.760251+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (213, '2026-05-22 00:05:18.868845+00', '2026-05-22 00:05:18.868845+00', NULL, 'NHZ4254800403', '3', 1, 1, '2026-05-22 00:05:18.860393+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (214, '2026-05-22 00:05:32.914212+00', '2026-05-22 00:05:32.914212+00', NULL, 'NHZ4254800403', '8', 1, 1, '2026-05-22 00:05:32.906333+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (215, '2026-05-22 00:05:43.893754+00', '2026-05-22 00:05:43.893754+00', NULL, 'NHZ4254800403', '7', 1, 1, '2026-05-22 00:05:43.885681+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (143, '2026-05-20 10:16:10.502665+00', '2026-05-20 10:16:10.502665+00', NULL, 'NHZ4254800403', '20', 1, 0, '2026-05-20 00:00:00+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (144, '2026-05-20 10:16:10.51519+00', '2026-05-20 10:16:10.51519+00', NULL, 'NHZ4254800403', '12', 1, 0, '2026-05-20 00:00:00+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (145, '2026-05-20 10:16:10.527367+00', '2026-05-20 10:16:10.527367+00', NULL, 'NHZ4254800403', '13', 1, 0, '2026-05-20 00:00:00+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (146, '2026-05-20 10:16:10.539776+00', '2026-05-20 10:16:10.539776+00', NULL, 'NHZ4254800403', '8', 1, 1, '2026-05-20 00:00:00+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (147, '2026-05-20 10:16:10.551899+00', '2026-05-20 10:16:10.551899+00', NULL, 'NHZ4254800403', '7', 1, 1, '2026-05-20 00:00:00+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (148, '2026-05-20 10:16:10.565019+00', '2026-05-20 10:16:10.565019+00', NULL, 'NHZ4254800403', '3', 1, 1, '2026-05-20 00:00:00+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (149, '2026-05-20 10:16:10.576888+00', '2026-05-20 10:16:10.576888+00', NULL, 'NHZ4254800403', '19', 1, 1, '2026-05-20 00:00:00+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (150, '2026-05-20 10:16:10.58898+00', '2026-05-20 10:16:10.58898+00', NULL, 'NHZ4254800403', '19', 1, 1, '2026-05-20 00:00:00+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (152, '2026-05-20 10:16:10.616842+00', '2026-05-20 10:16:10.616842+00', NULL, 'NHZ4254800403', '5', 1, 1, '2026-05-20 00:00:00+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (153, '2026-05-20 10:16:10.629188+00', '2026-05-20 10:16:10.629188+00', NULL, 'NHZ4254800403', '14', 1, 0, '2026-05-20 00:00:00+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (154, '2026-05-20 10:16:10.641738+00', '2026-05-20 10:16:10.641738+00', NULL, 'NHZ4254800403', '15', 1, 0, '2026-05-20 00:00:00+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (155, '2026-05-20 10:16:10.65385+00', '2026-05-20 10:16:10.65385+00', NULL, 'NHZ4254800403', '16', 1, 0, '2026-05-20 00:00:00+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (156, '2026-05-20 10:16:10.666388+00', '2026-05-20 10:16:10.666388+00', NULL, 'NHZ4254800403', '17', 1, 0, '2026-05-20 00:00:00+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (157, '2026-05-20 10:16:10.680275+00', '2026-05-20 10:16:10.680275+00', NULL, 'NHZ4254800403', '13', 1, 1, '2026-05-20 00:00:00+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (164, '2026-05-21 01:35:39.656218+00', '2026-05-21 01:35:39.656218+00', NULL, 'NHZ4254800403', '8', 1, 0, '2026-05-21 00:00:00+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (165, '2026-05-21 01:35:39.677817+00', '2026-05-21 01:35:39.677817+00', NULL, 'NHZ4254800403', '3', 1, 0, '2026-05-21 00:00:00+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (166, '2026-05-21 01:35:39.695405+00', '2026-05-21 01:35:39.695405+00', NULL, 'NHZ4254800403', '7', 1, 0, '2026-05-21 00:00:00+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (167, '2026-05-21 01:35:39.710467+00', '2026-05-21 01:35:39.710467+00', NULL, 'NHZ4254800403', '5', 1, 0, '2026-05-21 00:00:00+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (168, '2026-05-21 01:35:39.725756+00', '2026-05-21 01:35:39.725756+00', NULL, 'NHZ4254800403', '4', 1, 0, '2026-05-21 00:00:00+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (169, '2026-05-21 01:35:39.740693+00', '2026-05-21 01:35:39.740693+00', NULL, 'NHZ4254800403', '15', 1, 1, '2026-05-21 00:00:00+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (170, '2026-05-21 01:35:39.756315+00', '2026-05-21 01:35:39.756315+00', NULL, 'NHZ4254800403', '17', 1, 1, '2026-05-21 00:00:00+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (171, '2026-05-21 01:35:39.771748+00', '2026-05-21 01:35:39.771748+00', NULL, 'NHZ4254800403', '14', 1, 1, '2026-05-21 00:00:00+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (172, '2026-05-21 01:35:39.786568+00', '2026-05-21 01:35:39.786568+00', NULL, 'NHZ4254800403', '6', 1, 0, '2026-05-21 00:00:00+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (173, '2026-05-21 01:35:39.80209+00', '2026-05-21 01:35:39.80209+00', NULL, 'NHZ4254800403', '10', 1, 0, '2026-05-21 00:00:00+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (174, '2026-05-21 01:35:39.820603+00', '2026-05-21 01:35:39.820603+00', NULL, 'NHZ4254800403', '11', 1, 0, '2026-05-21 00:00:00+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (158, '2026-05-20 10:16:10.692536+00', '2026-05-20 10:16:10.692536+00', NULL, 'NHZ4254800403', '20', 1, 1, '2026-05-20 00:00:00+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (159, '2026-05-20 10:16:10.705694+00', '2026-05-20 10:16:10.705694+00', NULL, 'NHZ4254800403', '6', 1, 1, '2026-05-20 00:00:00+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (160, '2026-05-20 10:16:10.718051+00', '2026-05-20 10:16:10.718051+00', NULL, 'NHZ4254800403', '12', 1, 1, '2026-05-20 00:00:00+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (161, '2026-05-20 10:16:10.730743+00', '2026-05-20 10:16:10.730743+00', NULL, 'NHZ4254800403', '10', 1, 1, '2026-05-20 00:00:00+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (162, '2026-05-20 10:16:10.743457+00', '2026-05-20 10:16:10.743457+00', NULL, 'NHZ4254800403', '10', 1, 1, '2026-05-20 00:00:00+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (163, '2026-05-20 10:16:10.757119+00', '2026-05-20 10:16:10.757119+00', NULL, 'NHZ4254800403', '11', 1, 1, '2026-05-20 00:00:00+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (175, '2026-05-21 01:35:39.835719+00', '2026-05-21 01:35:39.835719+00', NULL, 'NHZ4254800403', '20', 1, 0, '2026-05-21 00:00:00+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (176, '2026-05-21 01:35:39.851759+00', '2026-05-21 01:35:39.851759+00', NULL, 'NHZ4254800403', '13', 1, 0, '2026-05-21 00:00:00+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (177, '2026-05-21 01:35:39.868084+00', '2026-05-21 01:35:39.868084+00', NULL, 'NHZ4254800403', '12', 1, 0, '2026-05-21 00:00:00+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (178, '2026-05-21 01:35:39.882947+00', '2026-05-21 01:35:39.882947+00', NULL, 'NHZ4254800403', '5', 1, 1, '2026-05-21 00:00:00+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (179, '2026-05-21 01:35:39.901119+00', '2026-05-21 01:35:39.901119+00', NULL, 'NHZ4254800403', '8', 1, 1, '2026-05-21 00:00:00+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (180, '2026-05-21 01:35:39.917587+00', '2026-05-21 01:35:39.917587+00', NULL, 'NHZ4254800403', '3', 1, 1, '2026-05-21 00:00:00+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (181, '2026-05-21 01:35:39.935008+00', '2026-05-21 01:35:39.935008+00', NULL, 'NHZ4254800403', '7', 1, 1, '2026-05-21 00:00:00+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (182, '2026-05-21 01:35:39.951741+00', '2026-05-21 01:35:39.951741+00', NULL, 'NHZ4254800403', '4', 1, 1, '2026-05-21 00:00:00+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (216, '2026-05-22 07:54:28.51091+00', '2026-05-22 07:54:28.51091+00', NULL, 'NHZ4254800403', '14', 1, 0, '2026-05-22 08:54:23+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (217, '2026-05-22 07:54:42.645344+00', '2026-05-22 07:54:42.645344+00', NULL, 'NHZ4254800403', '15', 1, 0, '2026-05-22 08:54:36+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (218, '2026-05-22 07:57:35.932707+00', '2026-05-22 07:57:35.932707+00', NULL, 'NHZ4254800403', '18', 1, 0, '2026-05-22 08:57:30+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (219, '2026-05-22 07:57:42.949183+00', '2026-05-22 07:57:42.949183+00', NULL, 'NHZ4254800403', '16', 1, 0, '2026-05-22 08:57:37+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (220, '2026-05-22 07:57:58.999125+00', '2026-05-22 07:57:58.999125+00', NULL, 'NHZ4254800403', '17', 1, 0, '2026-05-22 08:57:53+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (221, '2026-05-22 07:59:37.644805+00', '2026-05-22 07:59:37.644805+00', NULL, 'NHZ4254800403', '21', 1, 0, '2026-05-22 08:59:32+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (222, '2026-05-22 07:59:54.536643+00', '2026-05-22 07:59:54.536643+00', NULL, 'NHZ4254800403', '11', 1, 1, '2026-05-22 08:59:49+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (223, '2026-05-22 08:00:14.394073+00', '2026-05-22 08:00:14.394073+00', NULL, 'NHZ4254800403', '13', 1, 1, '2026-05-22 09:00:09+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (224, '2026-05-22 08:00:22.400376+00', '2026-05-22 08:00:22.400376+00', NULL, 'NHZ4254800403', '10', 1, 1, '2026-05-22 09:00:16+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (225, '2026-05-22 08:00:50.278284+00', '2026-05-22 08:00:50.278284+00', NULL, 'NHZ4254800403', '12', 1, 1, '2026-05-22 09:00:44+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (226, '2026-05-22 08:01:03.374003+00', '2026-05-22 08:01:03.374003+00', NULL, 'NHZ4254800403', '20', 1, 1, '2026-05-22 09:00:57+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (227, '2026-05-22 08:01:14.335196+00', '2026-05-22 08:01:14.335196+00', NULL, 'NHZ4254800403', '6', 1, 1, '2026-05-22 09:01:09+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (228, '2026-05-22 08:01:16.115018+00', '2026-05-22 08:01:16.115018+00', NULL, 'NHZ4254800403', '6', 1, 1, '2026-05-22 09:01:10+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (229, '2026-05-22 15:59:49.007598+00', '2026-05-22 15:59:49.007598+00', NULL, 'NHZ4254800403', '21', 1, 1, '2026-05-22 16:59:43+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (230, '2026-05-22 16:00:02.361496+00', '2026-05-22 16:00:02.361496+00', NULL, 'NHZ4254800403', '15', 1, 1, '2026-05-22 16:59:56+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (231, '2026-05-22 16:02:14.948402+00', '2026-05-22 16:02:14.948402+00', NULL, 'NHZ4254800403', '17', 1, 1, '2026-05-22 17:02:08+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (232, '2026-05-22 16:02:18.123684+00', '2026-05-22 16:02:18.123684+00', NULL, 'NHZ4254800403', '17', 1, 1, '2026-05-22 17:02:10+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (233, '2026-05-22 16:02:20.515843+00', '2026-05-22 16:02:20.515843+00', NULL, 'NHZ4254800403', '18', 1, 1, '2026-05-22 17:02:12+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (234, '2026-05-22 16:02:33.890172+00', '2026-05-22 16:02:33.890172+00', NULL, 'NHZ4254800403', '18', 1, 1, '2026-05-22 17:02:27+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (235, '2026-05-22 16:03:06.150605+00', '2026-05-22 16:03:06.150605+00', NULL, 'NHZ4254800403', '14', 1, 1, '2026-05-22 17:03:00+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (236, '2026-05-22 16:03:24.346485+00', '2026-05-22 16:03:24.346485+00', NULL, 'NHZ4254800403', '16', 1, 1, '2026-05-22 17:03:18+00', NULL, false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (259, '2026-05-28 15:52:33.361363+00', '2026-05-29 00:03:10.826262+00', NULL, 'NHZ4254800403', '10', 1, 1, '2026-05-28 15:52:25+00', '', false, '2026-05-28', 0, '2026-05-29 00:03:10.818565+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (240, '2026-05-25 03:33:18.066814+00', '2026-05-25 03:33:18.066814+00', NULL, 'NHZ4254800403', '2', 1, 0, '2026-05-25 04:33:12+00', '', false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (241, '2026-05-25 03:33:23.223491+00', '2026-05-25 03:33:23.223491+00', NULL, 'NHZ4254800403', '2', 1, 0, '2026-05-25 04:33:16+00', '', false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (242, '2026-05-25 03:33:27.041016+00', '2026-05-25 03:33:27.041016+00', NULL, 'NHZ4254800403', '2', 1, 0, '2026-05-25 04:33:21+00', '', false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (257, '2026-05-28 15:51:58.339922+00', '2026-05-29 00:03:19.674313+00', NULL, 'NHZ4254800403', '11', 1, 1, '2026-05-28 15:51:50+00', '', false, '2026-05-28', 0, '2026-05-29 00:03:19.666719+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (262, '2026-05-28 15:56:37.54599+00', '2026-05-29 00:03:59.90304+00', NULL, 'NHZ4254800403', '12', 1, 1, '2026-05-28 15:56:29+00', '', false, '2026-05-28', 0, '2026-05-29 00:03:59.895517+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (285, '2026-05-29 15:59:08.424845+00', '2026-05-29 23:59:59.928287+00', NULL, 'NHZ4254800403', '13', 1, 1, '2026-05-29 15:59:08.417394+00', '', false, '2026-05-29', 0, '2026-05-29 23:59:59.918303+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (283, '2026-05-29 15:56:17.449398+00', '2026-05-30 00:00:07.993704+00', NULL, 'NHZ4254800403', '20', 1, 1, '2026-05-29 15:56:17.443175+00', '', false, '2026-05-29', 0, '2026-05-30 00:00:07.987946+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (281, '2026-05-29 15:51:41.744444+00', '2026-05-30 00:00:16.728505+00', NULL, 'NHZ4254800403', '10', 1, 1, '2026-05-29 15:51:41.738557+00', '', false, '2026-05-29', 0, '2026-05-30 00:00:16.722289+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (280, '2026-05-29 15:51:33.288447+00', '2026-05-30 00:00:22.866395+00', NULL, 'NHZ4254800403', '11', 1, 1, '2026-05-29 15:51:33.280105+00', '', false, '2026-05-29', 0, '2026-05-30 00:00:22.859921+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (284, '2026-05-29 15:56:27.42686+00', '2026-05-30 00:00:30.893387+00', NULL, 'NHZ4254800403', '12', 1, 1, '2026-05-29 15:56:27.420562+00', '', false, '2026-05-29', 0, '2026-05-30 00:00:30.888398+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (254, '2026-05-28 08:23:01.508029+00', '2026-05-28 14:20:37.262843+00', NULL, 'NHZ4254800403', '18', 1, 1, '2026-05-28 08:22:54+00', '', false, NULL, 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (252, '2026-05-28 08:21:07.129923+00', '2026-05-30 05:37:56.375678+00', NULL, 'NHZ4254800403', '15', 1, 1, '2026-05-27 23:52:00+00', '', false, '2026-05-28', 0, '2026-05-28 08:03:00+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (255, '2026-05-28 08:23:13.307685+00', '2026-05-30 05:38:42.175957+00', NULL, 'NHZ4254800403', '14', 1, 1, '2026-05-27 23:52:00+00', '', false, '2026-05-28', 0, '2026-05-28 08:22:00+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (263, '2026-05-28 15:59:02.763607+00', '2026-05-30 05:39:28.688124+00', NULL, 'NHZ4254800403', '13', 1, 1, '2026-05-28 00:02:00+00', '', false, '2026-05-28', 2, '2026-05-28 08:02:00+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (253, '2026-05-28 08:22:55.673601+00', '2026-05-30 05:41:23.254099+00', NULL, 'NHZ4254800403', '18', 1, 1, '2026-05-27 23:22:00+00', '', false, '2026-05-28', 0, '2026-05-28 08:09:00+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (282, '2026-05-29 15:53:04.874674+00', '2026-05-30 05:42:27.468518+00', NULL, 'NHZ4254800403', '6', 1, 1, '2026-05-29 15:53:00+00', '', false, '2026-05-29', 0, '2026-05-30 00:04:00+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (275, '2026-05-29 08:06:29.15381+00', '2026-05-30 05:43:38.603686+00', NULL, 'NHZ4254800403', '14', 1, 1, '2026-05-28 23:52:00+00', '', false, '2026-05-29', 0, '2026-05-29 08:04:00+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (276, '2026-05-29 08:12:55.469488+00', '2026-05-30 05:45:10.562494+00', NULL, 'NHZ4254800403', '16', 1, 1, '2026-05-28 23:55:00+00', '', false, '2026-05-29', 0, '2026-05-29 08:09:00+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (258, '2026-05-28 15:52:21.255354+00', '2026-05-28 19:27:34.163165+00', NULL, 'NHZ4254800403', '11', 1, 0, '2026-05-28 15:52:13+00', '', false, '2026-05-28', 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (291, '2026-05-30 05:53:47.99817+00', '2026-05-30 23:57:25.324518+00', NULL, 'NHZ4254800403', '22', 1, 1, '2026-05-29 23:53:00+00', '', false, '2026-05-30', 0, '2026-05-30 23:57:25.074877+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (302, '2026-05-30 15:59:19.442021+00', '2026-05-30 23:59:13.430798+00', NULL, 'NHZ4254800403', '13', 1, 1, '2026-05-30 15:59:19.192798+00', '', false, '2026-05-30', 0, '2026-05-30 23:59:13.181066+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (298, '2026-05-30 15:54:04.370265+00', '2026-05-30 23:59:35.250122+00', NULL, 'NHZ4254800403', '11', 1, 1, '2026-05-30 15:54:04.120812+00', '', false, '2026-05-30', 0, '2026-05-30 23:59:34.999934+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (300, '2026-05-30 15:58:00.549934+00', '2026-05-31 00:00:17.452396+00', NULL, 'NHZ4254800403', '20', 1, 1, '2026-05-30 15:58:00.300179+00', '', false, '2026-05-30', 0, '2026-05-31 00:00:17.20107+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (299, '2026-05-30 15:54:28.16638+00', '2026-05-31 00:00:41.257009+00', NULL, 'NHZ4254800403', '10', 1, 1, '2026-05-30 15:54:27.916607+00', '', false, '2026-05-30', 0, '2026-05-31 00:00:41.006965+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (301, '2026-05-30 15:58:15.467772+00', '2026-05-31 00:01:14.076862+00', NULL, 'NHZ4254800403', '12', 1, 1, '2026-05-30 15:58:15.216455+00', '', false, '2026-05-30', 0, '2026-05-31 00:01:13.826981+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (297, '2026-05-30 15:52:28.44993+00', '2026-05-31 00:01:36.115516+00', NULL, 'NHZ4254800403', '6', 1, 1, '2026-05-30 15:52:28.074996+00', '', false, '2026-05-30', 0, '2026-05-31 00:01:35.8659+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (290, '2026-05-30 05:49:49.710778+00', '2026-05-31 00:03:14.089186+00', NULL, '', '16', 0, 1, '2026-05-29 23:54:00+00', '', false, '2026-05-30', 0, '2026-05-31 00:03:13.839367+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (303, '2026-05-31 07:51:13.426893+00', '2026-05-31 07:51:13.426893+00', NULL, 'NHZ4254800403', '8', 1, 0, '2026-05-31 07:51:13.176662+00', '', false, '2026-05-31', 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (304, '2026-05-31 07:51:26.475847+00', '2026-05-31 07:51:26.475847+00', NULL, 'NHZ4254800403', '7', 1, 0, '2026-05-31 07:51:26.226424+00', '', false, '2026-05-31', 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (305, '2026-05-31 07:51:45.37643+00', '2026-05-31 07:51:45.37643+00', NULL, 'NHZ4254800403', '12', 1, 0, '2026-05-31 07:51:45.126898+00', '', false, '2026-05-31', 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (306, '2026-05-31 07:55:44.644524+00', '2026-05-31 07:55:44.644524+00', NULL, 'NHZ4254800403', '3', 1, 0, '2026-05-31 07:55:44.392743+00', '', false, '2026-05-31', 0, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (307, '2026-05-31 08:08:21.681406+00', '2026-05-31 08:08:24.67483+00', NULL, 'NHZ4254800403', '15', 1, 1, '2026-05-31 08:08:21.431571+00', '', false, '2026-05-31', 8, '2026-05-31 08:08:24.425166+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (308, '2026-05-31 08:08:50.801837+00', '2026-05-31 08:08:50.801837+00', NULL, 'NHZ4254800403', '14', 1, 0, '2026-05-31 08:08:50.551957+00', '', false, '2026-05-31', 8, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (260, '2026-05-28 15:52:49.309367+00', '2026-05-29 00:02:20.849373+00', NULL, 'NHZ4254800403', '6', 1, 1, '2026-05-28 15:52:41+00', '', false, '2026-05-28', 0, '2026-05-29 00:02:20.841728+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (261, '2026-05-28 15:55:31.39532+00', '2026-05-29 00:02:53.997505+00', NULL, 'NHZ4254800403', '20', 1, 1, '2026-05-28 15:55:23+00', '', false, '2026-05-28', 0, '2026-05-29 00:02:53.989924+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (247, '2026-05-28 07:51:50.362579+00', '2026-05-28 19:27:40.144337+00', NULL, 'NHZ4254800403', '8', 1, 1, '2026-05-28 07:51:42+00', '', false, '2026-05-28', 0, '2026-05-28 16:13:17+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (250, '2026-05-28 07:58:57.639665+00', '2026-05-28 19:27:38.648253+00', NULL, 'NHZ4254800403', '3', 1, 1, '2026-05-28 07:58:51+00', '', false, '2026-05-28', 0, '2026-05-28 16:13:22+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (249, '2026-05-28 07:56:44.588026+00', '2026-05-28 19:27:39.020843+00', NULL, 'NHZ4254800403', '4', 1, 1, '2026-05-28 07:56:37+00', '', false, '2026-05-28', 0, '2026-05-28 16:13:36+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (248, '2026-05-28 07:52:24.333181+00', '2026-05-28 19:27:39.771294+00', NULL, 'NHZ4254800403', '7', 1, 1, '2026-05-28 07:52:17+00', '', false, '2026-05-28', 0, '2026-05-28 16:13:47+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (251, '2026-05-28 08:20:57.04488+00', '2026-05-30 05:36:21.630283+00', NULL, 'NHZ4254800403', '17', 1, 1, '2026-05-27 23:54:00+00', '', false, '2026-05-28', 0, '2026-05-28 08:20:00+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (256, '2026-05-28 08:48:55.569059+00', '2026-05-30 05:37:08.639219+00', NULL, 'NHZ4254800403', '16', 1, 1, '2026-05-28 00:00:00+00', '', false, '2026-05-28', 0, '2026-05-28 08:00:00+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (274, '2026-05-29 08:05:54.079596+00', '2026-05-30 05:44:04.567873+00', NULL, 'NHZ4254800403', '15', 1, 1, '2026-05-28 23:56:00+00', '', false, '2026-05-29', 0, '2026-05-29 08:04:00+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (295, '2026-05-30 08:02:17.353408+00', '2026-05-30 16:07:50.712027+00', NULL, 'NHZ4254800403', '5', 1, 1, '2026-05-30 08:02:17.344032+00', '', false, '2026-05-30', 2, '2026-05-30 16:07:50.462116+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (294, '2026-05-30 07:56:28.145429+00', '2026-05-30 16:09:50.050355+00', NULL, 'NHZ4254800403', '3', 1, 1, '2026-05-30 07:56:28.13276+00', '', false, '2026-05-30', 0, '2026-05-30 16:09:49.800855+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (293, '2026-05-30 07:55:08.082901+00', '2026-05-30 16:10:19.876652+00', NULL, 'NHZ4254800403', '4', 1, 1, '2026-05-30 07:55:08.07347+00', '', false, '2026-05-30', 0, '2026-05-30 16:10:19.627158+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (292, '2026-05-30 07:51:46.488557+00', '2026-05-30 16:10:30.039008+00', NULL, 'NHZ4254800403', '7', 1, 1, '2026-05-30 07:51:46.473383+00', '', false, '2026-05-30', 0, '2026-05-30 16:10:29.78943+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (309, '2026-05-31 08:09:01.818765+00', '2026-05-31 08:09:01.818765+00', NULL, 'NHZ4254800403', '22', 1, 0, '2026-05-31 08:09:01.56915+00', '', false, '2026-05-31', 9, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (310, '2026-05-31 08:09:42.860002+00', '2026-05-31 08:09:42.860002+00', NULL, 'NHZ4254800403', '17', 1, 0, '2026-05-31 08:09:42.610407+00', '', false, '2026-05-31', 9, NULL);
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (311, '2026-05-31 08:09:59.738584+00', '2026-05-31 08:10:02.443551+00', NULL, 'NHZ4254800403', '18', 1, 1, '2026-05-31 08:09:59.488957+00', '', false, '2026-05-31', 9, '2026-05-31 08:10:02.190593+00');
INSERT INTO public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) VALUES (312, '2026-05-31 08:10:11.369444+00', '2026-05-31 08:10:11.369444+00', NULL, 'NHZ4254800403', '16', 1, 0, '2026-05-31 08:10:11.120172+00', '', false, '2026-05-31', 10, NULL);


--
-- Data for Name: bahan_nono_items; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (39, '2026-05-04 03:22:17.624683+00', '2026-05-04 03:22:17.624683+00', '2026-05-04 03:22:52.73295+00', 62, 'Super A', 1538, 'Kg', 8300);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (40, '2026-05-04 03:24:28.159495+00', '2026-05-04 03:24:28.159495+00', NULL, 63, 'Super A', 1538, 'Kg', 8300);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (41, '2026-05-04 03:26:00.35051+00', '2026-05-04 03:26:00.35051+00', NULL, 64, 'Super A', 1602, 'Kg', 8300);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (42, '2026-05-04 03:27:40.978407+00', '2026-05-04 03:27:40.978407+00', NULL, 65, 'Super A', 2242, 'Kg', 8300);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (43, '2026-05-05 13:37:48.895224+00', '2026-05-05 13:37:48.895224+00', NULL, 67, 'Super A', 2294, 'Kg', 8500);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (44, '2026-05-12 07:21:01.331559+00', '2026-05-12 07:21:01.331559+00', NULL, 69, 'Super A', 2366, 'Kg', 8500);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (45, '2026-05-12 10:53:22.0169+00', '2026-05-12 10:53:22.0169+00', NULL, 70, 'Cerah A', 1291, 'Kg', 9500);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (46, '2026-05-12 10:53:22.025896+00', '2026-05-12 10:53:22.025896+00', NULL, 70, 'Super A', 982, 'Kg', 8500);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (47, '2026-05-13 08:22:55.683918+00', '2026-05-13 08:22:55.683918+00', NULL, 71, 'Super A', 3038, 'Kg', 8500);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (49, '2026-05-14 08:03:12.91541+00', '2026-05-14 08:03:12.91541+00', NULL, 74, 'Super A', 2274, 'Kg', 8500);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (48, '2026-05-14 07:56:44.353713+00', '2026-05-14 07:56:44.353713+00', '2026-05-14 08:03:28.572376+00', 73, 'Super A', 2194, 'Kg', 8500);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (50, '2026-05-14 08:03:28.580957+00', '2026-05-14 08:03:28.580957+00', NULL, 73, 'Super A', 2194, 'Kg', 8500);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (51, '2026-05-15 08:05:41.1075+00', '2026-05-15 08:05:41.1075+00', NULL, 76, 'Super A', 1712, 'Kg', 8500);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (52, '2026-05-23 02:43:55.229593+00', '2026-05-23 02:43:55.229593+00', NULL, 78, 'Super A', 1933, 'Kg', 8500);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (53, '2026-05-23 02:43:55.237659+00', '2026-05-23 02:43:55.237659+00', NULL, 78, '[JASA] Titip Giling', 476, 'Kg', 1200);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (54, '2026-05-23 03:07:42.309648+00', '2026-05-23 03:07:42.309648+00', '2026-05-23 03:08:10.398093+00', 82, 'Super A', 1220, 'Kg', 8500);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (55, '2026-05-23 03:07:42.317344+00', '2026-05-23 03:07:42.317344+00', '2026-05-23 03:08:10.398093+00', 82, '[JASA] Titip Giling', 202, 'Kg', 1200);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (56, '2026-05-23 03:08:10.401918+00', '2026-05-23 03:08:10.401918+00', '2026-05-23 03:08:36.334284+00', 82, 'Super A', 1231, 'Kg', 8500);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (57, '2026-05-23 03:08:10.405829+00', '2026-05-23 03:08:10.405829+00', '2026-05-23 03:08:36.334284+00', 82, '[JASA] Titip Giling', 202, 'Kg', 1200);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (58, '2026-05-23 03:08:36.338391+00', '2026-05-23 03:08:36.338391+00', '2026-05-23 03:09:26.491845+00', 82, 'Super A', 1241, 'Kg', 8500);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (59, '2026-05-23 03:08:36.342286+00', '2026-05-23 03:08:36.342286+00', '2026-05-23 03:09:26.491845+00', 82, '[JASA] Titip Giling', 202, 'Kg', 1200);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (60, '2026-05-23 03:09:26.495762+00', '2026-05-23 03:09:26.495762+00', '2026-05-23 03:10:39.464987+00', 82, 'Super A', 1214, 'Kg', 8500);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (61, '2026-05-23 03:09:26.499504+00', '2026-05-23 03:09:26.499504+00', '2026-05-23 03:10:39.464987+00', 82, '[JASA] Titip Giling', 202, 'Kg', 1200);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (62, '2026-05-23 03:10:39.468707+00', '2026-05-23 03:10:39.468707+00', '2026-05-23 03:11:07.33544+00', 82, 'Super A', 1228, 'Kg', 8500);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (63, '2026-05-23 03:10:39.472624+00', '2026-05-23 03:10:39.472624+00', '2026-05-23 03:11:07.33544+00', 82, '[JASA] Titip Giling', 202, 'Kg', 1200);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (64, '2026-05-23 03:11:07.339193+00', '2026-05-23 03:11:07.339193+00', '2026-05-23 03:12:53.41432+00', 82, 'Super A', 1228, 'Kg', 8500);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (65, '2026-05-23 03:11:07.343088+00', '2026-05-23 03:11:07.343088+00', '2026-05-23 03:12:53.41432+00', 82, '[JASA] Titip Giling', 204, 'Kg', 1200);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (66, '2026-05-23 03:12:53.41804+00', '2026-05-23 03:12:53.41804+00', '2026-05-23 03:13:16.998722+00', 82, 'Super A', 1220, 'Kg', 8500);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (67, '2026-05-23 03:12:53.421773+00', '2026-05-23 03:12:53.421773+00', '2026-05-23 03:13:16.998722+00', 82, '[JASA] Titip Giling', 204, 'Kg', 1200);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (68, '2026-05-23 03:13:17.002404+00', '2026-05-23 03:13:17.002404+00', '2026-05-23 03:14:26.661607+00', 82, 'Super A', 1220, 'Kg', 8500);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (69, '2026-05-23 03:13:17.006045+00', '2026-05-23 03:13:17.006045+00', '2026-05-23 03:14:26.661607+00', 82, '[JASA] Titip Giling', 202, 'Kg', 1200);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (70, '2026-05-23 03:14:26.665458+00', '2026-05-23 03:14:26.665458+00', '2026-05-23 03:14:44.31245+00', 82, 'Super A', 1220.5, 'Kg', 8500);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (71, '2026-05-23 03:14:26.669189+00', '2026-05-23 03:14:26.669189+00', '2026-05-23 03:14:44.31245+00', 82, '[JASA] Titip Giling', 202, 'Kg', 1200);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (72, '2026-05-23 03:14:44.316304+00', '2026-05-23 03:14:44.316304+00', '2026-05-23 03:15:01.24848+00', 82, 'Super A', 1221, 'Kg', 8500);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (73, '2026-05-23 03:14:44.320136+00', '2026-05-23 03:14:44.320136+00', '2026-05-23 03:15:01.24848+00', 82, '[JASA] Titip Giling', 202, 'Kg', 1200);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (74, '2026-05-23 03:15:01.252486+00', '2026-05-23 03:15:01.252486+00', '2026-05-23 03:16:21.762762+00', 82, 'Super A', 1220, 'Kg', 8500);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (75, '2026-05-23 03:15:01.25639+00', '2026-05-23 03:15:01.25639+00', '2026-05-23 03:16:21.762762+00', 82, '[JASA] Titip Giling', 202, 'Kg', 1200);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (76, '2026-05-23 03:16:21.766504+00', '2026-05-23 03:16:21.766504+00', '2026-05-23 03:17:31.115164+00', 82, 'Super A', 1220.59, 'Kg', 8500);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (77, '2026-05-23 03:16:21.770171+00', '2026-05-23 03:16:21.770171+00', '2026-05-23 03:17:31.115164+00', 82, '[JASA] Titip Giling', 202, 'Kg', 1200);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (80, '2026-05-23 03:33:22.890023+00', '2026-05-23 03:33:22.890023+00', NULL, 83, 'Super A', 2350, 'Kg', 8500);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (81, '2026-05-23 06:23:38.028107+00', '2026-05-23 06:23:38.028107+00', NULL, 86, 'Super A', 1413, 'Kg', 8500);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (78, '2026-05-23 03:17:31.119038+00', '2026-05-23 03:17:31.119038+00', '2026-05-23 06:24:14.921024+00', 82, 'Super A', 1220.55, 'Kg', 8500);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (79, '2026-05-23 03:17:31.1229+00', '2026-05-23 03:17:31.1229+00', '2026-05-23 06:24:14.921024+00', 82, '[JASA] Titip Giling', 202, 'Kg', 1200);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (82, '2026-05-23 06:24:14.92533+00', '2026-05-23 06:24:14.92533+00', NULL, 82, 'Super A', 1220, 'Kg', 8500);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (83, '2026-05-23 06:24:14.929117+00', '2026-05-23 06:24:14.929117+00', NULL, 82, '[JASA] Titip Giling', 202, 'Kg', 1200);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (84, '2026-05-23 06:26:59.506811+00', '2026-05-23 06:26:59.506811+00', '2026-05-23 06:29:33.578284+00', 87, 'Super A', 1300, 'Kg', 8500);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (85, '2026-05-23 06:26:59.51062+00', '2026-05-23 06:26:59.51062+00', '2026-05-23 06:29:33.578284+00', 87, '[JASA] Titip Giling', 690, 'Kg', 1200);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (86, '2026-05-23 06:29:33.581901+00', '2026-05-23 06:29:33.581901+00', NULL, 87, 'Super A', 1300, 'Kg', 8500);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (87, '2026-05-23 06:29:33.585574+00', '2026-05-23 06:29:33.585574+00', NULL, 87, '[JASA] Titip Giling', 690, 'Kg', 1200);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (88, '2026-05-23 06:39:54.642734+00', '2026-05-23 06:39:54.642734+00', NULL, 88, 'Super A', 1872, 'Kg', 8500);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (89, '2026-05-23 06:40:58.957132+00', '2026-05-23 06:40:58.957132+00', NULL, 89, 'Super A', 2018, 'Kg', 8500);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (90, '2026-05-23 06:41:59.735831+00', '2026-05-23 06:41:59.735831+00', '2026-05-23 06:42:42.77237+00', 90, 'Super A', 1464, 'Kg', 8500);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (91, '2026-05-23 06:41:59.739589+00', '2026-05-23 06:41:59.739589+00', '2026-05-23 06:42:42.77237+00', 90, '[JASA] Titip Giling', 290, 'Kg', 1200);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (92, '2026-05-23 06:42:42.776323+00', '2026-05-23 06:42:42.776323+00', NULL, 90, 'Super A', 1464, 'Kg', 8300);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (93, '2026-05-23 06:42:42.780215+00', '2026-05-23 06:42:42.780215+00', NULL, 90, '[JASA] Titip Giling', 290, 'Kg', 9500);
INSERT INTO public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) VALUES (94, '2026-05-30 08:08:57.894778+00', '2026-05-30 08:08:57.894778+00', NULL, 92, 'Super A', 2434, 'Kg', 8500);


--
-- Data for Name: bahan_nonos; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (27, '2026-04-14 05:44:47.992874+00', '2026-04-14 05:44:47.992874+00', NULL, '2026-04-10', 0, 'setoran pagi', 'uploads/tagihan_nono/17761454502722252457073246195439_guzcr9', 179440200, '2026-04-14 05:44:47.992874+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (28, '2026-04-14 05:45:59.594776+00', '2026-04-14 05:45:59.594776+00', NULL, '2026-04-10', 0, 'setoran pagi', 'uploads/tagihan_nono/17761455335001719376742123408505_r2beku', 17632000, '2026-04-14 05:45:59.594776+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (34, '2026-04-14 06:00:36.464712+00', '2026-04-14 06:00:36.464712+00', NULL, '2026-04-14', 20000000, 'TF', 'uploads/tagihan_nono/17761464197856947976188751566836_eu43mv', 0, '2026-04-14 06:00:36.464712+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (35, '2026-04-14 06:04:13.839212+00', '2026-04-14 06:04:13.839212+00', NULL, '2026-04-10', 20000000, 'TF', 'uploads/tagihan_nono/17761466431441880580606829538504_kyu7c3', 0, '2026-04-14 06:04:13.839212+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (36, '2026-04-14 06:04:53.406573+00', '2026-04-14 06:04:53.406573+00', NULL, '2026-04-13', 50000000, 'TF', 'uploads/tagihan_nono/17761466834582792451293268839747_vavvbj', 0, '2026-04-14 06:04:53.406573+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (37, '2026-04-14 08:51:57.321532+00', '2026-04-14 08:51:57.321532+00', NULL, '2026-04-14', 0, 'setoran sore', 'uploads/tagihan_nono/17761566957829027145017300715374_ri2mjn', 18320000, '2026-04-14 08:51:57.321532+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (38, '2026-04-15 07:45:38.263314+00', '2026-04-15 07:45:38.263314+00', NULL, '2026-04-15', 0, 'setoran sore', 'uploads/tagihan_nono/17762390918807468112277313179839_roa3f6', 18000000, '2026-04-15 07:45:38.263314+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (39, '2026-04-16 08:08:01.534708+00', '2026-04-16 08:08:01.534708+00', NULL, '2026-04-16', 0, 'setoran sore', 'uploads/tagihan_nono/17763268993418295397076887277854_fca9ge', 18990400, '2026-04-16 08:08:01.534708+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (40, '2026-04-16 11:43:06.213595+00', '2026-04-16 11:43:06.213595+00', NULL, '2026-04-16', 40000000, 'TF BRI', 'uploads/tagihan_nono/IMG-20260416-WA0022_rc80m1', 0, '2026-04-16 11:43:06.213595+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (41, '2026-04-17 08:19:36.181414+00', '2026-04-17 08:19:36.181414+00', NULL, '2026-04-17', 0, 'setoran sore', 'uploads/tagihan_nono/17764139289395154966230074889545_g5jwp6', 20608900, '2026-04-17 08:19:36.181414+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (42, '2026-04-20 12:11:18.96981+00', '2026-04-20 12:11:18.96981+00', NULL, '2026-04-20', 30000000, 'TF', 'uploads/tagihan_nono/IMG-20260420-WA0012_keul4m', 0, '2026-04-20 12:11:18.96981+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (43, '2026-04-21 02:48:16.837747+00', '2026-04-21 02:48:16.837747+00', NULL, '2026-04-20', 0, 'setoran sore', 'uploads/tagihan_nono/IMG_20260421_094629_hn571h', 16500400, '2026-04-21 02:48:16.837747+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (44, '2026-04-21 02:49:29.60494+00', '2026-04-21 02:49:29.60494+00', NULL, '2026-04-21', 30000000, 'Tunai ', 'uploads/tagihan_nono/17767397404302738797771452715500_fwqow8', 0, '2026-04-21 02:49:29.60494+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (45, '2026-04-21 08:19:36.562927+00', '2026-04-21 08:19:36.562927+00', NULL, '2026-04-21', 0, 'setoran sore', 'uploads/tagihan_nono/17767595408294200217397161003955_xnxj92', 22874800, '2026-04-21 08:19:36.562927+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (46, '2026-04-22 08:49:09.567383+00', '2026-04-22 08:49:09.567383+00', NULL, '2026-04-22', 0, 'setoran sore', 'uploads/tagihan_nono/IMG-20260423-WA0005_iuxl1a', 8300000, '2026-04-22 08:49:09.567383+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (47, '2026-04-23 10:17:51.550481+00', '2026-04-23 10:17:51.550481+00', NULL, '2026-04-23', 25000000, 'Tunai', '', 0, '2026-04-23 10:17:51.550481+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (48, '2026-04-24 04:16:45.718338+00', '2026-04-24 04:16:45.718338+00', NULL, '2026-04-24', 40000000, 'TF BRI', 'uploads/tagihan_nono/IMG-20260424-WA0049_qqcodz', 0, '2026-04-24 04:16:45.718338+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (49, '2026-04-24 11:01:57.423479+00', '2026-04-24 11:01:57.423479+00', NULL, '2026-04-24', 0, 'setoran sore', 'uploads/tagihan_nono/IMG_20260424_155131_rrioso', 16467200, '2026-04-24 11:01:57.423479+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (50, '2026-04-24 11:02:48.074742+00', '2026-04-24 11:02:48.074742+00', NULL, '2026-04-23', 0, 'setoran sore', 'uploads/tagihan_nono/IMG-20260424-WA0067_trei8a', 14491800, '2026-04-24 11:02:48.074742+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (51, '2026-04-25 07:49:28.48068+00', '2026-04-25 07:49:28.48068+00', NULL, '2026-04-25', 0, 'setoran sore', 'uploads/tagihan_nono/IMG-20260425-WA0027_gfrxxj', 29904900, '2026-04-25 07:49:28.48068+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (52, '2026-04-25 09:42:52.561789+00', '2026-04-25 09:42:52.561789+00', NULL, '2026-04-25', 2000000, 'TF', 'uploads/tagihan_nono/IMG-20260425-WA0036_qzpvuu', 0, '2026-04-25 09:42:52.561789+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (53, '2026-04-27 07:58:47.518623+00', '2026-04-27 07:58:47.518623+00', NULL, '2026-04-27', 0, 'Setoran Sore', 'uploads/tagihan_nono/IMG_20260427_145256_lhaqbk', 14201300, '2026-04-27 07:58:47.518623+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (54, '2026-04-28 05:11:09.209695+00', '2026-04-28 05:11:09.209695+00', NULL, '2026-04-28', 35000000, 'TF BRI', 'uploads/tagihan_nono/IMG-20260428-WA0064_sh34o2', 0, '2026-04-28 05:11:09.209695+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (25, '2026-04-14 05:26:37.714393+00', '2026-04-14 05:26:37.714393+00', NULL, '2026-04-13', 0, '', 'uploads/tagihan_nono/17761443724868552854407591220813_b9jlcj', 21200000, '2026-04-14 05:26:37.714393+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (55, '2026-04-29 15:54:07.399032+00', '2026-04-29 15:54:07.399032+00', '2026-04-29 15:54:27.476846+00', '2026-04-29', 40000000, 'Setoran Sore', '', 0, '0001-01-01 00:00:00+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (56, '2026-05-04 02:42:07.32638+00', '2026-05-04 02:42:07.32638+00', '2026-05-04 02:43:13.059806+00', '2026-05-04', 30000000, 'TF', '', 0, '0001-01-01 00:00:00+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (57, '2026-05-04 02:46:28.627049+00', '2026-05-04 02:46:28.627049+00', '2026-05-04 02:50:47.348621+00', '2026-05-04', 30000000, '', '', 0, '0001-01-01 00:00:00+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (58, '2026-05-04 02:51:06.697878+00', '2026-05-04 02:51:06.697878+00', '2026-05-04 02:51:20.888774+00', '2026-05-04', 30000000, '', '', 0, '0001-01-01 00:00:00+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (59, '2026-05-04 02:53:50.567494+00', '2026-05-04 02:53:50.567494+00', '2026-05-04 03:05:17.559195+00', '2026-05-04', 30000000, '', '', 0, '0001-01-01 00:00:00+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (60, '2026-05-04 03:06:23.307134+00', '2026-05-04 03:06:23.307134+00', '2026-05-04 03:13:45.237092+00', '2026-05-04', 30000000, '', '', 0, '0001-01-01 00:00:00+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (62, '2026-05-04 03:22:17.620586+00', '2026-05-04 03:22:17.620586+00', '2026-05-04 03:22:52.747131+00', '2026-05-04', 0, 'Setoran sore', '', 12765400, '0001-01-01 00:00:00+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (63, '2026-05-04 03:24:28.155237+00', '2026-05-04 03:24:28.155237+00', NULL, '2026-05-01', 0, 'Setoran Sore', 'https://res.cloudinary.com/dkkbizenf/image/upload/v1777865016/tagihan_nono/oruidmarofdscwr9irnb.jpg', 12765400, '0001-01-01 00:00:00+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (64, '2026-05-04 03:26:00.346444+00', '2026-05-04 03:26:00.346444+00', NULL, '2026-04-30', 0, 'Setoran Sore', 'https://res.cloudinary.com/dkkbizenf/image/upload/v1777865145/tagihan_nono/h0gawmv6qvojdpqbgvaa.jpg', 13296600, '0001-01-01 00:00:00+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (65, '2026-05-04 03:27:40.974409+00', '2026-05-04 03:27:40.974409+00', NULL, '2026-04-29', 0, 'setoran sore', 'https://res.cloudinary.com/dkkbizenf/image/upload/v1777865231/tagihan_nono/qfnsoxfzyhbtynkemxz8.jpg', 18608600, '0001-01-01 00:00:00+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (66, '2026-05-04 03:29:45.083487+00', '2026-05-04 03:29:45.083487+00', NULL, '2026-04-29', 40000000, 'TF BRI', 'https://res.cloudinary.com/dkkbizenf/image/upload/v1777865363/tagihan_nono/vho6xp24i5ea0gxqtjhk.jpg', 0, '0001-01-01 00:00:00+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (67, '2026-05-05 13:37:48.886024+00', '2026-05-05 13:37:48.886024+00', NULL, '2026-05-05', 0, '', 'https://res.cloudinary.com/dkkbizenf/image/upload/v1777988254/tagihan_nono/dfkma21izc64zzrye5ce.jpg', 19499000, '0001-01-01 00:00:00+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (74, '2026-05-14 08:03:12.906299+00', '2026-05-14 08:03:12.906299+00', NULL, '2026-05-14', 0, 'setoran sore', 'https://res.cloudinary.com/dkkbizenf/image/upload/v1778745773/tagihan_nono/ie3bkl6cgf5jmmvnrq1a.jpg', 19329000, '0001-01-01 00:00:00+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (61, '2026-05-04 03:14:59.151379+00', '2026-05-07 02:33:50.973218+00', NULL, '2026-05-04', 30000000, 'TF BCA', 'https://res.cloudinary.com/dkkbizenf/image/upload/v1777864490/tagihan_nono/gqpvmvadaetezls7gyh3.jpg', 0, '0001-01-01 00:00:00+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (68, '2026-05-05 13:38:46.91001+00', '2026-05-07 02:34:04.852714+00', NULL, '2026-05-05', 30000000, 'TF BCA', 'https://res.cloudinary.com/dkkbizenf/image/upload/v1777988311/tagihan_nono/hwfp5q9syxqpr5kkfmro.jpg', 0, '0001-01-01 00:00:00+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (69, '2026-05-12 07:21:01.32098+00', '2026-05-12 07:21:01.32098+00', NULL, '2026-05-12', 0, 'setoran sore', 'https://res.cloudinary.com/dkkbizenf/image/upload/v1778570447/tagihan_nono/wpu0i86vu5s37pwaatuz.jpg', 20111000, '0001-01-01 00:00:00+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (70, '2026-05-12 10:53:22.0067+00', '2026-05-12 10:53:22.0067+00', NULL, '2026-05-12', 0, 'setoran sore', 'https://res.cloudinary.com/dkkbizenf/image/upload/v1778583198/tagihan_nono/hi2wbvfyl2fdc2otyezw.jpg', 20611500, '0001-01-01 00:00:00+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (71, '2026-05-13 08:22:55.675204+00', '2026-05-13 08:22:55.675204+00', NULL, '2026-05-13', 0, 'setoran sore', 'https://res.cloudinary.com/dkkbizenf/image/upload/v1778660560/tagihan_nono/ts9bsemoaafcnu2msmyo.jpg', 25823000, '0001-01-01 00:00:00+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (72, '2026-05-13 08:24:35.123167+00', '2026-05-13 08:24:35.123167+00', NULL, '2026-05-12', 5000000, 'Tunai', '', 0, '0001-01-01 00:00:00+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (73, '2026-05-14 07:56:44.344712+00', '2026-05-14 08:03:28.585344+00', NULL, '2026-05-08', 0, 'setoran sore', 'https://res.cloudinary.com/dkkbizenf/image/upload/v1778745388/tagihan_nono/brdpbojnr1lmbd6ygua5.jpg', 18649000, '0001-01-01 00:00:00+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (75, '2026-05-14 12:42:29.103935+00', '2026-05-14 12:42:29.103935+00', NULL, '2026-05-14', 30000000, 'TF BCA', 'https://res.cloudinary.com/dkkbizenf/image/upload/v1778762536/tagihan_nono/z96rlbetvlac5ddxzhpa.jpg', 0, '0001-01-01 00:00:00+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (76, '2026-05-15 08:05:41.099404+00', '2026-05-15 08:05:41.099404+00', NULL, '2026-05-15', 0, 'setoran sore', 'https://res.cloudinary.com/dkkbizenf/image/upload/v1778832317/tagihan_nono/i84aifkefru7yxdflrzc.jpg', 14552000, '0001-01-01 00:00:00+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (77, '2026-05-15 08:40:32.888717+00', '2026-05-15 08:40:32.888717+00', NULL, '2026-05-15', 40000000, 'TF BRI', 'https://res.cloudinary.com/dkkbizenf/image/upload/v1778834418/tagihan_nono/pxsp1npidaedmfdo3imx.jpg', 0, '0001-01-01 00:00:00+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (78, '2026-05-23 02:43:55.221884+00', '2026-05-23 02:43:55.221884+00', NULL, '2026-05-18', 0, 'setoran sore', 'https://res.cloudinary.com/dkkbizenf/image/upload/v1779504211/tagihan_nono/jjpsoptmop6dcbhqmpgf.jpg', 17001700, '0001-01-01 00:00:00+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (79, '2026-05-23 02:51:22.456153+00', '2026-05-23 02:51:22.456153+00', NULL, '2026-05-23', 20000000, '', 'https://res.cloudinary.com/dkkbizenf/image/upload/v1779504673/tagihan_nono/dt8v7z7n4809l87m3ljv.jpg', 0, '0001-01-01 00:00:00+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (80, '2026-05-23 02:59:15.566834+00', '2026-05-23 02:59:45.795798+00', NULL, '2026-05-23', 6000000, 'BCA', 'https://res.cloudinary.com/dkkbizenf/image/upload/v1779505134/tagihan_nono/tq9gn0ou0zhmy9gdebrm.jpg', 0, '0001-01-01 00:00:00+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (81, '2026-05-23 03:00:06.411834+00', '2026-05-23 03:00:06.411834+00', NULL, '2026-05-23', 30000000, 'TF BRI', 'https://res.cloudinary.com/dkkbizenf/image/upload/v1779505199/tagihan_nono/owcvzg8pwtesyjactqps.jpg', 0, '0001-01-01 00:00:00+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (83, '2026-05-23 03:33:22.886156+00', '2026-05-23 03:33:22.886156+00', NULL, '2026-05-21', 0, 'setoran sore', 'https://res.cloudinary.com/dkkbizenf/image/upload/v1779507181/tagihan_nono/sfckbl3a2bicqyw4sovr.jpg', 19975000, '0001-01-01 00:00:00+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (84, '2026-05-23 03:44:51.919361+00', '2026-05-23 03:45:05.816757+00', NULL, '2026-05-20', 40000000, 'tunai', '', 0, '0001-01-01 00:00:00+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (85, '2026-05-23 03:45:41.336776+00', '2026-05-23 03:45:41.336776+00', NULL, '2026-05-21', 30000000, 'tunai', '', 0, '0001-01-01 00:00:00+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (86, '2026-05-23 06:23:38.024266+00', '2026-05-23 06:23:38.024266+00', NULL, '2026-05-23', 0, 'setoran sore', 'https://res.cloudinary.com/dkkbizenf/image/upload/v1779517390/tagihan_nono/ealfhbplapupfikxig8t.jpg', 12010500, '0001-01-01 00:00:00+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (82, '2026-05-23 03:07:42.305808+00', '2026-05-23 06:24:14.932682+00', NULL, '2026-05-20', 0, 'setoran sore', 'https://res.cloudinary.com/dkkbizenf/image/upload/v1779505471/tagihan_nono/smiiklwd2ayoeosoqjpt.jpg', 10612400, '0001-01-01 00:00:00+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (87, '2026-05-23 06:26:59.502984+00', '2026-05-23 06:29:33.589227+00', NULL, '2026-05-11', 0, 'setoran sore', 'https://res.cloudinary.com/dkkbizenf/image/upload/v1779517597/tagihan_nono/evb8cnxmwyqgk3p7otq5.jpg', 11878000, '0001-01-01 00:00:00+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (88, '2026-05-23 06:39:54.638748+00', '2026-05-23 06:39:54.638748+00', NULL, '2026-05-07', 0, 'setoran sore', 'https://res.cloudinary.com/dkkbizenf/image/upload/v1779518373/tagihan_nono/v2msqywhf3z0bvbli7js.jpg', 15912000, '0001-01-01 00:00:00+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (89, '2026-05-23 06:40:58.953179+00', '2026-05-23 06:40:58.953179+00', NULL, '2026-05-06', 0, 'setoran sore', 'https://res.cloudinary.com/dkkbizenf/image/upload/v1779518444/tagihan_nono/qzvyqonnkomkoi6qakjv.jpg', 17153000, '0001-01-01 00:00:00+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (90, '2026-05-23 06:41:59.732015+00', '2026-05-23 06:42:42.784182+00', NULL, '2026-05-04', 0, 'setoran sore', 'https://res.cloudinary.com/dkkbizenf/image/upload/v1779518500/tagihan_nono/dli6xqtfmllrg4eblqs1.jpg', 14906200, '0001-01-01 00:00:00+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (91, '2026-05-28 05:02:43.212357+00', '2026-05-28 05:02:43.212357+00', NULL, '2026-05-28', 30000000, 'setoran sore', 'https://res.cloudinary.com/dkkbizenf/image/upload/v1779944553/tagihan_nono/dkxeaaejapsuw1va4it9.jpg', 0, '0001-01-01 00:00:00+00', false);
INSERT INTO public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) VALUES (92, '2026-05-30 08:08:57.885313+00', '2026-05-30 08:08:57.885313+00', NULL, '2026-05-30', 0, 'setoran sore', 'https://res.cloudinary.com/dkkbizenf/image/upload/v1780128532/tagihan_nono/udh06a7dbjj3iruefyp4.jpg', 20689000, '0001-01-01 00:00:00+00', false);


--
-- Data for Name: cash_flows; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (353, '2026-04-30 04:36:21.666943+00', '2026-04-30 04:36:21.666943+00', NULL, '2026-04-28', 'KELUAR', 'jasa angkut - pak gito', 70000, NULL, '2026-04-30 04:36:21.658435+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (136, '2026-04-28 12:42:44.767511+00', '2026-04-28 12:42:44.767511+00', '2026-04-29 05:07:39.85175+00', '2026-02-17', 'KELUAR', 'Listrik PLN', 28246157, NULL, '2026-04-28 12:42:44.76163+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (354, '2026-04-30 04:36:51.113456+00', '2026-04-30 04:36:51.113456+00', NULL, '2026-04-28', 'KELUAR', 'uang saku - pak tono', 520000, NULL, '2026-04-30 04:36:51.105067+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (367, '2026-04-30 08:12:40.951648+00', '2026-04-30 08:12:40.951648+00', '2026-05-01 07:28:25.519674+00', '2026-02-17', 'KELUAR', 'bayar pln', 28246157, NULL, '2026-04-30 08:12:40.94418+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (357, '2026-04-30 04:42:01.63659+00', '2026-04-30 04:42:01.63659+00', '2026-05-01 07:28:34.720672+00', '2026-02-17', 'KELUAR', 'bayar pln', 28246157, NULL, '2026-04-30 04:42:01.627853+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (358, '2026-04-30 04:47:46.556709+00', '2026-04-30 04:47:46.556709+00', '2026-05-01 07:28:41.290574+00', '2026-02-17', 'KELUAR', 'bayar pln', 28246157, NULL, '2026-04-30 04:47:46.548363+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (368, '2026-04-30 11:07:10.626036+00', '2026-04-30 11:07:10.626036+00', '2026-05-01 07:28:57.060976+00', '2026-02-17', 'KELUAR', 'listrik pln', 2854187, NULL, '2026-04-30 11:07:10.616637+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (355, '2026-04-30 04:37:32.89133+00', '2026-04-30 04:37:32.89133+00', '2026-05-01 14:14:04.279032+00', '2026-04-27', 'KELUAR', 'meisn fingerprint, LAN, Router smartfren', 3000000, NULL, '2026-04-30 04:37:32.883054+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (373, '2026-05-01 17:05:32.50807+00', '2026-05-01 17:05:32.50807+00', '2026-05-01 17:05:45.17233+00', '2026-05-01', 'KELUAR', 'Pembayaran Gaji: ', 52500, NULL, '2026-05-01 17:05:32.50799+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (356, '2026-04-30 04:39:42.976008+00', '2026-04-30 04:39:42.976008+00', '2026-05-02 02:56:23.820404+00', '2026-02-17', 'MASUK', 'Bayar Listrik PLN ', 28246157, NULL, '2026-04-30 04:39:42.967616+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (377, '2026-05-02 03:20:21.140086+00', '2026-05-02 03:20:21.140086+00', '2026-05-02 03:20:52.654576+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: ', 315000, NULL, '2026-05-02 03:20:21.140068+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (378, '2026-05-02 03:22:38.472335+00', '2026-05-02 03:22:38.472335+00', '2026-05-02 03:23:07.842366+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: ', 360000, NULL, '2026-05-02 03:22:38.472317+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (381, '2026-05-02 03:22:39.751142+00', '2026-05-02 03:22:39.751142+00', '2026-05-02 03:23:11.033071+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: ', 300000, NULL, '2026-05-02 03:22:39.751118+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (380, '2026-05-02 03:22:39.443711+00', '2026-05-02 03:22:39.443711+00', '2026-05-02 03:23:13.839492+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: ', 315000, NULL, '2026-05-02 03:22:39.443694+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (379, '2026-05-02 03:22:38.980001+00', '2026-05-02 03:22:38.980001+00', '2026-05-02 03:23:16.761443+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: ', 345000, NULL, '2026-05-02 03:22:38.979982+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (393, '2026-05-02 03:49:08.929675+00', '2026-05-02 03:49:08.929675+00', '2026-05-02 03:52:28.998199+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: ', 157500, NULL, '2026-05-02 03:49:08.929647+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (392, '2026-05-02 03:49:08.445257+00', '2026-05-02 03:49:08.445257+00', '2026-05-02 03:52:31.588777+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: ', 262500, NULL, '2026-05-02 03:49:08.44524+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (391, '2026-05-02 03:49:07.955238+00', '2026-05-02 03:49:07.955238+00', '2026-05-02 03:52:34.014426+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: ', 405000, NULL, '2026-05-02 03:49:07.955217+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (390, '2026-05-02 03:49:07.44102+00', '2026-05-02 03:49:07.44102+00', '2026-05-02 03:52:36.288573+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: ', 450000, NULL, '2026-05-02 03:49:07.441007+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (410, '2026-05-02 05:39:17.615844+00', '2026-05-02 05:39:17.615844+00', '2026-05-02 05:51:59.534706+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: ', 157500, NULL, '2026-05-02 05:39:17.615831+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (409, '2026-05-02 05:39:17.151886+00', '2026-05-02 05:39:17.151886+00', '2026-05-02 05:52:01.939936+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: ', 210000, NULL, '2026-05-02 05:39:17.151873+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (408, '2026-05-02 05:39:16.664397+00', '2026-05-02 05:39:16.664397+00', '2026-05-02 05:52:04.382307+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: ', 220000, NULL, '2026-05-02 05:39:16.664381+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (407, '2026-05-02 05:39:16.190362+00', '2026-05-02 05:39:16.190362+00', '2026-05-02 05:52:06.569318+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: ', 300000, NULL, '2026-05-02 05:39:16.190343+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (412, '2026-05-02 05:58:17.595467+00', '2026-05-02 05:58:17.595467+00', '2026-05-02 05:59:02.57075+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: 60k', 315000, NULL, '2026-05-02 05:58:17.595448+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (416, '2026-05-02 05:58:18.774232+00', '2026-05-02 05:58:18.774232+00', '2026-05-02 05:59:02.57075+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: 60k', 315000, NULL, '2026-05-02 05:58:18.774215+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (415, '2026-05-02 05:58:18.533691+00', '2026-05-02 05:58:18.533691+00', '2026-05-02 05:59:05.894841+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: ', 345000, NULL, '2026-05-02 05:58:18.533677+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (411, '2026-05-02 05:58:17.294949+00', '2026-05-02 05:58:17.294949+00', '2026-05-02 05:59:08.447762+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: bonus 30k', 450000, NULL, '2026-05-02 05:58:17.294931+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (414, '2026-05-02 05:58:18.476416+00', '2026-05-02 05:58:18.476416+00', '2026-05-02 05:59:08.447762+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: bonus 30k', 450000, NULL, '2026-05-02 05:58:18.476395+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (413, '2026-05-02 05:58:18.067093+00', '2026-05-02 05:58:18.067093+00', '2026-05-02 05:59:11.244673+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: ', 300000, NULL, '2026-05-02 05:58:18.067042+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (420, '2026-05-02 06:06:42.837651+00', '2026-05-02 06:06:42.837651+00', '2026-05-02 06:09:08.345018+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: bonus : 42k extra : 25k tgl merah 15k', 345000, NULL, '2026-05-02 06:06:42.837635+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (419, '2026-05-02 06:06:42.355718+00', '2026-05-02 06:06:42.355718+00', '2026-05-02 06:09:11.187099+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: bonus : 30K extra 40k tgl merah 15k', 315000, NULL, '2026-05-02 06:06:42.3557+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (418, '2026-05-02 06:06:42.074741+00', '2026-05-02 06:06:42.074741+00', '2026-05-02 06:09:13.896457+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: bonus : 30k ekstra :20k tgl merah + 15k', 285000, NULL, '2026-05-02 06:06:42.074724+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (417, '2026-05-02 06:06:41.566504+00', '2026-05-02 06:06:41.566504+00', '2026-05-02 06:09:16.478816+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: bonus: 35k ekstra 20k tgl merah :+15k', 345000, NULL, '2026-05-02 06:06:41.566486+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (431, '2026-05-05 09:50:09.640731+00', '2026-05-05 09:50:09.640731+00', NULL, '2026-05-05', 'MASUK', 'Pembayaran Faktur BMP-2605-002 (mas wiranto)', 10000000, 7, '2026-05-05 09:50:09.640706+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (439, '2026-05-13 13:28:55.266397+00', '2026-05-13 13:28:55.266397+00', NULL, '2026-05-13', 'MASUK', 'Pembayaran Faktur BMP-0426-007 (abah ali)', 53000000, 12, '2026-05-13 13:28:55.266374+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (440, '2026-05-13 14:35:58.286544+00', '2026-05-13 14:35:58.286544+00', NULL, '2026-05-12', 'KELUAR', 'kapasitor Ducati 10 kvar, 2 pcs', 1786000, NULL, '2026-05-13 14:35:58.277037+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (445, '2026-05-15 04:10:44.35114+00', '2026-05-15 04:10:44.35114+00', NULL, '2026-05-15', 'MASUK', 'Pembayaran Faktur BMP-0426-010 (mas wiranto)', 14835000, 15, '2026-05-15 04:10:44.351125+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (450, '2026-05-22 07:03:23.250664+00', '2026-05-22 07:03:23.250664+00', NULL, '2026-05-21', 'MASUK', 'Pembayaran Faktur BMP-0426-001 (abah ali)', 31700000, 18, '2026-05-22 07:03:23.250641+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (451, '2026-05-22 07:07:14.592566+00', '2026-05-22 07:07:14.592566+00', NULL, '2026-05-15', 'KELUAR', 'Ongkir ', 1100000, NULL, '2026-05-22 07:07:14.58445+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (452, '2026-05-22 07:07:48.639028+00', '2026-05-22 07:07:48.639028+00', NULL, '2026-05-19', 'KELUAR', 'Ongkir - Ko Hary', 800000, NULL, '2026-05-22 07:07:48.631034+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (453, '2026-05-22 07:10:45.412352+00', '2026-05-22 07:10:45.412352+00', NULL, '2026-05-15', 'KELUAR', 'Jasa Angkut - Pak Sandi', 250000, NULL, '2026-05-22 07:10:45.40155+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (454, '2026-05-22 07:11:10.809016+00', '2026-05-22 07:11:10.809016+00', NULL, '2026-05-19', 'KELUAR', 'Jasa Angkut - Ko Hary', 250000, NULL, '2026-05-22 07:11:10.800864+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (455, '2026-05-22 07:11:52.598489+00', '2026-05-22 07:11:52.598489+00', NULL, '2026-05-21', 'KELUAR', 'Jasa Angkut - Ko Hary', 300000, NULL, '2026-05-22 07:11:52.590083+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (456, '2026-05-22 07:12:28.280271+00', '2026-05-22 07:12:28.280271+00', NULL, '2026-05-21', 'KELUAR', 'Ongkir - Abah Kosi''in Grobogan', 1620000, NULL, '2026-05-22 07:12:28.272064+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (448, '2026-05-22 04:31:06.995227+00', '2026-05-22 04:31:06.995227+00', '2026-05-26 03:21:01.024392+00', '2026-05-22', 'KELUAR', 'Pembelian barang khusus untuk Faktur BMP-2605-015', 5100, NULL, '2026-05-22 04:31:06.995211+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (449, '2026-05-22 04:32:37.182488+00', '2026-05-22 04:32:37.182488+00', '2026-05-26 03:21:10.975511+00', '2026-05-22', 'KELUAR', 'Pembelian barang khusus (Update) untuk Faktur BMP-2605-015', 5100, NULL, '2026-05-22 04:32:37.18247+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (461, '2026-05-26 03:35:50.809609+00', '2026-05-26 03:35:50.809609+00', NULL, '2026-05-23', 'MASUK', 'Pembayaran Faktur BMP-2605-013 (Mas Malvin)', 20000000, 23, '2026-05-26 03:35:50.809584+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (462, '2026-05-26 03:39:01.90627+00', '2026-05-26 03:39:01.90627+00', NULL, '2026-05-26', 'MASUK', 'Pembayaran Faktur BMP-2605-013 (Mas Malvin)', 13630000, 24, '2026-05-26 03:39:01.906243+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (465, '2026-05-26 19:12:52.561526+00', '2026-05-26 19:12:52.561526+00', '2026-05-26 19:13:30.02708+00', '2026-05-26', 'KELUAR', 'Pembayaran Gaji: Denda dihapus (manual)', 83300, NULL, '2026-05-26 19:12:52.561511+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (466, '2026-05-26 19:17:25.033116+00', '2026-05-26 19:17:25.033116+00', '2026-05-26 19:17:48.333027+00', '2026-05-26', 'KELUAR', 'Pembayaran Gaji: Denda dihapus (manual)', 83300, NULL, '2026-05-26 19:17:25.032841+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (359, '2026-04-30 05:11:17.131836+00', '2026-04-30 05:11:17.131836+00', '2026-05-01 07:28:05.694723+00', '2026-02-17', 'KELUAR', 'Bayar Listrik', 28246157, NULL, '2026-04-30 05:11:17.121149+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (369, '2026-05-01 14:10:11.402405+00', '2026-05-01 14:10:11.402405+00', NULL, '2026-04-30', 'KELUAR', 'ongkos kirim - pak tono', 1250000, NULL, '2026-05-01 14:10:11.393913+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (370, '2026-05-01 14:10:54.5959+00', '2026-05-01 14:10:54.5959+00', NULL, '2026-05-01', 'KELUAR', 'jasa angkutan mas arip', 20000, NULL, '2026-05-01 14:10:54.587724+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (371, '2026-05-01 14:13:30.873972+00', '2026-05-01 14:13:30.873972+00', NULL, '2026-04-17', 'KELUAR', 'Listrik PLN kWh 24364.0', 26534828, NULL, '2026-05-01 14:13:30.865684+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (374, '2026-05-02 02:19:15.098052+00', '2026-05-02 02:19:15.098052+00', '2026-05-02 02:19:41.754607+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: ', 405000, NULL, '2026-05-02 02:19:15.098027+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (385, '2026-05-02 03:32:21.116242+00', '2026-05-02 03:32:21.116242+00', '2026-05-02 03:32:44.881106+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: ', 300000, NULL, '2026-05-02 03:32:21.116224+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (384, '2026-05-02 03:32:20.684085+00', '2026-05-02 03:32:20.684085+00', '2026-05-02 03:33:31.155747+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: ', 315000, NULL, '2026-05-02 03:32:20.68407+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (383, '2026-05-02 03:32:20.348332+00', '2026-05-02 03:32:20.348332+00', '2026-05-02 03:33:35.187016+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: ', 345000, NULL, '2026-05-02 03:32:20.34831+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (382, '2026-05-02 03:32:19.780432+00', '2026-05-02 03:32:19.780432+00', '2026-05-02 03:33:37.891398+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: ', 360000, NULL, '2026-05-02 03:32:19.780412+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (396, '2026-05-02 04:49:05.88532+00', '2026-05-02 04:49:05.88532+00', '2026-05-02 04:50:16.622025+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: ', 315000, NULL, '2026-05-02 04:49:05.885305+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (397, '2026-05-02 04:49:06.372954+00', '2026-05-02 04:49:06.372954+00', '2026-05-02 04:50:16.622025+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: ', 315000, NULL, '2026-05-02 04:49:06.372934+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (394, '2026-05-02 04:49:04.885493+00', '2026-05-02 04:49:04.885493+00', '2026-05-02 04:50:25.422552+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: ', 450000, NULL, '2026-05-02 04:49:04.885475+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (395, '2026-05-02 04:49:05.262362+00', '2026-05-02 04:49:05.262362+00', '2026-05-02 04:50:25.422552+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: ', 450000, NULL, '2026-05-02 04:49:05.262348+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (401, '2026-05-02 04:51:21.542972+00', '2026-05-02 04:51:21.542972+00', '2026-05-02 05:07:13.718479+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: ', 105000, NULL, '2026-05-02 04:51:21.542956+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (398, '2026-05-02 04:51:19.875036+00', '2026-05-02 04:51:19.875036+00', '2026-05-02 05:07:19.368884+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: ', 157500, NULL, '2026-05-02 04:51:19.875017+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (399, '2026-05-02 04:51:20.366136+00', '2026-05-02 04:51:20.366136+00', '2026-05-02 05:07:19.368884+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: ', 157500, NULL, '2026-05-02 04:51:20.366118+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (400, '2026-05-02 04:51:20.911116+00', '2026-05-02 04:51:20.911116+00', '2026-05-02 05:07:19.368884+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: ', 157500, NULL, '2026-05-02 04:51:20.911101+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (424, '2026-05-02 06:13:04.400215+00', '2026-05-02 06:13:04.400215+00', '2026-05-02 06:15:23.915549+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: bonus : 30k extra : 20k tgl merah : 15k', 237500, NULL, '2026-05-02 06:13:04.4002+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (423, '2026-05-02 06:13:04.110793+00', '2026-05-02 06:13:04.110793+00', '2026-05-02 06:15:26.75739+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: bonus : 30k extra : 40k tgl merah : 15k', 315000, NULL, '2026-05-02 06:13:04.110777+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (422, '2026-05-02 06:13:03.423802+00', '2026-05-02 06:13:03.423802+00', '2026-05-02 06:15:29.53616+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: bonus : 42k extra : 25k tgl merah : 15k', 345000, NULL, '2026-05-02 06:13:03.423782+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (421, '2026-05-02 06:13:02.924825+00', '2026-05-02 06:13:02.924825+00', '2026-05-02 06:15:32.10033+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: bonus : 42k extra : 100k tgl merah : 15k', 450000, NULL, '2026-05-02 06:13:02.924216+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (432, '2026-05-05 13:42:46.4128+00', '2026-05-05 13:42:46.4128+00', NULL, '2026-04-28', 'MASUK', 'Pembayaran Faktur BMP-0426-003 (abah aan)', 20320000, 8, '2026-05-05 13:42:46.412783+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (245, '2026-04-29 08:24:03.137505+00', '2026-04-29 08:24:03.137505+00', NULL, '2026-04-28', 'MASUK', 'Pembayaran Borongan Faktur BMP-0426-048 (abah kosi''in)', 40790000, NULL, '2026-04-29 08:24:02.846893+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (247, '2026-04-29 08:24:06.123219+00', '2026-04-29 08:24:06.123219+00', NULL, '2026-04-27', 'KELUAR', 'mesin fingerprint, LAN, Router SmartFren', 3000000, NULL, '2026-04-29 08:24:05.512113+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (248, '2026-04-29 08:24:07.355741+00', '2026-04-29 08:24:07.355741+00', NULL, '2026-04-27', 'MASUK', 'Pembayaran Faktur BMP-0426-002 (abah ali)', 33200000, NULL, '2026-04-29 08:24:06.737607+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (249, '2026-04-29 08:24:08.639626+00', '2026-04-29 08:24:08.639626+00', NULL, '2026-04-27', 'MASUK', 'Pembayaran Faktur BMP-0426-054 (Mas Malvin)', 20000000, NULL, '2026-04-29 08:24:08.068704+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (250, '2026-04-29 08:24:10.01433+00', '2026-04-29 08:24:10.01433+00', NULL, '2026-04-25', 'KELUAR', 'jasa angkutan - mas adi', 70000, NULL, '2026-04-29 08:24:09.40001+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (251, '2026-04-29 08:24:11.447851+00', '2026-04-29 08:24:11.447851+00', NULL, '2026-04-24', 'KELUAR', 'jasa angkut - mas adit', 50000, NULL, '2026-04-29 08:24:10.654281+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (252, '2026-04-29 08:24:12.791446+00', '2026-04-29 08:24:12.791446+00', NULL, '2026-04-24', 'MASUK', 'Pembayaran Borongan Faktur BMP-0426-010 (mas wiranto)', 200000, NULL, '2026-04-29 08:24:12.062317+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (253, '2026-04-29 08:24:14.315194+00', '2026-04-29 08:24:14.315194+00', NULL, '2026-04-24', 'MASUK', 'Pembayaran Borongan Faktur BMP-0426-006 (mas wiranto)', 9655000, NULL, '2026-04-29 08:24:13.598292+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (254, '2026-04-29 08:24:15.749357+00', '2026-04-29 08:24:15.749357+00', NULL, '2026-04-24', 'MASUK', 'Pembayaran Borongan Faktur BMP-0426-005 (mas wiranto)', 6395000, NULL, '2026-04-29 08:24:14.938722+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (255, '2026-04-29 08:24:17.090663+00', '2026-04-29 08:24:17.090663+00', NULL, '2026-04-24', 'MASUK', 'Pelunasan Faktur BMP-0426-052 (mas wiranto)', 6150000, NULL, '2026-04-29 08:24:16.363171+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (256, '2026-04-29 08:24:18.513593+00', '2026-04-29 08:24:18.513593+00', NULL, '2026-04-23', 'MASUK', 'Pembayaran Borongan Faktur BMP-0426-048 (abah kosi''in)', 30000000, NULL, '2026-04-29 08:24:17.902347+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (257, '2026-04-29 08:24:19.844855+00', '2026-04-29 08:24:19.844855+00', NULL, '2026-04-22', 'KELUAR', 'jasa angkut - mas adhi', 50000, NULL, '2026-04-29 08:24:19.221935+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (258, '2026-04-29 08:24:21.176563+00', '2026-04-29 08:24:21.176563+00', NULL, '2026-04-22', 'KELUAR', 'ongkos kirim uang - Pak Tono', 200000, NULL, '2026-04-29 08:24:20.414314+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (259, '2026-04-29 08:24:22.568264+00', '2026-04-29 08:24:22.568264+00', NULL, '2026-04-21', 'KELUAR', 'jasa pengiriman pak tono - Grobogan Pak Kosi''in', 1630000, NULL, '2026-04-29 08:24:21.79063+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (260, '2026-04-29 08:24:23.941029+00', '2026-04-29 08:24:23.941029+00', NULL, '2026-04-21', 'KELUAR', 'jasa angkut - mas adhi', 75000, NULL, '2026-04-29 08:24:23.326476+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (261, '2026-04-29 08:24:25.272112+00', '2026-04-29 08:24:25.272112+00', NULL, '2026-04-21', 'KELUAR', 'uang mesin neng tin', 3400000, NULL, '2026-04-29 08:24:24.555363+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (262, '2026-04-29 08:24:26.500968+00', '2026-04-29 08:24:26.500968+00', NULL, '2026-04-21', 'KELUAR', 'jasa angkut - pak jito & pak sul (ALI)', 300000, NULL, '2026-04-29 08:24:25.886336+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (263, '2026-04-29 08:24:27.729695+00', '2026-04-29 08:24:27.729695+00', NULL, '2026-04-20', 'MASUK', 'Pelunasan Faktur BMP-0426-039 (Mas Arylah)', 8731000, NULL, '2026-04-29 08:24:27.066769+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (264, '2026-04-29 08:24:28.922738+00', '2026-04-29 08:24:28.922738+00', NULL, '2026-04-20', 'MASUK', 'Pelunasan Faktur BMP-0426-040 (mas wiranto)', 4860000, NULL, '2026-04-29 08:24:28.344095+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (265, '2026-04-29 08:24:30.39219+00', '2026-04-29 08:24:30.39219+00', NULL, '2026-04-20', 'MASUK', 'Pelunasan Faktur BMP-0426-041 (Mas Eko Cahyono)', 28755000, NULL, '2026-04-29 08:24:29.777688+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (266, '2026-04-29 08:24:32.435265+00', '2026-04-29 08:24:32.435265+00', NULL, '2026-04-20', 'MASUK', 'Pelunasan Faktur BMP-0426-042 (Linda Abadi)', 28000000, NULL, '2026-04-29 08:24:31.411732+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (267, '2026-04-29 08:24:33.87389+00', '2026-04-29 08:24:33.87389+00', NULL, '2026-04-20', 'MASUK', 'Pelunasan Faktur BMP-0426-026 (mas kolis)', 2825000, NULL, '2026-04-29 08:24:33.25944+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (268, '2026-04-29 08:24:35.206697+00', '2026-04-29 08:24:35.206697+00', NULL, '2026-04-20', 'MASUK', 'Pelunasan Faktur BMP-0426-044 (Pak Huda)', 22160000, NULL, '2026-04-29 08:24:34.543653+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (269, '2026-04-29 08:24:36.610803+00', '2026-04-29 08:24:36.610803+00', NULL, '2026-04-20', 'MASUK', 'Pelunasan Faktur BMP-0426-043 (Umik Erna)', 12000000, NULL, '2026-04-29 08:24:35.922209+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (270, '2026-04-29 08:24:38.072251+00', '2026-04-29 08:24:38.072251+00', NULL, '2026-04-20', 'KELUAR', 'jasa angkut - Pak Katiran', 70000, NULL, '2026-04-29 08:24:37.457829+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (271, '2026-04-29 08:24:39.6082+00', '2026-04-29 08:24:39.6082+00', NULL, '2026-04-20', 'MASUK', 'Pelunasan Faktur BMP-0426-047 (pak katiran)', 13805000, NULL, '2026-04-29 08:24:38.764426+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (434, '2026-05-05 13:45:28.715909+00', '2026-05-05 13:45:28.715909+00', NULL, '2026-05-05', 'MASUK', 'Pembayaran Faktur BMP-0426-024 (mas zahid)', 10700000, 10, '2026-05-05 13:45:28.715898+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (435, '2026-05-05 13:47:32.660696+00', '2026-05-05 13:47:32.660696+00', '2026-05-05 15:50:02.357748+00', '2026-05-05', 'MASUK', 'Pembayaran Faktur BMP-2605-003 (Mas Malvin)', 10700000, 11, '2026-05-05 13:47:32.66067+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (246, '2026-04-29 08:24:04.894226+00', '2026-04-29 08:24:04.894226+00', '2026-05-05 17:24:54.502465+00', '2026-04-28', 'MASUK', 'Pembayaran Faktur BMP-0426-003 (abah aan)', 20320000, NULL, '2026-04-29 08:24:04.17785+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (441, '2026-05-14 01:44:22.68616+00', '2026-05-14 01:44:22.68616+00', '2026-05-14 01:44:57.200134+00', '2026-05-14', 'KELUAR', 'Pembelian barang khusus (Update) untuk Faktur BMP-2605-005', 2600, NULL, '2026-05-14 01:44:22.686146+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (442, '2026-05-14 01:52:21.031201+00', '2026-05-14 01:52:21.031201+00', NULL, '2026-05-14', 'KELUAR', 'Pembelian barang khusus untuk Faktur BMP-2605-008', 6760000, NULL, '2026-05-14 01:52:21.031184+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (458, '2026-05-23 04:10:40.138872+00', '2026-05-23 04:10:40.138872+00', '2026-05-26 01:57:24.632659+00', '2026-05-23', 'MASUK', 'Pembayaran Faktur BMP-2605-013 (Mas Malvin)', 30000000, 20, '2026-05-23 04:10:40.138862+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (443, '2026-05-14 01:52:55.590342+00', '2026-05-14 01:52:55.590342+00', NULL, '2026-05-01', 'MASUK', 'Pembayaran Faktur BMP-2605-008 (mas wiranto)', 10000000, 13, '2026-05-14 01:52:55.590328+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (446, '2026-05-15 08:29:57.43274+00', '2026-05-15 08:29:57.43274+00', NULL, '2026-05-15', 'MASUK', 'Pembayaran Faktur BMP-2605-010 (abah kosi''in)', 80393000, 16, '2026-05-15 08:29:57.432722+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (457, '2026-05-23 04:08:40.597845+00', '2026-05-23 04:08:40.597845+00', NULL, '2026-05-19', 'MASUK', 'Pembayaran Faktur BMP-2605-013 (Mas Malvin)', 14000000, 19, '2026-05-23 04:08:40.597829+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (463, '2026-05-26 05:34:42.71438+00', '2026-05-26 05:34:42.71438+00', '2026-05-26 05:35:20.639452+00', '2026-05-26', 'KELUAR', 'Pembelian barang khusus (Update) untuk Faktur BMP-2605-010', 3700, NULL, '2026-05-26 05:34:42.714354+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (467, '2026-05-27 07:16:23.425845+00', '2026-05-27 07:17:27.667614+00', NULL, '2026-05-27', 'KELUAR', 'Pembelian barang khusus untuk Faktur BMP-2605-090', 2000000, NULL, '2026-05-27 07:16:23.425816+00', NULL, NULL, NULL, NULL, true);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (468, '2026-05-27 07:17:54.407723+00', '2026-05-27 07:17:54.407723+00', NULL, '2026-05-27', 'MASUK', 'Pembayaran Faktur BMP-2605-090 ([DEMO] PT Plastik Nusantara)', 2400000, 26, '2026-05-27 07:17:54.407691+00', NULL, NULL, NULL, NULL, true);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (272, '2026-04-29 08:24:40.922881+00', '2026-04-29 08:24:40.922881+00', NULL, '2026-04-20', 'MASUK', 'Pembayaran Faktur BMP-0426-025 (pak katiran)', 17385000, NULL, '2026-04-29 08:24:40.325491+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (273, '2026-04-29 08:24:42.292558+00', '2026-04-29 08:24:42.292558+00', NULL, '2026-04-20', 'KELUAR', 'jasa angkut - mas adhi', 50000, NULL, '2026-04-29 08:24:41.769345+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (274, '2026-04-29 08:24:43.499638+00', '2026-04-29 08:24:43.499638+00', NULL, '2026-04-20', 'KELUAR', 'Gaji Mas Dedi', 5000000, NULL, '2026-04-29 08:24:42.885128+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (275, '2026-04-29 08:24:44.830704+00', '2026-04-29 08:24:44.830704+00', NULL, '2026-04-20', 'KELUAR', 'ongkir Pak Katiran', 520000, NULL, '2026-04-29 08:24:44.173651+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (276, '2026-04-29 08:24:46.33061+00', '2026-04-29 08:24:46.33061+00', NULL, '2026-04-20', 'MASUK', 'Pelunasan Faktur BMP-0426-029 (mas zahid)', 6015000, NULL, '2026-04-29 08:24:45.64969+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (277, '2026-04-29 08:24:47.666683+00', '2026-04-29 08:24:47.666683+00', NULL, '2026-04-20', 'MASUK', 'Pelunasan Faktur BMP-0426-028 (mas kolis)', 780000, NULL, '2026-04-29 08:24:47.083511+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (278, '2026-04-29 08:24:49.1315+00', '2026-04-29 08:24:49.1315+00', NULL, '2026-04-20', 'MASUK', 'Pelunasan Faktur BMP-0426-046 (Mas Malvin)', 44575000, NULL, '2026-04-29 08:24:48.312393+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (279, '2026-04-29 08:24:50.546935+00', '2026-04-29 08:24:50.546935+00', NULL, '2026-04-20', 'MASUK', 'Pelunasan Faktur BMP-0426-027 (mas kolis)', 6850000, NULL, '2026-04-29 08:24:49.848499+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (280, '2026-04-29 08:24:51.998864+00', '2026-04-29 08:24:51.998864+00', NULL, '2026-04-20', 'MASUK', 'Pelunasan Faktur BMP-0426-045 (abah kosi''in)', 51248000, NULL, '2026-04-29 08:24:51.282017+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (281, '2026-04-29 08:24:53.433293+00', '2026-04-29 08:24:53.433293+00', NULL, '2026-04-20', 'MASUK', 'Pembayaran Faktur BMP-0426-023 (mas kolis)', 30000000, NULL, '2026-04-29 08:24:52.694449+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (282, '2026-04-29 08:24:54.858602+00', '2026-04-29 08:24:54.858602+00', NULL, '2026-04-20', 'MASUK', 'Pembayaran Faktur BMP-0426-009 (mas zahid)', 13582500, NULL, '2026-04-29 08:24:54.149305+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (283, '2026-04-29 08:24:56.299575+00', '2026-04-29 08:24:56.299575+00', NULL, '2026-04-20', 'MASUK', 'Pembayaran Faktur BMP-0426-034 (mas wiranto)', 16040000, NULL, '2026-04-29 08:24:55.685227+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (284, '2026-04-29 08:24:57.733298+00', '2026-04-29 08:24:57.733298+00', NULL, '2026-04-20', 'MASUK', 'Pelunasan Faktur BMP-0426-030 (mas kolis)', 9882500, NULL, '2026-04-29 08:24:57.002621+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (285, '2026-04-29 08:24:59.122535+00', '2026-04-29 08:24:59.122535+00', NULL, '2026-04-20', 'MASUK', 'Pelunasan Faktur BMP-0426-031 (mas kolis)', 15337500, NULL, '2026-04-29 08:24:58.450942+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (286, '2026-04-29 08:25:00.600667+00', '2026-04-29 08:25:00.600667+00', NULL, '2026-04-20', 'MASUK', 'Pelunasan Faktur BMP-0426-032 (mas kolis)', 10325000, NULL, '2026-04-29 08:24:59.884132+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (287, '2026-04-29 08:25:01.93196+00', '2026-04-29 08:25:01.93196+00', NULL, '2026-04-20', 'MASUK', 'Pelunasan Faktur BMP-0426-033 (mas kolis)', 15337500, NULL, '2026-04-29 08:25:01.27877+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (288, '2026-04-29 08:25:03.263629+00', '2026-04-29 08:25:03.263629+00', NULL, '2026-04-20', 'MASUK', 'Pelunasan Faktur BMP-0426-022 (mas wiranto)', 290000, NULL, '2026-04-29 08:25:02.545966+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (289, '2026-04-29 08:25:04.54269+00', '2026-04-29 08:25:04.54269+00', NULL, '2026-04-20', 'MASUK', 'Pelunasan Faktur BMP-0426-035 (mas wiranto)', 5984000, NULL, '2026-04-29 08:25:03.877319+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (290, '2026-04-29 08:25:05.925267+00', '2026-04-29 08:25:05.925267+00', NULL, '2026-04-20', 'MASUK', 'Pelunasan Faktur BMP-0426-036 (mas wiranto)', 10211500, NULL, '2026-04-29 08:25:05.208614+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (291, '2026-04-29 08:25:07.359194+00', '2026-04-29 08:25:07.359194+00', NULL, '2026-04-20', 'MASUK', 'Pelunasan Faktur BMP-0426-037 (mas wiranto)', 9182000, NULL, '2026-04-29 08:25:06.637681+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (292, '2026-04-29 08:25:08.690342+00', '2026-04-29 08:25:08.690342+00', NULL, '2026-04-20', 'MASUK', 'Pelunasan Faktur BMP-0426-038 (mas wiranto)', 2940000, NULL, '2026-04-29 08:25:08.075746+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (293, '2026-04-29 08:25:09.882573+00', '2026-04-29 08:25:09.882573+00', NULL, '2026-04-19', 'KELUAR', 'uang mesin ibu', 5000000, NULL, '2026-04-29 08:25:09.30466+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (294, '2026-04-29 08:25:11.352512+00', '2026-04-29 08:25:11.352512+00', NULL, '2026-04-17', 'KELUAR', 'beli inventaris kantor', 8075000, NULL, '2026-04-29 08:25:10.738093+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (295, '2026-04-29 08:25:12.888558+00', '2026-04-29 08:25:12.888558+00', NULL, '2026-04-17', 'KELUAR', 'beli Kartu Smartfren', 70000, NULL, '2026-04-29 08:25:12.042669+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (296, '2026-04-29 08:25:14.19551+00', '2026-04-29 08:25:14.19551+00', NULL, '2026-04-17', 'KELUAR', 'jasa angkut - mas adi', 50000, NULL, '2026-04-29 08:25:13.502978+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (297, '2026-04-29 08:25:15.653522+00', '2026-04-29 08:25:15.653522+00', NULL, '2026-04-17', 'KELUAR', 'beli website', 2000000, NULL, '2026-04-29 08:25:15.039189+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (298, '2026-04-29 08:25:16.782646+00', '2026-04-29 08:25:16.782646+00', NULL, '2026-04-17', 'MASUK', 'Pelunasan Faktur BMP-0426-017 (mas kolis)', 14500000, NULL, '2026-04-29 08:25:16.22156+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (299, '2026-04-29 08:25:18.008727+00', '2026-04-29 08:25:18.008727+00', NULL, '2026-04-17', 'MASUK', 'Pelunasan Faktur BMP-0426-012 (mas kolis)', 4125000, NULL, '2026-04-29 08:25:17.389584+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (300, '2026-04-29 08:25:19.241969+00', '2026-04-29 08:25:19.241969+00', NULL, '2026-04-17', 'MASUK', 'Pelunasan Faktur BMP-0426-018 (abah ali)', 16520000, NULL, '2026-04-29 08:25:18.584717+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (301, '2026-04-29 08:25:20.622587+00', '2026-04-29 08:25:20.622587+00', NULL, '2026-04-17', 'MASUK', 'Pelunasan Faktur BMP-0426-019 (abah ali)', 30600000, NULL, '2026-04-29 08:25:19.954693+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (302, '2026-04-29 08:25:22.002301+00', '2026-04-29 08:25:22.002301+00', NULL, '2026-04-17', 'MASUK', 'Pelunasan Faktur BMP-0426-020 (abah ali)', 6560000, NULL, '2026-04-29 08:25:21.285486+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (303, '2026-04-29 08:25:23.538643+00', '2026-04-29 08:25:23.538643+00', NULL, '2026-04-17', 'MASUK', 'Pelunasan Faktur BMP-0426-021 (mas kolis)', 13200000, NULL, '2026-04-29 08:25:22.719241+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (304, '2026-04-29 08:25:24.887428+00', '2026-04-29 08:25:24.887428+00', NULL, '2026-04-17', 'MASUK', 'Pelunasan Faktur BMP-0426-022 (mas wiranto)', 12400000, NULL, '2026-04-29 08:25:24.255209+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (305, '2026-04-29 08:25:26.405562+00', '2026-04-29 08:25:26.405562+00', NULL, '2026-04-17', 'KELUAR', '[FAKTUR BELI] Bengkel Sahabat - Kusmiantoro | Pembuatan Matras Baskom Rotan 14 (1,0Lsn x 1,0Qty), Pembuatan Matras Baskom Bahtera (1,0Lsn x 1,0Qty)', 61000000, NULL, '2026-04-29 08:25:25.688716+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (306, '2026-04-29 08:25:27.839497+00', '2026-04-29 08:25:27.839497+00', NULL, '2026-04-17', 'KELUAR', '[FAKTUR BELI] Bengkel Sahabat - Kusmiantoro | Krom Matras 2X (1,0Lsn x 1,0Qty)', 4250000, NULL, '2026-04-29 08:25:27.024069+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (307, '2026-04-29 08:25:29.222587+00', '2026-04-29 08:25:29.222587+00', NULL, '2026-04-17', 'KELUAR', '[FAKTUR BELI] Bengkel Sahabat - Kusmiantoro | servis Matras Bahtera (1,0Lsn x 1,0Qty)', 2500000, NULL, '2026-04-29 08:25:28.555987+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (308, '2026-04-29 08:25:30.604013+00', '2026-04-29 08:25:30.604013+00', NULL, '2026-04-17', 'KELUAR', '[FAKTUR BELI] Bengkel Sahabat - Kusmiantoro | Servis Matras & Krom (1,0Lsn x 1,0Qty)', 5400000, NULL, '2026-04-29 08:25:29.989994+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (309, '2026-04-29 08:25:32.140084+00', '2026-04-29 08:25:32.140084+00', NULL, '2026-04-17', 'KELUAR', '[FAKTUR BELI] Bengkel Sahabat - Kusmiantoro | servis Matras Baskom Panda (1,0Lsn x 1,0Qty)', 2500000, NULL, '2026-04-29 08:25:31.338648+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (310, '2026-04-29 08:25:33.490635+00', '2026-04-29 08:25:33.490635+00', NULL, '2026-04-17', 'KELUAR', '[FAKTUR BELI] Bengkel Sahabat - Kusmiantoro | Servis Matras (1,0Lsn x 1,0Qty)', 2500000, NULL, '2026-04-29 08:25:32.755697+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (311, '2026-04-29 08:25:34.802263+00', '2026-04-29 08:25:34.802263+00', NULL, '2026-04-17', 'KELUAR', '[FAKTUR BELI] Bengkel Sahabat - Kusmiantoro | krom Matras 2X (1,0Lsn x 1,0Qty)', 3000000, NULL, '2026-04-29 08:25:34.188346+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (312, '2026-04-29 08:25:36.236039+00', '2026-04-29 08:25:36.236039+00', NULL, '2026-04-17', 'KELUAR', '[FAKTUR BELI] Pak Hadi CNC | CNC Ulang Baskom Mawar (1,0Lsn x 1,0Qty)', 8000000, NULL, '2026-04-29 08:25:35.519529+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (313, '2026-04-29 08:25:37.669566+00', '2026-04-29 08:25:37.669566+00', NULL, '2026-04-17', 'KELUAR', '[FAKTUR BELI] Hwamda | Angsuran Mesin Hwamda (1,0Lsn x 1,0Qty)', 20000000, NULL, '2026-04-29 08:25:36.952806+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (314, '2026-04-29 08:25:39.103242+00', '2026-04-29 08:25:39.103242+00', NULL, '2026-04-17', 'KELUAR', '[FAKTUR BELI] Hwamda | Angsuran Mesin Hwamda (1,0Lsn x 1,0Qty)', 20000000, NULL, '2026-04-29 08:25:38.488937+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (315, '2026-04-29 08:25:40.639424+00', '2026-04-29 08:25:40.639424+00', NULL, '2026-04-17', 'KELUAR', '[FAKTUR BELI] Hwamda | Angsuran Mesin Hwamda (1,0Lsn x 1,0Qty)', 20000000, NULL, '2026-04-29 08:25:39.820229+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (316, '2026-04-29 08:25:42.042543+00', '2026-04-29 08:25:42.042543+00', NULL, '2026-04-17', 'KELUAR', '[FAKTUR BELI] Listrik PLN | kWh 19269.0 (1,0Lsn x 1,0Qty)', 20858656, NULL, '2026-04-29 08:25:41.356413+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (317, '2026-04-29 08:25:43.506451+00', '2026-04-29 08:25:43.506451+00', NULL, '2026-04-17', 'KELUAR', 'Tiner 8 Liter', 200000, NULL, '2026-04-29 08:25:42.892098+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (318, '2026-04-29 08:25:44.940183+00', '2026-04-29 08:25:44.940183+00', NULL, '2026-04-16', 'MASUK', 'Pelunasan Faktur BMP-0426-015 (mas wiranto)', 3100000, NULL, '2026-04-29 08:25:44.193663+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (319, '2026-04-29 08:25:46.332622+00', '2026-04-29 08:25:46.332622+00', NULL, '2026-04-16', 'MASUK', 'Pelunasan Faktur BMP-0426-014 (mas wiranto)', 5490000, NULL, '2026-04-29 08:25:45.659567+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (320, '2026-04-29 08:25:47.704939+00', '2026-04-29 08:25:47.704939+00', NULL, '2026-04-16', 'KELUAR', 'jasa angkut - mas adhi', 50000, NULL, '2026-04-29 08:25:47.090639+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (321, '2026-04-29 08:25:48.933775+00', '2026-04-29 08:25:48.933775+00', NULL, '2026-04-16', 'KELUAR', 'Cleo Gelas 2 Box, kmbli 2K ksih ke orng krja', 50000, NULL, '2026-04-29 08:25:48.321957+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (322, '2026-04-29 08:25:50.367411+00', '2026-04-29 08:25:50.367411+00', NULL, '2026-04-16', 'MASUK', 'Pelunasan Faktur BMP-0426-016 (ko hary)', 14400000, NULL, '2026-04-29 08:25:49.61356+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (323, '2026-04-29 08:25:51.596203+00', '2026-04-29 08:25:51.596203+00', NULL, '2026-04-15', 'KELUAR', 'Bayar Server Railway', 100000, NULL, '2026-04-29 08:25:50.981856+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (324, '2026-04-29 08:25:53.132496+00', '2026-04-29 08:25:53.132496+00', NULL, '2026-04-15', 'KELUAR', 'jasa angkut - mas adhi', 50000, NULL, '2026-04-29 08:25:52.313327+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (325, '2026-04-29 08:25:54.463389+00', '2026-04-29 08:25:54.463389+00', NULL, '2026-04-15', 'MASUK', 'Pembayaran Faktur BMP-0426-004 (mas wiranto)', 9012000, NULL, '2026-04-29 08:25:53.787334+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (326, '2026-04-29 08:25:55.962599+00', '2026-04-29 08:25:55.962599+00', NULL, '2026-04-15', 'MASUK', 'Pelunasan Faktur BMP-0426-013 (abah kosi''in)', 52350000, NULL, '2026-04-29 08:25:55.282611+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (327, '2026-04-29 08:25:57.433167+00', '2026-04-29 08:25:57.433167+00', NULL, '2026-04-14', 'KELUAR', '[FAKTUR BELI] Pak Kasnar | Wakul Telur (20,0Lsn x 50,0Qty), Wakul Telur Kotak (20,0Lsn x 100,0Qty), Karung Putih (1,0Lsn x 1,0Qty), Karung Kuning (1,0Lsn x 1,0Qty)', 16400000, NULL, '2026-04-29 08:25:56.722408+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (328, '2026-04-29 08:25:58.969198+00', '2026-04-29 08:25:58.969198+00', NULL, '2026-04-14', 'MASUK', 'Pembayaran Faktur BMP-0426-008 (Linda Abadi)', 52270000, NULL, '2026-04-29 08:25:58.130786+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (329, '2026-04-29 08:26:00.272977+00', '2026-04-29 08:26:00.272977+00', NULL, '2026-04-14', 'KELUAR', 'jasa angkut - mas adhi', 50000, NULL, '2026-04-29 08:25:59.583518+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (330, '2026-04-29 08:26:01.733997+00', '2026-04-29 08:26:01.733997+00', NULL, '2026-04-14', 'KELUAR', 'jasa pengiriman pak tono - Blora & Bojonegoro', 1750000, NULL, '2026-04-29 08:26:01.017414+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (331, '2026-04-29 08:26:02.962687+00', '2026-04-29 08:26:02.962687+00', NULL, '2026-04-13', 'KELUAR', 'jasa angkut - mas adi', 50000, NULL, '2026-04-29 08:26:02.348386+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (436, '2026-05-05 15:47:12.600732+00', '2026-05-05 15:47:12.600732+00', NULL, '2026-04-23', 'KELUAR', 'beli kambing', 3800000, NULL, '2026-05-05 15:47:12.591105+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (332, '2026-04-29 08:26:04.194003+00', '2026-04-29 08:26:04.194003+00', NULL, '2026-04-13', 'KELUAR', 'Uang Konsumsi Karyawan', 300000, NULL, '2026-04-29 08:26:03.524607+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (333, '2026-04-29 08:26:05.421813+00', '2026-04-29 08:26:05.421813+00', NULL, '2026-04-13', 'KELUAR', 'jasa angkut - pak jito & pak sul (ALI)', 260000, NULL, '2026-04-29 08:26:04.806359+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (334, '2026-04-29 08:26:06.644455+00', '2026-04-29 08:26:06.644455+00', NULL, '2026-04-10', 'KELUAR', '[FAKTUR BELI] Bengkel Sahabat - Kusmiantoro | DP Matras Piring "8" (1,0Lsn x 1,0Qty)', 8000000, NULL, '2026-04-29 08:26:06.036545+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (335, '2026-04-29 08:26:07.835598+00', '2026-04-29 08:26:07.835598+00', NULL, '2026-04-04', 'KELUAR', 'jasa pengiriman pak tono', 1000000, NULL, '2026-04-29 08:26:07.263666+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (336, '2026-04-29 08:26:09.106922+00', '2026-04-29 08:26:09.106922+00', NULL, '2026-04-04', 'KELUAR', 'jasa angkut - pak jito', 300000, NULL, '2026-04-29 08:26:08.492722+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (337, '2026-04-29 08:26:10.335594+00', '2026-04-29 08:26:10.335594+00', NULL, '2026-04-03', 'KELUAR', 'jasa pengiriman - arip (wiranto)', 15000, NULL, '2026-04-29 08:26:09.721222+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (338, '2026-04-29 08:26:11.57466+00', '2026-04-29 08:26:11.57466+00', NULL, '2026-04-02', 'KELUAR', 'jasa angkut - arip (wiranto)', 15000, NULL, '2026-04-29 08:26:10.928795+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (339, '2026-04-29 08:26:12.793994+00', '2026-04-29 08:26:12.793994+00', NULL, '2026-03-20', 'KELUAR', 'uang mesin ibu', 5000000, NULL, '2026-04-29 08:26:12.175149+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (340, '2026-04-29 08:26:14.332889+00', '2026-04-29 08:26:14.332889+00', NULL, '2026-03-20', 'KELUAR', 'Gaji Mas Dedi', 5000000, NULL, '2026-04-29 08:26:13.715298+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (342, '2026-04-29 08:26:16.889824+00', '2026-04-29 08:26:16.889824+00', NULL, '2026-03-09', 'KELUAR', 'jasa angkut - arip', 15000, NULL, '2026-04-29 08:26:16.172852+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (343, '2026-04-29 08:26:18.118011+00', '2026-04-29 08:26:18.118011+00', NULL, '2026-03-05', 'KELUAR', 'jasa angkut - pak jito', 300000, NULL, '2026-04-29 08:26:17.504398+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (344, '2026-04-29 08:26:19.449328+00', '2026-04-29 08:26:19.449328+00', NULL, '2026-03-05', 'KELUAR', 'jasa pengiriman pak tono', 1000000, NULL, '2026-04-29 08:26:18.803642+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (345, '2026-04-29 08:26:20.859598+00', '2026-04-29 08:26:20.859598+00', NULL, '2026-02-28', 'KELUAR', 'jasa pengiriman mas hendrik', 2000000, NULL, '2026-04-29 08:26:20.066457+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (346, '2026-04-29 08:26:22.316526+00', '2026-04-29 08:26:22.316526+00', NULL, '2026-02-28', 'KELUAR', 'jasa pengiriman pak tono', 1000000, NULL, '2026-04-29 08:26:21.599725+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (347, '2026-04-29 08:26:23.852489+00', '2026-04-29 08:26:23.852489+00', NULL, '2026-02-28', 'KELUAR', 'jasa angkut - pak jito', 300000, NULL, '2026-04-29 08:26:23.018681+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (348, '2026-04-29 08:26:25.245638+00', '2026-04-29 08:26:25.245638+00', NULL, '2026-02-26', 'KELUAR', 'jasa angkut - pak jito', 300000, NULL, '2026-04-29 08:26:24.570881+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (349, '2026-04-29 08:26:26.618241+00', '2026-04-29 08:26:26.618241+00', NULL, '2026-02-26', 'KELUAR', 'jasa pengiriman pak tono', 1000000, NULL, '2026-04-29 08:26:26.00312+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (350, '2026-04-29 08:26:28.051053+00', '2026-04-29 08:26:28.051053+00', NULL, '2026-02-20', 'KELUAR', 'Gaji Mas Dedi', 5000000, NULL, '2026-04-29 08:26:27.303052+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (351, '2026-04-29 08:26:29.452725+00', '2026-04-29 08:26:29.452725+00', NULL, '2026-02-20', 'KELUAR', 'uang mesin ibu', 5000000, NULL, '2026-04-29 08:26:28.767806+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (352, '2026-04-29 08:26:30.670596+00', '2026-04-29 08:26:30.670596+00', NULL, '2026-02-17', 'KELUAR', '[FAKTUR BELI] Listrik PLN | Listrik PLN (1,0Lsn x 1,0Qty)', 28246157, NULL, '2026-04-29 08:26:30.099201+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (341, '2026-04-29 08:26:15.509639+00', '2026-04-29 08:26:15.509639+00', '2026-04-30 04:39:07.560446+00', '2026-03-17', 'KELUAR', '[FAKTUR BELI] Listrik PLN | kWh 24364.0 (1,0Lsn x 1,0Qty)', 26534828, NULL, '2026-04-29 08:26:14.946584+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (361, '2026-04-30 07:44:15.056745+00', '2026-04-30 07:44:15.056745+00', '2026-04-30 07:44:25.953725+00', '2026-04-30', 'MASUK', 'tes', 1000000, NULL, '2026-04-30 07:44:15.049075+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (362, '2026-04-30 07:44:39.576771+00', '2026-04-30 07:44:39.576771+00', '2026-04-30 07:44:47.089357+00', '2026-04-30', 'KELUAR', 'tes', 1000000, NULL, '2026-04-30 07:44:39.568787+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (364, '2026-04-30 07:46:15.737683+00', '2026-04-30 07:46:15.737683+00', '2026-04-30 07:48:20.594042+00', '2026-04-30', 'KELUAR', 'listrik', 1000000, NULL, '2026-04-30 07:46:15.729209+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (365, '2026-04-30 07:46:32.407465+00', '2026-04-30 07:46:32.407465+00', '2026-04-30 07:48:26.176597+00', '2026-04-30', 'KELUAR', 'listrik', 28246157, NULL, '2026-04-30 07:46:32.398768+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (360, '2026-04-30 07:25:44.818304+00', '2026-04-30 07:25:44.818304+00', '2026-05-01 07:28:14.48964+00', '2026-02-17', 'KELUAR', 'Bayar Listrik ', 28246157, NULL, '2026-04-30 07:25:44.810686+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (363, '2026-04-30 07:45:13.076561+00', '2026-04-30 07:45:13.076561+00', '2026-05-01 07:28:47.013207+00', '2026-02-17', 'KELUAR', 'listrik', 28246157, NULL, '2026-04-30 07:45:13.068658+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (366, '2026-04-30 07:49:32.581723+00', '2026-04-30 07:49:32.581723+00', '2026-05-01 14:11:46.394652+00', '2026-02-27', 'KELUAR', 'ok', 100000, NULL, '2026-04-30 07:49:32.574355+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (372, '2026-05-01 16:11:52.602345+00', '2026-05-01 16:11:52.602345+00', '2026-05-01 16:12:18.787862+00', '2026-05-01', 'KELUAR', 'Pembayaran Gaji: ', 60000, NULL, '2026-05-01 16:11:52.602311+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (375, '2026-05-02 02:54:27.623938+00', '2026-05-02 02:54:27.623938+00', '2026-05-02 02:54:56.897892+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: ', 285000, NULL, '2026-05-02 02:54:27.623919+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (376, '2026-05-02 02:55:45.392985+00', '2026-05-02 02:55:45.392985+00', '2026-05-02 02:57:21.455379+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: ', 405000, NULL, '2026-05-02 02:55:45.392969+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (389, '2026-05-02 03:34:46.902017+00', '2026-05-02 03:34:46.902017+00', '2026-05-02 03:35:20.109715+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: ', 300000, NULL, '2026-05-02 03:34:46.901998+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (388, '2026-05-02 03:34:46.292063+00', '2026-05-02 03:34:46.292063+00', '2026-05-02 03:35:23.121455+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: ', 315000, NULL, '2026-05-02 03:34:46.292045+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (387, '2026-05-02 03:34:45.804191+00', '2026-05-02 03:34:45.804191+00', '2026-05-02 03:35:25.588573+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: ', 345000, NULL, '2026-05-02 03:34:45.804175+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (386, '2026-05-02 03:34:45.126995+00', '2026-05-02 03:34:45.126995+00', '2026-05-02 03:35:28.512024+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: ', 360000, NULL, '2026-05-02 03:34:45.126982+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (406, '2026-05-02 05:14:01.806502+00', '2026-05-02 05:14:01.806502+00', '2026-05-02 05:38:50.921704+00', '2026-05-02', 'MASUK', '{"error":"template: payroll-pdf.html:97:21: executing \"payroll-pdf.html\" at \u003c.HariKerja\u003e: can''t evaluate field HariKerja in type models.Payroll"}', 1, NULL, '2026-05-02 05:14:01.800581+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (404, '2026-05-02 05:12:59.453199+00', '2026-05-02 05:12:59.453199+00', '2026-05-02 05:51:59.534706+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: ', 157500, NULL, '2026-05-02 05:12:59.45315+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (405, '2026-05-02 05:12:59.905796+00', '2026-05-02 05:12:59.905796+00', '2026-05-02 05:52:08.916479+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: ', 165000, NULL, '2026-05-02 05:12:59.905747+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (403, '2026-05-02 05:12:58.962658+00', '2026-05-02 05:12:58.962658+00', '2026-05-02 05:52:13.708312+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: ', 262500, NULL, '2026-05-02 05:12:58.962613+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (402, '2026-05-02 05:12:58.522233+00', '2026-05-02 05:12:58.522233+00', '2026-05-02 05:52:15.85472+00', '2026-05-02', 'KELUAR', 'Pembayaran Gaji: ', 287500, NULL, '2026-05-02 05:12:58.522195+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (425, '2026-05-04 18:26:14.313234+00', '2026-05-04 18:26:14.313234+00', NULL, '2026-05-04', 'MASUK', 'Pembayaran Faktur BMP-2605-001 (mas zahid)', 12850000, 1, '2026-05-04 18:26:14.313192+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (426, '2026-05-04 18:27:41.615452+00', '2026-05-04 18:27:41.615452+00', NULL, '2026-05-04', 'MASUK', 'Pembayaran Borongan Faktur BMP-0426-054', 12625000, 2, '2026-05-04 18:27:41.596012+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (427, '2026-05-04 18:28:49.971444+00', '2026-05-04 18:28:49.971444+00', NULL, '2026-05-04', 'MASUK', 'Pembayaran Borongan Faktur BMP-2605-001', 21000000, 3, '2026-05-04 18:28:49.955775+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (429, '2026-05-05 02:38:14.576285+00', '2026-05-05 02:38:14.576285+00', NULL, '2026-05-05', 'MASUK', 'Pembayaran Borongan Faktur BMP-0426-055', 12400000, 5, '2026-05-05 02:38:14.553214+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (430, '2026-05-05 02:38:14.607075+00', '2026-05-05 02:38:14.607075+00', NULL, '2026-05-05', 'MASUK', 'Pembayaran Borongan Faktur BMP-2605-001', 16522000, 6, '2026-05-05 02:38:14.553214+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (428, '2026-05-04 18:31:08.379639+00', '2026-05-04 18:31:08.379639+00', '2026-05-05 17:28:03.145189+00', '2026-05-04', 'MASUK', 'Pembayaran Borongan Faktur BMP-0426-054', 20000000, 4, '2026-05-04 18:31:08.363764+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (437, '2026-05-09 05:06:54.472466+00', '2026-05-09 05:06:54.472466+00', NULL, '2026-05-08', 'KELUAR', 'beli oli pertamina 2 drum', 12310000, NULL, '2026-05-09 05:06:54.46659+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (438, '2026-05-09 05:07:48.64233+00', '2026-05-09 05:07:48.64233+00', NULL, '2026-05-09', 'KELUAR', 'bayar angsuran mesin', 20000000, NULL, '2026-05-09 05:07:48.632273+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (444, '2026-05-14 13:16:46.854734+00', '2026-05-14 13:16:46.854734+00', NULL, '2026-04-11', 'MASUK', 'Pembayaran Faktur BMP-2605-009 (abah ali)', 34692500, 14, '2026-05-14 13:16:46.854704+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (447, '2026-05-15 14:08:45.052968+00', '2026-05-15 14:08:45.052968+00', NULL, '2026-05-14', 'MASUK', 'Pembayaran Faktur BMP-2605-013 (Mas Malvin)', 20845000, 17, '2026-05-15 14:08:45.052948+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (459, '2026-05-23 09:24:27.848657+00', '2026-05-23 09:24:27.848657+00', NULL, '2026-05-23', 'MASUK', 'Pembayaran Borongan Faktur BMP-0426-011 (mas wiranto)', 15039000, 21, '2026-05-23 09:24:27.832923+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (460, '2026-05-23 09:24:27.87019+00', '2026-05-23 09:24:27.87019+00', NULL, '2026-05-23', 'MASUK', 'Pembayaran Borongan Faktur BMP-0426-050 (mas wiranto)', 3506000, 22, '2026-05-23 09:24:27.832923+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (464, '2026-05-26 08:35:52.865135+00', '2026-05-26 08:35:52.865135+00', NULL, '2026-05-12', 'MASUK', '[DEMO] Pembayaran Faktur DEMO-INV-002 ([DEMO] Toko Plastik Sejahtera)', 700000, 25, '2026-05-12 08:35:52.837802+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (473, '2026-05-30 05:58:05.949146+00', '2026-05-30 05:58:05.949146+00', '2026-05-30 08:18:27.513104+00', '2026-05-30', 'KELUAR', 'Pembayaran Gaji: Auto-fill (Minggu ini)', 50000, NULL, '2026-05-30 05:58:05.949124+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (470, '2026-05-30 05:58:04.884509+00', '2026-05-30 05:58:04.884509+00', '2026-05-30 08:18:33.131115+00', '2026-05-30', 'KELUAR', 'Pembayaran Gaji: Auto-fill (Minggu ini)', 157500, NULL, '2026-05-30 05:58:04.884496+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (472, '2026-05-30 05:58:05.596496+00', '2026-05-30 05:58:05.596496+00', '2026-05-30 08:18:33.131115+00', '2026-05-30', 'KELUAR', 'Pembayaran Gaji: Auto-fill (Minggu ini)', 157500, NULL, '2026-05-30 05:58:05.596472+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (471, '2026-05-30 05:58:05.240028+00', '2026-05-30 05:58:05.240028+00', '2026-05-30 08:18:38.81135+00', '2026-05-30', 'KELUAR', 'Pembayaran Gaji: Auto-fill (Minggu ini)', 225000, NULL, '2026-05-30 05:58:05.240013+00', NULL, NULL, NULL, NULL, false);
INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (469, '2026-05-30 05:58:04.523482+00', '2026-05-30 05:58:04.523482+00', '2026-05-30 08:18:46.788159+00', '2026-05-30', 'KELUAR', 'Pembayaran Gaji: Auto-fill (Minggu ini)', 180000, NULL, '2026-05-30 05:58:04.523465+00', NULL, NULL, NULL, NULL, false);


--
-- Data for Name: clients; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.clients (id, created_at, updated_at, deleted_at, saldo_titipan, client_name, address_line1, client_logo, province, postal_code, phone_number, email_address, tax_number, unique_id, slug, date_created, last_updated, is_demo) VALUES (8, '2026-03-14 07:36:27.816257+00', '2026-03-14 07:36:27.81638+00', NULL, 0.00, 'mas kolis', 'mojojejer, jombang', 'default_logo.jpg', 'Jawa Timur', NULL, NULL, NULL, NULL, 'e61abc8929eb', 'mas-kolis-jawa-timur-e61abc8929eb', '2026-03-14 07:36:27.816257+00', '2026-03-14 07:36:27.81638+00', false);
INSERT INTO public.clients (id, created_at, updated_at, deleted_at, saldo_titipan, client_name, address_line1, client_logo, province, postal_code, phone_number, email_address, tax_number, unique_id, slug, date_created, last_updated, is_demo) VALUES (7, '2026-03-12 12:58:52.419143+00', '2026-03-23 19:21:12.65418+00', NULL, 0.00, 'contoh', 'Jl. Cendrawasih No. 66 Kecamatan Gedangan Kabupaten Sidoarjo Ds. Punggul Rt. 05 Rw. 02', 'company_logos/8558_xa9uLKm.jpg', 'Jawa Timur', '61254', '6282652626237', 'muhammadmuizz8@gmail.com', NULL, 'a563faa58c48', 'contoh-jawa-timur-a563faa58c48', '2026-03-12 12:58:52.419143+00', '2026-03-23 19:21:12.65418+00', false);
INSERT INTO public.clients (id, created_at, updated_at, deleted_at, saldo_titipan, client_name, address_line1, client_logo, province, postal_code, phone_number, email_address, tax_number, unique_id, slug, date_created, last_updated, is_demo) VALUES (12, '2026-04-13 01:30:57.775291+00', '2026-04-13 01:30:57.775361+00', NULL, 0.00, 'abah ali', 'pasar turi', '', 'Jawa Timur', NULL, NULL, NULL, NULL, '4491ce4544e6', 'abah-ali-jawa-timur-4491ce4544e6', '2026-04-13 01:30:57.775291+00', '2026-04-13 01:30:57.775361+00', false);
INSERT INTO public.clients (id, created_at, updated_at, deleted_at, saldo_titipan, client_name, address_line1, client_logo, province, postal_code, phone_number, email_address, tax_number, unique_id, slug, date_created, last_updated, is_demo) VALUES (13, '2026-04-13 04:46:14.429537+00', '2026-04-13 04:46:14.429613+00', NULL, 0.00, 'abah aan', 'kudus', '', 'Jawa Timur', NULL, NULL, NULL, NULL, '9a865fb86b3f', 'abah-aan-jawa-timur-9a865fb86b3f', '2026-04-13 04:46:14.429537+00', '2026-04-13 04:46:14.429613+00', false);
INSERT INTO public.clients (id, created_at, updated_at, deleted_at, saldo_titipan, client_name, address_line1, client_logo, province, postal_code, phone_number, email_address, tax_number, unique_id, slug, date_created, last_updated, is_demo) VALUES (14, '2026-04-13 06:21:31.989129+00', '2026-04-13 06:21:31.98924+00', NULL, 0.00, 'mas wiranto', 'jombang', '', '', NULL, NULL, NULL, NULL, '5f52d5627185', 'mas-wiranto-5f52d5627185', '2026-04-13 06:21:31.989129+00', '2026-04-13 06:21:31.98924+00', false);
INSERT INTO public.clients (id, created_at, updated_at, deleted_at, saldo_titipan, client_name, address_line1, client_logo, province, postal_code, phone_number, email_address, tax_number, unique_id, slug, date_created, last_updated, is_demo) VALUES (15, '2026-04-13 11:51:00.200388+00', '2026-04-13 11:51:00.200457+00', NULL, 0.00, 'Linda Abadi', 'jl. Raya Sulang-Rembang RT 01 / RW 01', '', '', NULL, '082132939649', NULL, NULL, '5382f6e3c63a', 'linda-abadi-5382f6e3c63a', '2026-04-13 11:51:00.200388+00', '2026-04-13 11:51:00.200457+00', false);
INSERT INTO public.clients (id, created_at, updated_at, deleted_at, saldo_titipan, client_name, address_line1, client_logo, province, postal_code, phone_number, email_address, tax_number, unique_id, slug, date_created, last_updated, is_demo) VALUES (16, '2026-04-14 05:14:35.430808+00', '2026-04-14 05:14:35.430882+00', NULL, 0.00, 'mas zahid', 'krian', '', 'Jawa Timur', NULL, NULL, NULL, NULL, '008a9246f228', 'mas-zahid-jawa-timur-008a9246f228', '2026-04-14 05:14:35.430808+00', '2026-04-14 05:14:35.430882+00', false);
INSERT INTO public.clients (id, created_at, updated_at, deleted_at, saldo_titipan, client_name, address_line1, client_logo, province, postal_code, phone_number, email_address, tax_number, unique_id, slug, date_created, last_updated, is_demo) VALUES (17, '2026-04-15 10:35:21.008645+00', '2026-04-15 10:35:21.008734+00', NULL, 0.00, 'abah kosi''in', 'grobogan', '', 'Jawa Timur', NULL, NULL, NULL, NULL, 'fe67439feeda', 'abah-kosiin-jawa-timur-fe67439feeda', '2026-04-15 10:35:21.008645+00', '2026-04-15 10:35:21.008734+00', false);
INSERT INTO public.clients (id, created_at, updated_at, deleted_at, saldo_titipan, client_name, address_line1, client_logo, province, postal_code, phone_number, email_address, tax_number, unique_id, slug, date_created, last_updated, is_demo) VALUES (18, '2026-04-16 11:20:19.732479+00', '2026-04-16 11:20:19.732541+00', NULL, 0.00, 'ko hary', 'surabaya', '', 'Jawa Timur', NULL, NULL, NULL, NULL, 'a63dde0f5aa7', 'ko-hary-jawa-timur-a63dde0f5aa7', '2026-04-16 11:20:19.732479+00', '2026-04-16 11:20:19.732541+00', false);
INSERT INTO public.clients (id, created_at, updated_at, deleted_at, saldo_titipan, client_name, address_line1, client_logo, province, postal_code, phone_number, email_address, tax_number, unique_id, slug, date_created, last_updated, is_demo) VALUES (19, '2026-04-20 01:19:25.429696+00', '2026-04-20 01:19:25.429926+00', NULL, 0.00, 'pak katiran', NULL, '', '', NULL, NULL, NULL, NULL, '04f5fec83e9b', 'pak-katiran-04f5fec83e9b', '2026-04-20 01:19:25.429696+00', '2026-04-20 01:19:25.429926+00', false);
INSERT INTO public.clients (id, created_at, updated_at, deleted_at, saldo_titipan, client_name, address_line1, client_logo, province, postal_code, phone_number, email_address, tax_number, unique_id, slug, date_created, last_updated, is_demo) VALUES (20, '2026-04-20 05:28:34.698567+00', '2026-04-20 05:28:34.698655+00', NULL, 0.00, 'Mas Arylah', NULL, '', '', NULL, NULL, NULL, NULL, '2262fc8fe4c4', 'mas-arylah-2262fc8fe4c4', '2026-04-20 05:28:34.698567+00', '2026-04-20 05:28:34.698655+00', false);
INSERT INTO public.clients (id, created_at, updated_at, deleted_at, saldo_titipan, client_name, address_line1, client_logo, province, postal_code, phone_number, email_address, tax_number, unique_id, slug, date_created, last_updated, is_demo) VALUES (22, '2026-04-20 06:30:42.699424+00', '2026-04-20 06:30:42.699491+00', NULL, 0.00, 'Umik Erna', NULL, '', '', NULL, NULL, NULL, NULL, '1cfaa3143ccb', 'umik-erna-1cfaa3143ccb', '2026-04-20 06:30:42.699424+00', '2026-04-20 06:30:42.699491+00', false);
INSERT INTO public.clients (id, created_at, updated_at, deleted_at, saldo_titipan, client_name, address_line1, client_logo, province, postal_code, phone_number, email_address, tax_number, unique_id, slug, date_created, last_updated, is_demo) VALUES (23, '2026-04-20 06:33:11.726279+00', '2026-04-20 06:33:11.72635+00', NULL, 0.00, 'Pak Huda', NULL, '', '', NULL, NULL, NULL, NULL, '0b3afccfc639', 'pak-huda-0b3afccfc639', '2026-04-20 06:33:11.726279+00', '2026-04-20 06:33:11.72635+00', false);
INSERT INTO public.clients (id, created_at, updated_at, deleted_at, saldo_titipan, client_name, address_line1, client_logo, province, postal_code, phone_number, email_address, tax_number, unique_id, slug, date_created, last_updated, is_demo) VALUES (24, '2026-04-20 11:52:23.207269+00', '2026-04-20 11:52:23.207349+00', NULL, 0.00, 'Mas Malvin', NULL, '', '', NULL, NULL, NULL, NULL, '1ee859ca5484', 'mas-malvin-1ee859ca5484', '2026-04-20 11:52:23.207269+00', '2026-04-20 11:52:23.207349+00', false);
INSERT INTO public.clients (id, created_at, updated_at, deleted_at, saldo_titipan, client_name, address_line1, client_logo, province, postal_code, phone_number, email_address, tax_number, unique_id, slug, date_created, last_updated, is_demo) VALUES (25, '2026-04-23 01:38:58.002916+00', '2026-04-23 01:38:58.004041+00', NULL, 0.00, 'Mas Eka', 'Jombang', '', '', NULL, NULL, NULL, NULL, '6a80f3487e0f', 'mas-eka-6a80f3487e0f', '2026-04-23 01:38:58.002916+00', '2026-04-23 01:38:58.004041+00', false);
INSERT INTO public.clients (id, created_at, updated_at, deleted_at, saldo_titipan, client_name, address_line1, client_logo, province, postal_code, phone_number, email_address, tax_number, unique_id, slug, date_created, last_updated, is_demo) VALUES (26, '2026-04-28 03:04:02.092367+00', '2026-04-28 03:04:02.092453+00', NULL, 0.00, 'mas yeyen', 'blitar', '', 'Jawa Timur', NULL, NULL, NULL, NULL, 'c89dad5e8148', 'mas-yeyen-jawa-timur-c89dad5e8148', '2026-04-28 03:04:02.092367+00', '2026-04-28 03:04:02.092453+00', false);
INSERT INTO public.clients (id, created_at, updated_at, deleted_at, saldo_titipan, client_name, address_line1, client_logo, province, postal_code, phone_number, email_address, tax_number, unique_id, slug, date_created, last_updated, is_demo) VALUES (27, '2026-04-30 04:34:36.279282+00', '2026-05-01 14:51:07.797272+00', NULL, 0.00, 'Mas iyon', '', '', 'gersik', '', '', '', '', '80231e6b', '', '2026-04-30 04:34:36.27083+00', '2026-05-01 14:51:07.793428+00', false);
INSERT INTO public.clients (id, created_at, updated_at, deleted_at, saldo_titipan, client_name, address_line1, client_logo, province, postal_code, phone_number, email_address, tax_number, unique_id, slug, date_created, last_updated, is_demo) VALUES (35, '2026-05-05 13:32:18.843673+00', '2026-05-05 13:32:18.843673+00', NULL, 0.00, 'pak sandi', '', '', 'malang - jatim', '', '', '', '', '08d3d704', 'pak-sandi-08d3d704', '2026-05-05 13:32:18.835529+00', '2026-05-05 13:32:18.835529+00', false);
INSERT INTO public.clients (id, created_at, updated_at, deleted_at, saldo_titipan, client_name, address_line1, client_logo, province, postal_code, phone_number, email_address, tax_number, unique_id, slug, date_created, last_updated, is_demo) VALUES (21, '2026-04-20 05:37:53.961019+00', '2026-05-22 04:11:28.123106+00', NULL, 0.00, 'Mas Eko Cahyono', '', '', 'jawa tengah - cepu', '', '', '', '', 'a9ca31ee63a8', 'mas-eko-cahyono-a9ca31ee63a8', '2026-04-20 05:37:53.961019+00', '2026-05-22 04:11:28.117456+00', false);
INSERT INTO public.clients (id, created_at, updated_at, deleted_at, saldo_titipan, client_name, address_line1, client_logo, province, postal_code, phone_number, email_address, tax_number, unique_id, slug, date_created, last_updated, is_demo) VALUES (36, '2026-05-22 07:01:07.838885+00', '2026-05-22 07:01:07.838885+00', NULL, 0.00, 'Abah Hary', '', '', 'Jawa Timur - Lumajang', '', '', '', '', '09b6bef2', 'abah-hary-09b6bef2', '2026-05-22 07:01:07.829787+00', '2026-05-22 07:01:07.829787+00', false);
INSERT INTO public.clients (id, created_at, updated_at, deleted_at, saldo_titipan, client_name, address_line1, client_logo, province, postal_code, phone_number, email_address, tax_number, unique_id, slug, date_created, last_updated, is_demo) VALUES (37, '2026-05-26 08:35:52.624999+00', '2026-05-26 08:35:52.624999+00', NULL, 0.00, '[DEMO] PT Plastik Nusantara', 'Jl. Industri Demo No. 1, Tangerang', '', 'Banten', '15000', '021-0000001', 'demo1@example.com', '', '517091bb', 'demo-pt-plastik-nusantara-9d16', '2026-04-26 08:35:52.616972+00', '2026-05-26 08:35:52.616973+00', true);
INSERT INTO public.clients (id, created_at, updated_at, deleted_at, saldo_titipan, client_name, address_line1, client_logo, province, postal_code, phone_number, email_address, tax_number, unique_id, slug, date_created, last_updated, is_demo) VALUES (38, '2026-05-26 08:35:52.652655+00', '2026-05-26 08:35:52.652655+00', NULL, 0.00, '[DEMO] Toko Plastik Sejahtera', 'Jl. Pasar Demo No. 22, Bandung', '', 'Jawa Barat', '40001', '022-0000002', 'demo2@example.com', '', '182d27f3', 'demo-toko-plastik-sejahtera-8609', '2026-05-06 08:35:52.616973+00', '2026-05-26 08:35:52.616973+00', true);
INSERT INTO public.clients (id, created_at, updated_at, deleted_at, saldo_titipan, client_name, address_line1, client_logo, province, postal_code, phone_number, email_address, tax_number, unique_id, slug, date_created, last_updated, is_demo) VALUES (39, '2026-05-26 08:35:52.700981+00', '2026-05-26 08:35:52.700981+00', NULL, 0.00, '[DEMO] UD Karya Plastik Maju', 'Jl. Raya Demo No. 99, Surabaya', '', 'Jawa Timur', '60001', '031-0000003', 'demo3@example.com', '', '024d6cbf', 'demo-ud-karya-plastik-maju-5b0f', '2026-05-16 08:35:52.616974+00', '2026-05-26 08:35:52.616974+00', true);


--
-- Data for Name: employees; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.employees (id, created_at, updated_at, deleted_at, name, "position", salary_amount, is_active, fingerprint_pin, is_demo) VALUES (21, '2026-05-12 06:36:28.620103+00', '2026-05-12 06:36:38.034069+00', '2026-05-12 06:36:44.273883+00', 'muizz', 'admin', 83.30, true, NULL, false);
INSERT INTO public.employees (id, created_at, updated_at, deleted_at, name, "position", salary_amount, is_active, fingerprint_pin, is_demo) VALUES (22, '2026-05-12 06:37:12.385778+00', '2026-05-12 07:53:49.293778+00', NULL, 'muizz', 'admin', 83300.00, true, '1', false);
INSERT INTO public.employees (id, created_at, updated_at, deleted_at, name, "position", salary_amount, is_active, fingerprint_pin, is_demo) VALUES (4, '2026-05-01 17:01:57.114086+00', '2026-05-13 04:49:55.365231+00', NULL, 'mbak santi', 'operator', 50000.00, true, '3', false);
INSERT INTO public.employees (id, created_at, updated_at, deleted_at, name, "position", salary_amount, is_active, fingerprint_pin, is_demo) VALUES (10, '2026-05-01 17:04:31.230305+00', '2026-05-13 05:30:26.272514+00', NULL, 'mas ibnu', 'operator', 55000.00, true, '5', false);
INSERT INTO public.employees (id, created_at, updated_at, deleted_at, name, "position", salary_amount, is_active, fingerprint_pin, is_demo) VALUES (19, '2026-05-01 17:30:51.118514+00', '2026-05-13 05:30:44.835055+00', NULL, 'mas febri', 'operator', 52500.00, true, '4', false);
INSERT INTO public.employees (id, created_at, updated_at, deleted_at, name, "position", salary_amount, is_active, fingerprint_pin, is_demo) VALUES (15, '2026-05-01 17:29:47.997786+00', '2026-05-13 05:34:25.999349+00', NULL, 'mas vincent', 'operator', 50000.00, true, '6', false);
INSERT INTO public.employees (id, created_at, updated_at, deleted_at, name, "position", salary_amount, is_active, fingerprint_pin, is_demo) VALUES (6, '2026-05-01 17:02:49.06164+00', '2026-05-13 05:35:41.664547+00', NULL, 'mbak asih', 'operator', 47500.00, true, '7', false);
INSERT INTO public.employees (id, created_at, updated_at, deleted_at, name, "position", salary_amount, is_active, fingerprint_pin, is_demo) VALUES (5, '2026-05-01 17:02:16.865171+00', '2026-05-13 05:46:34.551968+00', NULL, 'mbak endah', 'operator', 47500.00, true, '8', false);
INSERT INTO public.employees (id, created_at, updated_at, deleted_at, name, "position", salary_amount, is_active, fingerprint_pin, is_demo) VALUES (11, '2026-05-01 17:28:42.225556+00', '2026-05-13 05:47:44.488906+00', NULL, 'mas wahyu', 'operator', 67500.00, true, '9', false);
INSERT INTO public.employees (id, created_at, updated_at, deleted_at, name, "position", salary_amount, is_active, fingerprint_pin, is_demo) VALUES (8, '2026-05-01 17:03:59.158157+00', '2026-05-13 07:56:50.156872+00', NULL, 'mas dimas', 'operator', 75000.00, true, '10', false);
INSERT INTO public.employees (id, created_at, updated_at, deleted_at, name, "position", salary_amount, is_active, fingerprint_pin, is_demo) VALUES (12, '2026-05-01 17:28:54.624581+00', '2026-05-13 07:57:49.508386+00', NULL, 'mas roby', 'operator', 52500.00, true, '11', false);
INSERT INTO public.employees (id, created_at, updated_at, deleted_at, name, "position", salary_amount, is_active, fingerprint_pin, is_demo) VALUES (2, '2026-05-01 16:33:47.120057+00', '2026-05-13 07:58:41.963682+00', NULL, 'mbak nur', 'operator', 57500.00, true, '12', false);
INSERT INTO public.employees (id, created_at, updated_at, deleted_at, name, "position", salary_amount, is_active, fingerprint_pin, is_demo) VALUES (13, '2026-05-01 17:29:14.805332+00', '2026-05-13 07:59:44.519471+00', NULL, 'mas farel', 'operator', 52500.00, true, '13', false);
INSERT INTO public.employees (id, created_at, updated_at, deleted_at, name, "position", salary_amount, is_active, fingerprint_pin, is_demo) VALUES (1, '2026-05-01 16:11:45.720327+00', '2026-05-13 15:54:16.782479+00', NULL, 'mbak lik', 'operator', 60000.00, true, '14', false);
INSERT INTO public.employees (id, created_at, updated_at, deleted_at, name, "position", salary_amount, is_active, fingerprint_pin, is_demo) VALUES (3, '2026-05-01 17:01:36.66719+00', '2026-05-13 15:54:26.17067+00', NULL, 'mbak eni', 'operator', 52500.00, true, '15', false);
INSERT INTO public.employees (id, created_at, updated_at, deleted_at, name, "position", salary_amount, is_active, fingerprint_pin, is_demo) VALUES (17, '2026-05-01 17:30:27.758812+00', '2026-05-13 16:01:22.991412+00', NULL, 'mas karem', 'operator', 52500.00, true, '16', false);
INSERT INTO public.employees (id, created_at, updated_at, deleted_at, name, "position", salary_amount, is_active, fingerprint_pin, is_demo) VALUES (9, '2026-05-01 17:04:14.571467+00', '2026-05-13 16:01:47.431144+00', NULL, 'mas agung', 'operator', 75000.00, true, '17', false);
INSERT INTO public.employees (id, created_at, updated_at, deleted_at, name, "position", salary_amount, is_active, fingerprint_pin, is_demo) VALUES (18, '2026-05-01 17:30:41.685459+00', '2026-05-13 16:02:10.967729+00', NULL, 'mas dian', 'operator', 52500.00, true, '18', false);
INSERT INTO public.employees (id, created_at, updated_at, deleted_at, name, "position", salary_amount, is_active, fingerprint_pin, is_demo) VALUES (14, '2026-05-01 17:29:37.919977+00', '2026-05-13 16:04:59.975933+00', NULL, 'mas candra', 'operator', 52500.00, true, '19', false);
INSERT INTO public.employees (id, created_at, updated_at, deleted_at, name, "position", salary_amount, is_active, fingerprint_pin, is_demo) VALUES (16, '2026-05-01 17:30:07.011422+00', '2026-05-02 05:57:11.434088+00', '2026-05-14 05:29:41.414906+00', 'mas nanda', 'operator', 50000.00, true, NULL, false);
INSERT INTO public.employees (id, created_at, updated_at, deleted_at, name, "position", salary_amount, is_active, fingerprint_pin, is_demo) VALUES (20, '2026-05-02 05:54:11.012681+00', '2026-05-02 05:54:11.012681+00', '2026-05-14 05:30:40.9617+00', 'mbak vivin', 'operator', 50000.00, true, NULL, false);
INSERT INTO public.employees (id, created_at, updated_at, deleted_at, name, "position", salary_amount, is_active, fingerprint_pin, is_demo) VALUES (7, '2026-05-01 17:03:12.291989+00', '2026-05-14 05:31:03.791727+00', NULL, 'mbak vivin', 'operator', 47500.00, true, '20', false);
INSERT INTO public.employees (id, created_at, updated_at, deleted_at, name, "position", salary_amount, is_active, fingerprint_pin, is_demo) VALUES (23, '2026-05-22 09:38:12.228808+00', '2026-05-22 09:38:12.228808+00', NULL, 'noval', 'operator', 50000.00, true, '21', false);
INSERT INTO public.employees (id, created_at, updated_at, deleted_at, name, "position", salary_amount, is_active, fingerprint_pin, is_demo) VALUES (24, '2026-05-30 05:54:56.962741+00', '2026-05-30 05:54:56.962741+00', NULL, 'fareliyan', 'operator', 50000.00, true, '22', false);


--
-- Data for Name: invoice_payments; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.invoice_payments (id, created_at, updated_at, deleted_at, invoice_id, payment_date, payment_amount, payment_method, date_created) VALUES (1, '2026-05-04 18:26:14.30422+00', '2026-05-04 18:26:14.30422+00', NULL, 251, '2026-05-04', 12850000, 'TRANSFER', '2026-05-04 18:26:14.304194+00');
INSERT INTO public.invoice_payments (id, created_at, updated_at, deleted_at, invoice_id, payment_date, payment_amount, payment_method, date_created) VALUES (2, '2026-05-04 18:27:41.611409+00', '2026-05-04 18:27:41.611409+00', NULL, 207, '2026-05-04', 12625000, 'Borongan TRANSFER', '2026-05-04 18:27:41.596012+00');
INSERT INTO public.invoice_payments (id, created_at, updated_at, deleted_at, invoice_id, payment_date, payment_amount, payment_method, date_created) VALUES (3, '2026-05-04 18:28:49.967401+00', '2026-05-04 18:28:49.967401+00', NULL, 211, '2026-05-04', 21000000, 'Borongan TRANSFER', '2026-05-04 18:28:49.955775+00');
INSERT INTO public.invoice_payments (id, created_at, updated_at, deleted_at, invoice_id, payment_date, payment_amount, payment_method, date_created) VALUES (5, '2026-05-05 02:38:14.568073+00', '2026-05-05 02:38:14.568073+00', NULL, 209, '2026-05-05', 12400000, 'Borongan TRANSFER', '2026-05-05 02:38:14.553214+00');
INSERT INTO public.invoice_payments (id, created_at, updated_at, deleted_at, invoice_id, payment_date, payment_amount, payment_method, date_created) VALUES (6, '2026-05-05 02:38:14.602782+00', '2026-05-05 02:38:14.602782+00', NULL, 250, '2026-05-05', 16522000, 'Borongan TRANSFER', '2026-05-05 02:38:14.553214+00');
INSERT INTO public.invoice_payments (id, created_at, updated_at, deleted_at, invoice_id, payment_date, payment_amount, payment_method, date_created) VALUES (7, '2026-05-05 09:50:09.630093+00', '2026-05-05 09:50:09.630093+00', NULL, 252, '2026-05-05', 10000000, 'TRANSFER', '2026-05-05 09:50:09.630072+00');
INSERT INTO public.invoice_payments (id, created_at, updated_at, deleted_at, invoice_id, payment_date, payment_amount, payment_method, date_created) VALUES (8, '2026-05-05 13:42:46.400666+00', '2026-05-05 13:42:46.400666+00', NULL, 111, '2026-04-28', 20320000, 'TRANSFER', '2026-05-05 13:42:46.400645+00');
INSERT INTO public.invoice_payments (id, created_at, updated_at, deleted_at, invoice_id, payment_date, payment_amount, payment_method, date_created) VALUES (10, '2026-05-05 13:45:28.711869+00', '2026-05-05 13:45:28.711869+00', NULL, 170, '2026-05-05', 10700000, 'TRANSFER', '2026-05-05 13:45:28.711852+00');
INSERT INTO public.invoice_payments (id, created_at, updated_at, deleted_at, invoice_id, payment_date, payment_amount, payment_method, date_created) VALUES (11, '2026-05-05 13:47:32.656464+00', '2026-05-05 13:47:32.656464+00', '2026-05-05 15:49:22.077269+00', 253, '2026-05-05', 10700000, 'TRANSFER', '2026-05-05 13:47:32.656437+00');
INSERT INTO public.invoice_payments (id, created_at, updated_at, deleted_at, invoice_id, payment_date, payment_amount, payment_method, date_created) VALUES (12, '2026-05-13 13:28:55.257008+00', '2026-05-13 13:28:55.257008+00', NULL, 115, '2026-05-13', 53000000, 'TRANSFER', '2026-05-13 13:28:55.256988+00');
INSERT INTO public.invoice_payments (id, created_at, updated_at, deleted_at, invoice_id, payment_date, payment_amount, payment_method, date_created) VALUES (13, '2026-05-14 01:52:55.581542+00', '2026-05-14 01:52:55.581542+00', NULL, 257, '2026-05-01', 10000000, 'TRANSFER', '2026-05-14 01:52:55.580947+00');
INSERT INTO public.invoice_payments (id, created_at, updated_at, deleted_at, invoice_id, payment_date, payment_amount, payment_method, date_created) VALUES (14, '2026-05-14 13:16:46.845969+00', '2026-05-14 13:16:46.845969+00', NULL, 258, '2026-04-11', 34692500, 'TRANSFER', '2026-05-14 13:16:46.845947+00');
INSERT INTO public.invoice_payments (id, created_at, updated_at, deleted_at, invoice_id, payment_date, payment_amount, payment_method, date_created) VALUES (15, '2026-05-15 04:10:44.342109+00', '2026-05-15 04:10:44.342109+00', NULL, 135, '2026-05-15', 14835000, 'TRANSFER', '2026-05-15 04:10:44.340903+00');
INSERT INTO public.invoice_payments (id, created_at, updated_at, deleted_at, invoice_id, payment_date, payment_amount, payment_method, date_created) VALUES (16, '2026-05-15 08:29:57.42802+00', '2026-05-15 08:29:57.42802+00', NULL, 259, '2026-05-15', 80393000, 'TRANSFER', '2026-05-15 08:29:57.427999+00');
INSERT INTO public.invoice_payments (id, created_at, updated_at, deleted_at, invoice_id, payment_date, payment_amount, payment_method, date_created) VALUES (17, '2026-05-15 14:08:45.04399+00', '2026-05-15 14:08:45.04399+00', NULL, 262, '2026-05-14', 20845000, 'TRANSFER', '2026-05-15 14:08:45.043966+00');
INSERT INTO public.invoice_payments (id, created_at, updated_at, deleted_at, invoice_id, payment_date, payment_amount, payment_method, date_created) VALUES (18, '2026-05-22 07:03:23.242156+00', '2026-05-22 07:03:23.242156+00', NULL, 109, '2026-05-21', 31700000, 'TRANSFER', '2026-05-22 07:03:23.242128+00');
INSERT INTO public.invoice_payments (id, created_at, updated_at, deleted_at, invoice_id, payment_date, payment_amount, payment_method, date_created) VALUES (19, '2026-05-23 04:08:40.590149+00', '2026-05-23 04:08:40.590149+00', NULL, 262, '2026-05-19', 14000000, 'TRANSFER', '2026-05-23 04:08:40.590123+00');
INSERT INTO public.invoice_payments (id, created_at, updated_at, deleted_at, invoice_id, payment_date, payment_amount, payment_method, date_created) VALUES (21, '2026-05-23 09:24:27.842882+00', '2026-05-23 09:24:27.842882+00', NULL, 138, '2026-05-23', 15039000, 'Borongan TRANSFER', '2026-05-23 09:24:27.832923+00');
INSERT INTO public.invoice_payments (id, created_at, updated_at, deleted_at, invoice_id, payment_date, payment_amount, payment_method, date_created) VALUES (22, '2026-05-23 09:24:27.867209+00', '2026-05-23 09:24:27.867209+00', NULL, 202, '2026-05-23', 3506000, 'Borongan TRANSFER', '2026-05-23 09:24:27.832923+00');
INSERT INTO public.invoice_payments (id, created_at, updated_at, deleted_at, invoice_id, payment_date, payment_amount, payment_method, date_created) VALUES (4, '2026-05-04 18:31:08.375648+00', '2026-05-04 18:31:08.375648+00', '2026-05-26 03:33:29.168301+00', 207, '2026-05-04', 20000000, 'Borongan TRANSFER', '2026-05-04 18:31:08.363764+00');
INSERT INTO public.invoice_payments (id, created_at, updated_at, deleted_at, invoice_id, payment_date, payment_amount, payment_method, date_created) VALUES (20, '2026-05-23 04:10:40.135258+00', '2026-05-23 04:10:40.135258+00', '2026-05-26 03:33:29.419095+00', 262, '2026-05-23', 30000000, 'TRANSFER', '2026-05-23 04:10:40.135244+00');
INSERT INTO public.invoice_payments (id, created_at, updated_at, deleted_at, invoice_id, payment_date, payment_amount, payment_method, date_created) VALUES (23, '2026-05-26 03:35:50.559026+00', '2026-05-26 03:35:50.559026+00', NULL, 262, '2026-05-23', 20000000, 'TRANSFER', '2026-05-26 03:35:50.558988+00');
INSERT INTO public.invoice_payments (id, created_at, updated_at, deleted_at, invoice_id, payment_date, payment_amount, payment_method, date_created) VALUES (24, '2026-05-26 03:39:01.780922+00', '2026-05-26 03:39:01.780922+00', NULL, 262, '2026-05-26', 13630000, 'TRANSFER', '2026-05-26 03:39:01.780884+00');
INSERT INTO public.invoice_payments (id, created_at, updated_at, deleted_at, invoice_id, payment_date, payment_amount, payment_method, date_created) VALUES (25, '2026-05-26 08:35:52.843083+00', '2026-05-26 08:35:52.843083+00', NULL, 268, '2026-05-12', 700000, 'TRANSFER', '2026-05-12 08:35:52.837802+00');
INSERT INTO public.invoice_payments (id, created_at, updated_at, deleted_at, invoice_id, payment_date, payment_amount, payment_method, date_created) VALUES (26, '2026-05-27 07:17:54.402533+00', '2026-05-27 07:17:54.402533+00', NULL, 269, '2026-05-27', 2400000, 'TRANSFER', '2026-05-27 07:17:54.402497+00');


--
-- Data for Name: invoices; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (132, '2026-04-13 11:58:55.230703+00', '2026-04-14 06:33:48.253745+00', NULL, NULL, 'BMP-0426-008', '2026-04-13 00:00:00+00', '14 days', 'PAID', NULL, 15, 'ba484592d050', 'bmp-0426-008-ba484592d050', '2026-04-13 11:58:55.230703+00', '2026-04-14 06:33:48.253745+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (181, '2026-04-20 04:33:27.228529+00', '2026-04-20 04:35:57.155324+00', NULL, 'april', 'BMP-0426-031', '2026-04-06 00:00:00+00', '14 days', 'PAID', '', 8, 'c1769dd67f90', 'bmp-0426-031-c1769dd67f90', '2026-04-20 04:33:27.228529+00', '2026-04-20 04:35:57.155324+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (183, '2026-04-20 04:41:06.4155+00', '2026-04-20 04:44:21.722343+00', NULL, 'april', 'BMP-0426-033', '2026-04-06 00:00:00+00', '14 days', 'PAID', '', 8, 'f6c232361901', 'bmp-0426-033-f6c232361901', '2026-04-20 04:41:06.4155+00', '2026-04-20 04:44:21.722343+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (167, '2026-04-17 08:48:50.179304+00', '2026-04-20 05:02:58.433431+00', NULL, 'wiranto', 'BMP-0426-022', '2026-02-18 00:00:00+00', '14 days', 'PAID', '', 14, '0689e7a5ca37', 'bmp-0426-022-0689e7a5ca37', '2026-04-17 08:48:50.179304+00', '2026-04-20 05:02:58.433431+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (112, '2026-04-13 06:20:24.784067+00', '2026-04-15 02:09:30.363036+00', NULL, 'wiranto', 'BMP-0426-004', '2026-03-09 00:00:00+00', '14 days', 'PAID', 'jatuh tempo 09/03/2026', 14, 'f22c2645b4ca', 'bmp-0426-004-f22c2645b4ca', '2026-04-13 06:20:24.784067+00', '2026-04-15 02:09:30.363036+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (155, '2026-04-16 11:13:30.484714+00', '2026-04-16 11:15:41.821922+00', NULL, 'wiranto', 'BMP-0426-014', '2026-03-03 00:00:00+00', '14 days', 'PAID', '', 14, 'c229bb5b2901', 'bmp-0426-014-c229bb5b2901', '2026-04-16 11:13:30.484714+00', '2026-04-16 11:15:41.821922+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (184, '2026-04-20 05:04:11.706662+00', '2026-04-20 05:10:51.448883+00', NULL, 'april', 'BMP-0426-034', '2026-02-23 00:00:00+00', '14 days', 'PAID', '', 14, '2957164f1bd3', 'bmp-0426-034-2957164f1bd3', '2026-04-20 05:04:11.706662+00', '2026-04-20 05:10:51.448883+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (185, '2026-04-20 05:11:52.364872+00', '2026-04-20 05:13:40.6995+00', NULL, 'april', 'BMP-0426-035', '2026-02-12 00:00:00+00', '14 days', 'PAID', '', 14, '00c699cad969', 'bmp-0426-035-00c699cad969', '2026-04-20 05:11:52.364872+00', '2026-04-20 05:13:40.6995+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (186, '2026-04-20 05:13:59.365191+00', '2026-04-20 05:17:41.570543+00', NULL, 'april', 'BMP-0426-036', '2026-02-12 00:00:00+00', '14 days', 'PAID', '', 14, 'f5567849cceb', 'bmp-0426-036-f5567849cceb', '2026-04-20 05:13:59.365191+00', '2026-04-20 05:17:41.570543+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (187, '2026-04-20 05:18:46.742827+00', '2026-04-20 05:20:58.332284+00', NULL, 'april', 'BMP-0426-037', '2026-02-28 00:00:00+00', '14 days', 'PAID', '', 14, 'c824f3289e0e', 'bmp-0426-037-c824f3289e0e', '2026-04-20 05:18:46.742827+00', '2026-04-20 05:20:58.332284+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (188, '2026-04-20 05:25:10.849641+00', '2026-04-20 05:26:54.920487+00', NULL, 'april', 'BMP-0426-038', '2026-03-10 00:00:00+00', '14 days', 'PAID', '', 14, '41f87ea14ebc', 'bmp-0426-038-41f87ea14ebc', '2026-04-20 05:25:10.849641+00', '2026-04-20 05:26:54.920487+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (190, '2026-04-20 05:38:22.786717+00', '2026-04-20 05:42:13.400867+00', NULL, 'April', 'BMP-0426-039', '2026-03-10 00:00:00+00', '14 days', 'PAID', '', 20, 'afbbd75f647b', 'bmp-0426-039-afbbd75f647b', '2026-04-20 05:38:22.786717+00', '2026-04-20 05:42:13.400867+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (191, '2026-04-20 05:47:39.781002+00', '2026-04-20 05:48:49.211048+00', NULL, 'april', 'BMP-0426-040', '2026-03-13 00:00:00+00', '14 days', 'PAID', '', 14, '5813288498f9', 'bmp-0426-040-5813288498f9', '2026-04-20 05:47:39.781002+00', '2026-04-20 05:48:49.211048+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (156, '2026-04-16 11:17:13.082539+00', '2026-04-16 11:19:08.763406+00', NULL, 'wiranto', 'BMP-0426-015', '2026-03-04 00:00:00+00', '14 days', 'PAID', '', 14, '9a1df5ac9fb8', 'bmp-0426-015-9a1df5ac9fb8', '2026-04-16 11:17:13.082539+00', '2026-04-16 11:19:08.763406+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (114, '2026-04-13 08:02:57.672657+00', '2026-04-24 05:49:23.838821+00', NULL, 'maret', 'BMP-0426-006', '2026-04-03 00:00:00+00', '14 days', 'PAID', 'jatuh tempo 03/04/2026', 14, '87902a8333fb', 'bmp-0426-006-87902a8333fb', '2026-04-13 08:02:57.672657+00', '2026-04-24 05:49:23.838821+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (150, '2026-04-15 10:33:33.203889+00', '2026-04-15 10:56:55.86398+00', NULL, 'ok', 'BMP-0426-013', '2026-03-02 00:00:00+00', '14 days', 'PAID', '', 17, '4626b369ace2', 'bmp-0426-013-4626b369ace2', '2026-04-15 10:33:33.203889+00', '2026-04-15 10:56:55.86398+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (192, '2026-04-20 05:48:55.721047+00', '2026-04-20 05:50:42.759251+00', NULL, 'april', 'BMP-0426-041', '2026-03-14 00:00:00+00', '14 days', 'PAID', '', 21, '6dbc817617bf', 'bmp-0426-041-6dbc817617bf', '2026-04-20 05:48:55.721047+00', '2026-04-20 05:50:42.759251+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (193, '2026-04-20 05:55:52.237317+00', '2026-04-20 05:56:37.946317+00', NULL, 'april', 'BMP-0426-042', '2026-03-14 00:00:00+00', '14 days', 'PAID', '', 15, '557f407bc957', 'bmp-0426-042-557f407bc957', '2026-04-20 05:55:52.237317+00', '2026-04-20 05:56:37.946317+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (194, '2026-04-20 06:30:49.863636+00', '2026-04-20 06:32:46.208595+00', NULL, 'April', 'BMP-0426-043', '2026-03-14 00:00:00+00', '14 days', 'PAID', '', 22, '965a80dd232f', 'bmp-0426-043-965a80dd232f', '2026-04-20 06:30:49.863636+00', '2026-04-20 06:32:46.208595+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (158, '2026-04-16 11:20:27.756609+00', '2026-04-16 11:21:31.350582+00', NULL, 'ko hary', 'BMP-0426-016', '2026-03-05 00:00:00+00', '14 days', 'PAID', '', 18, '8bafdd83eca9', 'bmp-0426-016-8bafdd83eca9', '2026-04-16 11:20:27.756609+00', '2026-04-16 11:21:31.350582+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (160, '2026-04-17 07:01:41.9626+00', '2026-04-17 07:03:38.037662+00', NULL, 'mas kolis', 'BMP-0426-017', '2026-02-27 00:00:00+00', '14 days', 'PAID', '', 8, 'c5342542eb13', 'bmp-0426-017-c5342542eb13', '2026-04-17 07:01:41.9626+00', '2026-04-17 07:03:38.037662+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (172, '2026-04-20 01:19:53.550281+00', '2026-04-20 06:55:42.211155+00', NULL, 'april', 'BMP-0426-025', '2026-04-20 00:00:00+00', '14 days', 'PAID', '', 19, '550f8bbf3c75', 'bmp-0426-025-550f8bbf3c75', '2026-04-20 01:19:53.550281+00', '2026-04-20 06:55:42.211155+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (195, '2026-04-20 06:33:23.235514+00', '2026-04-20 11:44:34.312561+00', NULL, 'April', 'BMP-0426-044', '2026-03-14 00:00:00+00', '14 days', 'PAID', '', 23, '93b3381f7d46', 'bmp-0426-044-93b3381f7d46', '2026-04-20 06:33:23.235514+00', '2026-04-20 11:44:34.312561+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (149, '2026-04-15 10:29:37.716192+00', '2026-04-17 08:35:23.268428+00', NULL, 'kolis', 'BMP-0426-012', '2026-03-02 00:00:00+00', '14 days', 'PAID', '', 8, '6650e0ec148c', 'bmp-0426-012-6650e0ec148c', '2026-04-15 10:29:37.716192+00', '2026-04-17 08:35:23.268428+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (163, '2026-04-17 08:35:46.073397+00', '2026-04-17 08:38:33.969204+00', NULL, 'desember', 'BMP-0426-018', '2025-12-22 00:00:00+00', '14 days', 'PAID', '', 12, '949eed424074', 'bmp-0426-018-949eed424074', '2026-04-17 08:35:46.073397+00', '2026-04-17 08:38:33.969204+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (196, '2026-04-20 11:45:34.424431+00', '2026-04-20 11:50:53.454465+00', NULL, 'April', 'BMP-0426-045', '2026-03-24 00:00:00+00', '14 days', 'PAID', '', 17, 'd4cb08234888', 'bmp-0426-045-d4cb08234888', '2026-04-20 11:45:34.424431+00', '2026-04-20 11:50:53.454465+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (164, '2026-04-17 08:39:17.632283+00', '2026-04-17 08:40:24.045339+00', NULL, 'ali', 'BMP-0426-019', '2026-01-08 00:00:00+00', '14 days', 'PAID', '', 12, 'be9dc85c37fd', 'bmp-0426-019-be9dc85c37fd', '2026-04-17 08:39:17.632283+00', '2026-04-17 08:40:24.045339+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (165, '2026-04-17 08:40:56.608314+00', '2026-04-17 08:43:05.573505+00', NULL, 'ali', 'BMP-0426-020', '2026-04-17 00:00:00+00', '14 days', 'PAID', '', 12, '0c0a34480e9d', 'bmp-0426-020-0c0a34480e9d', '2026-04-17 08:40:56.608314+00', '2026-04-17 08:43:05.573505+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (197, '2026-04-20 11:51:35.6128+00', '2026-04-20 11:58:34.720481+00', NULL, 'Maret', 'BMP-0426-046', '2026-03-29 00:00:00+00', '14 days', 'PAID', '', 24, '0cf8359599e5', 'bmp-0426-046-0cf8359599e5', '2026-04-20 11:51:35.6128+00', '2026-04-20 11:58:34.720481+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (166, '2026-04-17 08:44:35.937657+00', '2026-04-17 08:45:41.924406+00', NULL, 'kolis', 'BMP-0426-021', '2026-03-05 00:00:00+00', '14 days', 'PAID', '', 8, 'd8843b58024c', 'bmp-0426-021-d8843b58024c', '2026-04-17 08:44:35.937657+00', '2026-04-17 08:45:41.924406+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (198, '2026-04-20 12:03:38.904141+00', '2026-04-20 12:09:47.929137+00', NULL, 'April', 'BMP-0426-047', '2026-04-09 00:00:00+00', '14 days', 'PAID', '', 19, '824916832967', 'bmp-0426-047-824916832967', '2026-04-20 12:03:38.904141+00', '2026-04-20 12:09:47.929137+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (169, '2026-04-18 01:51:36.411162+00', '2026-04-20 01:07:13.280727+00', NULL, 'kolis', 'BMP-0426-023', '2026-05-02 00:00:00+00', '14 days', 'PAID', 'Jatuh Tempo 02/05/2026', 8, '4e51b15a3f2d', 'bmp-0426-023-4e51b15a3f2d', '2026-04-18 01:51:36.411162+00', '2026-04-20 01:07:13.280727+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (173, '2026-04-20 01:34:33.625611+00', '2026-04-20 01:36:24.103774+00', NULL, 'maret', 'BMP-0426-026', '2026-04-01 00:00:00+00', '14 days', 'PAID', '', 8, '585a7d38a91a', 'bmp-0426-026-585a7d38a91a', '2026-04-20 01:34:33.625611+00', '2026-04-20 01:36:24.103774+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (174, '2026-04-20 01:36:57.140507+00', '2026-04-20 01:38:56.133739+00', NULL, 'maret', 'BMP-0426-027', '2026-03-07 00:00:00+00', '14 days', 'PAID', '', 8, '3a1cb0e22771', 'bmp-0426-027-3a1cb0e22771', '2026-04-20 01:36:57.140507+00', '2026-04-20 01:38:56.133739+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (175, '2026-04-20 01:39:23.885121+00', '2026-04-20 01:40:46.262196+00', NULL, 'maret', 'BMP-0426-028', '2026-03-11 00:00:00+00', '14 days', 'PAID', '', 8, '13ec3348f9b4', 'bmp-0426-028-13ec3348f9b4', '2026-04-20 01:39:23.885121+00', '2026-04-20 01:40:46.262196+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (203, '2026-04-23 01:39:03.785473+00', '2026-04-23 01:40:46.818486+00', NULL, 'April', 'BMP-0426-051', '2026-04-23 00:00:00+00', '14 days', 'OVERDUE', '', 25, '0c9d954f1b9b', 'bmp-0426-051-0c9d954f1b9b', '2026-04-23 01:39:03.785473+00', '2026-04-23 01:40:46.818486+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (134, '2026-04-14 05:12:13.242555+00', '2026-04-20 01:59:57.471339+00', NULL, NULL, 'BMP-0426-009', '2026-04-11 00:00:00+00', '14 days', 'PAID', NULL, 16, '0d595fd9ce98', 'bmp-0426-009-0d595fd9ce98', '2026-04-14 05:12:13.242555+00', '2026-04-20 01:59:57.471339+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (135, '2026-04-14 05:54:46.856172+00', '2026-05-15 04:10:44.379207+00', NULL, '', 'BMP-0426-010', '2026-04-08 00:00:00+00', '14 days', 'PAID', '', 14, 'eb614695dac1', 'bmp-0426-010-eb614695dac1', '2026-04-14 05:54:46.856172+00', '2026-04-24 05:49:23.847196+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (170, '2026-04-19 10:10:05.301931+00', '2026-05-05 13:45:28.732612+00', NULL, 'april', 'BMP-0426-024', '2026-05-18 00:00:00+00', '30 days', 'PAID', 'Jatuh Tempo 18/05/2026', 16, '7e03092e65a8', 'bmp-0426-024-7e03092e65a8', '2026-04-19 10:10:05.301931+00', '2026-04-19 10:28:56.836127+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (115, '2026-04-13 08:26:21.098051+00', '2026-05-13 13:28:55.294473+00', NULL, 'maret', 'BMP-0426-007', '2026-04-04 00:00:00+00', '14 days', 'PAID', 'jatuh tempo 04/04/2026', 12, 'b6327ab2bc52', 'bmp-0426-007-b6327ab2bc52', '2026-04-13 08:26:21.098051+00', '2026-04-13 08:34:14.434853+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (182, '2026-04-20 04:36:27.502119+00', '2026-05-14 13:04:16.528479+00', NULL, 'april', 'BMP-0426-032', '2026-04-06 00:00:00+00', '14 days', 'UNPAID', '', 8, 'fd9bd5ccae75', 'bmp-0426-032-fd9bd5ccae75', '2026-04-20 04:36:27.502119+00', '2026-04-20 04:40:16.101591+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (109, '2026-04-13 01:31:08.38803+00', '2026-05-22 07:03:23.273346+00', NULL, 'maret', 'BMP-0426-001', '2026-03-05 00:00:00+00', '14 days', 'PAID', 'jatuh tempo tanggal 05/03/2026', 12, 'b1b1618544ab', 'bmp-0426-001-b1b1618544ab', '2026-04-13 01:31:08.38803+00', '2026-04-13 04:10:53.172034+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (138, '2026-04-15 01:28:38.455407+00', '2026-05-23 09:24:27.85469+00', NULL, '', 'BMP-0426-011', '2026-04-15 00:00:00+00', '14 days', 'PAID', '', 14, 'efb5b9e360c4', 'bmp-0426-011-efb5b9e360c4', '2026-04-15 01:28:38.455407+00', '2026-04-15 02:14:38.129807+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (180, '2026-04-20 04:21:27.699106+00', '2026-05-29 15:22:02.758258+00', NULL, 'april', 'BMP-0426-030', '2026-04-11 00:00:00+00', '14 days', 'UNPAID', '', 8, '7ed262bd1c5a', 'bmp-0426-030-7ed262bd1c5a', '2026-04-20 04:21:27.699106+00', '2026-04-20 04:30:38.331392+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (179, '2026-04-20 02:00:18.659115+00', '2026-04-20 02:02:09.019398+00', NULL, 'maret', 'BMP-0426-029', '2026-03-07 00:00:00+00', '14 days', 'PAID', '', 16, 'ad411ec1303a', 'bmp-0426-029-ad411ec1303a', '2026-04-20 02:00:18.659115+00', '2026-04-20 02:02:09.019398+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (113, '2026-04-13 06:42:37.684886+00', '2026-04-24 05:49:23.831387+00', NULL, 'februari', 'BMP-0426-005', '2026-04-02 00:00:00+00', '14 days', 'PAID', 'jatuh tempo 02/04/2026', 14, '0a50b6ceefc5', 'bmp-0426-005-0a50b6ceefc5', '2026-04-13 06:42:37.684886+00', '2026-04-24 05:49:23.831387+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (110, '2026-04-13 04:03:54.624887+00', '2026-04-27 08:53:52.862041+00', NULL, 'februari', 'BMP-0426-002', '2026-02-26 00:00:00+00', '14 days', 'PAID', 'jatuh tempo 26/02/2026', 12, 'aa7ed98b7600', 'bmp-0426-002-aa7ed98b7600', '2026-04-13 04:03:54.624887+00', '2026-04-27 08:53:52.862041+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (200, '2026-04-21 09:43:06.159773+00', '2026-04-28 07:53:51.36573+00', NULL, 'april', 'BMP-0426-048', '2026-04-28 00:00:00+00', '14 days', 'PAID', '', 17, '4cbc1cd3a1d8', 'bmp-0426-048-4cbc1cd3a1d8', '2026-04-21 09:43:06.159773+00', '2026-04-28 07:53:51.36573+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (205, '2026-04-24 05:51:51.955767+00', '2026-04-24 05:52:57.998408+00', NULL, 'April', 'BMP-0426-052', '2026-04-24 00:00:00+00', '14 days', 'PAID', '', 14, '0aed2dbac456', 'bmp-0426-052-0aed2dbac456', '2026-04-24 05:51:51.955767+00', '2026-04-24 05:52:57.998408+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (206, '2026-04-27 02:51:29.664357+00', '2026-04-27 03:01:50.762873+00', NULL, 'April', 'BMP-0426-053', '2026-05-09 00:00:00+00', '14 days', 'UNPAID', '', 23, '4f44ed5e1757', 'bmp-0426-053-4f44ed5e1757', '2026-04-27 02:51:29.664357+00', '2026-04-27 03:01:50.762873+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (210, '2026-04-28 03:06:40.300615+00', '2026-04-28 03:09:55.24913+00', NULL, 'april', 'BMP-0426-056', '2026-05-05 00:00:00+00', '14 days', 'UNPAID', '', 26, '0ecaeb93fc2a', 'bmp-0426-056-0ecaeb93fc2a', '2026-04-28 03:06:40.300615+00', '2026-04-28 03:09:55.24913+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (239, '2026-05-01 15:35:20.294007+00', '2026-05-01 15:35:20.294007+00', '2026-05-01 15:38:55.181273+00', 'Faktur Penjualan', 'BMP-2605-001', '2026-05-31 00:00:00+00', '14 days', 'UNPAID', '', 12, '461bf0aa', 'BMP-2605-001-461bf0aa', '2026-05-01 15:35:20.285855+00', '2026-05-01 15:35:20.285855+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (247, '2026-05-02 01:10:56.034599+00', '2026-05-02 01:10:56.034599+00', '2026-05-02 01:11:44.651727+00', 'Faktur Penjualan', 'BMP-2605-001', '2026-06-01 00:00:00+00', '14 days', 'UNPAID', '', 12, '51d69bd6', 'BMP-2605-001-51d69bd6', '2026-05-02 01:10:56.007965+00', '2026-05-02 01:10:56.007965+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (249, '2026-05-02 01:14:05.024981+00', '2026-05-02 01:14:05.024981+00', '2026-05-02 07:59:33.943401+00', 'Faktur Penjualan', 'BMP-2605-001', '2026-05-09 00:00:00+00', '14 days', 'UNPAID', '', 7, 'e0fc2445', 'BMP-2605-001-e0fc2445', '2026-05-02 01:14:05.018307+00', '2026-05-02 01:14:05.018307+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (251, '2026-05-04 18:25:53.261138+00', '2026-05-13 06:57:08.78228+00', NULL, 'Faktur Penjualan', 'BMP-2605-004', '2026-05-03 00:00:00+00', '14 days', 'PAID', '', 16, '9e707f6b', 'BMP-2605-004-9e707f6b', '2026-05-04 18:25:53.252525+00', '2026-05-04 18:25:53.252525+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (255, '2026-05-05 13:49:45.33981+00', '2026-05-13 06:57:12.167937+00', NULL, 'Faktur Penjualan', 'BMP-2605-006', '2026-06-04 00:00:00+00', '14 days', 'UNPAID', '', 35, '35ca2747', 'BMP-2605-006-35ca2747', '2026-05-05 13:49:45.331168+00', '2026-05-05 13:49:45.331168+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (256, '2026-05-09 05:35:55.040462+00', '2026-05-13 06:57:13.49137+00', NULL, 'Faktur Penjualan', 'BMP-2605-007', '2026-06-08 00:00:00+00', '14 days', 'UNPAID', '', 12, 'dd96ce2c', 'BMP-2605-007-dd96ce2c', '2026-05-09 05:35:55.035597+00', '2026-05-09 05:35:55.035597+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (262, '2026-05-15 14:07:39.335324+00', '2026-05-26 03:39:02.406663+00', NULL, 'Faktur Penjualan', 'BMP-2605-013', '2026-05-29 00:00:00+00', '14 days', 'PAID', '', 24, '4d43d330', 'BMP-2605-013-4d43d330', '2026-05-10 00:00:00+00', '2026-05-15 14:07:39.331408+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (263, '2026-05-22 04:25:42.696868+00', '2026-05-22 04:25:42.696868+00', NULL, 'Faktur Penjualan', 'BMP-2605-014', '2026-06-05 00:00:00+00', '14 days', 'UNPAID', '', 24, '13b59092', 'BMP-2605-014-13b59092', '2026-05-22 00:00:00+00', '2026-05-22 04:25:42.692787+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (211, '2026-05-01 08:19:25.28318+00', '2026-05-04 18:28:49.975752+00', NULL, 'Faktur Penjualan', 'BMP-2605-001', '2026-05-08 00:00:00+00', '14 days', 'PAID', '', 27, '037c4106', '', '2026-05-01 08:19:25.270454+00', '2026-05-01 08:19:25.270454+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (257, '2026-05-14 01:52:21.013767+00', '2026-05-26 09:06:27.597589+00', NULL, 'Faktur Penjualan', 'BMP-2605-008', '2026-06-13 00:00:00+00', '14 days', 'PARTIAL', '', 14, 'b971b6d0', 'BMP-2605-008-b971b6d0', '2026-05-01 00:00:00+00', '2026-05-14 01:52:21.009903+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (209, '2026-04-28 01:44:29.088876+00', '2026-05-05 02:38:14.580363+00', NULL, 'april', 'BMP-0426-055', '2026-04-28 00:00:00+00', '14 days', 'PAID', '', 8, '465eaf53d49d', 'bmp-0426-055-465eaf53d49d', '2026-04-28 01:44:29.088876+00', '2026-04-28 01:46:32.727978+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (252, '2026-05-05 09:49:58.51082+00', '2026-05-14 01:45:18.505999+00', NULL, 'Faktur Penjualan', 'BMP-2605-005', '2026-05-31 00:00:00+00', '14 days', 'PAID', '', 14, '25268dae', 'BMP-2605-005-25268dae', '2026-05-05 09:49:58.501189+00', '2026-05-05 09:49:58.501189+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (111, '2026-04-13 04:45:02.023171+00', '2026-05-05 13:42:46.44072+00', NULL, 'februari', 'BMP-0426-003', '2026-02-28 00:00:00+00', '14 days', 'PAID', 'jatuh tempo 28/02/2026', 13, '217f12bce51b', 'bmp-0426-003-217f12bce51b', '2026-04-13 04:45:02.023171+00', '2026-04-13 06:14:03.076882+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (254, '2026-05-05 13:35:14.856972+00', '2026-05-05 13:35:14.856972+00', '2026-05-05 13:49:12.644752+00', 'Faktur Penjualan', 'BMP-2605-004', '2026-05-19 00:00:00+00', '14 days', 'UNPAID', '', 35, '18ed465c', 'BMP-2605-004-18ed465c', '2026-05-05 13:35:14.845452+00', '2026-05-05 13:35:14.845452+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (253, '2026-05-05 10:26:59.30431+00', '2026-05-05 13:47:32.67725+00', '2026-05-05 15:49:22.086973+00', 'Faktur Penjualan', 'BMP-2605-003', '2026-05-19 00:00:00+00', '14 days', 'PAID', '', 24, '95768d12', 'BMP-2605-003-95768d12', '2026-05-05 10:26:59.293953+00', '2026-05-05 10:26:59.293953+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (248, '2026-05-02 01:13:22.504017+00', '2026-05-13 06:57:06.04413+00', NULL, 'Faktur Penjualan', 'BMP-2605-002', '2026-06-01 00:00:00+00', '14 days', 'UNPAID', '', 12, '7ce5ac61', 'BMP-2605-002-7ce5ac61', '2026-05-02 01:13:22.497607+00', '2026-05-02 01:13:22.497607+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (250, '2026-05-02 09:00:45.574609+00', '2026-05-13 06:57:07.560953+00', NULL, 'Faktur Penjualan', 'BMP-2605-003', '2026-05-16 00:00:00+00', '14 days', 'PAID', '', 8, 'd1aebb79', 'BMP-2605-003-d1aebb79', '2026-05-02 09:00:45.565751+00', '2026-05-02 09:00:45.565751+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (258, '2026-05-14 13:15:37.85645+00', '2026-05-14 13:16:46.880813+00', NULL, 'Faktur Penjualan', 'BMP-2605-009', '2026-02-15 00:00:00+00', '14 days', 'PAID', '', 12, '6bea2397', 'BMP-2605-009-6bea2397', '2026-04-11 00:00:00+00', '2026-05-14 13:15:37.852218+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (260, '2026-05-15 00:31:27.0279+00', '2026-05-15 01:27:26.297383+00', NULL, 'Faktur Penjualan', 'BMP-2605-011', '2026-05-29 00:00:00+00', '14 days', 'UNPAID', '', 35, '51099424', 'BMP-2605-011-51099424', '2026-05-15 00:00:00+00', '2026-05-15 00:31:27.021778+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (264, '2026-05-22 04:31:06.961129+00', '2026-05-22 04:32:37.194395+00', NULL, 'Faktur Penjualan', 'BMP-2605-015', '2026-06-05 00:00:00+00', '14 days', 'UNPAID', '', 21, '88bb7619', 'BMP-2605-015-88bb7619', '2026-05-22 00:00:00+00', '2026-05-22 04:31:06.957194+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (265, '2026-05-22 07:05:15.770151+00', '2026-05-22 07:05:15.770151+00', NULL, 'Faktur Penjualan', 'BMP-2605-016', '2026-06-02 00:00:00+00', '14 days', 'UNPAID', '', 36, '3df6596f', 'BMP-2605-016-3df6596f', '2026-05-19 00:00:00+00', '2026-05-22 07:05:15.766249+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (270, '2026-05-30 02:00:44.531468+00', '2026-05-30 02:01:22.190116+00', NULL, 'Faktur Penjualan', 'BMP-2605-018', '2026-06-13 00:00:00+00', '14 days', 'UNPAID', '', 8, '915e3416', 'BMP-2605-018-915e3416', '2026-05-30 00:00:00+00', '2026-05-30 02:00:44.526207+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (259, '2026-05-14 13:35:43.491948+00', '2026-05-26 09:10:18.713521+00', NULL, 'Faktur Penjualan', 'BMP-2605-010', '2026-05-06 00:00:00+00', '14 days', 'PAID', '', 17, 'c94477e0', 'BMP-2605-010-c94477e0', '2026-05-14 00:00:00+00', '2026-05-14 13:35:43.487723+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (261, '2026-05-15 02:35:08.347501+00', '2026-05-15 10:23:08.079809+00', NULL, 'Faktur Penjualan', 'BMP-2605-012', '2026-06-14 00:00:00+00', '14 days', 'UNPAID', '', 14, '006f3869', 'BMP-2605-012-006f3869', '2026-05-15 00:00:00+00', '2026-05-15 02:35:08.345126+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (202, '2026-04-21 13:49:37.440778+00', '2026-05-23 09:24:27.873107+00', NULL, 'april', 'BMP-0426-050', '2026-05-05 00:00:00+00', '14 days', 'PARTIAL', 'Jatuh Tempo 05/05/2026', 14, 'a0c9af198361', 'bmp-0426-050-a0c9af198361', '2026-04-21 13:49:37.440778+00', '2026-04-21 13:51:42.887029+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (266, '2026-05-25 04:32:42.654245+00', '2026-05-25 05:07:35.644933+00', NULL, 'Faktur Penjualan', 'BMP-2605-017', '2026-06-24 00:00:00+00', '14 days', 'UNPAID', '', 12, '00c25e20', 'BMP-2605-017-00c25e20', '2026-05-25 00:00:00+00', '2026-05-25 04:32:42.649276+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (207, '2026-04-27 03:33:28.906492+00', '2026-05-26 03:33:29.920341+00', NULL, 'april', 'BMP-0426-054', '2026-05-03 00:00:00+00', '14 days', 'PARTIAL', '', 24, '065702d819fa', 'bmp-0426-054-065702d819fa', '2026-04-27 03:33:28.906492+00', '2026-04-27 03:41:33.243666+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (269, '2026-05-27 07:16:23.409304+00', '2026-05-27 07:17:54.420493+00', NULL, 'Faktur Penjualan', 'BMP-2605-090', '2026-06-10 00:00:00+00', '14 days', 'PAID', '', 37, '4ad598bc', 'BMP-2605-090-4ad598bc', '2026-05-27 00:00:00+00', '2026-05-27 07:16:23.405272+00', true);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (267, '2026-05-26 08:35:52.734426+00', '2026-05-26 08:35:52.734426+00', NULL, 'Demo - Pembelian Kantong Plastik HDPE', 'DEMO-INV-001', '2026-06-02 08:35:52.729354+00', '14 days', 'UNPAID', 'Ini adalah faktur contoh untuk akun demo. Data ini tidak mempengaruhi data produksi.', 37, '2e4944eb', 'DEMO-INV-001-2e4944eb', '2026-05-21 08:35:52.72936+00', '2026-05-26 08:35:52.72936+00', false);
INSERT INTO public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) VALUES (268, '2026-05-26 08:35:52.793859+00', '2026-05-26 08:35:52.793859+00', NULL, 'Demo - Pengiriman Cup PP Reguler', 'DEMO-INV-002', '2026-05-21 08:35:52.788567+00', 'COD', 'PAID', 'Faktur demo sudah lunas.', 38, '1b7f6943', 'DEMO-INV-002-1b7f6943', '2026-05-11 08:35:52.788572+00', '2026-05-26 08:35:52.788572+00', false);


--
-- Data for Name: machine_bonus_logs; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.machine_bonus_logs (id, created_at, updated_at, deleted_at, employee_id, machine_name, shift_type, bonus_amount, date, jumlah_perolehan, is_demo) VALUES (1, '2026-05-15 08:33:23.401385+00', '2026-05-15 08:33:23.401385+00', NULL, 22, 'Baskom Mawar', 'Siang', 5000.00, '2026-05-15', 0, false);
INSERT INTO public.machine_bonus_logs (id, created_at, updated_at, deleted_at, employee_id, machine_name, shift_type, bonus_amount, date, jumlah_perolehan, is_demo) VALUES (3, '2026-05-16 10:34:10.530912+00', '2026-05-16 10:34:10.530912+00', NULL, 22, 'Baskom Panda', 'Pagi', 10000.00, '2026-05-16', 0, false);
INSERT INTO public.machine_bonus_logs (id, created_at, updated_at, deleted_at, employee_id, machine_name, shift_type, bonus_amount, date, jumlah_perolehan, is_demo) VALUES (4, '2026-05-17 05:59:50.221989+00', '2026-05-17 05:59:50.221989+00', NULL, 22, 'Baskom Panda', 'Pagi', 10000.00, '2026-05-17', 0, false);
INSERT INTO public.machine_bonus_logs (id, created_at, updated_at, deleted_at, employee_id, machine_name, shift_type, bonus_amount, date, jumlah_perolehan, is_demo) VALUES (5, '2026-05-17 06:00:24.588556+00', '2026-05-17 06:00:24.588556+00', NULL, 19, 'Baskom Panda', 'Pagi', 10000.00, '2026-05-17', 0, false);
INSERT INTO public.machine_bonus_logs (id, created_at, updated_at, deleted_at, employee_id, machine_name, shift_type, bonus_amount, date, jumlah_perolehan, is_demo) VALUES (6, '2026-05-17 06:00:34.555377+00', '2026-05-17 06:00:34.555377+00', NULL, 10, 'Baskom Panda', 'Pagi', 10000.00, '2026-05-17', 0, false);
INSERT INTO public.machine_bonus_logs (id, created_at, updated_at, deleted_at, employee_id, machine_name, shift_type, bonus_amount, date, jumlah_perolehan, is_demo) VALUES (7, '2026-05-17 06:00:43.415841+00', '2026-05-17 06:00:43.415841+00', NULL, 5, 'Baskom Panda', 'Pagi', 10000.00, '2026-05-17', 0, false);
INSERT INTO public.machine_bonus_logs (id, created_at, updated_at, deleted_at, employee_id, machine_name, shift_type, bonus_amount, date, jumlah_perolehan, is_demo) VALUES (8, '2026-05-18 17:46:55.044064+00', '2026-05-18 17:46:55.044064+00', NULL, 22, 'Wakul Moris', 'Pagi', 5000.00, '2026-05-18', 0, false);
INSERT INTO public.machine_bonus_logs (id, created_at, updated_at, deleted_at, employee_id, machine_name, shift_type, bonus_amount, date, jumlah_perolehan, is_demo) VALUES (9, '2026-05-22 09:22:26.9644+00', '2026-05-22 09:22:26.9644+00', NULL, 22, 'Baskom Panda', 'Sore', 5000.00, '2026-05-22', 50, false);
INSERT INTO public.machine_bonus_logs (id, created_at, updated_at, deleted_at, employee_id, machine_name, shift_type, bonus_amount, date, jumlah_perolehan, is_demo) VALUES (10, '2026-05-22 09:23:40.340454+00', '2026-05-22 09:23:40.340454+00', NULL, 12, 'Baskom Panda', 'Sore', 5000.00, '2026-05-22', 51, false);
INSERT INTO public.machine_bonus_logs (id, created_at, updated_at, deleted_at, employee_id, machine_name, shift_type, bonus_amount, date, jumlah_perolehan, is_demo) VALUES (11, '2026-05-22 09:24:22.728192+00', '2026-05-22 09:24:22.728192+00', NULL, 2, 'Baskom Jago', 'Sore', 7000.00, '2026-05-22', 51, false);
INSERT INTO public.machine_bonus_logs (id, created_at, updated_at, deleted_at, employee_id, machine_name, shift_type, bonus_amount, date, jumlah_perolehan, is_demo) VALUES (12, '2026-05-22 09:25:48.433287+00', '2026-05-22 09:25:48.433287+00', NULL, 13, 'BMP', 'Sore', 5000.00, '2026-05-22', 36, false);
INSERT INTO public.machine_bonus_logs (id, created_at, updated_at, deleted_at, employee_id, machine_name, shift_type, bonus_amount, date, jumlah_perolehan, is_demo) VALUES (13, '2026-05-22 09:26:20.839619+00', '2026-05-22 09:26:20.839619+00', NULL, 8, 'Wakul Moris', 'Sore', 5000.00, '2026-05-22', 60, false);
INSERT INTO public.machine_bonus_logs (id, created_at, updated_at, deleted_at, employee_id, machine_name, shift_type, bonus_amount, date, jumlah_perolehan, is_demo) VALUES (14, '2026-05-22 09:33:37.058526+00', '2026-05-22 09:33:37.058526+00', NULL, 15, 'Baskom Durian', 'Sore', 5000.00, '2026-05-22', 37, false);
INSERT INTO public.machine_bonus_logs (id, created_at, updated_at, deleted_at, employee_id, machine_name, shift_type, bonus_amount, date, jumlah_perolehan, is_demo) VALUES (15, '2026-05-22 09:34:28.190021+00', '2026-05-22 09:34:28.190021+00', NULL, 7, 'Baskom Panda', 'Sore', 5000.00, '2026-05-22', 99, false);
INSERT INTO public.machine_bonus_logs (id, created_at, updated_at, deleted_at, employee_id, machine_name, shift_type, bonus_amount, date, jumlah_perolehan, is_demo) VALUES (16, '2026-05-22 15:54:25.900092+00', '2026-05-22 15:54:25.900092+00', NULL, 17, 'Baskom Panda', 'Malam', 5000.00, '2026-05-22', 13, false);
INSERT INTO public.machine_bonus_logs (id, created_at, updated_at, deleted_at, employee_id, machine_name, shift_type, bonus_amount, date, jumlah_perolehan, is_demo) VALUES (17, '2026-05-22 15:57:47.731799+00', '2026-05-22 15:57:47.731799+00', NULL, 9, 'BMP', 'Malam', 5000.00, '2026-05-22', 10, false);
INSERT INTO public.machine_bonus_logs (id, created_at, updated_at, deleted_at, employee_id, machine_name, shift_type, bonus_amount, date, jumlah_perolehan, is_demo) VALUES (18, '2026-05-22 15:58:19.542624+00', '2026-05-22 15:58:19.542624+00', NULL, 3, 'Baskom Durian', 'Malam', 5000.00, '2026-05-22', 38, false);
INSERT INTO public.machine_bonus_logs (id, created_at, updated_at, deleted_at, employee_id, machine_name, shift_type, bonus_amount, date, jumlah_perolehan, is_demo) VALUES (19, '2026-05-22 15:58:58.507293+00', '2026-05-22 15:58:58.507293+00', NULL, 18, 'Wakul Moris', 'Malam', 5000.00, '2026-05-22', 52, false);
INSERT INTO public.machine_bonus_logs (id, created_at, updated_at, deleted_at, employee_id, machine_name, shift_type, bonus_amount, date, jumlah_perolehan, is_demo) VALUES (20, '2026-05-22 15:59:19.832466+00', '2026-05-22 15:59:19.832466+00', NULL, 23, 'Wakul Moris', 'Malam', 5000.00, '2026-05-22', 99, false);
INSERT INTO public.machine_bonus_logs (id, created_at, updated_at, deleted_at, employee_id, machine_name, shift_type, bonus_amount, date, jumlah_perolehan, is_demo) VALUES (21, '2026-05-29 15:11:29.409616+00', '2026-05-29 15:11:29.409616+00', NULL, 22, 'Bahtera TM', 'Pagi', 5000.00, '2026-05-29', 80, false);


--
-- Data for Name: master_products; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.master_products (id, created_at, updated_at, deleted_at, title, description, unit, price, berat_gram, cycle_time, unique_id, slug, date_created, last_updated, is_demo, cavity, reject_rate) VALUES (29, '2026-04-28 08:11:49.94791+00', '2026-04-28 08:11:49.94791+00', NULL, 'Baskom TM', NULL, 'lusin', 7000, 40, 9.5, NULL, NULL, NULL, NULL, false, 1, 0);
INSERT INTO public.master_products (id, created_at, updated_at, deleted_at, title, description, unit, price, berat_gram, cycle_time, unique_id, slug, date_created, last_updated, is_demo, cavity, reject_rate) VALUES (38, '2026-04-28 08:11:49.94791+00', '2026-04-28 08:11:49.94791+00', NULL, 'Smile 14', NULL, 'Lusin', 8200, 0, 0, NULL, NULL, NULL, NULL, false, 1, 0);
INSERT INTO public.master_products (id, created_at, updated_at, deleted_at, title, description, unit, price, berat_gram, cycle_time, unique_id, slug, date_created, last_updated, is_demo, cavity, reject_rate) VALUES (37, '2026-04-28 08:11:49.94791+00', '2026-04-28 08:11:49.94791+00', NULL, 'telor tali', NULL, 'Lusin', 3200, 0, 0, NULL, NULL, NULL, NULL, false, 1, 0);
INSERT INTO public.master_products (id, created_at, updated_at, deleted_at, title, description, unit, price, berat_gram, cycle_time, unique_id, slug, date_created, last_updated, is_demo, cavity, reject_rate) VALUES (39, '2026-04-28 08:11:49.94791+00', '2026-04-28 08:11:49.94791+00', NULL, 'tradisi cerah', NULL, 'Lusin', 5200, 0, 0, NULL, NULL, NULL, NULL, false, 1, 0);
INSERT INTO public.master_products (id, created_at, updated_at, deleted_at, title, description, unit, price, berat_gram, cycle_time, unique_id, slug, date_created, last_updated, is_demo, cavity, reject_rate) VALUES (40, '2026-04-28 08:11:49.94791+00', '2026-04-28 08:11:49.94791+00', NULL, 'Baskom Barca', NULL, 'Lusin', 7250, 0, 0, NULL, NULL, NULL, NULL, false, 1, 0);
INSERT INTO public.master_products (id, created_at, updated_at, deleted_at, title, description, unit, price, berat_gram, cycle_time, unique_id, slug, date_created, last_updated, is_demo, cavity, reject_rate) VALUES (41, '2026-04-28 08:11:49.94791+00', '2026-04-28 08:11:49.94791+00', NULL, 'Wakul Tanggok', NULL, 'Lusin', 5800, 0, 0, NULL, NULL, NULL, NULL, false, 1, 0);
INSERT INTO public.master_products (id, created_at, updated_at, deleted_at, title, description, unit, price, berat_gram, cycle_time, unique_id, slug, date_created, last_updated, is_demo, cavity, reject_rate) VALUES (42, '2026-04-28 08:11:49.94791+00', '2026-04-28 08:11:49.94791+00', NULL, 'Baskom Rotan', NULL, 'Lusin', 8400, 0, 0, NULL, NULL, NULL, NULL, false, 1, 0);
INSERT INTO public.master_products (id, created_at, updated_at, deleted_at, title, description, unit, price, berat_gram, cycle_time, unique_id, slug, date_created, last_updated, is_demo, cavity, reject_rate) VALUES (43, '2026-04-28 08:11:49.94791+00', '2026-04-28 08:11:49.94791+00', NULL, 'Wakul Mawar Super', NULL, 'Lusin', 9000, 0, 0, NULL, NULL, NULL, NULL, false, 1, 0);
INSERT INTO public.master_products (id, created_at, updated_at, deleted_at, title, description, unit, price, berat_gram, cycle_time, unique_id, slug, date_created, last_updated, is_demo, cavity, reject_rate) VALUES (44, '2026-04-28 08:11:49.94791+00', '2026-04-28 08:11:49.94791+00', NULL, 'Smile 14', NULL, 'Lusin', 8400, 0, 0, NULL, NULL, NULL, NULL, false, 1, 0);
INSERT INTO public.master_products (id, created_at, updated_at, deleted_at, title, description, unit, price, berat_gram, cycle_time, unique_id, slug, date_created, last_updated, is_demo, cavity, reject_rate) VALUES (45, '2026-04-28 08:11:49.94791+00', '2026-04-28 08:11:49.94791+00', NULL, 'Wakul Morris Super', NULL, 'Lusin', 5700, 0, 0, NULL, NULL, NULL, NULL, false, 1, 0);
INSERT INTO public.master_products (id, created_at, updated_at, deleted_at, title, description, unit, price, berat_gram, cycle_time, unique_id, slug, date_created, last_updated, is_demo, cavity, reject_rate) VALUES (46, '2026-05-01 08:18:43.627772+00', '2026-05-01 08:18:43.627772+00', NULL, 'BMP', '', 'Lusin', 7000, 0, 0, '44ba7f8a', '', '2026-05-01 08:18:43.616791+00', '2026-05-01 08:18:43.616792+00', false, 1, 0);
INSERT INTO public.master_products (id, created_at, updated_at, deleted_at, title, description, unit, price, berat_gram, cycle_time, unique_id, slug, date_created, last_updated, is_demo, cavity, reject_rate) VALUES (15, '2026-04-28 08:11:49.94791+00', '2026-04-28 08:11:49.94791+00', NULL, 'contoh', NULL, 'Lusin', 5000, 0, 0, NULL, NULL, NULL, NULL, false, 1, 0);
INSERT INTO public.master_products (id, created_at, updated_at, deleted_at, title, description, unit, price, berat_gram, cycle_time, unique_id, slug, date_created, last_updated, is_demo, cavity, reject_rate) VALUES (13, '2026-04-28 08:11:49.94791+00', '2026-04-28 08:11:49.94791+00', NULL, 'baskom panda', NULL, 'Lusin', 7000, 0, 0, NULL, NULL, NULL, NULL, false, 1, 0);
INSERT INTO public.master_products (id, created_at, updated_at, deleted_at, title, description, unit, price, berat_gram, cycle_time, unique_id, slug, date_created, last_updated, is_demo, cavity, reject_rate) VALUES (16, '2026-04-28 08:11:49.94791+00', '2026-04-28 08:11:49.94791+00', NULL, 'baskom mawar', NULL, 'Lusin', 5800, 0, 0, NULL, NULL, NULL, NULL, false, 1, 0);
INSERT INTO public.master_products (id, created_at, updated_at, deleted_at, title, description, unit, price, berat_gram, cycle_time, unique_id, slug, date_created, last_updated, is_demo, cavity, reject_rate) VALUES (17, '2026-04-28 08:11:49.94791+00', '2026-04-28 08:11:49.94791+00', NULL, 'baskom bahtera TM', NULL, 'Lusin', 6100, 0, 0, NULL, NULL, NULL, NULL, false, 1, 0);
INSERT INTO public.master_products (id, created_at, updated_at, deleted_at, title, description, unit, price, berat_gram, cycle_time, unique_id, slug, date_created, last_updated, is_demo, cavity, reject_rate) VALUES (19, '2026-04-28 08:11:49.94791+00', '2026-04-28 08:11:49.94791+00', NULL, 'wakul moris', NULL, 'Lusin', 5500, 0, 0, NULL, NULL, NULL, NULL, false, 1, 0);
INSERT INTO public.master_products (id, created_at, updated_at, deleted_at, title, description, unit, price, berat_gram, cycle_time, unique_id, slug, date_created, last_updated, is_demo, cavity, reject_rate) VALUES (20, '2026-04-28 08:11:49.94791+00', '2026-04-28 08:11:49.94791+00', NULL, 'baskom jago', NULL, 'Lusin', 5600, 0, 0, NULL, NULL, NULL, NULL, false, 1, 0);
INSERT INTO public.master_products (id, created_at, updated_at, deleted_at, title, description, unit, price, berat_gram, cycle_time, unique_id, slug, date_created, last_updated, is_demo, cavity, reject_rate) VALUES (23, '2026-04-28 08:11:49.94791+00', '2026-04-28 08:11:49.94791+00', NULL, 'smile 12', NULL, 'Lusin', 5400, 0, 0, NULL, NULL, NULL, NULL, false, 1, 0);
INSERT INTO public.master_products (id, created_at, updated_at, deleted_at, title, description, unit, price, berat_gram, cycle_time, unique_id, slug, date_created, last_updated, is_demo, cavity, reject_rate) VALUES (24, '2026-04-28 08:11:49.94791+00', '2026-04-28 08:11:49.94791+00', NULL, 'wakul rehana', NULL, 'Lusin', 4000, 0, 0, NULL, NULL, NULL, NULL, false, 1, 0);
INSERT INTO public.master_products (id, created_at, updated_at, deleted_at, title, description, unit, price, berat_gram, cycle_time, unique_id, slug, date_created, last_updated, is_demo, cavity, reject_rate) VALUES (25, '2026-04-28 08:11:49.94791+00', '2026-04-28 08:11:49.94791+00', NULL, 'baskom mawar', NULL, 'Lusin', 5800, 0, 0, NULL, NULL, NULL, NULL, false, 1, 0);
INSERT INTO public.master_products (id, created_at, updated_at, deleted_at, title, description, unit, price, berat_gram, cycle_time, unique_id, slug, date_created, last_updated, is_demo, cavity, reject_rate) VALUES (27, '2026-04-28 08:11:49.94791+00', '2026-04-28 08:11:49.94791+00', NULL, 'Wakul Rehana Super', NULL, 'Lusin', 4300, 0, 0, NULL, NULL, NULL, NULL, false, 1, 0);
INSERT INTO public.master_products (id, created_at, updated_at, deleted_at, title, description, unit, price, berat_gram, cycle_time, unique_id, slug, date_created, last_updated, is_demo, cavity, reject_rate) VALUES (28, '2026-04-28 08:11:49.94791+00', '2026-04-28 08:11:49.94791+00', NULL, 'baskom jago 12', NULL, 'Lusin', 5700, 0, 0, NULL, NULL, NULL, NULL, false, 1, 0);
INSERT INTO public.master_products (id, created_at, updated_at, deleted_at, title, description, unit, price, berat_gram, cycle_time, unique_id, slug, date_created, last_updated, is_demo, cavity, reject_rate) VALUES (31, '2026-04-28 08:11:49.94791+00', '2026-04-28 08:11:49.94791+00', NULL, 'wakul tradisi super', NULL, 'Lusin', 3600, 0, 0, NULL, NULL, NULL, NULL, false, 1, 0);
INSERT INTO public.master_products (id, created_at, updated_at, deleted_at, title, description, unit, price, berat_gram, cycle_time, unique_id, slug, date_created, last_updated, is_demo, cavity, reject_rate) VALUES (32, '2026-04-28 08:11:49.94791+00', '2026-04-28 08:11:49.94791+00', NULL, 'Baskom Bahtera TB', NULL, 'Lusin', 7100, 0, 0, NULL, NULL, NULL, NULL, false, 1, 0);
INSERT INTO public.master_products (id, created_at, updated_at, deleted_at, title, description, unit, price, berat_gram, cycle_time, unique_id, slug, date_created, last_updated, is_demo, cavity, reject_rate) VALUES (33, '2026-04-28 08:11:49.94791+00', '2026-04-28 08:11:49.94791+00', NULL, 'wakul kotak', NULL, 'Lusin', 5700, 0, 0, NULL, NULL, NULL, NULL, false, 1, 0);
INSERT INTO public.master_products (id, created_at, updated_at, deleted_at, title, description, unit, price, berat_gram, cycle_time, unique_id, slug, date_created, last_updated, is_demo, cavity, reject_rate) VALUES (35, '2026-04-28 08:11:49.94791+00', '2026-04-28 08:11:49.94791+00', NULL, 'wakul telur', NULL, 'Lusin', 2300, 0, 0, NULL, NULL, NULL, NULL, false, 1, 0);
INSERT INTO public.master_products (id, created_at, updated_at, deleted_at, title, description, unit, price, berat_gram, cycle_time, unique_id, slug, date_created, last_updated, is_demo, cavity, reject_rate) VALUES (36, '2026-04-28 08:11:49.94791+00', '2026-04-28 08:11:49.94791+00', NULL, 'telor japar', NULL, 'Lusin', 2100, 0, 0, NULL, NULL, NULL, NULL, false, 1, 0);
INSERT INTO public.master_products (id, created_at, updated_at, deleted_at, title, description, unit, price, berat_gram, cycle_time, unique_id, slug, date_created, last_updated, is_demo, cavity, reject_rate) VALUES (26, '2026-04-28 08:11:49.94791+00', '2026-04-28 08:11:49.94791+00', NULL, 'Baskom Bahtera', NULL, 'Lusin', 5900, 0, 0, NULL, NULL, NULL, NULL, false, 1, 0);
INSERT INTO public.master_products (id, created_at, updated_at, deleted_at, title, description, unit, price, berat_gram, cycle_time, unique_id, slug, date_created, last_updated, is_demo, cavity, reject_rate) VALUES (51, '2026-05-15 01:25:46.436535+00', '2026-05-15 08:01:01.047918+00', NULL, 'Baskom Panda Cerah', '', 'Lusin', 10000, 55, 10, '8ac53e80', 'Baskom Panda Cerah-8ac53e80', '2026-05-15 01:25:46.420523+00', '2026-05-15 08:01:01.045525+00', false, 1, 0);
INSERT INTO public.master_products (id, created_at, updated_at, deleted_at, title, description, unit, price, berat_gram, cycle_time, unique_id, slug, date_created, last_updated, is_demo, cavity, reject_rate) VALUES (52, '2026-05-16 04:48:27.025776+00', '2026-05-16 04:48:27.025776+00', NULL, 'Test Product AI', '', 'Pcspcs', 1000, 0, 0, 'dc023a4e', 'Test Product AI-dc023a4e', '2026-05-16 04:48:27.017395+00', '2026-05-16 04:48:27.017395+00', false, 1, 0);
INSERT INTO public.master_products (id, created_at, updated_at, deleted_at, title, description, unit, price, berat_gram, cycle_time, unique_id, slug, date_created, last_updated, is_demo, cavity, reject_rate) VALUES (30, '2026-04-28 08:11:49.94791+00', '2026-05-16 04:52:29.008962+00', NULL, 'Baskom Durian', NULL, 'Lusin', 9200, 50, 11, NULL, NULL, NULL, '2026-05-16 04:52:29.005325+00', false, 1, 0);
INSERT INTO public.master_products (id, created_at, updated_at, deleted_at, title, description, unit, price, berat_gram, cycle_time, unique_id, slug, date_created, last_updated, is_demo, cavity, reject_rate) VALUES (53, '2026-05-27 07:15:23.859001+00', '2026-05-27 07:15:23.859001+00', NULL, 'pot hitam 10', '', 'Pcs', 20000, 60, 15, 'c45b4c22', 'pot hitam 10-c45b4c22', '2026-05-27 07:15:23.853847+00', '2026-05-27 07:15:23.853847+00', true, 1, 0);
INSERT INTO public.master_products (id, created_at, updated_at, deleted_at, title, description, unit, price, berat_gram, cycle_time, unique_id, slug, date_created, last_updated, is_demo, cavity, reject_rate) VALUES (18, '2026-04-28 08:11:49.94791+00', '2026-05-31 06:31:11.60382+00', NULL, 'bak kuping12', NULL, 'Lusin', 13000, 72, 15, NULL, NULL, NULL, '2026-05-31 06:31:11.479366+00', false, 1, 0);
INSERT INTO public.master_products (id, created_at, updated_at, deleted_at, title, description, unit, price, berat_gram, cycle_time, unique_id, slug, date_created, last_updated, is_demo, cavity, reject_rate) VALUES (34, '2026-04-28 08:11:49.94791+00', '2026-05-31 06:32:59.934048+00', NULL, 'Tradisi Super 30', NULL, 'Lusin', 3400, 21, 7, NULL, NULL, NULL, '2026-05-31 06:32:59.809424+00', false, 1, 0);
INSERT INTO public.master_products (id, created_at, updated_at, deleted_at, title, description, unit, price, berat_gram, cycle_time, unique_id, slug, date_created, last_updated, is_demo, cavity, reject_rate) VALUES (21, '2026-04-28 08:11:49.94791+00', '2026-05-31 06:34:09.680507+00', NULL, 'tradisi super 2', NULL, 'Lusin', 3100, 21, 7, NULL, NULL, NULL, '2026-05-31 06:34:09.555923+00', false, 1, 0);
INSERT INTO public.master_products (id, created_at, updated_at, deleted_at, title, description, unit, price, berat_gram, cycle_time, unique_id, slug, date_created, last_updated, is_demo, cavity, reject_rate) VALUES (22, '2026-04-28 08:11:49.94791+00', '2026-05-31 06:35:49.413934+00', NULL, 'baskom panda super', NULL, 'Lusin', 8400, 52, 10, NULL, NULL, NULL, '2026-05-31 06:35:49.288842+00', false, 1, 0);


--
-- Data for Name: payrolls; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (10, '2026-05-02 03:22:39.743391+00', '2026-05-02 03:22:39.743391+00', '2026-05-02 03:30:26.088264+00', 4, '2026-05-02', 300000.00, '', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (9, '2026-05-02 03:22:39.438496+00', '2026-05-02 03:22:39.438496+00', '2026-05-02 03:30:30.830868+00', 3, '2026-05-02', 315000.00, '', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (8, '2026-05-02 03:22:38.975625+00', '2026-05-02 03:22:38.975625+00', '2026-05-02 03:30:33.913341+00', 2, '2026-05-02', 345000.00, '', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (7, '2026-05-02 03:22:38.467736+00', '2026-05-02 03:22:38.467736+00', '2026-05-02 03:30:36.617196+00', 1, '2026-05-02', 360000.00, '', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (6, '2026-05-02 03:20:21.130571+00', '2026-05-02 03:20:21.130571+00', '2026-05-02 03:30:38.862251+00', 12, '2026-05-02', 315000.00, '', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (5, '2026-05-02 02:55:45.388657+00', '2026-05-02 02:55:45.388657+00', '2026-05-02 03:30:41.527318+00', 11, '2026-05-02', 405000.00, '', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (4, '2026-05-02 02:54:27.611054+00', '2026-05-02 02:54:27.611054+00', '2026-05-02 03:30:44.397797+00', 6, '2026-05-02', 285000.00, '', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (3, '2026-05-02 02:19:15.089491+00', '2026-05-02 02:19:15.089491+00', '2026-05-02 03:30:46.905918+00', 11, '2026-05-02', 405000.00, '', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (2, '2026-05-01 17:05:32.4993+00', '2026-05-01 17:05:32.4993+00', '2026-05-02 03:30:49.4041+00', 3, '2026-05-01', 52500.00, '', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (1, '2026-05-01 16:11:52.593016+00', '2026-05-01 16:11:52.593016+00', '2026-05-02 03:30:52.199282+00', 1, '2026-05-01', 60000.00, '', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (14, '2026-05-02 03:32:21.110612+00', '2026-05-02 03:32:21.110612+00', '2026-05-02 03:32:44.884912+00', 4, '2026-05-02', 300000.00, '', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (13, '2026-05-02 03:32:20.680121+00', '2026-05-02 03:32:20.680121+00', '2026-05-02 03:33:31.159521+00', 3, '2026-05-02', 315000.00, '', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (12, '2026-05-02 03:32:20.344133+00', '2026-05-02 03:32:20.344133+00', '2026-05-02 03:33:35.19074+00', 2, '2026-05-02', 345000.00, '', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (11, '2026-05-02 03:32:19.772098+00', '2026-05-02 03:32:19.772098+00', '2026-05-02 03:33:37.89522+00', 1, '2026-05-02', 360000.00, '', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (18, '2026-05-02 03:34:46.898161+00', '2026-05-02 03:34:46.898161+00', '2026-05-02 03:35:20.1136+00', 4, '2026-05-02', 300000.00, '', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (17, '2026-05-02 03:34:46.288159+00', '2026-05-02 03:34:46.288159+00', '2026-05-02 03:35:23.125119+00', 3, '2026-05-02', 315000.00, '', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (16, '2026-05-02 03:34:45.80016+00', '2026-05-02 03:34:45.80016+00', '2026-05-02 03:35:25.592334+00', 2, '2026-05-02', 345000.00, '', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (15, '2026-05-02 03:34:45.122905+00', '2026-05-02 03:34:45.122905+00', '2026-05-02 03:35:28.515726+00', 1, '2026-05-02', 360000.00, '', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (22, '2026-05-02 03:49:08.924382+00', '2026-05-02 03:49:08.924382+00', '2026-05-02 03:52:29.009251+00', 14, '2026-05-02', 157500.00, '', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (21, '2026-05-02 03:49:08.440129+00', '2026-05-02 03:49:08.440129+00', '2026-05-02 03:52:31.593441+00', 12, '2026-05-02', 262500.00, '', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (20, '2026-05-02 03:49:07.950022+00', '2026-05-02 03:49:07.950022+00', '2026-05-02 03:52:34.018956+00', 11, '2026-05-02', 405000.00, '', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (19, '2026-05-02 03:49:07.429709+00', '2026-05-02 03:49:07.429709+00', '2026-05-02 03:52:36.294327+00', 9, '2026-05-02', 450000.00, '', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (26, '2026-05-02 04:49:06.368358+00', '2026-05-02 04:49:06.368358+00', '2026-05-02 04:50:16.631329+00', 13, '2026-05-02', 315000.00, '', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (25, '2026-05-02 04:49:05.880851+00', '2026-05-02 04:49:05.880851+00', '2026-05-02 04:50:22.768006+00', 12, '2026-05-02', 315000.00, '', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (24, '2026-05-02 04:49:05.257858+00', '2026-05-02 04:49:05.257858+00', '2026-05-02 04:50:25.426834+00', 9, '2026-05-02', 450000.00, '', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (23, '2026-05-02 04:49:04.876327+00', '2026-05-02 04:49:04.876327+00', '2026-05-02 04:50:31.58529+00', 8, '2026-05-02', 450000.00, '', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (30, '2026-05-02 04:51:21.538191+00', '2026-05-02 04:51:21.538191+00', '2026-05-02 05:07:13.727313+00', 3, '2026-05-02', 105000.00, '', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (29, '2026-05-02 04:51:20.906756+00', '2026-05-02 04:51:20.906756+00', '2026-05-02 05:07:19.371844+00', 19, '2026-05-02', 157500.00, '', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (28, '2026-05-02 04:51:20.361651+00', '2026-05-02 04:51:20.361651+00', '2026-05-02 05:07:21.819218+00', 14, '2026-05-02', 157500.00, '', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (27, '2026-05-02 04:51:19.870483+00', '2026-05-02 04:51:19.870483+00', '2026-05-02 05:11:45.510274+00', 17, '2026-05-02', 157500.00, '', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (38, '2026-05-02 05:39:17.611293+00', '2026-05-02 05:39:17.611293+00', '2026-05-02 05:51:59.545023+00', 14, '2026-05-02', 157500.00, '', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (37, '2026-05-02 05:39:17.147607+00', '2026-05-02 05:39:17.147607+00', '2026-05-02 05:52:01.944139+00', 12, '2026-05-02', 210000.00, '', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (36, '2026-05-02 05:39:16.660023+00', '2026-05-02 05:39:16.660023+00', '2026-05-02 05:52:04.386673+00', 10, '2026-05-02', 220000.00, '', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (35, '2026-05-02 05:39:16.181677+00', '2026-05-02 05:39:16.181677+00', '2026-05-02 05:52:06.57338+00', 9, '2026-05-02', 300000.00, '', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (34, '2026-05-02 05:12:59.902506+00', '2026-05-02 05:12:59.902506+00', '2026-05-02 05:52:08.920669+00', 10, '2026-05-02', 165000.00, '', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (33, '2026-05-02 05:12:59.449958+00', '2026-05-02 05:12:59.449958+00', '2026-05-02 05:52:11.181557+00', 16, '2026-05-02', 157500.00, '', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (32, '2026-05-02 05:12:58.95964+00', '2026-05-02 05:12:58.95964+00', '2026-05-02 05:52:13.712444+00', 15, '2026-05-02', 262500.00, '', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (31, '2026-05-02 05:12:58.512881+00', '2026-05-02 05:12:58.512881+00', '2026-05-02 05:52:15.858843+00', 2, '2026-05-02', 287500.00, '', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (44, '2026-05-02 05:58:18.770059+00', '2026-05-02 05:58:18.770059+00', '2026-05-02 05:59:02.574863+00', 13, '2026-05-02', 315000.00, '60k', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (43, '2026-05-02 05:58:18.528733+00', '2026-05-02 05:58:18.528733+00', '2026-05-02 05:59:05.898873+00', 2, '2026-05-02', 345000.00, '', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (42, '2026-05-02 05:58:18.471427+00', '2026-05-02 05:58:18.471427+00', '2026-05-02 05:59:08.451901+00', 8, '2026-05-02', 450000.00, 'bonus 30k', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (41, '2026-05-02 05:58:18.062826+00', '2026-05-02 05:58:18.062826+00', '2026-05-02 05:59:11.249412+00', 20, '2026-05-02', 300000.00, '', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (40, '2026-05-02 05:58:17.591142+00', '2026-05-02 05:58:17.591142+00', '2026-05-02 05:59:13.926713+00', 13, '2026-05-02', 315000.00, '60k', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (39, '2026-05-02 05:58:17.2902+00', '2026-05-02 05:58:17.2902+00', '2026-05-02 05:59:16.605269+00', 8, '2026-05-02', 450000.00, 'bonus 30k', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (48, '2026-05-02 06:06:42.833321+00', '2026-05-02 06:06:42.833321+00', '2026-05-02 06:09:08.349588+00', 2, '2026-05-02', 345000.00, 'bonus : 42k extra : 25k tgl merah 15k', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (47, '2026-05-02 06:06:42.351503+00', '2026-05-02 06:06:42.351503+00', '2026-05-02 06:09:11.191374+00', 13, '2026-05-02', 315000.00, 'bonus : 30K extra 40k tgl merah 15k', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (46, '2026-05-02 06:06:42.070608+00', '2026-05-02 06:06:42.070608+00', '2026-05-02 06:09:13.900631+00', 20, '2026-05-02', 285000.00, 'bonus : 30k ekstra :20k tgl merah + 15k', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (45, '2026-05-02 06:06:41.56223+00', '2026-05-02 06:06:41.56223+00', '2026-05-02 06:09:16.483+00', 2, '2026-05-02', 345000.00, 'bonus: 35k ekstra 20k tgl merah :+15k', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (52, '2026-05-02 06:13:04.396061+00', '2026-05-02 06:13:04.396061+00', '2026-05-02 06:15:23.919695+00', 20, '2026-05-02', 237500.00, 'bonus : 30k extra : 20k tgl merah : 15k', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (51, '2026-05-02 06:13:04.104863+00', '2026-05-02 06:13:04.104863+00', '2026-05-02 06:15:26.761752+00', 13, '2026-05-02', 315000.00, 'bonus : 30k extra : 40k tgl merah : 15k', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (50, '2026-05-02 06:13:03.41955+00', '2026-05-02 06:13:03.41955+00', '2026-05-02 06:15:29.540384+00', 2, '2026-05-02', 345000.00, 'bonus : 42k extra : 25k tgl merah : 15k', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (49, '2026-05-02 06:13:02.919893+00', '2026-05-02 06:13:02.919893+00', '2026-05-02 06:15:32.104555+00', 8, '2026-05-02', 450000.00, 'bonus : 42k extra : 100k tgl merah : 15k', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (53, '2026-05-26 19:12:52.55286+00', '2026-05-26 19:12:52.55286+00', '2026-05-26 19:13:30.035827+00', 22, '2026-05-26', 83300.00, 'Denda dihapus (manual)', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (54, '2026-05-26 19:17:25.028274+00', '2026-05-26 19:17:25.028274+00', '2026-05-26 19:17:48.337473+00', 22, '2026-05-26', 83300.00, 'Denda dihapus (manual)', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (59, '2026-05-30 05:58:05.944501+00', '2026-05-30 05:58:05.944501+00', '2026-05-30 08:18:27.520866+00', 24, '2026-05-30', 50000.00, 'Auto-fill (Minggu ini)', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (58, '2026-05-30 05:58:05.591778+00', '2026-05-30 05:58:05.591778+00', '2026-05-30 08:18:33.135014+00', 18, '2026-05-30', 157500.00, 'Auto-fill (Minggu ini)', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (57, '2026-05-30 05:58:05.235422+00', '2026-05-30 05:58:05.235422+00', '2026-05-30 08:18:38.815197+00', 9, '2026-05-30', 225000.00, 'Auto-fill (Minggu ini)', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (56, '2026-05-30 05:58:04.879857+00', '2026-05-30 05:58:04.879857+00', '2026-05-30 08:18:42.938252+00', 17, '2026-05-30', 157500.00, 'Auto-fill (Minggu ini)', 0, 0.00, false);
INSERT INTO public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) VALUES (55, '2026-05-30 05:58:04.513854+00', '2026-05-30 05:58:04.513854+00', '2026-05-30 08:18:46.792524+00', 1, '2026-05-30', 180000.00, 'Auto-fill (Minggu ini)', 0, 0.00, false);


--
-- Data for Name: pembayarans; Type: TABLE DATA; Schema: public; Owner: postgres
--



--
-- Data for Name: pembelian_barangs; Type: TABLE DATA; Schema: public; Owner: postgres
--



--
-- Data for Name: pembelian_items; Type: TABLE DATA; Schema: public; Owner: postgres
--



--
-- Data for Name: products; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (150, '2026-04-15 10:37:01.527239+00', '2026-04-15 10:37:01.527289+00', NULL, 20, 'baskom jago', '-', 5400, 50, 110, 'Rp', 150, '0e02cbaec1ee', 'baskom-jago-0e02cbaec1ee', '2026-04-15 10:37:01.527239+00', '2026-04-15 10:37:01.527289+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (151, '2026-04-15 10:37:01.531803+00', '2026-04-15 10:37:01.531847+00', NULL, 26, 'Bahkom Bahtera', '-', 5900, 50, 30, 'Rp', 150, 'a5ec365d8011', 'bahkom-bahtera-a5ec365d8011', '2026-04-15 10:37:01.531803+00', '2026-04-15 10:37:01.531847+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (152, '2026-04-15 10:56:55.868638+00', '2026-04-15 10:56:55.868682+00', NULL, 36, 'telor japar', '-', 2100, 40, 50, 'Rp', 150, '571b0c111d03', 'telor-japar-571b0c111d03', '2026-04-15 10:56:55.868638+00', '2026-04-15 10:56:55.868682+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (107, '2026-04-13 08:22:01.332535+00', '2026-04-17 06:58:46.135345+00', NULL, 23, 'smile 12', '-', 5700, 50, 10, 'Rp', 114, '2c1b3ef8dbde', 'smile-12-2c1b3ef8dbde', '2026-04-13 08:22:01.332535+00', '2026-04-17 06:58:46.135345+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (141, '2026-04-15 02:14:38.203706+00', '2026-04-21 14:32:03.855823+00', NULL, 34, 'Tradisi Super 30', '-', 3600, 30, 25, 'Rp', 138, 'b470e494e550', 'tradisi-super-30-b470e494e550', '2026-04-15 02:14:38.203706+00', '2026-04-21 14:32:03.855823+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (91, '2026-04-13 04:08:42.423736+00', '2026-04-13 04:08:42.423807+00', NULL, 18, 'bak kuping ANISA 12', '-', 12000, 30, 80, 'Rp', 110, 'fdd3ae4ff1b0', 'bak-kuping-anisa-12-fdd3ae4ff1b0', '2026-04-13 04:08:42.423736+00', '2026-04-13 04:08:42.423807+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (92, '2026-04-13 04:08:42.428383+00', '2026-04-13 04:08:42.428434+00', NULL, 19, 'wakul moris', '-', 5500, 40, 20, 'Rp', 110, 'ae46e07b616e', 'wakul-moris-ae46e07b616e', '2026-04-13 04:08:42.428383+00', '2026-04-13 04:08:42.428434+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (89, '2026-04-13 01:32:38.633484+00', '2026-04-13 01:32:38.633535+00', NULL, 18, 'bak kuping ANISA 12', '-', 12000, 30, 80, 'Rp', 109, '3df804a6b534', 'bak-kuping-anisa-12-3df804a6b534', '2026-04-13 01:32:38.633484+00', '2026-04-13 01:32:38.633535+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (90, '2026-04-13 01:32:38.636925+00', '2026-04-13 01:32:38.636963+00', NULL, 16, 'baskom mawar', '-', 5800, 50, 10, 'Rp', 109, 'd0ea04fea599', 'baskom-mawar-d0ea04fea599', '2026-04-13 01:32:38.636925+00', '2026-04-13 01:32:38.636963+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (94, '2026-04-13 06:13:22.880616+00', '2026-04-13 06:13:22.88069+00', NULL, 20, 'baskom jago', '-', 5600, 50, 40, 'Rp', 111, 'ef92c1609c19', 'baskom-jago-ef92c1609c19', '2026-04-13 06:13:22.880616+00', '2026-04-13 06:13:22.88069+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (95, '2026-04-13 06:14:03.08462+00', '2026-04-13 06:14:03.084687+00', NULL, 19, 'wakul moris', '-', 5700, 40, 40, 'Rp', 111, '81593b97335a', 'wakul-moris-81593b97335a', '2026-04-13 06:14:03.08462+00', '2026-04-13 06:14:03.084687+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (96, '2026-04-13 06:30:03.823582+00', '2026-04-13 06:30:03.823672+00', NULL, 21, 'tradisi super 2', '-', 3100, 20, 40, 'Rp', 112, 'c6112d4dc3a6', 'tradisi-super-2-c6112d4dc3a6', '2026-04-13 06:30:03.823582+00', '2026-04-13 06:30:03.823672+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (97, '2026-04-13 06:30:58.318572+00', '2026-04-13 06:30:58.318645+00', NULL, 22, 'baskom panda super', '-', 7200, 40, 4, 'Rp', 112, 'f20ee407581a', 'baskom-panda-super-f20ee407581a', '2026-04-13 06:30:58.318572+00', '2026-04-13 06:30:58.318645+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (98, '2026-04-13 06:31:50.532726+00', '2026-04-13 06:31:50.532825+00', NULL, 24, 'wakul rehana', '-', 4000, 50, 10, 'Rp', 112, 'a98c073b3c99', 'wakul-rehana-a98c073b3c99', '2026-04-13 06:31:50.532726+00', '2026-04-13 06:31:50.532825+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (99, '2026-04-13 06:36:43.580569+00', '2026-04-13 06:36:43.580649+00', NULL, 25, 'baskom mawar', '-', 5800, 50, 7, 'Rp', 112, 'c87f8316e58e', 'baskom-mawar-c87f8316e58e', '2026-04-13 06:36:43.580569+00', '2026-04-13 06:36:43.580649+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (100, '2026-04-13 06:37:35.004634+00', '2026-04-13 06:37:35.00473+00', NULL, 23, 'smile 12', '-', 5400, 50, 5, 'Rp', 112, '504614f38715', 'smile-12-504614f38715', '2026-04-13 06:37:35.004634+00', '2026-04-13 06:37:35.00473+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (101, '2026-04-13 07:27:27.651075+00', '2026-04-13 07:27:27.651142+00', NULL, 26, 'Bahkom Bahtera', '-', 5900, 50, 5, 'Rp', 113, '43789f437e72', 'bahkom-bahtera-43789f437e72', '2026-04-13 07:27:27.651075+00', '2026-04-13 07:27:27.651142+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (102, '2026-04-13 07:27:27.658458+00', '2026-04-13 07:27:27.658504+00', NULL, 17, 'baskom bahtera TM', '-', 5700, 50, 5, 'Rp', 113, 'c4491786e907', 'baskom-bahtera-tm-c4491786e907', '2026-04-13 07:27:27.658458+00', '2026-04-13 07:27:27.658504+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (103, '2026-04-13 07:27:27.666857+00', '2026-04-13 07:27:27.666898+00', NULL, 27, 'Wakul Rehana Super', '-', 4300, 50, 12, 'Rp', 113, '4d21f27a4126', 'wakul-rehana-super-4d21f27a4126', '2026-04-13 07:27:27.666857+00', '2026-04-13 07:27:27.666898+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (104, '2026-04-13 07:27:27.670895+00', '2026-04-13 07:27:27.670942+00', NULL, 25, 'baskom mawar', '-', 6100, 50, 3, 'Rp', 113, '835e5bad1628', 'baskom-mawar-835e5bad1628', '2026-04-13 07:27:27.670895+00', '2026-04-13 07:27:27.670942+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (105, '2026-04-13 08:18:56.230642+00', '2026-04-13 08:18:56.230686+00', NULL, 22, 'baskom panda super', '-', 7500, 40, 10, 'Rp', 114, '9f845c4e6274', 'baskom-panda-super-9f845c4e6274', '2026-04-13 08:18:56.230642+00', '2026-04-13 08:18:56.230686+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (108, '2026-04-13 08:22:01.335504+00', '2026-04-13 08:22:01.335538+00', NULL, 28, 'baskom jago 12', '-', 5700, 50, 3, 'Rp', 114, '0b1a104ea3c4', 'baskom-jago-12-0b1a104ea3c4', '2026-04-13 08:22:01.335504+00', '2026-04-13 08:22:01.335538+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (109, '2026-04-13 08:32:12.646669+00', '2026-04-13 08:32:12.646729+00', NULL, 18, 'bak kuping ANISA 12', '-', 12300, 30, 100, 'Rp', 115, '122b8a74011a', 'bak-kuping-anisa-12-122b8a74011a', '2026-04-13 08:32:12.646669+00', '2026-04-13 08:32:12.646729+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (110, '2026-04-13 08:32:12.651263+00', '2026-04-13 08:32:12.651304+00', NULL, 16, 'baskom mawar', '-', 6100, 50, 10, 'Rp', 115, '461f504f375f', 'baskom-mawar-461f504f375f', '2026-04-13 08:32:12.651263+00', '2026-04-13 08:32:12.651304+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (111, '2026-04-13 08:32:12.654111+00', '2026-04-13 08:32:12.654148+00', NULL, 29, 'Baskom TM', '-', 6100, 50, 10, 'Rp', 115, '2e8ee9881c1a', 'baskom-tm-2e8ee9881c1a', '2026-04-13 08:32:12.654111+00', '2026-04-13 08:32:12.654148+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (112, '2026-04-13 08:32:12.656645+00', '2026-04-13 08:32:12.656681+00', NULL, 13, 'baskom panda', '-', 7800, 40, 10, 'Rp', 115, '65dfd24637b0', 'baskom-panda-65dfd24637b0', '2026-04-13 08:32:12.656645+00', '2026-04-13 08:32:12.656681+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (113, '2026-04-13 08:32:12.659098+00', '2026-04-13 08:32:12.659127+00', NULL, 20, 'baskom jago', '-', 5800, 50, 4, 'Rp', 115, '9b67adb57beb', 'baskom-jago-9b67adb57beb', '2026-04-13 08:32:12.659098+00', '2026-04-13 08:32:12.659127+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (114, '2026-04-13 08:32:12.661689+00', '2026-04-13 08:32:12.661721+00', NULL, 19, 'wakul moris', '-', 5800, 40, 10, 'Rp', 115, 'f6696ce1c33a', 'wakul-moris-f6696ce1c33a', '2026-04-13 08:32:12.661689+00', '2026-04-13 08:32:12.661721+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (115, '2026-04-13 08:33:50.62072+00', '2026-04-13 08:33:50.620767+00', NULL, 30, 'Baskom Durian', '-', 8500, 40, 10, 'Rp', 115, '334d50b49069', 'baskom-durian-334d50b49069', '2026-04-13 08:33:50.62072+00', '2026-04-13 08:33:50.620767+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (123, '2026-04-13 12:00:07.550033+00', '2026-04-13 12:00:07.550134+00', NULL, 32, 'Baskom Bahtera TB', '-', 7100, 50, 50, 'Rp', 132, '865d5c6326f1', 'baskom-bahtera-tb-865d5c6326f1', '2026-04-13 12:00:07.550033+00', '2026-04-13 12:00:07.550134+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (124, '2026-04-13 12:00:07.558266+00', '2026-04-13 12:00:07.558477+00', NULL, 31, 'wakul tradisi super', '-', 3600, 20, 10, 'Rp', 132, '5a684c3ab2b0', 'wakul-tradisi-super-5a684c3ab2b0', '2026-04-13 12:00:07.558266+00', '2026-04-13 12:00:07.558477+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (125, '2026-04-13 12:06:24.403438+00', '2026-04-13 12:06:24.403523+00', NULL, 33, 'wakul kotak', '-', 5700, 20, 30, 'Rp', 132, 'f295179a15ef', 'wakul-kotak-f295179a15ef', '2026-04-13 12:06:24.403438+00', '2026-04-13 12:06:24.403523+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (128, '2026-04-14 05:20:45.007747+00', '2026-04-14 05:20:45.007815+00', NULL, 29, 'Baskom TM', '-', 6750, 50, 15, 'Rp', 134, '63020ceed7f9', 'baskom-tm-63020ceed7f9', '2026-04-14 05:20:45.007747+00', '2026-04-14 05:20:45.007815+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (129, '2026-04-14 05:20:45.011493+00', '2026-04-14 05:20:45.011548+00', NULL, 25, 'baskom mawar', '-', 6800, 50, 20, 'Rp', 134, '5979eb87bffb', 'baskom-mawar-5979eb87bffb', '2026-04-14 05:20:45.011493+00', '2026-04-14 05:20:45.011548+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (130, '2026-04-14 05:20:45.014409+00', '2026-04-14 05:20:45.014458+00', NULL, 30, 'Baskom Durian', '-', 8600, 40, 5, 'Rp', 134, 'e9b11a19b1ec', 'baskom-durian-e9b11a19b1ec', '2026-04-14 05:20:45.014409+00', '2026-04-14 05:20:45.014458+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (131, '2026-04-14 05:57:57.807441+00', '2026-04-14 05:57:57.807526+00', NULL, 29, 'Baskom TM', '-', 6500, 50, 10, 'Rp', 135, 'f0b65eecc616', 'baskom-tm-f0b65eecc616', '2026-04-14 05:57:57.807441+00', '2026-04-14 05:57:57.807526+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (132, '2026-04-14 05:57:57.816207+00', '2026-04-14 05:57:57.816281+00', NULL, 25, 'baskom mawar', '-', 7100, 50, 15, 'Rp', 135, 'f7546516aa9d', 'baskom-mawar-f7546516aa9d', '2026-04-14 05:57:57.816207+00', '2026-04-14 05:57:57.816281+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (133, '2026-04-14 05:57:57.82288+00', '2026-04-14 05:57:57.822945+00', NULL, 23, 'smile 12', '-', 6400, 50, 10, 'Rp', 135, '89817187abb3', 'smile-12-89817187abb3', '2026-04-14 05:57:57.82288+00', '2026-04-14 05:57:57.822945+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (134, '2026-04-14 05:59:31.475091+00', '2026-04-14 05:59:31.475173+00', NULL, 34, 'Tradisi Super 30', '-', 3400, 30, 30, 'Rp', 135, '478139c40375', 'tradisi-super-30-478139c40375', '2026-04-14 05:59:31.475091+00', '2026-04-14 05:59:31.475173+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (135, '2026-04-14 06:32:15.75184+00', '2026-04-14 06:32:15.751901+00', NULL, 20, 'baskom jago', '-', 6200, 50, 98, 'Rp', 132, '476a53a54f0d', 'baskom-jago-476a53a54f0d', '2026-04-14 06:32:15.75184+00', '2026-04-14 06:32:15.751901+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (137, '2026-04-15 01:42:08.767097+00', '2026-04-15 01:42:08.767153+00', NULL, 23, 'smile 12', '-', 6700, 50, 20, 'Rp', 138, '3e510bc77ddc', 'smile-12-3e510bc77ddc', '2026-04-15 01:42:08.767097+00', '2026-04-15 01:42:08.767153+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (138, '2026-04-15 01:42:34.979623+00', '2026-04-15 01:42:34.979681+00', NULL, 16, 'baskom mawar', '-', 7100, 50, 5, 'Rp', 138, 'f21c0b239468', 'baskom-mawar-f21c0b239468', '2026-04-15 01:42:34.979623+00', '2026-04-15 01:42:34.979681+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (140, '2026-04-15 01:44:12.601412+00', '2026-04-15 01:44:12.601473+00', NULL, 35, 'wakul telur', '-', 2300, 20, 84, 'Rp', 138, '0c5d77c6c877', 'wakul-telur-0c5d77c6c877', '2026-04-15 01:44:12.601412+00', '2026-04-15 01:44:12.601473+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (153, '2026-04-15 10:56:55.873758+00', '2026-04-15 10:56:55.873803+00', NULL, 37, 'telor tali', '-', 3200, 40, 50, 'Rp', 150, '9048090f9ae6', 'telor-tali-9048090f9ae6', '2026-04-15 10:56:55.873758+00', '2026-04-15 10:56:55.873803+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (154, '2026-04-15 10:56:55.87876+00', '2026-04-15 10:56:55.8788+00', NULL, 31, 'wakul tradisi super', '-', 3200, 40, 25, 'Rp', 150, 'b2404a735812', 'wakul-tradisi-super-b2404a735812', '2026-04-15 10:56:55.87876+00', '2026-04-15 10:56:55.8788+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (158, '2026-04-16 11:15:41.82706+00', '2026-04-16 11:15:41.827124+00', NULL, 31, 'wakul tradisi super', '-', 3100, 30, 30, 'Rp', 155, 'bc6288361ed2', 'wakul-tradisi-super-bc6288361ed2', '2026-04-16 11:15:41.82706+00', '2026-04-16 11:15:41.827124+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (159, '2026-04-16 11:15:41.830132+00', '2026-04-16 11:15:41.83017+00', NULL, 23, 'smile 12', '-', 5400, 50, 10, 'Rp', 155, '5e33f6cd553b', 'smile-12-5e33f6cd553b', '2026-04-16 11:15:41.830132+00', '2026-04-16 11:15:41.83017+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (160, '2026-04-16 11:19:08.767471+00', '2026-04-16 11:19:08.76753+00', NULL, 21, 'tradisi super 2', '-', 3100, 20, 50, 'Rp', 156, '1201710281ea', 'tradisi-super-2-1201710281ea', '2026-04-16 11:19:08.767471+00', '2026-04-16 11:19:08.76753+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (161, '2026-04-16 11:21:31.358661+00', '2026-04-16 11:21:31.358704+00', NULL, 22, 'baskom panda super', '-', 7200, 40, 50, 'Rp', 158, 'ef4417723e65', 'baskom-panda-super-ef4417723e65', '2026-04-16 11:21:31.358661+00', '2026-04-16 11:21:31.358704+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (106, '2026-04-13 08:22:01.32961+00', '2026-04-17 06:57:48.793232+00', NULL, 29, 'Baskom TM', '-', 5900, 50, 10, 'Rp', 114, '8cfe593e75af', 'baskom-tm-8cfe593e75af', '2026-04-13 08:22:01.32961+00', '2026-04-17 06:57:48.793232+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (162, '2026-04-17 07:03:38.041905+00', '2026-04-17 07:03:38.041946+00', NULL, 17, 'baskom bahtera TM', '-', 5500, 50, 43, 'Rp', 160, 'd15f0b4dd21e', 'baskom-bahtera-tm-d15f0b4dd21e', '2026-04-17 07:03:38.041905+00', '2026-04-17 07:03:38.041946+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (163, '2026-04-17 07:03:38.048674+00', '2026-04-17 07:03:38.04871+00', NULL, 28, 'baskom jago 12', '-', 5300, 50, 5, 'Rp', 160, '082981805e0e', 'baskom-jago-12-082981805e0e', '2026-04-17 07:03:38.048674+00', '2026-04-17 07:03:38.04871+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (164, '2026-04-17 07:03:38.052619+00', '2026-04-17 07:03:38.052648+00', NULL, 23, 'smile 12', '-', 5400, 50, 5, 'Rp', 160, 'b92d8b1c6d32', 'smile-12-b92d8b1c6d32', '2026-04-17 07:03:38.052619+00', '2026-04-17 07:03:38.052648+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (165, '2026-04-17 08:35:23.272346+00', '2026-04-17 08:35:23.272394+00', NULL, 18, 'bak kuping12', '-', 13000, 30, 5, 'Rp', 149, '9476a578ceb4', 'bak-kuping12-9476a578ceb4', '2026-04-17 08:35:23.272346+00', '2026-04-17 08:35:23.272394+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (166, '2026-04-17 08:35:23.276372+00', '2026-04-17 08:35:23.276409+00', NULL, 23, 'smile 12', '-', 5400, 50, 5, 'Rp', 149, 'ad30baa308c2', 'smile-12-ad30baa308c2', '2026-04-17 08:35:23.276372+00', '2026-04-17 08:35:23.276409+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (167, '2026-04-17 08:35:23.278972+00', '2026-04-17 08:35:23.279001+00', NULL, 17, 'baskom bahtera TM', '-', 5500, 50, 3, 'Rp', 149, 'f1326f4849a4', 'baskom-bahtera-tm-f1326f4849a4', '2026-04-17 08:35:23.278972+00', '2026-04-17 08:35:23.279001+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (168, '2026-04-17 08:38:33.973786+00', '2026-04-17 08:38:33.973831+00', NULL, 17, 'baskom bahtera TM', '-', 5800, 50, 15, 'Rp', 163, 'cba8915698ac', 'baskom-bahtera-tm-cba8915698ac', '2026-04-17 08:38:33.973786+00', '2026-04-17 08:38:33.973831+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (169, '2026-04-17 08:38:33.976633+00', '2026-04-17 08:38:33.976675+00', NULL, 13, 'baskom panda', '-', 7500, 40, 15, 'Rp', 163, '506417ec2cb1', 'baskom-panda-506417ec2cb1', '2026-04-17 08:38:33.976633+00', '2026-04-17 08:38:33.976675+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (170, '2026-04-17 08:38:33.979239+00', '2026-04-17 08:38:33.979266+00', NULL, 30, 'Baskom Durian', '-', 8200, 40, 15, 'Rp', 163, 'c8689e9c597d', 'baskom-durian-c8689e9c597d', '2026-04-17 08:38:33.979239+00', '2026-04-17 08:38:33.979266+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (171, '2026-04-17 08:38:33.981989+00', '2026-04-17 08:38:33.982017+00', NULL, 20, 'baskom jago', '-', 5500, 50, 10, 'Rp', 163, 'f54f6d129637', 'baskom-jago-f54f6d129637', '2026-04-17 08:38:33.981989+00', '2026-04-17 08:38:33.982017+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (172, '2026-04-17 08:40:24.049751+00', '2026-04-17 08:40:24.049792+00', NULL, 18, 'bak kuping12', '-', 12000, 30, 85, 'Rp', 164, '17b519f0b497', 'bak-kuping12-17b519f0b497', '2026-04-17 08:40:24.049751+00', '2026-04-17 08:40:24.049792+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (173, '2026-04-17 08:42:44.99413+00', '2026-04-17 08:42:44.994184+00', NULL, 38, 'Smile 14', 'Lusin', 8200, 40, 20, 'Rp', 165, 'a14009a24085', 'smile-14-a14009a24085', '2026-04-17 08:42:44.99413+00', '2026-04-17 08:42:44.994184+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (174, '2026-04-17 08:45:41.930755+00', '2026-04-17 08:45:41.930813+00', NULL, 17, 'baskom bahtera TM', '-', 5500, 50, 48, 'Rp', 166, '12da7dbc98b4', 'baskom-bahtera-tm-12da7dbc98b4', '2026-04-17 08:45:41.930755+00', '2026-04-17 08:45:41.930813+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (175, '2026-04-17 08:51:58.479934+00', '2026-04-17 08:51:58.480066+00', NULL, 36, 'telor japar', '-', 2100, 20, 100, 'Rp', 167, 'f05135291124', 'telor-japar-f05135291124', '2026-04-17 08:51:58.479934+00', '2026-04-17 08:51:58.480066+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (176, '2026-04-17 08:51:58.491689+00', '2026-04-17 08:51:58.491816+00', NULL, 24, 'wakul rehana', '-', 4000, 20, 50, 'Rp', 167, 'c3cbbac0aefe', 'wakul-rehana-c3cbbac0aefe', '2026-04-17 08:51:58.491689+00', '2026-04-17 08:51:58.491816+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (177, '2026-04-17 08:51:58.499748+00', '2026-04-17 08:51:58.499838+00', NULL, 26, 'Bahkom Bahtera', '-', 5600, 50, 15, 'Rp', 167, '35a5e59e2ac0', 'bahkom-bahtera-35a5e59e2ac0', '2026-04-17 08:51:58.499748+00', '2026-04-17 08:51:58.499838+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (178, '2026-04-18 01:52:52.104105+00', '2026-04-18 02:10:56.738508+00', NULL, 22, 'baskom panda super', '-', 8400, 40, 10, 'Rp', 169, '823558c7fc68', 'baskom-panda-super-823558c7fc68', '2026-04-18 01:52:52.104105+00', '2026-04-18 02:10:56.738508+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (179, '2026-04-18 02:15:02.081452+00', '2026-04-18 02:15:02.081555+00', NULL, 20, 'baskom jago', '-', 7000, 50, 3, 'Rp', 169, '4bfe49304491', 'baskom-jago-4bfe49304491', '2026-04-18 02:15:02.081452+00', '2026-04-18 02:15:02.081555+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (180, '2026-04-18 02:15:02.091306+00', '2026-04-18 02:15:02.091394+00', NULL, 17, 'baskom bahtera TM', '-', 7000, 50, 10, 'Rp', 169, '9654aa5cf13f', 'baskom-bahtera-tm-9654aa5cf13f', '2026-04-18 02:15:02.091306+00', '2026-04-18 02:15:02.091394+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (181, '2026-04-18 02:15:02.099143+00', '2026-04-18 02:15:02.099232+00', NULL, 18, 'bak kuping12', '-', 14000, 30, 5, 'Rp', 169, '0d543cc9da0e', 'bak-kuping12-0d543cc9da0e', '2026-04-18 02:15:02.099143+00', '2026-04-18 02:15:02.099232+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (183, '2026-04-19 10:28:56.841153+00', '2026-04-19 10:28:56.841195+00', NULL, 25, 'baskom mawar', '-', 7200, 50, 20, 'Rp', 170, '0deb43ebab06', 'baskom-mawar-0deb43ebab06', '2026-04-19 10:28:56.841153+00', '2026-04-19 10:28:56.841195+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (184, '2026-04-19 10:28:56.847212+00', '2026-04-19 10:28:56.847256+00', NULL, 17, 'baskom bahtera TM', '-', 7000, 50, 10, 'Rp', 170, 'ebdc66900d09', 'baskom-bahtera-tm-ebdc66900d09', '2026-04-19 10:28:56.847212+00', '2026-04-19 10:28:56.847256+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (182, '2026-04-18 02:16:41.9616+00', '2026-04-20 01:17:20.46189+00', NULL, 39, 'tradisi cerah', 'Lusin', 5000, 30, 5, 'Rp', 169, 'ce30a26aac50', 'tradisi-cerah-ce30a26aac50', '2026-04-18 02:16:41.9616+00', '2026-04-20 01:17:20.46189+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (185, '2026-04-20 01:33:34.144417+00', '2026-04-20 01:33:34.144516+00', NULL, 22, 'baskom panda super', '-', 8400, 40, 30, 'Rp', 172, '41ff5cfe41d2', 'baskom-panda-super-41ff5cfe41d2', '2026-04-20 01:33:34.144417+00', '2026-04-20 01:33:34.144516+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (186, '2026-04-20 01:33:34.152978+00', '2026-04-20 01:33:34.153073+00', NULL, 30, 'Baskom Durian', '-', 8900, 40, 5, 'Rp', 172, '8265cd64a155', 'baskom-durian-8265cd64a155', '2026-04-20 01:33:34.152978+00', '2026-04-20 01:33:34.153073+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (187, '2026-04-20 01:33:34.160491+00', '2026-04-20 01:33:34.160569+00', NULL, 26, 'Bahkom Bahtera', '-', 7500, 50, 5, 'Rp', 172, 'dd28fb01e30d', 'bahkom-bahtera-dd28fb01e30d', '2026-04-20 01:33:34.160491+00', '2026-04-20 01:33:34.160569+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (188, '2026-04-20 01:33:34.169457+00', '2026-04-20 01:33:34.169532+00', NULL, 16, 'baskom mawar', '-', 7300, 50, 10, 'Rp', 172, '0607a011be7c', 'baskom-mawar-0607a011be7c', '2026-04-20 01:33:34.169457+00', '2026-04-20 01:33:34.169532+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (189, '2026-04-20 01:36:24.117648+00', '2026-04-20 01:36:24.117741+00', NULL, 26, 'Bahkom Bahtera', '-', 5900, 50, 5, 'Rp', 173, '2191bca1fba5', 'bahkom-bahtera-2191bca1fba5', '2026-04-20 01:36:24.117648+00', '2026-04-20 01:36:24.117741+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (190, '2026-04-20 01:36:24.128512+00', '2026-04-20 01:36:24.128597+00', NULL, 17, 'baskom bahtera TM', '-', 5400, 50, 5, 'Rp', 173, '5eb4714b25e9', 'baskom-bahtera-tm-5eb4714b25e9', '2026-04-20 01:36:24.128512+00', '2026-04-20 01:36:24.128597+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (191, '2026-04-20 01:38:56.143056+00', '2026-04-20 01:38:56.143163+00', NULL, 17, 'baskom bahtera TM', '-', 5500, 50, 10, 'Rp', 174, '94bb9f524eff', 'baskom-bahtera-tm-94bb9f524eff', '2026-04-20 01:38:56.143056+00', '2026-04-20 01:38:56.143163+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (192, '2026-04-20 01:38:56.150829+00', '2026-04-20 01:38:56.150902+00', NULL, 28, 'baskom jago 12', '-', 5300, 50, 10, 'Rp', 174, 'af103ff79e47', 'baskom-jago-12-af103ff79e47', '2026-04-20 01:38:56.150829+00', '2026-04-20 01:38:56.150902+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (193, '2026-04-20 01:38:56.159089+00', '2026-04-20 01:38:56.159185+00', NULL, 25, 'baskom mawar', '-', 5800, 50, 5, 'Rp', 174, '875c56a13840', 'baskom-mawar-875c56a13840', '2026-04-20 01:38:56.159089+00', '2026-04-20 01:38:56.159185+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (194, '2026-04-20 01:40:46.273148+00', '2026-04-20 01:40:46.273232+00', NULL, 18, 'bak kuping12', '-', 13000, 30, 2, 'Rp', 175, '6a52b3e7b1d0', 'bak-kuping12-6a52b3e7b1d0', '2026-04-20 01:40:46.273148+00', '2026-04-20 01:40:46.273232+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (195, '2026-04-20 02:02:09.024885+00', '2026-04-20 02:02:09.024928+00', NULL, 17, 'baskom bahtera TM', '-', 5700, 50, 11, 'Rp', 179, '97e6f63fb8be', 'baskom-bahtera-tm-97e6f63fb8be', '2026-04-20 02:02:09.024885+00', '2026-04-20 02:02:09.024928+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (196, '2026-04-20 02:02:09.029872+00', '2026-04-20 02:02:09.029909+00', NULL, 22, 'baskom panda super', '-', 7200, 40, 10, 'Rp', 179, 'ef4eb8c60b12', 'baskom-panda-super-ef4eb8c60b12', '2026-04-20 02:02:09.029872+00', '2026-04-20 02:02:09.029909+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (200, '2026-04-20 04:35:57.164122+00', '2026-04-20 04:35:57.164175+00', NULL, 17, 'baskom bahtera TM', '-', 6750, 50, 15, 'Rp', 181, '3eebf179353f', 'baskom-bahtera-tm-3eebf179353f', '2026-04-20 04:35:57.164122+00', '2026-04-20 04:35:57.164175+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (201, '2026-04-20 04:35:57.174923+00', '2026-04-20 04:35:57.174993+00', NULL, 16, 'baskom mawar', '-', 7050, 50, 15, 'Rp', 181, '4cf850dc514b', 'baskom-mawar-4cf850dc514b', '2026-04-20 04:35:57.174923+00', '2026-04-20 04:35:57.174993+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (202, '2026-04-20 04:35:57.180485+00', '2026-04-20 04:35:57.180521+00', NULL, 23, 'smile 12', '-', 6650, 50, 15, 'Rp', 181, '7b4b20315509', 'smile-12-7b4b20315509', '2026-04-20 04:35:57.180485+00', '2026-04-20 04:35:57.180521+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (197, '2026-04-20 04:30:38.336837+00', '2026-04-20 04:30:38.336896+00', '2026-05-29 15:19:37.99138+00', 40, 'Baskom Barca', 'Lusin', 7250, 50, 15, 'Rp', 180, '63f730819cff', 'baskom-barca-63f730819cff', '2026-04-20 04:30:38.336837+00', '2026-04-20 04:30:38.336896+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (198, '2026-04-20 04:30:38.342423+00', '2026-04-20 04:30:38.342459+00', '2026-05-29 15:19:37.99138+00', 18, 'bak kuping12', '-', 14750, 30, 6, 'Rp', 180, '16f935ba8d16', 'bak-kuping12-16f935ba8d16', '2026-04-20 04:30:38.342423+00', '2026-04-20 04:30:38.342459+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (207, '2026-04-20 04:44:21.727582+00', '2026-04-20 04:44:21.727633+00', NULL, 17, 'baskom bahtera TM', '-', 6750, 50, 15, 'Rp', 183, '303e213e8e0f', 'baskom-bahtera-tm-303e213e8e0f', '2026-04-20 04:44:21.727582+00', '2026-04-20 04:44:21.727633+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (208, '2026-04-20 04:44:21.737006+00', '2026-04-20 04:44:21.73705+00', NULL, 16, 'baskom mawar', '-', 7050, 50, 15, 'Rp', 183, 'a866051992c6', 'baskom-mawar-a866051992c6', '2026-04-20 04:44:21.737006+00', '2026-04-20 04:44:21.73705+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (209, '2026-04-20 04:44:21.741892+00', '2026-04-20 04:44:21.74192+00', NULL, 23, 'smile 12', '-', 6650, 50, 15, 'Rp', 183, 'e3e195d2982f', 'smile-12-e3e195d2982f', '2026-04-20 04:44:21.741892+00', '2026-04-20 04:44:21.74192+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (210, '2026-04-20 05:02:58.440146+00', '2026-04-20 05:02:58.4402+00', NULL, 41, 'Wakul Tanggok', 'Lusin', 5800, 50, 1, 'Rp', 167, '9a655df17f85', 'wakul-tanggok-9a655df17f85', '2026-04-20 05:02:58.440146+00', '2026-04-20 05:02:58.4402+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (211, '2026-04-20 05:06:48.811152+00', '2026-04-20 05:06:48.811201+00', NULL, 26, 'Bahkom Bahtera', '-', 5600, 50, 5, 'Rp', 184, 'c7b7875afece', 'bahkom-bahtera-c7b7875afece', '2026-04-20 05:06:48.811152+00', '2026-04-20 05:06:48.811201+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (212, '2026-04-20 05:06:48.818024+00', '2026-04-20 05:06:48.818062+00', NULL, 31, 'wakul tradisi super', '-', 3100, 40, 30, 'Rp', 184, 'a06e62231689', 'wakul-tradisi-super-a06e62231689', '2026-04-20 05:06:48.818024+00', '2026-04-20 05:06:48.818062+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (213, '2026-04-20 05:06:48.822367+00', '2026-04-20 05:06:48.822403+00', NULL, 39, 'tradisi cerah', 'Lusin', 4200, 30, 20, 'Rp', 184, '44c24ef50126', 'tradisi-cerah-44c24ef50126', '2026-04-20 05:06:48.822367+00', '2026-04-20 05:06:48.822403+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (214, '2026-04-20 05:06:48.829293+00', '2026-04-20 05:06:48.82933+00', NULL, 36, 'telor japar', '-', 2100, 20, 200, 'Rp', 184, '2b87c93df5c7', 'telor-japar-2b87c93df5c7', '2026-04-20 05:06:48.829293+00', '2026-04-20 05:06:48.82933+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (215, '2026-04-20 05:13:40.70712+00', '2026-04-20 05:13:40.707169+00', NULL, 21, 'tradisi super 2', '-', 3100, 40, 32, 'Rp', 185, '26fb97ac4a8e', 'tradisi-super-2-26fb97ac4a8e', '2026-04-20 05:13:40.70712+00', '2026-04-20 05:13:40.707169+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (216, '2026-04-20 05:13:40.712783+00', '2026-04-20 05:13:40.712822+00', NULL, 13, 'baskom panda', '-', 7200, 40, 7, 'Rp', 185, '9dadc44071ca', 'baskom-panda-9dadc44071ca', '2026-04-20 05:13:40.712783+00', '2026-04-20 05:13:40.712822+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (217, '2026-04-20 05:17:41.578683+00', '2026-04-20 05:17:41.578732+00', NULL, 21, 'tradisi super 2', '-', 3100, 40, 32, 'Rp', 186, 'e95865d37e47', 'tradisi-super-2-e95865d37e47', '2026-04-20 05:17:41.578683+00', '2026-04-20 05:17:41.578732+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (218, '2026-04-20 05:17:41.583707+00', '2026-04-20 05:17:41.58375+00', NULL, 22, 'baskom panda super', '-', 7200, 40, 7, 'Rp', 186, '92709b9abbf0', 'baskom-panda-super-92709b9abbf0', '2026-04-20 05:17:41.583707+00', '2026-04-20 05:17:41.58375+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (219, '2026-04-20 05:17:41.587961+00', '2026-04-20 05:17:41.588006+00', NULL, 17, 'baskom bahtera TM', '-', 5400, 50, 5, 'Rp', 186, 'c014fe9a0f3e', 'baskom-bahtera-tm-c014fe9a0f3e', '2026-04-20 05:17:41.587961+00', '2026-04-20 05:17:41.588006+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (220, '2026-04-20 05:17:41.592703+00', '2026-04-20 05:17:41.592739+00', NULL, 24, 'wakul rehana', '-', 4000, 50, 10, 'Rp', 186, '684a6b175579', 'wakul-rehana-684a6b175579', '2026-04-20 05:17:41.592703+00', '2026-04-20 05:17:41.592739+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (221, '2026-04-20 05:17:41.596476+00', '2026-04-20 05:17:41.596517+00', NULL, 16, 'baskom mawar', '-', 5850, 50, 3, 'Rp', 186, 'd8c4b4a3ff49', 'baskom-mawar-d8c4b4a3ff49', '2026-04-20 05:17:41.596476+00', '2026-04-20 05:17:41.596517+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (222, '2026-04-20 05:20:58.370956+00', '2026-04-20 05:20:58.371025+00', NULL, 23, 'smile 12', '-', 5400, 50, 15, 'Rp', 187, 'f4209cb9a028', 'smile-12-f4209cb9a028', '2026-04-20 05:20:58.370956+00', '2026-04-20 05:20:58.371025+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (223, '2026-04-20 05:20:58.376282+00', '2026-04-20 05:20:58.376328+00', NULL, 17, 'baskom bahtera TM', '-', 5400, 50, 10, 'Rp', 187, '2482fb8db375', 'baskom-bahtera-tm-2482fb8db375', '2026-04-20 05:20:58.376282+00', '2026-04-20 05:20:58.376328+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (224, '2026-04-20 05:20:58.380581+00', '2026-04-20 05:20:58.38062+00', NULL, 22, 'baskom panda super', '-', 7200, 50, 4, 'Rp', 187, '1874a2da90fb', 'baskom-panda-super-1874a2da90fb', '2026-04-20 05:20:58.380581+00', '2026-04-20 05:20:58.38062+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (225, '2026-04-20 05:20:58.385282+00', '2026-04-20 05:20:58.385313+00', NULL, 31, 'wakul tradisi super', '-', 3100, 40, 8, 'Rp', 187, '62a36314824d', 'wakul-tradisi-super-62a36314824d', '2026-04-20 05:20:58.385282+00', '2026-04-20 05:20:58.385313+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (226, '2026-04-20 05:26:54.926593+00', '2026-04-20 05:26:54.926636+00', NULL, 37, 'telor tali', 'Lusin', 2100, 20, 70, 'Rp', 188, '6f632df678b9', 'telor-tali-6f632df678b9', '2026-04-20 05:26:54.926593+00', '2026-04-20 05:26:54.926636+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (227, '2026-04-20 05:42:13.408148+00', '2026-04-20 05:42:13.408212+00', NULL, 18, 'bak kuping12', '-', 13000, 50, 3, 'Rp', 190, 'b8bdc41abbda', 'bak-kuping12-b8bdc41abbda', '2026-04-20 05:42:13.408148+00', '2026-04-20 05:42:13.408212+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (228, '2026-04-20 05:42:13.419182+00', '2026-04-20 05:42:13.419237+00', NULL, 42, 'Baskom Rotan', 'Lusin', 8400, 40, 6, 'Rp', 190, '00ab3688529e', 'baskom-rotan-00ab3688529e', '2026-04-20 05:42:13.419182+00', '2026-04-20 05:42:13.419237+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (229, '2026-04-20 05:42:13.425577+00', '2026-04-20 05:42:13.425627+00', NULL, 26, 'Bahkom Bahtera', '-', 6500, 50, 5, 'Rp', 190, '7b309b9ba89c', 'bahkom-bahtera-7b309b9ba89c', '2026-04-20 05:42:13.425577+00', '2026-04-20 05:42:13.425627+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (230, '2026-04-20 05:42:13.433089+00', '2026-04-20 05:42:13.433132+00', NULL, 20, 'baskom jago', '-', 5800, 50, 5, 'Rp', 190, '9c977df0da96', 'baskom-jago-9c977df0da96', '2026-04-20 05:42:13.433089+00', '2026-04-20 05:42:13.433132+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (231, '2026-04-20 05:42:13.438511+00', '2026-04-20 05:42:13.438561+00', NULL, 23, 'smile 12', '-', 5800, 50, 5, 'Rp', 190, '81d091f02c40', 'smile-12-81d091f02c40', '2026-04-20 05:42:13.438511+00', '2026-04-20 05:42:13.438561+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (232, '2026-04-20 05:42:13.442898+00', '2026-04-20 05:42:13.442944+00', NULL, 36, 'telor japar', '-', 2400, 20, 5, 'Rp', 190, '7ff0960a8d5d', 'telor-japar-7ff0960a8d5d', '2026-04-20 05:42:13.442898+00', '2026-04-20 05:42:13.442944+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (233, '2026-04-20 05:48:49.217716+00', '2026-04-20 05:48:49.21777+00', NULL, 17, 'baskom bahtera TM', '-', 5400, 50, 15, 'Rp', 191, '38130e3940a8', 'baskom-bahtera-tm-38130e3940a8', '2026-04-20 05:48:49.217716+00', '2026-04-20 05:48:49.21777+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (234, '2026-04-20 05:48:49.223447+00', '2026-04-20 05:48:49.223484+00', NULL, 23, 'smile 12', '-', 5400, 50, 3, 'Rp', 191, 'fe7b27e62c29', 'smile-12-fe7b27e62c29', '2026-04-20 05:48:49.223447+00', '2026-04-20 05:48:49.223484+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (235, '2026-04-20 05:50:42.76462+00', '2026-04-20 05:50:42.764669+00', NULL, 26, 'Bahkom Bahtera', '-', 6100, 50, 51, 'Rp', 192, '8299d82592bd', 'bahkom-bahtera-8299d82592bd', '2026-04-20 05:50:42.76462+00', '2026-04-20 05:50:42.764669+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (236, '2026-04-20 05:50:42.769319+00', '2026-04-20 05:50:42.769365+00', NULL, 31, 'wakul tradisi super', '-', 3300, 40, 100, 'Rp', 192, '820b76d28798', 'wakul-tradisi-super-820b76d28798', '2026-04-20 05:50:42.769319+00', '2026-04-20 05:50:42.769365+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (237, '2026-04-20 05:56:37.952612+00', '2026-04-20 05:56:37.95266+00', NULL, 20, 'baskom jago', '-', 5600, 50, 100, 'Rp', 193, 'b3dca834903e', 'baskom-jago-b3dca834903e', '2026-04-20 05:56:37.952612+00', '2026-04-20 05:56:37.95266+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (238, '2026-04-20 06:32:46.214996+00', '2026-04-20 06:32:46.215047+00', NULL, 36, 'telor japar', '-', 2100, 20, 50, 'Rp', 194, '79f00e14a83e', 'telor-japar-79f00e14a83e', '2026-04-20 06:32:46.214996+00', '2026-04-20 06:32:46.215047+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (239, '2026-04-20 06:32:46.21974+00', '2026-04-20 06:32:46.219775+00', NULL, 31, 'wakul tradisi super', '-', 3300, 20, 150, 'Rp', 194, 'e6aeeb53643f', 'wakul-tradisi-super-e6aeeb53643f', '2026-04-20 06:32:46.21974+00', '2026-04-20 06:32:46.219775+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (240, '2026-04-20 11:36:57.254135+00', '2026-04-20 11:36:57.254183+00', NULL, 43, 'Wakul Mawar Super', 'Lusin', 3600, 50, 20, 'Rp', 195, '36617920434e', 'wakul-mawar-super-36617920434e', '2026-04-20 11:36:57.254135+00', '2026-04-20 11:36:57.254183+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (241, '2026-04-20 11:39:54.391348+00', '2026-04-20 11:39:54.391389+00', NULL, 38, 'Smile 14', 'Lusin', 8400, 40, 15, 'Rp', 195, '5683209e656b', 'smile-14-5683209e656b', '2026-04-20 11:39:54.391348+00', '2026-04-20 11:39:54.391389+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (242, '2026-04-20 11:44:34.319006+00', '2026-04-20 11:44:34.319055+00', NULL, 45, 'Wakul Morris Super', 'Lusin', 5700, 40, 15, 'Rp', 195, '8b72a72a5ff2', 'wakul-morris-super-8b72a72a5ff2', '2026-04-20 11:44:34.319006+00', '2026-04-20 11:44:34.319055+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (243, '2026-04-20 11:44:34.32425+00', '2026-04-20 11:44:34.324292+00', NULL, 22, 'baskom panda super', '-', 7250, 40, 10, 'Rp', 195, '722a459f52fc', 'baskom-panda-super-722a459f52fc', '2026-04-20 11:44:34.32425+00', '2026-04-20 11:44:34.324292+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (244, '2026-04-20 11:44:34.330031+00', '2026-04-20 11:44:34.33007+00', NULL, 17, 'baskom bahtera TM', '-', 5800, 50, 15, 'Rp', 195, '995634e6e91c', 'baskom-bahtera-tm-995634e6e91c', '2026-04-20 11:44:34.330031+00', '2026-04-20 11:44:34.33007+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (245, '2026-04-20 11:44:34.334601+00', '2026-04-20 11:44:34.334637+00', NULL, 23, 'smile 12', '-', 5700, 50, 10, 'Rp', 195, '30b03b79b75c', 'smile-12-30b03b79b75c', '2026-04-20 11:44:34.334601+00', '2026-04-20 11:44:34.334637+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (246, '2026-04-20 11:50:53.459669+00', '2026-04-20 11:50:53.459723+00', NULL, 20, 'baskom jago', '-', 5400, 50, 107, 'Rp', 196, '6bf28d847a43', 'baskom-jago-6bf28d847a43', '2026-04-20 11:50:53.459669+00', '2026-04-20 11:50:53.459723+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (247, '2026-04-20 11:50:53.464687+00', '2026-04-20 11:50:53.464731+00', NULL, 26, 'Bahkom Bahtera', '-', 5900, 50, 30, 'Rp', 196, '29c011670396', 'bahkom-bahtera-29c011670396', '2026-04-20 11:50:53.464687+00', '2026-04-20 11:50:53.464731+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (248, '2026-04-20 11:50:53.470271+00', '2026-04-20 11:50:53.47031+00', NULL, 13, 'baskom panda', '-', 7200, 40, 10, 'Rp', 196, 'db9e2b55b9e3', 'baskom-panda-db9e2b55b9e3', '2026-04-20 11:50:53.470271+00', '2026-04-20 11:50:53.47031+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (249, '2026-04-20 11:50:53.474833+00', '2026-04-20 11:50:53.474872+00', NULL, 36, 'telor japar', '-', 2100, 20, 58, 'Rp', 196, '5291ee737b25', 'telor-japar-5291ee737b25', '2026-04-20 11:50:53.474833+00', '2026-04-20 11:50:53.474872+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (250, '2026-04-20 11:50:53.478768+00', '2026-04-20 11:50:53.478799+00', NULL, 37, 'telor tali', 'Lusin', 3200, 20, 128, 'Rp', 196, '68d146af2205', 'telor-tali-68d146af2205', '2026-04-20 11:50:53.478768+00', '2026-04-20 11:50:53.478799+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (252, '2026-04-20 11:54:32.516737+00', '2026-04-20 11:54:42.676477+00', NULL, 17, 'baskom bahtera TM', '-', 5850, 50, 50, 'Rp', 197, '0e543178f0a6', 'baskom-bahtera-tm-0e543178f0a6', '2026-04-20 11:54:32.516737+00', '2026-04-20 11:54:42.676477+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (253, '2026-04-20 11:55:47.48321+00', '2026-04-20 11:55:47.48328+00', NULL, 16, 'baskom mawar', '-', 5900, 50, 20, 'Rp', 197, '545c4d71743d', 'baskom-mawar-545c4d71743d', '2026-04-20 11:55:47.48321+00', '2026-04-20 11:55:47.48328+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (254, '2026-04-20 11:56:46.601706+00', '2026-04-20 11:56:46.601766+00', NULL, 45, 'Wakul Morris Super', 'Lusin', 5750, 40, 25, 'Rp', 197, 'a4934cfc75e9', 'wakul-morris-super-a4934cfc75e9', '2026-04-20 11:56:46.601706+00', '2026-04-20 11:56:46.601766+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (255, '2026-04-20 11:58:34.725574+00', '2026-04-20 11:58:34.725626+00', NULL, 26, 'Baskom Bahtera', '-', 6100, 50, 60, 'Rp', 197, '102cf89c7ff7', 'baskom-bahtera-102cf89c7ff7', '2026-04-20 11:58:34.725574+00', '2026-04-20 11:58:34.725626+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (256, '2026-04-20 12:05:36.457295+00', '2026-04-20 12:05:36.457348+00', NULL, 23, 'smile 12', '-', 6700, 50, 25, 'Rp', 198, '8fbe43f563e4', 'smile-12-8fbe43f563e4', '2026-04-20 12:05:36.457295+00', '2026-04-20 12:05:36.457348+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (257, '2026-04-20 12:05:36.462711+00', '2026-04-20 12:06:11.213586+00', NULL, 16, 'baskom mawar', '-', 7100, 50, 5, 'Rp', 198, '898a83a2c8e4', 'baskom-mawar-898a83a2c8e4', '2026-04-20 12:05:36.462711+00', '2026-04-20 12:06:11.213586+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (258, '2026-04-20 12:08:53.784296+00', '2026-04-20 12:08:53.78434+00', NULL, 26, 'Baskom Bahtera', '-', 7500, 50, 5, 'Rp', 198, 'a58a2b686524', 'baskom-bahtera-a58a2b686524', '2026-04-20 12:08:53.784296+00', '2026-04-20 12:08:53.78434+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (259, '2026-04-20 12:08:53.789854+00', '2026-04-20 12:08:53.789885+00', NULL, 30, 'Baskom Durian', '-', 8900, 40, 5, 'Rp', 198, '2c574d987ff7', 'baskom-durian-2c574d987ff7', '2026-04-20 12:08:53.789854+00', '2026-04-20 12:08:53.789885+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (260, '2026-04-21 09:51:03.164359+00', '2026-04-21 09:51:03.164415+00', NULL, 36, 'telor japar', '-', 2600, 20, 70, 'Rp', 200, '9a62ba0ba35b', 'telor-japar-9a62ba0ba35b', '2026-04-21 09:51:03.164359+00', '2026-04-21 09:51:03.164415+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (261, '2026-04-21 09:51:03.168239+00', '2026-04-21 09:51:03.168276+00', NULL, 37, 'telor tali', 'Lusin', 3700, 20, 80, 'Rp', 200, '652d2a1988fd', 'telor-tali-652d2a1988fd', '2026-04-21 09:51:03.168239+00', '2026-04-21 09:51:03.168276+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (262, '2026-04-21 09:51:03.171992+00', '2026-04-21 09:51:03.172045+00', NULL, 23, 'smile 12', '-', 6900, 50, 2, 'Rp', 200, '88657a23c015', 'smile-12-88657a23c015', '2026-04-21 09:51:03.171992+00', '2026-04-21 09:51:03.172045+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (263, '2026-04-21 09:51:03.174927+00', '2026-04-21 09:51:03.17496+00', NULL, 26, 'Baskom Bahtera', '-', 7300, 50, 50, 'Rp', 200, 'f13f2946b435', 'baskom-bahtera-f13f2946b435', '2026-04-21 09:51:03.174927+00', '2026-04-21 09:51:03.17496+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (264, '2026-04-21 09:51:03.177449+00', '2026-04-21 09:51:03.177479+00', NULL, 20, 'baskom jago', '-', 7000, 50, 115, 'Rp', 200, 'bae39e72b60e', 'baskom-jago-bae39e72b60e', '2026-04-21 09:51:03.177449+00', '2026-04-21 09:51:03.177479+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (265, '2026-04-21 09:51:03.182178+00', '2026-04-21 09:51:03.182208+00', NULL, 22, 'baskom panda super', '-', 8500, 40, 6, 'Rp', 200, 'd5f87a9f71cc', 'baskom-panda-super-d5f87a9f71cc', '2026-04-21 09:51:03.182178+00', '2026-04-21 09:51:03.182208+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (266, '2026-04-21 13:51:42.900314+00', '2026-04-21 13:51:42.900457+00', NULL, 34, 'Tradisi Super 30', '-', 3600, 30, 30, 'Rp', 202, '4b2b85bf759e', 'tradisi-super-30-4b2b85bf759e', '2026-04-21 13:51:42.900314+00', '2026-04-21 13:51:42.900457+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (267, '2026-04-21 13:51:42.911593+00', '2026-04-21 13:51:42.911705+00', NULL, 36, 'telor japar', '-', 2500, 20, 27, 'Rp', 202, 'cddfe957b585', 'telor-japar-cddfe957b585', '2026-04-21 13:51:42.911593+00', '2026-04-21 13:51:42.911705+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (268, '2026-04-21 13:51:42.921354+00', '2026-04-21 13:51:42.921505+00', NULL, 17, 'baskom bahtera TM', '-', 7000, 50, 5, 'Rp', 202, '342b9668ae8a', 'baskom-bahtera-tm-342b9668ae8a', '2026-04-21 13:51:42.921354+00', '2026-04-21 13:51:42.921505+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (269, '2026-04-21 13:51:42.929166+00', '2026-04-21 13:51:42.929251+00', NULL, 13, 'baskom panda', '-', 8400, 40, 5, 'Rp', 202, '9ea97c5ab9a9', 'baskom-panda-9ea97c5ab9a9', '2026-04-21 13:51:42.929166+00', '2026-04-21 13:51:42.929251+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (270, '2026-04-23 01:40:46.826401+00', '2026-04-23 01:40:46.826449+00', NULL, 26, 'Baskom Bahtera', '-', 7700, 50, 4, 'Rp', 203, 'b6b5190a86ef', 'baskom-bahtera-b6b5190a86ef', '2026-04-23 01:40:46.826401+00', '2026-04-23 01:40:46.826449+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (271, '2026-04-23 01:40:46.838358+00', '2026-04-23 01:40:46.838403+00', NULL, 16, 'baskom mawar', '-', 7500, 50, 1, 'Rp', 203, 'abd3bd257f88', 'baskom-mawar-abd3bd257f88', '2026-04-23 01:40:46.838358+00', '2026-04-23 01:40:46.838403+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (272, '2026-04-24 05:52:58.003257+00', '2026-04-24 05:52:58.003299+00', NULL, 24, 'wakul rehana', '-', 4400, 50, 20, 'Rp', 205, '3e8ab106b3e5', 'wakul-rehana-3e8ab106b3e5', '2026-04-24 05:52:58.003257+00', '2026-04-24 05:52:58.003299+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (273, '2026-04-24 05:52:58.007528+00', '2026-04-24 05:52:58.007566+00', NULL, 17, 'baskom bahtera TM', '-', 7000, 50, 5, 'Rp', 205, '365380d361ed', 'baskom-bahtera-tm-365380d361ed', '2026-04-24 05:52:58.007528+00', '2026-04-24 05:52:58.007566+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (274, '2026-04-27 03:01:50.767327+00', '2026-04-27 03:01:50.767388+00', NULL, 18, 'bak kuping12', '-', 14300, 30, 14, 'Rp', 206, '76a146dbd17d', 'bak-kuping12-76a146dbd17d', '2026-04-27 03:01:50.767327+00', '2026-04-27 03:01:50.767388+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (275, '2026-04-27 03:01:50.771484+00', '2026-04-27 03:01:50.771532+00', NULL, 16, 'baskom mawar', '-', 7600, 50, 25, 'Rp', 206, '3f7c0b20829c', 'baskom-mawar-3f7c0b20829c', '2026-04-27 03:01:50.771484+00', '2026-04-27 03:01:50.771532+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (276, '2026-04-27 03:01:50.776686+00', '2026-04-27 03:01:50.776731+00', NULL, 17, 'baskom bahtera TM', '-', 9250, 40, 40, 'Rp', 206, '8b6b7939857c', 'baskom-bahtera-tm-8b6b7939857c', '2026-04-27 03:01:50.776686+00', '2026-04-27 03:01:50.776731+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (277, '2026-04-27 03:37:35.958759+00', '2026-04-27 03:41:15.134392+00', NULL, 17, 'baskom bahtera TM', '-', 7250, 50, 90, 'Rp', 207, '980706cda58b', 'baskom-bahtera-tm-980706cda58b', '2026-04-27 03:37:35.958759+00', '2026-04-27 03:41:15.134392+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (278, '2026-04-28 01:46:32.735882+00', '2026-04-28 01:46:32.735941+00', NULL, 16, 'baskom mawar', '-', 7200, 50, 15, 'Rp', 209, '8aa523519b37', 'baskom-mawar-8aa523519b37', '2026-04-28 01:46:32.735882+00', '2026-04-28 01:46:32.735941+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (279, '2026-04-28 01:46:32.739371+00', '2026-04-28 01:46:32.739405+00', NULL, 17, 'baskom bahtera TM', '-', 7000, 50, 10, 'Rp', 209, '660c6aac3430', 'baskom-bahtera-tm-660c6aac3430', '2026-04-28 01:46:32.739371+00', '2026-04-28 01:46:32.739405+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (280, '2026-04-28 01:46:32.74282+00', '2026-04-28 01:46:32.742864+00', NULL, 20, 'baskom jago', '-', 7000, 50, 10, 'Rp', 209, '3ef6efd54def', 'baskom-jago-3ef6efd54def', '2026-04-28 01:46:32.74282+00', '2026-04-28 01:46:32.742864+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (281, '2026-04-28 03:09:55.252988+00', '2026-04-28 03:09:55.253034+00', NULL, 26, 'Baskom Bahtera', '-', 7700, 50, 20, 'Rp', 210, '296064e108e5', 'baskom-bahtera-296064e108e5', '2026-04-28 03:09:55.252988+00', '2026-04-28 03:09:55.253034+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (282, '2026-04-28 03:09:55.256648+00', '2026-04-28 03:09:55.256678+00', NULL, 13, 'baskom panda', '-', 8600, 40, 20, 'Rp', 210, '7ae71a14be1f', 'baskom-panda-7ae71a14be1f', '2026-04-28 03:09:55.256648+00', '2026-04-28 03:09:55.256678+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (283, '2026-04-28 03:09:55.259933+00', '2026-04-28 03:09:55.25997+00', NULL, 16, 'baskom mawar', '-', 7400, 50, 10, 'Rp', 210, 'ec45f602afe9', 'baskom-mawar-ec45f602afe9', '2026-04-28 03:09:55.259933+00', '2026-04-28 03:09:55.25997+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (449, '2026-05-26 05:49:31.531519+00', '2026-05-26 05:49:31.531519+00', '2026-05-26 08:09:13.461233+00', 51, 'Baskom Panda Cerah', 'Lusin', 10000, 40, 5, 'Rp', 259, '3b637d61', 'BMP-2605-010-c94477e0-ec660179', '2026-05-26 05:49:31.531497+00', '2026-05-26 05:49:31.531498+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (450, '2026-05-26 05:49:31.541446+00', '2026-05-26 05:49:31.541446+00', '2026-05-26 08:09:13.461233+00', 37, 'telor tali', 'Lusin', 3700, 20, 80, 'Rp', 259, '10ab9f60', 'BMP-2605-010-c94477e0-8aae9605', '2026-05-26 05:49:31.541433+00', '2026-05-26 05:49:31.541433+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (469, '2026-05-26 09:09:04.305897+00', '2026-05-26 09:09:04.305897+00', '2026-05-26 09:10:18.649988+00', 20, 'baskom jago', 'Lusin', 7000, 50, 120, 'Rp', 259, 'ffda5ad1', 'BMP-2605-010-c94477e0-3a7ff5a3', '2026-05-26 09:09:04.305869+00', '2026-05-26 09:09:04.305869+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (470, '2026-05-26 09:09:04.318649+00', '2026-05-26 09:09:04.318649+00', '2026-05-26 09:10:18.649988+00', 26, 'Baskom Bahtera', 'Lusin', 7300, 50, 20, 'Rp', 259, '5af460db', 'BMP-2605-010-c94477e0-53d09059', '2026-05-26 09:09:04.318637+00', '2026-05-26 09:09:04.318637+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (471, '2026-05-26 09:09:04.326857+00', '2026-05-26 09:09:04.326857+00', '2026-05-26 09:10:18.649988+00', 23, 'smile 12', 'Lusin', 6900, 50, 15, 'Rp', 259, 'c9af326e', 'BMP-2605-010-c94477e0-815cad8f', '2026-05-26 09:09:04.326848+00', '2026-05-26 09:09:04.326848+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (472, '2026-05-26 09:09:04.335192+00', '2026-05-26 09:09:04.335192+00', '2026-05-26 09:10:18.649988+00', 38, 'Smile 14', 'Lusin', 9300, 50, 36, 'Rp', 259, 'c242e094', 'BMP-2605-010-c94477e0-5068389a', '2026-05-26 09:09:04.335181+00', '2026-05-26 09:09:04.335181+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (473, '2026-05-26 09:09:04.343409+00', '2026-05-26 09:09:04.343409+00', '2026-05-26 09:10:18.649988+00', 51, 'Baskom Panda Cerah', 'Lusin', 10000, 40, 5, 'Rp', 259, '4a946c46', 'BMP-2605-010-c94477e0-2ca701f1', '2026-05-26 09:09:04.343398+00', '2026-05-26 09:09:04.343398+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (474, '2026-05-26 09:09:04.351663+00', '2026-05-26 09:09:04.351663+00', '2026-05-26 09:10:18.649988+00', 37, 'telor tali', 'Lusin', 3700, 20, 97, 'Rp', 259, '2bd463c0', 'BMP-2605-010-c94477e0-d8907998', '2026-05-26 09:09:04.351654+00', '2026-05-26 09:09:04.351654+00', true, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (445, '2026-05-26 05:49:31.490687+00', '2026-05-26 05:49:31.490687+00', '2026-05-26 08:09:13.461233+00', 20, 'baskom jago', 'Lusin', 7000, 50, 120, 'Rp', 259, 'fd627645', 'BMP-2605-010-c94477e0-1efe04e2', '2026-05-26 05:49:31.49066+00', '2026-05-26 05:49:31.49066+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (446, '2026-05-26 05:49:31.50083+00', '2026-05-26 05:49:31.50083+00', '2026-05-26 08:09:13.461233+00', 26, 'Baskom Bahtera', 'Lusin', 7300, 50, 20, 'Rp', 259, '9f0b71ee', 'BMP-2605-010-c94477e0-dba9467f', '2026-05-26 05:49:31.500807+00', '2026-05-26 05:49:31.500807+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (284, '2026-05-01 08:19:25.310481+00', '2026-05-01 08:19:25.310481+00', NULL, 46, 'BMP', 'Lusin', 7000, 50, 60, 'Rp', 211, 'b67d475c', 'slug_284_36010001-617e-47b9-abbd-e22cc2bd2bf3', '2026-05-01 08:19:25.31046+00', '2026-05-01 08:19:25.310461+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (447, '2026-05-26 05:49:31.511162+00', '2026-05-26 05:49:31.511162+00', '2026-05-26 08:09:13.461233+00', 23, 'smile 12', 'Lusin', 6900, 50, 15, 'Rp', 259, '9dc25f15', 'BMP-2605-010-c94477e0-38fe0abe', '2026-05-26 05:49:31.511143+00', '2026-05-26 05:49:31.511143+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (303, '2026-05-01 15:35:20.311456+00', '2026-05-01 15:35:20.311456+00', '2026-05-01 15:38:55.165669+00', 18, 'bak kuping12', '-', 13000, 30, 50, 'Rp', 239, 'b58240e8', '', '2026-05-01 15:35:20.311432+00', '2026-05-01 15:35:20.311432+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (448, '2026-05-26 05:49:31.521355+00', '2026-05-26 05:49:31.521355+00', '2026-05-26 08:09:13.461233+00', 38, 'Smile 14', 'Lusin', 9300, 50, 36, 'Rp', 259, '0c261109', 'BMP-2605-010-c94477e0-a44c6692', '2026-05-26 05:49:31.52133+00', '2026-05-26 05:49:31.52133+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (451, '2026-05-26 08:09:13.478433+00', '2026-05-26 08:09:13.478433+00', '2026-05-26 08:15:53.948091+00', 20, 'baskom jago', 'Lusin', 7000, 50, 120, 'Rp', 259, '39d6fdd5', 'BMP-2605-010-c94477e0-c82b583e', '2026-05-26 08:09:13.478407+00', '2026-05-26 08:09:13.478407+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (452, '2026-05-26 08:09:13.492984+00', '2026-05-26 08:09:13.492984+00', '2026-05-26 08:15:53.948091+00', 26, 'Baskom Bahtera', 'Lusin', 7300, 50, 20, 'Rp', 259, '2bbc8daa', 'BMP-2605-010-c94477e0-c6c02c6d', '2026-05-26 08:09:13.492971+00', '2026-05-26 08:09:13.492971+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (453, '2026-05-26 08:09:13.501465+00', '2026-05-26 08:09:13.501465+00', '2026-05-26 08:15:53.948091+00', 23, 'smile 12', 'Lusin', 6900, 50, 15, 'Rp', 259, 'a8c3510e', 'BMP-2605-010-c94477e0-f18cfd05', '2026-05-26 08:09:13.501441+00', '2026-05-26 08:09:13.501441+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (454, '2026-05-26 08:09:13.510436+00', '2026-05-26 08:09:13.510436+00', '2026-05-26 08:15:53.948091+00', 38, 'Smile 14', 'Lusin', 9300, 50, 36, 'Rp', 259, '3d5759d0', 'BMP-2605-010-c94477e0-8ff37ab1', '2026-05-26 08:09:13.510427+00', '2026-05-26 08:09:13.510427+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (455, '2026-05-26 08:09:13.518743+00', '2026-05-26 08:09:13.518743+00', '2026-05-26 08:15:53.948091+00', 51, 'Baskom Panda Cerah', 'Lusin', 10000, 40, 5, 'Rp', 259, '38673f32', 'BMP-2605-010-c94477e0-21c28198', '2026-05-26 08:09:13.518733+00', '2026-05-26 08:09:13.518733+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (456, '2026-05-26 08:09:13.527015+00', '2026-05-26 08:09:13.527015+00', '2026-05-26 08:15:53.948091+00', 37, 'telor tali', 'Lusin', 3700, 20, 97, 'Rp', 259, '3327ab4d', 'BMP-2605-010-c94477e0-8ae2abdf', '2026-05-26 08:09:13.527008+00', '2026-05-26 08:09:13.527008+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (475, '2026-05-26 09:10:18.65843+00', '2026-05-26 09:10:18.65843+00', NULL, 20, 'baskom jago', 'Lusin', 7000, 50, 120, 'Rp', 259, '56ee39b5', 'BMP-2605-010-c94477e0-012d413b', '2026-05-26 09:10:18.6584+00', '2026-05-26 09:10:18.6584+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (311, '2026-05-02 01:10:56.047782+00', '2026-05-02 01:10:56.047782+00', '2026-05-02 01:11:44.643853+00', 18, 'bak kuping12', '-', 13000, 30, 50, 'Rp', 247, '0bffea00', 'BMP-2605-001-424e63c6', '2026-05-02 01:10:56.047752+00', '2026-05-02 01:10:56.047752+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (312, '2026-05-02 01:10:56.056647+00', '2026-05-02 01:10:56.056647+00', '2026-05-02 01:11:44.643853+00', 16, 'baskom mawar', '-', 7400, 50, 15, 'Rp', 247, 'c523aa2c', 'BMP-2605-001-efa0f611', '2026-05-02 01:10:56.056607+00', '2026-05-02 01:10:56.056607+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (313, '2026-05-02 01:10:56.062702+00', '2026-05-02 01:10:56.062702+00', '2026-05-02 01:11:44.643853+00', 46, 'BMP', 'Lusin', 7200, 50, 15, 'Rp', 247, 'ad99a22e', 'BMP-2605-001-fb11c340', '2026-05-02 01:10:56.062666+00', '2026-05-02 01:10:56.062666+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (315, '2026-05-02 01:13:22.511983+00', '2026-05-02 01:13:22.511983+00', NULL, 18, 'bak kuping12', '-', 13000, 30, 50, 'Rp', 248, 'f934cfbf', 'BMP-2605-001-88774ad6', '2026-05-02 01:13:22.511943+00', '2026-05-02 01:13:22.511943+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (316, '2026-05-02 01:13:22.521757+00', '2026-05-02 01:13:22.521757+00', NULL, 16, 'baskom mawar', '-', 7400, 50, 15, 'Rp', 248, 'ac8ecac7', 'BMP-2605-001-431c12b9', '2026-05-02 01:13:22.521714+00', '2026-05-02 01:13:22.521714+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (317, '2026-05-02 01:13:22.529131+00', '2026-05-02 01:13:22.529131+00', NULL, 46, 'BMP', 'Lusin', 7200, 50, 15, 'Rp', 248, '72221160', 'BMP-2605-001-654e7f6c', '2026-05-02 01:13:22.529097+00', '2026-05-02 01:13:22.529097+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (318, '2026-05-02 01:13:22.535316+00', '2026-05-02 01:13:22.535316+00', NULL, 20, 'baskom jago', '-', 7000, 50, 7, 'Rp', 248, 'c13d582a', 'BMP-2605-001-f789cb57', '2026-05-02 01:13:22.535284+00', '2026-05-02 01:13:22.535284+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (476, '2026-05-26 09:10:18.666647+00', '2026-05-26 09:10:18.666647+00', NULL, 26, 'Baskom Bahtera', 'Lusin', 7300, 50, 20, 'Rp', 259, 'bc72e2ff', 'BMP-2605-010-c94477e0-04f90fb5', '2026-05-26 09:10:18.666629+00', '2026-05-26 09:10:18.66663+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (477, '2026-05-26 09:10:18.674973+00', '2026-05-26 09:10:18.674973+00', NULL, 23, 'smile 12', 'Lusin', 6900, 50, 15, 'Rp', 259, '4e847528', 'BMP-2605-010-c94477e0-adfe71b3', '2026-05-26 09:10:18.674965+00', '2026-05-26 09:10:18.674965+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (478, '2026-05-26 09:10:18.683051+00', '2026-05-26 09:10:18.683051+00', NULL, 38, 'Smile 14', 'Lusin', 9300, 50, 36, 'Rp', 259, '8cced3e9', 'BMP-2605-010-c94477e0-90526fb3', '2026-05-26 09:10:18.683044+00', '2026-05-26 09:10:18.683044+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (479, '2026-05-26 09:10:18.691072+00', '2026-05-26 09:10:18.691072+00', NULL, 51, 'Baskom Panda Cerah', 'Lusin', 10000, 40, 5, 'Rp', 259, '16ceba72', 'BMP-2605-010-c94477e0-2bf44bea', '2026-05-26 09:10:18.691062+00', '2026-05-26 09:10:18.691062+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (480, '2026-05-26 09:10:18.699084+00', '2026-05-26 09:10:18.699084+00', NULL, 37, 'telor tali', 'Lusin', 3700, 20, 97, 'Rp', 259, 'e67f381d', 'BMP-2605-010-c94477e0-faf102af', '2026-05-26 09:10:18.699075+00', '2026-05-26 09:10:18.699075+00', true, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (319, '2026-05-02 01:14:05.040282+00', '2026-05-02 01:14:05.040282+00', '2026-05-02 07:59:33.926586+00', 16, 'baskom mawar', '-', 7000, 50, 90, 'Rp', 249, '3b956583', 'BMP-2605-001-88ac3661', '2026-05-02 01:14:05.040245+00', '2026-05-02 01:14:05.040246+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (323, '2026-05-02 09:00:45.593243+00', '2026-05-02 09:00:45.593243+00', '2026-05-02 09:02:00.943303+00', 17, 'baskom bahtera TM', '-', 7000, 50, 15, 'Rp', 250, 'ef8d5962', 'BMP-2605-001-d1aebb79-f8652a16', '2026-05-02 09:00:45.593216+00', '2026-05-02 09:00:45.593216+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (324, '2026-05-02 09:00:45.6062+00', '2026-05-02 09:00:45.6062+00', '2026-05-02 09:02:00.943303+00', 20, 'baskom jago', '-', 5600, 50, 10, 'Rp', 250, '1776aaf2', 'BMP-2605-001-d1aebb79-ef8e8456', '2026-05-02 09:00:45.606177+00', '2026-05-02 09:00:45.606177+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (325, '2026-05-02 09:00:45.614751+00', '2026-05-02 09:00:45.614751+00', '2026-05-02 09:02:00.943303+00', 23, 'smile 12', '-', 5400, 50, 10, 'Rp', 250, 'cb3c76eb', 'BMP-2605-001-d1aebb79-a0a5b06c', '2026-05-02 09:00:45.614728+00', '2026-05-02 09:00:45.614728+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (326, '2026-05-02 09:00:45.623213+00', '2026-05-02 09:00:45.623213+00', '2026-05-02 09:02:00.943303+00', 16, 'baskom mawar', '-', 5800, 50, 10, 'Rp', 250, 'bb09b72d', 'BMP-2605-001-d1aebb79-622ca9aa', '2026-05-02 09:00:45.623197+00', '2026-05-02 09:00:45.623197+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (327, '2026-05-02 09:00:45.631679+00', '2026-05-02 09:00:45.631679+00', '2026-05-02 09:02:00.943303+00', 13, 'baskom panda', '-', 7000, 40, 2, 'Rp', 250, 'f5012cfb', 'BMP-2605-001-d1aebb79-d071ca1b', '2026-05-02 09:00:45.631657+00', '2026-05-02 09:00:45.631657+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (199, '2026-04-20 04:30:38.34676+00', '2026-04-20 04:30:38.346788+00', '2026-05-29 15:19:37.99138+00', 13, 'baskom panda', '-', 7160, 50, 5, 'Rp', 180, '23b66706d24c', 'baskom-panda-23b66706d24c', '2026-04-20 04:30:38.34676+00', '2026-04-20 04:30:38.346788+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (328, '2026-05-02 09:02:00.956716+00', '2026-05-02 09:02:00.956716+00', '2026-05-02 09:02:48.676334+00', 17, 'baskom bahtera TM', '-', 7000, 50, 15, 'Rp', 250, '5c285a1a', 'BMP-2605-001-d1aebb79-8f2605a7', '2026-05-02 09:02:00.956694+00', '2026-05-02 09:02:00.956694+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (329, '2026-05-02 09:02:00.967783+00', '2026-05-02 09:02:00.967783+00', '2026-05-02 09:02:48.676334+00', 20, 'baskom jago', '-', 5600, 50, 10, 'Rp', 250, 'b86a516b', 'BMP-2605-001-d1aebb79-73413e27', '2026-05-02 09:02:00.967763+00', '2026-05-02 09:02:00.967763+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (330, '2026-05-02 09:02:00.976433+00', '2026-05-02 09:02:00.976433+00', '2026-05-02 09:02:48.676334+00', 23, 'smile 12', '-', 5400, 50, 10, 'Rp', 250, '2a209057', 'BMP-2605-001-d1aebb79-a0577e70', '2026-05-02 09:02:00.976419+00', '2026-05-02 09:02:00.976419+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (331, '2026-05-02 09:02:00.989941+00', '2026-05-02 09:02:00.989941+00', '2026-05-02 09:02:48.676334+00', 16, 'baskom mawar', '-', 5800, 50, 10, 'Rp', 250, '54c87b7c', 'BMP-2605-001-d1aebb79-d314bc80', '2026-05-02 09:02:00.989925+00', '2026-05-02 09:02:00.989925+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (493, '2026-05-29 15:19:38.002465+00', '2026-05-29 15:19:38.002465+00', '2026-05-29 15:21:10.730731+00', 17, 'baskom bahtera TM', 'Lusin', 6800, 50, 15, 'Rp', 180, '3c5d3e35', 'bmp-0426-030-7ed262bd1c5a-cb1b92e1', '2026-05-29 15:19:38.00242+00', '2026-05-29 15:19:38.00242+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (494, '2026-05-29 15:19:38.011479+00', '2026-05-29 15:19:38.011479+00', '2026-05-29 15:21:10.730731+00', 18, 'bak kuping12', 'Lusin', 13600, 30, 6, 'Rp', 180, '5bd4cc8b', 'bmp-0426-030-7ed262bd1c5a-7592a352', '2026-05-29 15:19:38.011428+00', '2026-05-29 15:19:38.011428+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (495, '2026-05-29 15:19:38.017016+00', '2026-05-29 15:19:38.017016+00', '2026-05-29 15:21:10.730731+00', 13, 'baskom panda', 'Lusin', 7900, 50, 5, 'Rp', 180, '58ec281c', 'bmp-0426-030-7ed262bd1c5a-a92f9764', '2026-05-29 15:19:38.016865+00', '2026-05-29 15:19:38.016865+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (496, '2026-05-29 15:21:10.737016+00', '2026-05-29 15:21:10.737016+00', '2026-05-29 15:22:02.734156+00', 17, 'baskom bahtera TM', 'Lusin', 6800, 50, 15, 'Rp', 180, '78e85cf9', 'bmp-0426-030-7ed262bd1c5a-ecf42c29', '2026-05-29 15:21:10.736946+00', '2026-05-29 15:21:10.736946+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (497, '2026-05-29 15:21:10.742952+00', '2026-05-29 15:21:10.742952+00', '2026-05-29 15:22:02.734156+00', 18, 'bak kuping12', 'Lusin', 13600, 30, 6, 'Rp', 180, 'eac22850', 'bmp-0426-030-7ed262bd1c5a-32aaaffb', '2026-05-29 15:21:10.742894+00', '2026-05-29 15:21:10.742894+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (498, '2026-05-29 15:21:10.748816+00', '2026-05-29 15:21:10.748816+00', '2026-05-29 15:22:02.734156+00', 13, 'baskom panda', 'Lusin', 7900, 50, 5, 'Rp', 180, '2d093054', 'bmp-0426-030-7ed262bd1c5a-ad516b6f', '2026-05-29 15:21:10.748715+00', '2026-05-29 15:21:10.748715+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (332, '2026-05-02 09:02:00.999798+00', '2026-05-02 09:02:00.999798+00', '2026-05-02 09:02:48.676334+00', 13, 'baskom panda', '-', 8400, 40, 2, 'Rp', 250, '03ff8ff9', 'BMP-2605-001-d1aebb79-f441ce4f', '2026-05-02 09:02:00.999778+00', '2026-05-02 09:02:00.999778+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (333, '2026-05-02 09:02:48.68496+00', '2026-05-02 09:02:48.68496+00', '2026-05-02 09:02:59.006673+00', 17, 'baskom bahtera TM', '-', 7000, 50, 15, 'Rp', 250, '169ef05b', 'BMP-2605-001-d1aebb79-81b46eed', '2026-05-02 09:02:48.684935+00', '2026-05-02 09:02:48.684935+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (334, '2026-05-02 09:02:48.696638+00', '2026-05-02 09:02:48.696638+00', '2026-05-02 09:02:59.006673+00', 20, 'baskom jago', '-', 5600, 50, 10, 'Rp', 250, '8c71defd', 'BMP-2605-001-d1aebb79-aff68729', '2026-05-02 09:02:48.696612+00', '2026-05-02 09:02:48.696613+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (335, '2026-05-02 09:02:48.705015+00', '2026-05-02 09:02:48.705015+00', '2026-05-02 09:02:59.006673+00', 23, 'smile 12', '-', 5400, 50, 10, 'Rp', 250, 'e22f19fa', 'BMP-2605-001-d1aebb79-e94c94a5', '2026-05-02 09:02:48.704994+00', '2026-05-02 09:02:48.704994+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (336, '2026-05-02 09:02:48.713793+00', '2026-05-02 09:02:48.713793+00', '2026-05-02 09:02:59.006673+00', 16, 'baskom mawar', '-', 5800, 50, 10, 'Rp', 250, 'fe40a23e', 'BMP-2605-001-d1aebb79-ed03fd96', '2026-05-02 09:02:48.713775+00', '2026-05-02 09:02:48.713775+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (337, '2026-05-02 09:02:48.722354+00', '2026-05-02 09:02:48.722354+00', '2026-05-02 09:02:59.006673+00', 13, 'baskom panda', '-', 8400, 40, 1, 'Rp', 250, 'a853507a', 'BMP-2605-001-d1aebb79-c7517a33', '2026-05-02 09:02:48.72234+00', '2026-05-02 09:02:48.72234+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (338, '2026-05-02 09:02:59.01542+00', '2026-05-02 09:02:59.01542+00', '2026-05-02 09:05:33.126714+00', 17, 'baskom bahtera TM', '-', 7000, 50, 15, 'Rp', 250, 'b7390b48', 'BMP-2605-001-d1aebb79-8113791a', '2026-05-02 09:02:59.015395+00', '2026-05-02 09:02:59.015395+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (339, '2026-05-02 09:02:59.024628+00', '2026-05-02 09:02:59.024628+00', '2026-05-02 09:05:33.126714+00', 20, 'baskom jago', '-', 5600, 50, 10, 'Rp', 250, 'd25bb642', 'BMP-2605-001-d1aebb79-aac6da14', '2026-05-02 09:02:59.024602+00', '2026-05-02 09:02:59.024602+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (340, '2026-05-02 09:02:59.033056+00', '2026-05-02 09:02:59.033056+00', '2026-05-02 09:05:33.126714+00', 23, 'smile 12', '-', 5400, 50, 10, 'Rp', 250, '1641ddad', 'BMP-2605-001-d1aebb79-4f34236a', '2026-05-02 09:02:59.033038+00', '2026-05-02 09:02:59.033038+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (341, '2026-05-02 09:02:59.041427+00', '2026-05-02 09:02:59.041427+00', '2026-05-02 09:05:33.126714+00', 16, 'baskom mawar', '-', 5800, 50, 10, 'Rp', 250, '2b705de3', 'BMP-2605-001-d1aebb79-55542718', '2026-05-02 09:02:59.041407+00', '2026-05-02 09:02:59.041407+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (342, '2026-05-02 09:02:59.049915+00', '2026-05-02 09:02:59.049915+00', '2026-05-02 09:05:33.126714+00', 13, 'baskom panda', '-', 8400, 40, 2, 'Rp', 250, '1bd8a238', 'BMP-2605-001-d1aebb79-67979ecb', '2026-05-02 09:02:59.049902+00', '2026-05-02 09:02:59.049902+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (343, '2026-05-02 09:05:33.135265+00', '2026-05-02 09:05:33.135265+00', NULL, 17, 'baskom bahtera TM', '-', 7000, 50, 15, 'Rp', 250, '524e0cf0', 'BMP-2605-001-d1aebb79-165326fe', '2026-05-02 09:05:33.135241+00', '2026-05-02 09:05:33.135241+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (344, '2026-05-02 09:05:33.144389+00', '2026-05-02 09:05:33.144389+00', NULL, 20, 'baskom jago', '-', 7000, 50, 10, 'Rp', 250, 'b4e743dc', 'BMP-2605-001-d1aebb79-34fe2793', '2026-05-02 09:05:33.144361+00', '2026-05-02 09:05:33.144362+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (345, '2026-05-02 09:05:33.152927+00', '2026-05-02 09:05:33.152927+00', NULL, 23, 'smile 12', '-', 7000, 50, 10, 'Rp', 250, '51b24081', 'BMP-2605-001-d1aebb79-1fb46617', '2026-05-02 09:05:33.152905+00', '2026-05-02 09:05:33.152905+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (346, '2026-05-02 09:05:33.161391+00', '2026-05-02 09:05:33.161391+00', NULL, 16, 'baskom mawar', '-', 7200, 50, 10, 'Rp', 250, '1156f6b7', 'BMP-2605-001-d1aebb79-148c1cf5', '2026-05-02 09:05:33.161373+00', '2026-05-02 09:05:33.161373+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (347, '2026-05-02 09:05:33.169534+00', '2026-05-02 09:05:33.169534+00', NULL, 13, 'baskom panda', '-', 8400, 40, 2, 'Rp', 250, 'c4a211ee', 'BMP-2605-001-d1aebb79-f3280fef', '2026-05-02 09:05:33.169517+00', '2026-05-02 09:05:33.169517+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (348, '2026-05-04 18:25:53.280627+00', '2026-05-04 18:25:53.280627+00', NULL, 25, 'baskom mawar', '-', 7400, 50, 25, 'Rp', 251, '42ab698a', 'BMP-2605-001-9e707f6b-1fd01cd4', '2026-05-04 18:25:53.280601+00', '2026-05-04 18:25:53.280601+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (349, '2026-05-04 18:25:53.293105+00', '2026-05-04 18:25:53.293105+00', NULL, 17, 'baskom bahtera TM', '-', 7200, 50, 10, 'Rp', 251, '9bc3d733', 'BMP-2605-001-9e707f6b-1738edf0', '2026-05-04 18:25:53.29308+00', '2026-05-04 18:25:53.29308+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (356, '2026-05-05 13:35:14.880984+00', '2026-05-05 13:35:14.880984+00', '2026-05-05 13:49:12.628024+00', 22, 'baskom panda super', '-', 8600, 40, 20, 'Rp', 254, '2988d20b', 'BMP-2605-004-18ed465c-d148ded9', '2026-05-05 13:35:14.880961+00', '2026-05-05 13:35:14.880961+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (357, '2026-05-05 13:49:45.358964+00', '2026-05-05 13:49:45.358964+00', NULL, 22, 'baskom panda super', '-', 8600, 40, 20, 'Rp', 255, '45102ee9', 'BMP-2605-004-35ca2747-8d4090ec', '2026-05-05 13:49:45.358947+00', '2026-05-05 13:49:45.358947+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (354, '2026-05-05 10:26:59.320781+00', '2026-05-05 10:26:59.320781+00', '2026-05-05 15:49:22.067994+00', 16, 'baskom mawar', '-', 7200, 50, 20, 'Rp', 253, '568307e3', 'BMP-2605-003-95768d12-9da24ea5', '2026-05-05 10:26:59.320754+00', '2026-05-05 10:26:59.320755+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (355, '2026-05-05 10:26:59.333023+00', '2026-05-05 10:26:59.333023+00', '2026-05-05 15:49:22.067994+00', 29, 'Baskom TM', 'lusin', 7000, 50, 10, 'Rp', 253, '1c18fb2e', 'BMP-2605-003-95768d12-b9259eeb', '2026-05-05 10:26:59.333003+00', '2026-05-05 10:26:59.333003+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (358, '2026-05-09 05:35:55.051957+00', '2026-05-09 05:35:55.051957+00', NULL, 18, 'bak kuping12', '-', 13000, 30, 70, 'Rp', 256, '35c0ffb0', 'BMP-2605-005-dd96ce2c-9c61d5a1', '2026-05-09 05:35:55.051916+00', '2026-05-09 05:35:55.051916+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (359, '2026-05-09 05:35:55.059563+00', '2026-05-09 05:35:55.059563+00', NULL, 28, 'baskom jago 12', '-', 7000, 50, 15, 'Rp', 256, '9df322ef', 'BMP-2605-005-dd96ce2c-6a646098', '2026-05-09 05:35:55.059537+00', '2026-05-09 05:35:55.059537+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (360, '2026-05-09 05:35:55.064739+00', '2026-05-09 05:35:55.064739+00', NULL, 22, 'baskom panda super', '-', 8600, 40, 5, 'Rp', 256, '339a1b57', 'BMP-2605-005-dd96ce2c-937b6ade', '2026-05-09 05:35:55.064713+00', '2026-05-09 05:35:55.064713+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (350, '2026-05-05 09:49:58.535163+00', '2026-05-05 09:49:58.535163+00', '2026-05-14 01:44:22.662374+00', 35, 'wakul telur', '-', 2600, 20, 130, 'Rp', 252, '41c11b46', 'BMP-2605-002-25268dae-fb2a9399', '2026-05-05 09:49:58.535143+00', '2026-05-05 09:49:58.535143+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (351, '2026-05-05 09:49:58.550206+00', '2026-05-05 09:49:58.550206+00', '2026-05-14 01:44:22.662374+00', 13, 'baskom panda', '-', 8400, 40, 10, 'Rp', 252, '744a1b3d', 'BMP-2605-002-25268dae-fdef4ea1', '2026-05-05 09:49:58.55019+00', '2026-05-05 09:49:58.55019+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (352, '2026-05-05 09:49:58.564957+00', '2026-05-05 09:49:58.564957+00', '2026-05-14 01:44:22.662374+00', 25, 'baskom mawar', '-', 7200, 50, 5, 'Rp', 252, 'e6d4a9d3', 'BMP-2605-002-25268dae-aa000c45', '2026-05-05 09:49:58.564944+00', '2026-05-05 09:49:58.564944+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (353, '2026-05-05 09:49:58.580938+00', '2026-05-05 09:49:58.580938+00', '2026-05-14 01:44:22.662374+00', 39, 'tradisi cerah', 'Lusin', 4900, 30, 10, 'Rp', 252, 'ed8eece4', 'BMP-2605-002-25268dae-08b9b844', '2026-05-05 09:49:58.580916+00', '2026-05-05 09:49:58.580917+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (361, '2026-05-14 01:44:22.678246+00', '2026-05-14 01:44:22.678246+00', '2026-05-14 01:45:18.46926+00', 35, 'wakul telur', '-', 2600, 20, 130, 'Rp', 252, 'c3690605', 'BMP-2605-005-25268dae-e623d854', '2026-05-14 01:44:22.678224+00', '2026-05-14 01:44:22.678224+00', true, 2600);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (362, '2026-05-14 01:44:22.697918+00', '2026-05-14 01:44:22.697918+00', '2026-05-14 01:45:18.46926+00', 13, 'baskom panda', '-', 8400, 40, 10, 'Rp', 252, 'd4b1a1c9', 'BMP-2605-005-25268dae-40b1587c', '2026-05-14 01:44:22.697908+00', '2026-05-14 01:44:22.697908+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (363, '2026-05-14 01:44:22.705646+00', '2026-05-14 01:44:22.705646+00', '2026-05-14 01:45:18.46926+00', 25, 'baskom mawar', '-', 7200, 50, 5, 'Rp', 252, '40d9f311', 'BMP-2605-005-25268dae-a6a38c52', '2026-05-14 01:44:22.705631+00', '2026-05-14 01:44:22.705631+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (364, '2026-05-14 01:44:22.713327+00', '2026-05-14 01:44:22.713327+00', '2026-05-14 01:45:18.46926+00', 39, 'tradisi cerah', 'Lusin', 4900, 30, 10, 'Rp', 252, 'cc61714b', 'BMP-2605-005-25268dae-3d5c694c', '2026-05-14 01:44:22.713316+00', '2026-05-14 01:44:22.713316+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (365, '2026-05-14 01:45:18.477594+00', '2026-05-14 01:45:18.477594+00', NULL, 13, 'baskom panda', '-', 8400, 40, 10, 'Rp', 252, 'f4a3dff9', 'BMP-2605-005-25268dae-af882674', '2026-05-14 01:45:18.477566+00', '2026-05-14 01:45:18.477566+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (366, '2026-05-14 01:45:18.486025+00', '2026-05-14 01:45:18.486025+00', NULL, 25, 'baskom mawar', '-', 7200, 50, 5, 'Rp', 252, '2f188033', 'BMP-2605-005-25268dae-3c81e59d', '2026-05-14 01:45:18.485989+00', '2026-05-14 01:45:18.485989+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (367, '2026-05-14 01:45:18.493995+00', '2026-05-14 01:45:18.493995+00', NULL, 39, 'tradisi cerah', 'Lusin', 4900, 30, 10, 'Rp', 252, '67a04202', 'BMP-2605-005-25268dae-e2d7dd3c', '2026-05-14 01:45:18.493948+00', '2026-05-14 01:45:18.493948+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (203, '2026-04-20 04:40:16.106387+00', '2026-04-20 04:40:16.106432+00', '2026-05-14 13:04:16.466759+00', 16, 'baskom mawar', '-', 6050, 50, 10, 'Rp', 182, 'bcbfde4d86af', 'baskom-mawar-bcbfde4d86af', '2026-04-20 04:40:16.106387+00', '2026-04-20 04:40:16.106432+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (204, '2026-04-20 04:40:16.110792+00', '2026-04-20 04:40:16.11083+00', '2026-05-14 13:04:16.466759+00', 17, 'baskom bahtera TM', '-', 5750, 50, 10, 'Rp', 182, 'b91ab45f9056', 'baskom-bahtera-tm-b91ab45f9056', '2026-04-20 04:40:16.110792+00', '2026-04-20 04:40:16.11083+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (205, '2026-04-20 04:40:16.114388+00', '2026-04-20 04:40:16.114421+00', '2026-05-14 13:04:16.466759+00', 28, 'baskom jago 12', '-', 5550, 50, 10, 'Rp', 182, '7c168235e3e0', 'baskom-jago-12-7c168235e3e0', '2026-04-20 04:40:16.114388+00', '2026-04-20 04:40:16.114421+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (206, '2026-04-20 04:40:16.118474+00', '2026-04-20 04:40:16.118518+00', '2026-05-14 13:04:16.466759+00', 30, 'Baskom Durian', '-', 6600, 50, 5, 'Rp', 182, 'b0ceaa926e03', 'baskom-durian-b0ceaa926e03', '2026-04-20 04:40:16.118474+00', '2026-04-20 04:40:16.118518+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (372, '2026-05-14 13:04:16.483912+00', '2026-05-14 13:04:16.483912+00', NULL, 16, 'baskom mawar', '-', 6050, 50, 10, 'Rp', 182, '754fc320', 'bmp-0426-032-fd9bd5ccae75-ff5fa4f7', '2026-05-14 13:04:16.483878+00', '2026-05-14 13:04:16.483878+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (373, '2026-05-14 13:04:16.497397+00', '2026-05-14 13:04:16.497397+00', NULL, 17, 'baskom bahtera TM', '-', 5750, 50, 10, 'Rp', 182, '853743f1', 'bmp-0426-032-fd9bd5ccae75-eb650e79', '2026-05-14 13:04:16.497372+00', '2026-05-14 13:04:16.497372+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (374, '2026-05-14 13:04:16.506611+00', '2026-05-14 13:04:16.506611+00', NULL, 28, 'baskom jago 12', '-', 5550, 50, 10, 'Rp', 182, '0bb896e5', 'bmp-0426-032-fd9bd5ccae75-6dc183ed', '2026-05-14 13:04:16.506585+00', '2026-05-14 13:04:16.506585+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (375, '2026-05-14 13:04:16.515771+00', '2026-05-14 13:04:16.515771+00', NULL, 30, 'Baskom Durian', '-', 6640, 50, 5, 'Rp', 182, '0d0480d7', 'bmp-0426-032-fd9bd5ccae75-a7e03515', '2026-05-14 13:04:16.515746+00', '2026-05-14 13:04:16.515747+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (376, '2026-05-14 13:15:37.870071+00', '2026-05-14 13:15:37.870071+00', NULL, 18, 'bak kuping12', '-', 12000, 30, 50, 'Rp', 258, 'f887db04', 'BMP-2605-009-6bea2397-7aaadbf6', '2026-05-14 13:15:37.870048+00', '2026-05-14 13:15:37.870048+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (377, '2026-05-14 13:15:37.878938+00', '2026-05-14 13:15:37.878938+00', NULL, 16, 'baskom mawar', '-', 5800, 50, 15, 'Rp', 258, '11ef4174', 'BMP-2605-009-6bea2397-069e1bdb', '2026-05-14 13:15:37.878922+00', '2026-05-14 13:15:37.878922+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (378, '2026-05-14 13:15:37.887436+00', '2026-05-14 13:15:37.887436+00', NULL, 20, 'baskom jago', '-', 5500, 50, 15, 'Rp', 258, 'd5be10b7', 'BMP-2605-009-6bea2397-2244ccc7', '2026-05-14 13:15:37.887421+00', '2026-05-14 13:15:37.887421+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (379, '2026-05-14 13:15:37.895915+00', '2026-05-14 13:15:37.895915+00', NULL, 30, 'Baskom Durian', '-', 8200, 40, 10, 'Rp', 258, 'df0f82d2', 'BMP-2605-009-6bea2397-ce5d4c27', '2026-05-14 13:15:37.8959+00', '2026-05-14 13:15:37.8959+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (380, '2026-05-14 13:15:37.904267+00', '2026-05-14 13:15:37.904267+00', NULL, 17, 'baskom bahtera TM', '-', 5800, 50, 10, 'Rp', 258, 'f3d11fd9', 'BMP-2605-009-6bea2397-d99d24ef', '2026-05-14 13:15:37.904251+00', '2026-05-14 13:15:37.904251+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (381, '2026-05-14 13:15:37.912942+00', '2026-05-14 13:15:37.912942+00', NULL, 19, 'wakul moris', '-', 4400, 50, 5, 'Rp', 258, '685969ff', 'BMP-2605-009-6bea2397-17907574', '2026-05-14 13:15:37.912927+00', '2026-05-14 13:15:37.912927+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (382, '2026-05-14 13:15:37.921469+00', '2026-05-14 13:15:37.921469+00', NULL, 43, 'Wakul Mawar Super', 'Lusin', 3750, 50, 5, 'Rp', 258, '7dac131c', 'BMP-2605-009-6bea2397-fa74a784', '2026-05-14 13:15:37.921454+00', '2026-05-14 13:15:37.921454+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (457, '2026-05-26 08:15:53.963899+00', '2026-05-26 08:15:53.963899+00', '2026-05-26 09:09:04.287052+00', 20, 'baskom jago', 'Lusin', 7000, 50, 120, 'Rp', 259, 'e651be4d', 'BMP-2605-010-c94477e0-bbe8ac8c', '2026-05-26 08:15:53.963822+00', '2026-05-26 08:15:53.963822+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (388, '2026-05-15 00:31:27.045928+00', '2026-05-15 00:31:27.045928+00', '2026-05-15 01:27:26.240317+00', 22, 'baskom panda super', '-', 8600, 40, 70, 'Rp', 260, '9d18fb66', 'BMP-2605-011-51099424-d140dfe8', '2026-05-15 00:31:27.045905+00', '2026-05-15 00:31:27.045905+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (389, '2026-05-15 00:31:27.061237+00', '2026-05-15 00:31:27.061237+00', '2026-05-15 01:27:26.240317+00', 29, 'Baskom TM', 'lusin', 7250, 50, 10, 'Rp', 260, '53bf971c', 'BMP-2605-011-51099424-b0bd3856', '2026-05-15 00:31:27.061223+00', '2026-05-15 00:31:27.061223+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (390, '2026-05-15 01:27:26.260627+00', '2026-05-15 01:27:26.260627+00', NULL, 22, 'baskom panda super', '-', 8600, 40, 70, 'Rp', 260, '0ce18d3a', 'BMP-2605-011-51099424-b5fd0a1e', '2026-05-15 01:27:26.260578+00', '2026-05-15 01:27:26.260578+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (391, '2026-05-15 01:27:26.271956+00', '2026-05-15 01:27:26.271956+00', NULL, 29, 'Baskom TM', 'lusin', 7250, 50, 10, 'Rp', 260, 'efd9670d', 'BMP-2605-011-51099424-df92eb09', '2026-05-15 01:27:26.271912+00', '2026-05-15 01:27:26.271912+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (392, '2026-05-15 01:27:26.282139+00', '2026-05-15 01:27:26.282139+00', NULL, 51, 'Baskom Panda Cerah', 'Lusin', 10000, 40, 30, 'Rp', 260, '57904c99', 'BMP-2605-011-51099424-fbfc1ec2', '2026-05-15 01:27:26.282097+00', '2026-05-15 01:27:26.282097+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (458, '2026-05-26 08:15:53.980024+00', '2026-05-26 08:15:53.980024+00', '2026-05-26 09:09:04.287052+00', 26, 'Baskom Bahtera', 'Lusin', 7300, 50, 20, 'Rp', 259, 'b658bbd4', 'BMP-2605-010-c94477e0-d701861a', '2026-05-26 08:15:53.979984+00', '2026-05-26 08:15:53.979985+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (459, '2026-05-26 08:15:53.98809+00', '2026-05-26 08:15:53.98809+00', '2026-05-26 09:09:04.287052+00', 23, 'smile 12', 'Lusin', 6900, 50, 15, 'Rp', 259, 'ae4f7540', 'BMP-2605-010-c94477e0-8e971af1', '2026-05-26 08:15:53.988042+00', '2026-05-26 08:15:53.988042+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (460, '2026-05-26 08:15:53.996133+00', '2026-05-26 08:15:53.996133+00', '2026-05-26 09:09:04.287052+00', 38, 'Smile 14', 'Lusin', 9300, 50, 36, 'Rp', 259, '53464ab3', 'BMP-2605-010-c94477e0-3893bb03', '2026-05-26 08:15:53.996102+00', '2026-05-26 08:15:53.996102+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (393, '2026-05-15 02:35:08.358168+00', '2026-05-15 02:35:08.358168+00', '2026-05-15 02:53:20.810265+00', 29, 'Baskom TM', 'lusin', 7250, 50, 10, 'Rp', 261, '5e7d071f', 'BMP-2605-012-006f3869-36a6a0b6', '2026-05-15 02:35:08.358125+00', '2026-05-15 02:35:08.358125+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (394, '2026-05-15 02:35:08.367835+00', '2026-05-15 02:35:08.367835+00', '2026-05-15 02:53:20.810265+00', 28, 'baskom jago 12', '-', 7000, 50, 15, 'Rp', 261, 'd0ded155', 'BMP-2605-012-006f3869-3ddcce8f', '2026-05-15 02:35:08.367784+00', '2026-05-15 02:35:08.367784+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (395, '2026-05-15 02:35:08.374012+00', '2026-05-15 02:35:08.374012+00', '2026-05-15 02:53:20.810265+00', 16, 'baskom mawar', '-', 7100, 50, 5, 'Rp', 261, '7b005ad2', 'BMP-2605-012-006f3869-b3b2d4a3', '2026-05-15 02:35:08.373972+00', '2026-05-15 02:35:08.373972+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (461, '2026-05-26 08:15:54.004132+00', '2026-05-26 08:15:54.004132+00', '2026-05-26 09:09:04.287052+00', 51, 'Baskom Panda Cerah', 'Lusin', 10000, 40, 5, 'Rp', 259, '70dd9423', 'BMP-2605-010-c94477e0-677a6635', '2026-05-26 08:15:54.004093+00', '2026-05-26 08:15:54.004094+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (462, '2026-05-26 08:15:54.012196+00', '2026-05-26 08:15:54.012196+00', '2026-05-26 09:09:04.287052+00', 37, 'telor tali', 'Lusin', 3700, 20, 97, 'Rp', 259, 'b9cbbb72', 'BMP-2605-010-c94477e0-1dae5ef0', '2026-05-26 08:15:54.012151+00', '2026-05-26 08:15:54.012151+00', true, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (396, '2026-05-15 02:53:20.8179+00', '2026-05-15 02:53:20.8179+00', '2026-05-15 02:53:44.47035+00', 28, 'baskom jago 12', '-', 7000, 50, 15, 'Rp', 261, '4cf58feb', 'BMP-2605-012-006f3869-a4952205', '2026-05-15 02:53:20.81784+00', '2026-05-15 02:53:20.81784+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (397, '2026-05-15 02:53:20.825629+00', '2026-05-15 02:53:20.825629+00', '2026-05-15 02:53:44.47035+00', 16, 'baskom mawar', '-', 7100, 50, 5, 'Rp', 261, '76efbe10', 'BMP-2605-012-006f3869-4204ad3e', '2026-05-15 02:53:20.825603+00', '2026-05-15 02:53:20.825603+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (383, '2026-05-14 13:35:43.516717+00', '2026-05-14 13:35:43.516717+00', '2026-05-15 08:29:39.024908+00', 20, 'baskom jago', '-', 7000, 50, 120, 'Rp', 259, '3070316c', 'BMP-2605-010-c94477e0-4ee67cc8', '2026-05-14 13:35:43.5167+00', '2026-05-14 13:35:43.5167+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (384, '2026-05-14 13:35:43.529311+00', '2026-05-14 13:35:43.529311+00', '2026-05-15 08:29:39.024908+00', 26, 'Baskom Bahtera', '-', 7300, 50, 20, 'Rp', 259, '540a92d5', 'BMP-2605-010-c94477e0-a00e4bad', '2026-05-14 13:35:43.529292+00', '2026-05-14 13:35:43.529292+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (385, '2026-05-14 13:35:43.537748+00', '2026-05-14 13:35:43.537748+00', '2026-05-15 08:29:39.024908+00', 23, 'smile 12', '-', 6900, 50, 15, 'Rp', 259, '83b6cccf', 'BMP-2605-010-c94477e0-e8635d8f', '2026-05-14 13:35:43.537728+00', '2026-05-14 13:35:43.537728+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (386, '2026-05-14 13:35:43.546164+00', '2026-05-14 13:35:43.546164+00', '2026-05-15 08:29:39.024908+00', 37, 'telor tali', 'Lusin', 3700, 20, 97, 'Rp', 259, 'ce1e5cd6', 'BMP-2605-010-c94477e0-156a81d8', '2026-05-14 13:35:43.546136+00', '2026-05-14 13:35:43.546136+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (387, '2026-05-14 13:35:43.554436+00', '2026-05-14 13:35:43.554436+00', '2026-05-15 08:29:39.024908+00', 38, 'Smile 14', 'Lusin', 9300, 50, 36, 'Rp', 259, 'a5912554', 'BMP-2605-010-c94477e0-ac61f46d', '2026-05-14 13:35:43.554421+00', '2026-05-14 13:35:43.554421+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (398, '2026-05-15 02:53:44.47557+00', '2026-05-15 02:53:44.47557+00', '2026-05-15 10:23:08.022893+00', 28, 'baskom jago 12', '-', 7000, 50, 15, 'Rp', 261, '4dd86e53', 'BMP-2605-012-006f3869-047f96d6', '2026-05-15 02:53:44.475519+00', '2026-05-15 02:53:44.475519+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (399, '2026-05-15 02:53:44.48073+00', '2026-05-15 02:53:44.48073+00', '2026-05-15 10:23:08.022893+00', 16, 'baskom mawar', '-', 7100, 50, 5, 'Rp', 261, '906aad4f', 'BMP-2605-012-006f3869-abfffeb2', '2026-05-15 02:53:44.480705+00', '2026-05-15 02:53:44.480705+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (400, '2026-05-15 02:53:44.486003+00', '2026-05-15 02:53:44.486003+00', '2026-05-15 10:23:08.022893+00', 17, 'baskom bahtera TM', '-', 7250, 50, 10, 'Rp', 261, '7941135f', 'BMP-2605-012-006f3869-804c0b02', '2026-05-15 02:53:44.485954+00', '2026-05-15 02:53:44.485954+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (401, '2026-05-15 08:29:39.032693+00', '2026-05-15 08:29:39.032693+00', '2026-05-26 05:34:42.64882+00', 20, 'baskom jago', 'Lusin', 7000, 50, 120, 'Rp', 259, '1a97babd', 'BMP-2605-010-c94477e0-c2a2fdb0', '2026-05-15 08:29:39.032658+00', '2026-05-15 08:29:39.032658+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (402, '2026-05-15 08:29:39.040928+00', '2026-05-15 08:29:39.040928+00', '2026-05-26 05:34:42.64882+00', 26, 'Baskom Bahtera', 'Lusin', 7300, 50, 20, 'Rp', 259, '76eae052', 'BMP-2605-010-c94477e0-ceac1ddf', '2026-05-15 08:29:39.040892+00', '2026-05-15 08:29:39.040892+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (403, '2026-05-15 08:29:39.046333+00', '2026-05-15 08:29:39.046333+00', '2026-05-26 05:34:42.64882+00', 23, 'smile 12', 'Lusin', 6900, 50, 15, 'Rp', 259, '39f07ac2', 'BMP-2605-010-c94477e0-cb3d0847', '2026-05-15 08:29:39.046303+00', '2026-05-15 08:29:39.046303+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (404, '2026-05-15 08:29:39.051521+00', '2026-05-15 08:29:39.051521+00', '2026-05-26 05:34:42.64882+00', 37, 'telor tali', 'Lusin', 3700, 20, 97, 'Rp', 259, 'f8c6d0f7', 'BMP-2605-010-c94477e0-2195ab48', '2026-05-15 08:29:39.051499+00', '2026-05-15 08:29:39.051499+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (405, '2026-05-15 08:29:39.056753+00', '2026-05-15 08:29:39.056753+00', '2026-05-26 05:34:42.64882+00', 38, 'Smile 14', 'Lusin', 9300, 50, 36, 'Rp', 259, 'd6019bd0', 'BMP-2605-010-c94477e0-877b942b', '2026-05-15 08:29:39.056721+00', '2026-05-15 08:29:39.056721+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (440, '2026-05-26 05:35:42.793583+00', '2026-05-26 05:35:42.793583+00', '2026-05-26 05:49:31.480431+00', 20, 'baskom jago', 'Lusin', 7000, 50, 120, 'Rp', 259, 'bb6cb229', 'BMP-2605-010-c94477e0-c68fad5f', '2026-05-26 05:35:42.793558+00', '2026-05-26 05:35:42.793558+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (407, '2026-05-15 10:23:08.041607+00', '2026-05-15 10:23:08.041607+00', NULL, 28, 'baskom jago 12', 'Lusin', 7000, 50, 15, 'Rp', 261, 'e49cec45', 'BMP-2605-012-006f3869-ed333c03', '2026-05-15 10:23:08.04086+00', '2026-05-15 10:23:08.04086+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (408, '2026-05-15 10:23:08.058217+00', '2026-05-15 10:23:08.058217+00', NULL, 16, 'baskom mawar', 'Lusin', 7100, 50, 5, 'Rp', 261, '2ec25522', 'BMP-2605-012-006f3869-144759b8', '2026-05-15 10:23:08.058197+00', '2026-05-15 10:23:08.058197+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (409, '2026-05-15 10:23:08.067075+00', '2026-05-15 10:23:08.067075+00', NULL, 17, 'baskom bahtera TM', 'Lusin', 7000, 50, 10, 'Rp', 261, 'a1d23d45', 'BMP-2605-012-006f3869-b02e4c44', '2026-05-15 10:23:08.067049+00', '2026-05-15 10:23:08.067049+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (410, '2026-05-15 14:07:39.351913+00', '2026-05-15 14:07:39.351913+00', NULL, 29, 'Baskom TM', 'lusin', 7500, 50, 100, 'Rp', 262, 'a0c9fed5', 'BMP-2605-013-4d43d330-615d9ed4', '2026-05-15 14:07:39.351884+00', '2026-05-15 14:07:39.351884+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (411, '2026-05-15 14:07:39.364375+00', '2026-05-15 14:07:39.364375+00', NULL, 46, 'BMP', 'Lusin', 7500, 50, 50, 'Rp', 262, '012cebe8', 'BMP-2605-013-4d43d330-85e761fc', '2026-05-15 14:07:39.364357+00', '2026-05-15 14:07:39.364357+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (412, '2026-05-15 14:07:39.372889+00', '2026-05-15 14:07:39.372889+00', NULL, 51, 'Baskom Panda Cerah', 'Lusin', 10000, 40, 10, 'Rp', 262, '7973a827', 'BMP-2605-013-4d43d330-89328e02', '2026-05-15 14:07:39.372872+00', '2026-05-15 14:07:39.372872+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (413, '2026-05-15 14:07:39.381127+00', '2026-05-15 14:07:39.381127+00', NULL, 16, 'baskom mawar', 'Lusin', 7500, 50, 15, 'Rp', 262, 'cce6c380', 'BMP-2605-013-4d43d330-2fba67dc', '2026-05-15 14:07:39.381111+00', '2026-05-15 14:07:39.381111+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (414, '2026-05-15 14:07:39.389401+00', '2026-05-15 14:07:39.389401+00', NULL, 43, 'Wakul Mawar Super', 'Lusin', 4000, 50, 13, 'Rp', 262, 'd2f8f071', 'BMP-2605-013-4d43d330-c90a75da', '2026-05-15 14:07:39.389386+00', '2026-05-15 14:07:39.389386+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (415, '2026-05-22 04:25:42.715217+00', '2026-05-22 04:25:42.715217+00', NULL, 46, 'BMP', 'Lusin', 7400, 50, 120, 'Rp', 263, '79954f3f', 'BMP-2605-014-13b59092-9cdf3c60', '2026-05-22 04:25:42.715192+00', '2026-05-22 04:25:42.715192+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (416, '2026-05-22 04:25:42.727765+00', '2026-05-22 04:25:42.727765+00', NULL, 29, 'Baskom TM', 'lusin', 7350, 50, 20, 'Rp', 263, '05344c53', 'BMP-2605-014-13b59092-a65a1e2e', '2026-05-22 04:25:42.727739+00', '2026-05-22 04:25:42.727739+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (417, '2026-05-22 04:25:42.736023+00', '2026-05-22 04:25:42.736023+00', NULL, 16, 'baskom mawar', 'Lusin', 7400, 50, 25, 'Rp', 263, '31e68a6b', 'BMP-2605-014-13b59092-21285ec2', '2026-05-22 04:25:42.736+00', '2026-05-22 04:25:42.736+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (418, '2026-05-22 04:25:42.744178+00', '2026-05-22 04:25:42.744178+00', NULL, 19, 'wakul moris', 'Lusin', 6700, 50, 10, 'Rp', 263, '6da2d879', 'BMP-2605-014-13b59092-466add53', '2026-05-22 04:25:42.744162+00', '2026-05-22 04:25:42.744162+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (419, '2026-05-22 04:25:42.752398+00', '2026-05-22 04:25:42.752398+00', NULL, 51, 'Baskom Panda Cerah', 'Lusin', 10000, 40, 10, 'Rp', 263, 'db109d9a', 'BMP-2605-014-13b59092-da1a9c8f', '2026-05-22 04:25:42.752381+00', '2026-05-22 04:25:42.752381+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (441, '2026-05-26 05:35:42.804047+00', '2026-05-26 05:35:42.804047+00', '2026-05-26 05:49:31.480431+00', 26, 'Baskom Bahtera', 'Lusin', 7300, 50, 20, 'Rp', 259, 'b789d1cf', 'BMP-2605-010-c94477e0-a1191eee', '2026-05-26 05:35:42.804029+00', '2026-05-26 05:35:42.804029+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (442, '2026-05-26 05:35:42.814392+00', '2026-05-26 05:35:42.814392+00', '2026-05-26 05:49:31.480431+00', 23, 'smile 12', 'Lusin', 6900, 50, 15, 'Rp', 259, '2d8b6a94', 'BMP-2605-010-c94477e0-13495ec6', '2026-05-26 05:35:42.81438+00', '2026-05-26 05:35:42.81438+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (420, '2026-05-22 04:31:06.978295+00', '2026-05-22 04:31:06.978295+00', '2026-05-22 04:32:37.157469+00', 46, 'BMP', 'Lusin', 7500, 50, 50, 'Rp', 264, 'a414dd7b', 'BMP-2605-015-88bb7619-8ee28e86', '2026-05-22 04:31:06.978274+00', '2026-05-22 04:31:06.978274+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (421, '2026-05-22 04:31:06.991059+00', '2026-05-22 04:31:06.991059+00', '2026-05-22 04:32:37.157469+00', 33, 'wakul kotak', 'Lusin', 5500, 20, 20, 'Rp', 264, '6b048920', 'BMP-2605-015-88bb7619-4d944fdd', '2026-05-22 04:31:06.991033+00', '2026-05-22 04:31:06.991033+00', true, 5100);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (422, '2026-05-22 04:32:37.170089+00', '2026-05-22 04:32:37.170089+00', NULL, 46, 'BMP', 'Lusin', 7500, 50, 50, 'Rp', 264, '109a6ce4', 'BMP-2605-015-88bb7619-9b11e1ba', '2026-05-22 04:32:37.169842+00', '2026-05-22 04:32:37.169842+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (423, '2026-05-22 04:32:37.178351+00', '2026-05-22 04:32:37.178351+00', NULL, 33, 'wakul kotak', 'Lusin', 5500, 20, 20, 'Rp', 264, '85b9fbd7', 'BMP-2605-015-88bb7619-ebeb09a3', '2026-05-22 04:32:37.178333+00', '2026-05-22 04:32:37.178333+00', true, 5100);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (424, '2026-05-22 07:05:15.778623+00', '2026-05-22 07:05:15.778623+00', NULL, 17, 'baskom bahtera TM', 'Lusin', 7200, 50, 20, 'Rp', 265, '08e7429e', 'BMP-2605-016-3df6596f-84a66aae', '2026-05-22 07:05:15.778592+00', '2026-05-22 07:05:15.778592+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (425, '2026-05-22 07:05:15.786881+00', '2026-05-22 07:05:15.786881+00', NULL, 13, 'baskom panda', 'Lusin', 8600, 40, 20, 'Rp', 265, 'c2beea11', 'BMP-2605-016-3df6596f-92654896', '2026-05-22 07:05:15.786856+00', '2026-05-22 07:05:15.786856+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (463, '2026-05-26 08:35:52.768327+00', '2026-05-26 08:35:52.768327+00', NULL, 13, 'baskom panda', 'Lusin', 7000, 1, 50, 'Rp', 267, 'ebf3c310', 'DEMO-INV-001-2e4944eb-p1', '2026-05-26 08:35:52.763248+00', '2026-05-26 08:35:52.763248+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (464, '2026-05-26 08:35:52.826166+00', '2026-05-26 08:35:52.826166+00', NULL, 13, 'baskom panda', 'Lusin', 7000, 1, 100, 'Rp', 268, '35f2d65f', 'DEMO-INV-002-1b7f6943-p1', '2026-05-26 08:35:52.82102+00', '2026-05-26 08:35:52.82102+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (426, '2026-05-25 04:32:42.670651+00', '2026-05-25 04:32:42.670651+00', '2026-05-25 05:07:35.59366+00', 30, 'Baskom Durian', 'Lusin', 9100, 40, 27, 'Rp', 266, '14530d46', 'BMP-2605-017-00c25e20-1f729f0f', '2026-05-25 04:32:42.67062+00', '2026-05-25 04:32:42.67062+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (427, '2026-05-25 04:32:42.68316+00', '2026-05-25 04:32:42.68316+00', '2026-05-25 05:07:35.59366+00', 22, 'baskom panda super', 'Lusin', 8600, 40, 16, 'Rp', 266, '4cb0544b', 'BMP-2605-017-00c25e20-84d6a5ff', '2026-05-25 04:32:42.68314+00', '2026-05-25 04:32:42.68314+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (428, '2026-05-25 04:32:42.690668+00', '2026-05-25 04:32:42.690668+00', '2026-05-25 05:07:35.59366+00', 46, 'BMP', 'Lusin', 7200, 50, 8, 'Rp', 266, 'e8668b63', 'BMP-2605-017-00c25e20-f51c2beb', '2026-05-25 04:32:42.690649+00', '2026-05-25 04:32:42.690649+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (429, '2026-05-25 04:32:42.699827+00', '2026-05-25 04:32:42.699827+00', '2026-05-25 05:07:35.59366+00', 43, 'Wakul Mawar Super', 'Lusin', 7400, 50, 7, 'Rp', 266, 'c615d49b', 'BMP-2605-017-00c25e20-0b2770b5', '2026-05-25 04:32:42.699803+00', '2026-05-25 04:32:42.699803+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (430, '2026-05-25 05:07:35.605344+00', '2026-05-25 05:07:35.605344+00', NULL, 30, 'Baskom Durian', 'Lusin', 9100, 40, 27, 'Rp', 266, '69c74eb3', 'BMP-2605-017-00c25e20-de728b45', '2026-05-25 05:07:35.605324+00', '2026-05-25 05:07:35.605324+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (431, '2026-05-25 05:07:35.612961+00', '2026-05-25 05:07:35.612961+00', NULL, 22, 'baskom panda super', 'Lusin', 8600, 40, 16, 'Rp', 266, 'f3c57642', 'BMP-2605-017-00c25e20-05be19e5', '2026-05-25 05:07:35.612941+00', '2026-05-25 05:07:35.612941+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (432, '2026-05-25 05:07:35.620436+00', '2026-05-25 05:07:35.620436+00', NULL, 46, 'BMP', 'Lusin', 7200, 50, 8, 'Rp', 266, '69dad51d', 'BMP-2605-017-00c25e20-eddbf1b1', '2026-05-25 05:07:35.620415+00', '2026-05-25 05:07:35.620415+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (433, '2026-05-25 05:07:35.629137+00', '2026-05-25 05:07:35.629137+00', NULL, 43, 'Wakul Mawar Super', 'Lusin', 7400, 50, 5, 'Rp', 266, 'a23f3c5d', 'BMP-2605-017-00c25e20-5fa34adb', '2026-05-25 05:07:35.629122+00', '2026-05-25 05:07:35.629122+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (406, '2026-05-15 08:29:39.061665+00', '2026-05-15 08:29:39.061665+00', '2026-05-26 05:34:42.64882+00', 51, 'Baskom Panda Cerah', 'Lusin', 10000, 40, 5, 'Rp', 259, '7d7a38a6', 'BMP-2605-010-c94477e0-25399a5d', '2026-05-15 08:29:39.061628+00', '2026-05-15 08:29:39.061628+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (499, '2026-05-29 15:22:02.739608+00', '2026-05-29 15:22:02.739608+00', NULL, 17, 'baskom bahtera TM', 'Lusin', 6800, 50, 15, 'Rp', 180, 'e0f2e987', 'bmp-0426-030-7ed262bd1c5a-489fc572', '2026-05-29 15:22:02.739572+00', '2026-05-29 15:22:02.739572+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (434, '2026-05-26 05:34:42.669579+00', '2026-05-26 05:34:42.669579+00', '2026-05-26 05:35:42.782843+00', 20, 'baskom jago', 'Lusin', 7000, 50, 120, 'Rp', 259, 'd45cfc46', 'BMP-2605-010-c94477e0-cba8b181', '2026-05-26 05:34:42.669484+00', '2026-05-26 05:34:42.669489+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (435, '2026-05-26 05:34:42.685907+00', '2026-05-26 05:34:42.685907+00', '2026-05-26 05:35:42.782843+00', 26, 'Baskom Bahtera', 'Lusin', 7300, 50, 20, 'Rp', 259, '9a9300f4', 'BMP-2605-010-c94477e0-b33fb173', '2026-05-26 05:34:42.68588+00', '2026-05-26 05:34:42.68588+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (436, '2026-05-26 05:34:42.696821+00', '2026-05-26 05:34:42.696821+00', '2026-05-26 05:35:42.782843+00', 23, 'smile 12', 'Lusin', 6900, 50, 15, 'Rp', 259, '5686e3ee', 'BMP-2605-010-c94477e0-428082c9', '2026-05-26 05:34:42.696792+00', '2026-05-26 05:34:42.696792+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (437, '2026-05-26 05:34:42.709225+00', '2026-05-26 05:34:42.709225+00', '2026-05-26 05:35:42.782843+00', 37, 'telor tali', 'Lusin', 3700, 20, 97, 'Rp', 259, '7417ada1', 'BMP-2605-010-c94477e0-c81afbe3', '2026-05-26 05:34:42.709207+00', '2026-05-26 05:34:42.709207+00', true, 3700);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (438, '2026-05-26 05:34:42.731732+00', '2026-05-26 05:34:42.731732+00', '2026-05-26 05:35:42.782843+00', 38, 'Smile 14', 'Lusin', 9300, 50, 36, 'Rp', 259, 'fb4849e4', 'BMP-2605-010-c94477e0-68819aef', '2026-05-26 05:34:42.731715+00', '2026-05-26 05:34:42.731715+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (439, '2026-05-26 05:34:42.742338+00', '2026-05-26 05:34:42.742338+00', '2026-05-26 05:35:42.782843+00', 51, 'Baskom Panda Cerah', 'Lusin', 10000, 40, 5, 'Rp', 259, 'b448ab08', 'BMP-2605-010-c94477e0-ba3eb7a3', '2026-05-26 05:34:42.742323+00', '2026-05-26 05:34:42.742323+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (443, '2026-05-26 05:35:42.824484+00', '2026-05-26 05:35:42.824484+00', '2026-05-26 05:49:31.480431+00', 38, 'Smile 14', 'Lusin', 9300, 50, 36, 'Rp', 259, '8dc4b005', 'BMP-2605-010-c94477e0-41318013', '2026-05-26 05:35:42.824472+00', '2026-05-26 05:35:42.824472+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (444, '2026-05-26 05:35:42.836048+00', '2026-05-26 05:35:42.836048+00', '2026-05-26 05:49:31.480431+00', 51, 'Baskom Panda Cerah', 'Lusin', 10000, 40, 5, 'Rp', 259, 'c112e9eb', 'BMP-2605-010-c94477e0-eafdc77b', '2026-05-26 05:35:42.836035+00', '2026-05-26 05:35:42.836035+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (368, '2026-05-14 01:52:21.027258+00', '2026-05-14 01:52:21.027258+00', '2026-05-26 09:06:27.51911+00', 35, 'wakul telur', '-', 2600, 20, 130, 'Rp', 257, '138c846d', 'BMP-2605-008-b971b6d0-63afd31b', '2026-05-14 01:52:21.02723+00', '2026-05-14 01:52:21.02723+00', true, 6760000);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (369, '2026-05-14 01:52:21.039227+00', '2026-05-14 01:52:21.039227+00', '2026-05-26 09:06:27.51911+00', 13, 'baskom panda', '-', 8400, 40, 10, 'Rp', 257, '45036519', 'BMP-2605-008-b971b6d0-c7b4bd7f', '2026-05-14 01:52:21.039212+00', '2026-05-14 01:52:21.039212+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (370, '2026-05-14 01:52:21.047255+00', '2026-05-14 01:52:21.047255+00', '2026-05-26 09:06:27.51911+00', 16, 'baskom mawar', '-', 7200, 50, 5, 'Rp', 257, '366febfe', 'BMP-2605-008-b971b6d0-c20f31ae', '2026-05-14 01:52:21.047235+00', '2026-05-14 01:52:21.047235+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (371, '2026-05-14 01:52:21.05534+00', '2026-05-14 01:52:21.05534+00', '2026-05-26 09:06:27.51911+00', 39, 'tradisi cerah', 'Lusin', 4900, 30, 10, 'Rp', 257, 'db5fe7e5', 'BMP-2605-008-b971b6d0-3a588995', '2026-05-14 01:52:21.055328+00', '2026-05-14 01:52:21.055328+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (465, '2026-05-26 09:06:27.538746+00', '2026-05-26 09:06:27.538746+00', NULL, 35, 'wakul telur', 'Lusin', 2600, 20, 130, 'Rp', 257, '3c8d1c7c', 'BMP-2605-008-b971b6d0-05d6414f', '2026-05-26 09:06:27.538718+00', '2026-05-26 09:06:27.538718+00', true, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (466, '2026-05-26 09:06:27.558808+00', '2026-05-26 09:06:27.558808+00', NULL, 13, 'baskom panda', 'Lusin', 8400, 40, 10, 'Rp', 257, 'ff7a7dbb', 'BMP-2605-008-b971b6d0-de1c9f75', '2026-05-26 09:06:27.558797+00', '2026-05-26 09:06:27.558797+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (467, '2026-05-26 09:06:27.569094+00', '2026-05-26 09:06:27.569094+00', NULL, 16, 'baskom mawar', 'Lusin', 7200, 50, 5, 'Rp', 257, '052dd66e', 'BMP-2605-008-b971b6d0-c126f13d', '2026-05-26 09:06:27.569086+00', '2026-05-26 09:06:27.569086+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (468, '2026-05-26 09:06:27.578514+00', '2026-05-26 09:06:27.578514+00', NULL, 39, 'tradisi cerah', 'Lusin', 4900, 30, 10, 'Rp', 257, 'dd078c93', 'BMP-2605-008-b971b6d0-030f5413', '2026-05-26 09:06:27.578504+00', '2026-05-26 09:06:27.578504+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (492, '2026-05-27 07:16:23.419856+00', '2026-05-27 07:16:23.419856+00', NULL, 53, 'pot hitam 10', 'Pcs', 20000, 12, 10, 'Rp', 269, '6b97a36a', 'BMP-2605-090-4ad598bc-8dbd2da5', '2026-05-27 07:16:23.419803+00', '2026-05-27 07:16:23.419803+00', true, 19000);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (500, '2026-05-29 15:22:02.744853+00', '2026-05-29 15:22:02.744853+00', NULL, 18, 'bak kuping12', 'Lusin', 13600, 30, 6, 'Rp', 180, '99d6a338', 'bmp-0426-030-7ed262bd1c5a-b4df63cc', '2026-05-29 15:22:02.744809+00', '2026-05-29 15:22:02.744809+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (501, '2026-05-29 15:22:02.750125+00', '2026-05-29 15:22:02.750125+00', NULL, 13, 'baskom panda', 'Lusin', 7900, 40, 5, 'Rp', 180, '7a346a9d', 'bmp-0426-030-7ed262bd1c5a-9667de4b', '2026-05-29 15:22:02.750092+00', '2026-05-29 15:22:02.750092+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (502, '2026-05-30 02:00:44.549375+00', '2026-05-30 02:00:44.549375+00', '2026-05-30 02:01:22.148883+00', 46, 'BMP', 'Lusin', 7000, 50, 6, 'Rp', 270, 'd4af7034', 'BMP-2605-018-915e3416-844d834b', '2026-05-30 02:00:44.549352+00', '2026-05-30 02:00:44.549352+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (503, '2026-05-30 02:00:44.56223+00', '2026-05-30 02:00:44.56223+00', '2026-05-30 02:01:22.148883+00', 20, 'baskom jago', 'Lusin', 5600, 50, 15, 'Rp', 270, '0cf3ccbd', 'BMP-2605-018-915e3416-63bf2ea7', '2026-05-30 02:00:44.562221+00', '2026-05-30 02:00:44.562221+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (504, '2026-05-30 02:00:44.570302+00', '2026-05-30 02:00:44.570302+00', '2026-05-30 02:01:22.148883+00', 45, 'Wakul Morris Super', 'Lusin', 5700, 40, 6, 'Rp', 270, 'dff9b581', 'BMP-2605-018-915e3416-1bd6b300', '2026-05-30 02:00:44.570297+00', '2026-05-30 02:00:44.570297+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (505, '2026-05-30 02:01:22.161852+00', '2026-05-30 02:01:22.161852+00', NULL, 46, 'BMP', 'Lusin', 7100, 50, 6, 'Rp', 270, '458b2d66', 'BMP-2605-018-915e3416-60cee0ac', '2026-05-30 02:01:22.161835+00', '2026-05-30 02:01:22.161835+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (506, '2026-05-30 02:01:22.170073+00', '2026-05-30 02:01:22.170073+00', NULL, 20, 'baskom jago', 'Lusin', 7000, 50, 15, 'Rp', 270, 'a43bf349', 'BMP-2605-018-915e3416-9def8d61', '2026-05-30 02:01:22.170065+00', '2026-05-30 02:01:22.170066+00', false, 0);
INSERT INTO public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) VALUES (507, '2026-05-30 02:01:22.177755+00', '2026-05-30 02:01:22.177755+00', NULL, 45, 'Wakul Morris Super', 'Lusin', 6700, 40, 6, 'Rp', 270, '4f80c9d6', 'BMP-2605-018-915e3416-62b9a648', '2026-05-30 02:01:22.177749+00', '2026-05-30 02:01:22.177749+00', false, 0);


--
-- Data for Name: settings; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.settings (id, created_at, updated_at, deleted_at, client_name, client_logo, address_line1, province, postal_code, phone_number, email_address, tax_number, listrik_bulanan, jumlah_mesin, jumlah_karyawan, gaji_harian, hari_kerja_sebulan, biaya_karung_per1000, unique_id, slug, date_created, last_updated, is_demo, hours_per_day) VALUES (2, '2026-05-26 09:07:14.013985+00', '2026-05-26 09:07:14.013985+00', NULL, 'BMP - Bintang Makmur Plastindo (DEMO)', '', 'Jl. Industri Raya No. 45, Kawasan Industri Candi', 'Jawa Tengah', '50181', '024-7654321', 'info@bintangmakmurplastindo.com', '01.234.567.8-901.000', 32500000, 6, 15, 85000, 26, 2150000, '75db9bf9', 'bmp-bintang-makmur-plastindo', '2026-05-26 09:07:14.009304+00', '2026-05-26 09:07:14.009304+00', true, 24);
INSERT INTO public.settings (id, created_at, updated_at, deleted_at, client_name, client_logo, address_line1, province, postal_code, phone_number, email_address, tax_number, listrik_bulanan, jumlah_mesin, jumlah_karyawan, gaji_harian, hari_kerja_sebulan, biaya_karung_per1000, unique_id, slug, date_created, last_updated, is_demo, hours_per_day) VALUES (1, '2026-04-28 19:22:16.281+00', '2026-05-30 08:29:08.965445+00', NULL, 'CV. BAHTERA MULYA PLASTIK', '', 'jl. arimbi, RT04 RW 01 Desa Ngrimbi', 'Jatim', '', '088986084722', 'bahteramulyap@gmail.com', '', 36000000, 5, 21, 85000, 26, 1700000, '', 'main-settings', '2026-04-28 19:23:40.124+00', '2026-05-30 08:29:08.840811+00', false, 24);


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.users (id, created_at, updated_at, deleted_at, username, password) VALUES (1, '2026-04-28 10:50:41.780445+00', '2026-04-28 10:50:41.780445+00', NULL, 'admin', '$2a$10$.FxDhLPcBMvJAJH.G..pCuJnmIi9Oe4MCdCz9V7n6HIiDqD1Kt48W');
INSERT INTO public.users (id, created_at, updated_at, deleted_at, username, password) VALUES (2, '2026-05-26 08:27:59.632758+00', '2026-05-26 08:27:59.632758+00', NULL, 'dedi', '$2a$10$k0rkVB9JIJ0KybFb2cft1.36XhksNpg2yuMLcs6HUB/fyyhKtH2lC');
INSERT INTO public.users (id, created_at, updated_at, deleted_at, username, password) VALUES (3, '2026-05-26 08:27:59.737478+00', '2026-05-26 08:27:59.737478+00', NULL, 'muizz', '$2a$10$BGV4t3yy8tJN1mxKx9/XpeO310/rZecKOih20bagvSIFWrvklBsMi');


--
-- Name: adms_devices_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.adms_devices_id_seq', 1, true);


--
-- Name: attendance_logs_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.attendance_logs_id_seq', 312, true);


--
-- Name: bahan_nono_items_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.bahan_nono_items_id_seq', 94, true);


--
-- Name: bahan_nonos_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.bahan_nonos_id_seq', 92, true);


--
-- Name: cash_flows_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.cash_flows_id_seq', 473, true);


--
-- Name: clients_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.clients_id_seq', 39, true);


--
-- Name: employees_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.employees_id_seq', 24, true);


--
-- Name: invoice_payments_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.invoice_payments_id_seq', 26, true);


--
-- Name: invoices_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.invoices_id_seq', 270, true);


--
-- Name: machine_bonus_logs_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.machine_bonus_logs_id_seq', 21, true);


--
-- Name: master_products_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.master_products_id_seq', 53, true);


--
-- Name: payrolls_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.payrolls_id_seq', 59, true);


--
-- Name: pembayarans_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.pembayarans_id_seq', 1, false);


--
-- Name: pembelian_barangs_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.pembelian_barangs_id_seq', 1, false);


--
-- Name: pembelian_items_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.pembelian_items_id_seq', 1, false);


--
-- Name: products_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.products_id_seq', 507, true);


--
-- Name: settings_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.settings_id_seq', 2, true);


--
-- Name: users_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.users_id_seq', 3, true);


--
-- Name: adms_devices adms_devices_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.adms_devices
    ADD CONSTRAINT adms_devices_pkey PRIMARY KEY (id);


--
-- Name: attendance_logs attendance_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.attendance_logs
    ADD CONSTRAINT attendance_logs_pkey PRIMARY KEY (id);


--
-- Name: bahan_nono_items bahan_nono_items_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bahan_nono_items
    ADD CONSTRAINT bahan_nono_items_pkey PRIMARY KEY (id);


--
-- Name: bahan_nonos bahan_nonos_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bahan_nonos
    ADD CONSTRAINT bahan_nonos_pkey PRIMARY KEY (id);


--
-- Name: cash_flows cash_flows_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.cash_flows
    ADD CONSTRAINT cash_flows_pkey PRIMARY KEY (id);


--
-- Name: clients clients_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.clients
    ADD CONSTRAINT clients_pkey PRIMARY KEY (id);


--
-- Name: employees employees_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.employees
    ADD CONSTRAINT employees_pkey PRIMARY KEY (id);


--
-- Name: invoice_payments invoice_payments_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.invoice_payments
    ADD CONSTRAINT invoice_payments_pkey PRIMARY KEY (id);


--
-- Name: invoices invoices_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT invoices_pkey PRIMARY KEY (id);


--
-- Name: machine_bonus_logs machine_bonus_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.machine_bonus_logs
    ADD CONSTRAINT machine_bonus_logs_pkey PRIMARY KEY (id);


--
-- Name: master_products master_products_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.master_products
    ADD CONSTRAINT master_products_pkey PRIMARY KEY (id);


--
-- Name: payrolls payrolls_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.payrolls
    ADD CONSTRAINT payrolls_pkey PRIMARY KEY (id);


--
-- Name: pembayarans pembayarans_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.pembayarans
    ADD CONSTRAINT pembayarans_pkey PRIMARY KEY (id);


--
-- Name: pembelian_barangs pembelian_barangs_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.pembelian_barangs
    ADD CONSTRAINT pembelian_barangs_pkey PRIMARY KEY (id);


--
-- Name: pembelian_items pembelian_items_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.pembelian_items
    ADD CONSTRAINT pembelian_items_pkey PRIMARY KEY (id);


--
-- Name: products products_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT products_pkey PRIMARY KEY (id);


--
-- Name: settings settings_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.settings
    ADD CONSTRAINT settings_pkey PRIMARY KEY (id);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: idx_adms_devices_deleted_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_adms_devices_deleted_at ON public.adms_devices USING btree (deleted_at);


--
-- Name: idx_adms_devices_is_demo; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_adms_devices_is_demo ON public.adms_devices USING btree (is_demo);


--
-- Name: idx_adms_devices_serial_number; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX idx_adms_devices_serial_number ON public.adms_devices USING btree (serial_number);


--
-- Name: idx_attendance_logs_check_out_time; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_attendance_logs_check_out_time ON public.attendance_logs USING btree (check_out_time);


--
-- Name: idx_attendance_logs_deleted_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_attendance_logs_deleted_at ON public.attendance_logs USING btree (deleted_at);


--
-- Name: idx_attendance_logs_is_demo; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_attendance_logs_is_demo ON public.attendance_logs USING btree (is_demo);


--
-- Name: idx_attendance_logs_log_time; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_attendance_logs_log_time ON public.attendance_logs USING btree (log_time);


--
-- Name: idx_attendance_logs_work_date; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_attendance_logs_work_date ON public.attendance_logs USING btree (work_date);


--
-- Name: idx_bahan_nono_items_deleted_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_bahan_nono_items_deleted_at ON public.bahan_nono_items USING btree (deleted_at);


--
-- Name: idx_bahan_nonos_deleted_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_bahan_nonos_deleted_at ON public.bahan_nonos USING btree (deleted_at);


--
-- Name: idx_bahan_nonos_is_demo; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_bahan_nonos_is_demo ON public.bahan_nonos USING btree (is_demo);


--
-- Name: idx_cash_flows_deleted_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_cash_flows_deleted_at ON public.cash_flows USING btree (deleted_at);


--
-- Name: idx_cash_flows_is_demo; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_cash_flows_is_demo ON public.cash_flows USING btree (is_demo);


--
-- Name: idx_clients_deleted_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_clients_deleted_at ON public.clients USING btree (deleted_at);


--
-- Name: idx_clients_is_demo; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_clients_is_demo ON public.clients USING btree (is_demo);


--
-- Name: idx_clients_slug; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX idx_clients_slug ON public.clients USING btree (slug);


--
-- Name: idx_employees_deleted_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_employees_deleted_at ON public.employees USING btree (deleted_at);


--
-- Name: idx_employees_is_demo; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_employees_is_demo ON public.employees USING btree (is_demo);


--
-- Name: idx_invoice_payments_deleted_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_invoice_payments_deleted_at ON public.invoice_payments USING btree (deleted_at);


--
-- Name: idx_invoices_deleted_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_invoices_deleted_at ON public.invoices USING btree (deleted_at);


--
-- Name: idx_invoices_is_demo; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_invoices_is_demo ON public.invoices USING btree (is_demo);


--
-- Name: idx_invoices_slug; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX idx_invoices_slug ON public.invoices USING btree (slug);


--
-- Name: idx_machine_bonus_logs_deleted_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_machine_bonus_logs_deleted_at ON public.machine_bonus_logs USING btree (deleted_at);


--
-- Name: idx_machine_bonus_logs_is_demo; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_machine_bonus_logs_is_demo ON public.machine_bonus_logs USING btree (is_demo);


--
-- Name: idx_master_products_deleted_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_master_products_deleted_at ON public.master_products USING btree (deleted_at);


--
-- Name: idx_master_products_is_demo; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_master_products_is_demo ON public.master_products USING btree (is_demo);


--
-- Name: idx_master_products_slug; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX idx_master_products_slug ON public.master_products USING btree (slug);


--
-- Name: idx_payrolls_deleted_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_payrolls_deleted_at ON public.payrolls USING btree (deleted_at);


--
-- Name: idx_payrolls_is_demo; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_payrolls_is_demo ON public.payrolls USING btree (is_demo);


--
-- Name: idx_pembayarans_deleted_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_pembayarans_deleted_at ON public.pembayarans USING btree (deleted_at);


--
-- Name: idx_pembelian_barangs_deleted_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_pembelian_barangs_deleted_at ON public.pembelian_barangs USING btree (deleted_at);


--
-- Name: idx_pembelian_barangs_is_demo; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_pembelian_barangs_is_demo ON public.pembelian_barangs USING btree (is_demo);


--
-- Name: idx_pembelian_items_deleted_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_pembelian_items_deleted_at ON public.pembelian_items USING btree (deleted_at);


--
-- Name: idx_products_deleted_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_products_deleted_at ON public.products USING btree (deleted_at);


--
-- Name: idx_products_slug; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX idx_products_slug ON public.products USING btree (slug);


--
-- Name: idx_settings_deleted_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_settings_deleted_at ON public.settings USING btree (deleted_at);


--
-- Name: idx_settings_is_demo; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_settings_is_demo ON public.settings USING btree (is_demo);


--
-- Name: idx_settings_slug; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX idx_settings_slug ON public.settings USING btree (slug);


--
-- Name: idx_users_deleted_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_deleted_at ON public.users USING btree (deleted_at);


--
-- Name: idx_users_username; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX idx_users_username ON public.users USING btree (username);


--
-- Name: bahan_nono_items fk_bahan_nono_items_bahan_nono; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bahan_nono_items
    ADD CONSTRAINT fk_bahan_nono_items_bahan_nono FOREIGN KEY (bahan_nono_id) REFERENCES public.bahan_nonos(id);


--
-- Name: bahan_nono_items fk_bahan_nonos_items; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bahan_nono_items
    ADD CONSTRAINT fk_bahan_nonos_items FOREIGN KEY (bahan_nono_id) REFERENCES public.bahan_nonos(id);


--
-- Name: cash_flows fk_cash_flows_payment_ref; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.cash_flows
    ADD CONSTRAINT fk_cash_flows_payment_ref FOREIGN KEY (payment_ref_id) REFERENCES public.invoice_payments(id);


--
-- Name: invoices fk_invoices_client; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT fk_invoices_client FOREIGN KEY (client_id) REFERENCES public.clients(id);


--
-- Name: invoice_payments fk_invoices_payments; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.invoice_payments
    ADD CONSTRAINT fk_invoices_payments FOREIGN KEY (invoice_id) REFERENCES public.invoices(id);


--
-- Name: machine_bonus_logs fk_machine_bonus_logs_employee; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.machine_bonus_logs
    ADD CONSTRAINT fk_machine_bonus_logs_employee FOREIGN KEY (employee_id) REFERENCES public.employees(id);


--
-- Name: payrolls fk_payrolls_employee; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.payrolls
    ADD CONSTRAINT fk_payrolls_employee FOREIGN KEY (employee_id) REFERENCES public.employees(id);


--
-- Name: pembayarans fk_pembayarans_invoice; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.pembayarans
    ADD CONSTRAINT fk_pembayarans_invoice FOREIGN KEY (invoice_id) REFERENCES public.invoices(id);


--
-- Name: pembelian_items fk_pembelian_barangs_items; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.pembelian_items
    ADD CONSTRAINT fk_pembelian_barangs_items FOREIGN KEY (pembelian_id) REFERENCES public.pembelian_barangs(id);


--
-- Name: pembelian_items fk_pembelian_items_pembelian; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.pembelian_items
    ADD CONSTRAINT fk_pembelian_items_pembelian FOREIGN KEY (pembelian_id) REFERENCES public.pembelian_barangs(id);


--
-- Name: products fk_products_invoice; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT fk_products_invoice FOREIGN KEY (invoice_id) REFERENCES public.invoices(id);


--
-- Name: products fk_products_master_item; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT fk_products_master_item FOREIGN KEY (master_item_id) REFERENCES public.master_products(id);


--
-- PostgreSQL database dump complete
--

\unrestrict W634R46Xl8oqxGBYnfYKN3p6d6bRkNGyNFKl8U9i5R0Rr98HKSMtZCW97A2uFhN

