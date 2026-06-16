package com.posbah.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.posbah.app.ui.navigation.Screen
import com.posbah.app.ui.screens.bmp.bahanbaku.BahanBakuFormScreen
import com.posbah.app.ui.screens.bmp.bahanbaku.BahanBakuListScreen
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
import com.posbah.app.ui.screens.bmp.settings.PrintSettingsScreen
import com.posbah.app.ui.screens.lock.LockScreen
import com.posbah.app.ui.screens.login.LoginScreen
import com.posbah.app.ui.screens.splash.SplashScreen
import com.posbah.app.ui.screens.tenant.TenantPickerScreen
import com.posbah.app.ui.screens.pos.PosScreen
import com.posbah.app.ui.screens.pos.MarginAnalysisScreen
import com.posbah.app.ui.screens.owner.outlet.OutletControlScreen
import com.posbah.app.ui.screens.owner.employee.EmployeeManagementScreen
import com.posbah.app.ui.screens.rental.RentalScreen
import com.posbah.app.ui.screens.laundry.LaundryScreen
import com.posbah.app.ui.screens.tenant.SystemSelectionScreen
import com.posbah.app.ui.screens.admin.AdminPanelScreen
import com.posbah.app.ui.screens.bmp.qr.QrScannerScreen
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun PosBahRoot(
    viewModel: PosBahRootViewModel = hiltViewModel()
) {
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()
    val updateState by viewModel.updateState.collectAsState()

    val goDashboard = { popUpRoute: String ->
        scope.launch {
            try {
                val route = viewModel.getDashboardRoute()
                nav.navigate(route) {
                    popUpTo(popUpRoute) { inclusive = true }
                }
            } catch (e: Exception) {
                android.util.Log.e("PosBahRoot", "Navigation error", e)
                nav.navigate(Screen.Login.route) {
                    popUpTo(popUpRoute) { inclusive = true }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                    goDashboard(Screen.Splash.route)
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onLoggedIn = {
                    goDashboard(Screen.Login.route)
                },
                onNeedTenantPick = {
                    nav.navigate(Screen.TenantPicker.route)
                }
            )
        }

        composable(Screen.Lock.route) {
            LockScreen(
                onUnlocked = {
                    goDashboard(Screen.Lock.route)
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
                    goDashboard(Screen.Login.route)
                },
                onLogout = {
                    nav.navigate(Screen.Login.route) {
                        popUpTo(Screen.TenantPicker.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.SystemSelection.route) {
            SystemSelectionScreen(
                onSelected = {
                    goDashboard(Screen.SystemSelection.route)
                },
                onLogout = {
                    nav.navigate(Screen.Login.route) {
                        popUpTo(Screen.SystemSelection.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.PosDashboard.route) {
            PosScreen(
                onBack = {
                    nav.navigate(Screen.TenantPicker.route) {
                        popUpTo(Screen.PosDashboard.route) { inclusive = true }
                    }
                },
                onNavigate = { route -> nav.navigate(route) },
                onNavigateToPrintSettings = { nav.navigate(Screen.PrintSettings.build("FNB")) }
            )
        }

        composable(Screen.OutletControl.route) {
            OutletControlScreen(onBack = { nav.popBackStack() })
        }

        composable(Screen.EmployeeManagement.route) {
            EmployeeManagementScreen(onBack = { nav.popBackStack() })
        }

        composable(Screen.RentalDashboard.route) {
            RentalScreen(
                onBack = {
                    nav.navigate(Screen.TenantPicker.route) {
                        popUpTo(Screen.RentalDashboard.route) { inclusive = true }
                    }
                },
                onNavigate = { route -> nav.navigate(route) },
                onNavigateToPrintSettings = { nav.navigate(Screen.PrintSettings.build("RENTAL")) }
            )
        }

        composable(Screen.LaundryDashboard.route) {
            LaundryScreen(
                onBack = {
                    nav.navigate(Screen.TenantPicker.route) {
                        popUpTo(Screen.LaundryDashboard.route) { inclusive = true }
                    }
                },
                onNavigate = { route -> nav.navigate(route) },
                onNavigateToPrintSettings = { nav.navigate(Screen.PrintSettings.build("LAUNDRY")) }
            )
        }

        composable(Screen.MarginAnalysis.route) {
            MarginAnalysisScreen(
                onBack = { nav.popBackStack() }
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
            SettingsScreen(
                onBack = { nav.popBackStack() },
                onNavigateToPrintSettings = { nav.navigate(Screen.PrintSettings.build("BMP")) }
            )
        }

        composable(
            route = Screen.PrintSettings.route,
            arguments = listOf(
                navArgument("moduleKey") {
                    type = NavType.StringType
                    defaultValue = "BMP"
                }
            )
        ) {
            PrintSettingsScreen(onBack = { nav.popBackStack() })
        }

        composable(Screen.BmpBahanBaku.route) {
            BahanBakuListScreen(
                onBack = { nav.popBackStack() },
                onAdd = { nav.navigate(Screen.BmpBahanBakuForm.build(null)) },
                onEdit = { id -> nav.navigate(Screen.BmpBahanBakuForm.build(id)) }
            )
        }

        composable(
            route = Screen.BmpBahanBakuForm.route,
            arguments = listOf(navArgument("id") { type = NavType.StringType; defaultValue = "-1" })
        ) {
            BahanBakuFormScreen(onDone = { nav.popBackStack() })
        }

        composable(Screen.AdminPanel.route) {
            AdminPanelScreen(onBack = { nav.popBackStack() })
        }

        composable(Screen.QrScanner.route) {
            QrScannerScreen(
                onBack = { nav.popBackStack() }
            )
        }
    }
    }

    if (updateState is UpdateState.UpdateRequired) {
        val required = updateState as UpdateState.UpdateRequired
        ForcedUpdateOverlay(
            version = required.version,
            description = required.description
        )
    }
}

@Composable
fun ForcedUpdateOverlay(
    version: String,
    description: String
) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    
    androidx.activity.compose.BackHandler(enabled = true) {
        // Intercept back button to prevent escaping the update screen
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E293B))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF0F172A),
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = "Pembaruan Wajib",
                    tint = Color(0xFFF97316),
                    modifier = Modifier.size(72.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Pembaruan Wajib!",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Versi aplikasi Anda tidak didukung lagi. Silakan unduh versi terbaru untuk melanjutkan penggunaan POSBah.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF94A3B8)
                    ),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "Versi Terbaru: v$version",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF97316)
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFE2E8F0)
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = {
                        uriHandler.openUri("https://www.zedmz.cloud/api/download-apk")
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF97316),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Unduh APK Sekarang",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}
