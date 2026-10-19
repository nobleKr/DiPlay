package com.shilapi.xcertplay.media

import android.media.MediaRecorder
import org.junit.Assert.assertEquals
import org.junit.Test

class MicrophoneSourceTest {
    @Test fun zeekrUsesPlainMic() {
        assertEquals(MediaRecorder.AudioSource.MIC,
            speechRecognitionMicrophoneSource(zeekrHeadUnit = true, geelyHeadUnit = false))
        // Zeekr wins even when the Geely matcher also fires.
        assertEquals(MediaRecorder.AudioSource.MIC,
            speechRecognitionMicrophoneSource(zeekrHeadUnit = true, geelyHeadUnit = true))
    }

    @Test fun geelyUsesVoiceCommunication() {
        assertEquals(MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            speechRecognitionMicrophoneSource(zeekrHeadUnit = false, geelyHeadUnit = true))
    }

    @Test fun othersKeepVoiceRecognition() {
        assertEquals(MediaRecorder.AudioSource.VOICE_RECOGNITION,
            speechRecognitionMicrophoneSource(zeekrHeadUnit = false, geelyHeadUnit = false))
    }
}
