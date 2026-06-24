package com.posbah.app.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Lock : Screen("lock")
    object TenantPicker : Screen("tenant_picker")
    object OutletPicker : Screen("outlet_picker")
    object Dashboard : Screen("dashboard")
    object PosDashboard : Screen("pos")
    object PosSettings : Screen("pos/settings")
    object RentalDashboard : Screen("rental/dashboard")
    object LaundryDashboard : Screen("laundry/dashboard")
    object SystemSelection : Screen("system_selection")
    object MarginAnalysis : Screen("margin_analysis")

    object BmpDashboard : Screen("bmp/dashboard")
    object BmpClients : Screen("bmp/clients")
    object BmpClientEdit : Screen("bmp/clients/edit?id={id}") {
        fun build(id: Long? = null) = "bmp/clients/edit?id=${id ?: -1}"
    }
    object BmpInvoices : Screen("bmp/invoices")
    object BmpInvoiceDetail : Screen("bmp/invoices/{id}") {
        fun build(id: Long) = "bmp/invoices/$id"
    }
    object BmpCreateInvoice : Screen("bmp/invoices/create?id={id}") {
        fun build(id: Long? = null) = "bmp/invoices/create?id=${id ?: -1}"
    }
    object BmpProducts : Screen("bmp/products")
    object BmpProductEdit : Screen("bmp/products/edit?id={id}") {
        fun build(id: Long? = null) = "bmp/products/edit?id=${id ?: -1}"
    }
    object BmpPayments : Screen("bmp/payments")
    object BmpCashFlow : Screen("bmp/cashflow")
    object BmpEmployees : Screen("bmp/employees")
    object BmpEmployeeEdit : Screen("bmp/employees/edit?id={id}") {
        fun build(id: Long? = null) = "bmp/employees/edit?id=${id ?: -1}"
    }
    object BmpPayroll : Screen("bmp/payroll")
    object BmpSettings : Screen("bmp/settings")
    object PrintSettings : Screen("print/settings/{moduleKey}") {
        fun build(moduleKey: String) = "print/settings/$moduleKey"
    }

    object BmpBahanBaku : Screen("bmp/bahanbaku")
    object BmpBahanBakuForm : Screen("bmp/bahanbaku/form?id={id}") {
        fun build(id: Long? = null) = "bmp/bahanbaku/form?id=${id ?: -1}"
    }
    object BmpStock : Screen("bmp/stock")
    object BmpProductionLog : Screen("bmp/production")
    object AdminPanel : Screen("admin/panel")

    object OutletControl : Screen("owner/outlet_control")
    object EmployeeManagement : Screen("owner/employees")
    object QrScanner : Screen("qr_scanner")
    object Migration : Screen("migration")
}
