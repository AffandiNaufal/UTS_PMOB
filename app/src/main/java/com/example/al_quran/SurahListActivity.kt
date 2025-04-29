package com.example.al_quran

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SurahListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SurahAdapter

    // Firebase Auth
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 100

    // UI Login
    private lateinit var btnLogin: Button
    private lateinit var btnLogout: Button
    private lateinit var tvProfile: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_surah_list)

        // Inisialisasi Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Google Sign-In Client
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // pastikan ini sesuai
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Inisialisasi View Login
        btnLogin = findViewById(R.id.btnLogin)
        btnLogout = findViewById(R.id.btnLogout)
        tvProfile = findViewById(R.id.tvProfile)

        btnLogin.setOnClickListener { signIn() }
        btnLogout.setOnClickListener { logout() }

        // Cek apakah user sudah login
        updateUI(auth.currentUser)

        // Inisialisasi RecyclerView
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = SurahAdapter { surah -> openSurahDetails(surah) }
        recyclerView.adapter = adapter

        fetchSurahList()
    }

    private fun fetchSurahList() {
        RetrofitClient.instance.getSurahList().enqueue(object : Callback<SurahListResponse> {
            override fun onResponse(call: Call<SurahListResponse>, response: Response<SurahListResponse>) {
                if (response.isSuccessful) {
                    val data = response.body()
                    Log.d("API_SUCCESS", "Jumlah Surah: ${data?.data?.size}")
                    data?.data?.let { adapter.submitList(it) }
                } else {
                    Log.e("API_ERROR", "Response tidak sukses: ${response.code()} - ${response.message()}")
                    Toast.makeText(this@SurahListActivity, "Gagal memuat data (response gagal)", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<SurahListResponse>, t: Throwable) {
                Log.e("API_ERROR", "Retrofit gagal: ${t.message}")
                Toast.makeText(this@SurahListActivity, "Gagal memuat data (jaringan)", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun openSurahDetails(surah: Surah) {
        val intent = Intent(this, SurahDetailsActivity::class.java)
        intent.putExtra("SURAH_ID", surah.number)
        intent.putExtra("SURAH_NAME", surah.name)
        startActivity(intent)
    }

    // Login
    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    // Logout
    private fun logout() {
        auth.signOut()
        googleSignInClient.signOut().addOnCompleteListener {
            updateUI(null)
        }
    }

    // Terima hasil login
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            if (task.isSuccessful) {
                val account = task.result
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                auth.signInWithCredential(credential).addOnCompleteListener {
                    if (it.isSuccessful) {
                        updateUI(auth.currentUser)
                    } else {
                        Toast.makeText(this, "Login gagal", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Login dibatalkan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Update UI berdasarkan user login
    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            tvProfile.text = "Halo, ${user.displayName} (${user.email})"
            btnLogin.visibility = View.GONE
            btnLogout.visibility = View.VISIBLE
        } else {
            tvProfile.text = "Belum login"
            btnLogin.visibility = View.VISIBLE
            btnLogout.visibility = View.GONE
        }
    }
}
