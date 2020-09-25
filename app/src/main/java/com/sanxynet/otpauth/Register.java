package com.sanxynet.otpauth;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.rilixtech.widget.countrycodepicker.CountryCodePicker;

import java.util.concurrent.TimeUnit;

public class Register extends AppCompatActivity {
    FirebaseAuth fAuth;
    String otpCode = "123456";
    String verificationId, phoneNum;
    EditText phone, optEnter;
    Button next;
    CountryCodePicker countryCodePicker;
    PhoneAuthCredential credential;
    Boolean verificationOnProgress = false;
    ProgressBar progressBar;
    TextView state,resend;
    PhoneAuthProvider.ForceResendingToken token;
    FirebaseFirestore fStore;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        phone = findViewById(R.id.phone);
        optEnter = findViewById(R.id.codeEnter);
        countryCodePicker = findViewById(R.id.ccp);
        next = findViewById(R.id.nextBtn);
        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        progressBar = findViewById(R.id.progressBar);
        state = findViewById(R.id.state);
        resend = findViewById(R.id.resendOtpBtn);

        resend.setOnClickListener(v -> {
            // todo:: resend OTP
        });


        next.setOnClickListener(v -> {
            if(!phone.getText().toString().isEmpty() && phone.getText().toString().length() == 10) {
                if(!verificationOnProgress){
                    next.setEnabled(false);
                    progressBar.setVisibility(View.VISIBLE);
                    state.setVisibility(View.VISIBLE);
                    phoneNum = "+"+countryCodePicker.getSelectedCountryCode()+phone.getText().toString();
                    Log.d("phone", "Phone No.: " + phoneNum);
                    requestPhoneAuth(phoneNum);
                }else {
                    next.setEnabled(false);
                    optEnter.setVisibility(View.GONE);
                    progressBar.setVisibility(View.VISIBLE);
                    state.setText("Logging in");
                    state.setVisibility(View.VISIBLE);
                    otpCode = optEnter.getText().toString();
                    if(otpCode.isEmpty()){
                        optEnter.setError("Required");
                        return;
                    }

                    credential = PhoneAuthProvider.getCredential(verificationId,otpCode);
                    verifyAuth(credential);
                }

            }else {
                phone.setError("Valid Phone Required");
            }
        });


    }

    private void requestPhoneAuth(String phoneNumber) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(phoneNumber,60L, TimeUnit.SECONDS,this,
                new PhoneAuthProvider.OnVerificationStateChangedCallbacks(){

                    @Override
                    public void onCodeAutoRetrievalTimeOut(String s) {
                        super.onCodeAutoRetrievalTimeOut(s);
                        Toast.makeText(Register.this, "OTP Timeout, Please Re-generate the OTP Again.", Toast.LENGTH_SHORT).show();
                        resend.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onCodeSent(String s, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        super.onCodeSent(s, forceResendingToken);
                        verificationId = s;
                        token = forceResendingToken;
                        verificationOnProgress = true;
                        progressBar.setVisibility(View.GONE);
                        state.setVisibility(View.GONE);
                        next.setText("Verify");
                        next.setEnabled(true);
                        optEnter.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {

                        /* called if otp is automatically detected by the app */
                        verifyAuth(phoneAuthCredential);

                    }

                    @Override
                    public void onVerificationFailed(FirebaseException e) {
                        Toast.makeText(Register.this, e.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                });
    }


    private void verifyAuth(PhoneAuthCredential credential) {
        fAuth.signInWithCredential(credential).addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                Toast.makeText(Register.this, "Phone Verified."+fAuth.getCurrentUser().getUid(), Toast.LENGTH_SHORT).show();
                checkUserProfile();

            }else {
                progressBar.setVisibility(View.GONE);
                state.setVisibility(View.GONE);
                Toast.makeText(Register.this, "Can not Verify phone and Create Account.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        if(fAuth.getCurrentUser() != null){
            progressBar.setVisibility(View.VISIBLE);
            state.setText("Checking..");
            state.setVisibility(View.VISIBLE);
            checkUserProfile();
        }
    }

    private void checkUserProfile() {
        DocumentReference docRef = fStore.collection("users").document(fAuth.getCurrentUser().getUid());
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if(documentSnapshot.exists()){
                startActivity(new Intent(getApplicationContext(),MainActivity.class));
                finish();
            }else {
                /* Profile is not set up yet, set it up */
                Intent intent = new Intent(getApplicationContext(), Details.class);
                intent.putExtra("phone", phoneNum);
                startActivity(intent);
                finish();
            }
        }).addOnFailureListener(e ->
                Toast.makeText(Register.this, "Profile Do Not Exists", Toast.LENGTH_SHORT).show());
    }
}
