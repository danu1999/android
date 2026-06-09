--
-- PostgreSQL database dump
--

\restrict bn5X67ueMLAFwPs893Qh2eGvQHndcVu544i5O6So2za6wBHg4Zfu4hX4aNPRtjJ

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
-- Name: adms_devices; Type: TABLE; Schema: public; Owner: -
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


--
-- Name: adms_devices_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.adms_devices_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: adms_devices_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.adms_devices_id_seq OWNED BY public.adms_devices.id;


--
-- Name: attendance_logs; Type: TABLE; Schema: public; Owner: -
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


--
-- Name: attendance_logs_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.attendance_logs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: attendance_logs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.attendance_logs_id_seq OWNED BY public.attendance_logs.id;


--
-- Name: bahan_nono_items; Type: TABLE; Schema: public; Owner: -
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


--
-- Name: bahan_nono_items_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.bahan_nono_items_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: bahan_nono_items_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.bahan_nono_items_id_seq OWNED BY public.bahan_nono_items.id;


--
-- Name: bahan_nonos; Type: TABLE; Schema: public; Owner: -
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


--
-- Name: bahan_nonos_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.bahan_nonos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: bahan_nonos_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.bahan_nonos_id_seq OWNED BY public.bahan_nonos.id;


--
-- Name: cash_flows; Type: TABLE; Schema: public; Owner: -
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


--
-- Name: cash_flows_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.cash_flows_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: cash_flows_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.cash_flows_id_seq OWNED BY public.cash_flows.id;


--
-- Name: clients; Type: TABLE; Schema: public; Owner: -
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


--
-- Name: clients_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.clients_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: clients_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.clients_id_seq OWNED BY public.clients.id;


--
-- Name: employees; Type: TABLE; Schema: public; Owner: -
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


--
-- Name: employees_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.employees_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: employees_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.employees_id_seq OWNED BY public.employees.id;


--
-- Name: invoice_payments; Type: TABLE; Schema: public; Owner: -
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


--
-- Name: invoice_payments_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.invoice_payments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: invoice_payments_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.invoice_payments_id_seq OWNED BY public.invoice_payments.id;


--
-- Name: invoices; Type: TABLE; Schema: public; Owner: -
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


--
-- Name: invoices_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.invoices_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: invoices_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.invoices_id_seq OWNED BY public.invoices.id;


--
-- Name: machine_bonus_logs; Type: TABLE; Schema: public; Owner: -
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


--
-- Name: machine_bonus_logs_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.machine_bonus_logs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: machine_bonus_logs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.machine_bonus_logs_id_seq OWNED BY public.machine_bonus_logs.id;


--
-- Name: master_products; Type: TABLE; Schema: public; Owner: -
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


--
-- Name: master_products_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.master_products_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: master_products_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.master_products_id_seq OWNED BY public.master_products.id;


--
-- Name: payrolls; Type: TABLE; Schema: public; Owner: -
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


--
-- Name: payrolls_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.payrolls_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: payrolls_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.payrolls_id_seq OWNED BY public.payrolls.id;


--
-- Name: pembayarans; Type: TABLE; Schema: public; Owner: -
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


--
-- Name: pembayarans_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.pembayarans_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: pembayarans_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.pembayarans_id_seq OWNED BY public.pembayarans.id;


--
-- Name: pembelian_barangs; Type: TABLE; Schema: public; Owner: -
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


--
-- Name: pembelian_barangs_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.pembelian_barangs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: pembelian_barangs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.pembelian_barangs_id_seq OWNED BY public.pembelian_barangs.id;


--
-- Name: pembelian_items; Type: TABLE; Schema: public; Owner: -
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


--
-- Name: pembelian_items_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.pembelian_items_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: pembelian_items_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.pembelian_items_id_seq OWNED BY public.pembelian_items.id;


--
-- Name: products; Type: TABLE; Schema: public; Owner: -
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


--
-- Name: products_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.products_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: products_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.products_id_seq OWNED BY public.products.id;


--
-- Name: settings; Type: TABLE; Schema: public; Owner: -
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


--
-- Name: settings_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.settings_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: settings_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.settings_id_seq OWNED BY public.settings.id;


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id bigint NOT NULL,
    created_at timestamp with time zone,
    updated_at timestamp with time zone,
    deleted_at timestamp with time zone,
    username text NOT NULL,
    password text NOT NULL
);


--
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.users_id_seq OWNED BY public.users.id;


--
-- Name: adms_devices id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.adms_devices ALTER COLUMN id SET DEFAULT nextval('public.adms_devices_id_seq'::regclass);


--
-- Name: attendance_logs id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attendance_logs ALTER COLUMN id SET DEFAULT nextval('public.attendance_logs_id_seq'::regclass);


--
-- Name: bahan_nono_items id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bahan_nono_items ALTER COLUMN id SET DEFAULT nextval('public.bahan_nono_items_id_seq'::regclass);


--
-- Name: bahan_nonos id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bahan_nonos ALTER COLUMN id SET DEFAULT nextval('public.bahan_nonos_id_seq'::regclass);


--
-- Name: cash_flows id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cash_flows ALTER COLUMN id SET DEFAULT nextval('public.cash_flows_id_seq'::regclass);


--
-- Name: clients id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.clients ALTER COLUMN id SET DEFAULT nextval('public.clients_id_seq'::regclass);


--
-- Name: employees id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.employees ALTER COLUMN id SET DEFAULT nextval('public.employees_id_seq'::regclass);


--
-- Name: invoice_payments id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoice_payments ALTER COLUMN id SET DEFAULT nextval('public.invoice_payments_id_seq'::regclass);


--
-- Name: invoices id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoices ALTER COLUMN id SET DEFAULT nextval('public.invoices_id_seq'::regclass);


--
-- Name: machine_bonus_logs id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.machine_bonus_logs ALTER COLUMN id SET DEFAULT nextval('public.machine_bonus_logs_id_seq'::regclass);


--
-- Name: master_products id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.master_products ALTER COLUMN id SET DEFAULT nextval('public.master_products_id_seq'::regclass);


--
-- Name: payrolls id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payrolls ALTER COLUMN id SET DEFAULT nextval('public.payrolls_id_seq'::regclass);


--
-- Name: pembayarans id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pembayarans ALTER COLUMN id SET DEFAULT nextval('public.pembayarans_id_seq'::regclass);


--
-- Name: pembelian_barangs id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pembelian_barangs ALTER COLUMN id SET DEFAULT nextval('public.pembelian_barangs_id_seq'::regclass);


--
-- Name: pembelian_items id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pembelian_items ALTER COLUMN id SET DEFAULT nextval('public.pembelian_items_id_seq'::regclass);


--
-- Name: products id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.products ALTER COLUMN id SET DEFAULT nextval('public.products_id_seq'::regclass);


--
-- Name: settings id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.settings ALTER COLUMN id SET DEFAULT nextval('public.settings_id_seq'::regclass);


--
-- Name: users id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users ALTER COLUMN id SET DEFAULT nextval('public.users_id_seq'::regclass);


--
-- Data for Name: adms_devices; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.adms_devices (id, created_at, updated_at, deleted_at, serial_number, alias, last_activity, is_demo) FROM stdin;
1	2026-05-12 14:41:04.402433+07	2026-06-08 16:57:52.1992+07	\N	NHZ4254800403		2026-06-08 16:57:52.198877+07	f
\.


--
-- Data for Name: attendance_logs; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.attendance_logs (id, created_at, updated_at, deleted_at, device_sn, employee_pin, verify_type, verify_state, log_time, alasan, is_demo, work_date, late_minutes, check_out_time) FROM stdin;
269	2026-05-29 06:57:47.741552+07	2026-05-29 06:57:47.741552+07	\N	NHZ4254800403	18	1	0	2026-05-29 06:57:47.732435+07		f	2026-05-28	0	\N
324	2026-06-08 16:57:56.689524+07	2026-06-08 16:57:56.689524+07	\N	NHZ4254800403	8	1	0	2026-06-08 08:51:37+07		f	2026-06-08	0	\N
325	2026-06-08 16:57:56.695148+07	2026-06-08 16:57:56.695148+07	\N	NHZ4254800403	14	1	0	2026-06-08 08:53:47+07		f	2026-06-08	0	\N
326	2026-06-08 16:57:56.702011+07	2026-06-08 16:57:56.702011+07	\N	NHZ4254800403	22	1	0	2026-06-08 08:55:47+07		f	2026-06-08	0	\N
327	2026-06-08 16:57:56.707+07	2026-06-08 16:57:56.707+07	\N	NHZ4254800403	17	1	0	2026-06-08 08:58:22+07		f	2026-06-08	0	\N
328	2026-06-08 16:57:56.711469+07	2026-06-08 16:57:56.711469+07	\N	NHZ4254800403	16	1	0	2026-06-08 08:58:33+07		f	2026-06-08	0	\N
329	2026-06-08 16:57:56.716138+07	2026-06-08 16:57:56.716138+07	\N	NHZ4254800403	11	1	0	2026-06-08 08:59:04+07		f	2026-06-08	0	\N
330	2026-06-08 16:57:56.720229+07	2026-06-08 16:57:56.720229+07	\N	NHZ4254800403	12	1	0	2026-06-08 08:59:28+07		f	2026-06-08	0	\N
331	2026-06-08 16:57:56.724472+07	2026-06-08 16:57:56.724472+07	\N	NHZ4254800403	20	1	0	2026-06-08 08:59:39+07		f	2026-06-08	0	\N
332	2026-06-08 16:57:56.728326+07	2026-06-08 16:57:56.728326+07	\N	NHZ4254800403	13	1	0	2026-06-08 08:59:46+07		f	2026-06-08	0	\N
333	2026-06-08 16:57:56.731898+07	2026-06-08 16:57:56.731898+07	\N	NHZ4254800403	10	1	0	2026-06-08 09:00:23+07		f	2026-06-08	0	\N
334	2026-06-08 16:57:56.740428+07	2026-06-08 16:57:56.740428+07	\N	NHZ4254800403	6	1	0	2026-06-08 09:00:29+07		f	2026-06-08	0	\N
272	2026-05-29 14:54:53.368995+07	2026-05-29 23:25:06.643417+07	\N	NHZ4254800403	3	1	1	2026-05-29 14:54:53.361316+07		f	2026-05-29	0	2026-05-29 23:25:06.634704+07
270	2026-05-29 14:51:57.991726+07	2026-05-29 23:25:14.721639+07	\N	NHZ4254800403	8	1	1	2026-05-29 14:51:57.984009+07		f	2026-05-29	0	2026-05-29 23:25:14.715638+07
271	2026-05-29 14:52:11.006637+07	2026-05-29 23:25:23.459982+07	\N	NHZ4254800403	7	1	1	2026-05-29 14:52:10.999005+07		f	2026-05-29	0	2026-05-29 23:25:23.454892+07
273	2026-05-29 14:55:00.441496+07	2026-05-29 23:25:35.231622+07	\N	NHZ4254800403	5	1	1	2026-05-29 14:55:00.432715+07		f	2026-05-29	0	2026-05-29 23:25:35.226733+07
277	2026-05-29 15:13:21.436569+07	2026-05-30 12:44:42.399758+07	\N	NHZ4254800403	18	1	1	2026-05-29 06:56:00+07		f	2026-05-29	0	2026-05-29 15:10:00+07
278	2026-05-29 15:16:48.520372+07	2026-05-30 12:46:10.172721+07	\N	NHZ4254800403	17	1	1	2026-05-29 06:52:00+07		f	2026-05-29	0	2026-05-29 15:02:00+07
288	2026-05-30 09:56:15.789358+07	2026-05-31 06:55:05.004006+07	\N		14	0	1	2026-05-30 06:56:00+07		f	2026-05-30	0	2026-05-31 06:55:04.754251+07
287	2026-05-30 09:55:38.368418+07	2026-05-31 06:58:14.26477+07	\N		17	0	1	2026-05-30 06:55:00+07		f	2026-05-30	0	2026-05-31 06:58:14.014415+07
296	2026-05-30 15:18:48.420326+07	2026-05-31 06:56:08.18615+07	\N	NHZ4254800403	15	1	1	2026-05-30 15:18:48.409205+07		f	2026-05-30	18	2026-05-31 06:56:07.935697+07
289	2026-05-30 09:56:56.650236+07	2026-05-31 06:57:32.479728+07	\N		18	0	1	2026-05-30 06:56:00+07		f	2026-05-30	0	2026-05-31 06:57:32.229331+07
305	2026-05-31 14:51:45.37643+07	2026-05-31 22:32:37.039887+07	\N	NHZ4254800403	12	1	1	2026-05-31 14:51:45.126898+07		f	2026-05-31	0	2026-05-31 22:32:37.030982+07
304	2026-05-31 14:51:26.475847+07	2026-06-01 06:56:21.166673+07	\N	NHZ4254800403	7	1	1	2026-05-31 14:51:26.226424+07		f	2026-05-31	0	2026-06-01 06:56:21.157707+07
315	2026-06-01 11:41:09.30775+07	2026-06-01 11:41:09.30775+07	\N	NHZ4254800403	1	1	0	2026-06-01 11:41:09.294838+07		f	2026-05-31	0	\N
316	2026-06-01 11:46:58.563893+07	2026-06-01 11:46:58.563893+07	\N		3	0	0	2026-06-01 07:00:00+07		f	2026-06-01	0	\N
317	2026-06-01 11:47:17.363657+07	2026-06-01 11:47:17.363657+07	\N		5	0	0	2026-06-01 06:58:00+07		f	2026-06-01	0	\N
318	2026-06-01 11:47:29.906641+07	2026-06-01 11:47:29.906641+07	\N		4	0	0	2026-06-01 06:59:00+07		f	2026-06-01	0	\N
319	2026-06-01 11:47:40.43582+07	2026-06-01 11:47:40.43582+07	\N		7	0	0	2026-06-01 06:47:00+07		f	2026-06-01	0	\N
322	2026-06-01 11:48:18.934118+07	2026-06-01 11:48:18.934118+07	\N		8	0	0	2026-06-01 06:44:00+07		f	2026-06-01	0	\N
323	2026-06-01 11:48:36.772433+07	2026-06-01 11:48:36.772433+07	\N		19	0	0	2026-06-01 07:00:00+07		f	2026-06-01	0	\N
335	2026-06-08 17:09:09.426258+07	2026-06-08 17:09:09.426258+07	\N	NHZ4254800403	3	1	0	2026-06-07 23:31:00+07	\N	f	2026-06-08	0	\N
336	2026-06-08 17:09:09.426258+07	2026-06-08 17:09:09.426258+07	\N	NHZ4254800403	4	1	0	2026-06-08 00:01:00+07	\N	f	2026-06-08	1	\N
337	2026-06-08 17:09:09.426258+07	2026-06-08 17:09:09.426258+07	\N	NHZ4254800403	5	1	0	2026-06-07 23:45:00+07	\N	f	2026-06-08	0	\N
338	2026-06-08 17:09:09.426258+07	2026-06-08 17:09:09.426258+07	\N	NHZ4254800403	7	1	0	2026-06-08 00:00:00+07	\N	f	2026-06-08	0	\N
339	2026-06-08 17:09:09.426258+07	2026-06-08 17:09:09.426258+07	\N	NHZ4254800403	8	1	0	2026-06-07 23:53:00+07	\N	f	2026-06-08	0	\N
340	2026-06-08 17:09:09.426258+07	2026-06-08 17:09:09.426258+07	\N	NHZ4254800403	9	1	0	2026-06-07 23:56:00+07	\N	f	2026-06-08	0	\N
341	2026-06-08 17:09:09.426258+07	2026-06-08 17:09:09.426258+07	\N	NHZ4254800403	19	1	0	2026-06-08 00:03:00+07	\N	f	2026-06-08	3	\N
254	2026-05-28 15:23:01.508029+07	2026-05-28 21:20:37.262843+07	\N	NHZ4254800403	18	1	1	2026-05-28 15:22:54+07		f	2026-05-28	0	\N
342	2026-06-08 22:52:43.210126+07	2026-06-08 22:52:43.210126+07	\N	NHZ4254800403	15	1	0	2026-06-08 22:52:43.207869+07	Hanya Scan Pulang / Lupa Scan Masuk	f	2026-06-08	0	\N
343	2026-06-08 22:55:00.543579+07	2026-06-08 22:55:00.543579+07	\N	NHZ4254800403	7	1	0	2026-06-08 22:55:00.542556+07	Hanya Scan Pulang / Lupa Scan Masuk	f	2026-06-08	0	\N
344	2026-06-08 22:55:14.662432+07	2026-06-08 22:55:14.662432+07	\N	NHZ4254800403	4	1	0	2026-06-08 22:55:14.661633+07	Hanya Scan Pulang / Lupa Scan Masuk	f	2026-06-08	0	\N
345	2026-06-08 22:56:52.921342+07	2026-06-08 22:56:52.921342+07	\N	NHZ4254800403	3	1	0	2026-06-08 22:56:52.920474+07	Hanya Scan Pulang / Lupa Scan Masuk	f	2026-06-08	0	\N
346	2026-06-08 23:00:51.234733+07	2026-06-08 23:00:51.234733+07	\N	NHZ4254800403	17	1	0	2026-06-08 23:00:51.233677+07	Hanya Scan Pulang / Lupa Scan Masuk	f	2026-06-08	0	\N
347	2026-06-08 23:01:03.427144+07	2026-06-08 23:01:03.427144+07	\N	NHZ4254800403	22	1	0	2026-06-08 23:01:03.425832+07	Hanya Scan Pulang / Lupa Scan Masuk	f	2026-06-08	1	\N
348	2026-06-08 23:04:24.727494+07	2026-06-08 23:04:24.727494+07	\N	NHZ4254800403	14	1	0	2026-06-08 23:04:24.726728+07	Hanya Scan Pulang / Lupa Scan Masuk	f	2026-06-08	4	\N
349	2026-06-08 23:05:07.945702+07	2026-06-08 23:05:07.945702+07	\N	NHZ4254800403	8	1	0	2026-06-08 23:05:07.944897+07	Hanya Scan Pulang / Lupa Scan Masuk	f	2026-06-08	5	\N
350	2026-06-08 23:05:28.08889+07	2026-06-08 23:05:28.08889+07	\N	NHZ4254800403	5	1	0	2026-06-08 23:05:28.08795+07	Hanya Scan Pulang / Lupa Scan Masuk	f	2026-06-08	5	\N
259	2026-05-28 22:52:33.361363+07	2026-05-29 07:03:10.826262+07	\N	NHZ4254800403	10	1	1	2026-05-28 22:52:25+07		f	2026-05-28	0	2026-05-29 07:03:10.818565+07
257	2026-05-28 22:51:58.339922+07	2026-05-29 07:03:19.674313+07	\N	NHZ4254800403	11	1	1	2026-05-28 22:51:50+07		f	2026-05-28	0	2026-05-29 07:03:19.666719+07
262	2026-05-28 22:56:37.54599+07	2026-05-29 07:03:59.90304+07	\N	NHZ4254800403	12	1	1	2026-05-28 22:56:29+07		f	2026-05-28	0	2026-05-29 07:03:59.895517+07
285	2026-05-29 22:59:08.424845+07	2026-05-30 06:59:59.928287+07	\N	NHZ4254800403	13	1	1	2026-05-29 22:59:08.417394+07		f	2026-05-29	0	2026-05-30 06:59:59.918303+07
283	2026-05-29 22:56:17.449398+07	2026-05-30 07:00:07.993704+07	\N	NHZ4254800403	20	1	1	2026-05-29 22:56:17.443175+07		f	2026-05-29	0	2026-05-30 07:00:07.987946+07
281	2026-05-29 22:51:41.744444+07	2026-05-30 07:00:16.728505+07	\N	NHZ4254800403	10	1	1	2026-05-29 22:51:41.738557+07		f	2026-05-29	0	2026-05-30 07:00:16.722289+07
280	2026-05-29 22:51:33.288447+07	2026-05-30 07:00:22.866395+07	\N	NHZ4254800403	11	1	1	2026-05-29 22:51:33.280105+07		f	2026-05-29	0	2026-05-30 07:00:22.859921+07
284	2026-05-29 22:56:27.42686+07	2026-05-30 07:00:30.893387+07	\N	NHZ4254800403	12	1	1	2026-05-29 22:56:27.420562+07		f	2026-05-29	0	2026-05-30 07:00:30.888398+07
252	2026-05-28 15:21:07.129923+07	2026-05-30 12:37:56.375678+07	\N	NHZ4254800403	15	1	1	2026-05-28 06:52:00+07		f	2026-05-28	0	2026-05-28 15:03:00+07
255	2026-05-28 15:23:13.307685+07	2026-05-30 12:38:42.175957+07	\N	NHZ4254800403	14	1	1	2026-05-28 06:52:00+07		f	2026-05-28	0	2026-05-28 15:22:00+07
263	2026-05-28 22:59:02.763607+07	2026-05-30 12:39:28.688124+07	\N	NHZ4254800403	13	1	1	2026-05-28 07:02:00+07		f	2026-05-28	2	2026-05-28 15:02:00+07
253	2026-05-28 15:22:55.673601+07	2026-05-30 12:41:23.254099+07	\N	NHZ4254800403	18	1	1	2026-05-28 06:22:00+07		f	2026-05-28	0	2026-05-28 15:09:00+07
282	2026-05-29 22:53:04.874674+07	2026-05-30 12:42:27.468518+07	\N	NHZ4254800403	6	1	1	2026-05-29 22:53:00+07		f	2026-05-29	0	2026-05-30 07:04:00+07
275	2026-05-29 15:06:29.15381+07	2026-05-30 12:43:38.603686+07	\N	NHZ4254800403	14	1	1	2026-05-29 06:52:00+07		f	2026-05-29	0	2026-05-29 15:04:00+07
276	2026-05-29 15:12:55.469488+07	2026-05-30 12:45:10.562494+07	\N	NHZ4254800403	16	1	1	2026-05-29 06:55:00+07		f	2026-05-29	0	2026-05-29 15:09:00+07
258	2026-05-28 22:52:21.255354+07	2026-05-29 02:27:34.163165+07	\N	NHZ4254800403	11	1	0	2026-05-28 22:52:13+07		f	2026-05-28	0	\N
291	2026-05-30 12:53:47.99817+07	2026-05-31 06:57:25.324518+07	\N	NHZ4254800403	22	1	1	2026-05-30 06:53:00+07		f	2026-05-30	0	2026-05-31 06:57:25.074877+07
302	2026-05-30 22:59:19.442021+07	2026-05-31 06:59:13.430798+07	\N	NHZ4254800403	13	1	1	2026-05-30 22:59:19.192798+07		f	2026-05-30	0	2026-05-31 06:59:13.181066+07
298	2026-05-30 22:54:04.370265+07	2026-05-31 06:59:35.250122+07	\N	NHZ4254800403	11	1	1	2026-05-30 22:54:04.120812+07		f	2026-05-30	0	2026-05-31 06:59:34.999934+07
300	2026-05-30 22:58:00.549934+07	2026-05-31 07:00:17.452396+07	\N	NHZ4254800403	20	1	1	2026-05-30 22:58:00.300179+07		f	2026-05-30	0	2026-05-31 07:00:17.20107+07
299	2026-05-30 22:54:28.16638+07	2026-05-31 07:00:41.257009+07	\N	NHZ4254800403	10	1	1	2026-05-30 22:54:27.916607+07		f	2026-05-30	0	2026-05-31 07:00:41.006965+07
301	2026-05-30 22:58:15.467772+07	2026-05-31 07:01:14.076862+07	\N	NHZ4254800403	12	1	1	2026-05-30 22:58:15.216455+07		f	2026-05-30	0	2026-05-31 07:01:13.826981+07
297	2026-05-30 22:52:28.44993+07	2026-05-31 07:01:36.115516+07	\N	NHZ4254800403	6	1	1	2026-05-30 22:52:28.074996+07		f	2026-05-30	0	2026-05-31 07:01:35.8659+07
290	2026-05-30 12:49:49.710778+07	2026-05-31 07:03:14.089186+07	\N		16	0	1	2026-05-30 06:54:00+07		f	2026-05-30	0	2026-05-31 07:03:13.839367+07
307	2026-05-31 15:08:21.681406+07	2026-05-31 15:08:24.67483+07	\N	NHZ4254800403	15	1	1	2026-05-31 15:08:21.431571+07		f	2026-05-31	8	2026-05-31 15:08:24.425166+07
308	2026-05-31 15:08:50.801837+07	2026-05-31 15:08:50.801837+07	\N	NHZ4254800403	14	1	0	2026-05-31 15:08:50.551957+07		f	2026-05-31	8	\N
303	2026-05-31 14:51:13.426893+07	2026-06-01 06:51:44.488474+07	\N	NHZ4254800403	8	1	1	2026-05-31 14:51:13.176662+07		f	2026-05-31	0	2026-06-01 06:51:44.479091+07
306	2026-05-31 14:55:44.644524+07	2026-06-01 06:56:10.039529+07	\N	NHZ4254800403	3	1	1	2026-05-31 14:55:44.392743+07		f	2026-05-31	0	2026-06-01 06:56:10.02998+07
260	2026-05-28 22:52:49.309367+07	2026-05-29 07:02:20.849373+07	\N	NHZ4254800403	6	1	1	2026-05-28 22:52:41+07		f	2026-05-28	0	2026-05-29 07:02:20.841728+07
261	2026-05-28 22:55:31.39532+07	2026-05-29 07:02:53.997505+07	\N	NHZ4254800403	20	1	1	2026-05-28 22:55:23+07		f	2026-05-28	0	2026-05-29 07:02:53.989924+07
247	2026-05-28 14:51:50.362579+07	2026-05-29 02:27:40.144337+07	\N	NHZ4254800403	8	1	1	2026-05-28 14:51:42+07		f	2026-05-28	0	2026-05-28 23:13:17+07
250	2026-05-28 14:58:57.639665+07	2026-05-29 02:27:38.648253+07	\N	NHZ4254800403	3	1	1	2026-05-28 14:58:51+07		f	2026-05-28	0	2026-05-28 23:13:22+07
249	2026-05-28 14:56:44.588026+07	2026-05-29 02:27:39.020843+07	\N	NHZ4254800403	4	1	1	2026-05-28 14:56:37+07		f	2026-05-28	0	2026-05-28 23:13:36+07
248	2026-05-28 14:52:24.333181+07	2026-05-29 02:27:39.771294+07	\N	NHZ4254800403	7	1	1	2026-05-28 14:52:17+07		f	2026-05-28	0	2026-05-28 23:13:47+07
251	2026-05-28 15:20:57.04488+07	2026-05-30 12:36:21.630283+07	\N	NHZ4254800403	17	1	1	2026-05-28 06:54:00+07		f	2026-05-28	0	2026-05-28 15:20:00+07
256	2026-05-28 15:48:55.569059+07	2026-05-30 12:37:08.639219+07	\N	NHZ4254800403	16	1	1	2026-05-28 07:00:00+07		f	2026-05-28	0	2026-05-28 15:00:00+07
274	2026-05-29 15:05:54.079596+07	2026-05-30 12:44:04.567873+07	\N	NHZ4254800403	15	1	1	2026-05-29 06:56:00+07		f	2026-05-29	0	2026-05-29 15:04:00+07
295	2026-05-30 15:02:17.353408+07	2026-05-30 23:07:50.712027+07	\N	NHZ4254800403	5	1	1	2026-05-30 15:02:17.344032+07		f	2026-05-30	2	2026-05-30 23:07:50.462116+07
294	2026-05-30 14:56:28.145429+07	2026-05-30 23:09:50.050355+07	\N	NHZ4254800403	3	1	1	2026-05-30 14:56:28.13276+07		f	2026-05-30	0	2026-05-30 23:09:49.800855+07
293	2026-05-30 14:55:08.082901+07	2026-05-30 23:10:19.876652+07	\N	NHZ4254800403	4	1	1	2026-05-30 14:55:08.07347+07		f	2026-05-30	0	2026-05-30 23:10:19.627158+07
292	2026-05-30 14:51:46.488557+07	2026-05-30 23:10:30.039008+07	\N	NHZ4254800403	7	1	1	2026-05-30 14:51:46.473383+07		f	2026-05-30	0	2026-05-30 23:10:29.78943+07
309	2026-05-31 15:09:01.818765+07	2026-05-31 15:09:01.818765+07	\N	NHZ4254800403	22	1	0	2026-05-31 15:09:01.56915+07		f	2026-05-31	9	\N
310	2026-05-31 15:09:42.860002+07	2026-05-31 15:09:42.860002+07	\N	NHZ4254800403	17	1	0	2026-05-31 15:09:42.610407+07		f	2026-05-31	9	\N
311	2026-05-31 15:09:59.738584+07	2026-05-31 15:10:02.443551+07	\N	NHZ4254800403	18	1	1	2026-05-31 15:09:59.488957+07		f	2026-05-31	9	2026-05-31 15:10:02.190593+07
312	2026-05-31 15:10:11.369444+07	2026-05-31 15:10:11.369444+07	\N	NHZ4254800403	16	1	0	2026-05-31 15:10:11.120172+07		f	2026-05-31	10	\N
313	2026-06-01 06:57:17.830323+07	2026-06-01 06:57:17.830323+07	\N	NHZ4254800403	19	1	0	2026-06-01 06:57:17.821283+07		f	2026-05-31	0	\N
314	2026-06-01 07:09:54.663412+07	2026-06-01 07:09:54.663412+07	\N	NHZ4254800403	4	1	0	2026-06-01 07:09:54.65448+07		f	2026-05-31	9	\N
\.


--
-- Data for Name: bahan_nono_items; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.bahan_nono_items (id, created_at, updated_at, deleted_at, bahan_nono_id, jenis_bahan, kuantitas, unit, rate) FROM stdin;
39	2026-05-04 10:22:17.624683+07	2026-05-04 10:22:17.624683+07	2026-05-04 10:22:52.73295+07	62	Super A	1538	Kg	8300
40	2026-05-04 10:24:28.159495+07	2026-05-04 10:24:28.159495+07	\N	63	Super A	1538	Kg	8300
41	2026-05-04 10:26:00.35051+07	2026-05-04 10:26:00.35051+07	\N	64	Super A	1602	Kg	8300
42	2026-05-04 10:27:40.978407+07	2026-05-04 10:27:40.978407+07	\N	65	Super A	2242	Kg	8300
43	2026-05-05 20:37:48.895224+07	2026-05-05 20:37:48.895224+07	\N	67	Super A	2294	Kg	8500
44	2026-05-12 14:21:01.331559+07	2026-05-12 14:21:01.331559+07	\N	69	Super A	2366	Kg	8500
45	2026-05-12 17:53:22.0169+07	2026-05-12 17:53:22.0169+07	\N	70	Cerah A	1291	Kg	9500
46	2026-05-12 17:53:22.025896+07	2026-05-12 17:53:22.025896+07	\N	70	Super A	982	Kg	8500
47	2026-05-13 15:22:55.683918+07	2026-05-13 15:22:55.683918+07	\N	71	Super A	3038	Kg	8500
49	2026-05-14 15:03:12.91541+07	2026-05-14 15:03:12.91541+07	\N	74	Super A	2274	Kg	8500
48	2026-05-14 14:56:44.353713+07	2026-05-14 14:56:44.353713+07	2026-05-14 15:03:28.572376+07	73	Super A	2194	Kg	8500
50	2026-05-14 15:03:28.580957+07	2026-05-14 15:03:28.580957+07	\N	73	Super A	2194	Kg	8500
51	2026-05-15 15:05:41.1075+07	2026-05-15 15:05:41.1075+07	\N	76	Super A	1712	Kg	8500
52	2026-05-23 09:43:55.229593+07	2026-05-23 09:43:55.229593+07	\N	78	Super A	1933	Kg	8500
53	2026-05-23 09:43:55.237659+07	2026-05-23 09:43:55.237659+07	\N	78	[JASA] Titip Giling	476	Kg	1200
54	2026-05-23 10:07:42.309648+07	2026-05-23 10:07:42.309648+07	2026-05-23 10:08:10.398093+07	82	Super A	1220	Kg	8500
55	2026-05-23 10:07:42.317344+07	2026-05-23 10:07:42.317344+07	2026-05-23 10:08:10.398093+07	82	[JASA] Titip Giling	202	Kg	1200
56	2026-05-23 10:08:10.401918+07	2026-05-23 10:08:10.401918+07	2026-05-23 10:08:36.334284+07	82	Super A	1231	Kg	8500
57	2026-05-23 10:08:10.405829+07	2026-05-23 10:08:10.405829+07	2026-05-23 10:08:36.334284+07	82	[JASA] Titip Giling	202	Kg	1200
58	2026-05-23 10:08:36.338391+07	2026-05-23 10:08:36.338391+07	2026-05-23 10:09:26.491845+07	82	Super A	1241	Kg	8500
59	2026-05-23 10:08:36.342286+07	2026-05-23 10:08:36.342286+07	2026-05-23 10:09:26.491845+07	82	[JASA] Titip Giling	202	Kg	1200
60	2026-05-23 10:09:26.495762+07	2026-05-23 10:09:26.495762+07	2026-05-23 10:10:39.464987+07	82	Super A	1214	Kg	8500
61	2026-05-23 10:09:26.499504+07	2026-05-23 10:09:26.499504+07	2026-05-23 10:10:39.464987+07	82	[JASA] Titip Giling	202	Kg	1200
62	2026-05-23 10:10:39.468707+07	2026-05-23 10:10:39.468707+07	2026-05-23 10:11:07.33544+07	82	Super A	1228	Kg	8500
63	2026-05-23 10:10:39.472624+07	2026-05-23 10:10:39.472624+07	2026-05-23 10:11:07.33544+07	82	[JASA] Titip Giling	202	Kg	1200
64	2026-05-23 10:11:07.339193+07	2026-05-23 10:11:07.339193+07	2026-05-23 10:12:53.41432+07	82	Super A	1228	Kg	8500
65	2026-05-23 10:11:07.343088+07	2026-05-23 10:11:07.343088+07	2026-05-23 10:12:53.41432+07	82	[JASA] Titip Giling	204	Kg	1200
66	2026-05-23 10:12:53.41804+07	2026-05-23 10:12:53.41804+07	2026-05-23 10:13:16.998722+07	82	Super A	1220	Kg	8500
67	2026-05-23 10:12:53.421773+07	2026-05-23 10:12:53.421773+07	2026-05-23 10:13:16.998722+07	82	[JASA] Titip Giling	204	Kg	1200
68	2026-05-23 10:13:17.002404+07	2026-05-23 10:13:17.002404+07	2026-05-23 10:14:26.661607+07	82	Super A	1220	Kg	8500
69	2026-05-23 10:13:17.006045+07	2026-05-23 10:13:17.006045+07	2026-05-23 10:14:26.661607+07	82	[JASA] Titip Giling	202	Kg	1200
70	2026-05-23 10:14:26.665458+07	2026-05-23 10:14:26.665458+07	2026-05-23 10:14:44.31245+07	82	Super A	1220.5	Kg	8500
71	2026-05-23 10:14:26.669189+07	2026-05-23 10:14:26.669189+07	2026-05-23 10:14:44.31245+07	82	[JASA] Titip Giling	202	Kg	1200
72	2026-05-23 10:14:44.316304+07	2026-05-23 10:14:44.316304+07	2026-05-23 10:15:01.24848+07	82	Super A	1221	Kg	8500
73	2026-05-23 10:14:44.320136+07	2026-05-23 10:14:44.320136+07	2026-05-23 10:15:01.24848+07	82	[JASA] Titip Giling	202	Kg	1200
74	2026-05-23 10:15:01.252486+07	2026-05-23 10:15:01.252486+07	2026-05-23 10:16:21.762762+07	82	Super A	1220	Kg	8500
75	2026-05-23 10:15:01.25639+07	2026-05-23 10:15:01.25639+07	2026-05-23 10:16:21.762762+07	82	[JASA] Titip Giling	202	Kg	1200
76	2026-05-23 10:16:21.766504+07	2026-05-23 10:16:21.766504+07	2026-05-23 10:17:31.115164+07	82	Super A	1220.59	Kg	8500
77	2026-05-23 10:16:21.770171+07	2026-05-23 10:16:21.770171+07	2026-05-23 10:17:31.115164+07	82	[JASA] Titip Giling	202	Kg	1200
80	2026-05-23 10:33:22.890023+07	2026-05-23 10:33:22.890023+07	\N	83	Super A	2350	Kg	8500
81	2026-05-23 13:23:38.028107+07	2026-05-23 13:23:38.028107+07	\N	86	Super A	1413	Kg	8500
78	2026-05-23 10:17:31.119038+07	2026-05-23 10:17:31.119038+07	2026-05-23 13:24:14.921024+07	82	Super A	1220.55	Kg	8500
79	2026-05-23 10:17:31.1229+07	2026-05-23 10:17:31.1229+07	2026-05-23 13:24:14.921024+07	82	[JASA] Titip Giling	202	Kg	1200
82	2026-05-23 13:24:14.92533+07	2026-05-23 13:24:14.92533+07	\N	82	Super A	1220	Kg	8500
83	2026-05-23 13:24:14.929117+07	2026-05-23 13:24:14.929117+07	\N	82	[JASA] Titip Giling	202	Kg	1200
84	2026-05-23 13:26:59.506811+07	2026-05-23 13:26:59.506811+07	2026-05-23 13:29:33.578284+07	87	Super A	1300	Kg	8500
85	2026-05-23 13:26:59.51062+07	2026-05-23 13:26:59.51062+07	2026-05-23 13:29:33.578284+07	87	[JASA] Titip Giling	690	Kg	1200
86	2026-05-23 13:29:33.581901+07	2026-05-23 13:29:33.581901+07	\N	87	Super A	1300	Kg	8500
87	2026-05-23 13:29:33.585574+07	2026-05-23 13:29:33.585574+07	\N	87	[JASA] Titip Giling	690	Kg	1200
88	2026-05-23 13:39:54.642734+07	2026-05-23 13:39:54.642734+07	\N	88	Super A	1872	Kg	8500
89	2026-05-23 13:40:58.957132+07	2026-05-23 13:40:58.957132+07	\N	89	Super A	2018	Kg	8500
90	2026-05-23 13:41:59.735831+07	2026-05-23 13:41:59.735831+07	2026-05-23 13:42:42.77237+07	90	Super A	1464	Kg	8500
91	2026-05-23 13:41:59.739589+07	2026-05-23 13:41:59.739589+07	2026-05-23 13:42:42.77237+07	90	[JASA] Titip Giling	290	Kg	1200
92	2026-05-23 13:42:42.776323+07	2026-05-23 13:42:42.776323+07	\N	90	Super A	1464	Kg	8300
93	2026-05-23 13:42:42.780215+07	2026-05-23 13:42:42.780215+07	\N	90	[JASA] Titip Giling	290	Kg	9500
94	2026-05-30 15:08:57.894778+07	2026-05-30 15:08:57.894778+07	\N	92	Super A	2434	Kg	8500
\.


--
-- Data for Name: bahan_nonos; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.bahan_nonos (id, created_at, updated_at, deleted_at, tanggal, nominal, notes, tagihan, total_harga, date_created, is_demo) FROM stdin;
27	2026-04-14 12:44:47.992874+07	2026-04-14 12:44:47.992874+07	\N	2026-04-10	0	setoran pagi	uploads/tagihan_nono/17761454502722252457073246195439_guzcr9	179440200	2026-04-14 12:44:47.992874+07	f
28	2026-04-14 12:45:59.594776+07	2026-04-14 12:45:59.594776+07	\N	2026-04-10	0	setoran pagi	uploads/tagihan_nono/17761455335001719376742123408505_r2beku	17632000	2026-04-14 12:45:59.594776+07	f
34	2026-04-14 13:00:36.464712+07	2026-04-14 13:00:36.464712+07	\N	2026-04-14	20000000	TF	uploads/tagihan_nono/17761464197856947976188751566836_eu43mv	0	2026-04-14 13:00:36.464712+07	f
35	2026-04-14 13:04:13.839212+07	2026-04-14 13:04:13.839212+07	\N	2026-04-10	20000000	TF	uploads/tagihan_nono/17761466431441880580606829538504_kyu7c3	0	2026-04-14 13:04:13.839212+07	f
36	2026-04-14 13:04:53.406573+07	2026-04-14 13:04:53.406573+07	\N	2026-04-13	50000000	TF	uploads/tagihan_nono/17761466834582792451293268839747_vavvbj	0	2026-04-14 13:04:53.406573+07	f
37	2026-04-14 15:51:57.321532+07	2026-04-14 15:51:57.321532+07	\N	2026-04-14	0	setoran sore	uploads/tagihan_nono/17761566957829027145017300715374_ri2mjn	18320000	2026-04-14 15:51:57.321532+07	f
38	2026-04-15 14:45:38.263314+07	2026-04-15 14:45:38.263314+07	\N	2026-04-15	0	setoran sore	uploads/tagihan_nono/17762390918807468112277313179839_roa3f6	18000000	2026-04-15 14:45:38.263314+07	f
39	2026-04-16 15:08:01.534708+07	2026-04-16 15:08:01.534708+07	\N	2026-04-16	0	setoran sore	uploads/tagihan_nono/17763268993418295397076887277854_fca9ge	18990400	2026-04-16 15:08:01.534708+07	f
40	2026-04-16 18:43:06.213595+07	2026-04-16 18:43:06.213595+07	\N	2026-04-16	40000000	TF BRI	uploads/tagihan_nono/IMG-20260416-WA0022_rc80m1	0	2026-04-16 18:43:06.213595+07	f
41	2026-04-17 15:19:36.181414+07	2026-04-17 15:19:36.181414+07	\N	2026-04-17	0	setoran sore	uploads/tagihan_nono/17764139289395154966230074889545_g5jwp6	20608900	2026-04-17 15:19:36.181414+07	f
42	2026-04-20 19:11:18.96981+07	2026-04-20 19:11:18.96981+07	\N	2026-04-20	30000000	TF	uploads/tagihan_nono/IMG-20260420-WA0012_keul4m	0	2026-04-20 19:11:18.96981+07	f
43	2026-04-21 09:48:16.837747+07	2026-04-21 09:48:16.837747+07	\N	2026-04-20	0	setoran sore	uploads/tagihan_nono/IMG_20260421_094629_hn571h	16500400	2026-04-21 09:48:16.837747+07	f
44	2026-04-21 09:49:29.60494+07	2026-04-21 09:49:29.60494+07	\N	2026-04-21	30000000	Tunai 	uploads/tagihan_nono/17767397404302738797771452715500_fwqow8	0	2026-04-21 09:49:29.60494+07	f
45	2026-04-21 15:19:36.562927+07	2026-04-21 15:19:36.562927+07	\N	2026-04-21	0	setoran sore	uploads/tagihan_nono/17767595408294200217397161003955_xnxj92	22874800	2026-04-21 15:19:36.562927+07	f
46	2026-04-22 15:49:09.567383+07	2026-04-22 15:49:09.567383+07	\N	2026-04-22	0	setoran sore	uploads/tagihan_nono/IMG-20260423-WA0005_iuxl1a	8300000	2026-04-22 15:49:09.567383+07	f
47	2026-04-23 17:17:51.550481+07	2026-04-23 17:17:51.550481+07	\N	2026-04-23	25000000	Tunai		0	2026-04-23 17:17:51.550481+07	f
48	2026-04-24 11:16:45.718338+07	2026-04-24 11:16:45.718338+07	\N	2026-04-24	40000000	TF BRI	uploads/tagihan_nono/IMG-20260424-WA0049_qqcodz	0	2026-04-24 11:16:45.718338+07	f
49	2026-04-24 18:01:57.423479+07	2026-04-24 18:01:57.423479+07	\N	2026-04-24	0	setoran sore	uploads/tagihan_nono/IMG_20260424_155131_rrioso	16467200	2026-04-24 18:01:57.423479+07	f
50	2026-04-24 18:02:48.074742+07	2026-04-24 18:02:48.074742+07	\N	2026-04-23	0	setoran sore	uploads/tagihan_nono/IMG-20260424-WA0067_trei8a	14491800	2026-04-24 18:02:48.074742+07	f
51	2026-04-25 14:49:28.48068+07	2026-04-25 14:49:28.48068+07	\N	2026-04-25	0	setoran sore	uploads/tagihan_nono/IMG-20260425-WA0027_gfrxxj	29904900	2026-04-25 14:49:28.48068+07	f
52	2026-04-25 16:42:52.561789+07	2026-04-25 16:42:52.561789+07	\N	2026-04-25	2000000	TF	uploads/tagihan_nono/IMG-20260425-WA0036_qzpvuu	0	2026-04-25 16:42:52.561789+07	f
53	2026-04-27 14:58:47.518623+07	2026-04-27 14:58:47.518623+07	\N	2026-04-27	0	Setoran Sore	uploads/tagihan_nono/IMG_20260427_145256_lhaqbk	14201300	2026-04-27 14:58:47.518623+07	f
54	2026-04-28 12:11:09.209695+07	2026-04-28 12:11:09.209695+07	\N	2026-04-28	35000000	TF BRI	uploads/tagihan_nono/IMG-20260428-WA0064_sh34o2	0	2026-04-28 12:11:09.209695+07	f
25	2026-04-14 12:26:37.714393+07	2026-04-14 12:26:37.714393+07	\N	2026-04-13	0		uploads/tagihan_nono/17761443724868552854407591220813_b9jlcj	21200000	2026-04-14 12:26:37.714393+07	f
55	2026-04-29 22:54:07.399032+07	2026-04-29 22:54:07.399032+07	2026-04-29 22:54:27.476846+07	2026-04-29	40000000	Setoran Sore		0	0001-01-01 07:07:12+07:07:12	f
56	2026-05-04 09:42:07.32638+07	2026-05-04 09:42:07.32638+07	2026-05-04 09:43:13.059806+07	2026-05-04	30000000	TF		0	0001-01-01 07:07:12+07:07:12	f
57	2026-05-04 09:46:28.627049+07	2026-05-04 09:46:28.627049+07	2026-05-04 09:50:47.348621+07	2026-05-04	30000000			0	0001-01-01 07:07:12+07:07:12	f
58	2026-05-04 09:51:06.697878+07	2026-05-04 09:51:06.697878+07	2026-05-04 09:51:20.888774+07	2026-05-04	30000000			0	0001-01-01 07:07:12+07:07:12	f
59	2026-05-04 09:53:50.567494+07	2026-05-04 09:53:50.567494+07	2026-05-04 10:05:17.559195+07	2026-05-04	30000000			0	0001-01-01 07:07:12+07:07:12	f
60	2026-05-04 10:06:23.307134+07	2026-05-04 10:06:23.307134+07	2026-05-04 10:13:45.237092+07	2026-05-04	30000000			0	0001-01-01 07:07:12+07:07:12	f
62	2026-05-04 10:22:17.620586+07	2026-05-04 10:22:17.620586+07	2026-05-04 10:22:52.747131+07	2026-05-04	0	Setoran sore		12765400	0001-01-01 07:07:12+07:07:12	f
63	2026-05-04 10:24:28.155237+07	2026-05-04 10:24:28.155237+07	\N	2026-05-01	0	Setoran Sore	https://res.cloudinary.com/dkkbizenf/image/upload/v1777865016/tagihan_nono/oruidmarofdscwr9irnb.jpg	12765400	0001-01-01 07:07:12+07:07:12	f
64	2026-05-04 10:26:00.346444+07	2026-05-04 10:26:00.346444+07	\N	2026-04-30	0	Setoran Sore	https://res.cloudinary.com/dkkbizenf/image/upload/v1777865145/tagihan_nono/h0gawmv6qvojdpqbgvaa.jpg	13296600	0001-01-01 07:07:12+07:07:12	f
65	2026-05-04 10:27:40.974409+07	2026-05-04 10:27:40.974409+07	\N	2026-04-29	0	setoran sore	https://res.cloudinary.com/dkkbizenf/image/upload/v1777865231/tagihan_nono/qfnsoxfzyhbtynkemxz8.jpg	18608600	0001-01-01 07:07:12+07:07:12	f
66	2026-05-04 10:29:45.083487+07	2026-05-04 10:29:45.083487+07	\N	2026-04-29	40000000	TF BRI	https://res.cloudinary.com/dkkbizenf/image/upload/v1777865363/tagihan_nono/vho6xp24i5ea0gxqtjhk.jpg	0	0001-01-01 07:07:12+07:07:12	f
67	2026-05-05 20:37:48.886024+07	2026-05-05 20:37:48.886024+07	\N	2026-05-05	0		https://res.cloudinary.com/dkkbizenf/image/upload/v1777988254/tagihan_nono/dfkma21izc64zzrye5ce.jpg	19499000	0001-01-01 07:07:12+07:07:12	f
74	2026-05-14 15:03:12.906299+07	2026-05-14 15:03:12.906299+07	\N	2026-05-14	0	setoran sore	https://res.cloudinary.com/dkkbizenf/image/upload/v1778745773/tagihan_nono/ie3bkl6cgf5jmmvnrq1a.jpg	19329000	0001-01-01 07:07:12+07:07:12	f
61	2026-05-04 10:14:59.151379+07	2026-05-07 09:33:50.973218+07	\N	2026-05-04	30000000	TF BCA	https://res.cloudinary.com/dkkbizenf/image/upload/v1777864490/tagihan_nono/gqpvmvadaetezls7gyh3.jpg	0	0001-01-01 07:07:12+07:07:12	f
68	2026-05-05 20:38:46.91001+07	2026-05-07 09:34:04.852714+07	\N	2026-05-05	30000000	TF BCA	https://res.cloudinary.com/dkkbizenf/image/upload/v1777988311/tagihan_nono/hwfp5q9syxqpr5kkfmro.jpg	0	0001-01-01 07:07:12+07:07:12	f
69	2026-05-12 14:21:01.32098+07	2026-05-12 14:21:01.32098+07	\N	2026-05-12	0	setoran sore	https://res.cloudinary.com/dkkbizenf/image/upload/v1778570447/tagihan_nono/wpu0i86vu5s37pwaatuz.jpg	20111000	0001-01-01 07:07:12+07:07:12	f
70	2026-05-12 17:53:22.0067+07	2026-05-12 17:53:22.0067+07	\N	2026-05-12	0	setoran sore	https://res.cloudinary.com/dkkbizenf/image/upload/v1778583198/tagihan_nono/hi2wbvfyl2fdc2otyezw.jpg	20611500	0001-01-01 07:07:12+07:07:12	f
71	2026-05-13 15:22:55.675204+07	2026-05-13 15:22:55.675204+07	\N	2026-05-13	0	setoran sore	https://res.cloudinary.com/dkkbizenf/image/upload/v1778660560/tagihan_nono/ts9bsemoaafcnu2msmyo.jpg	25823000	0001-01-01 07:07:12+07:07:12	f
72	2026-05-13 15:24:35.123167+07	2026-05-13 15:24:35.123167+07	\N	2026-05-12	5000000	Tunai		0	0001-01-01 07:07:12+07:07:12	f
73	2026-05-14 14:56:44.344712+07	2026-05-14 15:03:28.585344+07	\N	2026-05-08	0	setoran sore	https://res.cloudinary.com/dkkbizenf/image/upload/v1778745388/tagihan_nono/brdpbojnr1lmbd6ygua5.jpg	18649000	0001-01-01 07:07:12+07:07:12	f
75	2026-05-14 19:42:29.103935+07	2026-05-14 19:42:29.103935+07	\N	2026-05-14	30000000	TF BCA	https://res.cloudinary.com/dkkbizenf/image/upload/v1778762536/tagihan_nono/z96rlbetvlac5ddxzhpa.jpg	0	0001-01-01 07:07:12+07:07:12	f
76	2026-05-15 15:05:41.099404+07	2026-05-15 15:05:41.099404+07	\N	2026-05-15	0	setoran sore	https://res.cloudinary.com/dkkbizenf/image/upload/v1778832317/tagihan_nono/i84aifkefru7yxdflrzc.jpg	14552000	0001-01-01 07:07:12+07:07:12	f
77	2026-05-15 15:40:32.888717+07	2026-05-15 15:40:32.888717+07	\N	2026-05-15	40000000	TF BRI	https://res.cloudinary.com/dkkbizenf/image/upload/v1778834418/tagihan_nono/pxsp1npidaedmfdo3imx.jpg	0	0001-01-01 07:07:12+07:07:12	f
78	2026-05-23 09:43:55.221884+07	2026-05-23 09:43:55.221884+07	\N	2026-05-18	0	setoran sore	https://res.cloudinary.com/dkkbizenf/image/upload/v1779504211/tagihan_nono/jjpsoptmop6dcbhqmpgf.jpg	17001700	0001-01-01 07:07:12+07:07:12	f
79	2026-05-23 09:51:22.456153+07	2026-05-23 09:51:22.456153+07	\N	2026-05-23	20000000		https://res.cloudinary.com/dkkbizenf/image/upload/v1779504673/tagihan_nono/dt8v7z7n4809l87m3ljv.jpg	0	0001-01-01 07:07:12+07:07:12	f
80	2026-05-23 09:59:15.566834+07	2026-05-23 09:59:45.795798+07	\N	2026-05-23	6000000	BCA	https://res.cloudinary.com/dkkbizenf/image/upload/v1779505134/tagihan_nono/tq9gn0ou0zhmy9gdebrm.jpg	0	0001-01-01 07:07:12+07:07:12	f
81	2026-05-23 10:00:06.411834+07	2026-05-23 10:00:06.411834+07	\N	2026-05-23	30000000	TF BRI	https://res.cloudinary.com/dkkbizenf/image/upload/v1779505199/tagihan_nono/owcvzg8pwtesyjactqps.jpg	0	0001-01-01 07:07:12+07:07:12	f
83	2026-05-23 10:33:22.886156+07	2026-05-23 10:33:22.886156+07	\N	2026-05-21	0	setoran sore	https://res.cloudinary.com/dkkbizenf/image/upload/v1779507181/tagihan_nono/sfckbl3a2bicqyw4sovr.jpg	19975000	0001-01-01 07:07:12+07:07:12	f
84	2026-05-23 10:44:51.919361+07	2026-05-23 10:45:05.816757+07	\N	2026-05-20	40000000	tunai		0	0001-01-01 07:07:12+07:07:12	f
85	2026-05-23 10:45:41.336776+07	2026-05-23 10:45:41.336776+07	\N	2026-05-21	30000000	tunai		0	0001-01-01 07:07:12+07:07:12	f
86	2026-05-23 13:23:38.024266+07	2026-05-23 13:23:38.024266+07	\N	2026-05-23	0	setoran sore	https://res.cloudinary.com/dkkbizenf/image/upload/v1779517390/tagihan_nono/ealfhbplapupfikxig8t.jpg	12010500	0001-01-01 07:07:12+07:07:12	f
82	2026-05-23 10:07:42.305808+07	2026-05-23 13:24:14.932682+07	\N	2026-05-20	0	setoran sore	https://res.cloudinary.com/dkkbizenf/image/upload/v1779505471/tagihan_nono/smiiklwd2ayoeosoqjpt.jpg	10612400	0001-01-01 07:07:12+07:07:12	f
87	2026-05-23 13:26:59.502984+07	2026-05-23 13:29:33.589227+07	\N	2026-05-11	0	setoran sore	https://res.cloudinary.com/dkkbizenf/image/upload/v1779517597/tagihan_nono/evb8cnxmwyqgk3p7otq5.jpg	11878000	0001-01-01 07:07:12+07:07:12	f
88	2026-05-23 13:39:54.638748+07	2026-05-23 13:39:54.638748+07	\N	2026-05-07	0	setoran sore	https://res.cloudinary.com/dkkbizenf/image/upload/v1779518373/tagihan_nono/v2msqywhf3z0bvbli7js.jpg	15912000	0001-01-01 07:07:12+07:07:12	f
89	2026-05-23 13:40:58.953179+07	2026-05-23 13:40:58.953179+07	\N	2026-05-06	0	setoran sore	https://res.cloudinary.com/dkkbizenf/image/upload/v1779518444/tagihan_nono/qzvyqonnkomkoi6qakjv.jpg	17153000	0001-01-01 07:07:12+07:07:12	f
90	2026-05-23 13:41:59.732015+07	2026-05-23 13:42:42.784182+07	\N	2026-05-04	0	setoran sore	https://res.cloudinary.com/dkkbizenf/image/upload/v1779518500/tagihan_nono/dli6xqtfmllrg4eblqs1.jpg	14906200	0001-01-01 07:07:12+07:07:12	f
91	2026-05-28 12:02:43.212357+07	2026-05-28 12:02:43.212357+07	\N	2026-05-28	30000000	setoran sore	https://res.cloudinary.com/dkkbizenf/image/upload/v1779944553/tagihan_nono/dkxeaaejapsuw1va4it9.jpg	0	0001-01-01 07:07:12+07:07:12	f
92	2026-05-30 15:08:57.885313+07	2026-05-30 15:08:57.885313+07	\N	2026-05-30	0	setoran sore	https://res.cloudinary.com/dkkbizenf/image/upload/v1780128532/tagihan_nono/udh06a7dbjj3iruefyp4.jpg	20689000	0001-01-01 07:07:12+07:07:12	f
93	2026-05-31 19:40:34.270166+07	2026-05-31 19:40:34.270166+07	\N	2026-05-31	40000000	TF BRI	https://res.cloudinary.com/dkkbizenf/image/upload/v1780231225/tagihan_nono/nbseay1g6jc4kklynf8w.jpg	0	0001-01-01 07:07:12+07:07:12	f
\.


--
-- Data for Name: cash_flows; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) FROM stdin;
353	2026-04-30 11:36:21.666943+07	2026-04-30 11:36:21.666943+07	\N	2026-04-28	KELUAR	jasa angkut - pak gito	70000	\N	2026-04-30 11:36:21.658435+07	\N	\N	\N	\N	f
136	2026-04-28 19:42:44.767511+07	2026-04-28 19:42:44.767511+07	2026-04-29 12:07:39.85175+07	2026-02-17	KELUAR	Listrik PLN	28246157	\N	2026-04-28 19:42:44.76163+07	\N	\N	\N	\N	f
354	2026-04-30 11:36:51.113456+07	2026-04-30 11:36:51.113456+07	\N	2026-04-28	KELUAR	uang saku - pak tono	520000	\N	2026-04-30 11:36:51.105067+07	\N	\N	\N	\N	f
367	2026-04-30 15:12:40.951648+07	2026-04-30 15:12:40.951648+07	2026-05-01 14:28:25.519674+07	2026-02-17	KELUAR	bayar pln	28246157	\N	2026-04-30 15:12:40.94418+07	\N	\N	\N	\N	f
357	2026-04-30 11:42:01.63659+07	2026-04-30 11:42:01.63659+07	2026-05-01 14:28:34.720672+07	2026-02-17	KELUAR	bayar pln	28246157	\N	2026-04-30 11:42:01.627853+07	\N	\N	\N	\N	f
358	2026-04-30 11:47:46.556709+07	2026-04-30 11:47:46.556709+07	2026-05-01 14:28:41.290574+07	2026-02-17	KELUAR	bayar pln	28246157	\N	2026-04-30 11:47:46.548363+07	\N	\N	\N	\N	f
368	2026-04-30 18:07:10.626036+07	2026-04-30 18:07:10.626036+07	2026-05-01 14:28:57.060976+07	2026-02-17	KELUAR	listrik pln	2854187	\N	2026-04-30 18:07:10.616637+07	\N	\N	\N	\N	f
355	2026-04-30 11:37:32.89133+07	2026-04-30 11:37:32.89133+07	2026-05-01 21:14:04.279032+07	2026-04-27	KELUAR	meisn fingerprint, LAN, Router smartfren	3000000	\N	2026-04-30 11:37:32.883054+07	\N	\N	\N	\N	f
373	2026-05-02 00:05:32.50807+07	2026-05-02 00:05:32.50807+07	2026-05-02 00:05:45.17233+07	2026-05-01	KELUAR	Pembayaran Gaji: 	52500	\N	2026-05-02 00:05:32.50799+07	\N	\N	\N	\N	f
356	2026-04-30 11:39:42.976008+07	2026-04-30 11:39:42.976008+07	2026-05-02 09:56:23.820404+07	2026-02-17	MASUK	Bayar Listrik PLN 	28246157	\N	2026-04-30 11:39:42.967616+07	\N	\N	\N	\N	f
377	2026-05-02 10:20:21.140086+07	2026-05-02 10:20:21.140086+07	2026-05-02 10:20:52.654576+07	2026-05-02	KELUAR	Pembayaran Gaji: 	315000	\N	2026-05-02 10:20:21.140068+07	\N	\N	\N	\N	f
378	2026-05-02 10:22:38.472335+07	2026-05-02 10:22:38.472335+07	2026-05-02 10:23:07.842366+07	2026-05-02	KELUAR	Pembayaran Gaji: 	360000	\N	2026-05-02 10:22:38.472317+07	\N	\N	\N	\N	f
381	2026-05-02 10:22:39.751142+07	2026-05-02 10:22:39.751142+07	2026-05-02 10:23:11.033071+07	2026-05-02	KELUAR	Pembayaran Gaji: 	300000	\N	2026-05-02 10:22:39.751118+07	\N	\N	\N	\N	f
380	2026-05-02 10:22:39.443711+07	2026-05-02 10:22:39.443711+07	2026-05-02 10:23:13.839492+07	2026-05-02	KELUAR	Pembayaran Gaji: 	315000	\N	2026-05-02 10:22:39.443694+07	\N	\N	\N	\N	f
379	2026-05-02 10:22:38.980001+07	2026-05-02 10:22:38.980001+07	2026-05-02 10:23:16.761443+07	2026-05-02	KELUAR	Pembayaran Gaji: 	345000	\N	2026-05-02 10:22:38.979982+07	\N	\N	\N	\N	f
393	2026-05-02 10:49:08.929675+07	2026-05-02 10:49:08.929675+07	2026-05-02 10:52:28.998199+07	2026-05-02	KELUAR	Pembayaran Gaji: 	157500	\N	2026-05-02 10:49:08.929647+07	\N	\N	\N	\N	f
392	2026-05-02 10:49:08.445257+07	2026-05-02 10:49:08.445257+07	2026-05-02 10:52:31.588777+07	2026-05-02	KELUAR	Pembayaran Gaji: 	262500	\N	2026-05-02 10:49:08.44524+07	\N	\N	\N	\N	f
391	2026-05-02 10:49:07.955238+07	2026-05-02 10:49:07.955238+07	2026-05-02 10:52:34.014426+07	2026-05-02	KELUAR	Pembayaran Gaji: 	405000	\N	2026-05-02 10:49:07.955217+07	\N	\N	\N	\N	f
390	2026-05-02 10:49:07.44102+07	2026-05-02 10:49:07.44102+07	2026-05-02 10:52:36.288573+07	2026-05-02	KELUAR	Pembayaran Gaji: 	450000	\N	2026-05-02 10:49:07.441007+07	\N	\N	\N	\N	f
410	2026-05-02 12:39:17.615844+07	2026-05-02 12:39:17.615844+07	2026-05-02 12:51:59.534706+07	2026-05-02	KELUAR	Pembayaran Gaji: 	157500	\N	2026-05-02 12:39:17.615831+07	\N	\N	\N	\N	f
409	2026-05-02 12:39:17.151886+07	2026-05-02 12:39:17.151886+07	2026-05-02 12:52:01.939936+07	2026-05-02	KELUAR	Pembayaran Gaji: 	210000	\N	2026-05-02 12:39:17.151873+07	\N	\N	\N	\N	f
408	2026-05-02 12:39:16.664397+07	2026-05-02 12:39:16.664397+07	2026-05-02 12:52:04.382307+07	2026-05-02	KELUAR	Pembayaran Gaji: 	220000	\N	2026-05-02 12:39:16.664381+07	\N	\N	\N	\N	f
407	2026-05-02 12:39:16.190362+07	2026-05-02 12:39:16.190362+07	2026-05-02 12:52:06.569318+07	2026-05-02	KELUAR	Pembayaran Gaji: 	300000	\N	2026-05-02 12:39:16.190343+07	\N	\N	\N	\N	f
412	2026-05-02 12:58:17.595467+07	2026-05-02 12:58:17.595467+07	2026-05-02 12:59:02.57075+07	2026-05-02	KELUAR	Pembayaran Gaji: 60k	315000	\N	2026-05-02 12:58:17.595448+07	\N	\N	\N	\N	f
416	2026-05-02 12:58:18.774232+07	2026-05-02 12:58:18.774232+07	2026-05-02 12:59:02.57075+07	2026-05-02	KELUAR	Pembayaran Gaji: 60k	315000	\N	2026-05-02 12:58:18.774215+07	\N	\N	\N	\N	f
415	2026-05-02 12:58:18.533691+07	2026-05-02 12:58:18.533691+07	2026-05-02 12:59:05.894841+07	2026-05-02	KELUAR	Pembayaran Gaji: 	345000	\N	2026-05-02 12:58:18.533677+07	\N	\N	\N	\N	f
411	2026-05-02 12:58:17.294949+07	2026-05-02 12:58:17.294949+07	2026-05-02 12:59:08.447762+07	2026-05-02	KELUAR	Pembayaran Gaji: bonus 30k	450000	\N	2026-05-02 12:58:17.294931+07	\N	\N	\N	\N	f
414	2026-05-02 12:58:18.476416+07	2026-05-02 12:58:18.476416+07	2026-05-02 12:59:08.447762+07	2026-05-02	KELUAR	Pembayaran Gaji: bonus 30k	450000	\N	2026-05-02 12:58:18.476395+07	\N	\N	\N	\N	f
413	2026-05-02 12:58:18.067093+07	2026-05-02 12:58:18.067093+07	2026-05-02 12:59:11.244673+07	2026-05-02	KELUAR	Pembayaran Gaji: 	300000	\N	2026-05-02 12:58:18.067042+07	\N	\N	\N	\N	f
420	2026-05-02 13:06:42.837651+07	2026-05-02 13:06:42.837651+07	2026-05-02 13:09:08.345018+07	2026-05-02	KELUAR	Pembayaran Gaji: bonus : 42k extra : 25k tgl merah 15k	345000	\N	2026-05-02 13:06:42.837635+07	\N	\N	\N	\N	f
419	2026-05-02 13:06:42.355718+07	2026-05-02 13:06:42.355718+07	2026-05-02 13:09:11.187099+07	2026-05-02	KELUAR	Pembayaran Gaji: bonus : 30K extra 40k tgl merah 15k	315000	\N	2026-05-02 13:06:42.3557+07	\N	\N	\N	\N	f
418	2026-05-02 13:06:42.074741+07	2026-05-02 13:06:42.074741+07	2026-05-02 13:09:13.896457+07	2026-05-02	KELUAR	Pembayaran Gaji: bonus : 30k ekstra :20k tgl merah + 15k	285000	\N	2026-05-02 13:06:42.074724+07	\N	\N	\N	\N	f
417	2026-05-02 13:06:41.566504+07	2026-05-02 13:06:41.566504+07	2026-05-02 13:09:16.478816+07	2026-05-02	KELUAR	Pembayaran Gaji: bonus: 35k ekstra 20k tgl merah :+15k	345000	\N	2026-05-02 13:06:41.566486+07	\N	\N	\N	\N	f
431	2026-05-05 16:50:09.640731+07	2026-05-05 16:50:09.640731+07	\N	2026-05-05	MASUK	Pembayaran Faktur BMP-2605-002 (mas wiranto)	10000000	7	2026-05-05 16:50:09.640706+07	\N	\N	\N	\N	f
439	2026-05-13 20:28:55.266397+07	2026-05-13 20:28:55.266397+07	\N	2026-05-13	MASUK	Pembayaran Faktur BMP-0426-007 (abah ali)	53000000	12	2026-05-13 20:28:55.266374+07	\N	\N	\N	\N	f
440	2026-05-13 21:35:58.286544+07	2026-05-13 21:35:58.286544+07	\N	2026-05-12	KELUAR	kapasitor Ducati 10 kvar, 2 pcs	1786000	\N	2026-05-13 21:35:58.277037+07	\N	\N	\N	\N	f
445	2026-05-15 11:10:44.35114+07	2026-05-15 11:10:44.35114+07	\N	2026-05-15	MASUK	Pembayaran Faktur BMP-0426-010 (mas wiranto)	14835000	15	2026-05-15 11:10:44.351125+07	\N	\N	\N	\N	f
450	2026-05-22 14:03:23.250664+07	2026-05-22 14:03:23.250664+07	\N	2026-05-21	MASUK	Pembayaran Faktur BMP-0426-001 (abah ali)	31700000	18	2026-05-22 14:03:23.250641+07	\N	\N	\N	\N	f
451	2026-05-22 14:07:14.592566+07	2026-05-22 14:07:14.592566+07	\N	2026-05-15	KELUAR	Ongkir 	1100000	\N	2026-05-22 14:07:14.58445+07	\N	\N	\N	\N	f
452	2026-05-22 14:07:48.639028+07	2026-05-22 14:07:48.639028+07	\N	2026-05-19	KELUAR	Ongkir - Ko Hary	800000	\N	2026-05-22 14:07:48.631034+07	\N	\N	\N	\N	f
453	2026-05-22 14:10:45.412352+07	2026-05-22 14:10:45.412352+07	\N	2026-05-15	KELUAR	Jasa Angkut - Pak Sandi	250000	\N	2026-05-22 14:10:45.40155+07	\N	\N	\N	\N	f
454	2026-05-22 14:11:10.809016+07	2026-05-22 14:11:10.809016+07	\N	2026-05-19	KELUAR	Jasa Angkut - Ko Hary	250000	\N	2026-05-22 14:11:10.800864+07	\N	\N	\N	\N	f
455	2026-05-22 14:11:52.598489+07	2026-05-22 14:11:52.598489+07	\N	2026-05-21	KELUAR	Jasa Angkut - Ko Hary	300000	\N	2026-05-22 14:11:52.590083+07	\N	\N	\N	\N	f
456	2026-05-22 14:12:28.280271+07	2026-05-22 14:12:28.280271+07	\N	2026-05-21	KELUAR	Ongkir - Abah Kosi'in Grobogan	1620000	\N	2026-05-22 14:12:28.272064+07	\N	\N	\N	\N	f
448	2026-05-22 11:31:06.995227+07	2026-05-22 11:31:06.995227+07	2026-05-26 10:21:01.024392+07	2026-05-22	KELUAR	Pembelian barang khusus untuk Faktur BMP-2605-015	5100	\N	2026-05-22 11:31:06.995211+07	\N	\N	\N	\N	f
449	2026-05-22 11:32:37.182488+07	2026-05-22 11:32:37.182488+07	2026-05-26 10:21:10.975511+07	2026-05-22	KELUAR	Pembelian barang khusus (Update) untuk Faktur BMP-2605-015	5100	\N	2026-05-22 11:32:37.18247+07	\N	\N	\N	\N	f
461	2026-05-26 10:35:50.809609+07	2026-05-26 10:35:50.809609+07	\N	2026-05-23	MASUK	Pembayaran Faktur BMP-2605-013 (Mas Malvin)	20000000	23	2026-05-26 10:35:50.809584+07	\N	\N	\N	\N	f
462	2026-05-26 10:39:01.90627+07	2026-05-26 10:39:01.90627+07	\N	2026-05-26	MASUK	Pembayaran Faktur BMP-2605-013 (Mas Malvin)	13630000	24	2026-05-26 10:39:01.906243+07	\N	\N	\N	\N	f
465	2026-05-27 02:12:52.561526+07	2026-05-27 02:12:52.561526+07	2026-05-27 02:13:30.02708+07	2026-05-26	KELUAR	Pembayaran Gaji: Denda dihapus (manual)	83300	\N	2026-05-27 02:12:52.561511+07	\N	\N	\N	\N	f
466	2026-05-27 02:17:25.033116+07	2026-05-27 02:17:25.033116+07	2026-05-27 02:17:48.333027+07	2026-05-26	KELUAR	Pembayaran Gaji: Denda dihapus (manual)	83300	\N	2026-05-27 02:17:25.032841+07	\N	\N	\N	\N	f
359	2026-04-30 12:11:17.131836+07	2026-04-30 12:11:17.131836+07	2026-05-01 14:28:05.694723+07	2026-02-17	KELUAR	Bayar Listrik	28246157	\N	2026-04-30 12:11:17.121149+07	\N	\N	\N	\N	f
369	2026-05-01 21:10:11.402405+07	2026-05-01 21:10:11.402405+07	\N	2026-04-30	KELUAR	ongkos kirim - pak tono	1250000	\N	2026-05-01 21:10:11.393913+07	\N	\N	\N	\N	f
370	2026-05-01 21:10:54.5959+07	2026-05-01 21:10:54.5959+07	\N	2026-05-01	KELUAR	jasa angkutan mas arip	20000	\N	2026-05-01 21:10:54.587724+07	\N	\N	\N	\N	f
371	2026-05-01 21:13:30.873972+07	2026-05-01 21:13:30.873972+07	\N	2026-04-17	KELUAR	Listrik PLN kWh 24364.0	26534828	\N	2026-05-01 21:13:30.865684+07	\N	\N	\N	\N	f
374	2026-05-02 09:19:15.098052+07	2026-05-02 09:19:15.098052+07	2026-05-02 09:19:41.754607+07	2026-05-02	KELUAR	Pembayaran Gaji: 	405000	\N	2026-05-02 09:19:15.098027+07	\N	\N	\N	\N	f
385	2026-05-02 10:32:21.116242+07	2026-05-02 10:32:21.116242+07	2026-05-02 10:32:44.881106+07	2026-05-02	KELUAR	Pembayaran Gaji: 	300000	\N	2026-05-02 10:32:21.116224+07	\N	\N	\N	\N	f
384	2026-05-02 10:32:20.684085+07	2026-05-02 10:32:20.684085+07	2026-05-02 10:33:31.155747+07	2026-05-02	KELUAR	Pembayaran Gaji: 	315000	\N	2026-05-02 10:32:20.68407+07	\N	\N	\N	\N	f
383	2026-05-02 10:32:20.348332+07	2026-05-02 10:32:20.348332+07	2026-05-02 10:33:35.187016+07	2026-05-02	KELUAR	Pembayaran Gaji: 	345000	\N	2026-05-02 10:32:20.34831+07	\N	\N	\N	\N	f
382	2026-05-02 10:32:19.780432+07	2026-05-02 10:32:19.780432+07	2026-05-02 10:33:37.891398+07	2026-05-02	KELUAR	Pembayaran Gaji: 	360000	\N	2026-05-02 10:32:19.780412+07	\N	\N	\N	\N	f
396	2026-05-02 11:49:05.88532+07	2026-05-02 11:49:05.88532+07	2026-05-02 11:50:16.622025+07	2026-05-02	KELUAR	Pembayaran Gaji: 	315000	\N	2026-05-02 11:49:05.885305+07	\N	\N	\N	\N	f
397	2026-05-02 11:49:06.372954+07	2026-05-02 11:49:06.372954+07	2026-05-02 11:50:16.622025+07	2026-05-02	KELUAR	Pembayaran Gaji: 	315000	\N	2026-05-02 11:49:06.372934+07	\N	\N	\N	\N	f
394	2026-05-02 11:49:04.885493+07	2026-05-02 11:49:04.885493+07	2026-05-02 11:50:25.422552+07	2026-05-02	KELUAR	Pembayaran Gaji: 	450000	\N	2026-05-02 11:49:04.885475+07	\N	\N	\N	\N	f
395	2026-05-02 11:49:05.262362+07	2026-05-02 11:49:05.262362+07	2026-05-02 11:50:25.422552+07	2026-05-02	KELUAR	Pembayaran Gaji: 	450000	\N	2026-05-02 11:49:05.262348+07	\N	\N	\N	\N	f
401	2026-05-02 11:51:21.542972+07	2026-05-02 11:51:21.542972+07	2026-05-02 12:07:13.718479+07	2026-05-02	KELUAR	Pembayaran Gaji: 	105000	\N	2026-05-02 11:51:21.542956+07	\N	\N	\N	\N	f
398	2026-05-02 11:51:19.875036+07	2026-05-02 11:51:19.875036+07	2026-05-02 12:07:19.368884+07	2026-05-02	KELUAR	Pembayaran Gaji: 	157500	\N	2026-05-02 11:51:19.875017+07	\N	\N	\N	\N	f
399	2026-05-02 11:51:20.366136+07	2026-05-02 11:51:20.366136+07	2026-05-02 12:07:19.368884+07	2026-05-02	KELUAR	Pembayaran Gaji: 	157500	\N	2026-05-02 11:51:20.366118+07	\N	\N	\N	\N	f
400	2026-05-02 11:51:20.911116+07	2026-05-02 11:51:20.911116+07	2026-05-02 12:07:19.368884+07	2026-05-02	KELUAR	Pembayaran Gaji: 	157500	\N	2026-05-02 11:51:20.911101+07	\N	\N	\N	\N	f
424	2026-05-02 13:13:04.400215+07	2026-05-02 13:13:04.400215+07	2026-05-02 13:15:23.915549+07	2026-05-02	KELUAR	Pembayaran Gaji: bonus : 30k extra : 20k tgl merah : 15k	237500	\N	2026-05-02 13:13:04.4002+07	\N	\N	\N	\N	f
423	2026-05-02 13:13:04.110793+07	2026-05-02 13:13:04.110793+07	2026-05-02 13:15:26.75739+07	2026-05-02	KELUAR	Pembayaran Gaji: bonus : 30k extra : 40k tgl merah : 15k	315000	\N	2026-05-02 13:13:04.110777+07	\N	\N	\N	\N	f
422	2026-05-02 13:13:03.423802+07	2026-05-02 13:13:03.423802+07	2026-05-02 13:15:29.53616+07	2026-05-02	KELUAR	Pembayaran Gaji: bonus : 42k extra : 25k tgl merah : 15k	345000	\N	2026-05-02 13:13:03.423782+07	\N	\N	\N	\N	f
421	2026-05-02 13:13:02.924825+07	2026-05-02 13:13:02.924825+07	2026-05-02 13:15:32.10033+07	2026-05-02	KELUAR	Pembayaran Gaji: bonus : 42k extra : 100k tgl merah : 15k	450000	\N	2026-05-02 13:13:02.924216+07	\N	\N	\N	\N	f
432	2026-05-05 20:42:46.4128+07	2026-05-05 20:42:46.4128+07	\N	2026-04-28	MASUK	Pembayaran Faktur BMP-0426-003 (abah aan)	20320000	8	2026-05-05 20:42:46.412783+07	\N	\N	\N	\N	f
245	2026-04-29 15:24:03.137505+07	2026-04-29 15:24:03.137505+07	\N	2026-04-28	MASUK	Pembayaran Borongan Faktur BMP-0426-048 (abah kosi'in)	40790000	\N	2026-04-29 15:24:02.846893+07	\N	\N	\N	\N	f
247	2026-04-29 15:24:06.123219+07	2026-04-29 15:24:06.123219+07	\N	2026-04-27	KELUAR	mesin fingerprint, LAN, Router SmartFren	3000000	\N	2026-04-29 15:24:05.512113+07	\N	\N	\N	\N	f
248	2026-04-29 15:24:07.355741+07	2026-04-29 15:24:07.355741+07	\N	2026-04-27	MASUK	Pembayaran Faktur BMP-0426-002 (abah ali)	33200000	\N	2026-04-29 15:24:06.737607+07	\N	\N	\N	\N	f
249	2026-04-29 15:24:08.639626+07	2026-04-29 15:24:08.639626+07	\N	2026-04-27	MASUK	Pembayaran Faktur BMP-0426-054 (Mas Malvin)	20000000	\N	2026-04-29 15:24:08.068704+07	\N	\N	\N	\N	f
250	2026-04-29 15:24:10.01433+07	2026-04-29 15:24:10.01433+07	\N	2026-04-25	KELUAR	jasa angkutan - mas adi	70000	\N	2026-04-29 15:24:09.40001+07	\N	\N	\N	\N	f
251	2026-04-29 15:24:11.447851+07	2026-04-29 15:24:11.447851+07	\N	2026-04-24	KELUAR	jasa angkut - mas adit	50000	\N	2026-04-29 15:24:10.654281+07	\N	\N	\N	\N	f
252	2026-04-29 15:24:12.791446+07	2026-04-29 15:24:12.791446+07	\N	2026-04-24	MASUK	Pembayaran Borongan Faktur BMP-0426-010 (mas wiranto)	200000	\N	2026-04-29 15:24:12.062317+07	\N	\N	\N	\N	f
253	2026-04-29 15:24:14.315194+07	2026-04-29 15:24:14.315194+07	\N	2026-04-24	MASUK	Pembayaran Borongan Faktur BMP-0426-006 (mas wiranto)	9655000	\N	2026-04-29 15:24:13.598292+07	\N	\N	\N	\N	f
254	2026-04-29 15:24:15.749357+07	2026-04-29 15:24:15.749357+07	\N	2026-04-24	MASUK	Pembayaran Borongan Faktur BMP-0426-005 (mas wiranto)	6395000	\N	2026-04-29 15:24:14.938722+07	\N	\N	\N	\N	f
255	2026-04-29 15:24:17.090663+07	2026-04-29 15:24:17.090663+07	\N	2026-04-24	MASUK	Pelunasan Faktur BMP-0426-052 (mas wiranto)	6150000	\N	2026-04-29 15:24:16.363171+07	\N	\N	\N	\N	f
256	2026-04-29 15:24:18.513593+07	2026-04-29 15:24:18.513593+07	\N	2026-04-23	MASUK	Pembayaran Borongan Faktur BMP-0426-048 (abah kosi'in)	30000000	\N	2026-04-29 15:24:17.902347+07	\N	\N	\N	\N	f
257	2026-04-29 15:24:19.844855+07	2026-04-29 15:24:19.844855+07	\N	2026-04-22	KELUAR	jasa angkut - mas adhi	50000	\N	2026-04-29 15:24:19.221935+07	\N	\N	\N	\N	f
258	2026-04-29 15:24:21.176563+07	2026-04-29 15:24:21.176563+07	\N	2026-04-22	KELUAR	ongkos kirim uang - Pak Tono	200000	\N	2026-04-29 15:24:20.414314+07	\N	\N	\N	\N	f
259	2026-04-29 15:24:22.568264+07	2026-04-29 15:24:22.568264+07	\N	2026-04-21	KELUAR	jasa pengiriman pak tono - Grobogan Pak Kosi'in	1630000	\N	2026-04-29 15:24:21.79063+07	\N	\N	\N	\N	f
260	2026-04-29 15:24:23.941029+07	2026-04-29 15:24:23.941029+07	\N	2026-04-21	KELUAR	jasa angkut - mas adhi	75000	\N	2026-04-29 15:24:23.326476+07	\N	\N	\N	\N	f
261	2026-04-29 15:24:25.272112+07	2026-04-29 15:24:25.272112+07	\N	2026-04-21	KELUAR	uang mesin neng tin	3400000	\N	2026-04-29 15:24:24.555363+07	\N	\N	\N	\N	f
262	2026-04-29 15:24:26.500968+07	2026-04-29 15:24:26.500968+07	\N	2026-04-21	KELUAR	jasa angkut - pak jito & pak sul (ALI)	300000	\N	2026-04-29 15:24:25.886336+07	\N	\N	\N	\N	f
263	2026-04-29 15:24:27.729695+07	2026-04-29 15:24:27.729695+07	\N	2026-04-20	MASUK	Pelunasan Faktur BMP-0426-039 (Mas Arylah)	8731000	\N	2026-04-29 15:24:27.066769+07	\N	\N	\N	\N	f
264	2026-04-29 15:24:28.922738+07	2026-04-29 15:24:28.922738+07	\N	2026-04-20	MASUK	Pelunasan Faktur BMP-0426-040 (mas wiranto)	4860000	\N	2026-04-29 15:24:28.344095+07	\N	\N	\N	\N	f
265	2026-04-29 15:24:30.39219+07	2026-04-29 15:24:30.39219+07	\N	2026-04-20	MASUK	Pelunasan Faktur BMP-0426-041 (Mas Eko Cahyono)	28755000	\N	2026-04-29 15:24:29.777688+07	\N	\N	\N	\N	f
266	2026-04-29 15:24:32.435265+07	2026-04-29 15:24:32.435265+07	\N	2026-04-20	MASUK	Pelunasan Faktur BMP-0426-042 (Linda Abadi)	28000000	\N	2026-04-29 15:24:31.411732+07	\N	\N	\N	\N	f
267	2026-04-29 15:24:33.87389+07	2026-04-29 15:24:33.87389+07	\N	2026-04-20	MASUK	Pelunasan Faktur BMP-0426-026 (mas kolis)	2825000	\N	2026-04-29 15:24:33.25944+07	\N	\N	\N	\N	f
268	2026-04-29 15:24:35.206697+07	2026-04-29 15:24:35.206697+07	\N	2026-04-20	MASUK	Pelunasan Faktur BMP-0426-044 (Pak Huda)	22160000	\N	2026-04-29 15:24:34.543653+07	\N	\N	\N	\N	f
269	2026-04-29 15:24:36.610803+07	2026-04-29 15:24:36.610803+07	\N	2026-04-20	MASUK	Pelunasan Faktur BMP-0426-043 (Umik Erna)	12000000	\N	2026-04-29 15:24:35.922209+07	\N	\N	\N	\N	f
270	2026-04-29 15:24:38.072251+07	2026-04-29 15:24:38.072251+07	\N	2026-04-20	KELUAR	jasa angkut - Pak Katiran	70000	\N	2026-04-29 15:24:37.457829+07	\N	\N	\N	\N	f
271	2026-04-29 15:24:39.6082+07	2026-04-29 15:24:39.6082+07	\N	2026-04-20	MASUK	Pelunasan Faktur BMP-0426-047 (pak katiran)	13805000	\N	2026-04-29 15:24:38.764426+07	\N	\N	\N	\N	f
434	2026-05-05 20:45:28.715909+07	2026-05-05 20:45:28.715909+07	\N	2026-05-05	MASUK	Pembayaran Faktur BMP-0426-024 (mas zahid)	10700000	10	2026-05-05 20:45:28.715898+07	\N	\N	\N	\N	f
435	2026-05-05 20:47:32.660696+07	2026-05-05 20:47:32.660696+07	2026-05-05 22:50:02.357748+07	2026-05-05	MASUK	Pembayaran Faktur BMP-2605-003 (Mas Malvin)	10700000	11	2026-05-05 20:47:32.66067+07	\N	\N	\N	\N	f
246	2026-04-29 15:24:04.894226+07	2026-04-29 15:24:04.894226+07	2026-05-06 00:24:54.502465+07	2026-04-28	MASUK	Pembayaran Faktur BMP-0426-003 (abah aan)	20320000	\N	2026-04-29 15:24:04.17785+07	\N	\N	\N	\N	f
441	2026-05-14 08:44:22.68616+07	2026-05-14 08:44:22.68616+07	2026-05-14 08:44:57.200134+07	2026-05-14	KELUAR	Pembelian barang khusus (Update) untuk Faktur BMP-2605-005	2600	\N	2026-05-14 08:44:22.686146+07	\N	\N	\N	\N	f
442	2026-05-14 08:52:21.031201+07	2026-05-14 08:52:21.031201+07	\N	2026-05-14	KELUAR	Pembelian barang khusus untuk Faktur BMP-2605-008	6760000	\N	2026-05-14 08:52:21.031184+07	\N	\N	\N	\N	f
458	2026-05-23 11:10:40.138872+07	2026-05-23 11:10:40.138872+07	2026-05-26 08:57:24.632659+07	2026-05-23	MASUK	Pembayaran Faktur BMP-2605-013 (Mas Malvin)	30000000	20	2026-05-23 11:10:40.138862+07	\N	\N	\N	\N	f
443	2026-05-14 08:52:55.590342+07	2026-05-14 08:52:55.590342+07	\N	2026-05-01	MASUK	Pembayaran Faktur BMP-2605-008 (mas wiranto)	10000000	13	2026-05-14 08:52:55.590328+07	\N	\N	\N	\N	f
446	2026-05-15 15:29:57.43274+07	2026-05-15 15:29:57.43274+07	\N	2026-05-15	MASUK	Pembayaran Faktur BMP-2605-010 (abah kosi'in)	80393000	16	2026-05-15 15:29:57.432722+07	\N	\N	\N	\N	f
457	2026-05-23 11:08:40.597845+07	2026-05-23 11:08:40.597845+07	\N	2026-05-19	MASUK	Pembayaran Faktur BMP-2605-013 (Mas Malvin)	14000000	19	2026-05-23 11:08:40.597829+07	\N	\N	\N	\N	f
463	2026-05-26 12:34:42.71438+07	2026-05-26 12:34:42.71438+07	2026-05-26 12:35:20.639452+07	2026-05-26	KELUAR	Pembelian barang khusus (Update) untuk Faktur BMP-2605-010	3700	\N	2026-05-26 12:34:42.714354+07	\N	\N	\N	\N	f
272	2026-04-29 15:24:40.922881+07	2026-04-29 15:24:40.922881+07	\N	2026-04-20	MASUK	Pembayaran Faktur BMP-0426-025 (pak katiran)	17385000	\N	2026-04-29 15:24:40.325491+07	\N	\N	\N	\N	f
273	2026-04-29 15:24:42.292558+07	2026-04-29 15:24:42.292558+07	\N	2026-04-20	KELUAR	jasa angkut - mas adhi	50000	\N	2026-04-29 15:24:41.769345+07	\N	\N	\N	\N	f
274	2026-04-29 15:24:43.499638+07	2026-04-29 15:24:43.499638+07	\N	2026-04-20	KELUAR	Gaji Mas Dedi	5000000	\N	2026-04-29 15:24:42.885128+07	\N	\N	\N	\N	f
275	2026-04-29 15:24:44.830704+07	2026-04-29 15:24:44.830704+07	\N	2026-04-20	KELUAR	ongkir Pak Katiran	520000	\N	2026-04-29 15:24:44.173651+07	\N	\N	\N	\N	f
276	2026-04-29 15:24:46.33061+07	2026-04-29 15:24:46.33061+07	\N	2026-04-20	MASUK	Pelunasan Faktur BMP-0426-029 (mas zahid)	6015000	\N	2026-04-29 15:24:45.64969+07	\N	\N	\N	\N	f
277	2026-04-29 15:24:47.666683+07	2026-04-29 15:24:47.666683+07	\N	2026-04-20	MASUK	Pelunasan Faktur BMP-0426-028 (mas kolis)	780000	\N	2026-04-29 15:24:47.083511+07	\N	\N	\N	\N	f
278	2026-04-29 15:24:49.1315+07	2026-04-29 15:24:49.1315+07	\N	2026-04-20	MASUK	Pelunasan Faktur BMP-0426-046 (Mas Malvin)	44575000	\N	2026-04-29 15:24:48.312393+07	\N	\N	\N	\N	f
279	2026-04-29 15:24:50.546935+07	2026-04-29 15:24:50.546935+07	\N	2026-04-20	MASUK	Pelunasan Faktur BMP-0426-027 (mas kolis)	6850000	\N	2026-04-29 15:24:49.848499+07	\N	\N	\N	\N	f
280	2026-04-29 15:24:51.998864+07	2026-04-29 15:24:51.998864+07	\N	2026-04-20	MASUK	Pelunasan Faktur BMP-0426-045 (abah kosi'in)	51248000	\N	2026-04-29 15:24:51.282017+07	\N	\N	\N	\N	f
281	2026-04-29 15:24:53.433293+07	2026-04-29 15:24:53.433293+07	\N	2026-04-20	MASUK	Pembayaran Faktur BMP-0426-023 (mas kolis)	30000000	\N	2026-04-29 15:24:52.694449+07	\N	\N	\N	\N	f
282	2026-04-29 15:24:54.858602+07	2026-04-29 15:24:54.858602+07	\N	2026-04-20	MASUK	Pembayaran Faktur BMP-0426-009 (mas zahid)	13582500	\N	2026-04-29 15:24:54.149305+07	\N	\N	\N	\N	f
283	2026-04-29 15:24:56.299575+07	2026-04-29 15:24:56.299575+07	\N	2026-04-20	MASUK	Pembayaran Faktur BMP-0426-034 (mas wiranto)	16040000	\N	2026-04-29 15:24:55.685227+07	\N	\N	\N	\N	f
284	2026-04-29 15:24:57.733298+07	2026-04-29 15:24:57.733298+07	\N	2026-04-20	MASUK	Pelunasan Faktur BMP-0426-030 (mas kolis)	9882500	\N	2026-04-29 15:24:57.002621+07	\N	\N	\N	\N	f
285	2026-04-29 15:24:59.122535+07	2026-04-29 15:24:59.122535+07	\N	2026-04-20	MASUK	Pelunasan Faktur BMP-0426-031 (mas kolis)	15337500	\N	2026-04-29 15:24:58.450942+07	\N	\N	\N	\N	f
286	2026-04-29 15:25:00.600667+07	2026-04-29 15:25:00.600667+07	\N	2026-04-20	MASUK	Pelunasan Faktur BMP-0426-032 (mas kolis)	10325000	\N	2026-04-29 15:24:59.884132+07	\N	\N	\N	\N	f
287	2026-04-29 15:25:01.93196+07	2026-04-29 15:25:01.93196+07	\N	2026-04-20	MASUK	Pelunasan Faktur BMP-0426-033 (mas kolis)	15337500	\N	2026-04-29 15:25:01.27877+07	\N	\N	\N	\N	f
288	2026-04-29 15:25:03.263629+07	2026-04-29 15:25:03.263629+07	\N	2026-04-20	MASUK	Pelunasan Faktur BMP-0426-022 (mas wiranto)	290000	\N	2026-04-29 15:25:02.545966+07	\N	\N	\N	\N	f
289	2026-04-29 15:25:04.54269+07	2026-04-29 15:25:04.54269+07	\N	2026-04-20	MASUK	Pelunasan Faktur BMP-0426-035 (mas wiranto)	5984000	\N	2026-04-29 15:25:03.877319+07	\N	\N	\N	\N	f
290	2026-04-29 15:25:05.925267+07	2026-04-29 15:25:05.925267+07	\N	2026-04-20	MASUK	Pelunasan Faktur BMP-0426-036 (mas wiranto)	10211500	\N	2026-04-29 15:25:05.208614+07	\N	\N	\N	\N	f
291	2026-04-29 15:25:07.359194+07	2026-04-29 15:25:07.359194+07	\N	2026-04-20	MASUK	Pelunasan Faktur BMP-0426-037 (mas wiranto)	9182000	\N	2026-04-29 15:25:06.637681+07	\N	\N	\N	\N	f
292	2026-04-29 15:25:08.690342+07	2026-04-29 15:25:08.690342+07	\N	2026-04-20	MASUK	Pelunasan Faktur BMP-0426-038 (mas wiranto)	2940000	\N	2026-04-29 15:25:08.075746+07	\N	\N	\N	\N	f
293	2026-04-29 15:25:09.882573+07	2026-04-29 15:25:09.882573+07	\N	2026-04-19	KELUAR	uang mesin ibu	5000000	\N	2026-04-29 15:25:09.30466+07	\N	\N	\N	\N	f
294	2026-04-29 15:25:11.352512+07	2026-04-29 15:25:11.352512+07	\N	2026-04-17	KELUAR	beli inventaris kantor	8075000	\N	2026-04-29 15:25:10.738093+07	\N	\N	\N	\N	f
295	2026-04-29 15:25:12.888558+07	2026-04-29 15:25:12.888558+07	\N	2026-04-17	KELUAR	beli Kartu Smartfren	70000	\N	2026-04-29 15:25:12.042669+07	\N	\N	\N	\N	f
296	2026-04-29 15:25:14.19551+07	2026-04-29 15:25:14.19551+07	\N	2026-04-17	KELUAR	jasa angkut - mas adi	50000	\N	2026-04-29 15:25:13.502978+07	\N	\N	\N	\N	f
297	2026-04-29 15:25:15.653522+07	2026-04-29 15:25:15.653522+07	\N	2026-04-17	KELUAR	beli website	2000000	\N	2026-04-29 15:25:15.039189+07	\N	\N	\N	\N	f
298	2026-04-29 15:25:16.782646+07	2026-04-29 15:25:16.782646+07	\N	2026-04-17	MASUK	Pelunasan Faktur BMP-0426-017 (mas kolis)	14500000	\N	2026-04-29 15:25:16.22156+07	\N	\N	\N	\N	f
299	2026-04-29 15:25:18.008727+07	2026-04-29 15:25:18.008727+07	\N	2026-04-17	MASUK	Pelunasan Faktur BMP-0426-012 (mas kolis)	4125000	\N	2026-04-29 15:25:17.389584+07	\N	\N	\N	\N	f
300	2026-04-29 15:25:19.241969+07	2026-04-29 15:25:19.241969+07	\N	2026-04-17	MASUK	Pelunasan Faktur BMP-0426-018 (abah ali)	16520000	\N	2026-04-29 15:25:18.584717+07	\N	\N	\N	\N	f
301	2026-04-29 15:25:20.622587+07	2026-04-29 15:25:20.622587+07	\N	2026-04-17	MASUK	Pelunasan Faktur BMP-0426-019 (abah ali)	30600000	\N	2026-04-29 15:25:19.954693+07	\N	\N	\N	\N	f
302	2026-04-29 15:25:22.002301+07	2026-04-29 15:25:22.002301+07	\N	2026-04-17	MASUK	Pelunasan Faktur BMP-0426-020 (abah ali)	6560000	\N	2026-04-29 15:25:21.285486+07	\N	\N	\N	\N	f
303	2026-04-29 15:25:23.538643+07	2026-04-29 15:25:23.538643+07	\N	2026-04-17	MASUK	Pelunasan Faktur BMP-0426-021 (mas kolis)	13200000	\N	2026-04-29 15:25:22.719241+07	\N	\N	\N	\N	f
304	2026-04-29 15:25:24.887428+07	2026-04-29 15:25:24.887428+07	\N	2026-04-17	MASUK	Pelunasan Faktur BMP-0426-022 (mas wiranto)	12400000	\N	2026-04-29 15:25:24.255209+07	\N	\N	\N	\N	f
305	2026-04-29 15:25:26.405562+07	2026-04-29 15:25:26.405562+07	\N	2026-04-17	KELUAR	[FAKTUR BELI] Bengkel Sahabat - Kusmiantoro | Pembuatan Matras Baskom Rotan 14 (1,0Lsn x 1,0Qty), Pembuatan Matras Baskom Bahtera (1,0Lsn x 1,0Qty)	61000000	\N	2026-04-29 15:25:25.688716+07	\N	\N	\N	\N	f
306	2026-04-29 15:25:27.839497+07	2026-04-29 15:25:27.839497+07	\N	2026-04-17	KELUAR	[FAKTUR BELI] Bengkel Sahabat - Kusmiantoro | Krom Matras 2X (1,0Lsn x 1,0Qty)	4250000	\N	2026-04-29 15:25:27.024069+07	\N	\N	\N	\N	f
307	2026-04-29 15:25:29.222587+07	2026-04-29 15:25:29.222587+07	\N	2026-04-17	KELUAR	[FAKTUR BELI] Bengkel Sahabat - Kusmiantoro | servis Matras Bahtera (1,0Lsn x 1,0Qty)	2500000	\N	2026-04-29 15:25:28.555987+07	\N	\N	\N	\N	f
308	2026-04-29 15:25:30.604013+07	2026-04-29 15:25:30.604013+07	\N	2026-04-17	KELUAR	[FAKTUR BELI] Bengkel Sahabat - Kusmiantoro | Servis Matras & Krom (1,0Lsn x 1,0Qty)	5400000	\N	2026-04-29 15:25:29.989994+07	\N	\N	\N	\N	f
309	2026-04-29 15:25:32.140084+07	2026-04-29 15:25:32.140084+07	\N	2026-04-17	KELUAR	[FAKTUR BELI] Bengkel Sahabat - Kusmiantoro | servis Matras Baskom Panda (1,0Lsn x 1,0Qty)	2500000	\N	2026-04-29 15:25:31.338648+07	\N	\N	\N	\N	f
310	2026-04-29 15:25:33.490635+07	2026-04-29 15:25:33.490635+07	\N	2026-04-17	KELUAR	[FAKTUR BELI] Bengkel Sahabat - Kusmiantoro | Servis Matras (1,0Lsn x 1,0Qty)	2500000	\N	2026-04-29 15:25:32.755697+07	\N	\N	\N	\N	f
311	2026-04-29 15:25:34.802263+07	2026-04-29 15:25:34.802263+07	\N	2026-04-17	KELUAR	[FAKTUR BELI] Bengkel Sahabat - Kusmiantoro | krom Matras 2X (1,0Lsn x 1,0Qty)	3000000	\N	2026-04-29 15:25:34.188346+07	\N	\N	\N	\N	f
312	2026-04-29 15:25:36.236039+07	2026-04-29 15:25:36.236039+07	\N	2026-04-17	KELUAR	[FAKTUR BELI] Pak Hadi CNC | CNC Ulang Baskom Mawar (1,0Lsn x 1,0Qty)	8000000	\N	2026-04-29 15:25:35.519529+07	\N	\N	\N	\N	f
313	2026-04-29 15:25:37.669566+07	2026-04-29 15:25:37.669566+07	\N	2026-04-17	KELUAR	[FAKTUR BELI] Hwamda | Angsuran Mesin Hwamda (1,0Lsn x 1,0Qty)	20000000	\N	2026-04-29 15:25:36.952806+07	\N	\N	\N	\N	f
314	2026-04-29 15:25:39.103242+07	2026-04-29 15:25:39.103242+07	\N	2026-04-17	KELUAR	[FAKTUR BELI] Hwamda | Angsuran Mesin Hwamda (1,0Lsn x 1,0Qty)	20000000	\N	2026-04-29 15:25:38.488937+07	\N	\N	\N	\N	f
315	2026-04-29 15:25:40.639424+07	2026-04-29 15:25:40.639424+07	\N	2026-04-17	KELUAR	[FAKTUR BELI] Hwamda | Angsuran Mesin Hwamda (1,0Lsn x 1,0Qty)	20000000	\N	2026-04-29 15:25:39.820229+07	\N	\N	\N	\N	f
316	2026-04-29 15:25:42.042543+07	2026-04-29 15:25:42.042543+07	\N	2026-04-17	KELUAR	[FAKTUR BELI] Listrik PLN | kWh 19269.0 (1,0Lsn x 1,0Qty)	20858656	\N	2026-04-29 15:25:41.356413+07	\N	\N	\N	\N	f
317	2026-04-29 15:25:43.506451+07	2026-04-29 15:25:43.506451+07	\N	2026-04-17	KELUAR	Tiner 8 Liter	200000	\N	2026-04-29 15:25:42.892098+07	\N	\N	\N	\N	f
318	2026-04-29 15:25:44.940183+07	2026-04-29 15:25:44.940183+07	\N	2026-04-16	MASUK	Pelunasan Faktur BMP-0426-015 (mas wiranto)	3100000	\N	2026-04-29 15:25:44.193663+07	\N	\N	\N	\N	f
319	2026-04-29 15:25:46.332622+07	2026-04-29 15:25:46.332622+07	\N	2026-04-16	MASUK	Pelunasan Faktur BMP-0426-014 (mas wiranto)	5490000	\N	2026-04-29 15:25:45.659567+07	\N	\N	\N	\N	f
320	2026-04-29 15:25:47.704939+07	2026-04-29 15:25:47.704939+07	\N	2026-04-16	KELUAR	jasa angkut - mas adhi	50000	\N	2026-04-29 15:25:47.090639+07	\N	\N	\N	\N	f
321	2026-04-29 15:25:48.933775+07	2026-04-29 15:25:48.933775+07	\N	2026-04-16	KELUAR	Cleo Gelas 2 Box, kmbli 2K ksih ke orng krja	50000	\N	2026-04-29 15:25:48.321957+07	\N	\N	\N	\N	f
322	2026-04-29 15:25:50.367411+07	2026-04-29 15:25:50.367411+07	\N	2026-04-16	MASUK	Pelunasan Faktur BMP-0426-016 (ko hary)	14400000	\N	2026-04-29 15:25:49.61356+07	\N	\N	\N	\N	f
323	2026-04-29 15:25:51.596203+07	2026-04-29 15:25:51.596203+07	\N	2026-04-15	KELUAR	Bayar Server Railway	100000	\N	2026-04-29 15:25:50.981856+07	\N	\N	\N	\N	f
324	2026-04-29 15:25:53.132496+07	2026-04-29 15:25:53.132496+07	\N	2026-04-15	KELUAR	jasa angkut - mas adhi	50000	\N	2026-04-29 15:25:52.313327+07	\N	\N	\N	\N	f
325	2026-04-29 15:25:54.463389+07	2026-04-29 15:25:54.463389+07	\N	2026-04-15	MASUK	Pembayaran Faktur BMP-0426-004 (mas wiranto)	9012000	\N	2026-04-29 15:25:53.787334+07	\N	\N	\N	\N	f
326	2026-04-29 15:25:55.962599+07	2026-04-29 15:25:55.962599+07	\N	2026-04-15	MASUK	Pelunasan Faktur BMP-0426-013 (abah kosi'in)	52350000	\N	2026-04-29 15:25:55.282611+07	\N	\N	\N	\N	f
327	2026-04-29 15:25:57.433167+07	2026-04-29 15:25:57.433167+07	\N	2026-04-14	KELUAR	[FAKTUR BELI] Pak Kasnar | Wakul Telur (20,0Lsn x 50,0Qty), Wakul Telur Kotak (20,0Lsn x 100,0Qty), Karung Putih (1,0Lsn x 1,0Qty), Karung Kuning (1,0Lsn x 1,0Qty)	16400000	\N	2026-04-29 15:25:56.722408+07	\N	\N	\N	\N	f
328	2026-04-29 15:25:58.969198+07	2026-04-29 15:25:58.969198+07	\N	2026-04-14	MASUK	Pembayaran Faktur BMP-0426-008 (Linda Abadi)	52270000	\N	2026-04-29 15:25:58.130786+07	\N	\N	\N	\N	f
329	2026-04-29 15:26:00.272977+07	2026-04-29 15:26:00.272977+07	\N	2026-04-14	KELUAR	jasa angkut - mas adhi	50000	\N	2026-04-29 15:25:59.583518+07	\N	\N	\N	\N	f
330	2026-04-29 15:26:01.733997+07	2026-04-29 15:26:01.733997+07	\N	2026-04-14	KELUAR	jasa pengiriman pak tono - Blora & Bojonegoro	1750000	\N	2026-04-29 15:26:01.017414+07	\N	\N	\N	\N	f
331	2026-04-29 15:26:02.962687+07	2026-04-29 15:26:02.962687+07	\N	2026-04-13	KELUAR	jasa angkut - mas adi	50000	\N	2026-04-29 15:26:02.348386+07	\N	\N	\N	\N	f
436	2026-05-05 22:47:12.600732+07	2026-05-05 22:47:12.600732+07	\N	2026-04-23	KELUAR	beli kambing	3800000	\N	2026-05-05 22:47:12.591105+07	\N	\N	\N	\N	f
332	2026-04-29 15:26:04.194003+07	2026-04-29 15:26:04.194003+07	\N	2026-04-13	KELUAR	Uang Konsumsi Karyawan	300000	\N	2026-04-29 15:26:03.524607+07	\N	\N	\N	\N	f
333	2026-04-29 15:26:05.421813+07	2026-04-29 15:26:05.421813+07	\N	2026-04-13	KELUAR	jasa angkut - pak jito & pak sul (ALI)	260000	\N	2026-04-29 15:26:04.806359+07	\N	\N	\N	\N	f
334	2026-04-29 15:26:06.644455+07	2026-04-29 15:26:06.644455+07	\N	2026-04-10	KELUAR	[FAKTUR BELI] Bengkel Sahabat - Kusmiantoro | DP Matras Piring "8" (1,0Lsn x 1,0Qty)	8000000	\N	2026-04-29 15:26:06.036545+07	\N	\N	\N	\N	f
335	2026-04-29 15:26:07.835598+07	2026-04-29 15:26:07.835598+07	\N	2026-04-04	KELUAR	jasa pengiriman pak tono	1000000	\N	2026-04-29 15:26:07.263666+07	\N	\N	\N	\N	f
336	2026-04-29 15:26:09.106922+07	2026-04-29 15:26:09.106922+07	\N	2026-04-04	KELUAR	jasa angkut - pak jito	300000	\N	2026-04-29 15:26:08.492722+07	\N	\N	\N	\N	f
337	2026-04-29 15:26:10.335594+07	2026-04-29 15:26:10.335594+07	\N	2026-04-03	KELUAR	jasa pengiriman - arip (wiranto)	15000	\N	2026-04-29 15:26:09.721222+07	\N	\N	\N	\N	f
338	2026-04-29 15:26:11.57466+07	2026-04-29 15:26:11.57466+07	\N	2026-04-02	KELUAR	jasa angkut - arip (wiranto)	15000	\N	2026-04-29 15:26:10.928795+07	\N	\N	\N	\N	f
339	2026-04-29 15:26:12.793994+07	2026-04-29 15:26:12.793994+07	\N	2026-03-20	KELUAR	uang mesin ibu	5000000	\N	2026-04-29 15:26:12.175149+07	\N	\N	\N	\N	f
340	2026-04-29 15:26:14.332889+07	2026-04-29 15:26:14.332889+07	\N	2026-03-20	KELUAR	Gaji Mas Dedi	5000000	\N	2026-04-29 15:26:13.715298+07	\N	\N	\N	\N	f
342	2026-04-29 15:26:16.889824+07	2026-04-29 15:26:16.889824+07	\N	2026-03-09	KELUAR	jasa angkut - arip	15000	\N	2026-04-29 15:26:16.172852+07	\N	\N	\N	\N	f
343	2026-04-29 15:26:18.118011+07	2026-04-29 15:26:18.118011+07	\N	2026-03-05	KELUAR	jasa angkut - pak jito	300000	\N	2026-04-29 15:26:17.504398+07	\N	\N	\N	\N	f
344	2026-04-29 15:26:19.449328+07	2026-04-29 15:26:19.449328+07	\N	2026-03-05	KELUAR	jasa pengiriman pak tono	1000000	\N	2026-04-29 15:26:18.803642+07	\N	\N	\N	\N	f
345	2026-04-29 15:26:20.859598+07	2026-04-29 15:26:20.859598+07	\N	2026-02-28	KELUAR	jasa pengiriman mas hendrik	2000000	\N	2026-04-29 15:26:20.066457+07	\N	\N	\N	\N	f
346	2026-04-29 15:26:22.316526+07	2026-04-29 15:26:22.316526+07	\N	2026-02-28	KELUAR	jasa pengiriman pak tono	1000000	\N	2026-04-29 15:26:21.599725+07	\N	\N	\N	\N	f
347	2026-04-29 15:26:23.852489+07	2026-04-29 15:26:23.852489+07	\N	2026-02-28	KELUAR	jasa angkut - pak jito	300000	\N	2026-04-29 15:26:23.018681+07	\N	\N	\N	\N	f
348	2026-04-29 15:26:25.245638+07	2026-04-29 15:26:25.245638+07	\N	2026-02-26	KELUAR	jasa angkut - pak jito	300000	\N	2026-04-29 15:26:24.570881+07	\N	\N	\N	\N	f
349	2026-04-29 15:26:26.618241+07	2026-04-29 15:26:26.618241+07	\N	2026-02-26	KELUAR	jasa pengiriman pak tono	1000000	\N	2026-04-29 15:26:26.00312+07	\N	\N	\N	\N	f
350	2026-04-29 15:26:28.051053+07	2026-04-29 15:26:28.051053+07	\N	2026-02-20	KELUAR	Gaji Mas Dedi	5000000	\N	2026-04-29 15:26:27.303052+07	\N	\N	\N	\N	f
351	2026-04-29 15:26:29.452725+07	2026-04-29 15:26:29.452725+07	\N	2026-02-20	KELUAR	uang mesin ibu	5000000	\N	2026-04-29 15:26:28.767806+07	\N	\N	\N	\N	f
352	2026-04-29 15:26:30.670596+07	2026-04-29 15:26:30.670596+07	\N	2026-02-17	KELUAR	[FAKTUR BELI] Listrik PLN | Listrik PLN (1,0Lsn x 1,0Qty)	28246157	\N	2026-04-29 15:26:30.099201+07	\N	\N	\N	\N	f
341	2026-04-29 15:26:15.509639+07	2026-04-29 15:26:15.509639+07	2026-04-30 11:39:07.560446+07	2026-03-17	KELUAR	[FAKTUR BELI] Listrik PLN | kWh 24364.0 (1,0Lsn x 1,0Qty)	26534828	\N	2026-04-29 15:26:14.946584+07	\N	\N	\N	\N	f
361	2026-04-30 14:44:15.056745+07	2026-04-30 14:44:15.056745+07	2026-04-30 14:44:25.953725+07	2026-04-30	MASUK	tes	1000000	\N	2026-04-30 14:44:15.049075+07	\N	\N	\N	\N	f
362	2026-04-30 14:44:39.576771+07	2026-04-30 14:44:39.576771+07	2026-04-30 14:44:47.089357+07	2026-04-30	KELUAR	tes	1000000	\N	2026-04-30 14:44:39.568787+07	\N	\N	\N	\N	f
364	2026-04-30 14:46:15.737683+07	2026-04-30 14:46:15.737683+07	2026-04-30 14:48:20.594042+07	2026-04-30	KELUAR	listrik	1000000	\N	2026-04-30 14:46:15.729209+07	\N	\N	\N	\N	f
365	2026-04-30 14:46:32.407465+07	2026-04-30 14:46:32.407465+07	2026-04-30 14:48:26.176597+07	2026-04-30	KELUAR	listrik	28246157	\N	2026-04-30 14:46:32.398768+07	\N	\N	\N	\N	f
360	2026-04-30 14:25:44.818304+07	2026-04-30 14:25:44.818304+07	2026-05-01 14:28:14.48964+07	2026-02-17	KELUAR	Bayar Listrik 	28246157	\N	2026-04-30 14:25:44.810686+07	\N	\N	\N	\N	f
363	2026-04-30 14:45:13.076561+07	2026-04-30 14:45:13.076561+07	2026-05-01 14:28:47.013207+07	2026-02-17	KELUAR	listrik	28246157	\N	2026-04-30 14:45:13.068658+07	\N	\N	\N	\N	f
366	2026-04-30 14:49:32.581723+07	2026-04-30 14:49:32.581723+07	2026-05-01 21:11:46.394652+07	2026-02-27	KELUAR	ok	100000	\N	2026-04-30 14:49:32.574355+07	\N	\N	\N	\N	f
372	2026-05-01 23:11:52.602345+07	2026-05-01 23:11:52.602345+07	2026-05-01 23:12:18.787862+07	2026-05-01	KELUAR	Pembayaran Gaji: 	60000	\N	2026-05-01 23:11:52.602311+07	\N	\N	\N	\N	f
375	2026-05-02 09:54:27.623938+07	2026-05-02 09:54:27.623938+07	2026-05-02 09:54:56.897892+07	2026-05-02	KELUAR	Pembayaran Gaji: 	285000	\N	2026-05-02 09:54:27.623919+07	\N	\N	\N	\N	f
376	2026-05-02 09:55:45.392985+07	2026-05-02 09:55:45.392985+07	2026-05-02 09:57:21.455379+07	2026-05-02	KELUAR	Pembayaran Gaji: 	405000	\N	2026-05-02 09:55:45.392969+07	\N	\N	\N	\N	f
389	2026-05-02 10:34:46.902017+07	2026-05-02 10:34:46.902017+07	2026-05-02 10:35:20.109715+07	2026-05-02	KELUAR	Pembayaran Gaji: 	300000	\N	2026-05-02 10:34:46.901998+07	\N	\N	\N	\N	f
388	2026-05-02 10:34:46.292063+07	2026-05-02 10:34:46.292063+07	2026-05-02 10:35:23.121455+07	2026-05-02	KELUAR	Pembayaran Gaji: 	315000	\N	2026-05-02 10:34:46.292045+07	\N	\N	\N	\N	f
387	2026-05-02 10:34:45.804191+07	2026-05-02 10:34:45.804191+07	2026-05-02 10:35:25.588573+07	2026-05-02	KELUAR	Pembayaran Gaji: 	345000	\N	2026-05-02 10:34:45.804175+07	\N	\N	\N	\N	f
386	2026-05-02 10:34:45.126995+07	2026-05-02 10:34:45.126995+07	2026-05-02 10:35:28.512024+07	2026-05-02	KELUAR	Pembayaran Gaji: 	360000	\N	2026-05-02 10:34:45.126982+07	\N	\N	\N	\N	f
406	2026-05-02 12:14:01.806502+07	2026-05-02 12:14:01.806502+07	2026-05-02 12:38:50.921704+07	2026-05-02	MASUK	{"error":"template: payroll-pdf.html:97:21: executing \\"payroll-pdf.html\\" at \\u003c.HariKerja\\u003e: can't evaluate field HariKerja in type models.Payroll"}	1	\N	2026-05-02 12:14:01.800581+07	\N	\N	\N	\N	f
404	2026-05-02 12:12:59.453199+07	2026-05-02 12:12:59.453199+07	2026-05-02 12:51:59.534706+07	2026-05-02	KELUAR	Pembayaran Gaji: 	157500	\N	2026-05-02 12:12:59.45315+07	\N	\N	\N	\N	f
405	2026-05-02 12:12:59.905796+07	2026-05-02 12:12:59.905796+07	2026-05-02 12:52:08.916479+07	2026-05-02	KELUAR	Pembayaran Gaji: 	165000	\N	2026-05-02 12:12:59.905747+07	\N	\N	\N	\N	f
403	2026-05-02 12:12:58.962658+07	2026-05-02 12:12:58.962658+07	2026-05-02 12:52:13.708312+07	2026-05-02	KELUAR	Pembayaran Gaji: 	262500	\N	2026-05-02 12:12:58.962613+07	\N	\N	\N	\N	f
402	2026-05-02 12:12:58.522233+07	2026-05-02 12:12:58.522233+07	2026-05-02 12:52:15.85472+07	2026-05-02	KELUAR	Pembayaran Gaji: 	287500	\N	2026-05-02 12:12:58.522195+07	\N	\N	\N	\N	f
425	2026-05-05 01:26:14.313234+07	2026-05-05 01:26:14.313234+07	\N	2026-05-04	MASUK	Pembayaran Faktur BMP-2605-001 (mas zahid)	12850000	1	2026-05-05 01:26:14.313192+07	\N	\N	\N	\N	f
426	2026-05-05 01:27:41.615452+07	2026-05-05 01:27:41.615452+07	\N	2026-05-04	MASUK	Pembayaran Borongan Faktur BMP-0426-054	12625000	2	2026-05-05 01:27:41.596012+07	\N	\N	\N	\N	f
427	2026-05-05 01:28:49.971444+07	2026-05-05 01:28:49.971444+07	\N	2026-05-04	MASUK	Pembayaran Borongan Faktur BMP-2605-001	21000000	3	2026-05-05 01:28:49.955775+07	\N	\N	\N	\N	f
429	2026-05-05 09:38:14.576285+07	2026-05-05 09:38:14.576285+07	\N	2026-05-05	MASUK	Pembayaran Borongan Faktur BMP-0426-055	12400000	5	2026-05-05 09:38:14.553214+07	\N	\N	\N	\N	f
430	2026-05-05 09:38:14.607075+07	2026-05-05 09:38:14.607075+07	\N	2026-05-05	MASUK	Pembayaran Borongan Faktur BMP-2605-001	16522000	6	2026-05-05 09:38:14.553214+07	\N	\N	\N	\N	f
428	2026-05-05 01:31:08.379639+07	2026-05-05 01:31:08.379639+07	2026-05-06 00:28:03.145189+07	2026-05-04	MASUK	Pembayaran Borongan Faktur BMP-0426-054	20000000	4	2026-05-05 01:31:08.363764+07	\N	\N	\N	\N	f
437	2026-05-09 12:06:54.472466+07	2026-05-09 12:06:54.472466+07	\N	2026-05-08	KELUAR	beli oli pertamina 2 drum	12310000	\N	2026-05-09 12:06:54.46659+07	\N	\N	\N	\N	f
438	2026-05-09 12:07:48.64233+07	2026-05-09 12:07:48.64233+07	\N	2026-05-09	KELUAR	bayar angsuran mesin	20000000	\N	2026-05-09 12:07:48.632273+07	\N	\N	\N	\N	f
444	2026-05-14 20:16:46.854734+07	2026-05-14 20:16:46.854734+07	\N	2026-04-11	MASUK	Pembayaran Faktur BMP-2605-009 (abah ali)	34692500	14	2026-05-14 20:16:46.854704+07	\N	\N	\N	\N	f
447	2026-05-15 21:08:45.052968+07	2026-05-15 21:08:45.052968+07	\N	2026-05-14	MASUK	Pembayaran Faktur BMP-2605-013 (Mas Malvin)	20845000	17	2026-05-15 21:08:45.052948+07	\N	\N	\N	\N	f
459	2026-05-23 16:24:27.848657+07	2026-05-23 16:24:27.848657+07	\N	2026-05-23	MASUK	Pembayaran Borongan Faktur BMP-0426-011 (mas wiranto)	15039000	21	2026-05-23 16:24:27.832923+07	\N	\N	\N	\N	f
460	2026-05-23 16:24:27.87019+07	2026-05-23 16:24:27.87019+07	\N	2026-05-23	MASUK	Pembayaran Borongan Faktur BMP-0426-050 (mas wiranto)	3506000	22	2026-05-23 16:24:27.832923+07	\N	\N	\N	\N	f
473	2026-05-30 12:58:05.949146+07	2026-05-30 12:58:05.949146+07	2026-05-30 15:18:27.513104+07	2026-05-30	KELUAR	Pembayaran Gaji: Auto-fill (Minggu ini)	50000	\N	2026-05-30 12:58:05.949124+07	\N	\N	\N	\N	f
470	2026-05-30 12:58:04.884509+07	2026-05-30 12:58:04.884509+07	2026-05-30 15:18:33.131115+07	2026-05-30	KELUAR	Pembayaran Gaji: Auto-fill (Minggu ini)	157500	\N	2026-05-30 12:58:04.884496+07	\N	\N	\N	\N	f
472	2026-05-30 12:58:05.596496+07	2026-05-30 12:58:05.596496+07	2026-05-30 15:18:33.131115+07	2026-05-30	KELUAR	Pembayaran Gaji: Auto-fill (Minggu ini)	157500	\N	2026-05-30 12:58:05.596472+07	\N	\N	\N	\N	f
471	2026-05-30 12:58:05.240028+07	2026-05-30 12:58:05.240028+07	2026-05-30 15:18:38.81135+07	2026-05-30	KELUAR	Pembayaran Gaji: Auto-fill (Minggu ini)	225000	\N	2026-05-30 12:58:05.240013+07	\N	\N	\N	\N	f
469	2026-05-30 12:58:04.523482+07	2026-05-30 12:58:04.523482+07	2026-05-30 15:18:46.788159+07	2026-05-30	KELUAR	Pembayaran Gaji: Auto-fill (Minggu ini)	180000	\N	2026-05-30 12:58:04.523465+07	\N	\N	\N	\N	f
474	2026-06-08 16:57:38.748715+07	2026-06-08 16:57:38.748715+07	\N	2026-05-25	MASUK	[DEMO] Pembayaran Faktur DEMO-INV-002 ([DEMO] Toko Plastik Sejahtera)	13500000	27	2026-05-25 16:57:38.744821+07	\N	\N	\N	\N	t
\.


--
-- Data for Name: clients; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.clients (id, created_at, updated_at, deleted_at, saldo_titipan, client_name, address_line1, client_logo, province, postal_code, phone_number, email_address, tax_number, unique_id, slug, date_created, last_updated, is_demo) FROM stdin;
8	2026-03-14 14:36:27.816257+07	2026-03-14 14:36:27.81638+07	\N	0.00	mas kolis	mojojejer, jombang	default_logo.jpg	Jawa Timur	\N	\N	\N	\N	e61abc8929eb	mas-kolis-jawa-timur-e61abc8929eb	2026-03-14 14:36:27.816257+07	2026-03-14 14:36:27.81638+07	f
7	2026-03-12 19:58:52.419143+07	2026-03-24 02:21:12.65418+07	\N	0.00	contoh	Jl. Cendrawasih No. 66 Kecamatan Gedangan Kabupaten Sidoarjo Ds. Punggul Rt. 05 Rw. 02	company_logos/8558_xa9uLKm.jpg	Jawa Timur	61254	6282652626237	muhammadmuizz8@gmail.com	\N	a563faa58c48	contoh-jawa-timur-a563faa58c48	2026-03-12 19:58:52.419143+07	2026-03-24 02:21:12.65418+07	f
12	2026-04-13 08:30:57.775291+07	2026-04-13 08:30:57.775361+07	\N	0.00	abah ali	pasar turi		Jawa Timur	\N	\N	\N	\N	4491ce4544e6	abah-ali-jawa-timur-4491ce4544e6	2026-04-13 08:30:57.775291+07	2026-04-13 08:30:57.775361+07	f
13	2026-04-13 11:46:14.429537+07	2026-04-13 11:46:14.429613+07	\N	0.00	abah aan	kudus		Jawa Timur	\N	\N	\N	\N	9a865fb86b3f	abah-aan-jawa-timur-9a865fb86b3f	2026-04-13 11:46:14.429537+07	2026-04-13 11:46:14.429613+07	f
14	2026-04-13 13:21:31.989129+07	2026-04-13 13:21:31.98924+07	\N	0.00	mas wiranto	jombang			\N	\N	\N	\N	5f52d5627185	mas-wiranto-5f52d5627185	2026-04-13 13:21:31.989129+07	2026-04-13 13:21:31.98924+07	f
15	2026-04-13 18:51:00.200388+07	2026-04-13 18:51:00.200457+07	\N	0.00	Linda Abadi	jl. Raya Sulang-Rembang RT 01 / RW 01			\N	082132939649	\N	\N	5382f6e3c63a	linda-abadi-5382f6e3c63a	2026-04-13 18:51:00.200388+07	2026-04-13 18:51:00.200457+07	f
16	2026-04-14 12:14:35.430808+07	2026-04-14 12:14:35.430882+07	\N	0.00	mas zahid	krian		Jawa Timur	\N	\N	\N	\N	008a9246f228	mas-zahid-jawa-timur-008a9246f228	2026-04-14 12:14:35.430808+07	2026-04-14 12:14:35.430882+07	f
17	2026-04-15 17:35:21.008645+07	2026-04-15 17:35:21.008734+07	\N	0.00	abah kosi'in	grobogan		Jawa Timur	\N	\N	\N	\N	fe67439feeda	abah-kosiin-jawa-timur-fe67439feeda	2026-04-15 17:35:21.008645+07	2026-04-15 17:35:21.008734+07	f
18	2026-04-16 18:20:19.732479+07	2026-04-16 18:20:19.732541+07	\N	0.00	ko hary	surabaya		Jawa Timur	\N	\N	\N	\N	a63dde0f5aa7	ko-hary-jawa-timur-a63dde0f5aa7	2026-04-16 18:20:19.732479+07	2026-04-16 18:20:19.732541+07	f
19	2026-04-20 08:19:25.429696+07	2026-04-20 08:19:25.429926+07	\N	0.00	pak katiran	\N			\N	\N	\N	\N	04f5fec83e9b	pak-katiran-04f5fec83e9b	2026-04-20 08:19:25.429696+07	2026-04-20 08:19:25.429926+07	f
20	2026-04-20 12:28:34.698567+07	2026-04-20 12:28:34.698655+07	\N	0.00	Mas Arylah	\N			\N	\N	\N	\N	2262fc8fe4c4	mas-arylah-2262fc8fe4c4	2026-04-20 12:28:34.698567+07	2026-04-20 12:28:34.698655+07	f
22	2026-04-20 13:30:42.699424+07	2026-04-20 13:30:42.699491+07	\N	0.00	Umik Erna	\N			\N	\N	\N	\N	1cfaa3143ccb	umik-erna-1cfaa3143ccb	2026-04-20 13:30:42.699424+07	2026-04-20 13:30:42.699491+07	f
23	2026-04-20 13:33:11.726279+07	2026-04-20 13:33:11.72635+07	\N	0.00	Pak Huda	\N			\N	\N	\N	\N	0b3afccfc639	pak-huda-0b3afccfc639	2026-04-20 13:33:11.726279+07	2026-04-20 13:33:11.72635+07	f
24	2026-04-20 18:52:23.207269+07	2026-04-20 18:52:23.207349+07	\N	0.00	Mas Malvin	\N			\N	\N	\N	\N	1ee859ca5484	mas-malvin-1ee859ca5484	2026-04-20 18:52:23.207269+07	2026-04-20 18:52:23.207349+07	f
25	2026-04-23 08:38:58.002916+07	2026-04-23 08:38:58.004041+07	\N	0.00	Mas Eka	Jombang			\N	\N	\N	\N	6a80f3487e0f	mas-eka-6a80f3487e0f	2026-04-23 08:38:58.002916+07	2026-04-23 08:38:58.004041+07	f
26	2026-04-28 10:04:02.092367+07	2026-04-28 10:04:02.092453+07	\N	0.00	mas yeyen	blitar		Jawa Timur	\N	\N	\N	\N	c89dad5e8148	mas-yeyen-jawa-timur-c89dad5e8148	2026-04-28 10:04:02.092367+07	2026-04-28 10:04:02.092453+07	f
27	2026-04-30 11:34:36.279282+07	2026-05-01 21:51:07.797272+07	\N	0.00	Mas iyon			gersik					80231e6b		2026-04-30 11:34:36.27083+07	2026-05-01 21:51:07.793428+07	f
35	2026-05-05 20:32:18.843673+07	2026-05-05 20:32:18.843673+07	\N	0.00	pak sandi			malang - jatim					08d3d704	pak-sandi-08d3d704	2026-05-05 20:32:18.835529+07	2026-05-05 20:32:18.835529+07	f
21	2026-04-20 12:37:53.961019+07	2026-05-22 11:11:28.123106+07	\N	0.00	Mas Eko Cahyono			jawa tengah - cepu					a9ca31ee63a8	mas-eko-cahyono-a9ca31ee63a8	2026-04-20 12:37:53.961019+07	2026-05-22 11:11:28.117456+07	f
36	2026-05-22 14:01:07.838885+07	2026-05-22 14:01:07.838885+07	\N	0.00	Abah Hary			Jawa Timur - Lumajang					09b6bef2	abah-hary-09b6bef2	2026-05-22 14:01:07.829787+07	2026-05-22 14:01:07.829787+07	f
40	2026-06-08 16:57:38.718427+07	2026-06-08 16:57:38.718427+07	\N	0.00	[DEMO] PT Plastik Nusantara	Jl. Industri Demo No. 1, Tangerang		Banten	15000	021-0000001	demo1@example.com		48f067da	demo-pt-plastik-nusantara-2e43	2026-05-09 16:57:38.71822+07	2026-06-08 16:57:38.718221+07	t
41	2026-06-08 16:57:38.724342+07	2026-06-08 16:57:38.724342+07	\N	0.00	[DEMO] Toko Plastik Sejahtera	Jl. Pasar Demo No. 22, Bandung		Jawa Barat	40001	022-0000002	demo2@example.com		c7aa78a9	demo-toko-plastik-sejahtera-7312	2026-05-19 16:57:38.718224+07	2026-06-08 16:57:38.718224+07	t
42	2026-06-08 16:57:38.727729+07	2026-06-08 16:57:38.727729+07	\N	0.00	[DEMO] UD Karya Plastik Maju	Jl. Raya Demo No. 99, Surabaya		Jawa Timur	60001	031-0000003	demo3@example.com		d58cf7ea	demo-ud-karya-plastik-maju-0c44	2026-05-29 16:57:38.718227+07	2026-06-08 16:57:38.718227+07	t
\.


--
-- Data for Name: employees; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.employees (id, created_at, updated_at, deleted_at, name, "position", salary_amount, is_active, fingerprint_pin, is_demo) FROM stdin;
21	2026-05-12 13:36:28.620103+07	2026-05-12 13:36:38.034069+07	2026-05-12 13:36:44.273883+07	muizz	admin	83.30	t	\N	f
22	2026-05-12 13:37:12.385778+07	2026-05-12 14:53:49.293778+07	\N	muizz	admin	83300.00	t	1	f
4	2026-05-02 00:01:57.114086+07	2026-05-13 11:49:55.365231+07	\N	mbak santi	operator	50000.00	t	3	f
10	2026-05-02 00:04:31.230305+07	2026-05-13 12:30:26.272514+07	\N	mas ibnu	operator	55000.00	t	5	f
19	2026-05-02 00:30:51.118514+07	2026-05-13 12:30:44.835055+07	\N	mas febri	operator	52500.00	t	4	f
15	2026-05-02 00:29:47.997786+07	2026-05-13 12:34:25.999349+07	\N	mas vincent	operator	50000.00	t	6	f
6	2026-05-02 00:02:49.06164+07	2026-05-13 12:35:41.664547+07	\N	mbak asih	operator	47500.00	t	7	f
5	2026-05-02 00:02:16.865171+07	2026-05-13 12:46:34.551968+07	\N	mbak endah	operator	47500.00	t	8	f
11	2026-05-02 00:28:42.225556+07	2026-05-13 12:47:44.488906+07	\N	mas wahyu	operator	67500.00	t	9	f
8	2026-05-02 00:03:59.158157+07	2026-05-13 14:56:50.156872+07	\N	mas dimas	operator	75000.00	t	10	f
12	2026-05-02 00:28:54.624581+07	2026-05-13 14:57:49.508386+07	\N	mas roby	operator	52500.00	t	11	f
2	2026-05-01 23:33:47.120057+07	2026-05-13 14:58:41.963682+07	\N	mbak nur	operator	57500.00	t	12	f
13	2026-05-02 00:29:14.805332+07	2026-05-13 14:59:44.519471+07	\N	mas farel	operator	52500.00	t	13	f
1	2026-05-01 23:11:45.720327+07	2026-05-13 22:54:16.782479+07	\N	mbak lik	operator	60000.00	t	14	f
3	2026-05-02 00:01:36.66719+07	2026-05-13 22:54:26.17067+07	\N	mbak eni	operator	52500.00	t	15	f
17	2026-05-02 00:30:27.758812+07	2026-05-13 23:01:22.991412+07	\N	mas karem	operator	52500.00	t	16	f
9	2026-05-02 00:04:14.571467+07	2026-05-13 23:01:47.431144+07	\N	mas agung	operator	75000.00	t	17	f
18	2026-05-02 00:30:41.685459+07	2026-05-13 23:02:10.967729+07	\N	mas dian	operator	52500.00	t	18	f
14	2026-05-02 00:29:37.919977+07	2026-05-13 23:04:59.975933+07	\N	mas candra	operator	52500.00	t	19	f
16	2026-05-02 00:30:07.011422+07	2026-05-02 12:57:11.434088+07	2026-05-14 12:29:41.414906+07	mas nanda	operator	50000.00	t	\N	f
20	2026-05-02 12:54:11.012681+07	2026-05-02 12:54:11.012681+07	2026-05-14 12:30:40.9617+07	mbak vivin	operator	50000.00	t	\N	f
7	2026-05-02 00:03:12.291989+07	2026-05-14 12:31:03.791727+07	\N	mbak vivin	operator	47500.00	t	20	f
23	2026-05-22 16:38:12.228808+07	2026-05-22 16:38:12.228808+07	\N	noval	operator	50000.00	t	21	f
24	2026-05-30 12:54:56.962741+07	2026-05-30 12:54:56.962741+07	\N	fareliyan	operator	50000.00	t	22	f
25	2026-06-08 16:57:38.710084+07	2026-06-08 16:57:38.710084+07	\N	[DEMO] Budi Santoso	Operator Extruder 1	95000.00	t	9001	t
26	2026-06-08 16:57:38.713355+07	2026-06-08 16:57:38.713355+07	\N	[DEMO] Joko Susilo	Helper Gulung & Potong	80000.00	t	9002	t
27	2026-06-08 16:57:38.71572+07	2026-06-08 16:57:38.71572+07	\N	[DEMO] Siti Aminah	Checker & Packing	85000.00	t	9003	t
\.


--
-- Data for Name: invoice_payments; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.invoice_payments (id, created_at, updated_at, deleted_at, invoice_id, payment_date, payment_amount, payment_method, date_created) FROM stdin;
1	2026-05-05 01:26:14.30422+07	2026-05-05 01:26:14.30422+07	\N	251	2026-05-04	12850000	TRANSFER	2026-05-05 01:26:14.304194+07
2	2026-05-05 01:27:41.611409+07	2026-05-05 01:27:41.611409+07	\N	207	2026-05-04	12625000	Borongan TRANSFER	2026-05-05 01:27:41.596012+07
3	2026-05-05 01:28:49.967401+07	2026-05-05 01:28:49.967401+07	\N	211	2026-05-04	21000000	Borongan TRANSFER	2026-05-05 01:28:49.955775+07
5	2026-05-05 09:38:14.568073+07	2026-05-05 09:38:14.568073+07	\N	209	2026-05-05	12400000	Borongan TRANSFER	2026-05-05 09:38:14.553214+07
6	2026-05-05 09:38:14.602782+07	2026-05-05 09:38:14.602782+07	\N	250	2026-05-05	16522000	Borongan TRANSFER	2026-05-05 09:38:14.553214+07
7	2026-05-05 16:50:09.630093+07	2026-05-05 16:50:09.630093+07	\N	252	2026-05-05	10000000	TRANSFER	2026-05-05 16:50:09.630072+07
8	2026-05-05 20:42:46.400666+07	2026-05-05 20:42:46.400666+07	\N	111	2026-04-28	20320000	TRANSFER	2026-05-05 20:42:46.400645+07
10	2026-05-05 20:45:28.711869+07	2026-05-05 20:45:28.711869+07	\N	170	2026-05-05	10700000	TRANSFER	2026-05-05 20:45:28.711852+07
11	2026-05-05 20:47:32.656464+07	2026-05-05 20:47:32.656464+07	2026-05-05 22:49:22.077269+07	253	2026-05-05	10700000	TRANSFER	2026-05-05 20:47:32.656437+07
12	2026-05-13 20:28:55.257008+07	2026-05-13 20:28:55.257008+07	\N	115	2026-05-13	53000000	TRANSFER	2026-05-13 20:28:55.256988+07
13	2026-05-14 08:52:55.581542+07	2026-05-14 08:52:55.581542+07	\N	257	2026-05-01	10000000	TRANSFER	2026-05-14 08:52:55.580947+07
14	2026-05-14 20:16:46.845969+07	2026-05-14 20:16:46.845969+07	\N	258	2026-04-11	34692500	TRANSFER	2026-05-14 20:16:46.845947+07
15	2026-05-15 11:10:44.342109+07	2026-05-15 11:10:44.342109+07	\N	135	2026-05-15	14835000	TRANSFER	2026-05-15 11:10:44.340903+07
16	2026-05-15 15:29:57.42802+07	2026-05-15 15:29:57.42802+07	\N	259	2026-05-15	80393000	TRANSFER	2026-05-15 15:29:57.427999+07
17	2026-05-15 21:08:45.04399+07	2026-05-15 21:08:45.04399+07	\N	262	2026-05-14	20845000	TRANSFER	2026-05-15 21:08:45.043966+07
18	2026-05-22 14:03:23.242156+07	2026-05-22 14:03:23.242156+07	\N	109	2026-05-21	31700000	TRANSFER	2026-05-22 14:03:23.242128+07
19	2026-05-23 11:08:40.590149+07	2026-05-23 11:08:40.590149+07	\N	262	2026-05-19	14000000	TRANSFER	2026-05-23 11:08:40.590123+07
21	2026-05-23 16:24:27.842882+07	2026-05-23 16:24:27.842882+07	\N	138	2026-05-23	15039000	Borongan TRANSFER	2026-05-23 16:24:27.832923+07
22	2026-05-23 16:24:27.867209+07	2026-05-23 16:24:27.867209+07	\N	202	2026-05-23	3506000	Borongan TRANSFER	2026-05-23 16:24:27.832923+07
4	2026-05-05 01:31:08.375648+07	2026-05-05 01:31:08.375648+07	2026-05-26 10:33:29.168301+07	207	2026-05-04	20000000	Borongan TRANSFER	2026-05-05 01:31:08.363764+07
20	2026-05-23 11:10:40.135258+07	2026-05-23 11:10:40.135258+07	2026-05-26 10:33:29.419095+07	262	2026-05-23	30000000	TRANSFER	2026-05-23 11:10:40.135244+07
23	2026-05-26 10:35:50.559026+07	2026-05-26 10:35:50.559026+07	\N	262	2026-05-23	20000000	TRANSFER	2026-05-26 10:35:50.558988+07
24	2026-05-26 10:39:01.780922+07	2026-05-26 10:39:01.780922+07	\N	262	2026-05-26	13630000	TRANSFER	2026-05-26 10:39:01.780884+07
27	2026-06-08 16:57:38.745111+07	2026-06-08 16:57:38.745111+07	\N	273	2026-05-25	13500000	TRANSFER	2026-05-25 16:57:38.744821+07
\.


--
-- Data for Name: invoices; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.invoices (id, created_at, updated_at, deleted_at, title, number, due_date, payment_terms, status, notes, client_id, unique_id, slug, date_created, last_updated, is_demo) FROM stdin;
132	2026-04-13 18:58:55.230703+07	2026-04-14 13:33:48.253745+07	\N	\N	BMP-0426-008	2026-04-13 07:00:00+07	14 days	PAID	\N	15	ba484592d050	bmp-0426-008-ba484592d050	2026-04-13 18:58:55.230703+07	2026-04-14 13:33:48.253745+07	f
181	2026-04-20 11:33:27.228529+07	2026-04-20 11:35:57.155324+07	\N	april	BMP-0426-031	2026-04-06 07:00:00+07	14 days	PAID		8	c1769dd67f90	bmp-0426-031-c1769dd67f90	2026-04-20 11:33:27.228529+07	2026-04-20 11:35:57.155324+07	f
183	2026-04-20 11:41:06.4155+07	2026-04-20 11:44:21.722343+07	\N	april	BMP-0426-033	2026-04-06 07:00:00+07	14 days	PAID		8	f6c232361901	bmp-0426-033-f6c232361901	2026-04-20 11:41:06.4155+07	2026-04-20 11:44:21.722343+07	f
167	2026-04-17 15:48:50.179304+07	2026-04-20 12:02:58.433431+07	\N	wiranto	BMP-0426-022	2026-02-18 07:00:00+07	14 days	PAID		14	0689e7a5ca37	bmp-0426-022-0689e7a5ca37	2026-04-17 15:48:50.179304+07	2026-04-20 12:02:58.433431+07	f
112	2026-04-13 13:20:24.784067+07	2026-04-15 09:09:30.363036+07	\N	wiranto	BMP-0426-004	2026-03-09 07:00:00+07	14 days	PAID	jatuh tempo 09/03/2026	14	f22c2645b4ca	bmp-0426-004-f22c2645b4ca	2026-04-13 13:20:24.784067+07	2026-04-15 09:09:30.363036+07	f
155	2026-04-16 18:13:30.484714+07	2026-04-16 18:15:41.821922+07	\N	wiranto	BMP-0426-014	2026-03-03 07:00:00+07	14 days	PAID		14	c229bb5b2901	bmp-0426-014-c229bb5b2901	2026-04-16 18:13:30.484714+07	2026-04-16 18:15:41.821922+07	f
184	2026-04-20 12:04:11.706662+07	2026-04-20 12:10:51.448883+07	\N	april	BMP-0426-034	2026-02-23 07:00:00+07	14 days	PAID		14	2957164f1bd3	bmp-0426-034-2957164f1bd3	2026-04-20 12:04:11.706662+07	2026-04-20 12:10:51.448883+07	f
185	2026-04-20 12:11:52.364872+07	2026-04-20 12:13:40.6995+07	\N	april	BMP-0426-035	2026-02-12 07:00:00+07	14 days	PAID		14	00c699cad969	bmp-0426-035-00c699cad969	2026-04-20 12:11:52.364872+07	2026-04-20 12:13:40.6995+07	f
186	2026-04-20 12:13:59.365191+07	2026-04-20 12:17:41.570543+07	\N	april	BMP-0426-036	2026-02-12 07:00:00+07	14 days	PAID		14	f5567849cceb	bmp-0426-036-f5567849cceb	2026-04-20 12:13:59.365191+07	2026-04-20 12:17:41.570543+07	f
187	2026-04-20 12:18:46.742827+07	2026-04-20 12:20:58.332284+07	\N	april	BMP-0426-037	2026-02-28 07:00:00+07	14 days	PAID		14	c824f3289e0e	bmp-0426-037-c824f3289e0e	2026-04-20 12:18:46.742827+07	2026-04-20 12:20:58.332284+07	f
188	2026-04-20 12:25:10.849641+07	2026-04-20 12:26:54.920487+07	\N	april	BMP-0426-038	2026-03-10 07:00:00+07	14 days	PAID		14	41f87ea14ebc	bmp-0426-038-41f87ea14ebc	2026-04-20 12:25:10.849641+07	2026-04-20 12:26:54.920487+07	f
190	2026-04-20 12:38:22.786717+07	2026-04-20 12:42:13.400867+07	\N	April	BMP-0426-039	2026-03-10 07:00:00+07	14 days	PAID		20	afbbd75f647b	bmp-0426-039-afbbd75f647b	2026-04-20 12:38:22.786717+07	2026-04-20 12:42:13.400867+07	f
191	2026-04-20 12:47:39.781002+07	2026-04-20 12:48:49.211048+07	\N	april	BMP-0426-040	2026-03-13 07:00:00+07	14 days	PAID		14	5813288498f9	bmp-0426-040-5813288498f9	2026-04-20 12:47:39.781002+07	2026-04-20 12:48:49.211048+07	f
156	2026-04-16 18:17:13.082539+07	2026-04-16 18:19:08.763406+07	\N	wiranto	BMP-0426-015	2026-03-04 07:00:00+07	14 days	PAID		14	9a1df5ac9fb8	bmp-0426-015-9a1df5ac9fb8	2026-04-16 18:17:13.082539+07	2026-04-16 18:19:08.763406+07	f
114	2026-04-13 15:02:57.672657+07	2026-04-24 12:49:23.838821+07	\N	maret	BMP-0426-006	2026-04-03 07:00:00+07	14 days	PAID	jatuh tempo 03/04/2026	14	87902a8333fb	bmp-0426-006-87902a8333fb	2026-04-13 15:02:57.672657+07	2026-04-24 12:49:23.838821+07	f
150	2026-04-15 17:33:33.203889+07	2026-04-15 17:56:55.86398+07	\N	ok	BMP-0426-013	2026-03-02 07:00:00+07	14 days	PAID		17	4626b369ace2	bmp-0426-013-4626b369ace2	2026-04-15 17:33:33.203889+07	2026-04-15 17:56:55.86398+07	f
192	2026-04-20 12:48:55.721047+07	2026-04-20 12:50:42.759251+07	\N	april	BMP-0426-041	2026-03-14 07:00:00+07	14 days	PAID		21	6dbc817617bf	bmp-0426-041-6dbc817617bf	2026-04-20 12:48:55.721047+07	2026-04-20 12:50:42.759251+07	f
193	2026-04-20 12:55:52.237317+07	2026-04-20 12:56:37.946317+07	\N	april	BMP-0426-042	2026-03-14 07:00:00+07	14 days	PAID		15	557f407bc957	bmp-0426-042-557f407bc957	2026-04-20 12:55:52.237317+07	2026-04-20 12:56:37.946317+07	f
194	2026-04-20 13:30:49.863636+07	2026-04-20 13:32:46.208595+07	\N	April	BMP-0426-043	2026-03-14 07:00:00+07	14 days	PAID		22	965a80dd232f	bmp-0426-043-965a80dd232f	2026-04-20 13:30:49.863636+07	2026-04-20 13:32:46.208595+07	f
158	2026-04-16 18:20:27.756609+07	2026-04-16 18:21:31.350582+07	\N	ko hary	BMP-0426-016	2026-03-05 07:00:00+07	14 days	PAID		18	8bafdd83eca9	bmp-0426-016-8bafdd83eca9	2026-04-16 18:20:27.756609+07	2026-04-16 18:21:31.350582+07	f
160	2026-04-17 14:01:41.9626+07	2026-04-17 14:03:38.037662+07	\N	mas kolis	BMP-0426-017	2026-02-27 07:00:00+07	14 days	PAID		8	c5342542eb13	bmp-0426-017-c5342542eb13	2026-04-17 14:01:41.9626+07	2026-04-17 14:03:38.037662+07	f
172	2026-04-20 08:19:53.550281+07	2026-04-20 13:55:42.211155+07	\N	april	BMP-0426-025	2026-04-20 07:00:00+07	14 days	PAID		19	550f8bbf3c75	bmp-0426-025-550f8bbf3c75	2026-04-20 08:19:53.550281+07	2026-04-20 13:55:42.211155+07	f
195	2026-04-20 13:33:23.235514+07	2026-04-20 18:44:34.312561+07	\N	April	BMP-0426-044	2026-03-14 07:00:00+07	14 days	PAID		23	93b3381f7d46	bmp-0426-044-93b3381f7d46	2026-04-20 13:33:23.235514+07	2026-04-20 18:44:34.312561+07	f
149	2026-04-15 17:29:37.716192+07	2026-04-17 15:35:23.268428+07	\N	kolis	BMP-0426-012	2026-03-02 07:00:00+07	14 days	PAID		8	6650e0ec148c	bmp-0426-012-6650e0ec148c	2026-04-15 17:29:37.716192+07	2026-04-17 15:35:23.268428+07	f
163	2026-04-17 15:35:46.073397+07	2026-04-17 15:38:33.969204+07	\N	desember	BMP-0426-018	2025-12-22 07:00:00+07	14 days	PAID		12	949eed424074	bmp-0426-018-949eed424074	2026-04-17 15:35:46.073397+07	2026-04-17 15:38:33.969204+07	f
196	2026-04-20 18:45:34.424431+07	2026-04-20 18:50:53.454465+07	\N	April	BMP-0426-045	2026-03-24 07:00:00+07	14 days	PAID		17	d4cb08234888	bmp-0426-045-d4cb08234888	2026-04-20 18:45:34.424431+07	2026-04-20 18:50:53.454465+07	f
164	2026-04-17 15:39:17.632283+07	2026-04-17 15:40:24.045339+07	\N	ali	BMP-0426-019	2026-01-08 07:00:00+07	14 days	PAID		12	be9dc85c37fd	bmp-0426-019-be9dc85c37fd	2026-04-17 15:39:17.632283+07	2026-04-17 15:40:24.045339+07	f
165	2026-04-17 15:40:56.608314+07	2026-04-17 15:43:05.573505+07	\N	ali	BMP-0426-020	2026-04-17 07:00:00+07	14 days	PAID		12	0c0a34480e9d	bmp-0426-020-0c0a34480e9d	2026-04-17 15:40:56.608314+07	2026-04-17 15:43:05.573505+07	f
197	2026-04-20 18:51:35.6128+07	2026-04-20 18:58:34.720481+07	\N	Maret	BMP-0426-046	2026-03-29 07:00:00+07	14 days	PAID		24	0cf8359599e5	bmp-0426-046-0cf8359599e5	2026-04-20 18:51:35.6128+07	2026-04-20 18:58:34.720481+07	f
166	2026-04-17 15:44:35.937657+07	2026-04-17 15:45:41.924406+07	\N	kolis	BMP-0426-021	2026-03-05 07:00:00+07	14 days	PAID		8	d8843b58024c	bmp-0426-021-d8843b58024c	2026-04-17 15:44:35.937657+07	2026-04-17 15:45:41.924406+07	f
198	2026-04-20 19:03:38.904141+07	2026-04-20 19:09:47.929137+07	\N	April	BMP-0426-047	2026-04-09 07:00:00+07	14 days	PAID		19	824916832967	bmp-0426-047-824916832967	2026-04-20 19:03:38.904141+07	2026-04-20 19:09:47.929137+07	f
169	2026-04-18 08:51:36.411162+07	2026-04-20 08:07:13.280727+07	\N	kolis	BMP-0426-023	2026-05-02 07:00:00+07	14 days	PAID	Jatuh Tempo 02/05/2026	8	4e51b15a3f2d	bmp-0426-023-4e51b15a3f2d	2026-04-18 08:51:36.411162+07	2026-04-20 08:07:13.280727+07	f
173	2026-04-20 08:34:33.625611+07	2026-04-20 08:36:24.103774+07	\N	maret	BMP-0426-026	2026-04-01 07:00:00+07	14 days	PAID		8	585a7d38a91a	bmp-0426-026-585a7d38a91a	2026-04-20 08:34:33.625611+07	2026-04-20 08:36:24.103774+07	f
174	2026-04-20 08:36:57.140507+07	2026-04-20 08:38:56.133739+07	\N	maret	BMP-0426-027	2026-03-07 07:00:00+07	14 days	PAID		8	3a1cb0e22771	bmp-0426-027-3a1cb0e22771	2026-04-20 08:36:57.140507+07	2026-04-20 08:38:56.133739+07	f
175	2026-04-20 08:39:23.885121+07	2026-04-20 08:40:46.262196+07	\N	maret	BMP-0426-028	2026-03-11 07:00:00+07	14 days	PAID		8	13ec3348f9b4	bmp-0426-028-13ec3348f9b4	2026-04-20 08:39:23.885121+07	2026-04-20 08:40:46.262196+07	f
203	2026-04-23 08:39:03.785473+07	2026-04-23 08:40:46.818486+07	\N	April	BMP-0426-051	2026-04-23 07:00:00+07	14 days	OVERDUE		25	0c9d954f1b9b	bmp-0426-051-0c9d954f1b9b	2026-04-23 08:39:03.785473+07	2026-04-23 08:40:46.818486+07	f
134	2026-04-14 12:12:13.242555+07	2026-04-20 08:59:57.471339+07	\N	\N	BMP-0426-009	2026-04-11 07:00:00+07	14 days	PAID	\N	16	0d595fd9ce98	bmp-0426-009-0d595fd9ce98	2026-04-14 12:12:13.242555+07	2026-04-20 08:59:57.471339+07	f
135	2026-04-14 12:54:46.856172+07	2026-05-15 11:10:44.379207+07	\N		BMP-0426-010	2026-04-08 07:00:00+07	14 days	PAID		14	eb614695dac1	bmp-0426-010-eb614695dac1	2026-04-14 12:54:46.856172+07	2026-04-24 12:49:23.847196+07	f
170	2026-04-19 17:10:05.301931+07	2026-05-05 20:45:28.732612+07	\N	april	BMP-0426-024	2026-05-18 07:00:00+07	30 days	PAID	Jatuh Tempo 18/05/2026	16	7e03092e65a8	bmp-0426-024-7e03092e65a8	2026-04-19 17:10:05.301931+07	2026-04-19 17:28:56.836127+07	f
115	2026-04-13 15:26:21.098051+07	2026-05-13 20:28:55.294473+07	\N	maret	BMP-0426-007	2026-04-04 07:00:00+07	14 days	PAID	jatuh tempo 04/04/2026	12	b6327ab2bc52	bmp-0426-007-b6327ab2bc52	2026-04-13 15:26:21.098051+07	2026-04-13 15:34:14.434853+07	f
182	2026-04-20 11:36:27.502119+07	2026-05-14 20:04:16.528479+07	\N	april	BMP-0426-032	2026-04-06 07:00:00+07	14 days	UNPAID		8	fd9bd5ccae75	bmp-0426-032-fd9bd5ccae75	2026-04-20 11:36:27.502119+07	2026-04-20 11:40:16.101591+07	f
109	2026-04-13 08:31:08.38803+07	2026-05-22 14:03:23.273346+07	\N	maret	BMP-0426-001	2026-03-05 07:00:00+07	14 days	PAID	jatuh tempo tanggal 05/03/2026	12	b1b1618544ab	bmp-0426-001-b1b1618544ab	2026-04-13 08:31:08.38803+07	2026-04-13 11:10:53.172034+07	f
138	2026-04-15 08:28:38.455407+07	2026-05-23 16:24:27.85469+07	\N		BMP-0426-011	2026-04-15 07:00:00+07	14 days	PAID		14	efb5b9e360c4	bmp-0426-011-efb5b9e360c4	2026-04-15 08:28:38.455407+07	2026-04-15 09:14:38.129807+07	f
180	2026-04-20 11:21:27.699106+07	2026-05-29 22:22:02.758258+07	\N	april	BMP-0426-030	2026-04-11 07:00:00+07	14 days	UNPAID		8	7ed262bd1c5a	bmp-0426-030-7ed262bd1c5a	2026-04-20 11:21:27.699106+07	2026-04-20 11:30:38.331392+07	f
179	2026-04-20 09:00:18.659115+07	2026-04-20 09:02:09.019398+07	\N	maret	BMP-0426-029	2026-03-07 07:00:00+07	14 days	PAID		16	ad411ec1303a	bmp-0426-029-ad411ec1303a	2026-04-20 09:00:18.659115+07	2026-04-20 09:02:09.019398+07	f
113	2026-04-13 13:42:37.684886+07	2026-04-24 12:49:23.831387+07	\N	februari	BMP-0426-005	2026-04-02 07:00:00+07	14 days	PAID	jatuh tempo 02/04/2026	14	0a50b6ceefc5	bmp-0426-005-0a50b6ceefc5	2026-04-13 13:42:37.684886+07	2026-04-24 12:49:23.831387+07	f
110	2026-04-13 11:03:54.624887+07	2026-04-27 15:53:52.862041+07	\N	februari	BMP-0426-002	2026-02-26 07:00:00+07	14 days	PAID	jatuh tempo 26/02/2026	12	aa7ed98b7600	bmp-0426-002-aa7ed98b7600	2026-04-13 11:03:54.624887+07	2026-04-27 15:53:52.862041+07	f
200	2026-04-21 16:43:06.159773+07	2026-04-28 14:53:51.36573+07	\N	april	BMP-0426-048	2026-04-28 07:00:00+07	14 days	PAID		17	4cbc1cd3a1d8	bmp-0426-048-4cbc1cd3a1d8	2026-04-21 16:43:06.159773+07	2026-04-28 14:53:51.36573+07	f
205	2026-04-24 12:51:51.955767+07	2026-04-24 12:52:57.998408+07	\N	April	BMP-0426-052	2026-04-24 07:00:00+07	14 days	PAID		14	0aed2dbac456	bmp-0426-052-0aed2dbac456	2026-04-24 12:51:51.955767+07	2026-04-24 12:52:57.998408+07	f
206	2026-04-27 09:51:29.664357+07	2026-04-27 10:01:50.762873+07	\N	April	BMP-0426-053	2026-05-09 07:00:00+07	14 days	UNPAID		23	4f44ed5e1757	bmp-0426-053-4f44ed5e1757	2026-04-27 09:51:29.664357+07	2026-04-27 10:01:50.762873+07	f
210	2026-04-28 10:06:40.300615+07	2026-04-28 10:09:55.24913+07	\N	april	BMP-0426-056	2026-05-05 07:00:00+07	14 days	UNPAID		26	0ecaeb93fc2a	bmp-0426-056-0ecaeb93fc2a	2026-04-28 10:06:40.300615+07	2026-04-28 10:09:55.24913+07	f
239	2026-05-01 22:35:20.294007+07	2026-05-01 22:35:20.294007+07	2026-05-01 22:38:55.181273+07	Faktur Penjualan	BMP-2605-001	2026-05-31 07:00:00+07	14 days	UNPAID		12	461bf0aa	BMP-2605-001-461bf0aa	2026-05-01 22:35:20.285855+07	2026-05-01 22:35:20.285855+07	f
247	2026-05-02 08:10:56.034599+07	2026-05-02 08:10:56.034599+07	2026-05-02 08:11:44.651727+07	Faktur Penjualan	BMP-2605-001	2026-06-01 07:00:00+07	14 days	UNPAID		12	51d69bd6	BMP-2605-001-51d69bd6	2026-05-02 08:10:56.007965+07	2026-05-02 08:10:56.007965+07	f
249	2026-05-02 08:14:05.024981+07	2026-05-02 08:14:05.024981+07	2026-05-02 14:59:33.943401+07	Faktur Penjualan	BMP-2605-001	2026-05-09 07:00:00+07	14 days	UNPAID		7	e0fc2445	BMP-2605-001-e0fc2445	2026-05-02 08:14:05.018307+07	2026-05-02 08:14:05.018307+07	f
251	2026-05-05 01:25:53.261138+07	2026-05-13 13:57:08.78228+07	\N	Faktur Penjualan	BMP-2605-004	2026-05-03 07:00:00+07	14 days	PAID		16	9e707f6b	BMP-2605-004-9e707f6b	2026-05-05 01:25:53.252525+07	2026-05-05 01:25:53.252525+07	f
255	2026-05-05 20:49:45.33981+07	2026-05-13 13:57:12.167937+07	\N	Faktur Penjualan	BMP-2605-006	2026-06-04 07:00:00+07	14 days	UNPAID		35	35ca2747	BMP-2605-006-35ca2747	2026-05-05 20:49:45.331168+07	2026-05-05 20:49:45.331168+07	f
256	2026-05-09 12:35:55.040462+07	2026-05-13 13:57:13.49137+07	\N	Faktur Penjualan	BMP-2605-007	2026-06-08 07:00:00+07	14 days	UNPAID		12	dd96ce2c	BMP-2605-007-dd96ce2c	2026-05-09 12:35:55.035597+07	2026-05-09 12:35:55.035597+07	f
262	2026-05-15 21:07:39.335324+07	2026-05-26 10:39:02.406663+07	\N	Faktur Penjualan	BMP-2605-013	2026-05-29 07:00:00+07	14 days	PAID		24	4d43d330	BMP-2605-013-4d43d330	2026-05-10 07:00:00+07	2026-05-15 21:07:39.331408+07	f
263	2026-05-22 11:25:42.696868+07	2026-05-22 11:25:42.696868+07	\N	Faktur Penjualan	BMP-2605-014	2026-06-05 07:00:00+07	14 days	UNPAID		24	13b59092	BMP-2605-014-13b59092	2026-05-22 07:00:00+07	2026-05-22 11:25:42.692787+07	f
211	2026-05-01 15:19:25.28318+07	2026-05-05 01:28:49.975752+07	\N	Faktur Penjualan	BMP-2605-001	2026-05-08 07:00:00+07	14 days	PAID		27	037c4106		2026-05-01 15:19:25.270454+07	2026-05-01 15:19:25.270454+07	f
257	2026-05-14 08:52:21.013767+07	2026-05-26 16:06:27.597589+07	\N	Faktur Penjualan	BMP-2605-008	2026-06-13 07:00:00+07	14 days	PARTIAL		14	b971b6d0	BMP-2605-008-b971b6d0	2026-05-01 07:00:00+07	2026-05-14 08:52:21.009903+07	f
209	2026-04-28 08:44:29.088876+07	2026-05-05 09:38:14.580363+07	\N	april	BMP-0426-055	2026-04-28 07:00:00+07	14 days	PAID		8	465eaf53d49d	bmp-0426-055-465eaf53d49d	2026-04-28 08:44:29.088876+07	2026-04-28 08:46:32.727978+07	f
252	2026-05-05 16:49:58.51082+07	2026-05-14 08:45:18.505999+07	\N	Faktur Penjualan	BMP-2605-005	2026-05-31 07:00:00+07	14 days	PAID		14	25268dae	BMP-2605-005-25268dae	2026-05-05 16:49:58.501189+07	2026-05-05 16:49:58.501189+07	f
111	2026-04-13 11:45:02.023171+07	2026-05-05 20:42:46.44072+07	\N	februari	BMP-0426-003	2026-02-28 07:00:00+07	14 days	PAID	jatuh tempo 28/02/2026	13	217f12bce51b	bmp-0426-003-217f12bce51b	2026-04-13 11:45:02.023171+07	2026-04-13 13:14:03.076882+07	f
254	2026-05-05 20:35:14.856972+07	2026-05-05 20:35:14.856972+07	2026-05-05 20:49:12.644752+07	Faktur Penjualan	BMP-2605-004	2026-05-19 07:00:00+07	14 days	UNPAID		35	18ed465c	BMP-2605-004-18ed465c	2026-05-05 20:35:14.845452+07	2026-05-05 20:35:14.845452+07	f
253	2026-05-05 17:26:59.30431+07	2026-05-05 20:47:32.67725+07	2026-05-05 22:49:22.086973+07	Faktur Penjualan	BMP-2605-003	2026-05-19 07:00:00+07	14 days	PAID		24	95768d12	BMP-2605-003-95768d12	2026-05-05 17:26:59.293953+07	2026-05-05 17:26:59.293953+07	f
248	2026-05-02 08:13:22.504017+07	2026-05-13 13:57:06.04413+07	\N	Faktur Penjualan	BMP-2605-002	2026-06-01 07:00:00+07	14 days	UNPAID		12	7ce5ac61	BMP-2605-002-7ce5ac61	2026-05-02 08:13:22.497607+07	2026-05-02 08:13:22.497607+07	f
250	2026-05-02 16:00:45.574609+07	2026-05-13 13:57:07.560953+07	\N	Faktur Penjualan	BMP-2605-003	2026-05-16 07:00:00+07	14 days	PAID		8	d1aebb79	BMP-2605-003-d1aebb79	2026-05-02 16:00:45.565751+07	2026-05-02 16:00:45.565751+07	f
258	2026-05-14 20:15:37.85645+07	2026-05-14 20:16:46.880813+07	\N	Faktur Penjualan	BMP-2605-009	2026-02-15 07:00:00+07	14 days	PAID		12	6bea2397	BMP-2605-009-6bea2397	2026-04-11 07:00:00+07	2026-05-14 20:15:37.852218+07	f
260	2026-05-15 07:31:27.0279+07	2026-05-15 08:27:26.297383+07	\N	Faktur Penjualan	BMP-2605-011	2026-05-29 07:00:00+07	14 days	UNPAID		35	51099424	BMP-2605-011-51099424	2026-05-15 07:00:00+07	2026-05-15 07:31:27.021778+07	f
264	2026-05-22 11:31:06.961129+07	2026-05-22 11:32:37.194395+07	\N	Faktur Penjualan	BMP-2605-015	2026-06-05 07:00:00+07	14 days	UNPAID		21	88bb7619	BMP-2605-015-88bb7619	2026-05-22 07:00:00+07	2026-05-22 11:31:06.957194+07	f
265	2026-05-22 14:05:15.770151+07	2026-05-22 14:05:15.770151+07	\N	Faktur Penjualan	BMP-2605-016	2026-06-02 07:00:00+07	14 days	UNPAID		36	3df6596f	BMP-2605-016-3df6596f	2026-05-19 07:00:00+07	2026-05-22 14:05:15.766249+07	f
270	2026-05-30 09:00:44.531468+07	2026-05-30 09:01:22.190116+07	\N	Faktur Penjualan	BMP-2605-018	2026-06-13 07:00:00+07	14 days	UNPAID		8	915e3416	BMP-2605-018-915e3416	2026-05-30 07:00:00+07	2026-05-30 09:00:44.526207+07	f
259	2026-05-14 20:35:43.491948+07	2026-05-26 16:10:18.713521+07	\N	Faktur Penjualan	BMP-2605-010	2026-05-06 07:00:00+07	14 days	PAID		17	c94477e0	BMP-2605-010-c94477e0	2026-05-14 07:00:00+07	2026-05-14 20:35:43.487723+07	f
261	2026-05-15 09:35:08.347501+07	2026-05-15 17:23:08.079809+07	\N	Faktur Penjualan	BMP-2605-012	2026-06-14 07:00:00+07	14 days	UNPAID		14	006f3869	BMP-2605-012-006f3869	2026-05-15 07:00:00+07	2026-05-15 09:35:08.345126+07	f
202	2026-04-21 20:49:37.440778+07	2026-05-23 16:24:27.873107+07	\N	april	BMP-0426-050	2026-05-05 07:00:00+07	14 days	PARTIAL	Jatuh Tempo 05/05/2026	14	a0c9af198361	bmp-0426-050-a0c9af198361	2026-04-21 20:49:37.440778+07	2026-04-21 20:51:42.887029+07	f
266	2026-05-25 11:32:42.654245+07	2026-05-25 12:07:35.644933+07	\N	Faktur Penjualan	BMP-2605-017	2026-06-24 07:00:00+07	14 days	UNPAID		12	00c25e20	BMP-2605-017-00c25e20	2026-05-25 07:00:00+07	2026-05-25 11:32:42.649276+07	f
207	2026-04-27 10:33:28.906492+07	2026-05-26 10:33:29.920341+07	\N	april	BMP-0426-054	2026-05-03 07:00:00+07	14 days	PARTIAL		24	065702d819fa	bmp-0426-054-065702d819fa	2026-04-27 10:33:28.906492+07	2026-04-27 10:41:33.243666+07	f
271	2026-06-01 09:30:00.656065+07	2026-06-01 09:30:00.656065+07	\N	Faktur Penjualan	BMP-2606-001	2026-06-15 07:00:00+07	14 days	UNPAID		8	a849f0da	BMP-2606-001-a849f0da	2026-06-01 07:00:00+07	2026-06-01 09:30:00.650471+07	f
272	2026-06-08 16:57:38.730711+07	2026-06-08 16:57:38.730711+07	\N	Demo - Pembelian Kantong Plastik HDPE	DEMO-INV-001	2026-06-15 16:57:38.730483+07	14 days	UNPAID	Ini adalah faktur contoh untuk akun demo. Data ini tidak mempengaruhi data produksi.	40	d8a468c0	DEMO-INV-001-d8a468c0	2026-06-03 16:57:38.730491+07	2026-06-08 16:57:38.730491+07	t
273	2026-06-08 16:57:38.739108+07	2026-06-08 16:57:38.739108+07	\N	Demo - Pengiriman Cup PP Reguler	DEMO-INV-002	2026-06-03 16:57:38.738881+07	COD	PAID	Faktur demo sudah lunas.	41	586d8104	DEMO-INV-002-586d8104	2026-05-24 16:57:38.738887+07	2026-06-08 16:57:38.738888+07	t
276	2026-06-06 12:00:00+07	2026-06-06 12:00:00+07	\N	Faktur Penjualan	BMP-2606-007	2026-06-20 12:00:00+07	14 days	UNPAID		12	cf84e1b3	BMP-2606-007-cf84e1b3	2026-06-06 12:00:00+07	2026-06-06 12:00:00+07	f
\.


--
-- Data for Name: machine_bonus_logs; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.machine_bonus_logs (id, created_at, updated_at, deleted_at, employee_id, machine_name, shift_type, bonus_amount, date, jumlah_perolehan, is_demo) FROM stdin;
1	2026-05-15 15:33:23.401385+07	2026-05-15 15:33:23.401385+07	\N	22	Baskom Mawar	Siang	5000.00	2026-05-15	0	f
3	2026-05-16 17:34:10.530912+07	2026-05-16 17:34:10.530912+07	\N	22	Baskom Panda	Pagi	10000.00	2026-05-16	0	f
4	2026-05-17 12:59:50.221989+07	2026-05-17 12:59:50.221989+07	\N	22	Baskom Panda	Pagi	10000.00	2026-05-17	0	f
5	2026-05-17 13:00:24.588556+07	2026-05-17 13:00:24.588556+07	\N	19	Baskom Panda	Pagi	10000.00	2026-05-17	0	f
6	2026-05-17 13:00:34.555377+07	2026-05-17 13:00:34.555377+07	\N	10	Baskom Panda	Pagi	10000.00	2026-05-17	0	f
7	2026-05-17 13:00:43.415841+07	2026-05-17 13:00:43.415841+07	\N	5	Baskom Panda	Pagi	10000.00	2026-05-17	0	f
8	2026-05-19 00:46:55.044064+07	2026-05-19 00:46:55.044064+07	\N	22	Wakul Moris	Pagi	5000.00	2026-05-18	0	f
9	2026-05-22 16:22:26.9644+07	2026-05-22 16:22:26.9644+07	\N	22	Baskom Panda	Sore	5000.00	2026-05-22	50	f
10	2026-05-22 16:23:40.340454+07	2026-05-22 16:23:40.340454+07	\N	12	Baskom Panda	Sore	5000.00	2026-05-22	51	f
11	2026-05-22 16:24:22.728192+07	2026-05-22 16:24:22.728192+07	\N	2	Baskom Jago	Sore	7000.00	2026-05-22	51	f
12	2026-05-22 16:25:48.433287+07	2026-05-22 16:25:48.433287+07	\N	13	BMP	Sore	5000.00	2026-05-22	36	f
13	2026-05-22 16:26:20.839619+07	2026-05-22 16:26:20.839619+07	\N	8	Wakul Moris	Sore	5000.00	2026-05-22	60	f
14	2026-05-22 16:33:37.058526+07	2026-05-22 16:33:37.058526+07	\N	15	Baskom Durian	Sore	5000.00	2026-05-22	37	f
15	2026-05-22 16:34:28.190021+07	2026-05-22 16:34:28.190021+07	\N	7	Baskom Panda	Sore	5000.00	2026-05-22	99	f
16	2026-05-22 22:54:25.900092+07	2026-05-22 22:54:25.900092+07	\N	17	Baskom Panda	Malam	5000.00	2026-05-22	13	f
17	2026-05-22 22:57:47.731799+07	2026-05-22 22:57:47.731799+07	\N	9	BMP	Malam	5000.00	2026-05-22	10	f
18	2026-05-22 22:58:19.542624+07	2026-05-22 22:58:19.542624+07	\N	3	Baskom Durian	Malam	5000.00	2026-05-22	38	f
19	2026-05-22 22:58:58.507293+07	2026-05-22 22:58:58.507293+07	\N	18	Wakul Moris	Malam	5000.00	2026-05-22	52	f
20	2026-05-22 22:59:19.832466+07	2026-05-22 22:59:19.832466+07	\N	23	Wakul Moris	Malam	5000.00	2026-05-22	99	f
21	2026-05-29 22:11:29.409616+07	2026-05-29 22:11:29.409616+07	\N	22	Bahtera TM	Pagi	5000.00	2026-05-29	80	f
\.


--
-- Data for Name: master_products; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.master_products (id, created_at, updated_at, deleted_at, title, description, unit, price, berat_gram, cycle_time, unique_id, slug, date_created, last_updated, is_demo, cavity, reject_rate) FROM stdin;
29	2026-04-28 15:11:49.94791+07	2026-04-28 15:11:49.94791+07	\N	Baskom TM	\N	lusin	7000	40	9.5	\N	\N	\N	\N	f	1	0
38	2026-04-28 15:11:49.94791+07	2026-04-28 15:11:49.94791+07	\N	Smile 14	\N	Lusin	8200	0	0	\N	\N	\N	\N	f	1	0
37	2026-04-28 15:11:49.94791+07	2026-04-28 15:11:49.94791+07	\N	telor tali	\N	Lusin	3200	0	0	\N	\N	\N	\N	f	1	0
39	2026-04-28 15:11:49.94791+07	2026-04-28 15:11:49.94791+07	\N	tradisi cerah	\N	Lusin	5200	0	0	\N	\N	\N	\N	f	1	0
40	2026-04-28 15:11:49.94791+07	2026-04-28 15:11:49.94791+07	\N	Baskom Barca	\N	Lusin	7250	0	0	\N	\N	\N	\N	f	1	0
41	2026-04-28 15:11:49.94791+07	2026-04-28 15:11:49.94791+07	\N	Wakul Tanggok	\N	Lusin	5800	0	0	\N	\N	\N	\N	f	1	0
42	2026-04-28 15:11:49.94791+07	2026-04-28 15:11:49.94791+07	\N	Baskom Rotan	\N	Lusin	8400	0	0	\N	\N	\N	\N	f	1	0
43	2026-04-28 15:11:49.94791+07	2026-04-28 15:11:49.94791+07	\N	Wakul Mawar Super	\N	Lusin	9000	0	0	\N	\N	\N	\N	f	1	0
44	2026-04-28 15:11:49.94791+07	2026-04-28 15:11:49.94791+07	\N	Smile 14	\N	Lusin	8400	0	0	\N	\N	\N	\N	f	1	0
45	2026-04-28 15:11:49.94791+07	2026-04-28 15:11:49.94791+07	\N	Wakul Morris Super	\N	Lusin	5700	0	0	\N	\N	\N	\N	f	1	0
46	2026-05-01 15:18:43.627772+07	2026-05-01 15:18:43.627772+07	\N	BMP		Lusin	7000	0	0	44ba7f8a		2026-05-01 15:18:43.616791+07	2026-05-01 15:18:43.616792+07	f	1	0
15	2026-04-28 15:11:49.94791+07	2026-04-28 15:11:49.94791+07	\N	contoh	\N	Lusin	5000	0	0	\N	\N	\N	\N	f	1	0
13	2026-04-28 15:11:49.94791+07	2026-04-28 15:11:49.94791+07	\N	baskom panda	\N	Lusin	7000	0	0	\N	\N	\N	\N	f	1	0
16	2026-04-28 15:11:49.94791+07	2026-04-28 15:11:49.94791+07	\N	baskom mawar	\N	Lusin	5800	0	0	\N	\N	\N	\N	f	1	0
17	2026-04-28 15:11:49.94791+07	2026-04-28 15:11:49.94791+07	\N	baskom bahtera TM	\N	Lusin	6100	0	0	\N	\N	\N	\N	f	1	0
19	2026-04-28 15:11:49.94791+07	2026-04-28 15:11:49.94791+07	\N	wakul moris	\N	Lusin	5500	0	0	\N	\N	\N	\N	f	1	0
20	2026-04-28 15:11:49.94791+07	2026-04-28 15:11:49.94791+07	\N	baskom jago	\N	Lusin	5600	0	0	\N	\N	\N	\N	f	1	0
23	2026-04-28 15:11:49.94791+07	2026-04-28 15:11:49.94791+07	\N	smile 12	\N	Lusin	5400	0	0	\N	\N	\N	\N	f	1	0
24	2026-04-28 15:11:49.94791+07	2026-04-28 15:11:49.94791+07	\N	wakul rehana	\N	Lusin	4000	0	0	\N	\N	\N	\N	f	1	0
25	2026-04-28 15:11:49.94791+07	2026-04-28 15:11:49.94791+07	\N	baskom mawar	\N	Lusin	5800	0	0	\N	\N	\N	\N	f	1	0
27	2026-04-28 15:11:49.94791+07	2026-04-28 15:11:49.94791+07	\N	Wakul Rehana Super	\N	Lusin	4300	0	0	\N	\N	\N	\N	f	1	0
28	2026-04-28 15:11:49.94791+07	2026-04-28 15:11:49.94791+07	\N	baskom jago 12	\N	Lusin	5700	0	0	\N	\N	\N	\N	f	1	0
31	2026-04-28 15:11:49.94791+07	2026-04-28 15:11:49.94791+07	\N	wakul tradisi super	\N	Lusin	3600	0	0	\N	\N	\N	\N	f	1	0
32	2026-04-28 15:11:49.94791+07	2026-04-28 15:11:49.94791+07	\N	Baskom Bahtera TB	\N	Lusin	7100	0	0	\N	\N	\N	\N	f	1	0
33	2026-04-28 15:11:49.94791+07	2026-04-28 15:11:49.94791+07	\N	wakul kotak	\N	Lusin	5700	0	0	\N	\N	\N	\N	f	1	0
35	2026-04-28 15:11:49.94791+07	2026-04-28 15:11:49.94791+07	\N	wakul telur	\N	Lusin	2300	0	0	\N	\N	\N	\N	f	1	0
36	2026-04-28 15:11:49.94791+07	2026-04-28 15:11:49.94791+07	\N	telor japar	\N	Lusin	2100	0	0	\N	\N	\N	\N	f	1	0
26	2026-04-28 15:11:49.94791+07	2026-04-28 15:11:49.94791+07	\N	Baskom Bahtera	\N	Lusin	5900	0	0	\N	\N	\N	\N	f	1	0
51	2026-05-15 08:25:46.436535+07	2026-05-15 15:01:01.047918+07	\N	Baskom Panda Cerah		Lusin	10000	55	10	8ac53e80	Baskom Panda Cerah-8ac53e80	2026-05-15 08:25:46.420523+07	2026-05-15 15:01:01.045525+07	f	1	0
52	2026-05-16 11:48:27.025776+07	2026-05-16 11:48:27.025776+07	\N	Test Product AI		Pcspcs	1000	0	0	dc023a4e	Test Product AI-dc023a4e	2026-05-16 11:48:27.017395+07	2026-05-16 11:48:27.017395+07	f	1	0
30	2026-04-28 15:11:49.94791+07	2026-05-16 11:52:29.008962+07	\N	Baskom Durian	\N	Lusin	9200	50	11	\N	\N	\N	2026-05-16 11:52:29.005325+07	f	1	0
18	2026-04-28 15:11:49.94791+07	2026-05-31 13:31:11.60382+07	\N	bak kuping12	\N	Lusin	13000	72	15	\N	\N	\N	2026-05-31 13:31:11.479366+07	f	1	0
34	2026-04-28 15:11:49.94791+07	2026-05-31 13:32:59.934048+07	\N	Tradisi Super 30	\N	Lusin	3400	21	7	\N	\N	\N	2026-05-31 13:32:59.809424+07	f	1	0
21	2026-04-28 15:11:49.94791+07	2026-05-31 13:34:09.680507+07	\N	tradisi super 2	\N	Lusin	3100	21	7	\N	\N	\N	2026-05-31 13:34:09.555923+07	f	1	0
22	2026-04-28 15:11:49.94791+07	2026-05-31 13:35:49.413934+07	\N	baskom panda super	\N	Lusin	8400	52	10	\N	\N	\N	2026-05-31 13:35:49.288842+07	f	1	0
54	2026-06-08 16:57:38.693779+07	2026-06-08 16:57:38.693779+07	\N	[DEMO] Kantong HDPE Bening 30x50 (Tebal 0.03)	Kantong plastik high-density polyethylene bening kualitas premium (Demo).	Kg	23500	14.5	3.2	f1dbdd75	demo-kantong-hdpe-bening-30x50	2026-06-08 16:57:38.693512+07	2026-06-08 16:57:38.693512+07	t	1	0
55	2026-06-08 16:57:38.70283+07	2026-06-08 16:57:38.70283+07	\N	[DEMO] Sedotan Hitam Steril 6mm (Isi 500)	Sedotan plastik steril dibungkus kertas per pcs (Demo).	Pack	9000	1.2	0.6	5cc83015	demo-sedotan-hitam-steril-6mm	2026-06-08 16:57:38.693515+07	2026-06-08 16:57:38.693515+07	t	1	0
56	2026-06-08 16:57:38.706086+07	2026-06-08 16:57:38.706086+07	\N	[DEMO] Cup Plastik 16oz PP Tebal 7gr	Gelas cup plastik bahan Polypropylene (Demo).	Karton	135000	7	1.4	8808616b	demo-cup-plastik-16oz-pp-tebal	2026-06-08 16:57:38.693517+07	2026-06-08 16:57:38.693517+07	t	1	0
\.


--
-- Data for Name: payrolls; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.payrolls (id, created_at, updated_at, deleted_at, employee_id, payment_date, amount, description, attendance_count, daily_rate, is_demo) FROM stdin;
10	2026-05-02 10:22:39.743391+07	2026-05-02 10:22:39.743391+07	2026-05-02 10:30:26.088264+07	4	2026-05-02	300000.00		0	0.00	f
9	2026-05-02 10:22:39.438496+07	2026-05-02 10:22:39.438496+07	2026-05-02 10:30:30.830868+07	3	2026-05-02	315000.00		0	0.00	f
8	2026-05-02 10:22:38.975625+07	2026-05-02 10:22:38.975625+07	2026-05-02 10:30:33.913341+07	2	2026-05-02	345000.00		0	0.00	f
7	2026-05-02 10:22:38.467736+07	2026-05-02 10:22:38.467736+07	2026-05-02 10:30:36.617196+07	1	2026-05-02	360000.00		0	0.00	f
6	2026-05-02 10:20:21.130571+07	2026-05-02 10:20:21.130571+07	2026-05-02 10:30:38.862251+07	12	2026-05-02	315000.00		0	0.00	f
5	2026-05-02 09:55:45.388657+07	2026-05-02 09:55:45.388657+07	2026-05-02 10:30:41.527318+07	11	2026-05-02	405000.00		0	0.00	f
4	2026-05-02 09:54:27.611054+07	2026-05-02 09:54:27.611054+07	2026-05-02 10:30:44.397797+07	6	2026-05-02	285000.00		0	0.00	f
3	2026-05-02 09:19:15.089491+07	2026-05-02 09:19:15.089491+07	2026-05-02 10:30:46.905918+07	11	2026-05-02	405000.00		0	0.00	f
2	2026-05-02 00:05:32.4993+07	2026-05-02 00:05:32.4993+07	2026-05-02 10:30:49.4041+07	3	2026-05-01	52500.00		0	0.00	f
1	2026-05-01 23:11:52.593016+07	2026-05-01 23:11:52.593016+07	2026-05-02 10:30:52.199282+07	1	2026-05-01	60000.00		0	0.00	f
14	2026-05-02 10:32:21.110612+07	2026-05-02 10:32:21.110612+07	2026-05-02 10:32:44.884912+07	4	2026-05-02	300000.00		0	0.00	f
13	2026-05-02 10:32:20.680121+07	2026-05-02 10:32:20.680121+07	2026-05-02 10:33:31.159521+07	3	2026-05-02	315000.00		0	0.00	f
12	2026-05-02 10:32:20.344133+07	2026-05-02 10:32:20.344133+07	2026-05-02 10:33:35.19074+07	2	2026-05-02	345000.00		0	0.00	f
11	2026-05-02 10:32:19.772098+07	2026-05-02 10:32:19.772098+07	2026-05-02 10:33:37.89522+07	1	2026-05-02	360000.00		0	0.00	f
18	2026-05-02 10:34:46.898161+07	2026-05-02 10:34:46.898161+07	2026-05-02 10:35:20.1136+07	4	2026-05-02	300000.00		0	0.00	f
17	2026-05-02 10:34:46.288159+07	2026-05-02 10:34:46.288159+07	2026-05-02 10:35:23.125119+07	3	2026-05-02	315000.00		0	0.00	f
16	2026-05-02 10:34:45.80016+07	2026-05-02 10:34:45.80016+07	2026-05-02 10:35:25.592334+07	2	2026-05-02	345000.00		0	0.00	f
15	2026-05-02 10:34:45.122905+07	2026-05-02 10:34:45.122905+07	2026-05-02 10:35:28.515726+07	1	2026-05-02	360000.00		0	0.00	f
22	2026-05-02 10:49:08.924382+07	2026-05-02 10:49:08.924382+07	2026-05-02 10:52:29.009251+07	14	2026-05-02	157500.00		0	0.00	f
21	2026-05-02 10:49:08.440129+07	2026-05-02 10:49:08.440129+07	2026-05-02 10:52:31.593441+07	12	2026-05-02	262500.00		0	0.00	f
20	2026-05-02 10:49:07.950022+07	2026-05-02 10:49:07.950022+07	2026-05-02 10:52:34.018956+07	11	2026-05-02	405000.00		0	0.00	f
19	2026-05-02 10:49:07.429709+07	2026-05-02 10:49:07.429709+07	2026-05-02 10:52:36.294327+07	9	2026-05-02	450000.00		0	0.00	f
26	2026-05-02 11:49:06.368358+07	2026-05-02 11:49:06.368358+07	2026-05-02 11:50:16.631329+07	13	2026-05-02	315000.00		0	0.00	f
25	2026-05-02 11:49:05.880851+07	2026-05-02 11:49:05.880851+07	2026-05-02 11:50:22.768006+07	12	2026-05-02	315000.00		0	0.00	f
24	2026-05-02 11:49:05.257858+07	2026-05-02 11:49:05.257858+07	2026-05-02 11:50:25.426834+07	9	2026-05-02	450000.00		0	0.00	f
23	2026-05-02 11:49:04.876327+07	2026-05-02 11:49:04.876327+07	2026-05-02 11:50:31.58529+07	8	2026-05-02	450000.00		0	0.00	f
30	2026-05-02 11:51:21.538191+07	2026-05-02 11:51:21.538191+07	2026-05-02 12:07:13.727313+07	3	2026-05-02	105000.00		0	0.00	f
29	2026-05-02 11:51:20.906756+07	2026-05-02 11:51:20.906756+07	2026-05-02 12:07:19.371844+07	19	2026-05-02	157500.00		0	0.00	f
28	2026-05-02 11:51:20.361651+07	2026-05-02 11:51:20.361651+07	2026-05-02 12:07:21.819218+07	14	2026-05-02	157500.00		0	0.00	f
27	2026-05-02 11:51:19.870483+07	2026-05-02 11:51:19.870483+07	2026-05-02 12:11:45.510274+07	17	2026-05-02	157500.00		0	0.00	f
38	2026-05-02 12:39:17.611293+07	2026-05-02 12:39:17.611293+07	2026-05-02 12:51:59.545023+07	14	2026-05-02	157500.00		0	0.00	f
37	2026-05-02 12:39:17.147607+07	2026-05-02 12:39:17.147607+07	2026-05-02 12:52:01.944139+07	12	2026-05-02	210000.00		0	0.00	f
36	2026-05-02 12:39:16.660023+07	2026-05-02 12:39:16.660023+07	2026-05-02 12:52:04.386673+07	10	2026-05-02	220000.00		0	0.00	f
35	2026-05-02 12:39:16.181677+07	2026-05-02 12:39:16.181677+07	2026-05-02 12:52:06.57338+07	9	2026-05-02	300000.00		0	0.00	f
34	2026-05-02 12:12:59.902506+07	2026-05-02 12:12:59.902506+07	2026-05-02 12:52:08.920669+07	10	2026-05-02	165000.00		0	0.00	f
33	2026-05-02 12:12:59.449958+07	2026-05-02 12:12:59.449958+07	2026-05-02 12:52:11.181557+07	16	2026-05-02	157500.00		0	0.00	f
32	2026-05-02 12:12:58.95964+07	2026-05-02 12:12:58.95964+07	2026-05-02 12:52:13.712444+07	15	2026-05-02	262500.00		0	0.00	f
31	2026-05-02 12:12:58.512881+07	2026-05-02 12:12:58.512881+07	2026-05-02 12:52:15.858843+07	2	2026-05-02	287500.00		0	0.00	f
44	2026-05-02 12:58:18.770059+07	2026-05-02 12:58:18.770059+07	2026-05-02 12:59:02.574863+07	13	2026-05-02	315000.00	60k	0	0.00	f
43	2026-05-02 12:58:18.528733+07	2026-05-02 12:58:18.528733+07	2026-05-02 12:59:05.898873+07	2	2026-05-02	345000.00		0	0.00	f
42	2026-05-02 12:58:18.471427+07	2026-05-02 12:58:18.471427+07	2026-05-02 12:59:08.451901+07	8	2026-05-02	450000.00	bonus 30k	0	0.00	f
41	2026-05-02 12:58:18.062826+07	2026-05-02 12:58:18.062826+07	2026-05-02 12:59:11.249412+07	20	2026-05-02	300000.00		0	0.00	f
40	2026-05-02 12:58:17.591142+07	2026-05-02 12:58:17.591142+07	2026-05-02 12:59:13.926713+07	13	2026-05-02	315000.00	60k	0	0.00	f
39	2026-05-02 12:58:17.2902+07	2026-05-02 12:58:17.2902+07	2026-05-02 12:59:16.605269+07	8	2026-05-02	450000.00	bonus 30k	0	0.00	f
48	2026-05-02 13:06:42.833321+07	2026-05-02 13:06:42.833321+07	2026-05-02 13:09:08.349588+07	2	2026-05-02	345000.00	bonus : 42k extra : 25k tgl merah 15k	0	0.00	f
47	2026-05-02 13:06:42.351503+07	2026-05-02 13:06:42.351503+07	2026-05-02 13:09:11.191374+07	13	2026-05-02	315000.00	bonus : 30K extra 40k tgl merah 15k	0	0.00	f
46	2026-05-02 13:06:42.070608+07	2026-05-02 13:06:42.070608+07	2026-05-02 13:09:13.900631+07	20	2026-05-02	285000.00	bonus : 30k ekstra :20k tgl merah + 15k	0	0.00	f
45	2026-05-02 13:06:41.56223+07	2026-05-02 13:06:41.56223+07	2026-05-02 13:09:16.483+07	2	2026-05-02	345000.00	bonus: 35k ekstra 20k tgl merah :+15k	0	0.00	f
52	2026-05-02 13:13:04.396061+07	2026-05-02 13:13:04.396061+07	2026-05-02 13:15:23.919695+07	20	2026-05-02	237500.00	bonus : 30k extra : 20k tgl merah : 15k	0	0.00	f
51	2026-05-02 13:13:04.104863+07	2026-05-02 13:13:04.104863+07	2026-05-02 13:15:26.761752+07	13	2026-05-02	315000.00	bonus : 30k extra : 40k tgl merah : 15k	0	0.00	f
50	2026-05-02 13:13:03.41955+07	2026-05-02 13:13:03.41955+07	2026-05-02 13:15:29.540384+07	2	2026-05-02	345000.00	bonus : 42k extra : 25k tgl merah : 15k	0	0.00	f
49	2026-05-02 13:13:02.919893+07	2026-05-02 13:13:02.919893+07	2026-05-02 13:15:32.104555+07	8	2026-05-02	450000.00	bonus : 42k extra : 100k tgl merah : 15k	0	0.00	f
53	2026-05-27 02:12:52.55286+07	2026-05-27 02:12:52.55286+07	2026-05-27 02:13:30.035827+07	22	2026-05-26	83300.00	Denda dihapus (manual)	0	0.00	f
54	2026-05-27 02:17:25.028274+07	2026-05-27 02:17:25.028274+07	2026-05-27 02:17:48.337473+07	22	2026-05-26	83300.00	Denda dihapus (manual)	0	0.00	f
59	2026-05-30 12:58:05.944501+07	2026-05-30 12:58:05.944501+07	2026-05-30 15:18:27.520866+07	24	2026-05-30	50000.00	Auto-fill (Minggu ini)	0	0.00	f
58	2026-05-30 12:58:05.591778+07	2026-05-30 12:58:05.591778+07	2026-05-30 15:18:33.135014+07	18	2026-05-30	157500.00	Auto-fill (Minggu ini)	0	0.00	f
57	2026-05-30 12:58:05.235422+07	2026-05-30 12:58:05.235422+07	2026-05-30 15:18:38.815197+07	9	2026-05-30	225000.00	Auto-fill (Minggu ini)	0	0.00	f
56	2026-05-30 12:58:04.879857+07	2026-05-30 12:58:04.879857+07	2026-05-30 15:18:42.938252+07	17	2026-05-30	157500.00	Auto-fill (Minggu ini)	0	0.00	f
55	2026-05-30 12:58:04.513854+07	2026-05-30 12:58:04.513854+07	2026-05-30 15:18:46.792524+07	1	2026-05-30	180000.00	Auto-fill (Minggu ini)	0	0.00	f
\.


--
-- Data for Name: pembayarans; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.pembayarans (id, created_at, updated_at, deleted_at, invoice_id, tanggal_bayar, jumlah_bayar, keterangan) FROM stdin;
\.


--
-- Data for Name: pembelian_barangs; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.pembelian_barangs (id, created_at, updated_at, deleted_at, supplier, tanggal, keterangan, total_harga, cara_bayar, is_demo) FROM stdin;
\.


--
-- Data for Name: pembelian_items; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.pembelian_items (id, created_at, updated_at, deleted_at, pembelian_id, nama_barang, jumlah_lusin, kuantitas, unit, harga_satuan) FROM stdin;
\.


--
-- Data for Name: products; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.products (id, created_at, updated_at, deleted_at, master_item_id, title, unit, price, jumlah_lusin, quantity, currency, invoice_id, unique_id, slug, date_created, last_updated, is_khusus, harga_beli) FROM stdin;
150	2026-04-15 17:37:01.527239+07	2026-04-15 17:37:01.527289+07	\N	20	baskom jago	-	5400	50	110	Rp	150	0e02cbaec1ee	baskom-jago-0e02cbaec1ee	2026-04-15 17:37:01.527239+07	2026-04-15 17:37:01.527289+07	f	0
151	2026-04-15 17:37:01.531803+07	2026-04-15 17:37:01.531847+07	\N	26	Bahkom Bahtera	-	5900	50	30	Rp	150	a5ec365d8011	bahkom-bahtera-a5ec365d8011	2026-04-15 17:37:01.531803+07	2026-04-15 17:37:01.531847+07	f	0
152	2026-04-15 17:56:55.868638+07	2026-04-15 17:56:55.868682+07	\N	36	telor japar	-	2100	40	50	Rp	150	571b0c111d03	telor-japar-571b0c111d03	2026-04-15 17:56:55.868638+07	2026-04-15 17:56:55.868682+07	f	0
107	2026-04-13 15:22:01.332535+07	2026-04-17 13:58:46.135345+07	\N	23	smile 12	-	5700	50	10	Rp	114	2c1b3ef8dbde	smile-12-2c1b3ef8dbde	2026-04-13 15:22:01.332535+07	2026-04-17 13:58:46.135345+07	f	0
141	2026-04-15 09:14:38.203706+07	2026-04-21 21:32:03.855823+07	\N	34	Tradisi Super 30	-	3600	30	25	Rp	138	b470e494e550	tradisi-super-30-b470e494e550	2026-04-15 09:14:38.203706+07	2026-04-21 21:32:03.855823+07	f	0
91	2026-04-13 11:08:42.423736+07	2026-04-13 11:08:42.423807+07	\N	18	bak kuping ANISA 12	-	12000	30	80	Rp	110	fdd3ae4ff1b0	bak-kuping-anisa-12-fdd3ae4ff1b0	2026-04-13 11:08:42.423736+07	2026-04-13 11:08:42.423807+07	f	0
92	2026-04-13 11:08:42.428383+07	2026-04-13 11:08:42.428434+07	\N	19	wakul moris	-	5500	40	20	Rp	110	ae46e07b616e	wakul-moris-ae46e07b616e	2026-04-13 11:08:42.428383+07	2026-04-13 11:08:42.428434+07	f	0
89	2026-04-13 08:32:38.633484+07	2026-04-13 08:32:38.633535+07	\N	18	bak kuping ANISA 12	-	12000	30	80	Rp	109	3df804a6b534	bak-kuping-anisa-12-3df804a6b534	2026-04-13 08:32:38.633484+07	2026-04-13 08:32:38.633535+07	f	0
90	2026-04-13 08:32:38.636925+07	2026-04-13 08:32:38.636963+07	\N	16	baskom mawar	-	5800	50	10	Rp	109	d0ea04fea599	baskom-mawar-d0ea04fea599	2026-04-13 08:32:38.636925+07	2026-04-13 08:32:38.636963+07	f	0
94	2026-04-13 13:13:22.880616+07	2026-04-13 13:13:22.88069+07	\N	20	baskom jago	-	5600	50	40	Rp	111	ef92c1609c19	baskom-jago-ef92c1609c19	2026-04-13 13:13:22.880616+07	2026-04-13 13:13:22.88069+07	f	0
95	2026-04-13 13:14:03.08462+07	2026-04-13 13:14:03.084687+07	\N	19	wakul moris	-	5700	40	40	Rp	111	81593b97335a	wakul-moris-81593b97335a	2026-04-13 13:14:03.08462+07	2026-04-13 13:14:03.084687+07	f	0
96	2026-04-13 13:30:03.823582+07	2026-04-13 13:30:03.823672+07	\N	21	tradisi super 2	-	3100	20	40	Rp	112	c6112d4dc3a6	tradisi-super-2-c6112d4dc3a6	2026-04-13 13:30:03.823582+07	2026-04-13 13:30:03.823672+07	f	0
97	2026-04-13 13:30:58.318572+07	2026-04-13 13:30:58.318645+07	\N	22	baskom panda super	-	7200	40	4	Rp	112	f20ee407581a	baskom-panda-super-f20ee407581a	2026-04-13 13:30:58.318572+07	2026-04-13 13:30:58.318645+07	f	0
98	2026-04-13 13:31:50.532726+07	2026-04-13 13:31:50.532825+07	\N	24	wakul rehana	-	4000	50	10	Rp	112	a98c073b3c99	wakul-rehana-a98c073b3c99	2026-04-13 13:31:50.532726+07	2026-04-13 13:31:50.532825+07	f	0
99	2026-04-13 13:36:43.580569+07	2026-04-13 13:36:43.580649+07	\N	25	baskom mawar	-	5800	50	7	Rp	112	c87f8316e58e	baskom-mawar-c87f8316e58e	2026-04-13 13:36:43.580569+07	2026-04-13 13:36:43.580649+07	f	0
100	2026-04-13 13:37:35.004634+07	2026-04-13 13:37:35.00473+07	\N	23	smile 12	-	5400	50	5	Rp	112	504614f38715	smile-12-504614f38715	2026-04-13 13:37:35.004634+07	2026-04-13 13:37:35.00473+07	f	0
101	2026-04-13 14:27:27.651075+07	2026-04-13 14:27:27.651142+07	\N	26	Bahkom Bahtera	-	5900	50	5	Rp	113	43789f437e72	bahkom-bahtera-43789f437e72	2026-04-13 14:27:27.651075+07	2026-04-13 14:27:27.651142+07	f	0
102	2026-04-13 14:27:27.658458+07	2026-04-13 14:27:27.658504+07	\N	17	baskom bahtera TM	-	5700	50	5	Rp	113	c4491786e907	baskom-bahtera-tm-c4491786e907	2026-04-13 14:27:27.658458+07	2026-04-13 14:27:27.658504+07	f	0
103	2026-04-13 14:27:27.666857+07	2026-04-13 14:27:27.666898+07	\N	27	Wakul Rehana Super	-	4300	50	12	Rp	113	4d21f27a4126	wakul-rehana-super-4d21f27a4126	2026-04-13 14:27:27.666857+07	2026-04-13 14:27:27.666898+07	f	0
104	2026-04-13 14:27:27.670895+07	2026-04-13 14:27:27.670942+07	\N	25	baskom mawar	-	6100	50	3	Rp	113	835e5bad1628	baskom-mawar-835e5bad1628	2026-04-13 14:27:27.670895+07	2026-04-13 14:27:27.670942+07	f	0
105	2026-04-13 15:18:56.230642+07	2026-04-13 15:18:56.230686+07	\N	22	baskom panda super	-	7500	40	10	Rp	114	9f845c4e6274	baskom-panda-super-9f845c4e6274	2026-04-13 15:18:56.230642+07	2026-04-13 15:18:56.230686+07	f	0
108	2026-04-13 15:22:01.335504+07	2026-04-13 15:22:01.335538+07	\N	28	baskom jago 12	-	5700	50	3	Rp	114	0b1a104ea3c4	baskom-jago-12-0b1a104ea3c4	2026-04-13 15:22:01.335504+07	2026-04-13 15:22:01.335538+07	f	0
109	2026-04-13 15:32:12.646669+07	2026-04-13 15:32:12.646729+07	\N	18	bak kuping ANISA 12	-	12300	30	100	Rp	115	122b8a74011a	bak-kuping-anisa-12-122b8a74011a	2026-04-13 15:32:12.646669+07	2026-04-13 15:32:12.646729+07	f	0
110	2026-04-13 15:32:12.651263+07	2026-04-13 15:32:12.651304+07	\N	16	baskom mawar	-	6100	50	10	Rp	115	461f504f375f	baskom-mawar-461f504f375f	2026-04-13 15:32:12.651263+07	2026-04-13 15:32:12.651304+07	f	0
111	2026-04-13 15:32:12.654111+07	2026-04-13 15:32:12.654148+07	\N	29	Baskom TM	-	6100	50	10	Rp	115	2e8ee9881c1a	baskom-tm-2e8ee9881c1a	2026-04-13 15:32:12.654111+07	2026-04-13 15:32:12.654148+07	f	0
112	2026-04-13 15:32:12.656645+07	2026-04-13 15:32:12.656681+07	\N	13	baskom panda	-	7800	40	10	Rp	115	65dfd24637b0	baskom-panda-65dfd24637b0	2026-04-13 15:32:12.656645+07	2026-04-13 15:32:12.656681+07	f	0
113	2026-04-13 15:32:12.659098+07	2026-04-13 15:32:12.659127+07	\N	20	baskom jago	-	5800	50	4	Rp	115	9b67adb57beb	baskom-jago-9b67adb57beb	2026-04-13 15:32:12.659098+07	2026-04-13 15:32:12.659127+07	f	0
114	2026-04-13 15:32:12.661689+07	2026-04-13 15:32:12.661721+07	\N	19	wakul moris	-	5800	40	10	Rp	115	f6696ce1c33a	wakul-moris-f6696ce1c33a	2026-04-13 15:32:12.661689+07	2026-04-13 15:32:12.661721+07	f	0
115	2026-04-13 15:33:50.62072+07	2026-04-13 15:33:50.620767+07	\N	30	Baskom Durian	-	8500	40	10	Rp	115	334d50b49069	baskom-durian-334d50b49069	2026-04-13 15:33:50.62072+07	2026-04-13 15:33:50.620767+07	f	0
123	2026-04-13 19:00:07.550033+07	2026-04-13 19:00:07.550134+07	\N	32	Baskom Bahtera TB	-	7100	50	50	Rp	132	865d5c6326f1	baskom-bahtera-tb-865d5c6326f1	2026-04-13 19:00:07.550033+07	2026-04-13 19:00:07.550134+07	f	0
124	2026-04-13 19:00:07.558266+07	2026-04-13 19:00:07.558477+07	\N	31	wakul tradisi super	-	3600	20	10	Rp	132	5a684c3ab2b0	wakul-tradisi-super-5a684c3ab2b0	2026-04-13 19:00:07.558266+07	2026-04-13 19:00:07.558477+07	f	0
125	2026-04-13 19:06:24.403438+07	2026-04-13 19:06:24.403523+07	\N	33	wakul kotak	-	5700	20	30	Rp	132	f295179a15ef	wakul-kotak-f295179a15ef	2026-04-13 19:06:24.403438+07	2026-04-13 19:06:24.403523+07	f	0
128	2026-04-14 12:20:45.007747+07	2026-04-14 12:20:45.007815+07	\N	29	Baskom TM	-	6750	50	15	Rp	134	63020ceed7f9	baskom-tm-63020ceed7f9	2026-04-14 12:20:45.007747+07	2026-04-14 12:20:45.007815+07	f	0
129	2026-04-14 12:20:45.011493+07	2026-04-14 12:20:45.011548+07	\N	25	baskom mawar	-	6800	50	20	Rp	134	5979eb87bffb	baskom-mawar-5979eb87bffb	2026-04-14 12:20:45.011493+07	2026-04-14 12:20:45.011548+07	f	0
130	2026-04-14 12:20:45.014409+07	2026-04-14 12:20:45.014458+07	\N	30	Baskom Durian	-	8600	40	5	Rp	134	e9b11a19b1ec	baskom-durian-e9b11a19b1ec	2026-04-14 12:20:45.014409+07	2026-04-14 12:20:45.014458+07	f	0
131	2026-04-14 12:57:57.807441+07	2026-04-14 12:57:57.807526+07	\N	29	Baskom TM	-	6500	50	10	Rp	135	f0b65eecc616	baskom-tm-f0b65eecc616	2026-04-14 12:57:57.807441+07	2026-04-14 12:57:57.807526+07	f	0
132	2026-04-14 12:57:57.816207+07	2026-04-14 12:57:57.816281+07	\N	25	baskom mawar	-	7100	50	15	Rp	135	f7546516aa9d	baskom-mawar-f7546516aa9d	2026-04-14 12:57:57.816207+07	2026-04-14 12:57:57.816281+07	f	0
133	2026-04-14 12:57:57.82288+07	2026-04-14 12:57:57.822945+07	\N	23	smile 12	-	6400	50	10	Rp	135	89817187abb3	smile-12-89817187abb3	2026-04-14 12:57:57.82288+07	2026-04-14 12:57:57.822945+07	f	0
134	2026-04-14 12:59:31.475091+07	2026-04-14 12:59:31.475173+07	\N	34	Tradisi Super 30	-	3400	30	30	Rp	135	478139c40375	tradisi-super-30-478139c40375	2026-04-14 12:59:31.475091+07	2026-04-14 12:59:31.475173+07	f	0
135	2026-04-14 13:32:15.75184+07	2026-04-14 13:32:15.751901+07	\N	20	baskom jago	-	6200	50	98	Rp	132	476a53a54f0d	baskom-jago-476a53a54f0d	2026-04-14 13:32:15.75184+07	2026-04-14 13:32:15.751901+07	f	0
137	2026-04-15 08:42:08.767097+07	2026-04-15 08:42:08.767153+07	\N	23	smile 12	-	6700	50	20	Rp	138	3e510bc77ddc	smile-12-3e510bc77ddc	2026-04-15 08:42:08.767097+07	2026-04-15 08:42:08.767153+07	f	0
138	2026-04-15 08:42:34.979623+07	2026-04-15 08:42:34.979681+07	\N	16	baskom mawar	-	7100	50	5	Rp	138	f21c0b239468	baskom-mawar-f21c0b239468	2026-04-15 08:42:34.979623+07	2026-04-15 08:42:34.979681+07	f	0
140	2026-04-15 08:44:12.601412+07	2026-04-15 08:44:12.601473+07	\N	35	wakul telur	-	2300	20	84	Rp	138	0c5d77c6c877	wakul-telur-0c5d77c6c877	2026-04-15 08:44:12.601412+07	2026-04-15 08:44:12.601473+07	f	0
153	2026-04-15 17:56:55.873758+07	2026-04-15 17:56:55.873803+07	\N	37	telor tali	-	3200	40	50	Rp	150	9048090f9ae6	telor-tali-9048090f9ae6	2026-04-15 17:56:55.873758+07	2026-04-15 17:56:55.873803+07	f	0
154	2026-04-15 17:56:55.87876+07	2026-04-15 17:56:55.8788+07	\N	31	wakul tradisi super	-	3200	40	25	Rp	150	b2404a735812	wakul-tradisi-super-b2404a735812	2026-04-15 17:56:55.87876+07	2026-04-15 17:56:55.8788+07	f	0
158	2026-04-16 18:15:41.82706+07	2026-04-16 18:15:41.827124+07	\N	31	wakul tradisi super	-	3100	30	30	Rp	155	bc6288361ed2	wakul-tradisi-super-bc6288361ed2	2026-04-16 18:15:41.82706+07	2026-04-16 18:15:41.827124+07	f	0
159	2026-04-16 18:15:41.830132+07	2026-04-16 18:15:41.83017+07	\N	23	smile 12	-	5400	50	10	Rp	155	5e33f6cd553b	smile-12-5e33f6cd553b	2026-04-16 18:15:41.830132+07	2026-04-16 18:15:41.83017+07	f	0
160	2026-04-16 18:19:08.767471+07	2026-04-16 18:19:08.76753+07	\N	21	tradisi super 2	-	3100	20	50	Rp	156	1201710281ea	tradisi-super-2-1201710281ea	2026-04-16 18:19:08.767471+07	2026-04-16 18:19:08.76753+07	f	0
161	2026-04-16 18:21:31.358661+07	2026-04-16 18:21:31.358704+07	\N	22	baskom panda super	-	7200	40	50	Rp	158	ef4417723e65	baskom-panda-super-ef4417723e65	2026-04-16 18:21:31.358661+07	2026-04-16 18:21:31.358704+07	f	0
106	2026-04-13 15:22:01.32961+07	2026-04-17 13:57:48.793232+07	\N	29	Baskom TM	-	5900	50	10	Rp	114	8cfe593e75af	baskom-tm-8cfe593e75af	2026-04-13 15:22:01.32961+07	2026-04-17 13:57:48.793232+07	f	0
162	2026-04-17 14:03:38.041905+07	2026-04-17 14:03:38.041946+07	\N	17	baskom bahtera TM	-	5500	50	43	Rp	160	d15f0b4dd21e	baskom-bahtera-tm-d15f0b4dd21e	2026-04-17 14:03:38.041905+07	2026-04-17 14:03:38.041946+07	f	0
163	2026-04-17 14:03:38.048674+07	2026-04-17 14:03:38.04871+07	\N	28	baskom jago 12	-	5300	50	5	Rp	160	082981805e0e	baskom-jago-12-082981805e0e	2026-04-17 14:03:38.048674+07	2026-04-17 14:03:38.04871+07	f	0
164	2026-04-17 14:03:38.052619+07	2026-04-17 14:03:38.052648+07	\N	23	smile 12	-	5400	50	5	Rp	160	b92d8b1c6d32	smile-12-b92d8b1c6d32	2026-04-17 14:03:38.052619+07	2026-04-17 14:03:38.052648+07	f	0
165	2026-04-17 15:35:23.272346+07	2026-04-17 15:35:23.272394+07	\N	18	bak kuping12	-	13000	30	5	Rp	149	9476a578ceb4	bak-kuping12-9476a578ceb4	2026-04-17 15:35:23.272346+07	2026-04-17 15:35:23.272394+07	f	0
166	2026-04-17 15:35:23.276372+07	2026-04-17 15:35:23.276409+07	\N	23	smile 12	-	5400	50	5	Rp	149	ad30baa308c2	smile-12-ad30baa308c2	2026-04-17 15:35:23.276372+07	2026-04-17 15:35:23.276409+07	f	0
167	2026-04-17 15:35:23.278972+07	2026-04-17 15:35:23.279001+07	\N	17	baskom bahtera TM	-	5500	50	3	Rp	149	f1326f4849a4	baskom-bahtera-tm-f1326f4849a4	2026-04-17 15:35:23.278972+07	2026-04-17 15:35:23.279001+07	f	0
168	2026-04-17 15:38:33.973786+07	2026-04-17 15:38:33.973831+07	\N	17	baskom bahtera TM	-	5800	50	15	Rp	163	cba8915698ac	baskom-bahtera-tm-cba8915698ac	2026-04-17 15:38:33.973786+07	2026-04-17 15:38:33.973831+07	f	0
169	2026-04-17 15:38:33.976633+07	2026-04-17 15:38:33.976675+07	\N	13	baskom panda	-	7500	40	15	Rp	163	506417ec2cb1	baskom-panda-506417ec2cb1	2026-04-17 15:38:33.976633+07	2026-04-17 15:38:33.976675+07	f	0
170	2026-04-17 15:38:33.979239+07	2026-04-17 15:38:33.979266+07	\N	30	Baskom Durian	-	8200	40	15	Rp	163	c8689e9c597d	baskom-durian-c8689e9c597d	2026-04-17 15:38:33.979239+07	2026-04-17 15:38:33.979266+07	f	0
171	2026-04-17 15:38:33.981989+07	2026-04-17 15:38:33.982017+07	\N	20	baskom jago	-	5500	50	10	Rp	163	f54f6d129637	baskom-jago-f54f6d129637	2026-04-17 15:38:33.981989+07	2026-04-17 15:38:33.982017+07	f	0
172	2026-04-17 15:40:24.049751+07	2026-04-17 15:40:24.049792+07	\N	18	bak kuping12	-	12000	30	85	Rp	164	17b519f0b497	bak-kuping12-17b519f0b497	2026-04-17 15:40:24.049751+07	2026-04-17 15:40:24.049792+07	f	0
173	2026-04-17 15:42:44.99413+07	2026-04-17 15:42:44.994184+07	\N	38	Smile 14	Lusin	8200	40	20	Rp	165	a14009a24085	smile-14-a14009a24085	2026-04-17 15:42:44.99413+07	2026-04-17 15:42:44.994184+07	f	0
174	2026-04-17 15:45:41.930755+07	2026-04-17 15:45:41.930813+07	\N	17	baskom bahtera TM	-	5500	50	48	Rp	166	12da7dbc98b4	baskom-bahtera-tm-12da7dbc98b4	2026-04-17 15:45:41.930755+07	2026-04-17 15:45:41.930813+07	f	0
175	2026-04-17 15:51:58.479934+07	2026-04-17 15:51:58.480066+07	\N	36	telor japar	-	2100	20	100	Rp	167	f05135291124	telor-japar-f05135291124	2026-04-17 15:51:58.479934+07	2026-04-17 15:51:58.480066+07	f	0
176	2026-04-17 15:51:58.491689+07	2026-04-17 15:51:58.491816+07	\N	24	wakul rehana	-	4000	20	50	Rp	167	c3cbbac0aefe	wakul-rehana-c3cbbac0aefe	2026-04-17 15:51:58.491689+07	2026-04-17 15:51:58.491816+07	f	0
177	2026-04-17 15:51:58.499748+07	2026-04-17 15:51:58.499838+07	\N	26	Bahkom Bahtera	-	5600	50	15	Rp	167	35a5e59e2ac0	bahkom-bahtera-35a5e59e2ac0	2026-04-17 15:51:58.499748+07	2026-04-17 15:51:58.499838+07	f	0
178	2026-04-18 08:52:52.104105+07	2026-04-18 09:10:56.738508+07	\N	22	baskom panda super	-	8400	40	10	Rp	169	823558c7fc68	baskom-panda-super-823558c7fc68	2026-04-18 08:52:52.104105+07	2026-04-18 09:10:56.738508+07	f	0
179	2026-04-18 09:15:02.081452+07	2026-04-18 09:15:02.081555+07	\N	20	baskom jago	-	7000	50	3	Rp	169	4bfe49304491	baskom-jago-4bfe49304491	2026-04-18 09:15:02.081452+07	2026-04-18 09:15:02.081555+07	f	0
180	2026-04-18 09:15:02.091306+07	2026-04-18 09:15:02.091394+07	\N	17	baskom bahtera TM	-	7000	50	10	Rp	169	9654aa5cf13f	baskom-bahtera-tm-9654aa5cf13f	2026-04-18 09:15:02.091306+07	2026-04-18 09:15:02.091394+07	f	0
181	2026-04-18 09:15:02.099143+07	2026-04-18 09:15:02.099232+07	\N	18	bak kuping12	-	14000	30	5	Rp	169	0d543cc9da0e	bak-kuping12-0d543cc9da0e	2026-04-18 09:15:02.099143+07	2026-04-18 09:15:02.099232+07	f	0
183	2026-04-19 17:28:56.841153+07	2026-04-19 17:28:56.841195+07	\N	25	baskom mawar	-	7200	50	20	Rp	170	0deb43ebab06	baskom-mawar-0deb43ebab06	2026-04-19 17:28:56.841153+07	2026-04-19 17:28:56.841195+07	f	0
184	2026-04-19 17:28:56.847212+07	2026-04-19 17:28:56.847256+07	\N	17	baskom bahtera TM	-	7000	50	10	Rp	170	ebdc66900d09	baskom-bahtera-tm-ebdc66900d09	2026-04-19 17:28:56.847212+07	2026-04-19 17:28:56.847256+07	f	0
182	2026-04-18 09:16:41.9616+07	2026-04-20 08:17:20.46189+07	\N	39	tradisi cerah	Lusin	5000	30	5	Rp	169	ce30a26aac50	tradisi-cerah-ce30a26aac50	2026-04-18 09:16:41.9616+07	2026-04-20 08:17:20.46189+07	f	0
185	2026-04-20 08:33:34.144417+07	2026-04-20 08:33:34.144516+07	\N	22	baskom panda super	-	8400	40	30	Rp	172	41ff5cfe41d2	baskom-panda-super-41ff5cfe41d2	2026-04-20 08:33:34.144417+07	2026-04-20 08:33:34.144516+07	f	0
186	2026-04-20 08:33:34.152978+07	2026-04-20 08:33:34.153073+07	\N	30	Baskom Durian	-	8900	40	5	Rp	172	8265cd64a155	baskom-durian-8265cd64a155	2026-04-20 08:33:34.152978+07	2026-04-20 08:33:34.153073+07	f	0
187	2026-04-20 08:33:34.160491+07	2026-04-20 08:33:34.160569+07	\N	26	Bahkom Bahtera	-	7500	50	5	Rp	172	dd28fb01e30d	bahkom-bahtera-dd28fb01e30d	2026-04-20 08:33:34.160491+07	2026-04-20 08:33:34.160569+07	f	0
188	2026-04-20 08:33:34.169457+07	2026-04-20 08:33:34.169532+07	\N	16	baskom mawar	-	7300	50	10	Rp	172	0607a011be7c	baskom-mawar-0607a011be7c	2026-04-20 08:33:34.169457+07	2026-04-20 08:33:34.169532+07	f	0
189	2026-04-20 08:36:24.117648+07	2026-04-20 08:36:24.117741+07	\N	26	Bahkom Bahtera	-	5900	50	5	Rp	173	2191bca1fba5	bahkom-bahtera-2191bca1fba5	2026-04-20 08:36:24.117648+07	2026-04-20 08:36:24.117741+07	f	0
190	2026-04-20 08:36:24.128512+07	2026-04-20 08:36:24.128597+07	\N	17	baskom bahtera TM	-	5400	50	5	Rp	173	5eb4714b25e9	baskom-bahtera-tm-5eb4714b25e9	2026-04-20 08:36:24.128512+07	2026-04-20 08:36:24.128597+07	f	0
191	2026-04-20 08:38:56.143056+07	2026-04-20 08:38:56.143163+07	\N	17	baskom bahtera TM	-	5500	50	10	Rp	174	94bb9f524eff	baskom-bahtera-tm-94bb9f524eff	2026-04-20 08:38:56.143056+07	2026-04-20 08:38:56.143163+07	f	0
192	2026-04-20 08:38:56.150829+07	2026-04-20 08:38:56.150902+07	\N	28	baskom jago 12	-	5300	50	10	Rp	174	af103ff79e47	baskom-jago-12-af103ff79e47	2026-04-20 08:38:56.150829+07	2026-04-20 08:38:56.150902+07	f	0
193	2026-04-20 08:38:56.159089+07	2026-04-20 08:38:56.159185+07	\N	25	baskom mawar	-	5800	50	5	Rp	174	875c56a13840	baskom-mawar-875c56a13840	2026-04-20 08:38:56.159089+07	2026-04-20 08:38:56.159185+07	f	0
194	2026-04-20 08:40:46.273148+07	2026-04-20 08:40:46.273232+07	\N	18	bak kuping12	-	13000	30	2	Rp	175	6a52b3e7b1d0	bak-kuping12-6a52b3e7b1d0	2026-04-20 08:40:46.273148+07	2026-04-20 08:40:46.273232+07	f	0
195	2026-04-20 09:02:09.024885+07	2026-04-20 09:02:09.024928+07	\N	17	baskom bahtera TM	-	5700	50	11	Rp	179	97e6f63fb8be	baskom-bahtera-tm-97e6f63fb8be	2026-04-20 09:02:09.024885+07	2026-04-20 09:02:09.024928+07	f	0
196	2026-04-20 09:02:09.029872+07	2026-04-20 09:02:09.029909+07	\N	22	baskom panda super	-	7200	40	10	Rp	179	ef4eb8c60b12	baskom-panda-super-ef4eb8c60b12	2026-04-20 09:02:09.029872+07	2026-04-20 09:02:09.029909+07	f	0
200	2026-04-20 11:35:57.164122+07	2026-04-20 11:35:57.164175+07	\N	17	baskom bahtera TM	-	6750	50	15	Rp	181	3eebf179353f	baskom-bahtera-tm-3eebf179353f	2026-04-20 11:35:57.164122+07	2026-04-20 11:35:57.164175+07	f	0
201	2026-04-20 11:35:57.174923+07	2026-04-20 11:35:57.174993+07	\N	16	baskom mawar	-	7050	50	15	Rp	181	4cf850dc514b	baskom-mawar-4cf850dc514b	2026-04-20 11:35:57.174923+07	2026-04-20 11:35:57.174993+07	f	0
202	2026-04-20 11:35:57.180485+07	2026-04-20 11:35:57.180521+07	\N	23	smile 12	-	6650	50	15	Rp	181	7b4b20315509	smile-12-7b4b20315509	2026-04-20 11:35:57.180485+07	2026-04-20 11:35:57.180521+07	f	0
197	2026-04-20 11:30:38.336837+07	2026-04-20 11:30:38.336896+07	2026-05-29 22:19:37.99138+07	40	Baskom Barca	Lusin	7250	50	15	Rp	180	63f730819cff	baskom-barca-63f730819cff	2026-04-20 11:30:38.336837+07	2026-04-20 11:30:38.336896+07	f	0
198	2026-04-20 11:30:38.342423+07	2026-04-20 11:30:38.342459+07	2026-05-29 22:19:37.99138+07	18	bak kuping12	-	14750	30	6	Rp	180	16f935ba8d16	bak-kuping12-16f935ba8d16	2026-04-20 11:30:38.342423+07	2026-04-20 11:30:38.342459+07	f	0
207	2026-04-20 11:44:21.727582+07	2026-04-20 11:44:21.727633+07	\N	17	baskom bahtera TM	-	6750	50	15	Rp	183	303e213e8e0f	baskom-bahtera-tm-303e213e8e0f	2026-04-20 11:44:21.727582+07	2026-04-20 11:44:21.727633+07	f	0
208	2026-04-20 11:44:21.737006+07	2026-04-20 11:44:21.73705+07	\N	16	baskom mawar	-	7050	50	15	Rp	183	a866051992c6	baskom-mawar-a866051992c6	2026-04-20 11:44:21.737006+07	2026-04-20 11:44:21.73705+07	f	0
209	2026-04-20 11:44:21.741892+07	2026-04-20 11:44:21.74192+07	\N	23	smile 12	-	6650	50	15	Rp	183	e3e195d2982f	smile-12-e3e195d2982f	2026-04-20 11:44:21.741892+07	2026-04-20 11:44:21.74192+07	f	0
210	2026-04-20 12:02:58.440146+07	2026-04-20 12:02:58.4402+07	\N	41	Wakul Tanggok	Lusin	5800	50	1	Rp	167	9a655df17f85	wakul-tanggok-9a655df17f85	2026-04-20 12:02:58.440146+07	2026-04-20 12:02:58.4402+07	f	0
211	2026-04-20 12:06:48.811152+07	2026-04-20 12:06:48.811201+07	\N	26	Bahkom Bahtera	-	5600	50	5	Rp	184	c7b7875afece	bahkom-bahtera-c7b7875afece	2026-04-20 12:06:48.811152+07	2026-04-20 12:06:48.811201+07	f	0
212	2026-04-20 12:06:48.818024+07	2026-04-20 12:06:48.818062+07	\N	31	wakul tradisi super	-	3100	40	30	Rp	184	a06e62231689	wakul-tradisi-super-a06e62231689	2026-04-20 12:06:48.818024+07	2026-04-20 12:06:48.818062+07	f	0
213	2026-04-20 12:06:48.822367+07	2026-04-20 12:06:48.822403+07	\N	39	tradisi cerah	Lusin	4200	30	20	Rp	184	44c24ef50126	tradisi-cerah-44c24ef50126	2026-04-20 12:06:48.822367+07	2026-04-20 12:06:48.822403+07	f	0
214	2026-04-20 12:06:48.829293+07	2026-04-20 12:06:48.82933+07	\N	36	telor japar	-	2100	20	200	Rp	184	2b87c93df5c7	telor-japar-2b87c93df5c7	2026-04-20 12:06:48.829293+07	2026-04-20 12:06:48.82933+07	f	0
215	2026-04-20 12:13:40.70712+07	2026-04-20 12:13:40.707169+07	\N	21	tradisi super 2	-	3100	40	32	Rp	185	26fb97ac4a8e	tradisi-super-2-26fb97ac4a8e	2026-04-20 12:13:40.70712+07	2026-04-20 12:13:40.707169+07	f	0
216	2026-04-20 12:13:40.712783+07	2026-04-20 12:13:40.712822+07	\N	13	baskom panda	-	7200	40	7	Rp	185	9dadc44071ca	baskom-panda-9dadc44071ca	2026-04-20 12:13:40.712783+07	2026-04-20 12:13:40.712822+07	f	0
217	2026-04-20 12:17:41.578683+07	2026-04-20 12:17:41.578732+07	\N	21	tradisi super 2	-	3100	40	32	Rp	186	e95865d37e47	tradisi-super-2-e95865d37e47	2026-04-20 12:17:41.578683+07	2026-04-20 12:17:41.578732+07	f	0
218	2026-04-20 12:17:41.583707+07	2026-04-20 12:17:41.58375+07	\N	22	baskom panda super	-	7200	40	7	Rp	186	92709b9abbf0	baskom-panda-super-92709b9abbf0	2026-04-20 12:17:41.583707+07	2026-04-20 12:17:41.58375+07	f	0
219	2026-04-20 12:17:41.587961+07	2026-04-20 12:17:41.588006+07	\N	17	baskom bahtera TM	-	5400	50	5	Rp	186	c014fe9a0f3e	baskom-bahtera-tm-c014fe9a0f3e	2026-04-20 12:17:41.587961+07	2026-04-20 12:17:41.588006+07	f	0
220	2026-04-20 12:17:41.592703+07	2026-04-20 12:17:41.592739+07	\N	24	wakul rehana	-	4000	50	10	Rp	186	684a6b175579	wakul-rehana-684a6b175579	2026-04-20 12:17:41.592703+07	2026-04-20 12:17:41.592739+07	f	0
221	2026-04-20 12:17:41.596476+07	2026-04-20 12:17:41.596517+07	\N	16	baskom mawar	-	5850	50	3	Rp	186	d8c4b4a3ff49	baskom-mawar-d8c4b4a3ff49	2026-04-20 12:17:41.596476+07	2026-04-20 12:17:41.596517+07	f	0
222	2026-04-20 12:20:58.370956+07	2026-04-20 12:20:58.371025+07	\N	23	smile 12	-	5400	50	15	Rp	187	f4209cb9a028	smile-12-f4209cb9a028	2026-04-20 12:20:58.370956+07	2026-04-20 12:20:58.371025+07	f	0
223	2026-04-20 12:20:58.376282+07	2026-04-20 12:20:58.376328+07	\N	17	baskom bahtera TM	-	5400	50	10	Rp	187	2482fb8db375	baskom-bahtera-tm-2482fb8db375	2026-04-20 12:20:58.376282+07	2026-04-20 12:20:58.376328+07	f	0
224	2026-04-20 12:20:58.380581+07	2026-04-20 12:20:58.38062+07	\N	22	baskom panda super	-	7200	50	4	Rp	187	1874a2da90fb	baskom-panda-super-1874a2da90fb	2026-04-20 12:20:58.380581+07	2026-04-20 12:20:58.38062+07	f	0
225	2026-04-20 12:20:58.385282+07	2026-04-20 12:20:58.385313+07	\N	31	wakul tradisi super	-	3100	40	8	Rp	187	62a36314824d	wakul-tradisi-super-62a36314824d	2026-04-20 12:20:58.385282+07	2026-04-20 12:20:58.385313+07	f	0
226	2026-04-20 12:26:54.926593+07	2026-04-20 12:26:54.926636+07	\N	37	telor tali	Lusin	2100	20	70	Rp	188	6f632df678b9	telor-tali-6f632df678b9	2026-04-20 12:26:54.926593+07	2026-04-20 12:26:54.926636+07	f	0
227	2026-04-20 12:42:13.408148+07	2026-04-20 12:42:13.408212+07	\N	18	bak kuping12	-	13000	50	3	Rp	190	b8bdc41abbda	bak-kuping12-b8bdc41abbda	2026-04-20 12:42:13.408148+07	2026-04-20 12:42:13.408212+07	f	0
228	2026-04-20 12:42:13.419182+07	2026-04-20 12:42:13.419237+07	\N	42	Baskom Rotan	Lusin	8400	40	6	Rp	190	00ab3688529e	baskom-rotan-00ab3688529e	2026-04-20 12:42:13.419182+07	2026-04-20 12:42:13.419237+07	f	0
229	2026-04-20 12:42:13.425577+07	2026-04-20 12:42:13.425627+07	\N	26	Bahkom Bahtera	-	6500	50	5	Rp	190	7b309b9ba89c	bahkom-bahtera-7b309b9ba89c	2026-04-20 12:42:13.425577+07	2026-04-20 12:42:13.425627+07	f	0
230	2026-04-20 12:42:13.433089+07	2026-04-20 12:42:13.433132+07	\N	20	baskom jago	-	5800	50	5	Rp	190	9c977df0da96	baskom-jago-9c977df0da96	2026-04-20 12:42:13.433089+07	2026-04-20 12:42:13.433132+07	f	0
231	2026-04-20 12:42:13.438511+07	2026-04-20 12:42:13.438561+07	\N	23	smile 12	-	5800	50	5	Rp	190	81d091f02c40	smile-12-81d091f02c40	2026-04-20 12:42:13.438511+07	2026-04-20 12:42:13.438561+07	f	0
232	2026-04-20 12:42:13.442898+07	2026-04-20 12:42:13.442944+07	\N	36	telor japar	-	2400	20	5	Rp	190	7ff0960a8d5d	telor-japar-7ff0960a8d5d	2026-04-20 12:42:13.442898+07	2026-04-20 12:42:13.442944+07	f	0
233	2026-04-20 12:48:49.217716+07	2026-04-20 12:48:49.21777+07	\N	17	baskom bahtera TM	-	5400	50	15	Rp	191	38130e3940a8	baskom-bahtera-tm-38130e3940a8	2026-04-20 12:48:49.217716+07	2026-04-20 12:48:49.21777+07	f	0
234	2026-04-20 12:48:49.223447+07	2026-04-20 12:48:49.223484+07	\N	23	smile 12	-	5400	50	3	Rp	191	fe7b27e62c29	smile-12-fe7b27e62c29	2026-04-20 12:48:49.223447+07	2026-04-20 12:48:49.223484+07	f	0
235	2026-04-20 12:50:42.76462+07	2026-04-20 12:50:42.764669+07	\N	26	Bahkom Bahtera	-	6100	50	51	Rp	192	8299d82592bd	bahkom-bahtera-8299d82592bd	2026-04-20 12:50:42.76462+07	2026-04-20 12:50:42.764669+07	f	0
236	2026-04-20 12:50:42.769319+07	2026-04-20 12:50:42.769365+07	\N	31	wakul tradisi super	-	3300	40	100	Rp	192	820b76d28798	wakul-tradisi-super-820b76d28798	2026-04-20 12:50:42.769319+07	2026-04-20 12:50:42.769365+07	f	0
237	2026-04-20 12:56:37.952612+07	2026-04-20 12:56:37.95266+07	\N	20	baskom jago	-	5600	50	100	Rp	193	b3dca834903e	baskom-jago-b3dca834903e	2026-04-20 12:56:37.952612+07	2026-04-20 12:56:37.95266+07	f	0
238	2026-04-20 13:32:46.214996+07	2026-04-20 13:32:46.215047+07	\N	36	telor japar	-	2100	20	50	Rp	194	79f00e14a83e	telor-japar-79f00e14a83e	2026-04-20 13:32:46.214996+07	2026-04-20 13:32:46.215047+07	f	0
239	2026-04-20 13:32:46.21974+07	2026-04-20 13:32:46.219775+07	\N	31	wakul tradisi super	-	3300	20	150	Rp	194	e6aeeb53643f	wakul-tradisi-super-e6aeeb53643f	2026-04-20 13:32:46.21974+07	2026-04-20 13:32:46.219775+07	f	0
240	2026-04-20 18:36:57.254135+07	2026-04-20 18:36:57.254183+07	\N	43	Wakul Mawar Super	Lusin	3600	50	20	Rp	195	36617920434e	wakul-mawar-super-36617920434e	2026-04-20 18:36:57.254135+07	2026-04-20 18:36:57.254183+07	f	0
241	2026-04-20 18:39:54.391348+07	2026-04-20 18:39:54.391389+07	\N	38	Smile 14	Lusin	8400	40	15	Rp	195	5683209e656b	smile-14-5683209e656b	2026-04-20 18:39:54.391348+07	2026-04-20 18:39:54.391389+07	f	0
242	2026-04-20 18:44:34.319006+07	2026-04-20 18:44:34.319055+07	\N	45	Wakul Morris Super	Lusin	5700	40	15	Rp	195	8b72a72a5ff2	wakul-morris-super-8b72a72a5ff2	2026-04-20 18:44:34.319006+07	2026-04-20 18:44:34.319055+07	f	0
243	2026-04-20 18:44:34.32425+07	2026-04-20 18:44:34.324292+07	\N	22	baskom panda super	-	7250	40	10	Rp	195	722a459f52fc	baskom-panda-super-722a459f52fc	2026-04-20 18:44:34.32425+07	2026-04-20 18:44:34.324292+07	f	0
244	2026-04-20 18:44:34.330031+07	2026-04-20 18:44:34.33007+07	\N	17	baskom bahtera TM	-	5800	50	15	Rp	195	995634e6e91c	baskom-bahtera-tm-995634e6e91c	2026-04-20 18:44:34.330031+07	2026-04-20 18:44:34.33007+07	f	0
245	2026-04-20 18:44:34.334601+07	2026-04-20 18:44:34.334637+07	\N	23	smile 12	-	5700	50	10	Rp	195	30b03b79b75c	smile-12-30b03b79b75c	2026-04-20 18:44:34.334601+07	2026-04-20 18:44:34.334637+07	f	0
246	2026-04-20 18:50:53.459669+07	2026-04-20 18:50:53.459723+07	\N	20	baskom jago	-	5400	50	107	Rp	196	6bf28d847a43	baskom-jago-6bf28d847a43	2026-04-20 18:50:53.459669+07	2026-04-20 18:50:53.459723+07	f	0
247	2026-04-20 18:50:53.464687+07	2026-04-20 18:50:53.464731+07	\N	26	Bahkom Bahtera	-	5900	50	30	Rp	196	29c011670396	bahkom-bahtera-29c011670396	2026-04-20 18:50:53.464687+07	2026-04-20 18:50:53.464731+07	f	0
248	2026-04-20 18:50:53.470271+07	2026-04-20 18:50:53.47031+07	\N	13	baskom panda	-	7200	40	10	Rp	196	db9e2b55b9e3	baskom-panda-db9e2b55b9e3	2026-04-20 18:50:53.470271+07	2026-04-20 18:50:53.47031+07	f	0
249	2026-04-20 18:50:53.474833+07	2026-04-20 18:50:53.474872+07	\N	36	telor japar	-	2100	20	58	Rp	196	5291ee737b25	telor-japar-5291ee737b25	2026-04-20 18:50:53.474833+07	2026-04-20 18:50:53.474872+07	f	0
250	2026-04-20 18:50:53.478768+07	2026-04-20 18:50:53.478799+07	\N	37	telor tali	Lusin	3200	20	128	Rp	196	68d146af2205	telor-tali-68d146af2205	2026-04-20 18:50:53.478768+07	2026-04-20 18:50:53.478799+07	f	0
252	2026-04-20 18:54:32.516737+07	2026-04-20 18:54:42.676477+07	\N	17	baskom bahtera TM	-	5850	50	50	Rp	197	0e543178f0a6	baskom-bahtera-tm-0e543178f0a6	2026-04-20 18:54:32.516737+07	2026-04-20 18:54:42.676477+07	f	0
253	2026-04-20 18:55:47.48321+07	2026-04-20 18:55:47.48328+07	\N	16	baskom mawar	-	5900	50	20	Rp	197	545c4d71743d	baskom-mawar-545c4d71743d	2026-04-20 18:55:47.48321+07	2026-04-20 18:55:47.48328+07	f	0
254	2026-04-20 18:56:46.601706+07	2026-04-20 18:56:46.601766+07	\N	45	Wakul Morris Super	Lusin	5750	40	25	Rp	197	a4934cfc75e9	wakul-morris-super-a4934cfc75e9	2026-04-20 18:56:46.601706+07	2026-04-20 18:56:46.601766+07	f	0
255	2026-04-20 18:58:34.725574+07	2026-04-20 18:58:34.725626+07	\N	26	Baskom Bahtera	-	6100	50	60	Rp	197	102cf89c7ff7	baskom-bahtera-102cf89c7ff7	2026-04-20 18:58:34.725574+07	2026-04-20 18:58:34.725626+07	f	0
256	2026-04-20 19:05:36.457295+07	2026-04-20 19:05:36.457348+07	\N	23	smile 12	-	6700	50	25	Rp	198	8fbe43f563e4	smile-12-8fbe43f563e4	2026-04-20 19:05:36.457295+07	2026-04-20 19:05:36.457348+07	f	0
257	2026-04-20 19:05:36.462711+07	2026-04-20 19:06:11.213586+07	\N	16	baskom mawar	-	7100	50	5	Rp	198	898a83a2c8e4	baskom-mawar-898a83a2c8e4	2026-04-20 19:05:36.462711+07	2026-04-20 19:06:11.213586+07	f	0
258	2026-04-20 19:08:53.784296+07	2026-04-20 19:08:53.78434+07	\N	26	Baskom Bahtera	-	7500	50	5	Rp	198	a58a2b686524	baskom-bahtera-a58a2b686524	2026-04-20 19:08:53.784296+07	2026-04-20 19:08:53.78434+07	f	0
259	2026-04-20 19:08:53.789854+07	2026-04-20 19:08:53.789885+07	\N	30	Baskom Durian	-	8900	40	5	Rp	198	2c574d987ff7	baskom-durian-2c574d987ff7	2026-04-20 19:08:53.789854+07	2026-04-20 19:08:53.789885+07	f	0
260	2026-04-21 16:51:03.164359+07	2026-04-21 16:51:03.164415+07	\N	36	telor japar	-	2600	20	70	Rp	200	9a62ba0ba35b	telor-japar-9a62ba0ba35b	2026-04-21 16:51:03.164359+07	2026-04-21 16:51:03.164415+07	f	0
261	2026-04-21 16:51:03.168239+07	2026-04-21 16:51:03.168276+07	\N	37	telor tali	Lusin	3700	20	80	Rp	200	652d2a1988fd	telor-tali-652d2a1988fd	2026-04-21 16:51:03.168239+07	2026-04-21 16:51:03.168276+07	f	0
262	2026-04-21 16:51:03.171992+07	2026-04-21 16:51:03.172045+07	\N	23	smile 12	-	6900	50	2	Rp	200	88657a23c015	smile-12-88657a23c015	2026-04-21 16:51:03.171992+07	2026-04-21 16:51:03.172045+07	f	0
263	2026-04-21 16:51:03.174927+07	2026-04-21 16:51:03.17496+07	\N	26	Baskom Bahtera	-	7300	50	50	Rp	200	f13f2946b435	baskom-bahtera-f13f2946b435	2026-04-21 16:51:03.174927+07	2026-04-21 16:51:03.17496+07	f	0
264	2026-04-21 16:51:03.177449+07	2026-04-21 16:51:03.177479+07	\N	20	baskom jago	-	7000	50	115	Rp	200	bae39e72b60e	baskom-jago-bae39e72b60e	2026-04-21 16:51:03.177449+07	2026-04-21 16:51:03.177479+07	f	0
265	2026-04-21 16:51:03.182178+07	2026-04-21 16:51:03.182208+07	\N	22	baskom panda super	-	8500	40	6	Rp	200	d5f87a9f71cc	baskom-panda-super-d5f87a9f71cc	2026-04-21 16:51:03.182178+07	2026-04-21 16:51:03.182208+07	f	0
266	2026-04-21 20:51:42.900314+07	2026-04-21 20:51:42.900457+07	\N	34	Tradisi Super 30	-	3600	30	30	Rp	202	4b2b85bf759e	tradisi-super-30-4b2b85bf759e	2026-04-21 20:51:42.900314+07	2026-04-21 20:51:42.900457+07	f	0
267	2026-04-21 20:51:42.911593+07	2026-04-21 20:51:42.911705+07	\N	36	telor japar	-	2500	20	27	Rp	202	cddfe957b585	telor-japar-cddfe957b585	2026-04-21 20:51:42.911593+07	2026-04-21 20:51:42.911705+07	f	0
268	2026-04-21 20:51:42.921354+07	2026-04-21 20:51:42.921505+07	\N	17	baskom bahtera TM	-	7000	50	5	Rp	202	342b9668ae8a	baskom-bahtera-tm-342b9668ae8a	2026-04-21 20:51:42.921354+07	2026-04-21 20:51:42.921505+07	f	0
269	2026-04-21 20:51:42.929166+07	2026-04-21 20:51:42.929251+07	\N	13	baskom panda	-	8400	40	5	Rp	202	9ea97c5ab9a9	baskom-panda-9ea97c5ab9a9	2026-04-21 20:51:42.929166+07	2026-04-21 20:51:42.929251+07	f	0
270	2026-04-23 08:40:46.826401+07	2026-04-23 08:40:46.826449+07	\N	26	Baskom Bahtera	-	7700	50	4	Rp	203	b6b5190a86ef	baskom-bahtera-b6b5190a86ef	2026-04-23 08:40:46.826401+07	2026-04-23 08:40:46.826449+07	f	0
271	2026-04-23 08:40:46.838358+07	2026-04-23 08:40:46.838403+07	\N	16	baskom mawar	-	7500	50	1	Rp	203	abd3bd257f88	baskom-mawar-abd3bd257f88	2026-04-23 08:40:46.838358+07	2026-04-23 08:40:46.838403+07	f	0
272	2026-04-24 12:52:58.003257+07	2026-04-24 12:52:58.003299+07	\N	24	wakul rehana	-	4400	50	20	Rp	205	3e8ab106b3e5	wakul-rehana-3e8ab106b3e5	2026-04-24 12:52:58.003257+07	2026-04-24 12:52:58.003299+07	f	0
273	2026-04-24 12:52:58.007528+07	2026-04-24 12:52:58.007566+07	\N	17	baskom bahtera TM	-	7000	50	5	Rp	205	365380d361ed	baskom-bahtera-tm-365380d361ed	2026-04-24 12:52:58.007528+07	2026-04-24 12:52:58.007566+07	f	0
274	2026-04-27 10:01:50.767327+07	2026-04-27 10:01:50.767388+07	\N	18	bak kuping12	-	14300	30	14	Rp	206	76a146dbd17d	bak-kuping12-76a146dbd17d	2026-04-27 10:01:50.767327+07	2026-04-27 10:01:50.767388+07	f	0
275	2026-04-27 10:01:50.771484+07	2026-04-27 10:01:50.771532+07	\N	16	baskom mawar	-	7600	50	25	Rp	206	3f7c0b20829c	baskom-mawar-3f7c0b20829c	2026-04-27 10:01:50.771484+07	2026-04-27 10:01:50.771532+07	f	0
276	2026-04-27 10:01:50.776686+07	2026-04-27 10:01:50.776731+07	\N	17	baskom bahtera TM	-	9250	40	40	Rp	206	8b6b7939857c	baskom-bahtera-tm-8b6b7939857c	2026-04-27 10:01:50.776686+07	2026-04-27 10:01:50.776731+07	f	0
277	2026-04-27 10:37:35.958759+07	2026-04-27 10:41:15.134392+07	\N	17	baskom bahtera TM	-	7250	50	90	Rp	207	980706cda58b	baskom-bahtera-tm-980706cda58b	2026-04-27 10:37:35.958759+07	2026-04-27 10:41:15.134392+07	f	0
278	2026-04-28 08:46:32.735882+07	2026-04-28 08:46:32.735941+07	\N	16	baskom mawar	-	7200	50	15	Rp	209	8aa523519b37	baskom-mawar-8aa523519b37	2026-04-28 08:46:32.735882+07	2026-04-28 08:46:32.735941+07	f	0
279	2026-04-28 08:46:32.739371+07	2026-04-28 08:46:32.739405+07	\N	17	baskom bahtera TM	-	7000	50	10	Rp	209	660c6aac3430	baskom-bahtera-tm-660c6aac3430	2026-04-28 08:46:32.739371+07	2026-04-28 08:46:32.739405+07	f	0
280	2026-04-28 08:46:32.74282+07	2026-04-28 08:46:32.742864+07	\N	20	baskom jago	-	7000	50	10	Rp	209	3ef6efd54def	baskom-jago-3ef6efd54def	2026-04-28 08:46:32.74282+07	2026-04-28 08:46:32.742864+07	f	0
281	2026-04-28 10:09:55.252988+07	2026-04-28 10:09:55.253034+07	\N	26	Baskom Bahtera	-	7700	50	20	Rp	210	296064e108e5	baskom-bahtera-296064e108e5	2026-04-28 10:09:55.252988+07	2026-04-28 10:09:55.253034+07	f	0
282	2026-04-28 10:09:55.256648+07	2026-04-28 10:09:55.256678+07	\N	13	baskom panda	-	8600	40	20	Rp	210	7ae71a14be1f	baskom-panda-7ae71a14be1f	2026-04-28 10:09:55.256648+07	2026-04-28 10:09:55.256678+07	f	0
283	2026-04-28 10:09:55.259933+07	2026-04-28 10:09:55.25997+07	\N	16	baskom mawar	-	7400	50	10	Rp	210	ec45f602afe9	baskom-mawar-ec45f602afe9	2026-04-28 10:09:55.259933+07	2026-04-28 10:09:55.25997+07	f	0
449	2026-05-26 12:49:31.531519+07	2026-05-26 12:49:31.531519+07	2026-05-26 15:09:13.461233+07	51	Baskom Panda Cerah	Lusin	10000	40	5	Rp	259	3b637d61	BMP-2605-010-c94477e0-ec660179	2026-05-26 12:49:31.531497+07	2026-05-26 12:49:31.531498+07	f	0
450	2026-05-26 12:49:31.541446+07	2026-05-26 12:49:31.541446+07	2026-05-26 15:09:13.461233+07	37	telor tali	Lusin	3700	20	80	Rp	259	10ab9f60	BMP-2605-010-c94477e0-8aae9605	2026-05-26 12:49:31.541433+07	2026-05-26 12:49:31.541433+07	f	0
469	2026-05-26 16:09:04.305897+07	2026-05-26 16:09:04.305897+07	2026-05-26 16:10:18.649988+07	20	baskom jago	Lusin	7000	50	120	Rp	259	ffda5ad1	BMP-2605-010-c94477e0-3a7ff5a3	2026-05-26 16:09:04.305869+07	2026-05-26 16:09:04.305869+07	f	0
470	2026-05-26 16:09:04.318649+07	2026-05-26 16:09:04.318649+07	2026-05-26 16:10:18.649988+07	26	Baskom Bahtera	Lusin	7300	50	20	Rp	259	5af460db	BMP-2605-010-c94477e0-53d09059	2026-05-26 16:09:04.318637+07	2026-05-26 16:09:04.318637+07	f	0
471	2026-05-26 16:09:04.326857+07	2026-05-26 16:09:04.326857+07	2026-05-26 16:10:18.649988+07	23	smile 12	Lusin	6900	50	15	Rp	259	c9af326e	BMP-2605-010-c94477e0-815cad8f	2026-05-26 16:09:04.326848+07	2026-05-26 16:09:04.326848+07	f	0
472	2026-05-26 16:09:04.335192+07	2026-05-26 16:09:04.335192+07	2026-05-26 16:10:18.649988+07	38	Smile 14	Lusin	9300	50	36	Rp	259	c242e094	BMP-2605-010-c94477e0-5068389a	2026-05-26 16:09:04.335181+07	2026-05-26 16:09:04.335181+07	f	0
473	2026-05-26 16:09:04.343409+07	2026-05-26 16:09:04.343409+07	2026-05-26 16:10:18.649988+07	51	Baskom Panda Cerah	Lusin	10000	40	5	Rp	259	4a946c46	BMP-2605-010-c94477e0-2ca701f1	2026-05-26 16:09:04.343398+07	2026-05-26 16:09:04.343398+07	f	0
474	2026-05-26 16:09:04.351663+07	2026-05-26 16:09:04.351663+07	2026-05-26 16:10:18.649988+07	37	telor tali	Lusin	3700	20	97	Rp	259	2bd463c0	BMP-2605-010-c94477e0-d8907998	2026-05-26 16:09:04.351654+07	2026-05-26 16:09:04.351654+07	t	0
445	2026-05-26 12:49:31.490687+07	2026-05-26 12:49:31.490687+07	2026-05-26 15:09:13.461233+07	20	baskom jago	Lusin	7000	50	120	Rp	259	fd627645	BMP-2605-010-c94477e0-1efe04e2	2026-05-26 12:49:31.49066+07	2026-05-26 12:49:31.49066+07	f	0
446	2026-05-26 12:49:31.50083+07	2026-05-26 12:49:31.50083+07	2026-05-26 15:09:13.461233+07	26	Baskom Bahtera	Lusin	7300	50	20	Rp	259	9f0b71ee	BMP-2605-010-c94477e0-dba9467f	2026-05-26 12:49:31.500807+07	2026-05-26 12:49:31.500807+07	f	0
284	2026-05-01 15:19:25.310481+07	2026-05-01 15:19:25.310481+07	\N	46	BMP	Lusin	7000	50	60	Rp	211	b67d475c	slug_284_36010001-617e-47b9-abbd-e22cc2bd2bf3	2026-05-01 15:19:25.31046+07	2026-05-01 15:19:25.310461+07	f	0
447	2026-05-26 12:49:31.511162+07	2026-05-26 12:49:31.511162+07	2026-05-26 15:09:13.461233+07	23	smile 12	Lusin	6900	50	15	Rp	259	9dc25f15	BMP-2605-010-c94477e0-38fe0abe	2026-05-26 12:49:31.511143+07	2026-05-26 12:49:31.511143+07	f	0
303	2026-05-01 22:35:20.311456+07	2026-05-01 22:35:20.311456+07	2026-05-01 22:38:55.165669+07	18	bak kuping12	-	13000	30	50	Rp	239	b58240e8		2026-05-01 22:35:20.311432+07	2026-05-01 22:35:20.311432+07	f	0
448	2026-05-26 12:49:31.521355+07	2026-05-26 12:49:31.521355+07	2026-05-26 15:09:13.461233+07	38	Smile 14	Lusin	9300	50	36	Rp	259	0c261109	BMP-2605-010-c94477e0-a44c6692	2026-05-26 12:49:31.52133+07	2026-05-26 12:49:31.52133+07	f	0
451	2026-05-26 15:09:13.478433+07	2026-05-26 15:09:13.478433+07	2026-05-26 15:15:53.948091+07	20	baskom jago	Lusin	7000	50	120	Rp	259	39d6fdd5	BMP-2605-010-c94477e0-c82b583e	2026-05-26 15:09:13.478407+07	2026-05-26 15:09:13.478407+07	f	0
452	2026-05-26 15:09:13.492984+07	2026-05-26 15:09:13.492984+07	2026-05-26 15:15:53.948091+07	26	Baskom Bahtera	Lusin	7300	50	20	Rp	259	2bbc8daa	BMP-2605-010-c94477e0-c6c02c6d	2026-05-26 15:09:13.492971+07	2026-05-26 15:09:13.492971+07	f	0
453	2026-05-26 15:09:13.501465+07	2026-05-26 15:09:13.501465+07	2026-05-26 15:15:53.948091+07	23	smile 12	Lusin	6900	50	15	Rp	259	a8c3510e	BMP-2605-010-c94477e0-f18cfd05	2026-05-26 15:09:13.501441+07	2026-05-26 15:09:13.501441+07	f	0
454	2026-05-26 15:09:13.510436+07	2026-05-26 15:09:13.510436+07	2026-05-26 15:15:53.948091+07	38	Smile 14	Lusin	9300	50	36	Rp	259	3d5759d0	BMP-2605-010-c94477e0-8ff37ab1	2026-05-26 15:09:13.510427+07	2026-05-26 15:09:13.510427+07	f	0
455	2026-05-26 15:09:13.518743+07	2026-05-26 15:09:13.518743+07	2026-05-26 15:15:53.948091+07	51	Baskom Panda Cerah	Lusin	10000	40	5	Rp	259	38673f32	BMP-2605-010-c94477e0-21c28198	2026-05-26 15:09:13.518733+07	2026-05-26 15:09:13.518733+07	f	0
456	2026-05-26 15:09:13.527015+07	2026-05-26 15:09:13.527015+07	2026-05-26 15:15:53.948091+07	37	telor tali	Lusin	3700	20	97	Rp	259	3327ab4d	BMP-2605-010-c94477e0-8ae2abdf	2026-05-26 15:09:13.527008+07	2026-05-26 15:09:13.527008+07	f	0
475	2026-05-26 16:10:18.65843+07	2026-05-26 16:10:18.65843+07	\N	20	baskom jago	Lusin	7000	50	120	Rp	259	56ee39b5	BMP-2605-010-c94477e0-012d413b	2026-05-26 16:10:18.6584+07	2026-05-26 16:10:18.6584+07	f	0
311	2026-05-02 08:10:56.047782+07	2026-05-02 08:10:56.047782+07	2026-05-02 08:11:44.643853+07	18	bak kuping12	-	13000	30	50	Rp	247	0bffea00	BMP-2605-001-424e63c6	2026-05-02 08:10:56.047752+07	2026-05-02 08:10:56.047752+07	f	0
312	2026-05-02 08:10:56.056647+07	2026-05-02 08:10:56.056647+07	2026-05-02 08:11:44.643853+07	16	baskom mawar	-	7400	50	15	Rp	247	c523aa2c	BMP-2605-001-efa0f611	2026-05-02 08:10:56.056607+07	2026-05-02 08:10:56.056607+07	f	0
313	2026-05-02 08:10:56.062702+07	2026-05-02 08:10:56.062702+07	2026-05-02 08:11:44.643853+07	46	BMP	Lusin	7200	50	15	Rp	247	ad99a22e	BMP-2605-001-fb11c340	2026-05-02 08:10:56.062666+07	2026-05-02 08:10:56.062666+07	f	0
315	2026-05-02 08:13:22.511983+07	2026-05-02 08:13:22.511983+07	\N	18	bak kuping12	-	13000	30	50	Rp	248	f934cfbf	BMP-2605-001-88774ad6	2026-05-02 08:13:22.511943+07	2026-05-02 08:13:22.511943+07	f	0
316	2026-05-02 08:13:22.521757+07	2026-05-02 08:13:22.521757+07	\N	16	baskom mawar	-	7400	50	15	Rp	248	ac8ecac7	BMP-2605-001-431c12b9	2026-05-02 08:13:22.521714+07	2026-05-02 08:13:22.521714+07	f	0
317	2026-05-02 08:13:22.529131+07	2026-05-02 08:13:22.529131+07	\N	46	BMP	Lusin	7200	50	15	Rp	248	72221160	BMP-2605-001-654e7f6c	2026-05-02 08:13:22.529097+07	2026-05-02 08:13:22.529097+07	f	0
318	2026-05-02 08:13:22.535316+07	2026-05-02 08:13:22.535316+07	\N	20	baskom jago	-	7000	50	7	Rp	248	c13d582a	BMP-2605-001-f789cb57	2026-05-02 08:13:22.535284+07	2026-05-02 08:13:22.535284+07	f	0
476	2026-05-26 16:10:18.666647+07	2026-05-26 16:10:18.666647+07	\N	26	Baskom Bahtera	Lusin	7300	50	20	Rp	259	bc72e2ff	BMP-2605-010-c94477e0-04f90fb5	2026-05-26 16:10:18.666629+07	2026-05-26 16:10:18.66663+07	f	0
477	2026-05-26 16:10:18.674973+07	2026-05-26 16:10:18.674973+07	\N	23	smile 12	Lusin	6900	50	15	Rp	259	4e847528	BMP-2605-010-c94477e0-adfe71b3	2026-05-26 16:10:18.674965+07	2026-05-26 16:10:18.674965+07	f	0
478	2026-05-26 16:10:18.683051+07	2026-05-26 16:10:18.683051+07	\N	38	Smile 14	Lusin	9300	50	36	Rp	259	8cced3e9	BMP-2605-010-c94477e0-90526fb3	2026-05-26 16:10:18.683044+07	2026-05-26 16:10:18.683044+07	f	0
479	2026-05-26 16:10:18.691072+07	2026-05-26 16:10:18.691072+07	\N	51	Baskom Panda Cerah	Lusin	10000	40	5	Rp	259	16ceba72	BMP-2605-010-c94477e0-2bf44bea	2026-05-26 16:10:18.691062+07	2026-05-26 16:10:18.691062+07	f	0
480	2026-05-26 16:10:18.699084+07	2026-05-26 16:10:18.699084+07	\N	37	telor tali	Lusin	3700	20	97	Rp	259	e67f381d	BMP-2605-010-c94477e0-faf102af	2026-05-26 16:10:18.699075+07	2026-05-26 16:10:18.699075+07	t	0
319	2026-05-02 08:14:05.040282+07	2026-05-02 08:14:05.040282+07	2026-05-02 14:59:33.926586+07	16	baskom mawar	-	7000	50	90	Rp	249	3b956583	BMP-2605-001-88ac3661	2026-05-02 08:14:05.040245+07	2026-05-02 08:14:05.040246+07	f	0
323	2026-05-02 16:00:45.593243+07	2026-05-02 16:00:45.593243+07	2026-05-02 16:02:00.943303+07	17	baskom bahtera TM	-	7000	50	15	Rp	250	ef8d5962	BMP-2605-001-d1aebb79-f8652a16	2026-05-02 16:00:45.593216+07	2026-05-02 16:00:45.593216+07	f	0
324	2026-05-02 16:00:45.6062+07	2026-05-02 16:00:45.6062+07	2026-05-02 16:02:00.943303+07	20	baskom jago	-	5600	50	10	Rp	250	1776aaf2	BMP-2605-001-d1aebb79-ef8e8456	2026-05-02 16:00:45.606177+07	2026-05-02 16:00:45.606177+07	f	0
325	2026-05-02 16:00:45.614751+07	2026-05-02 16:00:45.614751+07	2026-05-02 16:02:00.943303+07	23	smile 12	-	5400	50	10	Rp	250	cb3c76eb	BMP-2605-001-d1aebb79-a0a5b06c	2026-05-02 16:00:45.614728+07	2026-05-02 16:00:45.614728+07	f	0
326	2026-05-02 16:00:45.623213+07	2026-05-02 16:00:45.623213+07	2026-05-02 16:02:00.943303+07	16	baskom mawar	-	5800	50	10	Rp	250	bb09b72d	BMP-2605-001-d1aebb79-622ca9aa	2026-05-02 16:00:45.623197+07	2026-05-02 16:00:45.623197+07	f	0
327	2026-05-02 16:00:45.631679+07	2026-05-02 16:00:45.631679+07	2026-05-02 16:02:00.943303+07	13	baskom panda	-	7000	40	2	Rp	250	f5012cfb	BMP-2605-001-d1aebb79-d071ca1b	2026-05-02 16:00:45.631657+07	2026-05-02 16:00:45.631657+07	f	0
199	2026-04-20 11:30:38.34676+07	2026-04-20 11:30:38.346788+07	2026-05-29 22:19:37.99138+07	13	baskom panda	-	7160	50	5	Rp	180	23b66706d24c	baskom-panda-23b66706d24c	2026-04-20 11:30:38.34676+07	2026-04-20 11:30:38.346788+07	f	0
328	2026-05-02 16:02:00.956716+07	2026-05-02 16:02:00.956716+07	2026-05-02 16:02:48.676334+07	17	baskom bahtera TM	-	7000	50	15	Rp	250	5c285a1a	BMP-2605-001-d1aebb79-8f2605a7	2026-05-02 16:02:00.956694+07	2026-05-02 16:02:00.956694+07	f	0
329	2026-05-02 16:02:00.967783+07	2026-05-02 16:02:00.967783+07	2026-05-02 16:02:48.676334+07	20	baskom jago	-	5600	50	10	Rp	250	b86a516b	BMP-2605-001-d1aebb79-73413e27	2026-05-02 16:02:00.967763+07	2026-05-02 16:02:00.967763+07	f	0
330	2026-05-02 16:02:00.976433+07	2026-05-02 16:02:00.976433+07	2026-05-02 16:02:48.676334+07	23	smile 12	-	5400	50	10	Rp	250	2a209057	BMP-2605-001-d1aebb79-a0577e70	2026-05-02 16:02:00.976419+07	2026-05-02 16:02:00.976419+07	f	0
331	2026-05-02 16:02:00.989941+07	2026-05-02 16:02:00.989941+07	2026-05-02 16:02:48.676334+07	16	baskom mawar	-	5800	50	10	Rp	250	54c87b7c	BMP-2605-001-d1aebb79-d314bc80	2026-05-02 16:02:00.989925+07	2026-05-02 16:02:00.989925+07	f	0
493	2026-05-29 22:19:38.002465+07	2026-05-29 22:19:38.002465+07	2026-05-29 22:21:10.730731+07	17	baskom bahtera TM	Lusin	6800	50	15	Rp	180	3c5d3e35	bmp-0426-030-7ed262bd1c5a-cb1b92e1	2026-05-29 22:19:38.00242+07	2026-05-29 22:19:38.00242+07	f	0
494	2026-05-29 22:19:38.011479+07	2026-05-29 22:19:38.011479+07	2026-05-29 22:21:10.730731+07	18	bak kuping12	Lusin	13600	30	6	Rp	180	5bd4cc8b	bmp-0426-030-7ed262bd1c5a-7592a352	2026-05-29 22:19:38.011428+07	2026-05-29 22:19:38.011428+07	f	0
495	2026-05-29 22:19:38.017016+07	2026-05-29 22:19:38.017016+07	2026-05-29 22:21:10.730731+07	13	baskom panda	Lusin	7900	50	5	Rp	180	58ec281c	bmp-0426-030-7ed262bd1c5a-a92f9764	2026-05-29 22:19:38.016865+07	2026-05-29 22:19:38.016865+07	f	0
496	2026-05-29 22:21:10.737016+07	2026-05-29 22:21:10.737016+07	2026-05-29 22:22:02.734156+07	17	baskom bahtera TM	Lusin	6800	50	15	Rp	180	78e85cf9	bmp-0426-030-7ed262bd1c5a-ecf42c29	2026-05-29 22:21:10.736946+07	2026-05-29 22:21:10.736946+07	f	0
497	2026-05-29 22:21:10.742952+07	2026-05-29 22:21:10.742952+07	2026-05-29 22:22:02.734156+07	18	bak kuping12	Lusin	13600	30	6	Rp	180	eac22850	bmp-0426-030-7ed262bd1c5a-32aaaffb	2026-05-29 22:21:10.742894+07	2026-05-29 22:21:10.742894+07	f	0
498	2026-05-29 22:21:10.748816+07	2026-05-29 22:21:10.748816+07	2026-05-29 22:22:02.734156+07	13	baskom panda	Lusin	7900	50	5	Rp	180	2d093054	bmp-0426-030-7ed262bd1c5a-ad516b6f	2026-05-29 22:21:10.748715+07	2026-05-29 22:21:10.748715+07	f	0
332	2026-05-02 16:02:00.999798+07	2026-05-02 16:02:00.999798+07	2026-05-02 16:02:48.676334+07	13	baskom panda	-	8400	40	2	Rp	250	03ff8ff9	BMP-2605-001-d1aebb79-f441ce4f	2026-05-02 16:02:00.999778+07	2026-05-02 16:02:00.999778+07	f	0
333	2026-05-02 16:02:48.68496+07	2026-05-02 16:02:48.68496+07	2026-05-02 16:02:59.006673+07	17	baskom bahtera TM	-	7000	50	15	Rp	250	169ef05b	BMP-2605-001-d1aebb79-81b46eed	2026-05-02 16:02:48.684935+07	2026-05-02 16:02:48.684935+07	f	0
334	2026-05-02 16:02:48.696638+07	2026-05-02 16:02:48.696638+07	2026-05-02 16:02:59.006673+07	20	baskom jago	-	5600	50	10	Rp	250	8c71defd	BMP-2605-001-d1aebb79-aff68729	2026-05-02 16:02:48.696612+07	2026-05-02 16:02:48.696613+07	f	0
335	2026-05-02 16:02:48.705015+07	2026-05-02 16:02:48.705015+07	2026-05-02 16:02:59.006673+07	23	smile 12	-	5400	50	10	Rp	250	e22f19fa	BMP-2605-001-d1aebb79-e94c94a5	2026-05-02 16:02:48.704994+07	2026-05-02 16:02:48.704994+07	f	0
336	2026-05-02 16:02:48.713793+07	2026-05-02 16:02:48.713793+07	2026-05-02 16:02:59.006673+07	16	baskom mawar	-	5800	50	10	Rp	250	fe40a23e	BMP-2605-001-d1aebb79-ed03fd96	2026-05-02 16:02:48.713775+07	2026-05-02 16:02:48.713775+07	f	0
337	2026-05-02 16:02:48.722354+07	2026-05-02 16:02:48.722354+07	2026-05-02 16:02:59.006673+07	13	baskom panda	-	8400	40	1	Rp	250	a853507a	BMP-2605-001-d1aebb79-c7517a33	2026-05-02 16:02:48.72234+07	2026-05-02 16:02:48.72234+07	f	0
338	2026-05-02 16:02:59.01542+07	2026-05-02 16:02:59.01542+07	2026-05-02 16:05:33.126714+07	17	baskom bahtera TM	-	7000	50	15	Rp	250	b7390b48	BMP-2605-001-d1aebb79-8113791a	2026-05-02 16:02:59.015395+07	2026-05-02 16:02:59.015395+07	f	0
339	2026-05-02 16:02:59.024628+07	2026-05-02 16:02:59.024628+07	2026-05-02 16:05:33.126714+07	20	baskom jago	-	5600	50	10	Rp	250	d25bb642	BMP-2605-001-d1aebb79-aac6da14	2026-05-02 16:02:59.024602+07	2026-05-02 16:02:59.024602+07	f	0
340	2026-05-02 16:02:59.033056+07	2026-05-02 16:02:59.033056+07	2026-05-02 16:05:33.126714+07	23	smile 12	-	5400	50	10	Rp	250	1641ddad	BMP-2605-001-d1aebb79-4f34236a	2026-05-02 16:02:59.033038+07	2026-05-02 16:02:59.033038+07	f	0
341	2026-05-02 16:02:59.041427+07	2026-05-02 16:02:59.041427+07	2026-05-02 16:05:33.126714+07	16	baskom mawar	-	5800	50	10	Rp	250	2b705de3	BMP-2605-001-d1aebb79-55542718	2026-05-02 16:02:59.041407+07	2026-05-02 16:02:59.041407+07	f	0
342	2026-05-02 16:02:59.049915+07	2026-05-02 16:02:59.049915+07	2026-05-02 16:05:33.126714+07	13	baskom panda	-	8400	40	2	Rp	250	1bd8a238	BMP-2605-001-d1aebb79-67979ecb	2026-05-02 16:02:59.049902+07	2026-05-02 16:02:59.049902+07	f	0
343	2026-05-02 16:05:33.135265+07	2026-05-02 16:05:33.135265+07	\N	17	baskom bahtera TM	-	7000	50	15	Rp	250	524e0cf0	BMP-2605-001-d1aebb79-165326fe	2026-05-02 16:05:33.135241+07	2026-05-02 16:05:33.135241+07	f	0
344	2026-05-02 16:05:33.144389+07	2026-05-02 16:05:33.144389+07	\N	20	baskom jago	-	7000	50	10	Rp	250	b4e743dc	BMP-2605-001-d1aebb79-34fe2793	2026-05-02 16:05:33.144361+07	2026-05-02 16:05:33.144362+07	f	0
345	2026-05-02 16:05:33.152927+07	2026-05-02 16:05:33.152927+07	\N	23	smile 12	-	7000	50	10	Rp	250	51b24081	BMP-2605-001-d1aebb79-1fb46617	2026-05-02 16:05:33.152905+07	2026-05-02 16:05:33.152905+07	f	0
346	2026-05-02 16:05:33.161391+07	2026-05-02 16:05:33.161391+07	\N	16	baskom mawar	-	7200	50	10	Rp	250	1156f6b7	BMP-2605-001-d1aebb79-148c1cf5	2026-05-02 16:05:33.161373+07	2026-05-02 16:05:33.161373+07	f	0
347	2026-05-02 16:05:33.169534+07	2026-05-02 16:05:33.169534+07	\N	13	baskom panda	-	8400	40	2	Rp	250	c4a211ee	BMP-2605-001-d1aebb79-f3280fef	2026-05-02 16:05:33.169517+07	2026-05-02 16:05:33.169517+07	f	0
348	2026-05-05 01:25:53.280627+07	2026-05-05 01:25:53.280627+07	\N	25	baskom mawar	-	7400	50	25	Rp	251	42ab698a	BMP-2605-001-9e707f6b-1fd01cd4	2026-05-05 01:25:53.280601+07	2026-05-05 01:25:53.280601+07	f	0
349	2026-05-05 01:25:53.293105+07	2026-05-05 01:25:53.293105+07	\N	17	baskom bahtera TM	-	7200	50	10	Rp	251	9bc3d733	BMP-2605-001-9e707f6b-1738edf0	2026-05-05 01:25:53.29308+07	2026-05-05 01:25:53.29308+07	f	0
356	2026-05-05 20:35:14.880984+07	2026-05-05 20:35:14.880984+07	2026-05-05 20:49:12.628024+07	22	baskom panda super	-	8600	40	20	Rp	254	2988d20b	BMP-2605-004-18ed465c-d148ded9	2026-05-05 20:35:14.880961+07	2026-05-05 20:35:14.880961+07	f	0
357	2026-05-05 20:49:45.358964+07	2026-05-05 20:49:45.358964+07	\N	22	baskom panda super	-	8600	40	20	Rp	255	45102ee9	BMP-2605-004-35ca2747-8d4090ec	2026-05-05 20:49:45.358947+07	2026-05-05 20:49:45.358947+07	f	0
354	2026-05-05 17:26:59.320781+07	2026-05-05 17:26:59.320781+07	2026-05-05 22:49:22.067994+07	16	baskom mawar	-	7200	50	20	Rp	253	568307e3	BMP-2605-003-95768d12-9da24ea5	2026-05-05 17:26:59.320754+07	2026-05-05 17:26:59.320755+07	f	0
355	2026-05-05 17:26:59.333023+07	2026-05-05 17:26:59.333023+07	2026-05-05 22:49:22.067994+07	29	Baskom TM	lusin	7000	50	10	Rp	253	1c18fb2e	BMP-2605-003-95768d12-b9259eeb	2026-05-05 17:26:59.333003+07	2026-05-05 17:26:59.333003+07	f	0
358	2026-05-09 12:35:55.051957+07	2026-05-09 12:35:55.051957+07	\N	18	bak kuping12	-	13000	30	70	Rp	256	35c0ffb0	BMP-2605-005-dd96ce2c-9c61d5a1	2026-05-09 12:35:55.051916+07	2026-05-09 12:35:55.051916+07	f	0
359	2026-05-09 12:35:55.059563+07	2026-05-09 12:35:55.059563+07	\N	28	baskom jago 12	-	7000	50	15	Rp	256	9df322ef	BMP-2605-005-dd96ce2c-6a646098	2026-05-09 12:35:55.059537+07	2026-05-09 12:35:55.059537+07	f	0
360	2026-05-09 12:35:55.064739+07	2026-05-09 12:35:55.064739+07	\N	22	baskom panda super	-	8600	40	5	Rp	256	339a1b57	BMP-2605-005-dd96ce2c-937b6ade	2026-05-09 12:35:55.064713+07	2026-05-09 12:35:55.064713+07	f	0
350	2026-05-05 16:49:58.535163+07	2026-05-05 16:49:58.535163+07	2026-05-14 08:44:22.662374+07	35	wakul telur	-	2600	20	130	Rp	252	41c11b46	BMP-2605-002-25268dae-fb2a9399	2026-05-05 16:49:58.535143+07	2026-05-05 16:49:58.535143+07	f	0
351	2026-05-05 16:49:58.550206+07	2026-05-05 16:49:58.550206+07	2026-05-14 08:44:22.662374+07	13	baskom panda	-	8400	40	10	Rp	252	744a1b3d	BMP-2605-002-25268dae-fdef4ea1	2026-05-05 16:49:58.55019+07	2026-05-05 16:49:58.55019+07	f	0
352	2026-05-05 16:49:58.564957+07	2026-05-05 16:49:58.564957+07	2026-05-14 08:44:22.662374+07	25	baskom mawar	-	7200	50	5	Rp	252	e6d4a9d3	BMP-2605-002-25268dae-aa000c45	2026-05-05 16:49:58.564944+07	2026-05-05 16:49:58.564944+07	f	0
353	2026-05-05 16:49:58.580938+07	2026-05-05 16:49:58.580938+07	2026-05-14 08:44:22.662374+07	39	tradisi cerah	Lusin	4900	30	10	Rp	252	ed8eece4	BMP-2605-002-25268dae-08b9b844	2026-05-05 16:49:58.580916+07	2026-05-05 16:49:58.580917+07	f	0
361	2026-05-14 08:44:22.678246+07	2026-05-14 08:44:22.678246+07	2026-05-14 08:45:18.46926+07	35	wakul telur	-	2600	20	130	Rp	252	c3690605	BMP-2605-005-25268dae-e623d854	2026-05-14 08:44:22.678224+07	2026-05-14 08:44:22.678224+07	t	2600
362	2026-05-14 08:44:22.697918+07	2026-05-14 08:44:22.697918+07	2026-05-14 08:45:18.46926+07	13	baskom panda	-	8400	40	10	Rp	252	d4b1a1c9	BMP-2605-005-25268dae-40b1587c	2026-05-14 08:44:22.697908+07	2026-05-14 08:44:22.697908+07	f	0
363	2026-05-14 08:44:22.705646+07	2026-05-14 08:44:22.705646+07	2026-05-14 08:45:18.46926+07	25	baskom mawar	-	7200	50	5	Rp	252	40d9f311	BMP-2605-005-25268dae-a6a38c52	2026-05-14 08:44:22.705631+07	2026-05-14 08:44:22.705631+07	f	0
364	2026-05-14 08:44:22.713327+07	2026-05-14 08:44:22.713327+07	2026-05-14 08:45:18.46926+07	39	tradisi cerah	Lusin	4900	30	10	Rp	252	cc61714b	BMP-2605-005-25268dae-3d5c694c	2026-05-14 08:44:22.713316+07	2026-05-14 08:44:22.713316+07	f	0
365	2026-05-14 08:45:18.477594+07	2026-05-14 08:45:18.477594+07	\N	13	baskom panda	-	8400	40	10	Rp	252	f4a3dff9	BMP-2605-005-25268dae-af882674	2026-05-14 08:45:18.477566+07	2026-05-14 08:45:18.477566+07	f	0
366	2026-05-14 08:45:18.486025+07	2026-05-14 08:45:18.486025+07	\N	25	baskom mawar	-	7200	50	5	Rp	252	2f188033	BMP-2605-005-25268dae-3c81e59d	2026-05-14 08:45:18.485989+07	2026-05-14 08:45:18.485989+07	f	0
367	2026-05-14 08:45:18.493995+07	2026-05-14 08:45:18.493995+07	\N	39	tradisi cerah	Lusin	4900	30	10	Rp	252	67a04202	BMP-2605-005-25268dae-e2d7dd3c	2026-05-14 08:45:18.493948+07	2026-05-14 08:45:18.493948+07	f	0
203	2026-04-20 11:40:16.106387+07	2026-04-20 11:40:16.106432+07	2026-05-14 20:04:16.466759+07	16	baskom mawar	-	6050	50	10	Rp	182	bcbfde4d86af	baskom-mawar-bcbfde4d86af	2026-04-20 11:40:16.106387+07	2026-04-20 11:40:16.106432+07	f	0
204	2026-04-20 11:40:16.110792+07	2026-04-20 11:40:16.11083+07	2026-05-14 20:04:16.466759+07	17	baskom bahtera TM	-	5750	50	10	Rp	182	b91ab45f9056	baskom-bahtera-tm-b91ab45f9056	2026-04-20 11:40:16.110792+07	2026-04-20 11:40:16.11083+07	f	0
205	2026-04-20 11:40:16.114388+07	2026-04-20 11:40:16.114421+07	2026-05-14 20:04:16.466759+07	28	baskom jago 12	-	5550	50	10	Rp	182	7c168235e3e0	baskom-jago-12-7c168235e3e0	2026-04-20 11:40:16.114388+07	2026-04-20 11:40:16.114421+07	f	0
206	2026-04-20 11:40:16.118474+07	2026-04-20 11:40:16.118518+07	2026-05-14 20:04:16.466759+07	30	Baskom Durian	-	6600	50	5	Rp	182	b0ceaa926e03	baskom-durian-b0ceaa926e03	2026-04-20 11:40:16.118474+07	2026-04-20 11:40:16.118518+07	f	0
372	2026-05-14 20:04:16.483912+07	2026-05-14 20:04:16.483912+07	\N	16	baskom mawar	-	6050	50	10	Rp	182	754fc320	bmp-0426-032-fd9bd5ccae75-ff5fa4f7	2026-05-14 20:04:16.483878+07	2026-05-14 20:04:16.483878+07	f	0
373	2026-05-14 20:04:16.497397+07	2026-05-14 20:04:16.497397+07	\N	17	baskom bahtera TM	-	5750	50	10	Rp	182	853743f1	bmp-0426-032-fd9bd5ccae75-eb650e79	2026-05-14 20:04:16.497372+07	2026-05-14 20:04:16.497372+07	f	0
374	2026-05-14 20:04:16.506611+07	2026-05-14 20:04:16.506611+07	\N	28	baskom jago 12	-	5550	50	10	Rp	182	0bb896e5	bmp-0426-032-fd9bd5ccae75-6dc183ed	2026-05-14 20:04:16.506585+07	2026-05-14 20:04:16.506585+07	f	0
375	2026-05-14 20:04:16.515771+07	2026-05-14 20:04:16.515771+07	\N	30	Baskom Durian	-	6640	50	5	Rp	182	0d0480d7	bmp-0426-032-fd9bd5ccae75-a7e03515	2026-05-14 20:04:16.515746+07	2026-05-14 20:04:16.515747+07	f	0
376	2026-05-14 20:15:37.870071+07	2026-05-14 20:15:37.870071+07	\N	18	bak kuping12	-	12000	30	50	Rp	258	f887db04	BMP-2605-009-6bea2397-7aaadbf6	2026-05-14 20:15:37.870048+07	2026-05-14 20:15:37.870048+07	f	0
377	2026-05-14 20:15:37.878938+07	2026-05-14 20:15:37.878938+07	\N	16	baskom mawar	-	5800	50	15	Rp	258	11ef4174	BMP-2605-009-6bea2397-069e1bdb	2026-05-14 20:15:37.878922+07	2026-05-14 20:15:37.878922+07	f	0
378	2026-05-14 20:15:37.887436+07	2026-05-14 20:15:37.887436+07	\N	20	baskom jago	-	5500	50	15	Rp	258	d5be10b7	BMP-2605-009-6bea2397-2244ccc7	2026-05-14 20:15:37.887421+07	2026-05-14 20:15:37.887421+07	f	0
379	2026-05-14 20:15:37.895915+07	2026-05-14 20:15:37.895915+07	\N	30	Baskom Durian	-	8200	40	10	Rp	258	df0f82d2	BMP-2605-009-6bea2397-ce5d4c27	2026-05-14 20:15:37.8959+07	2026-05-14 20:15:37.8959+07	f	0
380	2026-05-14 20:15:37.904267+07	2026-05-14 20:15:37.904267+07	\N	17	baskom bahtera TM	-	5800	50	10	Rp	258	f3d11fd9	BMP-2605-009-6bea2397-d99d24ef	2026-05-14 20:15:37.904251+07	2026-05-14 20:15:37.904251+07	f	0
381	2026-05-14 20:15:37.912942+07	2026-05-14 20:15:37.912942+07	\N	19	wakul moris	-	4400	50	5	Rp	258	685969ff	BMP-2605-009-6bea2397-17907574	2026-05-14 20:15:37.912927+07	2026-05-14 20:15:37.912927+07	f	0
382	2026-05-14 20:15:37.921469+07	2026-05-14 20:15:37.921469+07	\N	43	Wakul Mawar Super	Lusin	3750	50	5	Rp	258	7dac131c	BMP-2605-009-6bea2397-fa74a784	2026-05-14 20:15:37.921454+07	2026-05-14 20:15:37.921454+07	f	0
457	2026-05-26 15:15:53.963899+07	2026-05-26 15:15:53.963899+07	2026-05-26 16:09:04.287052+07	20	baskom jago	Lusin	7000	50	120	Rp	259	e651be4d	BMP-2605-010-c94477e0-bbe8ac8c	2026-05-26 15:15:53.963822+07	2026-05-26 15:15:53.963822+07	f	0
388	2026-05-15 07:31:27.045928+07	2026-05-15 07:31:27.045928+07	2026-05-15 08:27:26.240317+07	22	baskom panda super	-	8600	40	70	Rp	260	9d18fb66	BMP-2605-011-51099424-d140dfe8	2026-05-15 07:31:27.045905+07	2026-05-15 07:31:27.045905+07	f	0
389	2026-05-15 07:31:27.061237+07	2026-05-15 07:31:27.061237+07	2026-05-15 08:27:26.240317+07	29	Baskom TM	lusin	7250	50	10	Rp	260	53bf971c	BMP-2605-011-51099424-b0bd3856	2026-05-15 07:31:27.061223+07	2026-05-15 07:31:27.061223+07	f	0
390	2026-05-15 08:27:26.260627+07	2026-05-15 08:27:26.260627+07	\N	22	baskom panda super	-	8600	40	70	Rp	260	0ce18d3a	BMP-2605-011-51099424-b5fd0a1e	2026-05-15 08:27:26.260578+07	2026-05-15 08:27:26.260578+07	f	0
391	2026-05-15 08:27:26.271956+07	2026-05-15 08:27:26.271956+07	\N	29	Baskom TM	lusin	7250	50	10	Rp	260	efd9670d	BMP-2605-011-51099424-df92eb09	2026-05-15 08:27:26.271912+07	2026-05-15 08:27:26.271912+07	f	0
392	2026-05-15 08:27:26.282139+07	2026-05-15 08:27:26.282139+07	\N	51	Baskom Panda Cerah	Lusin	10000	40	30	Rp	260	57904c99	BMP-2605-011-51099424-fbfc1ec2	2026-05-15 08:27:26.282097+07	2026-05-15 08:27:26.282097+07	f	0
458	2026-05-26 15:15:53.980024+07	2026-05-26 15:15:53.980024+07	2026-05-26 16:09:04.287052+07	26	Baskom Bahtera	Lusin	7300	50	20	Rp	259	b658bbd4	BMP-2605-010-c94477e0-d701861a	2026-05-26 15:15:53.979984+07	2026-05-26 15:15:53.979985+07	f	0
459	2026-05-26 15:15:53.98809+07	2026-05-26 15:15:53.98809+07	2026-05-26 16:09:04.287052+07	23	smile 12	Lusin	6900	50	15	Rp	259	ae4f7540	BMP-2605-010-c94477e0-8e971af1	2026-05-26 15:15:53.988042+07	2026-05-26 15:15:53.988042+07	f	0
460	2026-05-26 15:15:53.996133+07	2026-05-26 15:15:53.996133+07	2026-05-26 16:09:04.287052+07	38	Smile 14	Lusin	9300	50	36	Rp	259	53464ab3	BMP-2605-010-c94477e0-3893bb03	2026-05-26 15:15:53.996102+07	2026-05-26 15:15:53.996102+07	f	0
393	2026-05-15 09:35:08.358168+07	2026-05-15 09:35:08.358168+07	2026-05-15 09:53:20.810265+07	29	Baskom TM	lusin	7250	50	10	Rp	261	5e7d071f	BMP-2605-012-006f3869-36a6a0b6	2026-05-15 09:35:08.358125+07	2026-05-15 09:35:08.358125+07	f	0
394	2026-05-15 09:35:08.367835+07	2026-05-15 09:35:08.367835+07	2026-05-15 09:53:20.810265+07	28	baskom jago 12	-	7000	50	15	Rp	261	d0ded155	BMP-2605-012-006f3869-3ddcce8f	2026-05-15 09:35:08.367784+07	2026-05-15 09:35:08.367784+07	f	0
395	2026-05-15 09:35:08.374012+07	2026-05-15 09:35:08.374012+07	2026-05-15 09:53:20.810265+07	16	baskom mawar	-	7100	50	5	Rp	261	7b005ad2	BMP-2605-012-006f3869-b3b2d4a3	2026-05-15 09:35:08.373972+07	2026-05-15 09:35:08.373972+07	f	0
461	2026-05-26 15:15:54.004132+07	2026-05-26 15:15:54.004132+07	2026-05-26 16:09:04.287052+07	51	Baskom Panda Cerah	Lusin	10000	40	5	Rp	259	70dd9423	BMP-2605-010-c94477e0-677a6635	2026-05-26 15:15:54.004093+07	2026-05-26 15:15:54.004094+07	f	0
462	2026-05-26 15:15:54.012196+07	2026-05-26 15:15:54.012196+07	2026-05-26 16:09:04.287052+07	37	telor tali	Lusin	3700	20	97	Rp	259	b9cbbb72	BMP-2605-010-c94477e0-1dae5ef0	2026-05-26 15:15:54.012151+07	2026-05-26 15:15:54.012151+07	t	0
396	2026-05-15 09:53:20.8179+07	2026-05-15 09:53:20.8179+07	2026-05-15 09:53:44.47035+07	28	baskom jago 12	-	7000	50	15	Rp	261	4cf58feb	BMP-2605-012-006f3869-a4952205	2026-05-15 09:53:20.81784+07	2026-05-15 09:53:20.81784+07	f	0
397	2026-05-15 09:53:20.825629+07	2026-05-15 09:53:20.825629+07	2026-05-15 09:53:44.47035+07	16	baskom mawar	-	7100	50	5	Rp	261	76efbe10	BMP-2605-012-006f3869-4204ad3e	2026-05-15 09:53:20.825603+07	2026-05-15 09:53:20.825603+07	f	0
383	2026-05-14 20:35:43.516717+07	2026-05-14 20:35:43.516717+07	2026-05-15 15:29:39.024908+07	20	baskom jago	-	7000	50	120	Rp	259	3070316c	BMP-2605-010-c94477e0-4ee67cc8	2026-05-14 20:35:43.5167+07	2026-05-14 20:35:43.5167+07	f	0
384	2026-05-14 20:35:43.529311+07	2026-05-14 20:35:43.529311+07	2026-05-15 15:29:39.024908+07	26	Baskom Bahtera	-	7300	50	20	Rp	259	540a92d5	BMP-2605-010-c94477e0-a00e4bad	2026-05-14 20:35:43.529292+07	2026-05-14 20:35:43.529292+07	f	0
385	2026-05-14 20:35:43.537748+07	2026-05-14 20:35:43.537748+07	2026-05-15 15:29:39.024908+07	23	smile 12	-	6900	50	15	Rp	259	83b6cccf	BMP-2605-010-c94477e0-e8635d8f	2026-05-14 20:35:43.537728+07	2026-05-14 20:35:43.537728+07	f	0
386	2026-05-14 20:35:43.546164+07	2026-05-14 20:35:43.546164+07	2026-05-15 15:29:39.024908+07	37	telor tali	Lusin	3700	20	97	Rp	259	ce1e5cd6	BMP-2605-010-c94477e0-156a81d8	2026-05-14 20:35:43.546136+07	2026-05-14 20:35:43.546136+07	f	0
387	2026-05-14 20:35:43.554436+07	2026-05-14 20:35:43.554436+07	2026-05-15 15:29:39.024908+07	38	Smile 14	Lusin	9300	50	36	Rp	259	a5912554	BMP-2605-010-c94477e0-ac61f46d	2026-05-14 20:35:43.554421+07	2026-05-14 20:35:43.554421+07	f	0
398	2026-05-15 09:53:44.47557+07	2026-05-15 09:53:44.47557+07	2026-05-15 17:23:08.022893+07	28	baskom jago 12	-	7000	50	15	Rp	261	4dd86e53	BMP-2605-012-006f3869-047f96d6	2026-05-15 09:53:44.475519+07	2026-05-15 09:53:44.475519+07	f	0
399	2026-05-15 09:53:44.48073+07	2026-05-15 09:53:44.48073+07	2026-05-15 17:23:08.022893+07	16	baskom mawar	-	7100	50	5	Rp	261	906aad4f	BMP-2605-012-006f3869-abfffeb2	2026-05-15 09:53:44.480705+07	2026-05-15 09:53:44.480705+07	f	0
400	2026-05-15 09:53:44.486003+07	2026-05-15 09:53:44.486003+07	2026-05-15 17:23:08.022893+07	17	baskom bahtera TM	-	7250	50	10	Rp	261	7941135f	BMP-2605-012-006f3869-804c0b02	2026-05-15 09:53:44.485954+07	2026-05-15 09:53:44.485954+07	f	0
401	2026-05-15 15:29:39.032693+07	2026-05-15 15:29:39.032693+07	2026-05-26 12:34:42.64882+07	20	baskom jago	Lusin	7000	50	120	Rp	259	1a97babd	BMP-2605-010-c94477e0-c2a2fdb0	2026-05-15 15:29:39.032658+07	2026-05-15 15:29:39.032658+07	f	0
402	2026-05-15 15:29:39.040928+07	2026-05-15 15:29:39.040928+07	2026-05-26 12:34:42.64882+07	26	Baskom Bahtera	Lusin	7300	50	20	Rp	259	76eae052	BMP-2605-010-c94477e0-ceac1ddf	2026-05-15 15:29:39.040892+07	2026-05-15 15:29:39.040892+07	f	0
403	2026-05-15 15:29:39.046333+07	2026-05-15 15:29:39.046333+07	2026-05-26 12:34:42.64882+07	23	smile 12	Lusin	6900	50	15	Rp	259	39f07ac2	BMP-2605-010-c94477e0-cb3d0847	2026-05-15 15:29:39.046303+07	2026-05-15 15:29:39.046303+07	f	0
404	2026-05-15 15:29:39.051521+07	2026-05-15 15:29:39.051521+07	2026-05-26 12:34:42.64882+07	37	telor tali	Lusin	3700	20	97	Rp	259	f8c6d0f7	BMP-2605-010-c94477e0-2195ab48	2026-05-15 15:29:39.051499+07	2026-05-15 15:29:39.051499+07	f	0
405	2026-05-15 15:29:39.056753+07	2026-05-15 15:29:39.056753+07	2026-05-26 12:34:42.64882+07	38	Smile 14	Lusin	9300	50	36	Rp	259	d6019bd0	BMP-2605-010-c94477e0-877b942b	2026-05-15 15:29:39.056721+07	2026-05-15 15:29:39.056721+07	f	0
440	2026-05-26 12:35:42.793583+07	2026-05-26 12:35:42.793583+07	2026-05-26 12:49:31.480431+07	20	baskom jago	Lusin	7000	50	120	Rp	259	bb6cb229	BMP-2605-010-c94477e0-c68fad5f	2026-05-26 12:35:42.793558+07	2026-05-26 12:35:42.793558+07	f	0
407	2026-05-15 17:23:08.041607+07	2026-05-15 17:23:08.041607+07	\N	28	baskom jago 12	Lusin	7000	50	15	Rp	261	e49cec45	BMP-2605-012-006f3869-ed333c03	2026-05-15 17:23:08.04086+07	2026-05-15 17:23:08.04086+07	f	0
408	2026-05-15 17:23:08.058217+07	2026-05-15 17:23:08.058217+07	\N	16	baskom mawar	Lusin	7100	50	5	Rp	261	2ec25522	BMP-2605-012-006f3869-144759b8	2026-05-15 17:23:08.058197+07	2026-05-15 17:23:08.058197+07	f	0
409	2026-05-15 17:23:08.067075+07	2026-05-15 17:23:08.067075+07	\N	17	baskom bahtera TM	Lusin	7000	50	10	Rp	261	a1d23d45	BMP-2605-012-006f3869-b02e4c44	2026-05-15 17:23:08.067049+07	2026-05-15 17:23:08.067049+07	f	0
410	2026-05-15 21:07:39.351913+07	2026-05-15 21:07:39.351913+07	\N	29	Baskom TM	lusin	7500	50	100	Rp	262	a0c9fed5	BMP-2605-013-4d43d330-615d9ed4	2026-05-15 21:07:39.351884+07	2026-05-15 21:07:39.351884+07	f	0
411	2026-05-15 21:07:39.364375+07	2026-05-15 21:07:39.364375+07	\N	46	BMP	Lusin	7500	50	50	Rp	262	012cebe8	BMP-2605-013-4d43d330-85e761fc	2026-05-15 21:07:39.364357+07	2026-05-15 21:07:39.364357+07	f	0
412	2026-05-15 21:07:39.372889+07	2026-05-15 21:07:39.372889+07	\N	51	Baskom Panda Cerah	Lusin	10000	40	10	Rp	262	7973a827	BMP-2605-013-4d43d330-89328e02	2026-05-15 21:07:39.372872+07	2026-05-15 21:07:39.372872+07	f	0
413	2026-05-15 21:07:39.381127+07	2026-05-15 21:07:39.381127+07	\N	16	baskom mawar	Lusin	7500	50	15	Rp	262	cce6c380	BMP-2605-013-4d43d330-2fba67dc	2026-05-15 21:07:39.381111+07	2026-05-15 21:07:39.381111+07	f	0
414	2026-05-15 21:07:39.389401+07	2026-05-15 21:07:39.389401+07	\N	43	Wakul Mawar Super	Lusin	4000	50	13	Rp	262	d2f8f071	BMP-2605-013-4d43d330-c90a75da	2026-05-15 21:07:39.389386+07	2026-05-15 21:07:39.389386+07	f	0
415	2026-05-22 11:25:42.715217+07	2026-05-22 11:25:42.715217+07	\N	46	BMP	Lusin	7400	50	120	Rp	263	79954f3f	BMP-2605-014-13b59092-9cdf3c60	2026-05-22 11:25:42.715192+07	2026-05-22 11:25:42.715192+07	f	0
416	2026-05-22 11:25:42.727765+07	2026-05-22 11:25:42.727765+07	\N	29	Baskom TM	lusin	7350	50	20	Rp	263	05344c53	BMP-2605-014-13b59092-a65a1e2e	2026-05-22 11:25:42.727739+07	2026-05-22 11:25:42.727739+07	f	0
417	2026-05-22 11:25:42.736023+07	2026-05-22 11:25:42.736023+07	\N	16	baskom mawar	Lusin	7400	50	25	Rp	263	31e68a6b	BMP-2605-014-13b59092-21285ec2	2026-05-22 11:25:42.736+07	2026-05-22 11:25:42.736+07	f	0
418	2026-05-22 11:25:42.744178+07	2026-05-22 11:25:42.744178+07	\N	19	wakul moris	Lusin	6700	50	10	Rp	263	6da2d879	BMP-2605-014-13b59092-466add53	2026-05-22 11:25:42.744162+07	2026-05-22 11:25:42.744162+07	f	0
419	2026-05-22 11:25:42.752398+07	2026-05-22 11:25:42.752398+07	\N	51	Baskom Panda Cerah	Lusin	10000	40	10	Rp	263	db109d9a	BMP-2605-014-13b59092-da1a9c8f	2026-05-22 11:25:42.752381+07	2026-05-22 11:25:42.752381+07	f	0
441	2026-05-26 12:35:42.804047+07	2026-05-26 12:35:42.804047+07	2026-05-26 12:49:31.480431+07	26	Baskom Bahtera	Lusin	7300	50	20	Rp	259	b789d1cf	BMP-2605-010-c94477e0-a1191eee	2026-05-26 12:35:42.804029+07	2026-05-26 12:35:42.804029+07	f	0
442	2026-05-26 12:35:42.814392+07	2026-05-26 12:35:42.814392+07	2026-05-26 12:49:31.480431+07	23	smile 12	Lusin	6900	50	15	Rp	259	2d8b6a94	BMP-2605-010-c94477e0-13495ec6	2026-05-26 12:35:42.81438+07	2026-05-26 12:35:42.81438+07	f	0
420	2026-05-22 11:31:06.978295+07	2026-05-22 11:31:06.978295+07	2026-05-22 11:32:37.157469+07	46	BMP	Lusin	7500	50	50	Rp	264	a414dd7b	BMP-2605-015-88bb7619-8ee28e86	2026-05-22 11:31:06.978274+07	2026-05-22 11:31:06.978274+07	f	0
421	2026-05-22 11:31:06.991059+07	2026-05-22 11:31:06.991059+07	2026-05-22 11:32:37.157469+07	33	wakul kotak	Lusin	5500	20	20	Rp	264	6b048920	BMP-2605-015-88bb7619-4d944fdd	2026-05-22 11:31:06.991033+07	2026-05-22 11:31:06.991033+07	t	5100
422	2026-05-22 11:32:37.170089+07	2026-05-22 11:32:37.170089+07	\N	46	BMP	Lusin	7500	50	50	Rp	264	109a6ce4	BMP-2605-015-88bb7619-9b11e1ba	2026-05-22 11:32:37.169842+07	2026-05-22 11:32:37.169842+07	f	0
423	2026-05-22 11:32:37.178351+07	2026-05-22 11:32:37.178351+07	\N	33	wakul kotak	Lusin	5500	20	20	Rp	264	85b9fbd7	BMP-2605-015-88bb7619-ebeb09a3	2026-05-22 11:32:37.178333+07	2026-05-22 11:32:37.178333+07	t	5100
424	2026-05-22 14:05:15.778623+07	2026-05-22 14:05:15.778623+07	\N	17	baskom bahtera TM	Lusin	7200	50	20	Rp	265	08e7429e	BMP-2605-016-3df6596f-84a66aae	2026-05-22 14:05:15.778592+07	2026-05-22 14:05:15.778592+07	f	0
425	2026-05-22 14:05:15.786881+07	2026-05-22 14:05:15.786881+07	\N	13	baskom panda	Lusin	8600	40	20	Rp	265	c2beea11	BMP-2605-016-3df6596f-92654896	2026-05-22 14:05:15.786856+07	2026-05-22 14:05:15.786856+07	f	0
426	2026-05-25 11:32:42.670651+07	2026-05-25 11:32:42.670651+07	2026-05-25 12:07:35.59366+07	30	Baskom Durian	Lusin	9100	40	27	Rp	266	14530d46	BMP-2605-017-00c25e20-1f729f0f	2026-05-25 11:32:42.67062+07	2026-05-25 11:32:42.67062+07	f	0
427	2026-05-25 11:32:42.68316+07	2026-05-25 11:32:42.68316+07	2026-05-25 12:07:35.59366+07	22	baskom panda super	Lusin	8600	40	16	Rp	266	4cb0544b	BMP-2605-017-00c25e20-84d6a5ff	2026-05-25 11:32:42.68314+07	2026-05-25 11:32:42.68314+07	f	0
428	2026-05-25 11:32:42.690668+07	2026-05-25 11:32:42.690668+07	2026-05-25 12:07:35.59366+07	46	BMP	Lusin	7200	50	8	Rp	266	e8668b63	BMP-2605-017-00c25e20-f51c2beb	2026-05-25 11:32:42.690649+07	2026-05-25 11:32:42.690649+07	f	0
429	2026-05-25 11:32:42.699827+07	2026-05-25 11:32:42.699827+07	2026-05-25 12:07:35.59366+07	43	Wakul Mawar Super	Lusin	7400	50	7	Rp	266	c615d49b	BMP-2605-017-00c25e20-0b2770b5	2026-05-25 11:32:42.699803+07	2026-05-25 11:32:42.699803+07	f	0
430	2026-05-25 12:07:35.605344+07	2026-05-25 12:07:35.605344+07	\N	30	Baskom Durian	Lusin	9100	40	27	Rp	266	69c74eb3	BMP-2605-017-00c25e20-de728b45	2026-05-25 12:07:35.605324+07	2026-05-25 12:07:35.605324+07	f	0
431	2026-05-25 12:07:35.612961+07	2026-05-25 12:07:35.612961+07	\N	22	baskom panda super	Lusin	8600	40	16	Rp	266	f3c57642	BMP-2605-017-00c25e20-05be19e5	2026-05-25 12:07:35.612941+07	2026-05-25 12:07:35.612941+07	f	0
432	2026-05-25 12:07:35.620436+07	2026-05-25 12:07:35.620436+07	\N	46	BMP	Lusin	7200	50	8	Rp	266	69dad51d	BMP-2605-017-00c25e20-eddbf1b1	2026-05-25 12:07:35.620415+07	2026-05-25 12:07:35.620415+07	f	0
433	2026-05-25 12:07:35.629137+07	2026-05-25 12:07:35.629137+07	\N	43	Wakul Mawar Super	Lusin	7400	50	5	Rp	266	a23f3c5d	BMP-2605-017-00c25e20-5fa34adb	2026-05-25 12:07:35.629122+07	2026-05-25 12:07:35.629122+07	f	0
406	2026-05-15 15:29:39.061665+07	2026-05-15 15:29:39.061665+07	2026-05-26 12:34:42.64882+07	51	Baskom Panda Cerah	Lusin	10000	40	5	Rp	259	7d7a38a6	BMP-2605-010-c94477e0-25399a5d	2026-05-15 15:29:39.061628+07	2026-05-15 15:29:39.061628+07	f	0
499	2026-05-29 22:22:02.739608+07	2026-05-29 22:22:02.739608+07	\N	17	baskom bahtera TM	Lusin	6800	50	15	Rp	180	e0f2e987	bmp-0426-030-7ed262bd1c5a-489fc572	2026-05-29 22:22:02.739572+07	2026-05-29 22:22:02.739572+07	f	0
434	2026-05-26 12:34:42.669579+07	2026-05-26 12:34:42.669579+07	2026-05-26 12:35:42.782843+07	20	baskom jago	Lusin	7000	50	120	Rp	259	d45cfc46	BMP-2605-010-c94477e0-cba8b181	2026-05-26 12:34:42.669484+07	2026-05-26 12:34:42.669489+07	f	0
435	2026-05-26 12:34:42.685907+07	2026-05-26 12:34:42.685907+07	2026-05-26 12:35:42.782843+07	26	Baskom Bahtera	Lusin	7300	50	20	Rp	259	9a9300f4	BMP-2605-010-c94477e0-b33fb173	2026-05-26 12:34:42.68588+07	2026-05-26 12:34:42.68588+07	f	0
436	2026-05-26 12:34:42.696821+07	2026-05-26 12:34:42.696821+07	2026-05-26 12:35:42.782843+07	23	smile 12	Lusin	6900	50	15	Rp	259	5686e3ee	BMP-2605-010-c94477e0-428082c9	2026-05-26 12:34:42.696792+07	2026-05-26 12:34:42.696792+07	f	0
437	2026-05-26 12:34:42.709225+07	2026-05-26 12:34:42.709225+07	2026-05-26 12:35:42.782843+07	37	telor tali	Lusin	3700	20	97	Rp	259	7417ada1	BMP-2605-010-c94477e0-c81afbe3	2026-05-26 12:34:42.709207+07	2026-05-26 12:34:42.709207+07	t	3700
438	2026-05-26 12:34:42.731732+07	2026-05-26 12:34:42.731732+07	2026-05-26 12:35:42.782843+07	38	Smile 14	Lusin	9300	50	36	Rp	259	fb4849e4	BMP-2605-010-c94477e0-68819aef	2026-05-26 12:34:42.731715+07	2026-05-26 12:34:42.731715+07	f	0
439	2026-05-26 12:34:42.742338+07	2026-05-26 12:34:42.742338+07	2026-05-26 12:35:42.782843+07	51	Baskom Panda Cerah	Lusin	10000	40	5	Rp	259	b448ab08	BMP-2605-010-c94477e0-ba3eb7a3	2026-05-26 12:34:42.742323+07	2026-05-26 12:34:42.742323+07	f	0
443	2026-05-26 12:35:42.824484+07	2026-05-26 12:35:42.824484+07	2026-05-26 12:49:31.480431+07	38	Smile 14	Lusin	9300	50	36	Rp	259	8dc4b005	BMP-2605-010-c94477e0-41318013	2026-05-26 12:35:42.824472+07	2026-05-26 12:35:42.824472+07	f	0
444	2026-05-26 12:35:42.836048+07	2026-05-26 12:35:42.836048+07	2026-05-26 12:49:31.480431+07	51	Baskom Panda Cerah	Lusin	10000	40	5	Rp	259	c112e9eb	BMP-2605-010-c94477e0-eafdc77b	2026-05-26 12:35:42.836035+07	2026-05-26 12:35:42.836035+07	f	0
368	2026-05-14 08:52:21.027258+07	2026-05-14 08:52:21.027258+07	2026-05-26 16:06:27.51911+07	35	wakul telur	-	2600	20	130	Rp	257	138c846d	BMP-2605-008-b971b6d0-63afd31b	2026-05-14 08:52:21.02723+07	2026-05-14 08:52:21.02723+07	t	6760000
369	2026-05-14 08:52:21.039227+07	2026-05-14 08:52:21.039227+07	2026-05-26 16:06:27.51911+07	13	baskom panda	-	8400	40	10	Rp	257	45036519	BMP-2605-008-b971b6d0-c7b4bd7f	2026-05-14 08:52:21.039212+07	2026-05-14 08:52:21.039212+07	f	0
370	2026-05-14 08:52:21.047255+07	2026-05-14 08:52:21.047255+07	2026-05-26 16:06:27.51911+07	16	baskom mawar	-	7200	50	5	Rp	257	366febfe	BMP-2605-008-b971b6d0-c20f31ae	2026-05-14 08:52:21.047235+07	2026-05-14 08:52:21.047235+07	f	0
371	2026-05-14 08:52:21.05534+07	2026-05-14 08:52:21.05534+07	2026-05-26 16:06:27.51911+07	39	tradisi cerah	Lusin	4900	30	10	Rp	257	db5fe7e5	BMP-2605-008-b971b6d0-3a588995	2026-05-14 08:52:21.055328+07	2026-05-14 08:52:21.055328+07	f	0
465	2026-05-26 16:06:27.538746+07	2026-05-26 16:06:27.538746+07	\N	35	wakul telur	Lusin	2600	20	130	Rp	257	3c8d1c7c	BMP-2605-008-b971b6d0-05d6414f	2026-05-26 16:06:27.538718+07	2026-05-26 16:06:27.538718+07	t	0
466	2026-05-26 16:06:27.558808+07	2026-05-26 16:06:27.558808+07	\N	13	baskom panda	Lusin	8400	40	10	Rp	257	ff7a7dbb	BMP-2605-008-b971b6d0-de1c9f75	2026-05-26 16:06:27.558797+07	2026-05-26 16:06:27.558797+07	f	0
467	2026-05-26 16:06:27.569094+07	2026-05-26 16:06:27.569094+07	\N	16	baskom mawar	Lusin	7200	50	5	Rp	257	052dd66e	BMP-2605-008-b971b6d0-c126f13d	2026-05-26 16:06:27.569086+07	2026-05-26 16:06:27.569086+07	f	0
468	2026-05-26 16:06:27.578514+07	2026-05-26 16:06:27.578514+07	\N	39	tradisi cerah	Lusin	4900	30	10	Rp	257	dd078c93	BMP-2605-008-b971b6d0-030f5413	2026-05-26 16:06:27.578504+07	2026-05-26 16:06:27.578504+07	f	0
500	2026-05-29 22:22:02.744853+07	2026-05-29 22:22:02.744853+07	\N	18	bak kuping12	Lusin	13600	30	6	Rp	180	99d6a338	bmp-0426-030-7ed262bd1c5a-b4df63cc	2026-05-29 22:22:02.744809+07	2026-05-29 22:22:02.744809+07	f	0
501	2026-05-29 22:22:02.750125+07	2026-05-29 22:22:02.750125+07	\N	13	baskom panda	Lusin	7900	40	5	Rp	180	7a346a9d	bmp-0426-030-7ed262bd1c5a-9667de4b	2026-05-29 22:22:02.750092+07	2026-05-29 22:22:02.750092+07	f	0
502	2026-05-30 09:00:44.549375+07	2026-05-30 09:00:44.549375+07	2026-05-30 09:01:22.148883+07	46	BMP	Lusin	7000	50	6	Rp	270	d4af7034	BMP-2605-018-915e3416-844d834b	2026-05-30 09:00:44.549352+07	2026-05-30 09:00:44.549352+07	f	0
503	2026-05-30 09:00:44.56223+07	2026-05-30 09:00:44.56223+07	2026-05-30 09:01:22.148883+07	20	baskom jago	Lusin	5600	50	15	Rp	270	0cf3ccbd	BMP-2605-018-915e3416-63bf2ea7	2026-05-30 09:00:44.562221+07	2026-05-30 09:00:44.562221+07	f	0
504	2026-05-30 09:00:44.570302+07	2026-05-30 09:00:44.570302+07	2026-05-30 09:01:22.148883+07	45	Wakul Morris Super	Lusin	5700	40	6	Rp	270	dff9b581	BMP-2605-018-915e3416-1bd6b300	2026-05-30 09:00:44.570297+07	2026-05-30 09:00:44.570297+07	f	0
505	2026-05-30 09:01:22.161852+07	2026-05-30 09:01:22.161852+07	\N	46	BMP	Lusin	7100	50	6	Rp	270	458b2d66	BMP-2605-018-915e3416-60cee0ac	2026-05-30 09:01:22.161835+07	2026-05-30 09:01:22.161835+07	f	0
506	2026-05-30 09:01:22.170073+07	2026-05-30 09:01:22.170073+07	\N	20	baskom jago	Lusin	7000	50	15	Rp	270	a43bf349	BMP-2605-018-915e3416-9def8d61	2026-05-30 09:01:22.170065+07	2026-05-30 09:01:22.170066+07	f	0
507	2026-05-30 09:01:22.177755+07	2026-05-30 09:01:22.177755+07	\N	45	Wakul Morris Super	Lusin	6700	40	6	Rp	270	4f80c9d6	BMP-2605-018-915e3416-62b9a648	2026-05-30 09:01:22.177749+07	2026-05-30 09:01:22.177749+07	f	0
508	2026-06-01 09:30:00.673992+07	2026-06-01 09:30:00.673992+07	\N	18	bak kuping12	Lusin	14000	30	3	Rp	271	ee2330b3	BMP-2606-001-a849f0da-363a9b02	2026-06-01 09:30:00.673967+07	2026-06-01 09:30:00.673967+07	f	0
509	2026-06-01 09:30:00.687655+07	2026-06-01 09:30:00.687655+07	\N	46	BMP	Lusin	7100	50	10	Rp	271	3d755088	BMP-2606-001-a849f0da-292ae7cc	2026-06-01 09:30:00.68763+07	2026-06-01 09:30:00.687631+07	f	0
510	2026-06-01 09:30:00.696269+07	2026-06-01 09:30:00.696269+07	\N	22	baskom panda super	Lusin	8400	40	3	Rp	271	26f27608	BMP-2606-001-a849f0da-f40f59e6	2026-06-01 09:30:00.696248+07	2026-06-01 09:30:00.696248+07	f	0
511	2026-06-08 16:57:38.735013+07	2026-06-08 16:57:38.735013+07	\N	54	[DEMO] Kantong HDPE Bening 30x50 (Tebal 0.03)	Kg	23500	1	50	Rp	272	264c5475	DEMO-INV-001-d8a468c0-p1	2026-06-08 16:57:38.734773+07	2026-06-08 16:57:38.734774+07	f	0
512	2026-06-08 16:57:38.741964+07	2026-06-08 16:57:38.741964+07	\N	56	[DEMO] Cup Plastik 16oz PP Tebal 7gr	Karton	135000	1	100	Rp	273	799de44e	DEMO-INV-002-586d8104-p1	2026-06-08 16:57:38.741715+07	2026-06-08 16:57:38.741716+07	f	0
520	2026-06-06 12:00:00+07	2026-06-06 12:00:00+07	\N	18	bak kuping12	-	13000	30	50	Rp	276	3af5866e70a642ca6989f90c2c1837de	BMP-2605-001-88774ad6-45cd57c1	2026-06-06 12:00:00+07	2026-06-06 12:00:00+07	f	0
521	2026-06-06 12:00:00+07	2026-06-06 12:00:00+07	\N	16	baskom mawar	-	7400	50	15	Rp	276	fef706370e151fb9631f90d15ab30b60	BMP-2605-001-431c12b9-e0ff9b55	2026-06-06 12:00:00+07	2026-06-06 12:00:00+07	f	0
522	2026-06-06 12:00:00+07	2026-06-06 12:00:00+07	\N	46	BMP	Lusin	7200	50	15	Rp	276	e7b993f691998f75f7b493e29f72def3	BMP-2605-001-654e7f6c-c0223fbd	2026-06-06 12:00:00+07	2026-06-06 12:00:00+07	f	0
523	2026-06-06 12:00:00+07	2026-06-06 12:00:00+07	\N	20	baskom jago	-	7000	50	7	Rp	276	15522cda21e7b4c2fb6338efed4c6058	BMP-2605-001-f789cb57-e30e0ccc	2026-06-06 12:00:00+07	2026-06-06 12:00:00+07	f	0
\.


--
-- Data for Name: settings; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.settings (id, created_at, updated_at, deleted_at, client_name, client_logo, address_line1, province, postal_code, phone_number, email_address, tax_number, listrik_bulanan, jumlah_mesin, jumlah_karyawan, gaji_harian, hari_kerja_sebulan, biaya_karung_per1000, unique_id, slug, date_created, last_updated, is_demo, hours_per_day) FROM stdin;
2	2026-05-26 16:07:14.013985+07	2026-05-26 16:07:14.013985+07	\N	BMP - Bintang Makmur Plastindo (DEMO)		Jl. Industri Raya No. 45, Kawasan Industri Candi	Jawa Tengah	50181	024-7654321	info@bintangmakmurplastindo.com	01.234.567.8-901.000	32500000	6	15	85000	26	2150000	75db9bf9	bmp-bintang-makmur-plastindo	2026-05-26 16:07:14.009304+07	2026-05-26 16:07:14.009304+07	t	24
1	2026-04-29 02:22:16.281+07	2026-05-30 15:29:08.965445+07	\N	CV. BAHTERA MULYA PLASTIK		jl. arimbi, RT04 RW 01 Desa Ngrimbi	Jatim		088986084722	bahteramulyap@gmail.com		36000000	5	21	85000	26	1700000		main-settings	2026-04-29 02:23:40.124+07	2026-05-30 15:29:08.840811+07	f	24
3	2026-06-08 16:57:38.688542+07	2026-06-08 16:57:38.688542+07	\N	BMP - Bintang Makmur Plastindo (DEMO)		Jl. Industri Demo No. 45, Kawasan Industri Candi	Jawa Tengah	50181	024-7654321	demo-info@bintangmakmurplastindo.com	01.234.567.8-901.000	32500000	6	15	85000	26	2150000	b4a40a0c	bmp-bintang-makmur-plastindo-demo	2026-06-08 16:57:38.688315+07	2026-06-08 16:57:38.688315+07	t	24
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.users (id, created_at, updated_at, deleted_at, username, password) FROM stdin;
1	2026-04-28 17:50:41.780445+07	2026-04-28 17:50:41.780445+07	\N	admin	$2a$10$.FxDhLPcBMvJAJH.G..pCuJnmIi9Oe4MCdCz9V7n6HIiDqD1Kt48W
2	2026-05-26 15:27:59.632758+07	2026-05-26 15:27:59.632758+07	\N	dedi	$2a$10$k0rkVB9JIJ0KybFb2cft1.36XhksNpg2yuMLcs6HUB/fyyhKtH2lC
3	2026-05-26 15:27:59.737478+07	2026-05-26 15:27:59.737478+07	\N	muizz	$2a$10$BGV4t3yy8tJN1mxKx9/XpeO310/rZecKOih20bagvSIFWrvklBsMi
\.


--
-- Name: adms_devices_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.adms_devices_id_seq', 1, true);


--
-- Name: attendance_logs_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.attendance_logs_id_seq', 350, true);


--
-- Name: bahan_nono_items_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.bahan_nono_items_id_seq', 94, true);


--
-- Name: bahan_nonos_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.bahan_nonos_id_seq', 93, true);


--
-- Name: cash_flows_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.cash_flows_id_seq', 474, true);


--
-- Name: clients_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.clients_id_seq', 42, true);


--
-- Name: employees_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.employees_id_seq', 27, true);


--
-- Name: invoice_payments_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.invoice_payments_id_seq', 27, true);


--
-- Name: invoices_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.invoices_id_seq', 276, true);


--
-- Name: machine_bonus_logs_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.machine_bonus_logs_id_seq', 21, true);


--
-- Name: master_products_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.master_products_id_seq', 56, true);


--
-- Name: payrolls_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.payrolls_id_seq', 59, true);


--
-- Name: pembayarans_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.pembayarans_id_seq', 1, false);


--
-- Name: pembelian_barangs_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.pembelian_barangs_id_seq', 1, false);


--
-- Name: pembelian_items_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.pembelian_items_id_seq', 1, false);


--
-- Name: products_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.products_id_seq', 523, true);


--
-- Name: settings_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.settings_id_seq', 3, true);


--
-- Name: users_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.users_id_seq', 3, true);


--
-- Name: adms_devices adms_devices_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.adms_devices
    ADD CONSTRAINT adms_devices_pkey PRIMARY KEY (id);


--
-- Name: attendance_logs attendance_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attendance_logs
    ADD CONSTRAINT attendance_logs_pkey PRIMARY KEY (id);


--
-- Name: bahan_nono_items bahan_nono_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bahan_nono_items
    ADD CONSTRAINT bahan_nono_items_pkey PRIMARY KEY (id);


--
-- Name: bahan_nonos bahan_nonos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bahan_nonos
    ADD CONSTRAINT bahan_nonos_pkey PRIMARY KEY (id);


--
-- Name: cash_flows cash_flows_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cash_flows
    ADD CONSTRAINT cash_flows_pkey PRIMARY KEY (id);


--
-- Name: clients clients_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.clients
    ADD CONSTRAINT clients_pkey PRIMARY KEY (id);


--
-- Name: employees employees_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.employees
    ADD CONSTRAINT employees_pkey PRIMARY KEY (id);


--
-- Name: invoice_payments invoice_payments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoice_payments
    ADD CONSTRAINT invoice_payments_pkey PRIMARY KEY (id);


--
-- Name: invoices invoices_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT invoices_pkey PRIMARY KEY (id);


--
-- Name: machine_bonus_logs machine_bonus_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.machine_bonus_logs
    ADD CONSTRAINT machine_bonus_logs_pkey PRIMARY KEY (id);


--
-- Name: master_products master_products_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.master_products
    ADD CONSTRAINT master_products_pkey PRIMARY KEY (id);


--
-- Name: payrolls payrolls_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payrolls
    ADD CONSTRAINT payrolls_pkey PRIMARY KEY (id);


--
-- Name: pembayarans pembayarans_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pembayarans
    ADD CONSTRAINT pembayarans_pkey PRIMARY KEY (id);


--
-- Name: pembelian_barangs pembelian_barangs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pembelian_barangs
    ADD CONSTRAINT pembelian_barangs_pkey PRIMARY KEY (id);


--
-- Name: pembelian_items pembelian_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pembelian_items
    ADD CONSTRAINT pembelian_items_pkey PRIMARY KEY (id);


--
-- Name: products products_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT products_pkey PRIMARY KEY (id);


--
-- Name: settings settings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.settings
    ADD CONSTRAINT settings_pkey PRIMARY KEY (id);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: idx_adms_devices_deleted_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_adms_devices_deleted_at ON public.adms_devices USING btree (deleted_at);


--
-- Name: idx_adms_devices_is_demo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_adms_devices_is_demo ON public.adms_devices USING btree (is_demo);


--
-- Name: idx_adms_devices_serial_number; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_adms_devices_serial_number ON public.adms_devices USING btree (serial_number);


--
-- Name: idx_attendance_logs_check_out_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_attendance_logs_check_out_time ON public.attendance_logs USING btree (check_out_time);


--
-- Name: idx_attendance_logs_deleted_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_attendance_logs_deleted_at ON public.attendance_logs USING btree (deleted_at);


--
-- Name: idx_attendance_logs_is_demo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_attendance_logs_is_demo ON public.attendance_logs USING btree (is_demo);


--
-- Name: idx_attendance_logs_log_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_attendance_logs_log_time ON public.attendance_logs USING btree (log_time);


--
-- Name: idx_attendance_logs_work_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_attendance_logs_work_date ON public.attendance_logs USING btree (work_date);


--
-- Name: idx_bahan_nono_items_deleted_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_bahan_nono_items_deleted_at ON public.bahan_nono_items USING btree (deleted_at);


--
-- Name: idx_bahan_nonos_deleted_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_bahan_nonos_deleted_at ON public.bahan_nonos USING btree (deleted_at);


--
-- Name: idx_bahan_nonos_is_demo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_bahan_nonos_is_demo ON public.bahan_nonos USING btree (is_demo);


--
-- Name: idx_cash_flows_deleted_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cash_flows_deleted_at ON public.cash_flows USING btree (deleted_at);


--
-- Name: idx_cash_flows_is_demo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cash_flows_is_demo ON public.cash_flows USING btree (is_demo);


--
-- Name: idx_clients_deleted_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_clients_deleted_at ON public.clients USING btree (deleted_at);


--
-- Name: idx_clients_is_demo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_clients_is_demo ON public.clients USING btree (is_demo);


--
-- Name: idx_clients_slug; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_clients_slug ON public.clients USING btree (slug);


--
-- Name: idx_employees_deleted_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_employees_deleted_at ON public.employees USING btree (deleted_at);


--
-- Name: idx_employees_is_demo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_employees_is_demo ON public.employees USING btree (is_demo);


--
-- Name: idx_invoice_payments_deleted_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_invoice_payments_deleted_at ON public.invoice_payments USING btree (deleted_at);


--
-- Name: idx_invoices_deleted_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_invoices_deleted_at ON public.invoices USING btree (deleted_at);


--
-- Name: idx_invoices_is_demo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_invoices_is_demo ON public.invoices USING btree (is_demo);


--
-- Name: idx_invoices_slug; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_invoices_slug ON public.invoices USING btree (slug);


--
-- Name: idx_machine_bonus_logs_deleted_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_machine_bonus_logs_deleted_at ON public.machine_bonus_logs USING btree (deleted_at);


--
-- Name: idx_machine_bonus_logs_is_demo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_machine_bonus_logs_is_demo ON public.machine_bonus_logs USING btree (is_demo);


--
-- Name: idx_master_products_deleted_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_master_products_deleted_at ON public.master_products USING btree (deleted_at);


--
-- Name: idx_master_products_is_demo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_master_products_is_demo ON public.master_products USING btree (is_demo);


--
-- Name: idx_master_products_slug; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_master_products_slug ON public.master_products USING btree (slug);


--
-- Name: idx_payrolls_deleted_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payrolls_deleted_at ON public.payrolls USING btree (deleted_at);


--
-- Name: idx_payrolls_is_demo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payrolls_is_demo ON public.payrolls USING btree (is_demo);


--
-- Name: idx_pembayarans_deleted_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pembayarans_deleted_at ON public.pembayarans USING btree (deleted_at);


--
-- Name: idx_pembelian_barangs_deleted_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pembelian_barangs_deleted_at ON public.pembelian_barangs USING btree (deleted_at);


--
-- Name: idx_pembelian_barangs_is_demo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pembelian_barangs_is_demo ON public.pembelian_barangs USING btree (is_demo);


--
-- Name: idx_pembelian_items_deleted_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pembelian_items_deleted_at ON public.pembelian_items USING btree (deleted_at);


--
-- Name: idx_products_deleted_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_products_deleted_at ON public.products USING btree (deleted_at);


--
-- Name: idx_products_slug; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_products_slug ON public.products USING btree (slug);


--
-- Name: idx_settings_deleted_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_settings_deleted_at ON public.settings USING btree (deleted_at);


--
-- Name: idx_settings_is_demo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_settings_is_demo ON public.settings USING btree (is_demo);


--
-- Name: idx_settings_slug; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_settings_slug ON public.settings USING btree (slug);


--
-- Name: idx_users_deleted_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_users_deleted_at ON public.users USING btree (deleted_at);


--
-- Name: idx_users_username; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_users_username ON public.users USING btree (username);


--
-- Name: bahan_nono_items fk_bahan_nono_items_bahan_nono; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bahan_nono_items
    ADD CONSTRAINT fk_bahan_nono_items_bahan_nono FOREIGN KEY (bahan_nono_id) REFERENCES public.bahan_nonos(id);


--
-- Name: bahan_nono_items fk_bahan_nonos_items; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bahan_nono_items
    ADD CONSTRAINT fk_bahan_nonos_items FOREIGN KEY (bahan_nono_id) REFERENCES public.bahan_nonos(id);


--
-- Name: cash_flows fk_cash_flows_payment_ref; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cash_flows
    ADD CONSTRAINT fk_cash_flows_payment_ref FOREIGN KEY (payment_ref_id) REFERENCES public.invoice_payments(id);


--
-- Name: invoices fk_invoices_client; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT fk_invoices_client FOREIGN KEY (client_id) REFERENCES public.clients(id);


--
-- Name: invoice_payments fk_invoices_payments; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoice_payments
    ADD CONSTRAINT fk_invoices_payments FOREIGN KEY (invoice_id) REFERENCES public.invoices(id);


--
-- Name: machine_bonus_logs fk_machine_bonus_logs_employee; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.machine_bonus_logs
    ADD CONSTRAINT fk_machine_bonus_logs_employee FOREIGN KEY (employee_id) REFERENCES public.employees(id);


--
-- Name: payrolls fk_payrolls_employee; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payrolls
    ADD CONSTRAINT fk_payrolls_employee FOREIGN KEY (employee_id) REFERENCES public.employees(id);


--
-- Name: pembayarans fk_pembayarans_invoice; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pembayarans
    ADD CONSTRAINT fk_pembayarans_invoice FOREIGN KEY (invoice_id) REFERENCES public.invoices(id);


--
-- Name: pembelian_items fk_pembelian_barangs_items; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pembelian_items
    ADD CONSTRAINT fk_pembelian_barangs_items FOREIGN KEY (pembelian_id) REFERENCES public.pembelian_barangs(id);


--
-- Name: pembelian_items fk_pembelian_items_pembelian; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pembelian_items
    ADD CONSTRAINT fk_pembelian_items_pembelian FOREIGN KEY (pembelian_id) REFERENCES public.pembelian_barangs(id);


--
-- Name: products fk_products_invoice; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT fk_products_invoice FOREIGN KEY (invoice_id) REFERENCES public.invoices(id);


--
-- Name: products fk_products_master_item; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT fk_products_master_item FOREIGN KEY (master_item_id) REFERENCES public.master_products(id);


--
-- PostgreSQL database dump complete
--

\unrestrict bn5X67ueMLAFwPs893Qh2eGvQHndcVu544i5O6So2za6wBHg4Zfu4hX4aNPRtjJ

