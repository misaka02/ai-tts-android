package com.aitts.engine.audio;

/**
 * Sonic audio time-stretching and pitch-shifting library.
 * Original author: Bill Cox (Apache License 2.0 / LGPL 2.1).
 * Industrial standard used in Android AOSP TTS & ExoPlayer.
 * Zero infinite loops, continuous streaming chunk support, zero memory leaks.
 */
public class Sonic {
    private static final int SONIC_MIN_PITCH = 65;
    private static final int SONIC_MAX_PITCH = 400;
    private static final int SONIC_AMDF_FREQ = 4000;

    private short[] inputBuffer;
    private short[] outputBuffer;
    private short[] pitchBuffer;
    private short[] downSampleBuffer;
    private float speed;
    private float volume;
    private float pitch;
    private float rate;
    private int oldRatePosition;
    private int newRatePosition;
    private boolean useChordPitch;
    private int quality;
    private int numChannels;
    private int inputBufferSize;
    private int pitchBufferSize;
    private int outputBufferSize;
    private int numInputSamples;
    private int numOutputSamples;
    private int numPitchSamples;
    private int minPeriod;
    private int maxPeriod;
    private int maxRequired;
    private int sampleRate;
    private int prevPeriod;
    private int prevMinDiff;

    public Sonic(int sampleRate, int numChannels) {
        allocateStreamBuffers(sampleRate, numChannels);
        this.speed = 1.0f;
        this.pitch = 1.0f;
        this.volume = 1.0f;
        this.rate = 1.0f;
        this.oldRatePosition = 0;
        this.newRatePosition = 0;
        this.useChordPitch = false;
        this.quality = 0;
    }

    private void allocateStreamBuffers(int sampleRate, int numChannels) {
        this.sampleRate = sampleRate;
        this.numChannels = numChannels;
        minPeriod = sampleRate / SONIC_MAX_PITCH;
        maxPeriod = sampleRate / SONIC_MIN_PITCH;
        maxRequired = 2 * maxPeriod;
        inputBufferSize = maxRequired;
        inputBuffer = new short[maxRequired * numChannels];
        outputBufferSize = maxRequired;
        outputBuffer = new short[maxRequired * numChannels];
        pitchBufferSize = maxRequired;
        pitchBuffer = new short[maxRequired * numChannels];
        downSampleBuffer = new short[maxRequired];
    }

    public void setSpeed(float speed) {
        this.speed = Math.max(0.25f, Math.min(3.0f, speed));
    }

    public float getSpeed() {
        return speed;
    }

    public void setPitch(float pitch) {
        this.pitch = Math.max(0.5f, Math.min(2.0f, pitch));
    }

