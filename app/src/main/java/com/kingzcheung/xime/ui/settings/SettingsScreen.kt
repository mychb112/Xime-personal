package com.kingzcheung.xime.ui.settings

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun SettingsScreen(
    initialRoute: String? = null,
    onThemeChanged: () -> Unit = {},
    onWizardBack: () -> Unit = {}
) {
    val navController = rememberNavController()
    val startDestination = when (initialRoute) {
        "manage_dict" -> SettingsRoutes.Dictionary
        "schema" -> SettingsRoutes.Schema
        else -> SettingsRoutes.Main
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(SettingsRoutes.Main) {
            SettingsMainContent(
                onNavigateToSchema = { navController.navigate(SettingsRoutes.Schema) },
                onNavigateToTheme = { navController.navigate(SettingsRoutes.Theme) },
                onNavigateToKeyEffect = { navController.navigate(SettingsRoutes.KeyEffect) },
                onNavigateToLayoutDisplay = { navController.navigate(SettingsRoutes.LayoutDisplay) },
                onNavigateToDictionary = { navController.navigate(SettingsRoutes.Dictionary) },
                onNavigateToFuzzyPinyin = { navController.navigate(SettingsRoutes.FuzzyPinyin) },
                onNavigateToAbout = { navController.navigate(SettingsRoutes.About) },
                onNavigateToClipboard = { navController.navigate(SettingsRoutes.Clipboard) }
            )
        }
        composable(SettingsRoutes.Schema) {
            SchemaSettingsContent(
                onBack = {
                    if (initialRoute == "schema") {
                        navController.navigate(SettingsRoutes.Main) {
                            popUpTo(SettingsRoutes.Main) { inclusive = true }
                        }
                    } else {
                        navController.popBackStack()
                    }
                    onWizardBack()
                },
                onNavigateToRimeFileBrowser = { navController.navigate(SettingsRoutes.RimeFileBrowser) },
                onNavigateToSchemaDictBrowser = { navController.navigate(SettingsRoutes.SchemaDictBrowser) },
                onNavigateToImported = { navController.navigate(SettingsRoutes.SchemaLocal) },
            )
        }
        composable(SettingsRoutes.SchemaLocal) {
            SchemaLocalContent(
                onBack = { navController.popBackStack() },
            )
        }
        composable(SettingsRoutes.Theme) {
            ThemeSettingsContent(
                onBack = { navController.popBackStack() },
                onThemeChanged = onThemeChanged
            )
        }
        composable(SettingsRoutes.KeyEffect) {
            KeyEffectSettingsContent(
                onBack = { navController.popBackStack() }
            )
        }
        composable(SettingsRoutes.LayoutDisplay) {
            LayoutDisplaySettingsContent(
                onBack = { navController.popBackStack() },
            )
        }
        composable(SettingsRoutes.FuzzyPinyin) {
            FuzzyPinyinSettingsContent(
                onBack = { navController.popBackStack() }
            )
        }
        composable(SettingsRoutes.Dictionary) {
            DictionaryHubContent(
                onNavigateToUserDict = { navController.navigate(SettingsRoutes.DictionaryUserDict) },
                onNavigateToCustomPhrase = { navController.navigate(SettingsRoutes.DictionaryCustomPhrase) },
                onNavigateToImportedDict = { navController.navigate(SettingsRoutes.DictionaryImported) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(SettingsRoutes.DictionaryUserDict) {
            UserDictContent(onBack = { navController.popBackStack() })
        }
        composable(SettingsRoutes.DictionaryCustomPhrase) {
            CustomPhraseSettingsContent(onBack = { navController.popBackStack() })
        }
        composable(SettingsRoutes.DictionaryImported) {
            ImportedDictContent(onBack = { navController.popBackStack() })
        }
        composable(SettingsRoutes.SchemaDictBrowser) {
            SchemaDictBrowserContent(
                onBack = { navController.popBackStack() }
            )
        }
        composable(SettingsRoutes.RimeFileBrowser) {
            RimeFileBrowserContent(
                onBack = { navController.popBackStack() }
            )
        }
        composable(SettingsRoutes.Clipboard) {
            ClipboardSettingsContent(
                onBack = { navController.popBackStack() },
            )
        }
        composable(SettingsRoutes.About) {
            AboutContent(
                onBack = { navController.popBackStack() },
                onNavigateToPrivacy = { navController.navigate(SettingsRoutes.Privacy) },
                onNavigateToLicenses = { navController.navigate(SettingsRoutes.Licenses) },
                onNavigateToLogViewer = { navController.navigate(SettingsRoutes.LogViewer) },
                onNavigateToDeveloper = { navController.navigate(SettingsRoutes.Developer) },
                onNavigateToStorageSpace = { navController.navigate(SettingsRoutes.StorageSpace) }
            )
        }
        composable(SettingsRoutes.Developer) {
            DeveloperContent(
                onBack = { navController.popBackStack() },
                onNavigateToHandwritingCapture = { navController.navigate(SettingsRoutes.HandwritingCapture) }
            )
        }
        composable(SettingsRoutes.StorageSpace) {
            StorageSpaceScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable(SettingsRoutes.LogViewer) {
            LogViewerScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(SettingsRoutes.HandwritingCapture) {
            HandwritingCaptureScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(SettingsRoutes.Privacy) {
            PrivacyPolicyContent(
                onBack = { navController.popBackStack() }
            )
        }
        composable(SettingsRoutes.Licenses) {
            LicensesContent(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
