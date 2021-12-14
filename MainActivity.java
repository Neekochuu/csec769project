/*
 * NfcActivity.java
 * NfcLibrary project.
 *
 * Created by : Daneo van Overloop - 17/6/2014.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 AppFoundry. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 */

package be.appfoundry.nfc.implementation;

import android.app.ProgressDialog;
import android.content.Intent;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NfcEvent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import be.appfoundry.nfclibrary.activities.NfcActivity;
import be.appfoundry.nfclibrary.exceptions.InsufficientCapacityException;
import be.appfoundry.nfclibrary.exceptions.ReadOnlyTagException;
import be.appfoundry.nfclibrary.exceptions.TagNotPresentException;
import be.appfoundry.nfclibrary.tasks.interfaces.AsyncOperationCallback;
import be.appfoundry.nfclibrary.tasks.interfaces.AsyncUiCallback;
import be.appfoundry.nfclibrary.utilities.async.WriteCallbackNfcAsync;
import be.appfoundry.nfclibrary.utilities.interfaces.NfcReadUtility;
import be.appfoundry.nfclibrary.utilities.interfaces.NfcWriteUtility;
import be.appfoundry.nfclibrary.utilities.sync.NfcMessageUtilityImpl;
import be.appfoundry.nfclibrary.utilities.sync.NfcReadUtilityImpl;


public class MainActivity extends NfcActivity {

    private static final String TAG = MainActivity.class.getName();

    NfcReadUtility mNfcReadUtility = new NfcReadUtilityImpl();
    ProgressDialog mProgressDialog;

    AsyncUiCallback mAsyncUiCallback = new AsyncUiCallback() {
        @Override
        public void callbackWithReturnValue(Boolean result) {
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
            if (result) {
                Toast.makeText(MainActivity.this, "Write has been done!", Toast.LENGTH_LONG).show();
            }

            Log.d(TAG,"Received our result : " + result);

        }

        @Override
        public void onProgressUpdate(Boolean... values) {
            if (values.length > 0 && values[0] && mProgressDialog != null) {
                mProgressDialog.setMessage("Writing");
                Log.d(TAG,"Writing !");
            }
        }

        @Override
        public void onError(Exception e) {
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
            Log.i(TAG,"Encountered an error !",e);
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    };

    AsyncOperationCallback mAsyncOperationCallback;
    private AsyncTask<Object, Void, Boolean> mTask;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button emailButton = (Button) findViewById(R.id.btn_write_email_nfc);
        Button smsButton = (Button) findViewById(R.id.btn_write_sms_nfc);

        if (emailButton != null) {
            emailButton.setOnClickListener(new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final EditText emailText = ((EditText) retrieveElement(R.id.input_text_data));
                    if (emailText != null) {
                        final String text = emailText.getText().toString();
                        mAsyncOperationCallback = new AsyncOperationCallback() {
                            @Override
                            public boolean performWrite(NfcWriteUtility writeUtility) throws ReadOnlyTagException, InsufficientCapacityException, TagNotPresentException, FormatException {
                                return writeUtility.writeEmailToTagFromIntent(text, null, null, getIntent());
                            }
                        };
                        showDialog();
                    }
                }
            });
        }

        if (smsButton != null) {
            smsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EditText smsText = (EditText) retrieveElement(R.id.input_text_key);
                    if (smsText != null) {
                        final String text = smsText.getText().toString();
                        mAsyncOperationCallback = new AsyncOperationCallback() {
                            @Override
                            public boolean performWrite(NfcWriteUtility writeUtility) throws ReadOnlyTagException, InsufficientCapacityException, TagNotPresentException, FormatException {
                                return writeUtility.writeSmsToTagFromIntent(text, null, getIntent());
                            }
                        };
                        showDialog();
                    }
                }
            });
        }



        enableBeam();
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (getNfcAdapter() != null) {
            getNfcAdapter().disableForegroundDispatch(this);
        }
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        return new NfcMessageUtilityImpl().createText("Message to beam");
    }

    /**
     * Launched when in foreground dispatch mode
     *
     * @param paramIntent
     *         containing found data
     */
    @Override
    public void onNewIntent(final Intent paramIntent) {
        super.onNewIntent(paramIntent);

        if (mAsyncOperationCallback != null && mProgressDialog != null && mProgressDialog.isShowing()) {
            new WriteCallbackNfcAsync(mAsyncUiCallback, mAsyncOperationCallback).executeWriteOperation();
            mAsyncOperationCallback = null;
        } else {
            for (String data : mNfcReadUtility.readFromTagWithMap(paramIntent).values()) {
                Toast.makeText(this, data, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showNoInputToast() {
        Toast.makeText(this, getString(R.string.no_input), Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
        if (mTask != null) {
            mTask.cancel(true);
            mTask = null;
        }
    }

    public void showDialog() {
        mProgressDialog = new ProgressDialog(MainActivity.this);
        mProgressDialog.setTitle(R.string.progressdialog_waiting_for_tag);
        mProgressDialog.setMessage(getString(R.string.progressdialog_waiting_for_tag_message));
        mProgressDialog.show();
    }

    private TextView retrieveElement(int id) {
        TextView element = (TextView) findViewById(id);
        return (element != null) && ((TextView) findViewById(id)).getText() != null && !"".equals(((TextView) findViewById(id)).getText().toString()) ? element : null;
    }

}
