package com.dzboot.gdprconsenttest

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import com.google.android.gms.ads.*
import com.google.android.ump.*


class MainActivity : AppCompatActivity() {

    private lateinit var consentForm: ConsentForm
    private val statusTV by lazy { findViewById<TextView>(R.id.status) }
    private val bannerAdLayout by lazy { findViewById<FrameLayout>(R.id.bannerAdLayout) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        MobileAds.initialize(this) {
            val configuration = RequestConfiguration.Builder()
                .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR, TEST_DEVICE_ID))
                .build()
            MobileAds.setRequestConfiguration(configuration)
        }

        findViewById<Button>(R.id.reset).setOnClickListener {
            bannerAdLayout.removeAllViews()
            UserMessagingPlatform.getConsentInformation(this).reset()
            getGDPRConsent()
        }

        getGDPRConsent()
    }

    private fun getGDPRConsent() {
        val paramsBuilder = ConsentRequestParameters.Builder().setTagForUnderAgeOfConsent(false)
        val consentInfo = UserMessagingPlatform.getConsentInformation(this)
        val debugSettings = ConsentDebugSettings.Builder(this)
            .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            .addTestDeviceHashedId(AdRequest.DEVICE_ID_EMULATOR)
            .addTestDeviceHashedId(Companion.TEST_DEVICE_ID)
            .build()

        paramsBuilder.setConsentDebugSettings(debugSettings)
        statusTV.text = getString(R.string.status_1_s, "Requesting consent info...")
        consentInfo.requestConsentInfoUpdate(
            this,
            paramsBuilder.build(), {
                if (consentInfo.isConsentFormAvailable) {
                    statusTV.append("\nConsent form available, loading it")
                    loadForm()
                } else {
                    statusTV.append("\nConsent form not available, loading ads")
                    loadAds()
                }
            }, {
                run {
                    statusTV.append("\nConsent info error: ${it.message}")
                }
            })
    }

    private fun loadForm() {
        UserMessagingPlatform.loadConsentForm(
            this, {
                consentForm = it
                statusTV.append("\nConsent form loaded successfully")
                if (!isFinishing && !isDestroyed) {
                    val consentStatus = UserMessagingPlatform.getConsentInformation(this).consentStatus
                    if (consentStatus == ConsentInformation.ConsentStatus.REQUIRED) {
                        statusTV.append("\nConsent status required")
                        consentForm.show(this) { formError ->
                            // Handle dismissal by reloading form.
                            statusTV.append("\nConsent info error: ${formError?.message}")
                            loadForm()
                        }
                    } else {
                        statusTV.append("\nConsent status=$consentStatus, loading ads")
                        loadAds()
                    }
                }
            }, { formError ->
                // Consent form error. This usually happens if the user is not in the EU.
                statusTV.append("\nLoad Consent form error: ${formError.message}")
                loadAds()
            })
    }

    private fun loadAds() {
        val display = windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)

        val density = outMetrics.density

        var adWidthPixels = bannerAdLayout.width.toFloat()
        if (adWidthPixels == 0f) {
            adWidthPixels = outMetrics.widthPixels.toFloat()
        }

        val adWidth = (adWidthPixels / density).toInt()
        val adView = AdView(this)
        bannerAdLayout.addView(adView)
        adView.adUnitId = "ca-app-pub-3940256099942544/6300978111"
        adView.adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                super.onAdLoaded()
                statusTV.append("\nAd loaded successfully")
            }

            override fun onAdFailedToLoad(p0: LoadAdError) {
                super.onAdFailedToLoad(p0)
                statusTV.append("\nAd failed to load ${p0.message}")
            }
        }
        adView.loadAd(AdRequest.Builder().build())
    }

    companion object {
        //change this to your device if you are not using an emulator
        private const val TEST_DEVICE_ID = "F40D8D0517577C9DFC3EDD5973A08B2B"
    }
}