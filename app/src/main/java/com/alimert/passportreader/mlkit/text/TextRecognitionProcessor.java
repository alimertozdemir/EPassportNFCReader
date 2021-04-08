package com.alimert.passportreader.mlkit.text;

import android.graphics.Color;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import com.alimert.passportreader.mlkit.other.FrameMetadata;
import com.alimert.passportreader.mlkit.other.GraphicOverlay;
import com.alimert.passportreader.model.DocType;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.common.MlKitException;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;

import net.sf.scuba.data.Gender;

import org.jmrtd.lds.icao.MRZInfo;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextRecognitionProcessor {

    private static final String TAG = TextRecognitionProcessor.class.getName();

    private final TextRecognizer textRecognizer;

    private ResultListener resultListener;

    private String scannedTextBuffer;

    private DocType docType;

    public static final String TYPE_PASSPORT = "P<";

    public static final String TYPE_ID_CARD = "I<";

    public static final String ID_CARD_TD_1_LINE_1_REGEX = "([A|C|I][A-Z0-9<]{1})([A-Z]{3})([A-Z0-9<]{31})";

    public static final String ID_CARD_TD_1_LINE_2_REGEX = "([0-9]{6})([0-9]{1})([M|F|X|<]{1})([0-9]{6})([0-9]{1})([A-Z]{3})([A-Z0-9<]{11})([0-9]{1})";

    public static final String ID_CARD_TD_1_LINE_3_REGEX = "([A-Z0-9<]{30})";

    public static final String PASSPORT_TD_3_LINE_1_REGEX = "(P[A-Z0-9<]{1})([A-Z]{3})([A-Z0-9<]{39})";

    public static final String PASSPORT_TD_3_LINE_2_REGEX = "([A-Z0-9<]{9})([0-9]{1})([A-Z]{3})([0-9]{6})([0-9]{1})([M|F|X|<]{1})([0-9]{6})([0-9]{1})([A-Z0-9<]{14})([0-9]{1})([0-9]{1})";

    // Whether we should ignore process(). This is usually caused by feeding input data faster than
    // the model can handle.
    private final AtomicBoolean shouldThrottle = new AtomicBoolean(false);

    public TextRecognitionProcessor(DocType docType, ResultListener resultListener) {
        this.docType = docType;
        this.resultListener = resultListener;
        textRecognizer = TextRecognition.getClient();
    }

    //region ----- Exposed Methods -----


    public void stop() {
        textRecognizer.close();
    }


    public void process(ByteBuffer data, FrameMetadata frameMetadata, GraphicOverlay graphicOverlay) throws MlKitException {

        if (shouldThrottle.get()) {
            return;
        }

        InputImage inputImage = InputImage.fromByteBuffer(data,
                frameMetadata.getWidth(),
                frameMetadata.getHeight(),
                frameMetadata.getRotation(),
                InputImage.IMAGE_FORMAT_NV21);

        detectInVisionImage(inputImage, frameMetadata, graphicOverlay);
    }

    //endregion

    //region ----- Helper Methods -----

    protected Task<Text> detectInImage(InputImage image) {
        return textRecognizer.process(image);
    }


    protected void onSuccess(@NonNull Text results, @NonNull FrameMetadata frameMetadata, @NonNull GraphicOverlay graphicOverlay) {

        graphicOverlay.clear();

        scannedTextBuffer = "";

        List<Text.TextBlock> blocks = results.getTextBlocks();

        for (int i = 0; i < blocks.size(); i++) {
            List<Text.Line> lines = blocks.get(i).getLines();
            for (int j = 0; j < lines.size(); j++) {
                List<Text.Element> elements = lines.get(j).getElements();
                for (int k = 0; k < elements.size(); k++) {
                    filterScannedText(graphicOverlay, elements.get(k));
                }
            }
        }
    }

    private void filterScannedText(GraphicOverlay graphicOverlay, Text.Element element) {
        GraphicOverlay.Graphic textGraphic = new TextGraphic(graphicOverlay, element, Color.GREEN);
        scannedTextBuffer += element.getText();

        if (docType == DocType.ID_CARD) {

            Pattern patternIDCardTD1Line1 = Pattern.compile(ID_CARD_TD_1_LINE_1_REGEX);
            Matcher matcherIDCardTD1Line1 = patternIDCardTD1Line1.matcher(scannedTextBuffer);

            Pattern patternIDCardTD1Line2 = Pattern.compile(ID_CARD_TD_1_LINE_2_REGEX);
            Matcher matcherIDCardTD1Line2 = patternIDCardTD1Line2.matcher(scannedTextBuffer);

            Pattern patternIDCardTD1Line3 = Pattern.compile(ID_CARD_TD_1_LINE_3_REGEX);
            Matcher matcherIDCardTD1Line3 = patternIDCardTD1Line3.matcher(scannedTextBuffer);

            if (matcherIDCardTD1Line1.find() && matcherIDCardTD1Line2.find() && matcherIDCardTD1Line3.find()) {
                graphicOverlay.add(textGraphic);
                String line1 = matcherIDCardTD1Line1.group(0);
                String line2 = matcherIDCardTD1Line2.group(0);
                String line3 = matcherIDCardTD1Line3.group(0);


                Log.d(TAG, "filterScannedText: Read line1: " + line1);
                Log.d(TAG, "filterScannedText: Read line2: " + line2);
                Log.d(TAG, "filterScannedText: Read line3: " + line3);

                if (line1.indexOf(TYPE_ID_CARD) > 0) {
                    line1 = line1.substring(line1.indexOf(TYPE_ID_CARD));
                    String documentNumber = line1.substring(5, 14);
                    documentNumber = documentNumber.replace("O", "0");
                    String dateOfBirthDay = line2.substring(0, 6);
                    String expiryDate = line2.substring(8, 14);
                    Log.d(TAG, "Scanned Text Buffer ID Card ->>>> " + "Doc Number: " + documentNumber + " DateOfBirth: " + dateOfBirthDay + " ExpiryDate: " + expiryDate);

                    MRZInfo mrzInfo = buildTempMrz(documentNumber, dateOfBirthDay, expiryDate);

                    if (mrzInfo != null)
                        finishScanning(mrzInfo);
                }
            }
        } else if (docType == DocType.PASSPORT) {

            Pattern patternPassportTD3Line1 = Pattern.compile(PASSPORT_TD_3_LINE_1_REGEX);
            Matcher matcherPassportTD3Line1 = patternPassportTD3Line1.matcher(scannedTextBuffer);

            Pattern patternPassportTD3Line2 = Pattern.compile(PASSPORT_TD_3_LINE_2_REGEX);
            Matcher matcherPassportTD3Line2 = patternPassportTD3Line2.matcher(scannedTextBuffer);

            if (matcherPassportTD3Line1.find() && matcherPassportTD3Line2.find()) {
                graphicOverlay.add(textGraphic);
                String line2 = matcherPassportTD3Line2.group(0);
                String documentNumber = line2.substring(0, 9);
                documentNumber = documentNumber.replace("O", "0");
                String dateOfBirthDay = line2.substring(13, 19);
                String expiryDate = line2.substring(21, 27);

                Log.d(TAG, "Scanned Text Buffer Passport ->>>> " + "Doc Number: " + documentNumber + " DateOfBirth: " + dateOfBirthDay + " ExpiryDate: " + expiryDate);

                MRZInfo mrzInfo = buildTempMrz(documentNumber, dateOfBirthDay, expiryDate);

                if (mrzInfo != null)
                    finishScanning(mrzInfo);
            }
        }
    }

    protected void onFailure(@NonNull Exception e) {
        Log.w(TAG, "Text detection failed." + e);
        resultListener.onError(e);
    }

    private void detectInVisionImage(InputImage image, final FrameMetadata metadata, final GraphicOverlay graphicOverlay) {

        detectInImage(image)
                .addOnSuccessListener(
                        new OnSuccessListener<Text>() {
                            @Override
                            public void onSuccess(Text results) {
                                shouldThrottle.set(false);
                                TextRecognitionProcessor.this.onSuccess(results, metadata, graphicOverlay);
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                shouldThrottle.set(false);
                                TextRecognitionProcessor.this.onFailure(e);
                            }
                        });
        // Begin throttling until this frame of input has been processed, either in onSuccess or
        // onFailure.
        shouldThrottle.set(true);
    }

    private void finishScanning(final MRZInfo mrzInfo) {
        try {
            if (isMrzValid(mrzInfo)) {
                // Delay returning result 1 sec. in order to make mrz text become visible on graphicOverlay by user
                // You want to call 'resultListener.onSuccess(mrzInfo)' without no delay
                new Handler().postDelayed(() -> resultListener.onSuccess(mrzInfo), 1000);
            }

        } catch (Exception exp) {
            Log.d(TAG, "MRZ DATA is not valid");
        }
    }

    private MRZInfo buildTempMrz(String documentNumber, String dateOfBirth, String expiryDate) {
        MRZInfo mrzInfo = null;
        try {
            mrzInfo = new MRZInfo("P", "NNN", "", "", documentNumber, "NNN", dateOfBirth, Gender.UNSPECIFIED, expiryDate, "");
        } catch (Exception e) {
            Log.d(TAG, "MRZInfo error : " + e.getLocalizedMessage());
        }

        return mrzInfo;
    }

    private boolean isMrzValid(MRZInfo mrzInfo) {
        return mrzInfo.getDocumentNumber() != null && mrzInfo.getDocumentNumber().length() >= 8 &&
                mrzInfo.getDateOfBirth() != null && mrzInfo.getDateOfBirth().length() == 6 &&
                mrzInfo.getDateOfExpiry() != null && mrzInfo.getDateOfExpiry().length() == 6;
    }

    public interface ResultListener {
        void onSuccess(MRZInfo mrzInfo);

        void onError(Exception exp);
    }
}