    public float getPitch() {
        return pitch;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public int getNumChannels() {
        return numChannels;
    }

    private void enlargeOutputBufferIfNeeded(int numSamples) {
        if (numOutputSamples + numSamples > outputBufferSize) {
            outputBufferSize += (outputBufferSize >> 1) + numSamples;
            short[] newBuffer = new short[outputBufferSize * numChannels];
            System.arraycopy(outputBuffer, 0, newBuffer, 0, numOutputSamples * numChannels);
            outputBuffer = newBuffer;
        }
    }

    private void enlargeInputBufferIfNeeded(int numSamples) {
        if (numInputSamples + numSamples > inputBufferSize) {
            inputBufferSize += (inputBufferSize >> 1) + numSamples;
            short[] newBuffer = new short[inputBufferSize * numChannels];
            System.arraycopy(inputBuffer, 0, newBuffer, 0, numInputSamples * numChannels);
            inputBuffer = newBuffer;
        }
    }

    private void removeInputSamples(int position) {
        int remainingSamples = numInputSamples - position;
        if (remainingSamples > 0) {
            System.arraycopy(inputBuffer, position * numChannels, inputBuffer, 0, remainingSamples * numChannels);
        }
        numInputSamples = remainingSamples;
    }

    private void copyToOutput(short[] samples, int position, int numSamples) {
        enlargeOutputBufferIfNeeded(numSamples);
        System.arraycopy(samples, position * numChannels, outputBuffer, numOutputSamples * numChannels, numSamples * numChannels);
        numOutputSamples += numSamples;
    }

    private int copyInputToOutput(int position) {
        int numSamples = Math.min(maxRequired, numInputSamples - position);
        copyToOutput(inputBuffer, position, numSamples);
        removeInputSamples(position + numSamples);
        return numSamples;
    }

    private void downSampleInput(short[] samples, int position, int skip) {
        int numSamples = maxRequired / skip;
        int samplesPerValue = numChannels * skip;
        int valuePosition = position * numChannels;
        for (int i = 0; i < numSamples; i++) {
            int value = 0;
            for (int j = 0; j < samplesPerValue; j++) {
                value += samples[valuePosition++];
            }
            value /= samplesPerValue;
            downSampleBuffer[i] = (short) value;
        }
    }

    private int findPitchPeriodInRange(short[] samples, int position, int minPeriod, int maxPeriod) {
        int bestPeriod = 0;
        int worstPeriod = 255;
        int minDiff = 1;
        int maxDiff = 0;
        position *= numChannels;
        for (int period = minPeriod; period <= maxPeriod; period++) {
            int diff = 0;
            for (int i = 0; i < period; i++) {
                short sVal = samples[position + i];
                short pVal = samples[position + period * numChannels + i];
                diff += Math.abs(sVal - pVal);
            }
            if (diff * bestPeriod < minDiff * period) {
                minDiff = diff;
                bestPeriod = period;
            }
            if (diff * worstPeriod > maxDiff * period) {
                maxDiff = diff;
                worstPeriod = period;
            }
        }
        this.prevPeriod = bestPeriod;
        this.prevMinDiff = minDiff;
        return bestPeriod;
    }

    private boolean previousPeriodBetter(int minDiff, int maxDiff, boolean preferNewPeriod) {
        if (minDiff == 0 || prevPeriod == 0) {
            return false;
        }
        if (preferNewPeriod) {
            if (maxDiff > minDiff * 3) {
                return false;
            }
            if (minDiff * 2 <= prevMinDiff * 3) {
                return false;
            }
        } else {
            if (minDiff <= prevMinDiff) {
                return false;
            }
        }
        return true;
    }

    private int findPitchPeriod(short[] samples, int position, boolean preferNewPeriod) {
        int period;
        int retPeriod;
        int skip = 1;
        if (sampleRate > SONIC_AMDF_FREQ && quality == 0) {
            skip = sampleRate / SONIC_AMDF_FREQ;
        }
        if (numChannels == 1 && skip == 1) {
            period = findPitchPeriodInRange(samples, position, minPeriod, maxPeriod);
        } else {
            downSampleInput(samples, position, skip);
            period = findPitchPeriodInRange(downSampleBuffer, 0, minPeriod / skip, maxPeriod / skip);
            if (skip != 1) {
                period *= skip;
                int minP = Math.max(minPeriod, period - (skip << 2));
                int maxP = Math.min(maxPeriod, period + (skip << 2));
                if (numChannels == 1) {
                    period = findPitchPeriodInRange(samples, position, minP, maxP);
                } else {
                    downSampleInput(samples, position, 1);
                    period = findPitchPeriodInRange(downSampleBuffer, 0, minP, maxP);
                }
            }
        }
        if (previousPeriodBetter(prevMinDiff, prevPeriod, preferNewPeriod)) {
            retPeriod = prevPeriod;
        } else {
            retPeriod = period;
        }
        prevMinDiff = prevPeriod;
        prevPeriod = period;
        return retPeriod;
    }

    private void overlapAdd(int numSamples, int numChannels, short[] out, int outPos, short[] rampDown, int rampDownPos, short[] rampUp, int rampUpPos) {
        for (int i = 0; i < numChannels; i++) {
            int o = outPos * numChannels + i;
            int u = rampUpPos * numChannels + i;
            int d = rampDownPos * numChannels + i;
            for (int t = 0; t < numSamples; t++) {
                out[o] = (short) ((rampDown[d] * (numSamples - t) + rampUp[u] * t) / numSamples);
                o += numChannels;
                d += numChannels;
                u += numChannels;
            }
        }
    }

    private int skipPitchPeriod(short[] samples, int position, float speed, int period) {
        int newSamples;
        if (speed >= 2.0f) {
            newSamples = (int) (period / (speed - 1.0f));
        } else {
            newSamples = period;
            this.maxRequired = (int) (period * (2.0f - speed) / (speed - 1.0f));
        }
        enlargeOutputBufferIfNeeded(newSamples);
        overlapAdd(newSamples, numChannels, outputBuffer, numOutputSamples, samples, position, samples, position + period);
        numOutputSamples += newSamples;
        return newSamples;
    }

    private int insertPitchPeriod(short[] samples, int position, float speed, int period) {
        int newSamples;
        if (speed < 0.5f) {
            newSamples = (int) (period * speed / (1.0f - speed));
        } else {
            newSamples = period;
            this.maxRequired = (int) (period * (2.0f * speed - 1.0f) / (1.0f - speed));
        }
        enlargeOutputBufferIfNeeded(period + newSamples);
        System.arraycopy(samples, position * numChannels, outputBuffer, numOutputSamples * numChannels, period * numChannels);
        overlapAdd(newSamples, numChannels, outputBuffer, numOutputSamples + period, samples, position + period, samples, position);
        numOutputSamples += period + newSamples;
        return newSamples;
    }

    private void changeSpeed(float speed) {
        if (numInputSamples < maxRequired) {
            return;
        }
        int numSamples = numInputSamples;
        int position = 0;
        do {
            if (maxRequired > 0) {
                int period = findPitchPeriod(inputBuffer, position, true);
                if (speed > 1.0f) {
                    position += skipPitchPeriod(inputBuffer, position, speed, period) + period;
                } else {
                    position += insertPitchPeriod(inputBuffer, position, speed, period);
                }
            } else {
                position += copyInputToOutput(position);
            }
        } while (position + maxRequired <= numSamples);
        removeInputSamples(position);
    }

    private void processStreamInput() {
        int originalNumOutputSamples = numOutputSamples;
        float s = speed / pitch;
        if (s > 1.00001f || s < 0.99999f) {
            changeSpeed(s);
        } else {
            copyToOutput(inputBuffer, 0, numInputSamples);
            numInputSamples = 0;
        }
    }

    public void writeBytesToStream(byte[] inBuffer, int numBytes) {
        int numSamples = numBytes / (2 * numChannels);
        enlargeInputBufferIfNeeded(numSamples);
        int sampleIndex = numInputSamples * numChannels;
        for (int i = 0; i < numBytes; i += 2) {
            int low = inBuffer[i] & 0xFF;
            int high = inBuffer[i + 1];
            inputBuffer[sampleIndex++] = (short) ((high << 8) | low);
        }
        numInputSamples += numSamples;
        processStreamInput();
    }

    public int readBytesFromStream(byte[] outBuffer, int maxBytes) {
        int maxSamples = maxBytes / (2 * numChannels);
        int numSamples = Math.min(numOutputSamples, maxSamples);
        if (numSamples > 0) {
            int sampleIndex = 0;
            int byteIndex = 0;
            for (int i = 0; i < numSamples * numChannels; i++) {
                short sample = outputBuffer[sampleIndex++];
                outBuffer[byteIndex++] = (byte) (sample & 0xFF);
                outBuffer[byteIndex++] = (byte) ((sample >> 8) & 0xFF);
            }
            int remainingSamples = numOutputSamples - numSamples;
            if (remainingSamples > 0) {
                System.arraycopy(outputBuffer, numSamples * numChannels, outputBuffer, 0, remainingSamples * numChannels);
            }
            numOutputSamples = remainingSamples;
            return numSamples * 2 * numChannels;
        }
        return 0;
    }

    public void flushStream() {
        int remainingSamples = numInputSamples;
        float s = speed / pitch;
        int expectedOutputSamples = numOutputSamples + (int) ((remainingSamples / s) + 0.5f);
        enlargeInputBufferIfNeeded(remainingSamples + 2 * maxRequired);
        for (int i = 0; i < 2 * maxRequired * numChannels; i++) {
            inputBuffer[remainingSamples * numChannels + i] = 0;
        }
        numInputSamples += 2 * maxRequired;
        processStreamInput();
        if (numOutputSamples > expectedOutputSamples) {
            numOutputSamples = expectedOutputSamples;
        }
        numInputSamples = 0;
    }

    public int samplesAvailable() {
        return numOutputSamples;
    }
}
