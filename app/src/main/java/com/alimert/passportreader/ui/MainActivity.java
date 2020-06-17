/*
 * Copyright 2016 - 2020 Anton Tananaev (anton.tananaev@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alimert.passportreader.ui;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.alimert.passportreader.R;
import com.alimert.passportreader.model.DocType;
import com.alimert.passportreader.util.DateUtil;
import com.alimert.passportreader.util.ImageUtil;
import com.alimert.passportreader.util.StringUtil;
import com.google.android.material.snackbar.Snackbar;

import net.sf.scuba.smartcards.CardFileInputStream;
import net.sf.scuba.smartcards.CardService;

import org.jmrtd.BACKey;
import org.jmrtd.BACKeySpec;
import org.jmrtd.PassportService;
import org.jmrtd.lds.CardSecurityFile;
import org.jmrtd.lds.PACEInfo;
import org.jmrtd.lds.SecurityInfo;
import org.jmrtd.lds.icao.DG1File;
import org.jmrtd.lds.icao.DG2File;
import org.jmrtd.lds.icao.MRZInfo;
import org.jmrtd.lds.iso19794.FaceImageInfo;
import org.jmrtd.lds.iso19794.FaceInfo;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.alimert.passportreader.ui.CaptureActivity.DOC_TYPE;
import static com.alimert.passportreader.ui.CaptureActivity.MRZ_RESULT;
import static org.jmrtd.PassportService.DEFAULT_MAX_BLOCKSIZE;
import static org.jmrtd.PassportService.NORMAL_MAX_TRANCEIVE_LENGTH;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private final static int APP_CAMERA_ACTIVITY_REQUEST_CODE = 150;

    private NfcAdapter adapter;

    private View mainLayout;
    private View loadingLayout;
    private View imageLayout;
    private Button scanIdCard, scanPassport, read;

    private String passportNumber, expirationDate , birthDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.action_bar_title);

        mainLayout = findViewById(R.id.main_layout);
        loadingLayout = findViewById(R.id.loading_layout);
        imageLayout = findViewById(R.id.image_layout);
        scanIdCard = findViewById(R.id.btn_scan_id_card);
        scanIdCard.setOnClickListener(this);
        scanPassport = findViewById(R.id.btn_scan_passport);
        scanPassport.setOnClickListener(this);
        read = findViewById(R.id.btn_read);
        read.setOnClickListener(this);

    }

    private void setMrzData(MRZInfo mrzInfo) {
        adapter = NfcAdapter.getDefaultAdapter(this);
        mainLayout.setVisibility(View.GONE);
        imageLayout.setVisibility(View.VISIBLE);

        passportNumber = mrzInfo.getDocumentNumber();
        expirationDate = mrzInfo.getDateOfExpiry();
        birthDate = mrzInfo.getDateOfBirth();
    }

    private void readCard() {

        String mrzData = "P<GBPANGELA<ZOE<<SMITH<<<<<<<<<<<<<<<<<<<<<<" +
                         "9990727768GBR7308196F2807041<<<<<<<<<<<<<<02";

        MRZInfo mrzInfo = new MRZInfo(mrzData);
        setMrzData(mrzInfo);
    }

    private void scanDocument(DocType docType) {
        Intent intent = new Intent(this, CaptureActivity.class);
        intent.putExtra(DOC_TYPE, docType);
        startActivityForResult(intent, APP_CAMERA_ACTIVITY_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case APP_CAMERA_ACTIVITY_REQUEST_CODE:
                    MRZInfo mrzInfo = (MRZInfo) data.getSerializableExtra(MRZ_RESULT);
                    if(mrzInfo != null) {
                        setMrzData(mrzInfo);
                    } else {
                        Snackbar.make(loadingLayout, R.string.error_input, Snackbar.LENGTH_SHORT).show();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_scan_id_card:
                scanDocument(DocType.ID_CARD);
                break;
            case R.id.btn_scan_passport:
                scanDocument(DocType.PASSPORT);
                break;
            case R.id.btn_read:
                readCard();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (adapter != null) {
            Intent intent = new Intent(getApplicationContext(), this.getClass());
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            String[][] filter = new String[][]{new String[]{"android.nfc.tech.IsoDep"}};
            adapter.enableForegroundDispatch(this, pendingIntent, null, filter);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();

        if (adapter != null) {
            adapter.disableForegroundDispatch(this);
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {
            Tag tag = intent.getExtras().getParcelable(NfcAdapter.EXTRA_TAG);
            if (Arrays.asList(tag.getTechList()).contains("android.nfc.tech.IsoDep")) {
                clearViews();
                if (passportNumber != null && !passportNumber.isEmpty()
                        && expirationDate != null && !expirationDate.isEmpty()
                        && birthDate != null && !birthDate.isEmpty()) {
                    BACKeySpec bacKey = new BACKey(passportNumber, birthDate, expirationDate);
                    new ReadTask(IsoDep.get(tag), bacKey).execute();
                    mainLayout.setVisibility(View.GONE);
                    imageLayout.setVisibility(View.GONE);
                    loadingLayout.setVisibility(View.VISIBLE);
                } else {
                    Snackbar.make(loadingLayout, R.string.error_input, Snackbar.LENGTH_SHORT).show();
                }
            }
        }
    }

    private class ReadTask extends AsyncTask<Void, Void, Exception> {

        private IsoDep isoDep;
        private BACKeySpec bacKey;

        private ReadTask(IsoDep isoDep, BACKeySpec bacKey) {
            this.isoDep = isoDep;
            this.bacKey = bacKey;
        }

        private DG1File dg1File;
        private DG2File dg2File;
        private String imageBase64;
        private Bitmap bitmap;

        @Override
        protected Exception doInBackground(Void... params) {
            try {

                CardService cardService = CardService.getInstance(isoDep);
                cardService.open();

                PassportService service = new PassportService(cardService, NORMAL_MAX_TRANCEIVE_LENGTH, DEFAULT_MAX_BLOCKSIZE, true, false);
                service.open();

                boolean paceSucceeded = false;
                try {
                    CardSecurityFile cardSecurityFile = new CardSecurityFile(service.getInputStream(PassportService.EF_CARD_SECURITY));
                    Collection<SecurityInfo> securityInfoCollection = cardSecurityFile.getSecurityInfos();
                    for (SecurityInfo securityInfo : securityInfoCollection) {
                        if (securityInfo instanceof PACEInfo) {
                            PACEInfo paceInfo = (PACEInfo) securityInfo;
                            service.doPACE(bacKey, paceInfo.getObjectIdentifier(), PACEInfo.toParameterSpec(paceInfo.getParameterId()), null);
                            paceSucceeded = true;
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, e);
                }

                service.sendSelectApplet(paceSucceeded);

                if (!paceSucceeded) {
                    try {
                        service.getInputStream(PassportService.EF_COM).read();
                    } catch (Exception e) {
                        service.doBAC(bacKey);
                    }
                }

                CardFileInputStream dg1In = service.getInputStream(PassportService.EF_DG1);
                dg1File = new DG1File(dg1In);

                CardFileInputStream dg2In = service.getInputStream(PassportService.EF_DG2);
                dg2File = new DG2File(dg2In);

                List<FaceImageInfo> allFaceImageInfos = new ArrayList<>();
                List<FaceInfo> faceInfos = dg2File.getFaceInfos();
                for (FaceInfo faceInfo : faceInfos) {
                    allFaceImageInfos.addAll(faceInfo.getFaceImageInfos());
                }

                if (!allFaceImageInfos.isEmpty()) {
                    FaceImageInfo faceImageInfo = allFaceImageInfos.iterator().next();

                    int imageLength = faceImageInfo.getImageLength();
                    DataInputStream dataInputStream = new DataInputStream(faceImageInfo.getImageInputStream());
                    byte[] buffer = new byte[imageLength];
                    dataInputStream.readFully(buffer, 0, imageLength);
                    InputStream inputStream = new ByteArrayInputStream(buffer, 0, imageLength);

                    bitmap = ImageUtil.decodeImage(
                            MainActivity.this, faceImageInfo.getMimeType(), inputStream);
                    imageBase64 = Base64.encodeToString(buffer, Base64.DEFAULT);
                }

            } catch (Exception e) {
                return e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Exception result) {
            mainLayout.setVisibility(View.VISIBLE);
            loadingLayout.setVisibility(View.GONE);

            if (result == null) {

                MRZInfo mrzInfo = dg1File.getMRZInfo();

                setImage(bitmap);
                setResultDatasToView(mrzInfo);

            } else {
                Snackbar.make(mainLayout, StringUtil.exceptionStack(result), Snackbar.LENGTH_LONG).show();
            }
        }

    }

    private void setImage(Bitmap bitmap) {
        if (bitmap != null) {
            double ratio = 400.0 / bitmap.getHeight();
            int targetHeight = (int) (bitmap.getHeight() * ratio);
            int targetWidth = (int) (bitmap.getWidth() * ratio);
            bitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, false);
            ((ImageView) findViewById(R.id.view_photo)).setImageBitmap(bitmap);
        }
    }

    private void setResultDatasToView(MRZInfo mrzInfo) {

        String result  = "NAME: " + mrzInfo.getSecondaryIdentifier().replace("<", " ").trim() + "\n";
        result += "SURNAME: " + mrzInfo.getPrimaryIdentifier().replace("<", " ").trim() + "\n";
        result += "PERSONAL NUMBER: " + mrzInfo.getPersonalNumber() + "\n";
        result += "GENDER: " + mrzInfo.getGender().toString() + "\n";
        result += "BIRTH DATE: " + DateUtil.convertFromMrzDate(mrzInfo.getDateOfBirth()) + "\n";
        result += "EXPIRY DATE: " + DateUtil.convertFromMrzDate(mrzInfo.getDateOfExpiry()) + "\n";
        result += "SERIAL NUMBER: " + mrzInfo.getDocumentNumber() + "\n";
        result += "NATIONALITY: " + mrzInfo.getIssuingState() + "\n";

        String documentType = "N/A";
        if("I".equals(mrzInfo.getDocumentCode())) {
            documentType = "ID Card";
        } else if("P".equals(mrzInfo.getDocumentCode())) {
            documentType = "Passport";
        }

        result += "DOC TYPE: " + documentType + "\n";
        result += "ISSUER AUTHORITY: " + mrzInfo.getIssuingState() + "\n";

        ((TextView) findViewById(R.id.text_result)).setText(result);
    }

    private void clearViews() {
        ((ImageView) findViewById(R.id.view_photo)).setImageBitmap(null);
        ((TextView) findViewById(R.id.text_result)).setText("");
    }

}
