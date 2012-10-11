// Copyright (c) 2012 Wolf Paulus - Tech Casita Productions
package com.techcasita.android.bot1;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * <code>Bot1</code> is a simple <i>Echo Bot</i>, capturing voice input, converting it in to text,
 * synthesizing the text into speech.
 *
 * @author <a href="mailto:wolf@wolfpaulus.com">Wolf Paulus</a>
 */
public class Bot1 extends Activity implements TextToSpeech.OnInitListener {
    private static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;
    private static final String UTTERANCE_ID = Bot1.class.getSimpleName();

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
            mTV_TTS.setText(R.string.err_Recognizer);
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

            startVoiceRecognitionActivity();

            //
            // OnUtteranceCompletedListener
            //

            //noinspection deprecation
            mTts.setOnUtteranceCompletedListener(new TextToSpeech.OnUtteranceCompletedListener() {
                @Override
                public void onUtteranceCompleted(final String s) {
                    startVoiceRecognitionActivity();
                }
            });
        } else {
            mTV_TTS.setText(R.string.err_TextToSpeech);
        }
    }

    /**
     * In case the speech recognizer times out, tapping on the screen restarts
     *
     * @param view <code.View</code> not used
     */
    @SuppressWarnings("UnusedParameters")
    public void onClick(View view) {
        startVoiceRecognitionActivity();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case VOICE_RECOGNITION_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    final ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    say(matches.get(0));
                } else {
                    mTV_STT.setText(R.string.tapScreen);
                }
                break;
            default:
                mTV_STT.setText("");
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
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, R.string.speakPROMPT);
        // Given an hint to the recognizer about what the user is going to say
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        // Specify how many results you want to receive. The results will be sorted
        // where the first result is the one with higher confidence.
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        //intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, new Locale("es").getLanguage());
        startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
    }
}
