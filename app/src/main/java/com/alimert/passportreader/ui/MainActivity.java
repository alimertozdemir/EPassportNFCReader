package com.alimert.passportreader.ui;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.alimert.passportreader.R;
import com.alimert.passportreader.model.AdditionalPersonDetails;
import com.alimert.passportreader.model.DocType;
import com.alimert.passportreader.model.EDocument;
import com.alimert.passportreader.model.PersonDetails;
import com.alimert.passportreader.util.AppUtil;
import com.alimert.passportreader.util.DateUtil;
import com.alimert.passportreader.util.Image;
import com.alimert.passportreader.util.ImageUtil;
import com.alimert.passportreader.util.PermissionUtil;
import com.alimert.passportreader.util.SecurityUtil;
import com.alimert.passportreader.util.StringUtil;
import com.google.android.material.snackbar.Snackbar;

import net.sf.scuba.smartcards.CardFileInputStream;
import net.sf.scuba.smartcards.CardService;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jmrtd.BACKey;
import org.jmrtd.BACKeySpec;
import org.jmrtd.PassportService;
import org.jmrtd.Util;
import org.jmrtd.lds.CardSecurityFile;
import org.jmrtd.lds.ChipAuthenticationInfo;
import org.jmrtd.lds.ChipAuthenticationPublicKeyInfo;
import org.jmrtd.lds.DisplayedImageInfo;
import org.jmrtd.lds.PACEInfo;
import org.jmrtd.lds.SODFile;
import org.jmrtd.lds.SecurityInfo;
import org.jmrtd.lds.icao.DG11File;
import org.jmrtd.lds.icao.DG12File;
import org.jmrtd.lds.icao.DG14File;
import org.jmrtd.lds.icao.DG15File;
import org.jmrtd.lds.icao.DG1File;
import org.jmrtd.lds.icao.DG2File;
import org.jmrtd.lds.icao.DG3File;
import org.jmrtd.lds.icao.DG5File;
import org.jmrtd.lds.icao.DG7File;
import org.jmrtd.lds.icao.MRZInfo;
import org.jmrtd.lds.iso19794.FaceImageInfo;
import org.jmrtd.lds.iso19794.FaceInfo;
import org.jmrtd.lds.iso19794.FingerImageInfo;
import org.jmrtd.lds.iso19794.FingerInfo;
import org.jmrtd.protocol.AAResult;
import org.jmrtd.protocol.EACCAResult;

