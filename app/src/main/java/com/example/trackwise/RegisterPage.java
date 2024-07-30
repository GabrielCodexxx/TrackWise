package com.example.trackwise;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class RegisterPage extends AppCompatActivity {
    private static final String TAG = "RegisterPage";
    private EditText edttxtUserName;
    private TextView txtLogin;
    private EditText edtPassword;
    private EditText edtRepeatPassword;
    private CheckBox checkBoxShowpass;
    private Button btnRegister;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_page);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        }

        db = FirebaseFirestore.getInstance();

        initViews();

        btnRegister.setOnClickListener(v -> {
            String username = edttxtUserName.getText().toString();
            String password = edtPassword.getText().toString();
            String repeatPassword = edtRepeatPassword.getText().toString();

            if (username.isEmpty() || password.isEmpty() || repeatPassword.isEmpty()) {
                Toast.makeText(RegisterPage.this, "Please Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(repeatPassword)) {
                Toast.makeText(RegisterPage.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> user = new HashMap<>();
            user.put("username", username);
            user.put("password", password);

            db.collection("users")
                    .add(user)
                    .addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentReference> task) {
                            if (task.isSuccessful()) {
                                edttxtUserName.setText("");
                                edtPassword.setText("");
                                edtRepeatPassword.setText("");
                                DocumentReference document = task.getResult();
                                Toast.makeText(RegisterPage.this, "Registration Complete", Toast.LENGTH_SHORT).show();
                                if (document != null) {
                                    Log.d(TAG, "DocumentSnapshot added with ID: " + document.getId());
                                }
                            } else {
                                Log.w(TAG, "Error adding document", task.getException());
                                Toast.makeText(RegisterPage.this, "Registration failed. Please try again.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        });

        checkBoxShowpass.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                edtPassword.setTransformationMethod(null);
                edtRepeatPassword.setTransformationMethod(null);
            } else {
                edtPassword.setTransformationMethod(new PasswordTransformationMethod());
                edtRepeatPassword.setTransformationMethod(new PasswordTransformationMethod());
            }
        });

        txtLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterPage.this, LoginPage.class);
            startActivity(intent);
        });
    }

    private void initViews() {
        txtLogin = findViewById(R.id.txtLogin);
        edttxtUserName = findViewById(R.id.edtTxtUserName);
        edtPassword = findViewById(R.id.edtTxtPassword);
        edtRepeatPassword = findViewById(R.id.edtTxtRepeatPassword);
        checkBoxShowpass = findViewById(R.id.checkBoxShowPass);
        btnRegister = findViewById(R.id.btnRegister);
    }
}
