package com.example.trackwise;

import static androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG;
import static androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.concurrent.Executor;

public class LoginPage extends AppCompatActivity {

    private static final String TAG = "LoginPage";
    private TextView txtRegister;
    private EditText edtUserName;
    private EditText edtPassword;
    private CheckBox checkBoxShowpass;
    private FirebaseFirestore db;
    private static final int REQUEST_CODE = 1;
    ImageView imageViewLogin;
    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    private static final int MAX_ATTEMPTS = 5;
    private static final int COOLDOWN_TIME_MS = 30000; // 30 seconds

    private int loginAttempts = 0;
    private boolean isCooldownActive = false;
    private Handler cooldownHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        imageViewLogin = findViewById(R.id.imageViewLogin);

        BiometricManager biometricManager = BiometricManager.from(this);
        switch (biometricManager.canAuthenticate(BIOMETRIC_STRONG | DEVICE_CREDENTIAL)) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                Log.d("MY_APP_TAG", "App can authenticate using biometrics.");
                break;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                Log.e("MY_APP_TAG", "No biometric features available on this device.");
                break;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                Log.e("MY_APP_TAG", "Biometric features are currently unavailable.");
                break;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                final Intent enrollIntent = new Intent(Settings.ACTION_BIOMETRIC_ENROLL);
                enrollIntent.putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                        BIOMETRIC_STRONG | DEVICE_CREDENTIAL);
                startActivityForResult(enrollIntent, REQUEST_CODE);
                break;
        }
        executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(LoginPage.this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode,
                                              @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(getApplicationContext(),
                                "Authentication error: " + errString, Toast.LENGTH_SHORT)
                        .show();
            }

            @Override
            public void onAuthenticationSucceeded(
                    @NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                startActivity(new Intent(LoginPage.this, index.class));
                Toast.makeText(getApplicationContext(),
                        "Authentication succeeded!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(getApplicationContext(), "Authentication failed",
                                Toast.LENGTH_SHORT)
                        .show();
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric login for my app")
                .setSubtitle("Log in using your biometric credential")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();

        imageViewLogin.setOnClickListener(view -> {
            biometricPrompt.authenticate(promptInfo);
        });

        db = FirebaseFirestore.getInstance();

        initViews();

        checkBoxShowpass.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                edtPassword.setTransformationMethod(null);
            } else {
                edtPassword.setTransformationMethod(new PasswordTransformationMethod());
            }
        });

        txtRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginPage.this, RegisterPage.class);
            startActivity(intent);
        });

        findViewById(R.id.btnLogin).setOnClickListener(v -> {
            if (isCooldownActive) {
                Toast.makeText(LoginPage.this, "Please wait for the cooldown period to end.", Toast.LENGTH_SHORT).show();
                return;
            }

            String username = edtUserName.getText().toString();
            String password = edtPassword.getText().toString();
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginPage.this, "Please enter username and password", Toast.LENGTH_SHORT).show();
                return;
            }
            login(username, password);
        });
    }

    private void initViews() {
        txtRegister = findViewById(R.id.txtRegister);
        edtUserName = findViewById(R.id.edtTxtUserName);
        edtPassword = findViewById(R.id.edtTxtPassword);
        checkBoxShowpass = findViewById(R.id.checkBox);
    }

    private void login(String username, String password) {
        db.collection("users").whereEqualTo("username", username).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            QuerySnapshot querySnapshot = task.getResult();
                            if (!querySnapshot.isEmpty()) {
                                for (QueryDocumentSnapshot document : querySnapshot) {
                                    String storedPassword = document.getString("password");
                                    if (storedPassword != null && storedPassword.equals(password)) {
                                        Toast.makeText(LoginPage.this, "Login Successful", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(LoginPage.this, index.class);
                                        startActivity(intent);
                                        finish();
                                        return;
                                    }
                                }
                                handleFailedAttempt();
                                Toast.makeText(LoginPage.this, "Incorrect password", Toast.LENGTH_SHORT).show();
                            } else {
                                handleFailedAttempt();
                                Toast.makeText(LoginPage.this, "User not found", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.w(TAG, "Error getting documents.", task.getException());
                            handleFailedAttempt();
                            Toast.makeText(LoginPage.this, "Authentication failed. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void handleFailedAttempt() {
        loginAttempts++;
        if (loginAttempts >= MAX_ATTEMPTS) {
            isCooldownActive = true;
            Toast.makeText(LoginPage.this, "Too many failed attempts. Please wait 30 seconds.", Toast.LENGTH_SHORT).show();
            cooldownHandler.postDelayed(() -> {
                isCooldownActive = false;
                loginAttempts = 0;
                Toast.makeText(LoginPage.this, "You can now try logging in again.", Toast.LENGTH_SHORT).show();
            }, COOLDOWN_TIME_MS);
        }
    }
}