import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.alimert.passportreader.ui.CaptureActivity.DOC_TYPE;
import static com.alimert.passportreader.ui.CaptureActivity.MRZ_RESULT;
import static org.jmrtd.PassportService.DEFAULT_MAX_BLOCKSIZE;
import static org.jmrtd.PassportService.NORMAL_MAX_TRANCEIVE_LENGTH;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int APP_CAMERA_ACTIVITY_REQUEST_CODE = 150;
    private static final int APP_SETTINGS_ACTIVITY_REQUEST_CODE = 550;

    private NfcAdapter adapter;

    private View mainLayout;
    private View loadingLayout;
    private View imageLayout;
    private Button scanIdCard, scanPassport, read;
    private TextView tvResult;
    private ImageView ivPhoto;

    private String passportNumber, expirationDate , birthDate;
    private DocType docType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.action_bar_title);

        mainLayout = findViewById(R.id.main_layout);
        loadingLayout = findViewById(R.id.loading_layout);
        imageLayout = findViewById(R.id.image_layout);
        ivPhoto = findViewById(R.id.view_photo);
        tvResult = findViewById(R.id.text_result);
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

    private void openCameraActivity() {
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
                docType = DocType.ID_CARD;
                requestPermissionForCamera();
                break;
            case R.id.btn_scan_passport:
                docType = DocType.PASSPORT;
                requestPermissionForCamera();
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
                    IsoDep isoDep = IsoDep.get(tag);
                    isoDep.setTimeout(5*1000);
                    new ReadTask(isoDep, bacKey).execute();
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

        EDocument eDocument = new EDocument();
        DocType docType = DocType.OTHER;
        PersonDetails personDetails = new PersonDetails();
        AdditionalPersonDetails additionalPersonDetails = new AdditionalPersonDetails();

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

                boolean hashesMatched = true;
                boolean activeAuth = true;
                boolean chipAuth = true;
                boolean docSignatureValid;

                CardFileInputStream sodIn = service.getInputStream(PassportService.EF_SOD);
                SODFile sodFile = new SODFile(sodIn);
                for (Map.Entry<Integer, byte[]> dgHash : sodFile.getDataGroupHashes().entrySet()) {
                    Log.d(TAG, "Data group: " + dgHash.getKey() + " hash value: " + StringUtil.byteArrayToHex(dgHash.getValue()));
                }

                String digestAlgorithm = sodFile.getDigestAlgorithm();
                Log.d(TAG, "Digest Algorithm: " + digestAlgorithm);

                X509Certificate docSigningCert = sodFile.getDocSigningCertificate();
                String pemFile = SecurityUtil.convertToPem(docSigningCert);
                Log.d(TAG, "Document Signer Certificate: " + docSigningCert.toString());
                Log.d(TAG, "Document Signer Certificate Pem : " + pemFile);

                String digestEncryptionAlgorithm = sodFile.getDigestEncryptionAlgorithm();
                String signerInfoDigestAlgorithm = sodFile.getSignerInfoDigestAlgorithm();

                byte[] eContent = sodFile.getEContent();
                byte[] signature = sodFile.getEncryptedDigest();

                MessageDigest digest;

                if (digestEncryptionAlgorithm == null) {
                    try {
                        digest = MessageDigest.getInstance(signerInfoDigestAlgorithm);
                    } catch (Exception e) {
                        digest = MessageDigest.getInstance(signerInfoDigestAlgorithm, new BouncyCastleProvider());
                    }

                    digest.update(eContent);
                    byte[] digestBytes = digest.digest();
                    docSignatureValid = Arrays.equals(digestBytes, signature);
                } else {

                    if ("SSAwithRSA/PSS".equals(digestEncryptionAlgorithm)) {
                        digestEncryptionAlgorithm = signerInfoDigestAlgorithm.replace("-", "") + "withRSA/PSS";
                    } else if ("RSA".equals(digestEncryptionAlgorithm)) {
                        digestEncryptionAlgorithm = signerInfoDigestAlgorithm.replace("-", "") + "withRSA";
                    }

                    Log.d(TAG, "Digest Encryption Algorithm: " + digestEncryptionAlgorithm);

                    Signature sig = Signature.getInstance(digestEncryptionAlgorithm, new BouncyCastleProvider());

                    if (digestEncryptionAlgorithm.endsWith("withRSA/PSS")) {
                        int saltLength = SecurityUtil.findSaltRSA_PSS(digestEncryptionAlgorithm, docSigningCert, eContent, signature);
                        MGF1ParameterSpec mgf1ParameterSpec = new MGF1ParameterSpec("SHA-256");
                        PSSParameterSpec pssParameterSpec = new PSSParameterSpec("SHA-256", "MGF1", mgf1ParameterSpec, saltLength, 1);
                        sig.setParameter(pssParameterSpec);
                    }

                    sig.initVerify(docSigningCert);
                    sig.update(eContent);
                    docSignatureValid = sig.verify(signature);
                }

                if (Security.getAlgorithms("MessageDigest").contains(digestAlgorithm)) {
                    digest = MessageDigest.getInstance(digestAlgorithm);
                } else {
                    digest = MessageDigest.getInstance(digestAlgorithm, new BouncyCastleProvider());
                }

                // -- Personal Details -- //
                CardFileInputStream dg1In = service.getInputStream(PassportService.EF_DG1);
                DG1File dg1File = new DG1File(dg1In);

                String encodedDg1File = new String(dg1File.getEncoded());

                MRZInfo mrzInfo = dg1File.getMRZInfo();
                personDetails.setName(mrzInfo.getSecondaryIdentifier().replace("<", " ").trim());
                personDetails.setSurname(mrzInfo.getPrimaryIdentifier().replace("<", " ").trim());
                personDetails.setPersonalNumber(mrzInfo.getPersonalNumber());
                personDetails.setGender(mrzInfo.getGender().toString());
                personDetails.setBirthDate(DateUtil.convertFromMrzDate(mrzInfo.getDateOfBirth()));
                personDetails.setExpiryDate(DateUtil.convertFromMrzDate(mrzInfo.getDateOfExpiry()));
                personDetails.setSerialNumber(mrzInfo.getDocumentNumber());
                personDetails.setNationality(mrzInfo.getNationality());
                personDetails.setIssuerAuthority(mrzInfo.getIssuingState());

                if("I".equals(mrzInfo.getDocumentCode())) {
                    docType = DocType.ID_CARD;
                    encodedDg1File = StringUtil.fixPersonalNumberMrzData(encodedDg1File, mrzInfo.getPersonalNumber());
                } else if("P".equals(mrzInfo.getDocumentCode())) {
                    docType = DocType.PASSPORT;
                }

                byte[] dg1StoredHash = sodFile.getDataGroupHashes().get(1);
                byte[] dg1ComputedHash = digest.digest(encodedDg1File.getBytes());
                Log.d(TAG, "DG1 Stored Hash: " + StringUtil.byteArrayToHex(dg1StoredHash));
                Log.d(TAG, "DG1 Computed Hash: " + StringUtil.byteArrayToHex(dg1ComputedHash));

                if (Arrays.equals(dg1StoredHash, dg1ComputedHash)) {
                    Log.d(TAG, "DG1 Hashes are matched");
                } else {
                    hashesMatched = false;
                }


                // -- Face Image -- //
                CardFileInputStream dg2In = service.getInputStream(PassportService.EF_DG2);
                DG2File dg2File = new DG2File(dg2In);

                byte[] dg2StoredHash = sodFile.getDataGroupHashes().get(2);
                byte[] dg2ComputedHash = digest.digest(dg2File.getEncoded());
                Log.d(TAG, "DG2 Stored Hash: " + StringUtil.byteArrayToHex(dg2StoredHash));
                Log.d(TAG, "DG2 Computed Hash: " + StringUtil.byteArrayToHex(dg2ComputedHash));

                if (Arrays.equals(dg2StoredHash, dg2ComputedHash)) {
                    Log.d(TAG, "DG2 Hashes are matched");
                } else {
                    hashesMatched = false;
                }

                List<FaceInfo> faceInfos = dg2File.getFaceInfos();
                List<FaceImageInfo> allFaceImageInfos = new ArrayList<>();
                for (FaceInfo faceInfo : faceInfos) {
                    allFaceImageInfos.addAll(faceInfo.getFaceImageInfos());
                }

                if (!allFaceImageInfos.isEmpty()) {
                    FaceImageInfo faceImageInfo = allFaceImageInfos.iterator().next();
                    Image image = ImageUtil.getImage(MainActivity.this, faceImageInfo);
                    personDetails.setFaceImage(image.getBitmapImage());
                    personDetails.setFaceImageBase64(image.getBase64Image());
                }

                // -- Fingerprint (if exist)-- //
                try {
                    CardFileInputStream dg3In = service.getInputStream(PassportService.EF_DG3);
                    DG3File dg3File = new DG3File(dg3In);

                    byte[] dg3StoredHash = sodFile.getDataGroupHashes().get(3);
                    byte[] dg3ComputedHash = digest.digest(dg3File.getEncoded());
                    Log.d(TAG, "DG3 Stored Hash: " + StringUtil.byteArrayToHex(dg3StoredHash));
                    Log.d(TAG, "DG3 Computed Hash: " + StringUtil.byteArrayToHex(dg3ComputedHash));

                    if (Arrays.equals(dg3StoredHash, dg3ComputedHash)) {
                        Log.d(TAG, "DG3 Hashes are matched");
                    } else {
                        hashesMatched = false;
                    }

                    List<FingerInfo> fingerInfos = dg3File.getFingerInfos();
                    List<FingerImageInfo> allFingerImageInfos = new ArrayList<>();
                    for (FingerInfo fingerInfo : fingerInfos) {
                        allFingerImageInfos.addAll(fingerInfo.getFingerImageInfos());
                    }

                    List<Bitmap> fingerprintsImage = new ArrayList<>();

                    if (!allFingerImageInfos.isEmpty()) {

                        for(FingerImageInfo fingerImageInfo : allFingerImageInfos) {
                            Image image = ImageUtil.getImage(MainActivity.this, fingerImageInfo);
                            fingerprintsImage.add(image.getBitmapImage());
                        }

                        personDetails.setFingerprints(fingerprintsImage);

                    }
                } catch (Exception e) {
                    Log.w(TAG, e);
                }

                // -- Portrait Picture -- //
                try {
                    CardFileInputStream dg5In = service.getInputStream(PassportService.EF_DG5);
                    DG5File dg5File = new DG5File(dg5In);

                    byte[] dg5StoredHash = sodFile.getDataGroupHashes().get(5);
                    byte[] dg5ComputedHash = digest.digest(dg5File.getEncoded());
                    Log.d(TAG, "DG5 Stored Hash: " + StringUtil.byteArrayToHex(dg5StoredHash));
                    Log.d(TAG, "DG5 Computed Hash: " + StringUtil.byteArrayToHex(dg5ComputedHash));

                    if (Arrays.equals(dg5StoredHash, dg5ComputedHash)) {
                        Log.d(TAG, "DG5 Hashes are matched");
                    } else {
                        hashesMatched = false;
                    }

                    List<DisplayedImageInfo> displayedImageInfos = dg5File.getImages();
                    if (!displayedImageInfos.isEmpty()) {
                        DisplayedImageInfo displayedImageInfo = displayedImageInfos.iterator().next();
                        Image image = ImageUtil.getImage(MainActivity.this, displayedImageInfo);
                        personDetails.setPortraitImage(image.getBitmapImage());
                        personDetails.setPortraitImageBase64(image.getBase64Image());
                    }
                } catch (Exception e) {
                    Log.w(TAG, e);
                }

                // -- Signature (if exist) -- //
                try {
                    CardFileInputStream dg7In = service.getInputStream(PassportService.EF_DG7);
                    DG7File dg7File = new DG7File(dg7In);

                    byte[] dg7StoredHash = sodFile.getDataGroupHashes().get(7);
                    byte[] dg7ComputedHash = digest.digest(dg7File.getEncoded());
                    Log.d(TAG, "DG7 Stored Hash: " + StringUtil.byteArrayToHex(dg7StoredHash));
                    Log.d(TAG, "DG7 Computed Hash: " + StringUtil.byteArrayToHex(dg7ComputedHash));

                    if (Arrays.equals(dg7StoredHash, dg7ComputedHash)) {
                        Log.d(TAG, "DG7 Hashes are matched");
                    } else {
                        hashesMatched = false;
                    }

                    List<DisplayedImageInfo> signatureImageInfos = dg7File.getImages();
                    if (!signatureImageInfos.isEmpty()) {
                        DisplayedImageInfo displayedImageInfo = signatureImageInfos.iterator().next();
                        Image image = ImageUtil.getImage(MainActivity.this, displayedImageInfo);
                        personDetails.setPortraitImage(image.getBitmapImage());
                        personDetails.setPortraitImageBase64(image.getBase64Image());
                    }

                } catch (Exception e) {
                    Log.w(TAG, e);
                }

                // -- Additional Personal Details (if exist) -- //
                try {
                    CardFileInputStream dg11In = service.getInputStream(PassportService.EF_DG11);
                    DG11File dg11File = new DG11File(dg11In);

                    byte[] dg11StoredHash = sodFile.getDataGroupHashes().get(11);
                    byte[] dg11ComputedHash = digest.digest(dg11File.getEncoded());
                    Log.d(TAG, "DG11 Stored Hash: " + StringUtil.byteArrayToHex(dg11StoredHash));
                    Log.d(TAG, "DG11 Computed Hash: " + StringUtil.byteArrayToHex(dg11ComputedHash));

                    if (Arrays.equals(dg11StoredHash, dg11ComputedHash)) {
                        Log.d(TAG, "DG11 Hashes are matched");
                    } else {
                        hashesMatched = false;
                    }

                    if(dg11File.getLength() > 0) {
                        additionalPersonDetails.setCustodyInformation(dg11File.getCustodyInformation());
                        additionalPersonDetails.setNameOfHolder(dg11File.getNameOfHolder());
                        additionalPersonDetails.setFullDateOfBirth(dg11File.getFullDateOfBirth());
                        additionalPersonDetails.setOtherNames(dg11File.getOtherNames());
                        additionalPersonDetails.setOtherValidTDNumbers(dg11File.getOtherValidTDNumbers());
                        additionalPersonDetails.setPermanentAddress(dg11File.getPermanentAddress());
                        additionalPersonDetails.setPersonalNumber(dg11File.getPersonalNumber());
                        additionalPersonDetails.setPersonalSummary(dg11File.getPersonalSummary());
                        additionalPersonDetails.setPlaceOfBirth(dg11File.getPlaceOfBirth());
                        additionalPersonDetails.setProfession(dg11File.getProfession());
                        additionalPersonDetails.setProofOfCitizenship(dg11File.getProofOfCitizenship());
                        additionalPersonDetails.setTag(dg11File.getTag());
                        additionalPersonDetails.setTagPresenceList(dg11File.getTagPresenceList());
                        additionalPersonDetails.setTelephone(dg11File.getTelephone());
                        additionalPersonDetails.setTitle(dg11File.getTitle());
                    }
                } catch (Exception e) {
                    Log.w(TAG, e);
                }

                // -- Additional Document Details (if exist) -- //
                try {
                    CardFileInputStream dg12In = service.getInputStream(PassportService.EF_DG12);
                    DG12File dg12File = new DG12File(dg12In);

                    byte[] dg12StoredHash = sodFile.getDataGroupHashes().get(12);
                    byte[] dg12ComputedHash = digest.digest(dg12File.getEncoded());
                    Log.d(TAG, "DG12 Stored Hash: " + StringUtil.byteArrayToHex(dg12StoredHash));
                    Log.d(TAG, "DG12 Computed Hash: " + StringUtil.byteArrayToHex(dg12ComputedHash));

                    if (Arrays.equals(dg12StoredHash, dg12ComputedHash)) {
                        Log.d(TAG, "DG12 Hashes are matched");
                    } else {
                        hashesMatched = false;
                    }

                } catch (Exception e) {
                    Log.w(TAG, e);
                }

                // -- Security Options (if exist) -- //
                try {
                    CardFileInputStream dg14In = service.getInputStream(PassportService.EF_DG14);
                    DG14File dg14File = new DG14File(dg14In);
                    byte[] dg14StoredHash = sodFile.getDataGroupHashes().get(14);
                    byte[] dg14ComputedHash = digest.digest(dg14File.getEncoded());

                    Log.d(TAG, "DG14 Stored Hash: " + StringUtil.byteArrayToHex(dg14StoredHash));
                    Log.d(TAG, "DG14 Computed Hash: " + StringUtil.byteArrayToHex(dg14ComputedHash));

                    if (Arrays.equals(dg14StoredHash, dg14ComputedHash)) {
                        Log.d(TAG, "DG14 Hashes are matched");
                    } else {
                        hashesMatched = false;
                    }

                    // Chip Authentication
                    List<EACCAResult> eaccaResults = new ArrayList<>();
                    List<ChipAuthenticationPublicKeyInfo> chipAuthenticationPublicKeyInfos = new ArrayList<>();
                    ChipAuthenticationInfo chipAuthenticationInfo = null;

                    if (!dg14File.getSecurityInfos().isEmpty()) {
                        for (SecurityInfo securityInfo : dg14File.getSecurityInfos()) {
                            Log.d(TAG, "DG14 Security Info Identifier: " + securityInfo.getObjectIdentifier());
                            if (securityInfo instanceof ChipAuthenticationInfo) {
                                chipAuthenticationInfo = (ChipAuthenticationInfo) securityInfo;
                            } else if (securityInfo instanceof ChipAuthenticationPublicKeyInfo) {
                                chipAuthenticationPublicKeyInfos.add((ChipAuthenticationPublicKeyInfo) securityInfo);
                            }
                        }

                        for (ChipAuthenticationPublicKeyInfo chipAuthenticationPublicKeyInfo: chipAuthenticationPublicKeyInfos) {
                            if (chipAuthenticationInfo != null) {
                                EACCAResult eaccaResult = service.doEACCA(chipAuthenticationInfo.getKeyId(), chipAuthenticationInfo.getObjectIdentifier(), chipAuthenticationInfo.getProtocolOIDString(), chipAuthenticationPublicKeyInfo.getSubjectPublicKey());
                                eaccaResults.add(eaccaResult);
                            } else {
                                Log.d(TAG, "Chip Authentication failed for key: " + chipAuthenticationPublicKeyInfo.toString());
                            }
                        }

                        if (eaccaResults.size() == 0)
                            chipAuth = false;
                    }

                    /*
                        if (paceSucceeded) {
                            service.doEACTA(caReference, terminalCerts, privateKey, null, eaccaResults.get(0), mrzInfo.getDocumentNumber())
                        } else {
                            service.doEACTA(caReference, terminalCerts, privateKey, null, eaccaResults.get(0), paceSucceeded)
                        }
                    */

                } catch (Exception e) {
                    Log.w(TAG, e);
                }

                // -- Document (Active Authentication) Public Key -- //
                try {
                    CardFileInputStream dg15In = service.getInputStream(PassportService.EF_DG15);
                    DG15File dg15File = new DG15File(dg15In);

                    byte[] dg15StoredHash = sodFile.getDataGroupHashes().get(15);
                    byte[] dg15ComputedHash = digest.digest(dg15File.getEncoded());
                    Log.d(TAG, "DG15 Stored Hash: " + StringUtil.byteArrayToHex(dg15StoredHash));
                    Log.d(TAG, "DG15 Computed Hash: " + StringUtil.byteArrayToHex(dg15ComputedHash));

                    if (Arrays.equals(dg15StoredHash, dg15ComputedHash)) {
                        Log.d(TAG, "DG15 Hashes are matched");
                    } else {
                        hashesMatched = false;
                    }

                    PublicKey publicKey = dg15File.getPublicKey();
                    String publicKeyAlgorithm = publicKey.getAlgorithm();
                    eDocument.setDocPublicKey(publicKey);

                    // Active Authentication
                    if ("EC".equals(publicKeyAlgorithm) || "ECDSA".equals(publicKeyAlgorithm)) {
                        digestAlgorithm = Util.inferDigestAlgorithmFromSignatureAlgorithm("SHA1WithRSA/ISO9796-2");
                    }

                    SecureRandom sr = new SecureRandom();
                    byte[] challenge = new byte[8];
                    sr.nextBytes(challenge);
                    AAResult result = service.doAA(dg15File.getPublicKey(), sodFile.getDigestAlgorithm(), sodFile.getSignerInfoDigestAlgorithm(), challenge);
                    activeAuth = SecurityUtil.verifyAA(dg15File.getPublicKey(), digestAlgorithm, digestEncryptionAlgorithm, challenge, result.getResponse());
                    Log.d(TAG, StringUtil.byteArrayToHex(result.getResponse()));
                } catch (Exception e) {
                    Log.w(TAG, e);
                }

                eDocument.setDocType(docType);
                eDocument.setPersonDetails(personDetails);
                eDocument.setAdditionalPersonDetails(additionalPersonDetails);
                eDocument.setPassiveAuth(hashesMatched);
                eDocument.setActiveAuth(activeAuth);
                eDocument.setChipAuth(chipAuth);
                eDocument.setDocSignatureValid(docSignatureValid);

            } catch (Exception e) {
                return e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Exception exception) {
            mainLayout.setVisibility(View.VISIBLE);
            loadingLayout.setVisibility(View.GONE);

            if (exception == null) {
                setResultToView(eDocument);
            } else {
                Snackbar.make(mainLayout, exception.getLocalizedMessage(), Snackbar.LENGTH_LONG).show();
            }
        }

    }

    private void setResultToView(EDocument eDocument) {

        Bitmap image = ImageUtil.scaleImage(eDocument.getPersonDetails().getFaceImage());

        ivPhoto.setImageBitmap(image);

        String result  = "NAME: " + eDocument.getPersonDetails().getName() + "\n";
        result += "SURNAME: " + eDocument.getPersonDetails().getSurname() + "\n";
        result += "PERSONAL NUMBER: " + eDocument.getPersonDetails().getPersonalNumber() + "\n";
        result += "GENDER: " + eDocument.getPersonDetails().getGender() + "\n";
        result += "BIRTH DATE: " + eDocument.getPersonDetails().getBirthDate() + "\n";
        result += "EXPIRY DATE: " + eDocument.getPersonDetails().getExpiryDate() + "\n";
        result += "SERIAL NUMBER: " + eDocument.getPersonDetails().getSerialNumber() + "\n";
        result += "NATIONALITY: " + eDocument.getPersonDetails().getNationality() + "\n";
        result += "DOC TYPE: " + eDocument.getDocType().name() + "\n";
        result += "ISSUER AUTHORITY: " + eDocument.getPersonDetails().getIssuerAuthority() + "\n";
        result += "PASSIVE AUTH: " + eDocument.isPassiveAuth() + "\n";

        tvResult.setText(result);
    }

    private void clearViews() {
        ivPhoto.setImageBitmap(null);
        tvResult.setText("");
    }

    private void requestPermissionForCamera() {
        String[] permissions = { Manifest.permission.CAMERA };
        boolean isPermissionGranted = PermissionUtil.hasPermissions(this, permissions);

        if (!isPermissionGranted) {
            AppUtil.showAlertDialog(this, getString(R.string.permission_title), getString(R.string.permission_description), getString(R.string.button_ok), false, (dialogInterface, i) -> ActivityCompat.requestPermissions(this, permissions, PermissionUtil.REQUEST_CODE_MULTIPLE_PERMISSIONS));
        } else {
            openCameraActivity();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PermissionUtil.REQUEST_CODE_MULTIPLE_PERMISSIONS) {
            int result = grantResults[0];
            if (result == PackageManager.PERMISSION_DENIED) {
                if (!PermissionUtil.showRationale(this, permissions[0])) {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivityForResult(intent, APP_SETTINGS_ACTIVITY_REQUEST_CODE);
                } else {
                    requestPermissionForCamera();
                }
            } else if (result == PackageManager.PERMISSION_GRANTED) {
                openCameraActivity();
            }
        }
    }

}
