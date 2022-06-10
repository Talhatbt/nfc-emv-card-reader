package com.nfctest

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.NfcAdapter.ReaderCallback
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.ui.AppBarConfiguration
import com.github.devnied.emvnfccard.model.EmvCard
import com.github.devnied.emvnfccard.parser.EmvTemplate
import com.nfctest.databinding.ActivityMainBinding
import net.sf.scuba.util.Hex.toHexString


class MainActivity : AppCompatActivity(), ReaderCallback {

    lateinit var activity: Activity
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    lateinit var mNfcAdapter: NfcAdapter
    var tag: WritableTag? = null
    var tagId: String? = null

    lateinit var nfcManager: com.nfctest.NfcManager

    @SuppressLint("ServiceCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        activity = this
        setSupportActionBar(binding.toolbar)
        nfcManager = NfcManager(this, this)

        val nfcManager = getSystemService(Context.NFC_SERVICE) as android.nfc.NfcManager
        mNfcAdapter = nfcManager.defaultAdapter
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun enableNfcForegroundDispatch() {
        try {
            val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val nfcPendingIntent =
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            mNfcAdapter.enableForegroundDispatch(this, nfcPendingIntent, null, null)
        } catch (ex: IllegalStateException) {
            Log.e("getTag", "Error enabling NFC foreground dispatch", ex)
        }
    }

    private fun disableNfcForegroundDispatch() {
        try {
            mNfcAdapter?.disableForegroundDispatch(this)
        } catch (ex: IllegalStateException) {
            Log.e("mNfcAdapter", "Error disabling NFC foreground dispatch", ex)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onResume() {
        super.onResume()
        enableNfcForegroundDispatch()
        nfcManager.onResume()
    }

    override fun onPause() {
        disableNfcForegroundDispatch()
        nfcManager.onPause()
        super.onPause()
    }

    override fun onTagDiscovered(tag: Tag?) {
        // get IsoDep handle and run xcvr thread
        val isoDep = IsoDep.get(tag)
        if (isoDep == null) {
            showToast("error")
        } else {

            isoDep.connect()
            val mProvider = MyProvider()
            // Define config
            mProvider.setmTagCom(isoDep)
            val config: EmvTemplate.Config = EmvTemplate.Config()
                .setContactLess(true) // Enable contact less reading (default: true)
                .setReadAllAids(true) // Read all aids in card (default: true)
                .setReadTransactions(true) // Read all transactions (default: true)
                .setReadCplc(false) // Read and extract CPCLC data (default: false)
                .setRemoveDefaultParsers(false) // Remove default parsers for GeldKarte and EmvCard (default: false)
                .setReadAt(true) // Read and extract ATR/ATS and description


            // Create Parser

            val parser = EmvTemplate.Builder() //
                .setProvider(mProvider) // Define provider
                .setConfig(config) // Define config
                //.setTerminal(terminal) (optional) you can define a custom terminal implementation to create APDU
                .build()

            // Read card

            val card: EmvCard = parser.readEmvCard()
            Log.e("card track2 number", card.track2.cardNumber)
            val cardNumber = card.track2.cardNumber
            val cardExpiry = card.expireDate.toString()
            val card_Expiry = toHexString(card.track2.raw).split("D")[1]
            var expiry= card_Expiry.substring(0,4)
            val aid = card.type.aid[0]
            val cardType = card.type.toString()

            Log.e("card expiry", card.track2.expireDate.time.toString())
            Log.e("card raw", toHexString(card.track2.raw))
            Log.e("card service code 1", card.track2.service.serviceCode1.technology)
            Log.e(
                "card service code 2", card.track2.service.serviceCode2.authorizationProcessing +
                        card.track2.service.serviceCode2.name
            )
            Log.e("card service 3", card.track2.service.serviceCode3.pinRequirements)
            Log.e("card service 3", card.track2.service.serviceCode3.allowedServices)

            var appendData =
                "Card Number: $cardNumber \nExpiry: $expiry\nAID: $aid\nCard Type: $cardType"

            activity.runOnUiThread {
                binding.tvCardDetails.text = appendData
            }
        }
    }
}