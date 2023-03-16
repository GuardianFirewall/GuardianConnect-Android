package com.guardianconnect

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.guardianconnect.api.IApiCalls
import com.guardianconnect.api.Repository
import okhttp3.ResponseBody

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Before
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ApiTest {

    private var apiCallsConnect: IApiCalls? = null

    @Before
    fun setup() {
        Repository.instance.initMainServer("connect-api.guardianapp.com")
    }

    @Test
    fun testMainServer() {
        apiCallsConnect = Repository.instance.apiCallsConnect

        //Assert that Retrofit is not null
        assert(apiCallsConnect != null)
    }

    @Test
    fun testAllGuardianRegions() {
        val call: Call<ResponseBody>? = apiCallsConnect?.requestAllGuardianRegions()
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.string().let {
                        // TODO: check (it works in both cases !- null and == null)
                        assert(!it.equals(null))
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                // TODO
            }
        })
    }
}