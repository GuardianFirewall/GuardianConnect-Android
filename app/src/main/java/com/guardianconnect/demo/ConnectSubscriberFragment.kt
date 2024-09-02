package com.guardianconnect.demo

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import com.guardianconnect.GRDConnectSubscriber
import com.guardianconnect.api.IOnApiResponse
import com.guardianconnect.helpers.GRDVPNHelper

class ConnectSubscriberFragment : Fragment() {

    private lateinit var etConnectApiHostname: EditText
    private lateinit var etConnectApiPublishableKey: EditText
    private lateinit var etConnectSubscriberIdentifier: EditText
    private lateinit var etConnectSubscriberSecret: EditText
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_connect_subscriber, container, false)

        etConnectApiHostname = view.findViewById(R.id.etConnectApiHostname)
        etConnectApiPublishableKey = view.findViewById(R.id.etConnectApiPublishableKey)
        etConnectSubscriberIdentifier = view.findViewById(R.id.etConnectSubscriberIdentifier)
        etConnectSubscriberSecret = view.findViewById(R.id.etConnectSubscriberSecret)
        progressBar = view.findViewById(R.id.progressBar)

        getValues()

        view.findViewById<Button>(R.id.btnSetConnectApiVariables).setOnClickListener {
            setConnectApiVariables()
        }

        view.findViewById<Button>(R.id.btnRegisterConnectSubscriber).setOnClickListener {
            registerConnectSubscriber()
        }

        view.findViewById<Button>(R.id.btnListConnectDevices).setOnClickListener {
            listConnectDevices()
        }

        return view
    }

    private fun getValues() {
        etConnectApiHostname.setText(GRDVPNHelper.connectAPIHostname)
        etConnectApiPublishableKey.setText(GRDVPNHelper.connectPublishableKey)
        val subscriber = GRDConnectSubscriber.currentSubscriber()
        etConnectSubscriberIdentifier.setText(subscriber?.identifier)
        etConnectSubscriberSecret.setText(subscriber?.secret)
    }

    private fun setConnectApiVariables() {
        val hostname = etConnectApiHostname.text.toString()
        val publishableKey = etConnectApiPublishableKey.text.toString()

        GRDVPNHelper.connectAPIHostname = hostname
        GRDVPNHelper.connectPublishableKey = publishableKey

        GRDVPNHelper.setVariables()

        Toast.makeText(context, "Connect API Variables set", Toast.LENGTH_SHORT).show()
    }

    private fun registerConnectSubscriber() {
        progressBar.visibility = View.VISIBLE
        val subscriberIdentifier = etConnectSubscriberIdentifier.text.toString()
        val subscriberSecret = etConnectSubscriberSecret.text.toString()

        val grdConnectSubscriber = GRDConnectSubscriber()
        grdConnectSubscriber.identifier = subscriberIdentifier
        grdConnectSubscriber.secret = subscriberSecret
        grdConnectSubscriber.registerNewConnectSubscriber(true, "test", object : IOnApiResponse {
            override fun onError(error: String?) {
                progressBar.visibility = View.GONE
                Toast.makeText(context, "Error registering subscriber", Toast.LENGTH_SHORT).show()
            }

            override fun onSuccess(any: Any?) {
                progressBar.visibility = View.GONE
                Toast.makeText(context, "Success. Subscriber registered.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun listConnectDevices() {
        val connectDevicesFragment = ConnectDevicesFragment()
        activity?.supportFragmentManager?.beginTransaction()
            ?.replace(R.id.fragment_container, connectDevicesFragment)
            ?.addToBackStack(null)
            ?.commit()
    }
}