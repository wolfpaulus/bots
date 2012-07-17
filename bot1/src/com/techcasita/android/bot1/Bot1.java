// Android Bots by Wolf Paulus is licensed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License.
package com.techcasita.android.bot1;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class Bot1 extends Activity implements TextToSpeech.OnInitListener {
    private static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;
    private static final String UTTERANCE_ID = "BOT1_123";

    private TextView mTV_TTS;
    private TextView mTV_STT;
    private TextToSpeech mTts;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mTV_STT = (TextView) findViewById(R.id.tv_stt);
        mTV_TTS = (TextView) findViewById(R.id.tv_tts);
        mTV_TTS.setMovementMethod(new ScrollingMovementMethod());
        // Check to see if a recognition activity is present
        List<ResolveInfo> activities = getPackageManager().queryIntentActivities(
                new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
        if (activities.size() == 0) {
            mTV_TTS.setText("Recognizer not present");
        }
        // Setup Text to Speech via InitListener
        try {
            mTts = new TextToSpeech(this, this);
        } catch (Throwable t) {
            mTV_TTS.setText(t.getMessage());
        }
    }

    @Override
    public void onInit(final int status) {
        // status can be either TextToSpeech.SUCCESS or TextToSpeech.ERROR.
        if (status == TextToSpeech.SUCCESS && mTts != null) {
            mTts.setSpeechRate(1.0f);
            mTts.setPitch(1.5f);
            // Set preferred language to US english.
            // Note that a language may not be available, and the result will indicate this.
            final int result = mTts.setLanguage(0 <= mTts.isLanguageAvailable(Locale.UK) ? Locale.UK : Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                mTV_TTS.setText("Language is not available.");
            } else {
                startVoiceRecognitionActivity();
            }
            //
            // OnUtteranceCompletedListener
            //
            //noinspection deprecation
            mTts.setOnUtteranceCompletedListener(new TextToSpeech.OnUtteranceCompletedListener() {
                @Override
                public void onUtteranceCompleted(String s) {
                    startVoiceRecognitionActivity();
                }
            });
        } else {
            mTV_TTS.setText("Could not initialize TextToSpeech.");
        }
    }

    private void say(final String s) {
        final HashMap<String, String> map = new HashMap<String, String>(1);
        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UTTERANCE_ID);
        mTts.speak(s, TextToSpeech.QUEUE_FLUSH, map);
        mTV_TTS.setText(s);
    }

    /**
     * Fire an intent to start the speech recognition activity.
     */
    private void startVoiceRecognitionActivity() {
        final Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        // Specify the calling package to identify your application
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getClass().getPackage().getName());
        // Display an hint to the user about what he should say.
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Bot1");
        // Given an hint to the recognizer about what the user is going to say
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        // Specify how many results you want to receive. The results will be sorted
        // where the first result is the one with higher confidence.
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        //intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, new Locale("es").getLanguage());
        startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case VOICE_RECOGNITION_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    final ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    say(matches.get(0));
                } else {
                    mTV_STT.setText("");
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        if (mTts != null) {
            mTts.shutdown();
        }
        super.onDestroy();
    }
}
