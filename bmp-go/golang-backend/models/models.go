package models

import (
	"time"

	"gorm.io/gorm"
)

type User struct {
	gorm.Model
	Username string `gorm:"uniqueIndex;not null"`
	Password string `gorm:"not null"`
}

type Client struct {
	gorm.Model
	SaldoTitipan float64 `gorm:"type:decimal(15,2);default:0"`
	ClientName   string  `gorm:"size:200"`
	AddressLine1 string  `gorm:"size:200"`
	ClientLogo   string  `gorm:"size:255"`
	Province     string  `gorm:"size:100"`
	PostalCode   string  `gorm:"size:10"`
	PhoneNumber  string  `gorm:"size:100"`
	EmailAddress string  `gorm:"size:100"`
	TaxNumber    string  `gorm:"size:100"`
	UniqueID     string  `gorm:"size:100"`
	Slug         string  `gorm:"size:500"`
	IsDemo       bool    `gorm:"index;default:false"`
	DateCreated  time.Time
	LastUpdated  time.Time
}

type Invoice struct {
	gorm.Model
	Title        string `gorm:"size:100"`
	Number       string `gorm:"size:100"`
	DueDate      *time.Time
	PaymentTerms string `gorm:"size:100;default:'14 days'"`
	Status       string `gorm:"size:100;default:'DRAFT'"`
	Notes        string `gorm:"type:text"`
	ClientID     *uint
	Client       Client `gorm:"foreignKey:ClientID"`
	UniqueID     string `gorm:"size:100"`
	Slug         string `gorm:"size:500;uniqueIndex"`
	IsDemo       bool   `gorm:"index;default:false"`
	DateCreated  time.Time
	LastUpdated  time.Time
	Payments     []InvoicePayment `gorm:"foreignKey:InvoiceID;constraint:OnUpdate:CASCADE,OnDelete:CASCADE;"`
}

type Settings struct {
	gorm.Model
	ClientName         string  `gorm:"size:200"`
	ClientLogo         string  `gorm:"size:255"`
	AddressLine1       string  `gorm:"size:200"`
	Province           string  `gorm:"size:100"`
	PostalCode         string  `gorm:"size:10"`
	PhoneNumber        string  `gorm:"size:100"`
	EmailAddress       string  `gorm:"size:100"`
	TaxNumber          string  `gorm:"size:100"`
	ListrikBulanan     float64 `gorm:"default:30000000"`
	JumlahMesin        int     `gorm:"default:5"`
	JumlahKaryawan     int     `gorm:"default:19"`
	GajiHarian         float64 `gorm:"default:80000"`
	HariKerjaSebulan   int     `gorm:"default:26"`
	BiayaKarungPer1000 float64 `gorm:"default:2100000"`
	HoursPerDay        int     `gorm:"default:24"`
	UniqueID           string  `gorm:"size:100"`
	Slug               string  `gorm:"size:500"`
	IsDemo             bool    `gorm:"index;default:false"`
	DateCreated        time.Time
	LastUpdated        time.Time
}

type MasterProduct struct {
	gorm.Model
	Title       string  `gorm:"size:100"`
	Description string  `gorm:"type:text"`
	Unit        string  `gorm:"size:50"`
	Price       float64 `gorm:"default:0"`
	BeratGram   float64 `gorm:"default:0"`
	CycleTime   float64 `gorm:"default:0"`
	Cavity      int     `gorm:"default:1"`
	RejectRate  float64 `gorm:"default:0"`
	UniqueID    string  `gorm:"size:100"`
	Slug        string  `gorm:"size:500"`
	IsDemo      bool    `gorm:"index;default:false"`
	DateCreated time.Time
	LastUpdated time.Time
}

type Product struct {
	gorm.Model
	MasterItemID *uint
	MasterItem   MasterProduct `gorm:"foreignKey:MasterItemID"`
	Title        string        `gorm:"size:100"`
	Unit         string        `gorm:"size:50"`
	Price        float64       `gorm:"default:0"`
	JumlahLusin  float64       `gorm:"default:1"`
	Quantity     float64       `gorm:"default:0"`
	IsKhusus     bool          `gorm:"default:false"`
	HargaBeli    float64       `gorm:"default:0"`
	Currency     string        `gorm:"size:100;default:'Rp'"`
	InvoiceID    *uint
	Invoice      Invoice `gorm:"foreignKey:InvoiceID"`
	UniqueID     string  `gorm:"size:100"`
	Slug         string  `gorm:"size:500"`
	DateCreated  time.Time
	LastUpdated  time.Time
}

type InvoicePayment struct {
	gorm.Model
	InvoiceID     uint
	Invoice       Invoice   `gorm:"foreignKey:InvoiceID"`
	PaymentDate   time.Time `gorm:"type:date"`
	PaymentAmount int
	PaymentMethod string `gorm:"size:50"`
	DateCreated   time.Time
}

type CashFlow struct {
	gorm.Model
	TransactionDate time.Time `gorm:"type:date"`
	TransactionType string    `gorm:"size:10"`
	Description     string    `gorm:"size:255"`
	Amount          float64   `gorm:"default:0"`
	PaymentRefID    *uint
	PaymentRef      InvoicePayment `gorm:"foreignKey:PaymentRefID;constraint:OnUpdate:CASCADE,OnDelete:CASCADE;"`
	IsDemo          bool           `gorm:"index;default:false"`
	DateCreated     time.Time
}

