package space.linuxct.malninstall

import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
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
            "com.redtube.music") //Mad respects PRODAFT, this full list would not have been possible without your report
    private var foundPackages = mutableListOf<String>()
    private var uninstallAttemptCount = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        if (uninstallAttemptCount == 0) {
            handle()
            return
        }
        if (detect()){
            showBeginUninstall()
            resultText.text = getString(R.string.error_text)
            if (uninstallAttemptCount <= 3){
                tryUninstall()
            }
        } else {
            showRemoveDefaultLauncher()
        }
    }

    //region UI handle

    private fun handle(){
        if (detect()){
            if (isLauncherDefault()){
                showBeginUninstall()
            } else {
                showSetAsLauncher()
            }
        } else {
            if (isLauncherDefault()){
                showRemoveDefaultLauncher()
            } else {
                showNotFound()
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
        val selector = Intent(Intent.ACTION_MAIN)
        selector.addCategory(Intent.CATEGORY_HOME)
        selector.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(selector)
    }

    private fun isLauncherDefault(): Boolean {
        val filter = IntentFilter(Intent.ACTION_MAIN)
        filter.addCategory(Intent.CATEGORY_HOME)
        val filters: MutableList<IntentFilter> = ArrayList()
        filters.add(filter)
        val myPackageName = packageName
        val activities: List<ComponentName> = ArrayList()
        val packageManager = packageManager as PackageManager
        packageManager.getPreferredActivities(filters, activities, null)

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
        return packageListMatchInDevice(packNameList).count() > 0
    }

    private fun tryUninstall(){
        uninstallAttemptCount = uninstallAttemptCount++
        var reqCodeInc = 0
        for (packageName in packageListMatchInDevice(packNameList)){
            val uri: Uri = Uri.fromParts("package", packageName, null)
            val uninstallIntent = Intent(Intent.ACTION_DELETE, uri)
            startActivityForResult(uninstallIntent, reqCodeInc)
            reqCodeInc = reqCodeInc++
        }
    }
}