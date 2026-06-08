package com.posbah.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.posbah.app.ui.navigation.Screen
import com.posbah.app.ui.screens.bmp.cashflow.CashFlowScreen
import com.posbah.app.ui.screens.bmp.clients.ClientEditScreen
import com.posbah.app.ui.screens.bmp.clients.ClientsScreen
import com.posbah.app.ui.screens.bmp.dashboard.BmpDashboardScreen
import com.posbah.app.ui.screens.bmp.employees.EmployeesScreen
import com.posbah.app.ui.screens.bmp.employees.PayrollScreen
import com.posbah.app.ui.screens.bmp.invoices.InvoiceDetailScreen
import com.posbah.app.ui.screens.bmp.invoices.InvoiceFormScreen
import com.posbah.app.ui.screens.bmp.invoices.InvoicesListScreen
import com.posbah.app.ui.screens.bmp.payments.PaymentsListScreen
import com.posbah.app.ui.screens.bmp.products.MasterProductsScreen
import com.posbah.app.ui.screens.bmp.settings.SettingsScreen
import com.posbah.app.ui.screens.lock.LockScreen
import com.posbah.app.ui.screens.login.LoginScreen
import com.posbah.app.ui.screens.splash.SplashScreen
import com.posbah.app.ui.screens.tenant.TenantPickerScreen

@Composable
fun PosBahRoot() {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = Screen.Splash.route) {

        composable(Screen.Splash.route) {
            SplashScreen(
                onGoLogin = {
                    nav.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onGoLock = {
                    nav.navigate(Screen.Lock.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onGoBmpDashboard = {
                    nav.navigate(Screen.BmpDashboard.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onLoggedIn = {
                    nav.navigate(Screen.BmpDashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNeedTenantPick = {
                    nav.navigate(Screen.TenantPicker.route)
                }
            )
        }

        composable(Screen.Lock.route) {
            LockScreen(
                onUnlocked = {
                    nav.navigate(Screen.BmpDashboard.route) {
                        popUpTo(Screen.Lock.route) { inclusive = true }
                    }
                },
                onLogout = {
                    nav.navigate(Screen.Login.route) {
                        popUpTo(Screen.Lock.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.TenantPicker.route) {
            TenantPickerScreen(
                onSelected = {
                    nav.navigate(Screen.BmpDashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onLogout = {
                    nav.navigate(Screen.Login.route) {
                        popUpTo(Screen.TenantPicker.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.BmpDashboard.route) {
            BmpDashboardScreen(
                onNavigate = { route -> nav.navigate(route) },
                onLogout = {
                    nav.navigate(Screen.Login.route) {
                        popUpTo(Screen.BmpDashboard.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.BmpClients.route) {
            ClientsScreen(
                onBack = { nav.popBackStack() },
                onEdit = { id -> nav.navigate(Screen.BmpClientEdit.build(id)) }
            )
        }

        composable(
            route = Screen.BmpClientEdit.route,
            arguments = listOf(navArgument("id") { type = NavType.StringType; defaultValue = "-1" })
        ) {
            ClientEditScreen(onDone = { nav.popBackStack() })
        }

        composable(Screen.BmpInvoices.route) {
            InvoicesListScreen(
                onBack = { nav.popBackStack() },
                onCreate = { nav.navigate(Screen.BmpCreateInvoice.build(null)) },
                onOpen = { id -> nav.navigate(Screen.BmpInvoiceDetail.build(id)) }
            )
        }

        composable(
            route = Screen.BmpInvoiceDetail.route,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) {
            InvoiceDetailScreen(
                onBack = { nav.popBackStack() },
                onEdit = { id -> nav.navigate(Screen.BmpCreateInvoice.build(id)) }
            )
        }

        composable(
            route = Screen.BmpCreateInvoice.route,
            arguments = listOf(navArgument("id") { type = NavType.StringType; defaultValue = "-1" })
        ) {
            InvoiceFormScreen(onDone = { nav.popBackStack() })
        }

        composable(Screen.BmpProducts.route) {
            MasterProductsScreen(onBack = { nav.popBackStack() })
        }

        composable(Screen.BmpPayments.route) {
            PaymentsListScreen(onBack = { nav.popBackStack() })
        }

        composable(Screen.BmpCashFlow.route) {
            CashFlowScreen(onBack = { nav.popBackStack() })
        }

        composable(Screen.BmpEmployees.route) {
            EmployeesScreen(onBack = { nav.popBackStack() })
        }

        composable(Screen.BmpPayroll.route) {
            PayrollScreen(onBack = { nav.popBackStack() })
        }

        composable(Screen.BmpSettings.route) {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
    }
}
