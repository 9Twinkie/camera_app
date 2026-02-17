package ru.rut.democamera.utils

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer

object VideoMerger {

    fun mergeVideos(outputFile: File, vararg inputFiles: File): Boolean {
        return try {
            if (inputFiles.isEmpty()) return false
            if (inputFiles.size == 1) {
                inputFiles[0].renameTo(outputFile)
                return true
            }

            val muxer = MediaMuxer(
                outputFile.absolutePath,
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            )

            var videoTrackIndex = -1
            var audioTrackIndex = -1
            var muxerStarted = false

            for ((index, inputFile) in inputFiles.withIndex()) {
                if (!inputFile.exists()) continue

                val extractor = MediaExtractor()
                extractor.setDataSource(inputFile.absolutePath)

                val trackCount = extractor.trackCount
                for (i in 0 until trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: continue

                    if (mime.startsWith("video/") && videoTrackIndex == -1) {
                        videoTrackIndex = muxer.addTrack(format)
                    } else if (mime.startsWith("audio/") && audioTrackIndex == -1) {
                        audioTrackIndex = muxer.addTrack(format)
                    }
                }

                if (!muxerStarted && (videoTrackIndex != -1 || audioTrackIndex != -1)) {
                    muxer.start()
                    muxerStarted = true
                }

                copyTracks(extractor, muxer, videoTrackIndex, audioTrackIndex)
                extractor.release()
            }

            muxer.stop()
            muxer.release()

            inputFiles.forEach { it.delete() }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun copyTracks(
        extractor: MediaExtractor,
        muxer: MediaMuxer,
        videoTrackIndex: Int,
        audioTrackIndex: Int
    ) {
        val trackCount = extractor.trackCount
        val buffer = ByteBuffer.allocate(1024 * 1024)
        val bufferInfo = MediaCodec.BufferInfo()

        for (i in 0 until trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue

            val isVideo = mime.startsWith("video/")
            val outputTrackIndex = if (isVideo) videoTrackIndex else audioTrackIndex

            if (outputTrackIndex == -1) continue

            extractor.selectTrack(i)

            while (true) {
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break

                bufferInfo.offset = 0
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = extractor.sampleTime

                val sampleFlags = extractor.sampleFlags
                bufferInfo.flags = if ((sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                    MediaCodec.BUFFER_FLAG_KEY_FRAME
                } else {
                    0
                }

                muxer.writeSampleData(outputTrackIndex, buffer, bufferInfo)
                extractor.advance()
            }

            extractor.unselectTrack(i)
        }
    }
}