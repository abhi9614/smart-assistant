package com.tricorder.smartassistant.service;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.transcribe.AmazonTranscribe;
import com.amazonaws.services.transcribe.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tricorder.smartassistant.dto.response.TranscriptionResponseDTO;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.utility.RandomString;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


@Slf4j
@Service
public class TranscribeServiceImpl {

    @Value("${smart-assistant.bucket.transcribe}")
    private String bucketName;
    @Autowired
    private AmazonS3 s3Client;

    @Autowired
    private AmazonTranscribe transcribeClient;

    public String check() {
        System.out.println("\n\n----------\n\n");
        System.out.println("\n\n----------\n\n");
        return "success";

    }


    public void uploadFileToS3Bucket(MultipartFile file) {
        log.info("uploading file {} to s3 bucket {}", file, bucketName);
        String key = Objects.requireNonNull(file.getOriginalFilename(), "file name can't be null")
                .replaceAll(" ", "_").toLowerCase();
        try {
            s3Client.putObject(bucketName, key, file.getInputStream(), null);
        } catch (SdkClientException | IOException e) {
            log.error("exception occur while uploading file into s3", e);
            throw new RuntimeException(e);
        }
    }


    public void deleteFileFromS3Bucket(String fileName) {
        log.info("delete file from s3 bucket {} with name {}", bucketName, fileName);
        String key = fileName.replaceAll(" ", "_").toLowerCase();
        s3Client.deleteObject(bucketName, key);
    }


    /*************** Step 4 ******************
     * **** Start Transcription Job Method******/

    StartTranscriptionJobResult startTranscriptionJob(String key) {
        log.debug("Start Transcription Job By Key {}", key);
        Media media = new Media().withMediaFileUri(s3Client.getUrl(bucketName, key).toExternalForm());
        String jobName = key.concat(RandomString.make());
        StartTranscriptionJobRequest startTranscriptionJobRequest = new StartTranscriptionJobRequest()
                .withLanguageCode(LanguageCode.EnUS).withTranscriptionJobName(jobName).withMedia(media);
        StartTranscriptionJobResult startTranscriptionJobResult = transcribeClient
                .startTranscriptionJob(startTranscriptionJobRequest);
        return startTranscriptionJobResult;
    }


    /**************************Step 5*****************
     * *** Get Transcription Job Result method *********/

    GetTranscriptionJobResult getTranscriptionJobResult(String jobName) {
        log.debug("Get Transcription Job Result By Job Name : {}", jobName);
        GetTranscriptionJobRequest getTranscriptionJobRequest = new GetTranscriptionJobRequest()
                .withTranscriptionJobName(jobName);
        Boolean resultFound = false;
        TranscriptionJob transcriptionJob = new TranscriptionJob();
        GetTranscriptionJobResult getTranscriptionJobResult = new GetTranscriptionJobResult();
        while (resultFound == false) {
            getTranscriptionJobResult = transcribeClient.getTranscriptionJob(getTranscriptionJobRequest);
            transcriptionJob = getTranscriptionJobResult.getTranscriptionJob();
            if (transcriptionJob.getTranscriptionJobStatus()
                    .equalsIgnoreCase(TranscriptionJobStatus.COMPLETED.name())) {
                return getTranscriptionJobResult;
            } else if (transcriptionJob.getTranscriptionJobStatus()
                    .equalsIgnoreCase(TranscriptionJobStatus.FAILED.name())) {
                return null;
            } else if (transcriptionJob.getTranscriptionJobStatus()
                    .equalsIgnoreCase(TranscriptionJobStatus.IN_PROGRESS.name())) {
                try {
                    Thread.sleep(15000);
                } catch (InterruptedException e) {
                    log.debug("Interrupted Exception {}", e.getMessage());
                }
            }
        }
        return getTranscriptionJobResult;
    }

    /******************Step 6 **************************
     *  Download Transcription Result from URI Method *********/

    TranscriptionResponseDTO downloadTranscriptionResponse(String uri) {
        log.debug("Download Transcription Result from Transcribe URi {}", uri);
        OkHttpClient okHttpClient = new OkHttpClient()
                .newBuilder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        Request request = new Request.Builder().url(uri).build();
        Response response;
        try {
            response = okHttpClient.newCall(request).execute();
            String body = response.body().string();
            ObjectMapper objectMapper = new ObjectMapper();
            response.close();
            return objectMapper.readValue(body, TranscriptionResponseDTO.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /***************************** Step 7 *****************************
     * **** Delete Transcription Job Method ************
     * TO delete transcription job after getting result****/

    void deleteTranscriptionJob(String jobName) {
        log.debug("Delete Transcription Job from amazon Transcribe {}", jobName);
        DeleteTranscriptionJobRequest deleteTranscriptionJobRequest = new DeleteTranscriptionJobRequest()
                .withTranscriptionJobName(jobName);
        transcribeClient.deleteTranscriptionJob(deleteTranscriptionJobRequest);
    }


    /********************************* Step 8****************************************
     *  Extract Speech Text method that combines all methods to a single method******
     ***** You can do skip upload delete methods if you want to just process file in AWs
     * ** by passing key for filename in bucket and create media *****/

    public TranscriptionResponseDTO extractSpeechTextFromVideo(MultipartFile file) {
        log.debug("Request to extract Speech Text from Video : {}", file);

        // Upload file to Aws
        uploadFileToS3Bucket(file);

        // Create a key that is like name for file and will be used for creating unique name based id for transcription job
        String key = file.getOriginalFilename().replaceAll(" ", "_").toLowerCase();

        // Start Transcription Job and get result
        StartTranscriptionJobResult startTranscriptionJobResult = startTranscriptionJob(key);


        // Get name of job started for the file
        String transcriptionJobName = startTranscriptionJobResult.getTranscriptionJob().getTranscriptionJobName();

        // Get result after the procesiing is complete
        GetTranscriptionJobResult getTranscriptionJobResult = getTranscriptionJobResult(transcriptionJobName);

        //delete file as processing is done
        deleteFileFromS3Bucket(key);

        // Url of result file for transcription
        String transcriptFileUriString = getTranscriptionJobResult.getTranscriptionJob().getTranscript().getTranscriptFileUri();

        // Get the transcription response by downloading the file
        TranscriptionResponseDTO transcriptionResponseDTO = downloadTranscriptionResponse(transcriptFileUriString);

        //Delete the transcription job after finishing or it will get deleted after 90 days automatically if you do not call
        deleteTranscriptionJob(transcriptionJobName);

        return transcriptionResponseDTO;
    }

}
