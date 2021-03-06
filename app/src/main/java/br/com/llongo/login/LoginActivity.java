package br.com.llongo.login;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentSender;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.plus.PlusClient;
import com.google.android.gms.plus.model.people.Person;

import java.util.ArrayList;
import java.util.List;

import br.com.llongo.R;
import br.com.llongo.home.HomeActivity;


/**
 * A login screen that offers login via email/password and via Google+ sign in.
 * <p/>
 * ************ IMPORTANT SETUP NOTES: ************
 * In order for Google+ sign in to work with your app, you must first go to:
 * https://developers.google.com/+/mobile/android/getting-started#step_1_enable_the_google_api
 * and follow the steps in "Step 1" to create an OAuth 2.0 client for your package.
 */
public class LoginActivity extends Activity implements OnClickListener,
        ConnectionCallbacks, OnConnectionFailedListener {
    private static final int REQUEST_CODE_RESOLVE_ERR = 9000;
    private static final String PROFILE_PIC_SIZE = "80";
    public static final String PREFS_NAME = "MyPrefsFile";

    private ProgressDialog mConnectionProgressDialog;
    private PlusClient mPlusClient;
    private ConnectionResult mConnectionResult;


    private static final String TAG = "LoginActivity";


    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mEmailLoginFormView;
    private SignInButton mPlusSignInButton;
    private View mSignOutButtons;
    private View mLoginFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        findViewById(R.id.email_sign_in_button).setOnClickListener(this);
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);


//        mPlusClient = new PlusClient.Builder(this, this, this)
//                .setActions("http://schemas.google.com/AddActivity", "http://schemas.google.com/BuyActivity")
//                .setScopes(Scopes.PROFILE,Scopes.PLUS_ME,Scopes.PLUS_LOGIN)
//                .build();
        // A barra de progresso deve ser exibida se a falha de conexão não for resolvida.
        mPlusClient = new PlusClient.Builder(this, this, this)
                .setActions("http://schemas.google.com/AddActivity", "http://schemas.google.com/BuyActivity")
                .setScopes(Scopes.PROFILE,Scopes.PLUS_LOGIN,Scopes.PLUS_ME)
                .build();
        mConnectionProgressDialog = new ProgressDialog(this);
        mConnectionProgressDialog.setMessage("Signing inN...");
    }

    @Override
    protected void onStart() {
        super.onStart();
        mPlusClient.connect();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_RESOLVE_ERR && resultCode == RESULT_OK) {
            mConnectionResult = null;
            mPlusClient.connect();
        }
    }
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = new ArrayList<String>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        addEmailsToAutoComplete(emails);
    }



    @Override
    public void onConnected(Bundle bundle) {
        mConnectionProgressDialog.dismiss();
//        Log.i("", bundle.toString());
        Toast.makeText(this, "User is connected!", Toast.LENGTH_LONG).show();
        getProfileInformation();

    }

    @Override
    public void onDisconnected() {

    }


    @Override
    protected void onStop() {
        super.onStop();
        mPlusClient.disconnect();
    }
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (mConnectionProgressDialog.isShowing()) {
            // O usuário já clicou no botão de login. Iniciar resolução
            // dos erros de conexão. Aguardar até que o onConnected() dispense a
            // caixa de diálogo de conexão.
            if (result.hasResolution()) {
                try {
                    result.startResolutionForResult(this, REQUEST_CODE_RESOLVE_ERR);
                } catch (IntentSender.SendIntentException e) {
                    mPlusClient.connect();
                }
            }
        }

        // Salvar a intenção para que possamos iniciar uma atividade quando o usuário clicar
        // no botão de login.
        mConnectionResult = result;

    }
    /**
     * Fetching user's information name, email, profile pic
     * */
    private void getProfileInformation() {
        try {
            if (mPlusClient.getCurrentPerson() != null) {
                SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, 0);
                Person currentPerson = mPlusClient.getCurrentPerson();
                SharedPreferences.Editor edit = sharedPreferences.edit();
                edit.putString("name",currentPerson.getDisplayName());
                edit.putString("email",mPlusClient.getAccountName());
                edit.putString("pic",currentPerson.getImage().getUrl());
                String personGooglePlusProfile = currentPerson.getUrl();
                String email = mPlusClient.getAccountName();
                edit.commit();
                Intent intent = new Intent(this, HomeActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(getApplicationContext(),
                        "Person information is null", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onClick(View view) {
        Log.i("login","login");
        if (view.getId() == R.id.email_sign_in_button && !mPlusClient.isConnected()) {
            if (mConnectionResult == null) {
                mConnectionProgressDialog.show();
            } else {
                try {
                    mConnectionResult.startResolutionForResult(this, REQUEST_CODE_RESOLVE_ERR);
                } catch (IntentSender.SendIntentException e) {
                    // Tente se conectar novamente.
                    mConnectionResult = null;
                    mPlusClient.connect();
                }
            }
        }
    }

    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(LoginActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mEmailView.setAdapter(adapter);
    }

    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }

}