type BahanNono struct {
	gorm.Model
	Tanggal     time.Time `gorm:"type:date"`
	Nominal     float64   `gorm:"default:0"`
	Notes       string    `gorm:"type:text"`
	Tagihan     string    `gorm:"size:255"`
	TotalHarga  float64   `gorm:"default:0"`
	IsDemo      bool      `gorm:"index;default:false"`
	DateCreated time.Time
	Items       []BahanNonoItem `gorm:"foreignKey:BahanNonoID"`
}

type BahanNonoItem struct {
	gorm.Model
	BahanNonoID uint
	BahanNono   BahanNono `gorm:"foreignKey:BahanNonoID;constraint:OnUpdate:CASCADE,OnDelete:CASCADE;"`
	JenisBahan  string    `gorm:"size:50"`
	Kuantitas   float64   `gorm:"default:0"`
	Unit        string    `gorm:"size:20;default:'Kg'"`
	Rate        float64   `gorm:"default:0"`
}

type PembelianBarang struct {
	gorm.Model
	Supplier   string          `gorm:"size:255"`
	Tanggal    time.Time       `gorm:"type:date"`
	Keterangan string          `gorm:"type:text"`
	TotalHarga float64         `gorm:"default:0"`
	CaraBayar  string          `gorm:"size:20;default:'HUTANG'"`
	IsDemo     bool            `gorm:"index;default:false"`
	Items      []PembelianItem `gorm:"foreignKey:PembelianID"`
}

type PembelianItem struct {
	gorm.Model
	PembelianID uint
	Pembelian   PembelianBarang `gorm:"foreignKey:PembelianID;constraint:OnUpdate:CASCADE,OnDelete:CASCADE;"`
	NamaBarang  string          `gorm:"size:255"`
	JumlahLusin float64         `gorm:"default:1"`
	Kuantitas   float64         `gorm:"default:0"`
	Unit        string          `gorm:"size:20;default:'Pcs'"`
	HargaSatuan float64         `gorm:"default:0"`
}
type Pembayaran struct {
	gorm.Model
	InvoiceID    uint
	Invoice      Invoice `gorm:"foreignKey:InvoiceID"`
	TanggalBayar time.Time
	JumlahBayar  float64 `gorm:"type:decimal(15,2)"`
	Keterangan   string  `gorm:"size:255"`
}

type Employee struct {
	gorm.Model
	Name           string  `gorm:"size:255;not null"`
	Position       string  `gorm:"size:100"`
	SalaryAmount   float64 `gorm:"type:decimal(15,2)"`
	IsActive       bool    `gorm:"default:true"`
	FingerprintPIN string  `gorm:"size:100"`
	IsDemo         bool    `gorm:"index;default:false"`
}

type Payroll struct {
	gorm.Model
	EmployeeID      uint
	Employee        Employee  `gorm:"foreignKey:EmployeeID"`
	PaymentDate     time.Time `gorm:"type:date"`
	Amount          float64   `gorm:"type:decimal(15,2)"`
	AttendanceCount int       `gorm:"default:0"`
	DailyRate       float64   `gorm:"type:decimal(15,2)"`
	Description     string    `gorm:"size:255"`
	IsDemo          bool      `gorm:"index;default:false"`
}

type AdmsDevice struct {
	gorm.Model
	SerialNumber string `gorm:"uniqueIndex;size:100;not null"`
	Alias        string `gorm:"size:100"`
	LastActivity time.Time
	IsDemo       bool `gorm:"index;default:false"`
}

type AttendanceLog struct {
	gorm.Model
	DeviceSN     string     `gorm:"size:100"`
	EmployeePIN  string     `gorm:"size:100"`
	VerifyType   int        // 1: Fingerprint, 4: Card, 15: Face, etc.
	VerifyState  int        // 0: Check In, 1: Check Out, etc.
	LogTime      time.Time  `gorm:"index"`
	CheckOutTime *time.Time `gorm:"index"`
	// WorkDate = tanggal kerja riil (WIB). Untuk shift malam yang check-out
	// lewat tengah malam, WorkDate tetap mengacu tanggal MASUK (check-in).
	// Contoh: masuk 23:00 Rabu → check-out 07:00 Kamis → WorkDate = Rabu.
	WorkDate     time.Time  `gorm:"type:date;index"`
	// LateMinutes = menit keterlambatan masuk (0 jika tepat waktu atau lebih cepat).
	// Dihitung saat log VerifyState=0 (check-in) berdasarkan shift standar.
	LateMinutes  int        `gorm:"default:0"`
	Alasan       string     `gorm:"size:255"`
	IsDemo       bool       `gorm:"index;default:false"`
}

type MachineBonusLog struct {
	gorm.Model
	EmployeeID      uint
	Employee        Employee  `gorm:"foreignKey:EmployeeID"`
	MachineName     string    `gorm:"size:100"`
	ShiftType       string    `gorm:"size:50"`
	BonusAmount     float64   `gorm:"type:decimal(15,2)"`
	JumlahPerolehan int       `gorm:"default:0"` // Jumlah produksi (bilangan bulat)
	Date            time.Time `gorm:"type:date"`
	IsDemo          bool      `gorm:"index;default:false"`
}
