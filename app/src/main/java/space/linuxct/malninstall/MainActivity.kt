package space.linuxct.malninstall

import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import space.linuxct.malninstall.R.layout.activity_main
import java.lang.Exception

class MainActivity : AppCompatActivity() {
    private lateinit var actionButton: Button
    private lateinit var resultText: TextView
    private lateinit var followMeText: TextView
    private var packNameList = listOf("com.tencent.mm",
            "com.tencent.mobileqq",
            "com.clubbing.photos",
            "com.redtube.music", //Mad respects PRODAFT, this full list would not have been possible without your report
            "com.taobao.taobao") //From 2021-03-26 Hungarian sample, thanks @malwrhunterteam
    private var foundPackages = mutableListOf<String>()
    private var uninstallAttemptCount = 0
    private lateinit var preferences: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = getPreferences(Context.MODE_PRIVATE)
        setContentView(activity_main)
        actionButton = findViewById(R.id.actionButton)
        resultText = findViewById(R.id.resultTextView)
        followMeText = findViewById(R.id.followMe)
        followMeText.setOnClickListener {
            handleFollowMeClick()
        }
        handle()
    }

    override fun onResume() {
        super.onResume()
        preferences = getPreferences(Context.MODE_PRIVATE)
        uninstallAttemptCount = preferences.getInt(uninstallAttemptCountKey, 0)
        if (uninstallAttemptCount == 0) {
            handle()
            return
        }
        if (detect()){
            showBeginUninstall()
            resultText.text = getString(R.string.error_text)
            if (uninstallAttemptCount <= 3){
                tryUninstall()
            } else {
                startAlternativeFlow()
            }
        } else {
            showRemoveDefaultLauncher()
        }
    }

    //region UI handle

    private fun handle(){
        if (detect()){
            when {
                isInAlternativeFlow() -> {
                    showAlternativeFlow()
                }
                isLauncherDefault() -> {
                    showBeginUninstall()
                }
                else -> {
                    showSetAsLauncher()
                }
            }
        } else {
            when {
                isLauncherDefault() && wasMalwareDetected() -> {
                    showRemoveDefaultLauncher()
                }
                else -> {
                    showNotFound()
                }
            }
        }
    }

    private fun showSetAsLauncher() {
        resultText.text = getString(R.string.showSetAsLauncher_text)
        actionButton.text = getString(R.string.showSetAsLauncher_button)
        actionButton.isEnabled = true
        actionButton.setOnClickListener {
            chooseLauncher()
            handle()
        }
    }

    private fun showBeginUninstall() {
        resultText.text = getString(R.string.showBeginUninstall_text)
        actionButton.text = getString(R.string.showBeginUninstall_button)
        actionButton.isEnabled = true
        actionButton.setOnClickListener {
            uninstallAttemptCount = 0
            preferences.edit().putInt(uninstallAttemptCountKey, 0).apply()
            tryUninstall()
            handle()
        }
    }

    private fun showRemoveDefaultLauncher() {
        resultText.text = getString(R.string.showRemoveDefaultLauncher_text)
        actionButton.text = getString(R.string.showRemoveDefaultLauncher_button)
        actionButton.isEnabled = true
        actionButton.setOnClickListener {
            resetPreferredLauncherAndOpenChooser()
            uninstallAttemptCount = 0
            preferences.edit()
                    .putInt(uninstallAttemptCountKey, uninstallAttemptCount)
                    .putBoolean(isInAlternativeFlowKey, false)
                    .putBoolean(isMalwareDetectedKey, false)
                    .apply()
            handle()
        }
    }

    private fun startAlternativeFlow(){
        resultText.text = getString(R.string.alternativeFlowStart_text)
        actionButton.text = getString(R.string.alternativeFlowStart_button)
        actionButton.isEnabled = true
        actionButton.setOnClickListener {
            resetPreferredLauncherAndOpenChooser()
            uninstallAttemptCount = 0
            preferences.edit()
                    .putInt(uninstallAttemptCountKey, uninstallAttemptCount)
                    .putBoolean(isInAlternativeFlowKey, true)
                    .apply()
            handle()
        }
    }

    private fun showAlternativeFlow(){
        resultText.text = getString(R.string.alternativeFlow_text)
        actionButton.text = getString(R.string.alternativeFlow_button)
        actionButton.isEnabled = true
        actionButton.setOnClickListener {
            tryUninstall()
            handle()
        }
    }

    private fun showNotFound() {
        resultText.text = getString(R.string.showNotFound_text)
        actionButton.text = getString(R.string.showNotFound_button)
        actionButton.isEnabled = false
        actionButton.setOnClickListener {}
    }

    private fun handleFollowMeClick() {
        try {
            packageManager.getPackageInfo("com.twitter.android", 0)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setClassName("com.twitter.android", "com.twitter.app.profiles.ProfileActivity")
            intent.putExtra("user_id", 3401507801L)
            startActivity(intent)
        } catch (e: Exception) {
            handleFallbackFollowMeClick()
        }
    }

    private fun handleFallbackFollowMeClick() {
        //From https://stackoverflow.com/a/12039201
        try {
            packageManager.getPackageInfo("com.twitter.android", 0)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setClassName("com.twitter.android", "com.twitter.android.ProfileActivity")
            intent.putExtra("user_id", 3401507801L)
            startActivity(intent)
        } catch (e: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/linuxct")))
        }
    }

    //endregion

    private fun resetPreferredLauncherAndOpenChooser() {
        val packageManager: PackageManager = packageManager
        val componentName = ComponentName(applicationContext, FakeLauncher::class.java)
        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        chooseLauncher()
        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, PackageManager.DONT_KILL_APP)
    }

    private fun chooseLauncher(){
        if (deviceIsBlacklisted()){
            val handled = chooseLauncherFallback()
            if (handled) return
        }

        val selector = Intent(Intent.ACTION_MAIN)
        selector.addCategory(Intent.CATEGORY_HOME)
        selector.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(selector)
    }

    private fun chooseLauncherFallback(): Boolean {
        var handled: Boolean
        try {
            packageManager.getPackageInfo("com.google.android.permissioncontroller", 0)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setClassName("com.google.android.permissioncontroller", "com.android.packageinstaller.role.ui.HomeSettingsActivity")
            handled = true
            startActivity(intent)
        }
        catch (e: Exception){
            handled = false
        }

        return handled
    }

    private fun isLauncherDefault(): Boolean {
        val filter = IntentFilter(Intent.ACTION_MAIN)
        filter.addCategory(Intent.CATEGORY_HOME)
        val filters: MutableList<IntentFilter> = ArrayList()
        filters.add(filter)
        val myPackageName = packageName
        val activities: List<ComponentName> = ArrayList()
        val packageManager = packageManager as PackageManager
        try {
            packageManager.getPreferredActivities(filters, activities, null)
        } catch (_: Exception){ }

        if (activities.isEmpty()){
            return isLauncherDefaultFallback()
        }

        for (activity in activities) {
            if (myPackageName == activity.packageName) {
                return true
            }
        }
        return false
    }

    private fun isLauncherDefaultFallback(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val currentLauncherName = resolveInfo!!.activityInfo.packageName
        return currentLauncherName == packageName
    }

    private fun isInAlternativeFlow(): Boolean = preferences.getBoolean(isInAlternativeFlowKey, false)

    private fun wasMalwareDetected(): Boolean = preferences.getBoolean(isMalwareDetectedKey, false)

    private fun deviceIsBlacklisted(): Boolean {
        for (device in blacklist){
            if (device == manufacturer){
                return true
            }
        }

        return false
    }

    private fun packageListMatchInDevice(packNameList: List<String>): List<String>{
        val pm = packageManager
        foundPackages = mutableListOf()
        for (packageName in packNameList){
            try {
                pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
                foundPackages.add(packageName)
            } catch (e: PackageManager.NameNotFoundException) {
            }
        }
        return foundPackages
    }

    private fun detect(): Boolean {
        val malwareDetected = packageListMatchInDevice(packNameList).count() > 0
        if (malwareDetected){
            preferences.edit().putBoolean(isMalwareDetectedKey, malwareDetected).apply()
        }
        return malwareDetected
    }

    private fun tryUninstall(){
        uninstallAttemptCount = preferences.getInt(uninstallAttemptCountKey, 1)
        preferences.edit().putInt(uninstallAttemptCountKey, uninstallAttemptCount+1).apply()
        var reqCodeInc = 0
        for (packageName in packageListMatchInDevice(packNameList)){
            val uri: Uri = Uri.fromParts("package", packageName, null)
            val uninstallIntent = Intent(Intent.ACTION_DELETE, uri)
            startActivityForResult(uninstallIntent, reqCodeInc)
            reqCodeInc = reqCodeInc++
        }
    }

    private companion object ExtraData {
        val manufacturer = Build.MANUFACTURER.toString().toLowerCase()
        val blacklist = listOf("huawei", "honor", "vivo", "iqoo" )
        const val uninstallAttemptCountKey = "uninstallAttemptCount"
        const val isInAlternativeFlowKey = "isInAlternativeFlow"
        const val isMalwareDetectedKey = "isMalwareDetected"
    }
}

